package com.qsdpdp.web.api;

import com.qsdpdp.compliance.rbi.RBIComplianceEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RBI Compliance Controller — Board-level compliance dashboard API
 * 
 * @version 1.0.0
 * @since Phase 5
 */
@RestController
@RequestMapping("/api/rbi-compliance")
public class RBIComplianceController {

    private static final Logger logger = LoggerFactory.getLogger(RBIComplianceController.class);

    @Autowired(required = false)
    private RBIComplianceEngine rbiEngine;

    /** GET /api/rbi-compliance/domains — All 12 domains with controls */
    @GetMapping("/domains")
    public ResponseEntity<?> getAllDomains() {
        ensureInit();
        return ResponseEntity.ok(Map.of("domains", rbiEngine.getAllDomains()));
    }

    /** GET /api/rbi-compliance/score — Overall compliance score */
    @GetMapping("/score")
    public ResponseEntity<Map<String, Object>> getScore() {
        ensureInit();
        return ResponseEntity.ok(rbiEngine.getOverallScore());
    }

    /** POST /api/rbi-compliance/score — Score a control */
    @PostMapping("/score")
    public ResponseEntity<Map<String, Object>> scoreControl(@RequestBody Map<String, Object> body) {
        ensureInit();
        return ResponseEntity.ok(rbiEngine.scoreControl(
                (String) body.getOrDefault("domainId", ""),
                (String) body.getOrDefault("controlId", ""),
                body.containsKey("score") ? ((Number) body.get("score")).intValue() : 0,
                (String) body.getOrDefault("maturityLevel", "INITIAL"),
                (String) body.getOrDefault("evidence", ""),
                (String) body.getOrDefault("assessedBy", "admin")
        ));
    }

    /** POST /api/rbi-compliance/raci — Set RACI matrix */
    @PostMapping("/raci")
    public ResponseEntity<Map<String, Object>> setRACI(@RequestBody Map<String, String> body) {
        ensureInit();
        return ResponseEntity.ok(rbiEngine.setRACI(
                body.getOrDefault("domainId", ""),
                body.getOrDefault("controlId", ""),
                body.getOrDefault("responsible", ""),
                body.getOrDefault("accountable", ""),
                body.getOrDefault("consulted", ""),
                body.getOrDefault("informed", "")
        ));
    }

    private void ensureInit() {
        if (rbiEngine != null && !rbiEngine.isInitialized()) {
            try { rbiEngine.initialize(); } catch (Exception e) {
                logger.debug("RBIComplianceEngine init skipped: {}", e.getMessage());
            }
        }
    }
}
