package com.qsdpdp.policy;

/**
 * Policy status enumeration
 */
public enum PolicyStatus {
    DRAFT("Draft"),
    PENDING_APPROVAL("Pending Approval"),
    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    PUBLISHED("Published"),
    ACTIVE("Active"),
    EXPIRED("Expired"),
    REVOKED("Revoked"),
    ARCHIVED("Archived"),
    SUPERSEDED("Superseded by new version");

    private final String description;

    PolicyStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
