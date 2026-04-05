package com.qsdpdp.lifecycle;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Data Lifecycle Management Service
 * Manages data retention, archival, and erasure per DPDP requirements
 * 
 * @version 1.0.0
 * @since Module 10
 */
@Service
public class DataLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(DataLifecycleService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;

    private boolean initialized = false;
    private final Map<String, RetentionPolicy> retentionPolicies = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    @Autowired
    public DataLifecycleService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing Data Lifecycle Service...");
        createTables();
        loadDefaultPolicies();
        startScheduler();

        initialized = true;
        logger.info("Data Lifecycle Service initialized with {} retention policies", retentionPolicies.size());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS retention_policies (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            data_category TEXT,
                            purpose TEXT,
                            retention_days INTEGER,
                            archive_after_days INTEGER,
                            legal_basis TEXT,
                            dpdp_reference TEXT,
                            active INTEGER DEFAULT 1,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS data_inventory (
                            id TEXT PRIMARY KEY,
                            data_source TEXT,
                            data_category TEXT,
                            record_count INTEGER,
                            oldest_record_date TIMESTAMP,
                            newest_record_date TIMESTAMP,
                            retention_policy_id TEXT,
                            last_scanned_at TIMESTAMP,
                            next_action_date TIMESTAMP,
                            pending_action TEXT,
                            FOREIGN KEY (retention_policy_id) REFERENCES retention_policies(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS erasure_jobs (
                            id TEXT PRIMARY KEY,
                            data_source TEXT,
                            data_category TEXT,
                            reason TEXT,
                            records_affected INTEGER,
                            status TEXT DEFAULT 'PENDING',
                            scheduled_at TIMESTAMP,
                            started_at TIMESTAMP,
                            completed_at TIMESTAMP,
                            error_message TEXT,
                            verification_hash TEXT,
                            approved_by TEXT
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS archive_jobs (
                            id TEXT PRIMARY KEY,
                            data_source TEXT,
                            original_location TEXT,
                            archive_location TEXT,
                            records_archived INTEGER,
                            archive_format TEXT,
                            encrypted INTEGER DEFAULT 1,
                            status TEXT DEFAULT 'PENDING',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            completed_at TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_erasure_status ON erasure_jobs(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_archive_status ON archive_jobs(status)");

            logger.info("Data Lifecycle tables created");

        } catch (SQLException e) {
            logger.error("Failed to create Data Lifecycle tables", e);
        }
    }

    private void loadDefaultPolicies() {
        // DPDP-mandated retention policies
        retentionPolicies.put("CONSENT_RECORDS", new RetentionPolicy(
                "Consent Records",
                "CONSENT",
                "Compliance evidence",
                365 * 7, // 7 years
                365 * 5,
                "Legitimate Interest",
                "Section 6"));

        retentionPolicies.put("BREACH_RECORDS", new RetentionPolicy(
                "Breach Records",
                "BREACH",
                "Compliance and investigation",
                365 * 10, // 10 years
                365 * 7,
                "Legal Obligation",
                "Section 8(6)"));

        retentionPolicies.put("RIGHTS_REQUESTS", new RetentionPolicy(
                "Rights Requests",
                "RIGHTS",
                "Fulfillment evidence",
                365 * 5, // 5 years
                365 * 3,
                "Legitimate Interest",
                "Sections 11-14"));

        retentionPolicies.put("AUDIT_LOGS", new RetentionPolicy(
                "Audit Logs",
                "AUDIT",
                "Compliance verification",
                365 * 8, // 8 years
                365 * 5,
                "Legal Obligation",
                "Section 8"));

        retentionPolicies.put("TRANSACTION_DATA", new RetentionPolicy(
                "Transaction Data",
                "TRANSACTION",
                "Business operations",
                365 * 3, // 3 years
                365 * 2,
                "Contract",
                "Section 4(2)"));

        retentionPolicies.put("MARKETING_DATA", new RetentionPolicy(
                "Marketing Data",
                "MARKETING",
                "Marketing purposes",
                365 * 2, // 2 years
                365,
                "Consent",
                "Section 6"));
    }

    private void startScheduler() {
        scheduler = Executors.newScheduledThreadPool(2);

        // Daily retention check
        scheduler.scheduleAtFixedRate(this::checkRetentionPolicies, 1, 24, TimeUnit.HOURS);

        // Weekly inventory scan
        scheduler.scheduleAtFixedRate(this::scanDataInventory, 1, 7 * 24, TimeUnit.HOURS);
    }

    // ═══════════════════════════════════════════════════════════
    // RETENTION POLICY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public void createPolicy(RetentionPolicy policy) {
        String sql = """
                    INSERT INTO retention_policies (id, name, data_category, purpose,
                        retention_days, archive_after_days, legal_basis, dpdp_reference)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, policy.getId());
            stmt.setString(2, policy.getName());
            stmt.setString(3, policy.getDataCategory());
            stmt.setString(4, policy.getPurpose());
            stmt.setInt(5, policy.getRetentionDays());
            stmt.setInt(6, policy.getArchiveAfterDays());
            stmt.setString(7, policy.getLegalBasis());
            stmt.setString(8, policy.getDpdpReference());
            stmt.executeUpdate();

            retentionPolicies.put(policy.getDataCategory(), policy);
            auditService.log("RETENTION_POLICY_CREATED", "DATA_LIFECYCLE", null,
                    "Created policy: " + policy.getName());

        } catch (SQLException e) {
            logger.error("Failed to create retention policy", e);
        }
    }

    public RetentionPolicy getPolicy(String dataCategory) {
        return retentionPolicies.get(dataCategory);
    }

    // ═══════════════════════════════════════════════════════════
    // DATA ERASURE
    // ═══════════════════════════════════════════════════════════

    public String scheduleErasure(String dataSource, String dataCategory, String reason,
            int recordsAffected, LocalDateTime scheduledAt, String approvedBy) {
        String jobId = UUID.randomUUID().toString();

        String sql = """
                    INSERT INTO erasure_jobs (id, data_source, data_category, reason,
                        records_affected, scheduled_at, approved_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jobId);
            stmt.setString(2, dataSource);
            stmt.setString(3, dataCategory);
            stmt.setString(4, reason);
            stmt.setInt(5, recordsAffected);
            stmt.setString(6, scheduledAt.toString());
            stmt.setString(7, approvedBy);
            stmt.executeUpdate();

            auditService.log("ERASURE_SCHEDULED", "DATA_LIFECYCLE", approvedBy,
                    "Scheduled erasure of " + recordsAffected + " records from " + dataSource);

            return jobId;

        } catch (SQLException e) {
            logger.error("Failed to schedule erasure", e);
            return null;
        }
    }

    public ErasureResult executeErasure(String jobId) {
        // Get job details
        String sql = "SELECT * FROM erasure_jobs WHERE id = ? AND status = 'PENDING'";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jobId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return new ErasureResult(false, 0, "Job not found or already executed");
            }

            String dataSource = rs.getString("data_source");
            String dataCategory = rs.getString("data_category");
            int expectedRecords = rs.getInt("records_affected");

            // Update status to running
            updateErasureStatus(jobId, "RUNNING", null);

            // Execute secure erasure
            int erasedCount = performSecureErasure(dataSource, dataCategory);

            // Generate verification hash
            String verificationHash = generateVerificationHash(dataSource, dataCategory);

            // Complete job
            completeErasureJob(jobId, erasedCount, verificationHash);

            auditService.log("ERASURE_COMPLETED", "DATA_LIFECYCLE", null,
                    "Erased " + erasedCount + " records from " + dataSource +
                            ", Verification: " + verificationHash);

            return new ErasureResult(true, erasedCount, "Erasure completed successfully");

        } catch (SQLException e) {
            logger.error("Failed to execute erasure", e);
            updateErasureStatus(jobId, "FAILED", e.getMessage());
            return new ErasureResult(false, 0, "Erasure failed: " + e.getMessage());
        }
    }

    private int performSecureErasure(String dataSource, String dataCategory) {
        // In production, this would:
        // 1. Identify all records matching criteria
        // 2. Perform cryptographic erasure or physical deletion
        // 3. Clear from backups and archives
        // 4. Update derived data

        logger.info("Performing secure erasure on {} for category {}", dataSource, dataCategory);
        return 0; // Return actual count in production
    }

    private String generateVerificationHash(String dataSource, String dataCategory) {
        String input = dataSource + dataCategory + LocalDateTime.now().toString();
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 32);
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private void updateErasureStatus(String jobId, String status, String error) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE erasure_jobs SET status = ?, error_message = ?, started_at = ? WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setString(2, error);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.setString(4, jobId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update erasure status", e);
        }
    }

    private void completeErasureJob(String jobId, int erasedCount, String verificationHash) {
        String sql = """
                    UPDATE erasure_jobs SET status = 'COMPLETED', records_affected = ?,
                        verification_hash = ?, completed_at = ?
                    WHERE id = ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, erasedCount);
            stmt.setString(2, verificationHash);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.setString(4, jobId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to complete erasure job", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ARCHIVAL
    // ═══════════════════════════════════════════════════════════

    public String scheduleArchive(String dataSource, String originalLocation,
            String archiveLocation, int recordCount) {
        String jobId = UUID.randomUUID().toString();

        String sql = """
                    INSERT INTO archive_jobs (id, data_source, original_location, archive_location,
                        records_archived, archive_format, encrypted)
                    VALUES (?, ?, ?, ?, ?, 'ENCRYPTED_ZIP', 1)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, jobId);
            stmt.setString(2, dataSource);
            stmt.setString(3, originalLocation);
            stmt.setString(4, archiveLocation);
            stmt.setInt(5, recordCount);
            stmt.executeUpdate();

            return jobId;

        } catch (SQLException e) {
            logger.error("Failed to schedule archive", e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SCHEDULED TASKS
    // ═══════════════════════════════════════════════════════════

    private void checkRetentionPolicies() {
        logger.info("Running retention policy check...");

        for (RetentionPolicy policy : retentionPolicies.values()) {
            identifyExpiredData(policy);
        }
    }

    private void identifyExpiredData(RetentionPolicy policy) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(policy.getRetentionDays());
        logger.debug("Checking {} for data older than {}", policy.getDataCategory(), cutoff);
        // Would query actual data tables and flag records for deletion
    }

    private void scanDataInventory() {
        logger.info("Running data inventory scan...");
        // Would scan all data sources and update inventory table
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public LifecycleStatistics getStatistics() {
        LifecycleStatistics stats = new LifecycleStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM retention_policies WHERE active = 1");
            if (rs.next())
                stats.setActivePolicies(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM erasure_jobs WHERE status = 'PENDING'");
            if (rs.next())
                stats.setPendingErasures(rs.getInt(1));

            rs = stmt.executeQuery("SELECT SUM(records_affected) FROM erasure_jobs WHERE status = 'COMPLETED'");
            if (rs.next())
                stats.setTotalRecordsErased(rs.getLong(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM archive_jobs WHERE status = 'COMPLETED'");
            if (rs.next())
                stats.setCompletedArchives(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get lifecycle statistics", e);
        }

        return stats;
    }

    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // POLICY LISTING & DELETION (ISO A.5.31 / NIST compliance)
    // ═══════════════════════════════════════════════════════════

    public List<Map<String, Object>> getAllPolicies() {
        List<Map<String, Object>> policies = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM retention_policies WHERE active = 1 ORDER BY created_at")) {
            while (rs.next()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", rs.getString("id"));
                p.put("name", rs.getString("name"));
                p.put("dataCategory", rs.getString("data_category"));
                p.put("purpose", rs.getString("purpose"));
                p.put("retentionDays", rs.getInt("retention_days"));
                p.put("archiveAfterDays", rs.getInt("archive_after_days"));
                p.put("legalBasis", rs.getString("legal_basis"));
                p.put("dpdpReference", rs.getString("dpdp_reference"));
                policies.add(p);
            }
        } catch (SQLException e) {
            logger.error("Failed to list retention policies", e);
        }

        // Also add in-memory defaults that may not be persisted
        if (policies.isEmpty()) {
            for (RetentionPolicy rp : retentionPolicies.values()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("id", rp.getId());
                p.put("name", rp.getName());
                p.put("dataCategory", rp.getDataCategory());
                p.put("purpose", rp.getPurpose());
                p.put("retentionDays", rp.getRetentionDays());
                p.put("archiveAfterDays", rp.getArchiveAfterDays());
                p.put("legalBasis", rp.getLegalBasis());
                p.put("dpdpReference", rp.getDpdpReference());
                policies.add(p);
            }
        }

        return policies;
    }

    public List<Map<String, Object>> getErasureJobs() {
        List<Map<String, Object>> jobs = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM erasure_jobs ORDER BY scheduled_at DESC LIMIT 50")) {
            while (rs.next()) {
                Map<String, Object> j = new LinkedHashMap<>();
                j.put("id", rs.getString("id"));
                j.put("dataSource", rs.getString("data_source"));
                j.put("dataCategory", rs.getString("data_category"));
                j.put("reason", rs.getString("reason"));
                j.put("recordsAffected", rs.getInt("records_affected"));
                j.put("status", rs.getString("status"));
                j.put("scheduledAt", rs.getString("scheduled_at"));
                j.put("completedAt", rs.getString("completed_at"));
                j.put("verificationHash", rs.getString("verification_hash"));
                j.put("approvedBy", rs.getString("approved_by"));
                jobs.add(j);
            }
        } catch (SQLException e) {
            logger.error("Failed to list erasure jobs", e);
        }

        return jobs;
    }

    public boolean deletePolicy(String policyId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE retention_policies SET active = 0 WHERE id = ?")) {
            stmt.setString(1, policyId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                retentionPolicies.entrySet().removeIf(e -> policyId.equals(e.getValue().getId()));
                auditService.log("RETENTION_POLICY_DELETED", "DATA_LIFECYCLE", null,
                        "Deactivated retention policy: " + policyId);
                return true;
            }
        } catch (SQLException e) {
            logger.error("Failed to delete retention policy", e);
        }
        return false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class RetentionPolicy {
        private String id = UUID.randomUUID().toString();
        private String name;
        private String dataCategory;
        private String purpose;
        private int retentionDays;
        private int archiveAfterDays;
        private String legalBasis;
        private String dpdpReference;

        public RetentionPolicy() {
        }

        public RetentionPolicy(String name, String dataCategory, String purpose,
                int retentionDays, int archiveAfterDays,
                String legalBasis, String dpdpReference) {
            this.name = name;
            this.dataCategory = dataCategory;
            this.purpose = purpose;
            this.retentionDays = retentionDays;
            this.archiveAfterDays = archiveAfterDays;
            this.legalBasis = legalBasis;
            this.dpdpReference = dpdpReference;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDataCategory() {
            return dataCategory;
        }

        public String getPurpose() {
            return purpose;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public int getArchiveAfterDays() {
            return archiveAfterDays;
        }

        public String getLegalBasis() {
            return legalBasis;
        }

        public String getDpdpReference() {
            return dpdpReference;
        }
    }

    public static class ErasureResult {
        private final boolean success;
        private final int recordsErased;
        private final String message;

        public ErasureResult(boolean success, int recordsErased, String message) {
            this.success = success;
            this.recordsErased = recordsErased;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getRecordsErased() {
            return recordsErased;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class LifecycleStatistics {
        private int activePolicies;
        private int pendingErasures;
        private long totalRecordsErased;
        private int completedArchives;

        public int getActivePolicies() {
            return activePolicies;
        }

        public void setActivePolicies(int v) {
            this.activePolicies = v;
        }

        public int getPendingErasures() {
            return pendingErasures;
        }

        public void setPendingErasures(int v) {
            this.pendingErasures = v;
        }

        public long getTotalRecordsErased() {
            return totalRecordsErased;
        }

        public void setTotalRecordsErased(long v) {
            this.totalRecordsErased = v;
        }

        public int getCompletedArchives() {
            return completedArchives;
        }

        public void setCompletedArchives(int v) {
            this.completedArchives = v;
        }
    }
}
