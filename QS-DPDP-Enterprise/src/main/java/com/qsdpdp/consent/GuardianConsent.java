package com.qsdpdp.consent;

import java.time.LocalDateTime;

/**
 * Guardian Consent — DPDP Act Section 9, Rule 11
 * Handles verifiable guardian consent for:
 *   - Children under 18 years of age
 *   - Persons with disabilities (RPwD Act 2016)
 * 
 * Requires KYC verification of the guardian before consent is valid.
 * Supports Aadhaar, PAN, Passport, Voter ID verification methods.
 *
 * @version 1.0.0
 * @since Phase 2 — Consent Enhancement
 */
public class GuardianConsent {

    private String id;
    private String childPrincipalId;
    private String guardianPrincipalId;
    private String childName;
    private int childAge;
    private String guardianName;
    private String guardianRelationship;      // Father, Mother, Legal Guardian, Court Appointed
    private String guardianIdType;            // AADHAAR, PAN, PASSPORT, VOTER_ID
    private String guardianIdNumber;
    private boolean guardianKycVerified;
    private LocalDateTime guardianKycDate;
    private boolean isDisability;
    private String disabilityType;            // null if not applicable
    private String consentId;                 // linked consent record
    private String purposeId;
    private String status;                    // pending, verified, rejected, withdrawn
    private String verificationMethod;        // OTP, AADHAAR_OTP, VIDEO_KYC
    private LocalDateTime verificationDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GuardianConsent() {
        this.status = "pending";
        this.verificationMethod = "OTP";
        this.guardianKycVerified = false;
        this.isDisability = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChildPrincipalId() { return childPrincipalId; }
    public void setChildPrincipalId(String childPrincipalId) { this.childPrincipalId = childPrincipalId; }

    public String getGuardianPrincipalId() { return guardianPrincipalId; }
    public void setGuardianPrincipalId(String guardianPrincipalId) { this.guardianPrincipalId = guardianPrincipalId; }

    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }

    public int getChildAge() { return childAge; }
    public void setChildAge(int childAge) { this.childAge = childAge; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    public String getGuardianRelationship() { return guardianRelationship; }
    public void setGuardianRelationship(String guardianRelationship) { this.guardianRelationship = guardianRelationship; }

    public String getGuardianIdType() { return guardianIdType; }
    public void setGuardianIdType(String guardianIdType) { this.guardianIdType = guardianIdType; }

    public String getGuardianIdNumber() { return guardianIdNumber; }
    public void setGuardianIdNumber(String guardianIdNumber) { this.guardianIdNumber = guardianIdNumber; }

    public boolean isGuardianKycVerified() { return guardianKycVerified; }
    public void setGuardianKycVerified(boolean guardianKycVerified) { this.guardianKycVerified = guardianKycVerified; }

    public LocalDateTime getGuardianKycDate() { return guardianKycDate; }
    public void setGuardianKycDate(LocalDateTime guardianKycDate) { this.guardianKycDate = guardianKycDate; }

    public boolean isDisability() { return isDisability; }
    public void setDisability(boolean disability) { isDisability = disability; }

    public String getDisabilityType() { return disabilityType; }
    public void setDisabilityType(String disabilityType) { this.disabilityType = disabilityType; }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }

    public String getPurposeId() { return purposeId; }
    public void setPurposeId(String purposeId) { this.purposeId = purposeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVerificationMethod() { return verificationMethod; }
    public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }

    public LocalDateTime getVerificationDate() { return verificationDate; }
    public void setVerificationDate(LocalDateTime verificationDate) { this.verificationDate = verificationDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** Returns true if the child is a minor (under 18) */
    public boolean isMinor() { return childAge < 18; }

    /** Returns true if this is a disability guardianship */
    public boolean isDisabilityGuardianship() { return isDisability && disabilityType != null; }
}
