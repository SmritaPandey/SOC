package com.qsdpdp.consent;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consent Orchestration Service — Global-Grade Consent Orchestration Platform
 *
 * Core principle: "CONSENT IS NOT A RECORD — IT IS A DYNAMIC, ENFORCEABLE, VERSIONED CONTRACT"
 *
 * Implements:
 * 1.  Consent as Contractual Object (legally enforceable digital contract)
 * 2.  Consent Versioning & Evolution (immutable version chain)
 * 3.  Purpose-Bundled Consent (granular multi-purpose bundles)
 * 4.  Consent Intelligence Layer (dormant/over-broad/unused detection)
 * 5.  Consent Receipt Generation (GDPR-grade downloadable receipt)
 * 6.  Consent Withdrawal Engine (instant, granular, bulk, propagation)
 * 7.  Consent Propagation System (event-driven)
 * 8.  Special Consent Types (minor, sensitive, emergency, research)
 * 9.  UX Compliance Engine (dark pattern prevention, proof storage)
 * 10. Trust & Transparency (purpose clarity score, usage frequency)
 * 11. AI-Driven Consent Governance (recommendations)
 *
 * BACKWARD COMPATIBLE — extends existing ConsentService without modification.
 *
 * @version 2.0.0
 * @since Phase 7 — Consent Orchestration Enhancement
 */
