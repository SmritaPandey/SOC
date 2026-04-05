package com.qsdpdp.policy;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Policy Management Service
 */
@Service
public class PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;

    private boolean initialized = false;

    @Autowired
    public PolicyService(DatabaseManager dbManager, AuditService auditService, EventBus eventBus) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (initialized)
            return;
        logger.info("Initializing Policy Service...");
        eventBus.subscribe("policy.*", e -> logger.debug("Policy event: {}", e.getType()));
        initialized = true;
        logger.info("Policy Service initialized");
    }

    public Policy createPolicy(PolicyRequest request) {
        logger.info("Creating policy: {}", request.getName());

        Policy policy = Policy.builder()
                .id(UUID.randomUUID().toString())
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .content(request.getContent())
                .owner(request.getOwner())
                .build();

        savePolicy(policy);

        auditService.log("POLICY_CREATED", "POLICY", request.getOwner(),
                String.format("Policy created: %s - %s", request.getCode(), request.getName()));

        eventBus.publish(new ComplianceEvent("policy.created",
                Map.of("policyId", policy.getId(), "name", policy.getName())));

        return policy;
    }

    private void savePolicy(Policy policy) {
        String sql = """
                    INSERT INTO policies (id, code, title, description, category, content, version, status, owner)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, policy.getId());
            stmt.setString(2, policy.getCode());
            stmt.setString(3, policy.getName()); // maps to 'title' column
            stmt.setString(4, policy.getDescription());
            stmt.setString(5, policy.getCategory());
            stmt.setString(6, policy.getContent());
            stmt.setString(7, policy.getVersion());
            stmt.setString(8, policy.getStatus().name());
            stmt.setString(9, policy.getOwner());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save policy", e);
        }
    }

    public Policy approvePolicy(String policyId, String actorId) {
        Policy policy = getPolicyById(policyId);
        if (policy == null)
            throw new IllegalArgumentException("Policy not found");

        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setApprover(actorId);
        policy.setApprovedAt(LocalDateTime.now());
        policy.setEffectiveDate(LocalDateTime.now());
        policy.setNextReviewDate(LocalDateTime.now().plusYears(1));
        policy.setUpdatedAt(LocalDateTime.now());

        updatePolicy(policy);

        auditService.log("POLICY_APPROVED", "POLICY", actorId,
                "Policy approved: " + policy.getCode());

        eventBus.publish(new ComplianceEvent("policy.approved",
                Map.of("policyId", policyId)));

        return policy;
    }

    public Policy archivePolicy(String policyId, String actorId) {
        Policy policy = getPolicyById(policyId);
        if (policy == null)
            throw new IllegalArgumentException("Policy not found");

        policy.setStatus(PolicyStatus.ARCHIVED);
        policy.setExpiryDate(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());

        updatePolicy(policy);

        auditService.log("POLICY_ARCHIVED", "POLICY", actorId,
                "Policy archived: " + policy.getCode());

        return policy;
    }

    // ═══════════════════════════════════════════════════════════
    // LIFECYCLE — Sprint 2 Additions
    // ═══════════════════════════════════════════════════════════

    public Policy submitForReview(String policyId, String actorId) {
        Policy policy = getPolicyById(policyId);
        if (policy == null) throw new IllegalArgumentException("Policy not found");
        if (policy.getStatus() != PolicyStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT policies can be submitted for review");

        policy.setStatus(PolicyStatus.PENDING_APPROVAL);
        policy.setUpdatedAt(LocalDateTime.now());
        updatePolicy(policy);

        auditService.log("POLICY_SUBMITTED", "POLICY", actorId,
                "Policy submitted for review: " + policy.getCode());
        eventBus.publish(new ComplianceEvent("policy.submitted",
                Map.of("policyId", policyId, "code", policy.getCode())));
        return policy;
    }

    public Policy rejectPolicy(String policyId, String actorId, String reason) {
        Policy policy = getPolicyById(policyId);
        if (policy == null) throw new IllegalArgumentException("Policy not found");
        if (policy.getStatus() != PolicyStatus.PENDING_APPROVAL)
            throw new IllegalStateException("Only PENDING_APPROVAL policies can be rejected");

        policy.setStatus(PolicyStatus.DRAFT);
        policy.setReviewedBy(actorId);
        policy.setRejectionReason(reason);
        policy.setUpdatedAt(LocalDateTime.now());
        updatePolicy(policy);

        auditService.log("POLICY_REJECTED", "POLICY", actorId,
                "Policy rejected: " + policy.getCode() + " — " + reason);
        eventBus.publish(new ComplianceEvent("policy.rejected",
                Map.of("policyId", policyId, "reason", reason)));
        return policy;
    }

    public Policy revisePolicy(String policyId, String actorId) {
        Policy existing = getPolicyById(policyId);
        if (existing == null) throw new IllegalArgumentException("Policy not found");
        if (existing.getStatus() != PolicyStatus.ACTIVE)
            throw new IllegalStateException("Only ACTIVE policies can be revised");

        // Supersede old version
        existing.setStatus(PolicyStatus.SUPERSEDED);
        existing.setUpdatedAt(LocalDateTime.now());
        updatePolicy(existing);

        // Create new draft version
        int newVersionNum = existing.getVersionNumber() + 1;
        Policy newVersion = Policy.builder()
                .id(UUID.randomUUID().toString())
                .code(existing.getCode())
                .name(existing.getName())
                .description(existing.getDescription())
                .category(existing.getCategory())
                .content(existing.getContent())
                .owner(actorId)
                .parentVersionId(existing.getId())
                .versionNumber(newVersionNum)
                .build();
        newVersion.setVersion(newVersionNum + ".0");
        savePolicy(newVersion);

        auditService.log("POLICY_REVISED", "POLICY", actorId,
                "Policy revised: " + existing.getCode() + " → v" + newVersionNum);
        eventBus.publish(new ComplianceEvent("policy.revised",
                Map.of("oldPolicyId", policyId, "newPolicyId", newVersion.getId(),
                        "version", String.valueOf(newVersionNum))));
        return newVersion;
    }

    public Policy updateDraft(String policyId, Map<String, String> updates, String actorId) {
        Policy policy = getPolicyById(policyId);
        if (policy == null) throw new IllegalArgumentException("Policy not found");
        if (policy.getStatus() != PolicyStatus.DRAFT)
            throw new IllegalStateException("Only DRAFT policies can be edited");

        if (updates.containsKey("name")) policy.setName(updates.get("name"));
        if (updates.containsKey("description")) policy.setDescription(updates.get("description"));
        if (updates.containsKey("content")) policy.setContent(updates.get("content"));
        if (updates.containsKey("category")) policy.setCategory(updates.get("category"));
        policy.setUpdatedAt(LocalDateTime.now());

        String sql = """
                    UPDATE policies SET title = ?, description = ?, content = ?, category = ?, updated_at = ?
                    WHERE id = ?
                """;
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, policy.getName());
            stmt.setString(2, policy.getDescription());
            stmt.setString(3, policy.getContent());
            stmt.setString(4, policy.getCategory());
            stmt.setString(5, policy.getUpdatedAt().toString());
            stmt.setString(6, policyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update draft", e);
        }

        auditService.log("POLICY_DRAFT_UPDATED", "POLICY", actorId,
                "Draft updated: " + policy.getCode());
        return policy;
    }

    public List<Policy> getPoliciesByStatus(String status) {
        List<Policy> policies = new ArrayList<>();
        String sql = "SELECT * FROM policies WHERE status = ? ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) policies.add(mapPolicy(rs));
        } catch (SQLException e) {
            logger.error("Failed to get policies by status", e);
        }
        return policies;
    }

    public List<Policy> getPoliciesByCategory(String category) {
        List<Policy> policies = new ArrayList<>();
        String sql = "SELECT * FROM policies WHERE category = ? ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) policies.add(mapPolicy(rs));
        } catch (SQLException e) {
            logger.error("Failed to get policies by category", e);
        }
        return policies;
    }

    public List<Policy> getVersionHistory(String policyId) {
        // Walk the version chain
        List<Policy> history = new ArrayList<>();
        Policy current = getPolicyById(policyId);
        if (current == null) return history;

        // Go up to root
        String rootCode = current.getCode();
        String sql = "SELECT * FROM policies WHERE code = ? ORDER BY created_at ASC";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rootCode);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) history.add(mapPolicy(rs));
        } catch (SQLException e) {
            logger.error("Failed to get version history", e);
        }
        return history;
    }

    public int getTotalCount() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM policies");
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to count policies", e);
        }
        return 0;
    }

    private void updatePolicy(Policy policy) {
        String sql = """
                    UPDATE policies SET status = ?, approver = ?, approved_at = ?,
                        effective_date = ?, review_date = ?, updated_at = ?
                    WHERE id = ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, policy.getStatus().name());
            stmt.setString(2, policy.getApprover());
            stmt.setString(3, policy.getApprovedAt() != null ? policy.getApprovedAt().toString() : null);
            stmt.setString(4, policy.getEffectiveDate() != null ? policy.getEffectiveDate().toString() : null);
            stmt.setString(5, policy.getNextReviewDate() != null ? policy.getNextReviewDate().toString() : null);
            stmt.setString(6, policy.getUpdatedAt().toString());
            stmt.setString(7, policy.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update policy", e);
        }
    }

    public Policy getPolicyById(String id) {
        String sql = "SELECT * FROM policies WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapPolicy(rs);
        } catch (SQLException e) {
            logger.error("Failed to get policy", e);
        }
        return null;
    }

    public List<Policy> getActivePolicies() {
        List<Policy> policies = new ArrayList<>();
        String sql = "SELECT * FROM policies WHERE status = 'ACTIVE' ORDER BY title";

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next())
                policies.add(mapPolicy(rs));
        } catch (SQLException e) {
            logger.error("Failed to get active policies", e);
        }
        return policies;
    }

    public List<Policy> getAllPolicies(int offset, int limit) {
        List<Policy> policies = new ArrayList<>();
        String sql = "SELECT * FROM policies ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                policies.add(mapPolicy(rs));
        } catch (SQLException e) {
            logger.error("Failed to get policies", e);
        }
        return policies;
    }

    public List<Policy> getPoliciesRequiringReview() {
        List<Policy> policies = new ArrayList<>();
        String sql = "SELECT * FROM policies WHERE status = 'ACTIVE' AND review_date < datetime('now')";

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next())
                policies.add(mapPolicy(rs));
        } catch (SQLException e) {
            logger.error("Failed to get policies requiring review", e);
        }
        return policies;
    }

    public PolicyStatistics getStatistics() {
        PolicyStatistics stats = new PolicyStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM policies");
            if (rs.next())
                stats.setTotalPolicies(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM policies WHERE status = 'ACTIVE'");
            if (rs.next())
                stats.setActivePolicies(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM policies WHERE status = 'DRAFT' OR status = 'PENDING_APPROVAL'");
            if (rs.next())
                stats.setPendingPolicies(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM policies WHERE status = 'ACTIVE' AND review_date < datetime('now')");
            if (rs.next())
                stats.setOverdueReviews(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get policy statistics", e);
        }

        return stats;
    }

    private Policy mapPolicy(ResultSet rs) throws SQLException {
        Policy policy = new Policy();
        policy.setId(rs.getString("id"));
        policy.setCode(rs.getString("code"));
        policy.setName(rs.getString("title")); // title in DB maps to name in entity
        policy.setDescription(rs.getString("description"));
        policy.setCategory(rs.getString("category"));
        policy.setContent(rs.getString("content"));
        policy.setVersion(rs.getString("version"));
        String statusStr = rs.getString("status");
        try {
            policy.setStatus(PolicyStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            policy.setStatus(PolicyStatus.ACTIVE); // default fallback for unknown statuses
        }
        policy.setOwner(rs.getString("owner"));
        policy.setApprover(rs.getString("approver"));

        String effectiveDate = rs.getString("effective_date");
        if (effectiveDate != null)
            policy.setEffectiveDate(LocalDateTime.parse(effectiveDate));

        String expiryDate = rs.getString("expiry_date");
        if (expiryDate != null)
            policy.setExpiryDate(LocalDateTime.parse(expiryDate));

        String nextReviewDate = rs.getString("review_date");
        if (nextReviewDate != null)
            policy.setNextReviewDate(LocalDateTime.parse(nextReviewDate));

        String approvedAt = rs.getString("approved_at");
        if (approvedAt != null)
            policy.setApprovedAt(LocalDateTime.parse(approvedAt));

        return policy;
    }

    public boolean isInitialized() {
        return initialized;
    }
}

