package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Fintech/Digital Lending Sector Compliance Service
 * Operationalises DPDP Rules for Digital Lending, UPI aggregators,
 * Payment Wallets, and Neo-banking platforms.
 *
 * Regulatory References:
 *   - RBI Digital Lending Guidelines 2022
 *   - RBI KYC Master Direction (Digital KYC)
 *   - RBI Guidelines on Account Aggregator Framework
 *   - DPDP Act 2023, Sections 6-8, 16
 *
 * @version 1.0.0
 * @since Phase 7 — Sector Compliance
 */
@Service
public class FintechComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(FintechComplianceService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public FintechComplianceService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Fintech Compliance Service...");
        createTables();
        seedRegulatoryMappings();
        initialized = true;
        logger.info("Fintech Compliance Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS fintech_lending_compliance (
                    id TEXT PRIMARY KEY,
                    platform_name TEXT NOT NULL,
                    platform_type TEXT DEFAULT 'DIGITAL_LENDING',
                    rbi_registration_number TEXT,
                    lsp_partner TEXT,
                    data_collection_scope TEXT,
                    consent_mechanism TEXT DEFAULT 'EXPLICIT',
                    data_stored_on_device INTEGER DEFAULT 0,
                    third_party_sharing_disclosed INTEGER DEFAULT 0,
                    grievance_mechanism_active INTEGER DEFAULT 1,
                    interest_rate_disclosed INTEGER DEFAULT 1,
                    data_deletion_on_repayment INTEGER DEFAULT 0,
                    purpose_limitation_enforced INTEGER DEFAULT 1,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    last_audit_date TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS fintech_api_banking_consent (
                    id TEXT PRIMARY KEY,
                    aggregator_name TEXT NOT NULL,
                    fip_name TEXT NOT NULL,
                    fiu_name TEXT NOT NULL,
                    consent_artifact_id TEXT,
                    data_categories TEXT,
                    purpose TEXT,
                    consent_mode TEXT DEFAULT 'VIEW',
                    fetch_type TEXT DEFAULT 'PERIODIC',
                    consent_status TEXT DEFAULT 'ACTIVE',
                    data_life_days INTEGER DEFAULT 365,
                    frequency TEXT DEFAULT 'MONTHLY',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS fintech_regulatory_mapping (
                    id TEXT PRIMARY KEY,
                    regulation_code TEXT NOT NULL,
                    regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    control_implemented INTEGER DEFAULT 0,
                    evidence_documented INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            logger.info("Fintech Compliance tables created");
        } catch (SQLException e) {
            logger.error("Failed to create Fintech Compliance tables", e);
        }
    }

    private void seedRegulatoryMappings() {
        String[][] regulations = {
            {"RBI-DL-2022-01", "RBI Digital Lending Guidelines 2022", "DPDP-CONSENT,DPDP-DISCLOSURE"},
            {"RBI-AA-01", "Account Aggregator Framework", "DPDP-CONSENT,DPDP-PURPOSE-LIMITATION"},
            {"RBI-KYC-DIG-01", "Digital KYC Direction", "DPDP-CONSENT,DPDP-RETENTION"},
            {"RBI-NBFC-P2P-01", "P2P Lending Platform Rules", "DPDP-CONSENT,DPDP-DISCLOSURE"},
            {"RBI-UPI-SEC-01", "UPI Security Guidelines", "DPDP-SECURITY,DPDP-BREACH"},
            {"RBI-WALLET-01", "PPI/Wallet Master Direction", "DPDP-CONSENT,DPDP-RETENTION"},
            {"RBI-LSP-01", "Lending Service Provider Guidelines", "DPDP-CONSENT,DPDP-THIRD-PARTY"},
        };

        String sql = "INSERT OR IGNORE INTO fintech_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String[] reg : regulations) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, reg[0]);
                stmt.setString(3, reg[1]);
                stmt.setString(4, reg[2]);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to seed Fintech regulatory mappings", e);
        }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM fintech_lending_compliance WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantPlatforms", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM fintech_api_banking_consent WHERE consent_status = 'ACTIVE'");
            stats.put("activeAggregatorConsents", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM fintech_regulatory_mapping WHERE compliance_status = 'COMPLIANT'");
            stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM fintech_regulatory_mapping");
            stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) {
            logger.error("Failed to get Fintech statistics", e);
        }
        return stats;
    }

    public boolean isInitialized() { return initialized; }
}
