package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Real Estate Sector Compliance — Tenant data, property registration, RERA compliance, CCTV in premises. */
@Service
public class RealEstateComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(RealEstateComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public RealEstateComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing RealEstate Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("RealEstate Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS realestate_tenant_data (
                    id TEXT PRIMARY KEY, property_name TEXT NOT NULL, data_categories TEXT DEFAULT 'PII,AADHAAR,FINANCIAL,CCTV',
                    tenant_consent_mechanism TEXT DEFAULT 'WRITTEN', cctv_notice_displayed INTEGER DEFAULT 0,
                    biometric_access_consent INTEGER DEFAULT 0, data_sharing_with_authorities INTEGER DEFAULT 0,
                    data_retention_days INTEGER DEFAULT 1825, compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS realestate_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create RealEstate tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"RERA-DATA-01", "RERA Buyer Data Protection", "DPDP-CONSENT,DPDP-RIGHTS"}, {"RENT-ACT-01", "Model Tenancy Act Data", "DPDP-CONSENT,DPDP-RETENTION"}, {"SMART-CITY-01", "Smart City Surveillance Data", "DPDP-CONSENT,DPDP-SECURITY"}};
        String sql = "INSERT OR IGNORE INTO realestate_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM realestate_tenant_data WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantProperties", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM realestate_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM realestate_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
