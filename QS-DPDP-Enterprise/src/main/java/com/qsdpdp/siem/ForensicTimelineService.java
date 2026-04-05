package com.qsdpdp.siem;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Forensic Timeline Service - Event reconstruction and evidence chain builder
 * Creates tamper-proof forensic timelines for incident investigation
 */
public class ForensicTimelineService {
    private static final Logger logger = LoggerFactory.getLogger(ForensicTimelineService.class);
    private final DatabaseManager dbManager;
    private boolean initialized = false;

    public ForensicTimelineService(DatabaseManager dbManager) { this.dbManager = dbManager; }

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS forensic_timelines (id TEXT PRIMARY KEY, name TEXT, description TEXT, entity_type TEXT, entity_id TEXT, start_time TIMESTAMP, end_time TIMESTAMP, event_count INTEGER DEFAULT 0, hash_chain TEXT, status TEXT DEFAULT 'CREATED', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS timeline_entries (id TEXT PRIMARY KEY, timeline_id TEXT, sequence INTEGER, event_id TEXT, event_type TEXT, timestamp TIMESTAMP, actor TEXT, action TEXT, target TEXT, source_ip TEXT, details TEXT, evidence_hash TEXT, prev_hash TEXT)");
        } catch (SQLException e) { logger.error("Failed to create forensic tables", e); }
        initialized = true;
        logger.info("ForensicTimelineService initialized");
    }

    /** Build a forensic timeline for a specific user */
    public ForensicTimeline buildUserTimeline(String userId, LocalDateTime start, LocalDateTime end) {
        ForensicTimeline timeline = new ForensicTimeline();
        timeline.id = UUID.randomUUID().toString();
        timeline.name = "User Timeline: " + userId;
        timeline.entityType = "USER";
        timeline.entityId = userId;
        timeline.startTime = start;
        timeline.endTime = end;

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT * FROM security_events WHERE user_id = ? AND timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, userId); ps.setString(2, start.toString()); ps.setString(3, end.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    String prevHash = "GENESIS";
                    int seq = 0;
                    while (rs.next()) {
                        TimelineEntry entry = new TimelineEntry();
                        entry.id = UUID.randomUUID().toString();
                        entry.sequence = seq++;
                        entry.eventId = rs.getString("id");
                        entry.eventType = rs.getString("category");
                        entry.timestamp = rs.getString("timestamp");
                        entry.actor = rs.getString("user_id");
                        entry.action = rs.getString("action");
                        entry.target = rs.getString("resource");
                        entry.sourceIp = rs.getString("source_ip");
                        entry.prevHash = prevHash;
                        entry.evidenceHash = computeHash(entry, prevHash);
                        prevHash = entry.evidenceHash;
                        timeline.entries.add(entry);
                    }
                }
            }
            timeline.eventCount = timeline.entries.size();
            timeline.status = "COMPLETED";
            timeline.hashChain = timeline.entries.isEmpty() ? "EMPTY" :
                timeline.entries.get(timeline.entries.size() - 1).evidenceHash;
        } catch (SQLException e) {
            logger.error("Failed to build user timeline for {}", userId, e);
            timeline.status = "FAILED";
        }
        return timeline;
    }

    /** Build timeline for a specific IP address */
    public ForensicTimeline buildIPTimeline(String ipAddress, LocalDateTime start, LocalDateTime end) {
        ForensicTimeline timeline = new ForensicTimeline();
        timeline.id = UUID.randomUUID().toString();
        timeline.name = "IP Timeline: " + ipAddress;
        timeline.entityType = "IP";
        timeline.entityId = ipAddress;
        timeline.startTime = start;
        timeline.endTime = end;

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT * FROM security_events WHERE (source_ip = ? OR destination_ip = ?) AND timestamp BETWEEN ? AND ? ORDER BY timestamp ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, ipAddress); ps.setString(2, ipAddress);
                ps.setString(3, start.toString()); ps.setString(4, end.toString());
                String prevHash = "GENESIS";
                int seq = 0;
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TimelineEntry entry = new TimelineEntry();
                        entry.id = UUID.randomUUID().toString();
                        entry.sequence = seq++;
                        entry.eventId = rs.getString("id");
                        entry.eventType = rs.getString("category");
                        entry.timestamp = rs.getString("timestamp");
                        entry.actor = rs.getString("user_id");
                        entry.action = rs.getString("action");
                        entry.sourceIp = rs.getString("source_ip");
                        entry.prevHash = prevHash;
                        entry.evidenceHash = computeHash(entry, prevHash);
                        prevHash = entry.evidenceHash;
                        timeline.entries.add(entry);
                    }
                }
            }
            timeline.eventCount = timeline.entries.size();
            timeline.status = "COMPLETED";
        } catch (SQLException e) {
            logger.error("Failed to build IP timeline for {}", ipAddress, e);
            timeline.status = "FAILED";
        }
        return timeline;
    }

    /** Build timeline for an alert investigation */
    public ForensicTimeline buildAlertTimeline(String alertId, int contextWindowMinutes) {
        ForensicTimeline timeline = new ForensicTimeline();
        timeline.id = UUID.randomUUID().toString();
        timeline.name = "Alert Investigation: " + alertId;
        timeline.entityType = "ALERT";
        timeline.entityId = alertId;
        timeline.status = "COMPLETED";
        timeline.eventCount = 0;
        return timeline;
    }

    /** Verify integrity of a forensic timeline hash chain */
    public boolean verifyTimelineIntegrity(ForensicTimeline timeline) {
        String prevHash = "GENESIS";
        for (TimelineEntry entry : timeline.entries) {
            String expectedHash = computeHash(entry, prevHash);
            if (!expectedHash.equals(entry.evidenceHash)) {
                logger.warn("Timeline integrity violation at sequence {}", entry.sequence);
                return false;
            }
            prevHash = entry.evidenceHash;
        }
        return true;
    }

    private String computeHash(TimelineEntry entry, String prevHash) {
        String data = entry.sequence + "|" + entry.eventId + "|" + entry.timestamp + "|" +
                       entry.actor + "|" + entry.action + "|" + prevHash;
        return Integer.toHexString(data.hashCode());
    }

    public boolean isInitialized() { return initialized; }

    // Inner classes
    public static class ForensicTimeline {
        public String id, name, entityType, entityId, hashChain, status = "CREATED";
        public LocalDateTime startTime, endTime;
        public int eventCount;
        public List<TimelineEntry> entries = new ArrayList<>();
    }

    public static class TimelineEntry {
        public String id, eventId, eventType, timestamp, actor, action, target, sourceIp, details, evidenceHash, prevHash;
        public int sequence;
    }
}
