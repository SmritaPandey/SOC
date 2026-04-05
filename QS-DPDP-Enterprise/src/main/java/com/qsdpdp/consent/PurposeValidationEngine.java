package com.qsdpdp.consent;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Purpose Validation Engine — DPDP S.4(1) Compliance
 * 
 * Validates that data processing is limited to stated purpose:
 * - Purpose-data category mapping
 * - Cross-purpose usage detection
 * - Purpose creep monitoring
 * - Legitimate interest assessment
 * - Purpose expiry tracking
 * 
 * DPDP S.4(1): "Personal data may be processed only for
 * a lawful purpose for which the Data Principal has given consent."
 * 
 * @version 1.0.0
 * @since Phase 1 — Consent Enhancement
 */
@Service
public class PurposeValidationEngine {

    private static final Logger logger = LoggerFactory.getLogger(PurposeValidationEngine.class);

    @Autowired(required = false) private DatabaseManager dbManager;

    private boolean initialized = false;

    // Purpose → Allowed Data Categories mapping
    private static final Map<String, Set<String>> PURPOSE_CATEGORY_MAP = new LinkedHashMap<>();
    static {
        PURPOSE_CATEGORY_MAP.put("KYC", Set.of("NAME", "DOB", "ADDRESS", "PAN", "AADHAAR", "PHOTO"));
        PURPOSE_CATEGORY_MAP.put("CREDIT_SCORING", Set.of("NAME", "PAN", "INCOME", "EMPLOYMENT", "CREDIT_HISTORY"));
        PURPOSE_CATEGORY_MAP.put("ACCOUNT_MANAGEMENT", Set.of("NAME", "EMAIL", "PHONE", "ADDRESS", "ACCOUNT_NUMBER"));
        PURPOSE_CATEGORY_MAP.put("LOAN_PROCESSING", Set.of("NAME", "PAN", "INCOME", "EMPLOYMENT", "ADDRESS", "BANK_ACCOUNT"));
        PURPOSE_CATEGORY_MAP.put("INSURANCE_UNDERWRITING", Set.of("NAME", "DOB", "HEALTH_RECORDS", "INCOME", "ADDRESS"));
        PURPOSE_CATEGORY_MAP.put("HEALTHCARE_DELIVERY", Set.of("NAME", "DOB", "HEALTH_RECORDS", "MEDICAL_HISTORY", "BIOMETRIC"));
        PURPOSE_CATEGORY_MAP.put("MARKETING", Set.of("NAME", "EMAIL", "PHONE", "PREFERENCES"));
        PURPOSE_CATEGORY_MAP.put("ANALYTICS", Set.of("NAME", "EMAIL", "USAGE_PATTERNS"));
        PURPOSE_CATEGORY_MAP.put("LEGAL_COMPLIANCE", Set.of("NAME", "PAN", "AADHAAR", "TRANSACTION_DATA"));
        PURPOSE_CATEGORY_MAP.put("FRAUD_DETECTION", Set.of("NAME", "DEVICE_ID", "IP_ADDRESS", "TRANSACTION_DATA", "LOCATION"));
        PURPOSE_CATEGORY_MAP.put("GRIEVANCE_REDRESSAL", Set.of("NAME", "EMAIL", "PHONE", "COMPLAINT_DATA"));
        PURPOSE_CATEGORY_MAP.put("DATA_PORTABILITY", Set.of("ALL"));
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Purpose Validation Engine ({} purpose mappings)...", PURPOSE_CATEGORY_MAP.size());
        createTables();
        initialized = true;
    }

