package com.cmips.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for tracking batch jobs in the database.
 */
@Entity
@Table(name = "batch_job_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 50)
    private String jobId;

    @Column(name = "correlation_id", length = 50)
    private String correlationId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "role", length = 50)
    private String role;

    @Column(name = "target_system", length = 50)
    private String targetSystem;

    @Column(name = "data_format", length = 20)
    private String dataFormat;

    @Column(name = "input_file_path", length = 500)
    private String inputFilePath;

    @Column(name = "output_file_path", length = 500)
    private String outputFilePath;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 5;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "records_processed")
    @Builder.Default
    private Long recordsProcessed = 0L;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "county_id", length = 50)
    private String countyId;

    @Column(name = "district_id", length = 50)
    private String districtId;

    @Column(name = "worker_id", length = 100)
    private String workerId;

    @Column(name = "aws_batch_job_id", length = 100)
    private String awsBatchJobId;

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
