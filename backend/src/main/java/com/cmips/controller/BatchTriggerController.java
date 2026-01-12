package com.cmips.controller;

import com.cmips.dto.BatchTriggerRequest;
import com.cmips.dto.BatchTriggerResponse;
import com.cmips.service.BatchJobTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for triggering Spring Batch jobs from the Scheduler Application.
 *
 * This controller exposes endpoints that the Scheduler app calls to:
 * 1. Trigger a batch job with specific parameters
 * 2. Query job execution status
 * 3. Stop a running job
 *
 * All endpoints are secured and require valid authentication.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Batch Trigger", description = "Spring Batch job trigger endpoints (Service-to-Service)")
public class BatchTriggerController {

    private final BatchJobTriggerService batchJobTriggerService;
    private final JobExplorer jobExplorer;

    @Operation(
        summary = "Trigger a batch job",
        description = "Called by the Scheduler application to trigger a Spring Batch job. Requires ADMIN, SCHEDULER, or SYSTEM_SCHEDULER role."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job triggered successfully"),
        @ApiResponse(responseCode = "400", description = "Failed to trigger job"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - valid JWT required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient role")
    })
    @PostMapping("/api/batch/trigger/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'SCHEDULER', 'SYSTEM_SCHEDULER')")
    public ResponseEntity<BatchTriggerResponse> triggerJob(@RequestBody BatchTriggerRequest request) {
        String correlationId = request.getCorrelationId();

        log.info("=================================================");
        log.info("  BATCH JOB TRIGGER REQUEST RECEIVED");
        log.info("  Job Name: {}", request.getJobName());
        log.info("  Trigger ID: {}", request.getTriggerId());
        log.info("  Correlation ID: {}", correlationId);
        log.info("  Parameters: {}", request.getParameters());
        log.info("=================================================");

        try {
            JobExecution jobExecution = batchJobTriggerService.triggerJob(
                request.getJobName(),
                correlationId,
                request.getParameters()
            );

            BatchTriggerResponse response = BatchTriggerResponse.builder()
                .success(true)
                .executionId(jobExecution.getId())
                .jobName(request.getJobName())
                .triggerId(correlationId)
                .status(jobExecution.getStatus().name())
                .message("Job triggered successfully")
                .build();

            log.info("Job triggered successfully. Spring Batch Execution ID: {}, Trigger ID: {}",
                jobExecution.getId(), correlationId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to trigger job: {} (triggerId: {})", request.getJobName(), correlationId, e);

            BatchTriggerResponse response = BatchTriggerResponse.builder()
                .success(false)
                .jobName(request.getJobName())
                .triggerId(correlationId)
                .status("FAILED")
                .message("Failed to trigger job: " + e.getMessage())
                .build();

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get the status of a job execution.
     *
     * @param executionId Spring Batch execution ID
     * @return Current status and details of the job execution
     */
    @GetMapping("/api/batch/trigger/status/{executionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SCHEDULER', 'SYSTEM_SCHEDULER', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable Long executionId) {
        log.debug("Getting status for execution ID: {}", executionId);

        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        if (jobExecution == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = new HashMap<>();
        status.put("executionId", jobExecution.getId());
        status.put("jobName", jobExecution.getJobInstance().getJobName());
        status.put("status", jobExecution.getStatus().name());
        status.put("exitStatus", jobExecution.getExitStatus().getExitCode());
        status.put("exitDescription", jobExecution.getExitStatus().getExitDescription());
        status.put("startTime", jobExecution.getStartTime());
        status.put("endTime", jobExecution.getEndTime());
        status.put("createTime", jobExecution.getCreateTime());
        status.put("lastUpdated", jobExecution.getLastUpdated());

        // Include step executions summary
        status.put("stepCount", jobExecution.getStepExecutions().size());

        return ResponseEntity.ok(status);
    }

    /**
     * Stop a running job.
     *
     * @param executionId Spring Batch execution ID
     * @return Result of the stop operation
     */
    @PostMapping("/api/batch/trigger/stop/{executionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_SCHEDULER')")
    public ResponseEntity<Map<String, Object>> stopJob(@PathVariable Long executionId) {
        log.info("Stopping job execution: {}", executionId);

        try {
            boolean stopped = batchJobTriggerService.stopJob(executionId);

            Map<String, Object> result = new HashMap<>();
            result.put("executionId", executionId);
            result.put("stopped", stopped);
            result.put("message", stopped ? "Job stop signal sent" : "Job not found or already completed");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to stop job: {}", executionId, e);

            Map<String, Object> result = new HashMap<>();
            result.put("executionId", executionId);
            result.put("stopped", false);
            result.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Get the status of a job execution by triggerId.
     * This endpoint is called by the scheduler which uses triggerId (UUID) for correlation.
     *
     * @param triggerId The scheduler's trigger ID (stored in job parameters)
     * @return Current status and details of the job execution
     */
    @GetMapping({"/api/batch/status/{triggerId}", "/api/batch/trigger/status-by-trigger/{triggerId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SCHEDULER', 'SYSTEM_SCHEDULER', 'SUPERVISOR')")
    public ResponseEntity<Map<String, Object>> getJobStatusByTriggerId(@PathVariable String triggerId) {
        log.debug("Getting status for trigger ID: {}", triggerId);

        // Find execution by triggerId parameter
        JobExecution jobExecution = batchJobTriggerService.findExecutionByTriggerId(triggerId);
        if (jobExecution == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> status = new HashMap<>();
        status.put("triggerId", triggerId);
        status.put("executionId", jobExecution.getId());
        status.put("jobName", jobExecution.getJobInstance().getJobName());
        status.put("status", jobExecution.getStatus().name());
        status.put("exitStatus", jobExecution.getExitStatus().getExitCode());
        status.put("exitDescription", jobExecution.getExitStatus().getExitDescription());
        status.put("startTime", jobExecution.getStartTime());
        status.put("endTime", jobExecution.getEndTime());
        status.put("createTime", jobExecution.getCreateTime());
        status.put("lastUpdated", jobExecution.getLastUpdated());
        status.put("stepCount", jobExecution.getStepExecutions().size());

        return ResponseEntity.ok(status);
    }

    /**
     * Stop a running job by triggerId.
     * This endpoint is called by the scheduler which uses triggerId (UUID) for correlation.
     *
     * @param triggerId The scheduler's trigger ID
     * @return Result of the stop operation
     */
    @PostMapping({"/api/batch/stop/{triggerId}", "/api/batch/trigger/stop-by-trigger/{triggerId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_SCHEDULER')")
    public ResponseEntity<Map<String, Object>> stopJobByTriggerId(@PathVariable String triggerId) {
        log.info("Stopping job by trigger ID: {}", triggerId);

        try {
            JobExecution jobExecution = batchJobTriggerService.findExecutionByTriggerId(triggerId);
            if (jobExecution == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("triggerId", triggerId);
                result.put("stopped", false);
                result.put("message", "Job not found for trigger ID");
                return ResponseEntity.notFound().build();
            }

            boolean stopped = batchJobTriggerService.stopJob(jobExecution.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("triggerId", triggerId);
            result.put("executionId", jobExecution.getId());
            result.put("stopped", stopped);
            result.put("message", stopped ? "Job stop signal sent" : "Job already completed");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to stop job by trigger ID: {}", triggerId, e);

            Map<String, Object> result = new HashMap<>();
            result.put("triggerId", triggerId);
            result.put("stopped", false);
            result.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Health check endpoint for the batch trigger service.
     */
    @GetMapping("/api/batch/trigger/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "BatchTriggerController");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
}
