package com.cmips.repository;

import com.cmips.entity.BatchJobLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for batch job log operations.
 */
@Repository
public interface BatchJobLogRepository extends JpaRepository<BatchJobLogEntity, Long> {

    /**
     * Find by job ID.
     */
    Optional<BatchJobLogEntity> findByJobId(String jobId);

    /**
     * Find all jobs by correlation ID.
     */
    List<BatchJobLogEntity> findByCorrelationId(String correlationId);

    /**
     * Find jobs by status.
     */
    List<BatchJobLogEntity> findByStatus(String status);

    /**
     * Find jobs by role.
     */
    List<BatchJobLogEntity> findByRole(String role);

    /**
     * Find jobs by report type.
     */
    List<BatchJobLogEntity> findByReportType(String reportType);

    /**
     * Find jobs by worker ID.
     */
    List<BatchJobLogEntity> findByWorkerId(String workerId);

    /**
     * Find jobs submitted after a certain time.
     */
    List<BatchJobLogEntity> findBySubmittedAtAfter(LocalDateTime after);

    /**
     * Find jobs by status and correlation ID.
     */
    List<BatchJobLogEntity> findByStatusAndCorrelationId(String status, String correlationId);

    /**
     * Count jobs by status.
     */
    long countByStatus(String status);

    /**
     * Count jobs by correlation ID.
     */
    long countByCorrelationId(String correlationId);

    /**
     * Count completed jobs by correlation ID.
     */
    @Query("SELECT COUNT(b) FROM BatchJobLogEntity b WHERE b.correlationId = :correlationId AND b.status = 'COMPLETED'")
    long countCompletedByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Count failed jobs by correlation ID.
     */
    @Query("SELECT COUNT(b) FROM BatchJobLogEntity b WHERE b.correlationId = :correlationId AND b.status = 'FAILED'")
    long countFailedByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Get average processing time by report type.
     */
    @Query("SELECT AVG(b.processingTimeMs) FROM BatchJobLogEntity b WHERE b.reportType = :reportType AND b.status = 'COMPLETED'")
    Double getAverageProcessingTimeByReportType(@Param("reportType") String reportType);

    /**
     * Get total records processed by correlation ID.
     */
    @Query("SELECT SUM(b.recordsProcessed) FROM BatchJobLogEntity b WHERE b.correlationId = :correlationId")
    Long getTotalRecordsProcessedByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find recent jobs (last N hours).
     */
    @Query("SELECT b FROM BatchJobLogEntity b WHERE b.submittedAt >= :since ORDER BY b.submittedAt DESC")
    List<BatchJobLogEntity> findRecentJobs(@Param("since") LocalDateTime since);

    /**
     * Find dead letter jobs.
     */
    @Query("SELECT b FROM BatchJobLogEntity b WHERE b.status = 'DEAD_LETTER' ORDER BY b.submittedAt DESC")
    List<BatchJobLogEntity> findDeadLetterJobs();

    /**
     * Get worker statistics.
     */
    @Query("SELECT b.workerId, COUNT(b), SUM(CASE WHEN b.status = 'COMPLETED' THEN 1 ELSE 0 END), AVG(b.processingTimeMs) " +
           "FROM BatchJobLogEntity b WHERE b.workerId IS NOT NULL GROUP BY b.workerId")
    List<Object[]> getWorkerStatistics();
}
