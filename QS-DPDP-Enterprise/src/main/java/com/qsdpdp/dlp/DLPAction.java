package com.qsdpdp.dlp;

/**
 * DLP Policy Action - What to do when a policy is violated
 * 
 * @version 1.0.0
 * @since Module 7
 */
public enum DLPAction {
    ALLOW("Allow", "Allow the action to proceed", false),
    LOG_ONLY("Log Only", "Log the activity but allow it", false),
    WARN("Warn", "Warn the user but allow after acknowledgment", false),
    BLOCK("Block", "Block the action", true),
    ENCRYPT("Encrypt", "Encrypt the data before allowing transfer", true),
    QUARANTINE("Quarantine", "Move data to quarantine for review", true),
    NOTIFY("Notify", "Notify security team", true),
    REDACT("Redact", "Redact sensitive content", true);

    private final String name;
    private final String description;
    private final boolean preventive;

    DLPAction(String name, String description, boolean preventive) {
        this.name = name;
        this.description = description;
        this.preventive = preventive;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPreventive() {
        return preventive;
    }
}
