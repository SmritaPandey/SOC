package com.qsdpdp.breach;

import java.time.LocalDateTime;

/**
 * Breach report request DTO
 */
public class BreachRequest {

    private String title;
    private String description;
    private BreachSeverity severity;
    private String breachType;
    private String dataCategories;
    private int affectedCount;
    private LocalDateTime detectedAt;
    private String reportedBy;
    private String assignedTo;

    public BreachRequest() {
        this.severity = BreachSeverity.MEDIUM;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BreachRequest request = new BreachRequest();

        public Builder title(String title) {
            request.title = title;
            return this;
        }

        public Builder description(String desc) {
            request.description = desc;
            return this;
        }

        public Builder severity(BreachSeverity sev) {
            request.severity = sev;
            return this;
        }

        public Builder breachType(String type) {
            request.breachType = type;
            return this;
        }

        public Builder dataCategories(String cats) {
            request.dataCategories = cats;
            return this;
        }

        public Builder affectedCount(int count) {
            request.affectedCount = count;
            return this;
        }

        public Builder detectedAt(LocalDateTime dt) {
            request.detectedAt = dt;
            return this;
        }

        public Builder reportedBy(String user) {
            request.reportedBy = user;
            return this;
        }

        public Builder assignedTo(String user) {
            request.assignedTo = user;
            return this;
        }

        public BreachRequest build() {
            return request;
        }
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
}
