package com.qsdpdp.web.api;

import com.qsdpdp.breach.BreachSimulationEngine;
import com.qsdpdp.consent.DarkPatternDetector;
import com.qsdpdp.consent.PurposeValidationEngine;
import com.qsdpdp.crypto.CryptoEraseService;
import com.qsdpdp.governance.PolicyExportService;
import com.qsdpdp.iam.PAMModuleService;
import com.qsdpdp.iam.PasswordlessAuthService;
import com.qsdpdp.iam.SSOService;
import com.qsdpdp.integration.NoCodeConnectorService;
import com.qsdpdp.pii.DataLineageGraphService;
import com.qsdpdp.rag.AnomalyDetectionEngine;
import com.qsdpdp.rag.ConsentFatigueDetector;
import com.qsdpdp.security.LocalCAService;
import com.qsdpdp.security.TokenizationService;
import com.qsdpdp.siem.UEBAEngine;
import com.qsdpdp.sync.FileBasedSyncService;
import com.qsdpdp.vendor.ThirdPartyRiskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Platform Controller — Unified V2 API
 * 
 * Exposes all Phase 0-15 enhanced capabilities via /api/v2/ endpoints.
 * Backward compatible — original /api/ endpoints remain unchanged.
 * 
 * @version 2.0.0
 * @since Phase 0-15 Enhancement
 */
