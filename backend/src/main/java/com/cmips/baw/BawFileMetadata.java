package com.cmips.baw;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Metadata about a file available in BAW.
 */
public record BawFileMetadata(
    String fileReference,
    String sourceSystem,
    String fileType,
    String originalFileName,
    LocalDate fileDate,
    LocalDateTime receivedAt,
    long recordCount,
    long fileSizeBytes,
    String checksum,
    FileStatus status
) {
    public enum FileStatus {
        AVAILABLE,
        PROCESSING,
        PROCESSED,
        ERROR
    }
}
