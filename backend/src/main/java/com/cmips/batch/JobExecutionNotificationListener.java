package com.cmips.batch;

import com.cmips.service.JobEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Spring Batch Job Execution Listener that publishes events to Redis.
 *
 * This listener:
 * 1. Publishes JOB_STARTED when a job begins execution
 * 2. Publishes JOB_COMPLETED or JOB_FAILED when a job finishes
 *
 * The Scheduler application subscribes to these events to:
 * - Update execution status in real-time
 * - Trigger dependent jobs when parent completes successfully
 * - Send notifications on job completion/failure
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobExecutionNotificationListener implements JobExecutionListener {

    private final JobEventPublisher jobEventPublisher;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("=================================================");
        log.info("  JOB STARTING");
        log.info("  Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("  Execution ID: {}", jobExecution.getId());
        log.info("  Scheduler Execution ID: {}",
            jobExecution.getJobParameters().getLong("schedulerExecutionId"));
        log.info("=================================================");

        // Publish job started event to Redis
        jobEventPublisher.publishJobStarted(jobExecution);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("=================================================");
        log.info("  JOB FINISHED");
        log.info("  Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("  Execution ID: {}", jobExecution.getId());
        log.info("  Status: {}", jobExecution.getStatus());
        log.info("  Exit Code: {}", jobExecution.getExitStatus().getExitCode());

        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            long durationMs = java.time.Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime()
            ).toMillis();
            log.info("  Duration: {}ms", durationMs);
        }

        // Log step summaries
        jobExecution.getStepExecutions().forEach(step -> {
            log.info("  Step '{}': read={}, write={}, skip={}",
                step.getStepName(),
                step.getReadCount(),
                step.getWriteCount(),
                step.getSkipCount());
        });

        log.info("=================================================");

        // Publish job completed event to Redis
        jobEventPublisher.publishJobCompleted(jobExecution);
    }
}
