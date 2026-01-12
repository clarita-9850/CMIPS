package com.cmips.baw;

import java.util.List;

/**
 * BAW (Business Automation Workflow) File Service Interface.
 *
 * This interface defines the contract between the batch processing system
 * and the BAW middleware layer. Your coworker should implement this interface
 * in the BAW library.
 *
 * BAW handles:
 * - SFTP file retrieval/delivery
 * - File format conversion (fixed-width <-> JSON <-> XML)
 * - File splitting/combining
 * - Encryption/decryption
 *
 * This system handles:
 * - Business logic transformations
 * - Data validation
 * - Database operations
 */
public interface BawFileService {

    /**
     * Fetch an inbound file from external source (e.g., STO, EDD, DOJ).
     * BAW handles: SFTP download, decryption, format conversion to JSON.
     *
     * @param sourceSystem The external system identifier (e.g., "STO", "EDD", "DOJ")
     * @param fileType The type of file to fetch (e.g., "WARRANT_PAID", "BENEFITS_FILE")
     * @return List of records as JSON-like DTOs (BAW converts from fixed-width)
     */
    <T> List<T> fetchInboundFile(String sourceSystem, String fileType, Class<T> recordType);

    /**
     * Send an outbound file to external destination.
     * BAW handles: Format conversion from JSON, encryption, SFTP upload.
     *
     * @param destinationSystem The external system identifier
     * @param fileType The type of file being sent
     * @param records The records to send (as DTOs)
     * @return File reference/tracking ID
     */
    <T> String sendOutboundFile(String destinationSystem, String fileType, List<T> records);

    /**
     * Check if an inbound file is available for processing.
     *
     * @param sourceSystem The external system identifier
     * @param fileType The type of file to check
     * @return true if file is available
     */
    boolean isFileAvailable(String sourceSystem, String fileType);

    /**
     * Get file metadata (record count, file date, etc.) without fetching full content.
     *
     * @param sourceSystem The external system identifier
     * @param fileType The type of file
     * @return File metadata
     */
    BawFileMetadata getFileMetadata(String sourceSystem, String fileType);

    /**
     * Acknowledge successful processing of an inbound file.
     * BAW may archive or delete the source file.
     *
     * @param sourceSystem The external system identifier
     * @param fileType The type of file
     * @param fileReference The file reference from metadata
     */
    void acknowledgeFileProcessed(String sourceSystem, String fileType, String fileReference);

    /**
     * Report a processing error for an inbound file.
     * BAW may move file to error directory or send alerts.
     *
     * @param sourceSystem The external system identifier
     * @param fileType The type of file
     * @param fileReference The file reference
     * @param errorMessage Description of the error
     */
    void reportProcessingError(String sourceSystem, String fileType, String fileReference, String errorMessage);
}
