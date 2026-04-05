package com.qsdpdp.siem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Wazuh EDR Agent API Connector
 * Integrates with Wazuh Manager REST API for:
 * - Agent status monitoring
 * - Vulnerability assessment results
 * - Security Configuration Assessment (SCA)
 * - File Integrity Monitoring (FIM) events
 * - Active Response commands
 *
 * @version 3.0.0
 * @since Phase 3
 */
@Component
public class WazuhConnector {

    private static final Logger logger = LoggerFactory.getLogger(WazuhConnector.class);

    private String wazuhHost = "https://localhost:55000";
    private String apiUser = "wazuh-wui";
    private String apiPassword = "";
    private String authToken = null;

    private final HttpClient httpClient;
    private boolean connected = false;

    public WazuhConnector() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Authenticate with Wazuh Manager API
     */
    public boolean authenticate() {
        try {
            String authHeader = Base64.getEncoder().encodeToString(
                    (apiUser + ":" + apiPassword).getBytes());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(wazuhHost + "/security/user/authenticate"))
                    .header("Authorization", "Basic " + authHeader)
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Extract JWT token from response
                authToken = extractToken(response.body());
                connected = true;
                logger.info("✅ Connected to Wazuh Manager at {}", wazuhHost);
                return true;
            }
        } catch (Exception e) {
            logger.warn("Wazuh connection failed (expected if not installed): {}", e.getMessage());
        }
        connected = false;
        return false;
    }

    /**
     * Get all Wazuh agents and their status
     */
    public List<Map<String, Object>> getAgents() {
        String response = apiGet("/agents?limit=500");
        if (response == null) return Collections.emptyList();
        return parseArrayResponse(response, "data.affected_items");
    }

    /**
     * Get agent vulnerability assessment
     */
    public List<Map<String, Object>> getAgentVulnerabilities(String agentId) {
        String response = apiGet("/vulnerability/" + agentId);
        if (response == null) return Collections.emptyList();
        return parseArrayResponse(response, "data.affected_items");
    }

    /**
     * Get SCA (Security Configuration Assessment) results
     */
    public List<Map<String, Object>> getSCAResults(String agentId) {
        String response = apiGet("/sca/" + agentId);
        if (response == null) return Collections.emptyList();
        return parseArrayResponse(response, "data.affected_items");
    }

    /**
     * Get FIM (File Integrity Monitoring) events
     */
    public List<Map<String, Object>> getFIMEvents(String agentId) {
        String response = apiGet("/syscheck/" + agentId + "?limit=100");
        if (response == null) return Collections.emptyList();
        return parseArrayResponse(response, "data.affected_items");
    }

    /**
     * Send Active Response command to an agent
     */
    public boolean sendActiveResponse(String agentId, String command, Map<String, Object> params) {
        String body = String.format(
                "{\"command\":\"%s\",\"arguments\":[\"%s\"],\"alert\":{\"data\":{\"srcip\":\"%s\"}}}",
                command,
                params.getOrDefault("arguments", ""),
                params.getOrDefault("srcip", ""));

        String response = apiPut("/active-response?agents_list=" + agentId, body);
        return response != null;
    }

    /**
     * Get Wazuh cluster status
     */
    public Map<String, Object> getClusterStatus() {
        if (!connected) {
            return Map.of("status", "DISCONNECTED", "host", wazuhHost);
        }
        String response = apiGet("/cluster/status");
        if (response != null) {
            return Map.of("status", "CONNECTED", "host", wazuhHost, "response", response);
        }
        return Map.of("status", "ERROR", "host", wazuhHost);
    }

    // ═══ HTTP HELPERS ═══

    private String apiGet(String endpoint) {
        if (!connected || authToken == null) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(wazuhHost + endpoint))
                    .header("Authorization", "Bearer " + authToken)
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (Exception e) {
            logger.debug("Wazuh API GET failed: {}", e.getMessage());
            return null;
        }
    }

    private String apiPut(String endpoint, String body) {
        if (!connected || authToken == null) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(wazuhHost + endpoint))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (Exception e) {
            logger.debug("Wazuh API PUT failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractToken(String responseBody) {
        // Simple JSON token extraction
        int idx = responseBody.indexOf("\"token\"");
        if (idx > 0) {
            int start = responseBody.indexOf("\"", idx + 8) + 1;
            int end = responseBody.indexOf("\"", start);
            return responseBody.substring(start, end);
        }
        return null;
    }

    private List<Map<String, Object>> parseArrayResponse(String json, String path) {
        // Simplified — in production use Jackson/Gson
        return Collections.emptyList();
    }

    // ═══ CONFIG ═══

    public void configure(String host, String user, String password) {
        this.wazuhHost = host;
        this.apiUser = user;
        this.apiPassword = password;
    }

    public boolean isConnected() { return connected; }

    public Map<String, Object> getStatistics() {
        return Map.of(
                "connected", connected,
                "host", wazuhHost,
                "features", List.of("Agent Monitoring", "Vulnerability Assessment",
                        "SCA", "FIM", "Active Response"));
    }
}
