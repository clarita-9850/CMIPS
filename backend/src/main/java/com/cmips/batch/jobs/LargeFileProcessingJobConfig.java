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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LARGE FILE PROCESSING JOB - For Memory Stress Testing
 *
 * This job demonstrates how virtual threads handle large file I/O operations:
 *
 * 1. GENERATE STEP: Creates a large file (configurable: 100MB, 500MB, 1GB, 2GB)
 *    - Uses buffered writing to avoid loading entire file in memory
 *    - Virtual thread yields during disk I/O
 *
 * 2. PROCESS STEP: Reads and processes the file in chunks
 *    - Uses memory-mapped files or buffered streams
 *    - Demonstrates chunked processing (not loading entire file)
 *    - Computes checksum/statistics while reading
 *
 * 3. TRANSFORM STEP: Transforms data and writes to output file
 *    - Streaming transformation (read chunk -> transform -> write)
 *    - Shows how virtual threads handle concurrent I/O
 *
 * Job Parameters:
 * - fileSizeMB: Size of file to generate (default: 500)
 * - chunkSizeMB: Size of each processing chunk (default: 64)
 * - processMode: "stream" or "mmap" (default: stream)
 *
 * Memory Behavior:
 * - With streaming: Only chunkSizeMB is held in memory at a time
 * - With mmap: OS manages memory mapping, may use more RAM
 * - Multiple concurrent jobs will show how JVM handles pressure
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class LargeFileProcessingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionNotificationListener jobListener;
    private final StepProgressListener stepListener;

    private static final String WORK_DIR = "./data/large-files";
    private static final int DEFAULT_FILE_SIZE_MB = 500;
    private static final int DEFAULT_CHUNK_SIZE_MB = 64;

    @Bean
    public Job largeFileProcessingJob() {
        return new JobBuilder("largeFileProcessingJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)
            .start(generateLargeFileStep())
            .next(processLargeFileStep())
            .next(transformAndWriteStep())
            .next(cleanupLargeFilesStep())
            .build();
    }

    /**
     * Step 1: Generate a large file with random data
     */
    @Bean
    public Step generateLargeFileStep() {
        return new StepBuilder("generateLargeFileStep", jobRepository)
            .listener(stepListener)
            .tasklet(generateLargeFileTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet generateLargeFileTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            boolean isVirtual = Thread.currentThread().isVirtual();

            log.info("[Job {}] Starting large file generation on {} thread",
                jobId, isVirtual ? "VIRTUAL" : "PLATFORM");

            // Get parameters
            int fileSizeMB = Integer.parseInt(
                chunkContext.getStepContext().getJobParameters()
                    .getOrDefault("fileSizeMB", String.valueOf(DEFAULT_FILE_SIZE_MB)).toString());

            int chunkSizeMB = Integer.parseInt(
                chunkContext.getStepContext().getJobParameters()
                    .getOrDefault("chunkSizeMB", String.valueOf(DEFAULT_CHUNK_SIZE_MB)).toString());

            // Create work directory
            Path workDir = Paths.get(WORK_DIR);
            Files.createDirectories(workDir);

            // Generate unique filename for this job
            String inputFileName = String.format("input_job_%d_%d.dat", jobId, System.currentTimeMillis());
            Path inputPath = workDir.resolve(inputFileName);

            long fileSizeBytes = (long) fileSizeMB * 1024 * 1024;
            long chunkSizeBytes = (long) chunkSizeMB * 1024 * 1024;

            log.info("[Job {}] Generating {} MB file: {} (chunk size: {} MB)",
                jobId, fileSizeMB, inputPath, chunkSizeMB);

            // Store metrics
            long startTime = System.currentTimeMillis();
            long bytesWritten = 0;
            int chunksWritten = 0;

            // Generate file using buffered output stream (memory efficient)
            try (BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(inputPath.toFile()), (int) Math.min(chunkSizeBytes, 64 * 1024 * 1024))) {

                Random random = new Random(jobId); // Reproducible for testing
                byte[] buffer = new byte[(int) Math.min(chunkSizeBytes, 64 * 1024 * 1024)];

                while (bytesWritten < fileSizeBytes) {
                    // Generate random data
                    random.nextBytes(buffer);

                    int toWrite = (int) Math.min(buffer.length, fileSizeBytes - bytesWritten);
                    bos.write(buffer, 0, toWrite);
                    bytesWritten += toWrite;
                    chunksWritten++;

                    // Log progress every 100MB
                    if (bytesWritten % (100 * 1024 * 1024) == 0 || bytesWritten >= fileSizeBytes) {
                        long elapsedMs = System.currentTimeMillis() - startTime;
                        double mbWritten = bytesWritten / (1024.0 * 1024.0);
                        double mbps = elapsedMs > 0 ? (mbWritten * 1000 / elapsedMs) : 0;
                        log.info("[Job {}] Written {} MB / {} MB ({} MB/s) - Virtual: {}",
                            jobId, String.format("%.1f", mbWritten), fileSizeMB, String.format("%.1f", mbps), isVirtual);
                    }

                    // Yield point - virtual thread can switch here
                    if (chunksWritten % 10 == 0) {
                        Thread.yield();
                    }
                }
            }

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("[Job {}] File generation complete: {} MB in {} ms ({} MB/s)",
                jobId, fileSizeMB, elapsedMs, String.format("%.1f", fileSizeMB * 1000.0 / elapsedMs));

            // Store in execution context
            chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().put("inputFilePath", inputPath.toString());
            chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().put("fileSizeBytes", fileSizeBytes);
            chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().put("chunkSizeBytes", chunkSizeBytes);
            chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().put("totalSteps", 4);

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 2: Process the large file by reading in chunks
     */
    @Bean
    public Step processLargeFileStep() {
        return new StepBuilder("processLargeFileStep", jobRepository)
            .listener(stepListener)
            .tasklet(processLargeFileTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet processLargeFileTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            boolean isVirtual = Thread.currentThread().isVirtual();

            String inputFilePath = (String) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("inputFilePath");
            long fileSizeBytes = (long) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("fileSizeBytes");
            long chunkSizeBytes = (long) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("chunkSizeBytes");

            Path inputPath = Paths.get(inputFilePath);

            log.info("[Job {}] Processing file: {} ({} MB) on {} thread",
                jobId, inputPath.getFileName(), fileSizeBytes / (1024 * 1024),
                isVirtual ? "VIRTUAL" : "PLATFORM");

            long startTime = System.currentTimeMillis();
            long bytesRead = 0;
            int chunksProcessed = 0;
            long lineCount = 0;
            long wordCount = 0;

            // Process using buffered input stream (memory efficient)
            try (BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(inputPath.toFile()), (int) Math.min(chunkSizeBytes, 64 * 1024 * 1024))) {

                byte[] buffer = new byte[(int) Math.min(chunkSizeBytes, 64 * 1024 * 1024)];
                MessageDigest md = MessageDigest.getInstance("MD5");

                int read;
                while ((read = bis.read(buffer)) != -1) {
                    // Update checksum
                    md.update(buffer, 0, read);

                    // Simulate processing - count newlines and spaces
                    for (int i = 0; i < read; i++) {
                        if (buffer[i] == '\n') lineCount++;
                        if (buffer[i] == ' ') wordCount++;
                    }

                    bytesRead += read;
                    chunksProcessed++;

                    // Log progress every 100MB
                    if (bytesRead % (100 * 1024 * 1024) == 0) {
                        long elapsedMs = System.currentTimeMillis() - startTime;
                        double mbRead = bytesRead / (1024.0 * 1024.0);
                        double mbps = elapsedMs > 0 ? (mbRead * 1000 / elapsedMs) : 0;
                        double progress = (bytesRead * 100.0) / fileSizeBytes;
                        log.info("[Job {}] Processed {}% ({} MB/s) - Virtual: {}",
                            jobId, String.format("%.1f", progress), String.format("%.1f", mbps), isVirtual);
                    }

                    // Yield point for virtual thread
                    if (chunksProcessed % 5 == 0) {
                        Thread.yield();
                    }
                }

                // Store checksum
                byte[] digest = md.digest();
                StringBuilder checksum = new StringBuilder();
                for (byte b : digest) {
                    checksum.append(String.format("%02x", b));
                }

                chunkContext.getStepContext().getStepExecution().getJobExecution()
                    .getExecutionContext().put("checksum", checksum.toString());
            }

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("[Job {}] Processing complete: {} chunks in {} ms ({} MB/s)",
                jobId, chunksProcessed, elapsedMs,
                String.format("%.1f", (bytesRead / (1024.0 * 1024.0)) * 1000 / elapsedMs));

            chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().put("chunksProcessed", chunksProcessed);

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 3: Transform and write output file
     */
    @Bean
    public Step transformAndWriteStep() {
        return new StepBuilder("transformAndWriteStep", jobRepository)
            .listener(stepListener)
            .tasklet(transformAndWriteTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet transformAndWriteTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            boolean isVirtual = Thread.currentThread().isVirtual();

            String inputFilePath = (String) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("inputFilePath");
            long chunkSizeBytes = (long) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("chunkSizeBytes");

            Path inputPath = Paths.get(inputFilePath);
            Path outputPath = inputPath.resolveSibling(
                inputPath.getFileName().toString().replace("input_", "output_"));

            log.info("[Job {}] Transforming file to: {} on {} thread",
                jobId, outputPath.getFileName(), isVirtual ? "VIRTUAL" : "PLATFORM");

            long startTime = System.currentTimeMillis();
            long bytesTransformed = 0;

            // Stream transform: read -> transform -> write (no full file in memory)
            try (BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(inputPath.toFile()), (int) Math.min(chunkSizeBytes, 64 * 1024 * 1024));
                 BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(outputPath.toFile()), (int) Math.min(chunkSizeBytes, 64 * 1024 * 1024))) {

                byte[] buffer = new byte[(int) Math.min(chunkSizeBytes, 64 * 1024 * 1024)];
                int read;
                int chunks = 0;

                while ((read = bis.read(buffer)) != -1) {
                    // Simple transformation: XOR each byte (simulates processing)
                    for (int i = 0; i < read; i++) {
                        buffer[i] = (byte) (buffer[i] ^ 0xFF);
                    }

                    bos.write(buffer, 0, read);
                    bytesTransformed += read;
                    chunks++;

                    // Log progress every 100MB
                    if (bytesTransformed % (100 * 1024 * 1024) == 0) {
                        log.info("[Job {}] Transformed {} MB - Virtual: {}",
                            jobId, bytesTransformed / (1024 * 1024), isVirtual);
                    }

                    // Yield point
                    if (chunks % 5 == 0) {
                        Thread.yield();
                    }
                }
            }

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("[Job {}] Transform complete: {} MB in {} ms ({} MB/s)",
                jobId, bytesTransformed / (1024 * 1024), elapsedMs,
                String.format("%.1f", (bytesTransformed / (1024.0 * 1024.0)) * 1000 / elapsedMs));

            chunkContext.getStepContext().getStepExecution().getJobExecution()
                .getExecutionContext().put("outputFilePath", outputPath.toString());

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * Step 4: Cleanup - delete temporary files
     */
    @Bean
    public Step cleanupLargeFilesStep() {
        return new StepBuilder("cleanupLargeFilesStep", jobRepository)
            .listener(stepListener)
            .tasklet(cleanupLargeFilesTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet cleanupLargeFilesTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();

            String inputFilePath = (String) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("inputFilePath");
            String outputFilePath = (String) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("outputFilePath");
            String checksum = (String) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("checksum");
            int chunksProcessed = (int) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("chunksProcessed");
            long fileSizeBytes = (long) chunkContext.getStepContext()
                .getStepExecution().getJobExecution().getExecutionContext().get("fileSizeBytes");

            // Delete files to free disk space
            try {
                Files.deleteIfExists(Paths.get(inputFilePath));
                Files.deleteIfExists(Paths.get(outputFilePath));
                log.info("[Job {}] Cleaned up temporary files", jobId);
            } catch (Exception e) {
                log.warn("[Job {}] Failed to cleanup files: {}", jobId, e.getMessage());
            }

            log.info("================================================================");
            log.info("  LARGE FILE PROCESSING JOB {} COMPLETED", jobId);
            log.info("  File Size:       {} MB", fileSizeBytes / (1024 * 1024));
            log.info("  Chunks Processed: {}", chunksProcessed);
            log.info("  Checksum:        {}", checksum);
            log.info("  Virtual Thread:  {}", Thread.currentThread().isVirtual());
            log.info("================================================================");

            return RepeatStatus.FINISHED;
        };
    }
}
