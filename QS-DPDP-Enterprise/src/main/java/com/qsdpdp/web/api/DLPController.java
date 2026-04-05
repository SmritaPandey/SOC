package com.qsdpdp.web.api;

import com.qsdpdp.dlp.DLPService;
import com.qsdpdp.pii.PIIScanner;
import com.qsdpdp.pii.PIIScanResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * DLP + PII Scanner REST API
 * Data Loss Prevention and PII detection endpoints.
 *
 * @version 3.0.0
 * @since Phase 2
 */
@RestController("dlpApiController")
@RequestMapping("/api/v1/dlp")
public class DLPController {

    @Autowired(required = false) private DLPService dlpService;
    @Autowired(required = false) private PIIScanner piiScanner;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "dlp",
                "status", "UP",
                "dlpInitialized", dlpService.isInitialized()));
    }

    /**
     * Scan content for PII (Personally Identifiable Information)
     */
    @PostMapping("/pii-scan")
    public ResponseEntity<Object> scanPII(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            if (content == null || content.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "content is required"));
            }

            PIIScanResult result = piiScanner.scanText(content, "API");

            return ResponseEntity.ok(Map.of(
                    "piiDetected", result.getTotalFindings() > 0,
                    "totalFindings", result.getFindings().size(),
                    "findings", result.getFindings()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get open DLP incidents
     */
    @GetMapping("/incidents")
    public ResponseEntity<Object> getOpenIncidents() {
        try {
            return ResponseEntity.ok(Map.of(
                    "incidents", dlpService.getOpenIncidents()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get DLP policies
     */
    @GetMapping("/policies")
    public ResponseEntity<Object> listPolicies() {
        try {
            return ResponseEntity.ok(Map.of(
                    "policies", dlpService.getAllPolicies()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get DLP statistics for dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        try {
            return ResponseEntity.ok(dlpService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
