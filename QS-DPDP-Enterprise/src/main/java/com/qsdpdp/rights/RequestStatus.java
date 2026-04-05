package com.qsdpdp.rights;

/**
 * Request status enumeration
 */
public enum RequestStatus {
    PENDING("Pending review"),
    ACKNOWLEDGED("Request acknowledged"),
    IN_PROGRESS("In progress"),
    AWAITING_VERIFICATION("Awaiting identity verification"),
    COMPLETED("Request completed"),
    REJECTED("Request rejected"),
    WITHDRAWN("Withdrawn by data principal");

    private final String description;

    RequestStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
