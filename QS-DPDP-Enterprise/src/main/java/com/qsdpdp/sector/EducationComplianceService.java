package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/**
 * Education Sector Compliance Service
 * Operationalises DPDP Rules for Schools, Universities, EdTech platforms.
 *
 * Regulatory References:
 *   - NCERT/UGC Data Guidelines
 *   - NEP 2020 Digital Learning Privacy
 *   - DPDP Act 2023, §9 (Children's Data), §33
 *
 * @version 1.0.0
 * @since Phase 7 — Sector Compliance
 */
@Service
public class EducationComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(EducationComplianceService.class);
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public EducationComplianceService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager; this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Education Compliance Service...");
        createTables(); seedRegulatoryMappings();
        initialized = true;
        logger.info("Education Compliance Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edu_student_data_compliance (
                    id TEXT PRIMARY KEY,
                    institution_name TEXT NOT NULL,
                    institution_type TEXT DEFAULT 'SCHOOL',
                    student_data_categories TEXT DEFAULT 'ACADEMIC,DEMOGRAPHIC,GUARDIAN',
                    children_data_safeguards INTEGER DEFAULT 1,
                    parental_consent_mechanism TEXT DEFAULT 'WRITTEN',
                    edtech_vendor_agreements INTEGER DEFAULT 0,
                    data_retention_policy_days INTEGER DEFAULT 2555,
                    anti_tracking_for_minors INTEGER DEFAULT 1,
                    anti_advertising_for_minors INTEGER DEFAULT 1,
                    behavioral_monitoring_prohibited INTEGER DEFAULT 1,
                    grievance_officer_appointed INTEGER DEFAULT 0,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS edu_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            logger.info("Education Compliance tables created");
        } catch (SQLException e) { logger.error("Failed to create Education Compliance tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regulations = {
            {"NEP-2020-DP-01", "NEP 2020 Digital Learning Privacy", "DPDP-CONSENT,DPDP-CHILDREN"},
            {"NCERT-DATA-01", "NCERT Student Data Guidelines", "DPDP-MINIMISATION,DPDP-RETENTION"},
            {"UGC-DATA-01", "UGC Higher Ed Data Standards", "DPDP-CONSENT,DPDP-RIGHTS"},
            {"DPDP-S33-01", "DPDP §33 Children Data Protection", "DPDP-CHILDREN,DPDP-CONSENT"},
            {"EDTECH-01", "EdTech Platform Compliance", "DPDP-THIRD-PARTY,DPDP-CONSENT"},
        };
        String sql = "INSERT OR IGNORE INTO edu_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] reg : regulations) {
                ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, reg[0]); ps.setString(3, reg[1]); ps.setString(4, reg[2]);
                ps.executeUpdate();
            }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM edu_student_data_compliance WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantInstitutions", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM edu_regulatory_mapping WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM edu_regulatory_mapping");
            stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }

    public boolean isInitialized() { return initialized; }
}
