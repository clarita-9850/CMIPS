package com.cmips.baw.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for BAW (Business Automation Workflow) integration.
 *
 * These properties configure SFTP connections to external systems like
 * STO (State Treasurer Office), SCO (State Controller Office), EDD, and DOJ.
 */
@Data
@ConfigurationProperties(prefix = "baw")
public class BawIntegrationProperties {

    /**
     * Whether to use mock service instead of real SFTP connections.
     */
    private MockConfig mock = new MockConfig();

    /**
     * SFTP connection configurations for external systems.
     */
    private Map<String, SftpSystemConfig> sftp = new HashMap<>();

    /**
     * Local directories for file processing.
     */
    private LocalDirectories local = new LocalDirectories();

    /**
     * File processing settings.
     */
    private FileProcessingConfig processing = new FileProcessingConfig();

    @Data
    public static class MockConfig {
        /**
         * If true, uses MockBawFileService instead of real SFTP connections.
         */
        private boolean enabled = true;
    }

    @Data
    public static class SftpSystemConfig {
        /**
         * SFTP host name or IP address.
         */
        private String host = "localhost";

        /**
         * SFTP port (default 22).
         */
        private int port = 22;

        /**
         * SFTP username.
         */
        private String username;

        /**
         * SFTP password (if using password auth).
         */
        private String password;

        /**
         * Path to private key file (if using key auth).
         */
        private String privateKeyPath;

        /**
         * Passphrase for private key (if encrypted).
         */
        private String privateKeyPassphrase;

        /**
         * Remote path for inbound files (to download from).
         */
        private String inboundPath = "/incoming";

        /**
         * Remote path for outbound files (to upload to).
         */
        private String outboundPath = "/outgoing";

        /**
         * Remote path for archiving processed files.
         */
        private String archivePath = "/archive";

        /**
         * Remote path for files with processing errors.
         */
        private String errorPath = "/error";

        /**
         * Path to known_hosts file for SSH verification.
         * Empty string disables strict host key checking.
         */
        private String knownHosts = "";

        /**
         * Connection timeout in milliseconds.
         */
        private int connectionTimeout = 30000;

        /**
         * Read timeout in milliseconds.
         */
        private int readTimeout = 60000;
    }

    @Data
    public static class LocalDirectories {
        /**
         * Local directory for temporary file processing.
         */
        private String tempDir = "./baw-temp";

        /**
         * Local directory for downloaded inbound files.
         */
        private String inboundDir = "./baw-inbound";

        /**
         * Local directory for outbound files before upload.
         */
        private String outboundDir = "./baw-outbound";

        /**
         * Local directory for archived files.
         */
        private String archiveDir = "./baw-archive";
    }

    @Data
    public static class FileProcessingConfig {
        /**
         * Whether to delete local temp files after processing.
         */
        private boolean deleteAfterProcess = true;

        /**
         * Whether to archive remote files after successful processing.
         */
        private boolean archiveAfterProcess = true;

        /**
         * File name pattern for STO warrant files.
         */
        private String stoWarrantFilePattern = "PRDR110A_*.DAT";

        /**
         * File name pattern for SCO payment files.
         */
        private String scoPaymentFilePattern = "PRDR120A_%s.DAT";

        /**
         * Maximum retry attempts for SFTP operations.
         */
        private int maxRetries = 3;

        /**
         * Delay between retries in milliseconds.
         */
        private long retryDelayMs = 1000;
    }

    /**
     * Gets the SFTP configuration for a specific system.
     *
     * @param systemCode system code (e.g., "sto", "sco", "edd", "doj")
     * @return the SFTP configuration, or null if not found
     */
    public SftpSystemConfig getSystemConfig(String systemCode) {
        return sftp.get(systemCode.toLowerCase());
    }
}
