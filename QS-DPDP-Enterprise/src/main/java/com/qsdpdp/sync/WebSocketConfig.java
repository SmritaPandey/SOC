package com.qsdpdp.sync;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration — Real-time Sync Layer
 * 
 * Provides STOMP over WebSocket with SockJS fallback for:
 * - Consent state changes (/topic/consent)
 * - Data usage events (/topic/data-usage)
 * - Breach notifications (/topic/breach)
 * - System-wide sync events (/topic/sync)
 * 
 * Compatible with: Web (SockJS), Mobile (native WS), Desktop (JavaFX WS)
 * 
 * @version 1.0.0
 * @since Phase 1 — Core Synchronization
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for topic-based pub/sub
        // Topics: /topic/consent, /topic/breach, /topic/data-usage, /topic/sync, /topic/notifications
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for messages FROM clients TO server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Primary WebSocket endpoint with SockJS fallback
        // Accessible at: ws://localhost:8080/ws-sync
        registry.addEndpoint("/ws-sync")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // Raw WebSocket endpoint for mobile/native clients (no SockJS)
        registry.addEndpoint("/ws-sync-native")
                .setAllowedOriginPatterns("*");
    }
}
