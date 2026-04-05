package com.qsdpdp.web;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.siem.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

/**
 * SIEM REST Controller — Security Information & Event Management
 * Events, alerts, playbooks, threat-intel, UEBA, forensics, MITRE, stats
 *
 * @version 1.0.0
 * @since Sprint 6
 */

@RestController
@RequestMapping("/api/siem")
public class SIEMController {

    private static final Logger logger = LoggerFactory.getLogger(SIEMController.class);

    @Autowired
    private SIEMService siemService;

    @Autowired
    private DatabaseManager dbManager;

    // ═══════════════════════════════════════════════════════════
    // EVENTS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<SecurityEvent> events = siemService.getRecentEvents(limit);
            List<Map<String, Object>> list = new ArrayList<>();
            for (SecurityEvent e : events) {
                list.add(eventToMap(e));
            }
            return ResponseEntity.ok(Map.of("events", list, "total", list.size()));
        } catch (Exception e) {
            logger.error("Failed to get SIEM events", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get events: " + e.getMessage()));
        }
    }

    @PostMapping("/events/ingest")
    public ResponseEntity<?> ingestEvent(@RequestBody Map<String, String> payload) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .category(EventCategory.valueOf(
                            payload.getOrDefault("category", "APP_WARNING")))
                    .severity(EventSeverity.valueOf(
                            payload.getOrDefault("severity", "INFO")))
                    .source(payload.getOrDefault("source", "EXTERNAL"), payload.get("sourceIp"))
                    .message(payload.getOrDefault("message", ""))
                    .build();

            siemService.ingestEvent(event);
            return ResponseEntity.ok(Map.of(
                    "status", "ingested",
                    "eventId", event.getId(),
                    "message", "Event queued for processing"));
        } catch (Exception e) {
            logger.error("Failed to ingest event", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to ingest event: " + e.getMessage()));
        }
    }

    @PostMapping("/events/ingest-raw")
    public ResponseEntity<?> ingestRawLog(@RequestBody Map<String, String> payload) {
        try {
            String source = payload.getOrDefault("source", "EXTERNAL");
            String rawLog = payload.getOrDefault("rawLog", "");
            if (rawLog.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "rawLog is required"));
            }
            siemService.ingestRawLog(source, rawLog);
            return ResponseEntity.ok(Map.of(
                    "status", "ingested",
                    "message", "Raw log queued for parsing and processing"));
        } catch (Exception e) {
            logger.error("Failed to ingest raw log", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to ingest raw log: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ALERTS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        try {
            List<SIEMAlert> alerts = siemService.getOpenAlerts();
            List<Map<String, Object>> list = new ArrayList<>();
            for (SIEMAlert a : alerts) {
                list.add(alertToMap(a));
            }
            return ResponseEntity.ok(Map.of("alerts", list, "total", list.size()));
        } catch (Exception e) {
            logger.error("Failed to get SIEM alerts", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get alerts: " + e.getMessage()));
        }
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(@PathVariable String id) {
        try {
            String sql = "UPDATE siem_alerts SET status = 'ACKNOWLEDGED', acknowledged_at = ? WHERE id = ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, java.time.LocalDateTime.now().toString());
                stmt.setString(2, id);
                int updated = stmt.executeUpdate();
                if (updated == 0) return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                    "status", "acknowledged",
                    "alertId", id,
                    "message", "Alert acknowledged"));
        } catch (Exception e) {
            logger.error("Failed to acknowledge alert: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to acknowledge alert: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PLAYBOOKS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/playbooks")
    public ResponseEntity<?> getPlaybooks(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return queryTable("soar_playbooks", offset, limit);
    }

    // ═══════════════════════════════════════════════════════════
    // SUB-MODULES: THREAT-INTEL, UEBA, FORENSICS, MITRE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/threat-intel")
    public ResponseEntity<?> getThreatIntel(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return queryTable("threat_intel_feeds", offset, limit);
    }

    @GetMapping("/ueba")
    public ResponseEntity<?> getUeba(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return queryTable("ueba_profiles", offset, limit);
    }

    @GetMapping("/forensics")
    public ResponseEntity<?> getForensics(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return queryTable("forensic_timelines", offset, limit);
    }

    @GetMapping("/mitre")
    public ResponseEntity<?> getMitre(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return queryTable("mitre_mappings", offset, limit);
    }

    @GetMapping("/correlation-rules")
    public ResponseEntity<?> getCorrelationRules(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return queryTable("siem_correlation_rules", offset, limit);
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS & DASHBOARD
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            SIEMService.SIEMStatistics stats = siemService.getStatistics();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalEventsProcessed", stats.getTotalEventsProcessed());
            result.put("alertsGenerated", stats.getAlertsGenerated());
            result.put("playbooksExecuted", stats.getPlaybooksExecuted());
            result.put("eventsToday", stats.getEventsToday());
            result.put("openAlerts", stats.getOpenAlerts());
            result.put("criticalAlerts", stats.getCriticalAlerts());
            result.put("pendingNotifications", stats.getPendingNotifications());
            return ResponseEntity.ok(Map.of("statistics", result));
        } catch (Exception e) {
            logger.error("Failed to get SIEM statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            SIEMService.SIEMStatistics stats = siemService.getStatistics();
            List<SIEMAlert> openAlerts = siemService.getOpenAlerts();
            List<SecurityEvent> recentEvents = siemService.getRecentEvents(10);

            Map<String, Object> dashboard = new LinkedHashMap<>();
            dashboard.put("eventsToday", stats.getEventsToday());
            dashboard.put("openAlerts", stats.getOpenAlerts());
            dashboard.put("criticalAlerts", stats.getCriticalAlerts());
            dashboard.put("pendingNotifications", stats.getPendingNotifications());
            dashboard.put("totalProcessed", stats.getTotalEventsProcessed());

            List<Map<String, Object>> alertList = new ArrayList<>();
            for (SIEMAlert a : openAlerts) alertList.add(alertToMap(a));
            dashboard.put("alerts", alertList);

            List<Map<String, Object>> eventList = new ArrayList<>();
            for (SecurityEvent e : recentEvents) eventList.add(eventToMap(e));
            dashboard.put("recentEvents", eventList);

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.error("Failed to get SIEM dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get dashboard: " + e.getMessage()));
        }
    }

    /**
     * Summary endpoint — reads from DB tables (includes sector-seeded data)
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            try (Connection conn = dbManager.getConnection()) {
                summary.put("totalEvents", countTable(conn, "siem_events"));
                summary.put("totalAlerts", countTable(conn, "siem_alerts"));
                summary.put("openAlerts", countWhere(conn, "siem_alerts", "status IN ('OPEN','ACKNOWLEDGED','INVESTIGATING')"));
                summary.put("criticalAlerts", countWhere(conn, "siem_alerts", "severity = 'CRITICAL'"));
                summary.put("totalPlaybooks", countTable(conn, "soar_playbooks"));
                summary.put("soarExecutions", countTable(conn, "soar_executions"));
                summary.put("threatIntelFeeds", countTable(conn, "threat_intel_feeds"));
                summary.put("uebaProfiles", countTable(conn, "ueba_profiles"));

                // Recent events from DB
                List<Map<String, Object>> recentEvents = new ArrayList<>();
                String sql = "SELECT * FROM siem_events ORDER BY timestamp DESC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ResultSet rs = ps.executeQuery();
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= cols; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        recentEvents.add(row);
                    }
                }
                summary.put("recentEvents", recentEvents);

                // Severity distribution
                Map<String, Integer> sevDist = new LinkedHashMap<>();
                for (String sev : new String[]{"CRITICAL", "HIGH", "MEDIUM", "LOW"}) {
                    sevDist.put(sev, countWhere(conn, "siem_events", "severity = '" + sev + "'"));
                }
                summary.put("severityDistribution", sevDist);
            }
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Failed to get SIEM summary", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get summary: " + e.getMessage()));
        }
    }

    /**
     * DB Events endpoint — reads from siem_events table (includes sector-seeded data)
     */
    @GetMapping("/db-events")
    public ResponseEntity<?> getDbEvents(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return queryTable("siem_events", offset, limit);
    }

    private int countTable(Connection conn, String table) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    private int countWhere(Connection conn, String table, String where) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table + " WHERE " + where)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> eventToMap(SecurityEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("timestamp", e.getTimestamp() != null ? e.getTimestamp().toString() : null);
        m.put("category", e.getCategory() != null ? e.getCategory().name() : null);
        m.put("severity", e.getSeverity() != null ? e.getSeverity().name() : null);
        m.put("source", e.getSource());
        m.put("sourceIp", e.getSourceIP());
        m.put("userId", e.getUserId());
        m.put("message", e.getMessage());
        m.put("status", e.getStatus());
        return m;
    }

    private Map<String, Object> alertToMap(SIEMAlert a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("ruleId", a.getRuleId());
        m.put("ruleName", a.getRuleName());
        m.put("severity", a.getSeverity() != null ? a.getSeverity().name() : null);
        m.put("category", a.getCategory() != null ? a.getCategory().name() : null);
        m.put("title", a.getTitle());
        m.put("description", a.getDescription());
        m.put("eventCount", a.getEventCount());
        m.put("status", a.getStatus());
        m.put("requiresNotification", a.isRequiresNotification());
        m.put("dpdpSection", a.getDpdpSection());
        return m;
    }

    private ResponseEntity<?> queryTable(String table, int offset, int limit) {
        // Whitelist tables
        Set<String> allowed = Set.of("soar_playbooks", "threat_intel_feeds",
                "ueba_profiles", "forensic_timelines", "mitre_mappings",
                "siem_correlation_rules", "soar_executions", "siem_events", "siem_alerts");
        if (!allowed.contains(table)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid table"));
        }
        try {
            List<Map<String, Object>> rows = new ArrayList<>();
            String sql = "SELECT * FROM " + table + " ORDER BY rowid DESC LIMIT ? OFFSET ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                stmt.setInt(2, offset);
                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
            return ResponseEntity.ok(Map.of("data", rows, "offset", offset, "limit", limit));
        } catch (Exception e) {
            // Table may not exist yet if SIEM not initialized
            return ResponseEntity.ok(Map.of("data", List.of(), "offset", offset, "limit", limit));
        }
    }
}
