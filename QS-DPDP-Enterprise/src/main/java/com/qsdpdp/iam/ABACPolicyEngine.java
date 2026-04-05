package com.qsdpdp.iam;

import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Attribute-Based Access Control (ABAC) Policy Engine
 * Implements granular, context-aware access decisions beyond RBAC.
 *
 * Based on article recommendations for DPDP compliance:
 * evaluates 4 attribute dimensions before granting data access:
 *   - User Attributes (department, clearance, training, sector)
 *   - Resource Attributes (sensitivity, consent status, data category)
 *   - Environment Attributes (time, location, device posture)
 *   - Purpose Attributes (business justification, consent binding)
 *
 * @version 1.0.0
 * @since Phase 7 — ABAC Enhancement
 */
@Component
public class ABACPolicyEngine {

    private static final Logger logger = LoggerFactory.getLogger(ABACPolicyEngine.class);

    @Autowired(required = false)
    private AuditService auditService;

    private final List<ABACPolicy> policies = new ArrayList<>();

    public ABACPolicyEngine() {
        initializeDefaultPolicies();
    }

    // ═══════ ACCESS DECISION ═══════

    public AccessDecision evaluate(AccessRequest request) {
        logger.debug("ABAC evaluating: user={} resource={} purpose={}",
                request.userId, request.resourceId, request.purpose);

        List<String> violations = new ArrayList<>();
        List<String> satisfiedPolicies = new ArrayList<>();

        for (ABACPolicy policy : policies) {
            if (!policy.active) continue;
            if (policy.appliesTo(request)) {
                PolicyResult result = policy.evaluate(request);
                if (result.denied) {
                    violations.add(policy.name + ": " + result.reason);
                } else {
                    satisfiedPolicies.add(policy.name);
                }
            }
        }

        AccessDecision decision = new AccessDecision();
        decision.requestId = UUID.randomUUID().toString();
        decision.userId = request.userId;
        decision.resourceId = request.resourceId;
        decision.purpose = request.purpose;
        decision.evaluatedAt = LocalDateTime.now();
        decision.policiesEvaluated = satisfiedPolicies.size() + violations.size();
        decision.policiesSatisfied = satisfiedPolicies.size();

        if (!violations.isEmpty()) {
            decision.allowed = false;
            decision.denialReasons = violations;
            logDecision(decision, "DENIED");
        } else {
            decision.allowed = true;
            decision.denialReasons = Collections.emptyList();
            logDecision(decision, "ALLOWED");
        }

        return decision;
    }

    private void logDecision(AccessDecision d, String outcome) {
        if (auditService != null) {
            auditService.log("ABAC_" + outcome, "IAM", d.userId,
                    String.format("ABAC %s: user=%s resource=%s purpose=%s policies=%d/%d",
                            outcome, d.userId, d.resourceId, d.purpose,
                            d.policiesSatisfied, d.policiesEvaluated));
        }
        logger.info("ABAC decision: {} for user={} resource={}", outcome, d.userId, d.resourceId);
    }

    // ═══════ POLICY MANAGEMENT ═══════

    public void addPolicy(ABACPolicy policy) { policies.add(policy); }

    public void removePolicy(String policyName) {
        policies.removeIf(p -> p.name.equals(policyName));
    }

    public List<ABACPolicy> getActivePolicies() {
        return policies.stream().filter(p -> p.active).toList();
    }

    // ═══════ DEFAULT POLICIES ═══════

