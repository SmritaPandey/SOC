package com.qsdpdp.dpia;

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
 * DPIA Service
 * Manages Data Protection Impact Assessments per DPDP Act
 */
@Service
public class DPIAService {

    private static final Logger logger = LoggerFactory.getLogger(DPIAService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;

    private boolean initialized = false;
    private int dpiaCounter = 0;

    @Autowired
    public DPIAService(DatabaseManager dbManager, AuditService auditService, EventBus eventBus) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing DPIA Service...");
        loadDpiaCounter();
        eventBus.subscribe("dpia.*", e -> logger.debug("DPIA event: {}", e.getType()));
        initialized = true;
        logger.info("DPIA Service initialized");
    }

    private void loadDpiaCounter() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM dpias");
            if (rs.next())
                dpiaCounter = rs.getInt(1);
        } catch (Exception e) {
            logger.warn("Could not load DPIA counter", e);
        }
    }

    public DPIA createDPIA(DPIARequest request) {
        logger.info("Creating DPIA: {}", request.getTitle());

        dpiaCounter++;
        String refNumber = String.format("DPIA-%d-%04d", LocalDateTime.now().getYear(), dpiaCounter);

        DPIA dpia = DPIA.builder()
                .id(UUID.randomUUID().toString())
                .referenceNumber(refNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .projectName(request.getProjectName())
                .dataTypes(request.getDataTypes())
                .assessor(request.getAssessor())
                .build();

        dpia.setStartedAt(LocalDateTime.now());

        saveDPIA(dpia);

        auditService.log("DPIA_CREATED", "DPIA", request.getAssessor(),
                String.format("DPIA created: %s - %s", refNumber, request.getTitle()));

        eventBus.publish(new ComplianceEvent("dpia.created",
                Map.of("dpiaId", dpia.getId(), "title", dpia.getTitle())));

        return dpia;
    }

    private void saveDPIA(DPIA dpia) {
        String sql = """
                    INSERT INTO dpias (id, reference_number, title, description, project_name,
                        data_types, status, risk_level, risk_score, assessor, started_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dpia.getId());
            stmt.setString(2, dpia.getReferenceNumber());
            stmt.setString(3, dpia.getTitle());
            stmt.setString(4, dpia.getDescription());
            stmt.setString(5, dpia.getProjectName());
            stmt.setString(6, dpia.getDataTypes());
            stmt.setString(7, dpia.getStatusString());
            stmt.setString(8, dpia.getRiskLevel().name());
            stmt.setDouble(9, dpia.getRiskScore());
            stmt.setString(10, dpia.getAssessor());
            stmt.setString(11, dpia.getStartedAt().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save DPIA", e);
        }
    }

    public DPIA submitForReview(String dpiaId, String findings, String mitigationPlan, String actorId) {
        DPIA dpia = getDPIAById(dpiaId);
        if (dpia == null)
            throw new IllegalArgumentException("DPIA not found");

        dpia.setStatus(DPIAStatus.PENDING_REVIEW);
        dpia.setFindings(findings);
        dpia.setMitigationPlan(mitigationPlan);
        dpia.setCompletedAt(LocalDateTime.now());
        dpia.setUpdatedAt(LocalDateTime.now());

        updateDPIA(dpia);

        auditService.log("DPIA_SUBMITTED", "DPIA", actorId,
                "DPIA submitted for review: " + dpia.getReferenceNumber());

        return dpia;
    }

    public DPIA approveDPIA(String dpiaId, String actorId) {
        DPIA dpia = getDPIAById(dpiaId);
        if (dpia == null)
            throw new IllegalArgumentException("DPIA not found");

        dpia.setStatus(DPIAStatus.APPROVED);
        dpia.setApprover(actorId);
        dpia.setApprovedAt(LocalDateTime.now());
        dpia.setNextReviewAt(LocalDateTime.now().plusYears(1));
        dpia.setUpdatedAt(LocalDateTime.now());

        updateDPIA(dpia);

        auditService.log("DPIA_APPROVED", "DPIA", actorId,
                "DPIA approved: " + dpia.getReferenceNumber());

        eventBus.publish(new ComplianceEvent("dpia.approved",
                Map.of("dpiaId", dpiaId, "riskLevel", dpia.getRiskLevel().name())));

        return dpia;
    }

    public DPIA rejectDPIA(String dpiaId, String reason, String actorId) {
        DPIA dpia = getDPIAById(dpiaId);
        if (dpia == null)
            throw new IllegalArgumentException("DPIA not found");

        dpia.setStatus(DPIAStatus.REJECTED);
        dpia.setResidualRisks(reason);
        dpia.setUpdatedAt(LocalDateTime.now());

        updateDPIA(dpia);

        auditService.log("DPIA_REJECTED", "DPIA", actorId,
                String.format("DPIA rejected: %s - %s", dpia.getReferenceNumber(), reason));

        return dpia;
    }

    public DPIA assessRisk(String dpiaId, List<DPIARisk> risks, String actorId) {
        DPIA dpia = getDPIAById(dpiaId);
        if (dpia == null)
            throw new IllegalArgumentException("DPIA not found");

        dpia.setRisks(risks);

        // Calculate overall risk score
        double totalScore = risks.stream().mapToDouble(DPIARisk::getScore).sum();
        double avgScore = risks.isEmpty() ? 0 : totalScore / risks.size();

        dpia.setRiskScore(avgScore * 4); // Scale to 100
        dpia.setRiskLevel(RiskLevel.fromScore(dpia.getRiskScore()));
        dpia.setStatus(DPIAStatus.IN_PROGRESS);
        dpia.setUpdatedAt(LocalDateTime.now());

        updateDPIA(dpia);

        if (dpia.requiresDPBIConsultation()) {
            eventBus.publish(new ComplianceEvent("dpia.high_risk",
                    Map.of("dpiaId", dpiaId, "riskLevel", dpia.getRiskLevel().name())));
        }

        auditService.log("DPIA_RISK_ASSESSED", "DPIA", actorId,
                String.format("DPIA risk assessed: %s - %s", dpia.getReferenceNumber(), dpia.getRiskLevel()));

        return dpia;
    }

    private void updateDPIA(DPIA dpia) {
        String sql = """
                    UPDATE dpias SET status = ?, risk_level = ?, risk_score = ?,
                        findings = ?, mitigation_plan = ?, residual_risks = ?,
                        approver = ?, completed_at = ?, approved_at = ?, next_review_at = ?, updated_at = ?
                    WHERE id = ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dpia.getStatusString());
            stmt.setString(2, dpia.getRiskLevel().name());
            stmt.setDouble(3, dpia.getRiskScore());
            stmt.setString(4, dpia.getFindings());
            stmt.setString(5, dpia.getMitigationPlan());
            stmt.setString(6, dpia.getResidualRisks());
            stmt.setString(7, dpia.getApprover());
            stmt.setString(8, dpia.getCompletedAt() != null ? dpia.getCompletedAt().toString() : null);
            stmt.setString(9, dpia.getApprovedAt() != null ? dpia.getApprovedAt().toString() : null);
            stmt.setString(10, dpia.getNextReviewAt() != null ? dpia.getNextReviewAt().toString() : null);
            stmt.setString(11, dpia.getUpdatedAt().toString());
            stmt.setString(12, dpia.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update DPIA", e);
        }
    }

    public DPIA getDPIAById(String id) {
        String sql = "SELECT * FROM dpias WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapDPIA(rs);
        } catch (SQLException e) {
            logger.error("Failed to get DPIA", e);
        }
        return null;
    }

    public List<DPIA> getAllDPIAs(int offset, int limit) {
        List<DPIA> dpias = new ArrayList<>();
        String sql = "SELECT * FROM dpias ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                dpias.add(mapDPIA(rs));
        } catch (SQLException e) {
            logger.error("Failed to get DPIAs", e);
        }
        return dpias;
    }

    public List<DPIA> getDPIAsRequiringReview() {
        List<DPIA> dpias = new ArrayList<>();
        String sql = "SELECT * FROM dpias WHERE status = 'APPROVED' AND next_review_at < datetime('now')";

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next())
                dpias.add(mapDPIA(rs));
        } catch (SQLException e) {
            logger.error("Failed to get DPIAs requiring review", e);
        }
        return dpias;
    }

    public DPIAStatistics getStatistics() {
        DPIAStatistics stats = new DPIAStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM dpias");
            if (rs.next())
                stats.setTotalDPIAs(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM dpias WHERE status = 'APPROVED'");
            if (rs.next())
                stats.setApprovedDPIAs(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM dpias WHERE status NOT IN ('APPROVED', 'REJECTED')");
            if (rs.next())
                stats.setPendingDPIAs(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM dpias WHERE risk_level IN ('HIGH', 'CRITICAL')");
            if (rs.next())
                stats.setHighRiskDPIAs(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM dpias WHERE status = 'APPROVED' AND next_review_at < datetime('now')");
            if (rs.next())
                stats.setOverdueReviews(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get DPIA statistics", e);
        }

        return stats;
    }

    private DPIA mapDPIA(ResultSet rs) throws SQLException {
        DPIA dpia = new DPIA();
        dpia.setId(rs.getString("id"));
        dpia.setReferenceNumber(rs.getString("reference_number"));
        dpia.setTitle(rs.getString("title"));
        dpia.setDescription(rs.getString("description"));
        dpia.setProjectName(rs.getString("project_name"));
        dpia.setDataTypes(rs.getString("data_types"));
        dpia.setStatus(rs.getString("status"));
        dpia.setRiskLevel(RiskLevel.valueOf(rs.getString("risk_level")));
        dpia.setRiskScore(rs.getDouble("risk_score"));
        dpia.setAssessor(rs.getString("assessor"));
        dpia.setApprover(rs.getString("approver"));

        String startedAt = rs.getString("started_at");
        if (startedAt != null)
            dpia.setStartedAt(LocalDateTime.parse(startedAt));

        String completedAt = rs.getString("completed_at");
        if (completedAt != null)
            dpia.setCompletedAt(LocalDateTime.parse(completedAt));

        String approvedAt = rs.getString("approved_at");
        if (approvedAt != null)
            dpia.setApprovedAt(LocalDateTime.parse(approvedAt));

        String nextReviewAt = rs.getString("next_review_at");
        if (nextReviewAt != null)
            dpia.setNextReviewAt(LocalDateTime.parse(nextReviewAt));

        dpia.setFindings(rs.getString("findings"));
        dpia.setMitigationPlan(rs.getString("mitigation_plan"));
        dpia.setResidualRisks(rs.getString("residual_risks"));

        return dpia;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
