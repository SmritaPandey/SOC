package com.qsdpdp.sector;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.*;

/** Pharmaceutical & Life Sciences Compliance — Clinical trial data, pharmacovigilance, drug prescription privacy. */
@Service
public class PharmaComplianceService {
    private static final Logger logger = LoggerFactory.getLogger(PharmaComplianceService.class);
    private final DatabaseManager dbManager; private final AuditService auditService; private boolean initialized = false;
    @Autowired public PharmaComplianceService(DatabaseManager dbManager, AuditService auditService) { this.dbManager = dbManager; this.auditService = auditService; }

    public void initialize() { if (initialized) return; logger.info("Initializing Pharma Compliance Service..."); createTables(); seedRegulatoryMappings(); initialized = true; logger.info("Pharma Compliance Service initialized"); }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pharma_clinical_trial_data (
                    id TEXT PRIMARY KEY, trial_name TEXT NOT NULL, ctri_registration TEXT,
                    participant_data_categories TEXT DEFAULT 'HEALTH,DEMOGRAPHIC,GENETIC',
                    informed_consent_mechanism TEXT DEFAULT 'WRITTEN_ICF', data_anonymisation_level TEXT DEFAULT 'PSEUDONYMISED',
                    adverse_event_reporting_compliant INTEGER DEFAULT 1, data_retention_years INTEGER DEFAULT 15,
                    cross_border_transfer_for_analysis INTEGER DEFAULT 0, ethics_committee_approved INTEGER DEFAULT 1,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pharma_regulatory_mapping (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) { logger.error("Failed to create Pharma tables", e); }
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {{"CDSCO-CT-01", "CDSCO Clinical Trial Data Rules 2019", "DPDP-CONSENT,DPDP-RETENTION"}, {"ICMR-ETHICS-01", "ICMR National Ethical Guidelines", "DPDP-CONSENT,DPDP-SECURITY"}, {"PV-DATA-01", "Pharmacovigilance Data Rules", "DPDP-BREACH,DPDP-RETENTION"}, {"SCHEDULE-Y-01", "Schedule Y Clinical Trial Requirements", "DPDP-CONSENT,DPDP-RIGHTS"}, {"NMC-PRESC-01", "NMC Prescription Data Privacy", "DPDP-CONSENT,DPDP-MINIMISATION"}};
        String sql = "INSERT OR IGNORE INTO pharma_regulatory_mapping (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
        } catch (SQLException e) { logger.error("Seed failed", e); }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pharma_clinical_trial_data WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantTrials", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM pharma_regulatory_mapping WHERE compliance_status = 'COMPLIANT'"); stats.put("compliantRegulations", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM pharma_regulatory_mapping"); stats.put("totalRegulations", rs.next() ? rs.getInt(1) : 0);
        } catch (SQLException e) { logger.error("Stats failed", e); }
        return stats;
    }
    public boolean isInitialized() { return initialized; }
}
