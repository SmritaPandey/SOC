package com.qsdpdp.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compliance Event representing system events
 */
public class ComplianceEvent {

    private final String id;
    private final String type;
    private final Object payload;
    private final LocalDateTime timestamp;
    private final String source;

    public ComplianceEvent(String type, Object payload) {
        this(type, payload, null);
    }

    public ComplianceEvent(String type, Object payload, String source) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
        this.source = source != null ? source : "SYSTEM";
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayloadAs(Class<T> type) {
        return (T) payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return String.format("ComplianceEvent[%s: %s]", type, id);
    }
}
