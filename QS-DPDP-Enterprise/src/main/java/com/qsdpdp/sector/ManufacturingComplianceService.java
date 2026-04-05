package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Manufacturing Sector Compliance — Employee IoT data, shop-floor surveillance, vendor data governance. */
@Service
public class ManufacturingComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(ManufacturingComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;

    @Autowired public ManufacturingComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing Manufacturing Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("Manufacturing Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS mfg_employee_data_compliance (
                    id TEXT PRIMARY KEY, company_name TEXT NOT NULL, employee_data_categories TEXT DEFAULT 'PII,BIOMETRIC,ATTENDANCE,CCTV',
                    biometric_consent INTEGER DEFAULT 0, cctv_notice_displayed INTEGER DEFAULT 0,
                    iot_sensor_data_anonymised INTEGER DEFAULT 0, vendor_data_agreements INTEGER DEFAULT 0,
                    data_retention_policy_days INTEGER DEFAULT 1825, grievance_officer_appointed INTEGER DEFAULT 0,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS mfg_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create Manufacturing tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"FACTORY-ACT-01", "Factories Act Worker Data", "DPDP-CONSENT,DPDP-RETENTION"}, {"ESI-DATA-01", "ESI Data Handling", "DPDP-LEGITIMATE-USE"}, {"POSH-DATA-01", "POSH Act Data Privacy", "DPDP-CONSENT,DPDP-SECURITY"}, {"IOT-IND-01", "Industrial IoT Data Standards", "DPDP-MINIMISATION,DPDP-SECURITY"}};
        String sql = "INSERT OR IGNORE INTO mfg_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM mfg_employee_data_compliance WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantCompanies", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM mfg_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM mfg_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
