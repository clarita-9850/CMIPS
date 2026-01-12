package com.cmips.batch;

import com.cmips.service.JobEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Spring Batch Step Execution Listener for progress tracking.
 *
 * Publishes step completion events to Redis so the Scheduler app
 * can show real-time progress for multi-step jobs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StepProgressListener implements StepExecutionListener {

    private final JobEventPublisher jobEventPublisher;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Step '{}' starting for job '{}'",
            stepExecution.getStepName(),
            stepExecution.getJobExecution().getJobInstance().getJobName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Step '{}' completed with status: {} (read: {}, write: {}, skip: {})",
            stepExecution.getStepName(),
            stepExecution.getStatus(),
            stepExecution.getReadCount(),
            stepExecution.getWriteCount(),
            stepExecution.getSkipCount());

        // Calculate progress as percentage of completed steps
        int totalSteps = getTotalStepsInJob(stepExecution);
        int completedSteps = stepExecution.getJobExecution().getStepExecutions().size();
        int progress = totalSteps > 0 ? (completedSteps * 100) / totalSteps : 100;

        // Publish step completed event
        jobEventPublisher.publishStepCompleted(
            stepExecution.getJobExecution(),
            stepExecution.getStepName(),
            progress
        );

        return stepExecution.getExitStatus();
    }

    /**
     * Get total number of steps in the job.
     * This is a simple heuristic - in real scenarios, you might
     * store this in job parameters or job execution context.
     */
    private int getTotalStepsInJob(StepExecution stepExecution) {
        // Default to 1 if not specified
        Object totalSteps = stepExecution.getJobExecution()
            .getExecutionContext().get("totalSteps");
        return totalSteps != null ? (Integer) totalSteps : 1;
    }
}
