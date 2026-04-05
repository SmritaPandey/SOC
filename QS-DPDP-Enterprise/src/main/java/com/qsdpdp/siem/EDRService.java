package com.qsdpdp.siem;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Endpoint Detection and Response (EDR) Service
 * Enterprise-grade endpoint security module for QS-DPDP.
 *
 * Capabilities:
 *  - Endpoint agent registration and heartbeat tracking
 *  - Process monitoring with behavioral analysis
 *  - File integrity monitoring (FIM) with hash verification
 *  - Threat detection with automated response actions
 *  - DPDP Act alignment: tracks personal data access at endpoint level
 *  - Integrates with SIEM (SecurityEvent) and XDR (telemetry)
 *
 * Operates as an independent product but auto-integrates when
 * deployed as part of the QS-DPDP Universal Compliance Platform.
 *
 * @version 1.0.0
 * @since Module 8 — Cybersecurity / EDR
 */
@Service
public class EDRService {

    private static final Logger logger = LoggerFactory.getLogger(EDRService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final SIEMService siemService;
    private final XDRService xdrService;

    private boolean initialized = false;
    private ScheduledExecutorService scheduler;
    private volatile boolean monitoring = false;

    // In-memory state
    private final Map<String, EndpointAgent> agents = new ConcurrentHashMap<>();
    private final Map<String, FileIntegrityBaseline> fimBaselines = new ConcurrentHashMap<>();
    private final List<EDRPolicy> policies = new CopyOnWriteArrayList<>();
    private final List<EDRThreat> activeThreats = new CopyOnWriteArrayList<>();

    // Statistics
    private long totalEventsProcessed = 0;
    private long threatsDetected = 0;
    private long threatsContained = 0;
    private long processesBlocked = 0;

    @Autowired
    public EDRService(DatabaseManager dbManager, AuditService auditService,
                      EventBus eventBus, SIEMService siemService, XDRService xdrService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.siemService = siemService;
        this.xdrService = xdrService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing EDR Service...");

        createTables();
        loadPolicies();
        loadAgents();
        startMonitoring();
        subscribeToEvents();

        initialized = true;
        logger.info("EDR Service initialized — {} agents, {} policies, {} FIM baselines",
                agents.size(), policies.size(), fimBaselines.size());
    }

    // ═══════════════════════════════════════════════════════════
    // TABLE CREATION
    // ═══════════════════════════════════════════════════════════

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Endpoint agents registry
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edr_agents (
                    id TEXT PRIMARY KEY,
                    hostname TEXT NOT NULL,
                    os_type TEXT NOT NULL,
                    os_version TEXT,
                    agent_version TEXT DEFAULT '1.0.0',
                    ip_address TEXT,
                    mac_address TEXT,
                    status TEXT DEFAULT 'ACTIVE',
                    isolation_status TEXT DEFAULT 'NONE',
                    last_heartbeat TIMESTAMP,
                    last_scan TIMESTAMP,
                    threat_count INTEGER DEFAULT 0,
                    department TEXT,
                    owner TEXT,
                    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP
                )
            """);

            // Process events
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edr_process_events (
                    id TEXT PRIMARY KEY,
                    agent_id TEXT NOT NULL,
                    process_name TEXT NOT NULL,
                    process_id INTEGER,
                    parent_process TEXT,
                    parent_pid INTEGER,
                    command_line TEXT,
                    file_path TEXT,
                    file_hash TEXT,
                    user_name TEXT,
                    action TEXT NOT NULL,
                    verdict TEXT DEFAULT 'ALLOWED',
                    risk_score INTEGER DEFAULT 0,
                    mitre_tactic TEXT,
                    mitre_technique TEXT,
                    personal_data_access INTEGER DEFAULT 0,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (agent_id) REFERENCES edr_agents(id)
                )
            """);

