package com.cmips.baw;

import com.cmips.integration.framework.baw.format.FileFormat;
import com.cmips.integration.framework.baw.repository.FileRepository;
import com.cmips.integration.framework.support.FilePattern;
import com.cmips.integration.framework.support.RemoteFile;
import com.cmips.integration.framework.support.SftpClient;
import com.cmips.integration.framework.support.SftpConfig;
import com.cmips.baw.config.BawIntegrationProperties;
import com.cmips.baw.config.BawIntegrationProperties.SftpSystemConfig;
import com.cmips.baw.dto.PaymentRecord;
import com.cmips.baw.dto.WarrantPaidRecord;
import com.cmips.baw.filetype.PaymentFileRecord;
import com.cmips.baw.filetype.WarrantPaidFileRecord;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real implementation of BawFileService using the Integration Hub Framework.
 *
 * This service connects to external SFTP servers (STO, SCO, EDD, DOJ) to
 * download/upload files and uses the Integration Hub's FileRepository
 * for file format conversion (fixed-width <-> JSON <-> DTOs).
 *
 * Enabled when: baw.mock.enabled=false
 *
 * Note: Bean creation is managed by BawIntegrationConfig - no @Service annotation needed.
 */
@Slf4j
public class IntegrationHubBawFileService implements BawFileService {

    private final BawIntegrationProperties properties;
    private final FileRepository<WarrantPaidFileRecord> warrantFileRepository;
    private final FileRepository<PaymentFileRecord> paymentFileRepository;

    // Track processed files for acknowledgment
    private final Map<String, FileTrackingInfo> trackedFiles = new ConcurrentHashMap<>();

    // Supported system/file type combinations
    private static final String STO = "STO";
    private static final String SCO = "SCO";
    private static final String WARRANT_PAID = "WARRANT_PAID";
    private static final String PAYMENT_REQUEST = "PAYMENT_REQUEST";

