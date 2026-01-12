package com.cmips.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Spring Batch Configuration for Trial Application with Virtual Threads
 *
 * Virtual Thread Benefits (Java 21+):
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ Platform Threads          │  Virtual Threads                       │
 * ├───────────────────────────┼─────────────────────────────────────────┤
 * │ ~1MB memory per thread    │  ~1KB memory per thread (1000x less)   │
 * │ OS-managed scheduling     │  JVM-managed scheduling                │
 * │ Blocking I/O blocks thread│  Blocking I/O yields to other tasks    │
 * │ Limited scalability       │  Millions of concurrent tasks possible │
 * │ Thread pools required     │  No pooling needed                     │
 * └───────────────────────────┴─────────────────────────────────────────┘
 *
 * This configuration provides THREE levels of parallelism:
 * 1. Job-level: Multiple jobs can run concurrently (asyncJobLauncher)
 * 2. Step-level: Multiple steps within a job can run in parallel (if configured)
 * 3. Chunk-level: Items within a chunk can be processed in parallel (stepTaskExecutor)
 */
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class SpringBatchConfig {

    private final DataSource dataSource;

    @Value("${spring.batch.job.concurrency-limit:20}")
    private int jobConcurrencyLimit;

    @Value("${spring.batch.step.concurrency-limit:50}")
    private int stepConcurrencyLimit;

    @Value("${spring.batch.chunk.concurrency-limit:100}")
    private int chunkConcurrencyLimit;

    // =========================================================================
    // LEVEL 1: JOB-LEVEL EXECUTOR (Multiple jobs running concurrently)
    // =========================================================================

    /**
     * Task Executor for launching multiple jobs concurrently.
     *
     * Use Case: Scheduler triggers 10 different report jobs at once.
     * Each job runs on its own virtual thread.
     *
     * Virtual threads are ideal here because:
     * - Jobs often wait on I/O (database queries, file writes)
     * - During I/O wait, virtual thread yields, allowing other jobs to run
     * - Can handle many more concurrent jobs than platform threads
     */
    @Bean(name = "jobLauncherTaskExecutor")
    public TaskExecutor jobLauncherTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch-job-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(jobConcurrencyLimit);

        log.info("=================================================");
        log.info("  JOB LAUNCHER EXECUTOR (Level 1)");
        log.info("  Type: Virtual Threads");
        log.info("  Concurrency Limit: {}", jobConcurrencyLimit);
        log.info("  Purpose: Run multiple jobs in parallel");
        log.info("=================================================");

        return executor;
    }

    /**
     * Async Job Launcher using virtual threads.
     *
     * When Scheduler calls POST /api/batch/trigger/start:
     * 1. Request is received on Tomcat thread
     * 2. Job is submitted to jobLauncherTaskExecutor
     * 3. Response returns immediately with executionId
     * 4. Job runs asynchronously on virtual thread
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(jobLauncherTaskExecutor());
        jobLauncher.afterPropertiesSet();

        log.info("=================================================");
        log.info("  ASYNC JOB LAUNCHER CONFIGURED");
        log.info("  Mode: Asynchronous (non-blocking)");
        log.info("  Returns immediately after job submission");
        log.info("=================================================");

        return jobLauncher;
    }

    // =========================================================================
    // LEVEL 2: STEP-LEVEL EXECUTOR (Parallel steps within a job)
    // =========================================================================

    /**
     * Task Executor for running steps in parallel within a job.
     *
     * Use Case: A job has independent steps that can run concurrently:
     * - Step A: Fetch data from Database 1
     * - Step B: Fetch data from Database 2
     * - Step C: Wait for A and B, then merge results
     *
     * Configure in job with: Flow.split(stepTaskExecutor())
     */
    @Bean(name = "stepTaskExecutor")
    public TaskExecutor stepTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch-step-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(stepConcurrencyLimit);

        log.info("=================================================");
        log.info("  STEP TASK EXECUTOR (Level 2)");
        log.info("  Type: Virtual Threads");
        log.info("  Concurrency Limit: {}", stepConcurrencyLimit);
        log.info("  Purpose: Run parallel steps within a job");
        log.info("=================================================");

        return executor;
    }

    // =========================================================================
    // LEVEL 3: CHUNK-LEVEL EXECUTOR (Parallel item processing)
    // =========================================================================

    /**
     * Task Executor for parallel chunk processing.
     *
     * Use Case: Processing 10,000 records in a step:
     * - Without parallelism: Process one at a time
     * - With parallelism: Process multiple items concurrently
     *
     * Configure in step with: .taskExecutor(chunkTaskExecutor())
     *
     * WARNING: When using parallel chunk processing:
     * - Ensure ItemReader is thread-safe (use synchronized or JpaPagingItemReader)
     * - ItemWriter must handle concurrent writes
     * - Set throttle limit to control concurrency
     */
    @Bean(name = "chunkTaskExecutor")
    public TaskExecutor chunkTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch-chunk-");
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(chunkConcurrencyLimit);

        log.info("=================================================");
        log.info("  CHUNK TASK EXECUTOR (Level 3)");
        log.info("  Type: Virtual Threads");
        log.info("  Concurrency Limit: {}", chunkConcurrencyLimit);
        log.info("  Purpose: Parallel item processing within chunks");
        log.info("=================================================");

        return executor;
    }

    // =========================================================================
    // VIRTUAL THREAD EXECUTOR FOR GENERAL ASYNC OPERATIONS
    // =========================================================================

    /**
     * General-purpose virtual thread executor for async operations.
     *
     * Use Case: Fire-and-forget tasks like:
     * - Sending notifications
     * - Publishing Redis events
     * - Async logging
     * - External API calls
     *
     * Uses unbounded virtual thread executor (no concurrency limit)
     * because virtual threads are cheap and the JVM handles scheduling.
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
            .name("async-vt-", 0)
            .factory();

        log.info("=================================================");
        log.info("  VIRTUAL THREAD EXECUTOR (General Purpose)");
        log.info("  Type: Unbounded Virtual Threads");
        log.info("  Purpose: Async fire-and-forget operations");
        log.info("=================================================");

        return Executors.newThreadPerTaskExecutor(factory);
    }

    // =========================================================================
    // FALLBACK: PLATFORM THREAD POOL (For CPU-bound operations)
    // =========================================================================

    /**
     * Platform thread pool for CPU-intensive operations.
     *
     * Virtual threads are NOT ideal for:
     * - Heavy computation (encryption, compression)
     * - CPU-bound algorithms
     * - Operations that don't block on I/O
     *
     * For these cases, use platform threads with a sized pool
     * to avoid overwhelming the CPU.
     */
    @Bean(name = "cpuBoundTaskExecutor")
    public TaskExecutor cpuBoundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("cpu-bound-");

        // Size based on available processors
        int processors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors * 2);
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("=================================================");
        log.info("  CPU-BOUND TASK EXECUTOR (Platform Threads)");
        log.info("  Core Pool Size: {}", processors);
        log.info("  Max Pool Size: {}", processors * 2);
        log.info("  Purpose: CPU-intensive operations");
        log.info("=================================================");

        return executor;
    }

    // =========================================================================
    // SPRING BATCH SCHEMA INITIALIZATION
    // =========================================================================

    /**
     * Initializes Spring Batch metadata tables in the database.
     *
     * Spring Batch 5.x with @EnableBatchProcessing no longer auto-creates
     * the schema, so we need to explicitly initialize it.
     */
    @Bean
    public DataSourceInitializer batchDataSourceInitializer() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-postgresql.sql"));
        populator.setContinueOnError(true); // Continue if tables already exist

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);

        log.info("=================================================");
        log.info("  SPRING BATCH SCHEMA INITIALIZATION");
        log.info("  Script: schema-postgresql.sql");
        log.info("  Continue on error: true");
        log.info("=================================================");

        return initializer;
    }
}
