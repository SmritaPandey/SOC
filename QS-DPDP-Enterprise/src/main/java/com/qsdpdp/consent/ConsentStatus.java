package com.qsdpdp.consent;

/**
 * Consent status enumeration
 */
public enum ConsentStatus {
    REQUESTED("Consent requested, awaiting data principal response"),
    PENDING("Pending verification"),
    ACTIVE("Active consent"),
    MODIFIED("Consent preferences modified by data principal"),
    WITHDRAWN("Consent withdrawn by data principal"),
    EXPIRED("Consent expired per retention policy"),
    REVOKED("Consent revoked by system or regulator");

    private final String description;

    ConsentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
