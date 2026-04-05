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
 * BFSI Sector Compliance Service
 * Operationalises DPDP Rules for Banking, Financial Services & Insurance sector.
 *
 * Implements recommendations from Vinod Shah's strategic framework:
 *   - Aadhaar Data Vault (UIDAI-mandated isolated architecture)
 *   - UPI/Payment Tokenisation & transaction log immutability
 *   - KYC Data Retention (AML/CFT segregation)
 *   - Credit Information Handling (CIC interaction consent)
 *   - RBI Regulatory Intersection Management
 *
 * Regulatory References:
 *   - RBI Master Direction on IT Governance
 *   - RBI Cyber Security Framework
 *   - RBI Data Localisation Directive
 *   - UIDAI Authentication Circulars
 *   - DPDP Act 2023, Sections 6-8, 16
 *
 * @version 1.0.0
 * @since Phase 7 — Sector Compliance
 */
@Service
public class BFSIComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(BFSIComplianceService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public BFSIComplianceService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing BFSI Compliance Service...");
        createTables();
        seedRegulatoryMappings();
        initialized = true;
        logger.info("BFSI Compliance Service initialized");
    }

    // ═══════════════════════════════════════════════════════════
    // TABLE CREATION
    // ═══════════════════════════════════════════════════════════

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Aadhaar Data Vault Registry
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bfsi_aadhaar_vault (
                    id TEXT PRIMARY KEY,
                    vault_name TEXT NOT NULL,
                    hsm_key_id TEXT,
                    encryption_algorithm TEXT DEFAULT 'AES-256-GCM',
                    network_isolation_verified INTEGER DEFAULT 0,
                    uidai_audit_date TIMESTAMP,
                    uidai_audit_status TEXT DEFAULT 'PENDING',
                    access_log_enabled INTEGER DEFAULT 1,
                    data_count INTEGER DEFAULT 0,
                    fips_certification TEXT,
                    key_escrow_configured INTEGER DEFAULT 0,
                    segregation_of_duties INTEGER DEFAULT 0,
                    last_security_audit TIMESTAMP,
                    status TEXT DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP
                )
            """);

            // UPI/Payment Tokenisation Records
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bfsi_payment_tokenisation (
                    id TEXT PRIMARY KEY,
                    token_type TEXT NOT NULL,
                    card_network TEXT,
                    tokenisation_method TEXT DEFAULT 'DEVICE_BINDING',
                    transaction_log_immutable INTEGER DEFAULT 1,
                    fraud_detection_purpose_limited INTEGER DEFAULT 1,
                    cross_border_restricted INTEGER DEFAULT 1,
                    purpose_limitation_verified INTEGER DEFAULT 0,
                    token_vault_provider TEXT,
                    compliance_status TEXT DEFAULT 'COMPLIANT',
                    last_review_date TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // KYC Data Retention & AML/CFT Segregation
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bfsi_kyc_retention (
                    id TEXT PRIMARY KEY,
                    customer_id TEXT NOT NULL,
                    kyc_type TEXT NOT NULL,
                    data_category TEXT,
                    aml_cft_hold INTEGER DEFAULT 0,
                    regulatory_retention_basis TEXT,
                    retention_start_date TIMESTAMP,
                    regulatory_expiry_date TIMESTAMP,
                    dpdp_deletion_eligible INTEGER DEFAULT 0,
                    segregated_from_operational INTEGER DEFAULT 0,
                    legal_basis_documented INTEGER DEFAULT 0,
                    auto_deletion_scheduled INTEGER DEFAULT 0,
                    deletion_audit_verified INTEGER DEFAULT 0,
                    data_minimised_at_collection INTEGER DEFAULT 0,
                    status TEXT DEFAULT 'RETAINED',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP
                )
            """);

            // Credit Information Handling (CIC Interactions)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bfsi_credit_info (
                    id TEXT PRIMARY KEY,
                    data_principal_id TEXT NOT NULL,
                    cic_name TEXT,
                    consent_for_sharing INTEGER DEFAULT 0,
                    consent_id TEXT,
                    purpose TEXT DEFAULT 'CREDIT_SCORING',
                    purpose_limitation_enforced INTEGER DEFAULT 1,
                    access_right_facilitated INTEGER DEFAULT 0,
                    correction_right_facilitated INTEGER DEFAULT 0,
                    grievance_right_facilitated INTEGER DEFAULT 0,
                    scoring_algorithm_transparent INTEGER DEFAULT 0,
                    data_shared_date TIMESTAMP,
                    consent_expiry_date TIMESTAMP,
                    status TEXT DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // RBI Regulatory Compliance Tracker
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bfsi_rbi_compliance (
                    id TEXT PRIMARY KEY,
                    regulation_code TEXT NOT NULL,
                    regulation_name TEXT NOT NULL,
                    dpdp_mapping TEXT,
                    compliance_status TEXT DEFAULT 'IN_PROGRESS',
                    control_implemented INTEGER DEFAULT 0,
                    evidence_documented INTEGER DEFAULT 0,
                    last_assessment_date TIMESTAMP,
                    next_review_date TIMESTAMP,
                    responsible_officer TEXT,
                    notes TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP
                )
            """);

            // Data Localisation Compliance
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bfsi_data_localisation (
                    id TEXT PRIMARY KEY,
                    data_category TEXT NOT NULL,
                    storage_location TEXT NOT NULL,
                    is_india_hosted INTEGER DEFAULT 1,
                    cloud_provider TEXT,
                    data_centre_city TEXT,
                    rbi_mandate_applicable INTEGER DEFAULT 0,
                    mirror_copy_maintained INTEGER DEFAULT 0,
                    cross_border_transfer_blocked INTEGER DEFAULT 1,
                    encryption_key_localised INTEGER DEFAULT 1,
                    compliance_verified INTEGER DEFAULT 0,
                    last_verification_date TIMESTAMP,
                    status TEXT DEFAULT 'COMPLIANT',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bfsi_kyc_customer ON bfsi_kyc_retention(customer_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_bfsi_credit_principal ON bfsi_credit_info(data_principal_id)");

            logger.info("BFSI Compliance tables created");

        } catch (SQLException e) {
            logger.error("Failed to create BFSI Compliance tables", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // AADHAAR DATA VAULT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public String registerAadhaarVault(AadhaarVaultConfig config) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO bfsi_aadhaar_vault (id, vault_name, hsm_key_id, encryption_algorithm,
                network_isolation_verified, fips_certification, key_escrow_configured,
                segregation_of_duties)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, config.vaultName);
            stmt.setString(3, config.hsmKeyId);
            stmt.setString(4, config.encryptionAlgorithm);
            stmt.setInt(5, config.networkIsolationVerified ? 1 : 0);
            stmt.setString(6, config.fipsCertification);
            stmt.setInt(7, config.keyEscrowConfigured ? 1 : 0);
            stmt.setInt(8, config.segregationOfDuties ? 1 : 0);
            stmt.executeUpdate();

            auditService.log("AADHAAR_VAULT_REGISTERED", "BFSI_COMPLIANCE", null,
                    "Aadhaar vault registered: " + config.vaultName);
            return id;
        } catch (SQLException e) {
            logger.error("Failed to register Aadhaar vault", e);
            return null;
        }
    }

    public AadhaarVaultAssessment assessAadhaarVault(String vaultId) {
        AadhaarVaultAssessment assessment = new AadhaarVaultAssessment();
        assessment.vaultId = vaultId;

        String sql = "SELECT * FROM bfsi_aadhaar_vault WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vaultId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                assessment.hsmDeployed = rs.getString("hsm_key_id") != null;
                assessment.networkIsolated = rs.getInt("network_isolation_verified") == 1;
                assessment.fipsCertified = rs.getString("fips_certification") != null;
                assessment.keyEscrowConfigured = rs.getInt("key_escrow_configured") == 1;
                assessment.segregationOfDuties = rs.getInt("segregation_of_duties") == 1;
                assessment.accessLogging = rs.getInt("access_log_enabled") == 1;
                assessment.uidaiAuditCompliant = "PASSED".equals(rs.getString("uidai_audit_status"));

                int score = 0;
                if (assessment.hsmDeployed) score += 20;
                if (assessment.networkIsolated) score += 20;
                if (assessment.fipsCertified) score += 15;
                if (assessment.keyEscrowConfigured) score += 10;
                if (assessment.segregationOfDuties) score += 10;
                if (assessment.accessLogging) score += 10;
                if (assessment.uidaiAuditCompliant) score += 15;
                assessment.complianceScore = score;
                assessment.compliant = score >= 80;
            }
        } catch (SQLException e) {
            logger.error("Failed to assess Aadhaar vault", e);
        }

        auditService.log("AADHAAR_VAULT_ASSESSED", "BFSI_COMPLIANCE", null,
                "Vault " + vaultId + " assessed — Score: " + assessment.complianceScore);
        return assessment;
    }

    // ═══════════════════════════════════════════════════════════
    // KYC DATA RETENTION & AML/CFT SEGREGATION
    // ═══════════════════════════════════════════════════════════

    public String recordKYCRetention(KYCRetentionRecord record) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO bfsi_kyc_retention (id, customer_id, kyc_type, data_category,
                aml_cft_hold, regulatory_retention_basis, retention_start_date,
                regulatory_expiry_date, segregated_from_operational, legal_basis_documented,
                data_minimised_at_collection)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, record.customerId);
            stmt.setString(3, record.kycType);
            stmt.setString(4, record.dataCategory);
            stmt.setInt(5, record.amlCftHold ? 1 : 0);
            stmt.setString(6, record.regulatoryRetentionBasis);
            stmt.setString(7, record.retentionStartDate != null ? record.retentionStartDate.toString() : null);
            stmt.setString(8, record.regulatoryExpiryDate != null ? record.regulatoryExpiryDate.toString() : null);
            stmt.setInt(9, record.segregatedFromOperational ? 1 : 0);
            stmt.setInt(10, record.legalBasisDocumented ? 1 : 0);
            stmt.setInt(11, record.dataMinimisedAtCollection ? 1 : 0);
            stmt.executeUpdate();

            auditService.log("KYC_RETENTION_RECORDED", "BFSI_COMPLIANCE", null,
                    "KYC retention recorded for customer: " + record.customerId + " type: " + record.kycType);
            return id;
        } catch (SQLException e) {
            logger.error("Failed to record KYC retention", e);
            return null;
        }
    }

    public List<KYCRetentionRecord> getExpiredKYCRecords() {
        List<KYCRetentionRecord> records = new ArrayList<>();
        String sql = """
            SELECT * FROM bfsi_kyc_retention
            WHERE regulatory_expiry_date <= ? AND status = 'RETAINED'
            ORDER BY regulatory_expiry_date ASC
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(mapKYCRetention(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to get expired KYC records", e);
        }

        return records;
    }

    // ═══════════════════════════════════════════════════════════
    // CREDIT INFORMATION HANDLING
    // ═══════════════════════════════════════════════════════════

    public String recordCreditInfoSharing(CreditInfoRecord record) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO bfsi_credit_info (id, data_principal_id, cic_name,
                consent_for_sharing, consent_id, purpose, purpose_limitation_enforced,
                data_shared_date, consent_expiry_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, record.dataPrincipalId);
            stmt.setString(3, record.cicName);
            stmt.setInt(4, record.consentForSharing ? 1 : 0);
            stmt.setString(5, record.consentId);
            stmt.setString(6, record.purpose);
            stmt.setInt(7, record.purposeLimitationEnforced ? 1 : 0);
            stmt.setString(8, record.dataSharedDate != null ? record.dataSharedDate.toString() : null);
            stmt.setString(9, record.consentExpiryDate != null ? record.consentExpiryDate.toString() : null);
            stmt.executeUpdate();

            auditService.log("CREDIT_INFO_SHARED", "BFSI_COMPLIANCE", null,
                    "Credit info shared with CIC: " + record.cicName + " for principal: " + record.dataPrincipalId);
            return id;
        } catch (SQLException e) {
            logger.error("Failed to record credit info sharing", e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DATA LOCALISATION COMPLIANCE
    // ═══════════════════════════════════════════════════════════

    public String recordDataLocalisation(DataLocalisationRecord record) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO bfsi_data_localisation (id, data_category, storage_location,
                is_india_hosted, cloud_provider, data_centre_city, rbi_mandate_applicable,
                mirror_copy_maintained, cross_border_transfer_blocked, encryption_key_localised)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, record.dataCategory);
            stmt.setString(3, record.storageLocation);
            stmt.setInt(4, record.indiaHosted ? 1 : 0);
            stmt.setString(5, record.cloudProvider);
            stmt.setString(6, record.dataCentreCity);
            stmt.setInt(7, record.rbiMandateApplicable ? 1 : 0);
            stmt.setInt(8, record.mirrorCopyMaintained ? 1 : 0);
            stmt.setInt(9, record.crossBorderTransferBlocked ? 1 : 0);
            stmt.setInt(10, record.encryptionKeyLocalised ? 1 : 0);
            stmt.executeUpdate();

            auditService.log("DATA_LOCALISATION_RECORDED", "BFSI_COMPLIANCE", null,
                    "Data localisation: " + record.dataCategory + " at " + record.storageLocation);
            return id;
        } catch (SQLException e) {
            logger.error("Failed to record data localisation", e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public BFSIComplianceStats getStatistics() {
        BFSIComplianceStats stats = new BFSIComplianceStats();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM bfsi_aadhaar_vault WHERE status = 'ACTIVE'");
            if (rs.next()) stats.totalAadhaarVaults = rs.getInt(1);

            rs = stmt.executeQuery("SELECT COUNT(*) FROM bfsi_kyc_retention WHERE status = 'RETAINED'");
            if (rs.next()) stats.totalKycRetentionRecords = rs.getInt(1);

            rs = stmt.executeQuery("SELECT COUNT(*) FROM bfsi_kyc_retention WHERE regulatory_expiry_date <= datetime('now') AND status = 'RETAINED'");
            if (rs.next()) stats.expiredKycRecords = rs.getInt(1);

            rs = stmt.executeQuery("SELECT COUNT(*) FROM bfsi_credit_info WHERE status = 'ACTIVE'");
            if (rs.next()) stats.activeCreditInfoRecords = rs.getInt(1);

            rs = stmt.executeQuery("SELECT COUNT(*) FROM bfsi_data_localisation WHERE compliance_verified = 1");
            if (rs.next()) stats.verifiedLocalisationRecords = rs.getInt(1);

            rs = stmt.executeQuery("SELECT COUNT(*) FROM bfsi_rbi_compliance WHERE compliance_status = 'COMPLIANT'");
            if (rs.next()) stats.rbiCompliantControls = rs.getInt(1);

            rs = stmt.executeQuery("SELECT COUNT(*) FROM bfsi_rbi_compliance");
            if (rs.next()) stats.totalRbiControls = rs.getInt(1);

            rs = stmt.executeQuery("SELECT COUNT(*) FROM bfsi_payment_tokenisation WHERE compliance_status = 'COMPLIANT'");
            if (rs.next()) stats.compliantTokenisationRecords = rs.getInt(1);

        } catch (SQLException e) {
            logger.error("Failed to get BFSI statistics", e);
        }

        return stats;
    }

    // ═══════════════════════════════════════════════════════════
    // REGULATORY SEED DATA
    // ═══════════════════════════════════════════════════════════

    private void seedRegulatoryMappings() {
        String[][] regulations = {
            {"RBI-ITG-01", "RBI Master Direction on IT Governance", "DPDP-FIDUCIARY,DPDP-SECURITY"},
            {"RBI-CSF-01", "RBI Cyber Security Framework", "DPDP-SECURITY,DPDP-BREACH"},
            {"RBI-DL-01", "RBI Data Localisation Directive", "DPDP-CROSSBORDER"},
            {"RBI-KYC-01", "RBI KYC Master Direction", "DPDP-CONSENT,DPDP-RETENTION"},
            {"RBI-NBFC-01", "RBI IT Framework for NBFCs", "DPDP-FIDUCIARY,DPDP-SECURITY"},
            {"UIDAI-AUTH-01", "UIDAI Authentication Circulars", "DPDP-CONSENT,DPDP-SECURITY"},
            {"UIDAI-DP-01", "UIDAI Data Protection Requirements", "DPDP-SECURITY,DPDP-RETENTION"},
            {"RBI-FRAUD-01", "RBI Fraud Monitoring Framework", "DPDP-CONSENT,DPDP-SECURITY"},
            {"RBI-UPI-01", "NPCI UPI Security Guidelines", "DPDP-SECURITY,DPDP-RETENTION"},
            {"RBI-CIC-01", "RBI Credit Information Companies Regulation", "DPDP-CONSENT,DPDP-RIGHTS-ACCESS"}
        };

        String sql = """
            INSERT OR IGNORE INTO bfsi_rbi_compliance (id, regulation_code, regulation_name, dpdp_mapping)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String[] reg : regulations) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, reg[0]);
                stmt.setString(3, reg[1]);
                stmt.setString(4, reg[2]);
                stmt.executeUpdate();
            }
            logger.info("RBI regulatory mappings seeded ({} regulations)", regulations.length);
        } catch (SQLException e) {
            logger.error("Failed to seed RBI regulatory mappings", e);
        }
    }

    private KYCRetentionRecord mapKYCRetention(ResultSet rs) throws SQLException {
        KYCRetentionRecord record = new KYCRetentionRecord();
        record.customerId = rs.getString("customer_id");
        record.kycType = rs.getString("kyc_type");
        record.dataCategory = rs.getString("data_category");
        record.amlCftHold = rs.getInt("aml_cft_hold") == 1;
        record.regulatoryRetentionBasis = rs.getString("regulatory_retention_basis");
        record.segregatedFromOperational = rs.getInt("segregated_from_operational") == 1;
        record.legalBasisDocumented = rs.getInt("legal_basis_documented") == 1;
        record.dataMinimisedAtCollection = rs.getInt("data_minimised_at_collection") == 1;
        return record;
    }

    public boolean isInitialized() { return initialized; }

    // ═══════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class AadhaarVaultConfig {
        public String vaultName;
        public String hsmKeyId;
        public String encryptionAlgorithm = "AES-256-GCM";
        public boolean networkIsolationVerified;
        public String fipsCertification;
        public boolean keyEscrowConfigured;
        public boolean segregationOfDuties;
    }

    public static class AadhaarVaultAssessment {
        public String vaultId;
        public boolean hsmDeployed;
        public boolean networkIsolated;
        public boolean fipsCertified;
        public boolean keyEscrowConfigured;
        public boolean segregationOfDuties;
        public boolean accessLogging;
        public boolean uidaiAuditCompliant;
        public int complianceScore;
        public boolean compliant;
    }

    public static class KYCRetentionRecord {
        public String customerId;
        public String kycType;
        public String dataCategory;
        public boolean amlCftHold;
        public String regulatoryRetentionBasis;
        public LocalDateTime retentionStartDate;
        public LocalDateTime regulatoryExpiryDate;
        public boolean segregatedFromOperational;
        public boolean legalBasisDocumented;
        public boolean dataMinimisedAtCollection;
    }

    public static class CreditInfoRecord {
        public String dataPrincipalId;
        public String cicName;
        public boolean consentForSharing;
        public String consentId;
        public String purpose = "CREDIT_SCORING";
        public boolean purposeLimitationEnforced = true;
        public LocalDateTime dataSharedDate;
        public LocalDateTime consentExpiryDate;
    }

    public static class DataLocalisationRecord {
        public String dataCategory;
        public String storageLocation;
        public boolean indiaHosted = true;
        public String cloudProvider;
        public String dataCentreCity;
        public boolean rbiMandateApplicable;
        public boolean mirrorCopyMaintained;
        public boolean crossBorderTransferBlocked = true;
        public boolean encryptionKeyLocalised = true;
    }

    public static class BFSIComplianceStats {
        public int totalAadhaarVaults;
        public int totalKycRetentionRecords;
        public int expiredKycRecords;
        public int activeCreditInfoRecords;
        public int verifiedLocalisationRecords;
        public int rbiCompliantControls;
        public int totalRbiControls;
        public int compliantTokenisationRecords;
    }
}
