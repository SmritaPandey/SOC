package com.qsdpdp.audit;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Audit Service for QS-DPDP Enterprise
 * Immutable hash-chained audit logging for compliance evidence
 * 
 * @version 1.0.0
 * @since Phase 1
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final DatabaseManager dbManager;
    private boolean initialized = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final BlockingQueue<AuditEntry> auditQueue = new LinkedBlockingQueue<>(10000);
    private volatile boolean running = false;

    private String lastHash = "GENESIS";
    private long sequenceNumber = 0;

    @Autowired
    public AuditService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        logger.info("Initializing Audit Service...");

        // Load last hash and sequence from database
        loadLastAuditState();

        // Start async audit processor
        running = true;
        executor.submit(this::processAuditQueue);

        initialized = true;
        logger.info("Audit Service initialized (sequence: {})", sequenceNumber);
    }

    private void loadLastAuditState() {
        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT sequence_number, hash FROM audit_log ORDER BY sequence_number DESC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    sequenceNumber = rs.getLong("sequence_number");
                    lastHash = rs.getString("hash");
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load last audit state, starting fresh", e);
        }
    }

    /**
     * Log an audit entry asynchronously
     */
    public void log(String action, String module, String actor, String details) {
        log(action, module, actor, null, null, null, details, null, null);
    }

    /**
     * Log an audit entry with full details
     */
    public void log(String action, String module, String actor, String entityType,
            String entityId, String actorRole, String details,
            String dpdpSection, String controlId) {

        AuditEntry entry = new AuditEntry();
        entry.action = action;
        entry.module = module;
        entry.actor = actor;
        entry.entityType = entityType;
        entry.entityId = entityId;
        entry.actorRole = actorRole;
        entry.details = details;
        entry.dpdpSection = dpdpSection;
        entry.controlId = controlId;
        entry.timestamp = LocalDateTime.now();

        try {
            auditQueue.offer(entry, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while queueing audit entry", e);
        }
    }

    /**
     * Log an audit entry synchronously (for critical events)
     */
    public void logSync(String action, String module, String actor, String details) {
        AuditEntry entry = new AuditEntry();
        entry.action = action;
        entry.module = module;
        entry.actor = actor;
        entry.details = details;
        entry.timestamp = LocalDateTime.now();

        persistAuditEntry(entry);
    }

    private void processAuditQueue() {
        logger.info("Audit processor started");

        while (running) {
            try {
                AuditEntry entry = auditQueue.poll(100, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    persistAuditEntry(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error processing audit entry", e);
            }
        }

        // Drain remaining entries on shutdown
        while (!auditQueue.isEmpty()) {
            try {
                AuditEntry entry = auditQueue.poll();
                if (entry != null) {
                    persistAuditEntry(entry);
                }
            } catch (Exception e) {
                logger.error("Error draining audit queue", e);
            }
        }

        logger.info("Audit processor stopped");
    }

    private synchronized void persistAuditEntry(AuditEntry entry) {
        try (Connection conn = dbManager.getConnection()) {
            sequenceNumber++;

            // Calculate hash for immutability
            String dataToHash = String.format("%d|%s|%s|%s|%s|%s|%s",
                    sequenceNumber, entry.timestamp, entry.action, entry.module,
                    entry.actor, entry.details, lastHash);
            String currentHash = calculateHash(dataToHash);

            String sql = """
                        INSERT INTO audit_log (id, sequence_number, timestamp, event_type, module, action,
                            actor, actor_role, entity_type, entity_id, details,
                            dpdp_section, control_id, hash, prev_hash)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setLong(2, sequenceNumber);
                stmt.setString(3, entry.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                stmt.setString(4, entry.action);
                stmt.setString(5, entry.module);
                stmt.setString(6, entry.action);
                stmt.setString(7, entry.actor);
                stmt.setString(8, entry.actorRole);
                stmt.setString(9, entry.entityType);
                stmt.setString(10, entry.entityId);
                stmt.setString(11, entry.details);
                stmt.setString(12, entry.dpdpSection);
                stmt.setString(13, entry.controlId);
                stmt.setString(14, currentHash);
                stmt.setString(15, lastHash);

                stmt.executeUpdate();
            }

            lastHash = currentHash;

            logger.trace("Audit entry persisted: {} (seq {})", entry.action, sequenceNumber);

        } catch (Exception e) {
            logger.error("Failed to persist audit entry", e);
        }
    }

    private String calculateHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.error("Failed to calculate hash", e);
            return "HASH_ERROR";
        }
    }

    /**
     * Verify audit log integrity
     */
    public AuditIntegrityReport verifyIntegrity() {
        logger.info("Verifying audit log integrity...");

        int totalEntries = 0;
        int validEntries = 0;
        int invalidEntries = 0;
        String firstInvalidId = null;

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT id, sequence_number, timestamp, action, module, actor, details, hash, prev_hash FROM audit_log ORDER BY sequence_number ASC";

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {

                String expectedPrevHash = "GENESIS";

                while (rs.next()) {
                    totalEntries++;

                    long seq = rs.getLong("sequence_number");
                    String timestamp = rs.getString("timestamp");
                    String action = rs.getString("action");
                    String module = rs.getString("module");
                    String actor = rs.getString("actor");
                    String details = rs.getString("details");
                    String storedHash = rs.getString("hash");
                    String storedPrevHash = rs.getString("prev_hash");

                    // Verify prev_hash chain
                    if (!storedPrevHash.equals(expectedPrevHash)) {
                        invalidEntries++;
                        if (firstInvalidId == null) {
                            firstInvalidId = rs.getString("id");
                        }
                    } else {
                        // Verify current hash
                        String dataToHash = String.format("%d|%s|%s|%s|%s|%s|%s",
                                seq, timestamp, action, module, actor, details, storedPrevHash);
                        String calculatedHash = calculateHash(dataToHash);

                        if (calculatedHash.equals(storedHash)) {
                            validEntries++;
                        } else {
                            invalidEntries++;
                            if (firstInvalidId == null) {
                                firstInvalidId = rs.getString("id");
                            }
                        }
                    }

                    expectedPrevHash = storedHash;
                }
            }
        } catch (Exception e) {
            logger.error("Error verifying audit integrity", e);
        }

        boolean isValid = invalidEntries == 0;
        logger.info("Audit integrity check complete: {} valid, {} invalid", validEntries, invalidEntries);

        return new AuditIntegrityReport(isValid, totalEntries, validEntries, invalidEntries, firstInvalidId);
    }

    /**
     * Get recent audit entries
     */
    public java.util.List<AuditLogEntry> getRecentEntries(int limit) {
        java.util.List<AuditLogEntry> entries = new java.util.ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {
            String sql = "SELECT * FROM audit_log ORDER BY sequence_number DESC LIMIT ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        AuditLogEntry entry = new AuditLogEntry(
                                rs.getString("id"),
                                rs.getLong("sequence_number"),
                                rs.getString("timestamp"),
                                rs.getString("event_type"),
                                rs.getString("module"),
                                rs.getString("action"),
                                rs.getString("actor"),
                                rs.getString("details"));
                        entries.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting recent audit entries", e);
        }

        return entries;
    }

    public void shutdown() {
        logger.info("Shutting down Audit Service...");
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Audit Service shutdown complete");
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // Internal audit entry holder
    private static class AuditEntry {
        String action;
        String module;
        String actor;
        String actorRole;
        String entityType;
        String entityId;
        String details;
        String dpdpSection;
        String controlId;
        LocalDateTime timestamp;
    }
}
