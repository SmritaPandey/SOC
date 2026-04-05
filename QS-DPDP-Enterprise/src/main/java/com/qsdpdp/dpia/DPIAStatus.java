package com.qsdpdp.dpia;

/**
 * DPIA status enumeration
 */
public enum DPIAStatus {
    DRAFT("Draft"),
    IN_PROGRESS("In Progress"),
    PENDING_REVIEW("Pending Review"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    EXPIRED("Requires Review");

    private final String description;

    DPIAStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
