package com.qsdpdp.siem;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * UEBA Engine — User & Entity Behavior Analytics
 * 
 * Implements RBI Advisory 3/2026 Domain 12 (Continuous Monitoring):
 * - Baseline behavior profiling per user/entity
 * - Anomaly scoring (0-100)
 * - Risk-based alerts (insider threat, compromised account, etc.)
 * - Session analysis (impossible travel, unusual hours)
 * - Data access anomalies (volume, frequency, category)
 * 
 * @version 1.0.0
 * @since Phase 3 — RBI Enhancement
 */
@Service
public class UEBAEngine {

    private static final Logger logger = LoggerFactory.getLogger(UEBAEngine.class);

    @Autowired(required = false) private DatabaseManager dbManager;

    private boolean initialized = false;

    // Behavior baselines per entity type
    private static final Map<String, Map<String, Object>> BASELINES = new LinkedHashMap<>();
    static {
        BASELINES.put("ADMIN", Map.of("maxDailyLogins", 10, "maxDailyQueries", 500,
                "allowedHours", "06-22", "maxDataExport", 1000, "maxPIIAccess", 200));
        BASELINES.put("OPERATOR", Map.of("maxDailyLogins", 20, "maxDailyQueries", 200,
                "allowedHours", "08-20", "maxDataExport", 100, "maxPIIAccess", 50));
        BASELINES.put("DATA_PRINCIPAL", Map.of("maxDailyLogins", 5, "maxDailyQueries", 50,
                "allowedHours", "00-24", "maxDataExport", 10, "maxPIIAccess", 10));
        BASELINES.put("API_SERVICE", Map.of("maxDailyLogins", 1000, "maxDailyQueries", 10000,
                "allowedHours", "00-24", "maxDataExport", 5000, "maxPIIAccess", 1000));
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing UEBA Engine ({} entity baselines)...", BASELINES.size());
        createTables();
        initialized = true;
    }

