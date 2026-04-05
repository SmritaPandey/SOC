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
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * SIEM Service - Security Information and Event Management
 * Core engine for event collection, correlation, and SOAR orchestration
 * Compliant with DPDP Act 2023 breach detection and notification requirements
 * 
 * @version 1.0.0
 * @since Module 6
 */
@Service
public class SIEMService {

    private static final Logger logger = LoggerFactory.getLogger(SIEMService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;

    private boolean initialized = false;
    private final List<CorrelationRule> correlationRules = new ArrayList<>();
    private final Map<String, SOARPlaybook> playbooks = new LinkedHashMap<>();
    private final ConcurrentLinkedQueue<SecurityEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, List<SecurityEvent>> correlationBuffer = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;
    private ExecutorService eventProcessor;
    private volatile boolean processing = true;

    // Statistics
    private long totalEventsProcessed = 0;
    private long alertsGenerated = 0;
    private long playbooksExecuted = 0;

    @Autowired
    public SIEMService(DatabaseManager dbManager, AuditService auditService, EventBus eventBus) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing SIEM Service...");

        createTables();
        loadCorrelationRules();
        loadPlaybooks();
        startEventProcessing();
        subscribeToEvents();

        initialized = true;
        logger.info("SIEM Service initialized with {} rules and {} playbooks",
                correlationRules.size(), playbooks.size());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            // Security Events table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS security_events (
                            id TEXT PRIMARY KEY,
                            timestamp TIMESTAMP NOT NULL,
                            category TEXT NOT NULL,
                            severity TEXT NOT NULL,
                            source TEXT,
                            source_ip TEXT,
                            destination_ip TEXT,
                            user_id TEXT,
                            user_name TEXT,
                            action TEXT,
                            resource TEXT,
                            resource_type TEXT,
                            success INTEGER,
                            message TEXT,
                            raw_log TEXT,
                            data_principal_id TEXT,
                            data_fiduciary_id TEXT,
                            processing_purpose TEXT,
                            personal_data_involved INTEGER DEFAULT 0,
                            sensitive_data_involved INTEGER DEFAULT 0,
                            dpdp_section TEXT,
                            correlation_id TEXT,
                            parent_event_id TEXT,
                            session_id TEXT,
                            status TEXT DEFAULT 'NEW',
                            assigned_to TEXT,
                            acknowledged_at TIMESTAMP,
                            resolved_at TIMESTAMP,
                            resolution TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // SIEM Alerts table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS siem_alerts (
                            id TEXT PRIMARY KEY,
                            rule_id TEXT NOT NULL,
                            rule_name TEXT NOT NULL,
                            severity TEXT NOT NULL,
                            category TEXT,
                            title TEXT NOT NULL,
                            description TEXT,
                            source_events TEXT,
                            event_count INTEGER,
                            dpdp_section TEXT,
                            requires_notification INTEGER DEFAULT 0,
                            notification_deadline_hours INTEGER,
                            playbook_id TEXT,
                            playbook_status TEXT,
                            status TEXT DEFAULT 'NEW',
                            assigned_to TEXT,
                            acknowledged_at TIMESTAMP,
                            resolved_at TIMESTAMP,
                            resolution TEXT,
                            false_positive INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // SOAR Executions table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS soar_executions (
                            id TEXT PRIMARY KEY,
                            playbook_id TEXT NOT NULL,
                            playbook_name TEXT NOT NULL,
                            alert_id TEXT,
                            trigger_event_id TEXT,
                            status TEXT DEFAULT 'PENDING',
                            current_step INTEGER DEFAULT 0,
                            total_steps INTEGER,
                            step_results TEXT,
                            started_at TIMESTAMP,
                            completed_at TIMESTAMP,
                            executed_by TEXT,
                            error TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Correlation buffer table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS correlation_buffer (
                            correlation_key TEXT NOT NULL,
                            event_id TEXT NOT NULL,
                            rule_id TEXT NOT NULL,
                            buffered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            expires_at TIMESTAMP,
                            PRIMARY KEY (correlation_key, event_id)
                        )
                    """);

            // Indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_timestamp ON security_events(timestamp)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_category ON security_events(category)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_severity ON security_events(severity)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_user ON security_events(user_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_events_status ON security_events(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_alerts_status ON siem_alerts(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_alerts_severity ON siem_alerts(severity)");

            logger.info("SIEM tables created");

        } catch (SQLException e) {
            logger.error("Failed to create SIEM tables", e);
        }
    }

    private void loadCorrelationRules() {
        correlationRules.addAll(CorrelationRule.getDefaultRules());
        logger.info("Loaded {} correlation rules", correlationRules.size());
    }

    private void loadPlaybooks() {
        playbooks.putAll(SOARPlaybook.getDefaultPlaybooks());
        logger.info("Loaded {} SOAR playbooks", playbooks.size());
    }

    private void subscribeToEvents() {
        // Subscribe to all compliance events
        eventBus.subscribe("*", this::handleComplianceEvent);
    }

    private void handleComplianceEvent(ComplianceEvent event) {
        SecurityEvent secEvent = convertToSecurityEvent(event);
        if (secEvent != null) {
            ingestEvent(secEvent);
        }
    }

    private SecurityEvent convertToSecurityEvent(ComplianceEvent event) {
        EventCategory category = mapEventCategory(event.getType());
        if (category == null)
            return null;

        return SecurityEvent.builder()
                .category(category)
                .severity(EventSeverity.fromDPDPImpact(event.getType()))
                .source("QS-DPDP", null)
                .message(event.getType())
                .metadata("event_data", event.getPayload())
                .build();
    }

    private EventCategory mapEventCategory(String eventType) {
        if (eventType.contains("consent"))
            return EventCategory.CONSENT_COLLECTED;
        if (eventType.contains("breach"))
            return EventCategory.BREACH_SUSPECTED;
        if (eventType.contains("rights"))
            return EventCategory.RIGHTS_REQUEST;
        if (eventType.contains("policy"))
            return EventCategory.POLICY_CREATED;
        if (eventType.contains("auth"))
            return EventCategory.AUTH_SUCCESS;
        if (eventType.contains("data"))
            return EventCategory.DATA_ACCESS;
        return null;
    }

    private void startEventProcessing() {
        scheduler = Executors.newScheduledThreadPool(2);
        eventProcessor = Executors.newFixedThreadPool(4);

        // Process event queue
        scheduler.scheduleAtFixedRate(this::processEventQueue, 0, 100, TimeUnit.MILLISECONDS);

        // Run correlation analysis
        scheduler.scheduleAtFixedRate(this::runCorrelationAnalysis, 5, 5, TimeUnit.SECONDS);

        // Clean expired correlation buffers
        scheduler.scheduleAtFixedRate(this::cleanExpiredBuffers, 1, 1, TimeUnit.MINUTES);

        logger.info("Event processing started");
    }

    // ═══════════════════════════════════════════════════════════
    // EVENT INGESTION
    // ═══════════════════════════════════════════════════════════

    public void ingestEvent(SecurityEvent event) {
        eventQueue.offer(event);
    }

    public void ingestRawLog(String source, String rawLog) {
        SecurityEvent event = parseRawLog(source, rawLog);
        if (event != null) {
            ingestEvent(event);
        }
    }

    private SecurityEvent parseRawLog(String source, String rawLog) {
        // Basic log parsing - detect patterns
        SecurityEvent.Builder builder = SecurityEvent.builder()
                .source(source, null)
                .rawLog(rawLog);

        String lower = rawLog.toLowerCase();

        // Detect category from log content
        if (lower.contains("login failed") || lower.contains("authentication failed")) {
            builder.category(EventCategory.AUTH_FAILURE)
                    .severity(EventSeverity.MEDIUM)
                    .message("Authentication failure detected");
        } else if (lower.contains("login success") || lower.contains("authenticated")) {
            builder.category(EventCategory.AUTH_SUCCESS)
                    .severity(EventSeverity.LOW)
                    .message("Successful authentication");
        } else if (lower.contains("access denied") || lower.contains("permission denied")) {
            builder.category(EventCategory.ACCESS_DENIED)
                    .severity(EventSeverity.MEDIUM)
                    .message("Access denied");
        } else if (lower.contains("personal data") || lower.contains("pii")) {
            builder.category(EventCategory.DATA_ACCESS)
                    .severity(EventSeverity.MEDIUM)
                    .sensitiveData(lower.contains("sensitive"))
                    .message("Personal data operation detected");
        } else if (lower.contains("malware") || lower.contains("virus")) {
            builder.category(EventCategory.MALWARE_DETECTED)
                    .severity(EventSeverity.CRITICAL)
                    .message("Malware detected");
        } else if (lower.contains("breach") || lower.contains("data leak")) {
            builder.category(EventCategory.BREACH_SUSPECTED)
                    .severity(EventSeverity.CRITICAL)
                    .message("Potential data breach detected");
        } else {
            builder.category(EventCategory.APP_WARNING)
                    .severity(EventSeverity.INFO)
                    .message(rawLog.length() > 100 ? rawLog.substring(0, 100) : rawLog);
        }

        return builder.build();
    }

    private void processEventQueue() {
        if (!processing)
            return;

        SecurityEvent event;
        int processedCount = 0;

        while ((event = eventQueue.poll()) != null && processedCount < 100) {
            try {
                processEvent(event);
                processedCount++;
                totalEventsProcessed++;
            } catch (Exception e) {
                logger.error("Error processing event: {}", event.getId(), e);
            }
        }
    }

    private void processEvent(SecurityEvent event) {
        // Persist event
        persistEvent(event);

        // Check for immediate alerts
        checkImmediateAlerts(event);

        // Add to correlation buffer
        addToCorrelationBuffer(event);
    }

    private void checkImmediateAlerts(SecurityEvent event) {
        if (event.getSeverity() == EventSeverity.CRITICAL) {
            createAlert(event, null, "Critical Security Event");
        }

        if (event.requiresDPBINotification()) {
            createDPDPAlert(event);
        }
    }

    private void addToCorrelationBuffer(SecurityEvent event) {
        for (CorrelationRule rule : correlationRules) {
            if (rule.isEnabled() && rule.matches(event)) {
                String correlationKey = buildCorrelationKey(rule, event);
                correlationBuffer.computeIfAbsent(correlationKey, k -> new CopyOnWriteArrayList<>())
                        .add(event);
            }
        }
    }

    private String buildCorrelationKey(CorrelationRule rule, SecurityEvent event) {
        StringBuilder key = new StringBuilder(rule.getName());

        if (rule.isSameSource() && event.getSourceIP() != null) {
            key.append(":").append(event.getSourceIP());
        }
        if (rule.isSameUser() && event.getUserId() != null) {
            key.append(":").append(event.getUserId());
        }
        if (rule.isSameSession() && event.getSessionId() != null) {
            key.append(":").append(event.getSessionId());
        }

        return key.toString();
    }

    // ═══════════════════════════════════════════════════════════
    // CORRELATION ANALYSIS
    // ═══════════════════════════════════════════════════════════

    private void runCorrelationAnalysis() {
        if (!processing)
            return;

        for (CorrelationRule rule : correlationRules) {
            if (!rule.isEnabled())
                continue;

            for (Map.Entry<String, List<SecurityEvent>> entry : correlationBuffer.entrySet()) {
                String key = entry.getKey();
                if (!key.startsWith(rule.getName()))
                    continue;

                List<SecurityEvent> events = entry.getValue();
                LocalDateTime windowStart = LocalDateTime.now()
                        .minus(rule.getTimeWindowSeconds(), ChronoUnit.SECONDS);

                // Filter events within time window
                List<SecurityEvent> windowEvents = events.stream()
                        .filter(e -> e.getTimestamp().isAfter(windowStart))
                        .toList();

                // Check if threshold is met
                if (windowEvents.size() >= rule.getEventThreshold()) {
                    triggerCorrelationAlert(rule, windowEvents);

                    // Clear processed events from buffer
                    events.clear();
                }
            }
        }
    }

    private void triggerCorrelationAlert(CorrelationRule rule, List<SecurityEvent> events) {
        logger.info("Correlation rule triggered: {} with {} events", rule.getName(), events.size());

        SIEMAlert alert = new SIEMAlert();
        alert.setRuleId(rule.getId());
        alert.setRuleName(rule.getName());
        alert.setSeverity(rule.getOutputSeverity());
        alert.setCategory(rule.getOutputCategory());
        alert.setTitle("Correlation Alert: " + rule.getDescription());
        alert.setDescription(buildAlertDescription(rule, events));
        alert.setSourceEvents(events.stream().map(SecurityEvent::getId).toList());
        alert.setEventCount(events.size());
        alert.setDpdpSection(rule.getDpdpSectionTarget());

        if (rule.getDpdpSectionTarget() != null) {
            alert.setRequiresNotification(true);
            alert.setNotificationDeadlineHours(rule.getOutputSeverity().getDPBINotificationHours());
        }

        persistAlert(alert);
        alertsGenerated++;

        // Execute SOAR playbook if configured
        if (rule.getSoarPlaybook() != null) {
            SOARPlaybook playbook = playbooks.get(rule.getSoarPlaybook());
            if (playbook != null && playbook.isEnabled()) {
                if (rule.isAutoExecutePlaybook()) {
                    executePlaybook(playbook, alert);
                } else {
                    alert.setPlaybookId(playbook.getId());
                    alert.setPlaybookStatus("PENDING_APPROVAL");
                }
            }
        }

        // Publish event
        eventBus.publish(new ComplianceEvent("siem.alert.created",
                Map.of("alertId", alert.getId(), "rule", rule.getName(), "severity",
                        rule.getOutputSeverity().getName())));
    }

    private String buildAlertDescription(CorrelationRule rule, List<SecurityEvent> events) {
        StringBuilder desc = new StringBuilder();
        desc.append("Detected ").append(events.size()).append(" events matching rule: ").append(rule.getDescription());
        desc.append("\n\nEvent Summary:");

        Map<EventCategory, Long> categoryCounts = events.stream()
                .collect(Collectors.groupingBy(SecurityEvent::getCategory, Collectors.counting()));

        for (Map.Entry<EventCategory, Long> entry : categoryCounts.entrySet()) {
            desc.append("\n- ").append(entry.getKey()).append(": ").append(entry.getValue());
        }

        if (rule.getDpdpSectionTarget() != null) {
            desc.append("\n\nDPDP Compliance Impact: ").append(rule.getDpdpSectionTarget());
        }

        return desc.toString();
    }

    // ═══════════════════════════════════════════════════════════
    // SOAR EXECUTION
    // ═══════════════════════════════════════════════════════════

    public void executePlaybook(SOARPlaybook playbook, SIEMAlert alert) {
        logger.info("Executing SOAR playbook: {} for alert: {}", playbook.getName(), alert.getId());

        SOARExecution execution = new SOARExecution();
        execution.setPlaybookId(playbook.getId());
        execution.setPlaybookName(playbook.getName());
        execution.setAlertId(alert.getId());
        execution.setTotalSteps(playbook.getSteps().size());
        execution.setStartedAt(LocalDateTime.now());
        execution.setStatus("RUNNING");

        List<String> stepResults = new ArrayList<>();

        try {
            for (SOARPlaybook.PlaybookStep step : playbook.getSteps()) {
                execution.setCurrentStep(step.getOrder());
                step.setStatus("RUNNING");

                boolean success = executePlaybookStep(step, alert);

                if (success) {
                    step.setStatus("COMPLETED");
                    stepResults.add("Step " + step.getOrder() + " (" + step.getName() + "): SUCCESS");
                } else {
                    step.setStatus("FAILED");
                    stepResults.add("Step " + step.getOrder() + " (" + step.getName() + "): FAILED");

                    if (step.isRequired()) {
                        throw new RuntimeException("Required step failed: " + step.getName());
                    }
                }
            }

            execution.setStatus("COMPLETED");
            execution.setCompletedAt(LocalDateTime.now());
            alert.setPlaybookStatus("COMPLETED");

        } catch (Exception e) {
            execution.setStatus("FAILED");
            execution.setError(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            alert.setPlaybookStatus("FAILED");
            logger.error("Playbook execution failed: {}", playbook.getName(), e);
        }

        execution.setStepResults(String.join("\n", stepResults));
        persistExecution(execution);
        playbooksExecuted++;

        auditService.log("SOAR_EXECUTION", "SIEM", "SYSTEM",
                String.format("Playbook %s executed: %s", playbook.getName(), execution.getStatus()));
    }

    private boolean executePlaybookStep(SOARPlaybook.PlaybookStep step, SIEMAlert alert) {
        logger.debug("Executing step: {} - {}", step.getOrder(), step.getAction());

        return switch (step.getAction()) {
            case "LOG" -> {
                logger.info("SOAR Step: {}", step.getDescription());
                auditService.log("SOAR_STEP", "SIEM", "SYSTEM", step.getDescription());
                yield true;
            }
            case "NOTIFY", "NOTIFY_DPO", "NOTIFY_SECURITY", "NOTIFY_SUPERVISOR", "NOTIFY_LEGAL" -> {
                // In production, this would send actual notifications
                logger.info("SOAR Notification: {}", step.getDescription());
                yield true;
            }
            case "BLOCK_IP", "BLOCK_TRANSFER", "REVOKE_SESSION", "REVOKE_SESSIONS" -> {
                logger.info("SOAR Block Action: {}", step.getDescription());
                yield true;
            }
            case "SUSPEND_USER" -> {
                logger.info("SOAR Suspend User: {}", step.getDescription());
                yield true;
            }
            case "PRESERVE_EVIDENCE" -> {
                logger.info("SOAR Evidence Preservation: {}", step.getDescription());
                yield true;
            }
            case "CREATE_TICKET", "CREATE_INCIDENT", "CREATE_BREACH_RECORD",
                    "CREATE_COMPLIANCE_REVIEW", "CREATE_REMEDIATION" -> {
                logger.info("SOAR Create Record: {}", step.getDescription());
                yield true;
            }
            case "PAUSE_PROCESSING", "STOP_PROCESSING" -> {
                logger.info("SOAR Processing Control: {}", step.getDescription());
                yield true;
            }
            default -> {
                logger.info("SOAR Generic Action: {} - {}", step.getAction(), step.getDescription());
                yield true;
            }
        };
    }

    private void cleanExpiredBuffers() {
        LocalDateTime now = LocalDateTime.now();

        for (CorrelationRule rule : correlationRules) {
            LocalDateTime expiry = now.minus(rule.getTimeWindowSeconds() * 2L, ChronoUnit.SECONDS);

            for (Map.Entry<String, List<SecurityEvent>> entry : correlationBuffer.entrySet()) {
                if (entry.getKey().startsWith(rule.getName())) {
                    entry.getValue().removeIf(e -> e.getTimestamp().isBefore(expiry));
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ALERT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    private void createAlert(SecurityEvent event, CorrelationRule rule, String title) {
        SIEMAlert alert = new SIEMAlert();
        alert.setRuleId(rule != null ? rule.getId() : "IMMEDIATE");
        alert.setRuleName(rule != null ? rule.getName() : "Immediate Alert");
        alert.setSeverity(event.getSeverity());
        alert.setCategory(event.getCategory());
        alert.setTitle(title);
        alert.setDescription(event.getMessage());
        alert.setSourceEvents(List.of(event.getId()));
        alert.setEventCount(1);
        alert.setDpdpSection(event.getDpdpSection());

        persistAlert(alert);
        alertsGenerated++;
    }

    private void createDPDPAlert(SecurityEvent event) {
        SIEMAlert alert = new SIEMAlert();
        alert.setRuleId("DPDP_BREACH");
        alert.setRuleName("DPDP Breach Notification");
        alert.setSeverity(EventSeverity.CRITICAL);
        alert.setCategory(event.getCategory());
        alert.setTitle("DPDP Breach Notification Required");
        alert.setDescription("Event requires DPBI notification under DPDP Act 2023");
        alert.setSourceEvents(List.of(event.getId()));
        alert.setEventCount(1);
        alert.setDpdpSection(event.getDpdpSection());
        alert.setRequiresNotification(true);
        alert.setNotificationDeadlineHours(event.getNotificationDeadlineHours());

        persistAlert(alert);
        alertsGenerated++;

        eventBus.publish(new ComplianceEvent("siem.dpdp.notification.required",
                Map.of("alertId", alert.getId(), "deadline", event.getNotificationDeadlineHours())));
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    private void persistEvent(SecurityEvent event) {
        String sql = """
                    INSERT INTO security_events (id, timestamp, category, severity, source, source_ip,
                        destination_ip, user_id, user_name, action, resource, resource_type, success,
                        message, raw_log, data_principal_id, data_fiduciary_id, processing_purpose,
                        personal_data_involved, sensitive_data_involved, dpdp_section, correlation_id,
                        parent_event_id, session_id, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, event.getId());
            stmt.setString(2, event.getTimestamp().toString());
            stmt.setString(3, event.getCategory().name());
            stmt.setString(4, event.getSeverity().name());
            stmt.setString(5, event.getSource());
            stmt.setString(6, event.getSourceIP());
            stmt.setString(7, event.getDestinationIP());
            stmt.setString(8, event.getUserId());
            stmt.setString(9, event.getUserName());
            stmt.setString(10, event.getAction());
            stmt.setString(11, event.getResource());
            stmt.setString(12, event.getResourceType());
            stmt.setInt(13, event.isSuccess() ? 1 : 0);
            stmt.setString(14, event.getMessage());
            stmt.setString(15, event.getRawLog());
            stmt.setString(16, event.getDataPrincipalId());
            stmt.setString(17, event.getDataFiduciaryId());
            stmt.setString(18, event.getProcessingPurpose());
            stmt.setInt(19, event.isPersonalDataInvolved() ? 1 : 0);
            stmt.setInt(20, event.isSensitiveDataInvolved() ? 1 : 0);
            stmt.setString(21, event.getDpdpSection());
            stmt.setString(22, event.getCorrelationId());
            stmt.setString(23, event.getParentEventId());
            stmt.setString(24, event.getSessionId());
            stmt.setString(25, event.getStatus());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist security event", e);
        }
    }

    private void persistAlert(SIEMAlert alert) {
        String sql = """
                    INSERT INTO siem_alerts (id, rule_id, rule_name, severity, category, title, description,
                        source_events, event_count, dpdp_section, requires_notification, notification_deadline_hours,
                        playbook_id, playbook_status, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, alert.getId());
            stmt.setString(2, alert.getRuleId());
            stmt.setString(3, alert.getRuleName());
            stmt.setString(4, alert.getSeverity().name());
            stmt.setString(5, alert.getCategory() != null ? alert.getCategory().name() : null);
            stmt.setString(6, alert.getTitle());
            stmt.setString(7, alert.getDescription());
            stmt.setString(8, String.join(",", alert.getSourceEvents()));
            stmt.setInt(9, alert.getEventCount());
            stmt.setString(10, alert.getDpdpSection());
            stmt.setInt(11, alert.isRequiresNotification() ? 1 : 0);
            stmt.setInt(12, alert.getNotificationDeadlineHours());
            stmt.setString(13, alert.getPlaybookId());
            stmt.setString(14, alert.getPlaybookStatus());
            stmt.setString(15, alert.getStatus());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist SIEM alert", e);
        }
    }

    private void persistExecution(SOARExecution execution) {
        String sql = """
                    INSERT INTO soar_executions (id, playbook_id, playbook_name, alert_id, trigger_event_id,
                        status, current_step, total_steps, step_results, started_at, completed_at, executed_by, error)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, execution.getId());
            stmt.setString(2, execution.getPlaybookId());
            stmt.setString(3, execution.getPlaybookName());
            stmt.setString(4, execution.getAlertId());
            stmt.setString(5, execution.getTriggerEventId());
            stmt.setString(6, execution.getStatus());
            stmt.setInt(7, execution.getCurrentStep());
            stmt.setInt(8, execution.getTotalSteps());
            stmt.setString(9, execution.getStepResults());
            stmt.setString(10, execution.getStartedAt() != null ? execution.getStartedAt().toString() : null);
            stmt.setString(11, execution.getCompletedAt() != null ? execution.getCompletedAt().toString() : null);
            stmt.setString(12, execution.getExecutedBy());
            stmt.setString(13, execution.getError());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist SOAR execution", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════

    public List<SecurityEvent> getRecentEvents(int limit) {
        List<SecurityEvent> events = new ArrayList<>();
        String sql = "SELECT * FROM security_events ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                SecurityEvent event = mapEventFromResultSet(rs);
                events.add(event);
            }
        } catch (SQLException e) {
            logger.error("Failed to get recent events", e);
        }

        return events;
    }

    public List<SIEMAlert> getOpenAlerts() {
        List<SIEMAlert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM siem_alerts WHERE status IN ('NEW', 'ACKNOWLEDGED') ORDER BY created_at DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                SIEMAlert alert = mapAlertFromResultSet(rs);
                alerts.add(alert);
            }
        } catch (SQLException e) {
            logger.error("Failed to get open alerts", e);
        }

        return alerts;
    }

    public SIEMStatistics getStatistics() {
        SIEMStatistics stats = new SIEMStatistics();
        stats.setTotalEventsProcessed(totalEventsProcessed);
        stats.setAlertsGenerated(alertsGenerated);
        stats.setPlaybooksExecuted(playbooksExecuted);

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt
                    .executeQuery("SELECT COUNT(*) FROM security_events WHERE DATE(timestamp) = DATE('now')");
            if (rs.next())
                stats.setEventsToday(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM siem_alerts WHERE status = 'NEW'");
            if (rs.next())
                stats.setOpenAlerts(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM siem_alerts WHERE severity = 'CRITICAL' AND status != 'RESOLVED'");
            if (rs.next())
                stats.setCriticalAlerts(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM siem_alerts WHERE requires_notification = 1 AND status != 'RESOLVED'");
            if (rs.next())
                stats.setPendingNotifications(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get SIEM statistics", e);
        }

        return stats;
    }

    private SecurityEvent mapEventFromResultSet(ResultSet rs) throws SQLException {
        SecurityEvent event = new SecurityEvent();
        event.setId(rs.getString("id"));
        event.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
        event.setCategory(EventCategory.valueOf(rs.getString("category")));
        event.setSeverity(EventSeverity.valueOf(rs.getString("severity")));
        event.setSource(rs.getString("source"));
        event.setSourceIP(rs.getString("source_ip"));
        event.setUserId(rs.getString("user_id"));
        event.setUserName(rs.getString("user_name"));
        event.setMessage(rs.getString("message"));
        event.setStatus(rs.getString("status"));
        return event;
    }

    private SIEMAlert mapAlertFromResultSet(ResultSet rs) throws SQLException {
        SIEMAlert alert = new SIEMAlert();
        alert.setId(rs.getString("id"));
        alert.setRuleId(rs.getString("rule_id"));
        alert.setRuleName(rs.getString("rule_name"));
        alert.setSeverity(EventSeverity.valueOf(rs.getString("severity")));
        String category = rs.getString("category");
        if (category != null)
            alert.setCategory(EventCategory.valueOf(category));
        alert.setTitle(rs.getString("title"));
        alert.setDescription(rs.getString("description"));
        alert.setStatus(rs.getString("status"));
        return alert;
    }

    public void shutdown() {
        processing = false;
        if (scheduler != null)
            scheduler.shutdown();
        if (eventProcessor != null)
            eventProcessor.shutdown();
        logger.info("SIEM Service shutdown");
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class SIEMStatistics {
        private long totalEventsProcessed;
        private long alertsGenerated;
        private long playbooksExecuted;
        private int eventsToday;
        private int openAlerts;
        private int criticalAlerts;
        private int pendingNotifications;

        public long getTotalEventsProcessed() {
            return totalEventsProcessed;
        }

        public void setTotalEventsProcessed(long v) {
            this.totalEventsProcessed = v;
        }

        public long getAlertsGenerated() {
            return alertsGenerated;
        }

        public void setAlertsGenerated(long v) {
            this.alertsGenerated = v;
        }

        public long getPlaybooksExecuted() {
            return playbooksExecuted;
        }

        public void setPlaybooksExecuted(long v) {
            this.playbooksExecuted = v;
        }

        public int getEventsToday() {
            return eventsToday;
        }

        public void setEventsToday(int v) {
            this.eventsToday = v;
        }

        public int getOpenAlerts() {
            return openAlerts;
        }

        public void setOpenAlerts(int v) {
            this.openAlerts = v;
        }

        public int getCriticalAlerts() {
            return criticalAlerts;
        }

        public void setCriticalAlerts(int v) {
            this.criticalAlerts = v;
        }

        public int getPendingNotifications() {
            return pendingNotifications;
        }

        public void setPendingNotifications(int v) {
            this.pendingNotifications = v;
        }
    }
}
