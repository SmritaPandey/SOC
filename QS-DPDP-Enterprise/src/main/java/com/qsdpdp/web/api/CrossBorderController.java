package com.qsdpdp.web.api;

import com.qsdpdp.crossborder.CrossBorderTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Cross-Border Data Transfer REST Controller — DPDP Act §9/§16
 * Provides REST API for managing cross-border data transfer assessments,
 * transfer records, and country whitelist management.
 *
 * @version 1.0.0
 * @since Phase 4
 */
@RestController("crossBorderApiController")
@RequestMapping("/api/v1/crossborder")
public class CrossBorderController {

    @Autowired(required = false)
    private CrossBorderTransferService crossBorderService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "cross-border-transfers",
                "status", "UP",
                "version", "1.0.0",
                "dpdpSections", "§9, §16",
                "initialized", crossBorderService != null && crossBorderService.isInitialized()));
    }

    // ═══════════════════════════════════════════════════════════
    // TRANSFER ASSESSMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Assess whether a transfer to a specific country is permitted.
     * Body: { "countryCode": "US", "dataCategories": "PII,Financial", "purpose": "Analytics" }
     */
    @PostMapping("/assess")
    public ResponseEntity<Object> assessTransfer(@RequestBody Map<String, String> body) {
        try {
            String countryCode = body.get("countryCode");
            String dataCategories = body.getOrDefault("dataCategories", "GENERAL");
            String purpose = body.getOrDefault("purpose", "Processing");

            if (countryCode == null || countryCode.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "countryCode is required"));
            }

            var decision = crossBorderService.evaluateTransfer(countryCode, dataCategories, purpose);

            return ResponseEntity.ok(Map.of(
                    "countryCode", countryCode.toUpperCase(),
                    "allowed", decision.isAllowed(),
                    "status", decision.getStatus(),
                    "reason", decision.getReason(),
                    "requiredActions", decision.getRequiredActions() != null ? decision.getRequiredActions() : List.of(),
                    "dpdpSection", "§9"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TRANSFER RECORDS
    // ═══════════════════════════════════════════════════════════

    /**
     * Record a new cross-border data transfer.
     */
    @PostMapping("/transfers")
    public ResponseEntity<Map<String, Object>> recordTransfer(@RequestBody Map<String, Object> body) {
        try {
            String countryCode = (String) body.get("countryCode");
            String destinationOrg = (String) body.getOrDefault("destinationOrg", "Unknown");
            String dataCategories = (String) body.getOrDefault("dataCategories", "GENERAL");
            int principalCount = body.containsKey("principalCount")
                    ? ((Number) body.get("principalCount")).intValue() : 0;
            String legalBasis = (String) body.getOrDefault("legalBasis", "CONSENT");
            String purpose = (String) body.getOrDefault("purpose", "Processing");
            String requestedBy = (String) body.getOrDefault("requestedBy", "SYSTEM");

            if (countryCode == null || countryCode.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "countryCode is required"));
            }

            String refNum = crossBorderService.recordTransfer(
                    countryCode, destinationOrg, dataCategories,
                    principalCount, legalBasis, purpose, requestedBy);

            if (refNum == null) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Transfer recording failed"));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "referenceNumber", refNum,
                    "countryCode", countryCode.toUpperCase(),
                    "destinationOrg", destinationOrg,
                    "status", "PENDING",
                    "message", "Cross-border transfer recorded for review per DPDP §9",
                    "dpdpSection", "§9"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Object> getStats() {
        try {
            var stats = crossBorderService.getStatistics();
            stats.put("dpdpSections", "§9, §16");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
