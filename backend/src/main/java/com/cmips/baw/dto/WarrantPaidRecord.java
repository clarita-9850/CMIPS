package com.cmips.baw.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Warrant Paid records from STO (State Treasurer's Office).
 *
 * BAW converts the fixed-width STO file format into this JSON-friendly DTO.
 *
 * Original STO File Format (PRDR110A):
 * Positions 1-10:   Warrant Number (right-justified, zero-filled)
 * Positions 11-18:  Issue Date (YYYYMMDD)
 * Positions 19-26:  Paid Date (YYYYMMDD)
 * Positions 27-38:  Amount (12 digits, 2 decimal implied)
 * Positions 39-40:  County Code
 * Positions 41-49:  Provider ID
 * Positions 50-59:  Case Number
 * Positions 60-60:  Warrant Status (P=Paid, V=Voided, S=Stale)
 */
public record WarrantPaidRecord(
    String warrantNumber,
    LocalDate issueDate,
    LocalDate paidDate,
    BigDecimal amount,
    String countyCode,
    String providerId,
    String caseNumber,
    WarrantStatus status
) {
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
            for (WarrantStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown warrant status code: " + code);
        }
    }
}
