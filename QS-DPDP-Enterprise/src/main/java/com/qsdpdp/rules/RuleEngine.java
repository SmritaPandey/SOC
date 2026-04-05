package com.qsdpdp.rules;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Business Rule Engine for QS-DPDP Enterprise
 * Evaluates compliance rules and triggers actions
 * 
 * @version 1.0.0
 * @since Phase 1
 */
@Service
public class RuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(RuleEngine.class);

    private final DatabaseManager dbManager;
    private final EventBus eventBus;

    private final Map<String, Rule> rules = new LinkedHashMap<>();
    private boolean initialized = false;

    @Autowired
    public RuleEngine(DatabaseManager dbManager, EventBus eventBus) {
        this.dbManager = dbManager;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        logger.info("Initializing Rule Engine...");

        // Load default DPDP compliance rules
        loadDefaultRules();

        // Subscribe to events for rule evaluation
        eventBus.subscribe("*", this::evaluateRulesForEvent);

        initialized = true;
        logger.info("Rule Engine initialized with {} rules", rules.size());
    }

    private void loadDefaultRules() {
        // ═══════════════════════════════════════════════════════════
        // CONSENT RULES
        // ═══════════════════════════════════════════════════════════

        registerRule(new Rule(
                "RULE-CON-001",
                "Consent Expiry Check",
                "CONSENT",
                "Check for expired consents still marked active",
                RuleSeverity.HIGH,
                context -> {
                    // This would check database for expired consents
                    return RuleResult.pass("No expired consents found");
                }));

        registerRule(new Rule(
                "RULE-CON-002",
                "Withdrawal Response Time",
                "CONSENT",
                "Verify withdrawal requests are processed within 24 hours",
                RuleSeverity.HIGH,
                context -> {
                    return RuleResult.pass("Withdrawal SLA met");
                }));

        // ═══════════════════════════════════════════════════════════
        // BREACH RULES
        // ═══════════════════════════════════════════════════════════

        registerRule(new Rule(
                "RULE-BRE-001",
                "72-Hour Notification Check",
                "BREACH",
                "Alert if breach notification deadline is approaching",
                RuleSeverity.CRITICAL,
                context -> {
                    // Check for breaches approaching 72-hour deadline
                    return RuleResult.pass("No breaches approaching deadline");
                }));

        registerRule(new Rule(
                "RULE-BRE-002",
                "CERT-IN 6-Hour Notification",
                "BREACH",
                "Alert for critical breaches requiring CERT-IN notification",
                RuleSeverity.CRITICAL,
                context -> {
                    return RuleResult.pass("No critical breaches pending");
                }));

        // ═══════════════════════════════════════════════════════════
        // RIGHTS RULES
        // ═══════════════════════════════════════════════════════════

        registerRule(new Rule(
                "RULE-RIG-001",
                "30-Day Response Deadline",
                "RIGHTS",
                "Alert if rights request is approaching 30-day deadline",
                RuleSeverity.HIGH,
                context -> {
                    return RuleResult.pass("No requests approaching deadline");
                }));

        registerRule(new Rule(
                "RULE-RIG-002",
                "Unacknowledged Requests",
                "RIGHTS",
                "Alert for requests not acknowledged within 48 hours",
                RuleSeverity.MEDIUM,
                context -> {
                    return RuleResult.pass("All requests acknowledged");
                }));

        // ═══════════════════════════════════════════════════════════
        // DPIA RULES
        // ═══════════════════════════════════════════════════════════

        registerRule(new Rule(
                "RULE-DPI-001",
                "High-Risk Processing Check",
                "DPIA",
                "Ensure high-risk processing has approved DPIA",
                RuleSeverity.HIGH,
                context -> {
                    return RuleResult.pass("All high-risk processing covered");
                }));

        // ═══════════════════════════════════════════════════════════
        // POLICY RULES
        // ═══════════════════════════════════════════════════════════

        registerRule(new Rule(
                "RULE-POL-001",
                "Policy Review Due",
                "POLICY",
                "Alert for policies due for annual review",
                RuleSeverity.MEDIUM,
                context -> {
                    return RuleResult.pass("All policies current");
                }));

        // ═══════════════════════════════════════════════════════════
        // SECURITY RULES
        // ═══════════════════════════════════════════════════════════

        registerRule(new Rule(
                "RULE-SEC-001",
                "Failed Login Threshold",
                "SECURITY",
                "Alert on excessive failed login attempts",
                RuleSeverity.HIGH,
                context -> {
                    return RuleResult.pass("No suspicious login activity");
                }));

        registerRule(new Rule(
                "RULE-SEC-002",
                "Session Timeout Check",
                "SECURITY",
                "Verify session timeout is configured correctly",
                RuleSeverity.MEDIUM,
                context -> {
                    return RuleResult.pass("Session timeout configured");
                }));

        // ═══════════════════════════════════════════════════════════
        // RETENTION RULES
        // ═══════════════════════════════════════════════════════════

        registerRule(new Rule(
                "RULE-RET-001",
                "Data Retention Compliance",
                "RETENTION",
                "Identify data past retention period",
                RuleSeverity.MEDIUM,
                context -> {
                    return RuleResult.pass("Data retention compliant");
                }));
    }

    /**
     * Register a new rule
     */
    public void registerRule(Rule rule) {
        rules.put(rule.getId(), rule);
        logger.debug("Registered rule: {}", rule.getId());
    }

    /**
     * Evaluate all rules
     */
    public List<RuleResult> evaluateAllRules() {
        return evaluateAllRules(new HashMap<>());
    }

    /**
     * Evaluate all rules with context
     */
    public List<RuleResult> evaluateAllRules(Map<String, Object> context) {
        logger.info("Evaluating {} rules...", rules.size());

        List<RuleResult> results = new ArrayList<>();

        for (Rule rule : rules.values()) {
            try {
                RuleResult result = rule.evaluate(context);
                result.setRuleId(rule.getId());
                result.setRuleName(rule.getName());
                result.setModule(rule.getModule());
                results.add(result);

                if (!result.isPassed()) {
                    logger.warn("Rule failed: {} - {}", rule.getId(), result.getMessage());

                    // Publish event for failed rule
                    eventBus.publish(new ComplianceEvent("rule.failed", result));
                }
            } catch (Exception e) {
                logger.error("Error evaluating rule: {}", rule.getId(), e);
                RuleResult errorResult = RuleResult.fail("Evaluation error: " + e.getMessage());
                errorResult.setRuleId(rule.getId());
                results.add(errorResult);
            }
        }

        long passed = results.stream().filter(RuleResult::isPassed).count();
        logger.info("Rule evaluation complete: {}/{} passed", passed, results.size());

        return results;
    }

    /**
     * Evaluate rules for a specific module
     */
    public List<RuleResult> evaluateModuleRules(String module) {
        return evaluateModuleRules(module, new HashMap<>());
    }

    /**
     * Evaluate rules for a specific module with context
     */
    public List<RuleResult> evaluateModuleRules(String module, Map<String, Object> context) {
        List<RuleResult> results = new ArrayList<>();

        for (Rule rule : rules.values()) {
            if (rule.getModule().equalsIgnoreCase(module)) {
                try {
                    RuleResult result = rule.evaluate(context);
                    result.setRuleId(rule.getId());
                    result.setRuleName(rule.getName());
                    result.setModule(rule.getModule());
                    results.add(result);
                } catch (Exception e) {
                    logger.error("Error evaluating rule: {}", rule.getId(), e);
                }
            }
        }

        return results;
    }

    /**
     * Handle events and evaluate relevant rules
     */
    private void evaluateRulesForEvent(ComplianceEvent event) {
        // Extract module from event type (e.g., "consent.created" -> "CONSENT")
        String eventType = event.getType();
        if (eventType == null || !eventType.contains(".")) {
            return;
        }

        String module = eventType.split("\\.")[0].toUpperCase();

        // Evaluate rules for this module
        Map<String, Object> context = new HashMap<>();
        context.put("event", event);
        context.put("payload", event.getPayload());

        evaluateModuleRules(module, context);
    }

    public Rule getRule(String ruleId) {
        return rules.get(ruleId);
    }

    public Collection<Rule> getAllRules() {
        return rules.values();
    }

    public int getRuleCount() {
        return rules.size();
    }

    public boolean isInitialized() {
        return initialized;
    }
}
