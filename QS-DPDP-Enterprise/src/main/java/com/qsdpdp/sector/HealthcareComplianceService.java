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
 * Healthcare Sector Compliance Service
 * Operationalises DPDP Rules for Hospitals, Health-tech, and ABDM ecosystem.
 *
 * Covers: EHR access governance, ABDM integration, telemedicine compliance,
 * clinical trials data governance, genetic/mental health data safeguards.
 *
 * Regulatory References: ABDM Health Data Management Policy, DPDP Act 2023 S.6-9,
 * Telemedicine Practice Guidelines (MoHFW), ICMR Bioethics Guidelines
 *
 * @version 1.0.0
 * @since Phase 7 — Sector Compliance
 */
@Service
public class HealthcareComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(HealthcareComplianceService.class);
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public HealthcareComplianceService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Healthcare Compliance Service...");
        createTables();
        seedRegulatoryMappings();
        initialized = true;
        logger.info("Healthcare Compliance Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS healthcare_ehr_access (
                    id TEXT PRIMARY KEY, ehr_system_name TEXT NOT NULL, clinician_id TEXT,
                    clinician_role TEXT, patient_id TEXT NOT NULL,
                    access_scope TEXT DEFAULT 'TREATMENT_RELEVANT',
                    data_categories_accessed TEXT, access_justified INTEGER DEFAULT 0,
                    access_purpose TEXT, de_identification_applied INTEGER DEFAULT 0,
                    de_identification_method TEXT, access_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    session_recorded INTEGER DEFAULT 0, anomaly_detected INTEGER DEFAULT 0,
                    status TEXT DEFAULT 'ACTIVE')
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS healthcare_abdm_compliance (
                    id TEXT PRIMARY KEY, facility_id TEXT NOT NULL, facility_name TEXT,
                    abha_number_protected INTEGER DEFAULT 0, hie_consent_aligned INTEGER DEFAULT 0,
                    interoperability_standard TEXT, privacy_preserving_design INTEGER DEFAULT 0,
                    facility_registry_integrated INTEGER DEFAULT 0, health_locker_compatible INTEGER DEFAULT 0,
                    consent_artefact_supported INTEGER DEFAULT 0, data_localisation_compliant INTEGER DEFAULT 1,
                    compliance_score INTEGER DEFAULT 0, last_assessment_date TIMESTAMP,
                    status TEXT DEFAULT 'PENDING', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP)
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS healthcare_telemedicine (
                    id TEXT PRIMARY KEY, platform_name TEXT NOT NULL,
                    video_recording_compliant INTEGER DEFAULT 0, recording_storage_encrypted INTEGER DEFAULT 0,
                    recording_retention_policy TEXT, prescription_data_safeguarded INTEGER DEFAULT 0,
                    pharmacy_integration_controlled INTEGER DEFAULT 0,
                    wearable_data_purpose_specified INTEGER DEFAULT 0, wearable_data_categories TEXT,
                    ai_diagnostic_anonymised INTEGER DEFAULT 0, ai_training_data_verified INTEGER DEFAULT 0,
                    patient_consent_digital INTEGER DEFAULT 0, compliance_status TEXT DEFAULT 'IN_REVIEW',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP)
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS healthcare_clinical_trials (
                    id TEXT PRIMARY KEY, trial_id TEXT NOT NULL, trial_name TEXT, sponsor TEXT,
                    informed_consent_digital INTEGER DEFAULT 0, withdrawal_mechanism_implemented INTEGER DEFAULT 0,
                    biobank_governance_established INTEGER DEFAULT 0, multi_site_dsa_in_place INTEGER DEFAULT 0,
                    publication_anonymisation_verified INTEGER DEFAULT 0,
                    genetic_data_special_handling INTEGER DEFAULT 0, ethics_committee_approved INTEGER DEFAULT 0,
                    icmr_guidelines_followed INTEGER DEFAULT 0, data_principal_count INTEGER DEFAULT 0,
                    data_categories TEXT, retention_period TEXT, status TEXT DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP)
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS healthcare_special_data (
                    id TEXT PRIMARY KEY, data_type TEXT NOT NULL, data_principal_id TEXT,
                    enhanced_consent_obtained INTEGER DEFAULT 0, consent_id TEXT,
                    additional_safeguards TEXT, access_restricted_to TEXT,
                    encryption_level TEXT DEFAULT 'AES-256', purpose_strictly_limited INTEGER DEFAULT 1,
                    discrimination_risk_assessed INTEGER DEFAULT 0, anonymisation_technique TEXT,
                    retention_justification TEXT, status TEXT DEFAULT 'PROTECTED',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS healthcare_regulatory (
                    id TEXT PRIMARY KEY, regulation_code TEXT NOT NULL, regulation_name TEXT NOT NULL,
                    regulator TEXT, dpdp_mapping TEXT, compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    control_implemented INTEGER DEFAULT 0, evidence_documented INTEGER DEFAULT 0,
                    last_assessment_date TIMESTAMP, next_review_date TIMESTAMP,
                    responsible_officer TEXT, notes TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
            """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_hc_ehr_patient ON healthcare_ehr_access(patient_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_hc_abdm_facility ON healthcare_abdm_compliance(facility_id)");
            logger.info("Healthcare Compliance tables created");
        } catch (SQLException e) {
            logger.error("Failed to create Healthcare Compliance tables", e);
        }
    }

    // ═══════ EHR ACCESS GOVERNANCE ═══════

    public String recordEHRAccess(EHRAccessRecord record) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO healthcare_ehr_access (id, ehr_system_name, clinician_id, clinician_role,
                patient_id, access_scope, data_categories_accessed, access_justified,
                access_purpose, de_identification_applied, de_identification_method)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, record.ehrSystemName);
            ps.setString(3, record.clinicianId); ps.setString(4, record.clinicianRole);
            ps.setString(5, record.patientId); ps.setString(6, record.accessScope);
            ps.setString(7, record.dataCategoriesAccessed != null ? String.join(",", record.dataCategoriesAccessed) : null);
            ps.setInt(8, record.accessJustified ? 1 : 0); ps.setString(9, record.accessPurpose);
            ps.setInt(10, record.deIdentificationApplied ? 1 : 0); ps.setString(11, record.deIdentificationMethod);
            ps.executeUpdate();
            auditService.log("EHR_ACCESS_RECORDED", "HEALTHCARE_COMPLIANCE", record.clinicianId,
                    "EHR access: clinician=" + record.clinicianId + " patient=" + record.patientId);
            return id;
        } catch (SQLException e) { logger.error("Failed to record EHR access", e); return null; }
    }

    // ═══════ ABDM INTEGRATION ═══════

    public String registerFacilityCompliance(ABDMFacilityRecord record) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO healthcare_abdm_compliance (id, facility_id, facility_name,
                abha_number_protected, hie_consent_aligned, interoperability_standard,
                privacy_preserving_design, facility_registry_integrated,
                health_locker_compatible, consent_artefact_supported)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, record.facilityId); ps.setString(3, record.facilityName);
            ps.setInt(4, record.abhaNumberProtected ? 1 : 0); ps.setInt(5, record.hieConsentAligned ? 1 : 0);
            ps.setString(6, record.interoperabilityStandard);
            ps.setInt(7, record.privacyPreservingDesign ? 1 : 0);
            ps.setInt(8, record.facilityRegistryIntegrated ? 1 : 0);
            ps.setInt(9, record.healthLockerCompatible ? 1 : 0);
            ps.setInt(10, record.consentArtefactSupported ? 1 : 0);
            ps.executeUpdate();
            auditService.log("ABDM_FACILITY_REGISTERED", "HEALTHCARE_COMPLIANCE", null,
                    "ABDM compliance registered for: " + record.facilityName);
            return id;
        } catch (SQLException e) { logger.error("Failed to register ABDM facility", e); return null; }
    }

    public ABDMAssessment assessABDMCompliance(String facilityId) {
        ABDMAssessment a = new ABDMAssessment(); a.facilityId = facilityId;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM healthcare_abdm_compliance WHERE facility_id = ?")) {
            ps.setString(1, facilityId); ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                a.abhaProtected = rs.getInt("abha_number_protected") == 1;
                a.hieConsentAligned = rs.getInt("hie_consent_aligned") == 1;
                a.privacyPreserving = rs.getInt("privacy_preserving_design") == 1;
                a.facilityRegistered = rs.getInt("facility_registry_integrated") == 1;
                a.healthLockerCompatible = rs.getInt("health_locker_compatible") == 1;
                a.consentArtefact = rs.getInt("consent_artefact_supported") == 1;
                a.dataLocalised = rs.getInt("data_localisation_compliant") == 1;
                int score = 0;
                if (a.abhaProtected) score += 20; if (a.hieConsentAligned) score += 20;
                if (a.privacyPreserving) score += 15; if (a.facilityRegistered) score += 10;
                if (a.healthLockerCompatible) score += 10; if (a.consentArtefact) score += 15;
                if (a.dataLocalised) score += 10;
                a.complianceScore = score; a.compliant = score >= 75;
            }
        } catch (SQLException e) { logger.error("Failed to assess ABDM compliance", e); }
        return a;
    }

    // ═══════ CLINICAL TRIALS ═══════

    public String registerClinicalTrial(ClinicalTrialRecord record) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO healthcare_clinical_trials (id, trial_id, trial_name, sponsor,
                informed_consent_digital, withdrawal_mechanism_implemented,
                biobank_governance_established, multi_site_dsa_in_place,
                genetic_data_special_handling, ethics_committee_approved,
                icmr_guidelines_followed, data_principal_count, data_categories)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, record.trialId); ps.setString(3, record.trialName);
            ps.setString(4, record.sponsor); ps.setInt(5, record.informedConsentDigital ? 1 : 0);
            ps.setInt(6, record.withdrawalMechanismImplemented ? 1 : 0);
            ps.setInt(7, record.biobankGovernanceEstablished ? 1 : 0);
            ps.setInt(8, record.multiSiteDsaInPlace ? 1 : 0);
            ps.setInt(9, record.geneticDataSpecialHandling ? 1 : 0);
            ps.setInt(10, record.ethicsCommitteeApproved ? 1 : 0);
            ps.setInt(11, record.icmrGuidelinesFollowed ? 1 : 0);
            ps.setInt(12, record.dataPrincipalCount);
            ps.setString(13, record.dataCategories != null ? String.join(",", record.dataCategories) : null);
            ps.executeUpdate();
            auditService.log("CLINICAL_TRIAL_REGISTERED", "HEALTHCARE_COMPLIANCE", null,
                    "Clinical trial registered: " + record.trialName);
            return id;
        } catch (SQLException e) { logger.error("Failed to register clinical trial", e); return null; }
    }

    // ═══════ STATISTICS ═══════

    public HealthcareComplianceStats getStatistics() {
        HealthcareComplianceStats stats = new HealthcareComplianceStats();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM healthcare_ehr_access");
            if (rs.next()) stats.totalEhrAccessRecords = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM healthcare_ehr_access WHERE anomaly_detected = 1");
            if (rs.next()) stats.anomalousAccessCount = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM healthcare_abdm_compliance WHERE status = 'COMPLIANT'");
            if (rs.next()) stats.compliantFacilities = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM healthcare_clinical_trials WHERE status = 'ACTIVE'");
            if (rs.next()) stats.activeTrials = rs.getInt(1);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM healthcare_special_data WHERE status = 'PROTECTED'");
            if (rs.next()) stats.specialDataRecords = rs.getInt(1);
        } catch (SQLException e) { logger.error("Failed to get healthcare statistics", e); }
        return stats;
    }

    private void seedRegulatoryMappings() {
        String[][] regs = {
            {"ABDM-HDP-01", "ABDM Health Data Management Policy", "MoHFW/NHA", "DPDP-CONSENT,DPDP-SECURITY"},
            {"ABDM-HIE-01", "ABDM Health Information Exchange Standards", "NHA", "DPDP-CONSENT,DPDP-CROSSBORDER"},
            {"ABDM-ABHA-01", "ABHA Number Protection Guidelines", "NHA", "DPDP-SECURITY,DPDP-RETENTION"},
            {"MoHFW-TM-01", "Telemedicine Practice Guidelines", "MoHFW", "DPDP-CONSENT,DPDP-SECURITY"},
            {"ICMR-BE-01", "ICMR Bioethics Guidelines", "ICMR", "DPDP-CONSENT,DPDP-CHILDREN"},
            {"ICMR-CT-01", "ICMR Clinical Trial Guidelines", "ICMR", "DPDP-CONSENT,DPDP-RIGHTS-ACCESS"},
            {"CEA-01", "Clinical Establishments Act", "MoHFW", "DPDP-FIDUCIARY,DPDP-RETENTION"},
            {"PCPNDT-01", "PCPNDT Act (Genetic Data)", "MoHFW", "DPDP-SECURITY,DPDP-CONSENT"},
            {"MHA-01", "Mental Healthcare Act 2017", "MoHFW", "DPDP-CONSENT,DPDP-CHILDREN"},
            {"NMC-01", "National Medical Commission Guidelines", "NMC", "DPDP-FIDUCIARY,DPDP-SECURITY"}
        };
        String sql = "INSERT OR IGNORE INTO healthcare_regulatory (id, regulation_code, regulation_name, regulator, dpdp_mapping) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] r : regs) { ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, r[0]);
                ps.setString(3, r[1]); ps.setString(4, r[2]); ps.setString(5, r[3]); ps.executeUpdate(); }
            logger.info("Healthcare regulatory mappings seeded ({} regulations)", regs.length);
        } catch (SQLException e) { logger.error("Failed to seed healthcare regulatory mappings", e); }
    }

    public boolean isInitialized() { return initialized; }

    // ═══════ DATA CLASSES ═══════

    public static class EHRAccessRecord {
        public String ehrSystemName, clinicianId, clinicianRole, patientId;
        public String accessScope = "TREATMENT_RELEVANT";
        public List<String> dataCategoriesAccessed;
        public boolean accessJustified, deIdentificationApplied;
        public String accessPurpose, deIdentificationMethod;
    }

    public static class ABDMFacilityRecord {
        public String facilityId, facilityName, interoperabilityStandard;
        public boolean abhaNumberProtected, hieConsentAligned, privacyPreservingDesign;
        public boolean facilityRegistryIntegrated, healthLockerCompatible, consentArtefactSupported;
    }

    public static class ABDMAssessment {
        public String facilityId;
        public boolean abhaProtected, hieConsentAligned, privacyPreserving;
        public boolean facilityRegistered, healthLockerCompatible, consentArtefact, dataLocalised;
        public int complianceScore; public boolean compliant;
    }

    public static class ClinicalTrialRecord {
        public String trialId, trialName, sponsor;
        public boolean informedConsentDigital, withdrawalMechanismImplemented;
        public boolean biobankGovernanceEstablished, multiSiteDsaInPlace;
        public boolean geneticDataSpecialHandling, ethicsCommitteeApproved, icmrGuidelinesFollowed;
        public int dataPrincipalCount; public List<String> dataCategories;
    }

    public static class HealthcareComplianceStats {
        public int totalEhrAccessRecords, anomalousAccessCount, compliantFacilities;
        public int activeTrials, specialDataRecords;
    }
}
