package com.qsdpdp.consent;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.iam.ABACPolicyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consent Enforcement Engine — Runtime Policy-Enforced Consent Validator
 * 
 * Validates consent BEFORE every data access operation ensuring:
 * - Active consent exists for the data principal
 * - Purpose matches the consented purpose (DPDP §5)
 * - Data category is within consented categories (DPDP §6)
 * - Consent has not expired or been withdrawn
 * - Unauthorized processing is blocked and logged
 *
 * Hash-chained enforcement log ensures tamper-proof audit trail.
 *
 * @version 1.0.0
 * @since Phase 7 — Consent Lifecycle Enforcement
 */
@Component
public class ConsentEnforcementEngine {

    private static final Logger logger = LoggerFactory.getLogger(ConsentEnforcementEngine.class);

    @Autowired private DatabaseManager dbManager;
    @Autowired private AuditService auditService;
    @Autowired(required = false) private ABACPolicyEngine abacEngine;
    @Autowired(required = false) private ConsentService consentService;

    private boolean initialized = false;
    private String lastEnforcementHash = "GENESIS_ENFORCEMENT";
    private final AtomicLong enforcementCounter = new AtomicLong(0);

    // Cache: principalId -> Map<purpose, ConsentGrant>
    private final ConcurrentHashMap<String, Map<String, ConsentGrant>> consentCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000; // 1-minute cache
    private final ConcurrentHashMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_enforcement_log (
                    id TEXT PRIMARY KEY,
                    block_number INTEGER NOT NULL,
                    principal_id TEXT NOT NULL,
                    purpose TEXT,
                    data_category TEXT,
                    resource_id TEXT,
                    action TEXT NOT NULL,
                    decision TEXT NOT NULL,
                    denial_reason TEXT,
                    consent_id TEXT,
                    abac_result TEXT,
                    previous_hash TEXT NOT NULL,
                    current_hash TEXT NOT NULL,
                    evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    latency_ms INTEGER DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_grants (
                    id TEXT PRIMARY KEY,
                    principal_id TEXT NOT NULL,
                    purpose TEXT NOT NULL,
                    data_categories TEXT,
                    granted_at TIMESTAMP,
                    expires_at TIMESTAMP,
                    withdrawn_at TIMESTAMP,
                    status TEXT DEFAULT 'ACTIVE',
                    legal_basis TEXT,
                    consent_record_id TEXT,
                    sector TEXT,
                    UNIQUE(principal_id, purpose)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS enforcement_statistics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    total_checks INTEGER DEFAULT 0,
                    allowed INTEGER DEFAULT 0,
                    denied INTEGER DEFAULT 0,
                    cache_hits INTEGER DEFAULT 0,
                    avg_latency_ms REAL DEFAULT 0,
                    UNIQUE(date)
                )
            """);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_enf_log_principal ON consent_enforcement_log(principal_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_enf_log_decision ON consent_enforcement_log(decision)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_consent_grants_principal ON consent_grants(principal_id, purpose)");

            // Load last enforcement hash
            ResultSet rs = stmt.executeQuery("SELECT current_hash, block_number FROM consent_enforcement_log ORDER BY block_number DESC LIMIT 1");
            if (rs.next()) {
                lastEnforcementHash = rs.getString(1);
                enforcementCounter.set(rs.getLong(2));
            }

            seedDefaultGrants();
            initialized = true;
            logger.info("ConsentEnforcementEngine initialized — enforcement chain at block #{}", enforcementCounter.get());

        } catch (SQLException e) {
            logger.error("Failed to initialize ConsentEnforcementEngine", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CORE ENFORCEMENT API
    // ═══════════════════════════════════════════════════════════

    /**
     * Validate consent BEFORE data access — the main enforcement entry point.
     * Returns an EnforcementDecision that must be checked before proceeding.
     */
    public EnforcementDecision validateAccess(EnforcementRequest request) {
        long startMs = System.currentTimeMillis();
        if (!initialized) initialize();

        EnforcementDecision decision = new EnforcementDecision();
        decision.requestId = UUID.randomUUID().toString();
        decision.principalId = request.principalId;
        decision.purpose = request.purpose;
        decision.dataCategory = request.dataCategory;
        decision.evaluatedAt = LocalDateTime.now();

        List<String> violations = new ArrayList<>();

        // 1. Check consent grant exists
        ConsentGrant grant = findActiveGrant(request.principalId, request.purpose);
        if (grant == null) {
            violations.add("NO_ACTIVE_CONSENT: No active consent found for principal '"
                    + request.principalId + "' with purpose '" + request.purpose + "' — blocked per DPDP §6");
        } else {
            decision.consentId = grant.id;

            // 2. Check purpose limitation (DPDP §5)
            if (!grant.purpose.equalsIgnoreCase(request.purpose)) {
                violations.add("PURPOSE_MISMATCH: Requested purpose '" + request.purpose
                        + "' does not match consented purpose '" + grant.purpose + "' — DPDP §5");
            }

            // 3. Check data category (DPDP §6)
            if (request.dataCategory != null && grant.dataCategories != null) {
                List<String> permitted = Arrays.asList(grant.dataCategories.split(","));
                boolean categoryPermitted = permitted.stream()
                        .anyMatch(c -> c.trim().equalsIgnoreCase(request.dataCategory));
                if (!categoryPermitted) {
                    violations.add("CATEGORY_DENIED: Data category '" + request.dataCategory
                            + "' is not within consented categories [" + grant.dataCategories + "]");
                }
            }

            // 4. Check expiry
            if (grant.expiresAt != null && grant.expiresAt.isBefore(LocalDateTime.now())) {
                violations.add("CONSENT_EXPIRED: Consent expired at " + grant.expiresAt
                        + " — renewal required per DPDP §6(4)");
            }

            // 5. Check withdrawal
            if ("WITHDRAWN".equalsIgnoreCase(grant.status)) {
                violations.add("CONSENT_WITHDRAWN: Consent was withdrawn at " + grant.withdrawnAt
                        + " — processing must cease per DPDP §6(6)");
            }

            // 6. Jurisdiction check (DPDP §16 — cross-border data transfer)
            if (request.crossBorderContext && grant.jurisdictionTag != null
                    && "IN".equalsIgnoreCase(grant.jurisdictionTag)) {
                violations.add("JURISDICTION_VIOLATION: Consent granted for jurisdiction 'IN' (India only) "
                        + "but request is from cross-border context — blocked per DPDP §16");
            }

            // 7. Retention policy check
            if (grant.retentionExpiresAt != null && grant.retentionExpiresAt.isBefore(LocalDateTime.now())) {
                violations.add("RETENTION_EXPIRED: Data retention period expired at "
                        + grant.retentionExpiresAt + " — data must be deleted per DPDP §8(7)");
            }
        }

        // 6. ABAC check (if available)
        if (abacEngine != null && request.userId != null) {
            ABACPolicyEngine.AccessRequest abacReq = new ABACPolicyEngine.AccessRequest();
            abacReq.userId = request.userId;
            abacReq.resourceId = request.resourceId;
            abacReq.purpose = request.purpose;
            abacReq.resourceConsentStatus = (grant != null && "ACTIVE".equals(grant.status)) ? "ACTIVE" : "NONE";
            abacReq.resourceSensitivity = request.sensitivity;
            abacReq.userClearance = request.userClearance;
            abacReq.userTrainingComplete = request.userTrainingComplete;
            abacReq.sector = request.sector;
            abacReq.mfaVerified = request.mfaVerified;

            ABACPolicyEngine.AccessDecision abacDecision = abacEngine.evaluate(abacReq);
            if (!abacDecision.allowed) {
                for (String reason : abacDecision.denialReasons) {
                    violations.add("ABAC_DENIED: " + reason);
                }
            }
            decision.abacResult = abacDecision.allowed ? "ALLOWED" : "DENIED";
        }

        // Final decision
        decision.latencyMs = (int)(System.currentTimeMillis() - startMs);
        if (violations.isEmpty()) {
            decision.allowed = true;
            decision.decision = "ALLOWED";
            decision.denialReasons = Collections.emptyList();
        } else {
            decision.allowed = false;
            decision.decision = "DENIED";
            decision.denialReasons = violations;
        }

        // Log enforcement decision (hash-chained)
        logEnforcement(decision);

        return decision;
    }

    /**
     * Quick check — returns true if access is permitted.
     */
    public boolean isAccessPermitted(String principalId, String purpose, String dataCategory) {
        EnforcementRequest req = new EnforcementRequest();
        req.principalId = principalId;
        req.purpose = purpose;
        req.dataCategory = dataCategory;
        return validateAccess(req).allowed;
    }

    /**
     * Block unauthorized processing — logs and returns enforcement violation.
     */
    public EnforcementDecision blockUnauthorizedProcessing(String principalId, String purpose,
                                                           String dataCategory, String resourceId) {
        EnforcementRequest req = new EnforcementRequest();
        req.principalId = principalId;
        req.purpose = purpose;
        req.dataCategory = dataCategory;
        req.resourceId = resourceId;
        EnforcementDecision decision = validateAccess(req);
        if (!decision.allowed) {
            auditService.log("PROCESSING_BLOCKED", "CONSENT_ENFORCEMENT", principalId,
                    "Unauthorized processing blocked: purpose=" + purpose + " category=" + dataCategory
                            + " reasons=" + String.join("; ", decision.denialReasons));
        }
        return decision;
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT GRANT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Register a consent grant — called when consent is collected.
     */
    public void registerGrant(ConsentGrant grant) {
        String sql = """
            INSERT OR REPLACE INTO consent_grants 
            (id, principal_id, purpose, data_categories, granted_at, expires_at, status, legal_basis, consent_record_id, sector)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, grant.id);
            ps.setString(2, grant.principalId);
            ps.setString(3, grant.purpose);
            ps.setString(4, grant.dataCategories);
            ps.setString(5, grant.grantedAt != null ? grant.grantedAt.toString() : LocalDateTime.now().toString());
            ps.setString(6, grant.expiresAt != null ? grant.expiresAt.toString() : null);
            ps.setString(7, "ACTIVE");
            ps.setString(8, grant.legalBasis);
            ps.setString(9, grant.consentRecordId);
            ps.setString(10, grant.sector);
            ps.executeUpdate();

