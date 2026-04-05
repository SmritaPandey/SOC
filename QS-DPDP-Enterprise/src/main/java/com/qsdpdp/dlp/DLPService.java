package com.qsdpdp.dlp;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import com.qsdpdp.pii.*;
import com.qsdpdp.siem.EventCategory;
import com.qsdpdp.siem.EventSeverity;
import com.qsdpdp.siem.SecurityEvent;
import com.qsdpdp.siem.SIEMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * DLP Service - Data Loss Prevention Engine
 * Monitors and enforces data protection policies
 * Integrates with PII Scanner and SIEM for comprehensive protection
 * 
 * @version 1.0.0
 * @since Module 7
 */
@Service
public class DLPService {

    private static final Logger logger = LoggerFactory.getLogger(DLPService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final PIIScanner piiScanner;
    private final SIEMService siemService;

    private boolean initialized = false;
    private final List<DLPPolicy> policies = new CopyOnWriteArrayList<>();
    private final Map<String, DLPIncident> activeIncidents = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private volatile boolean monitoring = false;

    // Statistics
    private long totalScanned = 0;
    private long violationsDetected = 0;
    private long blockedActions = 0;

    @Autowired
    public DLPService(DatabaseManager dbManager, AuditService auditService,
            EventBus eventBus, PIIScanner piiScanner, SIEMService siemService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.piiScanner = piiScanner;
        this.siemService = siemService;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing DLP Service...");

        createTables();
        loadPolicies();
        subscribeToEvents();
        startMonitoring();

        initialized = true;
        logger.info("DLP Service initialized with {} policies", policies.size());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            // DLP Policies table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS dlp_policies (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL UNIQUE,
                            description TEXT,
                            enabled INTEGER DEFAULT 1,
                            priority INTEGER DEFAULT 50,
                            protected_data_types TEXT,
                            custom_patterns TEXT,
                            min_match_count INTEGER DEFAULT 1,
                            confidence_threshold REAL DEFAULT 0.7,
                            monitor_endpoint INTEGER DEFAULT 0,
                            monitor_network INTEGER DEFAULT 0,
                            monitor_email INTEGER DEFAULT 0,
                            monitor_print INTEGER DEFAULT 0,
                            monitor_removable_media INTEGER DEFAULT 0,
                            primary_action TEXT NOT NULL,
                            fallback_action TEXT,
                            notify_user INTEGER DEFAULT 0,
                            notify_manager INTEGER DEFAULT 0,
                            notify_dpo INTEGER DEFAULT 0,
                            dpdp_section TEXT,
                            cross_border_restriction INTEGER DEFAULT 0,
                            sensitive_data_protection INTEGER DEFAULT 0,
                            created_by TEXT,
                            approved_by TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP
                        )
                    """);

            // DLP Incidents table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS dlp_incidents (
                            id TEXT PRIMARY KEY,
                            policy_id TEXT NOT NULL,
                            policy_name TEXT NOT NULL,
                            action_taken TEXT NOT NULL,
                            severity TEXT NOT NULL,
                            source_user TEXT,
                            source_system TEXT,
                            source_path TEXT,
                            source_application TEXT,
                            destination_type TEXT,
                            destination_address TEXT,
                            destination_country TEXT,
                            detected_data_types TEXT,
                            match_count INTEGER,
                            confidence_score REAL,
                            data_snippet TEXT,
                            data_size INTEGER,
                            process_name TEXT,
                            file_name TEXT,
                            file_hash TEXT,
                            status TEXT DEFAULT 'OPEN',
                            assigned_to TEXT,
                            detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            resolved_at TIMESTAMP,
                            resolution TEXT,
                            notes TEXT,
                            dpdp_section TEXT,
                            breach_indicator INTEGER DEFAULT 0,
                            notification_required INTEGER DEFAULT 0
                        )
                    """);

            // Indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dlp_incidents_status ON dlp_incidents(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dlp_incidents_policy ON dlp_incidents(policy_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dlp_incidents_severity ON dlp_incidents(severity)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dlp_incidents_user ON dlp_incidents(source_user)");

            logger.info("DLP tables created");

        } catch (SQLException e) {
            logger.error("Failed to create DLP tables", e);
        }
    }

    private void loadPolicies() {
        // Load default policies
        policies.addAll(DLPPolicy.getDefaultPolicies());

        // Persist default policies
        for (DLPPolicy policy : policies) {
            persistPolicy(policy);
        }

        logger.info("Loaded {} DLP policies", policies.size());
    }

    private void subscribeToEvents() {
        // Subscribe to file system events
        eventBus.subscribe("file.*", this::handleFileEvent);

        // Subscribe to network events
        eventBus.subscribe("network.*", this::handleNetworkEvent);
    }

    @SuppressWarnings("unchecked")
    private void handleFileEvent(ComplianceEvent event) {
        String eventType = event.getType();
        Object payload = event.getPayload();
        if (!(payload instanceof Map))
            return;
        Map<String, Object> data = (Map<String, Object>) payload;

        if (data.containsKey("path")) {
            String path = data.get("path").toString();
            String user = data.containsKey("user") ? data.get("user").toString() : "SYSTEM";

            if (eventType.contains("copy") || eventType.contains("export") || eventType.contains("transfer")) {
                evaluateFileTransfer(path, user, "FILE_TRANSFER");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleNetworkEvent(ComplianceEvent event) {
        Object payload = event.getPayload();
        if (!(payload instanceof Map))
            return;
        Map<String, Object> data = (Map<String, Object>) payload;

        if (data.containsKey("content")) {
            String content = data.get("content").toString();
            String user = data.containsKey("user") ? data.get("user").toString() : "SYSTEM";
            String destination = data.containsKey("destination") ? data.get("destination").toString() : "UNKNOWN";

            evaluateNetworkTransfer(content, user, destination);
        }
    }

    private void startMonitoring() {
        scheduler = Executors.newScheduledThreadPool(2);
        monitoring = true;

        // Periodic policy evaluation
        scheduler.scheduleAtFixedRate(this::checkQueuedItems, 0, 1, TimeUnit.SECONDS);

        // Statistics reporting
        scheduler.scheduleAtFixedRate(this::reportStatistics, 1, 1, TimeUnit.MINUTES);

        logger.info("DLP monitoring started");
    }

    private void checkQueuedItems() {
        // In production, this would check a queue of items to scan
    }

    private void reportStatistics() {
        if (violationsDetected > 0 || blockedActions > 0) {
            logger.info("DLP Stats - Scanned: {}, Violations: {}, Blocked: {}",
                    totalScanned, violationsDetected, blockedActions);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // EVALUATION METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Evaluate content for DLP policy violations
     */
    public DLPEvaluationResult evaluate(String content, String user, String destination, String channel) {
        logger.debug("Evaluating content for DLP policies - user: {}, channel: {}", user, channel);
        totalScanned++;

        DLPEvaluationResult result = new DLPEvaluationResult();
        result.setUser(user);
        result.setDestination(destination);
        result.setChannel(channel);

        // Scan for PII
        PIIScanResult piiResult = piiScanner.scanText(content, "dlp-scan:" + channel);
        result.setPiiResult(piiResult);

        if (piiResult.getTotalFindings() == 0) {
            result.setAllowed(true);
            result.setAction(DLPAction.ALLOW);
            return result;
        }

        // Check against policies
        DLPPolicy matchedPolicy = null;
        int highestPriority = -1;

        for (DLPPolicy policy : policies) {
            if (!policy.isEnabled())
                continue;
            if (!isChannelMonitored(policy, channel))
                continue;

            // Check if policy matches detected data types
            boolean matches = false;
            Set<PIIType> matchedTypes = new HashSet<>();

            for (PIIFinding finding : piiResult.getFindings()) {
                if (policy.getProtectedDataTypes().contains(finding.getType())) {
                    matches = true;
                    matchedTypes.add(finding.getType());
                }
            }

            // Check minimum match count
            int typeMatchCount = (int) piiResult.getFindings().stream()
                    .filter(f -> policy.getProtectedDataTypes().contains(f.getType()))
                    .count();

            if (matches && typeMatchCount >= policy.getMinMatchCount()) {
                if (policy.getPriority() > highestPriority) {
                    highestPriority = policy.getPriority();
                    matchedPolicy = policy;
                    result.setMatchedDataTypes(matchedTypes);
                }
            }
        }

        if (matchedPolicy != null) {
            result.setMatchedPolicy(matchedPolicy);
            result.setAction(matchedPolicy.getPrimaryAction());
            result.setAllowed(!matchedPolicy.getPrimaryAction().isPreventive());

            // Create incident
            DLPIncident incident = createIncident(matchedPolicy, piiResult, user, destination, channel);
            result.setIncident(incident);

            violationsDetected++;
            if (matchedPolicy.getPrimaryAction().isPreventive()) {
                blockedActions++;
            }

            // Send to SIEM
            sendToSIEM(incident, matchedPolicy);

            // Audit log
            auditService.log("DLP_VIOLATION", "DLP", user,
                    String.format("Policy: %s, Action: %s, DataTypes: %s",
                            matchedPolicy.getName(), matchedPolicy.getPrimaryAction(),
                            result.getMatchedDataTypes()));
        } else {
            result.setAllowed(true);
            result.setAction(DLPAction.ALLOW);
        }

        return result;
    }

    public DLPEvaluationResult evaluateFileTransfer(String filePath, String user, String destination) {
        try {
            if (Files.exists(Path.of(filePath))) {
                String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
                return evaluate(content, user, destination, "FILE_TRANSFER");
            }
        } catch (IOException e) {
            logger.error("Failed to read file for DLP evaluation: {}", filePath, e);
        }
        return DLPEvaluationResult.allowed();
    }

    public DLPEvaluationResult evaluateNetworkTransfer(String content, String user, String destination) {
        return evaluate(content, user, destination, "NETWORK");
    }

    public DLPEvaluationResult evaluateEmail(String subject, String body, String recipient, String user) {
        String content = subject + "\n" + body;
        return evaluate(content, user, recipient, "EMAIL");
    }

    public DLPEvaluationResult evaluatePrint(String content, String user, String printer) {
        return evaluate(content, user, printer, "PRINT");
    }

    public DLPEvaluationResult evaluateUSB(String filePath, String user, String deviceId) {
        try {
            if (Files.exists(Path.of(filePath))) {
                String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
                return evaluate(content, user, deviceId, "USB");
            }
        } catch (IOException e) {
            logger.error("Failed to read file for USB DLP evaluation: {}", filePath, e);
        }
        return DLPEvaluationResult.allowed();
    }

    private boolean isChannelMonitored(DLPPolicy policy, String channel) {
        return switch (channel.toUpperCase()) {
            case "ENDPOINT", "FILE_TRANSFER" -> policy.isMonitorEndpoint();
            case "NETWORK" -> policy.isMonitorNetwork();
            case "EMAIL" -> policy.isMonitorEmail();
            case "PRINT" -> policy.isMonitorPrint();
            case "USB", "REMOVABLE_MEDIA" -> policy.isMonitorRemovableMedia();
            case "CLIPBOARD" -> policy.isMonitorClipboard();
            case "CLOUD" -> policy.isMonitorCloud();
            default -> true; // Monitor by default
        };
    }

    private DLPIncident createIncident(DLPPolicy policy, PIIScanResult piiResult,
            String user, String destination, String channel) {
        DLPIncident incident = new DLPIncident(policy, user, channel);
        incident.setDestinationType(channel);
        incident.setDestinationAddress(destination);
        incident.setMatchCount(piiResult.getTotalFindings());
        incident.setConfidenceScore(piiResult.getFindings().stream()
                .mapToDouble(PIIFinding::getConfidence)
                .average()
                .orElse(0.0));

        // Set detected types
        Set<PIIType> types = new HashSet<>();
        for (PIIFinding finding : piiResult.getFindings()) {
            types.add(finding.getType());
        }
        incident.setDetectedDataTypes(types);

        // Check if breach indicator
        if (policy.isSensitiveDataProtection() &&
                policy.getPrimaryAction() == DLPAction.BLOCK &&
                piiResult.getCriticalFindings() > 0) {
            incident.setBreachIndicator(true);
            incident.setNotificationRequired(true);
        }

        // Persist incident
        persistIncident(incident);
        activeIncidents.put(incident.getId(), incident);

        // Publish event
        eventBus.publish(new ComplianceEvent("dlp.incident.created",
                Map.of("incidentId", incident.getId(),
                        "policy", policy.getName(),
                        "severity", incident.getSeverity())));

        return incident;
    }

    private void sendToSIEM(DLPIncident incident, DLPPolicy policy) {
        SecurityEvent siemEvent = SecurityEvent.builder()
                .category(EventCategory.DLP_VIOLATION)
                .severity(mapSeverity(incident.getSeverity()))
                .source("DLP", null)
                .user(null, incident.getSourceUser())
                .action("DLP_" + policy.getPrimaryAction().name(), !policy.getPrimaryAction().isPreventive())
                .resource(incident.getSourcePath(), "FILE")
                .message("DLP Policy Violation: " + policy.getName())
                .sensitiveData(incident.isSensitiveData())
                .metadata("policyId", policy.getId())
                .metadata("incidentId", incident.getId())
                .metadata("matchCount", incident.getMatchCount())
                .build();

        siemEvent.setDpdpSection(policy.getDpdpSection());
        siemService.ingestEvent(siemEvent);
    }

    private EventSeverity mapSeverity(String severity) {
        return switch (severity) {
            case "CRITICAL" -> EventSeverity.CRITICAL;
            case "HIGH" -> EventSeverity.HIGH;
            case "MEDIUM" -> EventSeverity.MEDIUM;
            default -> EventSeverity.LOW;
        };
    }

    // ═══════════════════════════════════════════════════════════
    // POLICY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public void addPolicy(DLPPolicy policy) {
        policies.add(policy);
        persistPolicy(policy);
        logger.info("Added DLP policy: {}", policy.getName());
    }

    public void updatePolicy(DLPPolicy policy) {
        policies.removeIf(p -> p.getId().equals(policy.getId()));
        policies.add(policy);
        policy.setUpdatedAt(LocalDateTime.now());
        updatePolicyInDb(policy);
        logger.info("Updated DLP policy: {}", policy.getName());
    }

    public void enablePolicy(String policyId, boolean enabled) {
        for (DLPPolicy policy : policies) {
            if (policy.getId().equals(policyId)) {
                policy.setEnabled(enabled);
                updatePolicyInDb(policy);
                logger.info("{} DLP policy: {}", enabled ? "Enabled" : "Disabled", policy.getName());
                break;
            }
        }
    }

    public List<DLPPolicy> getAllPolicies() {
        return new ArrayList<>(policies);
    }

    public List<DLPPolicy> getEnabledPolicies() {
        return policies.stream().filter(DLPPolicy::isEnabled).toList();
    }

    // ═══════════════════════════════════════════════════════════
    // INCIDENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public List<DLPIncident> getOpenIncidents() {
        List<DLPIncident> incidents = new ArrayList<>();
        String sql = "SELECT * FROM dlp_incidents WHERE status IN ('OPEN', 'INVESTIGATING') ORDER BY detected_at DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                incidents.add(mapIncidentFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to get open incidents", e);
        }

        return incidents;
    }

    public void resolveIncident(String incidentId, String resolution, String resolvedBy) {
        String sql = "UPDATE dlp_incidents SET status = 'RESOLVED', resolution = ?, resolved_at = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, resolution);
            stmt.setString(2, LocalDateTime.now().toString());
            stmt.setString(3, incidentId);
            stmt.executeUpdate();

            activeIncidents.remove(incidentId);

            auditService.log("DLP_INCIDENT_RESOLVED", "DLP", resolvedBy,
                    "Incident " + incidentId + " resolved: " + resolution);

        } catch (SQLException e) {
            logger.error("Failed to resolve incident: {}", incidentId, e);
        }
    }

    public void markFalsePositive(String incidentId, String markedBy, String notes) {
        String sql = "UPDATE dlp_incidents SET status = 'FALSE_POSITIVE', resolution = ?, notes = ?, resolved_at = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "Marked as false positive");
            stmt.setString(2, notes);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.setString(4, incidentId);
            stmt.executeUpdate();

            activeIncidents.remove(incidentId);

            auditService.log("DLP_FALSE_POSITIVE", "DLP", markedBy,
                    "Incident " + incidentId + " marked as false positive");

        } catch (SQLException e) {
            logger.error("Failed to mark incident as false positive: {}", incidentId, e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public DLPStatistics getStatistics() {
        DLPStatistics stats = new DLPStatistics();
        stats.setTotalScanned(totalScanned);
        stats.setViolationsDetected(violationsDetected);
        stats.setBlockedActions(blockedActions);
        stats.setActivePolicies((int) policies.stream().filter(DLPPolicy::isEnabled).count());

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM dlp_incidents WHERE status = 'OPEN'");
            if (rs.next())
                stats.setOpenIncidents(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM dlp_incidents WHERE severity = 'CRITICAL' AND status != 'RESOLVED'");
            if (rs.next())
                stats.setCriticalIncidents(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM dlp_incidents WHERE DATE(detected_at) = DATE('now')");
            if (rs.next())
                stats.setIncidentsToday(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get DLP statistics", e);
        }

        return stats;
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    private void persistPolicy(DLPPolicy policy) {
        String sql = """
                    INSERT OR REPLACE INTO dlp_policies
                    (id, name, description, enabled, priority, protected_data_types, min_match_count,
                     confidence_threshold, monitor_endpoint, monitor_network, monitor_email, monitor_print,
                     monitor_removable_media, primary_action, notify_user, notify_manager, notify_dpo,
                     dpdp_section, cross_border_restriction, sensitive_data_protection, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, policy.getId());
            stmt.setString(2, policy.getName());
            stmt.setString(3, policy.getDescription());
            stmt.setInt(4, policy.isEnabled() ? 1 : 0);
            stmt.setInt(5, policy.getPriority());
            stmt.setString(6, serializeDataTypes(policy.getProtectedDataTypes()));
            stmt.setInt(7, policy.getMinMatchCount());
            stmt.setDouble(8, policy.getConfidenceThreshold());
            stmt.setInt(9, policy.isMonitorEndpoint() ? 1 : 0);
            stmt.setInt(10, policy.isMonitorNetwork() ? 1 : 0);
            stmt.setInt(11, policy.isMonitorEmail() ? 1 : 0);
            stmt.setInt(12, policy.isMonitorPrint() ? 1 : 0);
            stmt.setInt(13, policy.isMonitorRemovableMedia() ? 1 : 0);
            stmt.setString(14, policy.getPrimaryAction().name());
            stmt.setInt(15, policy.isNotifyUser() ? 1 : 0);
            stmt.setInt(16, policy.isNotifyManager() ? 1 : 0);
            stmt.setInt(17, policy.isNotifyDPO() ? 1 : 0);
            stmt.setString(18, policy.getDpdpSection());
            stmt.setInt(19, policy.isCrossBorderRestriction() ? 1 : 0);
            stmt.setInt(20, policy.isSensitiveDataProtection() ? 1 : 0);
            stmt.setString(21, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist DLP policy", e);
        }
    }

    private void updatePolicyInDb(DLPPolicy policy) {
        persistPolicy(policy); // Uses INSERT OR REPLACE
    }

    private void persistIncident(DLPIncident incident) {
        String sql = """
                    INSERT INTO dlp_incidents
                    (id, policy_id, policy_name, action_taken, severity, source_user, source_system,
                     source_path, destination_type, destination_address, destination_country,
                     detected_data_types, match_count, confidence_score, data_snippet, status,
                     dpdp_section, breach_indicator, notification_required, detected_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, incident.getId());
            stmt.setString(2, incident.getPolicyId());
            stmt.setString(3, incident.getPolicyName());
            stmt.setString(4, incident.getActionTaken().name());
            stmt.setString(5, incident.getSeverity());
            stmt.setString(6, incident.getSourceUser());
            stmt.setString(7, incident.getSourceSystem());
            stmt.setString(8, incident.getSourcePath());
            stmt.setString(9, incident.getDestinationType());
            stmt.setString(10, incident.getDestinationAddress());
            stmt.setString(11, incident.getDestinationCountry());
            stmt.setString(12, serializeDataTypes(incident.getDetectedDataTypes()));
            stmt.setInt(13, incident.getMatchCount());
            stmt.setDouble(14, incident.getConfidenceScore());
            stmt.setString(15, incident.getDataSnippet());
            stmt.setString(16, incident.getStatus());
            stmt.setString(17, incident.getDpdpSection());
            stmt.setInt(18, incident.isBreachIndicator() ? 1 : 0);
            stmt.setInt(19, incident.isNotificationRequired() ? 1 : 0);
            stmt.setString(20, incident.getDetectedAt().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist DLP incident", e);
        }
    }

    private String serializeDataTypes(Set<PIIType> types) {
        return types.stream().map(PIIType::name).reduce((a, b) -> a + "," + b).orElse("");
    }

    private DLPIncident mapIncidentFromResultSet(ResultSet rs) throws SQLException {
        DLPIncident incident = new DLPIncident();
        incident.setId(rs.getString("id"));
        incident.setPolicyId(rs.getString("policy_id"));
        incident.setPolicyName(rs.getString("policy_name"));
        incident.setActionTaken(DLPAction.valueOf(rs.getString("action_taken")));
        incident.setSeverity(rs.getString("severity"));
        incident.setSourceUser(rs.getString("source_user"));
        incident.setSourcePath(rs.getString("source_path"));
        incident.setDestinationType(rs.getString("destination_type"));
        incident.setStatus(rs.getString("status"));
        return incident;
    }

    public void shutdown() {
        monitoring = false;
        if (scheduler != null)
            scheduler.shutdown();
        logger.info("DLP Service shutdown");
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class DLPStatistics {
        private long totalScanned;
        private long violationsDetected;
        private long blockedActions;
        private int activePolicies;
        private int openIncidents;
        private int criticalIncidents;
        private int incidentsToday;

        public long getTotalScanned() {
            return totalScanned;
        }

        public void setTotalScanned(long v) {
            this.totalScanned = v;
        }

        public long getViolationsDetected() {
            return violationsDetected;
        }

        public void setViolationsDetected(long v) {
            this.violationsDetected = v;
        }

        public long getBlockedActions() {
            return blockedActions;
        }

        public void setBlockedActions(long v) {
            this.blockedActions = v;
        }

        public int getActivePolicies() {
            return activePolicies;
        }

        public void setActivePolicies(int v) {
            this.activePolicies = v;
        }

        public int getOpenIncidents() {
            return openIncidents;
        }

        public void setOpenIncidents(int v) {
            this.openIncidents = v;
        }

        public int getCriticalIncidents() {
            return criticalIncidents;
        }

        public void setCriticalIncidents(int v) {
            this.criticalIncidents = v;
        }

        public int getIncidentsToday() {
            return incidentsToday;
        }

        public void setIncidentsToday(int v) {
            this.incidentsToday = v;
        }
    }
}
