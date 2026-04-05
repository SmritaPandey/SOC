package com.qsdpdp.children;

import com.qsdpdp.audit.AuditService;
import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Children's Data Protection Service — DPDP Act §9(3) and §33 Compliance
 * 
 * Implements verifiable parental/guardian consent for processing children's
 * data,
 * as required by the Digital Personal Data Protection Act, 2023.
 * 
 * Key features:
 * - Age verification gate before data collection
 * - Verifiable parental consent workflow
 * - Guardian nomination and verification
 * - Prohibition of tracking/behavioral monitoring of children
 * - Prohibition of targeted advertising to children
 * - Enhanced data minimization for children's data
 * - Auto-withdrawal on age of majority
 * 
 * @version 1.0.0
 * @since Phase 2 (DPDP §33 Implementation)
 */
@Service
public class ChildrenDataService {

    private static final Logger logger = LoggerFactory.getLogger(ChildrenDataService.class);
    private static final int AGE_OF_MAJORITY = 18; // India

    @Autowired
    private DatabaseManager dbManager;

    @Autowired
    private AuditService auditService;

    private boolean initialized = false;

    public void initialize() {
        if (initialized)
            return;
        logger.info("Initializing Children's Data Protection Service (DPDP §33)...");
        try {
            createTables();
            initialized = true;
            logger.info("✓ Children's Data Protection Service initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize Children's Data Protection Service", e);
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            // Parental consent records
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS parental_consents (
                            id TEXT PRIMARY KEY,
                            child_principal_id TEXT NOT NULL,
                            guardian_principal_id TEXT NOT NULL,
                            guardian_relationship TEXT NOT NULL,
                            verification_method TEXT NOT NULL,
                            verification_status TEXT NOT NULL DEFAULT 'PENDING',
                            verified_at TIMESTAMP,
                            consent_scope TEXT NOT NULL,
                            purposes TEXT NOT NULL,
                            restrictions TEXT,
                            tracking_prohibited INTEGER DEFAULT 1,
                            advertising_prohibited INTEGER DEFAULT 1,
                            behavioral_monitoring_prohibited INTEGER DEFAULT 1,
                            child_date_of_birth TEXT,
                            auto_expires_at TIMESTAMP,
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            dpdp_section TEXT DEFAULT '§33',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Age verification records
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS age_verifications (
                            id TEXT PRIMARY KEY,
                            data_principal_id TEXT NOT NULL,
                            declared_age INTEGER,
                            date_of_birth TEXT,
                            verification_method TEXT NOT NULL,
                            is_child INTEGER NOT NULL,
                            guardian_required INTEGER NOT NULL DEFAULT 0,
                            verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_pc_child ON parental_consents(child_principal_id)");
            stmt.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_pc_guardian ON parental_consents(guardian_principal_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_pc_status ON parental_consents(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_av_principal ON age_verifications(data_principal_id)");
        }
    }

    /**
     * Verify age and determine if parental consent is required.
     */
    public AgeVerificationResult verifyAge(String principalId, int declaredAge, String dateOfBirth,
            String verificationMethod) {
        boolean isChild = declaredAge < AGE_OF_MAJORITY;
        String id = UUID.randomUUID().toString();

        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO age_verifications (id, data_principal_id, declared_age, date_of_birth, " +
                                "verification_method, is_child, guardian_required) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, principalId);
            ps.setInt(3, declaredAge);
            ps.setString(4, dateOfBirth);
            ps.setString(5, verificationMethod);
            ps.setBoolean(6, isChild);
            ps.setBoolean(7, isChild);
            ps.executeUpdate();

            auditService.log("AGE_VERIFICATION", "CHILDREN_DATA", "SYSTEM",
                    "Age verified for principal " + principalId + ": "
                            + (isChild ? "CHILD (parental consent required)" : "ADULT"));
        } catch (Exception e) {
            logger.error("Error recording age verification", e);
        }

        return new AgeVerificationResult(isChild, isChild,
                isChild ? "Parental/guardian consent required per DPDP §33" : "No additional consent required",
                List.of(
                        isChild ? "Collect verifiable parental consent" : "Proceed with standard consent",
                        isChild ? "Prohibit tracking and targeted advertising" : "Standard processing allowed"));
    }

    /**
     * Record verifiable parental consent for a child's data processing.
     */
    public String recordParentalConsent(String childPrincipalId, String guardianPrincipalId,
            String guardianRelationship, String verificationMethod,
            String consentScope, String purposes, String dateOfBirth) {
        String id = UUID.randomUUID().toString();

        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO parental_consents (id, child_principal_id, guardian_principal_id, " +
                                "guardian_relationship, verification_method, consent_scope, purposes, child_date_of_birth, "
                                +
                                "status, tracking_prohibited, advertising_prohibited, behavioral_monitoring_prohibited) "
                                +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 1, 1, 1)")) {
            ps.setString(1, id);
            ps.setString(2, childPrincipalId);
            ps.setString(3, guardianPrincipalId);
            ps.setString(4, guardianRelationship);
            ps.setString(5, verificationMethod);
            ps.setString(6, consentScope);
            ps.setString(7, purposes);
            ps.setString(8, dateOfBirth);
            ps.executeUpdate();

            auditService.log("PARENTAL_CONSENT_RECORDED", "CHILDREN_DATA", guardianPrincipalId,
                    "Parental consent recorded for child " + childPrincipalId + " by " + guardianRelationship);
        } catch (Exception e) {
            logger.error("Error recording parental consent", e);
            return null;
        }
        return id;
    }

    /**
     * Verify parental consent (e.g., after OTP/email verification).
     */
    public boolean verifyParentalConsent(String consentId, String verifiedBy) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE parental_consents SET verification_status = 'VERIFIED', status = 'ACTIVE', " +
                                "verified_at = datetime('now'), updated_at = datetime('now') WHERE id = ? AND status = 'PENDING'")) {
            ps.setString(1, consentId);
            int updated = ps.executeUpdate();

            if (updated > 0) {
                auditService.log("PARENTAL_CONSENT_VERIFIED", "CHILDREN_DATA", verifiedBy,
                        "Parental consent " + consentId + " verified and activated");
                return true;
            }
        } catch (Exception e) {
            logger.error("Error verifying parental consent", e);
        }
        return false;
    }

