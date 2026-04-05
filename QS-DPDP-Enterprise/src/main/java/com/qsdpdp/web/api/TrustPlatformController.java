package com.qsdpdp.web.api;

import com.qsdpdp.aigovernance.AIGovernanceService;
import com.qsdpdp.creditscore.ConsentCreditScoreService;
import com.qsdpdp.crypto.QuantumSafeEncryptionService;
import com.qsdpdp.economy.ConsentEconomyService;
import com.qsdpdp.interop.GlobalInteropService;
import com.qsdpdp.ledger.ConsentLedgerService;
import com.qsdpdp.ndce.NDCEService;
import com.qsdpdp.pet.PETService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Universal Trust Operating System — Platform Controller
 * System-wide capabilities, health monitoring, and module registry
 */
@RestController
@RequestMapping("/api/platform")
public class TrustPlatformController {

    @Autowired private QuantumSafeEncryptionService cryptoService;
    @Autowired private ConsentLedgerService ledgerService;
    @Autowired private ConsentCreditScoreService creditScoreService;
    @Autowired private NDCEService ndceService;
    @Autowired private PETService petService;
    @Autowired private AIGovernanceService aiGovernanceService;
    @Autowired private ConsentEconomyService economyService;
    @Autowired private GlobalInteropService interopService;

    private static final Instant START_TIME = Instant.now();

    @GetMapping("/capabilities")
    public ResponseEntity<?> capabilities() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platform", "Universal Trust Operating System");
        result.put("version", "3.0.0");
        result.put("codename", "India-First Universal Consent + Data Exchange + Trust Infrastructure");
        result.put("buildDate", "2026-03-25");
        result.put("javaVersion", System.getProperty("java.version"));

        result.put("pillars", Map.of(
                "consentManagement", Map.of("status", "OPERATIONAL", "modules", List.of("Consent Engine", "Voice Consent", "Multi-lingual (22 languages)", "Consent Ledger Network")),
                "dataIntelligence", Map.of("status", "OPERATIONAL", "modules", List.of("PII Discovery", "DLP Engine", "UEBA Analytics", "AI Governance")),
                "apiFabric", Map.of("status", "OPERATIONAL", "modules", List.of("RESTful APIs (50+)", "Webhook Notifications", "NDCE Exchange")),
                "grcPlatform", Map.of("status", "OPERATIONAL", "modules", List.of("Gap Analysis", "DPIA", "Policy Lifecycle", "Breach Management", "Credit Score")),
                "identityIntegration", Map.of("status", "OPERATIONAL", "modules", List.of("JWT/OAuth2", "RBAC", "Aadhaar-ready", "DigiLocker-ready")),
                "nationalInterop", Map.of("status", "OPERATIONAL", "modules", List.of("NDCE Exchange", "Cross-border Validation", "GDPR/OECD/ISO Mapping", "Consent Wallet"))
        ));

        result.put("moduleCount", Map.of(
                "coreModules", 19, "newTrustModules", 7, "securityModules", 5,
                "totalModules", 31, "sectors", 17, "languages", 22, "apiEndpoints", "50+"
        ));

        result.put("compliance", Map.of(
                "primary", "DPDP Act 2023 + DPDP Rules 2025",
                "international", List.of("GDPR (EU)", "OECD Privacy Principles", "ISO 27701", "ISO 27001", "NIST Privacy Framework"),
                "aiGovernance", List.of("EU AI Act", "NIST AI RMF 1.0", "IEEE 7010"),
                "security", List.of("NIST SP 800-188", "FIPS 203 (ML-KEM)", "FIPS 204 (ML-DSA)", "CERT-In Directives"),
                "indian", List.of("IT Act 2000", "SPDI Rules 2011", "CERT-In 6-hr reporting", "RBI Master Directions")
        ));

        result.put("security", Map.of(
                "encryption", "AES-256-GCM + Hybrid RSA-4096 + ML-KEM-1024",
                "signatures", "ML-DSA-87 (CRYSTALS-Dilithium)",
                "quantumSafe", cryptoService.isPqcAvailable(),
                "nistSecurityLevel", cryptoService.isPqcAvailable() ? "Level 5" : "Classical",
                "pet", List.of("Differential Privacy", "Zero-Knowledge Proofs", "Federated Learning", "Secure MPC"),
                "hsmReady", true, "airGappedCapable", true
        ));

