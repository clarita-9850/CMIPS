package com.cmips.baw.filetype;

import com.cmips.integration.framework.baw.annotation.FileColumn;
import com.cmips.integration.framework.baw.annotation.FileId;
import com.cmips.integration.framework.baw.annotation.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * File type definition for SCO (State Controller Office) Payment Request file.
 *
 * This maps to the PRDR120A outbound file format sent to SCO for payment processing.
 * The file is generated from approved timesheets and contains payment requests
 * for IHSS providers.
 *
 * Format: Fixed-width or CSV (configurable)
 * - Payment Request ID (20 chars)
 * - Provider ID (9 chars)
 * - Provider Name (50 chars)
 * - Case Number (10 chars)
 * - County Code (2 chars)
 * - Pay Period Start (8 chars, YYYYMMDD)
 * - Pay Period End (8 chars, YYYYMMDD)
 * - Regular Hours (8 chars, 2 decimal)
 * - Overtime Hours (8 chars, 2 decimal)
 * - Total Hours (8 chars, 2 decimal)
 * - Payment Amount (12 chars, 2 decimal)
 * - Timesheet ID (10 chars)
 * - Payment Type (1 char: R=Regular, A=Adjustment, T=Retroactive)
 */
@FileType(
    name = "payment-request-sco",
    description = "SCO Payment Request File (PRDR120A)",
    version = "1.0"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFileRecord {

    @FileId
    @FileColumn(
        order = 1,
        name = "PAYMENT_REQUEST_ID",
        length = 20,
        nullable = false
    )
    private String paymentRequestId;

    @FileColumn(
        order = 2,
        name = "PROVIDER_ID",
        length = 9,
        nullable = false
    )
    private String providerId;

    @FileColumn(
        order = 3,
        name = "PROVIDER_NAME",
        length = 50,
        nullable = false
    )
    private String providerName;

    @FileColumn(
        order = 4,
        name = "CASE_NUMBER",
        length = 10,
        nullable = false
    )
    private String caseNumber;

    @FileColumn(
        order = 5,
        name = "COUNTY_CODE",
        length = 2,
        nullable = false
    )
    private String countyCode;

    @FileColumn(
        order = 6,
        name = "PAY_PERIOD_START",
        length = 8,
        format = "yyyyMMdd",
        nullable = false
    )
    private LocalDate payPeriodStart;

    @FileColumn(
        order = 7,
        name = "PAY_PERIOD_END",
        length = 8,
        format = "yyyyMMdd",
        nullable = false
    )
    private LocalDate payPeriodEnd;

    @FileColumn(
        order = 8,
        name = "REGULAR_HOURS",
        length = 8,
        padChar = '0',
        alignment = FileColumn.Alignment.RIGHT,
        format = "00000.00",
        nullable = false
    )
    private BigDecimal regularHours;

    @FileColumn(
        order = 9,
        name = "OVERTIME_HOURS",
        length = 8,
        padChar = '0',
        alignment = FileColumn.Alignment.RIGHT,
        format = "00000.00",
        nullable = false
    )
    private BigDecimal overtimeHours;

    @FileColumn(
        order = 10,
        name = "TOTAL_HOURS",
        length = 8,
        padChar = '0',
        alignment = FileColumn.Alignment.RIGHT,
        format = "00000.00",
        nullable = false
    )
    private BigDecimal totalHours;

    @FileColumn(
        order = 11,
        name = "PAYMENT_AMOUNT",
        length = 12,
        padChar = '0',
        alignment = FileColumn.Alignment.RIGHT,
        format = "000000000.00",
        nullable = false
    )
    private BigDecimal paymentAmount;

    @FileColumn(
        order = 12,
        name = "TIMESHEET_ID",
        length = 10,
        padChar = '0',
        alignment = FileColumn.Alignment.RIGHT,
        nullable = false
    )
    private Long timesheetId;

    @FileColumn(
        order = 13,
        name = "PAYMENT_TYPE",
        length = 1,
        nullable = false
    )
    private String paymentTypeCode;

    /**
     * Gets the payment type as enum.
     */
    public PaymentType getPaymentType() {
        return PaymentType.fromCode(paymentTypeCode);
    }

    /**
     * Sets payment type from enum.
     */
    public void setPaymentType(PaymentType type) {
        this.paymentTypeCode = type.getCode();
    }

    /**
     * Payment type enumeration matching SCO codes.
     */
    public enum PaymentType {
        REGULAR("R"),
        ADJUSTMENT("A"),
        RETROACTIVE("T");

        private final String code;

        PaymentType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static PaymentType fromCode(String code) {
            if (code == null) {
                return null;
            }
            for (PaymentType type : values()) {
                if (type.code.equals(code.trim())) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown payment type code: " + code);
        }
    }
}
