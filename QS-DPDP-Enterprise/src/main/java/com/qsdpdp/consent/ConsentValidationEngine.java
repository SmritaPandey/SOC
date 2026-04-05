package com.qsdpdp.consent;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.events.ComplianceEvent;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Consent Validation Engine — Real-time Data Usage vs Consent Enforcement
 * 
 * Validates every data access against granted consent scope:
 * - VALID: Data access is within consent scope
 * - MISUSE: Data accessed for purpose not covered by consent
 * - OVERUSE: Data accessed beyond allowed volume/frequency
 * - EXPIRED: Consent has expired but data was accessed
 * 
 * Automatically triggers alerts, audit logs, and EventBus notifications
 * for any violation detected.
 * 
 * Implements DPDP Act S.6 (consent-based processing),
 * S.7 (purpose limitation), S.8 (data access transparency).
 * 
 * @version 1.0.0
 * @since Phase 4 — Consent Validation Engine
 */
@Service
public class ConsentValidationEngine {

    private static final Logger logger = LoggerFactory.getLogger(ConsentValidationEngine.class);

    @Autowired(required = false) private DatabaseManager dbManager;
    @Autowired(required = false) private EventBus eventBus;
    @Autowired(required = false) private AuditService auditService;

    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Consent Validation Engine...");
        createTables();
        initialized = true;
        logger.info("Consent Validation Engine initialized");
    }

    private void createTables() {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS data_usage_log (
                    id TEXT PRIMARY KEY,
                    consent_id TEXT,
                    principal_id TEXT,
                    data_category TEXT NOT NULL,
                    purpose TEXT NOT NULL,
                    accessed_by TEXT,
                    access_type TEXT DEFAULT 'READ',
                    volume INTEGER DEFAULT 1,
                    validation_result TEXT,
                    violation_type TEXT,
                    violation_details TEXT,
                    accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS consent_violations (
                    id TEXT PRIMARY KEY,
                    consent_id TEXT,
                    principal_id TEXT,
                    violation_type TEXT NOT NULL,
                    severity TEXT DEFAULT 'HIGH',
                    data_category TEXT,
                    purpose TEXT,
                    details TEXT,
                    remediation_action TEXT,
                    status TEXT DEFAULT 'OPEN',
                    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved_at TIMESTAMP
                )
            """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_data_usage_consent ON data_usage_log(consent_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_violations_status ON consent_violations(status)");
        } catch (SQLException e) {
            logger.error("Failed to create validation tables", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Validate a data access request against existing consent
     * 
     * @param principalId Data principal ID
     * @param dataCategory Category of data being accessed
     * @param purpose Purpose of access
     * @param accessedBy Who is accessing the data
     * @param volume Number of records accessed
     * @return Validation result: VALID, MISUSE, OVERUSE, or EXPIRED
     */
    public Map<String, Object> validateAccess(String principalId, String dataCategory,
            String purpose, String accessedBy, int volume) {
        
        String usageId = UUID.randomUUID().toString();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("usageId", usageId);
        result.put("principalId", principalId);
        result.put("dataCategory", dataCategory);
        result.put("purpose", purpose);
        result.put("accessedBy", accessedBy);
        result.put("volume", volume);
        result.put("timestamp", LocalDateTime.now().toString());

        // Look up active consent for this principal + data category
        Map<String, Object> consent = findActiveConsent(principalId, dataCategory);

        if (consent == null) {
            // No consent found — MISUSE
            result.put("validationResult", "MISUSE");
            result.put("violationType", "NO_CONSENT");
            result.put("details", "No active consent found for data category: " + dataCategory);
            result.put("severity", "CRITICAL");
            recordViolation(usageId, null, principalId, "NO_CONSENT", "CRITICAL", dataCategory, purpose,
                    "Data accessed without any consent from data principal");
        } else {
            String consentId = (String) consent.get("id");
            String consentStatus = (String) consent.get("status");
            String consentPurpose = (String) consent.get("purpose");
            String expiresAt = (String) consent.get("expiresAt");

            result.put("consentId", consentId);

            if ("EXPIRED".equalsIgnoreCase(consentStatus) ||
                    (expiresAt != null && LocalDateTime.parse(expiresAt).isBefore(LocalDateTime.now()))) {
                // Consent expired — EXPIRED violation
                result.put("validationResult", "EXPIRED");
                result.put("violationType", "CONSENT_EXPIRED");
                result.put("details", "Consent expired at: " + expiresAt);
                result.put("severity", "HIGH");
                recordViolation(usageId, consentId, principalId, "CONSENT_EXPIRED", "HIGH",
                        dataCategory, purpose, "Data accessed after consent expiry: " + expiresAt);
            } else if (consentPurpose != null && !purposeMatches(purpose, consentPurpose)) {
                // Purpose mismatch — MISUSE
                result.put("validationResult", "MISUSE");
                result.put("violationType", "PURPOSE_MISMATCH");
                result.put("details", String.format("Access purpose '%s' does not match consent purpose '%s'",
                        purpose, consentPurpose));
                result.put("severity", "HIGH");
                recordViolation(usageId, consentId, principalId, "PURPOSE_MISMATCH", "HIGH",
                        dataCategory, purpose, "Purpose mismatch: requested=" + purpose + ", consent=" + consentPurpose);
            } else if (volume > 100) {
                // Excessive volume — OVERUSE
                result.put("validationResult", "OVERUSE");
                result.put("violationType", "EXCESSIVE_VOLUME");
                result.put("details", "Access volume (" + volume + ") exceeds threshold (100)");
                result.put("severity", "MEDIUM");
                recordViolation(usageId, consentId, principalId, "EXCESSIVE_VOLUME", "MEDIUM",
                        dataCategory, purpose, "Volume " + volume + " exceeds threshold");
            } else {
                // All checks passed — VALID
                result.put("validationResult", "VALID");
                result.put("violationType", null);
                result.put("details", "Access validated successfully against consent " + consentId);
            }
        }

        // Log usage
        logUsage(usageId, result);

        // Publish event for violations
        String validationResult = (String) result.get("validationResult");
        if (!"VALID".equals(validationResult) && eventBus != null) {
            eventBus.publish(new ComplianceEvent("consent.violation." + validationResult.toLowerCase(),
                    result, "CONSENT_VALIDATION_ENGINE"));
        }

        return result;
    }

    private boolean purposeMatches(String requestedPurpose, String consentPurpose) {
        if (consentPurpose == null || consentPurpose.isEmpty()) return true;
        String req = requestedPurpose.toLowerCase().trim();
        String consent = consentPurpose.toLowerCase().trim();
        // Exact match or consent contains the requested purpose
        return consent.equals(req) || consent.contains(req) || req.contains(consent);
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════

    /**
     * Get all open violations
     */
    public List<Map<String, Object>> getOpenViolations(int limit) {
        return getViolationsByStatus("OPEN", limit);
    }

    /**
     * Get violations by status
     */
    public List<Map<String, Object>> getViolationsByStatus(String status, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (dbManager == null || !dbManager.isInitialized()) return result;
        String sql = "SELECT * FROM consent_violations WHERE status = ? ORDER BY detected_at DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapViolation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get violations", e);
        }
        return result;
    }

    /**
     * Get validation statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        if (dbManager == null || !dbManager.isInitialized()) return stats;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT validation_result, COUNT(*) as cnt FROM data_usage_log GROUP BY validation_result");
            Map<String, Integer> resultCounts = new LinkedHashMap<>();
            int total = 0;
            while (rs.next()) {
                String vr = rs.getString("validation_result");
                int cnt = rs.getInt("cnt");
                resultCounts.put(vr != null ? vr : "UNKNOWN", cnt);
                total += cnt;
            }
            stats.put("totalValidations", total);
            stats.put("resultBreakdown", resultCounts);

            rs = stmt.executeQuery("SELECT status, COUNT(*) as cnt FROM consent_violations GROUP BY status");
            Map<String, Integer> violationCounts = new LinkedHashMap<>();
            while (rs.next()) {
                violationCounts.put(rs.getString("status"), rs.getInt("cnt"));
            }
            stats.put("violationsByStatus", violationCounts);

            rs = stmt.executeQuery("SELECT violation_type, COUNT(*) as cnt FROM consent_violations GROUP BY violation_type ORDER BY cnt DESC");
            Map<String, Integer> typeCounts = new LinkedHashMap<>();
            while (rs.next()) {
                typeCounts.put(rs.getString("violation_type"), rs.getInt("cnt"));
            }
            stats.put("violationsByType", typeCounts);
        } catch (SQLException e) {
            logger.error("Failed to get statistics", e);
        }
        stats.put("timestamp", LocalDateTime.now().toString());
        return stats;
    }

    /**
     * Resolve a violation
     */
    public Map<String, Object> resolveViolation(String violationId, String remediation) {
        if (dbManager == null || !dbManager.isInitialized()) return Map.of("success", false);
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE consent_violations SET status = 'RESOLVED', remediation_action = ?, resolved_at = ? WHERE id = ?")) {
            ps.setString(1, remediation);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, violationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to resolve violation", e);
            return Map.of("success", false, "error", e.getMessage());
        }
        return Map.of("success", true, "violationId", violationId, "status", "RESOLVED");
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> findActiveConsent(String principalId, String dataCategory) {
        if (dbManager == null || !dbManager.isInitialized()) return null;
        String sql = """
            SELECT id, status, purpose, expires_at as expiresAt FROM consents
            WHERE principal_id = ? AND status IN ('ACTIVE', 'GRANTED')
            ORDER BY created_at DESC LIMIT 1
        """;
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, principalId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("id", rs.getString("id"));
                    c.put("status", rs.getString("status"));
                    c.put("purpose", rs.getString("purpose"));
                    c.put("expiresAt", rs.getString("expiresAt"));
                    return c;
                }
            }
        } catch (SQLException e) {
            logger.debug("Consent lookup failed: {}", e.getMessage());
        }
        return null;
    }

    private void logUsage(String usageId, Map<String, Object> result) {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO data_usage_log (id, consent_id, principal_id, data_category, purpose, accessed_by, volume, validation_result, violation_type, violation_details) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, usageId);
            ps.setString(2, (String) result.get("consentId"));
            ps.setString(3, (String) result.get("principalId"));
            ps.setString(4, (String) result.get("dataCategory"));
            ps.setString(5, (String) result.get("purpose"));
            ps.setString(6, (String) result.get("accessedBy"));
            ps.setInt(7, (int) result.getOrDefault("volume", 1));
            ps.setString(8, (String) result.get("validationResult"));
            ps.setString(9, (String) result.get("violationType"));
            ps.setString(10, (String) result.get("details"));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to log usage: {}", e.getMessage());
        }
    }

    private void recordViolation(String usageId, String consentId, String principalId,
            String violationType, String severity, String dataCategory, String purpose, String details) {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO consent_violations (id, consent_id, principal_id, violation_type, severity, data_category, purpose, details) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, consentId);
            ps.setString(3, principalId);
            ps.setString(4, violationType);
            ps.setString(5, severity);
            ps.setString(6, dataCategory);
            ps.setString(7, purpose);
            ps.setString(8, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to record violation: {}", e.getMessage());
        }

        if (auditService != null) {
            auditService.log("CONSENT_VIOLATION", "CONSENT", "SYSTEM",
                    violationType + ": " + details + " [principal=" + principalId + "]");
        }
    }

    private Map<String, Object> mapViolation(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getString("id"));
        m.put("consentId", rs.getString("consent_id"));
        m.put("principalId", rs.getString("principal_id"));
        m.put("violationType", rs.getString("violation_type"));
        m.put("severity", rs.getString("severity"));
        m.put("dataCategory", rs.getString("data_category"));
        m.put("purpose", rs.getString("purpose"));
        m.put("details", rs.getString("details"));
        m.put("remediation", rs.getString("remediation_action"));
        m.put("status", rs.getString("status"));
        m.put("detectedAt", rs.getString("detected_at"));
        m.put("resolvedAt", rs.getString("resolved_at"));
        return m;
    }

    public boolean isInitialized() { return initialized; }
}
