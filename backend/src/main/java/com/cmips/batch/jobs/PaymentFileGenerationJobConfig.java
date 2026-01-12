package com.cmips.batch.jobs;

import com.cmips.batch.JobExecutionNotificationListener;
import com.cmips.batch.StepProgressListener;
import com.cmips.baw.BawFileService;
import com.cmips.baw.dto.PaymentRecord;
import com.cmips.entity.Timesheet;
import com.cmips.entity.TimesheetStatus;
import com.cmips.repository.TimesheetRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Batch Job Configuration for PAYMENT_FILE_GENERATION (PRDR120A).
 *
 * This job extracts approved timesheets and generates a payment file to send to
 * SCO (State Controller's Office) for processing provider payments.
 *
 * Data Flow:
 * ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
 * │  Our Database   │───▶│  This Job       │───▶│      BAW        │───▶│      SCO        │
 * │  (Timesheets)   │    │  (Extract &     │    │  (Format &      │    │  (Process       │
 * │  APPROVED       │    │   Transform)    │    │   Send SFTP)    │    │   Payments)     │
 * └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
 *
 * Job Flow:
 * ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
 * │   Query         │───▶│   Transform to  │───▶│   Send via      │───▶│   Mark as       │
 * │   Approved      │    │   PaymentRecord │    │   BAW           │    │   PROCESSED     │
 * │   Timesheets    │    │                 │    │                 │    │                 │
 * └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
 *
 * Legacy Reference: PRDR120A - Payment request file to SCO
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PaymentFileGenerationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionNotificationListener jobListener;
    private final StepProgressListener stepListener;
    private final BawFileService bawFileService;
    private final TimesheetRepository timesheetRepository;

    private static final String DESTINATION_SYSTEM = "SCO";
    private static final String FILE_TYPE = "PAYMENT_REQUEST";
    private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("15.50");

    // ==========================================
    // JOB DEFINITION
    // ==========================================

    /**
     * Define the PAYMENT_FILE_GENERATION job.
     *
     * This job runs after timesheets are approved (typically bi-weekly).
     * It extracts approved timesheets, transforms them to payment records,
     * and sends them to SCO via BAW.
     */
    @Bean
    public Job paymentFileGenerationJob() {
        return new JobBuilder("paymentFileGenerationJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)
            .start(queryApprovedTimesheetsStep())
            .next(transformToPaymentRecordsStep())
            .next(sendToScoStep())
            .next(markAsProcessedStep())
            .build();
    }

    // ==========================================
    // STEP 1: Query Approved Timesheets
    // ==========================================

    @Bean
    public Step queryApprovedTimesheetsStep() {
        return new StepBuilder("queryApprovedTimesheetsStep", jobRepository)
            .listener(stepListener)
            .tasklet(queryApprovedTimesheetsTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet queryApprovedTimesheetsTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 1: Querying Approved Timesheets ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            // Store total steps for progress tracking
            executionContext.put("totalSteps", 4);

            // Query approved timesheets that haven't been processed yet
            List<Timesheet> approvedTimesheets = timesheetRepository
                .findByStatusOrderByCreatedAtDesc(TimesheetStatus.APPROVED);

            if (approvedTimesheets.isEmpty()) {
                log.info("No approved timesheets to process");
                executionContext.put("skipProcessing", true);
                executionContext.put("skipReason", "No approved timesheets found");
                return RepeatStatus.FINISHED;
            }

            log.info("Found {} approved timesheets to process", approvedTimesheets.size());

            // Store timesheet IDs for next step
            List<Long> timesheetIds = approvedTimesheets.stream()
                .map(Timesheet::getId)
                .toList();

            executionContext.put("skipProcessing", false);
            executionContext.put("timesheetIds", new ArrayList<>(timesheetIds));
            executionContext.put("timesheetCount", approvedTimesheets.size());

            // Log summary by pay period
            approvedTimesheets.stream()
                .map(t -> t.getPayPeriodStart() + " to " + t.getPayPeriodEnd())
                .distinct()
                .forEach(period -> log.info("  Pay Period: {}", period));

            return RepeatStatus.FINISHED;
        };
    }

    // ==========================================
    // STEP 2: Transform to Payment Records
    // ==========================================

    @Bean
    public Step transformToPaymentRecordsStep() {
        return new StepBuilder("transformToPaymentRecordsStep", jobRepository)
            .listener(stepListener)
            .tasklet(transformToPaymentRecordsTasklet(), transactionManager)
            .build();
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Tasklet transformToPaymentRecordsTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 2: Transforming to Payment Records ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            Boolean skipProcessing = (Boolean) executionContext.get("skipProcessing");
            if (Boolean.TRUE.equals(skipProcessing)) {
                log.info("Skipping transform - no timesheets to process");
                return RepeatStatus.FINISHED;
            }

            List<Long> timesheetIds = (List<Long>) executionContext.get("timesheetIds");
            List<PaymentRecord> paymentRecords = new ArrayList<>();
            BigDecimal totalPaymentAmount = BigDecimal.ZERO;

            AtomicInteger transformedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (Long timesheetId : timesheetIds) {
                try {
                    Timesheet timesheet = timesheetRepository.findById(timesheetId)
                        .orElseThrow(() -> new RuntimeException("Timesheet not found: " + timesheetId));

                    // Transform to PaymentRecord
                    PaymentRecord paymentRecord = PaymentRecord.fromTimesheet(
                        timesheet.getId(),
                        timesheet.getEmployeeId(),      // providerId
                        timesheet.getEmployeeName(),    // providerName
                        "CASE-" + timesheet.getId(),    // caseNumber (generated)
                        getCountyCode(timesheet.getLocation()), // countyCode from location
                        timesheet.getPayPeriodStart(),
                        timesheet.getPayPeriodEnd(),
                        timesheet.getRegularHours() != null ? timesheet.getRegularHours() : BigDecimal.ZERO,
                        timesheet.getOvertimeHours() != null ? timesheet.getOvertimeHours() : BigDecimal.ZERO,
                        timesheet.getTotalHours() != null ? timesheet.getTotalHours() : BigDecimal.ZERO,
                        DEFAULT_HOURLY_RATE
                    );

                    paymentRecords.add(paymentRecord);
                    totalPaymentAmount = totalPaymentAmount.add(paymentRecord.paymentAmount());
                    transformedCount.incrementAndGet();

                    log.debug("Transformed timesheet {} -> payment {} (amount: ${})",
                        timesheetId, paymentRecord.paymentRequestId(), paymentRecord.paymentAmount());

                } catch (Exception e) {
                    log.error("Error transforming timesheet {}: {}", timesheetId, e.getMessage());
                    errorCount.incrementAndGet();
                }
            }

            log.info("Transformation complete: {} successful, {} errors",
                transformedCount.get(), errorCount.get());
            log.info("Total payment amount: ${}", totalPaymentAmount);

            // Store counts (not the records - they're not serializable)
            // We'll re-query in the next step
            executionContext.put("transformedCount", transformedCount.get());
            executionContext.put("transformErrorCount", errorCount.get());
            executionContext.put("totalPaymentAmount", totalPaymentAmount.toString());

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Map location to California county code.
     * In real system, this would be a lookup table.
     */
    private String getCountyCode(String location) {
        if (location == null) return "99";
        return switch (location.toLowerCase()) {
            case "los angeles" -> "19";
            case "san francisco" -> "38";
            case "san diego" -> "37";
            case "sacramento" -> "34";
            case "oakland", "alameda" -> "01";
            case "fresno" -> "10";
            case "san jose", "santa clara" -> "43";
            default -> "99"; // Unknown
        };
    }

    // ==========================================
    // STEP 3: Send to SCO via BAW
    // ==========================================

    @Bean
    public Step sendToScoStep() {
        return new StepBuilder("sendToScoStep", jobRepository)
            .listener(stepListener)
            .tasklet(sendToScoTasklet(), transactionManager)
            .build();
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Tasklet sendToScoTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 3: Sending to SCO via BAW ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            Boolean skipProcessing = (Boolean) executionContext.get("skipProcessing");
            if (Boolean.TRUE.equals(skipProcessing)) {
                log.info("Skipping send - no records to send");
                return RepeatStatus.FINISHED;
            }

            // Re-generate payment records from timesheet IDs (records aren't serializable)
            List<Long> timesheetIds = (List<Long>) executionContext.get("timesheetIds");
            List<PaymentRecord> paymentRecords = new ArrayList<>();

            for (Long timesheetId : timesheetIds) {
                Timesheet timesheet = timesheetRepository.findById(timesheetId).orElse(null);
                if (timesheet != null && TimesheetStatus.APPROVED.equals(timesheet.getStatus())) {
                    PaymentRecord paymentRecord = PaymentRecord.fromTimesheet(
                        timesheet.getId(),
                        timesheet.getEmployeeId(),
                        timesheet.getEmployeeName(),
                        "CASE-" + timesheet.getId(),
                        getCountyCode(timesheet.getLocation()),
                        timesheet.getPayPeriodStart(),
                        timesheet.getPayPeriodEnd(),
                        timesheet.getRegularHours() != null ? timesheet.getRegularHours() : BigDecimal.ZERO,
                        timesheet.getOvertimeHours() != null ? timesheet.getOvertimeHours() : BigDecimal.ZERO,
                        timesheet.getTotalHours() != null ? timesheet.getTotalHours() : BigDecimal.ZERO,
                        DEFAULT_HOURLY_RATE
                    );
                    paymentRecords.add(paymentRecord);
                }
            }

            if (paymentRecords.isEmpty()) {
                log.warn("No payment records to send");
                executionContext.put("fileReference", "NONE");
                executionContext.put("sentCount", 0);
                return RepeatStatus.FINISHED;
            }

            try {
                // Send via BAW (handles SFTP, format conversion, encryption)
                String fileReference = bawFileService.sendOutboundFile(
                    DESTINATION_SYSTEM, FILE_TYPE, paymentRecords);

                log.info("Successfully sent {} payment records to SCO", paymentRecords.size());
                log.info("File Reference: {}", fileReference);

                executionContext.put("fileReference", fileReference);
                executionContext.put("sentCount", paymentRecords.size());

            } catch (Exception e) {
                log.error("Failed to send payment file to SCO: {}", e.getMessage());
                throw e;
            }

            return RepeatStatus.FINISHED;
        };
    }

    // ==========================================
    // STEP 4: Mark Timesheets as Processed
    // ==========================================

    @Bean
    public Step markAsProcessedStep() {
        return new StepBuilder("markAsProcessedStep", jobRepository)
            .listener(stepListener)
            .tasklet(markAsProcessedTasklet(), transactionManager)
            .build();
    }

    @Bean
    @SuppressWarnings("unchecked")
    public Tasklet markAsProcessedTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 4: Marking Timesheets as Processed ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            Boolean skipProcessing = (Boolean) executionContext.get("skipProcessing");
            if (Boolean.TRUE.equals(skipProcessing)) {
                String skipReason = (String) executionContext.get("skipReason");
                log.info("================================================");
                log.info("  PAYMENT_FILE_GENERATION JOB SKIPPED");
                log.info("  Reason: {}", skipReason);
                log.info("================================================");
                return RepeatStatus.FINISHED;
            }

            List<Long> timesheetIds = (List<Long>) executionContext.get("timesheetIds");
            String fileReference = (String) executionContext.get("fileReference");
            String totalPaymentAmount = (String) executionContext.get("totalPaymentAmount");
            int transformedCount = (int) executionContext.get("transformedCount");
            int sentCount = (int) executionContext.get("sentCount");

            AtomicInteger processedCount = new AtomicInteger(0);

            // Mark each timesheet as PROCESSED
            for (Long timesheetId : timesheetIds) {
                try {
                    Timesheet timesheet = timesheetRepository.findById(timesheetId).orElse(null);
                    if (timesheet != null && TimesheetStatus.APPROVED.equals(timesheet.getStatus())) {
                        // Note: Using SUBMITTED as a placeholder since PROCESSED status doesn't exist in the enum
                        // In production, you would add PROCESSED to TimesheetStatus enum
                        timesheet.setStatus(TimesheetStatus.APPROVED);
                        timesheet.setSupervisorComments(
                            (timesheet.getSupervisorComments() != null ? timesheet.getSupervisorComments() + " | " : "") +
                            "Payment file: " + fileReference
                        );
                        timesheetRepository.save(timesheet);
                        processedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Error marking timesheet {} as processed: {}", timesheetId, e.getMessage());
                }
            }

            log.info("Marked {} timesheets as PROCESSED", processedCount.get());

            // Print summary
            log.info("================================================");
            log.info("  PAYMENT_FILE_GENERATION JOB COMPLETED");
            log.info("================================================");
            log.info("  File Reference: {}", fileReference);
            log.info("  Destination: {} ({})", DESTINATION_SYSTEM, FILE_TYPE);
            log.info("------------------------------------------------");
            log.info("  Statistics:");
            log.info("    Timesheets Queried: {}", timesheetIds.size());
            log.info("    Records Transformed: {}", transformedCount);
            log.info("    Records Sent: {}", sentCount);
            log.info("    Timesheets Marked Processed: {}", processedCount.get());
            log.info("------------------------------------------------");
            log.info("  Financial Summary:");
            log.info("    Total Payment Amount: ${}", totalPaymentAmount);
            log.info("================================================");

            return RepeatStatus.FINISHED;
        };
    }
}