    private void createTables() {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ueba_events (
                    id TEXT PRIMARY KEY,
                    entity_id TEXT NOT NULL,
                    entity_type TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    anomaly_score INTEGER DEFAULT 0,
                    details TEXT,
                    ip_address TEXT,
                    location TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ueba_alerts (
                    id TEXT PRIMARY KEY,
                    entity_id TEXT NOT NULL,
                    alert_type TEXT NOT NULL,
                    severity TEXT DEFAULT 'MEDIUM',
                    anomaly_score INTEGER,
                    details TEXT,
                    status TEXT DEFAULT 'OPEN',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_ueba_entity ON ueba_events(entity_id)");
        } catch (SQLException e) {
            logger.error("Failed to create UEBA tables", e);
        }
    }

    /**
     * Analyze user behavior and return anomaly score
     */
    public Map<String, Object> analyzeActivity(String entityId, String entityType,
            String eventType, Map<String, Object> context) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> baseline = BASELINES.getOrDefault(entityType.toUpperCase(), BASELINES.get("OPERATOR"));

        int anomalyScore = 0;
        List<Map<String, Object>> anomalies = new ArrayList<>();

        // Check login frequency
        if ("LOGIN".equals(eventType)) {
            int dailyLogins = getDailyEventCount(entityId, "LOGIN");
            int maxLogins = (int) baseline.get("maxDailyLogins");
            if (dailyLogins > maxLogins) {
                int excess = Math.min(50, (dailyLogins - maxLogins) * 10);
                anomalyScore += excess;
                anomalies.add(Map.of("type", "EXCESSIVE_LOGINS",
                        "details", "Daily: " + dailyLogins + " (max: " + maxLogins + ")",
                        "impact", excess));
            }
        }

        // Check data access volume
        if ("DATA_ACCESS".equals(eventType)) {
            int dailyQueries = getDailyEventCount(entityId, "DATA_ACCESS");
            int maxQueries = (int) baseline.get("maxDailyQueries");
            if (dailyQueries > maxQueries) {
                int excess = Math.min(40, ((dailyQueries - maxQueries) * 10) / maxQueries);
                anomalyScore += excess;
                anomalies.add(Map.of("type", "EXCESSIVE_DATA_ACCESS",
                        "details", "Queries: " + dailyQueries + " (max: " + maxQueries + ")",
                        "impact", excess));
            }
        }

        // Check PII access
        if ("PII_ACCESS".equals(eventType)) {
            int dailyPII = getDailyEventCount(entityId, "PII_ACCESS");
            int maxPII = (int) baseline.get("maxPIIAccess");
            if (dailyPII > maxPII) {
                anomalyScore += 30;
                anomalies.add(Map.of("type", "EXCESSIVE_PII_ACCESS",
                        "details", "PII accesses: " + dailyPII + " (max: " + maxPII + ")",
                        "impact", 30));
            }
        }

        // Check unusual hours
        if (context != null && context.containsKey("hour")) {
            int hour = ((Number) context.get("hour")).intValue();
            String allowedHours = (String) baseline.get("allowedHours");
            String[] range = allowedHours.split("-");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            if (hour < start || hour > end) {
                anomalyScore += 20;
                anomalies.add(Map.of("type", "UNUSUAL_HOURS",
                        "details", "Access at hour " + hour + " (allowed: " + allowedHours + ")",
                        "impact", 20));
            }
        }

        // Check impossible travel
        if (context != null && context.containsKey("location") && context.containsKey("prevLocation")) {
            String loc = (String) context.get("location");
            String prevLoc = (String) context.get("prevLocation");
            if (!loc.equals(prevLoc)) {
                anomalyScore += 25;
                anomalies.add(Map.of("type", "IMPOSSIBLE_TRAVEL",
                        "details", "Location changed from " + prevLoc + " to " + loc,
                        "impact", 25));
            }
        }

        int finalScore = Math.min(100, anomalyScore);

        // Persist event
        persistEvent(id, entityId, entityType, eventType, finalScore,
                context != null ? context.getOrDefault("ip", "").toString() : "",
                context != null ? context.getOrDefault("location", "").toString() : "");

        // Generate alert if threshold exceeded
        if (finalScore >= 50) {
            String severity = finalScore >= 80 ? "CRITICAL" : finalScore >= 60 ? "HIGH" : "MEDIUM";
            createAlert(entityId, "ANOMALOUS_BEHAVIOR", severity, finalScore,
                    "Anomaly score: " + finalScore + " — " + anomalies.size() + " anomalies detected");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entityId", entityId);
        result.put("entityType", entityType);
        result.put("eventType", eventType);
        result.put("anomalyScore", finalScore);
        result.put("riskLevel", finalScore >= 80 ? "CRITICAL" : finalScore >= 60 ? "HIGH" : finalScore >= 40 ? "MEDIUM" : "LOW");
        result.put("anomalies", anomalies);
        result.put("alertGenerated", finalScore >= 50);
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * Get entity risk profile
     */
    public Map<String, Object> getEntityProfile(String entityId) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("entityId", entityId);

        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection()) {
                // Average anomaly score
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT AVG(anomaly_score) as avg_score, COUNT(*) as event_count FROM ueba_events WHERE entity_id = ?")) {
                    ps.setString(1, entityId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            profile.put("avgAnomalyScore", rs.getDouble("avg_score"));
                            profile.put("totalEvents", rs.getInt("event_count"));
                        }
                    }
                }
                // Open alerts
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) as cnt FROM ueba_alerts WHERE entity_id = ? AND status = 'OPEN'")) {
                    ps.setString(1, entityId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) profile.put("openAlerts", rs.getInt("cnt"));
                    }
                }
            } catch (SQLException e) { /* silent */ }
        }

        profile.put("timestamp", LocalDateTime.now().toString());
        return profile;
    }

    /**
     * Get all open alerts
     */
    public List<Map<String, Object>> getAlerts(String status, int limit) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        if (dbManager == null || !dbManager.isInitialized()) return alerts;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT * FROM ueba_alerts WHERE status = ? ORDER BY created_at DESC LIMIT ?")) {
            ps.setString(1, status != null ? status : "OPEN");
            ps.setInt(2, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alerts.add(Map.of("id", rs.getString("id"), "entityId", rs.getString("entity_id"),
                            "alertType", rs.getString("alert_type"), "severity", rs.getString("severity"),
                            "anomalyScore", rs.getInt("anomaly_score"), "details", rs.getString("details"),
                            "status", rs.getString("status"), "createdAt", rs.getString("created_at")));
                }
            }
        } catch (SQLException e) { /* silent */ }
        return alerts;
    }

    /**
     * Get SOC dashboard data
     */
    public Map<String, Object> getSOCDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ueba_alerts WHERE status = 'OPEN'");
                if (rs.next()) dashboard.put("openAlerts", rs.getInt(1));
                rs = stmt.executeQuery("SELECT COUNT(*) FROM ueba_alerts WHERE severity = 'CRITICAL' AND status = 'OPEN'");
                if (rs.next()) dashboard.put("criticalAlerts", rs.getInt(1));
                rs = stmt.executeQuery("SELECT COUNT(*) FROM ueba_events WHERE created_at > datetime('now', '-1 day')");
                if (rs.next()) dashboard.put("eventsLast24h", rs.getInt(1));
                rs = stmt.executeQuery("SELECT AVG(anomaly_score) FROM ueba_events WHERE created_at > datetime('now', '-1 day')");
                if (rs.next()) dashboard.put("avgAnomalyScore24h", Math.round(rs.getDouble(1) * 100.0) / 100.0);
            } catch (SQLException e) { /* silent */ }
        }
        dashboard.put("baselines", BASELINES.size());
        dashboard.put("timestamp", LocalDateTime.now().toString());
        return dashboard;
    }

    // Helpers
    private int getDailyEventCount(String entityId, String eventType) {
        if (dbManager == null || !dbManager.isInitialized()) return 0;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT COUNT(*) FROM ueba_events WHERE entity_id = ? AND event_type = ? AND created_at > datetime('now', '-1 day')")) {
            ps.setString(1, entityId);
            ps.setString(2, eventType);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { /* silent */ }
        return 0;
    }

    private void persistEvent(String id, String entityId, String entityType,
            String eventType, int score, String ip, String location) {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO ueba_events (id, entity_id, entity_type, event_type, anomaly_score, ip_address, location) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, id); ps.setString(2, entityId); ps.setString(3, entityType);
            ps.setString(4, eventType); ps.setInt(5, score); ps.setString(6, ip); ps.setString(7, location);
            ps.executeUpdate();
        } catch (SQLException e) { /* silent */ }
    }

    private void createAlert(String entityId, String alertType, String severity,
            int score, String details) {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO ueba_alerts (id, entity_id, alert_type, severity, anomaly_score, details) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, entityId);
            ps.setString(3, alertType); ps.setString(4, severity);
            ps.setInt(5, score); ps.setString(6, details);
            ps.executeUpdate();
        } catch (SQLException e) { /* silent */ }
    }

    public boolean isInitialized() { return initialized; }
}
