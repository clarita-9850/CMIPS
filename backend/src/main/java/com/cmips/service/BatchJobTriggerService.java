package com.cmips.service;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for triggering Spring Batch jobs from external requests (Scheduler App).
 *
 * Responsibilities:
 * 1. Look up registered Spring Batch Job beans by name
 * 2. Build JobParameters from request parameters
 * 3. Launch jobs asynchronously using the async job launcher
 * 4. Stop running jobs on request
 *
 * Concurrency Strategy:
 * Uses a ReentrantLock to serialize ONLY the metadata creation phase.
 * This solves PostgreSQL SERIALIZABLE transaction conflicts in Spring Batch
 * metadata tables while allowing job execution to run fully in parallel.
 *
 * - Metadata creation (INSERT into BATCH_* tables): Serialized via lock
 * - Job execution (actual business logic): Fully parallel via asyncJobLauncher
 *
 * This gives 100% success rate with maximum parallel job execution throughput.
 */
@Service
@Slf4j
public class BatchJobTriggerService {

    private final ApplicationContext applicationContext;
    private final JobRepository jobRepository;
    private final JobExplorer jobExplorer;
    private final JobLauncher asyncJobLauncher;

    /**
     * Lock to serialize ONLY the metadata creation (jobLauncher.run).
     * This prevents PostgreSQL serialization conflicts while allowing
     * job execution to run fully in parallel.
     */
    private final ReentrantLock metadataLock = new ReentrantLock(true); // fair lock

    /**
     * Counter for tracking trigger requests.
     */
    private final AtomicLong triggerCounter = new AtomicLong(0);

    @Value("${batch.trigger.queue-timeout-seconds:120}")
    private int queueTimeoutSeconds;

    public BatchJobTriggerService(
            ApplicationContext applicationContext,
            JobRepository jobRepository,
            JobExplorer jobExplorer,
            @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher) {
        this.applicationContext = applicationContext;
        this.jobRepository = jobRepository;
        this.jobExplorer = jobExplorer;
        this.asyncJobLauncher = asyncJobLauncher;
    }

    @PostConstruct
    public void init() {
        log.info("BatchJobTriggerService initialized with metadata lock serialization, {}s timeout", queueTimeoutSeconds);
    }

    /**
     * Trigger a Spring Batch job by name.
     *
     * Uses a ReentrantLock to serialize ONLY the metadata creation (jobLauncher.run).
     * This prevents PostgreSQL serialization conflicts while allowing job execution
     * to run fully in parallel via the asyncJobLauncher.
     *
     * Flow:
     * 1. Request arrives (can be 100s concurrently)
     * 2. Each request waits for metadataLock (serialized)
     * 3. Once lock acquired, jobLauncher.run() creates metadata and starts job
     * 4. Lock released immediately - job continues executing in parallel
     * 5. Response returned to caller
     *
     * @param jobName Name of the job bean (e.g., "countyDailyReportJob")
     * @param triggerId Trigger ID from the Scheduler app for correlation (UUID string)
     * @param parameters Additional parameters for the job
     * @return JobExecution containing the execution ID and initial status
     */
    public JobExecution triggerJob(String jobName, String triggerId, Map<String, String> parameters)
            throws Exception {

        long requestNum = triggerCounter.incrementAndGet();
        log.debug("Trigger request #{} for job: {} with triggerId: {} (waiting for lock, queue depth: {})",
                requestNum, jobName, triggerId, metadataLock.getQueueLength());

        // Try to acquire lock with timeout
        boolean acquired = metadataLock.tryLock(queueTimeoutSeconds, TimeUnit.SECONDS);
        if (!acquired) {
            throw new RuntimeException("Timeout waiting for metadata lock: " + jobName +
                    ", triggerId: " + triggerId + ". Queue depth was: " + metadataLock.getQueueLength());
        }

        long lockAcquiredAt = System.currentTimeMillis();
        try {
            // Under lock: create job metadata and launch (this is the serialized part)
            log.debug("Lock acquired for request #{}, triggering job: {}", requestNum, jobName);
            JobExecution execution = doTriggerJob(jobName, triggerId, parameters);
            long lockHeldMs = System.currentTimeMillis() - lockAcquiredAt;
            log.info("Job {} triggered (request #{}), execution ID: {}, lock held: {}ms, queue: {}",
                    jobName, requestNum, execution.getId(), lockHeldMs, metadataLock.getQueueLength());
            return execution;
        } finally {
            // Release lock immediately - job execution continues in parallel
            metadataLock.unlock();
        }
    }

