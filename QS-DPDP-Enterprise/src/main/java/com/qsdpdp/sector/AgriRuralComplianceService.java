package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Agriculture & Rural Sector Compliance — Farmer data, PM-KISAN, AgriStack, FPO data. */
@Service
public class AgriRuralComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(AgriRuralComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public AgriRuralComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing AgriRural Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("AgriRural Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS agri_farmer_data_compliance (
                    id TEXT PRIMARY KEY, scheme_name TEXT NOT NULL, data_categories TEXT DEFAULT 'AADHAAR,LAND_RECORDS,BANK_ACCOUNT',
                    farmer_consent_mechanism TEXT DEFAULT 'VERBAL_RECORDED', data_used_for_profiling INTEGER DEFAULT 0,
                    third_party_agritech_sharing INTEGER DEFAULT 0, data_localisation_india INTEGER DEFAULT 1,
                    grievance_mechanism TEXT DEFAULT 'DISTRICT_OFFICER', data_retention_days INTEGER DEFAULT 3650,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS agri_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create AgriRural tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"AGRISTACK-01", "India AgriStack Data Governance", "DPDP-CONSENT,DPDP-MINIMISATION"}, {"PM-KISAN-01", "PM-KISAN Beneficiary Data", "DPDP-LEGITIMATE-USE,DPDP-RETENTION"}, {"FPO-DATA-01", "FPO Member Data Privacy", "DPDP-CONSENT,DPDP-RIGHTS"}, {"CROP-INS-01", "Crop Insurance Data Rules", "DPDP-CONSENT,DPDP-THIRD-PARTY"}};
        String sql = "INSERT OR IGNORE INTO agri_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM agri_farmer_data_compliance WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantSchemes", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM agri_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM agri_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
