package com.qsdpdp.web.api;

import com.qsdpdp.consent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Universal Consent Manager REST API — DPDP Act 2023
 * Full CRUD for consent lifecycle, data categories, notices,
 * delegations, legitimate uses, data access tracking, and analytics.
 *
 * @version 4.0.0 — UCM Enhancement
 * @since Phase 2
 */
@RestController("consentApiController")
@RequestMapping("/api/v1/consent")
public class ConsentController {

    @Autowired(required = false) private ConsentService consentService;

    // ═══════════════════════════════════════════════════════════
    // HEALTH & STATS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "universal-consent-manager",
                "status", "UP",
                "version", "4.0.0",
                "initialized", consentService.isInitialized()));
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        try {
            return ResponseEntity.ok(consentService.getEnhancedStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT CRUD — S.6 Collection / Withdrawal / Modification
    // ═══════════════════════════════════════════════════════════

    /** Collect new consent (S.6 — free, specific, informed) */
    @PostMapping
    public ResponseEntity<Map<String, Object>> collectConsent(@RequestBody ConsentRequest request) {
        try {
            // Auto-generate IDs if names provided but IDs missing (frontend sends names)
            if (request.getDataPrincipalId() == null && request.getDataPrincipalName() != null) {
                request.setDataPrincipalId(UUID.randomUUID().toString());
            }
            if (request.getPurposeId() == null && request.getPurposeName() != null) {
                request.setPurposeId(UUID.randomUUID().toString());
            }
            if (request.getDataPrincipalId() == null || request.getPurposeId() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Either dataPrincipalId/purposeId or dataPrincipalName/purposeName are required"));
            }
            if (request.getConsentMethod() == null) request.setConsentMethod("WEB_FORM");
            if (request.getNoticeVersion() == null) request.setNoticeVersion("v1.0");
            Consent consent = consentService.collectConsent(request);
            // Set display names on the saved consent for immediate UI feedback
            if (request.getDataPrincipalName() != null) consent.setDataPrincipalName(request.getDataPrincipalName());
            if (request.getPurposeName() != null) consent.setPurposeName(request.getPurposeName());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "consentId", consent.getId(),
                    "status", consent.getStatus().name(),
                    "hash", consent.getHash(),
                    "dataPrincipalName", request.getDataPrincipalName() != null ? request.getDataPrincipalName() : "",
                    "purposeName", request.getPurposeName() != null ? request.getPurposeName() : "",
                    "message", "Consent collected successfully under DPDP Act S.6"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }


    /** Request consent (creates in REQUESTED status, awaiting principal action) */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestConsent(@RequestBody ConsentRequest request) {
        try {
            Consent consent = consentService.requestConsent(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "consentId", consent.getId(),
                    "status", "REQUESTED",
                    "message", "Consent request sent to data principal"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get consent by ID */
    @GetMapping("/{consentId}")
    public ResponseEntity<Object> getConsent(@PathVariable String consentId) {
        try {
            Consent consent = consentService.getConsentById(consentId);
            if (consent == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(consent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** List all consents (paginated) */
    @GetMapping
    public ResponseEntity<Object> listConsents(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<Consent> consents = consentService.getAllConsents(offset, limit);
            return ResponseEntity.ok(Map.of(
                    "total", consentService.getStatistics().getTotalConsents(),
                    "offset", offset,
                    "limit", limit,
                    "consents", consents));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** List consents for a data principal */
    @GetMapping("/principal/{principalId}")
    public ResponseEntity<Object> getConsentsByPrincipal(@PathVariable String principalId) {
        try {
            List<Consent> consents = consentService.getConsentsByPrincipal(principalId);
            return ResponseEntity.ok(Map.of(
                    "principalId", principalId,
                    "totalConsents", consents.size(),
                    "consents", consents));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Withdraw consent (S.6 — withdrawal as easy as collection) */
    @PostMapping("/{consentId}/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawConsent(
            @PathVariable String consentId,
            @RequestBody Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason", "Data principal requested withdrawal");
            String actorId = body.getOrDefault("actorId", "DATA_PRINCIPAL");
            Consent consent = consentService.withdrawConsent(consentId, reason, actorId);
            return ResponseEntity.ok(Map.of(
                    "consentId", consentId,
                    "status", consent.getStatus().name(),
                    "withdrawnAt", consent.getWithdrawnAt().toString(),
                    "message", "Consent withdrawn per DPDP Act S.6(6)"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Propagate withdrawal to all data processors (S.6 cascade) */
    @PostMapping("/{consentId}/propagate-withdrawal")
    public ResponseEntity<Map<String, Object>> propagateWithdrawal(@PathVariable String consentId) {
        try {
            var result = consentService.propagateWithdrawal(consentId);
            return ResponseEntity.ok(Map.of(
                    "consentId", consentId,
                    "status", result.status,
                    "totalProcessors", result.totalProcessors,
                    "successCount", result.successCount,
                    "failCount", result.failCount,
                    "processorStatuses", result.processorStatuses,
                    "propagatedAt", result.propagatedAt.toString(),
                    "message", "Withdrawal propagated to all registered processors per DPDP S.6"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Modify consent preferences (transition to MODIFIED state) */
    @PutMapping("/{consentId}/modify")
    public ResponseEntity<Map<String, Object>> modifyConsent(
            @PathVariable String consentId,
            @RequestBody Map<String, Object> body) {
        try {
            Consent consent = consentService.modifyConsent(consentId, body);
            return ResponseEntity.ok(Map.of(
                    "consentId", consentId,
                    "status", consent.getStatus().name(),
                    "modifiedAt", consent.getModifiedAt() != null ? consent.getModifiedAt().toString() : "",
                    "message", "Consent preferences modified"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Renew expiring consent */
    @PostMapping("/{consentId}/renew")
    public ResponseEntity<Map<String, Object>> renewConsent(
            @PathVariable String consentId,
            @RequestBody Map<String, Object> body) {
        try {
            int days = (int) body.getOrDefault("extensionDays", 365);
            Consent consent = consentService.renewConsent(consentId, days);
            if (consent == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(Map.of(
                    "consentId", consentId,
                    "newExpiry", consent.getExpiresAt() != null ? consent.getExpiresAt().toString() : "none",
                    "message", "Consent renewed for " + days + " days"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get consents expiring within N days */
    @GetMapping("/expiring")
    public ResponseEntity<Object> getExpiringConsents(@RequestParam(defaultValue = "30") int days) {
        try {
            List<Consent> expiring = consentService.getExpiringConsents(days);
            return ResponseEntity.ok(Map.of(
                    "expiringWithinDays", days,
                    "count", expiring.size(),
                    "consents", expiring));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT AUDIT TRAIL — Immutable Hash-Chained Ledger
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/audit-trail")
    public ResponseEntity<Object> getAuditTrail(
            @RequestParam(required = false) String consentId,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            var trail = consentService.getAuditTrail(consentId, limit);
            return ResponseEntity.ok(Map.of(
                    "totalEntries", trail.size(),
                    "entries", trail));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify-audit-chain")
    public ResponseEntity<Map<String, Object>> verifyAuditChain() {
        try {
            boolean valid = consentService.verifyAuditChain();
            return ResponseEntity.ok(Map.of(
                    "auditChainValid", valid,
                    "verificationMethod", "SHA-256 Hash Chain",
                    "dpdpCompliance", valid ? "PASS" : "FAIL"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT PREFERENCES — Granular per-category controls
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/{consentId}/preferences")
    public ResponseEntity<Object> getPreferences(@PathVariable String consentId) {
        try {
            var prefs = consentService.getConsentPreferences(consentId);
            return ResponseEntity.ok(Map.of("consentId", consentId, "preferences", prefs));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{consentId}/preferences")
    public ResponseEntity<Object> savePreference(
            @PathVariable String consentId,
            @RequestBody ConsentPreference preference) {
        try {
            preference.setConsentId(consentId);
            var saved = consentService.saveConsentPreference(preference);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GUARDIAN CONSENT — S.9 Children & Persons with Disability
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/guardian")
    public ResponseEntity<Object> getGuardianConsents(@RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(consentService.getGuardianConsents(status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/guardian")
    public ResponseEntity<Object> saveGuardianConsent(@RequestBody GuardianConsent gc) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consentService.saveGuardianConsent(gc));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/guardian/{id}/verify")
    public ResponseEntity<Object> verifyGuardianConsent(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        try {
            String method = body.getOrDefault("verificationMethod", "OTP");
            return ResponseEntity.ok(consentService.verifyGuardianConsent(id, method));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SECTOR PURPOSE TEMPLATES — Multi-sector support
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/sector-templates")
    public ResponseEntity<Object> getSectorTemplates(@RequestParam(required = false) String sector) {
        try {
            return ResponseEntity.ok(consentService.getSectorTemplates(sector));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DATA CATEGORIES — UCM Registry
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/data-categories")
    public ResponseEntity<Object> getDataCategories() {
        try {
            return ResponseEntity.ok(consentService.getDataCategories());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/data-categories")
    public ResponseEntity<Object> saveDataCategory(@RequestBody DataCategoryRegistry category) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consentService.saveDataCategory(category));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT NOTICES — S.5 Privacy Notices
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/notices")
    public ResponseEntity<Object> getConsentNotices() {
        try {
            return ResponseEntity.ok(consentService.getConsentNotices());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/notices")
    public ResponseEntity<Object> saveConsentNotice(@RequestBody ConsentNotice notice) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consentService.saveConsentNotice(notice));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT DELEGATIONS — S.9 Guardian + Representative
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/delegations")
    public ResponseEntity<Object> getDelegations(@RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(consentService.getConsentDelegations(status));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/delegations")
    public ResponseEntity<Object> saveDelegation(@RequestBody ConsentDelegation delegation) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consentService.saveConsentDelegation(delegation));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LEGITIMATE USES — S.7 Processing Without Consent
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/legitimate-uses")
    public ResponseEntity<Object> getLegitimateUses() {
        try {
            return ResponseEntity.ok(consentService.getLegitimateUses());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/legitimate-uses")
    public ResponseEntity<Object> saveLegitimateUse(@RequestBody LegitimateUse use) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consentService.saveLegitimateUse(use));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DATA ACCESS LOG — Privacy Wallet Tracking
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/data-access-log")
    public ResponseEntity<Object> getDataAccessLog(
            @RequestParam(required = false) String principalId,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            return ResponseEntity.ok(consentService.getDataAccessLog(principalId, limit));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/data-access-log")
    public ResponseEntity<Object> logDataAccess(@RequestBody DataAccessLog log) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(consentService.logDataAccess(log));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ANALYTICS — RAG AI-ready consent analytics
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/analytics/patterns")
    public ResponseEntity<Object> getConsentPatterns() {
        try {
            return ResponseEntity.ok(consentService.getConsentAnalytics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/analytics/compliance")
    public ResponseEntity<Object> getComplianceScore() {
        try {
            var stats = consentService.getEnhancedStatistics();
            return ResponseEntity.ok(Map.of(
                    "complianceScore", stats.getComplianceScore(),
                    "auditChainIntegrity", stats.isChainIntegrity(),
                    "activeRate", stats.getActiveRate(),
                    "withdrawalRate", stats.getWithdrawalRate(),
                    "guardianConsentsVerified", stats.getVerifiedGuardianConsents(),
                    "languageDistribution", stats.getLanguageDistribution() != null ? stats.getLanguageDistribution() : Map.of()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
