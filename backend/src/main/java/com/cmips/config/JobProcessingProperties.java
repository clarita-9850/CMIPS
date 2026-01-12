package com.cmips.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "job-processing")
public class JobProcessingProperties {

    /**
     * Default chunk size for manually submitted jobs.
     */
    private int defaultChunkSize = 1000;

    /**
     * Chunk size for dependent/child jobs in Spring Batch processing.
     */
    private int dependentChunkSize = 2000;

    /**
     * Minimum chunk size allowed to avoid overly chatty progress updates.
     */
    private int minChunkSize = 50;

    /**
     * Maximum chunk size allowed to keep memory usage predictable.
     */
    private int maxChunkSize = 5000;

    public int normalizeChunkSize(Integer requestedChunkSize) {
        int chunkSize = requestedChunkSize != null ? requestedChunkSize : defaultChunkSize;
        return clamp(chunkSize);
    }

    public int getEffectiveDependentChunkSize(Integer parentChunkSize) {
        int chunkSize = dependentChunkSize > 0 ? dependentChunkSize : defaultChunkSize;
        if (parentChunkSize != null && parentChunkSize > chunkSize) {
            chunkSize = parentChunkSize;
        }
        return clamp(chunkSize);
    }

    private int clamp(int chunkSize) {
        if (chunkSize < minChunkSize) {
            return minChunkSize;
        }
        if (chunkSize > maxChunkSize) {
            return maxChunkSize;
        }
        return chunkSize;
    }

    public int getDefaultChunkSize() {
        return defaultChunkSize;
    }

    public void setDefaultChunkSize(int defaultChunkSize) {
        this.defaultChunkSize = defaultChunkSize;
    }

    public int getDependentChunkSize() {
        return dependentChunkSize;
    }

    public void setDependentChunkSize(int dependentChunkSize) {
        this.dependentChunkSize = dependentChunkSize;
    }

    public int getMinChunkSize() {
        return minChunkSize;
    }

    public void setMinChunkSize(int minChunkSize) {
        this.minChunkSize = minChunkSize;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }
}

