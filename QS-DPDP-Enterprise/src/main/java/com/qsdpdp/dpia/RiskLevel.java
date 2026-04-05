package com.qsdpdp.dpia;

/**
 * Risk level enumeration
 */
public enum RiskLevel {
    CRITICAL(4, "Critical risk - Requires DPBI consultation"),
    HIGH(3, "High risk - May require DPBI consultation"),
    MEDIUM(2, "Medium risk"),
    LOW(1, "Low risk");

    private final int level;
    private final String description;

    RiskLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    public static RiskLevel fromScore(double score) {
        if (score >= 80)
            return CRITICAL;
        if (score >= 60)
            return HIGH;
        if (score >= 40)
            return MEDIUM;
        return LOW;
    }
}
