package com.qsdpdp.web.api;

import com.qsdpdp.consent.ConsentOrchestrationService;
import com.qsdpdp.consent.ConsentOrchestrationService.*;
import com.qsdpdp.consent.ConsentTemplateRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Consent Orchestration Controller — REST API for Global-Grade Consent Platform
 *
 * Exposes:
 * - Consent contract lifecycle (create, update, version history)
 * - Purpose-bundled consent
 * - Intelligence layer (dormant/over-broad/unused)
 * - Consent receipts
 * - Withdrawal (granular + bulk)
 * - Special consent types (minor, emergency, research)
 * - Template registry (CRUD)
 * - Trust & transparency
 * - Dashboard & analytics
 *
 * @version 2.0.0
 * @since Phase 7 — Consent Orchestration
 */
@RestController
@RequestMapping("/api/consent-orchestration")
public class ConsentOrchestrationController {

    private static final Logger logger = LoggerFactory.getLogger(ConsentOrchestrationController.class);

    @Autowired(required = false) private ConsentOrchestrationService orchestration;
    @Autowired(required = false) private ConsentTemplateRegistry templateRegistry;

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        logger.error("ConsentOrchestration error", e);
        return ResponseEntity.status(500).body(Map.of(
                "error", e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error",
                "trace", e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "no trace"
        ));
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT CONTRACT LIFECYCLE
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/consent/create")
    public ResponseEntity<?> createConsent(@RequestBody Map<String, Object> body) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();

        ConsentContractRequest req = new ConsentContractRequest();
        req.principalToken = str(body, "principalToken");
        req.fiduciaryId = str(body, "fiduciaryId");
        req.purposeId = str(body, "purposeId");
        req.purposeDescription = str(body, "purposeDescription");
        req.dataCategories = str(body, "dataCategories");
        req.processingActions = str(body, "processingActions");
        req.legalBasis = str(body, "legalBasis");
        req.jurisdictionTag = str(body, "jurisdictionTag");
        req.retentionPolicyId = str(body, "retentionPolicyId");
        req.captureChannel = str(body, "captureChannel");
        req.languageCode = str(body, "languageCode");
        req.consentType = str(body, "consentType");
        req.bundleId = str(body, "bundleId");
        req.sector = str(body, "sector");
        req.actorId = str(body, "actorId");
        req.uxSnapshotData = str(body, "uxSnapshotData");
        req.uxElements = str(body, "uxElements");
        String expiryStr = str(body, "expiryDate");
        if (expiryStr != null) req.expiryDate = LocalDateTime.parse(expiryStr);