    /**
     * Internal method that performs the actual job trigger.
     * Called sequentially by the single-threaded executor.
     */
    @Retry(name = "batchJobTrigger", fallbackMethod = "doTriggerJobFallback")
    public JobExecution doTriggerJob(String jobName, String triggerId, Map<String, String> parameters)
            throws Exception {

        log.info("Triggering job: {} with triggerId: {}", jobName, triggerId);

        // Look up the job bean
        Job job = getJobByName(jobName);
        if (job == null) {
            throw new IllegalArgumentException("Job not found: " + jobName);
        }

        // Build job parameters
        JobParameters jobParameters = buildJobParameters(triggerId, parameters);

        // Launch the job asynchronously
        JobExecution execution = asyncJobLauncher.run(job, jobParameters);

        log.info("Job {} launched with execution ID: {}, triggerId: {}", jobName, execution.getId(), triggerId);
        return execution;
    }

    /**
     * Fallback method called when retry attempts are exhausted.
     */
    public JobExecution doTriggerJobFallback(String jobName, String triggerId,
            Map<String, String> parameters, Exception e) throws Exception {
        log.error("Failed to trigger job {} after retries. TriggerId: {}, Error: {}",
                jobName, triggerId, e.getMessage());
        throw new RuntimeException("Job trigger failed for job: " + jobName +
                ", triggerId: " + triggerId + ". Cause: " + e.getMessage(), e);
    }

    /**
     * Get current queue size for monitoring.
     * Returns the number of threads waiting for the metadata lock.
     */
    public int getQueueSize() {
        return metadataLock.getQueueLength();
    }

    /**
     * Stop a running job by execution ID.
     *
     * @param executionId Spring Batch execution ID
     * @return true if stop signal was sent, false if job not found or already completed
     */
    public boolean stopJob(Long executionId) {
        log.info("Attempting to stop job execution: {}", executionId);

        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null) {
            log.warn("Job execution not found: {}", executionId);
            return false;
        }

        if (execution.isRunning()) {
            execution.setStatus(BatchStatus.STOPPING);
            jobRepository.update(execution);
            log.info("Stop signal sent for execution: {}", executionId);
            return true;
        } else {
            log.info("Job execution {} is not running (status: {})", executionId, execution.getStatus());
            return false;
        }
    }

    /**
     * Get a Job bean by name from the application context.
     */
    private Job getJobByName(String jobName) {
        try {
            return applicationContext.getBean(jobName, Job.class);
        } catch (Exception e) {
            log.error("Failed to find job bean: {}", jobName, e);
            return null;
        }
    }

    /**
     * Build JobParameters from the request.
     *
     * Always includes:
     * - triggerId: For correlation with Scheduler app (UUID string)
     * - timestamp: To make each run unique (required by Spring Batch)
     *
     * Plus any additional parameters from the request.
     */
    private JobParameters buildJobParameters(String triggerId, Map<String, String> parameters) {
        JobParametersBuilder builder = new JobParametersBuilder();

        // Always include trigger ID (for correlation with Scheduler) and timestamp
        if (triggerId != null) {
            builder.addString("triggerId", triggerId, true);
        }
        builder.addLong("timestamp", Instant.now().toEpochMilli(), true);

        // Add any additional parameters from the request
        if (parameters != null) {
            parameters.forEach((key, value) -> {
                if (value != null) {
                    builder.addString(key, value, false);
                }
            });
        }

        return builder.toJobParameters();
    }

    /**
     * Get all registered job names for discovery.
     */
    public Set<String> getRegisteredJobNames() {
        return Set.of(applicationContext.getBeanNamesForType(Job.class));
    }

    /**
     * Find a job execution by triggerId (scheduler correlation ID).
     *
     * This method searches recent job executions for one that has the matching triggerId
     * in its job parameters. The triggerId is a UUID string passed by the scheduler
     * when triggering jobs.
     *
     * @param triggerId The trigger ID from the scheduler
     * @return The JobExecution if found, null otherwise
     */
    public JobExecution findExecutionByTriggerId(String triggerId) {
        if (triggerId == null || triggerId.isBlank()) {
            return null;
        }

        // Get all job names and search through recent executions
        for (String jobName : jobExplorer.getJobNames()) {
            // Get recent job instances
            for (var jobInstance : jobExplorer.getJobInstances(jobName, 0, 100)) {
                // Get all executions for this instance
                for (var execution : jobExplorer.getJobExecutions(jobInstance)) {
                    String execTriggerId = execution.getJobParameters().getString("triggerId");
                    if (triggerId.equals(execTriggerId)) {
                        return execution;
                    }
                }
            }
        }

        log.debug("No execution found for triggerId: {}", triggerId);
        return null;
    }
}
