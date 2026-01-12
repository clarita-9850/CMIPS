package com.cmips.baw.config;

import com.cmips.baw.BawFileService;
import com.cmips.baw.IntegrationHubBawFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for BAW (Business Automation Workflow) integration.
 *
 * This configuration enables the IntegrationHubBawFileService which uses
 * the Integration Hub Framework for real SFTP connections to external systems.
 */
@Configuration
@EnableConfigurationProperties(BawIntegrationProperties.class)
@Slf4j
public class BawIntegrationConfig {

    /**
     * Creates the Integration Hub based BawFileService.
     *
     * This service uses the Integration Hub Framework to:
     * - Connect to external SFTP servers (STO, SCO, EDD, DOJ)
     * - Download/upload files using JSch
     * - Parse/write fixed-width file formats using FileRepository
     * - Convert between file records and DTOs
     */
    @Bean
    public BawFileService integrationHubBawFileService(BawIntegrationProperties properties) {
        log.info("=== Initializing IntegrationHubBawFileService ===");
        log.info("SFTP connections configured for: {}", properties.getSftp().keySet());
        return new IntegrationHubBawFileService(properties);
    }
}
