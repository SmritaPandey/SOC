package com.qsdpdp.breach;

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

/**
 * Breach Management Service
 * Handles breach detection, containment, notification, and resolution
 * DPDP Act 2023 compliant with mandatory DPBI/CERT-IN notifications
 * 
 * @version 1.0.0
 * @since Phase 2
 */
@Service
public class BreachService {

    private static final Logger logger = LoggerFactory.getLogger(BreachService.class);

    // DPDP Act notification requirements
    private static final int DPBI_NOTIFICATION_HOURS = 72;
    private static final int CERTIN_NOTIFICATION_HOURS = 6;

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;

    private boolean initialized = false;
    private int breachCounter = 0;

    @Autowired
    public BreachService(DatabaseManager dbManager, AuditService auditService, EventBus eventBus) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing Breach Service...");

        // Load breach counter
        loadBreachCounter();

        // Subscribe to events
        eventBus.subscribe("breach.*", this::handleBreachEvent);
        eventBus.subscribe("security.incident", e -> reportBreach(createBreachFromSecurityEvent(e)));

        initialized = true;
        logger.info("Breach Service initialized");
    }

    private void loadBreachCounter() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM breaches");
            if (rs.next()) {
                breachCounter = rs.getInt(1);
            }
        } catch (Exception e) {
            logger.warn("Could not load breach counter", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // BREACH REPORTING
    // ═══════════════════════════════════════════════════════════

    /**
     * Report a new breach incident
     */
    public Breach reportBreach(BreachRequest request) {
        logger.info("Reporting breach: {}", request.getTitle());

        String refNumber = generateReferenceNumber();
        LocalDateTime detectedAt = request.getDetectedAt() != null ? request.getDetectedAt() : LocalDateTime.now();

        Breach breach = Breach.builder()
                .id(UUID.randomUUID().toString())
                .referenceNumber(refNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .breachType(request.getBreachType())
                .dataCategories(request.getDataCategories())
                .affectedCount(request.getAffectedCount())
                .detectedAt(detectedAt)
                .reportedBy(request.getReportedBy())
                .assignedTo(request.getAssignedTo())
                .build();

        // Set notification deadlines per DPDP Act
        breach.setDpbiDeadline(detectedAt.plusHours(DPBI_NOTIFICATION_HOURS));
        breach.setCertinDeadline(detectedAt.plusHours(CERTIN_NOTIFICATION_HOURS));
        breach.setReportedAt(LocalDateTime.now());

        // Persist
        saveBreach(breach);

        // Add timeline event
        addTimelineEvent(breach.getId(), "REPORTED",
                "Breach reported: " + request.getTitle(), request.getReportedBy());

        // Audit
        auditService.log("BREACH_REPORTED", "BREACH", request.getReportedBy(),
                String.format("Breach reported: %s severity=%s affected=%d",
                        refNumber, request.getSeverity(), request.getAffectedCount()));

        // Publish event
        eventBus.publish(new ComplianceEvent("breach.reported",
                Map.of("breachId", breach.getId(), "severity", request.getSeverity().name())));

        // Alert if critical
        if (breach.getSeverity() == BreachSeverity.CRITICAL) {
            eventBus.publish(new ComplianceEvent("breach.critical_alert",
                    Map.of("breachId", breach.getId(), "title", breach.getTitle())));
        }

        logger.info("Breach reported with reference: {}", refNumber);
        return breach;
    }

    private String generateReferenceNumber() {
        breachCounter++;
        return String.format("BRE-%d-%04d", LocalDateTime.now().getYear(), breachCounter);
    }

    private void saveBreach(Breach breach) {
        String sql = """
                    INSERT INTO breaches (id, reference_number, title, description, severity, breach_type,
                        data_categories, affected_count, detected_at, reported_at, status,
                        dpbi_deadline, certin_deadline, reported_by, assigned_to)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, breach.getId());
            stmt.setString(2, breach.getReferenceNumber());
            stmt.setString(3, breach.getTitle());
            stmt.setString(4, breach.getDescription());
            stmt.setString(5, breach.getSeverity().name());
            stmt.setString(6, breach.getBreachType());
            stmt.setString(7, breach.getDataCategories());
            stmt.setInt(8, breach.getAffectedCount());
            stmt.setString(9, breach.getDetectedAt().toString());
            stmt.setString(10, breach.getReportedAt().toString());
            stmt.setString(11, breach.getStatus().name());
            stmt.setString(12, breach.getDpbiDeadline().toString());
            stmt.setString(13, breach.getCertinDeadline().toString());
            stmt.setString(14, breach.getReportedBy());
            stmt.setString(15, breach.getAssignedTo());

            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Failed to save breach", e);
            throw new RuntimeException("Failed to save breach", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATUS TRANSITIONS
    // ═══════════════════════════════════════════════════════════

    public Breach updateStatus(String breachId, BreachStatus newStatus, String actorId) {
        Breach breach = getBreachById(breachId);
        if (breach == null) {
            throw new IllegalArgumentException("Breach not found");
        }

        BreachStatus oldStatus = breach.getStatus();
        breach.setStatus(newStatus);
        breach.setUpdatedAt(LocalDateTime.now());

        // Set timestamps based on status
        switch (newStatus) {
            case CONTAINED -> breach.setContainedAt(LocalDateTime.now());
            case RESOLVED -> breach.setResolvedAt(LocalDateTime.now());
        }

        // Update database
        String sql = "UPDATE breaches SET status = ?, updated_at = ?, contained_at = ?, resolved_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setString(2, breach.getUpdatedAt().toString());
            stmt.setString(3, breach.getContainedAt() != null ? breach.getContainedAt().toString() : null);
            stmt.setString(4, breach.getResolvedAt() != null ? breach.getResolvedAt().toString() : null);
            stmt.setString(5, breachId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update breach status", e);
        }

        // Timeline
        addTimelineEvent(breachId, "STATUS_CHANGED",
                String.format("Status changed: %s → %s", oldStatus, newStatus), actorId);

        // Audit
        auditService.log("BREACH_STATUS_UPDATED", "BREACH", actorId,
                String.format("Breach %s status: %s → %s", breach.getReferenceNumber(), oldStatus, newStatus));

        // Event
        eventBus.publish(new ComplianceEvent("breach.status_changed",
                Map.of("breachId", breachId, "oldStatus", oldStatus.name(), "newStatus", newStatus.name())));

        return breach;
    }

    // ═══════════════════════════════════════════════════════════
    // NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Record DPBI notification (mandatory within 72 hours per DPDP Act Section 8)
     */
    public void recordDpbiNotification(String breachId, String reference, String actorId) {
        logger.info("Recording DPBI notification for breach: {}", breachId);

        String sql = "UPDATE breaches SET dpbi_notified = 1, dpbi_notification_date = ?, dpbi_reference = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, reference);
            stmt.setString(3, breachId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record DPBI notification", e);
        }

        addTimelineEvent(breachId, "DPBI_NOTIFIED",
                "DPBI notified with reference: " + reference, actorId);

        auditService.log("DPBI_NOTIFIED", "BREACH", actorId,
                String.format("DPBI notified for breach %s, reference: %s", breachId, reference));

        eventBus.publish(new ComplianceEvent("breach.dpbi_notified",
                Map.of("breachId", breachId, "reference", reference)));
    }

    /**
     * Record CERT-IN notification (mandatory within 6 hours for critical)
     */
    public void recordCertinNotification(String breachId, String actorId) {
        logger.info("Recording CERT-IN notification for breach: {}", breachId);

        String sql = "UPDATE breaches SET certin_notified = 1, certin_notification_date = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, breachId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record CERT-IN notification", e);
        }

        addTimelineEvent(breachId, "CERTIN_NOTIFIED",
                "CERT-IN notified", actorId);

        auditService.log("CERTIN_NOTIFIED", "BREACH", actorId,
                String.format("CERT-IN notified for breach %s", breachId));
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════

    public Breach getBreachById(String id) {
        String sql = "SELECT * FROM breaches WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapBreach(rs);
            }
        } catch (SQLException e) {
            logger.error("Failed to get breach", e);
        }
        return null;
    }

    public List<Breach> getOpenBreaches() {
        return getBreachesByStatus(List.of(BreachStatus.OPEN, BreachStatus.INVESTIGATING, BreachStatus.CONTAINED));
    }

    public List<Breach> getBreachesByStatus(List<BreachStatus> statuses) {
        List<Breach> breaches = new ArrayList<>();
        String placeholders = String.join(",", Collections.nCopies(statuses.size(), "?"));
        String sql = "SELECT * FROM breaches WHERE status IN (" + placeholders + ") ORDER BY detected_at DESC";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < statuses.size(); i++) {
                stmt.setString(i + 1, statuses.get(i).name());
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                breaches.add(mapBreach(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to get breaches by status", e);
        }
        return breaches;
    }

    public List<Breach> getAllBreaches(int offset, int limit) {
        List<Breach> breaches = new ArrayList<>();
        String sql = "SELECT * FROM breaches ORDER BY detected_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                breaches.add(mapBreach(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to get breaches", e);
        }
        return breaches;
    }

    /**
     * Get breaches with overdue notifications
     */
    public List<Breach> getOverdueBreaches() {
        List<Breach> breaches = new ArrayList<>();
        String sql = """
                    SELECT * FROM breaches
                    WHERE status NOT IN ('RESOLVED', 'CLOSED')
                    AND ((dpbi_notified = 0 AND dpbi_deadline < datetime('now'))
                         OR (certin_notified = 0 AND certin_deadline < datetime('now') AND severity = 'CRITICAL'))
                    ORDER BY detected_at DESC
                """;

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                breaches.add(mapBreach(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to get overdue breaches", e);
        }
        return breaches;
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public BreachStatistics getStatistics() {
        BreachStatistics stats = new BreachStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM breaches");
            if (rs.next())
                stats.setTotalBreaches(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM breaches WHERE status NOT IN ('RESOLVED', 'CLOSED')");
            if (rs.next())
                stats.setOpenBreaches(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM breaches WHERE severity = 'CRITICAL'");
            if (rs.next())
                stats.setCriticalBreaches(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM breaches WHERE dpbi_notified = 1");
            if (rs.next())
                stats.setNotifiedBreaches(rs.getInt(1));

            rs = stmt.executeQuery("SELECT SUM(affected_count) FROM breaches");
            if (rs.next())
                stats.setTotalAffected(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT AVG(julianday(resolved_at) - julianday(detected_at)) * 24 FROM breaches WHERE resolved_at IS NOT NULL");
            if (rs.next())
                stats.setAvgResolutionHours(rs.getDouble(1));

            // DPBI compliance rate
            rs = stmt.executeQuery("""
                        SELECT COUNT(*) * 1.0 / NULLIF(
                            (SELECT COUNT(*) FROM breaches WHERE status NOT IN ('OPEN')), 0)
                        FROM breaches
                        WHERE dpbi_notified = 1 AND dpbi_notification_date <= dpbi_deadline
                    """);
            if (rs.next())
                stats.setDpbiComplianceRate(rs.getDouble(1) * 100);

        } catch (SQLException e) {
            logger.error("Failed to get breach statistics", e);
        }

        return stats;
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private void addTimelineEvent(String breachId, String eventType, String description, String actor) {
        // Would persist timeline events - simplified for now
        logger.debug("Timeline event: {} - {} - {}", eventType, description, actor);
    }

    private Breach mapBreach(ResultSet rs) throws SQLException {
        Breach breach = new Breach();
        breach.setId(rs.getString("id"));
        breach.setReferenceNumber(rs.getString("reference_number"));
        breach.setTitle(rs.getString("title"));
        breach.setDescription(rs.getString("description"));
        breach.setSeverity(BreachSeverity.valueOf(rs.getString("severity")));
        breach.setBreachType(rs.getString("breach_type"));
        breach.setDataCategories(rs.getString("data_categories"));
        breach.setAffectedCount(rs.getInt("affected_count"));

        String detectedAt = rs.getString("detected_at");
        if (detectedAt != null)
            breach.setDetectedAt(LocalDateTime.parse(detectedAt));

        String reportedAt = rs.getString("reported_at");
        if (reportedAt != null)
            breach.setReportedAt(LocalDateTime.parse(reportedAt));

        String containedAt = rs.getString("contained_at");
        if (containedAt != null)
            breach.setContainedAt(LocalDateTime.parse(containedAt));

        String resolvedAt = rs.getString("resolved_at");
        if (resolvedAt != null)
            breach.setResolvedAt(LocalDateTime.parse(resolvedAt));

        breach.setStatus(BreachStatus.valueOf(rs.getString("status")));
        breach.setRootCause(rs.getString("root_cause"));
        breach.setRemediationSteps(rs.getString("remediation_steps"));

        breach.setDpbiNotified(rs.getInt("dpbi_notified") == 1);
        String dpbiDate = rs.getString("dpbi_notification_date");
        if (dpbiDate != null)
            breach.setDpbiNotificationDate(LocalDateTime.parse(dpbiDate));

        String dpbiDeadline = rs.getString("dpbi_deadline");
        if (dpbiDeadline != null)
            breach.setDpbiDeadline(LocalDateTime.parse(dpbiDeadline));

        breach.setDpbiReference(rs.getString("dpbi_reference"));
        breach.setCertinNotified(rs.getInt("certin_notified") == 1);

        String certinDate = rs.getString("certin_notification_date");
        if (certinDate != null)
            breach.setCertinNotificationDate(LocalDateTime.parse(certinDate));

        breach.setAffectedPartiesNotified(rs.getInt("affected_parties_notified") == 1);
        breach.setReportedBy(rs.getString("reported_by"));
        breach.setAssignedTo(rs.getString("assigned_to"));

        return breach;
    }

    private BreachRequest createBreachFromSecurityEvent(ComplianceEvent event) {
        // Convert security incident to breach request
        return BreachRequest.builder()
                .title("Security Incident: " + event.getType())
                .description("Auto-generated from security event")
                .severity(BreachSeverity.MEDIUM)
                .breachType("Security Incident")
                .reportedBy("SYSTEM")
                .build();
    }

    private void handleBreachEvent(ComplianceEvent event) {
        logger.debug("Handling breach event: {}", event.getType());
    }

    public boolean isInitialized() {
        return initialized;
    }
}