@Service
public class ConsentOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(ConsentOrchestrationService.class);

    @Autowired private DatabaseManager dbManager;
    @Autowired private AuditService auditService;
    @Autowired private EventBus eventBus;
    @Autowired(required = false) private ConsentEnforcementEngine enforcementEngine;

    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            // ── Consent Contractual Object (enhanced schema) ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_contracts (
                    consent_id TEXT NOT NULL,
                    version INTEGER NOT NULL DEFAULT 1,
                    data_principal_token TEXT NOT NULL,
                    data_fiduciary_id TEXT NOT NULL DEFAULT 'QS-DPDP-ENTERPRISE',
                    purpose_id TEXT NOT NULL,
                    purpose_description TEXT,
                    data_categories TEXT NOT NULL,
                    processing_actions TEXT DEFAULT 'read',
                    legal_basis TEXT DEFAULT 'CONSENT',
                    jurisdiction_tag TEXT DEFAULT 'IN',
                    retention_policy_id TEXT,
                    start_date TIMESTAMP NOT NULL,
                    expiry_date TIMESTAMP,
                    status TEXT DEFAULT 'ACTIVE',
                    capture_channel TEXT DEFAULT 'web',
                    ux_snapshot_hash TEXT,
                    language_code TEXT DEFAULT 'en',
                    cryptographic_hash TEXT,
                    digital_signature TEXT,
                    consent_type TEXT DEFAULT 'STANDARD',
                    bundle_id TEXT,
                    parent_consent_id TEXT,
                    change_reason TEXT,
                    changed_by TEXT,
                    previous_hash TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (consent_id, version)
                )
            """);

            // ── Consent Version Audit ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_version_log (
                    id TEXT PRIMARY KEY,
                    consent_id TEXT NOT NULL,
                    from_version INTEGER,
                    to_version INTEGER NOT NULL,
                    change_type TEXT NOT NULL,
                    change_description TEXT,
                    changed_by TEXT,
                    changed_fields TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Purpose Registry ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purpose_registry (
                    purpose_id TEXT PRIMARY KEY,
                    purpose_code TEXT UNIQUE NOT NULL,
                    purpose_name TEXT NOT NULL,
                    purpose_description TEXT,
                    sector TEXT,
                    is_mandatory INTEGER DEFAULT 0,
                    data_categories TEXT,
                    legal_reference TEXT,
                    retention_days INTEGER DEFAULT 365,
                    language_translations TEXT,
                    active INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Consent Bundle ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_bundles (
                    bundle_id TEXT PRIMARY KEY,
                    bundle_name TEXT NOT NULL,
                    sector TEXT,
                    purpose_ids TEXT NOT NULL,
                    mandatory_purposes TEXT,
                    optional_purposes TEXT,
                    description TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Consent Receipts ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_receipts (
                    receipt_id TEXT PRIMARY KEY,
                    consent_id TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    principal_id TEXT NOT NULL,
                    receipt_content TEXT NOT NULL,
                    receipt_format TEXT DEFAULT 'JSON',
                    delivery_channel TEXT,
                    delivered_at TIMESTAMP,
                    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Consent Intelligence ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_intelligence (
                    id TEXT PRIMARY KEY,
                    consent_id TEXT NOT NULL,
                    intelligence_type TEXT NOT NULL,
                    finding TEXT NOT NULL,
                    severity TEXT DEFAULT 'INFO',
                    recommendation TEXT,
                    auto_action TEXT,
                    resolved INTEGER DEFAULT 0,
                    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── UX Compliance Proofs ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_ux_proofs (
                    id TEXT PRIMARY KEY,
                    consent_id TEXT NOT NULL,
                    ux_snapshot_hash TEXT NOT NULL,
                    screenshot_reference TEXT,
                    ui_elements TEXT,
                    dark_pattern_check INTEGER DEFAULT 1,
                    pre_ticked_check INTEGER DEFAULT 1,
                    layered_notice_check INTEGER DEFAULT 1,
                    plain_language_check INTEGER DEFAULT 1,
                    accessibility_check INTEGER DEFAULT 1,
                    compliance_score REAL DEFAULT 100.0,
                    captured_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Special Consent ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS special_consents (
                    id TEXT PRIMARY KEY,
                    consent_id TEXT NOT NULL,
                    consent_type TEXT NOT NULL,
                    minor_dob TEXT,
                    minor_age INTEGER,
                    guardian_id TEXT,
                    guardian_verified INTEGER DEFAULT 0,
                    sensitivity_level TEXT,
                    emergency_justification TEXT,
                    emergency_approved_by TEXT,
                    research_anonymisation_level TEXT,
                    research_ethics_approval TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Consent Propagation Events ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_propagation_log (
                    id TEXT PRIMARY KEY,
                    consent_id TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    source_system TEXT DEFAULT 'QS-DPDP',
                    target_systems TEXT,
                    targets_notified INTEGER DEFAULT 0,
                    targets_acknowledged INTEGER DEFAULT 0,
                    propagation_status TEXT DEFAULT 'PENDING',
                    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP
                )
            """);

            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cc_principal ON consent_contracts(data_principal_token)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cc_status ON consent_contracts(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cc_purpose ON consent_contracts(purpose_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cc_bundle ON consent_contracts(bundle_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ci_type ON consent_intelligence(intelligence_type)");

            seedPurposeRegistry(stmt);
            seedConsentBundles(stmt);
            initialized = true;
            logger.info("ConsentOrchestrationService initialized — Global-Grade Consent Platform active");

        } catch (SQLException e) {
            logger.error("Failed to initialize ConsentOrchestrationService", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 1. CONSENT CONTRACTUAL OBJECT — CREATE
    // ═══════════════════════════════════════════════════════════

    /**
     * Create a legally enforceable consent contract.
     * Generates cryptographic hash: consent becomes a tamper-evident digital contract.
     */
    public ConsentContract createConsent(ConsentContractRequest request) {
        if (!initialized) initialize();

        ConsentContract contract = new ConsentContract();
        contract.consentId = "CON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        contract.version = 1;
        contract.principalToken = request.principalToken;
        contract.fiduciaryId = request.fiduciaryId != null ? request.fiduciaryId : "QS-DPDP-ENTERPRISE";
        contract.purposeId = request.purposeId;
        contract.purposeDescription = request.purposeDescription;
        contract.dataCategories = request.dataCategories;
        contract.processingActions = request.processingActions != null ? request.processingActions : "read";
        contract.legalBasis = request.legalBasis != null ? request.legalBasis : "CONSENT";
        contract.jurisdictionTag = request.jurisdictionTag != null ? request.jurisdictionTag : "IN";
        contract.retentionPolicyId = request.retentionPolicyId;
        contract.startDate = LocalDateTime.now();
        contract.expiryDate = request.expiryDate != null ? request.expiryDate : LocalDateTime.now().plusYears(1);
        contract.status = "ACTIVE";
        contract.captureChannel = request.captureChannel != null ? request.captureChannel : "web";
        contract.languageCode = request.languageCode != null ? request.languageCode : "en";
        contract.consentType = request.consentType != null ? request.consentType : "STANDARD";
        contract.bundleId = request.bundleId;

        // Generate cryptographic hash (tamper evidence)
        String hashInput = contract.consentId + "|" + contract.version + "|" + contract.principalToken
                + "|" + contract.purposeId + "|" + contract.dataCategories + "|" + contract.startDate;
        contract.cryptographicHash = sha256(hashInput);
        contract.previousHash = "GENESIS";

        // UX snapshot hash (proof of what user saw)
        if (request.uxSnapshotData != null) {
            contract.uxSnapshotHash = sha256(request.uxSnapshotData);
            storeUXProof(contract.consentId, contract.uxSnapshotHash, request.uxElements);
        }

        // Persist
        persistContract(contract);

        // Log version creation
        logVersionChange(contract.consentId, 0, 1, "CREATED",
                "Initial consent contract created", request.actorId);

        // Register with EnforcementEngine
        if (enforcementEngine != null) {
            ConsentEnforcementEngine.ConsentGrant grant = new ConsentEnforcementEngine.ConsentGrant();
            grant.id = contract.consentId;
            grant.principalId = contract.principalToken;
            grant.purpose = contract.purposeId;
            grant.dataCategories = contract.dataCategories;
            grant.sector = request.sector;
            grant.legalBasis = contract.legalBasis;
            grant.grantedAt = contract.startDate;
            grant.expiresAt = contract.expiryDate;
            enforcementEngine.initialize();
            enforcementEngine.registerGrant(grant);
        }

        // Publish event for propagation
        publishConsentEvent(contract.consentId, "CONSENT_CREATED", contract.principalToken);

        // Generate receipt
        ConsentReceipt receipt = generateReceipt(contract);
        contract.receiptId = receipt.receiptId;

        // Audit
        auditService.log("CONSENT_CONTRACT_CREATED", "CONSENT_ORCHESTRATION", request.actorId,
                "Contract created: " + contract.consentId + " v1 purpose=" + contract.purposeId);

        logger.info("Consent contract created: {} v{} for principal {}", contract.consentId, contract.version, contract.principalToken);
        return contract;
    }

    // ═══════════════════════════════════════════════════════════
    // 2. CONSENT VERSIONING — UPDATE
    // ═══════════════════════════════════════════════════════════

    /**
     * Modify consent — creates a NEW immutable version. Old versions never change.
     */
    public ConsentContract updateConsent(String consentId, ConsentUpdateRequest update) {
        if (!initialized) initialize();

        ConsentContract current = getLatestVersion(consentId);
        if (current == null) throw new IllegalArgumentException("Consent not found: " + consentId);

        // Create new version (immutable — old version stays unchanged)
        ConsentContract newVersion = new ConsentContract();
        newVersion.consentId = consentId;
        newVersion.version = current.version + 1;
        newVersion.principalToken = current.principalToken;
        newVersion.fiduciaryId = current.fiduciaryId;
        newVersion.purposeId = update.purposeId != null ? update.purposeId : current.purposeId;
        newVersion.purposeDescription = update.purposeDescription != null ? update.purposeDescription : current.purposeDescription;
        newVersion.dataCategories = update.dataCategories != null ? update.dataCategories : current.dataCategories;
        newVersion.processingActions = update.processingActions != null ? update.processingActions : current.processingActions;
        newVersion.legalBasis = current.legalBasis;
        newVersion.jurisdictionTag = current.jurisdictionTag;
        newVersion.retentionPolicyId = update.retentionPolicyId != null ? update.retentionPolicyId : current.retentionPolicyId;
        newVersion.startDate = current.startDate;
        newVersion.expiryDate = update.expiryDate != null ? update.expiryDate : current.expiryDate;
        newVersion.status = update.status != null ? update.status : current.status;
        newVersion.captureChannel = current.captureChannel;
        newVersion.languageCode = current.languageCode;
        newVersion.consentType = current.consentType;
        newVersion.bundleId = current.bundleId;
        newVersion.changeReason = update.changeReason;
        newVersion.changedBy = update.actorId;

        // Hash chain: new version's previousHash = current version's hash
        newVersion.previousHash = current.cryptographicHash;
        String hashInput = newVersion.consentId + "|" + newVersion.version + "|" + newVersion.principalToken
                + "|" + newVersion.purposeId + "|" + newVersion.dataCategories + "|" + LocalDateTime.now();
        newVersion.cryptographicHash = sha256(hashInput);

        // Detect changed fields
        List<String> changedFields = new ArrayList<>();
        if (update.purposeId != null && !update.purposeId.equals(current.purposeId)) changedFields.add("purposeId");
        if (update.dataCategories != null && !update.dataCategories.equals(current.dataCategories)) changedFields.add("dataCategories");
        if (update.processingActions != null && !update.processingActions.equals(current.processingActions)) changedFields.add("processingActions");
        if (update.status != null && !update.status.equals(current.status)) changedFields.add("status");
        if (update.expiryDate != null && !update.expiryDate.equals(current.expiryDate)) changedFields.add("expiryDate");

        persistContract(newVersion);
        logVersionChange(consentId, current.version, newVersion.version, "UPDATED",
                update.changeReason, update.actorId);

        // Propagate change event
        publishConsentEvent(consentId, "CONSENT_UPDATED", current.principalToken);

        auditService.log("CONSENT_VERSION_CREATED", "CONSENT_ORCHESTRATION", update.actorId,
                "v" + current.version + " → v" + newVersion.version + ": changed=" + String.join(",", changedFields));

        return newVersion;
    }

    // ═══════════════════════════════════════════════════════════
    // 3. PURPOSE-BUNDLED CONSENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Create a purpose-bundled consent — multiple purposes in one bundle.
     * Mandatory purposes auto-accept; optional ones are individually selectable.
     */
    public List<ConsentContract> createBundledConsent(BundledConsentRequest request) {
        if (!initialized) initialize();
        List<ConsentContract> contracts = new ArrayList<>();

        // Lookup bundle
        String bundleId = request.bundleId;
        Map<String, Object> bundle = getBundleDetails(bundleId);
        if (bundle == null) throw new IllegalArgumentException("Bundle not found: " + bundleId);

        String mandatoryPurposes = (String) bundle.get("mandatory_purposes");
        String optionalPurposes = (String) bundle.get("optional_purposes");

        // Create mandatory consents
        if (mandatoryPurposes != null) {
            for (String purposeId : mandatoryPurposes.split(",")) {
                purposeId = purposeId.trim();
                ConsentContractRequest req = new ConsentContractRequest();
                req.principalToken = request.principalToken;
                req.purposeId = purposeId;
                req.dataCategories = getPurposeCategories(purposeId);
                req.captureChannel = request.captureChannel;
                req.languageCode = request.languageCode;
                req.bundleId = bundleId;
                req.actorId = request.actorId;
                req.sector = request.sector;
                contracts.add(createConsent(req));
            }
        }

        // Create optional consents (only for selected ones)
        if (optionalPurposes != null && request.selectedOptionalPurposes != null) {
            for (String purposeId : request.selectedOptionalPurposes) {
                ConsentContractRequest req = new ConsentContractRequest();
                req.principalToken = request.principalToken;
                req.purposeId = purposeId;
                req.dataCategories = getPurposeCategories(purposeId);
                req.captureChannel = request.captureChannel;
                req.languageCode = request.languageCode;
                req.bundleId = bundleId;
                req.actorId = request.actorId;
                req.sector = request.sector;
                contracts.add(createConsent(req));
            }
        }

        return contracts;
    }

    // ═══════════════════════════════════════════════════════════
    // 4. CONSENT INTELLIGENCE LAYER
    // ═══════════════════════════════════════════════════════════

    /**
     * AI-powered consent intelligence — detects dormant, over-broad, and unused consents.
     */
    public List<IntelligenceFinding> analyzeConsentIntelligence() {
        if (!initialized) initialize();
        List<IntelligenceFinding> findings = new ArrayList<>();

        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            // DORMANT: Active consents with no enforcement checks in 90+ days
            ResultSet rs = stmt.executeQuery("""
                SELECT cc.consent_id, cc.purpose_id, cc.data_principal_token, cc.start_date
                FROM consent_contracts cc
                WHERE cc.status = 'ACTIVE'
                AND cc.version = (SELECT MAX(version) FROM consent_contracts WHERE consent_id = cc.consent_id)
                AND cc.consent_id NOT IN (
                    SELECT DISTINCT consent_id FROM consent_enforcement_log
                    WHERE evaluated_at > datetime('now', '-90 days') AND consent_id IS NOT NULL
                )
            """);
            while (rs.next()) {
                IntelligenceFinding f = new IntelligenceFinding();
                f.consentId = rs.getString("consent_id");
                f.type = "DORMANT_CONSENT";
                f.severity = "WARNING";
                f.finding = "Consent for purpose '" + rs.getString("purpose_id")
                        + "' has not been used in 90+ days — may indicate unnecessary data retention";
                f.recommendation = "Review necessity. Consider expiring or notifying principal for re-confirmation";
                findings.add(f);
                persistIntelligence(conn, f);
            }

            // OVER-BROAD: Consent with 5+ data categories
            rs = stmt.executeQuery("""
                SELECT consent_id, purpose_id, data_categories, data_principal_token
                FROM consent_contracts
                WHERE status = 'ACTIVE'
                AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                AND LENGTH(data_categories) - LENGTH(REPLACE(data_categories, ',', '')) + 1 >= 5
            """);
            while (rs.next()) {
                IntelligenceFinding f = new IntelligenceFinding();
                f.consentId = rs.getString("consent_id");
                f.type = "OVER_BROAD_CONSENT";
                f.severity = "WARNING";
                f.finding = "Consent covers 5+ data categories: [" + rs.getString("data_categories")
                        + "] — violates data minimisation principle (DPDP §4)";
                f.recommendation = "Split into purpose-specific consents with minimal data categories";
                findings.add(f);
                persistIntelligence(conn, f);
            }

            // EXPIRING SOON: Active consents expiring in 30 days
            rs = stmt.executeQuery("""
                SELECT consent_id, purpose_id, expiry_date, data_principal_token
                FROM consent_contracts
                WHERE status = 'ACTIVE' AND expiry_date IS NOT NULL
                AND expiry_date BETWEEN datetime('now') AND datetime('now', '+30 days')
                AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
            """);
            while (rs.next()) {
                IntelligenceFinding f = new IntelligenceFinding();
                f.consentId = rs.getString("consent_id");
                f.type = "EXPIRING_SOON";
                f.severity = "INFO";
                f.finding = "Consent for purpose '" + rs.getString("purpose_id")
                        + "' expiring at " + rs.getString("expiry_date");
                f.recommendation = "Initiate renewal campaign for principal. Send reminder via preferred channel";
                findings.add(f);
                persistIntelligence(conn, f);
            }

            // UNUSED PURPOSE: Purposes with consents but zero data access
            rs = stmt.executeQuery("""
                SELECT cc.purpose_id, COUNT(*) as consent_count
                FROM consent_contracts cc
                WHERE cc.status = 'ACTIVE'
                AND cc.version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = cc.consent_id)
                AND cc.purpose_id NOT IN (
                    SELECT DISTINCT purpose FROM consent_enforcement_log WHERE purpose IS NOT NULL
                )
                GROUP BY cc.purpose_id
            """);
            while (rs.next()) {
                IntelligenceFinding f = new IntelligenceFinding();
                f.type = "UNUSED_PURPOSE";
                f.severity = "INFO";
                f.finding = "Purpose '" + rs.getString("purpose_id") + "' has "
                        + rs.getInt("consent_count") + " active consents but zero data access records";
                f.recommendation = "Verify if purpose is still needed. Consider purpose consolidation";
                findings.add(f);
                persistIntelligence(conn, f);
            }

            auditService.log("CONSENT_INTELLIGENCE_SCAN", "CONSENT_ORCHESTRATION", null,
                    "Intelligence scan complete: " + findings.size() + " findings");

        } catch (SQLException e) {
            logger.error("Failed to analyze consent intelligence", e);
        }
        return findings;
    }

    // ═══════════════════════════════════════════════════════════
    // 5. CONSENT RECEIPT GENERATION (GDPR-GRADE)
    // ═══════════════════════════════════════════════════════════

    /**
     * Generate a legally compliant consent receipt.
     */
    public ConsentReceipt generateReceipt(ConsentContract contract) {
        ConsentReceipt receipt = new ConsentReceipt();
        receipt.receiptId = "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        receipt.consentId = contract.consentId;
        receipt.version = contract.version;
        receipt.principalId = contract.principalToken;
        receipt.generatedAt = LocalDateTime.now();

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("receiptId", receipt.receiptId);
        content.put("consentId", contract.consentId);
        content.put("version", contract.version);
        content.put("organisation", Map.of(
                "name", "QS-DPDP Enterprise",
                "registrationId", contract.fiduciaryId,
                "dpo", "Data Protection Officer",
                "contact", "dpo@enterprise.in"
        ));
        content.put("purpose", Map.of(
                "id", contract.purposeId,
                "description", contract.purposeDescription != null ? contract.purposeDescription : contract.purposeId,
                "legalBasis", contract.legalBasis
        ));
        content.put("dataCategories", contract.dataCategories);
        content.put("processingActions", contract.processingActions);
        content.put("jurisdiction", contract.jurisdictionTag);
        content.put("validity", Map.of(
                "startDate", contract.startDate.toString(),
                "expiryDate", contract.expiryDate != null ? contract.expiryDate.toString() : "No expiry"
        ));
        content.put("rights", Map.of(
                "access", "You have the right to access your personal data — DPDP §11",
                "correction", "You have the right to correct your personal data — DPDP §12",
                "erasure", "You have the right to erase your personal data — DPDP §12",
                "withdrawal", "You can withdraw consent at any time via the QS-DPDP portal — DPDP §6(6)",
                "grievance", "File a grievance with DPBI — DPDP §13"
        ));
        content.put("withdrawalMethod", "Login → My Consents → Select consent → Click 'Withdraw'");
        content.put("timestamp", receipt.generatedAt.toString());
        content.put("cryptographicHash", contract.cryptographicHash);

        receipt.content = content.toString();
        receipt.format = "JSON";

        // Persist receipt
        String sql = "INSERT INTO consent_receipts (receipt_id, consent_id, version, principal_id, receipt_content, receipt_format) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receipt.receiptId);
            ps.setString(2, receipt.consentId);
            ps.setInt(3, receipt.version);
            ps.setString(4, receipt.principalId);
            ps.setString(5, receipt.content);
            ps.setString(6, receipt.format);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist receipt", e);
        }

        return receipt;
    }

    // ═══════════════════════════════════════════════════════════
    // 6. CONSENT WITHDRAWAL ENGINE (ENHANCED)
    // ═══════════════════════════════════════════════════════════

    /**
     * Withdraw consent — instant, granular (purpose-level).
     * Triggers enforcement block + downstream propagation.
     */
    public WithdrawalResult withdrawConsent(String consentId, String actorId, String reason) {
        if (!initialized) initialize();
        WithdrawalResult result = new WithdrawalResult();
        result.consentId = consentId;
        result.withdrawnAt = LocalDateTime.now();

        ConsentContract current = getLatestVersion(consentId);
        if (current == null) {
            result.success = false;
            result.error = "Consent not found";
            return result;
        }

        // Create withdrawal version
        ConsentUpdateRequest update = new ConsentUpdateRequest();
        update.status = "WITHDRAWN";
        update.changeReason = reason != null ? reason : "Data principal requested withdrawal";
        update.actorId = actorId;
        ConsentContract withdrawn = updateConsent(consentId, update);

        // Revoke enforcement grant (immediate block)
        if (enforcementEngine != null) {
            enforcementEngine.initialize();
            enforcementEngine.revokeGrant(current.principalToken, current.purposeId);
        }

        // Propagate withdrawal to all subscribed systems
        int systemsNotified = propagateConsentChange(consentId, "CONSENT_WITHDRAWN", current.principalToken);

        result.success = true;
        result.newVersion = withdrawn.version;
        result.systemsNotified = systemsNotified;
        result.enforcementBlocked = true;

        auditService.log("CONSENT_WITHDRAWN", "CONSENT_ORCHESTRATION", actorId,
                "Consent " + consentId + " withdrawn → v" + withdrawn.version + " (" + systemsNotified + " systems notified)");

        return result;
    }

    /**
     * Bulk withdrawal — revoke all consents for a sector.
     */
    public List<WithdrawalResult> bulkWithdrawBySector(String principalToken, String sector, String actorId) {
        if (!initialized) initialize();
        List<WithdrawalResult> results = new ArrayList<>();
        String sql = """
            SELECT DISTINCT consent_id FROM consent_contracts 
            WHERE data_principal_token = ? AND status = 'ACTIVE'
            AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
            AND purpose_id IN (SELECT purpose_id FROM purpose_registry WHERE sector = ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, principalToken);
            ps.setString(2, sector);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(withdrawConsent(rs.getString("consent_id"), actorId, "Bulk sector withdrawal: " + sector));
            }
        } catch (SQLException e) {
            logger.error("Bulk withdrawal failed", e);
        }
        return results;
    }

    // ═══════════════════════════════════════════════════════════
    // 7. CONSENT PROPAGATION (EVENT-DRIVEN)
    // ═══════════════════════════════════════════════════════════

    private int propagateConsentChange(String consentId, String eventType, String principalToken) {
        // Notify internal systems via EventBus
        eventBus.publish(new ComplianceEvent(eventType,
                Map.of("consentId", consentId, "principalToken", principalToken, "timestamp", LocalDateTime.now().toString())));

        // Log propagation
        String[] targetSystems = {"ENFORCEMENT_ENGINE", "BREACH_ENGINE", "VENDOR_SYSTEM", "ANALYTICS_ENGINE", "DATA_REGISTRY"};
        int notified = targetSystems.length;
        Map<String, String> systemStatuses = new LinkedHashMap<>();

        // §18 — Module integration: propagate to vendor engine on withdrawal
        if ("CONSENT_WITHDRAWN".equals(eventType)) {
            try (Connection conn = dbManager.getConnection()) {
                // Deactivate vendor data sharing for the withdrawn purpose
                ConsentContract contract = getLatestVersion(consentId);
                if (contract != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE vendor_data_sharing SET active = 0, deactivated_at = ?, deactivation_reason = ? WHERE purpose_id = ? AND principal_id = ? AND active = 1")) {
                        ps.setString(1, LocalDateTime.now().toString());
                        ps.setString(2, "Consent withdrawn: " + consentId);
                        ps.setString(3, contract.purposeId);
                        ps.setString(4, principalToken);
                        int deactivated = ps.executeUpdate();
                        systemStatuses.put("VENDOR_SYSTEM", "PROPAGATED (" + deactivated + " shares deactivated)");
                    } catch (SQLException e) {
                        systemStatuses.put("VENDOR_SYSTEM", "SKIPPED (table not available)");
                    }

                    // Mark breach assessments for review
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE breach_assessments SET requires_review = 1, review_reason = ? WHERE affected_principal_id = ? AND status != 'CLOSED'")) {
                        ps.setString(1, "Consent withdrawn — reassess impact: " + consentId);
                        ps.setString(2, principalToken);
                        ps.executeUpdate();
                        systemStatuses.put("BREACH_ENGINE", "PROPAGATED");
                    } catch (SQLException e) {
                        systemStatuses.put("BREACH_ENGINE", "SKIPPED (table not available)");
                    }
                }
            } catch (SQLException e) {
                logger.debug("Vendor/breach propagation skipped", e);
            }
        }

        for (String sys : targetSystems) {
            systemStatuses.putIfAbsent(sys, "NOTIFIED");
        }

        String sql = "INSERT INTO consent_propagation_log (id, consent_id, event_type, target_systems, targets_notified, propagation_status) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, consentId);
            ps.setString(3, eventType);
            ps.setString(4, systemStatuses.toString());
            ps.setInt(5, notified);
            ps.setString(6, "COMPLETED");
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to log propagation", e);
        }

        return notified;
    }

    private void publishConsentEvent(String consentId, String eventType, String principalToken) {
        propagateConsentChange(consentId, eventType, principalToken);
    }

    // ═══════════════════════════════════════════════════════════
    // 8. SPECIAL CONSENT TYPES
    // ═══════════════════════════════════════════════════════════

    /**
     * Create minor consent — requires age verification + guardian consent (DPDP §9).
     */
    public ConsentContract createMinorConsent(ConsentContractRequest request, MinorConsentDetails minor) {
        request.consentType = "MINOR";
        ConsentContract contract = createConsent(request);

        String sql = "INSERT INTO special_consents (id, consent_id, consent_type, minor_dob, minor_age, guardian_id, guardian_verified) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, contract.consentId);
            ps.setString(3, "MINOR");
            ps.setString(4, minor.dateOfBirth);
            ps.setInt(5, minor.age);
            ps.setString(6, minor.guardianId);
            ps.setInt(7, minor.guardianVerified ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to record minor consent details", e);
        }

        auditService.log("MINOR_CONSENT_CREATED", "CONSENT_ORCHESTRATION", request.actorId,
                "Minor consent: guardian=" + minor.guardianId + " age=" + minor.age + " verified=" + minor.guardianVerified);
        return contract;
    }

    /**
     * Create emergency override consent — logged + justified (DPDP §7).
     */
    public ConsentContract createEmergencyConsent(ConsentContractRequest request, String justification, String approvedBy) {
        request.consentType = "EMERGENCY";
        ConsentContract contract = createConsent(request);

        String sql = "INSERT INTO special_consents (id, consent_id, consent_type, emergency_justification, emergency_approved_by) VALUES (?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, contract.consentId);
            ps.setString(3, "EMERGENCY");
            ps.setString(4, justification);
            ps.setString(5, approvedBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to record emergency consent", e);
        }

        auditService.log("EMERGENCY_CONSENT_CREATED", "CONSENT_ORCHESTRATION", approvedBy,
                "Emergency override: " + justification);
        return contract;
    }

    /**
     * Create research consent — requires anonymisation (DPDP §7).
     */
    public ConsentContract createResearchConsent(ConsentContractRequest request, String anonymisationLevel, String ethicsApproval) {
        request.consentType = "RESEARCH";
        ConsentContract contract = createConsent(request);

        String sql = "INSERT INTO special_consents (id, consent_id, consent_type, research_anonymisation_level, research_ethics_approval) VALUES (?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, contract.consentId);
            ps.setString(3, "RESEARCH");
            ps.setString(4, anonymisationLevel);
            ps.setString(5, ethicsApproval);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to record research consent", e);
        }

        return contract;
    }

    // ═══════════════════════════════════════════════════════════
    // 9. UX COMPLIANCE ENGINE
    // ═══════════════════════════════════════════════════════════

    private void storeUXProof(String consentId, String uxHash, String uiElements) {
        String sql = "INSERT INTO consent_ux_proofs (id, consent_id, ux_snapshot_hash, ui_elements) VALUES (?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, consentId);
            ps.setString(3, uxHash);
            ps.setString(4, uiElements);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to store UX proof", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 10. TRUST & TRANSPARENCY
    // ═══════════════════════════════════════════════════════════

    /**
     * Get trust transparency report for a consent — shows how data is actually being used.
     */
    public Map<String, Object> getConsentTransparency(String consentId) {
        Map<String, Object> report = new LinkedHashMap<>();
        ConsentContract contract = getLatestVersion(consentId);
        if (contract == null) return Map.of("error", "Consent not found");

        report.put("consentId", consentId);
        report.put("version", contract.version);
        report.put("purpose", contract.purposeId);
        report.put("status", contract.status);

        try (Connection conn = dbManager.getConnection()) {
            // Usage frequency
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM consent_enforcement_log WHERE consent_id = ?")) {
                ps.setString(1, consentId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) report.put("usageCount", rs.getInt(1));
            }

            // Compliance status
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM consent_enforcement_log WHERE consent_id = ? AND decision = 'DENIED'")) {
                ps.setString(1, consentId);
                ResultSet rs = ps.executeQuery();
                int denied = rs.next() ? rs.getInt(1) : 0;
                int total = (int) report.getOrDefault("usageCount", 0);
                if (total == 0) report.put("complianceStatus", "NOT_USED");
                else if (denied > total / 2) report.put("complianceStatus", "OVERUSED");
                else report.put("complianceStatus", "USED_AS_INTENDED");
            }

            // Purpose clarity score (1-100 based on description completeness)
            int clarityScore = 50;
            if (contract.purposeDescription != null) clarityScore += 20;
            if (contract.dataCategories != null && !contract.dataCategories.isEmpty()) clarityScore += 15;
            if (contract.processingActions != null) clarityScore += 15;
            report.put("purposeClarityScore", Math.min(clarityScore, 100));

        } catch (SQLException e) {
            logger.error("Failed to get consent transparency", e);
        }
        return report;
    }

    // ═══════════════════════════════════════════════════════════
    // 11. CONSENT MANAGER INTEROPERABILITY (§9)
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns the consent contract JSON schema — Kantara Initiative compatible.
     */
    public Map<String, Object> getConsentSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.put("title", "QS-DPDP Consent Contract");
        schema.put("version", "2.0.0");
        schema.put("standard", "Kantara Consent Receipt Specification v1.1");
        schema.put("jurisdiction", "DPDP Act 2023 (India)");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("consentId", Map.of("type", "string", "format", "uuid", "description", "Globally unique consent identifier"));
        properties.put("version", Map.of("type", "integer", "minimum", 1, "description", "Immutable version number"));
        properties.put("dataPrincipalToken", Map.of("type", "string", "description", "Pseudonymised data principal identifier"));
        properties.put("dataFiduciaryId", Map.of("type", "string", "description", "DPDP S.2(i) — Data Fiduciary registration ID"));
        properties.put("purposeId", Map.of("type", "string", "description", "Purpose registry code"));
        properties.put("dataCategories", Map.of("type", "string", "description", "Comma-separated: Identity,Contact,Financial,Health,Biometric"));
        properties.put("processingActions", Map.of("type", "string", "enum", List.of("read", "write", "share", "profile", "read,write", "read,write,share")));
        properties.put("legalBasis", Map.of("type", "string", "enum", List.of("CONSENT", "LEGAL_OBLIGATION", "VITAL_INTEREST", "CONTRACT", "LEGITIMATE_USE")));
        properties.put("jurisdictionTag", Map.of("type", "string", "enum", List.of("IN", "IN-CB", "GLOBAL")));
        properties.put("retentionPolicyId", Map.of("type", "string", "description", "Reference to retention schedule"));
        properties.put("startDate", Map.of("type", "string", "format", "date-time"));
        properties.put("expiryDate", Map.of("type", "string", "format", "date-time"));
        properties.put("status", Map.of("type", "string", "enum", List.of("ACTIVE", "SUSPENDED", "WITHDRAWN", "EXPIRED")));
        properties.put("captureChannel", Map.of("type", "string", "enum", List.of("web", "mobile", "api", "assisted", "desktop")));
        properties.put("uxSnapshotHash", Map.of("type", "string", "description", "SHA-256 hash of UX at consent capture"));
        properties.put("languageCode", Map.of("type", "string", "description", "ISO 639-1 language code"));
        properties.put("cryptographicHash", Map.of("type", "string", "description", "SHA-256 tamper-evidence hash"));
        properties.put("consentType", Map.of("type", "string", "enum", List.of("STANDARD", "MINOR", "SENSITIVE", "EMERGENCY", "RESEARCH")));

        schema.put("properties", properties);
        schema.put("required", List.of("consentId", "dataPrincipalToken", "dataFiduciaryId", "purposeId", "dataCategories", "legalBasis", "startDate", "status"));
        return schema;
    }

    /**
     * Export all consents for a principal in standard interoperable format.
     */
    public Map<String, Object> exportConsents(String principalToken) {
        if (!initialized) initialize();
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportFormat", "QS-DPDP Consent Export v2.0");
        export.put("standard", "Kantara Consent Receipt v1.1");
        export.put("exportedAt", LocalDateTime.now().toString());
        export.put("principalToken", principalToken);

        List<ConsentContract> consents = getPrincipalConsents(principalToken, null);
        List<Map<String, Object>> exportedConsents = new ArrayList<>();
        for (ConsentContract c : consents) {
            Map<String, Object> ec = new LinkedHashMap<>();
            ec.put("consentId", c.consentId); ec.put("version", c.version);
            ec.put("dataPrincipalToken", c.principalToken); ec.put("dataFiduciaryId", c.fiduciaryId);
            ec.put("purposeId", c.purposeId); ec.put("purposeDescription", c.purposeDescription);
            ec.put("dataCategories", c.dataCategories); ec.put("processingActions", c.processingActions);
            ec.put("legalBasis", c.legalBasis); ec.put("jurisdictionTag", c.jurisdictionTag);
            ec.put("retentionPolicyId", c.retentionPolicyId);
            ec.put("startDate", c.startDate != null ? c.startDate.toString() : null);
            ec.put("expiryDate", c.expiryDate != null ? c.expiryDate.toString() : null);
            ec.put("status", c.status); ec.put("captureChannel", c.captureChannel);
            ec.put("languageCode", c.languageCode); ec.put("consentType", c.consentType);
            ec.put("cryptographicHash", c.cryptographicHash);
            exportedConsents.add(ec);
        }
        export.put("consents", exportedConsents);
        export.put("totalConsents", exportedConsents.size());

        auditService.log("CONSENT_EXPORTED", "CONSENT_INTEROP", null,
                "Exported " + exportedConsents.size() + " consents for principal " + principalToken);
        return export;
    }

    /**
     * Import consent from external consent manager in standard format.
     */
    public ConsentContract importConsent(Map<String, Object> externalConsent) {
        if (!initialized) initialize();

        ConsentContractRequest req = new ConsentContractRequest();
        req.principalToken = (String) externalConsent.getOrDefault("dataPrincipalToken",
                externalConsent.get("principalToken"));
        req.fiduciaryId = (String) externalConsent.getOrDefault("dataFiduciaryId",
                externalConsent.get("fiduciaryId"));
        req.purposeId = (String) externalConsent.get("purposeId");
        req.purposeDescription = (String) externalConsent.get("purposeDescription");
        req.dataCategories = (String) externalConsent.get("dataCategories");
        req.processingActions = (String) externalConsent.get("processingActions");
        req.legalBasis = (String) externalConsent.get("legalBasis");
        req.jurisdictionTag = (String) externalConsent.getOrDefault("jurisdictionTag", "IN");
        req.captureChannel = "api_import";
        req.consentType = (String) externalConsent.getOrDefault("consentType", "STANDARD");
        req.actorId = "INTEROP_IMPORT";

        ConsentContract contract = createConsent(req);

        auditService.log("CONSENT_IMPORTED", "CONSENT_INTEROP", "INTEROP_IMPORT",
                "Imported consent from external system: " + contract.consentId);
        return contract;
    }

    // ═══════════════════════════════════════════════════════════
    // 12. UX COMPLIANCE ENGINE — ACTIVE VALIDATION (§10)
    // ═══════════════════════════════════════════════════════════

    /**
     * Validate UX compliance for consent capture — ensures no dark patterns.
     * Checks: pre-ticked boxes, deceptive wording, layered notices, plain language,
     * multi-language support, accessibility.
     */
    public UXComplianceResult validateUXCompliance(UXComplianceRequest request) {
        if (!initialized) initialize();
        UXComplianceResult result = new UXComplianceResult();
        result.evaluatedAt = LocalDateTime.now();
        int score = 100;
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 1. Pre-ticked box check
        if (request.hasPreTickedBoxes) {
            violations.add("PRE_TICKED_BOXES: Consent form has pre-ticked checkboxes — violates DPDP §6(1) and EU GDPR Art.7");
            score -= 25;
        }

        // 2. Dark pattern detection
        if (request.consentText != null) {
            String lower = request.consentText.toLowerCase();
            // Deceptive wording patterns
            if (lower.contains("by continuing you agree")) {
                violations.add("DARK_PATTERN: 'By continuing you agree' is a forced consent pattern");
                score -= 15;
            }
            if (lower.contains("we may share") && !lower.contains("you can opt out")) {
                warnings.add("SHARE_WITHOUT_OPTOUT: Mentions data sharing without clear opt-out mechanism");
                score -= 10;
            }
            if (lower.length() > 5000 && !request.hasLayeredNotice) {
                warnings.add("EXCESSIVE_LENGTH: Consent text exceeds 5000 chars without layered notice structure");
                score -= 10;
            }
        }

        // 3. Layered notice check
        if (!request.hasLayeredNotice) {
            warnings.add("NO_LAYERED_NOTICE: Consent should use layered notice approach (short summary + detailed)");
            score -= 10;
        }

        // 4. Plain language check
        if (request.readabilityGrade > 12) {
            warnings.add("READABILITY: Text readability grade " + request.readabilityGrade + " exceeds grade 12 — use simpler language");
            score -= 10;
        }

        // 5. Multi-language support
        if (request.languagesOffered < 2) {
            warnings.add("SINGLE_LANGUAGE: Only " + request.languagesOffered + " language offered — DPDP requires 22 scheduled Indian languages");
            score -= 5;
        } else if (request.languagesOffered < 5) {
            warnings.add("LIMITED_LANGUAGES: Only " + request.languagesOffered + " languages — recommend supporting all 22 scheduled languages");
            score -= 3;
        }

        // 6. Accessibility check
        if (!request.hasAudioOption) {
            warnings.add("NO_AUDIO: No audio consent option for visually impaired users");
            score -= 5;
        }
        if (!request.hasAssistedMode) {
            warnings.add("NO_ASSISTED_MODE: No assisted consent capture mode available");
            score -= 5;
        }

        // 7. Withdrawal method clarity
        if (!request.withdrawalMethodClear) {
            violations.add("WITHDRAWAL_NOT_CLEAR: Withdrawal method must be as easy as giving consent — DPDP §6(6)");
            score -= 15;
        }

        result.complianceScore = Math.max(score, 0);
        result.compliant = violations.isEmpty();
        result.violations = violations;
        result.warnings = warnings;
        result.recommendation = score >= 90 ? "EXCELLENT — fully compliant"
                : score >= 70 ? "GOOD — minor improvements recommended"
                : score >= 50 ? "NEEDS_IMPROVEMENT — address violations before deployment"
                : "FAILED — critical dark pattern violations detected";

        // Store UX proof if consent ID provided
        if (request.consentId != null) {
            String proofHash = sha256(request.toString() + result.complianceScore);
            storeUXProof(request.consentId, proofHash, String.join(",", violations));
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // 13. DATA PRINCIPAL EXPERIENCE — VERSION COMPARISON (§15)
    // ═══════════════════════════════════════════════════════════

    /**
     * Compare two versions of a consent — returns a structured diff.
     */
    public Map<String, Object> compareVersions(String consentId, int v1, int v2) {
        if (!initialized) initialize();
        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("consentId", consentId);
        diff.put("comparing", "v" + v1 + " → v" + v2);

        ConsentContract ver1 = getVersion(consentId, v1);
        ConsentContract ver2 = getVersion(consentId, v2);

        if (ver1 == null || ver2 == null) {
            diff.put("error", "Version not found");
            return diff;
        }

        List<Map<String, Object>> changes = new ArrayList<>();
        addChange(changes, "purposeId", ver1.purposeId, ver2.purposeId);
        addChange(changes, "purposeDescription", ver1.purposeDescription, ver2.purposeDescription);
        addChange(changes, "dataCategories", ver1.dataCategories, ver2.dataCategories);
        addChange(changes, "processingActions", ver1.processingActions, ver2.processingActions);
        addChange(changes, "status", ver1.status, ver2.status);
        addChange(changes, "legalBasis", ver1.legalBasis, ver2.legalBasis);
        addChange(changes, "jurisdictionTag", ver1.jurisdictionTag, ver2.jurisdictionTag);
        addChange(changes, "retentionPolicyId", ver1.retentionPolicyId, ver2.retentionPolicyId);
        addChange(changes, "expiryDate",
                ver1.expiryDate != null ? ver1.expiryDate.toString() : null,
                ver2.expiryDate != null ? ver2.expiryDate.toString() : null);
        addChange(changes, "languageCode", ver1.languageCode, ver2.languageCode);

        diff.put("changes", changes);
        diff.put("totalChanges", changes.size());
        diff.put("changeReason", ver2.changeReason);
        diff.put("changedBy", ver2.changedBy);
        return diff;
    }

    private void addChange(List<Map<String, Object>> changes, String field, String oldVal, String newVal) {
        if (oldVal == null && newVal == null) return;
        if (oldVal != null && oldVal.equals(newVal)) return;
        changes.add(Map.of("field", field, "before", oldVal != null ? oldVal : "(none)", "after", newVal != null ? newVal : "(none)"));
    }

    private ConsentContract getVersion(String consentId, int version) {
        String sql = "SELECT * FROM consent_contracts WHERE consent_id = ? AND version = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, consentId); ps.setInt(2, version);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapContract(rs);
        } catch (SQLException e) {
            logger.error("Failed to get version", e);
        }
        return null;
    }

    /**
     * Get all receipts for a data principal.
     */
    public List<Map<String, Object>> getReceiptsForPrincipal(String principalToken) {
        if (!initialized) initialize();
        List<Map<String, Object>> receipts = new ArrayList<>();
        String sql = "SELECT * FROM consent_receipts WHERE principal_id = ? ORDER BY generated_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, principalToken);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("receiptId", rs.getString("receipt_id"));
                r.put("consentId", rs.getString("consent_id"));
                r.put("version", rs.getInt("version"));
                r.put("format", rs.getString("receipt_format"));
                r.put("content", rs.getString("receipt_content"));
                r.put("generatedAt", rs.getString("generated_at"));
                receipts.add(r);
            }
        } catch (SQLException e) {
            logger.error("Failed to get receipts", e);
        }
        return receipts;
    }

    /**
     * Get principal consents with advanced filtering (§15).
     */
    public List<ConsentContract> getPrincipalConsentsFiltered(String principalToken,
            String sector, String purpose, String organisation) {
        if (!initialized) initialize();
        List<ConsentContract> consents = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT cc.* FROM consent_contracts cc
            WHERE cc.data_principal_token = ?
            AND cc.version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = cc.consent_id)
        """);
        List<String> params = new ArrayList<>();
        params.add(principalToken);

        if (sector != null && !sector.isEmpty()) {
            sql.append(" AND cc.purpose_id IN (SELECT purpose_id FROM purpose_registry WHERE sector = ?)");
            params.add(sector);
        }
        if (purpose != null && !purpose.isEmpty()) {
            sql.append(" AND cc.purpose_id = ?");
            params.add(purpose);
        }
        if (organisation != null && !organisation.isEmpty()) {
            sql.append(" AND cc.data_fiduciary_id = ?");
            params.add(organisation);
        }
        sql.append(" ORDER BY cc.start_date DESC");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setString(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) consents.add(mapContract(rs));
        } catch (SQLException e) {
            logger.error("Failed to get filtered consents", e);
        }
        return consents;
    }

    // ═══════════════════════════════════════════════════════════
    // 14. AI-DRIVEN CONSENT GOVERNANCE (§17)
    // ═══════════════════════════════════════════════════════════

    /**
     * AI-powered governance recommendations: expiry, consolidation, risk flags.
     */
    public Map<String, Object> getAIGovernanceRecommendations() {
        if (!initialized) initialize();
        Map<String, Object> governance = new LinkedHashMap<>();
        governance.put("generatedAt", LocalDateTime.now().toString());

        List<Map<String, Object>> recommendations = new ArrayList<>();

        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            // 1. Expiry recommendations — long-running consents (1+ year active)
            ResultSet rs = stmt.executeQuery("""
                SELECT consent_id, purpose_id, data_principal_token, start_date,
                       CAST((julianday('now') - julianday(start_date)) AS INTEGER) as days_active
                FROM consent_contracts
                WHERE status = 'ACTIVE'
                AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                AND start_date < datetime('now', '-365 days')
                ORDER BY days_active DESC
                LIMIT 20
            """);
            while (rs.next()) {
                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("type", "CONSENT_EXPIRY_RECOMMENDATION");
                rec.put("consentId", rs.getString("consent_id"));
                rec.put("purposeId", rs.getString("purpose_id"));
                rec.put("daysActive", rs.getInt("days_active"));
                rec.put("severity", rs.getInt("days_active") > 730 ? "HIGH" : "MEDIUM");
                rec.put("recommendation", "Consent active for " + rs.getInt("days_active")
                        + " days — consider re-consent campaign to refresh data principal's intent");
                recommendations.add(rec);
            }

            // 2. Purpose consolidation — principals with overlapping purposes
            rs = stmt.executeQuery("""
                SELECT data_principal_token, COUNT(DISTINCT purpose_id) as purpose_count,
                       GROUP_CONCAT(DISTINCT purpose_id) as purposes
                FROM consent_contracts
                WHERE status = 'ACTIVE'
                AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                GROUP BY data_principal_token
                HAVING purpose_count > 5
                ORDER BY purpose_count DESC
                LIMIT 10
            """);
            while (rs.next()) {
                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("type", "PURPOSE_CONSOLIDATION");
                rec.put("principalToken", rs.getString("data_principal_token"));
                rec.put("purposeCount", rs.getInt("purpose_count"));
                rec.put("purposes", rs.getString("purposes"));
                rec.put("severity", "MEDIUM");
                rec.put("recommendation", "Principal has " + rs.getInt("purpose_count")
                        + " active purposes — review for consolidation opportunities to reduce consent fatigue");
                recommendations.add(rec);
            }

            // 3. Risk flags — sensitive data without enhanced protection
            rs = stmt.executeQuery("""
                SELECT consent_id, purpose_id, data_categories, data_principal_token
                FROM consent_contracts
                WHERE status = 'ACTIVE'
                AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                AND (data_categories LIKE '%Health%' OR data_categories LIKE '%Biometric%' OR data_categories LIKE '%Financial%')
                AND consent_type = 'STANDARD'
            """);
            while (rs.next()) {
                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("type", "SENSITIVE_DATA_RISK_FLAG");
                rec.put("consentId", rs.getString("consent_id"));
                rec.put("dataCategories", rs.getString("data_categories"));
                rec.put("severity", "HIGH");
                rec.put("recommendation", "Consent covers sensitive data [" + rs.getString("data_categories")
                        + "] but is classified as STANDARD — should be upgraded to SENSITIVE consent type with enhanced safeguards");
                recommendations.add(rec);
            }

            // 4. Unused consent resources
            rs = stmt.executeQuery("""
                SELECT COUNT(*) as unused FROM consent_contracts
                WHERE status = 'ACTIVE'
                AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                AND consent_id NOT IN (SELECT DISTINCT consent_id FROM consent_enforcement_log WHERE consent_id IS NOT NULL)
            """);
            if (rs.next() && rs.getInt(1) > 0) {
                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("type", "UNUSED_CONSENTS");
                rec.put("count", rs.getInt(1));
                rec.put("severity", "INFO");
                rec.put("recommendation", rs.getInt(1) + " active consents have never been used for data processing — "
                        + "consider notifying principals about unused consents or archiving them");
                recommendations.add(rec);
            }

        } catch (SQLException e) {
            logger.error("Failed to generate governance recommendations", e);
        }

        governance.put("recommendations", recommendations);
        governance.put("totalRecommendations", recommendations.size());

        auditService.log("AI_GOVERNANCE_SCAN", "CONSENT_ORCHESTRATION", null,
                "Generated " + recommendations.size() + " governance recommendations");
        return governance;
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES & DASHBOARD
    // ═══════════════════════════════════════════════════════════

    public ConsentContract getLatestVersion(String consentId) {
        String sql = "SELECT * FROM consent_contracts WHERE consent_id = ? ORDER BY version DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, consentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapContract(rs);
        } catch (SQLException e) {
            logger.error("Failed to get latest version", e);
        }
        return null;
    }

    public List<ConsentContract> getVersionHistory(String consentId) {
        List<ConsentContract> versions = new ArrayList<>();
        String sql = "SELECT * FROM consent_contracts WHERE consent_id = ? ORDER BY version ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, consentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) versions.add(mapContract(rs));
        } catch (SQLException e) {
            logger.error("Failed to get version history", e);
        }
        return versions;
    }

    public List<ConsentContract> getPrincipalConsents(String principalToken, String statusFilter) {
        List<ConsentContract> consents = new ArrayList<>();
        String sql = statusFilter != null
                ? """
                    SELECT * FROM consent_contracts WHERE data_principal_token = ? AND status = ?
                    AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                    ORDER BY start_date DESC
                  """
                : """
                    SELECT * FROM consent_contracts WHERE data_principal_token = ?
                    AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                    ORDER BY start_date DESC
                  """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, principalToken);
            if (statusFilter != null) ps.setString(2, statusFilter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) consents.add(mapContract(rs));
        } catch (SQLException e) {
            logger.error("Failed to get principal consents", e);
        }
        return consents;
    }

    public Map<String, Object> getOrchestrationDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT consent_id) FROM consent_contracts");
            if (rs.next()) dashboard.put("totalConsentContracts", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(DISTINCT consent_id) FROM consent_contracts WHERE status = 'ACTIVE' AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)");
            if (rs.next()) dashboard.put("activeContracts", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(DISTINCT consent_id) FROM consent_contracts WHERE status = 'WITHDRAWN' AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)");
            if (rs.next()) dashboard.put("withdrawnContracts", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_receipts");
            if (rs.next()) dashboard.put("receiptsGenerated", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_intelligence WHERE resolved = 0");
            if (rs.next()) dashboard.put("pendingIntelligence", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_propagation_log");
            if (rs.next()) dashboard.put("propagationEvents", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM special_consents");
            if (rs.next()) dashboard.put("specialConsents", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_ux_proofs");
            if (rs.next()) dashboard.put("uxProofsStored", rs.getInt(1));

            rs = stmt.executeQuery("SELECT SUM(version) FROM consent_contracts");
            if (rs.next()) dashboard.put("totalVersions", rs.getInt(1));

            // Purpose distribution
            List<Map<String, Object>> byPurpose = new ArrayList<>();
            rs = stmt.executeQuery("""
                SELECT purpose_id, COUNT(DISTINCT consent_id) as cnt 
                FROM consent_contracts 
                WHERE status = 'ACTIVE' AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                GROUP BY purpose_id ORDER BY cnt DESC LIMIT 10
            """);
            while (rs.next()) {
                byPurpose.add(Map.of("purpose", rs.getString(1), "count", rs.getInt(2)));
            }
            dashboard.put("consentsByPurpose", byPurpose);

            // ── §12 ENHANCED ANALYTICS ──

            // Withdrawal trends (last 6 months)
            List<Map<String, Object>> withdrawalTrends = new ArrayList<>();
            rs = stmt.executeQuery("""
                SELECT strftime('%Y-%m', created_at) as month, COUNT(*) as cnt
                FROM consent_contracts
                WHERE status = 'WITHDRAWN'
                AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                AND created_at >= datetime('now', '-6 months')
                GROUP BY month ORDER BY month
            """);
            while (rs.next()) {
                withdrawalTrends.add(Map.of("month", rs.getString(1), "withdrawals", rs.getInt(2)));
            }
            dashboard.put("withdrawalTrends", withdrawalTrends);

            // Sector distribution
            List<Map<String, Object>> bySector = new ArrayList<>();
            rs = stmt.executeQuery("""
                SELECT pr.sector, COUNT(DISTINCT cc.consent_id) as cnt
                FROM consent_contracts cc
                JOIN purpose_registry pr ON cc.purpose_id = pr.purpose_id OR cc.purpose_id = pr.purpose_code
                WHERE cc.status = 'ACTIVE'
                AND cc.version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = cc.consent_id)
                GROUP BY pr.sector ORDER BY cnt DESC
            """);
            while (rs.next()) {
                bySector.add(Map.of("sector", rs.getString(1), "count", rs.getInt(2)));
            }
            dashboard.put("sectorDistribution", bySector);

            // Expiry patterns
            Map<String, Integer> expiryPatterns = new LinkedHashMap<>();
            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_contracts WHERE status = 'ACTIVE' AND expiry_date BETWEEN datetime('now') AND datetime('now', '+7 days') AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)");
            if (rs.next()) expiryPatterns.put("expiringIn7Days", rs.getInt(1));
            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_contracts WHERE status = 'ACTIVE' AND expiry_date BETWEEN datetime('now') AND datetime('now', '+30 days') AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)");
            if (rs.next()) expiryPatterns.put("expiringIn30Days", rs.getInt(1));
            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_contracts WHERE status = 'ACTIVE' AND expiry_date BETWEEN datetime('now') AND datetime('now', '+90 days') AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)");
            if (rs.next()) expiryPatterns.put("expiringIn90Days", rs.getInt(1));
            dashboard.put("expiryPatterns", expiryPatterns);

            // Channel distribution
            List<Map<String, Object>> byChannel = new ArrayList<>();
            rs = stmt.executeQuery("""
                SELECT capture_channel, COUNT(DISTINCT consent_id) as cnt
                FROM consent_contracts
                WHERE status = 'ACTIVE' AND version = (SELECT MAX(version) FROM consent_contracts cc2 WHERE cc2.consent_id = consent_contracts.consent_id)
                GROUP BY capture_channel ORDER BY cnt DESC
            """);
            while (rs.next()) {
                byChannel.add(Map.of("channel", rs.getString(1), "count", rs.getInt(2)));
            }
            dashboard.put("channelDistribution", byChannel);

            // Risk indicators from intelligence
            Map<String, Object> riskIndicators = new LinkedHashMap<>();
            rs = stmt.executeQuery("SELECT intelligence_type, COUNT(*) as cnt FROM consent_intelligence WHERE resolved = 0 GROUP BY intelligence_type");
            while (rs.next()) {
                riskIndicators.put(rs.getString(1), rs.getInt(2));
            }
            dashboard.put("riskIndicators", riskIndicators);

            dashboard.put("status", "OPERATIONAL");
            dashboard.put("platform", "Global-Grade Consent Orchestration Platform v2.0");
        } catch (SQLException e) {
            dashboard.put("status", "ERROR");
        }
        return dashboard;
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL — PERSISTENCE & HELPERS
    // ═══════════════════════════════════════════════════════════

    private void persistContract(ConsentContract c) {
        String sql = """
            INSERT INTO consent_contracts (consent_id, version, data_principal_token, data_fiduciary_id,
                purpose_id, purpose_description, data_categories, processing_actions, legal_basis,
                jurisdiction_tag, retention_policy_id, start_date, expiry_date, status, capture_channel,
                ux_snapshot_hash, language_code, cryptographic_hash, digital_signature, consent_type,
                bundle_id, change_reason, changed_by, previous_hash)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.consentId); ps.setInt(2, c.version); ps.setString(3, c.principalToken);
            ps.setString(4, c.fiduciaryId); ps.setString(5, c.purposeId); ps.setString(6, c.purposeDescription);
            ps.setString(7, c.dataCategories); ps.setString(8, c.processingActions); ps.setString(9, c.legalBasis);
            ps.setString(10, c.jurisdictionTag); ps.setString(11, c.retentionPolicyId);
            ps.setString(12, c.startDate.toString()); ps.setString(13, c.expiryDate != null ? c.expiryDate.toString() : null);
            ps.setString(14, c.status); ps.setString(15, c.captureChannel); ps.setString(16, c.uxSnapshotHash);
            ps.setString(17, c.languageCode); ps.setString(18, c.cryptographicHash); ps.setString(19, c.digitalSignature);
            ps.setString(20, c.consentType); ps.setString(21, c.bundleId); ps.setString(22, c.changeReason);
            ps.setString(23, c.changedBy); ps.setString(24, c.previousHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist consent contract", e);
        }
    }

    private void logVersionChange(String consentId, int fromVer, int toVer, String changeType, String description, String actorId) {
        String sql = "INSERT INTO consent_version_log (id, consent_id, from_version, to_version, change_type, change_description, changed_by) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, consentId);
            ps.setInt(3, fromVer); ps.setInt(4, toVer); ps.setString(5, changeType);
            ps.setString(6, description); ps.setString(7, actorId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to log version change", e);
        }
    }

    private void persistIntelligence(Connection conn, IntelligenceFinding f) {
        String sql = "INSERT INTO consent_intelligence (id, consent_id, intelligence_type, finding, severity, recommendation) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString()); ps.setString(2, f.consentId);
            ps.setString(3, f.type); ps.setString(4, f.finding);
            ps.setString(5, f.severity); ps.setString(6, f.recommendation);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to persist intelligence finding", e);
        }
    }

    private Map<String, Object> getBundleDetails(String bundleId) {
        String sql = "SELECT * FROM consent_bundles WHERE bundle_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bundleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> bundle = new HashMap<>();
                bundle.put("bundle_id", rs.getString("bundle_id"));
                bundle.put("bundle_name", rs.getString("bundle_name"));
                bundle.put("mandatory_purposes", rs.getString("mandatory_purposes"));
                bundle.put("optional_purposes", rs.getString("optional_purposes"));
                return bundle;
            }
        } catch (SQLException e) {
            logger.error("Failed to get bundle", e);
        }
        return null;
    }

    private String getPurposeCategories(String purposeId) {
        String sql = "SELECT data_categories FROM purpose_registry WHERE purpose_id = ? OR purpose_code = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, purposeId); ps.setString(2, purposeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("data_categories");
        } catch (SQLException e) {
            logger.debug("Purpose categories not found: {}", purposeId);
        }
        return "General";
    }

    private ConsentContract mapContract(ResultSet rs) throws SQLException {
        ConsentContract c = new ConsentContract();
        c.consentId = rs.getString("consent_id"); c.version = rs.getInt("version");
        c.principalToken = rs.getString("data_principal_token"); c.fiduciaryId = rs.getString("data_fiduciary_id");
        c.purposeId = rs.getString("purpose_id"); c.purposeDescription = rs.getString("purpose_description");
        c.dataCategories = rs.getString("data_categories"); c.processingActions = rs.getString("processing_actions");
        c.legalBasis = rs.getString("legal_basis"); c.jurisdictionTag = rs.getString("jurisdiction_tag");
        c.retentionPolicyId = rs.getString("retention_policy_id");
        String sd = rs.getString("start_date"); if (sd != null) c.startDate = LocalDateTime.parse(sd);
        String ed = rs.getString("expiry_date"); if (ed != null) c.expiryDate = LocalDateTime.parse(ed);
        c.status = rs.getString("status"); c.captureChannel = rs.getString("capture_channel");
        c.uxSnapshotHash = rs.getString("ux_snapshot_hash"); c.languageCode = rs.getString("language_code");
        c.cryptographicHash = rs.getString("cryptographic_hash"); c.consentType = rs.getString("consent_type");
        c.bundleId = rs.getString("bundle_id"); c.changeReason = rs.getString("change_reason");
        c.changedBy = rs.getString("changed_by"); c.previousHash = rs.getString("previous_hash");
        return c;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) { return UUID.randomUUID().toString(); }
    }

    private void seedPurposeRegistry(Statement stmt) throws SQLException {
        String sql = "INSERT OR IGNORE INTO purpose_registry (purpose_id, purpose_code, purpose_name, sector, is_mandatory, data_categories, legal_reference) VALUES (?,?,?,?,?,?,?)";
        String[][] purposes = {
            {"PUR-KYC", "KYC_VERIFICATION", "KYC Verification", "BFSI", "1", "Identity,Contact,Financial", "DPDP §7(c)"},
            {"PUR-ACCT", "ACCOUNT_MANAGEMENT", "Account Management", "BFSI", "1", "Identity,Contact,Transaction", "DPDP §7(a)"},
            {"PUR-MKT", "MARKETING", "Marketing & Promotions", "ALL", "0", "Contact,Preferences", "DPDP §6(1)"},
            {"PUR-TREAT", "TREATMENT", "Medical Treatment", "Healthcare", "1", "Identity,Health,Biometric", "DPDP §7(c)"},
            {"PUR-CLAIMS", "INSURANCE_CLAIMS", "Insurance Claims Processing", "Insurance", "1", "Identity,Health,Financial", "IRDAI"},
            {"PUR-ORDER", "ORDER_PROCESSING", "Order Processing", "E-Commerce", "1", "Identity,Contact,Transaction", "DPDP §7(a)"},
            {"PUR-RECOM", "RECOMMENDATION_ENGINE", "Personalised Recommendations", "E-Commerce", "0", "Preferences,Browsing", "DPDP §6(1)"},
            {"PUR-ENROL", "STUDENT_ENROLLMENT", "Student Enrollment", "Education", "1", "Identity,Academic,Contact", "NEP 2020"},
            {"PUR-SUB", "SUBSCRIBER_SERVICES", "Subscriber Services", "Telecom", "1", "Identity,Contact,Usage", "DoT"},
            {"PUR-TAX", "TAX_COMPLIANCE", "Tax Compliance", "BFSI", "1", "Identity,Financial,Transaction", "IT Act"},
            {"PUR-HR", "HR_MANAGEMENT", "HR Employee Data", "ALL", "1", "Identity,Contact,Employment", "DPDP §7(a)"},
            {"PUR-RES", "RESEARCH", "Research & Analytics", "ALL", "0", "Anonymised", "DPDP §7(d)"},
        };
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(sql)) {
            for (String[] p : purposes) {
                ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
                ps.setString(4, p[3]); ps.setInt(5, Integer.parseInt(p[4]));
                ps.setString(6, p[5]); ps.setString(7, p[6]);
                ps.executeUpdate();
            }
        }
    }

    private void seedConsentBundles(Statement stmt) throws SQLException {
        String sql = "INSERT OR IGNORE INTO consent_bundles (bundle_id, bundle_name, sector, purpose_ids, mandatory_purposes, optional_purposes) VALUES (?,?,?,?,?,?)";
        String[][] bundles = {
            {"BDL-BFSI", "BFSI Onboarding", "BFSI", "KYC_VERIFICATION,ACCOUNT_MANAGEMENT,MARKETING,TAX_COMPLIANCE", "KYC_VERIFICATION,ACCOUNT_MANAGEMENT,TAX_COMPLIANCE", "MARKETING"},
            {"BDL-HEALTH", "Healthcare Patient", "Healthcare", "TREATMENT,INSURANCE_CLAIMS,RESEARCH", "TREATMENT", "INSURANCE_CLAIMS,RESEARCH"},
            {"BDL-ECOM", "E-Commerce Customer", "E-Commerce", "ORDER_PROCESSING,RECOMMENDATION_ENGINE,MARKETING", "ORDER_PROCESSING", "RECOMMENDATION_ENGINE,MARKETING"},
            {"BDL-EDU", "Education Student", "Education", "STUDENT_ENROLLMENT,RESEARCH", "STUDENT_ENROLLMENT", "RESEARCH"},
            {"BDL-TELECOM", "Telecom Subscriber", "Telecom", "SUBSCRIBER_SERVICES,MARKETING", "SUBSCRIBER_SERVICES", "MARKETING"},
        };
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(sql)) {
            for (String[] b : bundles) {
                ps.setString(1, b[0]); ps.setString(2, b[1]); ps.setString(3, b[2]);
                ps.setString(4, b[3]); ps.setString(5, b[4]); ps.setString(6, b[5]);
                ps.executeUpdate();
            }
        }
    }

    public boolean isInitialized() { return initialized; }

    // ═══════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class ConsentContract {
        public String consentId, principalToken, fiduciaryId, purposeId, purposeDescription;
        public int version;
        public String dataCategories, processingActions, legalBasis, jurisdictionTag;
        public String retentionPolicyId, status, captureChannel, uxSnapshotHash;
        public String languageCode, cryptographicHash, digitalSignature, consentType;
        public String bundleId, parentConsentId, changeReason, changedBy, previousHash;
        public String receiptId;
        public LocalDateTime startDate, expiryDate;
    }

    public static class ConsentContractRequest {
        public String principalToken, fiduciaryId, purposeId, purposeDescription;
        public String dataCategories, processingActions, legalBasis, jurisdictionTag;
        public String retentionPolicyId, captureChannel, languageCode, consentType;
        public String bundleId, sector, actorId;
        public String uxSnapshotData, uxElements;
        public LocalDateTime expiryDate;
    }

    public static class ConsentUpdateRequest {
        public String purposeId, purposeDescription, dataCategories, processingActions;
        public String retentionPolicyId, status, changeReason, actorId;
        public LocalDateTime expiryDate;
    }

    public static class BundledConsentRequest {
        public String principalToken, bundleId, captureChannel, languageCode, sector, actorId;
        public List<String> selectedOptionalPurposes;
    }

    public static class MinorConsentDetails {
        public String dateOfBirth, guardianId;
        public int age;
        public boolean guardianVerified;
    }

    public static class ConsentReceipt {
        public String receiptId, consentId, principalId, content, format;
        public int version;
        public LocalDateTime generatedAt;
    }

    public static class WithdrawalResult {
        public String consentId, error;
        public boolean success, enforcementBlocked;
        public int newVersion, systemsNotified;
        public LocalDateTime withdrawnAt;
    }

    public static class IntelligenceFinding {
        public String consentId, type, finding, severity, recommendation;
    }

    public static class UXComplianceRequest {
        public String consentId;
        public String consentText;
        public boolean hasPreTickedBoxes;
        public boolean hasLayeredNotice;
        public int readabilityGrade;
        public int languagesOffered;
        public boolean hasAudioOption;
        public boolean hasAssistedMode;
        public boolean withdrawalMethodClear;
    }

    public static class UXComplianceResult {
        public int complianceScore;
        public boolean compliant;
        public List<String> violations;
        public List<String> warnings;
        public String recommendation;
        public LocalDateTime evaluatedAt;
    }
}
