package com.qsdpdp.siem;

/**
 * SIEM Event Severity Levels
 * Aligned with DPDP Act breach notification requirements
 * 
 * @version 1.0.0
 * @since Module 6
 */
public enum EventSeverity {
    CRITICAL("CRITICAL", 1, true, "Immediate escalation required - potential DPDP breach"),
    HIGH("HIGH", 2, true, "Urgent attention required - compliance risk"),
    MEDIUM("MEDIUM", 3, false, "Standard investigation required"),
    LOW("LOW", 4, false, "Informational - for audit trail"),
    INFO("INFO", 5, false, "Normal operational event");

    private final String name;
    private final int priority;
    private final boolean escalate;
    private final String description;

    EventSeverity(String name, int priority, boolean escalate, String description) {
        this.name = name;
        this.priority = priority;
        this.escalate = escalate;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public boolean shouldEscalate() {
        return escalate;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get severity from DPDP impact level
     */
    public static EventSeverity fromDPDPImpact(String impact) {
        return switch (impact.toUpperCase()) {
            case "PERSONAL_DATA_BREACH" -> CRITICAL;
            case "SENSITIVE_DATA_ACCESS" -> CRITICAL;
            case "UNAUTHORIZED_PROCESSING" -> HIGH;
            case "CONSENT_VIOLATION" -> HIGH;
            case "POLICY_VIOLATION" -> MEDIUM;
            case "ACCESS_ATTEMPT" -> LOW;
            default -> INFO;
        };
    }

    /**
     * Get CERT-In notification timeline (hours)
     */
    public int getCERTInNotificationHours() {
        return switch (this) {
            case CRITICAL -> 6; // 6 hours for cyber incidents
            case HIGH -> 24;
            case MEDIUM -> 72;
            default -> -1; // No notification required
        };
    }

    /**
     * Get DPBI notification timeline (hours)
     */
    public int getDPBINotificationHours() {
        return switch (this) {
            case CRITICAL -> 72; // 72 hours for DPDP breaches
            case HIGH -> 72;
            default -> -1;
        };
    }
}