            // Invalidate cache
            consentCache.remove(grant.principalId);
            logger.info("Consent grant registered: principal={} purpose={}", grant.principalId, grant.purpose);
        } catch (SQLException e) {
            logger.error("Failed to register consent grant", e);
        }
    }

    /**
     * Revoke a consent grant — called when consent is withdrawn.
     */
    public void revokeGrant(String principalId, String purpose) {
        String sql = "UPDATE consent_grants SET status = 'WITHDRAWN', withdrawn_at = ? WHERE principal_id = ? AND purpose = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, principalId);
            ps.setString(3, purpose);
            ps.executeUpdate();
            consentCache.remove(principalId);
            auditService.log("CONSENT_GRANT_REVOKED", "CONSENT_ENFORCEMENT", principalId,
                    "Consent revoked for purpose: " + purpose);
        } catch (SQLException e) {
            logger.error("Failed to revoke consent grant", e);
        }
    }

    /**
     * Get all active grants for a principal.
     */
    public List<ConsentGrant> getActiveGrants(String principalId) {
        List<ConsentGrant> grants = new ArrayList<>();
        String sql = "SELECT * FROM consent_grants WHERE principal_id = ? AND status = 'ACTIVE'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, principalId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) grants.add(mapGrant(rs));
        } catch (SQLException e) {
            logger.error("Failed to get active grants", e);
        }
        return grants;
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS & DASHBOARD
    // ═══════════════════════════════════════════════════════════

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_enforcement_log");
            if (rs.next()) stats.put("totalEnforcementChecks", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_enforcement_log WHERE decision = 'ALLOWED'");
            if (rs.next()) stats.put("allowed", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_enforcement_log WHERE decision = 'DENIED'");
            if (rs.next()) stats.put("denied", rs.getInt(1));

            rs = stmt.executeQuery("SELECT AVG(latency_ms) FROM consent_enforcement_log");
            if (rs.next()) stats.put("avgLatencyMs", rs.getDouble(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_grants WHERE status = 'ACTIVE'");
            if (rs.next()) stats.put("activeGrants", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_grants WHERE status = 'WITHDRAWN'");
            if (rs.next()) stats.put("revokedGrants", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_grants WHERE status = 'ACTIVE' AND expires_at < datetime('now')");
            if (rs.next()) stats.put("expiredGrants", rs.getInt(1));

            stats.put("enforcementChainBlock", enforcementCounter.get());
            stats.put("cacheSize", consentCache.size());
            stats.put("status", "OPERATIONAL");
        } catch (SQLException e) {
            logger.error("Failed to get enforcement statistics", e);
            stats.put("status", "ERROR");
        }
        return stats;
    }

    /**
     * Get recent enforcement decisions for audit review.
     */
    public List<Map<String, Object>> getRecentDecisions(int limit) {
        List<Map<String, Object>> decisions = new ArrayList<>();
        String sql = "SELECT * FROM consent_enforcement_log ORDER BY block_number DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("id", rs.getString("id"));
                d.put("blockNumber", rs.getInt("block_number"));
                d.put("principalId", rs.getString("principal_id"));
                d.put("purpose", rs.getString("purpose"));
                d.put("dataCategory", rs.getString("data_category"));
                d.put("decision", rs.getString("decision"));
                d.put("denialReason", rs.getString("denial_reason"));
                d.put("evaluatedAt", rs.getString("evaluated_at"));
                d.put("latencyMs", rs.getInt("latency_ms"));
                d.put("chainHash", rs.getString("current_hash"));
                decisions.add(d);
            }
        } catch (SQLException e) {
            logger.error("Failed to get recent decisions", e);
        }
        return decisions;
    }

    /**
     * Verify the integrity of the enforcement chain.
     */
    public Map<String, Object> verifyChainIntegrity() {
        Map<String, Object> result = new LinkedHashMap<>();
        int totalBlocks = 0;
        boolean valid = true;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT previous_hash, current_hash, block_number FROM consent_enforcement_log ORDER BY block_number ASC")) {
            String expectedPrev = "GENESIS_ENFORCEMENT";
            while (rs.next()) {
                totalBlocks++;
                String actualPrev = rs.getString("previous_hash");
                if (!expectedPrev.equals(actualPrev)) {
                    valid = false;
                    result.put("brokenAtBlock", rs.getInt("block_number"));
                    break;
                }
                expectedPrev = rs.getString("current_hash");
            }
        } catch (SQLException e) {
            logger.error("Chain integrity check failed", e);
            valid = false;
        }
        result.put("valid", valid);
        result.put("totalBlocks", totalBlocks);
        result.put("lastHash", lastEnforcementHash);
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL METHODS
    // ═══════════════════════════════════════════════════════════

    private ConsentGrant findActiveGrant(String principalId, String purpose) {
        // Check cache first
        if (consentCache.containsKey(principalId)) {
            Long ts = cacheTimestamps.get(principalId);
            if (ts != null && System.currentTimeMillis() - ts < CACHE_TTL_MS) {
                Map<String, ConsentGrant> grants = consentCache.get(principalId);
                return grants.get(purpose);
            }
            consentCache.remove(principalId);
        }

        // Query database
        Map<String, ConsentGrant> grants = new HashMap<>();
        String sql = "SELECT * FROM consent_grants WHERE principal_id = ? AND status = 'ACTIVE'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, principalId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ConsentGrant g = mapGrant(rs);
                grants.put(g.purpose, g);
            }
        } catch (SQLException e) {
            logger.error("Failed to lookup consent grant", e);
        }

        // Populate cache
        consentCache.put(principalId, grants);
        cacheTimestamps.put(principalId, System.currentTimeMillis());
        return grants.get(purpose);
    }

    private void logEnforcement(EnforcementDecision decision) {
        long blockNum = enforcementCounter.incrementAndGet();
        String data = blockNum + "|" + decision.principalId + "|" + decision.purpose + "|"
                + decision.decision + "|" + lastEnforcementHash;
        String currentHash = sha256(data);

        String sql = """
            INSERT INTO consent_enforcement_log 
            (id, block_number, principal_id, purpose, data_category, resource_id, action, decision, 
             denial_reason, consent_id, abac_result, previous_hash, current_hash, latency_ms)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, decision.requestId);
            ps.setLong(2, blockNum);
            ps.setString(3, decision.principalId);
            ps.setString(4, decision.purpose);
            ps.setString(5, decision.dataCategory);
            ps.setString(6, decision.resourceId);
            ps.setString(7, "DATA_ACCESS");
            ps.setString(8, decision.decision);
            ps.setString(9, decision.denialReasons != null ? String.join("; ", decision.denialReasons) : null);
            ps.setString(10, decision.consentId);
            ps.setString(11, decision.abacResult);
            ps.setString(12, lastEnforcementHash);
            ps.setString(13, currentHash);
            ps.setInt(14, decision.latencyMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to log enforcement decision", e);
        }
        lastEnforcementHash = currentHash;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString().substring(0, 16);
        } catch (Exception e) {
            return UUID.randomUUID().toString().substring(0, 16);
        }
    }

    private ConsentGrant mapGrant(ResultSet rs) throws SQLException {
        ConsentGrant g = new ConsentGrant();
        g.id = rs.getString("id");
        g.principalId = rs.getString("principal_id");
        g.purpose = rs.getString("purpose");
        g.dataCategories = rs.getString("data_categories");
        g.status = rs.getString("status");
        g.legalBasis = rs.getString("legal_basis");
        g.sector = rs.getString("sector");
        String ga = rs.getString("granted_at");
        if (ga != null) g.grantedAt = LocalDateTime.parse(ga);
        String ea = rs.getString("expires_at");
        if (ea != null) g.expiresAt = LocalDateTime.parse(ea);
        String wa = rs.getString("withdrawn_at");
        if (wa != null) g.withdrawnAt = LocalDateTime.parse(wa);
        return g;
    }

    private void seedDefaultGrants() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_grants");
            if (rs.next() && rs.getInt(1) > 0) return; // Already seeded
        } catch (SQLException e) { return; }

        ConsentGrant[] defaults = {
            grant("DP-001", "KYC_VERIFICATION", "Identity,Contact,Financial", "BFSI", "legal_obligation"),
            grant("DP-001", "ACCOUNT_MANAGEMENT", "Identity,Contact,Transaction", "BFSI", "contract"),
            grant("DP-001", "MARKETING", "Contact,Preferences", "BFSI", "consent"),
            grant("DP-002", "TREATMENT", "Identity,Health,Biometric", "Healthcare", "vital_interest"),
            grant("DP-002", "INSURANCE_CLAIMS", "Identity,Health", "Insurance", "legal_obligation"),
            grant("DP-003", "ORDER_PROCESSING", "Identity,Contact,Transaction", "E-Commerce", "contract"),
            grant("DP-003", "RECOMMENDATION_ENGINE", "Preferences,Browsing", "E-Commerce", "consent"),
            grant("DP-004", "STUDENT_ENROLLMENT", "Identity,Academic,Contact", "Education", "contract"),
            grant("DP-005", "SUBSCRIBER_SERVICES", "Identity,Contact,Usage", "Telecom", "contract"),
        };
        for (ConsentGrant g : defaults) registerGrant(g);
        logger.info("Seeded {} default consent grants", defaults.length);
    }

    private ConsentGrant grant(String principalId, String purpose, String categories, String sector, String basis) {
        ConsentGrant g = new ConsentGrant();
        g.id = UUID.randomUUID().toString();
        g.principalId = principalId;
        g.purpose = purpose;
        g.dataCategories = categories;
        g.sector = sector;
        g.legalBasis = basis;
        g.grantedAt = LocalDateTime.now();
        g.expiresAt = LocalDateTime.now().plusYears(1);
        g.status = "ACTIVE";
        return g;
    }

    public boolean isInitialized() { return initialized; }

    // ═══════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class EnforcementRequest {
        public String principalId;
        public String purpose;
        public String dataCategory;
        public String resourceId;
        public boolean crossBorderContext; // true if request originates from cross-border
        // Optional — for combined ABAC evaluation
        public String userId;
        public String sensitivity;
        public String userClearance;
        public boolean userTrainingComplete;
        public String sector;
        public boolean mfaVerified;
    }

    public static class EnforcementDecision {
        public String requestId;
        public String principalId;
        public String purpose;
        public String dataCategory;
        public String resourceId;
        public String consentId;
        public boolean allowed;
        public String decision; // ALLOWED or DENIED
        public List<String> denialReasons;
        public String abacResult;
        public LocalDateTime evaluatedAt;
        public int latencyMs;
    }

    public static class ConsentGrant {
        public String id;
        public String principalId;
        public String purpose;
        public String dataCategories;
        public LocalDateTime grantedAt;
        public LocalDateTime expiresAt;
        public LocalDateTime withdrawnAt;
        public LocalDateTime retentionExpiresAt; // When data retention period ends
        public String status;
        public String legalBasis;
        public String consentRecordId;
        public String sector;
        public String jurisdictionTag; // IN, cross-border
        public String retentionPolicyId;
    }
}
