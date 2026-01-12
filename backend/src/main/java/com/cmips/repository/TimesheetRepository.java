package com.cmips.repository;

import com.cmips.entity.Timesheet;
import com.cmips.entity.TimesheetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet, Long> {
    
    // Find timesheets by user ID
    List<Timesheet> findByUserIdOrderByPayPeriodStartDesc(String userId);
    
    // Find timesheets by employee ID
    List<Timesheet> findByEmployeeIdOrderByPayPeriodStartDesc(String employeeId);
    
    // Find timesheets by status
    List<Timesheet> findByStatusOrderByCreatedAtDesc(TimesheetStatus status);
    
    // Find timesheets by department
    List<Timesheet> findByDepartmentOrderByPayPeriodStartDesc(String department);
    
    // Find timesheets by location
    List<Timesheet> findByLocationOrderByPayPeriodStartDesc(String location);
    
    // Find timesheets by pay period
    List<Timesheet> findByPayPeriodStartAndPayPeriodEnd(LocalDate startDate, LocalDate endDate);
    
    // Find timesheets by pay period range
    List<Timesheet> findByPayPeriodStartBetweenOrderByPayPeriodStartDesc(LocalDate startDate, LocalDate endDate);
    
    // Find timesheets by user and status
    List<Timesheet> findByUserIdAndStatusOrderByPayPeriodStartDesc(String userId, TimesheetStatus status);
    
    // Find timesheets by department and status
    List<Timesheet> findByDepartmentAndStatusOrderByCreatedAtDesc(String department, TimesheetStatus status);
    
    // Find timesheets by location and status
    List<Timesheet> findByLocationAndStatusOrderByCreatedAtDesc(String location, TimesheetStatus status);
    
    // Find timesheets by user and pay period
    Optional<Timesheet> findByUserIdAndPayPeriodStartAndPayPeriodEnd(String userId, LocalDate startDate, LocalDate endDate);
    
    // Find timesheets by employee and pay period
    Optional<Timesheet> findByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(String employeeId, LocalDate startDate, LocalDate endDate);
    
    // Count timesheets by status
    long countByStatus(TimesheetStatus status);
    
    // Count timesheets by user
    long countByUserId(String userId);
    
    // Count timesheets by department
    long countByDepartment(String department);
    
    // Count timesheets by location
    long countByLocation(String location);
    
    // Find pending timesheets (submitted but not approved/rejected)
    @Query("SELECT t FROM Timesheet t WHERE t.status IN ('SUBMITTED', 'REVISION_REQUESTED') ORDER BY t.submittedAt ASC")
    List<Timesheet> findPendingTimesheets();
    
    // Find timesheets requiring approval
    @Query("SELECT t FROM Timesheet t WHERE t.status = 'SUBMITTED' ORDER BY t.submittedAt ASC")
    List<Timesheet> findTimesheetsRequiringApproval();
    
    // Find timesheets by supervisor
    @Query("SELECT t FROM Timesheet t WHERE t.approvedBy = :supervisorId ORDER BY t.approvedAt DESC")
    List<Timesheet> findByApprovedBy(@Param("supervisorId") String supervisorId);
    
    // Find timesheets submitted by user
    @Query("SELECT t FROM Timesheet t WHERE t.submittedBy = :userId ORDER BY t.submittedAt DESC")
    List<Timesheet> findBySubmittedBy(@Param("userId") String userId);
    
    // Find timesheets with total hours greater than threshold
    @Query("SELECT t FROM Timesheet t WHERE t.totalHours > :threshold ORDER BY t.totalHours DESC")
    List<Timesheet> findByTotalHoursGreaterThan(@Param("threshold") Double threshold);
    
    // Find timesheets by date range and status
    @Query("SELECT t FROM Timesheet t WHERE t.payPeriodStart >= :startDate AND t.payPeriodEnd <= :endDate AND t.status = :status ORDER BY t.payPeriodStart DESC")
    List<Timesheet> findByDateRangeAndStatus(@Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate, 
                                           @Param("status") TimesheetStatus status);
    // New method to find timesheets by user ID
    Page<Timesheet> findByUserId(String userId, Pageable pageable);
    
    // New method to find timesheets by status
    Page<Timesheet> findByStatus(TimesheetStatus status, Pageable pageable);
    
    // IHSS Pipeline methods - Pagination support
    @Query(value = "SELECT * FROM timesheets WHERE user_id = :userId ORDER BY created_at DESC LIMIT :pageSize OFFSET :offset", nativeQuery = true)
    List<Timesheet> findByUserWithPagination(@Param("userId") String userId, @Param("offset") int offset, @Param("pageSize") int pageSize);
    
    @Query(value = "SELECT * FROM timesheets WHERE user_id = :userId AND pay_period_start <= :endDate AND pay_period_end >= :startDate ORDER BY created_at DESC LIMIT :pageSize OFFSET :offset", nativeQuery = true)
    List<Timesheet> findByUserAndDateRangeWithPagination(@Param("userId") String userId,
                                                         @Param("startDate") LocalDate startDate, 
                                                         @Param("endDate") LocalDate endDate,
                                                         @Param("offset") int offset,
                                                         @Param("pageSize") int pageSize);
    
    @Query(value = "SELECT * FROM timesheets ORDER BY created_at DESC LIMIT :pageSize OFFSET :offset", nativeQuery = true)
    List<Timesheet> findMostRecentWithPagination(@Param("offset") int offset, @Param("pageSize") int pageSize);
    
    @Query(value = "SELECT * FROM timesheets WHERE pay_period_start <= :endDate AND pay_period_end >= :startDate ORDER BY created_at DESC LIMIT :pageSize OFFSET :offset", nativeQuery = true)
    List<Timesheet> findByDateRangeWithPagination(@Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate,
                                                   @Param("offset") int offset,
                                                   @Param("pageSize") int pageSize);
    
    @Query(value = "SELECT * FROM timesheets WHERE location = :location ORDER BY created_at DESC LIMIT :pageSize OFFSET :offset", nativeQuery = true)
    List<Timesheet> findByLocationWithPagination(@Param("location") String location, @Param("offset") int offset, @Param("pageSize") int pageSize);
    
    @Query(value = "SELECT * FROM timesheets WHERE location = :location AND pay_period_start <= :endDate AND pay_period_end >= :startDate ORDER BY created_at DESC LIMIT :pageSize OFFSET :offset", nativeQuery = true)
    List<Timesheet> findByLocationAndDateRangeWithPagination(@Param("location") String location,
                                                             @Param("startDate") LocalDate startDate, 
                                                             @Param("endDate") LocalDate endDate,
                                                             @Param("offset") int offset,
                                                             @Param("pageSize") int pageSize);
    
    @Query(value = "SELECT * FROM timesheets WHERE pay_period_start <= :endDate AND pay_period_end >= :startDate ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Timesheet> findByDateRangeWithLimit(@Param("startDate") LocalDate startDate, 
                                             @Param("endDate") LocalDate endDate,
                                             @Param("limit") int limit);
    
    @Query(value = "SELECT * FROM timesheets ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Timesheet> findMostRecentWithLimit(@Param("limit") int limit);
    
    // Count methods for pagination
    @Query(value = "SELECT COUNT(*) FROM timesheets WHERE pay_period_start <= :endDate AND pay_period_end >= :startDate", nativeQuery = true)
    long countByDateRange(@Param("startDate") LocalDate startDate, 
                         @Param("endDate") LocalDate endDate);
    
    @Query(value = "SELECT COUNT(*) FROM timesheets", nativeQuery = true)
    long countMostRecent();
    
    @Query(value = "SELECT COUNT(*) FROM timesheets WHERE user_id = :userId", nativeQuery = true)
    long countByUser(@Param("userId") String userId);
    
    @Query(value = "SELECT COUNT(*) FROM timesheets WHERE user_id = :userId AND pay_period_start <= :endDate AND pay_period_end >= :startDate", nativeQuery = true)
    long countByUserAndDateRange(@Param("userId") String userId,
                                 @Param("startDate") LocalDate startDate, 
                                 @Param("endDate") LocalDate endDate);
    
    @Query(value = "SELECT COUNT(*) FROM timesheets WHERE location = :location", nativeQuery = true)
    long countByLocationNative(@Param("location") String location);
    
    @Query(value = "SELECT COUNT(*) FROM timesheets WHERE location = :location AND pay_period_start <= :endDate AND pay_period_end >= :startDate", nativeQuery = true)
    long countByLocationAndDateRange(@Param("location") String location,
                                     @Param("startDate") LocalDate startDate, 
                                     @Param("endDate") LocalDate endDate);
    
    // Additional methods for analytics
    @Query(value = "SELECT COUNT(*) FROM timesheets " +
            "WHERE (:createdAfter IS NULL OR created_at >= :createdAfter) " +
            "AND (:createdBefore IS NULL OR created_at <= :createdBefore) " +
            "AND (:location IS NULL OR location = :location) " +
            "AND (:department IS NULL OR department = :department) " +
            "AND (:statusFilter IS NULL OR status = :statusFilter)", nativeQuery = true)
    long countCreatedAfterWithFilters(@Param("createdAfter") LocalDateTime createdAfter,
                                      @Param("createdBefore") LocalDateTime createdBefore,
                                      @Param("location") String location,
                                      @Param("department") String department,
                                      @Param("statusFilter") String statusFilter);
    
    @Query(value = "SELECT COUNT(*) FROM timesheets " +
            "WHERE status IN ('SUBMITTED', 'REVISION_REQUESTED') " +
            "AND (:location IS NULL OR location = :location) " +
            "AND (:department IS NULL OR department = :department) " +
            "AND (:statusFilter IS NULL OR status = :statusFilter) " +
            "AND (:createdAfter IS NULL OR created_at >= :createdAfter) " +
            "AND (:createdBefore IS NULL OR created_at <= :createdBefore)", nativeQuery = true)
    long countPendingApprovalsWithFilters(@Param("location") String location,
                                          @Param("department") String department,
                                          @Param("statusFilter") String statusFilter,
                                          @Param("createdAfter") LocalDateTime createdAfter,
                                          @Param("createdBefore") LocalDateTime createdBefore);
    
    // Status as String for compatibility with IHSS pipeline
    @Query(value = "SELECT * FROM timesheets WHERE status = :status ORDER BY created_at DESC", nativeQuery = true)
    List<Timesheet> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    // Batch job query methods
    List<Timesheet> findByPayPeriodStartBetween(LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByStatusAndPayPeriodStartBetween(TimesheetStatus status, LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByDepartmentAndPayPeriodStartBetween(String department, LocalDate startDate, LocalDate endDate);

    List<Timesheet> findByDepartmentAndStatusAndPayPeriodStartBetween(String department, TimesheetStatus status, LocalDate startDate, LocalDate endDate);

    // Batch job query methods with String status (for compatibility)
    @Query("SELECT t FROM Timesheet t WHERE t.status = :status AND t.payPeriodStart BETWEEN :startDate AND :endDate")
    List<Timesheet> findByStatusStringAndPayPeriodStartBetween(@Param("status") String status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Timesheet t WHERE t.department = :department AND t.status = :status AND t.payPeriodStart BETWEEN :startDate AND :endDate")
    List<Timesheet> findByDepartmentAndStatusStringAndPayPeriodStartBetween(@Param("department") String department, @Param("status") String status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
