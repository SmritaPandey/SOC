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
 * Government/Public Sector Compliance Service
 * Operationalises DPDP Rules for Central/State Government bodies,
 * e-Governance platforms, and Digital India initiatives.
 *
 * Regulatory References:
 *   - MeitY IT Act Notifications
 *   - e-Governance Data Standards
 *   - RTI Act intersection with DPDP
 *   - DPDP Act 2023, Section 7 (Legitimate Uses by State)
 *
 * @version 1.0.0
 * @since Phase 7 — Sector Compliance
 */
@Service
public class GovernmentComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(GovernmentComplianceService.class);
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public GovernmentComplianceService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager; this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Government Compliance Service...");
        createTables(); seedRegulatoryMappings();
        initialized = true;
        logger.info("Government Compliance Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS govt_citizen_data_registry (
                    id TEXT PRIMARY KEY,
                    department TEXT NOT NULL,
                    scheme_name TEXT,
                    data_categories TEXT DEFAULT 'AADHAAR,PAN,DEMOGRAPHIC',
                    citizen_count INTEGER DEFAULT 0,
                    legal_basis TEXT DEFAULT 'LEGITIMATE_USE_STATE',
                    consent_required INTEGER DEFAULT 0,
                    data_sharing_agreements TEXT,
                    rti_exemption_documented INTEGER DEFAULT 0,
                    data_localisation_india INTEGER DEFAULT 1,
                    grievance_officer_appointed INTEGER DEFAULT 0,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS govt_regulatory_mapping (
                    id TEXT PRIMARY KEY,
                    regulation_code TEXT NOT NULL,
                    regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            logger.info("Government Compliance tables created");
        } catch (SQLException e) { logger.error("Failed to create Government Compliance tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regulations = {
            {"MEITY-IT-01", "MeitY IT Act Notifications", "DPDP-SECURITY,DPDP-BREACH"},
            {"EGOV-DS-01", "e-Governance Data Standards", "DPDP-MINIMISATION,DPDP-RETENTION"},
            {"RTI-DPDP-01", "RTI Act Intersection with DPDP", "DPDP-LEGITIMATE-USE,DPDP-RIGHTS"},
            {"DPDP-S7-01", "DPDP §7 Legitimate Use by State", "DPDP-LEGITIMATE-USE"},
            {"UIDAI-GOV-01", "UIDAI Guidelines for Government", "DPDP-CONSENT,DPDP-SECURITY"},
            {"DIGILOCKER-01", "DigiLocker Data Governance", "DPDP-CONSENT,DPDP-ACCESS"},
        };
        String sql = "INSERT OR IGNORE INTO govt_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] reg : regulations) {
                ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, reg[0]); ps.setString(3, reg[1]); ps.setString(4, reg[2]);
                ps.executeUpdate();
            }
        } catch (SQLException e) { logger.error("Failed to seed Government regulatory mappings", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM govt_citizen_data_registry WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantDepartments", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM govt_regulatory_mapping WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM govt_regulatory_mapping");
            stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Failed to get Government statistics", e); }
        return stats;
    }

    public boolean isInitialized() { return initialized; }
}
