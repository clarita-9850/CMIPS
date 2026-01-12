package com.cmips.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing batch job aggregations in database instead of memory.
 * This enables streaming pattern where records are processed and persisted
 * immediately, avoiding heap memory exhaustion.
 */
@Entity
@Table(name = "batch_aggregation",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_batch_agg_job_type_key",
            columnNames = {"job_execution_id", "aggregation_type", "group_key"})
    },
    indexes = {
        @Index(name = "idx_batch_agg_job_execution_id", columnList = "job_execution_id"),
        @Index(name = "idx_batch_agg_group_key", columnList = "group_key"),
        @Index(name = "idx_batch_agg_aggregation_type", columnList = "aggregation_type")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAggregationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Job execution ID to link aggregations to specific job run
     */
    @Column(name = "job_execution_id", nullable = false)
    private Long jobExecutionId;

    /**
     * Type of aggregation (e.g., "BY_DEPARTMENT", "BY_REGION", "BY_DEPARTMENT_REGION")
     */
    @Column(name = "aggregation_type", nullable = false, length = 50)
    private String aggregationType;

    /**
     * Group key (e.g., "DEPT_01", "REGION_05", "DEPT_01_REGION_05")
     */
    @Column(name = "group_key", nullable = false, length = 200)
    private String groupKey;

    /**
     * Count of records in this group
     */
    @Column(name = "record_count", nullable = false)
    @Builder.Default
    private Long recordCount = 0L;

    /**
     * Sum of salary values
     */
    @Column(name = "total_salary")
    @Builder.Default
    private Double totalSalary = 0.0;

    /**
     * Sum of hours worked
     */
    @Column(name = "total_hours")
    @Builder.Default
    private Double totalHours = 0.0;

    /**
     * Sum of bonus values
     */
    @Column(name = "total_bonus")
    @Builder.Default
    private Double totalBonus = 0.0;

    /**
     * Average productivity score
     */
    @Column(name = "avg_productivity")
    private Double avgProductivity;

    /**
     * Average quality score
     */
    @Column(name = "avg_quality_score")
    private Double avgQualityScore;

    /**
     * Minimum salary in group
     */
    @Column(name = "min_salary")
    private Double minSalary;

    /**
     * Maximum salary in group
     */
    @Column(name = "max_salary")
    private Double maxSalary;

    /**
     * Created timestamp
     */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Updated timestamp
     */
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
