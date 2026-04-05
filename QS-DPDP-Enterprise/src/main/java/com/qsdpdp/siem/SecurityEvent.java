package com.qsdpdp.siem;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Security Event model for SIEM
 * Captures all security-relevant events with DPDP compliance metadata
 * 
 * @version 1.0.0
 * @since Module 6
 */
public class SecurityEvent {

    private String id;
    private LocalDateTime timestamp;
    private EventCategory category;
    private EventSeverity severity;
    private String source; // Source system/application
    private String sourceIP;
    private String destinationIP;
    private String userId;
    private String userName;
    private String action;
    private String resource; // Resource being accessed
    private String resourceType; // Type of resource
    private boolean success;
    private String message;
    private String rawLog; // Original log entry
    private Map<String, Object> metadata;

    // DPDP-specific fields
    private String dataPrincipalId;
    private String dataFiduciaryId;
    private String processingPurpose;
    private boolean personalDataInvolved;
    private boolean sensitiveDataInvolved;
    private String dpdpSection;

    // Correlation fields
    private String correlationId;
    private String parentEventId;
    private String sessionId;

    // Processing status
    private String status; // NEW, ANALYZED, ESCALATED, RESOLVED
    private String assignedTo;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String resolution;

    public SecurityEvent() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.status = "NEW";
        this.metadata = new HashMap<>();
    }

    public SecurityEvent(EventCategory category, EventSeverity severity, String message) {
        this();
        this.category = category;
        this.severity = severity;
        this.message = message;
        this.personalDataInvolved = category.affectsPersonalData();
        this.dpdpSection = category.getDPDPSection();
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SecurityEvent event = new SecurityEvent();

        public Builder category(EventCategory category) {
            event.category = category;
            event.dpdpSection = category.getDPDPSection();
            return this;
        }

        public Builder severity(EventSeverity severity) {
            event.severity = severity;
            return this;
        }

        public Builder source(String source, String sourceIP) {
            event.source = source;
            event.sourceIP = sourceIP;
            return this;
        }

        public Builder destination(String destinationIP) {
            event.destinationIP = destinationIP;
            return this;
        }

        public Builder user(String userId, String userName) {
            event.userId = userId;
            event.userName = userName;
            return this;
        }

        public Builder action(String action, boolean success) {
            event.action = action;
            event.success = success;
            return this;
        }

        public Builder resource(String resource, String resourceType) {
            event.resource = resource;
            event.resourceType = resourceType;
            return this;
        }

        public Builder message(String message) {
            event.message = message;
            return this;
        }

        public Builder rawLog(String rawLog) {
            event.rawLog = rawLog;
            return this;
        }

        public Builder dataPrincipal(String dataPrincipalId) {
            event.dataPrincipalId = dataPrincipalId;
            event.personalDataInvolved = true;
            return this;
        }

        public Builder dataFiduciary(String dataFiduciaryId) {
            event.dataFiduciaryId = dataFiduciaryId;
            return this;
        }

        public Builder processingPurpose(String purpose) {
            event.processingPurpose = purpose;
            return this;
        }

        public Builder personalData(boolean involved) {
            event.personalDataInvolved = involved;
            return this;
        }

        public Builder sensitiveData(boolean sensitive) {
            event.sensitiveDataInvolved = sensitive;
            return this;
        }

        public Builder correlationId(String correlationId) {
            event.correlationId = correlationId;
            return this;
        }

        public Builder parentEvent(String parentEventId) {
            event.parentEventId = parentEventId;
            return this;
        }

        public Builder session(String sessionId) {
            event.sessionId = sessionId;
            return this;
        }

        public Builder metadata(String key, Object value) {
            event.metadata.put(key, value);
            return this;
        }

        public SecurityEvent build() {
            return event;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // COMPLIANCE HELPERS
    // ═══════════════════════════════════════════════════════════

    public boolean requiresEscalation() {
        return severity.shouldEscalate() ||
                (personalDataInvolved && sensitiveDataInvolved);
    }

    public boolean requiresCERTInNotification() {
        return severity == EventSeverity.CRITICAL ||
                category == EventCategory.BREACH_CONFIRMED ||
                category == EventCategory.INTRUSION_ATTEMPT;
    }

    public boolean requiresDPBINotification() {
        return category.requiresDPDPNotification() ||
                (personalDataInvolved && category == EventCategory.BREACH_CONFIRMED);
    }

    public int getNotificationDeadlineHours() {
        if (requiresCERTInNotification()) {
            return 6; // CERT-In 6-hour requirement
        }
        if (requiresDPBINotification()) {
            return 72; // DPBI 72-hour requirement
        }
        return -1;
    }

    public String getComplianceRisk() {
        if (requiresDPBINotification()) {
            return "DPDP Breach Notification Required";
        }
        if (personalDataInvolved) {
            return "Personal Data Processing Event";
        }
        return "Standard Security Event";
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(EventSeverity severity) {
        this.severity = severity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public String getDestinationIP() {
        return destinationIP;
    }

    public void setDestinationIP(String destinationIP) {
        this.destinationIP = destinationIP;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawLog() {
        return rawLog;
    }

    public void setRawLog(String rawLog) {
        this.rawLog = rawLog;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getDataPrincipalId() {
        return dataPrincipalId;
    }

    public void setDataPrincipalId(String dataPrincipalId) {
        this.dataPrincipalId = dataPrincipalId;
    }

    public String getDataFiduciaryId() {
        return dataFiduciaryId;
    }

    public void setDataFiduciaryId(String dataFiduciaryId) {
        this.dataFiduciaryId = dataFiduciaryId;
    }

    public String getProcessingPurpose() {
        return processingPurpose;
    }

    public void setProcessingPurpose(String processingPurpose) {
        this.processingPurpose = processingPurpose;
    }

    public boolean isPersonalDataInvolved() {
        return personalDataInvolved;
    }

    public void setPersonalDataInvolved(boolean personalDataInvolved) {
        this.personalDataInvolved = personalDataInvolved;
    }

    public boolean isSensitiveDataInvolved() {
        return sensitiveDataInvolved;
    }

    public void setSensitiveDataInvolved(boolean sensitiveDataInvolved) {
        this.sensitiveDataInvolved = sensitiveDataInvolved;
    }

    public String getDpdpSection() {
        return dpdpSection;
    }

    public void setDpdpSection(String dpdpSection) {
        this.dpdpSection = dpdpSection;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getParentEventId() {
        return parentEventId;
    }

    public void setParentEventId(String parentEventId) {
        this.parentEventId = parentEventId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    @Override
    public String toString() {
        return String.format("SecurityEvent{id=%s, category=%s, severity=%s, message=%s, personalData=%s}",
                id, category, severity, message, personalDataInvolved);
    }
}
