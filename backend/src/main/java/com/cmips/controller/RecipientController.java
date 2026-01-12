package com.cmips.controller;

import com.cmips.annotation.RequirePermission;
import com.cmips.entity.*;
import com.cmips.entity.RecipientEntity.PersonType;
import com.cmips.repository.RecipientRepository;
import com.cmips.service.CaseManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Recipient/Person Management REST Controller
 * Implements DSD Section 20 - Online Search Business Rules
 * All endpoints are protected by configurable Keycloak permissions
 */
@RestController
@RequestMapping("/api/recipients")
@CrossOrigin(origins = "*")
public class RecipientController {

    private static final Logger log = LoggerFactory.getLogger(RecipientController.class);

    private final RecipientRepository recipientRepository;
    private final CaseManagementService caseManagementService;

    public RecipientController(RecipientRepository recipientRepository,
                               CaseManagementService caseManagementService) {
        this.recipientRepository = recipientRepository;
        this.caseManagementService = caseManagementService;
    }

    // ==================== PERSON SEARCH (BR OS 01-10) ====================

    @GetMapping("/search")
    @RequirePermission(resource = "Recipient Resource", scope = "view")
    public ResponseEntity<List<RecipientEntity>> searchPersons(
            @RequestParam(required = false) String ssn,
            @RequestParam(required = false) String cin,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String countyCode,
            @RequestParam(required = false) String personType) {

        List<RecipientEntity> recipients;

        if (ssn != null) {
            // BR OS 02: SSN exact match
            recipients = recipientRepository.findBySsn(ssn)
                    .map(List::of)
                    .orElse(List.of());
        } else if (cin != null) {
            // BR OS 03: CIN exact match
            recipients = recipientRepository.findByCin(cin)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            // BR OS 05: Name/county search
            recipients = recipientRepository.searchRecipients(null, null, lastName, firstName, countyCode, personType);
        }

        return ResponseEntity.ok(recipients);
    }

    @GetMapping("/{id}")
    @RequirePermission(resource = "Recipient Resource", scope = "view")
    public ResponseEntity<RecipientEntity> getRecipientById(@PathVariable Long id) {
        RecipientEntity recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
        return ResponseEntity.ok(recipient);
    }

    @GetMapping
    @RequirePermission(resource = "Recipient Resource", scope = "view")
    public ResponseEntity<java.util.Map<String, Object>> getAllRecipients(
            @RequestParam(required = false) String personType,
            @RequestParam(required = false) String countyCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<RecipientEntity> recipientPage;

        if (personType != null && !personType.isEmpty() && countyCode != null && !countyCode.isEmpty()) {
            recipientPage = recipientRepository.findByResidenceCountyAndPersonType(countyCode, PersonType.valueOf(personType), pageable);
        } else if (personType != null && !personType.isEmpty()) {
            recipientPage = recipientRepository.findByPersonType(PersonType.valueOf(personType), pageable);
        } else if (countyCode != null && !countyCode.isEmpty()) {
            recipientPage = recipientRepository.findByResidenceCounty(countyCode, pageable);
        } else {
            recipientPage = recipientRepository.findAll(pageable);
        }

        // Return paginated response with metadata
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("content", recipientPage.getContent());
        response.put("totalElements", recipientPage.getTotalElements());
        response.put("totalPages", recipientPage.getTotalPages());
        response.put("currentPage", recipientPage.getNumber());
        response.put("pageSize", recipientPage.getSize());

        return ResponseEntity.ok(response);
    }

    // ==================== REFERRAL MANAGEMENT (BR OS 11-19, 42) ====================

