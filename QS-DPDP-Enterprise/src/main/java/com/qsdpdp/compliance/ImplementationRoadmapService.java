package com.qsdpdp.compliance;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 180-Day DPDP Implementation Roadmap Tracker
 * Tracks the 4-phase operationalisation plan from Vinod Shah's framework:
 *   Phase 1 (Days 1-45):  Foundation & Discovery
 *   Phase 2 (Days 46-90): Vendor Ecosystem & Control Design
 *   Phase 3 (Days 91-135): Technical Implementation
 *   Phase 4 (Days 136-180): Validation & Operationalisation
 *
 * @version 1.0.0
 * @since Phase 7 — Compliance Enhancement
 */
@Service
public class ImplementationRoadmapService {

    private static final Logger logger = LoggerFactory.getLogger(ImplementationRoadmapService.class);
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public ImplementationRoadmapService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Implementation Roadmap Service...");
        createTables();
        seedDefaultMilestones();
        initialized = true;
        logger.info("Implementation Roadmap Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS roadmap_milestones (
                    id TEXT PRIMARY KEY, phase INTEGER NOT NULL, phase_name TEXT NOT NULL,
                    week_start INTEGER, week_end INTEGER,
                    milestone_name TEXT NOT NULL, description TEXT, sector TEXT,
                    status TEXT DEFAULT 'NOT_STARTED', progress_pct INTEGER DEFAULT 0,
                    owner TEXT, deliverable TEXT,
                    started_at TIMESTAMP, completed_at TIMESTAMP,
                    due_date TIMESTAMP, notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS roadmap_deliverables (
                    id TEXT PRIMARY KEY, milestone_id TEXT NOT NULL,
                    deliverable_name TEXT NOT NULL, description TEXT,
                    status TEXT DEFAULT 'PENDING', evidence_path TEXT,
                    reviewed_by TEXT, reviewed_at TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (milestone_id) REFERENCES roadmap_milestones(id))
            """);
            logger.info("Roadmap tables created");
        } catch (SQLException e) { logger.error("Failed to create roadmap tables", e); }
    }

    // ═══════ MILESTONE MANAGEMENT ═══════

    public String updateMilestoneStatus(String milestoneId, String status, int progressPct, String notes) {
        String sql = "UPDATE roadmap_milestones SET status = ?, progress_pct = ?, notes = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "UPDATE roadmap_milestones SET status = ?, progress_pct = ?, notes = ? WHERE id = ?")) {
            ps.setString(1, status); ps.setInt(2, progressPct); ps.setString(3, notes);
            ps.setString(4, milestoneId); ps.executeUpdate();
            if ("COMPLETED".equals(status)) {
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "UPDATE roadmap_milestones SET completed_at = ? WHERE id = ?")) {
                    ps2.setString(1, LocalDateTime.now().toString()); ps2.setString(2, milestoneId); ps2.executeUpdate();
                }
            }
            if ("IN_PROGRESS".equals(status)) {
                try (PreparedStatement ps2 = conn.prepareStatement(
                        "UPDATE roadmap_milestones SET started_at = COALESCE(started_at, ?) WHERE id = ?")) {
                    ps2.setString(1, LocalDateTime.now().toString()); ps2.setString(2, milestoneId); ps2.executeUpdate();
                }
            }
            auditService.log("ROADMAP_MILESTONE_UPDATED", "COMPLIANCE", null,
                    "Milestone " + milestoneId + " → " + status + " (" + progressPct + "%)");
            return milestoneId;
        } catch (SQLException e) { logger.error("Failed to update milestone", e); return null; }
    }

    public List<RoadmapMilestone> getMilestonesByPhase(int phase) {
        List<RoadmapMilestone> milestones = new ArrayList<>();
        String sql = "SELECT * FROM roadmap_milestones WHERE phase = ? ORDER BY week_start, id";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, phase); ResultSet rs = ps.executeQuery();
            while (rs.next()) milestones.add(mapMilestone(rs));
        } catch (SQLException e) { logger.error("Failed to get milestones", e); }
        return milestones;
    }

    public List<RoadmapMilestone> getAllMilestones() {
        List<RoadmapMilestone> milestones = new ArrayList<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM roadmap_milestones ORDER BY phase, week_start")) {
            while (rs.next()) milestones.add(mapMilestone(rs));
        } catch (SQLException e) { logger.error("Failed to get all milestones", e); }
        return milestones;
    }

    // ═══════ STATISTICS ═══════

    public RoadmapStats getStatistics() {
        RoadmapStats stats = new RoadmapStats();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM roadmap_milestones");
            if (rs.next()) stats.totalMilestones = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM roadmap_milestones WHERE status = 'COMPLETED'");
            if (rs.next()) stats.completedMilestones = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM roadmap_milestones WHERE status = 'IN_PROGRESS'");
            if (rs.next()) stats.inProgressMilestones = rs.getInt(1);
            rs = stmt.executeQuery("SELECT AVG(progress_pct) FROM roadmap_milestones");
            if (rs.next()) stats.overallProgressPct = rs.getInt(1);
            for (int p = 1; p <= 4; p++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT AVG(progress_pct) FROM roadmap_milestones WHERE phase = ?")) {
                    ps.setInt(1, p); rs = ps.executeQuery();
                    if (rs.next()) stats.phaseProgress.put(p, rs.getInt(1));
                }
            }
        } catch (SQLException e) { logger.error("Failed to get roadmap statistics", e); }
        return stats;
    }

    private RoadmapMilestone mapMilestone(ResultSet rs) throws SQLException {
        RoadmapMilestone m = new RoadmapMilestone();
        m.id = rs.getString("id"); m.phase = rs.getInt("phase"); m.phaseName = rs.getString("phase_name");
        m.weekStart = rs.getInt("week_start"); m.weekEnd = rs.getInt("week_end");
        m.milestoneName = rs.getString("milestone_name"); m.description = rs.getString("description");
        m.sector = rs.getString("sector"); m.status = rs.getString("status");
        m.progressPct = rs.getInt("progress_pct"); m.owner = rs.getString("owner");
        m.deliverable = rs.getString("deliverable"); m.notes = rs.getString("notes");
        return m;
    }

    // ═══════ SEED DATA ═══════

    private void seedDefaultMilestones() {
        String[][] milestones = {
            // Phase 1: Foundation & Discovery (Days 1-45)
            {"1", "Foundation & Discovery", "1", "2", "Governance & Resource Mobilisation",
             "Appoint exec sponsor, establish Privacy Council, stand up Programme Office", "ALL", "Governance charter, resource plan"},
            {"1", "Foundation & Discovery", "3", "4", "Comprehensive Data Archaeology",
             "Deploy discovery tools, conduct departmental interviews, map data flows", "ALL", "Data inventory registry"},
            {"1", "Foundation & Discovery", "5", "6", "Legal Basis & Purpose Alignment",
             "Audit processing activities, identify gaps, prepare consent refresh", "ALL", "Processing activity records"},
            // Phase 2: Vendor Ecosystem & Control Design (Days 46-90)
            {"2", "Vendor Ecosystem & Control Design", "7", "9", "Vendor Risk Remediation",
             "Vendor inventory, risk assessments, contract renegotiation", "ALL", "Vendor risk register"},
            {"2", "Vendor Ecosystem & Control Design", "10", "11", "Technical Control Architecture",
             "ABAC design, HSM/encryption architecture, consent management workflows", "ALL", "Technical architecture designs"},
            {"2", "Vendor Ecosystem & Control Design", "12", "13", "Policy & Procedure Development",
             "Privacy policies, DSAR procedures, breach playbooks, training curriculum", "ALL", "Policy suite, training materials"},
            // Phase 3: Technical Implementation (Days 91-135)
            {"3", "Technical Implementation", "14", "16", "Core Controls Deployment",
             "Data classification, HSM, column encryption, ABAC, tamper-proof logging", "ALL", "Operational technical controls"},
            {"3", "Technical Implementation", "17", "18", "Consent & Rights Management",
             "Consent collection, DSAR portals, withdrawal propagation, minors workflows", "ALL", "Consent management system"},
            {"3", "Technical Implementation", "19", "20", "BFSI Controls",
             "Aadhaar vault deployment, RBI reporting integration", "BFSI", "BFSI sector controls"},
            {"3", "Technical Implementation", "19", "20", "Healthcare Controls",
             "ABDM interoperability, clinical system access refinement", "HEALTHCARE", "Healthcare sector controls"},
            {"3", "Technical Implementation", "19", "20", "Insurance Controls",
             "TPA monitoring, underwriting data flow controls", "INSURANCE", "Insurance sector controls"},
            // Phase 4: Validation & Operationalisation (Days 136-180)
            {"4", "Validation & Operationalisation", "21", "23", "Testing & Validation",
             "Breach simulation, deletion drills, consent withdrawal tests, pen testing", "ALL", "Test results"},
            {"4", "Validation & Operationalisation", "24", "25", "Training & Change Management",
             "Role-based training, phishing simulations, internal awareness campaign", "ALL", "Training completion records"},
            {"4", "Validation & Operationalisation", "26", "26", "Go-Live & Monitoring",
             "Declare compliance readiness, continuous monitoring dashboards", "ALL", "Compliance readiness declaration"},
        };

        String sql = """
            INSERT OR IGNORE INTO roadmap_milestones (id, phase, phase_name, week_start, week_end,
                milestone_name, description, sector, deliverable)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] m : milestones) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setInt(2, Integer.parseInt(m[0])); ps.setString(3, m[1]);
                ps.setInt(4, Integer.parseInt(m[2])); ps.setInt(5, Integer.parseInt(m[3]));
                ps.setString(6, m[4]); ps.setString(7, m[5]); ps.setString(8, m[6]); ps.setString(9, m[7]);
                ps.executeUpdate();
            }
            logger.info("180-day roadmap milestones seeded ({} milestones)", milestones.length);
        } catch (SQLException e) { logger.error("Failed to seed roadmap milestones", e); }
    }

    public boolean isInitialized() { return initialized; }

    // ═══════ DATA CLASSES ═══════

    public static class RoadmapMilestone {
        public String id, phaseName, milestoneName, description, sector, status, owner, deliverable, notes;
        public int phase, weekStart, weekEnd, progressPct;
    }

    public static class RoadmapStats {
        public int totalMilestones, completedMilestones, inProgressMilestones, overallProgressPct;
        public Map<Integer, Integer> phaseProgress = new LinkedHashMap<>();
    }
}
