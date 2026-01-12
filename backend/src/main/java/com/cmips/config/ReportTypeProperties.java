package com.cmips.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for report types loaded from application.yml
 * Centralizes all report type definitions to avoid hardcoding
 */
@Configuration
@ConfigurationProperties(prefix = "report-types")
@Data
public class ReportTypeProperties {
    
    /**
     * All valid report types in the system
     */
    private List<String> all = new ArrayList<>();
    
    /**
     * Role-specific report type mappings
     * Maps role name to list of allowed report types
     */
    private Map<String, List<String>> roleMappings = new HashMap<>();
    
    /**
     * Scheduler-specific report types
     */
    private SchedulerReportTypes schedulers = new SchedulerReportTypes();
    
    /**
     * Default report type for various contexts
     */
    private String defaultReportType = "TIMESHEET_REPORT";
    
    /**
     * Report processing durations (in minutes)
     */
    private Map<String, Long> estimatedDurations = new HashMap<>();
    
    /**
     * Get report types for a specific role
     * @param role The user role
     * @return List of report types allowed for that role, or empty list if role not configured
     */
    public List<String> getReportTypesForRole(String role) {
        return roleMappings.getOrDefault(role, new ArrayList<>());
    }
    
    /**
     * Check if a report type is valid
     * @param reportType The report type to check
     * @return true if the report type is in the "all" list
     */
    public boolean isValidReportType(String reportType) {
        return all.contains(reportType);
    }
    
    /**
     * Get estimated duration for a report type
     * @param reportType The report type
     * @return Duration in minutes, or 5 minutes as default
     */
    public long getEstimatedDuration(String reportType) {
        return estimatedDurations.getOrDefault(reportType, 5L);
    }
    
    @Data
    public static class SchedulerReportTypes {
        private List<String> daily = new ArrayList<>();
        private List<String> weekly = new ArrayList<>();
        private List<String> monthly = new ArrayList<>();
        private List<String> quarterly = new ArrayList<>();
        private List<String> annual = new ArrayList<>();
        private List<String> test = new ArrayList<>();
    }
}

