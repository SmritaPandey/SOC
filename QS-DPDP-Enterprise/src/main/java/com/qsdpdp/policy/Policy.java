package com.qsdpdp.policy;

import java.time.LocalDateTime;

/**
 * Policy entity
 */
public class Policy {

    private String id;
    private String code;
    private String name;
    private String description;
    private String category;
    private String content;
    private String version;
    private int versionNumber;
    private PolicyStatus status;
    private String owner;
    private String approver;
    private String reviewedBy;
    private String reviewNotes;
    private String rejectionReason;
    private String parentVersionId;
    private LocalDateTime effectiveDate;
    private LocalDateTime expiryDate;
    private LocalDateTime nextReviewDate;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Policy() {
        this.status = PolicyStatus.DRAFT;
        this.version = "1.0";
        this.versionNumber = 1;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Policy policy = new Policy();

        public Builder id(String id) {
            policy.id = id;
            return this;
        }

        public Builder code(String code) {
            policy.code = code;
            return this;
        }

        public Builder name(String name) {
            policy.name = name;
            return this;
        }

        public Builder description(String desc) {
            policy.description = desc;
            return this;
        }

        public Builder category(String cat) {
            policy.category = cat;
            return this;
        }

        public Builder content(String content) {
            policy.content = content;
            return this;
        }

        public Builder owner(String owner) {
            policy.owner = owner;
            return this;
        }

        public Builder versionNumber(int vn) {
            policy.versionNumber = vn;
            return this;
        }

        public Builder parentVersionId(String pvId) {
            policy.parentVersionId = pvId;
            return this;
        }

        public Policy build() {
            return policy;
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyStatus status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDateTime effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public LocalDateTime getNextReviewDate() {
        return nextReviewDate;
    }

    public void setNextReviewDate(LocalDateTime nextReviewDate) {
        this.nextReviewDate = nextReviewDate;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
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

    public boolean isActive() {
        return status == PolicyStatus.ACTIVE &&
                (expiryDate == null || LocalDateTime.now().isBefore(expiryDate));
    }

    public boolean requiresReview() {
        return nextReviewDate != null && LocalDateTime.now().isAfter(nextReviewDate);
    }

    // Version history & review fields
    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getParentVersionId() { return parentVersionId; }
    public void setParentVersionId(String parentVersionId) { this.parentVersionId = parentVersionId; }
}
