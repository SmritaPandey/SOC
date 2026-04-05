package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Energy & Utilities Sector Compliance — Smart meter data, consumer billing, AMI infrastructure. */
@Service
public class EnergyUtilityComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(EnergyUtilityComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public EnergyUtilityComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing Energy/Utility Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("Energy/Utility Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS energy_smart_meter_data (
                    id TEXT PRIMARY KEY, utility_name TEXT NOT NULL, meter_type TEXT DEFAULT 'AMI',
                    consumer_data_categories TEXT DEFAULT 'CONSUMPTION,BILLING,ADDRESS',
                    consent_for_analytics INTEGER DEFAULT 0, data_anonymised_for_research INTEGER DEFAULT 0,
                    billing_data_retention_days INTEGER DEFAULT 2555, grievance_officer_appointed INTEGER DEFAULT 0,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS energy_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create Energy tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"CERC-DATA-01", "CERC Smart Meter Data Rules", "DPDP-CONSENT,DPDP-MINIMISATION"}, {"BEE-AMI-01", "BEE AMI Infrastructure Standards", "DPDP-SECURITY,DPDP-RETENTION"}, {"DISCOM-BILL-01", "DISCOM Billing Data Governance", "DPDP-CONSENT,DPDP-RIGHTS"}};
        String sql = "INSERT OR IGNORE INTO energy_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM energy_smart_meter_data WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantUtilities", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM energy_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM energy_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
