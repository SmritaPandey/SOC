package com.qsdpdp.web.api;

import com.qsdpdp.rag.AIRiskScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI Risk Controller — Risk scoring & prediction API
 * 
 * @version 1.0.0
 * @since Phase 11
 */
@RestController
@RequestMapping("/api/ai-risk")
public class AIRiskController {

    private static final Logger logger = LoggerFactory.getLogger(AIRiskController.class);

    @Autowired(required = false)
    private AIRiskScoringEngine riskEngine;

    /** GET /api/ai-risk/principal/{id} — Individual risk score */
    @GetMapping("/principal/{id}")
    public ResponseEntity<Map<String, Object>> principalRisk(@PathVariable String id) {
        ensureInit();
        return ResponseEntity.ok(riskEngine.calculatePrincipalRisk(id));
    }

    /** GET /api/ai-risk/organization — Org-wide risk score */
    @GetMapping("/organization")
    public ResponseEntity<Map<String, Object>> orgRisk() {
        ensureInit();
        return ResponseEntity.ok(riskEngine.calculateOrgRisk());
    }

    /** POST /api/ai-risk/query — Process consent query */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> processQuery(@RequestBody Map<String, String> body) {
        ensureInit();
        return ResponseEntity.ok(riskEngine.processQuery(body.getOrDefault("query", "")));
    }

    private void ensureInit() {
        if (riskEngine != null && !riskEngine.isInitialized()) {
            try { riskEngine.initialize(); } catch (Exception e) {
                logger.debug("AIRiskScoringEngine init skipped: {}", e.getMessage());
            }
        }
    }
}
