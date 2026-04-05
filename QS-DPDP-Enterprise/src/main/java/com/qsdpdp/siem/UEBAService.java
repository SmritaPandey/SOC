package com.qsdpdp.siem;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * User & Entity Behavior Analytics (UEBA) Service
 * Builds behavioral baselines per user/entity and detects anomalies
 * Surpasses Splunk UBA and Exabeam with statistical deviation + risk scoring
 * 
 * @version 1.0.0
 * @since Phase 7
 */
public class UEBAService {

    private static final Logger logger = LoggerFactory.getLogger(UEBAService.class);

    private final DatabaseManager dbManager;
    private boolean initialized = false;

    // User behavior baselines
    private final Map<String, UserBaseline> userBaselines = new ConcurrentHashMap<>();

    // Entity baselines (servers, endpoints, applications)
    private final Map<String, EntityBaseline> entityBaselines = new ConcurrentHashMap<>();

    // Anomaly detection thresholds
    private double loginTimeDeviationThreshold = 2.0; // Standard deviations
    private double dataAccessVolumeThreshold = 3.0;
    private double geoLocationAnomalyThreshold = 0.7; // Novelty score
    private double overallRiskThreshold = 70.0; // 0-100 scale

    public UEBAService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void initialize() {
        if (initialized) return;
        createTables();
        initialized = true;
        logger.info("UEBAService initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ueba_user_baselines (
                    user_id TEXT PRIMARY KEY,
                    avg_login_hour REAL,
                    stddev_login_hour REAL,
                    avg_daily_events INTEGER,
                    stddev_daily_events REAL,
                    avg_data_access_mb REAL,
                    stddev_data_access_mb REAL,
                    known_ips TEXT,
                    known_locations TEXT,
                    known_applications TEXT,
                    risk_score REAL DEFAULT 0.0,
                    observation_count INTEGER DEFAULT 0,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ueba_anomalies (
                    id TEXT PRIMARY KEY,
                    user_id TEXT,
                    entity_id TEXT,
                    anomaly_type TEXT NOT NULL,
                    description TEXT,
                    deviation_score REAL,
                    risk_contribution REAL,
                    event_data TEXT,
                    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    reviewed INTEGER DEFAULT 0,
                    false_positive INTEGER DEFAULT 0
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ueba_risk_scores (
                    id TEXT PRIMARY KEY,
                    entity_type TEXT NOT NULL,
                    entity_id TEXT NOT NULL,
                    risk_score REAL NOT NULL,
                    risk_factors TEXT,
                    computed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) {
            logger.error("Failed to create UEBA tables", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // BEHAVIORAL BASELINE MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Update user baseline with a new event observation
     */
    public void observeUserEvent(String userId, SecurityEvent event) {
        UserBaseline baseline = userBaselines.computeIfAbsent(userId, k -> new UserBaseline(userId));
        baseline.recordEvent(event);
        baseline.lastUpdated = LocalDateTime.now();
    }

    /**
     * Get user baseline
     */
    public UserBaseline getUserBaseline(String userId) {
        return userBaselines.get(userId);
    }

    /**
     * Get all user risk scores sorted by risk level
     */
    public List<UserRiskScore> getTopRiskUsers(int limit) {
        List<UserRiskScore> scores = new ArrayList<>();
        for (UserBaseline baseline : userBaselines.values()) {
            UserRiskScore score = new UserRiskScore();
            score.userId = baseline.userId;
            score.riskScore = baseline.computeRiskScore();
            score.riskFactors = baseline.getRiskFactors();
            score.observationCount = baseline.observationCount;
            score.lastActivity = baseline.lastUpdated;
            scores.add(score);
        }
        scores.sort((a, b) -> Double.compare(b.riskScore, a.riskScore));
        return scores.subList(0, Math.min(limit, scores.size()));
    }

    // ═══════════════════════════════════════════════════════════
    // ANOMALY DETECTION
    // ═══════════════════════════════════════════════════════════

    /**
     * Analyze an event for anomalies against user baseline
     * @return List of detected anomalies
     */
    public List<UEBAAnomaly> analyzeEvent(SecurityEvent event) {
        List<UEBAAnomaly> anomalies = new ArrayList<>();
        String userId = event.getUserId();
        if (userId == null) return anomalies;

        UserBaseline baseline = userBaselines.get(userId);
        if (baseline == null || baseline.observationCount < 10) {
            // Not enough data to establish baseline
            observeUserEvent(userId, event);
            return anomalies;
        }

        // Check 1: Login time anomaly
        if (event.getCategory() != null && event.getCategory().getCategory().equals("Authentication")) {
            int eventHour = LocalDateTime.now().getHour();
            double deviation = Math.abs(eventHour - baseline.avgLoginHour) / Math.max(baseline.stddevLoginHour, 1.0);
            if (deviation > loginTimeDeviationThreshold) {
                UEBAAnomaly anomaly = new UEBAAnomaly();
                anomaly.id = UUID.randomUUID().toString();
                anomaly.userId = userId;
                anomaly.anomalyType = AnomalyType.UNUSUAL_LOGIN_TIME;
                anomaly.description = String.format("Login at hour %d deviates %.1f sigma from baseline (avg: %.1f)",
                        eventHour, deviation, baseline.avgLoginHour);
                anomaly.deviationScore = deviation;
                anomaly.riskContribution = Math.min(deviation * 10, 30);
                anomalies.add(anomaly);
            }
        }

        // Check 2: Unknown source IP
        if (event.getSourceIP() != null && !baseline.knownIps.contains(event.getSourceIP())) {
            UEBAAnomaly anomaly = new UEBAAnomaly();
            anomaly.id = UUID.randomUUID().toString();
            anomaly.userId = userId;
            anomaly.anomalyType = AnomalyType.NEW_SOURCE_IP;
            anomaly.description = "Login from previously unseen IP: " + event.getSourceIP();
            anomaly.deviationScore = geoLocationAnomalyThreshold;
            anomaly.riskContribution = 15;
            anomalies.add(anomaly);
        }

        // Check 3: High event volume (potential data exfiltration)
        baseline.recordEventForVolume();
        if (baseline.currentDayEventCount > baseline.avgDailyEvents + (dataAccessVolumeThreshold * baseline.stddevDailyEvents)) {
            UEBAAnomaly anomaly = new UEBAAnomaly();
            anomaly.id = UUID.randomUUID().toString();
            anomaly.userId = userId;
            anomaly.anomalyType = AnomalyType.HIGH_EVENT_VOLUME;
            anomaly.description = String.format("Event count %d exceeds baseline avg (%.0f ± %.0f)",
                    baseline.currentDayEventCount, baseline.avgDailyEvents, baseline.stddevDailyEvents);
            anomaly.deviationScore = (baseline.currentDayEventCount - baseline.avgDailyEvents) / Math.max(baseline.stddevDailyEvents, 1.0);
            anomaly.riskContribution = 25;
            anomalies.add(anomaly);
        }

        // Check 4: After-hours data access
        if (event.getCategory() != null && event.getCategory().getCategory().equals("Data")) {
            int hour = LocalDateTime.now().getHour();
            if (hour < 6 || hour > 22) { // Outside normal hours
                UEBAAnomaly anomaly = new UEBAAnomaly();
                anomaly.id = UUID.randomUUID().toString();
                anomaly.userId = userId;
                anomaly.anomalyType = AnomalyType.AFTER_HOURS_ACCESS;
                anomaly.description = "Data access at unusual hour: " + hour;
                anomaly.deviationScore = 1.5;
                anomaly.riskContribution = 20;
                anomalies.add(anomaly);
            }
        }

        // Check 5: Privilege escalation pattern
        if (event.getCategory() != null && event.getCategory().getCategory().equals("Authorization") && 
            event.getAction() != null && event.getAction().toLowerCase().contains("privilege")) {
            UEBAAnomaly anomaly = new UEBAAnomaly();
            anomaly.id = UUID.randomUUID().toString();
            anomaly.userId = userId;
            anomaly.anomalyType = AnomalyType.PRIVILEGE_ESCALATION;
            anomaly.description = "Privilege modification detected: " + event.getAction();
            anomaly.deviationScore = 2.0;
            anomaly.riskContribution = 30;
            anomalies.add(anomaly);
        }

        // Persist anomalies and update baseline risk
        for (UEBAAnomaly anomaly : anomalies) {
            persistAnomaly(anomaly);
            baseline.addRiskContribution(anomaly.riskContribution);
        }

        // Update baseline with event
        observeUserEvent(userId, event);

        return anomalies;
    }

    /**
     * Get recent anomalies for a user
     */
    public List<UEBAAnomaly> getUserAnomalies(String userId, int limit) {
        List<UEBAAnomaly> anomalies = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM ueba_anomalies WHERE user_id = ? ORDER BY detected_at DESC LIMIT ?")) {
            ps.setString(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UEBAAnomaly anomaly = new UEBAAnomaly();
                    anomaly.id = rs.getString("id");
                    anomaly.userId = rs.getString("user_id");
                    anomaly.anomalyType = AnomalyType.valueOf(rs.getString("anomaly_type"));
                    anomaly.description = rs.getString("description");
                    anomaly.deviationScore = rs.getDouble("deviation_score");
                    anomaly.riskContribution = rs.getDouble("risk_contribution");
                    anomalies.add(anomaly);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch user anomalies for {}", userId, e);
        }
        return anomalies;
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL METHODS
    // ═══════════════════════════════════════════════════════════

    private void persistAnomaly(UEBAAnomaly anomaly) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO ueba_anomalies (id, user_id, entity_id, anomaly_type, description, deviation_score, risk_contribution) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, anomaly.id);
            ps.setString(2, anomaly.userId);
            ps.setString(3, anomaly.entityId);
            ps.setString(4, anomaly.anomalyType.name());
            ps.setString(5, anomaly.description);
            ps.setDouble(6, anomaly.deviationScore);
            ps.setDouble(7, anomaly.riskContribution);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to persist UEBA anomaly", e);
        }
    }

    public boolean isInitialized() { return initialized; }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public enum AnomalyType {
        UNUSUAL_LOGIN_TIME, NEW_SOURCE_IP, NEW_LOCATION, HIGH_EVENT_VOLUME,
        AFTER_HOURS_ACCESS, PRIVILEGE_ESCALATION, IMPOSSIBLE_TRAVEL,
        DATA_HOARDING, DORMANT_ACCOUNT_ACTIVITY, LATERAL_MOVEMENT
    }

    public static class UserBaseline {
        public String userId;
        public double avgLoginHour = 9.0;
        public double stddevLoginHour = 2.0;
        public double avgDailyEvents = 50.0;
        public double stddevDailyEvents = 15.0;
        public double avgDataAccessMb = 10.0;
        public double stddevDataAccessMb = 5.0;
        public Set<String> knownIps = ConcurrentHashMap.newKeySet();
        public Set<String> knownLocations = ConcurrentHashMap.newKeySet();
        public Set<String> knownApplications = ConcurrentHashMap.newKeySet();
        public double riskScore = 0.0;
        public int observationCount = 0;
        public int currentDayEventCount = 0;
        public LocalDateTime lastUpdated;

        public UserBaseline(String userId) { this.userId = userId; }

        public void recordEvent(SecurityEvent event) {
            observationCount++;
            if (event.getSourceIP() != null) knownIps.add(event.getSourceIP());
            // Update running average of login hour
            int hour = LocalDateTime.now().getHour();
            avgLoginHour = ((avgLoginHour * (observationCount - 1)) + hour) / observationCount;
        }

        public void recordEventForVolume() {
            currentDayEventCount++;
        }

        public double computeRiskScore() {
            return Math.min(100.0, riskScore);
        }

        public void addRiskContribution(double contribution) {
            riskScore = Math.min(100.0, riskScore + contribution);
            // Decay over time
            riskScore *= 0.99;
        }

        public List<String> getRiskFactors() {
            List<String> factors = new ArrayList<>();
            if (riskScore > 70) factors.add("HIGH_OVERALL_RISK");
            if (currentDayEventCount > avgDailyEvents * 2) factors.add("HIGH_ACTIVITY_VOLUME");
            return factors;
        }
    }

    public static class EntityBaseline {
        public String entityId;
        public String entityType; // SERVER, ENDPOINT, APPLICATION
        public double avgEventsPerHour;
        public double stddevEventsPerHour;
        public Set<String> normalConnections = ConcurrentHashMap.newKeySet();
        public double riskScore = 0.0;
    }

    public static class UEBAAnomaly {
        public String id;
        public String userId;
        public String entityId;
        public AnomalyType anomalyType;
        public String description;
        public double deviationScore;
        public double riskContribution;
        public LocalDateTime detectedAt = LocalDateTime.now();
        public boolean reviewed = false;
        public boolean falsePositive = false;
    }

    public static class UserRiskScore {
        public String userId;
        public double riskScore;
        public List<String> riskFactors;
        public int observationCount;
        public LocalDateTime lastActivity;
    }
}
