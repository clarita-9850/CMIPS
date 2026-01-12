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
 * File type definition for STO (State Treasurer Office) Warrant Paid file.
 *
 * This maps to the PRDR110A fixed-width file format:
 * - Positions 1-10:   Warrant Number (10 chars, right-padded with zeros)
 * - Positions 11-18:  Issue Date (YYYYMMDD)
 * - Positions 19-26:  Paid Date (YYYYMMDD)
 * - Positions 27-38:  Amount (12 digits, 2 decimal implied)
 * - Positions 39-40:  County Code (2 chars)
 * - Positions 41-49:  Provider ID (9 chars)
 * - Positions 50-59:  Case Number (10 chars)
 * - Position 60:      Status (P=Paid, V=Voided, S=Stale)
 *
 * Total record length: 60 characters
 */
@FileType(
    name = "warrant-paid-sto",
    description = "STO Warrant Paid File (PRDR110A)",
    version = "1.0"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantPaidFileRecord {

    @FileId
    @FileColumn(
        order = 1,
        name = "WARRANT_NUMBER",
        length = 10,
        padChar = '0',
        alignment = FileColumn.Alignment.RIGHT,
        nullable = false
    )
    private String warrantNumber;

    @FileColumn(
        order = 2,
        name = "ISSUE_DATE",
        length = 8,
        format = "yyyyMMdd",
        nullable = false
    )
    private LocalDate issueDate;

    @FileColumn(
        order = 3,
        name = "PAID_DATE",
        length = 8,
        format = "yyyyMMdd",
        nullable = true
    )
    private LocalDate paidDate;

    @FileColumn(
        order = 4,
        name = "AMOUNT",
        length = 12,
        padChar = '0',
        alignment = FileColumn.Alignment.RIGHT,
        format = "000000000000",  // 12 digits, 2 decimal implied
        nullable = false
    )
    private BigDecimal amount;

    @FileColumn(
        order = 5,
        name = "COUNTY_CODE",
        length = 2,
        nullable = false
    )
    private String countyCode;

    @FileColumn(
        order = 6,
        name = "PROVIDER_ID",
        length = 9,
        nullable = false
    )
    private String providerId;

    @FileColumn(
        order = 7,
        name = "CASE_NUMBER",
        length = 10,
        nullable = false
    )
    private String caseNumber;

    @FileColumn(
        order = 8,
        name = "STATUS",
        length = 1,
        nullable = false
    )
    private String statusCode;

    /**
     * Converts the status code to enum.
     */
    public WarrantStatus getStatus() {
        return WarrantStatus.fromCode(statusCode);
    }

    /**
     * Sets status from enum.
     */
    public void setStatus(WarrantStatus status) {
        this.statusCode = status.getCode();
    }

    /**
     * Warrant status enumeration matching STO codes.
     */
    public enum WarrantStatus {
        PAID("P"),
        VOIDED("V"),
        STALE("S");

        private final String code;

        WarrantStatus(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static WarrantStatus fromCode(String code) {
            if (code == null) {
                return null;
            }
            for (WarrantStatus status : values()) {
                if (status.code.equals(code.trim())) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown warrant status code: " + code);
        }
    }
}