    public IntegrationHubBawFileService(BawIntegrationProperties properties) {
        this.properties = properties;
        this.warrantFileRepository = FileRepository.forType(WarrantPaidFileRecord.class);
        this.paymentFileRepository = FileRepository.forType(PaymentFileRecord.class);

        // Ensure local directories exist
        createLocalDirectories();

        log.info("IntegrationHubBawFileService initialized with Integration Hub Framework");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> fetchInboundFile(String sourceSystem, String fileType, Class<T> recordType) {
        log.info("=== INTEGRATION HUB: Fetching inbound file ===");
        log.info("Source System: {}, File Type: {}, Record Type: {}",
                sourceSystem, fileType, recordType.getSimpleName());

        validateInboundRequest(sourceSystem, fileType, recordType);

        SftpSystemConfig config = getSystemConfig(sourceSystem);
        Path localFile = null;

        try (SftpClient sftpClient = createSftpClient(config)) {
            sftpClient.connect();

            // Find the latest file matching the pattern
            String pattern = getFilePattern(sourceSystem, fileType);
            List<RemoteFile> files = sftpClient.listFiles(config.getInboundPath(), FilePattern.glob(pattern));

            if (files.isEmpty()) {
                log.warn("No files found matching pattern: {} in {}", pattern, config.getInboundPath());
                return List.of();
            }

            // Get most recent file
            RemoteFile latestFile = files.stream()
                    .max((a, b) -> a.getLastModified().compareTo(b.getLastModified()))
                    .orElseThrow();

            log.info("Found file: {} (size: {} bytes, modified: {})",
                    latestFile.getName(), latestFile.getSize(), latestFile.getLastModified());

            // Download to local temp directory
            Path localDir = Paths.get(properties.getLocal().getInboundDir());
            localFile = sftpClient.download(latestFile.getPath(), localDir);
            log.info("Downloaded to: {}", localFile);

            // Track this file for later acknowledgment
            String fileReference = generateFileReference(sourceSystem, fileType);
            trackedFiles.put(fileReference, new FileTrackingInfo(
                    fileReference, sourceSystem, fileType,
                    latestFile.getName(), latestFile.getPath(),
                    localFile, LocalDateTime.now()
            ));

            // Parse the file based on type
            if (STO.equals(sourceSystem) && WARRANT_PAID.equals(fileType)) {
                List<WarrantPaidFileRecord> fileRecords = warrantFileRepository.read(
                        localFile, FileFormat.fixedWidth().build()
                );
                log.info("Parsed {} warrant records from file", fileRecords.size());

                // Convert to DTOs
                List<WarrantPaidRecord> dtos = convertToWarrantDtos(fileRecords);
                return (List<T>) dtos;
            }

            throw new UnsupportedOperationException(
                    "Unsupported source/type combination: " + sourceSystem + "/" + fileType);

        } catch (Exception e) {
            log.error("Failed to fetch inbound file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch inbound file from " + sourceSystem, e);
        }
    }

    @Override
    public <T> String sendOutboundFile(String destinationSystem, String fileType, List<T> records) {
        log.info("=== INTEGRATION HUB: Sending outbound file ===");
        log.info("Destination: {}, File Type: {}, Record Count: {}",
                destinationSystem, fileType, records.size());

        validateOutboundRequest(destinationSystem, fileType);

        if (records.isEmpty()) {
            log.warn("No records to send");
            return "EMPTY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        SftpSystemConfig config = getSystemConfig(destinationSystem);
        String fileReference = generateFileReference(destinationSystem, fileType);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("PRDR120A_%s.DAT", timestamp);

        try {
            // Convert DTOs to file records
            List<PaymentFileRecord> fileRecords;
            if (records.get(0) instanceof PaymentRecord) {
                @SuppressWarnings("unchecked")
                List<PaymentRecord> paymentRecords = (List<PaymentRecord>) records;
                fileRecords = convertToPaymentFileRecords(paymentRecords);
            } else {
                throw new IllegalArgumentException("Unsupported record type: " + records.get(0).getClass());
            }

            // Write to local file first
            Path localDir = Paths.get(properties.getLocal().getOutboundDir());
            Files.createDirectories(localDir);
            Path localFile = localDir.resolve(fileName);

            paymentFileRepository.write(fileRecords, localFile, FileFormat.fixedWidth().build());
            log.info("Written {} records to local file: {}", fileRecords.size(), localFile);

            // Upload via SFTP
            try (SftpClient sftpClient = createSftpClient(config)) {
                sftpClient.connect();

                String remotePath = config.getOutboundPath();
                sftpClient.upload(localFile, remotePath, fileName);
                log.info("Uploaded to SFTP: {}/{}", remotePath, fileName);
            }

            // Track the file
            trackedFiles.put(fileReference, new FileTrackingInfo(
                    fileReference, destinationSystem, fileType,
                    fileName, config.getOutboundPath() + "/" + fileName,
                    localFile, LocalDateTime.now()
            ));

            // Cleanup local file if configured
            if (properties.getProcessing().isDeleteAfterProcess()) {
                Files.deleteIfExists(localFile);
                log.debug("Deleted local temp file: {}", localFile);
            }

            log.info("Successfully sent outbound file. Reference: {}", fileReference);
            return fileReference;

        } catch (Exception e) {
            log.error("Failed to send outbound file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send outbound file to " + destinationSystem, e);
        }
    }

    @Override
    public boolean isFileAvailable(String sourceSystem, String fileType) {
        log.info("=== INTEGRATION HUB: Checking file availability ===");
        log.info("Source: {}, Type: {}", sourceSystem, fileType);

        SftpSystemConfig config = getSystemConfig(sourceSystem);

        try (SftpClient sftpClient = createSftpClient(config)) {
            sftpClient.connect();

            String pattern = getFilePattern(sourceSystem, fileType);
            List<RemoteFile> files = sftpClient.listFiles(config.getInboundPath(), FilePattern.glob(pattern));

            boolean available = !files.isEmpty();
            log.info("File available: {} (found {} matching files)", available, files.size());
            return available;

        } catch (Exception e) {
            log.error("Failed to check file availability: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public BawFileMetadata getFileMetadata(String sourceSystem, String fileType) {
        log.info("=== INTEGRATION HUB: Getting file metadata ===");

        SftpSystemConfig config = getSystemConfig(sourceSystem);

        try (SftpClient sftpClient = createSftpClient(config)) {
            sftpClient.connect();

            String pattern = getFilePattern(sourceSystem, fileType);
            List<RemoteFile> files = sftpClient.listFiles(config.getInboundPath(), FilePattern.glob(pattern));

            if (files.isEmpty()) {
                log.warn("No files found for metadata query");
                return new BawFileMetadata(
                        null, sourceSystem, fileType, null,
                        null, null, 0, 0, null,
                        BawFileMetadata.FileStatus.AVAILABLE
                );
            }

            // Get most recent file
            RemoteFile latestFile = files.stream()
                    .max((a, b) -> a.getLastModified().compareTo(b.getLastModified()))
                    .orElseThrow();

            String fileReference = generateFileReference(sourceSystem, fileType);

            // Estimate record count based on file size (60 bytes per record for warrant files)
            long estimatedRecords = latestFile.getSize() / 60;

            BawFileMetadata metadata = new BawFileMetadata(
                    fileReference,
                    sourceSystem,
                    fileType,
                    latestFile.getName(),
                    LocalDate.now(), // File date approximation
                    LocalDateTime.now(),
                    estimatedRecords,
                    latestFile.getSize(),
                    "SFTP-" + latestFile.getLastModified().toEpochMilli(),
                    BawFileMetadata.FileStatus.AVAILABLE
            );

            log.info("File metadata: {}", metadata);
            return metadata;

        } catch (Exception e) {
            log.error("Failed to get file metadata: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get file metadata from " + sourceSystem, e);
        }
    }

    @Override
    public void acknowledgeFileProcessed(String sourceSystem, String fileType, String fileReference) {
        log.info("=== INTEGRATION HUB: Acknowledging file processed ===");
        log.info("File Reference: {}", fileReference);

        FileTrackingInfo trackingInfo = trackedFiles.get(fileReference);
        if (trackingInfo == null) {
            log.warn("No tracking info found for file reference: {}", fileReference);
            return;
        }

        SftpSystemConfig config = getSystemConfig(sourceSystem);

        try (SftpClient sftpClient = createSftpClient(config)) {
            sftpClient.connect();

            if (properties.getProcessing().isArchiveAfterProcess()) {
                // Move file to archive directory
                String archivePath = config.getArchivePath() + "/" + trackingInfo.originalFileName;
                sftpClient.rename(trackingInfo.remotePath, archivePath);
                log.info("Archived file to: {}", archivePath);
            }

            // Cleanup local file
            if (trackingInfo.localPath != null && Files.exists(trackingInfo.localPath)) {
                Files.deleteIfExists(trackingInfo.localPath);
                log.debug("Deleted local file: {}", trackingInfo.localPath);
            }

            // Remove from tracking
            trackedFiles.remove(fileReference);
            log.info("File processing acknowledged successfully");

        } catch (Exception e) {
            log.error("Failed to acknowledge file: {}", e.getMessage(), e);
            // Don't throw - acknowledgment failure shouldn't break the flow
        }
    }

    @Override
    public void reportProcessingError(String sourceSystem, String fileType, String fileReference, String errorMessage) {
        log.error("=== INTEGRATION HUB: Processing error reported ===");
        log.error("File Reference: {}", fileReference);
        log.error("Error: {}", errorMessage);

        FileTrackingInfo trackingInfo = trackedFiles.get(fileReference);
        if (trackingInfo == null) {
            log.warn("No tracking info found for file reference: {}", fileReference);
            return;
        }

        SftpSystemConfig config = getSystemConfig(sourceSystem);

        try (SftpClient sftpClient = createSftpClient(config)) {
            sftpClient.connect();

            // Move file to error directory
            String errorPath = config.getErrorPath() + "/" + trackingInfo.originalFileName;
            sftpClient.rename(trackingInfo.remotePath, errorPath);
            log.info("Moved file to error directory: {}", errorPath);

            // Remove from tracking
            trackedFiles.remove(fileReference);

        } catch (Exception e) {
            log.error("Failed to move file to error directory: {}", e.getMessage(), e);
        }
    }

    // ========== Helper Methods ==========

    private void createLocalDirectories() {
        try {
            Files.createDirectories(Paths.get(properties.getLocal().getTempDir()));
            Files.createDirectories(Paths.get(properties.getLocal().getInboundDir()));
            Files.createDirectories(Paths.get(properties.getLocal().getOutboundDir()));
            Files.createDirectories(Paths.get(properties.getLocal().getArchiveDir()));
        } catch (IOException e) {
            log.warn("Failed to create local directories: {}", e.getMessage());
        }
    }

    private SftpSystemConfig getSystemConfig(String systemCode) {
        SftpSystemConfig config = properties.getSystemConfig(systemCode);
        if (config == null) {
            throw new IllegalArgumentException("No SFTP configuration found for system: " + systemCode);
        }
        return config;
    }

    private SftpClient createSftpClient(SftpSystemConfig config) {
        SftpConfig.Builder builder = SftpConfig.builder()
                .host(config.getHost())
                .port(config.getPort())
                .username(config.getUsername())
                .connectionTimeout(config.getConnectionTimeout())
                .strictHostKeyChecking(config.getKnownHosts() != null && !config.getKnownHosts().isEmpty());

        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            builder.password(config.getPassword());
        }

        if (config.getPrivateKeyPath() != null && !config.getPrivateKeyPath().isEmpty()) {
            builder.privateKeyFile(Paths.get(config.getPrivateKeyPath()));
            if (config.getPrivateKeyPassphrase() != null) {
                builder.privateKeyPassphrase(config.getPrivateKeyPassphrase());
            }
        }

        if (config.getKnownHosts() != null && !config.getKnownHosts().isEmpty()) {
            builder.knownHostsFile(Paths.get(config.getKnownHosts()));
        }

        return new SftpClient(builder.build());
    }

    private String getFilePattern(String sourceSystem, String fileType) {
        if (STO.equals(sourceSystem) && WARRANT_PAID.equals(fileType)) {
            return properties.getProcessing().getStoWarrantFilePattern();
        }
        return "*";
    }

    private String generateFileReference(String system, String fileType) {
        return String.format("%s-%s-%s-%s",
                system,
                fileType,
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    private void validateInboundRequest(String sourceSystem, String fileType, Class<?> recordType) {
        if (STO.equals(sourceSystem) && WARRANT_PAID.equals(fileType)) {
            if (!WarrantPaidRecord.class.equals(recordType)) {
                throw new IllegalArgumentException(
                        "Expected WarrantPaidRecord for STO/WARRANT_PAID, got: " + recordType);
            }
            return;
        }
        throw new UnsupportedOperationException(
                "Unsupported inbound combination: " + sourceSystem + "/" + fileType);
    }

    private void validateOutboundRequest(String destinationSystem, String fileType) {
        if (SCO.equals(destinationSystem) && PAYMENT_REQUEST.equals(fileType)) {
            return;
        }
        throw new UnsupportedOperationException(
                "Unsupported outbound combination: " + destinationSystem + "/" + fileType);
    }

    /**
     * Converts file records to WarrantPaidRecord DTOs.
     */
    private List<WarrantPaidRecord> convertToWarrantDtos(List<WarrantPaidFileRecord> fileRecords) {
        List<WarrantPaidRecord> dtos = new ArrayList<>();
        for (WarrantPaidFileRecord fr : fileRecords) {
            WarrantPaidRecord.WarrantStatus status = switch (fr.getStatusCode()) {
                case "P" -> WarrantPaidRecord.WarrantStatus.PAID;
                case "V" -> WarrantPaidRecord.WarrantStatus.VOIDED;
                case "S" -> WarrantPaidRecord.WarrantStatus.STALE;
                default -> throw new IllegalArgumentException("Unknown status: " + fr.getStatusCode());
            };

            dtos.add(new WarrantPaidRecord(
                    fr.getWarrantNumber(),
                    fr.getIssueDate(),
                    fr.getPaidDate(),
                    fr.getAmount(),
                    fr.getCountyCode(),
                    fr.getProviderId(),
                    fr.getCaseNumber(),
                    status
            ));
        }
        return dtos;
    }

    /**
     * Converts PaymentRecord DTOs to file records.
     */
    private List<PaymentFileRecord> convertToPaymentFileRecords(List<PaymentRecord> dtos) {
        List<PaymentFileRecord> fileRecords = new ArrayList<>();
        for (PaymentRecord dto : dtos) {
            String typeCode = switch (dto.paymentType()) {
                case REGULAR -> "R";
                case ADJUSTMENT -> "A";
                case RETROACTIVE -> "T";
            };

            fileRecords.add(PaymentFileRecord.builder()
                    .paymentRequestId(dto.paymentRequestId())
                    .providerId(dto.providerId())
                    .providerName(dto.providerName())
                    .caseNumber(dto.caseNumber())
                    .countyCode(dto.countyCode())
                    .payPeriodStart(dto.payPeriodStart())
                    .payPeriodEnd(dto.payPeriodEnd())
                    .regularHours(dto.regularHours())
                    .overtimeHours(dto.overtimeHours())
                    .totalHours(dto.totalHours())
                    .paymentAmount(dto.paymentAmount())
                    .timesheetId(dto.timesheetId())
                    .paymentTypeCode(typeCode)
                    .build());
        }
        return fileRecords;
    }

    /**
     * Internal class for tracking processed files.
     */
    private record FileTrackingInfo(
            String fileReference,
            String sourceSystem,
            String fileType,
            String originalFileName,
            String remotePath,
            Path localPath,
            LocalDateTime processedAt
    ) {}
}