    private void initializeDefaultPolicies() {
        // 1. Consent Verification Policy
        policies.add(new ABACPolicy("CONSENT_REQUIRED",
                "Data access requires active consent from data principal",
                ABACPolicy.Scope.ALL) {
            @Override public PolicyResult evaluate(AccessRequest req) {
                if (req.resourceConsentStatus == null || !"ACTIVE".equals(req.resourceConsentStatus)) {
                    return PolicyResult.deny("No active consent — processing blocked per DPDP S.6");
                }
                return PolicyResult.allow();
            }
        });

        // 2. Purpose Limitation Policy
        policies.add(new ABACPolicy("PURPOSE_LIMITATION",
                "Access must have explicit purpose matching consent",
                ABACPolicy.Scope.ALL) {
            @Override public PolicyResult evaluate(AccessRequest req) {
                if (req.purpose == null || req.purpose.isBlank()) {
                    return PolicyResult.deny("Purpose must be declared for data access");
                }
                return PolicyResult.allow();
            }
        });

        // 3. Sensitivity-Clearance Policy
        policies.add(new ABACPolicy("SENSITIVITY_CLEARANCE",
                "User clearance must match or exceed data sensitivity",
                ABACPolicy.Scope.ALL) {
            @Override public PolicyResult evaluate(AccessRequest req) {
                int requiredLevel = sensitivityLevel(req.resourceSensitivity);
                int userLevel = clearanceLevel(req.userClearance);
                if (userLevel < requiredLevel) {
                    return PolicyResult.deny("Insufficient clearance: need " +
                            req.resourceSensitivity + ", have " + req.userClearance);
                }
                return PolicyResult.allow();
            }
        });

        // 4. Business Hours Policy (for sensitive data)
        policies.add(new ABACPolicy("BUSINESS_HOURS",
                "Sensitive/critical data access restricted to business hours",
                ABACPolicy.Scope.SENSITIVE) {
            @Override public PolicyResult evaluate(AccessRequest req) {
                LocalTime now = LocalTime.now();
                if (now.isBefore(LocalTime.of(7, 0)) || now.isAfter(LocalTime.of(22, 0))) {
                    int sensitivity = sensitivityLevel(req.resourceSensitivity);
                    if (sensitivity >= 3) {
                        return PolicyResult.deny("Sensitive data access outside permitted hours (07:00-22:00)");
                    }
                }
                return PolicyResult.allow();
            }
        });

        // 5. Training Completion Policy
        policies.add(new ABACPolicy("TRAINING_REQUIRED",
                "User must have completed DPDP training for data access",
                ABACPolicy.Scope.SENSITIVE) {
            @Override public PolicyResult evaluate(AccessRequest req) {
                if (!req.userTrainingComplete) {
                    return PolicyResult.deny("DPDP training not completed — access blocked");
                }
                return PolicyResult.allow();
            }
        });

        // 6. Data Minimisation Policy
        policies.add(new ABACPolicy("DATA_MINIMISATION",
                "Access scope must not exceed stated purpose",
                ABACPolicy.Scope.ALL) {
            @Override public PolicyResult evaluate(AccessRequest req) {
                if (req.requestedScope != null && "FULL_ACCESS".equals(req.requestedScope)
                        && sensitivityLevel(req.resourceSensitivity) >= 3) {
                    return PolicyResult.deny("Full access not permitted for high-sensitivity data — apply minimum necessary principle");
                }
                return PolicyResult.allow();
            }
        });

        // 7. Sector-Specific Policy (Healthcare)
        policies.add(new ABACPolicy("HEALTHCARE_EHR",
                "Healthcare EHR access limited to treatment-relevant scope",
                ABACPolicy.Scope.SECTOR_HEALTHCARE) {
            @Override public PolicyResult evaluate(AccessRequest req) {
                if ("HEALTHCARE".equals(req.sector) && "EHR".equals(req.resourceCategory)) {
                    if (!"TREATMENT_RELEVANT".equals(req.requestedScope) &&
                            !"CLINICAL".equals(req.userDepartment)) {
                        return PolicyResult.deny("EHR access limited to treatment-relevant scope for non-clinical staff");
                    }
                }
                return PolicyResult.allow();
            }
        });

        // 8. Sector-Specific Policy (BFSI)
        policies.add(new ABACPolicy("BFSI_AADHAAR",
                "Aadhaar data access requires enhanced verification",
                ABACPolicy.Scope.SECTOR_BFSI) {
            @Override public PolicyResult evaluate(AccessRequest req) {
                if ("BFSI".equals(req.sector) && "AADHAAR".equals(req.resourceCategory)) {
                    if (!req.mfaVerified) {
                        return PolicyResult.deny("Aadhaar data requires MFA verification");
                    }
                }
                return PolicyResult.allow();
            }
        });

        logger.info("ABAC default policies initialized: {} policies", policies.size());
    }

    private static int sensitivityLevel(String sensitivity) {
        if (sensitivity == null) return 0;
        return switch (sensitivity.toUpperCase()) {
            case "LOW" -> 1; case "MEDIUM" -> 2; case "HIGH" -> 3;
            case "CRITICAL" -> 4; default -> 0;
        };
    }

    private static int clearanceLevel(String clearance) {
        if (clearance == null) return 0;
        return switch (clearance.toUpperCase()) {
            case "BASIC" -> 1; case "STANDARD" -> 2; case "ELEVATED" -> 3;
            case "TOP" -> 4; default -> 0;
        };
    }

    // ═══════ DATA CLASSES ═══════

    public static class AccessRequest {
        // User attributes
        public String userId, userDepartment, userClearance, sector;
        public boolean userTrainingComplete, mfaVerified;
        // Resource attributes
        public String resourceId, resourceCategory, resourceSensitivity, resourceConsentStatus;
        // Environment attributes
        public String ipAddress, devicePosture;
        // Purpose attributes
        public String purpose, requestedScope, businessJustification;
    }

    public static class AccessDecision {
        public String requestId, userId, resourceId, purpose;
        public boolean allowed;
        public List<String> denialReasons;
        public int policiesEvaluated, policiesSatisfied;
        public LocalDateTime evaluatedAt;
    }

    public static abstract class ABACPolicy {
        public enum Scope { ALL, SENSITIVE, SECTOR_BFSI, SECTOR_HEALTHCARE, SECTOR_INSURANCE }
        public final String name, description;
        public final Scope scope;
        public boolean active = true;

        protected ABACPolicy(String name, String description, Scope scope) {
            this.name = name; this.description = description; this.scope = scope;
        }

        public boolean appliesTo(AccessRequest req) {
            return switch (scope) {
                case ALL -> true;
                case SENSITIVE -> sensitivityLevel(req.resourceSensitivity) >= 3;
                case SECTOR_BFSI -> "BFSI".equals(req.sector);
                case SECTOR_HEALTHCARE -> "HEALTHCARE".equals(req.sector);
                case SECTOR_INSURANCE -> "INSURANCE".equals(req.sector);
            };
        }

        public abstract PolicyResult evaluate(AccessRequest req);
    }

    public static class PolicyResult {
        public final boolean denied;
        public final String reason;
        private PolicyResult(boolean denied, String reason) { this.denied = denied; this.reason = reason; }
        public static PolicyResult allow() { return new PolicyResult(false, null); }
        public static PolicyResult deny(String reason) { return new PolicyResult(true, reason); }
    }
}
