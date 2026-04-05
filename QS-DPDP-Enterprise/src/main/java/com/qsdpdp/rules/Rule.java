package com.qsdpdp.rules;

import java.util.Map;
import java.util.function.Function;

/**
 * Rule definition for compliance checking
 */
public class Rule {

    private final String id;
    private final String name;
    private final String module;
    private final String description;
    private final RuleSeverity severity;
    private final Function<Map<String, Object>, RuleResult> evaluator;

    public Rule(String id, String name, String module, String description,
            RuleSeverity severity, Function<Map<String, Object>, RuleResult> evaluator) {
        this.id = id;
        this.name = name;
        this.module = module;
        this.description = description;
        this.severity = severity;
        this.evaluator = evaluator;
    }

    public RuleResult evaluate(Map<String, Object> context) {
        return evaluator.apply(context);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getModule() {
        return module;
    }

    public String getDescription() {
        return description;
    }

    public RuleSeverity getSeverity() {
        return severity;
    }
}
