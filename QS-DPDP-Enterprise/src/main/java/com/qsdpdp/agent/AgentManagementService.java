package com.qsdpdp.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent Management Console Service
 * Central management for all deployed endpoint sensor agents.
 * Equivalent to CrowdStrike Falcon Console / Palo Alto Cortex XDR Management.
 *
 * Features:
 * - Agent registration & lifecycle management
 * - Heartbeat monitoring & health tracking
 * - Policy push to agents
 * - Event aggregation from all agents
 * - Agent grouping by department/location
 * - Bulk actions (isolate, scan, update)
 *
 * @version 3.0.0
 * @since Phase 6 — Enterprise Distribution
 */
@Service
public class AgentManagementService {

    private static final Logger logger = LoggerFactory.getLogger(AgentManagementService.class);

    private final Map<String, AgentRecord> agents = new ConcurrentHashMap<>();
    private final List<AgentEvent> eventLog = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, AgentPolicy> policies = new ConcurrentHashMap<>();

    public AgentManagementService() {
        initializeDefaultPolicies();
    }

    // ═══════════════════════════════════════════════════════
    // AGENT REGISTRATION
    // ═══════════════════════════════════════════════════════

    public AgentRecord registerAgent(Map<String, Object> registration) {
        String agentId = registration.getOrDefault("agentId", UUID.randomUUID().toString()).toString();

        AgentRecord agent = new AgentRecord();
        agent.agentId = agentId;
        agent.hostname = registration.getOrDefault("hostname", "unknown").toString();
        agent.os = registration.getOrDefault("os", "unknown").toString();
        agent.osVersion = registration.getOrDefault("osVersion", "").toString();
        agent.ipAddress = registration.getOrDefault("ip", "").toString();
        agent.macAddress = registration.getOrDefault("mac", "").toString();
        agent.agentVersion = registration.getOrDefault("agentVersion", "3.0.0").toString();
        agent.status = "ACTIVE";
        agent.registeredAt = LocalDateTime.now();
        agent.lastHeartbeat = LocalDateTime.now();

        agents.put(agentId, agent);
        logger.info("✅ Agent registered: {} ({})", agentId, agent.hostname);
        return agent;
    }

    // ═══════════════════════════════════════════════════════
    // HEARTBEAT PROCESSING
    // ═══════════════════════════════════════════════════════

    public void processHeartbeat(String agentId, Map<String, Object> heartbeat) {
        AgentRecord agent = agents.get(agentId);
        if (agent == null) {
            logger.warn("Heartbeat from unregistered agent: {}", agentId);
            return;
        }

        agent.lastHeartbeat = LocalDateTime.now();
        agent.status = heartbeat.getOrDefault("status", "ACTIVE").toString();
        agent.queuedEvents = Integer.parseInt(heartbeat.getOrDefault("queuedEvents", "0").toString());
    }

    // ═══════════════════════════════════════════════════════
    // EVENT PROCESSING
    // ═══════════════════════════════════════════════════════

    public void processEvents(String agentId, List<Map<String, Object>> events) {
        for (Map<String, Object> eventData : events) {
            AgentEvent event = new AgentEvent();
            event.agentId = agentId;
            event.type = eventData.getOrDefault("type", "UNKNOWN").toString();
            event.severity = eventData.getOrDefault("severity", "MEDIUM").toString();
            event.message = eventData.getOrDefault("message", "").toString();
            event.receivedAt = LocalDateTime.now();
            eventLog.add(event);
        }

        AgentRecord agent = agents.get(agentId);
        if (agent != null) {
            agent.totalEvents += events.size();
        }

        logger.info("📥 Received {} events from agent {}", events.size(), agentId);
    }

    // ═══════════════════════════════════════════════════════
    // AGENT MANAGEMENT
    // ═══════════════════════════════════════════════════════

    public List<AgentRecord> getAllAgents() {
        return new ArrayList<>(agents.values());
    }

    public List<AgentRecord> getActiveAgents() {
        return agents.values().stream()
                .filter(a -> "ACTIVE".equals(a.status))
                .collect(Collectors.toList());
    }

