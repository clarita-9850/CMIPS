package com.cmips.controller;

import com.cmips.entity.Timesheet;
import com.cmips.entity.TimesheetStatus;
import com.cmips.repository.TimesheetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    @Autowired
    private TimesheetRepository timesheetRepository;

    /**
     * Get real-time metrics for dashboard
     */
    @GetMapping("/realtime-metrics")
    public ResponseEntity<Map<String, Object>> getRealTimeMetrics(
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priorityLevel,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String startYear,
            @RequestParam(required = false) String endYear) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            LocalDateTime endOfToday = LocalDate.now().atTime(23, 59, 59);
            LocalDate startOfWeek = LocalDate.now().minusDays(7);
            
            // Get all timesheets for filtering
            List<Timesheet> allTimesheets = timesheetRepository.findAll();
            
            // Apply filters
            List<Timesheet> filtered = allTimesheets;
            if (county != null && !county.isEmpty() && !"all".equalsIgnoreCase(county)) {
                filtered = filtered.stream()
                    .filter(t -> county.equalsIgnoreCase(t.getLocation()))
                    .collect(Collectors.toList());
            }
            if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
                try {
                    TimesheetStatus statusEnum = TimesheetStatus.valueOf(status.toUpperCase());
                    filtered = filtered.stream()
                        .filter(t -> t.getStatus() == statusEnum)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore filter
                }
            }
            
            // Total timesheets
            long totalTimesheets = filtered.size();
            
            // Timesheets created today
            long totalTimesheetsToday = filtered.stream()
                .filter(t -> t.getCreatedAt() != null && 
                    t.getCreatedAt().isAfter(startOfToday) && 
                    t.getCreatedAt().isBefore(endOfToday))
                .count();
            
            // Pending approvals (SUBMITTED status)
            long pendingApprovals = filtered.stream()
                .filter(t -> t.getStatus() == TimesheetStatus.SUBMITTED || 
                            t.getStatus() == TimesheetStatus.REVISION_REQUESTED)
                .count();
            
            // Distinct employees
            long distinctEmployees = filtered.stream()
                .map(Timesheet::getEmployeeId)
                .distinct()
                .count();
            
            // Distinct users
            long distinctUsers = filtered.stream()
                .map(Timesheet::getUserId)
                .distinct()
                .count();
            
            // Approved timesheets today
            List<Timesheet> approvedToday = filtered.stream()
                .filter(t -> t.getStatus() == TimesheetStatus.APPROVED && 
                            t.getApprovedAt() != null &&
                            t.getApprovedAt().isAfter(startOfToday) &&
                            t.getApprovedAt().isBefore(endOfToday))
                .collect(Collectors.toList());
            
            // Calculate average approval time
            double avgApprovalTimeHours = filtered.stream()
                .filter(t -> t.getStatus() == TimesheetStatus.APPROVED && 
                            t.getSubmittedAt() != null && 
                            t.getApprovedAt() != null)
                .mapToLong(t -> ChronoUnit.HOURS.between(t.getSubmittedAt(), t.getApprovedAt()))
                .average()
                .orElse(0.0);
            
            // Build response
            metrics.put("totalTimesheetsToday", totalTimesheetsToday);
            metrics.put("totalTimesheets", totalTimesheets);
            metrics.put("pendingApprovals", pendingApprovals);
            metrics.put("totalParticipants", distinctUsers);
            metrics.put("distinctEmployees", distinctEmployees);
            metrics.put("distinctProviders", distinctEmployees);
            metrics.put("distinctRecipients", distinctUsers);
            metrics.put("totalApprovedAmountToday", 0.0); // Not available in current schema
            metrics.put("totalApprovedAmountThisWeek", 0.0); // Not available in current schema
            metrics.put("avgApprovalTimeHours", avgApprovalTimeHours);
            metrics.put("approvedToday", approvedToday.size());
            metrics.put("lastUpdated", LocalDateTime.now());
            metrics.put("status", "SUCCESS");
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            log.error("Error getting real-time metrics", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            error.put("lastUpdated", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Get filter options
     */
    @GetMapping("/filters")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Timesheet> allTimesheets = timesheetRepository.findAll();

            // Get unique locations (counties)
            List<String> counties = allTimesheets.stream()
                .map(Timesheet::getLocation)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

            // Get unique departments
            List<String> departments = allTimesheets.stream()
                .map(Timesheet::getDepartment)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

            // Get statuses
            List<String> statuses = new ArrayList<>();
            for (TimesheetStatus status : TimesheetStatus.values()) {
                statuses.add(status.name());
            }

            response.put("districts", new ArrayList<>());
            response.put("counties", counties);
            response.put("departments", departments);
            response.put("statuses", statuses);
            response.put("priorityLevels", new ArrayList<>());
            response.put("serviceTypes", new ArrayList<>());
            response.put("status", "SUCCESS");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting filter options", e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get adhoc filter options for analytics dashboard
     * Returns locations (county codes) that match what's in the database
     */
    @GetMapping("/adhoc-filters")
    public ResponseEntity<Map<String, Object>> getAdhocFilterOptions() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Timesheet> allTimesheets = timesheetRepository.findAll();

            // Get unique locations (county codes CT1-CT5)
            List<String> locations = allTimesheets.stream()
                .map(Timesheet::getLocation)
                .filter(loc -> loc != null && !loc.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

            // Get unique departments
            List<String> departments = allTimesheets.stream()
                .map(Timesheet::getDepartment)
                .filter(dept -> dept != null && !dept.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

            // Get statuses
            List<String> statuses = new ArrayList<>();
            for (TimesheetStatus status : TimesheetStatus.values()) {
                statuses.add(status.name());
            }

            response.put("locations", locations);
            response.put("departments", departments);
            response.put("statuses", statuses);
            // Demographic filters (placeholders for now)
            response.put("providerGenders", List.of("Male", "Female", "Other"));
            response.put("recipientGenders", List.of("Male", "Female", "Other"));
            response.put("providerEthnicities", List.of("Hispanic", "White", "Black", "Asian", "Other"));
            response.put("recipientEthnicities", List.of("Hispanic", "White", "Black", "Asian", "Other"));
            response.put("providerAgeGroups", List.of("18-25", "26-35", "36-45", "46-55", "56-65", "65+"));
            response.put("recipientAgeGroups", List.of("0-17", "18-25", "26-35", "36-45", "46-55", "56-65", "65+"));
            response.put("status", "SUCCESS");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting adhoc filter options", e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get adhoc statistics
     */
    @GetMapping("/adhoc-stats")
    public ResponseEntity<Map<String, Object>> getAdhocStats(
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Timesheet> filtered = timesheetRepository.findAll();

            // Apply filters
            if (county != null && !county.isEmpty() && !"(All)".equals(county)) {
                filtered = filtered.stream()
                    .filter(t -> county.equalsIgnoreCase(t.getLocation()))
                    .collect(Collectors.toList());
            }
            if (department != null && !department.isEmpty() && !"(All)".equals(department)) {
                filtered = filtered.stream()
                    .filter(t -> department.equalsIgnoreCase(t.getDepartment()))
                    .collect(Collectors.toList());
            }
            if (status != null && !status.isEmpty() && !"(All)".equals(status)) {
                try {
                    TimesheetStatus statusEnum = TimesheetStatus.valueOf(status.toUpperCase());
                    filtered = filtered.stream()
                        .filter(t -> t.getStatus() == statusEnum)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }

            double totalHours = filtered.stream()
                .mapToDouble(t -> t.getTotalHours() != null ? t.getTotalHours().doubleValue() : 0.0)
                .sum();
            double avgHours = filtered.isEmpty() ? 0.0 : totalHours / filtered.size();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalRecords", filtered.size());
            stats.put("totalHours", totalHours);
            stats.put("totalAmount", 0.0);
            stats.put("avgHours", avgHours);
            stats.put("avgAmount", 0.0);

            response.put("status", "SUCCESS");
            response.put("stats", stats);
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting adhoc stats", e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get adhoc data rows
     */
    @GetMapping("/adhoc-data")
    public ResponseEntity<Map<String, Object>> getAdhocData(
            @RequestParam(required = false) String county,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1000") int limit) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Timesheet> filtered = timesheetRepository.findAll();

            // Apply filters
            if (county != null && !county.isEmpty() && !"(All)".equals(county)) {
                filtered = filtered.stream()
                    .filter(t -> county.equalsIgnoreCase(t.getLocation()))
                    .collect(Collectors.toList());
            }
            if (department != null && !department.isEmpty() && !"(All)".equals(department)) {
                filtered = filtered.stream()
                    .filter(t -> department.equalsIgnoreCase(t.getDepartment()))
                    .collect(Collectors.toList());
            }
            if (status != null && !status.isEmpty() && !"(All)".equals(status)) {
                try {
                    TimesheetStatus statusEnum = TimesheetStatus.valueOf(status.toUpperCase());
                    filtered = filtered.stream()
                        .filter(t -> t.getStatus() == statusEnum)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore
                }
            }

            // Limit results
            List<Timesheet> limited = filtered.stream().limit(limit).collect(Collectors.toList());

            // Convert to rows
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Timesheet t : limited) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", t.getId());
                row.put("employeeId", t.getEmployeeId());
                row.put("employeeName", t.getEmployeeName());
                row.put("location", t.getLocation());
                row.put("department", t.getDepartment());
                row.put("status", t.getStatus() != null ? t.getStatus().name() : null);
                row.put("totalHours", t.getTotalHours());
                row.put("regularHours", t.getRegularHours());
                row.put("overtimeHours", t.getOvertimeHours());
                row.put("payPeriodStart", t.getPayPeriodStart());
                row.put("payPeriodEnd", t.getPayPeriodEnd());
                rows.add(row);
            }

            List<String> columns = List.of("id", "employeeId", "employeeName", "location",
                "department", "status", "totalHours", "regularHours", "overtimeHours",
                "payPeriodStart", "payPeriodEnd");

            response.put("status", "SUCCESS");
            response.put("columns", columns);
            response.put("rows", rows);
            response.put("count", rows.size());
            response.put("limit", limit);
            response.put("lastUpdated", LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting adhoc data", e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get demographic data by gender
     */
    @GetMapping("/demographics/gender")
    public ResponseEntity<Map<String, Object>> getDemographicsByGender(
            @RequestParam(required = false) String county) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Placeholder - demographic fields not in current schema
            Map<String, Long> provider = new HashMap<>();
            Map<String, Long> recipient = new HashMap<>();
            
            response.put("provider", provider);
            response.put("recipient", recipient);
            response.put("status", "SUCCESS");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting demographics by gender", e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get demographic data by ethnicity
     */
    @GetMapping("/demographics/ethnicity")
    public ResponseEntity<Map<String, Object>> getDemographicsByEthnicity(
            @RequestParam(required = false) String districtId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Placeholder - demographic fields not in current schema
            Map<String, Long> provider = new HashMap<>();
            Map<String, Long> recipient = new HashMap<>();
            
            response.put("provider", provider);
            response.put("recipient", recipient);
            response.put("status", "SUCCESS");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting demographics by ethnicity", e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Get demographic data by age
     */
    @GetMapping("/demographics/age")
    public ResponseEntity<Map<String, Object>> getDemographicsByAge(
            @RequestParam(required = false) String county) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Placeholder - demographic fields not in current schema
            Map<String, Long> provider = new HashMap<>();
            Map<String, Long> recipient = new HashMap<>();
            
            response.put("provider", provider);
            response.put("recipient", recipient);
            response.put("status", "SUCCESS");
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting demographics by age", e);
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Health check for analytics endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "analytics");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }
    
    private String normalizeParam(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "all".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }
}

