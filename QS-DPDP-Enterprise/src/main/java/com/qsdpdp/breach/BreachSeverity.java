package com.qsdpdp.breach;

/**
 * Breach severity enumeration
 */
public enum BreachSeverity {
    CRITICAL(4, "Critical - Immediate action required", 6),
    HIGH(3, "High - Action within 24 hours", 24),
    MEDIUM(2, "Medium - Action within 72 hours", 72),
    LOW(1, "Low - Action within 7 days", 168);

    private final int level;
    private final String description;
    private final int responseHours;

    BreachSeverity(int level, String description, int responseHours) {
        this.level = level;
        this.description = description;
        this.responseHours = responseHours;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public int getResponseHours() {
        return responseHours;
    }
}
