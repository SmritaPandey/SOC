package com.qsdpdp.breach;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Breach incident entity
 */
public class Breach {

    private String id;
    private String referenceNumber;
    private String title;
    private String description;
    private BreachSeverity severity;
    private String breachType;
    private String dataCategories;
    private int affectedCount;
    private LocalDateTime detectedAt;
    private LocalDateTime reportedAt;
    private LocalDateTime containedAt;
    private LocalDateTime resolvedAt;
    private BreachStatus status;
    private String rootCause;
    private String remediationSteps;

    // DPBI Notification (72 hours per DPDP Act)
    private boolean dpbiNotified;
    private LocalDateTime dpbiNotificationDate;
    private LocalDateTime dpbiDeadline;
    private String dpbiReference;

    // CERT-IN Notification (6 hours)
    private boolean certinNotified;
    private LocalDateTime certinNotificationDate;
    private LocalDateTime certinDeadline;

    // Affected parties notification
    private boolean affectedPartiesNotified;

    private String reportedBy;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Timeline events
    private List<BreachTimelineEvent> timeline = new ArrayList<>();

    public Breach() {
        this.status = BreachStatus.OPEN;
        this.severity = BreachSeverity.MEDIUM;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Breach breach = new Breach();

        public Builder id(String id) {
            breach.id = id;
            return this;
        }

        public Builder referenceNumber(String ref) {
            breach.referenceNumber = ref;
            return this;
        }

        public Builder title(String title) {
            breach.title = title;
            return this;
        }

        public Builder description(String desc) {
            breach.description = desc;
            return this;
        }

        public Builder severity(BreachSeverity sev) {
            breach.severity = sev;
            return this;
        }

        public Builder breachType(String type) {
            breach.breachType = type;
            return this;
        }

        public Builder dataCategories(String cats) {
            breach.dataCategories = cats;
            return this;
        }

        public Builder affectedCount(int count) {
            breach.affectedCount = count;
            return this;
        }

        public Builder detectedAt(LocalDateTime dt) {
            breach.detectedAt = dt;
            return this;
        }

        public Builder reportedBy(String user) {
            breach.reportedBy = user;
            return this;
        }

        public Builder assignedTo(String user) {
            breach.assignedTo = user;
            return this;
        }

        public Breach build() {
            return breach;
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
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

    public BreachSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(BreachSeverity severity) {
        this.severity = severity;
    }

    public String getBreachType() {
        return breachType;
    }

    public void setBreachType(String breachType) {
        this.breachType = breachType;
    }

    public String getDataCategories() {
        return dataCategories;
    }

    public void setDataCategories(String dataCategories) {
        this.dataCategories = dataCategories;
    }

    public int getAffectedCount() {
        return affectedCount;
    }

    public void setAffectedCount(int affectedCount) {
        this.affectedCount = affectedCount;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public LocalDateTime getContainedAt() {
        return containedAt;
    }

    public void setContainedAt(LocalDateTime containedAt) {
        this.containedAt = containedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public BreachStatus getStatus() {
        return status;
    }

    public void setStatus(BreachStatus status) {
        this.status = status;
    }

    public String getRootCause() {
        return rootCause;
    }

    public void setRootCause(String rootCause) {
        this.rootCause = rootCause;
    }

    public String getRemediationSteps() {
        return remediationSteps;
    }

    public void setRemediationSteps(String remediationSteps) {
        this.remediationSteps = remediationSteps;
    }

    public boolean isDpbiNotified() {
        return dpbiNotified;
    }

    public void setDpbiNotified(boolean dpbiNotified) {
        this.dpbiNotified = dpbiNotified;
    }

    public LocalDateTime getDpbiNotificationDate() {
        return dpbiNotificationDate;
    }

    public void setDpbiNotificationDate(LocalDateTime dpbiNotificationDate) {
        this.dpbiNotificationDate = dpbiNotificationDate;
    }

    public LocalDateTime getDpbiDeadline() {
        return dpbiDeadline;
    }

    public void setDpbiDeadline(LocalDateTime dpbiDeadline) {
        this.dpbiDeadline = dpbiDeadline;
    }

    public String getDpbiReference() {
        return dpbiReference;
    }

    public void setDpbiReference(String dpbiReference) {
        this.dpbiReference = dpbiReference;
    }

    public boolean isCertinNotified() {
        return certinNotified;
    }

    public void setCertinNotified(boolean certinNotified) {
        this.certinNotified = certinNotified;
    }

    public LocalDateTime getCertinNotificationDate() {
        return certinNotificationDate;
    }

    public void setCertinNotificationDate(LocalDateTime certinNotificationDate) {
        this.certinNotificationDate = certinNotificationDate;
    }

    public LocalDateTime getCertinDeadline() {
        return certinDeadline;
    }

    public void setCertinDeadline(LocalDateTime certinDeadline) {
        this.certinDeadline = certinDeadline;
    }

    public boolean isAffectedPartiesNotified() {
        return affectedPartiesNotified;
    }

    public void setAffectedPartiesNotified(boolean affectedPartiesNotified) {
        this.affectedPartiesNotified = affectedPartiesNotified;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<BreachTimelineEvent> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<BreachTimelineEvent> timeline) {
        this.timeline = timeline;
    }

    // Business logic
    public boolean isDpbiDeadlineApproaching() {
        if (dpbiNotified || dpbiDeadline == null)
            return false;
        return LocalDateTime.now().plusHours(12).isAfter(dpbiDeadline);
    }

    public boolean isDpbiOverdue() {
        if (dpbiNotified || dpbiDeadline == null)
            return false;
        return LocalDateTime.now().isAfter(dpbiDeadline);
    }

    public boolean isCertinOverdue() {
        if (certinNotified || certinDeadline == null)
            return false;
        return LocalDateTime.now().isAfter(certinDeadline);
    }

    public long getHoursSinceDetection() {
        if (detectedAt == null)
            return 0;
        return java.time.Duration.between(detectedAt, LocalDateTime.now()).toHours();
    }
}
