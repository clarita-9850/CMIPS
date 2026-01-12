package com.cmips.batch.jobs;

import com.cmips.batch.JobExecutionNotificationListener;
import com.cmips.batch.StepProgressListener;
import com.cmips.entity.BatchAggregationEntity;
import com.cmips.repository.BatchAggregationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * COMPUTE-INTENSIVE FILE PROCESSING JOB - For Memory Stress Testing
 *
 * This job combines HEAVY I/O with HEAVY COMPUTE to stress test memory:
 *
 * STEPS:
 * 1. GENERATE: Create JSON data file with complex nested objects
 * 2. PARSE & AGGREGATE: Parse JSON, hold aggregations in memory
 * 3. ENCRYPT: AES-256-GCM encryption (CPU + memory intensive)
 * 4. COMPRESS: GZIP compression (CPU intensive)
 * 5. DECOMPRESS & VERIFY: Verify data integrity
 * 6. CLEANUP: Delete temp files
 *
 * JOB PARAMETERS:
 * - recordCount: Number of JSON records to generate (default: 100000)
 * - recordSizeKB: Size of each record in KB (default: 10)
 * - holdInMemory: Whether to hold parsed data in memory (default: true)
 * - streamToDb: Whether to stream aggregations to DB instead of memory (default: false)
 * - aggregationDepth: Depth of aggregation operations (default: 3)
 * - encryptionPasses: Number of encryption passes (default: 1)
 *
 * MEMORY BEHAVIOR:
 * - recordCount * recordSizeKB = Raw data size
 * - JSON parsing creates ~3x memory overhead (strings, objects)
 * - Aggregations hold Maps/Lists in memory
 * - Multiple passes multiply memory usage
 *
 * TO CAUSE OOM:
 * - Increase recordCount and recordSizeKB
 * - Set holdInMemory=true
 * - Increase aggregationDepth
 * - Run multiple concurrent jobs
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ComputeIntensiveFileJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobExecutionNotificationListener jobListener;
    private final StepProgressListener stepListener;
    private final BatchAggregationRepository aggregationRepository;

    private static final String WORK_DIR = "./data/compute-intensive";
    private static final int DEFAULT_RECORD_COUNT = 100000;
    private static final int DEFAULT_RECORD_SIZE_KB = 10;
    private static final int DEFAULT_AGGREGATION_DEPTH = 3;
    private static final int DEFAULT_ENCRYPTION_PASSES = 1;

    @org.springframework.beans.factory.annotation.Value("${batch.streaming.flush-size:5000}")
    private int batchFlushSize;

    @Bean
    public Job computeIntensiveFileJob() {
        return new JobBuilder("computeIntensiveFileJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(jobListener)
            .start(generateJsonDataStep())
            .next(parseAndAggregateStep())
            .next(encryptDataStep())
            .next(compressDataStep())
            .next(decompressAndVerifyStep())
            .next(cleanupComputeFilesStep())
            .build();
    }

    // ==================== STEP 1: Generate JSON Data ====================

    @Bean
    public Step generateJsonDataStep() {
        return new StepBuilder("generateJsonDataStep", jobRepository)
            .listener(stepListener)
            .tasklet(generateJsonDataTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet generateJsonDataTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            boolean isVirtual = Thread.currentThread().isVirtual();

            // Get parameters
            int recordCount = getIntParam(chunkContext, "recordCount", DEFAULT_RECORD_COUNT);
            int recordSizeKB = getIntParam(chunkContext, "recordSizeKB", DEFAULT_RECORD_SIZE_KB);

            log.info("[Job {}] === STEP 1: GENERATE JSON DATA ===", jobId);
            log.info("[Job {}] Records: {}, Size per record: {} KB, Total: ~{} MB, Virtual: {}",
                jobId, recordCount, recordSizeKB, (recordCount * recordSizeKB) / 1024, isVirtual);

            // Create work directory
            Path workDir = Paths.get(WORK_DIR);
            Files.createDirectories(workDir);

            String dataFileName = String.format("data_job_%d_%d.json", jobId, System.currentTimeMillis());
            Path dataPath = workDir.resolve(dataFileName);

            long startTime = System.currentTimeMillis();
            long bytesWritten = 0;
            Random random = new Random(jobId);
            ObjectMapper mapper = new ObjectMapper();

            // Generate JSON records
            try (BufferedWriter writer = Files.newBufferedWriter(dataPath, StandardCharsets.UTF_8)) {
                writer.write("[\n");

                for (int i = 0; i < recordCount; i++) {
                    Map<String, Object> record = generateComplexRecord(i, recordSizeKB, random);
                    String json = mapper.writeValueAsString(record);

                    if (i > 0) writer.write(",\n");
                    writer.write(json);
                    bytesWritten += json.length();

                    // Log progress every 10%
                    if ((i + 1) % (recordCount / 10) == 0) {
                        double progress = ((i + 1) * 100.0) / recordCount;
                        long elapsedMs = System.currentTimeMillis() - startTime;
                        double mbWritten = bytesWritten / (1024.0 * 1024.0);
                        log.info("[Job {}] Generated {}% ({} MB, {} records/sec)",
                            jobId, String.format("%.0f", progress), String.format("%.1f", mbWritten),
                            elapsedMs > 0 ? (i + 1) * 1000 / elapsedMs : 0);
                    }
                }

                writer.write("\n]");
            }

            long elapsedMs = System.currentTimeMillis() - startTime;
            long fileSizeBytes = Files.size(dataPath);

            log.info("[Job {}] Generated {} MB in {} ms ({} MB/s)",
                jobId, fileSizeBytes / (1024 * 1024), elapsedMs,
                String.format("%.1f", (fileSizeBytes / (1024.0 * 1024.0)) * 1000 / elapsedMs));

            // Store in context
            storeInContext(chunkContext, "dataFilePath", dataPath.toString());
            storeInContext(chunkContext, "recordCount", recordCount);
            storeInContext(chunkContext, "fileSizeBytes", fileSizeBytes);

            logMemoryUsage(jobId, "After Generate");

            return RepeatStatus.FINISHED;
        };
    }

    // ==================== STEP 2: Parse & Aggregate (Memory Intensive) ====================

    @Bean
    public Step parseAndAggregateStep() {
        return new StepBuilder("parseAndAggregateStep", jobRepository)
            .listener(stepListener)
            .tasklet(parseAndAggregateTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet parseAndAggregateTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            boolean isVirtual = Thread.currentThread().isVirtual();
            boolean holdInMemory = getBooleanParam(chunkContext, "holdInMemory", true);
            int aggregationDepth = getIntParam(chunkContext, "aggregationDepth", DEFAULT_AGGREGATION_DEPTH);
            boolean useStreamingMode = getBooleanParam(chunkContext, "streamToDb", false);

            log.info("[Job {}] === STEP 2: PARSE & AGGREGATE ===", jobId);
            log.info("[Job {}] Hold in memory: {}, Streaming to DB: {}, Aggregation depth: {}, Virtual: {}",
                jobId, holdInMemory, useStreamingMode, aggregationDepth, isVirtual);

            String dataFilePath = getFromContext(chunkContext, "dataFilePath");
            Path dataPath = Paths.get(dataFilePath);

            long startTime = System.currentTimeMillis();
            ObjectMapper mapper = new ObjectMapper();

            // STREAMING MODE: Write aggregations directly to DB instead of memory
            if (useStreamingMode) {
                log.info("[Job {}] Using STREAMING MODE - aggregations written directly to DB (batchFlushSize: {})", jobId, batchFlushSize);
                int recordsParsed = parseAndStreamToDb(jobId, dataPath, mapper, aggregationDepth);

                long elapsedMs = System.currentTimeMillis() - startTime;

                // Get stats from DB
                Long totalRecords = aggregationRepository.getTotalRecordCount(jobId);
                long departmentCount = aggregationRepository.countDistinctGroupsByType(jobId, "BY_DEPARTMENT");
                long regionCount = aggregationRepository.countDistinctGroupsByType(jobId, "BY_REGION");

                log.info("[Job {}] STREAMING Parse & Aggregate complete in {} ms", jobId, elapsedMs);
                log.info("[Job {}] Records processed: {}, Departments: {}, Regions: {}",
                    jobId, totalRecords, departmentCount, regionCount);

                // Store aggregation results
                storeInContext(chunkContext, "departmentCount", (int) departmentCount);
                storeInContext(chunkContext, "aggregationCount", (int) (departmentCount + regionCount));
                storeInContext(chunkContext, "recordsInMemory", 0); // No records in memory!

                logMemoryUsage(jobId, "After Streaming Parse & Aggregate");

                return RepeatStatus.FINISHED;
            }

            // LEGACY MODE: In-memory processing (kept for backwards compatibility)
            List<Map<String, Object>> allRecords = new ArrayList<>();
            Map<String, List<Map<String, Object>>> byDepartment = new ConcurrentHashMap<>();
            Map<String, Map<String, Double>> aggregations = new ConcurrentHashMap<>();
            Map<String, Set<String>> uniqueValues = new ConcurrentHashMap<>();
            List<Map<String, Object>> sortedRecords = new ArrayList<>();

            // Parse JSON file
            log.info("[Job {}] Parsing JSON file (legacy in-memory mode)...", jobId);
            try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8)) {
                String line;
                StringBuilder recordBuilder = new StringBuilder();
                int recordsParsed = 0;
                int braceCount = 0;
                boolean inRecord = false;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.equals("[") || line.equals("]")) continue;

                    if (line.endsWith(",")) {
                        line = line.substring(0, line.length() - 1);
                    }

                    for (char c : line.toCharArray()) {
                        if (c == '{') {
                            if (!inRecord) inRecord = true;
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                        }
                    }

                    recordBuilder.append(line);

                    if (inRecord && braceCount == 0) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> record = mapper.readValue(
                                recordBuilder.toString(), Map.class);

                            if (holdInMemory) {
                                allRecords.add(record);
                            }

                            // Only store in byDepartment if holdInMemory is true
                            // FIX: Previously this stored ALL records regardless of holdInMemory
                            performAggregationsLegacy(record, holdInMemory ? byDepartment : null,
                                aggregations, uniqueValues, aggregationDepth);

                            recordsParsed++;

                            if (recordsParsed % 10000 == 0) {
                                logMemoryUsage(jobId, "Parsing progress: " + recordsParsed + " records");
                            }
                        } catch (Exception e) {
                            // Skip malformed records
                        }

                        recordBuilder = new StringBuilder();
                        inRecord = false;
                    }
                }

                log.info("[Job {}] Parsed {} records", jobId, recordsParsed);
            }

            if (holdInMemory && !allRecords.isEmpty()) {
                log.info("[Job {}] Performing sorting operations...", jobId);

                sortedRecords = allRecords.stream()
                    .sorted((a, b) -> {
                        String deptA = (String) a.getOrDefault("department", "");
                        String deptB = (String) b.getOrDefault("department", "");
                        return deptA.compareTo(deptB);
                    })
                    .collect(Collectors.toList());

                if (aggregationDepth >= 3) {
                    log.info("[Job {}] Creating deep copy (aggregationDepth >= 3)...", jobId);
                    List<Map<String, Object>> deepCopy = new ArrayList<>();
                    for (Map<String, Object> record : allRecords) {
                        deepCopy.add(new HashMap<>(record));
                    }
                    log.info("[Job {}] Deep copy created with {} records", jobId, deepCopy.size());
                }
            }

            long elapsedMs = System.currentTimeMillis() - startTime;

            log.info("[Job {}] Parse & Aggregate complete in {} ms", jobId, elapsedMs);
            log.info("[Job {}] Departments: {}, Aggregations: {}, Unique value sets: {}",
                jobId, byDepartment.size(), aggregations.size(), uniqueValues.size());

            if (holdInMemory) {
                log.info("[Job {}] Records in memory: {}, Sorted records: {}",
                    jobId, allRecords.size(), sortedRecords.size());
            }

            storeInContext(chunkContext, "departmentCount", byDepartment.size());
            storeInContext(chunkContext, "aggregationCount", aggregations.size());
            storeInContext(chunkContext, "recordsInMemory", allRecords.size());

            logMemoryUsage(jobId, "After Parse & Aggregate");

            System.gc();
            Thread.sleep(100);
            logMemoryUsage(jobId, "After GC");

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * STREAMING MODE: Parse JSON and write aggregations to database using batched inserts.
     * This keeps memory usage bounded while dramatically improving performance.
     *
     * Strategy: Aggregate in-memory by group key, flush to DB every batchFlushSize records.
     * This reduces DB calls from N (one per record) to N/batchFlushSize.
     * batchFlushSize is configured via application.properties: batch.streaming.flush-size
     */
    private int parseAndStreamToDb(long jobId, Path dataPath, ObjectMapper mapper, int aggregationDepth)
            throws IOException {
        int recordsParsed = 0;

        // In-memory aggregation buffers - key is "aggregationType:groupKey"
        Map<String, AggregationBuffer> buffers = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8)) {
            String line;
            StringBuilder recordBuilder = new StringBuilder();
            int braceCount = 0;
            boolean inRecord = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.equals("[") || line.equals("]")) continue;

                if (line.endsWith(",")) {
                    line = line.substring(0, line.length() - 1);
                }

                for (char c : line.toCharArray()) {
                    if (c == '{') {
                        if (!inRecord) inRecord = true;
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                    }
                }

                recordBuilder.append(line);

                if (inRecord && braceCount == 0) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> record = mapper.readValue(
                            recordBuilder.toString(), Map.class);

                        // Buffer aggregation in memory instead of immediate DB write
                        bufferAggregation(buffers, record, aggregationDepth);

                        recordsParsed++;

                        // Flush buffer to DB every batchFlushSize records
                        if (recordsParsed % batchFlushSize == 0) {
                            flushBuffersToDb(jobId, buffers);
                            buffers.clear(); // Clear buffers after flush
                            logMemoryUsage(jobId, "Batched streaming progress: " + recordsParsed + " records (flush every " + batchFlushSize + ")");
                        }
                    } catch (Exception e) {
                        // Skip malformed records
                    }

                    // Clear the record builder - record is now out of scope and eligible for GC
                    recordBuilder = new StringBuilder();
                    inRecord = false;
                }
            }
        }

        // Flush any remaining buffered aggregations
        if (!buffers.isEmpty()) {
            flushBuffersToDb(jobId, buffers);
        }

        log.info("[Job {}] Batched stream completed - {} records processed", jobId, recordsParsed);
        return recordsParsed;
    }

    /**
     * Helper class to buffer aggregation data in memory before batch flush.
     */
    private static class AggregationBuffer {
        String aggregationType;
        String groupKey;
        int recordCount = 0;
        double totalSalary = 0;
        double totalHours = 0;
        double totalBonus = 0;
        double minSalary = Double.MAX_VALUE;
        double maxSalary = Double.MIN_VALUE;

        AggregationBuffer(String aggregationType, String groupKey) {
            this.aggregationType = aggregationType;
            this.groupKey = groupKey;
        }

        void add(double salary, double hours, double bonus) {
            recordCount++;
            totalSalary += salary;
            totalHours += hours;
            totalBonus += bonus;
            minSalary = Math.min(minSalary, salary);
            maxSalary = Math.max(maxSalary, salary);
        }
    }

    /**
     * Buffer a record's aggregation data in memory.
     */
    private void bufferAggregation(Map<String, AggregationBuffer> buffers, Map<String, Object> record, int aggregationDepth) {
        String dept = (String) record.getOrDefault("department", "UNKNOWN");
        String region = (String) record.getOrDefault("region", "UNKNOWN");
        String status = (String) record.getOrDefault("status", "UNKNOWN");

        @SuppressWarnings("unchecked")
        Map<String, Object> employee = (Map<String, Object>) record.getOrDefault("employee", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) record.getOrDefault("metrics", Map.of());

        double salary = ((Number) employee.getOrDefault("salary", 0)).doubleValue();
        double hours = ((Number) metrics.getOrDefault("hoursWorked", 0)).doubleValue();
        double bonus = ((Number) employee.getOrDefault("bonus", 0)).doubleValue();

        // Buffer BY_DEPARTMENT
        String deptKey = "BY_DEPARTMENT:" + dept;
        buffers.computeIfAbsent(deptKey, k -> new AggregationBuffer("BY_DEPARTMENT", dept)).add(salary, hours, bonus);

        // Buffer BY_REGION
        String regionKey = "BY_REGION:" + region;
        buffers.computeIfAbsent(regionKey, k -> new AggregationBuffer("BY_REGION", region)).add(salary, hours, bonus);

        // Buffer BY_STATUS
        String statusKey = "BY_STATUS:" + status;
        buffers.computeIfAbsent(statusKey, k -> new AggregationBuffer("BY_STATUS", status)).add(salary, hours, bonus);

        // Additional aggregations based on depth
        if (aggregationDepth >= 2) {
            String deptRegion = dept + "_" + region;
            String deptRegionKey = "BY_DEPARTMENT_REGION:" + deptRegion;
            buffers.computeIfAbsent(deptRegionKey, k -> new AggregationBuffer("BY_DEPARTMENT_REGION", deptRegion)).add(salary, hours, bonus);
        }

        if (aggregationDepth >= 3) {
            String deptRegionStatus = dept + "_" + region + "_" + status;
            String deptRegionStatusKey = "BY_DEPARTMENT_REGION_STATUS:" + deptRegionStatus;
            buffers.computeIfAbsent(deptRegionStatusKey, k -> new AggregationBuffer("BY_DEPARTMENT_REGION_STATUS", deptRegionStatus)).add(salary, hours, bonus);
        }
    }

    /**
     * Flush all buffered aggregations to the database.
     * This uses the batch upsert method for efficiency.
     */
    private void flushBuffersToDb(long jobId, Map<String, AggregationBuffer> buffers) {
        for (AggregationBuffer buffer : buffers.values()) {
            aggregationRepository.upsertAggregationBatch(
                jobId,
                buffer.aggregationType,
                buffer.groupKey,
                buffer.recordCount,
                buffer.totalSalary,
                buffer.totalHours,
                buffer.totalBonus,
                buffer.minSalary == Double.MAX_VALUE ? 0.0 : buffer.minSalary,
                buffer.maxSalary == Double.MIN_VALUE ? 0.0 : buffer.maxSalary
            );
        }
        log.debug("[Job {}] Flushed {} aggregation groups to DB", jobId, buffers.size());
    }

    /**
     * Write a single record's aggregation data directly to the database.
     * The record is NOT stored in memory after this method completes.
     */
    private void streamAggregationToDb(long jobId, Map<String, Object> record, int aggregationDepth) {
        String dept = (String) record.getOrDefault("department", "UNKNOWN");
        String region = (String) record.getOrDefault("region", "UNKNOWN");
        String status = (String) record.getOrDefault("status", "UNKNOWN");

        @SuppressWarnings("unchecked")
        Map<String, Object> employee = (Map<String, Object>) record.getOrDefault("employee", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) record.getOrDefault("metrics", Map.of());

        double salary = ((Number) employee.getOrDefault("salary", 0)).doubleValue();
        double hours = ((Number) metrics.getOrDefault("hoursWorked", 0)).doubleValue();
        double bonus = ((Number) employee.getOrDefault("bonus", 0)).doubleValue();

        // Upsert BY_DEPARTMENT aggregation
        aggregationRepository.upsertAggregation(jobId, "BY_DEPARTMENT", dept, salary, hours, bonus);

        // Upsert BY_REGION aggregation
        aggregationRepository.upsertAggregation(jobId, "BY_REGION", region, salary, hours, bonus);

        // Upsert BY_STATUS aggregation
        aggregationRepository.upsertAggregation(jobId, "BY_STATUS", status, salary, hours, bonus);

        // Additional aggregations based on depth
        if (aggregationDepth >= 2) {
            String deptRegionKey = dept + "_" + region;
            aggregationRepository.upsertAggregation(jobId, "BY_DEPARTMENT_REGION", deptRegionKey, salary, hours, bonus);
        }

        if (aggregationDepth >= 3) {
            String deptRegionStatusKey = dept + "_" + region + "_" + status;
            aggregationRepository.upsertAggregation(jobId, "BY_DEPARTMENT_REGION_STATUS", deptRegionStatusKey, salary, hours, bonus);
        }
    }

    // ==================== STEP 3: Encrypt Data (CPU + Memory Intensive) ====================

    @Bean
    public Step encryptDataStep() {
        return new StepBuilder("encryptDataStep", jobRepository)
            .listener(stepListener)
            .tasklet(encryptDataTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet encryptDataTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            boolean isVirtual = Thread.currentThread().isVirtual();
            int encryptionPasses = getIntParam(chunkContext, "encryptionPasses", DEFAULT_ENCRYPTION_PASSES);

            log.info("[Job {}] === STEP 3: ENCRYPT DATA ===", jobId);
            log.info("[Job {}] Encryption passes: {}, Virtual: {}", jobId, encryptionPasses, isVirtual);

            String dataFilePath = getFromContext(chunkContext, "dataFilePath");
            Path inputPath = Paths.get(dataFilePath);
            Path encryptedPath = inputPath.resolveSibling(
                inputPath.getFileName().toString().replace(".json", ".encrypted"));

            long startTime = System.currentTimeMillis();
            long fileSizeBytes = Files.size(inputPath);

            // Generate AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();

            // Multiple encryption passes
            Path currentInput = inputPath;
            Path currentOutput;

            for (int pass = 1; pass <= encryptionPasses; pass++) {
                log.info("[Job {}] Encryption pass {}/{}", jobId, pass, encryptionPasses);

                currentOutput = pass == encryptionPasses ? encryptedPath :
                    inputPath.resolveSibling("temp_pass_" + pass + ".encrypted");

                // AES-GCM encryption (CPU intensive)
                byte[] iv = new byte[12];
                new SecureRandom().nextBytes(iv);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

                // Process in chunks to manage memory
                int chunkSize = 64 * 1024 * 1024; // 64MB chunks
                byte[] buffer = new byte[chunkSize];
                long bytesProcessed = 0;

                try (InputStream in = new BufferedInputStream(new FileInputStream(currentInput.toFile()));
                     OutputStream out = new BufferedOutputStream(new FileOutputStream(currentOutput.toFile()))) {

                    // Write IV at the start
                    out.write(iv);

                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        byte[] encrypted = cipher.update(buffer, 0, read);
                        if (encrypted != null) {
                            out.write(encrypted);
                        }
                        bytesProcessed += read;

                        // Log progress
                        if (bytesProcessed % (100 * 1024 * 1024) == 0) {
                            double progress = (bytesProcessed * 100.0) / fileSizeBytes;
                            log.info("[Job {}] Pass {}: Encrypted {}%",
                                jobId, pass, String.format("%.0f", progress));
                        }
                    }

                    // Finalize encryption
                    byte[] finalBytes = cipher.doFinal();
                    if (finalBytes != null) {
                        out.write(finalBytes);
                    }
                }

                // Cleanup intermediate files
                if (pass > 1 && pass < encryptionPasses) {
                    Files.deleteIfExists(currentInput);
                }

                currentInput = currentOutput;
            }

            long elapsedMs = System.currentTimeMillis() - startTime;
            long encryptedSize = Files.size(encryptedPath);

            log.info("[Job {}] Encryption complete: {} MB -> {} MB in {} ms",
                jobId, fileSizeBytes / (1024 * 1024), encryptedSize / (1024 * 1024), elapsedMs);

            storeInContext(chunkContext, "encryptedFilePath", encryptedPath.toString());
            storeInContext(chunkContext, "secretKeyEncoded", Base64.getEncoder().encodeToString(secretKey.getEncoded()));

            logMemoryUsage(jobId, "After Encryption");

            return RepeatStatus.FINISHED;
        };
    }

    // ==================== STEP 4: Compress Data (CPU Intensive) ====================

    @Bean
    public Step compressDataStep() {
        return new StepBuilder("compressDataStep", jobRepository)
            .listener(stepListener)
            .tasklet(compressDataTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet compressDataTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            boolean isVirtual = Thread.currentThread().isVirtual();

            log.info("[Job {}] === STEP 4: COMPRESS DATA ===", jobId);
            log.info("[Job {}] Virtual: {}", jobId, isVirtual);

            String encryptedFilePath = getFromContext(chunkContext, "encryptedFilePath");
            Path inputPath = Paths.get(encryptedFilePath);
            Path compressedPath = inputPath.resolveSibling(
                inputPath.getFileName().toString() + ".gz");

            long startTime = System.currentTimeMillis();
            long inputSize = Files.size(inputPath);

            // GZIP compression
            int bufferSize = 64 * 1024 * 1024; // 64MB buffer
            byte[] buffer = new byte[bufferSize];
            long bytesCompressed = 0;

            try (InputStream in = new BufferedInputStream(new FileInputStream(inputPath.toFile()));
                 GZIPOutputStream gzOut = new GZIPOutputStream(
                     new BufferedOutputStream(new FileOutputStream(compressedPath.toFile())), bufferSize)) {

                int read;
                while ((read = in.read(buffer)) != -1) {
                    gzOut.write(buffer, 0, read);
                    bytesCompressed += read;

                    // Log progress
                    if (bytesCompressed % (100 * 1024 * 1024) == 0) {
                        double progress = (bytesCompressed * 100.0) / inputSize;
                        log.info("[Job {}] Compressed {}%", jobId, String.format("%.0f", progress));
                    }
                }
            }

            long elapsedMs = System.currentTimeMillis() - startTime;
            long compressedSize = Files.size(compressedPath);
            double ratio = (1 - (compressedSize * 1.0 / inputSize)) * 100;

            log.info("[Job {}] Compression complete: {} MB -> {} MB ({}% reduction) in {} ms",
                jobId, inputSize / (1024 * 1024), compressedSize / (1024 * 1024),
                String.format("%.1f", ratio), elapsedMs);

            storeInContext(chunkContext, "compressedFilePath", compressedPath.toString());
            storeInContext(chunkContext, "compressionRatio", ratio);

            logMemoryUsage(jobId, "After Compression");

            return RepeatStatus.FINISHED;
        };
    }

    // ==================== STEP 5: Decompress & Verify ====================

    @Bean
    public Step decompressAndVerifyStep() {
        return new StepBuilder("decompressAndVerifyStep", jobRepository)
            .listener(stepListener)
            .tasklet(decompressAndVerifyTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet decompressAndVerifyTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();
            boolean isVirtual = Thread.currentThread().isVirtual();

            log.info("[Job {}] === STEP 5: DECOMPRESS & VERIFY ===", jobId);
            log.info("[Job {}] Virtual: {}", jobId, isVirtual);

            String compressedFilePath = getFromContext(chunkContext, "compressedFilePath");
            Path inputPath = Paths.get(compressedFilePath);
            Path decompressedPath = inputPath.resolveSibling("verified_" + jobId + ".dat");

            long startTime = System.currentTimeMillis();

            // Decompress and compute checksum for verification
            int bufferSize = 64 * 1024 * 1024;
            byte[] buffer = new byte[bufferSize];
            long bytesDecompressed = 0;
            long checksum = 0;

            try (GZIPInputStream gzIn = new GZIPInputStream(
                     new BufferedInputStream(new FileInputStream(inputPath.toFile())), bufferSize);
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(decompressedPath.toFile()))) {

                int read;
                while ((read = gzIn.read(buffer)) != -1) {
                    out.write(buffer, 0, read);

                    // Compute simple checksum
                    for (int i = 0; i < read; i++) {
                        checksum += buffer[i] & 0xFF;
                    }

                    bytesDecompressed += read;

                    // Log progress
                    if (bytesDecompressed % (100 * 1024 * 1024) == 0) {
                        log.info("[Job {}] Decompressed {} MB", jobId, bytesDecompressed / (1024 * 1024));
                    }
                }
            }

            long elapsedMs = System.currentTimeMillis() - startTime;

            log.info("[Job {}] Decompression complete: {} MB in {} ms, checksum: {}",
                jobId, bytesDecompressed / (1024 * 1024), elapsedMs, checksum);

            storeInContext(chunkContext, "decompressedFilePath", decompressedPath.toString());
            storeInContext(chunkContext, "verificationChecksum", checksum);

            logMemoryUsage(jobId, "After Verification");

            return RepeatStatus.FINISHED;
        };
    }

    // ==================== STEP 6: Cleanup ====================

    @Bean
    public Step cleanupComputeFilesStep() {
        return new StepBuilder("cleanupComputeFilesStep", jobRepository)
            .listener(stepListener)
            .tasklet(cleanupComputeFilesTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet cleanupComputeFilesTasklet() {
        return (contribution, chunkContext) -> {
            long jobId = chunkContext.getStepContext().getStepExecution().getJobExecutionId();

            log.info("[Job {}] === STEP 6: CLEANUP ===", jobId);

            // Get all file paths
            String dataFilePath = getFromContext(chunkContext, "dataFilePath");
            String encryptedFilePath = getFromContext(chunkContext, "encryptedFilePath");
            String compressedFilePath = getFromContext(chunkContext, "compressedFilePath");
            String decompressedFilePath = getFromContext(chunkContext, "decompressedFilePath");

            // Delete all temporary files
            int deleted = 0;
            for (String path : Arrays.asList(dataFilePath, encryptedFilePath,
                    compressedFilePath, decompressedFilePath)) {
                if (path != null) {
                    try {
                        Files.deleteIfExists(Paths.get(path));
                        deleted++;
                    } catch (Exception e) {
                        log.warn("[Job {}] Failed to delete: {}", jobId, path);
                    }
                }
            }

            // Get stats
            int recordCount = getIntFromContext(chunkContext, "recordCount");
            long fileSizeBytes = getLongFromContext(chunkContext, "fileSizeBytes");
            int departmentCount = getIntFromContext(chunkContext, "departmentCount");
            int aggregationCount = getIntFromContext(chunkContext, "aggregationCount");
            int recordsInMemory = getIntFromContext(chunkContext, "recordsInMemory");
            double compressionRatio = getDoubleFromContext(chunkContext, "compressionRatio");
            long checksum = getLongFromContext(chunkContext, "verificationChecksum");

            log.info("================================================================");
            log.info("  COMPUTE-INTENSIVE JOB {} COMPLETED", jobId);
            log.info("================================================================");
            log.info("  Records Generated:   {}", recordCount);
            log.info("  Data File Size:      {} MB", fileSizeBytes / (1024 * 1024));
            log.info("  Records In Memory:   {}", recordsInMemory);
            log.info("  Departments:         {}", departmentCount);
            log.info("  Aggregations:        {}", aggregationCount);
            log.info("  Compression Ratio:   {}%", String.format("%.1f", compressionRatio));
            log.info("  Verification Sum:    {}", checksum);
            log.info("  Files Cleaned:       {}", deleted);
            log.info("  Virtual Thread:      {}", Thread.currentThread().isVirtual());
            log.info("================================================================");

            logMemoryUsage(jobId, "Final");

            return RepeatStatus.FINISHED;
        };
    }

    // ==================== Helper Methods ====================

    private Map<String, Object> generateComplexRecord(int index, int sizeKB, Random random) {
        Map<String, Object> record = new LinkedHashMap<>();

        record.put("id", index);
        record.put("timestamp", System.currentTimeMillis());
        record.put("department", "DEPT_" + (index % 50)); // 50 departments
        record.put("region", "REGION_" + (index % 10));
        record.put("status", index % 3 == 0 ? "ACTIVE" : (index % 3 == 1 ? "PENDING" : "COMPLETED"));

        // Employee data
        Map<String, Object> employee = new LinkedHashMap<>();
        employee.put("empId", "EMP" + String.format("%08d", index));
        employee.put("firstName", generateRandomString(10, random));
        employee.put("lastName", generateRandomString(15, random));
        employee.put("email", generateRandomString(8, random) + "@example.com");
        employee.put("salary", 50000 + random.nextInt(100000));
        employee.put("bonus", random.nextDouble() * 10000);
        record.put("employee", employee);

        // Metrics
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("hoursWorked", 40 + random.nextInt(20));
        metrics.put("overtime", random.nextInt(10));
        metrics.put("productivity", 70 + random.nextDouble() * 30);
        metrics.put("qualityScore", 80 + random.nextDouble() * 20);
        record.put("metrics", metrics);

        // Add padding to reach target size
        int currentSize = record.toString().length();
        int targetSize = sizeKB * 1024;
        if (currentSize < targetSize) {
            int paddingNeeded = targetSize - currentSize;
            record.put("data", generateRandomString(paddingNeeded, random));
        }

        // Nested arrays for additional memory pressure
        List<Map<String, Object>> transactions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> tx = new LinkedHashMap<>();
            tx.put("txId", UUID.randomUUID().toString());
            tx.put("amount", random.nextDouble() * 1000);
            tx.put("type", i % 2 == 0 ? "CREDIT" : "DEBIT");
            transactions.add(tx);
        }
        record.put("transactions", transactions);

        return record;
    }

    private String generateRandomString(int length, Random random) {
        StringBuilder sb = new StringBuilder(length);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Legacy in-memory aggregation method.
     * FIX: byDepartment is now nullable - if null, records are NOT stored in the map.
     */
    private void performAggregationsLegacy(Map<String, Object> record,
            Map<String, List<Map<String, Object>>> byDepartment,
            Map<String, Map<String, Double>> aggregations,
            Map<String, Set<String>> uniqueValues,
            int depth) {

        String dept = (String) record.getOrDefault("department", "UNKNOWN");
        String region = (String) record.getOrDefault("region", "UNKNOWN");
        String status = (String) record.getOrDefault("status", "UNKNOWN");

        // Group by department - ONLY if byDepartment map is provided (holdInMemory=true)
        // FIX: Previously this ALWAYS stored records, causing OOM even with holdInMemory=false
        if (byDepartment != null) {
            byDepartment.computeIfAbsent(dept, k -> new ArrayList<>()).add(record);
        }

        // Aggregations (these are lightweight - just counters and sums)
        @SuppressWarnings("unchecked")
        Map<String, Object> employee = (Map<String, Object>) record.getOrDefault("employee", Map.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) record.getOrDefault("metrics", Map.of());

        double salary = ((Number) employee.getOrDefault("salary", 0)).doubleValue();
        double hours = ((Number) metrics.getOrDefault("hoursWorked", 0)).doubleValue();

        aggregations.computeIfAbsent(dept, k -> new HashMap<>())
            .merge("totalSalary", salary, Double::sum);
        aggregations.computeIfAbsent(dept, k -> new HashMap<>())
            .merge("totalHours", hours, Double::sum);
        aggregations.computeIfAbsent(dept, k -> new HashMap<>())
            .merge("count", 1.0, Double::sum);

        // Unique values (small sets)
        uniqueValues.computeIfAbsent("departments", k -> new HashSet<>()).add(dept);
        uniqueValues.computeIfAbsent("regions", k -> new HashSet<>()).add(region);
        uniqueValues.computeIfAbsent("statuses", k -> new HashSet<>()).add(status);

        // Additional aggregations based on depth
        if (depth >= 2) {
            String key = dept + "_" + region;
            aggregations.computeIfAbsent(key, k -> new HashMap<>())
                .merge("totalSalary", salary, Double::sum);
        }

        if (depth >= 3) {
            String key = dept + "_" + region + "_" + status;
            aggregations.computeIfAbsent(key, k -> new HashMap<>())
                .merge("totalSalary", salary, Double::sum);
        }
    }

    private void logMemoryUsage(long jobId, String phase) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        double usedMB = usedMemory / (1024.0 * 1024.0);
        double maxMB = maxMemory / (1024.0 * 1024.0);
        double usedPercent = (usedMemory * 100.0) / maxMemory;

        log.info("[Job {}] MEMORY [{}]: Used: {} MB / {} MB ({}%)",
            jobId, phase, String.format("%.0f", usedMB), String.format("%.0f", maxMB),
            String.format("%.1f", usedPercent));
    }

    // Context helpers
    private void storeInContext(org.springframework.batch.core.scope.context.ChunkContext ctx, String key, Object value) {
        ctx.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put(key, value);
    }

    private String getFromContext(org.springframework.batch.core.scope.context.ChunkContext ctx, String key) {
        return (String) ctx.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get(key);
    }

    private int getIntFromContext(org.springframework.batch.core.scope.context.ChunkContext ctx, String key) {
        Object val = ctx.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get(key);
        return val != null ? ((Number) val).intValue() : 0;
    }

    private long getLongFromContext(org.springframework.batch.core.scope.context.ChunkContext ctx, String key) {
        Object val = ctx.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get(key);
        return val != null ? ((Number) val).longValue() : 0L;
    }

    private double getDoubleFromContext(org.springframework.batch.core.scope.context.ChunkContext ctx, String key) {
        Object val = ctx.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get(key);
        return val != null ? ((Number) val).doubleValue() : 0.0;
    }

    private int getIntParam(org.springframework.batch.core.scope.context.ChunkContext ctx, String key, int defaultValue) {
        Object val = ctx.getStepContext().getJobParameters().get(key);
        if (val != null) {
            return Integer.parseInt(val.toString());
        }
        return defaultValue;
    }

    private boolean getBooleanParam(org.springframework.batch.core.scope.context.ChunkContext ctx, String key, boolean defaultValue) {
        Object val = ctx.getStepContext().getJobParameters().get(key);
        if (val != null) {
            return Boolean.parseBoolean(val.toString());
        }
        return defaultValue;
    }
}
