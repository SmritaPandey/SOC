package com.qsdpdp.pii;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * PII Classification Engine — Automated data classification
 * 
 * Classifies discovered PII into sensitivity levels:
 * - CRITICAL: Aadhaar, biometrics, health records, genetic data (DPDP S.2+S.3)
 * - SENSITIVE: PAN, bank account, salary, religion, caste (DPDP S.3(b))
 * - PERSONAL: Name, email, phone, address (DPDP S.2(t))
 * - PUBLIC: Already public information
 * 
 * Auto-tags each finding with DPDP Act section reference,
 * retention period, and recommended access controls.
 * 
 * @version 1.0.0
 * @since Phase 3 — PII Discovery Enhancement
 */
@Service
public class PIIClassificationEngine {

    private static final Logger logger = LoggerFactory.getLogger(PIIClassificationEngine.class);

    @Autowired(required = false)
    private DatabaseManager dbManager;

    private boolean initialized = false;

    // Classification rules
    private static final Map<String, PIIClassification> CLASSIFICATION_RULES = new LinkedHashMap<>();

    static {
        // CRITICAL — highest protection required
        CLASSIFICATION_RULES.put("AADHAAR", new PIIClassification("CRITICAL", "DPDP S.3(b), Aadhaar Act S.29",
                "Biometric-linked national ID", 365, "ENCRYPT_AT_REST + ACCESS_LOG + CONSENT_REQUIRED"));
        CLASSIFICATION_RULES.put("BIOMETRIC", new PIIClassification("CRITICAL", "DPDP S.3(b)",
                "Fingerprint, iris, facial recognition data", 180, "ENCRYPT_AES256 + HARDWARE_TOKEN"));
        CLASSIFICATION_RULES.put("HEALTH_RECORD", new PIIClassification("CRITICAL", "DPDP S.3(b), ABDM",
                "Medical records, diagnoses, prescriptions", 730, "ENCRYPT + CONSENT + AUDIT_ALL_ACCESS"));
        CLASSIFICATION_RULES.put("GENETIC_DATA", new PIIClassification("CRITICAL", "DPDP S.3(b)",
                "DNA, genomic data", 365, "ENCRYPT + CONSENT + NO_CROSS_BORDER"));
        CLASSIFICATION_RULES.put("SEXUAL_ORIENTATION", new PIIClassification("CRITICAL", "DPDP S.3(b)",
                "SPDI category personal data", 180, "ENCRYPT + EXPLICIT_CONSENT"));

        // SENSITIVE — elevated protection
        CLASSIFICATION_RULES.put("PAN", new PIIClassification("SENSITIVE", "IT Act + DPDP",
                "Permanent Account Number (tax ID)", 365, "MASK_DISPLAY + ENCRYPT"));
        CLASSIFICATION_RULES.put("BANK_ACCOUNT", new PIIClassification("SENSITIVE", "DPDP + RBI",
                "Bank account numbers, IFSC", 365, "ENCRYPT + ACCESS_LOG"));
        CLASSIFICATION_RULES.put("CREDIT_CARD", new PIIClassification("SENSITIVE", "PCI-DSS + DPDP",
                "Credit/debit card numbers", 90, "TOKENIZE + PCI_VAULT"));
        CLASSIFICATION_RULES.put("UPI_ID", new PIIClassification("SENSITIVE", "NPCI + DPDP",
                "UPI virtual payment address", 365, "ENCRYPT"));
        CLASSIFICATION_RULES.put("SALARY", new PIIClassification("SENSITIVE", "DPDP S.3",
                "Compensation and salary records", 365, "ENCRYPT + HR_ACCESS_ONLY"));
        CLASSIFICATION_RULES.put("RELIGION", new PIIClassification("SENSITIVE", "DPDP S.3(b)",
                "Religious affiliation data", 180, "EXPLICIT_CONSENT + ENCRYPT"));
        CLASSIFICATION_RULES.put("CASTE", new PIIClassification("SENSITIVE", "DPDP S.3(b)",
                "Caste/tribe information", 180, "EXPLICIT_CONSENT + ENCRYPT"));
        CLASSIFICATION_RULES.put("PASSPORT", new PIIClassification("SENSITIVE", "DPDP + MEA",
                "Passport numbers", 365, "ENCRYPT + MASK_DISPLAY"));
        CLASSIFICATION_RULES.put("VOTER_ID", new PIIClassification("SENSITIVE", "DPDP",
                "Voter ID / EPIC number", 365, "ENCRYPT"));
        CLASSIFICATION_RULES.put("DRIVING_LICENSE", new PIIClassification("SENSITIVE", "DPDP + MoRTH",
                "Driving license numbers", 365, "ENCRYPT"));

        // PERSONAL — standard protection
        CLASSIFICATION_RULES.put("NAME", new PIIClassification("PERSONAL", "DPDP S.2(t)",
                "Full name, first/last name", 365, "CONSENT_REQUIRED"));
        CLASSIFICATION_RULES.put("EMAIL", new PIIClassification("PERSONAL", "DPDP S.2(t)",
                "Email addresses", 365, "CONSENT_REQUIRED"));
        CLASSIFICATION_RULES.put("PHONE", new PIIClassification("PERSONAL", "DPDP S.2(t) + TRAI",
                "Phone numbers, mobile", 365, "CONSENT + DND_CHECK"));
        CLASSIFICATION_RULES.put("ADDRESS", new PIIClassification("PERSONAL", "DPDP S.2(t)",
                "Physical/postal address", 365, "CONSENT_REQUIRED"));
        CLASSIFICATION_RULES.put("DOB", new PIIClassification("PERSONAL", "DPDP S.2(t)",
                "Date of birth", 365, "CONSENT_REQUIRED"));
        CLASSIFICATION_RULES.put("IP_ADDRESS", new PIIClassification("PERSONAL", "DPDP S.2(t)",
                "IP addresses (IPv4/IPv6)", 90, "CONSENT + LOG_ROTATION"));
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing PII Classification Engine ({} rules)...", CLASSIFICATION_RULES.size());
        createTables();
        initialized = true;
    }

