package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Legal & Professional Services Compliance — Law firm client data, attorney–client privilege, case data. */
@Service
public class LegalComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(LegalComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public LegalComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing Legal Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("Legal Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS legal_client_data_compliance (
                    id TEXT PRIMARY KEY, firm_name TEXT NOT NULL, data_categories TEXT DEFAULT 'PII,CASE_DATA,FINANCIAL,PRIVILEGED',
                    attorney_client_privilege_safeguard INTEGER DEFAULT 1, client_consent_mechanism TEXT DEFAULT 'WRITTEN_ENGAGEMENT_LETTER',
                    data_encryption_at_rest INTEGER DEFAULT 1, data_encryption_in_transit INTEGER DEFAULT 1,
                    case_data_retention_years INTEGER DEFAULT 10, regulator_access_documented INTEGER DEFAULT 0,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS legal_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create Legal tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"BCI-DATA-01", "Bar Council Data Ethics Rules", "DPDP-CONSENT,DPDP-SECURITY"}, {"ADV-ACT-01", "Advocates Act Client Data", "DPDP-CONSENT,DPDP-RETENTION"}, {"ECOURTS-01", "e-Courts Data Privacy", "DPDP-SECURITY,DPDP-RIGHTS"}, {"ARB-DATA-01", "Arbitration/Mediation Data Rules", "DPDP-CONSENT,DPDP-RETENTION"}};
        String sql = "INSERT OR IGNORE INTO legal_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM legal_client_data_compliance WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantFirms", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM legal_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM legal_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
