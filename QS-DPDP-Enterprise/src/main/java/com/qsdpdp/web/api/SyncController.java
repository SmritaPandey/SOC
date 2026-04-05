package com.qsdpdp.web.api;

import com.qsdpdp.sync.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Sync Controller — REST API for synchronization
 * 
 * Provides REST endpoints for sync operations:
 * - GET /api/sync/consent — Consent sync (sequence-based pagination)
 * - GET /api/sync/data-usage — Data usage sync
 * - GET /api/sync/breach — Breach sync
 * - GET /api/sync/status — Overall sync status
 * - POST /api/sync/register — Register client for offline queue
 * - GET /api/sync/offline/{clientId} — Drain offline queue
 * 
 * @version 1.0.0
 * @since Phase 1 — Core Synchronization
 */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private static final Logger logger = LoggerFactory.getLogger(SyncController.class);

    @Autowired(required = false)
    private SyncService syncService;

    /**
     * GET /api/sync/consent?since=0&limit=100
     * Sync consent events since a given sequence number
     */
    @GetMapping("/consent")
    public ResponseEntity<Map<String, Object>> syncConsent(
            @RequestParam(defaultValue = "0") long since,
            @RequestParam(defaultValue = "100") int limit) {
        ensureInitialized();
        return ResponseEntity.ok(syncService.syncConsent(since, limit));
    }

    /**
     * GET /api/sync/data-usage?since=0&limit=100
     * Sync data usage events since a given sequence number
     */
    @GetMapping("/data-usage")
    public ResponseEntity<Map<String, Object>> syncDataUsage(
            @RequestParam(defaultValue = "0") long since,
            @RequestParam(defaultValue = "100") int limit) {
        ensureInitialized();
        return ResponseEntity.ok(syncService.syncDataUsage(since, limit));
    }

    /**
     * GET /api/sync/breach?since=0&limit=100
     * Sync breach events since a given sequence number
     */
    @GetMapping("/breach")
    public ResponseEntity<Map<String, Object>> syncBreach(
            @RequestParam(defaultValue = "0") long since,
            @RequestParam(defaultValue = "100") int limit) {
        ensureInitialized();
        return ResponseEntity.ok(syncService.syncBreach(since, limit));
    }

    /**
     * GET /api/sync/status
     * Get overall sync status including WebSocket availability, queue sizes, sequence
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> syncStatus() {
        ensureInitialized();
        return ResponseEntity.ok(syncService.getSyncStatus());
    }

    /**
     * POST /api/sync/register
     * Register a client for offline queue management
     * Body: { "clientId": "mobile-abc123", "clientType": "MOBILE" }
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerClient(@RequestBody Map<String, String> body) {
        ensureInitialized();
        String clientId = body.getOrDefault("clientId", "client-" + System.currentTimeMillis());
        String clientType = body.getOrDefault("clientType", "WEB");
        syncService.registerClient(clientId, clientType);
        return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "clientType", clientType,
                "status", "REGISTERED",
                "syncStatus", syncService.getSyncStatus()
        ));
    }

    /**
     * GET /api/sync/offline/{clientId}
     * Drain offline queue for a reconnecting client
     */
    @GetMapping("/offline/{clientId}")
    public ResponseEntity<Map<String, Object>> drainOfflineQueue(@PathVariable String clientId) {
        ensureInitialized();
        List<Map<String, Object>> events = syncService.drainOfflineQueue(clientId);
        return ResponseEntity.ok(Map.of(
                "clientId", clientId,
                "eventCount", events.size(),
                "events", events
        ));
    }

    private void ensureInitialized() {
        if (syncService != null && !syncService.isInitialized()) {
            try { syncService.initialize(); } catch (Exception e) {
                logger.debug("SyncService init skipped: {}", e.getMessage());
            }
        }
    }
}
