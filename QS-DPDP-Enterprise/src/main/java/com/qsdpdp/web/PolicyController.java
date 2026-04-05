package com.qsdpdp.web;

import com.qsdpdp.policy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Policy Engine REST Controller
 * Full ISO policy lifecycle: DRAFT → PENDING_APPROVAL → ACTIVE → SUPERSEDED/ARCHIVED
 *
 * @version 1.0.0
 * @since Sprint 2
 */

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyController.class);

    @Autowired
    private PolicyService policyService;

    // ═══════════════════════════════════════════════════════════
    // CRUD
    // ═══════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<?> listPolicies(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {

        List<Policy> policies;
        if (status != null && !status.isBlank()) {
            policies = policyService.getPoliciesByStatus(status.toUpperCase());
        } else if (category != null && !category.isBlank()) {
            policies = policyService.getPoliciesByCategory(category);
        } else {
            policies = policyService.getAllPolicies(offset, limit);
        }

        return ResponseEntity.ok(Map.of(
                "policies", policies.stream().map(this::policyToMap).collect(Collectors.toList()),
                "total", policyService.getTotalCount(),
                "offset", offset,
                "limit", limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPolicy(@PathVariable String id,
            @RequestParam(defaultValue = "false") boolean includeHistory) {
        Policy policy = policyService.getPolicyById(id);
        if (policy == null) return ResponseEntity.notFound().build();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policy", policyToMap(policy));
        if (includeHistory) {
            result.put("versionHistory", policyService.getVersionHistory(id)
                    .stream().map(this::policyToMap).collect(Collectors.toList()));
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> createPolicy(@RequestBody Map<String, String> payload) {
        try {
            PolicyRequest request = new PolicyRequest();
            request.setCode(payload.getOrDefault("code", "POL-" + System.currentTimeMillis()));
            request.setName(payload.getOrDefault("name", ""));
            request.setDescription(payload.getOrDefault("description", ""));
            request.setCategory(payload.getOrDefault("category", "GENERAL"));
            request.setContent(payload.getOrDefault("content", ""));
            request.setOwner(payload.getOrDefault("owner", "admin"));

            Policy policy = policyService.createPolicy(request);
            return ResponseEntity.ok(Map.of("status", "created", "policy", policyToMap(policy)));
        } catch (Exception e) {
            logger.error("Failed to create policy", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create policy: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePolicy(@PathVariable String id, @RequestBody Map<String, String> payload) {
        try {
            String actor = payload.getOrDefault("actor", "admin");
            Policy policy = policyService.updateDraft(id, payload, actor);
            return ResponseEntity.ok(Map.of("status", "updated", "policy", policyToMap(policy)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update policy", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update policy: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LIFECYCLE TRANSITIONS
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitForReview(@PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String actor = payload != null ? payload.getOrDefault("actor", "admin") : "admin";
            Policy policy = policyService.submitForReview(id, actor);
            return ResponseEntity.ok(Map.of(
                    "status", "submitted",
                    "message", "Policy '" + policy.getName() + "' submitted for review",
                    "policy", policyToMap(policy)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approvePolicy(@PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String actor = payload != null ? payload.getOrDefault("actor", "admin") : "admin";
            Policy policy = policyService.approvePolicy(id, actor);
            return ResponseEntity.ok(Map.of(
                    "status", "approved",
                    "message", "Policy '" + policy.getName() + "' is now ACTIVE",
                    "policy", policyToMap(policy)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectPolicy(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String actor = payload.getOrDefault("actor", "admin");
            String reason = payload.getOrDefault("reason", "No reason provided");
            Policy policy = policyService.rejectPolicy(id, actor, reason);
            return ResponseEntity.ok(Map.of(
                    "status", "rejected",
                    "message", "Policy '" + policy.getName() + "' rejected: " + reason,
                    "policy", policyToMap(policy)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<?> archivePolicy(@PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String actor = payload != null ? payload.getOrDefault("actor", "admin") : "admin";
            Policy policy = policyService.archivePolicy(id, actor);
            return ResponseEntity.ok(Map.of(
                    "status", "archived",
                    "message", "Policy '" + policy.getName() + "' archived",
                    "policy", policyToMap(policy)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/revise")
    public ResponseEntity<?> revisePolicy(@PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String actor = payload != null ? payload.getOrDefault("actor", "admin") : "admin";
            Policy newVersion = policyService.revisePolicy(id, actor);
            return ResponseEntity.ok(Map.of(
                    "status", "revised",
                    "message", "New version v" + newVersion.getVersion() + " created as DRAFT",
                    "newPolicy", policyToMap(newVersion),
                    "supersededPolicyId", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS & QUERIES
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        PolicyStatistics stats = policyService.getStatistics();
        return ResponseEntity.ok(Map.of("statistics", Map.of(
                "totalPolicies", stats.getTotalPolicies(),
                "activePolicies", stats.getActivePolicies(),
                "pendingPolicies", stats.getPendingPolicies(),
                "overdueReviews", stats.getOverdueReviews())));
    }

    @GetMapping("/review-due")
    public ResponseEntity<?> getPoliciesRequiringReview() {
        List<Policy> policies = policyService.getPoliciesRequiringReview();
        return ResponseEntity.ok(Map.of(
                "reviewDue", policies.stream().map(this::policyToMap).collect(Collectors.toList()),
                "count", policies.size()));
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> policyToMap(Policy p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("code", p.getCode());
        map.put("name", p.getName());
        map.put("description", p.getDescription());
        map.put("category", p.getCategory());
        map.put("version", p.getVersion());
        map.put("versionNumber", p.getVersionNumber());
        map.put("status", p.getStatus().name());
        map.put("statusDescription", p.getStatus().getDescription());
        map.put("owner", p.getOwner());
        map.put("approver", p.getApprover());
        map.put("reviewedBy", p.getReviewedBy());
        map.put("rejectionReason", p.getRejectionReason());
        map.put("parentVersionId", p.getParentVersionId());
        map.put("effectiveDate", p.getEffectiveDate() != null ? p.getEffectiveDate().toString() : null);
        map.put("expiryDate", p.getExpiryDate() != null ? p.getExpiryDate().toString() : null);
        map.put("nextReviewDate", p.getNextReviewDate() != null ? p.getNextReviewDate().toString() : null);
        map.put("approvedAt", p.getApprovedAt() != null ? p.getApprovedAt().toString() : null);
        map.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        map.put("isActive", p.isActive());
        map.put("requiresReview", p.requiresReview());
        return map;
    }
}
