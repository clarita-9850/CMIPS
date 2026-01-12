package com.cmips.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for triggering a batch job from the Scheduler application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTriggerRequest {

    /**
     * Name of the Spring Batch job to execute.
     * Must match a registered job bean name (e.g., "countyDailyReportJob").
     */
    private String jobName;

    /**
     * Unique trigger ID from the Scheduler application (UUID).
     * Used to correlate Trial's Spring Batch execution with Scheduler's execution record.
     * This is published back via Redis events for status tracking.
     */
    private String triggerId;

    /**
     * Execution ID from the Scheduler application (legacy field for backward compatibility).
     * @deprecated Use triggerId instead
     */
    @Deprecated
    private Long schedulerExecutionId;

    /**
     * Job parameters passed from the Scheduler.
     * These become Spring Batch JobParameters and are used to make each run unique.
     */
    private Map<String, String> parameters;

    /**
     * Get the correlation ID - prefers triggerId, falls back to schedulerExecutionId.
     */
    public String getCorrelationId() {
        if (triggerId != null && !triggerId.isEmpty()) {
            return triggerId;
        }
        return schedulerExecutionId != null ? schedulerExecutionId.toString() : null;
    }
}