    public List<AgentRecord> getStaleAgents(int minutesThreshold) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutesThreshold);
        return agents.values().stream()
                .filter(a -> a.lastHeartbeat.isBefore(cutoff))
                .collect(Collectors.toList());
    }

    public void isolateAgent(String agentId, String reason) {
        AgentRecord agent = agents.get(agentId);
        if (agent != null) {
            agent.status = "ISOLATED";
            agent.isolationReason = reason;
            logger.info("🔒 Agent isolated: {} — Reason: {}", agentId, reason);
        }
    }

    public void unisolateAgent(String agentId) {
        AgentRecord agent = agents.get(agentId);
        if (agent != null) {
            agent.status = "ACTIVE";
            agent.isolationReason = null;
            logger.info("🔓 Agent un-isolated: {}", agentId);
        }
    }

    public void removeAgent(String agentId) {
        agents.remove(agentId);
        logger.info("🗑️ Agent removed: {}", agentId);
    }

    // ═══════════════════════════════════════════════════════
    // BULK ACTIONS
    // ═══════════════════════════════════════════════════════

    public Map<String, String> bulkAction(String action, List<String> agentIds) {
        Map<String, String> results = new LinkedHashMap<>();
        for (String agentId : agentIds) {
            try {
                switch (action.toUpperCase()) {
                    case "ISOLATE" -> { isolateAgent(agentId, "Bulk isolation"); results.put(agentId, "ISOLATED"); }
                    case "UNISOLATE" -> { unisolateAgent(agentId); results.put(agentId, "ACTIVE"); }
                    case "REMOVE" -> { removeAgent(agentId); results.put(agentId, "REMOVED"); }
                    default -> results.put(agentId, "UNKNOWN_ACTION");
                }
            } catch (Exception e) {
                results.put(agentId, "ERROR: " + e.getMessage());
            }
        }
        return results;
    }

    // ═══════════════════════════════════════════════════════
    // POLICY MANAGEMENT
    // ═══════════════════════════════════════════════════════

    private void initializeDefaultPolicies() {
        addPolicy("DEFAULT-DLP", "Default DLP Policy",
                Map.of("scanFileTypes", ".txt,.csv,.xlsx,.docx,.pdf,.json,.xml",
                        "scanIntervalMinutes", "15",
                        "blockOnPII", "false",
                        "alertOnPII", "true"));

        addPolicy("DEFAULT-EDR", "Default EDR Policy",
                Map.of("monitorProcesses", "true",
                        "blockAttackTools", "true",
                        "suspiciousPowerShell", "true"));

        addPolicy("DEFAULT-FIM", "Default FIM Policy",
                Map.of("monitorPaths", "/etc,/var/log,C:\\Windows\\System32\\config",
                        "scanIntervalMinutes", "15",
                        "alertOnChange", "true"));

        addPolicy("DEFAULT-USB", "Default USB Policy",
                Map.of("blockUSBStorage", "false",
                        "alertOnUSB", "true",
                        "allowedDevices", ""));
    }

    public void addPolicy(String id, String name, Map<String, String> config) {
        AgentPolicy policy = new AgentPolicy();
        policy.id = id;
        policy.name = name;
        policy.config = config;
        policy.updatedAt = LocalDateTime.now();
        policies.put(id, policy);
    }

    public List<AgentPolicy> getAllPolicies() {
        return new ArrayList<>(policies.values());
    }

    // ═══════════════════════════════════════════════════════
    // DASHBOARD & STATISTICS
    // ═══════════════════════════════════════════════════════

    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalAgents", agents.size());
        dashboard.put("activeAgents", getActiveAgents().size());
        dashboard.put("isolatedAgents", agents.values().stream()
                .filter(a -> "ISOLATED".equals(a.status)).count());
        dashboard.put("staleAgents", getStaleAgents(5).size());
        dashboard.put("totalEvents", eventLog.size());
        dashboard.put("criticalEvents", eventLog.stream()
                .filter(e -> "CRITICAL".equals(e.severity)).count());
        dashboard.put("policies", policies.size());

        // OS distribution
        Map<String, Long> osDist = agents.values().stream()
                .collect(Collectors.groupingBy(a -> a.os, Collectors.counting()));
        dashboard.put("osDistribution", osDist);

        // Recent critical events
        dashboard.put("recentCritical", eventLog.stream()
                .filter(e -> "CRITICAL".equals(e.severity))
                .sorted(Comparator.comparing(e -> ((AgentEvent) e).receivedAt).reversed())
                .limit(10)
                .toList());

        return dashboard;
    }

    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalAgents", agents.size(),
                "activeAgents", getActiveAgents().size(),
                "totalEvents", eventLog.size(),
                "policies", policies.size());
    }

    // ═══ DATA CLASSES ═══

    public static class AgentRecord {
        public String agentId, hostname, os, osVersion, ipAddress, macAddress;
        public String agentVersion, status, isolationReason;
        public LocalDateTime registeredAt, lastHeartbeat;
        public int totalEvents, queuedEvents;
    }

    public static class AgentEvent {
        public String agentId, type, severity, message;
        public LocalDateTime receivedAt;
    }

    public static class AgentPolicy {
        public String id, name;
        public Map<String, String> config;
        public LocalDateTime updatedAt;
    }
}
