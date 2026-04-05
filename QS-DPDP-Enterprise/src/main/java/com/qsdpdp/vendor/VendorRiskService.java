package com.qsdpdp.vendor;

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
 * Vendor Risk Management Service
 * Manages third-party processor assessments and monitoring
 * 
 * @version 1.0.0
 * @since Module 16
 */
@Service
public class VendorRiskService {

    private static final Logger logger = LoggerFactory.getLogger(VendorRiskService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;

    private boolean initialized = false;

    @Autowired
    public VendorRiskService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing Vendor Risk Service...");
        createTables();

        initialized = true;
        logger.info("Vendor Risk Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS vendors (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            category TEXT,
                            tier TEXT,
                            description TEXT,
                            country TEXT,
                            sector TEXT,
                            contact_name TEXT,
                            contact_email TEXT,
                            contact_phone TEXT,
                            data_processing_agreement INTEGER DEFAULT 0,
                            dpa_expiry_date TIMESTAMP,
                            dpdp_contractual_clauses INTEGER DEFAULT 0,
                            breach_notification_clause INTEGER DEFAULT 0,
                            audit_rights_clause INTEGER DEFAULT 0,
                            deletion_obligation_clause INTEGER DEFAULT 0,
                            status TEXT DEFAULT 'ACTIVE',
                            risk_tier TEXT DEFAULT 'MEDIUM',
                            last_assessment_date TIMESTAMP,
                            next_assessment_due TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS vendor_assessments (
                            id TEXT PRIMARY KEY,
                            vendor_id TEXT NOT NULL,
                            assessment_type TEXT,
                            status TEXT DEFAULT 'PENDING',
                            assessor TEXT,
                            overall_score REAL,
                            risk_rating TEXT,
                            findings_count INTEGER DEFAULT 0,
                            critical_findings INTEGER DEFAULT 0,
                            started_at TIMESTAMP,
                            completed_at TIMESTAMP,
                            next_review_date TIMESTAMP,
                            notes TEXT,
                            FOREIGN KEY (vendor_id) REFERENCES vendors(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS vendor_assessment_responses (
                            id TEXT PRIMARY KEY,
                            assessment_id TEXT NOT NULL,
                            question_category TEXT,
                            question_text TEXT,
                            response TEXT,
                            score INTEGER,
                            max_score INTEGER,
                            evidence_provided INTEGER DEFAULT 0,
                            finding TEXT,
                            FOREIGN KEY (assessment_id) REFERENCES vendor_assessments(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS vendor_data_sharing (
                            id TEXT PRIMARY KEY,
                            vendor_id TEXT NOT NULL,
                            data_category TEXT,
                            data_types TEXT,
                            purpose TEXT,
                            legal_basis TEXT,
                            data_volume TEXT,
                            transfer_frequency TEXT,
                            encryption_required INTEGER DEFAULT 1,
                            cross_border INTEGER DEFAULT 0,
                            destination_country TEXT,
                            active INTEGER DEFAULT 1,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (vendor_id) REFERENCES vendors(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS vendor_incidents (
                            id TEXT PRIMARY KEY,
                            vendor_id TEXT NOT NULL,
                            incident_type TEXT,
                            severity TEXT,
                            description TEXT,
                            impact TEXT,
                            root_cause TEXT,
                            remediation TEXT,
                            status TEXT DEFAULT 'OPEN',
                            reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            resolved_at TIMESTAMP,
                            FOREIGN KEY (vendor_id) REFERENCES vendors(id)
                        )
                    """);

            // TPA Due Diligence (Insurance Sector — Article Recommendation)
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS vendor_tpa_diligence (
                            id TEXT PRIMARY KEY,
                            vendor_id TEXT NOT NULL,
                            tpa_name TEXT NOT NULL,
                            tpa_license_number TEXT,
                            due_diligence_completed INTEGER DEFAULT 0,
                            technical_audit_rights INTEGER DEFAULT 0,
                            real_time_monitoring INTEGER DEFAULT 0,
                            breach_joint_response_plan INTEGER DEFAULT 0,
                            breach_simulation_conducted INTEGER DEFAULT 0,
                            last_simulation_date TIMESTAMP,
                            data_processing_volume TEXT,
                            data_sensitivity_level TEXT,
                            contractual_dpdp_clauses INTEGER DEFAULT 0,
                            risk_rating TEXT DEFAULT 'MEDIUM',
                            last_assessment_date TIMESTAMP,
                            next_review_date TIMESTAMP,
                            status TEXT DEFAULT 'ACTIVE',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (vendor_id) REFERENCES vendors(id)
                        )
                    """);

            // Sector-Specific Assessment Question Templates
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS vendor_sector_questions (
                            id TEXT PRIMARY KEY,
                            sector TEXT NOT NULL,
                            question_category TEXT NOT NULL,
                            question_text TEXT NOT NULL,
                            max_score INTEGER DEFAULT 5,
                            weight REAL DEFAULT 1.0,
                            regulatory_reference TEXT,
                            active INTEGER DEFAULT 1
                        )
                    """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vendor_assess_vendor ON vendor_assessments(vendor_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vendor_data_vendor ON vendor_data_sharing(vendor_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vendor_tpa_vendor ON vendor_tpa_diligence(vendor_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vendor_sector_q ON vendor_sector_questions(sector)");

            seedSectorAssessmentQuestions(stmt);

            logger.info("Vendor Risk tables created");

        } catch (SQLException e) {
            logger.error("Failed to create Vendor Risk tables", e);
        }
    }

    private void seedSectorAssessmentQuestions(Statement stmt) throws SQLException {
        String[][] questions = {
            // BFSI Sector Questions
            {"BFSI", "DATA_LOCALISATION", "Does the vendor store all payment data exclusively in India per RBI mandate?", "RBI-DL"},
            {"BFSI", "AADHAAR_HANDLING", "Does the vendor maintain an isolated Aadhaar data vault with HSM encryption?", "UIDAI-AUTH"},
            {"BFSI", "KYC_RETENTION", "Does the vendor segregate AML/CFT-held KYC data from operational data?", "RBI-KYC"},
            {"BFSI", "BREACH_RESPONSE", "Can the vendor notify within 6 hours (CERT-IN) and 72 hours (DPBI)?", "DPDP-BREACH"},
            // Healthcare Sector Questions
            {"HEALTHCARE", "ABDM_COMPLIANCE", "Is the vendor integrated with ABDM and ABHA-compliant?", "ABDM-HDP"},
            {"HEALTHCARE", "EHR_ACCESS", "Does the vendor enforce treatment-relevant access scope for EHR data?", "ABDM-HIE"},
            {"HEALTHCARE", "DE_IDENTIFICATION", "Does the vendor apply certified de-identification for research data?", "ICMR-BE"},
            {"HEALTHCARE", "TELEMEDICINE", "Does the vendor comply with MoHFW Telemedicine Practice Guidelines?", "MoHFW-TM"},
            // Insurance Sector Questions
            {"INSURANCE", "TPA_GOVERNANCE", "Does the TPA have documented DPDP-compliant processing controls?", "IRDAI-TPA"},
            {"INSURANCE", "CLAIMS_PRIVACY", "Does the vendor limit medical record access to claims-relevant scope?", "IRDAI-CLAIMS"},
            {"INSURANCE", "UNDERWRITING", "Does the vendor enforce data proportionality for underwriting decisions?", "IRDAI-UW"},
            {"INSURANCE", "CROSS_SELLING", "Does the vendor obtain separate consent for cross-selling purposes?", "IRDAI-MKT"}
        };

        String sql = "INSERT OR IGNORE INTO vendor_sector_questions (id, sector, question_category, question_text, regulatory_reference) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(sql)) {
            for (String[] q : questions) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, q[0]); ps.setString(3, q[1]); ps.setString(4, q[2]); ps.setString(5, q[3]);
                ps.executeUpdate();
            }
        }
        logger.info("Vendor sector assessment questions seeded ({} questions)", questions.length);
    }


    // ═══════════════════════════════════════════════════════════
    // VENDOR MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public String createVendor(Vendor vendor) {
        String sql = """
                    INSERT INTO vendors (id, name, category, tier, description, country,
                        contact_name, contact_email, contact_phone, data_processing_agreement,
                        dpa_expiry_date, risk_tier)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vendor.getId());
            stmt.setString(2, vendor.getName());
            stmt.setString(3, vendor.getCategory());
            stmt.setString(4, vendor.getTier());
            stmt.setString(5, vendor.getDescription());
            stmt.setString(6, vendor.getCountry());
            stmt.setString(7, vendor.getContactName());
            stmt.setString(8, vendor.getContactEmail());
            stmt.setString(9, vendor.getContactPhone());
            stmt.setInt(10, vendor.isDataProcessingAgreement() ? 1 : 0);
            stmt.setString(11, vendor.getDpaExpiryDate() != null ? vendor.getDpaExpiryDate().toString() : null);
            stmt.setString(12, vendor.getRiskTier());
            stmt.executeUpdate();

            auditService.log("VENDOR_CREATED", "VENDOR_RISK", null, "Created vendor: " + vendor.getName());
            return vendor.getId();

        } catch (SQLException e) {
            logger.error("Failed to create vendor", e);
            return null;
        }
    }

    public List<Vendor> getVendorsByRisk(String riskTier) {
        List<Vendor> vendors = new ArrayList<>();
        String sql = riskTier != null ? "SELECT * FROM vendors WHERE risk_tier = ? AND status = 'ACTIVE'"
                : "SELECT * FROM vendors WHERE status = 'ACTIVE'";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (riskTier != null)
                stmt.setString(1, riskTier);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                vendors.add(mapVendor(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to get vendors", e);
        }

        return vendors;
    }

    public List<Vendor> getVendorsDueForAssessment() {
        List<Vendor> vendors = new ArrayList<>();
        String sql = "SELECT * FROM vendors WHERE next_assessment_due <= ? AND status = 'ACTIVE'";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().plusDays(30).toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                vendors.add(mapVendor(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to get vendors due for assessment", e);
        }

        return vendors;
    }

    // ═══════════════════════════════════════════════════════════
    // ASSESSMENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public String startAssessment(String vendorId, String assessmentType, String assessor) {
        String assessmentId = UUID.randomUUID().toString();

        String sql = """
                    INSERT INTO vendor_assessments (id, vendor_id, assessment_type, assessor,
                        status, started_at)
                    VALUES (?, ?, ?, ?, 'IN_PROGRESS', ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assessmentId);
            stmt.setString(2, vendorId);
            stmt.setString(3, assessmentType);
            stmt.setString(4, assessor);
            stmt.setString(5, LocalDateTime.now().toString());
            stmt.executeUpdate();

            auditService.log("VENDOR_ASSESSMENT_STARTED", "VENDOR_RISK", assessor,
                    "Started assessment for vendor: " + vendorId);

            return assessmentId;

        } catch (SQLException e) {
            logger.error("Failed to start assessment", e);
            return null;
        }
    }

    public void completeAssessment(String assessmentId, double overallScore,
            String riskRating, int findings, int criticalFindings) {
        String sql = """
                    UPDATE vendor_assessments SET status = 'COMPLETED', overall_score = ?,
                        risk_rating = ?, findings_count = ?, critical_findings = ?, completed_at = ?,
                        next_review_date = ?
                    WHERE id = ?
                """;

        LocalDateTime nextReview = calculateNextReview(riskRating);

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, overallScore);
            stmt.setString(2, riskRating);
            stmt.setInt(3, findings);
            stmt.setInt(4, criticalFindings);
            stmt.setString(5, LocalDateTime.now().toString());
            stmt.setString(6, nextReview.toString());
            stmt.setString(7, assessmentId);
            stmt.executeUpdate();

            // Update vendor's last assessment date
            updateVendorAssessmentDate(assessmentId);

            auditService.log("VENDOR_ASSESSMENT_COMPLETED", "VENDOR_RISK", null,
                    "Completed assessment: " + assessmentId + ", Risk: " + riskRating);

        } catch (SQLException e) {
            logger.error("Failed to complete assessment", e);
        }
    }

    private LocalDateTime calculateNextReview(String riskRating) {
        return switch (riskRating) {
            case "CRITICAL" -> LocalDateTime.now().plusMonths(3);
            case "HIGH" -> LocalDateTime.now().plusMonths(6);
            case "MEDIUM" -> LocalDateTime.now().plusYears(1);
            default -> LocalDateTime.now().plusYears(2);
        };
    }

    private void updateVendorAssessmentDate(String assessmentId) {
        String sql = """
                    UPDATE vendors SET last_assessment_date = ?, next_assessment_due =
                        (SELECT next_review_date FROM vendor_assessments WHERE id = ?)
                    WHERE id = (SELECT vendor_id FROM vendor_assessments WHERE id = ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, assessmentId);
            stmt.setString(3, assessmentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update vendor assessment date", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DATA SHARING MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public void recordDataSharing(String vendorId, DataSharingRecord record) {
        String sql = """
                    INSERT INTO vendor_data_sharing (id, vendor_id, data_category, data_types,
                        purpose, legal_basis, data_volume, transfer_frequency, encryption_required,
                        cross_border, destination_country)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.getId());
            stmt.setString(2, vendorId);
            stmt.setString(3, record.getDataCategory());
            stmt.setString(4, String.join(",", record.getDataTypes()));
            stmt.setString(5, record.getPurpose());
            stmt.setString(6, record.getLegalBasis());
            stmt.setString(7, record.getDataVolume());
            stmt.setString(8, record.getTransferFrequency());
            stmt.setInt(9, record.isEncryptionRequired() ? 1 : 0);
            stmt.setInt(10, record.isCrossBorder() ? 1 : 0);
            stmt.setString(11, record.getDestinationCountry());
            stmt.executeUpdate();

            if (record.isCrossBorder()) {
                auditService.log("CROSS_BORDER_DATA_SHARING", "VENDOR_RISK", null,
                        "Cross-border data sharing to " + record.getDestinationCountry() +
                                " for vendor: " + vendorId);
            }

        } catch (SQLException e) {
            logger.error("Failed to record data sharing", e);
        }
    }

    public List<DataSharingRecord> getCrossBorderTransfers() {
        List<DataSharingRecord> records = new ArrayList<>();
        String sql = """
                    SELECT ds.*, v.name as vendor_name FROM vendor_data_sharing ds
                    JOIN vendors v ON ds.vendor_id = v.id
                    WHERE ds.cross_border = 1 AND ds.active = 1
                """;

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                DataSharingRecord record = new DataSharingRecord();
                record.setId(rs.getString("id"));
                record.setVendorId(rs.getString("vendor_id"));
                record.setVendorName(rs.getString("vendor_name"));
                record.setDataCategory(rs.getString("data_category"));
                record.setPurpose(rs.getString("purpose"));
                record.setDestinationCountry(rs.getString("destination_country"));
                record.setCrossBorder(true);
                records.add(record);
            }
        } catch (SQLException e) {
            logger.error("Failed to get cross-border transfers", e);
        }

        return records;
    }

    // ═══════════════════════════════════════════════════════════
    // INCIDENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public void reportVendorIncident(String vendorId, VendorIncident incident) {
        String sql = """
                    INSERT INTO vendor_incidents (id, vendor_id, incident_type, severity,
                        description, impact, root_cause, remediation, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, incident.getId());
            stmt.setString(2, vendorId);
            stmt.setString(3, incident.getIncidentType());
            stmt.setString(4, incident.getSeverity());
            stmt.setString(5, incident.getDescription());
            stmt.setString(6, incident.getImpact());
            stmt.setString(7, incident.getRootCause());
            stmt.setString(8, incident.getRemediation());
            stmt.executeUpdate();

            auditService.log("VENDOR_INCIDENT_REPORTED", "VENDOR_RISK", null,
                    "Vendor incident: " + incident.getIncidentType() + " - " + incident.getSeverity());

            // Update vendor risk tier if critical incident
            if ("CRITICAL".equals(incident.getSeverity())) {
                updateVendorRiskTier(vendorId, "HIGH");
            }

        } catch (SQLException e) {
            logger.error("Failed to report vendor incident", e);
        }
    }

    private void updateVendorRiskTier(String vendorId, String newTier) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE vendors SET risk_tier = ?, updated_at = ? WHERE id = ?")) {
            stmt.setString(1, newTier);
            stmt.setString(2, LocalDateTime.now().toString());
            stmt.setString(3, vendorId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update vendor risk tier", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public VendorStatistics getStatistics() {
        VendorStatistics stats = new VendorStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vendors WHERE status = 'ACTIVE'");
            if (rs.next())
                stats.setTotalVendors(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM vendors WHERE risk_tier = 'HIGH' OR risk_tier = 'CRITICAL'");
            if (rs.next())
                stats.setHighRiskVendors(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM vendors WHERE next_assessment_due <= datetime('now', '+30 days')");
            if (rs.next())
                stats.setAssessmentsDue(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM vendor_incidents WHERE status = 'OPEN'");
            if (rs.next())
                stats.setOpenIncidents(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM vendor_data_sharing WHERE cross_border = 1 AND active = 1");
            if (rs.next())
                stats.setCrossBorderTransfers(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get vendor statistics", e);
        }

        return stats;
    }

    private Vendor mapVendor(ResultSet rs) throws SQLException {
        Vendor vendor = new Vendor();
        vendor.setId(rs.getString("id"));
        vendor.setName(rs.getString("name"));
        vendor.setCategory(rs.getString("category"));
        vendor.setTier(rs.getString("tier"));
        vendor.setDescription(rs.getString("description"));
        vendor.setCountry(rs.getString("country"));
        vendor.setContactName(rs.getString("contact_name"));
        vendor.setContactEmail(rs.getString("contact_email"));
        vendor.setRiskTier(rs.getString("risk_tier"));
        vendor.setStatus(rs.getString("status"));
        return vendor;
    }

    // ═══════════════════════════════════════════════════════════
    // VENDOR UPDATE / DEACTIVATE (ISO A.5.22-A.5.23)
    // ═══════════════════════════════════════════════════════════

    public boolean updateVendor(String vendorId, Vendor updates) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE vendors SET name=?, category=?, risk_tier=?, country=?, " +
                                "contact_name=?, contact_email=?, description=?, updated_at=CURRENT_TIMESTAMP " +
                                "WHERE id=? AND status='ACTIVE'")) {
            stmt.setString(1, updates.getName());
            stmt.setString(2, updates.getCategory());
            stmt.setString(3, updates.getRiskTier());
            stmt.setString(4, updates.getCountry());
            stmt.setString(5, updates.getContactName());
            stmt.setString(6, updates.getContactEmail());
            stmt.setString(7, updates.getDescription());
            stmt.setString(8, vendorId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                auditService.log("VENDOR_UPDATED", "VENDOR_RISK", null,
                        "Updated vendor: " + vendorId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Failed to update vendor: " + vendorId, e);
        }
        return false;
    }

    public boolean deactivateVendor(String vendorId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE vendors SET status='DEACTIVATED', updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            stmt.setString(1, vendorId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                auditService.log("VENDOR_DEACTIVATED", "VENDOR_RISK", null,
                        "Deactivated vendor (ISO A.5.23 offboarding): " + vendorId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Failed to deactivate vendor: " + vendorId, e);
        }
        return false;
    }

    public List<Map<String, Object>> getAssessmentHistory(String vendorId) {
        List<Map<String, Object>> history = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT * FROM vendor_assessments WHERE vendor_id=? ORDER BY created_at DESC")) {
            stmt.setString(1, vendorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("id", rs.getString("id"));
                a.put("type", rs.getString("assessment_type"));
                a.put("status", rs.getString("status"));
                a.put("assessor", rs.getString("assessor"));
                a.put("score", rs.getInt("score"));
                a.put("createdAt", rs.getString("created_at"));
                a.put("completedAt", rs.getString("completed_at"));
                history.add(a);
            }
        } catch (SQLException e) {
            logger.error("Failed to get assessment history for vendor: " + vendorId, e);
        }
        return history;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class Vendor {
        private String id = UUID.randomUUID().toString();
        private String name;
        private String category;
        private String tier;
        private String description;
        private String country;
        private String contactName;
        private String contactEmail;
        private String contactPhone;
        private boolean dataProcessingAgreement;
        private LocalDateTime dpaExpiryDate;
        private String status = "ACTIVE";
        private String riskTier = "MEDIUM";

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getTier() {
            return tier;
        }

        public void setTier(String tier) {
            this.tier = tier;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String name) {
            this.contactName = name;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public void setContactEmail(String email) {
            this.contactEmail = email;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public void setContactPhone(String phone) {
            this.contactPhone = phone;
        }

        public boolean isDataProcessingAgreement() {
            return dataProcessingAgreement;
        }

        public void setDataProcessingAgreement(boolean dpa) {
            this.dataProcessingAgreement = dpa;
        }

        public LocalDateTime getDpaExpiryDate() {
            return dpaExpiryDate;
        }

        public void setDpaExpiryDate(LocalDateTime date) {
            this.dpaExpiryDate = date;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getRiskTier() {
            return riskTier;
        }

        public void setRiskTier(String tier) {
            this.riskTier = tier;
        }
    }

    public static class DataSharingRecord {
        private String id = UUID.randomUUID().toString();
        private String vendorId;
        private String vendorName;
        private String dataCategory;
        private List<String> dataTypes = new ArrayList<>();
        private String purpose;
        private String legalBasis;
        private String dataVolume;
        private String transferFrequency;
        private boolean encryptionRequired = true;
        private boolean crossBorder;
        private String destinationCountry;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVendorId() {
            return vendorId;
        }

        public void setVendorId(String id) {
            this.vendorId = id;
        }

        public String getVendorName() {
            return vendorName;
        }

        public void setVendorName(String name) {
            this.vendorName = name;
        }

        public String getDataCategory() {
            return dataCategory;
        }

        public void setDataCategory(String cat) {
            this.dataCategory = cat;
        }

        public List<String> getDataTypes() {
            return dataTypes;
        }

        public void setDataTypes(List<String> types) {
            this.dataTypes = types;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public String getLegalBasis() {
            return legalBasis;
        }

        public void setLegalBasis(String basis) {
            this.legalBasis = basis;
        }

        public String getDataVolume() {
            return dataVolume;
        }

        public void setDataVolume(String vol) {
            this.dataVolume = vol;
        }

        public String getTransferFrequency() {
            return transferFrequency;
        }

        public void setTransferFrequency(String freq) {
            this.transferFrequency = freq;
        }

        public boolean isEncryptionRequired() {
            return encryptionRequired;
        }

        public void setEncryptionRequired(boolean req) {
            this.encryptionRequired = req;
        }

        public boolean isCrossBorder() {
            return crossBorder;
        }

        public void setCrossBorder(boolean cb) {
            this.crossBorder = cb;
        }

        public String getDestinationCountry() {
            return destinationCountry;
        }

        public void setDestinationCountry(String country) {
            this.destinationCountry = country;
        }
    }

    public static class VendorIncident {
        private String id = UUID.randomUUID().toString();
        private String incidentType;
        private String severity;
        private String description;
        private String impact;
        private String rootCause;
        private String remediation;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getIncidentType() {
            return incidentType;
        }

        public void setIncidentType(String type) {
            this.incidentType = type;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String sev) {
            this.severity = sev;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String desc) {
            this.description = desc;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String impact) {
            this.impact = impact;
        }

        public String getRootCause() {
            return rootCause;
        }

        public void setRootCause(String cause) {
            this.rootCause = cause;
        }

        public String getRemediation() {
            return remediation;
        }

        public void setRemediation(String rem) {
            this.remediation = rem;
        }
    }

    public static class VendorStatistics {
        private int totalVendors;
        private int highRiskVendors;
        private int assessmentsDue;
        private int openIncidents;
        private int crossBorderTransfers;

        public int getTotalVendors() {
            return totalVendors;
        }

        public void setTotalVendors(int v) {
            this.totalVendors = v;
        }

        public int getHighRiskVendors() {
            return highRiskVendors;
        }

        public void setHighRiskVendors(int v) {
            this.highRiskVendors = v;
        }

        public int getAssessmentsDue() {
            return assessmentsDue;
        }

        public void setAssessmentsDue(int v) {
            this.assessmentsDue = v;
        }

        public int getOpenIncidents() {
            return openIncidents;
        }

        public void setOpenIncidents(int v) {
            this.openIncidents = v;
        }

        public int getCrossBorderTransfers() {
            return crossBorderTransfers;
        }

        public void setCrossBorderTransfers(int v) {
            this.crossBorderTransfers = v;
        }
    }
}
