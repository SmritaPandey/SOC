package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** E-commerce Sector Compliance Service — DPDP Act compliance for online marketplaces, D2C brands, and retail platforms. */
@Service
public class EcommerceComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(EcommerceComplianceService.class);
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public EcommerceComplianceService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager; this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing E-commerce Compliance Service...");
        createTables(); seedRegulatoryMappings(); initialized = true;
        logger.info("E-commerce Compliance Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ecom_customer_profiling (
                    id TEXT PRIMARY KEY, platform_name TEXT NOT NULL, profiling_type TEXT DEFAULT 'BEHAVIORAL',
                    consent_for_profiling INTEGER DEFAULT 0, consent_for_marketing INTEGER DEFAULT 0,
                    consent_for_cross_selling INTEGER DEFAULT 0, data_categories TEXT DEFAULT 'PURCHASE,BROWSE,PAYMENT',
                    cookies_consent_mechanism TEXT DEFAULT 'OPT_IN', third_party_ad_trackers INTEGER DEFAULT 0,
                    data_retention_policy_days INTEGER DEFAULT 730, purpose_limitation_enforced INTEGER DEFAULT 1,
                    dark_pattern_free INTEGER DEFAULT 1, compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ecom_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create E-commerce tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {
            {"CCPA-ECOM-01", "CCPA E-Commerce Guidelines 2020", "DPDP-CONSENT,DPDP-DISCLOSURE"},
            {"FDI-ECOM-01", "FDI E-Commerce Rules 2020", "DPDP-THIRD-PARTY,DPDP-CONSENT"},
            {"IT-INTER-01", "IT Intermediary Guidelines 2021", "DPDP-BREACH,DPDP-SECURITY"},
            {"ECOM-DARK-01", "Dark Pattern Prevention Guidelines", "DPDP-CONSENT,DPDP-TRANSPARENCY"},
            {"CPA-DATA-01", "Consumer Protection (E-Commerce) Rules", "DPDP-RIGHTS,DPDP-GRIEVANCE"},
        };
        String sql = "INSERT OR IGNORE INTO ecom_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ecom_customer_profiling WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantPlatforms", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM ecom_regulatory_mapping WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM ecom_regulatory_mapping");
            stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }

    public boolean isInitialized() { return initialized; }
}
