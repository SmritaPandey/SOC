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
 * Insurance Sector Compliance Service
 * Operationalises DPDP Rules for Insurance companies, TPAs, and intermediaries.
 *
 * Covers: Underwriting data justification, claims processing privacy,
 * TPA risk management, marketing/distribution consent, IRDAI coordination.
 *
 * Regulatory References: IRDAI Cyber Security Guidelines, IRDAI Data Protection
 * Guidelines (2017), DPDP Act 2023 S.6-8, Insurance Act 1938
 *
 * @version 1.0.0
 * @since Phase 7 — Sector Compliance
 */
@Service
public class InsuranceComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(InsuranceComplianceService.class);
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public InsuranceComplianceService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Insurance Compliance Service...");
        createTables();
        seedRegulatoryMappings();
        initialized = true;
        logger.info("Insurance Compliance Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            // Underwriting Data Proportionality
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS insurance_underwriting (
                    id TEXT PRIMARY KEY, policy_type TEXT NOT NULL, data_element TEXT NOT NULL,
                    mapped_purpose TEXT NOT NULL, proportionality_justified INTEGER DEFAULT 0,
                    collection_limitation_enforced INTEGER DEFAULT 0,
                    alternative_data_evaluated INTEGER DEFAULT 0,
                    alternative_data_source TEXT, transparency_provided INTEGER DEFAULT 0,
                    declinature_explanation_capable INTEGER DEFAULT 0,
                    consent_id TEXT, status TEXT DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP)
            """);
            // Claims Processing Privacy
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS insurance_claims_privacy (
                    id TEXT PRIMARY KEY, claim_id TEXT NOT NULL, policy_id TEXT,
                    medical_access_limited INTEGER DEFAULT 0,
                    medical_data_scope TEXT DEFAULT 'CLAIMS_RELEVANT',
                    investigator_guidelines_followed INTEGER DEFAULT 0,
                    fraud_detection_purpose_limited INTEGER DEFAULT 0,
                    third_party_assessor_controlled INTEGER DEFAULT 0,
                    assessor_contract_dpdp_compliant INTEGER DEFAULT 0,
                    settlement_retention_automated INTEGER DEFAULT 0,
                    settlement_deletion_scheduled INTEGER DEFAULT 0,
                    data_categories TEXT, status TEXT DEFAULT 'OPEN',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP)
            """);
            // TPA Due Diligence
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS insurance_tpa_diligence (
                    id TEXT PRIMARY KEY, tpa_name TEXT NOT NULL, tpa_license_number TEXT,
                    due_diligence_completed INTEGER DEFAULT 0,
                    technical_audit_rights_implemented INTEGER DEFAULT 0,
                    real_time_monitoring_feasible INTEGER DEFAULT 0,
                    monitoring_implemented INTEGER DEFAULT 0,
                    breach_response_plan_joint INTEGER DEFAULT 0,
                    breach_simulation_conducted INTEGER DEFAULT 0,
                    last_simulation_date TIMESTAMP,
                    data_processing_volume TEXT, data_sensitivity_level TEXT,
                    contractual_dpdp_clauses INTEGER DEFAULT 0,
                    breach_notification_clause INTEGER DEFAULT 0,
                    audit_rights_clause INTEGER DEFAULT 0,
                    deletion_obligation_clause INTEGER DEFAULT 0,
                    risk_rating TEXT DEFAULT 'MEDIUM',
                    last_assessment_date TIMESTAMP, next_review_date TIMESTAMP,
                    status TEXT DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP)
            """);
            // Marketing & Distribution Consent
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS insurance_marketing_consent (
                    id TEXT PRIMARY KEY, channel TEXT NOT NULL,
                    agent_broker_id TEXT, data_principal_id TEXT,
                    cold_calling_consent_verified INTEGER DEFAULT 0,
                    policy_renewal_consent_current INTEGER DEFAULT 0,
                    cross_selling_consent_obtained INTEGER DEFAULT 0,
                    cross_selling_purpose_specified TEXT,
                    consent_refresh_date TIMESTAMP, consent_expiry_date TIMESTAMP,
                    data_handling_trained INTEGER DEFAULT 0,
                    accountability_documented INTEGER DEFAULT 0,
                    status TEXT DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            """);
            // IRDAI Regulatory Compliance
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS insurance_irdai_compliance (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL,
                    regulation_name TEXT NOT NULL, dpdp_mapping TEXT,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    control_implemented INTEGER DEFAULT 0,
                    evidence_documented INTEGER DEFAULT 0,
                    dual_oversight_prepared INTEGER DEFAULT 0,
                    last_assessment_date TIMESTAMP, next_review_date TIMESTAMP,
                    responsible_officer TEXT, notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ins_uw_policy ON insurance_underwriting(policy_type)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ins_claims_id ON insurance_claims_privacy(claim_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ins_tpa_name ON insurance_tpa_diligence(tpa_name)");
            logger.info("Insurance Compliance tables created");
        } catch (SQLException e) { logger.error("Failed to create Insurance Compliance tables", e); }
    }

    // ═══════ UNDERWRITING DATA JUSTIFICATION ═══════

    public String recordUnderwritingDataElement(UnderwritingRecord record) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO insurance_underwriting (id, policy_type, data_element, mapped_purpose,
                proportionality_justified, collection_limitation_enforced,
                alternative_data_evaluated, alternative_data_source, transparency_provided,
                declinature_explanation_capable, consent_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, record.policyType); ps.setString(3, record.dataElement);
            ps.setString(4, record.mappedPurpose);
            ps.setInt(5, record.proportionalityJustified ? 1 : 0);
            ps.setInt(6, record.collectionLimitationEnforced ? 1 : 0);
            ps.setInt(7, record.alternativeDataEvaluated ? 1 : 0);
            ps.setString(8, record.alternativeDataSource);
            ps.setInt(9, record.transparencyProvided ? 1 : 0);
            ps.setInt(10, record.declinatureExplanationCapable ? 1 : 0);
            ps.setString(11, record.consentId);
            ps.executeUpdate();
            auditService.log("UNDERWRITING_DATA_RECORDED", "INSURANCE_COMPLIANCE", null,
                    "Underwriting data element: " + record.dataElement + " for " + record.policyType);
            return id;
        } catch (SQLException e) { logger.error("Failed to record underwriting data", e); return null; }
    }

    public UnderwritingAssessment assessDataProportionality(String policyType) {
        UnderwritingAssessment assessment = new UnderwritingAssessment();
        assessment.policyType = policyType;
        String sql = "SELECT * FROM insurance_underwriting WHERE policy_type = ? AND status = 'ACTIVE'";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, policyType); ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                assessment.totalElements++;
                if (rs.getInt("proportionality_justified") == 1) assessment.justifiedElements++;
                if (rs.getInt("collection_limitation_enforced") == 1) assessment.limitedElements++;
                if (rs.getInt("alternative_data_evaluated") == 1) assessment.alternativesEvaluated++;
                if (rs.getInt("transparency_provided") == 1) assessment.transparentElements++;
            }
            assessment.proportionalityScore = assessment.totalElements > 0
                    ? (int) ((double) assessment.justifiedElements / assessment.totalElements * 100) : 0;
            assessment.compliant = assessment.proportionalityScore >= 80;
        } catch (SQLException e) { logger.error("Failed to assess data proportionality", e); }
        return assessment;
    }

    // ═══════ TPA RISK MANAGEMENT ═══════

    public String registerTPA(TPARecord record) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO insurance_tpa_diligence (id, tpa_name, tpa_license_number,
                due_diligence_completed, technical_audit_rights_implemented,
                real_time_monitoring_feasible, breach_response_plan_joint,
                data_processing_volume, data_sensitivity_level,
                contractual_dpdp_clauses, breach_notification_clause,
                audit_rights_clause, deletion_obligation_clause)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, record.tpaName); ps.setString(3, record.tpaLicenseNumber);
            ps.setInt(4, record.dueDiligenceCompleted ? 1 : 0);
            ps.setInt(5, record.technicalAuditRightsImplemented ? 1 : 0);
            ps.setInt(6, record.realTimeMonitoringFeasible ? 1 : 0);
            ps.setInt(7, record.breachResponsePlanJoint ? 1 : 0);
            ps.setString(8, record.dataProcessingVolume); ps.setString(9, record.dataSensitivityLevel);
            ps.setInt(10, record.contractualDpdpClauses ? 1 : 0);
            ps.setInt(11, record.breachNotificationClause ? 1 : 0);
            ps.setInt(12, record.auditRightsClause ? 1 : 0);
            ps.setInt(13, record.deletionObligationClause ? 1 : 0);
            ps.executeUpdate();
            auditService.log("TPA_REGISTERED", "INSURANCE_COMPLIANCE", null,
                    "TPA registered: " + record.tpaName);
            return id;
        } catch (SQLException e) { logger.error("Failed to register TPA", e); return null; }
    }

    public TPAAssessment assessTPA(String tpaId) {
        TPAAssessment a = new TPAAssessment(); a.tpaId = tpaId;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM insurance_tpa_diligence WHERE id = ?")) {
            ps.setString(1, tpaId); ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int score = 0;
                a.dueDiligence = rs.getInt("due_diligence_completed") == 1;
                a.auditRights = rs.getInt("technical_audit_rights_implemented") == 1;
                a.monitoring = rs.getInt("monitoring_implemented") == 1;
                a.breachPlan = rs.getInt("breach_response_plan_joint") == 1;
                a.breachSimulated = rs.getInt("breach_simulation_conducted") == 1;
                a.dpdpClauses = rs.getInt("contractual_dpdp_clauses") == 1;
                a.breachClause = rs.getInt("breach_notification_clause") == 1;
                a.auditClause = rs.getInt("audit_rights_clause") == 1;
                a.deletionClause = rs.getInt("deletion_obligation_clause") == 1;
                if (a.dueDiligence) score += 15; if (a.auditRights) score += 15;
                if (a.monitoring) score += 10; if (a.breachPlan) score += 10;
                if (a.breachSimulated) score += 10; if (a.dpdpClauses) score += 15;
                if (a.breachClause) score += 10; if (a.auditClause) score += 5;
                if (a.deletionClause) score += 10;
                a.complianceScore = score; a.compliant = score >= 75;
            }
        } catch (SQLException e) { logger.error("Failed to assess TPA", e); }
        return a;
    }

    // ═══════ STATISTICS ═══════

    public InsuranceComplianceStats getStatistics() {
        InsuranceComplianceStats stats = new InsuranceComplianceStats();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM insurance_underwriting WHERE status = 'ACTIVE'");
            if (rs.next()) stats.totalUnderwritingElements = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM insurance_underwriting WHERE proportionality_justified = 1");
            if (rs.next()) stats.justifiedElements = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM insurance_claims_privacy WHERE status = 'OPEN'");
            if (rs.next()) stats.openClaims = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM insurance_tpa_diligence WHERE status = 'ACTIVE'");
            if (rs.next()) stats.activeTpas = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM insurance_tpa_diligence WHERE due_diligence_completed = 1");
            if (rs.next()) stats.tpaDueDiligenceComplete = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM insurance_irdai_compliance WHERE compliance_status = 'COMPLIANT'");
            if (rs.next()) stats.irdaiCompliantControls = rs.getInt(1);
        } catch (SQLException e) { logger.error("Failed to get insurance statistics", e); }
        return stats;
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {
            {"IRDAI-CS-01", "IRDAI Cyber Security Guidelines", "DPDP-SECURITY,DPDP-BREACH"},
            {"IRDAI-DP-01", "IRDAI Data Protection Guidelines 2017", "DPDP-FIDUCIARY,DPDP-CONSENT"},
            {"IRDAI-TPA-01", "IRDAI TPA Regulations", "DPDP-FIDUCIARY,DPDP-SECURITY"},
            {"IRDAI-CLAIMS-01", "IRDAI Claims Settlement Guidelines", "DPDP-RETENTION,DPDP-RIGHTS-ACCESS"},
            {"IRDAI-UW-01", "IRDAI Underwriting Guidelines", "DPDP-CONSENT,DPDP-RETENTION"},
            {"IRDAI-MKT-01", "IRDAI Marketing & Solicitation Guidelines", "DPDP-CONSENT"},
            {"IRDAI-OUTSRC-01", "IRDAI Outsourcing Guidelines", "DPDP-FIDUCIARY,DPDP-SECURITY"},
            {"IA-1938", "Insurance Act 1938 (as amended)", "DPDP-FIDUCIARY,DPDP-RETENTION"},
            {"IRDAI-AML-01", "IRDAI Anti-Money Laundering Guidelines", "DPDP-RETENTION,DPDP-SECURITY"},
            {"IRDAI-DIG-01", "IRDAI Digital Distribution Guidelines", "DPDP-CONSENT,DPDP-RIGHTS-ACCESS"}
        };
        String sql = "INSERT OR IGNORE INTO insurance_irdai_compliance (id, regulation_code, regulation_name, dpdp_mapping) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]);
                ps.setString(3, r[1]); ps.setString(4, r[2]); ps.executeUpdate(); }
            logger.info("IRDAI regulatory mappings seeded ({} regulations)", regs.length);
        } catch (SQLException e) { logger.error("Failed to seed IRDAI mappings", e); }
    }

    public boolean isInitialized() { return initialized; }

    // ═══════ DATA CLASSES ═══════

    public static class UnderwritingRecord {
        public String policyType, dataElement, mappedPurpose, alternativeDataSource, consentId;
        public boolean proportionalityJustified, collectionLimitationEnforced;
        public boolean alternativeDataEvaluated, transparencyProvided, declinatureExplanationCapable;
    }

    public static class UnderwritingAssessment {
        public String policyType; public int totalElements, justifiedElements, limitedElements;
        public int alternativesEvaluated, transparentElements, proportionalityScore;
        public boolean compliant;
    }

    public static class TPARecord {
        public String tpaName, tpaLicenseNumber, dataProcessingVolume, dataSensitivityLevel;
        public boolean dueDiligenceCompleted, technicalAuditRightsImplemented, realTimeMonitoringFeasible;
        public boolean breachResponsePlanJoint, contractualDpdpClauses, breachNotificationClause;
        public boolean auditRightsClause, deletionObligationClause;
    }

    public static class TPAAssessment {
        public String tpaId;
        public boolean dueDiligence, auditRights, monitoring, breachPlan, breachSimulated;
        public boolean dpdpClauses, breachClause, auditClause, deletionClause;
        public int complianceScore; public boolean compliant;
    }

    public static class InsuranceComplianceStats {
        public int totalUnderwritingElements, justifiedElements, openClaims;
        public int activeTpas, tpaDueDiligenceComplete, irdaiCompliantControls;
    }
}
