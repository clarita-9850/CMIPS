package com.cmips.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a Warrant in the IHSS payment system.
 *
 * Warrants are payment vouchers issued by the State Controller's Office (SCO)
 * and paid through the State Treasurer's Office (STO).
 *
 * Lifecycle:
 * 1. ISSUED - Warrant created and sent to provider
 * 2. PAID - STO confirms warrant has been cashed/deposited
 * 3. VOIDED - Warrant canceled (e.g., duplicate payment, error)
 * 4. STALE - Warrant expired (not cashed within time limit, typically 1 year)
 *
 * Data Source: Daily file from STO (PRDR110A) via BAW middleware
 */
@Entity
@Table(name = "warrants", indexes = {
    @Index(name = "idx_warrant_number", columnList = "warrant_number", unique = true),
    @Index(name = "idx_warrant_provider", columnList = "provider_id"),
    @Index(name = "idx_warrant_case", columnList = "case_number"),
    @Index(name = "idx_warrant_status", columnList = "status"),
    @Index(name = "idx_warrant_county", columnList = "county_code"),
    @Index(name = "idx_warrant_paid_date", columnList = "paid_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarrantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Unique warrant number from SCO (10 digits).
     */
    @Column(name = "warrant_number", nullable = false, unique = true, length = 20)
    private String warrantNumber;

    /**
     * Date the warrant was issued.
     */
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    /**
     * Date the warrant was paid/cashed (null if not yet paid).
     */
    @Column(name = "paid_date")
    private LocalDate paidDate;

    /**
     * Warrant amount in dollars.
     */
    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * California county code (2 digits).
     */
    @Column(name = "county_code", nullable = false, length = 2)
    private String countyCode;

    /**
     * IHSS Provider ID.
     */
    @Column(name = "provider_id", nullable = false, length = 20)
    private String providerId;

    /**
     * IHSS Case Number.
     */
    @Column(name = "case_number", nullable = false, length = 20)
    private String caseNumber;

    /**
     * Current warrant status.
     */
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WarrantStatus status;

    /**
     * Pay period start date this warrant covers.
     */
    @Column(name = "pay_period_start")
    private LocalDate payPeriodStart;

    /**
     * Pay period end date this warrant covers.
     */
    @Column(name = "pay_period_end")
    private LocalDate payPeriodEnd;

    /**
     * File reference from BAW when this record was imported.
     */
    @Column(name = "source_file_reference", length = 100)
    private String sourceFileReference;

    /**
     * Batch job execution ID that processed this warrant.
     */
    @Column(name = "batch_job_execution_id")
    private Long batchJobExecutionId;

    /**
     * Date/time the status was last updated from STO file.
     */
    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Warrant status enum matching STO file codes.
     */
    public enum WarrantStatus {
        ISSUED,    // Initial state - warrant created
        PAID,      // P - Warrant cashed/deposited
        VOIDED,    // V - Warrant canceled
        STALE      // S - Warrant expired (not cashed in time)
    }
}