@RestController
@RequestMapping("/api/v2")
public class EnhancedPlatformController {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedPlatformController.class);

    @Autowired(required = false) private DarkPatternDetector darkPatternDetector;
    @Autowired(required = false) private PurposeValidationEngine purposeEngine;
    @Autowired(required = false) private NoCodeConnectorService connectorService;
    @Autowired(required = false) private DataLineageGraphService lineageService;
    @Autowired(required = false) private UEBAEngine uebaEngine;
    @Autowired(required = false) private ThirdPartyRiskMonitor vendorRisk;
    @Autowired(required = false) private CryptoEraseService cryptoErase;
    @Autowired(required = false) private PolicyExportService policyExport;
    @Autowired(required = false) private BreachSimulationEngine breachSim;
    @Autowired(required = false) private ConsentFatigueDetector fatigueDetector;
    @Autowired(required = false) private AnomalyDetectionEngine anomalyEngine;
    @Autowired(required = false) private SSOService ssoService;
    @Autowired(required = false) private PasswordlessAuthService passwordlessAuth;
    @Autowired(required = false) private PAMModuleService pamService;
    @Autowired(required = false) private TokenizationService tokenService;
    @Autowired(required = false) private FileBasedSyncService fileSyncService;
    @Autowired(required = false) private LocalCAService caService;

    // ═══════════════════════════════════════════════════════════
    // PHASE 1: DARK PATTERN + PURPOSE VALIDATION
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/dark-patterns/rules")
    public ResponseEntity<?> darkPatternRules() {
        return ResponseEntity.ok(darkPatternDetector.getRules());
    }

    @PostMapping("/dark-patterns/scan")
    public ResponseEntity<?> scanDarkPatterns(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(darkPatternDetector.scanConsentFlow(
                (String) body.get("consentText"), (String) body.get("consentHTML"),
                body.containsKey("uiMetadata") ? (Map<String, Object>) body.get("uiMetadata") : null));
    }

    @GetMapping("/purpose/mappings")
    public ResponseEntity<?> purposeMappings() {
        initPurpose();
        return ResponseEntity.ok(purposeEngine.getAllPurposeMappings());
    }

    @PostMapping("/purpose/validate")
    public ResponseEntity<?> validatePurpose(@RequestBody Map<String, String> body) {
        initPurpose();
        return ResponseEntity.ok(purposeEngine.validatePurpose(
                body.getOrDefault("purpose", ""), body.getOrDefault("dataCategory", ""),
                body.getOrDefault("principalId", ""), body.getOrDefault("accessedBy", "")));
    }

    @GetMapping("/purpose/creep-alerts")
    public ResponseEntity<?> creepAlerts(@RequestParam(defaultValue = "20") int limit) {
        initPurpose();
        return ResponseEntity.ok(purposeEngine.getCreepAlerts(limit));
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 2: NO-CODE CONNECTORS + DATA LINEAGE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/connectors/templates")
    public ResponseEntity<?> connectorTemplates() {
        initConnectors();
        return ResponseEntity.ok(connectorService.getTemplates());
    }

    @PostMapping("/connectors/create")
    public ResponseEntity<?> createConnector(@RequestBody Map<String, Object> body) {
        initConnectors();
        return ResponseEntity.ok(connectorService.createConnector(
                (String) body.get("templateId"), (String) body.get("name"),
                (Map<String, String>) body.get("config")));
    }

    @GetMapping("/connectors/instances")
    public ResponseEntity<?> connectorInstances() {
        initConnectors();
        return ResponseEntity.ok(connectorService.getInstances());
    }

    @GetMapping("/lineage/graph")
    public ResponseEntity<?> lineageGraph() {
        initLineage();
        return ResponseEntity.ok(lineageService.getGraph());
    }

    @GetMapping("/lineage/impact")
    public ResponseEntity<?> lineageImpact(@RequestParam String dataCategory) {
        initLineage();
        return ResponseEntity.ok(lineageService.getImpactAnalysis(dataCategory));
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 3: UEBA + VENDOR RISK + CRYPTO ERASE
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/ueba/analyze")
    public ResponseEntity<?> uebaAnalyze(@RequestBody Map<String, Object> body) {
        initUEBA();
        return ResponseEntity.ok(uebaEngine.analyzeActivity(
                (String) body.get("entityId"), (String) body.getOrDefault("entityType", "OPERATOR"),
                (String) body.getOrDefault("eventType", "DATA_ACCESS"),
                (Map<String, Object>) body.get("context")));
    }

    @GetMapping("/ueba/soc-dashboard")
    public ResponseEntity<?> socDashboard() {
        initUEBA();
        return ResponseEntity.ok(uebaEngine.getSOCDashboard());
    }

    @GetMapping("/ueba/alerts")
    public ResponseEntity<?> uebaAlerts(@RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "20") int limit) {
        initUEBA();
        return ResponseEntity.ok(uebaEngine.getAlerts(status, limit));
    }

    @PostMapping("/vendor-risk/assess")
    public ResponseEntity<?> assessVendor(@RequestBody Map<String, Object> body) {
        initVendorRisk();
        return ResponseEntity.ok(vendorRisk.assessVendor(
                (String) body.get("vendorId"), (String) body.get("vendorName"),
                (Map<String, Boolean>) body.get("criteria"),
                (List<String>) body.get("dataCategories")));
    }

    @GetMapping("/vendor-risk/vendors")
    public ResponseEntity<?> vendorList(@RequestParam(defaultValue = "ACTIVE") String status) {
        initVendorRisk();
        return ResponseEntity.ok(vendorRisk.getVendors(status));
    }

    @GetMapping("/vendor-risk/criteria")
    public ResponseEntity<?> vendorCriteria() {
        initVendorRisk();
        return ResponseEntity.ok(vendorRisk.getCriteria());
    }

    @GetMapping("/crypto-erase/retention-policies")
    public ResponseEntity<?> retentionPolicies() {
        return ResponseEntity.ok(cryptoErase.getRetentionPolicies());
    }

    @PostMapping("/crypto-erase/generate-key")
    public ResponseEntity<?> generateKey(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(cryptoErase.generateKey(
                body.getOrDefault("entityId", ""), body.getOrDefault("entityType", ""),
                body.getOrDefault("retentionDays", "365")));
    }

    @PostMapping("/crypto-erase/erase")
    public ResponseEntity<?> cryptoEraseKey(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(cryptoErase.cryptoErase(
                body.getOrDefault("keyId", ""), body.getOrDefault("reason", ""),
                body.getOrDefault("authorizedBy", "")));
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 4: POLICY EXPORT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/export/dpdp-report")
    public ResponseEntity<?> dpdpReport(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(policyExport.generateDPDPReport(
                (String) body.getOrDefault("orgName", ""), (String) body.getOrDefault("sector", ""),
                body));
    }

    @GetMapping("/export/iso-soa")
    public ResponseEntity<?> isoSoA(@RequestParam(defaultValue = "QS-DPDP Enterprise") String orgName) {
        return ResponseEntity.ok(policyExport.generateISOSoA(orgName));
    }

    @GetMapping("/export/formats")
    public ResponseEntity<?> exportFormats() { return ResponseEntity.ok(policyExport.getFormats()); }

    // ═══════════════════════════════════════════════════════════
    // PHASE 6: BREACH SIMULATION
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/breach-sim/templates")
    public ResponseEntity<?> breachTemplates(@RequestParam(required = false) String sector) {
        return ResponseEntity.ok(breachSim.getTemplates(sector));
    }

    @PostMapping("/breach-sim/start")
    public ResponseEntity<?> startSimulation(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(breachSim.startSimulation(
                body.getOrDefault("templateId", ""), body.getOrDefault("teamLead", "")));
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 7: AI (FATIGUE + ANOMALY)
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/ai/consent-fatigue")
    public ResponseEntity<?> analyzeConsentFatigue(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(fatigueDetector.analyze(
                (String) body.getOrDefault("principalId", ""),
                ((Number) body.getOrDefault("timeSpentMs", 5000)).longValue(),
                Boolean.TRUE.equals(body.get("accepted")),
                ((Number) body.getOrDefault("totalConsentsToday", 0)).intValue(),
                ((Number) body.getOrDefault("acceptedToday", 0)).intValue(),
                (String) body.getOrDefault("consentTextLength", ""),
                (Map<String, Object>) body.get("interactionData")));
    }

    @GetMapping("/ai/consent-fatigue/thresholds")
    public ResponseEntity<?> fatigueThresholds() { return ResponseEntity.ok(fatigueDetector.getThresholds()); }

    @PostMapping("/ai/anomaly-detect")
    public ResponseEntity<?> detectAnomalies(@RequestBody Map<String, Object> signals) {
        return ResponseEntity.ok(anomalyEngine.detect(signals));
    }

    @GetMapping("/ai/anomaly-detect/signals")
    public ResponseEntity<?> anomalySignals() { return ResponseEntity.ok(anomalyEngine.getSignals()); }

    // ═══════════════════════════════════════════════════════════
    // PHASE 9: IDAM (SSO + PASSWORDLESS + PAM)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/sso/providers")
    public ResponseEntity<?> ssoProviders() { return ResponseEntity.ok(ssoService.getProviders()); }

    @PostMapping("/sso/login")
    public ResponseEntity<?> ssoLogin(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ssoService.initiateLogin(
                body.getOrDefault("providerId", ""), body.getOrDefault("redirectUri", "")));
    }

    @GetMapping("/auth/methods")
    public ResponseEntity<?> authMethods() { return ResponseEntity.ok(passwordlessAuth.getMethods()); }

    @PostMapping("/auth/otp")
    public ResponseEntity<?> sendOTP(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(passwordlessAuth.sendOTP(
                body.getOrDefault("identifier", ""), body.getOrDefault("channel", "SMS")));
    }

    @PostMapping("/auth/magic-link")
    public ResponseEntity<?> magicLink(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(passwordlessAuth.generateMagicLink(
                body.getOrDefault("email", ""), body.getOrDefault("redirectUri", "")));
    }

    @PostMapping("/auth/fido2")
    public ResponseEntity<?> fido2(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(passwordlessAuth.initiateFIDO2(body.getOrDefault("userId", "")));
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<?> verifyAuth(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(passwordlessAuth.verify(
                body.getOrDefault("challengeId", ""), body.getOrDefault("verificationData", "")));
    }

    @PostMapping("/pam/request")
    public ResponseEntity<?> pamRequest(@RequestBody Map<String, Object> body) {
        initPAM();
        return ResponseEntity.ok(pamService.requestAccess(
                (String) body.getOrDefault("userId", ""), (String) body.getOrDefault("privilegeLevel", ""),
                (String) body.getOrDefault("reason", ""), ((Number) body.getOrDefault("durationMinutes", 60)).intValue()));
    }

    @PostMapping("/pam/approve")
    public ResponseEntity<?> pamApprove(@RequestBody Map<String, String> body) {
        initPAM();
        return ResponseEntity.ok(pamService.approveAccess(
                body.getOrDefault("sessionId", ""), body.getOrDefault("approvedBy", "")));
    }

    @PostMapping("/pam/break-glass")
    public ResponseEntity<?> breakGlass(@RequestBody Map<String, String> body) {
        initPAM();
        return ResponseEntity.ok(pamService.breakGlass(
                body.getOrDefault("userId", ""), body.getOrDefault("reason", "")));
    }

    @GetMapping("/pam/sessions")
    public ResponseEntity<?> pamSessions() {
        initPAM();
        return ResponseEntity.ok(pamService.getActiveSessions());
    }

    // ═══════════════════════════════════════════════════════════
    // PHASE 10: SECURITY (TOKENIZATION)
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/security/tokenize")
    public ResponseEntity<?> tokenize(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tokenService.tokenize(
                body.getOrDefault("value", ""), body.getOrDefault("dataType", "")));
    }

    @PostMapping("/security/detokenize")
    public ResponseEntity<?> detokenize(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(tokenService.detokenize(
                body.getOrDefault("token", ""), body.getOrDefault("authorizedBy", ""),
                body.getOrDefault("purpose", "")));
    }

    @PostMapping("/security/mask")
    public ResponseEntity<?> mask(@RequestBody Map<String, String> body) {
        String masked = tokenService.mask(body.getOrDefault("value", ""), body.getOrDefault("dataType", ""));
        return ResponseEntity.ok(Map.of("original", body.getOrDefault("value", ""), "masked", masked,
                "dataType", body.getOrDefault("dataType", "")));
    }

    @GetMapping("/security/token-stats")
    public ResponseEntity<?> tokenStats() { return ResponseEntity.ok(tokenService.getStatistics()); }

    // ═══════════════════════════════════════════════════════════
    // PHASE 11: AIR-GAPPED (FILE SYNC + LOCAL CA)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/air-gap/capabilities")
    public ResponseEntity<?> airGapCapabilities() { return ResponseEntity.ok(fileSyncService.getCapabilities()); }

    @PostMapping("/air-gap/export")
    public ResponseEntity<?> airGapExport(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(fileSyncService.exportSyncPackage(
                (String) body.getOrDefault("packageType", "CONSENTS"),
                body, (String) body.get("exportPath")));
    }

    @GetMapping("/ca/status")
    public ResponseEntity<?> caStatus() {
        initCA();
        return ResponseEntity.ok(caService.getCAStatus());
    }

    @PostMapping("/ca/issue")
    public ResponseEntity<?> issueCert(@RequestBody Map<String, Object> body) {
        initCA();
        return ResponseEntity.ok(caService.issueCertificate(
                (String) body.getOrDefault("commonName", ""), (String) body.getOrDefault("ou", ""),
                ((Number) body.getOrDefault("validDays", 365)).intValue()));
    }

    @GetMapping("/ca/certificates")
    public ResponseEntity<?> caCerts() {
        initCA();
        return ResponseEntity.ok(caService.getCertificates());
    }

    // ═══════════════════════════════════════════════════════════
    // PLATFORM STATUS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/status")
    public ResponseEntity<?> v2Status() {
        Map<String, Object> modules = new LinkedHashMap<>();
        modules.put("darkPatternDetector", darkPatternDetector != null);
        modules.put("purposeValidation", purposeEngine != null);
        modules.put("noCodeConnectors", connectorService != null);
        modules.put("dataLineage", lineageService != null);
        modules.put("ueba", uebaEngine != null);
        modules.put("vendorRisk", vendorRisk != null);
        modules.put("cryptoErase", cryptoErase != null);
        modules.put("policyExport", policyExport != null);
        modules.put("breachSimulation", breachSim != null);
        modules.put("consentFatigue", fatigueDetector != null);
        modules.put("anomalyDetection", anomalyEngine != null);
        modules.put("sso", ssoService != null);
        modules.put("passwordlessAuth", passwordlessAuth != null);
        modules.put("pam", pamService != null);
        modules.put("tokenization", tokenService != null);
        modules.put("fileSync", fileSyncService != null);
        modules.put("localCA", caService != null);

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("version", "2.0.0");
        status.put("platform", "QS-DPDP Enterprise (Enhanced)");
        status.put("modules", modules);
        status.put("totalNewModules", 17);
        status.put("backwardCompatible", true);
        status.put("apiPrefix", "/api/v2");
        return ResponseEntity.ok(status);
    }

    // Init helpers
    private void initPurpose() { if (purposeEngine != null && !purposeEngine.isInitialized()) try { purposeEngine.initialize(); } catch (Exception e) {} }
    private void initConnectors() { if (connectorService != null && !connectorService.isInitialized()) try { connectorService.initialize(); } catch (Exception e) {} }
    private void initLineage() { if (lineageService != null && !lineageService.isInitialized()) try { lineageService.initialize(); } catch (Exception e) {} }
    private void initUEBA() { if (uebaEngine != null && !uebaEngine.isInitialized()) try { uebaEngine.initialize(); } catch (Exception e) {} }
    private void initVendorRisk() { if (vendorRisk != null && !vendorRisk.isInitialized()) try { vendorRisk.initialize(); } catch (Exception e) {} }
    private void initPAM() { if (pamService != null && !pamService.isInitialized()) try { pamService.initialize(); } catch (Exception e) {} }
    private void initCA() { if (caService != null && !caService.isInitialized()) try { caService.initialize(); } catch (Exception e) {} }
}