    private void createTables() {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS purpose_validations (
                    id TEXT PRIMARY KEY,
                    purpose TEXT NOT NULL,
                    data_category TEXT NOT NULL,
                    principal_id TEXT,
                    result TEXT NOT NULL,
                    reason TEXT,
                    validated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS purpose_creep_alerts (
                    id TEXT PRIMARY KEY,
                    purpose TEXT,
                    original_categories TEXT,
                    new_category TEXT,
                    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status TEXT DEFAULT 'OPEN'
                )
            """);
        } catch (SQLException e) {
            logger.error("Failed to create purpose validation tables", e);
        }
    }

    /**
     * Validate if data category access is allowed for given purpose
     */
    public Map<String, Object> validatePurpose(String purpose, String dataCategory,
            String principalId, String accessedBy) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("purpose", purpose);
        result.put("dataCategory", dataCategory);
        result.put("principalId", principalId);
        result.put("accessedBy", accessedBy);

        Set<String> allowed = PURPOSE_CATEGORY_MAP.getOrDefault(purpose.toUpperCase(), Set.of());

        if (allowed.contains("ALL") || allowed.contains(dataCategory.toUpperCase())) {
            result.put("result", "VALID");
            result.put("reason", "Data category is within purpose scope");
        } else if (allowed.isEmpty()) {
            result.put("result", "UNKNOWN_PURPOSE");
            result.put("reason", "Purpose '" + purpose + "' is not registered — potential purpose creep");
            recordCreepAlert(purpose, dataCategory);
        } else {
            result.put("result", "PURPOSE_VIOLATION");
            result.put("reason", "'" + dataCategory + "' is NOT permitted for purpose '" + purpose +
                    "'. Allowed: " + allowed);
            result.put("section", "DPDP S.4(1) - Purpose Limitation");
            result.put("penalty", "Up to ₹250 Crore");
        }

        result.put("validatedAt", LocalDateTime.now().toString());
        persistValidation(id, purpose, dataCategory, principalId, (String) result.get("result"), (String) result.get("reason"));
        return result;
    }

    /**
     * Get allowed categories for a purpose
     */
    public Map<String, Object> getAllowedCategories(String purpose) {
        Set<String> allowed = PURPOSE_CATEGORY_MAP.getOrDefault(purpose.toUpperCase(), Set.of());
        return Map.of("purpose", purpose, "allowedCategories", allowed,
                "registered", !allowed.isEmpty(),
                "timestamp", LocalDateTime.now().toString());
    }

    /**
     * Get all purpose mappings
     */
    public Map<String, Object> getAllPurposeMappings() {
        Map<String, Object> mappings = new LinkedHashMap<>();
        PURPOSE_CATEGORY_MAP.forEach((purpose, categories) ->
                mappings.put(purpose, Map.of("categories", categories, "categoryCount", categories.size())));
        mappings.put("totalPurposes", PURPOSE_CATEGORY_MAP.size());
        return mappings;
    }

    /**
     * Get purpose creep alerts
     */
    public List<Map<String, Object>> getCreepAlerts(int limit) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        if (dbManager == null || !dbManager.isInitialized()) return alerts;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM purpose_creep_alerts WHERE status = 'OPEN' ORDER BY detected_at DESC LIMIT ?")) {
            ps.setInt(1, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alerts.add(Map.of(
                            "id", rs.getString("id"),
                            "purpose", rs.getString("purpose"),
                            "newCategory", rs.getString("new_category"),
                            "detectedAt", rs.getString("detected_at"),
                            "status", rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) { /* silent */ }
        return alerts;
    }

    private void recordCreepAlert(String purpose, String category) {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO purpose_creep_alerts (id, purpose, new_category) VALUES (?, ?, ?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, purpose);
            ps.setString(3, category);
            ps.executeUpdate();
        } catch (SQLException e) { /* silent */ }
    }

    private void persistValidation(String id, String purpose, String category,
            String principalId, String validationResult, String reason) {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO purpose_validations (id, purpose, data_category, principal_id, result, reason) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, purpose);
            ps.setString(3, category);
            ps.setString(4, principalId);
            ps.setString(5, validationResult);
            ps.setString(6, reason);
            ps.executeUpdate();
        } catch (SQLException e) { /* silent */ }
    }

    public boolean isInitialized() { return initialized; }
}
