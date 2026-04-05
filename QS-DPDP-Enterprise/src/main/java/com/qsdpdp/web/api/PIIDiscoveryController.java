package com.qsdpdp.web.api;

import com.qsdpdp.pii.PIIClassificationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * PII Discovery Controller — Classification & Lineage API
 * 
 * @version 1.0.0
 * @since Phase 3
 */
@RestController
@RequestMapping("/api/pii-discovery")
public class PIIDiscoveryController {

    private static final Logger logger = LoggerFactory.getLogger(PIIDiscoveryController.class);

    @Autowired(required = false)
    private PIIClassificationEngine classEngine;

    /** GET /api/pii-discovery/rules — All classification rules */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> getAllRules() {
        ensureInit();
        return ResponseEntity.ok(classEngine.getAllRules());
    }

    /** POST /api/pii-discovery/classify — Classify a PII finding */
    @PostMapping("/classify")
    public ResponseEntity<Map<String, Object>> classify(@RequestBody Map<String, String> body) {
        ensureInit();
        return ResponseEntity.ok(classEngine.classify(
                body.getOrDefault("piiType", ""),
                body.getOrDefault("dataSource", ""),
                body.getOrDefault("dataLocation", "")
        ));
    }

    /** POST /api/pii-discovery/lineage — Record data lineage */
    @PostMapping("/lineage")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> recordLineage(@RequestBody Map<String, Object> body) {
        ensureInit();
        return ResponseEntity.ok(classEngine.recordLineage(
                (String) body.getOrDefault("piiType", ""),
                (String) body.getOrDefault("sourceSystem", ""),
                (List<String>) body.getOrDefault("processingSystems", List.of()),
                (List<String>) body.getOrDefault("storageSystems", List.of()),
                (List<String>) body.getOrDefault("sharingParties", List.of()),
                (String) body.getOrDefault("deletionPolicy", "")
        ));
    }

    /** GET /api/pii-discovery/lineage/{piiType} — Get lineage */
    @GetMapping("/lineage/{piiType}")
    public ResponseEntity<?> getLineage(@PathVariable String piiType) {
        ensureInit();
        return ResponseEntity.ok(Map.of("piiType", piiType, "lineage", classEngine.getLineage(piiType)));
    }

    private void ensureInit() {
        if (classEngine != null && !classEngine.isInitialized()) {
            try { classEngine.initialize(); } catch (Exception e) {
                logger.debug("PIIClassificationEngine init skipped: {}", e.getMessage());
            }
        }
    }
}
