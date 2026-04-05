package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Transport & Logistics Sector Compliance — Passenger data, FASTag, GPS tracking, fleet management. */
@Service
public class TransportLogisticsComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(TransportLogisticsComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public TransportLogisticsComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing Transport/Logistics Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("Transport/Logistics Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transport_passenger_data (
                    id TEXT PRIMARY KEY, operator_name TEXT NOT NULL, transport_mode TEXT DEFAULT 'ROAD',
                    passenger_data_categories TEXT DEFAULT 'PII,TICKET,LOCATION,PAYMENT',
                    gps_tracking_consent INTEGER DEFAULT 0, location_data_retention_days INTEGER DEFAULT 365,
                    fastag_data_anonymised INTEGER DEFAULT 0, driver_data_consent INTEGER DEFAULT 0,
                    fleet_telematics_disclosed INTEGER DEFAULT 0, compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transport_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create Transport tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"NHAI-FASTAG-01", "NHAI FASTag Data Privacy", "DPDP-CONSENT,DPDP-MINIMISATION"}, {"MV-ACT-DATA-01", "Motor Vehicle Act Data Rules", "DPDP-RETENTION,DPDP-SECURITY"}, {"IRCTC-DATA-01", "IRCTC Passenger Data Policy", "DPDP-CONSENT,DPDP-RIGHTS"}, {"DGCA-PNR-01", "DGCA PNR Data Requirements", "DPDP-RETENTION,DPDP-CROSSBORDER"}};
        String sql = "INSERT OR IGNORE INTO transport_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transport_passenger_data WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantOperators", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM transport_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM transport_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