            // File integrity monitoring
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edr_fim_events (
                    id TEXT PRIMARY KEY,
                    agent_id TEXT NOT NULL,
                    file_path TEXT NOT NULL,
                    change_type TEXT NOT NULL,
                    old_hash TEXT,
                    new_hash TEXT,
                    old_size INTEGER,
                    new_size INTEGER,
                    modified_by TEXT,
                    process_name TEXT,
                    severity TEXT DEFAULT 'LOW',
                    personal_data_file INTEGER DEFAULT 0,
                    acknowledged INTEGER DEFAULT 0,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (agent_id) REFERENCES edr_agents(id)
                )
            """);

            // Threats
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edr_threats (
                    id TEXT PRIMARY KEY,
                    agent_id TEXT NOT NULL,
                    threat_type TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    description TEXT,
                    process_name TEXT,
                    file_path TEXT,
                    file_hash TEXT,
                    mitre_tactic TEXT,
                    mitre_technique TEXT,
                    indicators TEXT,
                    status TEXT DEFAULT 'DETECTED',
                    response_action TEXT,
                    response_at TIMESTAMP,
                    personal_data_risk INTEGER DEFAULT 0,
                    dpdp_section TEXT,
                    analyst_notes TEXT,
                    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved_at TIMESTAMP,
                    FOREIGN KEY (agent_id) REFERENCES edr_agents(id)
                )
            """);

            // EDR Policies
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edr_policies (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT,
                    category TEXT NOT NULL,
                    enabled INTEGER DEFAULT 1,
                    severity TEXT DEFAULT 'MEDIUM',
                    detection_pattern TEXT,
                    auto_response TEXT,
                    mitre_mapping TEXT,
                    dpdp_section TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP
                )
            """);

            // Response actions log
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edr_response_actions (
                    id TEXT PRIMARY KEY,
                    threat_id TEXT NOT NULL,
                    agent_id TEXT NOT NULL,
                    action_type TEXT NOT NULL,
                    status TEXT DEFAULT 'PENDING',
                    details TEXT,
                    executed_by TEXT,
                    executed_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    result TEXT,
                    FOREIGN KEY (threat_id) REFERENCES edr_threats(id),
                    FOREIGN KEY (agent_id) REFERENCES edr_agents(id)
                )
            """);

            // Indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edr_proc_agent ON edr_process_events(agent_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edr_proc_ts ON edr_process_events(timestamp)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edr_fim_agent ON edr_fim_events(agent_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edr_threats_status ON edr_threats(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edr_threats_severity ON edr_threats(severity)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edr_agents_status ON edr_agents(status)");

            logger.info("EDR tables created");

        } catch (SQLException e) {
            logger.error("Failed to create EDR tables", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // AGENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public EndpointAgent registerAgent(String hostname, String osType, String osVersion,
                                        String ipAddress, String macAddress, String department, String owner) {
        EndpointAgent agent = new EndpointAgent();
        agent.id = UUID.randomUUID().toString();
        agent.hostname = hostname;
        agent.osType = osType;
        agent.osVersion = osVersion;
        agent.ipAddress = ipAddress;
        agent.macAddress = macAddress;
        agent.department = department;
        agent.owner = owner;
        agent.status = "ACTIVE";
        agent.isolationStatus = "NONE";
        agent.registeredAt = LocalDateTime.now();
        agent.lastHeartbeat = LocalDateTime.now();

        agents.put(agent.id, agent);
        persistAgent(agent);

        auditService.log("EDR_AGENT_REGISTERED", "EDR", "SYSTEM",
                String.format("Agent registered: %s (%s %s) at %s", hostname, osType, osVersion, ipAddress));

        // Notify XDR
        xdrService.ingestTelemetry("EDR", "agent_registered",
                "New endpoint agent: " + hostname,
                Map.of("agentId", agent.id, "hostname", hostname, "os", osType));

        logger.info("Registered EDR agent: {} ({})", hostname, agent.id);
        return agent;
    }

    public void processHeartbeat(String agentId) {
        EndpointAgent agent = agents.get(agentId);
        if (agent != null) {
            agent.lastHeartbeat = LocalDateTime.now();
            agent.status = "ACTIVE";
        }
    }

    public void isolateEndpoint(String agentId, String reason, String isolatedBy) {
        EndpointAgent agent = agents.get(agentId);
        if (agent == null) {
            logger.warn("Cannot isolate unknown agent: {}", agentId);
            return;
        }

        agent.isolationStatus = "ISOLATED";
        agent.status = "ISOLATED";
        updateAgentStatus(agentId, "ISOLATED", "ISOLATED");

        // Log response action
        logResponseAction(null, agentId, "ISOLATE_ENDPOINT",
                "Endpoint isolated: " + reason, isolatedBy);

        // Send to SIEM
        SecurityEvent siemEvent = SecurityEvent.builder()
                .category(EventCategory.NETWORK_BLOCK)
                .severity(EventSeverity.HIGH)
                .source("EDR", agentId)
                .message("Endpoint isolated: " + agent.hostname + " — " + reason)
                .build();
        siemService.ingestEvent(siemEvent);

        auditService.log("EDR_ENDPOINT_ISOLATED", "EDR", isolatedBy,
                "Isolated endpoint " + agent.hostname + ": " + reason);

        logger.warn("Endpoint ISOLATED: {} ({}) — {}", agent.hostname, agentId, reason);
    }

    public void releaseEndpoint(String agentId, String releasedBy) {
        EndpointAgent agent = agents.get(agentId);
        if (agent == null) return;

        agent.isolationStatus = "NONE";
        agent.status = "ACTIVE";
        updateAgentStatus(agentId, "ACTIVE", "NONE");

        logResponseAction(null, agentId, "RELEASE_ENDPOINT",
                "Endpoint released from isolation", releasedBy);

        auditService.log("EDR_ENDPOINT_RELEASED", "EDR", releasedBy,
                "Released endpoint " + agent.hostname + " from isolation");

        logger.info("Endpoint released: {} ({})", agent.hostname, agentId);
    }

    // ═══════════════════════════════════════════════════════════
    // PROCESS MONITORING
    // ═══════════════════════════════════════════════════════════

    public ProcessVerdict evaluateProcess(String agentId, String processName, int pid,
                                           String parentProcess, int parentPid,
                                           String commandLine, String filePath,
                                           String fileHash, String userName) {
        totalEventsProcessed++;

        ProcessVerdict verdict = new ProcessVerdict();
        verdict.processName = processName;
        verdict.allowed = true;
        verdict.riskScore = 0;

        // Check against policies
        for (EDRPolicy policy : policies) {
            if (!policy.enabled) continue;
            if (!"PROCESS".equals(policy.category) && !"ALL".equals(policy.category)) continue;

            int matchScore = evaluateProcessAgainstPolicy(policy, processName, commandLine,
                    filePath, fileHash, parentProcess);

            if (matchScore > verdict.riskScore) {
                verdict.riskScore = matchScore;
                verdict.matchedPolicy = policy;
            }
        }

        // Determine verdict based on risk score
        if (verdict.riskScore >= 90) {
            verdict.allowed = false;
            verdict.action = "BLOCKED";
            verdict.reason = "Critical threat detected: " + verdict.matchedPolicy.name;
            processesBlocked++;

            // Create threat
            createThreat(agentId, "MALICIOUS_PROCESS", "CRITICAL",
                    "Blocked process: " + processName + " (" + commandLine + ")",
                    processName, filePath, fileHash,
                    verdict.matchedPolicy.mitreMapping, "BLOCK_PROCESS");

        } else if (verdict.riskScore >= 70) {
            verdict.allowed = true; // Allow but alert
            verdict.action = "ALERT";
            verdict.reason = "Suspicious activity detected";

            createThreat(agentId, "SUSPICIOUS_PROCESS", "HIGH",
                    "Suspicious process: " + processName,
                    processName, filePath, fileHash,
                    verdict.matchedPolicy != null ? verdict.matchedPolicy.mitreMapping : null,
                    "ALERT");

        } else if (verdict.riskScore >= 40) {
            verdict.allowed = true;
            verdict.action = "LOG";
            verdict.reason = "Moderate risk — logged for review";
        } else {
            verdict.action = "ALLOW";
            verdict.reason = "Clean";
        }

        // Persist process event
        persistProcessEvent(agentId, processName, pid, parentProcess, parentPid,
                commandLine, filePath, fileHash, userName, verdict);

        // Feed to SIEM if risky
        if (verdict.riskScore >= 40) {
            SecurityEvent siemEvent = SecurityEvent.builder()
                    .category(verdict.riskScore >= 70 ? EventCategory.MALWARE_DETECTED : EventCategory.APP_WARNING)
                    .severity(verdict.riskScore >= 90 ? EventSeverity.CRITICAL
                            : verdict.riskScore >= 70 ? EventSeverity.HIGH : EventSeverity.MEDIUM)
                    .source("EDR", agentId)
                    .user(null, userName)
                    .message("EDR: " + verdict.reason + " — " + processName)
                    .build();
            siemService.ingestEvent(siemEvent);
        }

        // Feed to XDR
        if (verdict.riskScore >= 30) {
            xdrService.ingestTelemetry("EDR",
                    verdict.riskScore >= 70 ? "malware_execution" : "suspicious_process",
                    processName + " on " + getAgentHostname(agentId),
                    Map.of("process", processName, "risk", verdict.riskScore,
                            "action", verdict.action, "agentId", agentId));
        }

        return verdict;
    }

    private int evaluateProcessAgainstPolicy(EDRPolicy policy, String processName,
                                              String commandLine, String filePath,
                                              String fileHash, String parentProcess) {
        int score = 0;
        String pattern = policy.detectionPattern != null ? policy.detectionPattern.toLowerCase() : "";

        // Known malicious process names
        if (pattern.contains("process_name") && processName != null) {
            String procLower = processName.toLowerCase();
            if (procLower.contains("mimikatz") || procLower.contains("psexec") ||
                procLower.contains("cobaltstrike") || procLower.contains("lazagne") ||
                procLower.contains("bloodhound") || procLower.contains("rubeus")) {
                score = 95;
            }
        }

        // Suspicious command-line patterns
        if (commandLine != null) {
            String cmdLower = commandLine.toLowerCase();
            if (cmdLower.contains("powershell") && cmdLower.contains("-encodedcommand")) score = Math.max(score, 85);
            if (cmdLower.contains("certutil") && cmdLower.contains("-urlcache")) score = Math.max(score, 80);
            if (cmdLower.contains("bitsadmin") && cmdLower.contains("/transfer")) score = Math.max(score, 75);
            if (cmdLower.contains("reg.exe") && cmdLower.contains("sam")) score = Math.max(score, 85);
            if (cmdLower.contains("net user") && cmdLower.contains("/add")) score = Math.max(score, 70);
            if (cmdLower.contains("schtasks") && cmdLower.contains("/create")) score = Math.max(score, 60);
            if (cmdLower.contains("wmic") && cmdLower.contains("process call")) score = Math.max(score, 70);
            if (cmdLower.contains("vssadmin") && cmdLower.contains("delete shadows")) score = Math.max(score, 90);
        }

        // Living-off-the-land binaries (LOLBins) from unusual parents
        if (processName != null && parentProcess != null) {
            String procLower = processName.toLowerCase();
            String parentLower = parentProcess.toLowerCase();
            if (procLower.equals("cmd.exe") && parentLower.contains("excel")) score = Math.max(score, 65);
            if (procLower.equals("powershell.exe") && parentLower.contains("winword")) score = Math.max(score, 70);
            if (procLower.equals("mshta.exe")) score = Math.max(score, 75);
            if (procLower.equals("cscript.exe") && parentLower.contains("outlook")) score = Math.max(score, 70);
        }

        return score;
    }

    // ═══════════════════════════════════════════════════════════
    // FILE INTEGRITY MONITORING (FIM)
    // ═══════════════════════════════════════════════════════════

    public void setFIMBaseline(String agentId, String filePath, String hash, long fileSize) {
        String key = agentId + ":" + filePath;
        fimBaselines.put(key, new FileIntegrityBaseline(filePath, hash, fileSize, LocalDateTime.now()));
    }

    public FIMResult checkFileIntegrity(String agentId, String filePath, String currentHash,
                                         long currentSize, String modifiedBy, String processName) {
        String key = agentId + ":" + filePath;
        FileIntegrityBaseline baseline = fimBaselines.get(key);

        FIMResult result = new FIMResult();
        result.filePath = filePath;
        result.agentId = agentId;

        if (baseline == null) {
            // New file — set baseline
            setFIMBaseline(agentId, filePath, currentHash, currentSize);
            result.changeType = "NEW";
            result.severity = "LOW";
            result.changed = false;
            return result;
        }

        if (!baseline.hash.equals(currentHash)) {
            result.changed = true;
            result.changeType = "MODIFIED";
            result.oldHash = baseline.hash;
            result.newHash = currentHash;
            result.oldSize = baseline.fileSize;
            result.newSize = currentSize;

            // Determine severity based on file location
            boolean isCriticalPath = isPersonalDataPath(filePath) || isSystemPath(filePath);
            result.personalDataFile = isPersonalDataPath(filePath);
            result.severity = isCriticalPath ? "HIGH" : "MEDIUM";

            // Persist
            persistFIMEvent(agentId, filePath, result, modifiedBy, processName);

            // Update baseline
            setFIMBaseline(agentId, filePath, currentHash, currentSize);

            // Alert if critical
            if (isCriticalPath) {
                SecurityEvent siemEvent = SecurityEvent.builder()
                        .category(EventCategory.CONFIG_CHANGE)
                        .severity(result.personalDataFile ? EventSeverity.HIGH : EventSeverity.MEDIUM)
                        .source("EDR-FIM", agentId)
                        .user(null, modifiedBy)
                        .resource(filePath, "FILE")
                        .message("File integrity change: " + filePath)
                        .sensitiveData(result.personalDataFile)
                        .build();
                siemService.ingestEvent(siemEvent);

                if (result.personalDataFile) {
                    xdrService.ingestTelemetry("EDR", "data_staging",
                            "Personal data file modified: " + filePath,
                            Map.of("file", filePath, "agent", agentId, "modifiedBy", modifiedBy));
                }
            }
        } else {
            result.changed = false;
            result.changeType = "NONE";
            result.severity = "INFO";
        }

        return result;
    }

    private boolean isPersonalDataPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.contains("personal") || lower.contains("pii") || lower.contains("customer") ||
               lower.contains("employee") || lower.contains("patient") || lower.contains("aadhaar") ||
               lower.contains("passport") || lower.contains("consent") || lower.contains("kyc") ||
               lower.contains("pan_card") || lower.contains("bank_detail");
    }

    private boolean isSystemPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.contains("system32") || lower.contains("/etc/") || lower.contains("\\windows\\") ||
               lower.contains("/bin/") || lower.contains("/sbin/") || lower.contains("boot.ini") ||
               lower.contains("registry") || lower.contains("shadow") || lower.contains("passwd");
    }

    // ═══════════════════════════════════════════════════════════
    // THREAT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public EDRThreat createThreat(String agentId, String threatType, String severity,
                                   String description, String processName, String filePath,
                                   String fileHash, String mitreMapping, String autoResponse) {
        EDRThreat threat = new EDRThreat();
        threat.id = "EDR-THR-" + System.currentTimeMillis();
        threat.agentId = agentId;
        threat.threatType = threatType;
        threat.severity = severity;
        threat.description = description;
        threat.processName = processName;
        threat.filePath = filePath;
        threat.fileHash = fileHash;
        threat.mitreTactic = mitreMapping;
        threat.status = "DETECTED";
        threat.detectedAt = LocalDateTime.now();
        threat.personalDataRisk = isPersonalDataPath(filePath);

        if (threat.personalDataRisk) {
            threat.dpdpSection = "Section 8 - Breach Notification";
        }

        activeThreats.add(threat);
        persistThreat(threat);
        threatsDetected++;

        // Update agent threat count
        EndpointAgent agent = agents.get(agentId);
        if (agent != null) {
            agent.threatCount++;
        }

        // Auto-respond if configured
        if (autoResponse != null && !"ALERT".equals(autoResponse)) {
            executeResponse(threat, autoResponse, "AUTO");
        }

        // Publish event
        eventBus.publish(new ComplianceEvent("edr.threat.detected",
                Map.of("threatId", threat.id, "type", threatType,
                        "severity", severity, "agent", agentId)));

        logger.warn("EDR THREAT: {} — {} [{}] on agent {}",
                threat.id, threatType, severity, agentId);
        return threat;
    }

    public void executeResponse(EDRThreat threat, String actionType, String executedBy) {
        logResponseAction(threat.id, threat.agentId, actionType,
                "Response to threat: " + threat.description, executedBy);

        switch (actionType.toUpperCase()) {
            case "BLOCK_PROCESS" -> {
                threat.responseAction = "Process blocked";
                logger.info("EDR Response: Blocking process {} on agent {}", threat.processName, threat.agentId);
            }
            case "QUARANTINE_FILE" -> {
                threat.responseAction = "File quarantined";
                logger.info("EDR Response: Quarantining {} on agent {}", threat.filePath, threat.agentId);
            }
            case "ISOLATE_ENDPOINT" -> {
                isolateEndpoint(threat.agentId, "Auto-response to threat: " + threat.threatType, executedBy);
                threat.responseAction = "Endpoint isolated";
            }
            case "KILL_PROCESS" -> {
                threat.responseAction = "Process terminated";
                logger.info("EDR Response: Killing process {} (agent {})", threat.processName, threat.agentId);
            }
            case "COLLECT_FORENSICS" -> {
                threat.responseAction = "Forensic data collected";
                logger.info("EDR Response: Collecting forensics from agent {}", threat.agentId);
            }
            default -> {
                threat.responseAction = "Action logged: " + actionType;
                logger.info("EDR Response: {} on agent {}", actionType, threat.agentId);
            }
        }

        threat.responseAt = LocalDateTime.now();
        threat.status = "RESPONDING";
        threatsContained++;

        auditService.log("EDR_RESPONSE", "EDR", executedBy,
                String.format("Response %s for threat %s on %s", actionType, threat.id, threat.agentId));
    }

    public void resolveThreat(String threatId, String resolution, String resolvedBy) {
        for (EDRThreat threat : activeThreats) {
            if (threat.id.equals(threatId)) {
                threat.status = "RESOLVED";
                threat.analystNotes = resolution;
                threat.resolvedAt = LocalDateTime.now();

                String sql = "UPDATE edr_threats SET status = 'RESOLVED', analyst_notes = ?, resolved_at = ? WHERE id = ?";
                try (Connection conn = dbManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, resolution);
                    stmt.setString(2, LocalDateTime.now().toString());
                    stmt.setString(3, threatId);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    logger.error("Failed to resolve threat: {}", threatId, e);
                }

                auditService.log("EDR_THREAT_RESOLVED", "EDR", resolvedBy,
                        "Threat " + threatId + " resolved: " + resolution);
                break;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // POLICY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public EDRPolicy addPolicy(String name, String description, String category,
                                String severity, String detectionPattern,
                                String autoResponse, String mitreMapping, String dpdpSection) {
        EDRPolicy policy = new EDRPolicy();
        policy.id = UUID.randomUUID().toString();
        policy.name = name;
        policy.description = description;
        policy.category = category;
        policy.enabled = true;
        policy.severity = severity;
        policy.detectionPattern = detectionPattern;
        policy.autoResponse = autoResponse;
        policy.mitreMapping = mitreMapping;
        policy.dpdpSection = dpdpSection;

        policies.add(policy);
        persistPolicy(policy);

        auditService.log("EDR_POLICY_CREATED", "EDR", "SYSTEM", "Policy created: " + name);
        return policy;
    }

    public void updatePolicy(String policyId, Map<String, String> updates) {
        for (EDRPolicy policy : policies) {
            if (policy.id.equals(policyId)) {
                if (updates.containsKey("name")) policy.name = updates.get("name");
                if (updates.containsKey("description")) policy.description = updates.get("description");
                if (updates.containsKey("enabled")) policy.enabled = Boolean.parseBoolean(updates.get("enabled"));
                if (updates.containsKey("severity")) policy.severity = updates.get("severity");
                if (updates.containsKey("autoResponse")) policy.autoResponse = updates.get("autoResponse");
                persistPolicy(policy);
                break;
            }
        }
    }

    public void deletePolicy(String policyId) {
        policies.removeIf(p -> p.id.equals(policyId));
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM edr_policies WHERE id = ?")) {
            stmt.setString(1, policyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to delete EDR policy: {}", policyId, e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES & STATISTICS
    // ═══════════════════════════════════════════════════════════

    public List<EndpointAgent> getAllAgents() {
        return new ArrayList<>(agents.values());
    }

    public List<EndpointAgent> getActiveAgents() {
        return agents.values().stream()
                .filter(a -> "ACTIVE".equals(a.status))
                .toList();
    }

    public List<EndpointAgent> getIsolatedAgents() {
        return agents.values().stream()
                .filter(a -> "ISOLATED".equals(a.isolationStatus))
                .toList();
    }

    public List<EDRThreat> getActiveThreats() {
        return activeThreats.stream()
                .filter(t -> !"RESOLVED".equals(t.status))
                .toList();
    }

    public List<EDRThreat> getThreatsByAgent(String agentId) {
        return activeThreats.stream()
                .filter(t -> agentId.equals(t.agentId))
                .toList();
    }

    public List<EDRPolicy> getAllPolicies() {
        return new ArrayList<>(policies);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAgents", agents.size());
        stats.put("activeAgents", getActiveAgents().size());
        stats.put("isolatedAgents", getIsolatedAgents().size());
        stats.put("totalEventsProcessed", totalEventsProcessed);
        stats.put("threatsDetected", threatsDetected);
        stats.put("threatsContained", threatsContained);
        stats.put("processesBlocked", processesBlocked);
        stats.put("activeThreats", getActiveThreats().size());
        stats.put("activePolicies", policies.stream().filter(p -> p.enabled).count());
        stats.put("fimBaselines", fimBaselines.size());

        // Threat severity breakdown
        Map<String, Long> severityBreakdown = new LinkedHashMap<>();
        for (EDRThreat t : activeThreats) {
            severityBreakdown.merge(t.severity, 1L, Long::sum);
        }
        stats.put("threatsBySeverity", severityBreakdown);

        // OS distribution
        Map<String, Long> osDistribution = new LinkedHashMap<>();
        for (EndpointAgent agent : agents.values()) {
            osDistribution.merge(agent.osType, 1L, Long::sum);
        }
        stats.put("osDistribution", osDistribution);

        return stats;
    }

    public Map<String, Object> getDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("statistics", getStatistics());
        dashboard.put("recentThreats", getActiveThreats().stream().limit(10).toList());
        dashboard.put("agents", getAllAgents());
        dashboard.put("policies", getAllPolicies());
        return dashboard;
    }

    // ═══════════════════════════════════════════════════════════
    // MONITORING & EVENTS
    // ═══════════════════════════════════════════════════════════

    private void startMonitoring() {
        scheduler = Executors.newScheduledThreadPool(2);
        monitoring = true;

        // Check agent heartbeats every 30 seconds
        scheduler.scheduleAtFixedRate(this::checkAgentHeartbeats, 30, 30, TimeUnit.SECONDS);

        // Clean old events every hour
        scheduler.scheduleAtFixedRate(this::cleanOldEvents, 1, 1, TimeUnit.HOURS);

        logger.info("EDR monitoring started");
    }

    private void checkAgentHeartbeats() {
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(5);
        for (EndpointAgent agent : agents.values()) {
            if ("ACTIVE".equals(agent.status) && agent.lastHeartbeat != null &&
                agent.lastHeartbeat.isBefore(staleThreshold)) {
                agent.status = "STALE";
                logger.warn("EDR agent heartbeat stale: {} ({})", agent.hostname, agent.id);

                xdrService.ingestTelemetry("EDR", "agent_offline",
                        "Agent heartbeat lost: " + agent.hostname,
                        Map.of("agentId", agent.id, "lastHeartbeat", agent.lastHeartbeat.toString()));
            }
        }
    }

    private void cleanOldEvents() {
        // Remove resolved threats older than 30 days from memory
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        activeThreats.removeIf(t -> "RESOLVED".equals(t.status) &&
                t.resolvedAt != null && t.resolvedAt.isBefore(cutoff));
    }

    private void subscribeToEvents() {
        eventBus.subscribe("edr.*", this::handleEDREvent);
        eventBus.subscribe("siem.alert.created", this::handleSIEMAlert);
    }

    private void handleEDREvent(ComplianceEvent event) {
        logger.debug("EDR event received: {}", event.getType());
    }

    @SuppressWarnings("unchecked")
    private void handleSIEMAlert(ComplianceEvent event) {
        // Auto-respond to SIEM alerts that involve endpoints
        Object payload = event.getPayload();
        if (payload instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) payload;
            String severity = (String) data.get("severity");
            if ("CRITICAL".equals(severity)) {
                logger.info("EDR received CRITICAL SIEM alert — checking endpoint involvement");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    private void persistAgent(EndpointAgent agent) {
        String sql = """
            INSERT OR REPLACE INTO edr_agents
            (id, hostname, os_type, os_version, agent_version, ip_address, mac_address,
             status, isolation_status, last_heartbeat, threat_count, department, owner, registered_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, agent.id);
            stmt.setString(2, agent.hostname);
            stmt.setString(3, agent.osType);
            stmt.setString(4, agent.osVersion);
            stmt.setString(5, agent.agentVersion);
            stmt.setString(6, agent.ipAddress);
            stmt.setString(7, agent.macAddress);
            stmt.setString(8, agent.status);
            stmt.setString(9, agent.isolationStatus);
            stmt.setString(10, agent.lastHeartbeat != null ? agent.lastHeartbeat.toString() : null);
            stmt.setInt(11, agent.threatCount);
            stmt.setString(12, agent.department);
            stmt.setString(13, agent.owner);
            stmt.setString(14, agent.registeredAt.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist EDR agent", e);
        }
    }

    private void updateAgentStatus(String agentId, String status, String isolation) {
        String sql = "UPDATE edr_agents SET status = ?, isolation_status = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, isolation);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.setString(4, agentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update agent status", e);
        }
    }

    private void persistProcessEvent(String agentId, String processName, int pid,
                                      String parentProcess, int parentPid,
                                      String commandLine, String filePath, String fileHash,
                                      String userName, ProcessVerdict verdict) {
        String sql = """
            INSERT INTO edr_process_events
            (id, agent_id, process_name, process_id, parent_process, parent_pid,
             command_line, file_path, file_hash, user_name, action, verdict, risk_score,
             mitre_tactic, personal_data_access)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, agentId);
            stmt.setString(3, processName);
            stmt.setInt(4, pid);
            stmt.setString(5, parentProcess);
            stmt.setInt(6, parentPid);
            stmt.setString(7, commandLine);
            stmt.setString(8, filePath);
            stmt.setString(9, fileHash);
            stmt.setString(10, userName);
            stmt.setString(11, verdict.action);
            stmt.setString(12, verdict.allowed ? "ALLOWED" : "BLOCKED");
            stmt.setInt(13, verdict.riskScore);
            stmt.setString(14, verdict.matchedPolicy != null ? verdict.matchedPolicy.mitreMapping : null);
            stmt.setInt(15, isPersonalDataPath(filePath) ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist process event", e);
        }
    }

    private void persistFIMEvent(String agentId, String filePath, FIMResult result,
                                  String modifiedBy, String processName) {
        String sql = """
            INSERT INTO edr_fim_events
            (id, agent_id, file_path, change_type, old_hash, new_hash, old_size, new_size,
             modified_by, process_name, severity, personal_data_file)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, agentId);
            stmt.setString(3, filePath);
            stmt.setString(4, result.changeType);
            stmt.setString(5, result.oldHash);
            stmt.setString(6, result.newHash);
            stmt.setLong(7, result.oldSize);
            stmt.setLong(8, result.newSize);
            stmt.setString(9, modifiedBy);
            stmt.setString(10, processName);
            stmt.setString(11, result.severity);
            stmt.setInt(12, result.personalDataFile ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist FIM event", e);
        }
    }

    private void persistThreat(EDRThreat threat) {
        String sql = """
            INSERT INTO edr_threats
            (id, agent_id, threat_type, severity, description, process_name, file_path, file_hash,
             mitre_tactic, status, personal_data_risk, dpdp_section, detected_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, threat.id);
            stmt.setString(2, threat.agentId);
            stmt.setString(3, threat.threatType);
            stmt.setString(4, threat.severity);
            stmt.setString(5, threat.description);
            stmt.setString(6, threat.processName);
            stmt.setString(7, threat.filePath);
            stmt.setString(8, threat.fileHash);
            stmt.setString(9, threat.mitreTactic);
            stmt.setString(10, threat.status);
            stmt.setInt(11, threat.personalDataRisk ? 1 : 0);
            stmt.setString(12, threat.dpdpSection);
            stmt.setString(13, threat.detectedAt.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist EDR threat", e);
        }
    }

    private void persistPolicy(EDRPolicy policy) {
        String sql = """
            INSERT OR REPLACE INTO edr_policies
            (id, name, description, category, enabled, severity, detection_pattern,
             auto_response, mitre_mapping, dpdp_section, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, policy.id);
            stmt.setString(2, policy.name);
            stmt.setString(3, policy.description);
            stmt.setString(4, policy.category);
            stmt.setInt(5, policy.enabled ? 1 : 0);
            stmt.setString(6, policy.severity);
            stmt.setString(7, policy.detectionPattern);
            stmt.setString(8, policy.autoResponse);
            stmt.setString(9, policy.mitreMapping);
            stmt.setString(10, policy.dpdpSection);
            stmt.setString(11, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist EDR policy", e);
        }
    }

    private void logResponseAction(String threatId, String agentId, String actionType,
                                    String details, String executedBy) {
        String sql = """
            INSERT INTO edr_response_actions
            (id, threat_id, agent_id, action_type, status, details, executed_by, executed_at)
            VALUES (?, ?, ?, ?, 'COMPLETED', ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, threatId);
            stmt.setString(3, agentId);
            stmt.setString(4, actionType);
            stmt.setString(5, details);
            stmt.setString(6, executedBy);
            stmt.setString(7, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to log response action", e);
        }
    }

    private void loadPolicies() {
        // Load defaults
        addPolicy("Credential Theft Detection", "Detects credential dumping tools and techniques",
                "PROCESS", "CRITICAL", "process_name:mimikatz,lazagne,rubeus",
                "BLOCK_PROCESS", "T1003 - Credential Dumping", "Section 8");

        addPolicy("Ransomware Prevention", "Blocks shadow copy deletion and mass file encryption",
                "PROCESS", "CRITICAL", "process_name:vssadmin;command:delete shadows",
                "KILL_PROCESS", "T1490 - Inhibit System Recovery", "Section 8");

        addPolicy("PowerShell Abuse Detection", "Detects encoded PowerShell commands",
                "PROCESS", "HIGH", "process_name:powershell;command:encodedcommand",
                "ALERT", "T1059.001 - PowerShell", null);

        addPolicy("Lateral Movement Detection", "Detects PsExec and WMI remote execution",
                "PROCESS", "HIGH", "process_name:psexec,wmic",
                "ALERT", "T1570 - Lateral Tool Transfer", null);

        addPolicy("PII File Access Monitor", "Monitors access to personal data files",
                "FILE", "MEDIUM", "path:personal,pii,customer,employee,aadhaar,pan_card",
                "LOG", null, "Section 7 - Processing");

        addPolicy("System File Integrity", "Monitors changes to critical system files",
                "FILE", "HIGH", "path:system32,windows,etc",
                "ALERT", "T1565 - Data Manipulation", null);

        addPolicy("Phishing Document Detection", "Detects Office macros spawning shells",
                "PROCESS", "HIGH", "parent:winword,excel;child:cmd,powershell,mshta",
                "BLOCK_PROCESS", "T1566 - Phishing", null);

        addPolicy("Data Exfiltration Monitor", "Monitors large data transfers from endpoints",
                "ALL", "HIGH", "action:copy,transfer;size:>100MB",
                "ALERT", "T1041 - Exfiltration Over C2", "Section 8");
    }

    private void loadAgents() {
        // Load from database
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM edr_agents WHERE status != 'DECOMMISSIONED'")) {
            while (rs.next()) {
                EndpointAgent agent = new EndpointAgent();
                agent.id = rs.getString("id");
                agent.hostname = rs.getString("hostname");
                agent.osType = rs.getString("os_type");
                agent.osVersion = rs.getString("os_version");
                agent.agentVersion = rs.getString("agent_version");
                agent.ipAddress = rs.getString("ip_address");
                agent.macAddress = rs.getString("mac_address");
                agent.status = rs.getString("status");
                agent.isolationStatus = rs.getString("isolation_status");
                agent.threatCount = rs.getInt("threat_count");
                agent.department = rs.getString("department");
                agent.owner = rs.getString("owner");
                agents.put(agent.id, agent);
            }
        } catch (SQLException e) {
            logger.debug("Loading agents (table may not exist yet): {}", e.getMessage());
        }
    }

    private String getAgentHostname(String agentId) {
        EndpointAgent agent = agents.get(agentId);
        return agent != null ? agent.hostname : "Unknown";
    }

    public void shutdown() {
        monitoring = false;
        if (scheduler != null) scheduler.shutdown();
        logger.info("EDR Service shutdown");
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class EndpointAgent {
        public String id, hostname, osType, osVersion, agentVersion;
        public String ipAddress, macAddress, status, isolationStatus;
        public String department, owner;
        public int threatCount;
        public LocalDateTime lastHeartbeat, lastScan, registeredAt;

        public String getId() { return id; }
        public String getHostname() { return hostname; }
        public String getOsType() { return osType; }
        public String getOsVersion() { return osVersion; }
        public String getIpAddress() { return ipAddress; }
        public String getStatus() { return status; }
        public String getIsolationStatus() { return isolationStatus; }
        public int getThreatCount() { return threatCount; }
        public String getDepartment() { return department; }
        public String getOwner() { return owner; }
    }

    public static class EDRThreat {
        public String id, agentId, threatType, severity, description;
        public String processName, filePath, fileHash;
        public String mitreTactic, status, responseAction;
        public String dpdpSection, analystNotes;
        public boolean personalDataRisk;
        public LocalDateTime detectedAt, responseAt, resolvedAt;

        public String getId() { return id; }
        public String getAgentId() { return agentId; }
        public String getThreatType() { return threatType; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public String getStatus() { return status; }
        public String getMitreTactic() { return mitreTactic; }
        public boolean isPersonalDataRisk() { return personalDataRisk; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
    }

    public static class EDRPolicy {
        public String id, name, description, category, severity;
        public String detectionPattern, autoResponse, mitreMapping, dpdpSection;
        public boolean enabled;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getSeverity() { return severity; }
        public boolean isEnabled() { return enabled; }
        public String getMitreMapping() { return mitreMapping; }
    }

    public static class ProcessVerdict {
        public String processName, action, reason;
        public boolean allowed;
        public int riskScore;
        public EDRPolicy matchedPolicy;

        public boolean isAllowed() { return allowed; }
        public String getAction() { return action; }
        public int getRiskScore() { return riskScore; }
    }

    public static class FIMResult {
        public String filePath, agentId, changeType, severity;
        public String oldHash, newHash;
        public long oldSize, newSize;
        public boolean changed, personalDataFile;

        public boolean isChanged() { return changed; }
        public String getSeverity() { return severity; }
    }

    public static class FileIntegrityBaseline {
        String path, hash;
        long fileSize;
        LocalDateTime baselineAt;

        FileIntegrityBaseline(String path, String hash, long fileSize, LocalDateTime baselineAt) {
            this.path = path;
            this.hash = hash;
            this.fileSize = fileSize;
            this.baselineAt = baselineAt;
        }
    }
}
