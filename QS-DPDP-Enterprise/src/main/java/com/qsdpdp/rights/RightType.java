package com.qsdpdp.rights;

/**
 * Data subject right types per DPDP Act 2023
 */
public enum RightType {
    ACCESS("Right to Access", "Right to obtain confirmation and access personal data"),
    CORRECTION("Right to Correction", "Right to correct inaccurate or incomplete data"),
    ERASURE("Right to Erasure", "Right to erasure of personal data (with limitations)"),
    PORTABILITY("Right to Portability", "Right to receive data in portable format"),
    OBJECTION("Right to Object", "Right to object to processing"),
    RESTRICTION("Right to Restrict", "Right to restrict processing"),
    WITHDRAWAL("Right to Withdraw Consent", "Right to withdraw previously given consent (DPDP Section 6(5))"),
    NOMINATION("Right to Nominate", "Right to nominate another person to exercise rights");

    private final String name;
    private final String description;

    RightType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
