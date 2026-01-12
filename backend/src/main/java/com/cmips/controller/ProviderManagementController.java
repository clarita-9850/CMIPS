package com.cmips.controller;

import com.cmips.annotation.RequirePermission;
import com.cmips.entity.*;
import com.cmips.entity.ProviderEntity.ProviderStatus;
import com.cmips.repository.ProviderRepository;
import com.cmips.repository.ProviderAssignmentRepository;
import com.cmips.repository.OvertimeViolationRepository;
import com.cmips.service.ProviderManagementService;
import com.cmips.service.FieldLevelAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Provider Management REST Controller
 * All endpoints are protected by configurable Keycloak permissions
 */
@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProviderManagementController {

    private static final Logger log = LoggerFactory.getLogger(ProviderManagementController.class);

    private final ProviderManagementService providerService;
    private final ProviderRepository providerRepository;
    private final ProviderAssignmentRepository assignmentRepository;
    private final OvertimeViolationRepository violationRepository;
    private final FieldLevelAuthorizationService fieldAuthService;

    public ProviderManagementController(ProviderManagementService providerService,
                                        ProviderRepository providerRepository,
                                        ProviderAssignmentRepository assignmentRepository,
                                        OvertimeViolationRepository violationRepository,
                                        FieldLevelAuthorizationService fieldAuthService) {
        this.providerService = providerService;
        this.providerRepository = providerRepository;
        this.assignmentRepository = assignmentRepository;
        this.violationRepository = violationRepository;
        this.fieldAuthService = fieldAuthService;
    }

    // ==================== PROVIDER CRUD ====================

    @GetMapping
    @RequirePermission(resource = "Provider Resource", scope = "view")
    public ResponseEntity<java.util.Map<String, Object>> getAllProviders(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestParam(required = false) String countyCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<ProviderEntity> providerPage;

        if (countyCode != null && !countyCode.isEmpty()) {
            providerPage = providerRepository.findByDojCountyCode(countyCode, pageable);
        } else if (status != null && !status.isEmpty()) {
            providerPage = providerRepository.findByProviderStatus(ProviderStatus.valueOf(status), pageable);
        } else {
            providerPage = providerRepository.findAll(pageable);
        }

        // Return paginated response with metadata
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("content", providerPage.getContent());
        response.put("totalElements", providerPage.getTotalElements());
        response.put("totalPages", providerPage.getTotalPages());
        response.put("currentPage", providerPage.getNumber());
        response.put("pageSize", providerPage.getSize());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @RequirePermission(resource = "Provider Resource", scope = "view")
    public ResponseEntity<Map<String, Object>> getProviderById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        ProviderEntity provider = providerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        Map<String, Object> filteredProvider = fieldAuthService.filterFieldsForRole(provider, roles, "Provider Resource");
        return ResponseEntity.ok(filteredProvider);
    }

    @GetMapping("/search")
    @RequirePermission(resource = "Provider Resource", scope = "view")
    public ResponseEntity<List<Map<String, Object>>> searchProviders(
            @RequestParam(required = false) String providerNumber,
            @RequestParam(required = false) String ssn,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String countyCode,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        List<ProviderEntity> providers = providerRepository.searchProviders(
                providerNumber, ssn, lastName, firstName, countyCode);

        List<Map<String, Object>> filteredProviders = providers.stream()
                .map(p -> fieldAuthService.filterFieldsForRole(p, roles, "Provider Resource"))
                .toList();

        return ResponseEntity.ok(filteredProviders);
    }

    @PostMapping
    @RequirePermission(resource = "Provider Resource", scope = "create")
    public ResponseEntity<ProviderEntity> createProvider(
            @RequestBody ProviderEntity provider,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderEntity created = providerService.createProvider(provider, userId);
        return ResponseEntity.ok(created);
    }

    // ==================== ENROLLMENT MANAGEMENT ====================

    @PutMapping("/{id}/approve-enrollment")
    @RequirePermission(resource = "Provider Resource", scope = "approve")
    public ResponseEntity<ProviderEntity> approveEnrollment(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderEntity provider = providerService.approveEnrollment(id, userId);
        return ResponseEntity.ok(provider);
    }

    @PutMapping("/{id}/set-ineligible")
    @RequirePermission(resource = "Provider Resource", scope = "edit")
    public ResponseEntity<ProviderEntity> setProviderIneligible(
            @PathVariable Long id,
            @RequestBody IneligibleRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderEntity provider = providerService.setProviderIneligible(id, request.getReason(), userId);
        return ResponseEntity.ok(provider);
    }

    @PutMapping("/{id}/reinstate")
    @RequirePermission(resource = "Provider Resource", scope = "reinstate")
    public ResponseEntity<ProviderEntity> reinstateProvider(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderEntity provider = providerService.reinstateProvider(id, userId);
        return ResponseEntity.ok(provider);
    }

    @PutMapping("/{id}/re-enroll")
    @RequirePermission(resource = "Provider Resource", scope = "enroll")
    public ResponseEntity<ProviderEntity> reEnrollProvider(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderEntity provider = providerService.reEnrollProvider(id, userId);
        return ResponseEntity.ok(provider);
    }

    // ==================== PROVIDER ASSIGNMENTS ====================

    @GetMapping("/{id}/assignments")
    @RequirePermission(resource = "Provider Assignment Resource", scope = "view")
    public ResponseEntity<List<ProviderAssignmentEntity>> getProviderAssignments(@PathVariable Long id) {
        List<ProviderAssignmentEntity> assignments = assignmentRepository.findByProviderId(id);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/assignments")
    @RequirePermission(resource = "Provider Assignment Resource", scope = "create")
    public ResponseEntity<ProviderAssignmentEntity> assignProviderToCase(
            @RequestBody AssignmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderAssignmentEntity assignment = providerService.assignProviderToCase(
                request.getProviderId(),
                request.getCaseId(),
                request.getProviderType(),
                request.getRelationship(),
                request.getAssignedHours(),
                userId);

        return ResponseEntity.ok(assignment);
    }

    @PutMapping("/assignments/{id}/terminate")
    @RequirePermission(resource = "Provider Assignment Resource", scope = "terminate")
    public ResponseEntity<ProviderAssignmentEntity> terminateAssignment(
            @PathVariable Long id,
            @RequestBody TerminationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderAssignmentEntity assignment = providerService.terminateProviderAssignment(id, request.getReason(), userId);
        return ResponseEntity.ok(assignment);
    }

    @PutMapping("/assignments/{id}/leave")
    @RequirePermission(resource = "Provider Assignment Resource", scope = "edit")
    public ResponseEntity<ProviderAssignmentEntity> placeOnLeave(
            @PathVariable Long id,
            @RequestBody LeaveRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderAssignmentEntity assignment = providerService.placeProviderOnLeave(id, request.getReason(), userId);
        return ResponseEntity.ok(assignment);
    }

    // ==================== RECIPIENT WAIVER ====================

    @PostMapping("/assignments/{id}/recipient-waiver")
    @RequirePermission(resource = "Provider Assignment Resource", scope = "edit")
    public ResponseEntity<ProviderAssignmentEntity> addRecipientWaiver(
            @PathVariable Long id,
            @RequestBody RecipientWaiverRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderAssignmentEntity assignment = providerService.addRecipientWaiver(
                id, request.getCoriId(), request.getEndDate(), userId);
        return ResponseEntity.ok(assignment);
    }

    // ==================== CORI MANAGEMENT ====================

    @GetMapping("/{id}/cori")
    @RequirePermission(resource = "Provider CORI Resource", scope = "view")
    public ResponseEntity<List<ProviderCoriEntity>> getProviderCori(@PathVariable Long id) {
        List<ProviderCoriEntity> coriRecords = providerService.getProviderCoriRecords(id);
        return ResponseEntity.ok(coriRecords);
    }

    @PostMapping("/{id}/cori")
    @RequirePermission(resource = "Provider CORI Resource", scope = "create")
    public ResponseEntity<ProviderCoriEntity> createCori(
            @PathVariable Long id,
            @RequestBody ProviderCoriEntity cori,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderCoriEntity created = providerService.createCori(id, cori, userId);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/cori/{coriId}/general-exception")
    @RequirePermission(resource = "Provider CORI Resource", scope = "edit")
    public ResponseEntity<ProviderCoriEntity> addGeneralException(
            @PathVariable Long coriId,
            @RequestBody GeneralExceptionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        ProviderCoriEntity cori = providerService.addGeneralException(
                coriId, request.getBeginDate(), request.getEndDate(), request.getNotes(), userId);
        return ResponseEntity.ok(cori);
    }

    // ==================== OVERTIME VIOLATIONS ====================

    @GetMapping("/{id}/violations")
    @RequirePermission(resource = "Overtime Violation Resource", scope = "view")
    public ResponseEntity<List<OvertimeViolationEntity>> getProviderViolations(@PathVariable Long id) {
        List<OvertimeViolationEntity> violations = violationRepository.findByProviderId(id);
        return ResponseEntity.ok(violations);
    }

    @PostMapping("/{id}/violations")
    @RequirePermission(resource = "Overtime Violation Resource", scope = "create")
    public ResponseEntity<OvertimeViolationEntity> createViolation(
            @PathVariable Long id,
            @RequestBody ViolationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        OvertimeViolationEntity violation = providerService.createOvertimeViolation(
                id, request.getViolationType(), request.getMonth(), request.getYear(),
                request.getHoursClaimed(), request.getMaximumAllowed(), userId);

        return ResponseEntity.ok(violation);
    }

    @PutMapping("/violations/{violationId}/county-review")
    @RequirePermission(resource = "Overtime Violation Resource", scope = "review")
    public ResponseEntity<OvertimeViolationEntity> countyReview(
            @PathVariable Long violationId,
            @RequestBody CountyReviewRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String reviewerId) {

        OvertimeViolationEntity violation = providerService.countyReviewViolation(
                violationId, request.getOutcome(), request.getComments(), reviewerId);
        return ResponseEntity.ok(violation);
    }

    @PutMapping("/violations/{violationId}/supervisor-review")
    @RequirePermission(resource = "Overtime Violation Resource", scope = "supervisor-review")
    public ResponseEntity<OvertimeViolationEntity> supervisorReview(
            @PathVariable Long violationId,
            @RequestBody SupervisorReviewRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String reviewerId) {

        OvertimeViolationEntity violation = providerService.supervisorReviewViolation(
                violationId, request.getOutcome(), request.getComments(), reviewerId);
        return ResponseEntity.ok(violation);
    }

    @PutMapping("/violations/{violationId}/training-completed")
    @RequirePermission(resource = "Overtime Violation Resource", scope = "edit")
    public ResponseEntity<OvertimeViolationEntity> recordTrainingCompletion(
            @PathVariable Long violationId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        OvertimeViolationEntity violation = providerService.recordTrainingCompletion(violationId, userId);
        return ResponseEntity.ok(violation);
    }

    @GetMapping("/violations/pending-review")
    @RequirePermission(resource = "Overtime Violation Resource", scope = "view")
    public ResponseEntity<List<OvertimeViolationEntity>> getPendingReviewViolations() {
        List<OvertimeViolationEntity> violations = violationRepository.findPendingReviewViolations();
        return ResponseEntity.ok(violations);
    }

    @GetMapping("/violations/pending-supervisor-review")
    @RequirePermission(resource = "Overtime Violation Resource", scope = "view")
    public ResponseEntity<List<OvertimeViolationEntity>> getPendingSupervisorReviewViolations() {
        List<OvertimeViolationEntity> violations = violationRepository.findViolationsWithPendingSupervisorReview();
        return ResponseEntity.ok(violations);
    }

    // ==================== SICK LEAVE ====================

    @PutMapping("/{id}/check-sick-leave-accrual")
    @RequirePermission(resource = "Provider Resource", scope = "edit")
    public ResponseEntity<Void> checkSickLeaveAccrual(@PathVariable Long id) {
        providerService.checkAndAccrueSickLeave(id);
        return ResponseEntity.ok().build();
    }

    // ==================== ELIGIBLE PROVIDERS ====================

    @GetMapping("/eligible")
    @RequirePermission(resource = "Provider Resource", scope = "view")
    public ResponseEntity<List<ProviderEntity>> getEligibleProviders(
            @RequestParam(required = false) String countyCode) {

        List<ProviderEntity> providers;
        if (countyCode != null) {
            providers = providerRepository.findEligibleProvidersByCounty(countyCode);
        } else {
            providers = providerRepository.findByEligible("YES");
        }
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/pending-reinstatement")
    @RequirePermission(resource = "Provider Resource", scope = "view")
    public ResponseEntity<List<ProviderEntity>> getProvidersEligibleForReinstatement() {
        LocalDate cutoffDate = LocalDate.now().minusDays(30);
        List<ProviderEntity> providers = providerRepository.findProvidersEligibleForReinstatement(cutoffDate);
        return ResponseEntity.ok(providers);
    }

    // ==================== REQUEST DTOs ====================

    @lombok.Data
    public static class IneligibleRequest {
        private String reason;
    }

    @lombok.Data
    public static class AssignmentRequest {
        private Long providerId;
        private Long caseId;
        private String providerType;
        private String relationship;
        private Double assignedHours;
    }

    @lombok.Data
    public static class TerminationRequest {
        private String reason;
    }

    @lombok.Data
    public static class LeaveRequest {
        private String reason;
    }

    @lombok.Data
    public static class RecipientWaiverRequest {
        private Long coriId;
        private LocalDate endDate;
    }

    @lombok.Data
    public static class GeneralExceptionRequest {
        private LocalDate beginDate;
        private LocalDate endDate;
        private String notes;
    }

    @lombok.Data
    public static class ViolationRequest {
        private String violationType;
        private Integer month;
        private Integer year;
        private Double hoursClaimed;
        private Double maximumAllowed;
    }

    @lombok.Data
    public static class CountyReviewRequest {
        private String outcome;
        private String comments;
    }

    @lombok.Data
    public static class SupervisorReviewRequest {
        private String outcome;
        private String comments;
    }
}
