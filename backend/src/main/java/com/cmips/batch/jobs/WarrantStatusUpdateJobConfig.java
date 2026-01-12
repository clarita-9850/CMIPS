package com.cmips.batch.jobs;

import com.cmips.batch.JobExecutionNotificationListener;
import com.cmips.batch.StepProgressListener;
import com.cmips.baw.BawFileMetadata;
import com.cmips.baw.BawFileService;
import com.cmips.baw.dto.WarrantPaidRecord;
import com.cmips.entity.WarrantEntity;
import com.cmips.entity.WarrantEntity.WarrantStatus;
import com.cmips.repository.WarrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Batch Job Configuration for WARRANT_STATUS_UPDATE (PRDR110A).
 *
 * This job processes daily warrant payment status files from STO (State Treasurer's Office).
 * When providers cash their IHSS payment warrants, STO sends a daily file containing
 * warrant numbers and their status (PAID, VOIDED, or STALE).
 *
 * Data Flow:
 * ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────────────────────┐
 * │    STO      │───▶│    BAW      │───▶│  This Job (Warrant Status Update)      │
 * │  (SFTP)     │    │ (Middleware)│    │  - Validates file                      │
 * │             │    │             │    │  - Updates warrant status in DB         │
 * │ Fixed-width │    │ Converts to │    │  - Logs statistics                      │
 * │    file     │    │    JSON     │    │  - Notifies completion                  │
 * └─────────────┘    └─────────────┘    └─────────────────────────────────────────┘
 *
 * Job Flow:
 * ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
 * │   Check File    │───▶│   Fetch &       │───▶│   Update        │───▶│   Generate      │
 * │   Availability  │    │   Validate      │    │   Database      │    │   Summary       │
 * └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
 *
 * Legacy Reference: PRDR110A - Daily warrant paid file from STO
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WarrantStatusUpdateJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionNotificationListener jobListener;
    private final StepProgressListener stepListener;
    private final BawFileService bawFileService;
    private final WarrantRepository warrantRepository;

    private static final String SOURCE_SYSTEM = "STO";
    private static final String FILE_TYPE = "WARRANT_PAID";

    // ==========================================
    // JOB DEFINITION
    // ==========================================

    /**
     * Define the WARRANT_STATUS_UPDATE job.
     *
     * This job runs daily (typically early morning) after STO sends the warrant paid file.
     * It updates the payment status of warrants in the database based on the STO file.
     */
    @Bean
    public Job warrantStatusUpdateJob() {
        return new JobBuilder("warrantStatusUpdateJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)  // Publishes Redis events for real-time status
            .start(checkFileAvailabilityStep())
            .next(fetchAndValidateStep())
            .next(updateDatabaseStep())
            .next(generateSummaryStep())
            .build();
    }

    // ==========================================
    // STEP 1: Check File Availability
    // ==========================================

    @Bean
    public Step checkFileAvailabilityStep() {
        return new StepBuilder("checkFileAvailabilityStep", jobRepository)
            .listener(stepListener)
            .tasklet(checkFileAvailabilityTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet checkFileAvailabilityTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 1: Checking File Availability ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            // Store total steps for progress tracking
            executionContext.put("totalSteps", 4);

            // Check if file is available from BAW
            boolean fileAvailable = bawFileService.isFileAvailable(SOURCE_SYSTEM, FILE_TYPE);

            if (!fileAvailable) {
                log.warn("No warrant paid file available from STO today");
                // Store flag to skip processing
                executionContext.put("skipProcessing", true);
                executionContext.put("skipReason", "No file available from STO");
                return RepeatStatus.FINISHED;
            }

            // Get file metadata
            BawFileMetadata metadata = bawFileService.getFileMetadata(SOURCE_SYSTEM, FILE_TYPE);
            log.info("File available: {} with {} records", metadata.originalFileName(), metadata.recordCount());

            // Store metadata in context
            executionContext.put("skipProcessing", false);
            executionContext.put("fileReference", metadata.fileReference());
            executionContext.put("originalFileName", metadata.originalFileName());
            executionContext.put("expectedRecordCount", metadata.recordCount());

            return RepeatStatus.FINISHED;
        };
    }

    // ==========================================
    // STEP 2: Fetch and Validate File
    // ==========================================

    @Bean
    public Step fetchAndValidateStep() {
        return new StepBuilder("fetchAndValidateStep", jobRepository)
            .listener(stepListener)
            .tasklet(fetchAndValidateTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet fetchAndValidateTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 2: Fetching and Validating File ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            // Check if we should skip
            Boolean skipProcessing = (Boolean) executionContext.get("skipProcessing");
            if (Boolean.TRUE.equals(skipProcessing)) {
                log.info("Skipping fetch - no file available");
                return RepeatStatus.FINISHED;
            }

            String fileReference = (String) executionContext.get("fileReference");

            try {
                // Fetch file from BAW (BAW handles SFTP, decryption, format conversion)
                List<WarrantPaidRecord> records = bawFileService.fetchInboundFile(
                    SOURCE_SYSTEM, FILE_TYPE, WarrantPaidRecord.class);

                log.info("Fetched {} warrant records from BAW", records.size());

                // Validate record counts
                long expectedCount = (long) executionContext.get("expectedRecordCount");
                if (records.size() != expectedCount) {
                    log.warn("Record count mismatch: expected {}, got {}", expectedCount, records.size());
                }

                // Validate individual records
                AtomicInteger validCount = new AtomicInteger(0);
                AtomicInteger invalidCount = new AtomicInteger(0);

                records.forEach(record -> {
                    if (isValidRecord(record)) {
                        validCount.incrementAndGet();
                    } else {
                        invalidCount.incrementAndGet();
                        log.warn("Invalid record: warrant={}", record.warrantNumber());
                    }
                });

                log.info("Validation complete: {} valid, {} invalid", validCount.get(), invalidCount.get());

                // Store validated records in context (as serializable format)
                // For large files, you'd use a staging table instead
                executionContext.put("validRecordCount", validCount.get());
                executionContext.put("invalidRecordCount", invalidCount.get());
                executionContext.put("totalRecordCount", records.size());

                // Store job execution ID for the update step
                Long jobExecutionId = chunkContext.getStepContext()
                    .getStepExecution().getJobExecution().getId();
                executionContext.put("jobExecutionId", jobExecutionId);

            } catch (Exception e) {
                log.error("Error fetching file from BAW: {}", e.getMessage());
                bawFileService.reportProcessingError(SOURCE_SYSTEM, FILE_TYPE, fileReference, e.getMessage());
                throw e;
            }

            return RepeatStatus.FINISHED;
        };
    }

    private boolean isValidRecord(WarrantPaidRecord record) {
        return record.warrantNumber() != null && !record.warrantNumber().isBlank()
            && record.issueDate() != null
            && record.amount() != null
            && record.countyCode() != null && record.countyCode().matches("\\d{2}")
            && record.providerId() != null
            && record.caseNumber() != null
            && record.status() != null;
    }

    // ==========================================
    // STEP 3: Update Database
    // ==========================================

    @Bean
    public Step updateDatabaseStep() {
        return new StepBuilder("updateDatabaseStep", jobRepository)
            .listener(stepListener)
            .tasklet(updateDatabaseTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet updateDatabaseTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 3: Updating Database ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            // Check if we should skip
            Boolean skipProcessing = (Boolean) executionContext.get("skipProcessing");
            if (Boolean.TRUE.equals(skipProcessing)) {
                log.info("Skipping database update - no file available");
                return RepeatStatus.FINISHED;
            }

            String fileReference = (String) executionContext.get("fileReference");
            Long jobExecutionId = (Long) executionContext.get("jobExecutionId");

            // Re-fetch records (in production, use staging table for large files)
            List<WarrantPaidRecord> records = bawFileService.fetchInboundFile(
                SOURCE_SYSTEM, FILE_TYPE, WarrantPaidRecord.class);

            AtomicInteger insertedCount = new AtomicInteger(0);
            AtomicInteger updatedCount = new AtomicInteger(0);
            AtomicInteger skippedCount = new AtomicInteger(0);

            LocalDateTime now = LocalDateTime.now();

            for (WarrantPaidRecord record : records) {
                if (!isValidRecord(record)) {
                    skippedCount.incrementAndGet();
                    continue;
                }

                try {
                    // Check if warrant already exists
                    var existingWarrant = warrantRepository.findByWarrantNumber(record.warrantNumber());

                    if (existingWarrant.isPresent()) {
                        // Update existing warrant
                        WarrantEntity warrant = existingWarrant.get();
                        WarrantStatus newStatus = mapStatus(record.status());

                        // Only update if status actually changed
                        if (warrant.getStatus() != newStatus) {
                            warrant.setStatus(newStatus);
                            warrant.setPaidDate(record.paidDate());
                            warrant.setStatusUpdatedAt(now);
                            warrant.setBatchJobExecutionId(jobExecutionId);
                            warrantRepository.save(warrant);
                            updatedCount.incrementAndGet();
                            log.debug("Updated warrant {}: {} -> {}",
                                record.warrantNumber(), warrant.getStatus(), newStatus);
                        } else {
                            skippedCount.incrementAndGet();
                        }
                    } else {
                        // Insert new warrant (this is unusual for status update file,
                        // but handle it for robustness)
                        WarrantEntity newWarrant = new WarrantEntity();
                        newWarrant.setWarrantNumber(record.warrantNumber());
                        newWarrant.setIssueDate(record.issueDate());
                        newWarrant.setPaidDate(record.paidDate());
                        newWarrant.setAmount(record.amount());
                        newWarrant.setCountyCode(record.countyCode());
                        newWarrant.setProviderId(record.providerId());
                        newWarrant.setCaseNumber(record.caseNumber());
                        newWarrant.setStatus(mapStatus(record.status()));
                        newWarrant.setSourceFileReference(fileReference);
                        newWarrant.setBatchJobExecutionId(jobExecutionId);
                        newWarrant.setStatusUpdatedAt(now);
                        warrantRepository.save(newWarrant);
                        insertedCount.incrementAndGet();
                        log.debug("Inserted new warrant {}", record.warrantNumber());
                    }
                } catch (Exception e) {
                    log.error("Error processing warrant {}: {}", record.warrantNumber(), e.getMessage());
                    skippedCount.incrementAndGet();
                }
            }

            log.info("Database update complete: {} inserted, {} updated, {} skipped",
                insertedCount.get(), updatedCount.get(), skippedCount.get());

            // Store counts for summary
            executionContext.put("insertedCount", insertedCount.get());
            executionContext.put("updatedCount", updatedCount.get());
            executionContext.put("skippedCount", skippedCount.get());

            // Acknowledge file processed with BAW
            bawFileService.acknowledgeFileProcessed(SOURCE_SYSTEM, FILE_TYPE, fileReference);

            return RepeatStatus.FINISHED;
        };
    }

    private WarrantStatus mapStatus(WarrantPaidRecord.WarrantStatus bawStatus) {
        return switch (bawStatus) {
            case PAID -> WarrantStatus.PAID;
            case VOIDED -> WarrantStatus.VOIDED;
            case STALE -> WarrantStatus.STALE;
        };
    }

    // ==========================================
    // STEP 4: Generate Summary
    // ==========================================

    @Bean
    public Step generateSummaryStep() {
        return new StepBuilder("generateSummaryStep", jobRepository)
            .listener(stepListener)
            .tasklet(generateSummaryTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet generateSummaryTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 4: Generating Summary ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            // Check if we should skip
            Boolean skipProcessing = (Boolean) executionContext.get("skipProcessing");
            if (Boolean.TRUE.equals(skipProcessing)) {
                String skipReason = (String) executionContext.get("skipReason");
                log.info("================================================");
                log.info("  WARRANT_STATUS_UPDATE JOB SKIPPED");
                log.info("  Reason: {}", skipReason);
                log.info("================================================");
                return RepeatStatus.FINISHED;
            }

            String fileReference = (String) executionContext.get("fileReference");
            String originalFileName = (String) executionContext.get("originalFileName");
            int totalRecordCount = (int) executionContext.get("totalRecordCount");
            int validRecordCount = (int) executionContext.get("validRecordCount");
            int invalidRecordCount = (int) executionContext.get("invalidRecordCount");
            int insertedCount = (int) executionContext.get("insertedCount");
            int updatedCount = (int) executionContext.get("updatedCount");
            int skippedCount = (int) executionContext.get("skippedCount");

            // Get current totals from database
            long totalPaid = warrantRepository.countByStatus(WarrantStatus.PAID);
            long totalVoided = warrantRepository.countByStatus(WarrantStatus.VOIDED);
            long totalStale = warrantRepository.countByStatus(WarrantStatus.STALE);

            log.info("================================================");
            log.info("  WARRANT_STATUS_UPDATE JOB COMPLETED");
            log.info("================================================");
            log.info("  Source File: {}", originalFileName);
            log.info("  File Reference: {}", fileReference);
            log.info("------------------------------------------------");
            log.info("  File Statistics:");
            log.info("    Total Records: {}", totalRecordCount);
            log.info("    Valid Records: {}", validRecordCount);
            log.info("    Invalid Records: {}", invalidRecordCount);
            log.info("------------------------------------------------");
            log.info("  Database Changes:");
            log.info("    Inserted: {}", insertedCount);
            log.info("    Updated: {}", updatedCount);
            log.info("    Skipped: {}", skippedCount);
            log.info("------------------------------------------------");
            log.info("  Current Database Totals:");
            log.info("    PAID Warrants: {}", totalPaid);
            log.info("    VOIDED Warrants: {}", totalVoided);
            log.info("    STALE Warrants: {}", totalStale);
            log.info("================================================");

            return RepeatStatus.FINISHED;
        };
    }
}