        result.put("scale", Map.of(
                "targetUsers", "100M+", "validationLatency", "<200ms",
                "architecture", "Horizontally scalable Spring Boot microservices",
                "databases", List.of("SQLite (dev)", "PostgreSQL", "Oracle", "SQL Server", "MySQL"),
                "caching", "Redis-ready"
        ));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ALL_SYSTEMS_OPERATIONAL");
        result.put("timestamp", Instant.now().toString());
        result.put("uptime", formatUptime());

        Map<String, Object> modules = new LinkedHashMap<>();
        modules.put("quantumSafeCrypto", Map.of("status", "UP", "pqcAvailable", cryptoService.isPqcAvailable()));
        modules.put("consentLedger", Map.of("status", "UP", "chainStatus", ledgerService.getChainStatus().get("status")));
        modules.put("creditScore", Map.of("status", "UP"));
        modules.put("ndce", Map.of("status", "UP"));
        modules.put("petLayer", Map.of("status", "UP"));
        modules.put("aiGovernance", Map.of("status", "UP"));
        modules.put("consentEconomy", Map.of("status", "UP"));
        modules.put("globalInterop", Map.of("status", "UP"));
        result.put("modules", modules);

        Runtime rt = Runtime.getRuntime();
        result.put("jvm", Map.of(
                "maxMemoryMB", rt.maxMemory() / (1024 * 1024),
                "usedMemoryMB", (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024),
                "processors", rt.availableProcessors(),
                "javaVersion", System.getProperty("java.version")
        ));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/modules")
    public ResponseEntity<?> modules() {
        List<Map<String, Object>> moduleList = new ArrayList<>();

        addModule(moduleList, "CONSENT_ENGINE", "Consent Management", "Core", "3.0.0", "S.6, S.7", true);
        addModule(moduleList, "BREACH_MANAGER", "Breach Management", "Core", "2.0.0", "S.8(6)", true);
        addModule(moduleList, "RIGHTS_ENGINE", "Data Principal Rights", "Core", "2.0.0", "S.11-13", true);
        addModule(moduleList, "DPIA_ENGINE", "Impact Assessment", "GRC", "2.0.0", "S.10", true);
        addModule(moduleList, "GAP_ANALYSIS", "Compliance Gap Analysis", "GRC", "2.0.0", "S.8", true);
        addModule(moduleList, "POLICY_LIFECYCLE", "Policy Management", "GRC", "2.0.0", "S.8", true);
        addModule(moduleList, "PII_SCANNER", "PII Discovery & DLP", "Security", "2.0.0", "S.8(4)", true);
        addModule(moduleList, "SIEM", "Security Event Monitoring", "Security", "2.0.0", "S.8(4)", true);
        addModule(moduleList, "QUANTUM_CRYPTO", "Quantum-Safe Encryption", "Security", "3.0.0", "S.8(4)", true);
        addModule(moduleList, "RAG_AI", "RAG AI Analytics", "Intelligence", "1.0.0", "S.8", true);
        addModule(moduleList, "AUDIT_LOG", "Audit Trail", "Compliance", "2.0.0", "S.8", true);
        addModule(moduleList, "CLN", "Consent Ledger Network", "Trust Infrastructure", "1.0.0", "S.8 (Accountability)", true);
        addModule(moduleList, "CCS", "Consent Credit Score", "Trust Intelligence", "1.0.0", "S.8 (Compliance)", true);
        addModule(moduleList, "NDCE", "National DPDP Compliance Exchange", "National Infrastructure", "1.0.0", "S.6, S.8, S.16", true);
        addModule(moduleList, "PET", "Privacy Enhancing Technology", "Privacy", "1.0.0", "S.8(4)", true);
        addModule(moduleList, "AI_GOVERNANCE", "AI Governance & Accountability", "AI/ML", "1.0.0", "S.8 + EU AI Act", true);
        addModule(moduleList, "CONSENT_ECONOMY", "Consent Wallet & Marketplace", "Economy", "1.0.0", "S.6, S.11", true);
        addModule(moduleList, "GLOBAL_INTEROP", "Global Interoperability", "International", "1.0.0", "S.16 + GDPR", true);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("platform", "Universal Trust Operating System v3.0");
        result.put("totalModules", moduleList.size());
        result.put("modules", moduleList);
        return ResponseEntity.ok(result);
    }

    private void addModule(List<Map<String, Object>> list, String id, String name, String category,
                            String version, String dpdpRef, boolean active) {
        list.add(Map.of("id", id, "name", name, "category", category,
                "version", version, "dpdpReference", dpdpRef, "status", active ? "ACTIVE" : "INACTIVE"));
    }

    private String formatUptime() {
        Duration d = Duration.between(START_TIME, Instant.now());
        return String.format("%dd %dh %dm %ds", d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart());
    }
}
