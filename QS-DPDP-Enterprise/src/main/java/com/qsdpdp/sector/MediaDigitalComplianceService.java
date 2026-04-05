package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Media & Digital Content Sector Compliance — OTT, news, content personalization, ad-tech. */
@Service
public class MediaDigitalComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(MediaDigitalComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public MediaDigitalComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing Media/Digital Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("Media/Digital Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS media_content_personalization (
                    id TEXT PRIMARY KEY, platform_name TEXT NOT NULL, platform_type TEXT DEFAULT 'OTT',
                    consent_for_personalization INTEGER DEFAULT 0, consent_for_ad_tracking INTEGER DEFAULT 0,
                    children_content_safeguards INTEGER DEFAULT 1, data_categories TEXT DEFAULT 'VIEWING,SEARCH,PROFILE',
                    third_party_ad_networks TEXT, cookie_consent_mechanism TEXT DEFAULT 'OPT_IN',
                    algorithmic_transparency INTEGER DEFAULT 0, compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS media_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create Media tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"IT-OTT-01", "IT (OTT) Intermediary Guidelines 2021", "DPDP-CONSENT,DPDP-BREACH"}, {"MEITY-SAFE-01", "MeitY Safe Harbour Rules", "DPDP-SECURITY,DPDP-THIRD-PARTY"}, {"PRESS-DATA-01", "Press Council Data Ethics", "DPDP-CONSENT,DPDP-RIGHTS"}, {"AD-TECH-01", "ASCI Digital Advertising Code", "DPDP-CONSENT,DPDP-TRANSPARENCY"}};
        String sql = "INSERT OR IGNORE INTO media_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM media_content_personalization WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantPlatforms", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM media_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM media_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