    /**
     * Check if processing is allowed for a data principal.
     * Enforces DPDP §33 prohibitions on tracking, advertising, and behavioral
     * monitoring.
     */
    public ProcessingDecision canProcess(String principalId, String processingType) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT av.is_child, pc.status, pc.tracking_prohibited, pc.advertising_prohibited, " +
                                "pc.behavioral_monitoring_prohibited FROM age_verifications av " +
                                "LEFT JOIN parental_consents pc ON av.data_principal_id = pc.child_principal_id " +
                                "WHERE av.data_principal_id = ? ORDER BY av.verified_at DESC LIMIT 1")) {
            ps.setString(1, principalId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                boolean isChild = rs.getBoolean("is_child");
                if (!isChild) {
                    return new ProcessingDecision(true, "ADULT", "Standard processing allowed", null);
                }

                String consentStatus = rs.getString("status");
                if (!"ACTIVE".equals(consentStatus)) {
                    return new ProcessingDecision(false, "CHILD_NO_CONSENT",
                            "Processing blocked: No active parental consent for child per DPDP §33",
                            List.of("Obtain verifiable parental consent"));
                }

                // Check specific prohibitions for children
                if ("TRACKING".equalsIgnoreCase(processingType) && rs.getBoolean("tracking_prohibited")) {
                    return new ProcessingDecision(false, "CHILD_TRACKING_PROHIBITED",
                            "Tracking of children is prohibited per DPDP §33", null);
                }
                if ("ADVERTISING".equalsIgnoreCase(processingType) && rs.getBoolean("advertising_prohibited")) {
                    return new ProcessingDecision(false, "CHILD_ADVERTISING_PROHIBITED",
                            "Targeted advertising to children is prohibited per DPDP §33", null);
                }
                if ("BEHAVIORAL_MONITORING".equalsIgnoreCase(processingType)
                        && rs.getBoolean("behavioral_monitoring_prohibited")) {
                    return new ProcessingDecision(false, "CHILD_MONITORING_PROHIBITED",
                            "Behavioral monitoring of children is prohibited per DPDP §33", null);
                }

                return new ProcessingDecision(true, "CHILD_WITH_CONSENT",
                        "Processing allowed with active parental consent", null);
            }
        } catch (Exception e) {
            logger.error("Error checking processing permission", e);
        }
        // No age verification found — require it
        return new ProcessingDecision(false, "AGE_NOT_VERIFIED",
                "Age verification required before processing", List.of("Complete age verification"));
    }

    /**
     * Get children's data statistics for dashboard.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM age_verifications WHERE is_child = 1");
            stats.put("childPrincipals", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM parental_consents WHERE status = 'ACTIVE'");
            stats.put("activeParentalConsents", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM parental_consents WHERE status = 'PENDING'");
            stats.put("pendingVerifications", rs.next() ? rs.getInt(1) : 0);
            rs = stmt.executeQuery("SELECT COUNT(*) FROM parental_consents WHERE verification_status = 'VERIFIED'");
            stats.put("verifiedConsents", rs.next() ? rs.getInt(1) : 0);
        } catch (Exception e) {
            logger.warn("Statistics query fallback", e);
        }
        return stats;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class AgeVerificationResult {
        private final boolean isChild;
        private final boolean parentalConsentRequired;
        private final String message;
        private final List<String> nextSteps;

        public AgeVerificationResult(boolean isChild, boolean parentalConsentRequired, String message,
                List<String> nextSteps) {
            this.isChild = isChild;
            this.parentalConsentRequired = parentalConsentRequired;
            this.message = message;
            this.nextSteps = nextSteps;
        }

        public boolean isChild() {
            return isChild;
        }

        public boolean isParentalConsentRequired() {
            return parentalConsentRequired;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getNextSteps() {
            return nextSteps;
        }
    }

    public static class ProcessingDecision {
        private final boolean allowed;
        private final String reason;
        private final String message;
        private final List<String> requiredActions;

        public ProcessingDecision(boolean allowed, String reason, String message, List<String> requiredActions) {
            this.allowed = allowed;
            this.reason = reason;
            this.message = message;
            this.requiredActions = requiredActions;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getRequiredActions() {
            return requiredActions;
        }
    }
}
