package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Hospitality & Travel Sector Compliance — Guest data, PNR, hotel surveillance, loyalty programmes. */
@Service
public class HospitalityTravelComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(HospitalityTravelComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public HospitalityTravelComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing Hospitality/Travel Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("Hospitality/Travel Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hospitality_guest_data (
                    id TEXT PRIMARY KEY, hotel_chain TEXT NOT NULL, data_categories TEXT DEFAULT 'PII,PASSPORT,PAYMENT,CCTV,LOYALTY',
                    form_c_compliance INTEGER DEFAULT 1, cctv_notice_displayed INTEGER DEFAULT 0,
                    guest_consent_mechanism TEXT DEFAULT 'CHECK_IN_FORM', loyalty_data_consent INTEGER DEFAULT 0,
                    cross_border_booking_data INTEGER DEFAULT 0, data_retention_days INTEGER DEFAULT 1825,
                    wifi_tracking_disclosed INTEGER DEFAULT 0, compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS hospitality_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create Hospitality tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"FRRO-FORMC-01", "FRRO Form C Guest Data Rules", "DPDP-LEGITIMATE-USE,DPDP-RETENTION"}, {"TOURISM-DATA-01", "Tourism Ministry Data Guidelines", "DPDP-CONSENT,DPDP-RIGHTS"}, {"OTA-DATA-01", "Online Travel Agent Data Rules", "DPDP-CONSENT,DPDP-THIRD-PARTY"}, {"HOTEL-CCTV-01", "Hotel CCTV Surveillance Rules", "DPDP-CONSENT,DPDP-SECURITY"}};
        String sql = "INSERT OR IGNORE INTO hospitality_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM hospitality_guest_data WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantHotels", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM hospitality_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM hospitality_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
