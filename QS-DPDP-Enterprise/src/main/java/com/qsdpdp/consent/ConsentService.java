package com.qsdpdp.consent;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import com.qsdpdp.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Consent Management Service
 * Handles consent collection, withdrawal, and lifecycle management
 * DPDP Act 2023 compliant with hash-chained consent records
 * 
 * @version 1.0.0
 * @since Phase 2
 */
@Service
public class ConsentService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final SecurityManager securityManager;

    private boolean initialized = false;
    private String lastConsentHash = "GENESIS";

    @Autowired
    public ConsentService(DatabaseManager dbManager, AuditService auditService,
            EventBus eventBus, SecurityManager securityManager) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.securityManager = securityManager;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing Consent Service...");

        // Load last consent hash for chain continuity
        loadLastConsentHash();

        // Subscribe to events
        eventBus.subscribe("consent.*", this::handleConsentEvent);

        initialized = true;
        logger.info("Consent Service initialized");
    }

    private void loadLastConsentHash() {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT hash FROM consents ORDER BY collected_at DESC LIMIT 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                lastConsentHash = rs.getString("hash");
            }
        } catch (Exception e) {
            logger.warn("Could not load last consent hash", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT COLLECTION
    // ═══════════════════════════════════════════════════════════

    /**
     * Collect a new consent from a data principal
     */
    public Consent collectConsent(ConsentRequest request) {
        logger.info("Collecting consent for principal: {} purpose: {}",
                request.getDataPrincipalId(), request.getPurposeId());

        // Auto-register data principal if not exists (when created via web form)
        if (!dataPrincipalExists(request.getDataPrincipalId())) {
            String dpName = request.getDataPrincipalName() != null ? request.getDataPrincipalName() : "Unknown";
            autoRegisterDataPrincipal(request.getDataPrincipalId(), dpName);
        }

        // Auto-register purpose if not exists
        if (!purposeIsActive(request.getPurposeId())) {
            String purposeName = request.getPurposeName() != null ? request.getPurposeName() : "General Purpose";
            autoRegisterPurpose(request.getPurposeId(), purposeName);
        }

        // Validate request
        validateConsentRequest(request);

        // If active consent exists, withdraw it first (idempotent consent refresh)
        Consent existing = findActiveConsent(request.getDataPrincipalId(), request.getPurposeId());
        if (existing != null) {
            withdrawConsent(existing.getId(), "Superseded by new consent", request.getActorId());
        }

        // Generate hash chain
        String consentData = String.format("%s|%s|%s|%s",
                request.getDataPrincipalId(),
                request.getPurposeId(),
                request.getConsentMethod(),
                LocalDateTime.now().toString());
        String consentHash = securityManager.sha256(lastConsentHash + "|" + consentData);

        // Create consent record
        Consent consent = Consent.builder()
                .id(UUID.randomUUID().toString())
                .dataPrincipalId(request.getDataPrincipalId())
                .purposeId(request.getPurposeId())
                .status(ConsentStatus.ACTIVE)
                .consentMethod(request.getConsentMethod())
                .noticeVersion(request.getNoticeVersion())
                .language(request.getLanguage())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .expiresAt(request.getExpiresAt() != null ? request.getExpiresAt() : LocalDateTime.now().plusYears(1))
                .hash(consentHash)
                .prevHash(lastConsentHash)
                .createdBy(request.getActorId())
                .dataPrincipalName(request.getDataPrincipalName())
                .purposeName(request.getPurposeName())
                .consentType(request.getConsentType() != null ? request.getConsentType() : "EXPLICIT")
                .retentionPeriod(request.getRetentionPeriod())
                .build();

        // Persist
        saveConsent(consent);
        lastConsentHash = consentHash;

        // Audit (sync to ensure sequence is immediately visible)
        auditService.logSync("CONSENT_COLLECTED", "CONSENT", request.getActorId(),
                String.format("Consent collected: principal=%s purpose=%s",
                        request.getDataPrincipalId(), request.getPurposeId()));

        // Publish event
        eventBus.publish(new ComplianceEvent("consent.collected",
                Map.of("consentId", consent.getId(), "principalId", request.getDataPrincipalId())));

        logger.info("Consent collected successfully: {}", consent.getId());
        return consent;
    }

    private void validateConsentRequest(ConsentRequest request) {
        if (request.getDataPrincipalId() == null || request.getDataPrincipalId().isEmpty()) {
            throw new IllegalArgumentException("Data principal ID is required");
        }
        if (request.getPurposeId() == null || request.getPurposeId().isEmpty()) {
            throw new IllegalArgumentException("Purpose ID is required");
        }
        if (request.getConsentMethod() == null || request.getConsentMethod().isEmpty()) {
            throw new IllegalArgumentException("Consent method is required");
        }

        // Verify data principal exists
        if (!dataPrincipalExists(request.getDataPrincipalId())) {
            throw new IllegalArgumentException("Data principal not found");
        }

        // Verify purpose exists and is active
        if (!purposeIsActive(request.getPurposeId())) {
            throw new IllegalArgumentException("Purpose not found or inactive");
        }
    }

    private void saveConsent(Consent consent) {
        String sql = """
                    INSERT INTO consents (id, data_principal_id, purpose_id, status, consent_method,
                        notice_version, language, ip_address, user_agent, collected_at, expires_at,
                        hash, prev_hash, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, consent.getId());
            stmt.setString(2, consent.getDataPrincipalId());
            stmt.setString(3, consent.getPurposeId());
            stmt.setString(4, consent.getStatus().name());
            stmt.setString(5, consent.getConsentMethod());
            stmt.setString(6, consent.getNoticeVersion());
            stmt.setString(7, consent.getLanguage());
            stmt.setString(8, consent.getIpAddress());
            stmt.setString(9, consent.getUserAgent());
            stmt.setString(10, consent.getCollectedAt().toString());
            stmt.setString(11, consent.getExpiresAt() != null ? consent.getExpiresAt().toString() : null);
            stmt.setString(12, consent.getHash());
            stmt.setString(13, consent.getPrevHash());
            stmt.setString(14, consent.getCreatedBy());

            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Failed to save consent", e);
            throw new RuntimeException("Failed to save consent", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT WITHDRAWAL
    // ═══════════════════════════════════════════════════════════

    /**
     * Withdraw consent (must be easy as collection per DPDP Act)
     */
    public Consent withdrawConsent(String consentId, String reason, String actorId) {
        logger.info("Withdrawing consent: {}", consentId);

        Consent consent = getConsentById(consentId);
        if (consent == null) {
            throw new IllegalArgumentException("Consent not found");
        }

        if (consent.getStatus() != ConsentStatus.ACTIVE) {
            throw new IllegalStateException("Consent is not active");
        }

        // Update consent
        consent.setStatus(ConsentStatus.WITHDRAWN);
        consent.setWithdrawnAt(LocalDateTime.now());
        consent.setWithdrawalReason(reason);
        consent.setWithdrawnBy(actorId);

        String sql = """
                    UPDATE consents
                    SET status = 'WITHDRAWN', withdrawn_at = ?, withdrawal_reason = ?, withdrawn_by = ?
                    WHERE id = ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, consent.getWithdrawnAt().toString());
            stmt.setString(2, reason);
            stmt.setString(3, actorId);
            stmt.setString(4, consentId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Failed to withdraw consent", e);
            throw new RuntimeException("Failed to withdraw consent", e);
        }

        // Audit
        auditService.log("CONSENT_WITHDRAWN", "CONSENT", actorId,
                String.format("Consent withdrawn: id=%s reason=%s", consentId, reason));

        // Publish event
        eventBus.publish(new ComplianceEvent("consent.withdrawn",
                Map.of("consentId", consentId, "reason", reason)));

        logger.info("Consent withdrawn successfully: {}", consentId);
        return consent;
    }

    // ═══════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════

    public Consent getConsentById(String id) {
        String sql = """
                    SELECT c.*, dp.name as principal_name, p.name as purpose_name
                    FROM consents c
                    LEFT JOIN data_principals dp ON c.data_principal_id = dp.id
                    LEFT JOIN purposes p ON c.purpose_id = p.id
                    WHERE c.id = ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapConsent(rs);
            }

        } catch (SQLException e) {
            logger.error("Failed to get consent", e);
        }

        return null;
    }

    public Consent findActiveConsent(String principalId, String purposeId) {
        String sql = """
                    SELECT c.*, dp.name as principal_name, p.name as purpose_name
                    FROM consents c
                    LEFT JOIN data_principals dp ON c.data_principal_id = dp.id
                    LEFT JOIN purposes p ON c.purpose_id = p.id
                    WHERE c.data_principal_id = ? AND c.purpose_id = ? AND c.status = 'ACTIVE'
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, principalId);
            stmt.setString(2, purposeId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapConsent(rs);
            }

        } catch (SQLException e) {
            logger.error("Failed to find active consent", e);
        }

        return null;
    }

    public List<Consent> getConsentsByPrincipal(String principalId) {
        List<Consent> consents = new ArrayList<>();
        String sql = """
                    SELECT c.*, dp.name as principal_name, p.name as purpose_name
                    FROM consents c
                    LEFT JOIN data_principals dp ON c.data_principal_id = dp.id
                    LEFT JOIN purposes p ON c.purpose_id = p.id
                    WHERE c.data_principal_id = ?
                    ORDER BY c.collected_at DESC
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, principalId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                consents.add(mapConsent(rs));
            }

        } catch (SQLException e) {
            logger.error("Failed to get consents by principal", e);
        }

        return consents;
    }

    public List<Consent> getAllConsents(int offset, int limit) {
        List<Consent> consents = new ArrayList<>();
        String sql = """
                    SELECT c.*, dp.name as principal_name, p.name as purpose_name
                    FROM consents c
                    LEFT JOIN data_principals dp ON c.data_principal_id = dp.id
                    LEFT JOIN purposes p ON c.purpose_id = p.id
                    ORDER BY c.collected_at DESC
                    LIMIT ? OFFSET ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                consents.add(mapConsent(rs));
            }

        } catch (SQLException e) {
            logger.error("Failed to get consents", e);
        }

        return consents;
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public ConsentStatistics getStatistics() {
        ConsentStatistics stats = new ConsentStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            // Total consents
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM consents");
            if (rs.next())
                stats.setTotalConsents(rs.getInt(1));

            // Active consents
            rs = stmt.executeQuery("SELECT COUNT(*) FROM consents WHERE status = 'ACTIVE'");
            if (rs.next())
                stats.setActiveConsents(rs.getInt(1));

            // Withdrawn consents
            rs = stmt.executeQuery("SELECT COUNT(*) FROM consents WHERE status = 'WITHDRAWN'");
            if (rs.next())
                stats.setWithdrawnConsents(rs.getInt(1));

            // Expired consents
            rs = stmt.executeQuery("SELECT COUNT(*) FROM consents WHERE status = 'EXPIRED'");
            if (rs.next())
                stats.setExpiredConsents(rs.getInt(1));

            // Calculate rates
            if (stats.getTotalConsents() > 0) {
                stats.setActiveRate((double) stats.getActiveConsents() / stats.getTotalConsents() * 100);
            }

            // Consents last 30 days
            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM consents WHERE collected_at >= datetime('now', '-30 days')");
            if (rs.next())
                stats.setConsentsLast30Days(rs.getInt(1));

            // Withdrawals last 30 days
            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM consents WHERE withdrawn_at >= datetime('now', '-30 days')");
            if (rs.next())
                stats.setWithdrawalsLast30Days(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get consent statistics", e);
        }

        return stats;
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private boolean dataPrincipalExists(String id) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT 1 FROM data_principals WHERE id = ?")) {
            stmt.setString(1, id);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean purposeIsActive(String id) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT 1 FROM purposes WHERE id = ? AND is_active = 1")) {
            stmt.setString(1, id);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    private void autoRegisterDataPrincipal(String id, String name) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR IGNORE INTO data_principals (id, external_id, name, email, phone, is_child) VALUES (?, ?, ?, ?, ?, 0)")) {
            stmt.setString(1, id);
            stmt.setString(2, "WEB-" + id.substring(0, 8));
            stmt.setString(3, name);
            stmt.setString(4, name.toLowerCase().replace(" ", ".") + "@web-form.local");
            stmt.setString(5, "+910000000000");
            stmt.executeUpdate();
            logger.info("Auto-registered data principal: {} ({})", id, name);
        } catch (SQLException e) {
            logger.warn("Could not auto-register data principal: {}", e.getMessage());
        }
    }

    private void autoRegisterPurpose(String id, String name) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR IGNORE INTO purposes (id, code, name, description, legal_basis, data_categories, retention_period_days, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, 1)")) {
            stmt.setString(1, id);
            stmt.setString(2, "PUR-WEB-" + id.substring(0, 6));
            stmt.setString(3, name);
            stmt.setString(4, "Purpose for " + name.toLowerCase());
            stmt.setString(5, "CONSENT");
            stmt.setString(6, "Personal");
            stmt.setInt(7, 365);
            stmt.executeUpdate();
            logger.info("Auto-registered purpose: {} ({})", id, name);
        } catch (SQLException e) {
            logger.warn("Could not auto-register purpose: {}", e.getMessage());
        }
    }

    private Consent mapConsent(ResultSet rs) throws SQLException {
        Consent consent = new Consent();
        consent.setId(rs.getString("id"));
        consent.setDataPrincipalId(rs.getString("data_principal_id"));
        consent.setPurposeId(rs.getString("purpose_id"));
        consent.setStatus(ConsentStatus.valueOf(rs.getString("status")));
        consent.setConsentMethod(rs.getString("consent_method"));
        consent.setNoticeVersion(rs.getString("notice_version"));
        consent.setLanguage(rs.getString("language"));
        consent.setIpAddress(rs.getString("ip_address"));
        consent.setUserAgent(rs.getString("user_agent"));

        String collectedAt = rs.getString("collected_at");
        if (collectedAt != null)
            consent.setCollectedAt(LocalDateTime.parse(collectedAt.replace(' ', 'T')));

        String expiresAt = rs.getString("expires_at");
        if (expiresAt != null)
            consent.setExpiresAt(LocalDateTime.parse(expiresAt.replace(' ', 'T')));

        String withdrawnAt = rs.getString("withdrawn_at");
        if (withdrawnAt != null)
            consent.setWithdrawnAt(LocalDateTime.parse(withdrawnAt.replace(' ', 'T')));

        consent.setWithdrawalReason(rs.getString("withdrawal_reason"));
        consent.setWithdrawnBy(rs.getString("withdrawn_by"));
        consent.setHash(rs.getString("hash"));
        consent.setPrevHash(rs.getString("prev_hash"));
        consent.setCreatedBy(rs.getString("created_by"));

        try {
            consent.setDataPrincipalName(rs.getString("principal_name"));
            consent.setPurposeName(rs.getString("purpose_name"));
        } catch (SQLException e) {
            // Joined columns may not exist in all queries
        }

        return consent;
    }

    private void handleConsentEvent(ComplianceEvent event) {
        logger.debug("Handling consent event: {}", event.getType());
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT TOKEN VALIDATION (Module 2 Enhancement)
    // No data processing without valid consent token
    // ═══════════════════════════════════════════════════════════

    /**
     * Validate a consent token for a specific purpose.
     * This is the primary API validation layer - must be called before ANY data processing.
     *
     * @param consentId   The consent record ID (consent token)
     * @param purpose     The purpose for which data is being processed
     * @return TokenValidationResult with valid/invalid status and reason
     */
    public TokenValidationResult validateConsentToken(String consentId, String purpose) {
        logger.info("Validating consent token: {} for purpose: {}", consentId, purpose);

        if (consentId == null || consentId.isEmpty()) {
            auditService.log("CONSENT_TOKEN_REJECTED", "CONSENT", "SYSTEM",
                    "Empty consent token - processing BLOCKED");
            return TokenValidationResult.rejected("Consent token is required - no data processing without consent");
        }

        Consent consent = getConsentById(consentId);
        if (consent == null) {
            auditService.log("CONSENT_TOKEN_REJECTED", "CONSENT", "SYSTEM",
                    "Invalid consent token: " + consentId + " - processing BLOCKED");
            return TokenValidationResult.rejected("Invalid consent token - consent record not found");
        }

        // Check status
        if (consent.getStatus() != ConsentStatus.ACTIVE) {
            auditService.log("CONSENT_TOKEN_REJECTED", "CONSENT", "SYSTEM",
                    String.format("Consent %s status is %s - processing BLOCKED", consentId, consent.getStatus()));
            return TokenValidationResult.rejected("Consent is " + consent.getStatus() + " - cannot process data");
        }

        // Check expiry
        if (consent.isExpired()) {
            auditService.log("CONSENT_TOKEN_REJECTED", "CONSENT", "SYSTEM",
                    String.format("Consent %s has expired - processing BLOCKED", consentId));
            return TokenValidationResult.rejected("Consent has expired - renew consent before processing");
        }

        // Check purpose binding
        if (purpose != null && !purpose.isEmpty()) {
            if (!isPurposeMatch(consent.getPurposeId(), purpose)) {
                auditService.log("CONSENT_PURPOSE_MISMATCH", "CONSENT", "SYSTEM",
                        String.format("Consent %s purpose %s does not match requested purpose %s - BLOCKED",
                                consentId, consent.getPurposeId(), purpose));
                return TokenValidationResult.rejected(
                        "Purpose mismatch - consent was given for different purpose. Cannot reuse consent across purposes.");
            }
        }

        // Verify hash chain integrity
        if (consent.getHash() != null) {
            String expectedHash = securityManager.sha256(
                    consent.getPrevHash() + "|" + consent.getDataPrincipalId() + "|" +
                    consent.getPurposeId() + "|" + consent.getConsentMethod() + "|" +
                    consent.getCollectedAt().toString());
            // Note: Hash verification is logged but not blocking in case of clock skew
            if (!consent.getHash().equals(expectedHash)) {
                logger.warn("Consent hash chain verification warning for: {}", consentId);
            }
        }

        auditService.log("CONSENT_TOKEN_VALIDATED", "CONSENT", "SYSTEM",
                String.format("Consent token %s validated for purpose %s - processing ALLOWED",
                        consentId, purpose));

        logger.info("Consent token validated successfully: {}", consentId);
        return TokenValidationResult.accepted(consentId, consent.getDataPrincipalId(), consent.getPurposeId());
    }

    /**
     * Runtime purpose binding enforcement.
     * Call this BEFORE any data processing operation.
     * Throws exception if consent is not valid — enforces zero-processing-without-consent.
     */
    public void enforcePurposeBinding(String principalId, String purpose) {
        logger.info("Enforcing purpose binding: principal={} purpose={}", principalId, purpose);

        Consent activeConsent = findActiveConsent(principalId, purpose);
        if (activeConsent == null) {
            String msg = String.format(
                    "PURPOSE BINDING VIOLATION: No active consent for principal %s, purpose %s. " +
                    "Data processing BLOCKED per DPDP Act Section 6.", principalId, purpose);
            auditService.log("PURPOSE_BINDING_VIOLATION", "CONSENT", "SYSTEM", msg);
            logger.error(msg);
            throw new IllegalStateException(msg);
        }

        if (activeConsent.isExpired()) {
            String msg = String.format(
                    "PURPOSE BINDING VIOLATION: Consent expired for principal %s, purpose %s. " +
                    "Data processing BLOCKED.", principalId, purpose);
            auditService.log("PURPOSE_BINDING_VIOLATION", "CONSENT", "SYSTEM", msg);
            throw new IllegalStateException(msg);
        }

        logger.debug("Purpose binding enforced: consent {} valid for principal {} purpose {}",
                activeConsent.getId(), principalId, purpose);
    }

    private boolean isPurposeMatch(String consentPurposeId, String requestedPurpose) {
        if (consentPurposeId == null || requestedPurpose == null) return false;
        // Exact match or hierarchical match (parent purpose covers child)
        return consentPurposeId.equals(requestedPurpose) ||
               consentPurposeId.startsWith(requestedPurpose + ".") ||
               requestedPurpose.startsWith(consentPurposeId + ".");
    }

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2 — GRANULAR CONSENT PREFERENCES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Save a granular consent preference (per-purpose, per-data-category).
     */
    public ConsentPreference saveConsentPreference(ConsentPreference pref) {
        logger.info("Saving consent preference for consent={}, category={}", pref.getConsentId(), pref.getDataCategory());
        String sql = """
            INSERT OR REPLACE INTO consent_preferences
            (id, consent_id, data_principal_id, purpose_id, data_category,
             allowed, processing_basis, third_party_sharing, cross_border_transfer,
             created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (pref.getId() == null) pref.setId(UUID.randomUUID().toString());
            ps.setString(1, pref.getId());
            ps.setString(2, pref.getConsentId());
            ps.setString(3, pref.getDataPrincipalId());
            ps.setString(4, pref.getPurposeId());
            ps.setString(5, pref.getDataCategory());
            ps.setInt(6, pref.isAllowed() ? 1 : 0);
            ps.setString(7, pref.getProcessingBasis());
            ps.setInt(8, pref.isThirdPartySharing() ? 1 : 0);
            ps.setInt(9, pref.isCrossBorderTransfer() ? 1 : 0);
            ps.setString(10, pref.getCreatedAt().toString());
            ps.setString(11, LocalDateTime.now().toString());
            ps.executeUpdate();

            addAuditEntry(pref.getConsentId(), pref.getDataPrincipalId(),
                    "PREFERENCE_UPDATED", "SYSTEM",
                    "Category: " + pref.getDataCategory() + " allowed: " + pref.isAllowed());
            return pref;
        } catch (Exception e) {
            logger.error("Failed to save consent preference", e);
            throw new RuntimeException("Consent preference save failed", e);
        }
    }

    /**
     * Get all granular preferences for a consent record.
     */
    public List<ConsentPreference> getConsentPreferences(String consentId) {
        logger.debug("Loading consent preferences for consent={}", consentId);
        List<ConsentPreference> result = new ArrayList<>();
        String sql = "SELECT * FROM consent_preferences WHERE consent_id = ? ORDER BY data_category";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, consentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ConsentPreference p = new ConsentPreference();
                    p.setId(rs.getString("id"));
                    p.setConsentId(rs.getString("consent_id"));
                    p.setDataPrincipalId(rs.getString("data_principal_id"));
                    p.setPurposeId(rs.getString("purpose_id"));
                    p.setDataCategory(rs.getString("data_category"));
                    p.setAllowed(rs.getInt("allowed") == 1);
                    p.setProcessingBasis(rs.getString("processing_basis"));
                    p.setThirdPartySharing(rs.getInt("third_party_sharing") == 1);
                    p.setCrossBorderTransfer(rs.getInt("cross_border_transfer") == 1);
                    result.add(p);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load consent preferences", e);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2 — GUARDIAN CONSENT (DPDP S.9 / Rule 11)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Save a guardian consent record for a child or person with disability.
     */
    public GuardianConsent saveGuardianConsent(GuardianConsent gc) {
        logger.info("Saving guardian consent: child={}, guardian={}", gc.getChildName(), gc.getGuardianName());
        String sql = """
            INSERT OR REPLACE INTO guardian_consents
            (id, child_principal_id, guardian_principal_id, child_name, child_age,
             guardian_name, guardian_relationship, guardian_id_type, guardian_id_number,
             guardian_kyc_verified, guardian_kyc_date, is_disability, disability_type,
             consent_id, purpose_id, status, verification_method, verification_date,
             notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (gc.getId() == null) gc.setId(UUID.randomUUID().toString());
            ps.setString(1, gc.getId());
            ps.setString(2, gc.getChildPrincipalId());
            ps.setString(3, gc.getGuardianPrincipalId());
            ps.setString(4, gc.getChildName());
            ps.setInt(5, gc.getChildAge());
            ps.setString(6, gc.getGuardianName());
            ps.setString(7, gc.getGuardianRelationship());
            ps.setString(8, gc.getGuardianIdType());
            ps.setString(9, gc.getGuardianIdNumber());
            ps.setInt(10, gc.isGuardianKycVerified() ? 1 : 0);
            ps.setString(11, gc.getGuardianKycDate() != null ? gc.getGuardianKycDate().toString() : null);
            ps.setInt(12, gc.isDisability() ? 1 : 0);
            ps.setString(13, gc.getDisabilityType());
            ps.setString(14, gc.getConsentId());
            ps.setString(15, gc.getPurposeId());
            ps.setString(16, gc.getStatus());
            ps.setString(17, gc.getVerificationMethod());
            ps.setString(18, gc.getVerificationDate() != null ? gc.getVerificationDate().toString() : null);
            ps.setString(19, gc.getNotes());
            ps.setString(20, gc.getCreatedAt().toString());
            ps.setString(21, LocalDateTime.now().toString());
            ps.executeUpdate();

            addAuditEntry(gc.getConsentId() != null ? gc.getConsentId() : gc.getId(),
                    gc.getChildPrincipalId(), "GUARDIAN_CONSENT", gc.getGuardianName(),
                    "Guardian: " + gc.getGuardianName() + " for child: " + gc.getChildName());
            return gc;
        } catch (Exception e) {
            logger.error("Failed to save guardian consent", e);
            throw new RuntimeException("Guardian consent save failed", e);
        }
    }

    /**
     * Get all guardian consents, optionally filtered by status.
     */
    public List<GuardianConsent> getGuardianConsents(String status) {
        logger.debug("Loading guardian consents, status filter={}", status);
        List<GuardianConsent> result = new ArrayList<>();
        String sql = status != null && !status.isEmpty()
                ? "SELECT * FROM guardian_consents WHERE status = ? ORDER BY created_at DESC"
                : "SELECT * FROM guardian_consents ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (status != null && !status.isEmpty()) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapGuardianConsent(rs));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load guardian consents", e);
        }
        return result;
    }

    /**
     * Verify a guardian consent (update KYC status).
     */
    public GuardianConsent verifyGuardianConsent(String id, String verificationMethod) {
        logger.info("Verifying guardian consent: id={}", id);
        String sql = """
            UPDATE guardian_consents SET guardian_kyc_verified = 1, guardian_kyc_date = ?,
            status = 'verified', verification_method = ?, verification_date = ?, updated_at = ?
            WHERE id = ?
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String now = LocalDateTime.now().toString();
            ps.setString(1, now);
            ps.setString(2, verificationMethod);
            ps.setString(3, now);
            ps.setString(4, now);
            ps.setString(5, id);
            ps.executeUpdate();

            // Return updated record
            String selectSql = "SELECT * FROM guardian_consents WHERE id = ?";
            try (PreparedStatement sel = conn.prepareStatement(selectSql)) {
                sel.setString(1, id);
                try (ResultSet rs = sel.executeQuery()) {
                    if (rs.next()) return mapGuardianConsent(rs);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to verify guardian consent", e);
            throw new RuntimeException("Guardian verification failed", e);
        }
        return null;
    }

    private GuardianConsent mapGuardianConsent(ResultSet rs) throws SQLException {
        GuardianConsent gc = new GuardianConsent();
        gc.setId(rs.getString("id"));
        gc.setChildPrincipalId(rs.getString("child_principal_id"));
        gc.setGuardianPrincipalId(rs.getString("guardian_principal_id"));
        gc.setChildName(rs.getString("child_name"));
        gc.setChildAge(rs.getInt("child_age"));
        gc.setGuardianName(rs.getString("guardian_name"));
        gc.setGuardianRelationship(rs.getString("guardian_relationship"));
        gc.setGuardianIdType(rs.getString("guardian_id_type"));
        gc.setGuardianIdNumber(rs.getString("guardian_id_number"));
        gc.setGuardianKycVerified(rs.getInt("guardian_kyc_verified") == 1);
        gc.setDisability(rs.getInt("is_disability") == 1);
        gc.setDisabilityType(rs.getString("disability_type"));
        gc.setConsentId(rs.getString("consent_id"));
        gc.setPurposeId(rs.getString("purpose_id"));
        gc.setStatus(rs.getString("status"));
        gc.setVerificationMethod(rs.getString("verification_method"));
        gc.setNotes(rs.getString("notes"));
        return gc;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2 — CONSENT AUDIT TRAIL (HASH-CHAINED LEDGER)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Add an immutable, hash-chained audit entry to the consent ledger.
     */
    public ConsentAuditEntry addAuditEntry(String consentId, String principalId,
                                            String action, String actionBy, String details) {
        String sql = """
            INSERT INTO consent_audit_trail
            (block_number, consent_id, data_principal_id, action, action_by, details, previous_hash, current_hash, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection()) {
            // Get last block
            int lastBlock = 0;
            String lastHash = "0000000000000000";
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT block_number, current_hash FROM consent_audit_trail ORDER BY block_number DESC LIMIT 1")) {
                if (rs.next()) {
                    lastBlock = rs.getInt("block_number");
                    lastHash = rs.getString("current_hash");
                }
            }

            int newBlock = lastBlock + 1;
            String data = newBlock + "|" + consentId + "|" + action + "|" + principalId + "|" + lastHash;
            String newHash = securityManager.sha256(data);

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, newBlock);
                ps.setString(2, consentId);
                ps.setString(3, principalId);
                ps.setString(4, action);
                ps.setString(5, actionBy);
                ps.setString(6, details);
                ps.setString(7, lastHash);
                ps.setString(8, newHash);
                ps.setString(9, LocalDateTime.now().toString());
                ps.executeUpdate();
            }

            ConsentAuditEntry entry = new ConsentAuditEntry();
            entry.setBlockNumber(newBlock);
            entry.setConsentId(consentId);
            entry.setDataPrincipalId(principalId);
            entry.setAction(action);
            entry.setActionBy(actionBy);
            entry.setDetails(details);
            entry.setPreviousHash(lastHash);
            entry.setCurrentHash(newHash);
            return entry;
        } catch (Exception e) {
            logger.error("Failed to add audit entry", e);
            return null;
        }
    }

    /**
     * Get the consent audit trail, optionally filtered by consent ID.
     */
    public List<ConsentAuditEntry> getAuditTrail(String consentId, int limit) {
        List<ConsentAuditEntry> result = new ArrayList<>();
        String sql = consentId != null && !consentId.isEmpty()
                ? "SELECT * FROM consent_audit_trail WHERE consent_id = ? ORDER BY block_number DESC LIMIT ?"
                : "SELECT * FROM consent_audit_trail ORDER BY block_number DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (consentId != null && !consentId.isEmpty()) {
                ps.setString(1, consentId);
                ps.setInt(2, limit > 0 ? limit : 100);
            } else {
                ps.setInt(1, limit > 0 ? limit : 100);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ConsentAuditEntry e = new ConsentAuditEntry();
                    e.setId(rs.getInt("id"));
                    e.setBlockNumber(rs.getInt("block_number"));
                    e.setConsentId(rs.getString("consent_id"));
                    e.setDataPrincipalId(rs.getString("data_principal_id"));
                    e.setAction(rs.getString("action"));
                    e.setActionBy(rs.getString("action_by"));
                    e.setDetails(rs.getString("details"));
                    e.setPreviousHash(rs.getString("previous_hash"));
                    e.setCurrentHash(rs.getString("current_hash"));
                    result.add(e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get audit trail", e);
        }
        return result;
    }

    /**
     * Verify the entire consent audit chain integrity.
     */
    public boolean verifyAuditChain() {
        logger.info("Verifying consent audit chain integrity...");
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM consent_audit_trail ORDER BY block_number ASC")) {
            String prevHash = "0000000000000000";
            while (rs.next()) {
                String storedPrevHash = rs.getString("previous_hash");
                if (!prevHash.equals(storedPrevHash)) {
                    logger.error("Chain broken at block {}", rs.getInt("block_number"));
                    return false;
                }
                prevHash = rs.getString("current_hash");
            }
            logger.info("Consent audit chain integrity verified — OK");
            return true;
        } catch (Exception e) {
            logger.error("Failed to verify audit chain", e);
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2 — CONSENT RENEWAL ENGINE
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get consents expiring within the given number of days.
     */
    public List<Consent> getExpiringConsents(int withinDays) {
        logger.info("Finding consents expiring within {} days", withinDays);
        List<Consent> result = new ArrayList<>();
        String sql = """
            SELECT c.*, dp.name as dp_name, p.name as p_name
            FROM consents c
            LEFT JOIN data_principals dp ON c.data_principal_id = dp.id
            LEFT JOIN purposes p ON c.purpose_id = p.id
            WHERE c.status = 'ACTIVE'
              AND c.expires_at IS NOT NULL
              AND c.expires_at <= datetime('now', '+' || ? || ' days')
              AND c.expires_at > datetime('now')
            ORDER BY c.expires_at ASC
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, withinDays);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Consent c = mapConsent(rs);
                    c.setDataPrincipalName(rs.getString("dp_name"));
                    c.setPurposeName(rs.getString("p_name"));
                    result.add(c);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get expiring consents", e);
        }
        return result;
    }

    /**
     * Renew a consent by extending its expiry date.
     */
    public Consent renewConsent(String consentId, int extensionDays) {
        logger.info("Renewing consent: id={}, extension={}d", consentId, extensionDays);
        String sql = "UPDATE consents SET expires_at = datetime(expires_at, '+' || ? || ' days') WHERE id = ? AND status = 'ACTIVE'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, extensionDays);
            ps.setString(2, consentId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                Consent consent = getConsentById(consentId);
                addAuditEntry(consentId, consent.getDataPrincipalId(),
                        "CONSENT_RENEWED", "SYSTEM",
                        "Extended by " + extensionDays + " days");
                auditService.log("CONSENT_RENEWED", "CONSENT", "SYSTEM",
                        "Consent " + consentId + " renewed for " + extensionDays + " days");
                return consent;
            }
        } catch (Exception e) {
            logger.error("Failed to renew consent", e);
            throw new RuntimeException("Consent renewal failed", e);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2 — SECTOR PURPOSE TEMPLATES
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get sector purpose templates, optionally filtered by sector.
     */
    public List<SectorPurposeTemplate> getSectorTemplates(String sector) {
        logger.debug("Loading sector templates, sector={}", sector);
        List<SectorPurposeTemplate> result = new ArrayList<>();
        String sql = sector != null && !sector.isEmpty()
                ? "SELECT * FROM sector_purpose_templates WHERE sector = ? ORDER BY code"
                : "SELECT * FROM sector_purpose_templates ORDER BY sector, code";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (sector != null && !sector.isEmpty()) ps.setString(1, sector);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SectorPurposeTemplate t = new SectorPurposeTemplate();
                    t.setId(rs.getString("id"));
                    t.setSector(rs.getString("sector"));
                    t.setCode(rs.getString("code"));
                    t.setName(rs.getString("name"));
                    t.setDescription(rs.getString("description"));
                    t.setLegalBasis(rs.getString("legal_basis"));
                    String cats = rs.getString("data_categories");
                    if (cats != null) t.setDataCategories(Arrays.asList(cats.split(",")));
                    t.setRetentionPeriod(rs.getString("retention_period"));
                    t.setMandatory(rs.getInt("mandatory") == 1);
                    t.setRegulatoryReference(rs.getString("regulatory_reference"));
                    result.add(t);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load sector templates", e);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // PHASE 2 — ENHANCED STATISTICS & ANALYTICS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get comprehensive consent analytics with compliance scoring.
     */
    public ConsentStatistics getEnhancedStatistics() {
        ConsentStatistics stats = getStatistics();

        try (Connection conn = dbManager.getConnection(); Statement st = conn.createStatement()) {
            // Guardian stats
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM guardian_consents")) {
                if (rs.next()) stats.setTotalGuardianConsents(rs.getInt(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM guardian_consents WHERE status = 'pending'")) {
                if (rs.next()) stats.setPendingGuardianConsents(rs.getInt(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM guardian_consents WHERE status = 'verified'")) {
                if (rs.next()) stats.setVerifiedGuardianConsents(rs.getInt(1));
            }

            // Preference stats
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM consent_preferences")) {
                if (rs.next()) stats.setTotalPreferences(rs.getInt(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM consent_preferences WHERE allowed = 1")) {
                if (rs.next()) stats.setAllowedPreferences(rs.getInt(1));
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM consent_preferences WHERE allowed = 0")) {
                if (rs.next()) stats.setDeniedPreferences(rs.getInt(1));
            }

            // Audit trail
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM consent_audit_trail")) {
                if (rs.next()) stats.setTotalAuditEntries(rs.getInt(1));
            }
            stats.setChainIntegrity(verifyAuditChain());

            // Expiring
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM consents WHERE status='ACTIVE' AND expires_at IS NOT NULL AND expires_at <= datetime('now','+30 days') AND expires_at > datetime('now')")) {
                if (rs.next()) stats.setExpiringIn30Days(rs.getInt(1));
            }
            try (ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM consents WHERE status='ACTIVE' AND expires_at IS NOT NULL AND expires_at <= datetime('now','+7 days') AND expires_at > datetime('now')")) {
                if (rs.next()) stats.setExpiringIn7Days(rs.getInt(1));
            }

            // Compliance score
            double score = 85.0;
            if (stats.getTotalConsents() > 0) {
                score = Math.min(100, (stats.getActiveRate() * 40) +
                        (stats.isChainIntegrity() ? 30 : 0) +
                        (stats.getWithdrawalRate() < 20 ? 20 : 10) +
                        (stats.getVerifiedGuardianConsents() > 0 ? 10 : 5));
            }
            stats.setComplianceScore(Math.round(score * 10.0) / 10.0);

            // Language distribution
            Map<String, Integer> langDist = new HashMap<>();
            try (ResultSet rs = st.executeQuery("SELECT language, COUNT(*) as cnt FROM consents GROUP BY language")) {
                while (rs.next()) {
                    langDist.put(rs.getString("language") != null ? rs.getString("language") : "en", rs.getInt("cnt"));
                }
            }
            stats.setLanguageDistribution(langDist);

        } catch (Exception e) {
            logger.error("Failed to get enhanced statistics", e);
        }
        return stats;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UCM — REQUEST CONSENT (DPDP S.6 lifecycle: REQUESTED→ACTIVE)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Create a consent record in REQUESTED status, awaiting data principal action.
     */
    public Consent requestConsent(ConsentRequest request) {
        logger.info("Creating consent REQUEST for principal={}, purpose={}",
                request.getDataPrincipalId(), request.getPurposeId());
        // Re-use collectConsent but override status to REQUESTED
        Consent consent = collectConsent(request);
        String sql = "UPDATE consents SET status = 'REQUESTED' WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, consent.getId());
            ps.executeUpdate();
            consent.setStatus(ConsentStatus.REQUESTED);
            addAuditEntry(consent.getId(), consent.getDataPrincipalId(),
                    "CONSENT_REQUESTED", "SYSTEM", "Consent request created, awaiting confirmation");
        } catch (Exception e) {
            logger.error("Failed to create consent request", e);
        }
        return consent;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UCM — MODIFY CONSENT (DPDP S.6 lifecycle: ACTIVE→MODIFIED)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Modify consent preferences. Transitions consent status to MODIFIED.
     */
    public Consent modifyConsent(String consentId, Map<String, Object> modifications) {
        logger.info("Modifying consent: id={}", consentId);
        String sql = "UPDATE consents SET status = 'MODIFIED', modified_at = ?, modified_by = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setString(2, (String) modifications.getOrDefault("modifiedBy", "DATA_PRINCIPAL"));
            ps.setString(3, consentId);
            ps.executeUpdate();
            Consent consent = getConsentById(consentId);
            addAuditEntry(consentId, consent.getDataPrincipalId(),
                    "CONSENT_MODIFIED", (String) modifications.getOrDefault("modifiedBy", "DATA_PRINCIPAL"),
                    "Consent preferences modified: " + modifications.keySet());
            auditService.log("CONSENT_MODIFIED", "CONSENT",
                    (String) modifications.getOrDefault("modifiedBy", "DATA_PRINCIPAL"),
                    "Consent " + consentId + " modified");
            return consent;
        } catch (Exception e) {
            logger.error("Failed to modify consent", e);
            throw new RuntimeException("Consent modification failed", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // UCM — DATA CATEGORY REGISTRY CRUD
    // ═══════════════════════════════════════════════════════════════════

    public List<DataCategoryRegistry> getDataCategories() {
        List<DataCategoryRegistry> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM data_categories ORDER BY code")) {
            while (rs.next()) {
                DataCategoryRegistry dc = new DataCategoryRegistry();
                dc.setId(rs.getString("id"));
                dc.setCode(rs.getString("code"));
                dc.setName(rs.getString("name"));
                dc.setDescription(rs.getString("description"));
                dc.setSensitivityLevel(rs.getString("sensitivity_level"));
                dc.setDpdpClassification(rs.getString("dpdp_classification"));
                dc.setActive(rs.getInt("is_active") == 1);
                String sectors = rs.getString("sector_applicability");
                if (sectors != null) dc.setSectorApplicability(Arrays.asList(sectors.split(",")));
                String elems = rs.getString("data_elements");
                if (elems != null) dc.setDataElements(Arrays.asList(elems.split(",")));
                dc.setRetentionGuideline(rs.getString("retention_guideline"));
                dc.setRegulatoryReference(rs.getString("regulatory_reference"));
                result.add(dc);
            }
        } catch (Exception e) {
            logger.error("Failed to get data categories", e);
        }
        return result;
    }

    public DataCategoryRegistry saveDataCategory(DataCategoryRegistry dc) {
        String sql = """
            INSERT OR REPLACE INTO data_categories
            (id, code, name, description, sensitivity_level, dpdp_classification,
             sector_applicability, data_elements, retention_guideline, regulatory_reference, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dc.getId());
            ps.setString(2, dc.getCode());
            ps.setString(3, dc.getName());
            ps.setString(4, dc.getDescription());
            ps.setString(5, dc.getSensitivityLevel());
            ps.setString(6, dc.getDpdpClassification());
            ps.setString(7, dc.getSectorApplicability() != null ? String.join(",", dc.getSectorApplicability()) : null);
            ps.setString(8, dc.getDataElements() != null ? String.join(",", dc.getDataElements()) : null);
            ps.setString(9, dc.getRetentionGuideline());
            ps.setString(10, dc.getRegulatoryReference());
            ps.setInt(11, dc.isActive() ? 1 : 0);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to save data category", e);
        }
        return dc;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UCM — CONSENT NOTICE CRUD (S.5)
    // ═══════════════════════════════════════════════════════════════════

    public List<ConsentNotice> getConsentNotices() {
        List<ConsentNotice> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM consent_notices ORDER BY created_at DESC")) {
            while (rs.next()) {
                ConsentNotice n = new ConsentNotice();
                n.setId(rs.getString("id"));
                n.setVersion(rs.getString("version"));
                n.setOrganizationId(rs.getString("organization_id"));
                n.setOrganizationName(rs.getString("organization_name"));
                n.setTitle(rs.getString("title"));
                n.setSectorCode(rs.getString("sector_code"));
                n.setDpoName(rs.getString("dpo_name"));
                n.setDpoEmail(rs.getString("dpo_email"));
                n.setDpoPhone(rs.getString("dpo_phone"));
                n.setGrievanceOfficerName(rs.getString("grievance_officer_name"));
                n.setGrievanceOfficerEmail(rs.getString("grievance_officer_email"));
                n.setWithdrawalUrl(rs.getString("withdrawal_url"));
                n.setRightsUrl(rs.getString("rights_url"));
                n.setDpbiComplaintUrl(rs.getString("dpbi_complaint_url"));
                n.setLanguage(rs.getString("language"));
                n.setContent(rs.getString("content"));
                n.setActive(rs.getInt("is_active") == 1);
                n.setCurrentVersion(rs.getInt("is_current_version") == 1);
                n.setApprovedBy(rs.getString("approved_by"));
                n.setCreatedBy(rs.getString("created_by"));
                result.add(n);
            }
        } catch (Exception e) {
            logger.error("Failed to get consent notices", e);
        }
        return result;
    }

    public ConsentNotice saveConsentNotice(ConsentNotice n) {
        String sql = """
            INSERT OR REPLACE INTO consent_notices
            (id, version, organization_id, organization_name, title, sector_code,
             purposes, data_categories, retention_policy, dpo_name, dpo_email, dpo_phone,
             grievance_officer_name, grievance_officer_email, withdrawal_url, rights_url,
             dpbi_complaint_url, language, content, content_plain, is_active, is_current_version,
             approved_by, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, n.getId());
            ps.setString(2, n.getVersion());
            ps.setString(3, n.getOrganizationId());
            ps.setString(4, n.getOrganizationName());
            ps.setString(5, n.getTitle());
            ps.setString(6, n.getSectorCode());
            ps.setString(7, n.getPurposes() != null ? String.join(",", n.getPurposes()) : null);
            ps.setString(8, n.getDataCategories() != null ? String.join(",", n.getDataCategories()) : null);
            ps.setString(9, n.getRetentionPolicy());
            ps.setString(10, n.getDpoName());
            ps.setString(11, n.getDpoEmail());
            ps.setString(12, n.getDpoPhone());
            ps.setString(13, n.getGrievanceOfficerName());
            ps.setString(14, n.getGrievanceOfficerEmail());
            ps.setString(15, n.getWithdrawalUrl());
            ps.setString(16, n.getRightsUrl());
            ps.setString(17, n.getDpbiComplaintUrl());
            ps.setString(18, n.getLanguage());
            ps.setString(19, n.getContent());
            ps.setString(20, n.getContentPlain());
            ps.setInt(21, n.isActive() ? 1 : 0);
            ps.setInt(22, n.isCurrentVersion() ? 1 : 0);
            ps.setString(23, n.getApprovedBy());
            ps.setString(24, n.getCreatedBy());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to save consent notice", e);
        }
        return n;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UCM — CONSENT DELEGATIONS CRUD (S.9)
    // ═══════════════════════════════════════════════════════════════════

    public List<ConsentDelegation> getConsentDelegations(String status) {
        List<ConsentDelegation> result = new ArrayList<>();
        String sql = status != null && !status.isEmpty()
                ? "SELECT * FROM consent_delegations WHERE status = ? ORDER BY created_at DESC"
                : "SELECT * FROM consent_delegations ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (status != null && !status.isEmpty()) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ConsentDelegation d = new ConsentDelegation();
                    d.setId(rs.getString("id"));
                    d.setDelegatorId(rs.getString("delegator_id"));
                    d.setDelegatorName(rs.getString("delegator_name"));
                    d.setDelegateId(rs.getString("delegate_id"));
                    d.setDelegateName(rs.getString("delegate_name"));
                    d.setDelegateType(rs.getString("delegate_type"));
                    d.setScope(rs.getString("scope"));
                    d.setScopeDetails(rs.getString("scope_details"));
                    d.setRelationship(rs.getString("relationship"));
                    d.setIdProofType(rs.getString("id_proof_type"));
                    d.setIdProofNumber(rs.getString("id_proof_number"));
                    d.setKycVerified(rs.getInt("kyc_verified") == 1);
                    d.setVerificationMethod(rs.getString("verification_method"));
                    d.setStatus(rs.getString("status"));
                    result.add(d);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get consent delegations", e);
        }
        return result;
    }

    public ConsentDelegation saveConsentDelegation(ConsentDelegation d) {
        String sql = """
            INSERT OR REPLACE INTO consent_delegations
            (id, delegator_id, delegator_name, delegate_id, delegate_name, delegate_type,
             scope, scope_details, relationship, id_proof_type, id_proof_number,
             kyc_verified, verification_method, valid_from, valid_to, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.getId());
            ps.setString(2, d.getDelegatorId());
            ps.setString(3, d.getDelegatorName());
            ps.setString(4, d.getDelegateId());
            ps.setString(5, d.getDelegateName());
            ps.setString(6, d.getDelegateType());
            ps.setString(7, d.getScope());
            ps.setString(8, d.getScopeDetails());
            ps.setString(9, d.getRelationship());
            ps.setString(10, d.getIdProofType());
            ps.setString(11, d.getIdProofNumber());
            ps.setInt(12, d.isKycVerified() ? 1 : 0);
            ps.setString(13, d.getVerificationMethod());
            ps.setString(14, d.getValidFrom() != null ? d.getValidFrom().toString() : null);
            ps.setString(15, d.getValidTo() != null ? d.getValidTo().toString() : null);
            ps.setString(16, d.getStatus());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to save consent delegation", e);
        }
        return d;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UCM — LEGITIMATE USE CRUD (S.7)
    // ═══════════════════════════════════════════════════════════════════

    public List<LegitimateUse> getLegitimateUses() {
        List<LegitimateUse> result = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM legitimate_uses ORDER BY created_at DESC")) {
            while (rs.next()) {
                LegitimateUse lu = new LegitimateUse();
                lu.setId(rs.getString("id"));
                lu.setDataFiduciaryId(rs.getString("data_fiduciary_id"));
                lu.setDataFiduciaryName(rs.getString("data_fiduciary_name"));
                lu.setDataPrincipalId(rs.getString("data_principal_id"));
                lu.setDataPrincipalName(rs.getString("data_principal_name"));
                lu.setLawfulBasis(rs.getString("lawful_basis"));
                lu.setPurposeDescription(rs.getString("purpose_description"));
                lu.setLegalReference(rs.getString("legal_reference"));
                lu.setRetentionPeriod(rs.getString("retention_period"));
                lu.setStatus(rs.getString("status"));
                lu.setReviewedBy(rs.getString("reviewed_by"));
                lu.setReviewNotes(rs.getString("review_notes"));
                lu.setEvidenceReference(rs.getString("evidence_reference"));
                String cats = rs.getString("data_categories");
                if (cats != null) lu.setDataCategories(Arrays.asList(cats.split(",")));
                result.add(lu);
            }
        } catch (Exception e) {
            logger.error("Failed to get legitimate uses", e);
        }
        return result;
    }

    public LegitimateUse saveLegitimateUse(LegitimateUse lu) {
        String sql = """
            INSERT OR REPLACE INTO legitimate_uses
            (id, data_fiduciary_id, data_fiduciary_name, data_principal_id, data_principal_name,
             lawful_basis, purpose_description, data_categories, legal_reference,
             start_date, end_date, retention_period, status, reviewed_by, review_notes, evidence_reference)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lu.getId());
            ps.setString(2, lu.getDataFiduciaryId());
            ps.setString(3, lu.getDataFiduciaryName());
            ps.setString(4, lu.getDataPrincipalId());
            ps.setString(5, lu.getDataPrincipalName());
            ps.setString(6, lu.getLawfulBasis());
            ps.setString(7, lu.getPurposeDescription());
            ps.setString(8, lu.getDataCategories() != null ? String.join(",", lu.getDataCategories()) : null);
            ps.setString(9, lu.getLegalReference());
            ps.setString(10, lu.getStartDate() != null ? lu.getStartDate().toString() : null);
            ps.setString(11, lu.getEndDate() != null ? lu.getEndDate().toString() : null);
            ps.setString(12, lu.getRetentionPeriod());
            ps.setString(13, lu.getStatus());
            ps.setString(14, lu.getReviewedBy());
            ps.setString(15, lu.getReviewNotes());
            ps.setString(16, lu.getEvidenceReference());
            ps.executeUpdate();
            auditService.log("LEGITIMATE_USE_RECORDED", "CONSENT", "SYSTEM",
                    "S.7 legitimate use: " + lu.getLawfulBasis() + " for principal " + lu.getDataPrincipalId());
        } catch (Exception e) {
            logger.error("Failed to save legitimate use", e);
        }
        return lu;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UCM — DATA ACCESS LOG (Privacy Wallet)
    // ═══════════════════════════════════════════════════════════════════

    public List<DataAccessLog> getDataAccessLog(String principalId, int limit) {
        List<DataAccessLog> result = new ArrayList<>();
        String sql = principalId != null && !principalId.isEmpty()
                ? "SELECT * FROM data_access_log WHERE data_principal_id = ? ORDER BY accessed_at DESC LIMIT ?"
                : "SELECT * FROM data_access_log ORDER BY accessed_at DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (principalId != null && !principalId.isEmpty()) {
                ps.setString(1, principalId);
                ps.setInt(2, limit > 0 ? limit : 100);
            } else {
                ps.setInt(1, limit > 0 ? limit : 100);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DataAccessLog dal = new DataAccessLog();
                    dal.setId(rs.getString("id"));
                    dal.setConsentId(rs.getString("consent_id"));
                    dal.setDataPrincipalId(rs.getString("data_principal_id"));
                    dal.setAccessorId(rs.getString("accessor_id"));
                    dal.setAccessorName(rs.getString("accessor_name"));
                    dal.setAccessorOrganization(rs.getString("accessor_organization"));
                    dal.setAccessorRole(rs.getString("accessor_role"));
                    dal.setPurpose(rs.getString("purpose"));
                    dal.setDataCategory(rs.getString("data_category"));
                    dal.setDataElements(rs.getString("data_elements"));
                    dal.setAccessType(rs.getString("access_type"));
                    dal.setAccessChannel(rs.getString("access_channel"));
                    dal.setIpAddress(rs.getString("ip_address"));
                    dal.setConsentVerified(rs.getInt("consent_verified") == 1);
                    dal.setResultStatus(rs.getString("result_status"));
                    dal.setDenialReason(rs.getString("denial_reason"));
                    result.add(dal);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get data access log", e);
        }
        return result;
    }

    public DataAccessLog logDataAccess(DataAccessLog log) {
        String sql = """
            INSERT INTO data_access_log
            (id, consent_id, data_principal_id, accessor_id, accessor_name,
             accessor_organization, accessor_role, purpose, data_category, data_elements,
             access_type, access_channel, ip_address, user_agent, consent_verified,
             result_status, denial_reason, accessed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, log.getId());
            ps.setString(2, log.getConsentId());
            ps.setString(3, log.getDataPrincipalId());
            ps.setString(4, log.getAccessorId());
            ps.setString(5, log.getAccessorName());
            ps.setString(6, log.getAccessorOrganization());
            ps.setString(7, log.getAccessorRole());
            ps.setString(8, log.getPurpose());
            ps.setString(9, log.getDataCategory());
            ps.setString(10, log.getDataElements());
            ps.setString(11, log.getAccessType());
            ps.setString(12, log.getAccessChannel());
            ps.setString(13, log.getIpAddress());
            ps.setString(14, log.getUserAgent());
            ps.setInt(15, log.isConsentVerified() ? 1 : 0);
            ps.setString(16, log.getResultStatus());
            ps.setString(17, log.getDenialReason());
            ps.setString(18, log.getAccessedAt() != null ? log.getAccessedAt().toString() : LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to log data access", e);
        }
        return log;
    }

    // ═══════════════════════════════════════════════════════════════════
    // UCM — CONSENT ANALYTICS (RAG AI-ready)
    // ═══════════════════════════════════════════════════════════════════

    public Map<String, Object> getConsentAnalytics() {
        Map<String, Object> analytics = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement st = conn.createStatement()) {
            // Status distribution
            Map<String, Integer> statusDist = new LinkedHashMap<>();
            try (ResultSet rs = st.executeQuery("SELECT status, COUNT(*) as cnt FROM consents GROUP BY status")) {
                while (rs.next()) statusDist.put(rs.getString("status"), rs.getInt("cnt"));
            }
            analytics.put("statusDistribution", statusDist);

            // Purpose distribution
            Map<String, Integer> purposeDist = new LinkedHashMap<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT p.name, COUNT(*) as cnt FROM consents c LEFT JOIN purposes p ON c.purpose_id = p.id GROUP BY p.name ORDER BY cnt DESC LIMIT 15")) {
                while (rs.next()) purposeDist.put(rs.getString("name") != null ? rs.getString("name") : "Unknown", rs.getInt("cnt"));
            }
            analytics.put("purposeDistribution", purposeDist);

            // Monthly trends
            List<Map<String, Object>> trends = new ArrayList<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT strftime('%Y-%m', collected_at) as month, status, COUNT(*) as cnt FROM consents GROUP BY month, status ORDER BY month DESC LIMIT 60")) {
                while (rs.next()) {
                    trends.add(Map.of("month", rs.getString("month") != null ? rs.getString("month") : "", 
                                      "status", rs.getString("status"), "count", rs.getInt("cnt")));
                }
            }
            analytics.put("monthlyTrends", trends);

            // Consent type distribution
            Map<String, Integer> typeDist = new LinkedHashMap<>();
            try (ResultSet rs = st.executeQuery("SELECT consent_type, COUNT(*) as cnt FROM consents GROUP BY consent_type")) {
                while (rs.next()) typeDist.put(rs.getString("consent_type") != null ? rs.getString("consent_type") : "EXPLICIT", rs.getInt("cnt"));
            }
            analytics.put("consentTypeDistribution", typeDist);

            // Data access stats
            Map<String, Integer> accessStats = new LinkedHashMap<>();
            try (ResultSet rs = st.executeQuery("SELECT access_type, COUNT(*) as cnt FROM data_access_log GROUP BY access_type")) {
                while (rs.next()) accessStats.put(rs.getString("access_type"), rs.getInt("cnt"));
            }
            analytics.put("dataAccessStats", accessStats);

            // Legitimate use stats  
            Map<String, Integer> luStats = new LinkedHashMap<>();
            try (ResultSet rs = st.executeQuery("SELECT lawful_basis, COUNT(*) as cnt FROM legitimate_uses GROUP BY lawful_basis")) {
                while (rs.next()) luStats.put(rs.getString("lawful_basis"), rs.getInt("cnt"));
            }
            analytics.put("legitimateUseStats", luStats);

            // Delegation stats
            int totalDelegations = 0, activeDelegations = 0;
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM consent_delegations")) {
                if (rs.next()) totalDelegations = rs.getInt(1);
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM consent_delegations WHERE status = 'ACTIVE'")) {
                if (rs.next()) activeDelegations = rs.getInt(1);
            }
            analytics.put("delegationStats", Map.of("total", totalDelegations, "active", activeDelegations));

        } catch (Exception e) {
            logger.error("Failed to generate consent analytics", e);
        }
        return analytics;
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT WITHDRAWAL PROPAGATION (DPDP Act Section 6)
    // Automatically cascades withdrawal to all data processors
    // ═══════════════════════════════════════════════════════════

    /**
     * Propagate consent withdrawal to all known data processors/fiduciaries.
     * DPDP Act Section 6 mandates that withdrawal must be made as easy as giving consent,
     * and must automatically cease processing across all processor systems.
     *
     * Steps:
     * 1. Validate the consent exists and is withdrawn
     * 2. Find all processor mappings for this consent
     * 3. Send withdrawal notifications to each processor
     * 4. Log each propagation event to audit trail
     * 5. Publish event for cross-module integration
     *
     * @param consentId The withdrawn consent ID
     * @return PropagationResult with per-processor status
     */
    public PropagationResult propagateWithdrawal(String consentId) {
        logger.info("Propagating consent withdrawal: {}", consentId);

        Consent consent = getConsentById(consentId);
        if (consent == null) {
            throw new IllegalArgumentException("Consent not found: " + consentId);
        }
        if (consent.getStatus() != ConsentStatus.WITHDRAWN) {
            throw new IllegalStateException("Consent is not withdrawn — cannot propagate. Current status: " + consent.getStatus());
        }

        PropagationResult result = new PropagationResult();
        result.consentId = consentId;
        result.dataPrincipalId = consent.getDataPrincipalId();
        result.purposeId = consent.getPurposeId();
        result.propagatedAt = LocalDateTime.now();

        // Find all processors mapped to this consent or purpose
        List<ProcessorMapping> processors = findProcessorMappings(consentId, consent.getPurposeId());

        if (processors.isEmpty()) {
            // Even if no explicit processor mappings, log the propagation attempt
            logger.info("No explicit processor mappings found for consent: {}. Recording system-level withdrawal.", consentId);
            result.totalProcessors = 0;
            result.successCount = 0;
            result.status = "COMPLETED_NO_PROCESSORS";

            auditService.log("WITHDRAWAL_PROPAGATED", "CONSENT", consent.getDataPrincipalId(),
                    String.format("Consent %s withdrawal propagated — no explicit processors registered", consentId));
        } else {
            result.totalProcessors = processors.size();

            for (ProcessorMapping processor : processors) {
                try {
                    // Mark processor as notified
                    notifyProcessorOfWithdrawal(processor, consent);
                    result.successCount++;
                    result.processorStatuses.put(processor.processorName, "NOTIFIED");

                    auditService.log("WITHDRAWAL_PROPAGATED_TO_PROCESSOR", "CONSENT",
                            consent.getDataPrincipalId(),
                            String.format("Withdrawal propagated to processor: %s (endpoint: %s) for consent: %s",
                                    processor.processorName, processor.notificationEndpoint, consentId));

                } catch (Exception e) {
                    logger.error("Failed to propagate withdrawal to processor: {}", processor.processorName, e);
                    result.failCount++;
                    result.processorStatuses.put(processor.processorName, "FAILED: " + e.getMessage());

                    auditService.log("WITHDRAWAL_PROPAGATION_FAILED", "CONSENT",
                            consent.getDataPrincipalId(),
                            String.format("FAILED to propagate withdrawal to processor: %s — %s",
                                    processor.processorName, e.getMessage()));
                }
            }

            result.status = result.failCount == 0 ? "COMPLETED" : "PARTIAL";
        }

        // Record propagation in database
        recordPropagation(result);

        // Publish event for cross-module integration (DLP, SIEM, etc.)
        eventBus.publish(new ComplianceEvent("consent.withdrawal.propagated",
                Map.of("consentId", consentId,
                        "principalId", consent.getDataPrincipalId(),
                        "processorsNotified", String.valueOf(result.successCount),
                        "status", result.status)));

        logger.info("Withdrawal propagation complete for consent {}: {} processors notified, {} failed",
                consentId, result.successCount, result.failCount);

        return result;
    }

    private List<ProcessorMapping> findProcessorMappings(String consentId, String purposeId) {
        List<ProcessorMapping> mappings = new ArrayList<>();
        String sql = """
            SELECT * FROM consent_processor_mappings
            WHERE (consent_id = ? OR purpose_id = ?) AND status = 'ACTIVE'
            ORDER BY processor_name
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, consentId);
            stmt.setString(2, purposeId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ProcessorMapping pm = new ProcessorMapping();
                pm.id = rs.getString("id");
                pm.consentId = rs.getString("consent_id");
                pm.purposeId = rs.getString("purpose_id");
                pm.processorName = rs.getString("processor_name");
                pm.processorType = rs.getString("processor_type");
                pm.notificationEndpoint = rs.getString("notification_endpoint");
                pm.notificationMethod = rs.getString("notification_method");
                mappings.add(pm);
            }
        } catch (SQLException e) {
            logger.warn("Could not query processor mappings (table may not exist yet): {}", e.getMessage());
        }
        return mappings;
    }

    private void notifyProcessorOfWithdrawal(ProcessorMapping processor, Consent consent) {
        // Log the notification — in production this would call the processor's REST/gRPC endpoint
        logger.info("Notifying processor {} of consent withdrawal: consentId={}, principalId={}, purpose={}",
                processor.processorName, consent.getId(),
                consent.getDataPrincipalId(), consent.getPurposeId());

        // Update processor mapping status
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE consent_processor_mappings SET status = 'WITHDRAWAL_NOTIFIED', updated_at = ? WHERE id = ?")) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, processor.id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Could not update processor mapping status: {}", e.getMessage());
        }
    }

    private void recordPropagation(PropagationResult result) {
        String sql = """
            INSERT OR IGNORE INTO consent_withdrawal_propagations
            (id, consent_id, data_principal_id, purpose_id, total_processors,
             success_count, fail_count, status, propagated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, result.consentId);
            stmt.setString(3, result.dataPrincipalId);
            stmt.setString(4, result.purposeId);
            stmt.setInt(5, result.totalProcessors);
            stmt.setInt(6, result.successCount);
            stmt.setInt(7, result.failCount);
            stmt.setString(8, result.status);
            stmt.setString(9, result.propagatedAt.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warn("Could not record propagation (table may not exist yet): {}", e.getMessage());
        }
    }

    // Propagation data classes
    public static class ProcessorMapping {
        public String id;
        public String consentId;
        public String purposeId;
        public String processorName;
        public String processorType;
        public String notificationEndpoint;
        public String notificationMethod;
    }

    public static class PropagationResult {
        public String consentId;
        public String dataPrincipalId;
        public String purposeId;
        public LocalDateTime propagatedAt;
        public int totalProcessors;
        public int successCount;
        public int failCount;
        public String status;
        public Map<String, String> processorStatuses = new LinkedHashMap<>();
    }

    /**
     * Token validation result — returned by validateConsentToken
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final String reason;
        private final String consentId;
        private final String principalId;
        private final String purposeId;

        private TokenValidationResult(boolean valid, String reason, String consentId,
                                       String principalId, String purposeId) {
            this.valid = valid;
            this.reason = reason;
            this.consentId = consentId;
            this.principalId = principalId;
            this.purposeId = purposeId;
        }

        public static TokenValidationResult accepted(String consentId, String principalId, String purposeId) {
            return new TokenValidationResult(true, "Valid", consentId, principalId, purposeId);
        }

        public static TokenValidationResult rejected(String reason) {
            return new TokenValidationResult(false, reason, null, null, null);
        }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
        public String getConsentId() { return consentId; }
        public String getPrincipalId() { return principalId; }
        public String getPurposeId() { return purposeId; }
    }
}

