package com.qsdpdp.web;

import com.qsdpdp.consent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Consent Management REST Controller
 * DPDP Act 2023 Section 6 — Consent collection, withdrawal, purpose binding,
 * guardian consent, granular preferences, and validation
 *
 * @version 1.0.0
 * @since Sprint 3
 */

@RestController
@RequestMapping("/api/consents")
public class ConsentController {

    private static final Logger logger = LoggerFactory.getLogger(ConsentController.class);

    @Autowired
    private ConsentService consentService;

    // ═══════════════════════════════════════════════════════════
    // CONSENT LIFECYCLE
    // ═══════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> collectConsent(@RequestBody Map<String, String> payload) {
        try {
            ConsentRequest request = ConsentRequest.builder()
                    .dataPrincipalId(payload.getOrDefault("dataPrincipalId", ""))
                    .purposeId(payload.getOrDefault("purposeId", ""))
                    .consentMethod(payload.getOrDefault("consentMethod", "web"))
                    .noticeVersion(payload.getOrDefault("noticeVersion", "1.0"))
                    .language(payload.getOrDefault("language", "en"))
                    .ipAddress(payload.getOrDefault("ipAddress", ""))
                    .userAgent(payload.getOrDefault("userAgent", ""))
                    .actorId(payload.getOrDefault("actorId", "admin"))
                    .build();

            String expiresAt = payload.get("expiresAt");
            if (expiresAt != null && !expiresAt.isBlank()) {
                request.setExpiresAt(LocalDateTime.parse(expiresAt));
            }

            Consent consent = consentService.collectConsent(request);
            return ResponseEntity.ok(Map.of(
                    "status", "collected",
                    "consent", consentToMap(consent),
                    "message", "Consent collected successfully. ID: " + consent.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to collect consent", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to collect consent: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listConsents(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<Consent> consents = consentService.getAllConsents(offset, limit);
            return ResponseEntity.ok(Map.of(
                    "consents", consents.stream().map(this::consentToMap).collect(Collectors.toList()),
                    "offset", offset,
                    "limit", limit));
        } catch (Exception e) {
            logger.error("Failed to list consents", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list consents: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConsent(@PathVariable String id) {
        Consent consent = consentService.getConsentById(id);
        if (consent == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("consent", consentToMap(consent)));
    }

    @GetMapping("/principal/{principalId}")
    public ResponseEntity<?> getConsentsByPrincipal(@PathVariable String principalId) {
        try {
            List<Consent> consents = consentService.getConsentsByPrincipal(principalId);
            return ResponseEntity.ok(Map.of(
                    "principalId", principalId,
                    "consents", consents.stream().map(this::consentToMap).collect(Collectors.toList()),
                    "total", consents.size()));
        } catch (Exception e) {
            logger.error("Failed to get consents for principal", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get consents: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // WITHDRAWAL (DPDP Act: must be as easy as collection)
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdrawConsent(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.getOrDefault("reason", "Data principal requested withdrawal");
            String actorId = payload.getOrDefault("actorId", "admin");
            Consent consent = consentService.withdrawConsent(id, reason, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "withdrawn",
                    "consent", consentToMap(consent),
                    "message", "Consent withdrawn successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to withdraw consent", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to withdraw consent: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT VALIDATION & PURPOSE BINDING
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/validate")
    public ResponseEntity<?> validateConsent(@RequestBody Map<String, String> payload) {
        try {
            String consentId = payload.getOrDefault("consentId", "");
            String purpose = payload.getOrDefault("purpose", "");

            ConsentService.TokenValidationResult result =
                    consentService.validateConsentToken(consentId, purpose);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("valid", result.isValid());
            response.put("reason", result.getReason());
            if (result.isValid()) {
                response.put("consentId", result.getConsentId());
                response.put("principalId", result.getPrincipalId());
                response.put("purposeId", result.getPurposeId());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to validate consent", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to validate: " + e.getMessage()));
        }
    }

    @PostMapping("/check-purpose")
    public ResponseEntity<?> checkPurposeBinding(@RequestBody Map<String, String> payload) {
        try {
            String principalId = payload.getOrDefault("principalId", "");
            String purpose = payload.getOrDefault("purpose", "");
            consentService.enforcePurposeBinding(principalId, purpose);
            return ResponseEntity.ok(Map.of(
                    "allowed", true,
                    "principalId", principalId,
                    "purpose", purpose,
                    "message", "Purpose binding check passed"));
        } catch (SecurityException e) {
            return ResponseEntity.ok(Map.of(
                    "allowed", false,
                    "principalId", payload.getOrDefault("principalId", ""),
                    "purpose", payload.getOrDefault("purpose", ""),
                    "reason", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to check purpose binding", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to check: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GRANULAR PREFERENCES
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{consentId}/preferences")
    public ResponseEntity<?> savePreference(@PathVariable String consentId,
            @RequestBody Map<String, String> payload) {
        try {
            ConsentPreference pref = new ConsentPreference();
            pref.setConsentId(consentId);
            pref.setDataCategory(payload.getOrDefault("dataCategory", ""));
            pref.setPurposeId(payload.getOrDefault("purposeId", ""));
            pref.setProcessingBasis(payload.getOrDefault("processingBasis", "consent"));
            pref.setAllowed("true".equalsIgnoreCase(payload.getOrDefault("allowed", "true")));
            pref.setThirdPartySharing("true".equalsIgnoreCase(payload.getOrDefault("thirdPartySharing", "false")));
            pref.setCrossBorderTransfer("true".equalsIgnoreCase(payload.getOrDefault("crossBorderTransfer", "false")));

            consentService.saveConsentPreference(pref);
            return ResponseEntity.ok(Map.of(
                    "status", "saved",
                    "consentId", consentId,
                    "dataCategory", pref.getDataCategory()));
        } catch (Exception e) {
            logger.error("Failed to save preference", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to save preference: " + e.getMessage()));
        }
    }

    @GetMapping("/{consentId}/preferences")
    public ResponseEntity<?> getPreferences(@PathVariable String consentId) {
        try {
            List<ConsentPreference> prefs = consentService.getConsentPreferences(consentId);
            List<Map<String, Object>> prefList = new ArrayList<>();
            for (ConsentPreference p : prefs) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("consentId", p.getConsentId());
                m.put("dataCategory", p.getDataCategory());
                m.put("purposeId", p.getPurposeId());
                m.put("processingBasis", p.getProcessingBasis());
                m.put("allowed", p.isAllowed());
                m.put("thirdPartySharing", p.isThirdPartySharing());
                m.put("crossBorderTransfer", p.isCrossBorderTransfer());
                prefList.add(m);
            }
            return ResponseEntity.ok(Map.of(
                    "consentId", consentId,
                    "preferences", prefList,
                    "total", prefList.size()));
        } catch (Exception e) {
            logger.error("Failed to get preferences", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get preferences: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GUARDIAN CONSENT (DPDP Act: children & persons with disability)
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/guardian")
    public ResponseEntity<?> saveGuardianConsent(@RequestBody Map<String, String> payload) {
        try {
            GuardianConsent gc = new GuardianConsent();
            gc.setChildPrincipalId(payload.getOrDefault("childPrincipalId", ""));
            gc.setGuardianPrincipalId(payload.getOrDefault("guardianPrincipalId", ""));
            gc.setGuardianRelationship(payload.getOrDefault("relationship", ""));
            gc.setPurposeId(payload.getOrDefault("purposeId", ""));
            gc.setChildName(payload.getOrDefault("childName", ""));
            gc.setGuardianName(payload.getOrDefault("guardianName", ""));
            gc.setVerificationMethod(payload.getOrDefault("verificationMethod", "OTP"));

            consentService.saveGuardianConsent(gc);
            return ResponseEntity.ok(Map.of(
                    "status", "saved",
                    "guardianConsentId", gc.getId(),
                    "message", "Guardian consent recorded. Pending KYC verification."));
        } catch (Exception e) {
            logger.error("Failed to save guardian consent", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to save guardian consent: " + e.getMessage()));
        }
    }

    @GetMapping("/guardian")
    public ResponseEntity<?> getGuardianConsents(
            @RequestParam(required = false) String status) {
        try {
            List<GuardianConsent> consents = consentService.getGuardianConsents(status);
            List<Map<String, Object>> list = new ArrayList<>();
            for (GuardianConsent gc : consents) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", gc.getId());
                m.put("childPrincipalId", gc.getChildPrincipalId());
                m.put("childName", gc.getChildName());
                m.put("guardianPrincipalId", gc.getGuardianPrincipalId());
                m.put("guardianName", gc.getGuardianName());
                m.put("relationship", gc.getGuardianRelationship());
                m.put("purposeId", gc.getPurposeId());
                m.put("status", gc.getStatus());
                m.put("kycVerified", gc.isGuardianKycVerified());
                list.add(m);
            }
            return ResponseEntity.ok(Map.of("guardianConsents", list, "total", list.size()));
        } catch (Exception e) {
            logger.error("Failed to get guardian consents", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get guardian consents: " + e.getMessage()));
        }
    }

    @PostMapping("/guardian/{id}/verify")
    public ResponseEntity<?> verifyGuardianConsent(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String method = payload.getOrDefault("verificationMethod", "aadhaar");
            consentService.verifyGuardianConsent(id, method);
            return ResponseEntity.ok(Map.of(
                    "status", "verified",
                    "id", id,
                    "message", "Guardian KYC verified via " + method));
        } catch (Exception e) {
            logger.error("Failed to verify guardian consent", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to verify: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            ConsentStatistics stats = consentService.getStatistics();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalConsents", stats.getTotalConsents());
            result.put("activeConsents", stats.getActiveConsents());
            result.put("withdrawnConsents", stats.getWithdrawnConsents());
            result.put("expiredConsents", stats.getExpiredConsents());
            result.put("activeRate", stats.getActiveRate());
            result.put("withdrawalRate", stats.getWithdrawalRate());
            result.put("complianceScore", stats.getComplianceScore());
            result.put("consentsLast30Days", stats.getConsentsLast30Days());
            result.put("withdrawalsLast30Days", stats.getWithdrawalsLast30Days());
            result.put("guardian", Map.of(
                    "total", stats.getTotalGuardianConsents(),
                    "pending", stats.getPendingGuardianConsents(),
                    "verified", stats.getVerifiedGuardianConsents()));
            result.put("preferences", Map.of(
                    "total", stats.getTotalPreferences(),
                    "allowed", stats.getAllowedPreferences(),
                    "denied", stats.getDeniedPreferences()));
            result.put("chainIntegrity", stats.isChainIntegrity());
            return ResponseEntity.ok(Map.of("statistics", result));
        } catch (Exception e) {
            logger.error("Failed to get consent statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> consentToMap(Consent c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("dataPrincipalId", c.getDataPrincipalId());
        map.put("dataPrincipalName", c.getDataPrincipalName());
        map.put("purposeId", c.getPurposeId());
        map.put("purposeName", c.getPurposeName());
        map.put("status", c.getStatus().name());
        map.put("consentMethod", c.getConsentMethod());
        map.put("noticeVersion", c.getNoticeVersion());
        map.put("language", c.getLanguage());
        map.put("collectedAt", c.getCollectedAt() != null ? c.getCollectedAt().toString() : null);
        map.put("expiresAt", c.getExpiresAt() != null ? c.getExpiresAt().toString() : null);
        map.put("withdrawnAt", c.getWithdrawnAt() != null ? c.getWithdrawnAt().toString() : null);
        map.put("withdrawalReason", c.getWithdrawalReason());
        map.put("isActive", c.isActive());
        map.put("isExpired", c.isExpired());
        map.put("hash", c.getHash());
        return map;
    }
}
