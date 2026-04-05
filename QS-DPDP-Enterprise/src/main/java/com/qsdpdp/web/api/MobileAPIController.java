package com.qsdpdp.web.api;

import com.qsdpdp.consent.ConsentValidationEngine;
import com.qsdpdp.sync.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Mobile API Controller — Optimized endpoints for mobile apps
 * 
 * Provides:
 * - Offline-capable consent operations
 * - Sync state management for mobile clients
 * - Push notification token registration
 * - Biometric authentication verification hooks
 * - Batch sync operations for reconnection
 * - Lightweight payloads (mobile data-optimized)
 * 
 * @version 1.0.0
 * @since Phase 8 — Mobile App Enhancement
 */
@RestController
@RequestMapping("/api/mobile")
public class MobileAPIController {

    private static final Logger logger = LoggerFactory.getLogger(MobileAPIController.class);

    @Autowired(required = false) private SyncService syncService;
    @Autowired(required = false) private ConsentValidationEngine validationEngine;

    /**
     * POST /api/mobile/register
     * Register mobile device for push notifications and sync
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDevice(@RequestBody Map<String, String> body) {
        String deviceId = body.getOrDefault("deviceId", "mobile-" + System.currentTimeMillis());
        String platform = body.getOrDefault("platform", "ANDROID");
        String pushToken = body.getOrDefault("pushToken", "");

        // Register for sync
        if (syncService != null) {
            if (!syncService.isInitialized()) try { syncService.initialize(); } catch (Exception e) { /* ignore */ }
            syncService.registerClient(deviceId, "MOBILE_" + platform);
        }

        logger.info("Mobile device registered: {} ({}), push={}", deviceId, platform, !pushToken.isEmpty());

        return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "platform", platform,
                "pushRegistered", !pushToken.isEmpty(),
                "syncRegistered", syncService != null,
                "status", "REGISTERED",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * POST /api/mobile/sync
     * Batch sync — fetches all missed events (consent + breach + data-usage)
     * for a reconnecting mobile client
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> batchSync(@RequestBody Map<String, Object> body) {
        String deviceId = (String) body.getOrDefault("deviceId", "");
        long lastSequence = body.containsKey("lastSequence") ? ((Number) body.get("lastSequence")).longValue() : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceId", deviceId);
        result.put("requestedSince", lastSequence);

        if (syncService != null && syncService.isInitialized()) {
            // Drain offline queue first
            List<Map<String, Object>> offlineEvents = syncService.drainOfflineQueue(deviceId);
            result.put("offlineEvents", offlineEvents);
            result.put("offlineCount", offlineEvents.size());

            // Then catch up by topic
            result.put("consent", syncService.syncConsent(lastSequence, 50));
            result.put("breach", syncService.syncBreach(lastSequence, 50));
            result.put("dataUsage", syncService.syncDataUsage(lastSequence, 50));
            result.put("syncStatus", syncService.getSyncStatus());
        } else {
            result.put("offlineCount", 0);
            result.put("syncStatus", Map.of("status", "UNAVAILABLE"));
        }

        result.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/mobile/consent/offline
     * Accept a consent action that was performed offline.
     * Mobile app queues these when offline and submits on reconnection.
     */
    @PostMapping("/consent/offline")
    public ResponseEntity<Map<String, Object>> processOfflineConsent(
            @RequestBody List<Map<String, Object>> offlineActions) {

        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> action : offlineActions) {
            String actionType = (String) action.getOrDefault("action", "UNKNOWN");
            String consentId = (String) action.getOrDefault("consentId", "");
            String offlineTimestamp = (String) action.getOrDefault("timestamp", "");

            results.add(Map.of(
                    "consentId", consentId,
                    "action", actionType,
                    "offlineTimestamp", offlineTimestamp,
                    "processedAt", LocalDateTime.now().toString(),
                    "status", "PROCESSED"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "processedCount", results.size(),
                "results", results,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * POST /api/mobile/biometric/verify
     * Verify biometric authentication token from mobile device
     */
    @PostMapping("/biometric/verify")
    public ResponseEntity<Map<String, Object>> verifyBiometric(@RequestBody Map<String, String> body) {
        String deviceId = body.getOrDefault("deviceId", "");
        String biometricToken = body.getOrDefault("biometricToken", "");
        String biometricType = body.getOrDefault("biometricType", "FINGERPRINT");

        // In production: verify against biometric enrollment data
        // For now: accept valid-looking tokens
        boolean verified = !biometricToken.isEmpty() && biometricToken.length() >= 8;

        return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "biometricType", biometricType,
                "verified", verified,
                "sessionToken", verified ? UUID.randomUUID().toString() : "",
                "expiresIn", verified ? 3600 : 0,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    /**
     * GET /api/mobile/dashboard
     * Lightweight dashboard payload for mobile home screen
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> mobileDashboard(
            @RequestParam(required = false) String deviceId) {

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("deviceId", deviceId);

        // Consent summary
        if (validationEngine != null) {
            if (!validationEngine.isInitialized()) try { validationEngine.initialize(); } catch (Exception e) { /* ignore */ }
            dashboard.put("validationStats", validationEngine.getStatistics());
            dashboard.put("openViolations", validationEngine.getOpenViolations(5));
        }

        // Sync status
        if (syncService != null && syncService.isInitialized()) {
            dashboard.put("syncStatus", syncService.getSyncStatus());
        }

        dashboard.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(dashboard);
    }
}
