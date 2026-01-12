package com.cmips.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for batch job trigger requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTriggerResponse {

    /**
     * Whether the job was triggered successfully.
     */
    private boolean success;

    /**
     * Spring Batch execution ID for tracking.
     */
    private Long executionId;

    /**
     * Name of the triggered job.
     */
    private String jobName;

    /**
     * Trigger ID from the Scheduler app (UUID for correlation).
     */
    private String triggerId;

    /**
     * Scheduler's execution ID (for backward compatibility).
     * @deprecated Use triggerId instead
     */
    @Deprecated
    private Long schedulerExecutionId;

    /**
     * Current status of the job (STARTING, STARTED, FAILED, etc.).
     */
    private String status;

    /**
     * Human-readable message describing the result.
     */
    private String message;
}
