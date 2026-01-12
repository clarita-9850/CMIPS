package com.cmips.batch.jobs;

import com.cmips.batch.JobExecutionNotificationListener;
import com.cmips.batch.StepProgressListener;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Batch Job Configuration for COUNTY_DAILY_REPORT.
 *
 * This job demonstrates:
 * 1. Multi-step job execution
 * 2. Virtual thread integration
 * 3. Progress tracking via Redis events
 * 4. Parallel data processing (optional)
 *
 * Job Flow:
 * ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
 * │ Initialize  │───▶│  Generate   │───▶│   Write     │───▶│   Cleanup   │
 * │    Step     │    │  Data Step  │    │ Report Step │    │    Step     │
 * └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
 *
 * Virtual Thread Benefits for this Job:
 * - Initialize: File I/O operations yield to other threads
 * - Generate Data: Database queries yield during I/O wait
 * - Write Report: File writes yield to other threads
 * - Multiple instances can run concurrently without blocking
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CountyDailyReportJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionNotificationListener jobListener;
    private final StepProgressListener stepListener;

    @Qualifier("chunkTaskExecutor")
    private final TaskExecutor chunkTaskExecutor;

    private static final String REPORT_OUTPUT_DIR = "./reports/county-daily";

    /**
     * Define the COUNTY_DAILY_REPORT job.
     * The bean name "countyDailyReportJob" is used by the trigger service.
     */
    @Bean
    public Job countyDailyReportJob() {
        return new JobBuilder("countyDailyReportJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)
            .start(initializeStep())
            .next(generateDataStep())
            .next(writeReportStep())
            .next(cleanupStep())
            .build();
    }

    /**
     * Step 1: Initialize report parameters.
     *
     * This step runs on a virtual thread. While waiting for
     * file system operations, it yields to allow other tasks to run.
     */
    @Bean
    public Step initializeStep() {
        return new StepBuilder("initializeStep", jobRepository)
            .listener(stepListener)
            .tasklet(initializeTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet initializeTasklet() {
        return (contribution, chunkContext) -> {
            String threadInfo = Thread.currentThread().toString();
            log.info("Initializing COUNTY_DAILY_REPORT on thread: {}", threadInfo);
            log.info("Is Virtual Thread: {}", Thread.currentThread().isVirtual());

            // Get parameters from job context
            String reportDate = chunkContext.getStepContext()
                .getJobParameters()
                .getOrDefault("reportDate", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
                .toString();

            String countyCode = chunkContext.getStepContext()
                .getJobParameters()
                .getOrDefault("countyCode", "ALL")
                .toString();

            // Store in execution context for later steps
            chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("reportDate", reportDate);

            chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("countyCode", countyCode);

            // Set total steps for progress tracking
            chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("totalSteps", 4);

            log.info("Report Date: {}, County Code: {}", reportDate, countyCode);

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 2: Generate report data.
     *
     * Simulates database queries and data processing.
     * Virtual threads excel here because:
     * - Database queries involve I/O wait
     * - During wait, thread yields to process other tasks
     * - Much better resource utilization than blocking threads
     */
    @Bean
    public Step generateDataStep() {
        return new StepBuilder("generateDataStep", jobRepository)
            .listener(stepListener)
            .tasklet(generateDataTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet generateDataTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Generating report data on virtual thread: {}", Thread.currentThread().isVirtual());

            // Simulate data generation (would normally query database)
            Random random = new Random();
            int recordCount = 100 + random.nextInt(500);

            // Simulate I/O-bound database queries
            // Virtual thread will yield during this sleep, allowing other work
            Thread.sleep(2000);

            // Store generated data in execution context
            StringBuilder csvData = new StringBuilder();
            csvData.append("county_code,report_date,total_cases,active_cases,closed_cases,pending_cases\n");

            String[] countyCodes = {"LA", "SF", "SD", "SJ", "OC", "SAC", "ALA", "CON"};
            String reportDate = (String) chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .get("reportDate");

            // Simulate parallel data generation using virtual threads
            AtomicInteger processedCount = new AtomicInteger(0);

            for (int i = 0; i < recordCount; i++) {
                String county = countyCodes[i % countyCodes.length];
                int total = 100 + random.nextInt(1000);
                int active = random.nextInt(total);
                int closed = random.nextInt(total - active);
                int pending = total - active - closed;

                csvData.append(String.format("%s,%s,%d,%d,%d,%d%n",
                    county, reportDate, total, active, closed, pending));

                processedCount.incrementAndGet();

                // Log progress every 100 records
                if (processedCount.get() % 100 == 0) {
                    log.debug("Generated {} records...", processedCount.get());
                }
            }

            chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("csvData", csvData.toString());

            chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("recordCount", recordCount);

            log.info("Generated {} records", recordCount);

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 3: Write the report file.
     *
     * File I/O is another area where virtual threads shine.
     * While waiting for disk writes, the thread yields.
     */
    @Bean
    public Step writeReportStep() {
        return new StepBuilder("writeReportStep", jobRepository)
            .listener(stepListener)
            .tasklet(writeReportTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet writeReportTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Writing report file on virtual thread: {}", Thread.currentThread().isVirtual());

            // Get data from execution context
            String csvData = (String) chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .get("csvData");

            String reportDate = (String) chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .get("reportDate");

            // Create output directory if needed
            Path outputDir = Paths.get(REPORT_OUTPUT_DIR);
            Files.createDirectories(outputDir);

            // Generate filename
            String filename = String.format("county_daily_report_%s_%d.csv",
                reportDate,
                System.currentTimeMillis());
            Path outputPath = outputDir.resolve(filename);

            // Write file - virtual thread yields during disk I/O
            Files.writeString(outputPath, csvData);

            // Store output path for final step
            chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("outputFilePath", outputPath.toString());

            log.info("Report written to: {}", outputPath);

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 4: Cleanup and finalization.
     */
    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
            .listener(stepListener)
            .tasklet(cleanupTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet cleanupTasklet() {
        return (contribution, chunkContext) -> {
            log.info("Performing cleanup on virtual thread: {}", Thread.currentThread().isVirtual());

            // Log final summary
            String outputPath = (String) chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .get("outputFilePath");

            Integer recordCount = (Integer) chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .get("recordCount");

            log.info("=================================================");
            log.info("  COUNTY_DAILY_REPORT COMPLETED");
            log.info("  Records Generated: {}", recordCount);
            log.info("  Output File: {}", outputPath);
            log.info("  Executed on Virtual Thread: {}", Thread.currentThread().isVirtual());
            log.info("=================================================");

            return RepeatStatus.FINISHED;
        };
    }
}