    private void createTables() {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pii_classifications (
                    id TEXT PRIMARY KEY,
                    pii_type TEXT NOT NULL,
                    sensitivity TEXT NOT NULL,
                    legal_basis TEXT,
                    description TEXT,
                    data_source TEXT,
                    data_location TEXT,
                    retention_days INTEGER DEFAULT 365,
                    controls TEXT,
                    tags TEXT,
                    classified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    classified_by TEXT DEFAULT 'AUTO'
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS pii_lineage (
                    id TEXT PRIMARY KEY,
                    pii_type TEXT NOT NULL,
                    source_system TEXT,
                    processing_systems TEXT,
                    storage_systems TEXT,
                    sharing_parties TEXT,
                    deletion_policy TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) {
            logger.error("Failed to create PII classification tables", e);
        }
    }

    /**
     * Classify a PII finding
     */
    public Map<String, Object> classify(String piiType, String dataSource, String dataLocation) {
        PIIClassification rule = CLASSIFICATION_RULES.getOrDefault(piiType.toUpperCase(),
                new PIIClassification("PERSONAL", "DPDP S.2(t)", "Unknown PII type", 365, "CONSENT_REQUIRED"));

        String id = UUID.randomUUID().toString();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("piiType", piiType);
        result.put("sensitivity", rule.sensitivity);
        result.put("legalBasis", rule.legalBasis);
        result.put("description", rule.description);
        result.put("retentionDays", rule.retentionDays);
        result.put("controls", rule.controls);
        result.put("dataSource", dataSource);
        result.put("dataLocation", dataLocation);
        result.put("classifiedAt", LocalDateTime.now().toString());

        // Persist
        persistClassification(id, piiType, rule, dataSource, dataLocation);

        return result;
    }

    /**
     * Get classification rules for a PII type
     */
    public PIIClassification getClassificationRule(String piiType) {
        return CLASSIFICATION_RULES.get(piiType.toUpperCase());
    }

    /**
     * Get all classification rules
     */
    public Map<String, Object> getAllRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        for (Map.Entry<String, PIIClassification> e : CLASSIFICATION_RULES.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("sensitivity", e.getValue().sensitivity);
            r.put("legalBasis", e.getValue().legalBasis);
            r.put("description", e.getValue().description);
            r.put("retentionDays", e.getValue().retentionDays);
            r.put("controls", e.getValue().controls);
            rules.put(e.getKey(), r);
        }
        return rules;
    }

    /**
     * Record data lineage for a PII type
     */
    public Map<String, Object> recordLineage(String piiType, String sourceSystem,
            List<String> processingSystems, List<String> storageSystems,
            List<String> sharingParties, String deletionPolicy) {
        String id = UUID.randomUUID().toString();
        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO pii_lineage (id, pii_type, source_system, processing_systems, storage_systems, sharing_parties, deletion_policy) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, id);
                ps.setString(2, piiType);
                ps.setString(3, sourceSystem);
                ps.setString(4, String.join(",", processingSystems));
                ps.setString(5, String.join(",", storageSystems));
                ps.setString(6, String.join(",", sharingParties));
                ps.setString(7, deletionPolicy);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.error("Failed to record lineage", e);
            }
        }
        return Map.of("id", id, "piiType", piiType, "sourceSystem", sourceSystem, "status", "RECORDED");
    }

    /**
     * Get data lineage for a PII type
     */
    public List<Map<String, Object>> getLineage(String piiType) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (dbManager == null || !dbManager.isInitialized()) return result;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM pii_lineage WHERE pii_type = ? ORDER BY created_at DESC")) {
            ps.setString(1, piiType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getString("id"));
                    m.put("piiType", rs.getString("pii_type"));
                    m.put("sourceSystem", rs.getString("source_system"));
                    m.put("processingSystems", rs.getString("processing_systems"));
                    m.put("storageSystems", rs.getString("storage_systems"));
                    m.put("sharingParties", rs.getString("sharing_parties"));
                    m.put("deletionPolicy", rs.getString("deletion_policy"));
                    m.put("createdAt", rs.getString("created_at"));
                    result.add(m);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get lineage", e);
        }
        return result;
    }

    private void persistClassification(String id, String piiType, PIIClassification rule,
                                        String dataSource, String dataLocation) {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO pii_classifications (id, pii_type, sensitivity, legal_basis, description, data_source, data_location, retention_days, controls) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, piiType);
            ps.setString(3, rule.sensitivity);
            ps.setString(4, rule.legalBasis);
            ps.setString(5, rule.description);
            ps.setString(6, dataSource);
            ps.setString(7, dataLocation);
            ps.setInt(8, rule.retentionDays);
            ps.setString(9, rule.controls);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to persist classification: {}", e.getMessage());
        }
    }

    public boolean isInitialized() { return initialized; }

    // ═══════════════════════════════════════════════════════════
    // DTO
    // ═══════════════════════════════════════════════════════════

    public static class PIIClassification {
        public String sensitivity;
        public String legalBasis;
        public String description;
        public int retentionDays;
        public String controls;

        public PIIClassification(String sensitivity, String legalBasis, String description,
                                  int retentionDays, String controls) {
            this.sensitivity = sensitivity;
            this.legalBasis = legalBasis;
            this.description = description;
            this.retentionDays = retentionDays;
            this.controls = controls;
        }
    }
}
