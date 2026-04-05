package com.qsdpdp.web;

import com.qsdpdp.licensing.License;
import com.qsdpdp.licensing.LicensingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Licensing REST Controller
 * License activation, validation, pricing, agreements, usage, history
 *
 * @version 1.0.0
 * @since Sprint 6
 */

@RestController
@RequestMapping("/api/licensing")
public class LicensingController {

    private static final Logger logger = LoggerFactory.getLogger(LicensingController.class);

    @Autowired
    private LicensingService licensingService;

    // ═══════════════════════════════════════════════════════════
    // LICENSE STATUS & ACTIVATION
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            License license = licensingService.getCurrentLicense();
            return ResponseEntity.ok(Map.of("license", licenseToMap(license)));
        } catch (Exception e) {
            logger.error("Failed to get license status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get status: " + e.getMessage()));
        }
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activate(@RequestBody Map<String, String> payload) {
        try {
            String licenseKey = payload.getOrDefault("licenseKey", "");
            String orgName = payload.getOrDefault("organizationName", "");
            String email = payload.getOrDefault("contactEmail", "");
            String activatedBy = payload.getOrDefault("activatedBy", "admin");

            License license = licensingService.activate(licenseKey, orgName, email, activatedBy);
            return ResponseEntity.ok(Map.of(
                    "status", "activated",
                    "license", licenseToMap(license),
                    "message", "License activated successfully: " + license.getType()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to activate license", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to activate: " + e.getMessage()));
        }
    }

    @PostMapping("/activate-file")
    public ResponseEntity<?> activateWithFile(@RequestBody Map<String, String> payload) {
        try {
            String licenseFileContent = payload.getOrDefault("licenseFile", "");
            String activatedBy = payload.getOrDefault("activatedBy", "admin");

            if (licenseFileContent.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "licenseFile is required"));
            }

            License license = licensingService.activateWithLicenseFile(licenseFileContent, activatedBy);
            return ResponseEntity.ok(Map.of(
                    "status", "activated",
                    "license", licenseToMap(license),
                    "hardwareBound", true,
                    "message", "License activated via signed file (hardware-bound): " + license.getType()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to activate license file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to activate file: " + e.getMessage()));
        }
    }

    @GetMapping("/fingerprint")
    public ResponseEntity<?> getFingerprint() {
        try {
            return ResponseEntity.ok(licensingService.getHardwareFingerprint());
        } catch (Exception e) {
            logger.error("Failed to get hardware fingerprint", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get fingerprint: " + e.getMessage()));
        }
    }

    @PostMapping("/deactivate")
    public ResponseEntity<?> deactivate(@RequestBody Map<String, String> payload) {
        try {
            String deactivatedBy = payload.getOrDefault("deactivatedBy", "admin");
            licensingService.deactivate(deactivatedBy);
            return ResponseEntity.ok(Map.of(
                    "status", "deactivated",
                    "message", "License deactivated — reverted to demo mode"));
        } catch (Exception e) {
            logger.error("Failed to deactivate license", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to deactivate: " + e.getMessage()));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> payload) {
        try {
            String licenseKey = payload.getOrDefault("licenseKey", "");
            boolean valid = licensingService.isValidKeyFormat(licenseKey);
            return ResponseEntity.ok(Map.of(
                    "valid", valid,
                    "licenseKey", licenseKey,
                    "message", valid ? "License key format is valid" : "Invalid license key format"));
        } catch (Exception e) {
            logger.error("Failed to validate license key", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to validate: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FEATURES & USAGE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/features")
    public ResponseEntity<?> getFeatures() {
        try {
            License license = licensingService.getCurrentLicense();
            return ResponseEntity.ok(Map.of(
                    "licenseType", license.getType().name(),
                    "features", license.getFeatures() != null ? license.getFeatures() : "{}"));
        } catch (Exception e) {
            logger.error("Failed to get features", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get features: " + e.getMessage()));
        }
    }

    @GetMapping("/usage")
    public ResponseEntity<?> getUsage() {
        try {
            return ResponseEntity.ok(Map.of("usage", licensingService.getUsageStats()));
        } catch (Exception e) {
            logger.error("Failed to get usage", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get usage: " + e.getMessage()));
        }
    }

    @GetMapping("/pricing")
    public ResponseEntity<?> getPricing() {
        try {
            return ResponseEntity.ok(Map.of("pricingTiers", licensingService.getPricingTiers()));
        } catch (Exception e) {
            logger.error("Failed to get pricing", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get pricing: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // AGREEMENTS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/agreements")
    public ResponseEntity<?> getAgreements() {
        try {
            return ResponseEntity.ok(Map.of("agreements", licensingService.getAgreements()));
        } catch (Exception e) {
            logger.error("Failed to get agreements", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get agreements: " + e.getMessage()));
        }
    }

    @PostMapping("/agreements")
    public ResponseEntity<?> createAgreement(@RequestBody Map<String, String> payload) {
        try {
            String agreementType = payload.getOrDefault("agreementType", "EULA");
            String orgName = payload.getOrDefault("organizationName", "");
            String signedBy = payload.getOrDefault("signedBy", "admin");
            String document = payload.getOrDefault("document", "");

            String id = licensingService.createAgreement(agreementType, orgName, signedBy, document);
            return ResponseEntity.ok(Map.of(
                    "status", "created",
                    "agreementId", id,
                    "message", "Agreement created"));
        } catch (Exception e) {
            logger.error("Failed to create agreement", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create agreement: " + e.getMessage()));
        }
    }

    @GetMapping("/agreements/{id}")
    public ResponseEntity<?> getAgreement(@PathVariable String id) {
        Map<String, Object> agreement = licensingService.getAgreement(id);
        if (agreement == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("agreement", agreement));
    }

    // ═══════════════════════════════════════════════════════════
    // HISTORY & STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        try {
            return ResponseEntity.ok(Map.of("history", licensingService.getHistory()));
        } catch (Exception e) {
            logger.error("Failed to get history", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get history: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            License license = licensingService.getCurrentLicense();
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("licenseType", license.getType().name());
            stats.put("status", license.getStatus().name());
            stats.put("isActive", license.isActive());
            stats.put("isDemo", license.isDemo());
            stats.put("remainingDays", license.getRemainingDays());
            stats.put("maxUsers", license.getMaxUsers());
            stats.put("currentUsers", license.getCurrentUsers());
            stats.put("usage", licensingService.getUsageStats());
            stats.put("agreementCount", licensingService.getAgreements().size());
            return ResponseEntity.ok(Map.of("statistics", stats));
        } catch (Exception e) {
            logger.error("Failed to get license statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> licenseToMap(License l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("licenseKey", l.getLicenseKey());
        m.put("type", l.getType().name());
        m.put("status", l.getStatus().name());
        m.put("organizationName", l.getOrganizationName());
        m.put("contactEmail", l.getContactEmail());
        m.put("activatedAt", l.getActivatedAt() != null ? l.getActivatedAt().toString() : null);
        m.put("expiresAt", l.getExpiresAt() != null ? l.getExpiresAt().toString() : null);
        m.put("maxUsers", l.getMaxUsers());
        m.put("currentUsers", l.getCurrentUsers());
        m.put("features", l.getFeatures());
        m.put("isActive", l.isActive());
        m.put("isDemo", l.isDemo());
        m.put("isExpired", l.isExpired());
        m.put("remainingDays", l.getRemainingDays());
        if (l.getHardwareFingerprint() != null) m.put("hardwareFingerprint", l.getHardwareFingerprint());
        if (l.getSignature() != null) m.put("signaturePresent", true);
        return m;
    }
}
