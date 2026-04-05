package com.qsdpdp.web.api;

import com.qsdpdp.breach.BreachService;
import com.qsdpdp.chatbot.ChatQuery;
import com.qsdpdp.chatbot.ChatResponse;
import com.qsdpdp.chatbot.ChatbotService;
import com.qsdpdp.core.ComplianceEngine;
import com.qsdpdp.consent.ConsentService;
import com.qsdpdp.crypto.QuantumSafeEncryptionService;
import com.qsdpdp.dlp.DLPService;
import com.qsdpdp.rag.RAGEvaluator;
import com.qsdpdp.siem.SIEMService;
import com.qsdpdp.siem.ThreatIntelligenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Unified Dashboard, Chatbot, and System REST API
 * Provides cross-module dashboard data, chatbot interface, and system health.
 *
 * @version 3.0.0
 * @since Phase 2
 */
@RestController("dashboardApiController")
@RequestMapping("/api/v1")
public class DashboardController {

    @Autowired(required = false) private ComplianceEngine complianceEngine;
    @Autowired(required = false) private ConsentService consentService;
    @Autowired(required = false) private BreachService breachService;
    @Autowired(required = false) private SIEMService siemService;
    @Autowired(required = false) private DLPService dlpService;
    @Autowired(required = false) private RAGEvaluator ragEvaluator;
    @Autowired(required = false) private ThreatIntelligenceService threatIntelService;
    @Autowired(required = false) private ChatbotService chatbotService;
    @Autowired(required = false) private QuantumSafeEncryptionService cryptoService;

    // ═══ SYSTEM HEALTH ═══

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> systemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("platform", "QS-DPDP Enterprise v3.0");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("modules", Map.of(
                "consent", consentService != null && consentService.isInitialized(),
                "breach", breachService != null && breachService.isInitialized(),
                "siem", siemService != null && siemService.isInitialized(),
                "dlp", dlpService != null && dlpService.isInitialized(),
                "threatIntel", threatIntelService != null && threatIntelService.isInitialized(),
                "crypto", cryptoService != null && cryptoService.isInitialized(),
                "pqcAvailable", cryptoService != null && cryptoService.isPqcAvailable()));
        return ResponseEntity.ok(health);
    }

    // ═══ UNIFIED DASHBOARD ═══

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        try {
            Map<String, Object> dashboard = new LinkedHashMap<>();

            // RAG (Red/Amber/Green) Compliance Scores per module
            Map<String, Object> ragScores = new LinkedHashMap<>();
            for (String module : List.of("consent", "breach", "rights", "dlp", "siem", "policy", "dpia", "gap")) {
                try {
                    ragScores.put(module, ragEvaluator.evaluateModule(module));
                } catch (Exception e) {
                    ragScores.put(module, Map.of("error", e.getMessage()));
                }
            }
            dashboard.put("ragScores", ragScores);

            // Module statistics
            dashboard.put("consent", consentService.getStatistics());
            dashboard.put("breach", breachService.getStatistics());
            dashboard.put("threatIntel", threatIntelService.getStatistics());

            // Crypto capabilities
            dashboard.put("crypto", cryptoService.getCryptoCapabilities());

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard/compliance-score/{module}")
    public ResponseEntity<Object> getModuleComplianceScore(@PathVariable String module) {
        try {
            return ResponseEntity.ok(ragEvaluator.evaluateModule(module));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ CHATBOT (delegated to ChatbotController at /api/v1/chatbot) ═══
    // Chatbot queries handled by ChatbotController — no duplicate mapping

    // ═══ CRYPTO CAPABILITIES ═══

    @GetMapping("/crypto/capabilities")
    public ResponseEntity<Object> getCryptoCapabilities() {
        try {
            return ResponseEntity.ok(cryptoService.getCryptoCapabilities());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/crypto/encrypt")
    public ResponseEntity<Object> encryptData(@RequestBody Map<String, String> request) {
        try {
            String plaintext = request.get("data");
            if (plaintext == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "data is required"));
            }

            QuantumSafeEncryptionService.HybridEncryptedData encrypted =
                    cryptoService.encryptHybrid(plaintext);

            return ResponseEntity.ok(Map.of(
                    "algorithm", encrypted.getAlgorithm(),
                    "ciphertext", Base64.getEncoder().encodeToString(encrypted.getCiphertext()),
                    "pqcUsed", encrypted.getPqcEncapsulation() != null));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/crypto/mask-pii")
    public ResponseEntity<Object> maskPII(@RequestBody Map<String, String> request) {
        try {
            String pii = request.get("value");
            String type = request.getOrDefault("type", "GENERIC");

            if (pii == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "value is required"));
            }

            String masked = cryptoService.maskPII(pii,
                    QuantumSafeEncryptionService.PIIMaskType.valueOf(type.toUpperCase()));

            return ResponseEntity.ok(Map.of(
                    "original", pii,
                    "masked", masked,
                    "type", type));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
