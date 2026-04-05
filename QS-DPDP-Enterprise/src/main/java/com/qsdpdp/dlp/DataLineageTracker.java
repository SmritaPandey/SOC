package com.qsdpdp.dlp;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Lineage Tracker - Tracks data movement from source to destination
 * Builds lineage graphs for breach impact analysis
 */
public class DataLineageTracker {
    private static final Logger logger = LoggerFactory.getLogger(DataLineageTracker.class);
    private final DatabaseManager dbManager;
    private boolean initialized = false;

    public DataLineageTracker(DatabaseManager dbManager) { this.dbManager = dbManager; }

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS data_lineage (id TEXT PRIMARY KEY, data_id TEXT, source_system TEXT, source_location TEXT, destination_system TEXT, destination_location TEXT, transfer_type TEXT, user_id TEXT, data_classification TEXT, bytes_transferred INTEGER, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, metadata TEXT)");
            stmt.execute("CREATE TABLE IF NOT EXISTS data_movement_graph (id TEXT PRIMARY KEY, data_id TEXT, node_type TEXT, node_id TEXT, parent_node_id TEXT, depth INTEGER DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        } catch (SQLException e) { logger.error("Failed to create lineage tables", e); }
        initialized = true;
        logger.info("DataLineageTracker initialized");
    }

    /** Record a data movement event */
    public String recordMovement(String dataId, String srcSystem, String srcLocation,
                                  String dstSystem, String dstLocation, String transferType,
                                  String userId, String classification, long bytes) {
        String id = UUID.randomUUID().toString();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO data_lineage (id, data_id, source_system, source_location, destination_system, destination_location, transfer_type, user_id, data_classification, bytes_transferred) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, id); ps.setString(2, dataId); ps.setString(3, srcSystem);
            ps.setString(4, srcLocation); ps.setString(5, dstSystem); ps.setString(6, dstLocation);
            ps.setString(7, transferType); ps.setString(8, userId);
            ps.setString(9, classification); ps.setLong(10, bytes);
            ps.executeUpdate();
        } catch (SQLException e) { logger.error("Failed to record data movement", e); }
        return id;
    }

    /** Get full lineage trail for a data asset */
    public List<LineageEntry> getLineage(String dataId) {
        List<LineageEntry> lineage = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM data_lineage WHERE data_id = ? ORDER BY timestamp ASC")) {
            ps.setString(1, dataId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LineageEntry entry = new LineageEntry();
                    entry.id = rs.getString("id"); entry.dataId = rs.getString("data_id");
                    entry.sourceSystem = rs.getString("source_system"); entry.sourceLocation = rs.getString("source_location");
                    entry.destSystem = rs.getString("destination_system"); entry.destLocation = rs.getString("destination_location");
                    entry.transferType = rs.getString("transfer_type"); entry.userId = rs.getString("user_id");
                    entry.classification = rs.getString("data_classification");
                    entry.bytesTransferred = rs.getLong("bytes_transferred");
                    entry.timestamp = rs.getString("timestamp");
                    lineage.add(entry);
                }
            }
        } catch (SQLException e) { logger.error("Failed to get lineage for {}", dataId, e); }
        return lineage;
    }

    /** Analyze breach impact - what data was exposed if a system was breached */
    public BreachImpactAnalysis analyzeBreachImpact(String compromisedSystem) {
        BreachImpactAnalysis analysis = new BreachImpactAnalysis();
        analysis.compromisedSystem = compromisedSystem;
        analysis.analysisTime = LocalDateTime.now();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT data_id, data_classification, bytes_transferred FROM data_lineage WHERE destination_system = ? OR source_system = ?")) {
            ps.setString(1, compromisedSystem); ps.setString(2, compromisedSystem);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dataId = rs.getString("data_id");
                    String classification = rs.getString("data_classification");
                    analysis.affectedDataIds.add(dataId);
                    analysis.classificationCounts.merge(classification != null ? classification : "UNKNOWN", 1, Integer::sum);
                    analysis.totalBytesAtRisk += rs.getLong("bytes_transferred");
                }
            }
        } catch (SQLException e) { logger.error("Breach impact analysis failed for {}", compromisedSystem, e); }
        analysis.totalDataAssetsAtRisk = analysis.affectedDataIds.size();
        analysis.riskLevel = analysis.totalDataAssetsAtRisk > 100 ? "CRITICAL" :
                             analysis.totalDataAssetsAtRisk > 50 ? "HIGH" :
                             analysis.totalDataAssetsAtRisk > 10 ? "MEDIUM" : "LOW";
        return analysis;
    }

    /** Get data movement summary for a user */
    public List<LineageEntry> getUserDataMovements(String userId, int limit) {
        List<LineageEntry> movements = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM data_lineage WHERE user_id = ? ORDER BY timestamp DESC LIMIT ?")) {
            ps.setString(1, userId); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LineageEntry entry = new LineageEntry();
                    entry.id = rs.getString("id"); entry.dataId = rs.getString("data_id");
                    entry.sourceSystem = rs.getString("source_system"); entry.destSystem = rs.getString("destination_system");
                    entry.transferType = rs.getString("transfer_type"); entry.userId = rs.getString("user_id");
                    entry.timestamp = rs.getString("timestamp");
                    movements.add(entry);
                }
            }
        } catch (SQLException e) { logger.error("Failed to get user movements for {}", userId, e); }
        return movements;
    }

    public boolean isInitialized() { return initialized; }

    public static class LineageEntry {
        public String id, dataId, sourceSystem, sourceLocation, destSystem, destLocation;
        public String transferType, userId, classification, timestamp;
        public long bytesTransferred;
    }

    public static class BreachImpactAnalysis {
        public String compromisedSystem, riskLevel;
        public LocalDateTime analysisTime;
        public Set<String> affectedDataIds = new HashSet<>();
        public Map<String, Integer> classificationCounts = new LinkedHashMap<>();
        public int totalDataAssetsAtRisk;
        public long totalBytesAtRisk;
    }
}
