package com.cmips.repository;

import com.cmips.entity.BatchAggregationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository for batch aggregation operations.
 * Supports upsert operations for streaming aggregation pattern.
 */
@Repository
public interface BatchAggregationRepository extends JpaRepository<BatchAggregationEntity, Long> {

    /**
     * Find aggregation by job execution ID, type, and group key.
     */
    Optional<BatchAggregationEntity> findByJobExecutionIdAndAggregationTypeAndGroupKey(
            Long jobExecutionId, String aggregationType, String groupKey);

    /**
     * Find all aggregations for a job execution.
     */
    List<BatchAggregationEntity> findByJobExecutionId(Long jobExecutionId);

    /**
     * Find all aggregations of a specific type for a job.
     */
    List<BatchAggregationEntity> findByJobExecutionIdAndAggregationType(
            Long jobExecutionId, String aggregationType);

    /**
     * Count distinct groups by aggregation type for a job.
     */
    @Query("SELECT COUNT(DISTINCT a.groupKey) FROM BatchAggregationEntity a " +
           "WHERE a.jobExecutionId = :jobExecutionId AND a.aggregationType = :aggregationType")
    long countDistinctGroupsByType(
            @Param("jobExecutionId") Long jobExecutionId,
            @Param("aggregationType") String aggregationType);

    /**
     * Get total record count for a job.
     */
    @Query("SELECT SUM(a.recordCount) FROM BatchAggregationEntity a " +
           "WHERE a.jobExecutionId = :jobExecutionId AND a.aggregationType = 'BY_DEPARTMENT'")
    Long getTotalRecordCount(@Param("jobExecutionId") Long jobExecutionId);

    /**
     * Get aggregation summary by department for a job.
     */
    @Query("SELECT a FROM BatchAggregationEntity a " +
           "WHERE a.jobExecutionId = :jobExecutionId AND a.aggregationType = 'BY_DEPARTMENT' " +
           "ORDER BY a.recordCount DESC")
    List<BatchAggregationEntity> getDepartmentSummary(@Param("jobExecutionId") Long jobExecutionId);

    /**
     * Upsert aggregation - increment counts and sums atomically.
     * This is the key method for streaming pattern - updates in place without loading to memory.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO batch_aggregation
            (job_execution_id, aggregation_type, group_key, record_count, total_salary, total_hours,
             total_bonus, min_salary, max_salary, created_at, updated_at)
        VALUES
            (:jobExecutionId, :aggregationType, :groupKey, 1, :salary, :hours, :bonus, :salary, :salary, NOW(), NOW())
        ON CONFLICT (job_execution_id, aggregation_type, group_key)
        DO UPDATE SET
            record_count = batch_aggregation.record_count + 1,
            total_salary = batch_aggregation.total_salary + :salary,
            total_hours = batch_aggregation.total_hours + :hours,
            total_bonus = batch_aggregation.total_bonus + :bonus,
            min_salary = LEAST(batch_aggregation.min_salary, :salary),
            max_salary = GREATEST(batch_aggregation.max_salary, :salary),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertAggregation(
            @Param("jobExecutionId") Long jobExecutionId,
            @Param("aggregationType") String aggregationType,
            @Param("groupKey") String groupKey,
            @Param("salary") Double salary,
            @Param("hours") Double hours,
            @Param("bonus") Double bonus);

    /**
     * Simple increment for count-only aggregations.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO batch_aggregation
            (job_execution_id, aggregation_type, group_key, record_count, total_salary, total_hours,
             total_bonus, created_at, updated_at)
        VALUES
            (:jobExecutionId, :aggregationType, :groupKey, 1, 0, 0, 0, NOW(), NOW())
        ON CONFLICT (job_execution_id, aggregation_type, group_key)
        DO UPDATE SET
            record_count = batch_aggregation.record_count + 1,
            updated_at = NOW()
        """, nativeQuery = true)
    void incrementCount(
            @Param("jobExecutionId") Long jobExecutionId,
            @Param("aggregationType") String aggregationType,
            @Param("groupKey") String groupKey);

    /**
     * Delete all aggregations for a job (cleanup).
     */
    @Modifying
    @Transactional
    void deleteByJobExecutionId(Long jobExecutionId);

    /**
     * Get top N departments by record count.
     */
    @Query("SELECT a FROM BatchAggregationEntity a " +
           "WHERE a.jobExecutionId = :jobExecutionId AND a.aggregationType = 'BY_DEPARTMENT' " +
           "ORDER BY a.recordCount DESC")
    List<BatchAggregationEntity> getTopDepartments(
            @Param("jobExecutionId") Long jobExecutionId,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Get average salary by department for a job.
     */
    @Query("SELECT a.groupKey, (a.totalSalary / a.recordCount) as avgSalary " +
           "FROM BatchAggregationEntity a " +
           "WHERE a.jobExecutionId = :jobExecutionId AND a.aggregationType = 'BY_DEPARTMENT' " +
           "ORDER BY avgSalary DESC")
    List<Object[]> getAverageSalaryByDepartment(@Param("jobExecutionId") Long jobExecutionId);

    /**
     * Batch upsert aggregations - much faster than individual upserts.
     * Uses a single query with multiple value sets.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO batch_aggregation
            (job_execution_id, aggregation_type, group_key, record_count, total_salary, total_hours,
             total_bonus, min_salary, max_salary, created_at, updated_at)
        VALUES
            (:jobExecutionId, :aggregationType, :groupKey, :recordCount, :totalSalary, :totalHours,
             :totalBonus, :minSalary, :maxSalary, NOW(), NOW())
        ON CONFLICT (job_execution_id, aggregation_type, group_key)
        DO UPDATE SET
            record_count = batch_aggregation.record_count + :recordCount,
            total_salary = batch_aggregation.total_salary + :totalSalary,
            total_hours = batch_aggregation.total_hours + :totalHours,
            total_bonus = batch_aggregation.total_bonus + :totalBonus,
            min_salary = LEAST(batch_aggregation.min_salary, :minSalary),
            max_salary = GREATEST(batch_aggregation.max_salary, :maxSalary),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertAggregationBatch(
            @Param("jobExecutionId") Long jobExecutionId,
            @Param("aggregationType") String aggregationType,
            @Param("groupKey") String groupKey,
            @Param("recordCount") Integer recordCount,
            @Param("totalSalary") Double totalSalary,
            @Param("totalHours") Double totalHours,
            @Param("totalBonus") Double totalBonus,
            @Param("minSalary") Double minSalary,
            @Param("maxSalary") Double maxSalary);
}
