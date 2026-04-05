package com.qsdpdp.web.api;

import com.qsdpdp.consent.ConsentValidationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Consent Validation Controller — Real-time enforcement API
 * 
 * @version 1.0.0
 * @since Phase 4
 */
@RestController
@RequestMapping("/api/consent-validation")
public class ConsentValidationController {

    private static final Logger logger = LoggerFactory.getLogger(ConsentValidationController.class);

    @Autowired(required = false)
    private ConsentValidationEngine validationEngine;

    /**
     * POST /api/consent-validation/validate
     * Validate a data access request against consent
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> request) {
        ensureInit();
        String principalId = (String) request.getOrDefault("principalId", "");
        String dataCategory = (String) request.getOrDefault("dataCategory", "");
        String purpose = (String) request.getOrDefault("purpose", "");
        String accessedBy = (String) request.getOrDefault("accessedBy", "SYSTEM");
        int volume = request.containsKey("volume") ? ((Number) request.get("volume")).intValue() : 1;
        return ResponseEntity.ok(validationEngine.validateAccess(principalId, dataCategory, purpose, accessedBy, volume));
    }

    /** GET /api/consent-validation/violations?status=OPEN&limit=50 */
    @GetMapping("/violations")
    public ResponseEntity<?> getViolations(
            @RequestParam(defaultValue = "OPEN") String status,
            @RequestParam(defaultValue = "50") int limit) {
        ensureInit();
        return ResponseEntity.ok(Map.of(
                "status", status,
                "violations", validationEngine.getViolationsByStatus(status, limit)
        ));
    }

    /** GET /api/consent-validation/statistics */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics() {
        ensureInit();
        return ResponseEntity.ok(validationEngine.getStatistics());
    }

    /** POST /api/consent-validation/violations/{id}/resolve */
    @PostMapping("/violations/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolve(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        ensureInit();
        return ResponseEntity.ok(validationEngine.resolveViolation(id,
                body.getOrDefault("remediation", "Reviewed and resolved")));
    }

    private void ensureInit() {
        if (validationEngine != null && !validationEngine.isInitialized()) {
            try { validationEngine.initialize(); } catch (Exception e) {
                logger.debug("ConsentValidationEngine init skipped: {}", e.getMessage());
            }
        }
    }
}
