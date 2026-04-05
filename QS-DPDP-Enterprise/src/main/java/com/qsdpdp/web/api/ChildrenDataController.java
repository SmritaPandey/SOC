package com.qsdpdp.web.api;

import com.qsdpdp.children.ChildrenDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Children's Data Protection REST Controller — DPDP Act §9/§33
 * Provides REST API for age verification, parental consent,
 * and children's data processing governance.
 *
 * @version 1.0.0
 * @since Phase 4
 */
@RestController("childrenDataApiController")
@RequestMapping("/api/v1/children")
public class ChildrenDataController {

    @Autowired(required = false)
    private ChildrenDataService childrenDataService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "children-data-protection",
                "status", "UP",
                "version", "1.0.0",
                "ageOfMajority", 18,
                "dpdpSections", "§9, §33",
                "initialized", childrenDataService != null && childrenDataService.isInitialized()));
    }

    // ═══════════════════════════════════════════════════════════
    // AGE VERIFICATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Verify age and determine if parental consent is required.
     * Body: { "principalId": "...", "declaredAge": 15, "dateOfBirth": "2010-01-15", "verificationMethod": "SELF_DECLARATION" }
     */
    @PostMapping("/verify-age")
    public ResponseEntity<Object> verifyAge(@RequestBody Map<String, Object> body) {
        try {
            String principalId = (String) body.get("principalId");
            int declaredAge = body.containsKey("declaredAge") ? ((Number) body.get("declaredAge")).intValue() : 0;
            String dateOfBirth = (String) body.getOrDefault("dateOfBirth", "");
            String verificationMethod = (String) body.getOrDefault("verificationMethod", "SELF_DECLARATION");

            if (principalId == null || principalId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "principalId is required"));
            }

            var result = childrenDataService.verifyAge(principalId, declaredAge, dateOfBirth, verificationMethod);

            return ResponseEntity.ok(Map.of(
                    "principalId", principalId,
                    "isChild", result.isChild(),
                    "parentalConsentRequired", result.isParentalConsentRequired(),
                    "message", result.getMessage(),
                    "nextSteps", result.getNextSteps() != null ? result.getNextSteps() : List.of(),
                    "dpdpSection", "§33"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PARENTAL/GUARDIAN CONSENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Record parental consent for a child's data processing.
     */
    @PostMapping("/guardian-consent")
    public ResponseEntity<Map<String, Object>> recordGuardianConsent(@RequestBody Map<String, String> body) {
        try {
            String childPrincipalId = body.get("childPrincipalId");
            String guardianPrincipalId = body.get("guardianPrincipalId");
            String guardianRelationship = body.getOrDefault("guardianRelationship", "PARENT");
            String verificationMethod = body.getOrDefault("verificationMethod", "AADHAAR_OTP");
            String consentScope = body.getOrDefault("consentScope", "FULL");
            String purposes = body.getOrDefault("purposes", "EDUCATION");
            String dateOfBirth = body.getOrDefault("dateOfBirth", "");

            if (childPrincipalId == null || guardianPrincipalId == null) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "childPrincipalId and guardianPrincipalId are required"));
            }

            String id = childrenDataService.recordParentalConsent(
                    childPrincipalId, guardianPrincipalId, guardianRelationship,
                    verificationMethod, consentScope, purposes, dateOfBirth);

            if (id == null) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to record parental consent"));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "consentId", id,
                    "childPrincipalId", childPrincipalId,
                    "guardianPrincipalId", guardianPrincipalId,
                    "status", "PENDING",
                    "message", "Parental consent recorded. Verification required per DPDP §33.",
                    "dpdpSection", "§33"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verify parental consent (e.g., after OTP/email verification).
     */
    @PostMapping("/guardian-consent/{consentId}/verify")
    public ResponseEntity<Map<String, Object>> verifyGuardianConsent(
            @PathVariable String consentId,
            @RequestBody Map<String, String> body) {
        try {
            String verifiedBy = body.getOrDefault("verifiedBy", "SYSTEM");
            boolean verified = childrenDataService.verifyParentalConsent(consentId, verifiedBy);

            return ResponseEntity.ok(Map.of(
                    "consentId", consentId,
                    "verified", verified,
                    "status", verified ? "ACTIVE" : "VERIFICATION_FAILED",
                    "message", verified
                            ? "Parental consent verified and activated per DPDP §33"
                            : "Verification failed — consent remains in PENDING state"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PROCESSING DECISIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Check if processing is allowed for a data principal.
     * Enforces DPDP §33 prohibitions on tracking and advertising.
     */
    @GetMapping("/can-process")
    public ResponseEntity<Object> canProcess(
            @RequestParam String principalId,
            @RequestParam(defaultValue = "GENERAL") String processingType) {
        try {
            var decision = childrenDataService.canProcess(principalId, processingType);
            return ResponseEntity.ok(Map.of(
                    "principalId", principalId,
                    "processingType", processingType,
                    "allowed", decision.isAllowed(),
                    "reason", decision.getReason(),
                    "message", decision.getMessage(),
                    "requiredActions", decision.getRequiredActions() != null ? decision.getRequiredActions() : List.of()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Object> getStats() {
        try {
            var stats = childrenDataService.getStatistics();
            stats.put("dpdpSections", "§9, §33");
            stats.put("ageOfMajority", 18);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
