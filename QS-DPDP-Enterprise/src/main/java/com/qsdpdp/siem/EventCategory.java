package com.qsdpdp.siem;

/**
 * SIEM Event Categories aligned with DPDP compliance requirements
 * 
 * @version 1.0.0
 * @since Module 6
 */
public enum EventCategory {
    // Authentication & Access
    AUTH_SUCCESS("Authentication", "Successful authentication"),
    AUTH_FAILURE("Authentication", "Failed authentication attempt"),
    AUTH_LOCKOUT("Authentication", "Account lockout due to failed attempts"),
    AUTH_MFA("Authentication", "MFA challenge"),

    // Authorization & Access Control
    ACCESS_GRANTED("Authorization", "Access granted to resource"),
    ACCESS_DENIED("Authorization", "Access denied to resource"),
    PRIVILEGE_ESCALATION("Authorization", "Privilege escalation attempt"),

    // Data Operations (DPDP Critical)
    DATA_ACCESS("Data", "Personal data accessed"),
    DATA_MODIFICATION("Data", "Personal data modified"),
    DATA_DELETION("Data", "Personal data deleted"),
    DATA_EXPORT("Data", "Personal data exported"),
    DATA_TRANSFER("Data", "Personal data transferred"),
    SENSITIVE_DATA_ACCESS("Data", "Sensitive personal data accessed"),

    // Consent Events
    CONSENT_COLLECTED("Consent", "Consent collected from data principal"),
    CONSENT_WITHDRAWN("Consent", "Consent withdrawn by data principal"),
    CONSENT_EXPIRED("Consent", "Consent expired"),
    PURPOSE_CHANGE("Consent", "Processing purpose changed"),

    // Rights Requests
    RIGHTS_REQUEST("Rights", "Data principal rights request"),
    RIGHTS_FULFILLED("Rights", "Rights request fulfilled"),
    RIGHTS_DENIED("Rights", "Rights request denied"),

    // Policy Events
    POLICY_CREATED("Policy", "New policy created"),
    POLICY_APPROVED("Policy", "Policy approved"),
    POLICY_VIOLATION("Policy", "Policy violation detected"),

    // Security Incidents
    MALWARE_DETECTED("Security", "Malware detected"),
    INTRUSION_ATTEMPT("Security", "Intrusion attempt detected"),
    DLP_VIOLATION("Security", "DLP policy violation"),
    PII_EXPOSURE("Security", "PII exposure detected"),
    BREACH_SUSPECTED("Security", "Potential data breach"),
    BREACH_CONFIRMED("Security", "Data breach confirmed"),

    // EDR Endpoint Events
    EDR_PROCESS_BLOCKED("EDR", "Malicious process blocked at endpoint"),
    EDR_FILE_QUARANTINED("EDR", "Suspicious file quarantined"),
    EDR_ENDPOINT_ISOLATED("EDR", "Endpoint network-isolated due to threat"),
    EDR_FIM_ALERT("EDR", "File integrity monitoring alert"),
    EDR_AGENT_OFFLINE("EDR", "Endpoint agent lost heartbeat"),
    EDR_THREAT_DETECTED("EDR", "Endpoint threat detected"),

    // System Events
    SYSTEM_START("System", "System started"),
    SYSTEM_STOP("System", "System stopped"),
    CONFIG_CHANGE("System", "Configuration changed"),
    BACKUP_COMPLETE("System", "Backup completed"),

    // Audit Events
    AUDIT_START("Audit", "Audit started"),
    AUDIT_COMPLETE("Audit", "Audit completed"),
    COMPLIANCE_CHECK("Audit", "Compliance check performed"),

    // Network Events
    NETWORK_CONNECT("Network", "Network connection established"),
    NETWORK_BLOCK("Network", "Network connection blocked"),
    FIREWALL_EVENT("Network", "Firewall event"),

    // Application Events
    APP_ERROR("Application", "Application error"),
    APP_WARNING("Application", "Application warning"),
    API_CALL("Application", "API call made");

    private final String category;
    private final String description;

    EventCategory(String category, String description) {
        this.category = category;
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this event type requires DPDP notification
     */
    public boolean requiresDPDPNotification() {
        return switch (this) {
            case BREACH_SUSPECTED, BREACH_CONFIRMED, PII_EXPOSURE,
                    SENSITIVE_DATA_ACCESS, DATA_TRANSFER, DLP_VIOLATION ->
                true;
            default -> false;
        };
    }

    /**
     * Check if this event affects personal data
     */
    public boolean affectsPersonalData() {
        return switch (this) {
            case DATA_ACCESS, DATA_MODIFICATION, DATA_DELETION, DATA_EXPORT,
                    DATA_TRANSFER, SENSITIVE_DATA_ACCESS, PII_EXPOSURE,
                    CONSENT_COLLECTED, CONSENT_WITHDRAWN, RIGHTS_REQUEST ->
                true;
            default -> false;
        };
    }

    /**
     * Get DPDP Act section reference
     */
    public String getDPDPSection() {
        return switch (this) {
            case CONSENT_COLLECTED, CONSENT_WITHDRAWN, CONSENT_EXPIRED -> "Section 6 - Consent";
            case RIGHTS_REQUEST, RIGHTS_FULFILLED, RIGHTS_DENIED -> "Section 11-14 - Rights";
            case BREACH_SUSPECTED, BREACH_CONFIRMED, PII_EXPOSURE -> "Section 8 - Breach Notification";
            case DATA_ACCESS, DATA_MODIFICATION, DATA_DELETION -> "Section 7 - Processing";
            case POLICY_VIOLATION -> "Section 5 - Lawful Processing";
            default -> "";
        };
    }
}
