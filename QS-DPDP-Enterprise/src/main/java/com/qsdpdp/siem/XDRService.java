package com.qsdpdp.siem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Extended Detection and Response (XDR) Service
 * Correlates telemetry across endpoint, network, cloud, email, and identity domains.
 * Implements cross-domain threat detection with automated incident generation.
 *
 * @version 1.0.0
 * @since Module 7 — Cybersecurity
 */
@Service
public class XDRService {

    private static final Logger logger = LoggerFactory.getLogger(XDRService.class);

    private boolean initialized = false;

    // Telemetry sources
    private final Map<String, TelemetrySource> telemetrySources = new ConcurrentHashMap<>();
    // Correlation policies
    private final List<CorrelationPolicy> correlationPolicies = new CopyOnWriteArrayList<>();
    // Incidents
    private final List<XDRIncident> incidents = new CopyOnWriteArrayList<>();
    // Telemetry events buffer
    private final List<TelemetryEvent> eventBuffer = new CopyOnWriteArrayList<>();

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing XDR Service...");
        registerDefaultSources();
        loadCorrelationPolicies();
        initialized = true;
        logger.info("XDR Service initialized — {} sources, {} correlation policies",
                telemetrySources.size(), correlationPolicies.size());
    }

    // ═══════════════════════════════════════════════════════════
    // TELEMETRY SOURCES
    // ═══════════════════════════════════════════════════════════

    private void registerDefaultSources() {
        registerSource("SIEM", "Security Information & Event Management", "network");
        registerSource("DLP", "Data Loss Prevention", "data");
        registerSource("EDR", "Endpoint Detection & Response", "endpoint");
        registerSource("FIREWALL", "Network Firewall", "network");
        registerSource("WAF", "Web Application Firewall", "application");
        registerSource("EMAIL_GW", "Email Gateway", "email");
        registerSource("IAM", "Identity & Access Management", "identity");
        registerSource("CLOUD_TRAIL", "Cloud Activity Logs", "cloud");
        registerSource("DNS", "DNS Security", "network");
        registerSource("PROXY", "Web Proxy", "network");
        registerSource("THREAT_INTEL", "Threat Intelligence Feed", "intelligence");
    }

    public void registerSource(String id, String name, String domain) {
        telemetrySources.put(id, new TelemetrySource(id, name, domain));
    }

    // ═══════════════════════════════════════════════════════════
    // CORRELATION POLICIES
    // ═══════════════════════════════════════════════════════════

    private void loadCorrelationPolicies() {
        // Policy 1: APT Detection (multi-stage attack)
        correlationPolicies.add(new CorrelationPolicy(
                "XDR-POL-001", "Advanced Persistent Threat",
                List.of("reconnaissance", "initial_access", "lateral_movement", "data_exfiltration"),
                "CRITICAL", 24 * 60, // 24-hour window
                "Isolate affected endpoints, block C2 communications, initiate IR playbook"));

        // Policy 2: Insider Threat
        correlationPolicies.add(new CorrelationPolicy(
                "XDR-POL-002", "Insider Threat Detection",
                List.of("privilege_escalation", "unusual_access", "data_staging", "data_exfiltration"),
                "HIGH", 7 * 24 * 60, // 7-day window
                "Restrict user access, preserve evidence, notify security team"));

        // Policy 3: Ransomware
        correlationPolicies.add(new CorrelationPolicy(
                "XDR-POL-003", "Ransomware Attack Chain",
                List.of("phishing_email", "malware_execution", "file_encryption", "ransom_note"),
                "CRITICAL", 4 * 60, // 4-hour window
                "Isolate network segment, restore from backup, report to CERT-In within 6 hours"));

        // Policy 4: Phishing Campaign
        correlationPolicies.add(new CorrelationPolicy(
                "XDR-POL-004", "Coordinated Phishing Campaign",
                List.of("suspicious_email", "credential_harvest", "unauthorized_login"),
                "HIGH", 12 * 60,
                "Block sender domains, reset affected credentials, alert all users"));

        // Policy 5: Cloud Compromise
        correlationPolicies.add(new CorrelationPolicy(
                "XDR-POL-005", "Cloud Infrastructure Compromise",
                List.of("unusual_cloud_api", "permission_change", "resource_creation", "data_download"),
                "HIGH", 6 * 60,
                "Revoke compromised tokens, audit cloud permissions, enable enhanced logging"));

        // Policy 6: Supply Chain Attack
        correlationPolicies.add(new CorrelationPolicy(
                "XDR-POL-006", "Supply Chain Compromise",
                List.of("vendor_access_anomaly", "software_tampering", "unusual_update", "c2_communication"),
                "CRITICAL", 48 * 60,
                "Quarantine vendor software, validate checksums, audit all vendor access"));
    }

    // ═══════════════════════════════════════════════════════════
    // TELEMETRY INGESTION & CORRELATION
    // ═══════════════════════════════════════════════════════════

    public void ingestTelemetry(String sourceId, String eventType, String description,
                                 Map<String, Object> metadata) {
        TelemetryEvent event = new TelemetryEvent(
                UUID.randomUUID().toString(), sourceId, eventType,
                description, metadata, LocalDateTime.now());

        eventBuffer.add(event);

        // Update source stats
        TelemetrySource source = telemetrySources.get(sourceId);
        if (source != null) {
            source.incrementEventCount();
            source.setLastEventAt(LocalDateTime.now());
        }

        // Run correlation
        evaluateCorrelations(event);

        logger.debug("XDR telemetry ingested: {} from {} — {}", eventType, sourceId, description);
    }

    private void evaluateCorrelations(TelemetryEvent newEvent) {
        for (CorrelationPolicy policy : correlationPolicies) {
            List<String> matchedPhases = new ArrayList<>();
            List<TelemetryEvent> matchedEvents = new ArrayList<>();

            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(policy.windowMinutes);

            for (String phase : policy.phases) {
                for (TelemetryEvent event : eventBuffer) {
                    if (event.timestamp.isAfter(windowStart) &&
                        event.eventType.toLowerCase().contains(phase.toLowerCase())) {
                        if (!matchedPhases.contains(phase)) {
                            matchedPhases.add(phase);
                            matchedEvents.add(event);
                        }
                    }
                }
            }

            // Trigger if >50% of phases matched
            if (matchedPhases.size() > policy.phases.size() / 2) {
                createIncident(policy, matchedPhases, matchedEvents);
            }
        }
    }

    private void createIncident(CorrelationPolicy policy, List<String> phases,
                                 List<TelemetryEvent> events) {
        // Check for existing incident to avoid duplicates
        for (XDRIncident existing : incidents) {
            if (existing.policyId.equals(policy.id) &&
                existing.createdAt.isAfter(LocalDateTime.now().minusHours(1))) {
                return; // Recent duplicate
            }
        }

        XDRIncident incident = new XDRIncident();
        incident.id = "XDR-INC-" + System.currentTimeMillis();
        incident.policyId = policy.id;
        incident.policyName = policy.name;
        incident.severity = policy.severity;
        incident.matchedPhases = new ArrayList<>(phases);
        incident.relatedEvents = events.size();
        incident.recommendedActions = policy.recommendedActions;
        incident.status = "OPEN";
        incident.createdAt = LocalDateTime.now();

        // Build attack chain description
        StringBuilder chain = new StringBuilder();
        for (int i = 0; i < phases.size(); i++) {
            chain.append(phases.get(i));
            if (i < phases.size() - 1) chain.append(" → ");
        }
        incident.attackChain = chain.toString();

        incidents.add(incident);
        logger.warn("XDR INCIDENT CREATED: {} — {} [{}] Attack chain: {}",
                incident.id, policy.name, policy.severity, incident.attackChain);
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════

    public List<XDRIncident> getIncidents() {
        return Collections.unmodifiableList(incidents);
    }

    public List<XDRIncident> getOpenIncidents() {
        return incidents.stream()
                .filter(i -> "OPEN".equals(i.status))
                .toList();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSources", telemetrySources.size());
        stats.put("totalEvents", eventBuffer.size());
        stats.put("totalIncidents", incidents.size());
        stats.put("openIncidents", getOpenIncidents().size());
        stats.put("correlationPolicies", correlationPolicies.size());

        // Source breakdown
        Map<String, Integer> sourceStats = new LinkedHashMap<>();
        for (TelemetrySource src : telemetrySources.values()) {
            sourceStats.put(src.name, src.eventCount);
        }
        stats.put("sourceBreakdown", sourceStats);
        return stats;
    }

    public List<CorrelationPolicy> getCorrelationPolicies() {
        return Collections.unmodifiableList(correlationPolicies);
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class TelemetrySource {
        String id, name, domain;
        int eventCount = 0;
        LocalDateTime lastEventAt;

        TelemetrySource(String id, String name, String domain) {
            this.id = id;
            this.name = name;
            this.domain = domain;
        }

        void incrementEventCount() { eventCount++; }
        void setLastEventAt(LocalDateTime t) { lastEventAt = t; }
    }

    public static class TelemetryEvent {
        String id, sourceId, eventType, description;
        Map<String, Object> metadata;
        LocalDateTime timestamp;

        TelemetryEvent(String id, String sourceId, String eventType,
                       String description, Map<String, Object> metadata, LocalDateTime timestamp) {
            this.id = id;
            this.sourceId = sourceId;
            this.eventType = eventType;
            this.description = description;
            this.metadata = metadata;
            this.timestamp = timestamp;
        }
    }

    public static class CorrelationPolicy {
        String id, name, severity, recommendedActions;
        List<String> phases;
        int windowMinutes;

        CorrelationPolicy(String id, String name, List<String> phases,
                          String severity, int windowMinutes, String recommendedActions) {
            this.id = id;
            this.name = name;
            this.phases = phases;
            this.severity = severity;
            this.windowMinutes = windowMinutes;
            this.recommendedActions = recommendedActions;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getSeverity() { return severity; }
        public List<String> getPhases() { return phases; }
    }

    public static class XDRIncident {
        String id, policyId, policyName, severity, attackChain;
        String status, recommendedActions;
        List<String> matchedPhases;
        int relatedEvents;
        LocalDateTime createdAt;

        public String getId() { return id; }
        public String getPolicyName() { return policyName; }
        public String getSeverity() { return severity; }
        public String getAttackChain() { return attackChain; }
        public String getStatus() { return status; }
        public String getRecommendedActions() { return recommendedActions; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }
}