    @PostMapping("/referrals")
    @RequirePermission(resource = "Referral Resource", scope = "create")
    public ResponseEntity<RecipientEntity> createReferral(
            @RequestBody RecipientEntity recipient,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // BR OS 11: Create new referral with OPEN_REFERRAL status
        RecipientEntity created = caseManagementService.createReferral(recipient, userId);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/referrals/{id}/close")
    @RequirePermission(resource = "Referral Resource", scope = "close")
    public ResponseEntity<RecipientEntity> closeReferral(
            @PathVariable Long id,
            @RequestBody CloseReferralRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // BR OS 14: Close referral with reason
        RecipientEntity recipient = caseManagementService.closeReferral(id, request.getReason(), userId);
        return ResponseEntity.ok(recipient);
    }

    @PutMapping("/referrals/{id}/reopen")
    @RequirePermission(resource = "Referral Resource", scope = "reopen")
    public ResponseEntity<RecipientEntity> reopenReferral(
            @PathVariable Long id,
            @RequestBody ReopenReferralRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        // BR OS 42: Reopen closed referral (within 30 days)
        RecipientEntity recipient = caseManagementService.reopenReferral(
                id, request.getReferralSource(), request.getCountyCode(), userId);
        return ResponseEntity.ok(recipient);
    }

    // ==================== RECIPIENT CRUD ====================

    @PostMapping
    @RequirePermission(resource = "Recipient Resource", scope = "create")
    public ResponseEntity<RecipientEntity> createRecipient(
            @RequestBody RecipientEntity recipient,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        recipient.setCreatedBy(userId);
        RecipientEntity saved = recipientRepository.save(recipient);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @RequirePermission(resource = "Recipient Resource", scope = "edit")
    public ResponseEntity<RecipientEntity> updateRecipient(
            @PathVariable Long id,
            @RequestBody RecipientEntity updates,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        RecipientEntity existing = recipientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        // Update allowed fields
        if (updates.getFirstName() != null) existing.setFirstName(updates.getFirstName());
        if (updates.getLastName() != null) existing.setLastName(updates.getLastName());
        if (updates.getMiddleName() != null) existing.setMiddleName(updates.getMiddleName());
        if (updates.getDateOfBirth() != null) existing.setDateOfBirth(updates.getDateOfBirth());
        if (updates.getGender() != null) existing.setGender(updates.getGender());
        if (updates.getResidenceCity() != null) existing.setResidenceCity(updates.getResidenceCity());
        if (updates.getResidenceState() != null) existing.setResidenceState(updates.getResidenceState());
        if (updates.getResidenceZip() != null) existing.setResidenceZip(updates.getResidenceZip());
        if (updates.getMailingCity() != null) existing.setMailingCity(updates.getMailingCity());
        if (updates.getMailingState() != null) existing.setMailingState(updates.getMailingState());
        if (updates.getMailingZip() != null) existing.setMailingZip(updates.getMailingZip());
        if (updates.getPrimaryPhone() != null) existing.setPrimaryPhone(updates.getPrimaryPhone());
        if (updates.getSecondaryPhone() != null) existing.setSecondaryPhone(updates.getSecondaryPhone());
        if (updates.getEmail() != null) existing.setEmail(updates.getEmail());
        if (updates.getSpokenLanguage() != null) existing.setSpokenLanguage(updates.getSpokenLanguage());
        if (updates.getWrittenLanguage() != null) existing.setWrittenLanguage(updates.getWrittenLanguage());

        existing.setUpdatedBy(userId);
        RecipientEntity saved = recipientRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    // ==================== ADDRESS MANAGEMENT ====================

    @PutMapping("/{id}/address")
    @RequirePermission(resource = "Recipient Resource", scope = "edit")
    public ResponseEntity<RecipientEntity> updateAddress(
            @PathVariable Long id,
            @RequestBody AddressUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        RecipientEntity recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        if ("RESIDENCE".equals(request.getAddressType())) {
            recipient.setResidenceStreetName(request.getStreetName());
            recipient.setResidenceStreetNumber(request.getStreetNumber());
            recipient.setResidenceCity(request.getCity());
            recipient.setResidenceState(request.getState());
            recipient.setResidenceZip(request.getZipCode());
        } else if ("MAILING".equals(request.getAddressType())) {
            recipient.setMailingStreetName(request.getStreetName());
            recipient.setMailingCity(request.getCity());
            recipient.setMailingState(request.getState());
            recipient.setMailingZip(request.getZipCode());
        }

        recipient.setUpdatedBy(userId);
        RecipientEntity saved = recipientRepository.save(recipient);
        return ResponseEntity.ok(saved);
    }

    // ==================== ESP REGISTRATION (BR OS 35-38) ====================

    @PutMapping("/{id}/esp-registration")
    @RequirePermission(resource = "Recipient Resource", scope = "edit")
    public ResponseEntity<RecipientEntity> updateEspRegistration(
            @PathVariable Long id,
            @RequestBody EspRegistrationRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        RecipientEntity recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        // BR OS 35-38: ESP registration for electronic timesheet submission
        recipient.setEspRegistered(request.getEspRegistered());
        recipient.setUpdatedBy(userId);

        RecipientEntity saved = recipientRepository.save(recipient);
        return ResponseEntity.ok(saved);
    }

    // ==================== ACCESSIBILITY OPTIONS ====================

    @PutMapping("/{id}/accessibility")
    @RequirePermission(resource = "Recipient Resource", scope = "edit")
    public ResponseEntity<RecipientEntity> updateAccessibilityOptions(
            @PathVariable Long id,
            @RequestBody AccessibilityRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        RecipientEntity recipient = recipientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        // Accessibility options - using NOA option
        if (request.getLargeFont() != null && request.getLargeFont()) {
            recipient.setNoaOption("LARGE_FONT");
        } else {
            recipient.setNoaOption("STANDARD");
        }
        if (request.getScreenReader() != null) {
            recipient.setBlindVisuallyImpaired(request.getScreenReader());
        }
        recipient.setUpdatedBy(userId);

        RecipientEntity saved = recipientRepository.save(recipient);
        return ResponseEntity.ok(saved);
    }

    // ==================== COMPANION CASES (BR SE 26-27) ====================

    @GetMapping("/{id}/companion-cases")
    @RequirePermission(resource = "Recipient Resource", scope = "view")
    public ResponseEntity<List<RecipientEntity>> findCompanionCases(@PathVariable Long id) {
        List<RecipientEntity> companions = caseManagementService.findCompanionCases(id);
        return ResponseEntity.ok(companions);
    }

    // ==================== OPEN/CLOSED REFERRALS BY COUNTY ====================

    @GetMapping("/referrals/open")
    @RequirePermission(resource = "Referral Resource", scope = "view")
    public ResponseEntity<List<RecipientEntity>> getOpenReferrals(
            @RequestParam(required = false) String countyCode) {

        List<RecipientEntity> referrals;
        if (countyCode != null) {
            referrals = recipientRepository.findByCountyCodeAndPersonType(countyCode, PersonType.OPEN_REFERRAL);
        } else {
            referrals = recipientRepository.findByPersonType(PersonType.OPEN_REFERRAL);
        }
        return ResponseEntity.ok(referrals);
    }

    @GetMapping("/referrals/closed")
    @RequirePermission(resource = "Referral Resource", scope = "view")
    public ResponseEntity<List<RecipientEntity>> getClosedReferrals(
            @RequestParam(required = false) String countyCode) {

        List<RecipientEntity> referrals;
        if (countyCode != null) {
            referrals = recipientRepository.findByCountyCodeAndPersonType(countyCode, PersonType.CLOSED_REFERRAL);
        } else {
            referrals = recipientRepository.findByPersonType(PersonType.CLOSED_REFERRAL);
        }
        return ResponseEntity.ok(referrals);
    }

    // ==================== REQUEST DTOs ====================

    public static class CloseReferralRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ReopenReferralRequest {
        private String referralSource;
        private String countyCode;

        public String getReferralSource() { return referralSource; }
        public void setReferralSource(String referralSource) { this.referralSource = referralSource; }

        public String getCountyCode() { return countyCode; }
        public void setCountyCode(String countyCode) { this.countyCode = countyCode; }
    }

    public static class AddressUpdateRequest {
        private String addressType; // RESIDENCE or MAILING
        private String streetNumber;
        private String streetName;
        private String city;
        private String state;
        private String zipCode;

        public String getAddressType() { return addressType; }
        public void setAddressType(String addressType) { this.addressType = addressType; }

        public String getStreetNumber() { return streetNumber; }
        public void setStreetNumber(String streetNumber) { this.streetNumber = streetNumber; }

        public String getStreetName() { return streetName; }
        public void setStreetName(String streetName) { this.streetName = streetName; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    }

    public static class EspRegistrationRequest {
        private Boolean espRegistered;

        public Boolean getEspRegistered() { return espRegistered; }
        public void setEspRegistered(Boolean espRegistered) { this.espRegistered = espRegistered; }
    }

    public static class AccessibilityRequest {
        private Boolean largeFont;
        private Boolean highContrast;
        private Boolean screenReader;

        public Boolean getLargeFont() { return largeFont; }
        public void setLargeFont(Boolean largeFont) { this.largeFont = largeFont; }

        public Boolean getHighContrast() { return highContrast; }
        public void setHighContrast(Boolean highContrast) { this.highContrast = highContrast; }

        public Boolean getScreenReader() { return screenReader; }
        public void setScreenReader(Boolean screenReader) { this.screenReader = screenReader; }
    }
}
