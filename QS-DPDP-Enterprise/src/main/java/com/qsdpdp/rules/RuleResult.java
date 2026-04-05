package com.qsdpdp.rules;

/**
 * Rule Result from evaluation
 */
public class RuleResult {

    private String ruleId;
    private String ruleName;
    private String module;
    private boolean passed;
    private String message;
    private Object details;

    private RuleResult(boolean passed, String message) {
        this.passed = passed;
        this.message = message;
    }

    public static RuleResult pass(String message) {
        return new RuleResult(true, message);
    }

    public static RuleResult fail(String message) {
        return new RuleResult(false, message);
    }

    public static RuleResult passWithDetails(String message, Object details) {
        RuleResult result = new RuleResult(true, message);
        result.details = details;
        return result;
    }

    public static RuleResult failWithDetails(String message, Object details) {
        RuleResult result = new RuleResult(false, message);
        result.details = details;
        return result;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }

    public Object getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return String.format("RuleResult[%s: %s - %s]", ruleId, passed ? "PASS" : "FAIL", message);
    }
}
