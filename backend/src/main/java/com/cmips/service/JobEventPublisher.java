package com.cmips.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing job execution events to Redis Pub/Sub.
 *
 * The Scheduler application subscribes to these events to:
 * 1. Update execution status in real-time
 * 2. Trigger dependent jobs when a job completes successfully
 * 3. Send notifications on job completion/failure
 *
 * Channel Configuration (must match Scheduler's subscription channels):
 * - job-started:   batch:events:job-started
 * - job-progress:  batch:events:job-progress
 * - job-completed: batch:events:job-completed
 * - job-failed:    batch:events:job-failed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${scheduler-app.events.job-started:batch:events:job-started}")
    private String jobStartedChannel;

    @Value("${scheduler-app.events.job-progress:batch:events:job-progress}")
    private String jobProgressChannel;

    @Value("${scheduler-app.events.job-completed:batch:events:job-completed}")
    private String jobCompletedChannel;

    @Value("${scheduler-app.events.job-failed:batch:events:job-failed}")
    private String jobFailedChannel;

    /**
     * Publish a job started event.
     */
    public void publishJobStarted(JobExecution jobExecution) {
        Map<String, Object> event = buildBaseEvent(jobExecution, "JOB_STARTED");
        publish(jobStartedChannel, event);
    }

    /**
     * Publish a job completed event.
     */
    public void publishJobCompleted(JobExecution jobExecution) {
        Map<String, Object> event = buildBaseEvent(jobExecution,
            jobExecution.getStatus() == BatchStatus.COMPLETED ? "JOB_COMPLETED" : "JOB_FAILED");

        String channel = jobExecution.getStatus() == BatchStatus.COMPLETED
            ? jobCompletedChannel
            : jobFailedChannel;

        publish(channel, event);
    }

    /**
     * Publish a job stopped event.
     */
    public void publishJobStopped(JobExecution jobExecution) {
        Map<String, Object> event = buildBaseEvent(jobExecution, "JOB_STOPPED");
        publish(jobFailedChannel, event);
    }

    /**
     * Publish a step completed event (for progress tracking).
     */
    public void publishStepCompleted(JobExecution jobExecution, String stepName, int progress) {
        Map<String, Object> event = buildBaseEvent(jobExecution, "STEP_COMPLETED");
        event.put("stepName", stepName);
        event.put("progress", progress);
        publish(jobProgressChannel, event);
    }

    /**
     * Build the base event structure.
     */
    private Map<String, Object> buildBaseEvent(JobExecution jobExecution, String eventType) {
        Map<String, Object> event = new HashMap<>();

        // Event metadata
        event.put("eventType", eventType);
        event.put("timestamp", Instant.now().toString());

        // Job execution info
        event.put("executionId", jobExecution.getId());
        event.put("jobName", jobExecution.getJobInstance().getJobName());
        event.put("status", jobExecution.getStatus().name());
        event.put("exitCode", jobExecution.getExitStatus().getExitCode());
        event.put("exitDescription", jobExecution.getExitStatus().getExitDescription());

        // Timing info
        if (jobExecution.getStartTime() != null) {
            event.put("startTime", jobExecution.getStartTime().toString());
        }
        if (jobExecution.getEndTime() != null) {
            event.put("endTime", jobExecution.getEndTime().toString());
        }

        // Scheduler correlation ID from job parameters (triggerId is the primary key)
        String triggerId = jobExecution.getJobParameters().getString("triggerId");
        if (triggerId != null) {
            event.put("triggerId", triggerId);
        }

        // Legacy support for schedulerExecutionId
        Long schedulerExecutionId = jobExecution.getJobParameters().getLong("schedulerExecutionId");
        if (schedulerExecutionId != null) {
            event.put("schedulerExecutionId", schedulerExecutionId);
        }

        // Step execution count
        event.put("stepCount", jobExecution.getStepExecutions().size());

        // Count items processed (aggregate from all steps)
        long readCount = jobExecution.getStepExecutions().stream()
            .mapToLong(step -> step.getReadCount())
            .sum();
        long writeCount = jobExecution.getStepExecutions().stream()
            .mapToLong(step -> step.getWriteCount())
            .sum();
        long skipCount = jobExecution.getStepExecutions().stream()
            .mapToLong(step -> step.getSkipCount())
            .sum();

        event.put("readCount", readCount);
        event.put("writeCount", writeCount);
        event.put("skipCount", skipCount);

        return event;
    }

    /**
     * Publish the event to a specific Redis channel.
     */
    private void publish(String channel, Map<String, Object> event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);

            log.debug("Publishing event to channel {}: {}", channel, eventJson);
            redisTemplate.convertAndSend(channel, eventJson);

            log.info("Published {} event to {} for job {} (executionId: {})",
                event.get("eventType"),
                channel,
                event.get("jobName"),
                event.get("executionId"));

        } catch (Exception e) {
            log.error("Failed to publish job event to {}: {}", channel, event, e);
        }
    }
}
