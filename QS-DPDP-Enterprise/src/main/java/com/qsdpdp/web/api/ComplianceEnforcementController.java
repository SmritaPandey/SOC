package com.qsdpdp.web.api;

import com.qsdpdp.breach.BreachImpactEngine;
import com.qsdpdp.consent.ConsentEnforcementEngine;
import com.qsdpdp.crossborder.DataResidencyService;
import com.qsdpdp.crypto.QuantumSafeEncryptionService;
import com.qsdpdp.rag.ConsentAnalyticsEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Compliance Enforcement Controller — Unified REST API
 *
 * Exposes all enhanced DPDP compliance enforcement capabilities:
 * - Consent Enforcement Engine (runtime validation)
 * - Data Residency (sovereign tagging, cloud routing)
 * - Breach Impact Analysis (consent-aware)
 * - AI Analytics (misuse detection, risk scoring, insights)
 * - Key Management (HSM, column encryption, key rotation)
 *
 * @version 1.0.0
 * @since Phase 7 — Enforcement API Layer
 */
@RestController
@RequestMapping("/api/enforcement")
@CrossOrigin(origins = "*")
public class ComplianceEnforcementController {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceEnforcementController.class);

    @Autowired(required = false) private ConsentEnforcementEngine enforcementEngine;
    @Autowired(required = false) private DataResidencyService residencyService;
    @Autowired(required = false) private BreachImpactEngine breachImpactEngine;
    @Autowired(required = false) private ConsentAnalyticsEngine analyticsEngine;
    @Autowired(required = false) private QuantumSafeEncryptionService cryptoService;

    // ═══════════════════════════════════════════════════════════
    // CONSENT ENFORCEMENT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/consent/validate")
    public ResponseEntity<?> validateConsentAccess(@RequestBody Map<String, Object> body) {
        if (enforcementEngine == null) return error("ConsentEnforcementEngine not available");
        enforcementEngine.initialize();

        ConsentEnforcementEngine.EnforcementRequest req = new ConsentEnforcementEngine.EnforcementRequest();
        req.principalId = str(body, "principalId");
        req.purpose = str(body, "purpose");
        req.dataCategory = str(body, "dataCategory");
        req.resourceId = str(body, "resourceId");
        req.userId = str(body, "userId");
        req.sensitivity = str(body, "sensitivity");
        req.userClearance = str(body, "userClearance");
        req.sector = str(body, "sector");
        req.userTrainingComplete = bool(body, "userTrainingComplete");
        req.mfaVerified = bool(body, "mfaVerified");

        return ResponseEntity.ok(enforcementEngine.validateAccess(req));
    }

    @GetMapping("/consent/check")
    public ResponseEntity<?> quickConsentCheck(@RequestParam String principalId,
                                                @RequestParam String purpose,
                                                @RequestParam(required = false) String dataCategory) {
        if (enforcementEngine == null) return error("ConsentEnforcementEngine not available");
        enforcementEngine.initialize();
        boolean allowed = enforcementEngine.isAccessPermitted(principalId, purpose, dataCategory);
        return ResponseEntity.ok(Map.of("principalId", principalId, "purpose", purpose,
                "allowed", allowed, "timestamp", java.time.LocalDateTime.now().toString()));
    }

    @PostMapping("/consent/grant")
    public ResponseEntity<?> registerConsentGrant(@RequestBody Map<String, Object> body) {
        if (enforcementEngine == null) return error("ConsentEnforcementEngine not available");
        enforcementEngine.initialize();

        ConsentEnforcementEngine.ConsentGrant grant = new ConsentEnforcementEngine.ConsentGrant();
        grant.id = UUID.randomUUID().toString();
        grant.principalId = str(body, "principalId");
        grant.purpose = str(body, "purpose");
        grant.dataCategories = str(body, "dataCategories");
        grant.sector = str(body, "sector");
        grant.legalBasis = str(body, "legalBasis");
        enforcementEngine.registerGrant(grant);
        return ResponseEntity.ok(Map.of("status", "REGISTERED", "grantId", grant.id));
    }

    @PostMapping("/consent/revoke")
    public ResponseEntity<?> revokeConsentGrant(@RequestParam String principalId,
                                                 @RequestParam String purpose) {
        if (enforcementEngine == null) return error("ConsentEnforcementEngine not available");
        enforcementEngine.initialize();
        enforcementEngine.revokeGrant(principalId, purpose);
        return ResponseEntity.ok(Map.of("status", "REVOKED", "principalId", principalId, "purpose", purpose));
    }

    @GetMapping("/consent/grants/{principalId}")
    public ResponseEntity<?> getActiveGrants(@PathVariable String principalId) {
        if (enforcementEngine == null) return error("ConsentEnforcementEngine not available");
        enforcementEngine.initialize();
        return ResponseEntity.ok(enforcementEngine.getActiveGrants(principalId));
    }

    @GetMapping("/consent/stats")
    public ResponseEntity<?> getEnforcementStats() {
        if (enforcementEngine == null) return error("ConsentEnforcementEngine not available");
        enforcementEngine.initialize();
        return ResponseEntity.ok(enforcementEngine.getStatistics());
    }

    @GetMapping("/consent/decisions")
    public ResponseEntity<?> getRecentDecisions(@RequestParam(defaultValue = "50") int limit) {
        if (enforcementEngine == null) return error("ConsentEnforcementEngine not available");
        enforcementEngine.initialize();
        return ResponseEntity.ok(enforcementEngine.getRecentDecisions(limit));
    }

    @GetMapping("/consent/chain/verify")
    public ResponseEntity<?> verifyChainIntegrity() {
        if (enforcementEngine == null) return error("ConsentEnforcementEngine not available");
        enforcementEngine.initialize();
        return ResponseEntity.ok(enforcementEngine.verifyChainIntegrity());
    }

    // ═══════════════════════════════════════════════════════════
    // DATA RESIDENCY
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/residency/tag")
    public ResponseEntity<?> tagDataResidency(@RequestBody Map<String, Object> body) {
        if (residencyService == null) return error("DataResidencyService not available");
        residencyService.initialize();
        String id = residencyService.tagDataResidency(
                str(body, "dataId"), str(body, "dataType"), str(body, "region"),
                str(body, "sovereignClassification"), str(body, "sector"));
        return ResponseEntity.ok(Map.of("tagId", id, "status", "TAGGED"));
    }

    @PostMapping("/residency/enforce")
    public ResponseEntity<?> enforceResidency(@RequestParam String dataId,
                                               @RequestParam String destinationRegion) {
        if (residencyService == null) return error("DataResidencyService not available");
        residencyService.initialize();
        return ResponseEntity.ok(residencyService.enforceResidencyRules(dataId, destinationRegion));
    }

    @GetMapping("/residency/dashboard")
    public ResponseEntity<?> getResidencyDashboard() {
        if (residencyService == null) return error("DataResidencyService not available");
        residencyService.initialize();
        return ResponseEntity.ok(residencyService.getResidencyDashboard());
    }

    // ═══════════════════════════════════════════════════════════
    // BREACH IMPACT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/breach/analyze/{breachId}")
    public ResponseEntity<?> analyzeBreachImpact(@PathVariable String breachId) {
        if (breachImpactEngine == null) return error("BreachImpactEngine not available");
        breachImpactEngine.initialize();
        return ResponseEntity.ok(breachImpactEngine.analyzeBreachImpact(breachId));
    }

    @GetMapping("/breach/consents/{breachId}")
    public ResponseEntity<?> mapAffectedConsents(@PathVariable String breachId) {
        if (breachImpactEngine == null) return error("BreachImpactEngine not available");
        breachImpactEngine.initialize();
        return ResponseEntity.ok(breachImpactEngine.mapAffectedConsents(breachId));
    }

    @PostMapping("/breach/report/{breachId}")
    public ResponseEntity<?> generateRegulatorReport(@PathVariable String breachId,
                                                      @RequestParam(defaultValue = "DPBI") String reportType,
                                                      @RequestParam(defaultValue = "en") String language) {
        if (breachImpactEngine == null) return error("BreachImpactEngine not available");
        breachImpactEngine.initialize();
        return ResponseEntity.ok(breachImpactEngine.generateRegulatorReport(breachId, reportType, language));
    }

    @PostMapping("/breach/notify/{breachId}")
    public ResponseEntity<?> notifyAffectedPrincipals(@PathVariable String breachId,
                                                       @RequestParam(defaultValue = "en") String language) {
        if (breachImpactEngine == null) return error("BreachImpactEngine not available");
        breachImpactEngine.initialize();
        return ResponseEntity.ok(breachImpactEngine.notifyAffectedPrincipals(breachId, language));
    }

    @GetMapping("/breach/stats")
    public ResponseEntity<?> getBreachImpactStats() {
        if (breachImpactEngine == null) return error("BreachImpactEngine not available");
        breachImpactEngine.initialize();
        return ResponseEntity.ok(breachImpactEngine.getStatistics());
    }

    // ═══════════════════════════════════════════════════════════
    // AI ANALYTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/analytics/misuse")
    public ResponseEntity<?> detectConsentMisuse() {
        if (analyticsEngine == null) return error("ConsentAnalyticsEngine not available");
        analyticsEngine.initialize();
        return ResponseEntity.ok(analyticsEngine.detectConsentMisuse());
    }

    @GetMapping("/analytics/risk/{principalId}")
    public ResponseEntity<?> calculateRiskScore(@PathVariable String principalId) {
        if (analyticsEngine == null) return error("ConsentAnalyticsEngine not available");
        analyticsEngine.initialize();
        return ResponseEntity.ok(analyticsEngine.calculateRiskScore(principalId));
    }

    @GetMapping("/analytics/vendor-risk")
    public ResponseEntity<?> analyzeVendorRisk() {
        if (analyticsEngine == null) return error("ConsentAnalyticsEngine not available");
        analyticsEngine.initialize();
        return ResponseEntity.ok(analyticsEngine.analyzeVendorRisk());
    }

    @GetMapping("/analytics/insights")
    public ResponseEntity<?> generateComplianceInsights() {
        if (analyticsEngine == null) return error("ConsentAnalyticsEngine not available");
        analyticsEngine.initialize();
        return ResponseEntity.ok(analyticsEngine.generateComplianceInsights());
    }

    @GetMapping("/analytics/expiry-prediction")
    public ResponseEntity<?> predictConsentExpiry() {
        if (analyticsEngine == null) return error("ConsentAnalyticsEngine not available");
        analyticsEngine.initialize();
        return ResponseEntity.ok(analyticsEngine.predictConsentExpiry());
    }

    @GetMapping("/analytics/stats")
    public ResponseEntity<?> getAnalyticsStats() {
        if (analyticsEngine == null) return error("ConsentAnalyticsEngine not available");
        analyticsEngine.initialize();
        return ResponseEntity.ok(analyticsEngine.getStatistics());
    }

    // ═══════════════════════════════════════════════════════════
    // KEY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/crypto/capabilities")
    public ResponseEntity<?> getCryptoCapabilities() {
        if (cryptoService == null) return error("QuantumSafeEncryptionService not available");
        return ResponseEntity.ok(cryptoService.getCryptoCapabilities());
    }

    @GetMapping("/crypto/keys")
    public ResponseEntity<?> getKeyInventory() {
        if (cryptoService == null) return error("QuantumSafeEncryptionService not available");
        return ResponseEntity.ok(cryptoService.getKeyInventory());
    }

    @PostMapping("/crypto/keys/rotate/{keyId}")
    public ResponseEntity<?> rotateKey(@PathVariable String keyId) {
        if (cryptoService == null) return error("QuantumSafeEncryptionService not available");
        return ResponseEntity.ok(cryptoService.rotateKeys(keyId));
    }

    // ═══════════════════════════════════════════════════════════
    // SYSTEM-WIDE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/status")
    public ResponseEntity<?> getEnforcementSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("system", "QS-DPDP Consent Lifecycle Enforcement System");
        status.put("version", "2.0.0");
        status.put("consentEnforcement", enforcementEngine != null ? "AVAILABLE" : "NOT_AVAILABLE");
        status.put("dataResidency", residencyService != null ? "AVAILABLE" : "NOT_AVAILABLE");
        status.put("breachImpact", breachImpactEngine != null ? "AVAILABLE" : "NOT_AVAILABLE");
        status.put("aiAnalytics", analyticsEngine != null ? "AVAILABLE" : "NOT_AVAILABLE");
        status.put("cryptoService", cryptoService != null ? "AVAILABLE" : "NOT_AVAILABLE");
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }

    private boolean bool(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null && Boolean.parseBoolean(v.toString());
    }

    private ResponseEntity<?> error(String msg) {
        return ResponseEntity.status(503).body(Map.of("error", msg, "status", "UNAVAILABLE"));
    }
}
