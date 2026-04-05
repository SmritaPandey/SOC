package com.qsdpdp.consent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consent Delegation — DPDP Act 2023 Section 9
 * Supports guardian consent for minors and authorized representative management.
 * Enables delegation of consent authority from one Data Principal to another.
 *
 * @version 1.0.0
 */
public class ConsentDelegation {

    private String id;
    private String delegatorId;             // Data Principal granting delegation
    private String delegatorName;
    private String delegateId;              // Person receiving delegation authority
    private String delegateName;
    private String delegateType;            // GUARDIAN, AUTHORIZED_REPRESENTATIVE, LEGAL_HEIR, POWER_OF_ATTORNEY
    private String scope;                   // ALL, SPECIFIC_PURPOSES, READ_ONLY
    private String scopeDetails;            // JSON: specific purpose IDs if scope is SPECIFIC_PURPOSES
    private String relationship;            // Parent, Legal Guardian, Spouse, etc.
    private String idProofType;             // Aadhaar, PAN, Passport, etc.
    private String idProofNumber;           // Masked ID proof number
    private boolean kycVerified;            // Whether delegate has been KYC verified
    private LocalDateTime kycVerifiedAt;
    private String verificationMethod;      // OTP, VIDEO_KYC, IN_PERSON, DOCUMENT
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String status;                  // ACTIVE, EXPIRED, REVOKED, PENDING_VERIFICATION
    private String revokedBy;
    private String revokedReason;
    private LocalDateTime revokedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConsentDelegation() {
        this.id = UUID.randomUUID().toString();
        this.status = "PENDING_VERIFICATION";
        this.kycVerified = false;
        this.validFrom = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDelegatorId() { return delegatorId; }
    public void setDelegatorId(String delegatorId) { this.delegatorId = delegatorId; }

    public String getDelegatorName() { return delegatorName; }
    public void setDelegatorName(String delegatorName) { this.delegatorName = delegatorName; }

    public String getDelegateId() { return delegateId; }
    public void setDelegateId(String delegateId) { this.delegateId = delegateId; }

    public String getDelegateName() { return delegateName; }
    public void setDelegateName(String delegateName) { this.delegateName = delegateName; }

    public String getDelegateType() { return delegateType; }
    public void setDelegateType(String delegateType) { this.delegateType = delegateType; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getScopeDetails() { return scopeDetails; }
    public void setScopeDetails(String scopeDetails) { this.scopeDetails = scopeDetails; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getIdProofType() { return idProofType; }
    public void setIdProofType(String idProofType) { this.idProofType = idProofType; }

    public String getIdProofNumber() { return idProofNumber; }
    public void setIdProofNumber(String idProofNumber) { this.idProofNumber = idProofNumber; }

    public boolean isKycVerified() { return kycVerified; }
    public void setKycVerified(boolean kycVerified) { this.kycVerified = kycVerified; }

    public LocalDateTime getKycVerifiedAt() { return kycVerifiedAt; }
    public void setKycVerifiedAt(LocalDateTime kycVerifiedAt) { this.kycVerifiedAt = kycVerifiedAt; }

    public String getVerificationMethod() { return verificationMethod; }
    public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }

    public String getRevokedReason() { return revokedReason; }
    public void setRevokedReason(String revokedReason) { this.revokedReason = revokedReason; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
