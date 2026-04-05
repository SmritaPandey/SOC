package com.qsdpdp.core;

/**
 * Gap severity levels for compliance assessment
 */
public enum GapSeverity {
    CRITICAL(4, "Immediate action required"),
    HIGH(3, "Action required within 24 hours"),
    MEDIUM(2, "Action required within 7 days"),
    LOW(1, "Action required within 30 days");

    private final int level;
    private final String description;

    GapSeverity(int level, String description) {
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