        ConsentContract contract = orchestration.createConsent(req);
        return ResponseEntity.ok(contract);
    }

    @PutMapping("/consent/{consentId}")
    public ResponseEntity<?> updateConsent(@PathVariable String consentId, @RequestBody Map<String, Object> body) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();

        ConsentUpdateRequest update = new ConsentUpdateRequest();
        update.purposeId = str(body, "purposeId");
        update.purposeDescription = str(body, "purposeDescription");
        update.dataCategories = str(body, "dataCategories");
        update.processingActions = str(body, "processingActions");
        update.status = str(body, "status");
        update.changeReason = str(body, "changeReason");
        update.actorId = str(body, "actorId");

        ConsentContract updated = orchestration.updateConsent(consentId, update);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/consent/{consentId}")
    public ResponseEntity<?> getConsent(@PathVariable String consentId) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        ConsentContract contract = orchestration.getLatestVersion(consentId);
        if (contract == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(contract);
    }

    @GetMapping("/consent/{consentId}/versions")
    public ResponseEntity<?> getVersionHistory(@PathVariable String consentId) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.getVersionHistory(consentId));
    }

    @GetMapping("/principal/{principalToken}/consents")
    public ResponseEntity<?> getPrincipalConsents(@PathVariable String principalToken,
                                                   @RequestParam(required = false) String status) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.getPrincipalConsents(principalToken, status));
    }

    // ═══════════════════════════════════════════════════════════
    // PURPOSE-BUNDLED CONSENT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/consent/bundle")
    public ResponseEntity<?> createBundledConsent(@RequestBody Map<String, Object> body) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();

        BundledConsentRequest req = new BundledConsentRequest();
        req.principalToken = str(body, "principalToken");
        req.bundleId = str(body, "bundleId");
        req.captureChannel = str(body, "captureChannel");
        req.languageCode = str(body, "languageCode");
        req.sector = str(body, "sector");
        req.actorId = str(body, "actorId");
        @SuppressWarnings("unchecked")
        List<String> selected = (List<String>) body.get("selectedOptionalPurposes");
        req.selectedOptionalPurposes = selected;

        return ResponseEntity.ok(orchestration.createBundledConsent(req));
    }

    // ═══════════════════════════════════════════════════════════
    // INTELLIGENCE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/intelligence/scan")
    public ResponseEntity<?> runIntelligenceScan() {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.analyzeConsentIntelligence());
    }

    // ═══════════════════════════════════════════════════════════
    // RECEIPTS
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/consent/{consentId}/receipt")
    public ResponseEntity<?> generateReceipt(@PathVariable String consentId) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        ConsentContract contract = orchestration.getLatestVersion(consentId);
        if (contract == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(orchestration.generateReceipt(contract));
    }

    // ═══════════════════════════════════════════════════════════
    // WITHDRAWAL
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/consent/{consentId}/withdraw")
    public ResponseEntity<?> withdrawConsent(@PathVariable String consentId,
                                              @RequestParam(required = false) String actorId,
                                              @RequestParam(required = false) String reason) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.withdrawConsent(consentId, actorId, reason));
    }

    @PostMapping("/principal/{principalToken}/withdraw/sector/{sector}")
    public ResponseEntity<?> bulkWithdrawBySector(@PathVariable String principalToken,
                                                   @PathVariable String sector,
                                                   @RequestParam(required = false) String actorId) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.bulkWithdrawBySector(principalToken, sector, actorId));
    }

    // ═══════════════════════════════════════════════════════════
    // SPECIAL CONSENT TYPES
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/consent/minor")
    public ResponseEntity<?> createMinorConsent(@RequestBody Map<String, Object> body) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();

        ConsentContractRequest req = new ConsentContractRequest();
        req.principalToken = str(body, "principalToken");
        req.purposeId = str(body, "purposeId");
        req.dataCategories = str(body, "dataCategories");
        req.actorId = str(body, "actorId");
        req.sector = str(body, "sector");

        MinorConsentDetails minor = new MinorConsentDetails();
        minor.dateOfBirth = str(body, "dateOfBirth");
        minor.age = body.get("age") != null ? Integer.parseInt(body.get("age").toString()) : 0;
        minor.guardianId = str(body, "guardianId");
        minor.guardianVerified = body.get("guardianVerified") != null
                && Boolean.parseBoolean(body.get("guardianVerified").toString());

        return ResponseEntity.ok(orchestration.createMinorConsent(req, minor));
    }

    @PostMapping("/consent/emergency")
    public ResponseEntity<?> createEmergencyConsent(@RequestBody Map<String, Object> body) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();

        ConsentContractRequest req = new ConsentContractRequest();
        req.principalToken = str(body, "principalToken");
        req.purposeId = str(body, "purposeId");
        req.dataCategories = str(body, "dataCategories");
        req.actorId = str(body, "actorId");

        return ResponseEntity.ok(orchestration.createEmergencyConsent(req,
                str(body, "justification"), str(body, "approvedBy")));
    }

    @PostMapping("/consent/research")
    public ResponseEntity<?> createResearchConsent(@RequestBody Map<String, Object> body) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();

        ConsentContractRequest req = new ConsentContractRequest();
        req.principalToken = str(body, "principalToken");
        req.purposeId = str(body, "purposeId");
        req.dataCategories = str(body, "dataCategories");
        req.actorId = str(body, "actorId");

        return ResponseEntity.ok(orchestration.createResearchConsent(req,
                str(body, "anonymisationLevel"), str(body, "ethicsApproval")));
    }

    // ═══════════════════════════════════════════════════════════
    // TRUST & TRANSPARENCY
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/consent/{consentId}/transparency")
    public ResponseEntity<?> getConsentTransparency(@PathVariable String consentId) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.getConsentTransparency(consentId));
    }

    // ═══════════════════════════════════════════════════════════
    // INTEROPERABILITY (§9)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/interop/schema")
    public ResponseEntity<?> getConsentSchema() {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.getConsentSchema());
    }

    @GetMapping("/interop/export/{principalToken}")
    public ResponseEntity<?> exportConsents(@PathVariable String principalToken) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.exportConsents(principalToken));
    }

    @PostMapping("/interop/import")
    public ResponseEntity<?> importConsent(@RequestBody Map<String, Object> body) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.importConsent(body));
    }

    // ═══════════════════════════════════════════════════════════
    // UX COMPLIANCE ENGINE (§10)
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/ux/validate")
    public ResponseEntity<?> validateUXCompliance(@RequestBody Map<String, Object> body) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();

        UXComplianceRequest req = new UXComplianceRequest();
        req.consentId = str(body, "consentId");
        req.consentText = str(body, "consentText");
        req.hasPreTickedBoxes = Boolean.parseBoolean(String.valueOf(body.getOrDefault("hasPreTickedBoxes", "false")));
        req.hasLayeredNotice = Boolean.parseBoolean(String.valueOf(body.getOrDefault("hasLayeredNotice", "false")));
        req.readabilityGrade = body.get("readabilityGrade") != null ? Integer.parseInt(body.get("readabilityGrade").toString()) : 8;
        req.languagesOffered = body.get("languagesOffered") != null ? Integer.parseInt(body.get("languagesOffered").toString()) : 1;
        req.hasAudioOption = Boolean.parseBoolean(String.valueOf(body.getOrDefault("hasAudioOption", "false")));
        req.hasAssistedMode = Boolean.parseBoolean(String.valueOf(body.getOrDefault("hasAssistedMode", "false")));
        req.withdrawalMethodClear = Boolean.parseBoolean(String.valueOf(body.getOrDefault("withdrawalMethodClear", "false")));

        return ResponseEntity.ok(orchestration.validateUXCompliance(req));
    }

    // ═══════════════════════════════════════════════════════════
    // DATA PRINCIPAL EXPERIENCE (§15)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/consent/{consentId}/compare")
    public ResponseEntity<?> compareVersions(@PathVariable String consentId,
                                              @RequestParam int v1, @RequestParam int v2) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.compareVersions(consentId, v1, v2));
    }

    @GetMapping("/principal/{principalToken}/receipts")
    public ResponseEntity<?> getPrincipalReceipts(@PathVariable String principalToken) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.getReceiptsForPrincipal(principalToken));
    }

    @GetMapping("/principal/{principalToken}/consents/filter")
    public ResponseEntity<?> getFilteredConsents(@PathVariable String principalToken,
                                                   @RequestParam(required = false) String sector,
                                                   @RequestParam(required = false) String purpose,
                                                   @RequestParam(required = false) String org) {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.getPrincipalConsentsFiltered(principalToken, sector, purpose, org));
    }

    // ═══════════════════════════════════════════════════════════
    // AI-DRIVEN CONSENT GOVERNANCE (§17)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/governance/recommendations")
    public ResponseEntity<?> getGovernanceRecommendations() {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.getAIGovernanceRecommendations());
    }

    // ═══════════════════════════════════════════════════════════
    // TEMPLATE REGISTRY
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/templates/sector/{sector}")
    public ResponseEntity<?> getTemplatesBySector(@PathVariable String sector) {
        if (templateRegistry == null) return unavailable("ConsentTemplateRegistry");
        templateRegistry.initialize();
        return ResponseEntity.ok(templateRegistry.getTemplatesBySector(sector));
    }

    @GetMapping("/templates/{templateId}")
    public ResponseEntity<?> getTemplate(@PathVariable String templateId,
                                          @RequestParam(defaultValue = "en") String language) {
        if (templateRegistry == null) return unavailable("ConsentTemplateRegistry");
        templateRegistry.initialize();
        return ResponseEntity.ok(templateRegistry.getLatestTemplate(templateId, language));
    }

    @PostMapping("/templates")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> body) {
        if (templateRegistry == null) return unavailable("ConsentTemplateRegistry");
        templateRegistry.initialize();

        ConsentTemplateRegistry.ConsentTemplate t = new ConsentTemplateRegistry.ConsentTemplate();
        t.templateCode = str(body, "templateCode");
        t.templateName = str(body, "templateName");
        t.sector = str(body, "sector");
        t.purposeId = str(body, "purposeId");
        t.languageCode = str(body, "languageCode") != null ? str(body, "languageCode") : "en";
        t.title = str(body, "title");
        t.description = str(body, "description");
        t.dataCategories = str(body, "dataCategories");
        t.processingActions = str(body, "processingActions");
        t.legalBasis = str(body, "legalBasis");
        t.retentionDescription = str(body, "retentionDescription");
        t.withdrawalInstructions = str(body, "withdrawalInstructions");
        t.rightsSummary = str(body, "rightsSummary");
        t.layeredNoticeShort = str(body, "layeredNoticeShort");
        t.layeredNoticeDetailed = str(body, "layeredNoticeDetailed");
        t.createdBy = str(body, "createdBy");

        return ResponseEntity.ok(templateRegistry.createTemplate(t));
    }

    @PostMapping("/templates/{templateId}/clone")
    public ResponseEntity<?> cloneTemplate(@PathVariable String templateId,
                                            @RequestParam String sector,
                                            @RequestParam(required = false) String language) {
        if (templateRegistry == null) return unavailable("ConsentTemplateRegistry");
        templateRegistry.initialize();
        return ResponseEntity.ok(templateRegistry.cloneTemplate(templateId, sector, language));
    }

    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<?> archiveTemplate(@PathVariable String templateId) {
        if (templateRegistry == null) return unavailable("ConsentTemplateRegistry");
        templateRegistry.initialize();
        templateRegistry.archiveTemplate(templateId);
        return ResponseEntity.ok(Map.of("status", "ARCHIVED", "templateId", templateId));
    }

    @GetMapping("/templates/dashboard")
    public ResponseEntity<?> getTemplatesDashboard() {
        if (templateRegistry == null) return unavailable("ConsentTemplateRegistry");
        templateRegistry.initialize();
        return ResponseEntity.ok(templateRegistry.getRegistryDashboard());
    }

    // ═══════════════════════════════════════════════════════════
    // ORCHESTRATION DASHBOARD
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/dashboard")
    public ResponseEntity<?> getOrchestrationDashboard() {
        if (orchestration == null) return unavailable("ConsentOrchestrationService");
        orchestration.initialize();
        return ResponseEntity.ok(orchestration.getOrchestrationDashboard());
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("platform", "Global-Grade Consent Orchestration Platform");
        status.put("version", "2.0.0");
        status.put("orchestrationService", orchestration != null ? "AVAILABLE" : "UNAVAILABLE");
        status.put("templateRegistry", templateRegistry != null ? "AVAILABLE" : "UNAVAILABLE");
        status.put("capabilities", List.of(
                "consent_contracts", "versioning", "purpose_bundles", "intelligence",
                "receipts", "withdrawal_propagation", "special_consent_types",
                "ux_compliance", "trust_transparency", "template_registry",
                "interoperability", "ux_validation", "version_comparison",
                "ai_governance", "filtered_consents", "receipt_download"
        ));
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }

    // ─── HELPERS ───

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }

    private ResponseEntity<?> unavailable(String service) {
        return ResponseEntity.status(503).body(Map.of("error", service + " not available", "status", "UNAVAILABLE"));
    }
}
