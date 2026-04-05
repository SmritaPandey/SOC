package com.qsdpdp.rights;

import java.time.LocalDateTime;

/**
 * Rights Request entity (Data Subject Request - DSR)
 */
public class RightsRequest {

    private String id;
    private String referenceNumber;
    private String dataPrincipalId;
    private RightType requestType;
    private String description;
    private RequestStatus status;
    private RequestPriority priority;
    private String assignedTo;
    private LocalDateTime receivedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;
    private String response;
    private String evidencePackage;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Joined fields
    private String dataPrincipalName;
    private String dataPrincipalEmail;

    public RightsRequest() {
        this.status = RequestStatus.PENDING;
        this.priority = RequestPriority.NORMAL;
        this.receivedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.deadline = LocalDateTime.now().plusDays(30); // DPDP Act: 30 days
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RightsRequest request = new RightsRequest();

        public Builder id(String id) {
            request.id = id;
            return this;
        }

        public Builder referenceNumber(String ref) {
            request.referenceNumber = ref;
            return this;
        }

        public Builder dataPrincipalId(String id) {
            request.dataPrincipalId = id;
            return this;
        }

        public Builder requestType(RightType type) {
            request.requestType = type;
            return this;
        }

        public Builder description(String desc) {
            request.description = desc;
            return this;
        }

        public Builder priority(RequestPriority p) {
            request.priority = p;
            return this;
        }

        public Builder assignedTo(String user) {
            request.assignedTo = user;
            return this;
        }

        public RightsRequest build() {
            return request;
        }
    }

    // Getters and Setters
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

    public String getDataPrincipalId() {
        return dataPrincipalId;
    }

    public void setDataPrincipalId(String dataPrincipalId) {
        this.dataPrincipalId = dataPrincipalId;
    }

    public RightType getRequestType() {
        return requestType;
    }

    public void setRequestType(RightType requestType) {
        this.requestType = requestType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public RequestPriority getPriority() {
        return priority;
    }

    public void setPriority(RequestPriority priority) {
        this.priority = priority;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getEvidencePackage() {
        return evidencePackage;
    }

    public void setEvidencePackage(String evidencePackage) {
        this.evidencePackage = evidencePackage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getDataPrincipalName() {
        return dataPrincipalName;
    }

    public void setDataPrincipalName(String dataPrincipalName) {
        this.dataPrincipalName = dataPrincipalName;
    }

    public String getDataPrincipalEmail() {
        return dataPrincipalEmail;
    }

    public void setDataPrincipalEmail(String dataPrincipalEmail) {
        this.dataPrincipalEmail = dataPrincipalEmail;
    }

    // Business logic
    public boolean isOverdue() {
        return status != RequestStatus.COMPLETED && status != RequestStatus.REJECTED
                && LocalDateTime.now().isAfter(deadline);
    }

    public long getDaysRemaining() {
        if (deadline == null)
            return 0;
        return java.time.Duration.between(LocalDateTime.now(), deadline).toDays();
    }

    public boolean isApproachingDeadline() {
        return getDaysRemaining() <= 5 && getDaysRemaining() > 0;
    }
}
