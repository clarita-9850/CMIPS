package com.cmips.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Batch Scheduler integration.
 * Defines how this Trial app connects to and interacts with the Scheduler app.
 */
@ConfigurationProperties(prefix = "batch-scheduler")
@Data
public class BatchSchedulerProperties {

    /**
     * Whether the batch scheduler integration is enabled
     */
    private boolean enabled = true;

    /**
     * Base URL of the Scheduler application
     */
    private String schedulerUrl = "http://scheduler-backend:8084";

    /**
     * Redis configuration for job event pub/sub
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * Retry configuration for scheduler communication
     */
    private RetryConfig retry = new RetryConfig();

    @Data
    public static class RedisConfig {
        /**
         * Redis channel prefix for batch job events
         */
        private String channelPrefix = "batch:events:";

        /**
         * Channels for different event types
         */
        private String jobStartedChannel = "batch:events:job-started";
        private String jobProgressChannel = "batch:events:job-progress";
        private String jobCompletedChannel = "batch:events:job-completed";
        private String jobFailedChannel = "batch:events:job-failed";
    }

    @Data
    public static class RetryConfig {
        /**
         * Maximum number of retry attempts
         */
        private int maxAttempts = 3;

        /**
         * Initial delay between retries in milliseconds
         */
        private long initialDelayMs = 1000;

        /**
         * Multiplier for exponential backoff
         */
        private double multiplier = 2.0;

        /**
         * Maximum delay between retries in milliseconds
         */
        private long maxDelayMs = 30000;
    }
}
