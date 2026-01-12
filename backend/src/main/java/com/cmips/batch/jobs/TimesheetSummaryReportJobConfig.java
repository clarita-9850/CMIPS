package com.cmips.batch.jobs;

import com.cmips.batch.JobExecutionNotificationListener;
import com.cmips.batch.StepProgressListener;
import com.cmips.entity.Timesheet;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring Batch Job Configuration for TIMESHEET_SUMMARY_REPORT.
 *
 * This job queries REAL DATA from the timesheets table and generates
 * a summary report grouped by department and status.
 *
 * Job Flow:
 * ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
 * │   Validate      │───▶│   Query         │───▶│   Aggregate     │───▶│   Generate      │───▶│   Notify        │
 * │   Parameters    │    │   Database      │    │   Data          │    │   Report        │    │   Complete      │
 * └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
 *
 * Example Output CSV:
 * department,status,count,total_regular_hours,total_overtime_hours,total_hours,avg_hours_per_timesheet
 * Engineering,APPROVED,45,1800.00,120.50,1920.50,42.68
 * HR,SUBMITTED,12,480.00,0.00,480.00,40.00
 * ...
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TimesheetSummaryReportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionNotificationListener jobListener;
    private final StepProgressListener stepListener;
    private final TimesheetRepository timesheetRepository;

    @Qualifier("chunkTaskExecutor")
    private final TaskExecutor chunkTaskExecutor;

    private static final String REPORT_OUTPUT_DIR = "./reports/timesheet-summary";

    // ==========================================
    // JOB DEFINITION - This defines the steps!
    // ==========================================

    /**
     * Define the TIMESHEET_SUMMARY_REPORT job.
     *
     * THIS IS WHERE YOU DEFINE WHAT STEPS THE JOB HAS!
     *
     * You can chain steps with:
     * - .start(step1)           -> First step
     * - .next(step2)            -> Sequential next step
     * - .on("COMPLETED").to(step3)  -> Conditional flow
     * - .split(executor).add(flow1, flow2)  -> Parallel steps
     */
    @Bean
    public Job timesheetSummaryReportJob() {
        return new JobBuilder("timesheetSummaryReportJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)  // Publishes Redis events
            // Define the step sequence:
            .start(validateParametersStep())
            .next(queryDatabaseStep())
            .next(aggregateDataStep())
            .next(generateReportStep())
            .next(notifyCompleteStep())
            .build();
    }

    // ==========================================
    // STEP 1: Validate Parameters
    // ==========================================

    @Bean
    public Step validateParametersStep() {
        return new StepBuilder("validateParametersStep", jobRepository)
            .listener(stepListener)
            .tasklet(validateParametersTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet validateParametersTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 1: Validating Parameters ===");

            // Get parameters from job
            Map<String, Object> params = chunkContext.getStepContext().getJobParameters();

            String startDate = params.getOrDefault("startDate",
                LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_DATE)).toString();
            String endDate = params.getOrDefault("endDate",
                LocalDate.now().format(DateTimeFormatter.ISO_DATE)).toString();
            String department = params.getOrDefault("department", "ALL").toString();
            String status = params.getOrDefault("status", "ALL").toString();

            // Validate dates
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (start.isAfter(end)) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }

            // Store validated params in execution context for next steps
            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            executionContext.put("startDate", startDate);
            executionContext.put("endDate", endDate);
            executionContext.put("department", department);
            executionContext.put("status", status);
            executionContext.put("totalSteps", 5);

            log.info("Parameters validated: startDate={}, endDate={}, department={}, status={}",
                startDate, endDate, department, status);

            return RepeatStatus.FINISHED;
        };
    }

    // ==========================================
    // STEP 2: Query Database (REAL DATA!)
    // ==========================================

    @Bean
    public Step queryDatabaseStep() {
        return new StepBuilder("queryDatabaseStep", jobRepository)
            .listener(stepListener)
            .tasklet(queryDatabaseTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet queryDatabaseTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 2: Querying Database ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            String startDate = (String) executionContext.get("startDate");
            String endDate = (String) executionContext.get("endDate");
            String department = (String) executionContext.get("department");
            String status = (String) executionContext.get("status");

            // Query REAL data from database
            List<Timesheet> timesheets;

            if ("ALL".equals(department) && "ALL".equals(status)) {
                timesheets = timesheetRepository.findByPayPeriodStartBetween(
                    LocalDate.parse(startDate), LocalDate.parse(endDate));
            } else if ("ALL".equals(department)) {
                timesheets = timesheetRepository.findByStatusStringAndPayPeriodStartBetween(
                    status, LocalDate.parse(startDate), LocalDate.parse(endDate));
            } else if ("ALL".equals(status)) {
                timesheets = timesheetRepository.findByDepartmentAndPayPeriodStartBetween(
                    department, LocalDate.parse(startDate), LocalDate.parse(endDate));
            } else {
                timesheets = timesheetRepository.findByDepartmentAndStatusStringAndPayPeriodStartBetween(
                    department, status, LocalDate.parse(startDate), LocalDate.parse(endDate));
            }

            log.info("Retrieved {} timesheets from database", timesheets.size());

            // Aggregate data immediately (don't store entities in context - they're not serializable)
            Map<String, Map<String, DepartmentStatusSummary>> aggregated = timesheets.stream()
                .collect(Collectors.groupingBy(
                    Timesheet::getDepartment,
                    Collectors.groupingBy(
                        t -> t.getStatus().name(),
                        Collectors.collectingAndThen(
                            Collectors.toList(),
                            this::calculateSummary
                        )
                    )
                ));

            // Store only serializable data
            executionContext.put("timesheetCount", timesheets.size());

            // Convert aggregated data to serializable CSV format
            StringBuilder csvData = new StringBuilder();
            csvData.append("department,status,count,total_regular_hours,total_overtime_hours,total_hours,avg_hours_per_timesheet\n");

            int rowCount = 0;
            for (Map.Entry<String, Map<String, DepartmentStatusSummary>> deptEntry : aggregated.entrySet()) {
                String dept = deptEntry.getKey();
                for (Map.Entry<String, DepartmentStatusSummary> statusEntry : deptEntry.getValue().entrySet()) {
                    String stat = statusEntry.getKey();
                    DepartmentStatusSummary summary = statusEntry.getValue();
                    csvData.append(String.format("%s,%s,%d,%.2f,%.2f,%.2f,%.2f%n",
                        dept, stat, summary.count(),
                        summary.totalRegularHours(), summary.totalOvertimeHours(),
                        summary.totalHours(), summary.avgHoursPerTimesheet()));
                    rowCount++;
                }
            }

            executionContext.put("csvData", csvData.toString());
            executionContext.put("reportRowCount", rowCount);
            executionContext.put("departmentCount", aggregated.size());

            log.info("Aggregated data into {} department groups with {} rows", aggregated.size(), rowCount);

            return RepeatStatus.FINISHED;
        };
    }

    // ==========================================
    // STEP 3: Aggregate Data
    // ==========================================

    @Bean
    public Step aggregateDataStep() {
        return new StepBuilder("aggregateDataStep", jobRepository)
            .listener(stepListener)
            .tasklet(aggregateDataTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet aggregateDataTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 3: Verifying Aggregation ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            int departmentCount = (int) executionContext.get("departmentCount");
            int rowCount = (int) executionContext.get("reportRowCount");

            log.info("Verified {} department groups with {} data rows", departmentCount, rowCount);

            return RepeatStatus.FINISHED;
        };
    }

    private DepartmentStatusSummary calculateSummary(List<Timesheet> timesheets) {
        int count = timesheets.size();
        BigDecimal totalRegular = timesheets.stream()
            .map(t -> t.getRegularHours() != null ? t.getRegularHours() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOvertime = timesheets.stream()
            .map(t -> t.getOvertimeHours() != null ? t.getOvertimeHours() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHours = timesheets.stream()
            .map(t -> t.getTotalHours() != null ? t.getTotalHours() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgHours = count > 0
            ? totalHours.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new DepartmentStatusSummary(count, totalRegular, totalOvertime, totalHours, avgHours);
    }

    // Simple record to hold summary data
    private record DepartmentStatusSummary(
        int count,
        BigDecimal totalRegularHours,
        BigDecimal totalOvertimeHours,
        BigDecimal totalHours,
        BigDecimal avgHoursPerTimesheet
    ) {}

    // ==========================================
    // STEP 4: Generate Report File
    // ==========================================

    @Bean
    public Step generateReportStep() {
        return new StepBuilder("generateReportStep", jobRepository)
            .listener(stepListener)
            .tasklet(generateReportTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet generateReportTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 4: Generating Report ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            String csvData = (String) executionContext.get("csvData");
            String startDate = (String) executionContext.get("startDate");
            String endDate = (String) executionContext.get("endDate");
            int rowCount = (int) executionContext.get("reportRowCount");

            // Write to file
            Path outputDir = Paths.get(REPORT_OUTPUT_DIR);
            Files.createDirectories(outputDir);

            String filename = String.format("timesheet_summary_%s_to_%s_%d.csv",
                startDate, endDate, System.currentTimeMillis());
            Path outputPath = outputDir.resolve(filename);

            Files.writeString(outputPath, csvData);

            executionContext.put("outputFilePath", outputPath.toString());

            log.info("Report written to: {} ({} rows)", outputPath, rowCount);

            return RepeatStatus.FINISHED;
        };
    }

    // ==========================================
    // STEP 5: Notify Completion
    // ==========================================

    @Bean
    public Step notifyCompleteStep() {
        return new StepBuilder("notifyCompleteStep", jobRepository)
            .listener(stepListener)
            .tasklet(notifyCompleteTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet notifyCompleteTasklet() {
        return (contribution, chunkContext) -> {
            log.info("=== STEP 5: Notifying Completion ===");

            var executionContext = chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext();

            String outputPath = (String) executionContext.get("outputFilePath");
            int timesheetCount = (int) executionContext.get("timesheetCount");
            int reportRowCount = (int) executionContext.get("reportRowCount");

            log.info("================================================");
            log.info("  TIMESHEET_SUMMARY_REPORT COMPLETED");
            log.info("  Timesheets Processed: {}", timesheetCount);
            log.info("  Report Rows Generated: {}", reportRowCount);
            log.info("  Output File: {}", outputPath);
            log.info("================================================");

            // Here you could:
            // - Send email notification
            // - Upload to SFTP
            // - Trigger downstream jobs
            // - Update a status table

            return RepeatStatus.FINISHED;
        };
    }
}
