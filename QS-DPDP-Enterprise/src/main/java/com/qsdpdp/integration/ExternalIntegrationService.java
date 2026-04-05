package com.qsdpdp.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal External Integration Service
 * Factory-pattern connector management for SIEM/DLP/ITSM/Cloud vendor integrations
 */
public class ExternalIntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(ExternalIntegrationService.class);
    private boolean initialized = false;
    private final Map<String, ConnectorRegistration> connectors = new ConcurrentHashMap<>();

    public void initialize() {
        if (initialized) return;
        registerDefaultConnectors();
        initialized = true;
        logger.info("ExternalIntegrationService initialized with {} connectors", connectors.size());
    }

    private void registerDefaultConnectors() {
        // SIEM connectors
        register(new ConnectorRegistration("splunk", "Splunk Enterprise", ConnectorType.SIEM, "Send events to Splunk HEC", false));
        register(new ConnectorRegistration("qradar", "IBM QRadar", ConnectorType.SIEM, "Forward via QRadar SIEM API", false));
        register(new ConnectorRegistration("sentinel", "Microsoft Sentinel", ConnectorType.SIEM, "Azure Sentinel ingestion", false));
        register(new ConnectorRegistration("elastic", "Elastic SIEM", ConnectorType.SIEM, "Elasticsearch/Kibana integration", false));

        // DLP connectors
        register(new ConnectorRegistration("symantec-dlp", "Symantec DLP", ConnectorType.DLP, "Broadcom Symantec DLP integration", false));
        register(new ConnectorRegistration("mcafee-dlp", "McAfee/Trellix DLP", ConnectorType.DLP, "Trellix DLP connector", false));
        register(new ConnectorRegistration("forcepoint", "Forcepoint DLP", ConnectorType.DLP, "Forcepoint DLP cloud", false));

        // ITSM connectors
        register(new ConnectorRegistration("servicenow", "ServiceNow", ConnectorType.ITSM, "ServiceNow incident/CMDB", false));
        register(new ConnectorRegistration("jira", "Jira Service Management", ConnectorType.ITSM, "Atlassian Jira integration", false));

        // Cloud connectors
        register(new ConnectorRegistration("aws-macie", "AWS Macie", ConnectorType.CLOUD, "Amazon Macie data discovery", false));
        register(new ConnectorRegistration("gcp-dlp", "Google Cloud DLP", ConnectorType.CLOUD, "GCP DLP API", false));
        register(new ConnectorRegistration("azure-purview", "Azure Purview", ConnectorType.CLOUD, "Microsoft Purview", false));

        // Communication connectors
        register(new ConnectorRegistration("slack", "Slack", ConnectorType.NOTIFICATION, "Slack webhook notifications", false));
        register(new ConnectorRegistration("teams", "Microsoft Teams", ConnectorType.NOTIFICATION, "Teams webhook integration", false));
        register(new ConnectorRegistration("email-smtp", "Email SMTP", ConnectorType.NOTIFICATION, "SMTP email alerts", false));
        register(new ConnectorRegistration("pagerduty", "PagerDuty", ConnectorType.NOTIFICATION, "PagerDuty incident alerts", false));
    }

    /** Register a connector */
    public void register(ConnectorRegistration reg) { connectors.put(reg.id, reg); }

    /** Enable a connector with configuration */
    public IntegrationResult enableConnector(String connectorId, Map<String, String> config) {
        ConnectorRegistration reg = connectors.get(connectorId);
        if (reg == null) return new IntegrationResult(false, "Connector not found: " + connectorId);
        reg.enabled = true;
        reg.config = config;
        reg.lastConfigured = LocalDateTime.now();
        logger.info("Connector enabled: {} ({})", reg.name, connectorId);
        return new IntegrationResult(true, "Connector enabled: " + reg.name);
    }

    /** Disable a connector */
    public IntegrationResult disableConnector(String connectorId) {
        ConnectorRegistration reg = connectors.get(connectorId);
        if (reg == null) return new IntegrationResult(false, "Not found");
        reg.enabled = false;
        return new IntegrationResult(true, "Connector disabled: " + reg.name);
    }

    /** Send data to an enabled connector (stub – production would call real APIs) */
    public IntegrationResult sendEvent(String connectorId, Map<String, Object> eventData) {
        ConnectorRegistration reg = connectors.get(connectorId);
        if (reg == null || !reg.enabled) return new IntegrationResult(false, "Connector not available");
        reg.eventsSent++;
        reg.lastEventTime = LocalDateTime.now();
        logger.debug("Event sent to {}: {} fields", reg.name, eventData.size());
        return new IntegrationResult(true, "Event sent to " + reg.name);
    }

    /** Broadcast event to all enabled connectors of a type */
    public List<IntegrationResult> broadcastEvent(ConnectorType type, Map<String, Object> eventData) {
        List<IntegrationResult> results = new ArrayList<>();
        for (ConnectorRegistration reg : connectors.values()) {
            if (reg.enabled && reg.type == type) {
                results.add(sendEvent(reg.id, eventData));
            }
        }
        return results;
    }

    /** Test connectivity to a connector */
    public IntegrationResult testConnector(String connectorId) {
        ConnectorRegistration reg = connectors.get(connectorId);
        if (reg == null) return new IntegrationResult(false, "Not found");
        // Stub test – production would actually ping the endpoint
        reg.lastTestedAt = LocalDateTime.now();
        reg.connectionStatus = "OK";
        return new IntegrationResult(true, "Connection test successful for " + reg.name);
    }

    /** Get all connectors */
    public List<ConnectorRegistration> getAllConnectors() { return new ArrayList<>(connectors.values()); }

    /** Get connectors by type */
    public List<ConnectorRegistration> getConnectorsByType(ConnectorType type) {
        return connectors.values().stream().filter(c -> c.type == type).toList();
    }

    /** Get enabled connectors */
    public List<ConnectorRegistration> getEnabledConnectors() {
        return connectors.values().stream().filter(c -> c.enabled).toList();
    }

    public boolean isInitialized() { return initialized; }

    // Types
    public enum ConnectorType { SIEM, DLP, ITSM, CLOUD, NOTIFICATION, CUSTOM }

    public static class ConnectorRegistration {
        public String id, name, description, connectionStatus;
        public ConnectorType type;
        public boolean enabled;
        public Map<String, String> config = new HashMap<>();
        public int eventsSent;
        public LocalDateTime lastConfigured, lastEventTime, lastTestedAt;

        public ConnectorRegistration(String id, String name, ConnectorType type, String description, boolean enabled) {
            this.id = id; this.name = name; this.type = type; this.description = description; this.enabled = enabled;
        }
    }

    public static class IntegrationResult {
        public boolean success; public String message;
        public IntegrationResult(boolean s, String m) { this.success = s; this.message = m; }
    }
}
