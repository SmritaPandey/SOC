package com.qsdpdp.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket Message Handler — STOMP endpoints
 * 
 * Handles incoming WebSocket messages from clients and routes
 * them through the sync layer. Works alongside SyncController (REST).
 * 
 * Client → /app/sync.register → register for offline queue
 * Client → /app/sync.consent → publish consent event via WS
 * Client → /app/sync.breach → publish breach event via WS
 * 
 * @version 1.0.0
 * @since Phase 1 — Core Synchronization
 */
@Controller
public class SyncWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SyncWebSocketHandler.class);

    @Autowired(required = false)
    private SyncService syncService;

    /**
     * Client registration for sync — receives client metadata
     * and returns confirmation with current sync state.
     */
    @MessageMapping("/sync.register")
    @SendTo("/topic/sync")
    public Map<String, Object> handleRegistration(Map<String, Object> message) {
        String clientId = (String) message.getOrDefault("clientId", "unknown");
        String clientType = (String) message.getOrDefault("clientType", "WEB");

        if (syncService != null) {
            syncService.registerClient(clientId, clientType);
        }

        logger.info("WS client registered: {} ({})", clientId, clientType);

        return Map.of(
                "type", "SYNC_REGISTERED",
                "clientId", clientId,
                "clientType", clientType,
                "status", "CONNECTED",
                "syncStatus", syncService != null ? syncService.getSyncStatus() : Map.of("status", "UNAVAILABLE")
        );
    }

    /**
     * Consent event from client (e.g., consent granted/revoked via mobile)
     */
    @MessageMapping("/sync.consent")
    @SendTo("/topic/consent")
    public Map<String, Object> handleConsentSync(Map<String, Object> message) {
        logger.debug("WS consent event from client: {}", message.get("action"));
        return Map.of(
                "type", "CONSENT_SYNC",
                "action", message.getOrDefault("action", "UNKNOWN"),
                "consentId", message.getOrDefault("consentId", ""),
                "source", "WEBSOCKET",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    /**
     * Breach event from client (e.g., breach report from mobile)
     */
    @MessageMapping("/sync.breach")
    @SendTo("/topic/breach")
    public Map<String, Object> handleBreachSync(Map<String, Object> message) {
        logger.debug("WS breach event from client: {}", message.get("action"));
        return Map.of(
                "type", "BREACH_SYNC",
                "action", message.getOrDefault("action", "UNKNOWN"),
                "breachId", message.getOrDefault("breachId", ""),
                "source", "WEBSOCKET",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }

    /**
     * Data usage event from client
     */
    @MessageMapping("/sync.data-usage")
    @SendTo("/topic/data-usage")
    public Map<String, Object> handleDataUsageSync(Map<String, Object> message) {
        logger.debug("WS data-usage event from client");
        return Map.of(
                "type", "DATA_USAGE_SYNC",
                "action", message.getOrDefault("action", "UNKNOWN"),
                "source", "WEBSOCKET",
                "timestamp", java.time.LocalDateTime.now().toString()
        );
    }
}
