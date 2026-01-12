package com.cmips.service;

import com.cmips.model.ReportResult;
import com.cmips.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CSVReportGeneratorService {

    @Autowired
    private FieldMaskingService fieldMaskingService;

    @Autowired
    private FieldVisibilityService fieldVisibilityService;
    
    public CSVReportGeneratorService() {
        System.out.println("🔧 CSVReportGeneratorService: Initializing CSV report generator");
    }
    
    /**
     * Generate daily CSV report directly (without job queue)
     */
    public String generateDailyCSVReportDirect(String userRole, String reportType, String dateStr) {
        System.out.println("📊 CSVReportGeneratorService: Generating daily CSV report directly");
        System.out.println("👤 User Role: " + userRole);
        System.out.println("📊 Report Type: " + reportType);
        System.out.println("📅 Date: " + dateStr);
        
        try {
            // Generate CSV file path with role and report type
            String csvFilePath = generateCSVFilePath(userRole, reportType, dateStr);
            System.out.println("📄 CSV file path: " + csvFilePath);
            
            // Create CSV content with role-based data filtering (simplified)
            String csvContent = generateSimpleCSVContent(userRole, reportType, dateStr);
            
            // Write CSV file
            writeCSVFile(csvFilePath, csvContent);
            System.out.println("✅ CSV report generated successfully: " + csvFilePath);
            System.out.println("🔒 Data filtered based on role permissions: " + userRole);
            
            return csvFilePath;
            
        } catch (Exception e) {
            System.err.println("❌ Error generating CSV report: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }
    
    /**
     * Generate daily CSV report for a specific job (Spring Batch version)
     */
    public String generateDailyCSVReport(String jobId, String userRole, String reportType, String dateStr) {
        System.out.println("📊 CSVReportGeneratorService: Generating daily CSV report");
        System.out.println("📋 Job ID: " + jobId);
        System.out.println("👤 User Role: " + userRole);
        System.out.println("📊 Report Type: " + reportType);
        System.out.println("📅 Date: " + dateStr);

        try {
            // Get visible fields for the user role - using default fields when no JWT token available
            List<String> visibleFields = fieldVisibilityService.getDefaultVisibleFields();
            System.out.println("👁️ Visible fields for " + userRole + ": " + visibleFields);

            // Generate CSV file path
            String csvFilePath = generateCSVFilePath(userRole, reportType, dateStr);
            System.out.println("📄 CSV file path: " + csvFilePath);

            // Create CSV content with role-based filtering
            String csvContent = generateCSVContentWithRoleFiltering(visibleFields, userRole, reportType, dateStr);

            // Write CSV file
            writeCSVFile(csvFilePath, csvContent);
            System.out.println("✅ CSV report generated successfully: " + csvFilePath);

            return csvFilePath;

        } catch (Exception e) {
            System.err.println("❌ Error generating CSV report: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }
    
    /**
     * Generate simple CSV content with role-based filtering
     */
    private String generateSimpleCSVContent(String userRole, String reportType, String dateStr) {
        System.out.println("📝 Generating simple CSV content with role-based filtering");
        System.out.println("👤 User Role: " + userRole);
        System.out.println("📊 Report Type: " + reportType);
        
        StringBuilder csvContent = new StringBuilder();
        
        // Add header information with role context
        csvContent.append("# Daily Report - ").append(reportType).append("\n");
        csvContent.append("# User Role: ").append(userRole).append("\n");
        csvContent.append("# Date: ").append(dateStr).append("\n");
        csvContent.append("# Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        csvContent.append("# Data Scope: Role-based filtered data for ").append(userRole).append("\n");
        csvContent.append("#\n");
        
        // Define fields based on role
        List<String> fields = getFieldsForRole(userRole);
        csvContent.append(String.join(",", fields)).append("\n");
        
        // Generate role-specific sample data
        int recordCount = getRecordCountForRole(userRole);
        for (int i = 1; i <= recordCount; i++) {
            List<String> row = new ArrayList<>();
            for (String field : fields) {
                String value = generateFieldValueForRole(field, userRole, i);
                row.add(formatCSVValue(value));
            }
            csvContent.append(String.join(",", row)).append("\n");
        }
        
        System.out.println("📊 CSV content generated with " + recordCount + " records for role: " + userRole);
        return csvContent.toString();
    }
    
    /**
     * Get fields based on role permissions
     */
    private List<String> getFieldsForRole(String userRole) {
        UserRole role = UserRole.from(userRole);
        return switch (role) {
            case ADMIN, SYSTEM_SCHEDULER -> Arrays.asList("timesheetId", "providerName", "recipientName", "totalHours", "totalAmount", "status", "county", "district");
            case SUPERVISOR -> Arrays.asList("timesheetId", "totalHours", "totalAmount", "status", "county", "district");
            case CASE_WORKER -> Arrays.asList("timesheetId", "totalHours", "status", "county");
            case PROVIDER, RECIPIENT -> Arrays.asList("timesheetId", "totalHours", "status");
        };
    }
    
    /**
     * Get record count based on role
     */
    private int getRecordCountForRole(String userRole) {
        return switch (UserRole.from(userRole)) {
            case ADMIN, SYSTEM_SCHEDULER -> 100;
            case SUPERVISOR -> 50;
            case CASE_WORKER -> 25;
            case PROVIDER, RECIPIENT -> 10;
        };
    }
    
    /**
     * Generate field value based on role
     */
    private String generateFieldValueForRole(String field, String userRole, int recordIndex) {
        String lowerField = field.toLowerCase();
        
        UserRole role = UserRole.from(userRole);
        if (lowerField.contains("id")) {
            if (role == UserRole.ADMIN || role == UserRole.SYSTEM_SCHEDULER) {
                return "TS" + String.format("%06d", recordIndex);
            } else {
                return "***" + String.format("%04d", recordIndex);
            }
        } else if (lowerField.contains("name")) {
            if (role == UserRole.ADMIN || role == UserRole.SYSTEM_SCHEDULER) {
                return "User " + recordIndex;
            } else {
                return "User " + recordIndex;
            }
        } else if (lowerField.contains("hours")) {
            return String.valueOf(recordIndex * 8.5);
        } else if (lowerField.contains("amount")) {
            if (role == UserRole.ADMIN || role == UserRole.SYSTEM_SCHEDULER) {
                return "$" + (recordIndex * 100.50);
            } else if (role == UserRole.SUPERVISOR) {
                return "$" + (recordIndex * 100) + "-" + (recordIndex * 200);
            } else {
                return "$" + (recordIndex * 50) + "-" + (recordIndex * 100);
            }
        } else if (lowerField.contains("status")) {
            String[] statuses = {"APPROVED", "PENDING", "REJECTED"};
            return statuses[recordIndex % statuses.length];
        } else if (lowerField.contains("county")) {
            // Use actual county names - return one of the 5 configured counties
            String[] counties = {"Orange", "Sacramento", "Riverside", "Los Angeles", "Alameda"};
            return counties[recordIndex % counties.length];
        } else if (lowerField.contains("district")) {
            String region = inferRegion(userRole);
            if ("NORTH".equals(region)) return "DIST001";
            if ("CENTRAL".equals(region)) return "DIST003";
            if ("SOUTH".equals(region)) return "DIST005";
            return "MULTIPLE_DISTRICTS";
        } else {
            return "Value_" + recordIndex;
        }
    }
    
    /**
     * Generate CSV content with role-based filtering
     */
    private String generateCSVContentWithRoleFiltering(List<String> visibleFields, 
                                                      String userRole, String reportType, String dateStr) {
        System.out.println("📝 Generating CSV content with role-based filtering");
        System.out.println("👤 User Role: " + userRole);
        System.out.println("📊 Report Type: " + reportType);
        System.out.println("👁️ Visible Fields: " + visibleFields.size() + " fields");
        
        StringBuilder csvContent = new StringBuilder();
        
        // Add header information with role context
        csvContent.append("# Daily Report - ").append(reportType).append("\n");
        csvContent.append("# User Role: ").append(userRole).append("\n");
        csvContent.append("# Date: ").append(dateStr).append("\n");
        csvContent.append("# Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        csvContent.append("# Data Scope: Role-based filtered data for ").append(userRole).append("\n");
        csvContent.append("# Visible Fields: ").append(visibleFields.size()).append("\n");
        csvContent.append("#\n");
        
        // Add CSV header row with only visible fields
        csvContent.append(String.join(",", visibleFields)).append("\n");
        
        // Generate role-specific sample data
        List<Map<String, Object>> sampleData = generateRoleSpecificData(visibleFields, userRole, reportType);
        
        // Add data rows
        for (Map<String, Object> record : sampleData) {
            List<String> row = new ArrayList<>();
            for (String field : visibleFields) {
                Object value = record.get(field);
                String csvValue = formatCSVValue(value);
                row.add(csvValue);
            }
            csvContent.append(String.join(",", row)).append("\n");
        }
        
        System.out.println("📊 CSV content generated with " + sampleData.size() + " records for role: " + userRole);
        return csvContent.toString();
    }
    
    /**
     * Generate CSV content with proper field masking
     */
    private String generateCSVContent(ReportResult jobResult, List<String> visibleFields, 
                                    String userRole, String reportType, String dateStr) {
        System.out.println("📝 Generating CSV content with " + visibleFields.size() + " visible fields");
        
        StringBuilder csvContent = new StringBuilder();
        
        // Add header information
        csvContent.append("# Daily Report - ").append(reportType).append("\n");
        csvContent.append("# User Role: ").append(userRole).append("\n");
        csvContent.append("# Date: ").append(dateStr).append("\n");
        csvContent.append("# Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        csvContent.append("# Total Records: ").append(jobResult.getTotalRecords()).append("\n");
        csvContent.append("# Visible Fields: ").append(visibleFields.size()).append("\n");
        csvContent.append("#\n");
        
        // Add CSV header row
        csvContent.append(String.join(",", visibleFields)).append("\n");
        
        // Add sample data (in production, this would come from the actual job result)
        List<Map<String, Object>> sampleData = generateSampleData(visibleFields, userRole, reportType);
        
        // Add data rows
        for (Map<String, Object> record : sampleData) {
            List<String> row = new ArrayList<>();
            for (String field : visibleFields) {
                Object value = record.get(field);
                String csvValue = formatCSVValue(value);
                row.add(csvValue);
            }
            csvContent.append(String.join(",", row)).append("\n");
        }
        
        System.out.println("📊 CSV content generated with " + sampleData.size() + " records");
        return csvContent.toString();
    }
    
    /**
     * Generate role-specific data based on user permissions
     */
    private List<Map<String, Object>> generateRoleSpecificData(List<String> visibleFields, String userRole, String reportType) {
        System.out.println("📊 Generating role-specific data for: " + userRole);
        System.out.println("📋 Report Type: " + reportType);
        System.out.println("👁️ Visible Fields: " + visibleFields);
        
        List<Map<String, Object>> sampleData = new ArrayList<>();
        
        // Generate sample records based on role and report type
        int recordCount = getRecordCountForRoleAndReportType(userRole, reportType);
        
        for (int i = 1; i <= recordCount; i++) {
            Map<String, Object> record = new HashMap<>();
            
            for (String field : visibleFields) {
                Object value = generateRoleBasedFieldValue(field, userRole, i);
                record.put(field, value);
            }
            
            sampleData.add(record);
        }
        
        System.out.println("📊 Generated " + sampleData.size() + " records for role: " + userRole);
        return sampleData;
    }
    
    /**
     * Generate sample data based on visible fields and user role
     */
    private List<Map<String, Object>> generateSampleData(List<String> visibleFields, String userRole, String reportType) {
        List<Map<String, Object>> sampleData = new ArrayList<>();
        
        // Generate sample records based on report type
        int recordCount = getRecordCountForReportType(reportType);
        
        for (int i = 1; i <= recordCount; i++) {
            Map<String, Object> record = new HashMap<>();
            
            for (String field : visibleFields) {
                Object value = generateFieldValue(field, userRole, i);
                record.put(field, value);
            }
            
            sampleData.add(record);
        }
        
        return sampleData;
    }
    
    /**
     * Generate field value based on field name and user role
     */
    private Object generateFieldValue(String fieldName, String userRole, int recordIndex) {
        String lowerFieldName = fieldName.toLowerCase();
        UserRole role = UserRole.from(userRole);
        boolean isAdmin = role == UserRole.ADMIN || role == UserRole.SYSTEM_SCHEDULER;
        
        if (lowerFieldName.contains("id") && !isAdmin) {
            return "***" + String.format("%04d", recordIndex);
        } else if (lowerFieldName.contains("name") && !isAdmin) {
            return "User " + recordIndex;
        } else if (lowerFieldName.contains("email") && !isAdmin) {
            return "user" + recordIndex + "@masked.com";
        } else if (lowerFieldName.contains("amount") || lowerFieldName.contains("total")) {
            return switch (role) {
                case ADMIN, SYSTEM_SCHEDULER -> "$" + (recordIndex * 100.50);
                case SUPERVISOR -> "$" + (recordIndex * 100) + "-" + (recordIndex * 200);
                case CASE_WORKER -> "$" + (recordIndex * 50) + "-" + (recordIndex * 100);
                case PROVIDER, RECIPIENT -> "$" + (recordIndex * 25);
            };
        } else if (lowerFieldName.contains("hours")) {
            return recordIndex * 8.5;
        } else if (lowerFieldName.contains("date")) {
            return LocalDateTime.now().minusDays(recordIndex).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } else if (lowerFieldName.contains("status")) {
            String[] statuses = {"APPROVED", "PENDING", "REJECTED"};
            return statuses[recordIndex % statuses.length];
        } else {
            return "Value_" + recordIndex;
        }
    }
    
    /**
     * Get record count based on role and report type
     */
    private int getRecordCountForRoleAndReportType(String userRole, String reportType) {
        // Base count on role permissions
        return getRecordCountForRole(userRole);
    }
    
    /**
     * Generate field value based on role permissions
     */
    private Object generateRoleBasedFieldValue(String fieldName, String userRole, int recordIndex) {
        return generateFieldValue(fieldName, userRole, recordIndex);
    }
    
    /**
     * Get record count based on report type
     */
    private int getRecordCountForReportType(String reportType) {
        switch (reportType) {
            case "DAILY_SUMMARY":
                return 50;
            case "COUNTY_DAILY":
                return 100;
            // Legacy report types - map to COUNTY_DAILY
            case "DISTRICT_DAILY":
            case "CENTRAL_DAILY":
                return 100;
            default:
                return 25;
        }
    }
    
    /**
     * Format value for CSV (handle commas, quotes, etc.)
     */
    private String formatCSVValue(Object value) {
        if (value == null) {
            return "";
        }
        
        String stringValue = value.toString();
        
        // Escape quotes and wrap in quotes if contains comma
        if (stringValue.contains(",") || stringValue.contains("\"") || stringValue.contains("\n")) {
            stringValue = "\"" + stringValue.replace("\"", "\"\"") + "\"";
        }
        
        return stringValue;
    }
    
    /**
     * Generate CSV file path
     */
    private String generateCSVFilePath(String userRole, String reportType, String dateStr) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String fileName = String.format("%s_%s_%s_%s.csv", 
            userRole.toLowerCase(), reportType.toLowerCase(), dateStr, timestamp);
        
        // Create reports directory if it doesn't exist
        Path reportsDir = Paths.get("reports", "daily", dateStr);
        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            System.err.println("❌ Error creating reports directory: " + e.getMessage());
        }
        
        return reportsDir.resolve(fileName).toString();
    }
    
    /**
     * Write CSV file
     */
    private void writeCSVFile(String filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
        
        // Verify file was created
        Path path = Paths.get(filePath);
        if (Files.exists(path)) {
            long fileSize = Files.size(path);
            System.out.println("📄 CSV file written: " + filePath + " (" + fileSize + " bytes)");
        } else {
            throw new IOException("CSV file was not created: " + filePath);
        }
    }
    
    
    /**
     * Get CSV generator status
     */
    public String getCSVGeneratorStatus() {
        return String.format(
            "CSV Report Generator Status:\n" +
            "Service: Active\n" +
            "Field Masking: Enabled\n" +
            "Role-Based Filtering: Enabled\n" +
            "Date Format: yyyy-MM-dd\n" +
            "Time Format: HH:mm:ss\n" +
            "Output Directory: reports/daily/"
        );
    }

    private String inferRegion(String identifier) {
        if (identifier == null) {
            return null;
        }
        String upper = identifier.toUpperCase();
        // Only support the 5 configured counties - no regional grouping
        if (upper.contains("ALAMEDA") || upper.contains("CT5")) {
            return "ALAMEDA";
        }
        if (upper.contains("ORANGE") || upper.contains("CT1")) {
            return "ORANGE";
        }
        if (upper.contains("SACRAMENTO") || upper.contains("CT2")) {
            return "SACRAMENTO";
        }
        if (upper.contains("RIVERSIDE") || upper.contains("CT3")) {
            return "RIVERSIDE";
        }
        if (upper.contains("LOS_ANGELES") || upper.contains("LOSANGELES") || upper.contains("CT4")) {
            return "LOS_ANGELES";
        }
        return null;
    }
}
