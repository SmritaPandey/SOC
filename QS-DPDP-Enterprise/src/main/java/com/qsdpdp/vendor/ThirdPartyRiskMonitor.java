package com.qsdpdp.vendor;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Third-Party Risk Monitor — RBI Advisory Domain 7
 * 
 * Continuous vendor/third-party risk assessment:
 * - Vendor risk scoring (0-100)
 * - Data sharing compliance tracking
 * - SLA monitoring
 * - Certification validation
 * - Continuous compliance monitoring
 * 
 * @version 1.0.0
 * @since Phase 3 — RBI Enhancement
 */
@Service
public class ThirdPartyRiskMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ThirdPartyRiskMonitor.class);

    @Autowired(required = false) private DatabaseManager dbManager;

    private boolean initialized = false;

    // Risk assessment criteria with weights
    private static final Map<String, Integer> RISK_CRITERIA = new LinkedHashMap<>();
    static {
        RISK_CRITERIA.put("ISO_27001_CERTIFIED", 15);
        RISK_CRITERIA.put("ISO_27701_CERTIFIED", 10);
        RISK_CRITERIA.put("SOC2_REPORT", 10);
        RISK_CRITERIA.put("DPDP_REGISTERED", 15);
        RISK_CRITERIA.put("DATA_ENCRYPTION", 10);
        RISK_CRITERIA.put("BREACH_HISTORY_CLEAN", 10);
        RISK_CRITERIA.put("SLA_COMPLIANCE", 10);
        RISK_CRITERIA.put("ACCESS_CONTROLS", 10);
        RISK_CRITERIA.put("INCIDENT_RESPONSE", 5);
        RISK_CRITERIA.put("AUDIT_ACCESS", 5);
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Third-Party Risk Monitor ({} criteria)...", RISK_CRITERIA.size());
        createTables();
        initialized = true;
    }

    private void createTables() {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS vendor_assessments (
                    id TEXT PRIMARY KEY,
                    vendor_id TEXT NOT NULL,
                    vendor_name TEXT NOT NULL,
                    risk_score INTEGER DEFAULT 50,
                    risk_level TEXT DEFAULT 'MEDIUM',
                    criteria_met TEXT,
                    criteria_failed TEXT,
                    data_categories_shared TEXT,
                    contract_expiry TEXT,
                    last_audit_date TEXT,
                    status TEXT DEFAULT 'ACTIVE',
                    assessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS vendor_monitoring_log (
                    id TEXT PRIMARY KEY,
                    vendor_id TEXT NOT NULL,
                    check_type TEXT NOT NULL,
                    result TEXT NOT NULL,
                    details TEXT,
                    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) {
            logger.error("Failed to create vendor risk tables", e);
        }
    }

    /**
     * Assess vendor risk
     */
    public Map<String, Object> assessVendor(String vendorId, String vendorName,
            Map<String, Boolean> criteriaResults, List<String> dataCategories) {
        String id = UUID.randomUUID().toString();
        int score = 0;
        List<String> met = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (Map.Entry<String, Integer> criterion : RISK_CRITERIA.entrySet()) {
            boolean passed = criteriaResults != null &&
                    Boolean.TRUE.equals(criteriaResults.get(criterion.getKey()));
            if (passed) {
                score += criterion.getValue();
                met.add(criterion.getKey());
            } else {
                failed.add(criterion.getKey());
            }
        }

        String riskLevel = score >= 80 ? "LOW" : score >= 60 ? "MEDIUM" : score >= 40 ? "HIGH" : "CRITICAL";

        // Persist
        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO vendor_assessments (id, vendor_id, vendor_name, risk_score, risk_level, criteria_met, criteria_failed, data_categories_shared) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, id); ps.setString(2, vendorId); ps.setString(3, vendorName);
                ps.setInt(4, score); ps.setString(5, riskLevel);
                ps.setString(6, String.join(",", met)); ps.setString(7, String.join(",", failed));
                ps.setString(8, dataCategories != null ? String.join(",", dataCategories) : "");
                ps.executeUpdate();
            } catch (SQLException e) { /* silent */ }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assessmentId", id);
        result.put("vendorId", vendorId);
        result.put("vendorName", vendorName);
        result.put("riskScore", score);
        result.put("riskLevel", riskLevel);
        result.put("criteriaMet", met);
        result.put("criteriaFailed", failed);
        result.put("dataCategories", dataCategories);
        result.put("recommendations", getRecommendations(failed));
        result.put("assessedAt", LocalDateTime.now().toString());
        return result;
    }

    /**
     * Get all vendor assessments
     */
    public List<Map<String, Object>> getVendors(String status) {
        List<Map<String, Object>> vendors = new ArrayList<>();
        if (dbManager == null || !dbManager.isInitialized()) return vendors;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM vendor_assessments WHERE status = ? ORDER BY risk_score ASC")) {
            ps.setString(1, status != null ? status : "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vendors.add(Map.of("vendorId", rs.getString("vendor_id"),
                            "vendorName", rs.getString("vendor_name"),
                            "riskScore", rs.getInt("risk_score"),
                            "riskLevel", rs.getString("risk_level"),
                            "dataCategories", rs.getString("data_categories_shared"),
                            "assessedAt", rs.getString("assessed_at")));
                }
            }
        } catch (SQLException e) { /* silent */ }
        return vendors;
    }

    /**
     * Run continuous monitoring check
     */
    public Map<String, Object> monitorVendor(String vendorId, String checkType,
            boolean passed, String details) {
        String id = UUID.randomUUID().toString();
        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO vendor_monitoring_log (id, vendor_id, check_type, result, details) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, id); ps.setString(2, vendorId); ps.setString(3, checkType);
                ps.setString(4, passed ? "PASS" : "FAIL"); ps.setString(5, details);
                ps.executeUpdate();
            } catch (SQLException e) { /* silent */ }
        }
        return Map.of("id", id, "vendorId", vendorId, "checkType", checkType,
                "result", passed ? "PASS" : "FAIL", "timestamp", LocalDateTime.now().toString());
    }

    /**
     * Get risk assessment criteria
     */
    public Map<String, Object> getCriteria() {
        return Map.of("criteria", RISK_CRITERIA, "totalWeight", 100,
                "timestamp", LocalDateTime.now().toString());
    }

    private List<String> getRecommendations(List<String> failed) {
        List<String> recs = new ArrayList<>();
        for (String f : failed) {
            switch (f) {
                case "ISO_27001_CERTIFIED" -> recs.add("Require ISO 27001 certification within 6 months");
                case "DPDP_REGISTERED" -> recs.add("Vendor must register as Data Processor under DPDP Act S.8");
                case "DATA_ENCRYPTION" -> recs.add("Mandate AES-256 encryption for all shared data");
                case "BREACH_HISTORY_CLEAN" -> recs.add("Review vendor breach history and demand RCA reports");
                case "SLA_COMPLIANCE" -> recs.add("Review and strengthen SLA terms for data protection");
                default -> recs.add("Address " + f.replace("_", " ").toLowerCase() + " compliance gap");
            }
        }
        return recs;
    }

    public boolean isInitialized() { return initialized; }
}
