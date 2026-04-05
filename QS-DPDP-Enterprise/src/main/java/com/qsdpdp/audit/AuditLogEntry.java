package com.qsdpdp.audit;

/**
 * Audit Log Entry data class
 */
public class AuditLogEntry {

    private final String id;
    private final long sequenceNumber;
    private final String timestamp;
    private final String eventType;
    private final String module;
    private final String action;
    private final String actor;
    private final String details;

    public AuditLogEntry(String id, long sequenceNumber, String timestamp,
            String eventType, String module, String action,
            String actor, String details) {
        this.id = id;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.module = module;
        this.action = action;
        this.actor = actor;
        this.details = details;
    }

    public String getId() {
        return id;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public String getModule() {
        return module;
    }

    public String getAction() {
        return action;
    }

    public String getActor() {
        return actor;
    }

    public String getDetails() {
        return details;
    }
}
