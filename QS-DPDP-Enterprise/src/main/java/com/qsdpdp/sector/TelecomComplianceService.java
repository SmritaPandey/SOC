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
 * Telecom Sector Compliance Service
 * Operationalises DPDP Rules for Telecom operators, ISPs, and VAS providers.
 *
 * Regulatory References:
 *   - TRAI Regulations on Data Privacy
 *   - DoT License Conditions (UL/ISP)
 *   - TRAI SMS/Call Data Retention Rules
 *   - DPDP Act 2023, Sections 6-8, 16
 *
 * @version 1.0.0
 * @since Phase 7 — Sector Compliance
 */
@Service
public class TelecomComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(TelecomComplianceService.class);
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public TelecomComplianceService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Telecom Compliance Service...");
        createTables();
        seedRegulatoryMappings();
        initialized = true;
        logger.info("Telecom Compliance Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS telecom_subscriber_compliance (
                    id TEXT PRIMARY KEY,
                    operator_name TEXT NOT NULL,
                    subscriber_data_category TEXT DEFAULT 'CDR,LOCATION,KYC',
                    cdr_retention_days INTEGER DEFAULT 730,
                    location_data_anonymised INTEGER DEFAULT 0,
                    kyc_data_secured INTEGER DEFAULT 1,
                    consent_for_vas INTEGER DEFAULT 0,
                    consent_for_marketing INTEGER DEFAULT 0,
                    dnc_registry_integrated INTEGER DEFAULT 1,
                    lawful_interception_isolated INTEGER DEFAULT 1,
                    data_localisation_compliant INTEGER DEFAULT 1,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS telecom_regulatory_mapping (
                    id TEXT PRIMARY KEY,
                    regulation_code TEXT NOT NULL,
                    regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            logger.info("Telecom Compliance tables created");
        } catch (SQLException e) {
            logger.error("Failed to create Telecom Compliance tables", e);
        }
    }

    private void seedRegulatoryMappings() {
        String[][] regulations = {
            {"TRAI-DP-01", "TRAI Data Privacy Regulations", "DPDP-CONSENT,DPDP-PURPOSE"},
            {"DOT-UL-01", "DoT Unified License Conditions", "DPDP-SECURITY,DPDP-RETENTION"},
            {"TRAI-DNC-01", "DNC Registry Regulations", "DPDP-CONSENT,DPDP-WITHDRAWAL"},
            {"DOT-LI-01", "Lawful Interception Rules", "DPDP-LEGITIMATE-USE"},
            {"TRAI-CDR-01", "CDR Retention Guidelines", "DPDP-RETENTION,DPDP-MINIMISATION"},
            {"DOT-KYC-01", "Telecom KYC Requirements", "DPDP-CONSENT,DPDP-RETENTION"},
        };
        String sql = "INSERT OR IGNORE INTO telecom_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] reg : regulations) {
                ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, reg[0]); ps.setString(3, reg[1]); ps.setString(4, reg[2]);
                ps.executeUpdate();
            }
        } catch (SQLException e) { logger.error("Failed to seed Telecom regulatory mappings", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM telecom_subscriber_compliance WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantOperators", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM telecom_regulatory_mapping WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM telecom_regulatory_mapping");
            stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Failed to get Telecom statistics", e); }
        return stats;
    }

    public boolean isInitialized() { return initialized; }
}
