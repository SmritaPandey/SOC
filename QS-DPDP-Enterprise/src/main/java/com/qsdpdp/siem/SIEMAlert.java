package com.qsdpdp.siem;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SIEM Alert model
 * 
 * @version 1.0.0
 * @since Module 6
 */
public class SIEMAlert {
    private String id;
    private String ruleId;
    private String ruleName;
    private EventSeverity severity;
    private EventCategory category;
    private String title;
    private String description;
    private List<String> sourceEvents;
    private int eventCount;
    private String dpdpSection;
    private boolean requiresNotification;
    private int notificationDeadlineHours;
    private String playbookId;
    private String playbookStatus;
    private String status;
    private String assignedTo;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String resolution;
    private boolean falsePositive;
    private LocalDateTime createdAt;

    public SIEMAlert() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.status = "NEW";
        this.sourceEvents = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(EventSeverity severity) {
        this.severity = severity;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSourceEvents() {
        return sourceEvents;
    }

    public void setSourceEvents(List<String> sourceEvents) {
        this.sourceEvents = sourceEvents;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public String getDpdpSection() {
        return dpdpSection;
    }

    public void setDpdpSection(String dpdpSection) {
        this.dpdpSection = dpdpSection;
    }

    public boolean isRequiresNotification() {
        return requiresNotification;
    }

    public void setRequiresNotification(boolean requiresNotification) {
        this.requiresNotification = requiresNotification;
    }

    public int getNotificationDeadlineHours() {
        return notificationDeadlineHours;
    }

    public void setNotificationDeadlineHours(int hours) {
        this.notificationDeadlineHours = hours;
    }

    public String getPlaybookId() {
        return playbookId;
    }

    public void setPlaybookId(String playbookId) {
        this.playbookId = playbookId;
    }

    public String getPlaybookStatus() {
        return playbookStatus;
    }

    public void setPlaybookStatus(String playbookStatus) {
        this.playbookStatus = playbookStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public boolean isFalsePositive() {
        return falsePositive;
    }

    public void setFalsePositive(boolean falsePositive) {
        this.falsePositive = falsePositive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
