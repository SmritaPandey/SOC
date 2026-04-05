package com.qsdpdp.core;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Fiduciary Entity — DPDP Act 2023 Section 2(i)
 * Represents any person/organization that determines the purpose and means of processing personal data.
 * Maps to the mandatory Global Data Model entity.
 *
 * @version 1.0.0
 * @since Module 1
 */
public class DataFiduciary {

    private String fiduciaryId;
    private String organizationName;
    private String organizationType;       // Corporation, LLP, Government, Startup, NGO
    private String sector;                 // BFSI, Healthcare, Telecom, E-commerce, Government, etc.
    private String riskCategory;           // SDF (Significant Data Fiduciary) or NORMAL
    private String registeredAddress;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private double complianceScore;        // 0.0 to 100.0
    private String registrationNumber;     // CIN / GSTIN / LLP-IN
    private String dpoName;               // DPDP mandated DPO
    private String dpoEmail;
    private String dpoPhone;
    private String grievanceOfficerName;
    private String grievanceOfficerEmail;
    private String websiteUrl;
    private boolean isSignificantDataFiduciary;
    private LocalDateTime registeredAt;
    private LocalDateTime lastAuditDate;
    private LocalDateTime nextAuditDue;
    private String status;                 // ACTIVE, SUSPENDED, REVOKED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataFiduciary() {
        this.fiduciaryId = UUID.randomUUID().toString();
        this.status = "ACTIVE";
        this.riskCategory = "NORMAL";
        this.complianceScore = 0.0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public String getFiduciaryId() { return fiduciaryId; }
    public void setFiduciaryId(String fiduciaryId) { this.fiduciaryId = fiduciaryId; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getOrganizationType() { return organizationType; }
    public void setOrganizationType(String organizationType) { this.organizationType = organizationType; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
        this.isSignificantDataFiduciary = "SDF".equalsIgnoreCase(riskCategory);
    }

    public String getRegisteredAddress() { return registeredAddress; }
    public void setRegisteredAddress(String registeredAddress) { this.registeredAddress = registeredAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }

    public double getComplianceScore() { return complianceScore; }
    public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getDpoName() { return dpoName; }
    public void setDpoName(String dpoName) { this.dpoName = dpoName; }

    public String getDpoEmail() { return dpoEmail; }
    public void setDpoEmail(String dpoEmail) { this.dpoEmail = dpoEmail; }

    public String getDpoPhone() { return dpoPhone; }
    public void setDpoPhone(String dpoPhone) { this.dpoPhone = dpoPhone; }

    public String getGrievanceOfficerName() { return grievanceOfficerName; }
    public void setGrievanceOfficerName(String n) { this.grievanceOfficerName = n; }

    public String getGrievanceOfficerEmail() { return grievanceOfficerEmail; }
    public void setGrievanceOfficerEmail(String e) { this.grievanceOfficerEmail = e; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public boolean isSignificantDataFiduciary() { return isSignificantDataFiduciary; }
    public void setSignificantDataFiduciary(boolean sdf) { this.isSignificantDataFiduciary = sdf; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public LocalDateTime getLastAuditDate() { return lastAuditDate; }
    public void setLastAuditDate(LocalDateTime lastAuditDate) { this.lastAuditDate = lastAuditDate; }

    public LocalDateTime getNextAuditDue() { return nextAuditDue; }
    public void setNextAuditDue(LocalDateTime nextAuditDue) { this.nextAuditDue = nextAuditDue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Check if this fiduciary requires enhanced compliance measures per DPDP Section 10
     */
    public boolean requiresEnhancedCompliance() {
        return isSignificantDataFiduciary || "SDF".equalsIgnoreCase(riskCategory);
    }

    /**
     * Check if compliance score meets minimum threshold
     */
    public boolean isCompliant(double threshold) {
        return complianceScore >= threshold;
    }

    /**
     * Check if audit is overdue
     */
    public boolean isAuditOverdue() {
        return nextAuditDue != null && nextAuditDue.isBefore(LocalDateTime.now());
    }
}
