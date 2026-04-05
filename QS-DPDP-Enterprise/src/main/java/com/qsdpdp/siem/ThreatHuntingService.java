package com.qsdpdp.siem;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class ThreatHuntingService {
    private static final Logger logger = LoggerFactory.getLogger(ThreatHuntingService.class);
    private final DatabaseManager dbManager;
    private final ThreatIntelligenceService threatIntel;
    private boolean initialized = false;
    private final Map<String, HuntingQuery> savedQueries = new LinkedHashMap<>();

    public ThreatHuntingService(DatabaseManager dbManager, ThreatIntelligenceService threatIntel) {
        this.dbManager = dbManager;
        this.threatIntel = threatIntel;
    }

    public void initialize() {
        if (initialized) return;
        createTables();
        loadDefaultHuntingQueries();
        initialized = true;
        logger.info("ThreatHuntingService initialized with {} queries", savedQueries.size());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS threat_hunts (id TEXT PRIMARY KEY, name TEXT, description TEXT, query_type TEXT, status TEXT DEFAULT 'CREATED', results_count INTEGER DEFAULT 0, started_at TIMESTAMP, completed_at TIMESTAMP, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS hunt_results (id TEXT PRIMARY KEY, hunt_id TEXT, event_id TEXT, match_type TEXT, match_value TEXT, context TEXT, severity TEXT, mitre_technique TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) { logger.error("Failed to create threat hunting tables", e); }
    }

    public HuntResult huntForIndicator(String indicator, LocalDateTime start, LocalDateTime end) {
        HuntResult result = new HuntResult(UUID.randomUUID().toString(), indicator, HuntType.IOC_SEARCH);
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT * FROM security_events WHERE (source_ip LIKE ? OR destination_ip LIKE ? OR user_id LIKE ? OR action LIKE ?) AND timestamp BETWEEN ? AND ? ORDER BY timestamp DESC LIMIT 1000";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String p = "%" + indicator + "%";
                ps.setString(1, p); ps.setString(2, p); ps.setString(3, p); ps.setString(4, p);
                ps.setString(5, start.toString()); ps.setString(6, end.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.matches.add(new HuntMatch(rs.getString("id"), rs.getString("timestamp"),
                            rs.getString("source_ip"), rs.getString("user_id"), rs.getString("action"),
                            rs.getString("category"), rs.getString("severity"), null));
                    }
                }
            }
            if (threatIntel != null && threatIntel.isInitialized()) {
                var rep = threatIntel.checkReputation(indicator);
                if (rep.matched) { result.threatIntelMatch = true; result.threatIntelRiskScore = rep.riskScore; }
            }
            result.status = "COMPLETED"; result.resultsCount = result.matches.size();
        } catch (SQLException e) { logger.error("Hunt failed: {}", indicator, e); result.status = "FAILED"; }
        return result;
    }

    public HuntResult huntByMITRETechnique(String techId, LocalDateTime start, LocalDateTime end) {
        HuntResult result = new HuntResult(UUID.randomUUID().toString(), techId, HuntType.MITRE_TECHNIQUE);
        Map<String, List<String>> patterns = Map.of(
            "T1078", List.of("login","authentication"), "T1110", List.of("brute_force","failed_login"),
            "T1048", List.of("exfiltration","data_transfer"), "T1059", List.of("command","script","powershell"),
            "T1021", List.of("remote","ssh","rdp"), "T1570", List.of("lateral","movement","psexec"));
        List<String> searchPatterns = patterns.getOrDefault(techId, List.of());
        try (Connection conn = dbManager.getConnection()) {
            for (String pat : searchPatterns) {
                String sql = "SELECT * FROM security_events WHERE (action LIKE ? OR resource LIKE ?) AND timestamp BETWEEN ? AND ? LIMIT 500";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, "%" + pat + "%"); ps.setString(2, "%" + pat + "%");
                    ps.setString(3, start.toString()); ps.setString(4, end.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            result.matches.add(new HuntMatch(rs.getString("id"), rs.getString("timestamp"),
                                rs.getString("source_ip"), rs.getString("user_id"), rs.getString("action"),
                                rs.getString("category"), rs.getString("severity"), techId));
                        }
                    }
                }
            }
            result.status = "COMPLETED"; result.resultsCount = result.matches.size();
        } catch (SQLException e) { logger.error("MITRE hunt failed: {}", techId, e); result.status = "FAILED"; }
        return result;
    }

    public Map<String, HuntingQuery> getSavedQueries() { return Collections.unmodifiableMap(savedQueries); }
    public void saveQuery(HuntingQuery q) { savedQueries.put(q.id, q); }
    public boolean isInitialized() { return initialized; }

    private void loadDefaultHuntingQueries() {
        saveQuery(new HuntingQuery("hunt-brute-force", "Brute Force Detection", "AUTHENTICATION", "HIGH", "failed_login"));
        saveQuery(new HuntingQuery("hunt-data-exfil", "Data Exfiltration", "DATA_ACCESS", null, "export"));
        saveQuery(new HuntingQuery("hunt-priv-esc", "Privilege Escalation", "AUTHORIZATION", "CRITICAL", "privilege"));
        saveQuery(new HuntingQuery("hunt-lateral", "Lateral Movement", "NETWORK", null, "lateral"));
        saveQuery(new HuntingQuery("hunt-insider", "Insider Threat", "DATA_ACCESS", null, null));
    }

    public enum HuntType { IOC_SEARCH, MITRE_TECHNIQUE, PATTERN_SEARCH }
    public static class HuntingQuery {
        public String id, name, categoryFilter, severityFilter, actionPattern;
        public HuntingQuery(String id, String name, String cat, String sev, String action) {
            this.id = id; this.name = name; this.categoryFilter = cat; this.severityFilter = sev; this.actionPattern = action;
        }
    }
    public static class HuntResult {
        public String id, indicator, status = "CREATED";
        public HuntType huntType;
        public int resultsCount;
        public List<HuntMatch> matches = new ArrayList<>();
        public boolean threatIntelMatch; public double threatIntelRiskScore;
        public HuntResult(String id, String indicator, HuntType type) { this.id = id; this.indicator = indicator; this.huntType = type; }
    }
    public static class HuntMatch {
        public String eventId, timestamp, sourceIp, userId, action, category, severity, mitreTechnique;
        public HuntMatch(String eid, String ts, String sip, String uid, String act, String cat, String sev, String mitre) {
            this.eventId=eid; this.timestamp=ts; this.sourceIp=sip; this.userId=uid; this.action=act; this.category=cat; this.severity=sev; this.mitreTechnique=mitre;
        }
    }
}
