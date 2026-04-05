package com.qsdpdp.breach;

/**
 * Breach status enumeration
 */
public enum BreachStatus {
    OPEN("Open - Initial assessment"),
    INVESTIGATING("Under investigation"),
    CONTAINED("Breach contained"),
    NOTIFYING("Notifying affected parties"),
    RESOLVED("Breach resolved"),
    CLOSED("Case closed");

    private final String description;

    BreachStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
