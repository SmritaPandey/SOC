package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Social Media Platform Compliance — User profiling, content moderation data, ad-tech, misinformation. */
@Service
public class SocialMediaComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(SocialMediaComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public SocialMediaComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing SocialMedia Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("SocialMedia Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS socialmedia_platform_compliance (
                    id TEXT PRIMARY KEY, platform_name TEXT NOT NULL, user_base_india INTEGER DEFAULT 0,
                    significant_social_media_intermediary INTEGER DEFAULT 0,
                    data_categories TEXT DEFAULT 'PROFILE,POSTS,MESSAGES,LOCATION,BEHAVIORAL',
                    consent_for_profiling INTEGER DEFAULT 0, consent_for_ad_targeting INTEGER DEFAULT 0,
                    children_account_safeguards INTEGER DEFAULT 1, content_moderation_data_policy TEXT,
                    algorithmic_transparency INTEGER DEFAULT 0, data_portability_supported INTEGER DEFAULT 0,
                    right_to_erasure_supported INTEGER DEFAULT 1, user_data_download_available INTEGER DEFAULT 1,
                    first_originator_traceable INTEGER DEFAULT 0, grievance_officer_appointed INTEGER DEFAULT 0,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS socialmedia_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create SocialMedia tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"IT-SSMI-01", "IT (SSMI) Guidelines 2021", "DPDP-CONSENT,DPDP-BREACH,DPDP-GRIEVANCE"}, {"IT-INTER-21-01", "IT Intermediary Guidelines 2021", "DPDP-SECURITY,DPDP-THIRD-PARTY"}, {"DPDP-SDF-01", "DPDP Significant Data Fiduciary Rules", "DPDP-DPIA,DPDP-DPO"}, {"MEITY-SAFE-01", "MeitY Safe Harbour Provisions", "DPDP-SECURITY,DPDP-BREACH"}, {"CHILDREN-SM-01", "Children Safety on Social Media", "DPDP-CHILDREN,DPDP-CONSENT"}};
        String sql = "INSERT OR IGNORE INTO socialmedia_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM socialmedia_platform_compliance WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantPlatforms", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM socialmedia_platform_compliance WHERE significant_social_media_intermediary = 1"); stats.put("ssmiPlatforms", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM socialmedia_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM socialmedia_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
