package com.qsdpdp.breach;

import java.time.LocalDateTime;

/**
 * Breach timeline event
 */
public class BreachTimelineEvent {

    private String id;
    private String breachId;
    private LocalDateTime timestamp;
    private String eventType;
    private String description;
    private String actor;

    public BreachTimelineEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public BreachTimelineEvent(String eventType, String description, String actor) {
        this();
        this.eventType = eventType;
        this.description = description;
        this.actor = actor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBreachId() {
        return breachId;
    }

    public void setBreachId(String breachId) {
        this.breachId = breachId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}
