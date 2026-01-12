package com.cmips.repository;

import com.cmips.entity.WarrantEntity;
import com.cmips.entity.WarrantEntity.WarrantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Warrant entity operations.
 *
 * Supports the Warrant Status Update job (PRDR110A) and related queries
 * for tracking IHSS provider payments.
 */
@Repository
public interface WarrantRepository extends JpaRepository<WarrantEntity, Long> {

    // ========================================
    // Basic Lookups
    // ========================================

    /**
     * Find warrant by unique warrant number.
     */
    Optional<WarrantEntity> findByWarrantNumber(String warrantNumber);

    /**
     * Check if warrant exists by number.
     */
    boolean existsByWarrantNumber(String warrantNumber);

    /**
     * Find all warrants for a provider.
     */
    List<WarrantEntity> findByProviderIdOrderByIssueDateDesc(String providerId);

    /**
     * Find all warrants for a case.
     */
    List<WarrantEntity> findByCaseNumberOrderByIssueDateDesc(String caseNumber);

    /**
     * Find all warrants by status.
     */
    List<WarrantEntity> findByStatusOrderByIssueDateDesc(WarrantStatus status);

    /**
     * Find all warrants for a county.
     */
    List<WarrantEntity> findByCountyCodeOrderByIssueDateDesc(String countyCode);

    // ========================================
    // Status Update Operations (for batch job)
    // ========================================

    /**
     * Update warrant status and paid date (for batch processing).
     */
    @Modifying
    @Query("UPDATE WarrantEntity w SET w.status = :status, w.paidDate = :paidDate, " +
           "w.statusUpdatedAt = :statusUpdatedAt, w.batchJobExecutionId = :jobExecutionId " +
           "WHERE w.warrantNumber = :warrantNumber")
    int updateWarrantStatus(@Param("warrantNumber") String warrantNumber,
                            @Param("status") WarrantStatus status,
                            @Param("paidDate") LocalDate paidDate,
                            @Param("statusUpdatedAt") LocalDateTime statusUpdatedAt,
                            @Param("jobExecutionId") Long jobExecutionId);

    /**
     * Find warrants that need status updates (ISSUED warrants older than cutoff).
     * Used for detecting potentially stale warrants.
     */
    @Query("SELECT w FROM WarrantEntity w WHERE w.status = 'ISSUED' AND w.issueDate < :cutoffDate")
    List<WarrantEntity> findPotentiallyStaleWarrants(@Param("cutoffDate") LocalDate cutoffDate);

    // ========================================
    // Date Range Queries
    // ========================================

    /**
     * Find warrants issued in date range.
     */
    List<WarrantEntity> findByIssueDateBetweenOrderByIssueDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * Find warrants paid in date range.
     */
    List<WarrantEntity> findByPaidDateBetweenOrderByPaidDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * Find warrants by county and issue date range.
     */
    List<WarrantEntity> findByCountyCodeAndIssueDateBetweenOrderByIssueDateDesc(
            String countyCode, LocalDate startDate, LocalDate endDate);

    /**
     * Find warrants by provider and date range.
     */
    @Query("SELECT w FROM WarrantEntity w WHERE w.providerId = :providerId " +
           "AND w.issueDate BETWEEN :startDate AND :endDate ORDER BY w.issueDate DESC")
    List<WarrantEntity> findByProviderAndDateRange(@Param("providerId") String providerId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    // ========================================
    // Analytics & Reporting
    // ========================================

    /**
     * Count warrants by status.
     */
    long countByStatus(WarrantStatus status);

    /**
     * Count warrants by county and status.
     */
    long countByCountyCodeAndStatus(String countyCode, WarrantStatus status);

    /**
     * Sum total amount by status.
     */
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WarrantEntity w WHERE w.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") WarrantStatus status);

    /**
     * Sum total amount by county and status.
     */
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WarrantEntity w " +
           "WHERE w.countyCode = :countyCode AND w.status = :status")
    BigDecimal sumAmountByCountyAndStatus(@Param("countyCode") String countyCode,
                                           @Param("status") WarrantStatus status);

    /**
     * Get warrant statistics grouped by county.
     * Returns: county_code, status, count, total_amount
     */
    @Query(value = "SELECT county_code, status, COUNT(*), COALESCE(SUM(amount), 0) " +
                   "FROM warrants GROUP BY county_code, status ORDER BY county_code, status",
           nativeQuery = true)
    List<Object[]> getWarrantStatsByCounty();

    /**
     * Get warrant statistics grouped by status for date range.
     */
    @Query(value = "SELECT status, COUNT(*), COALESCE(SUM(amount), 0) " +
                   "FROM warrants WHERE issue_date BETWEEN :startDate AND :endDate " +
                   "GROUP BY status ORDER BY status",
           nativeQuery = true)
    List<Object[]> getWarrantStatsByStatusForDateRange(@Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    // ========================================
    // Batch Processing Support
    // ========================================

    /**
     * Find warrants processed by a specific batch job execution.
     */
    List<WarrantEntity> findByBatchJobExecutionIdOrderByWarrantNumber(Long batchJobExecutionId);

    /**
     * Find warrants from a specific source file.
     */
    List<WarrantEntity> findBySourceFileReferenceOrderByWarrantNumber(String sourceFileReference);

    /**
     * Count warrants processed in a batch job.
     */
    long countByBatchJobExecutionId(Long batchJobExecutionId);

    // ========================================
    // Distinct Values for Filters
    // ========================================

    @Query(value = "SELECT DISTINCT county_code FROM warrants WHERE county_code IS NOT NULL ORDER BY county_code",
           nativeQuery = true)
    List<String> findDistinctCountyCodes();

    @Query(value = "SELECT DISTINCT status FROM warrants WHERE status IS NOT NULL ORDER BY status",
           nativeQuery = true)
    List<String> findDistinctStatuses();
}
