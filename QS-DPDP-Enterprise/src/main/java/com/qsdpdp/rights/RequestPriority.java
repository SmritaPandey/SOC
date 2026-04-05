package com.qsdpdp.rights;

/**
 * Request priority enumeration
 */
public enum RequestPriority {
    URGENT(4, "Urgent - respond within 48 hours"),
    HIGH(3, "High priority"),
    NORMAL(2, "Normal priority"),
    LOW(1, "Low priority");

    private final int level;
    private final String description;

    RequestPriority(int level, String description) {
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
