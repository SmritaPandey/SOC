package com.qsdpdp.rules;

/**
 * Rule severity levels
 */
public enum RuleSeverity {
    CRITICAL(4, "Immediate action required"),
    HIGH(3, "Action within 24 hours"),
    MEDIUM(2, "Action within 7 days"),
    LOW(1, "Action within 30 days"),
    INFO(0, "Informational only");

    private final int level;
    private final String description;

    RuleSeverity(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }
}
