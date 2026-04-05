package com.qsdpdp.consent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Access Log — DPDP Act 2023 / DPDP Rules 2025
 * Tracks every access to personal data under a consent.
 * Enables Data Principals to see which organizations accessed their data.
 * Required for: Privacy Wallet display, audit compliance, breach impact analysis.
 *
 * Retention: minimum 1 year per DPDP Rules 2025
 * @version 1.0.0
 */
public class DataAccessLog {

    private String id;
    private String consentId;               // Consent under which access was made
    private String dataPrincipalId;          // Whose data was accessed
    private String accessorId;              // Who accessed (user/system ID)
    private String accessorName;            // Accessor display name
    private String accessorOrganization;    // Accessor's organization
    private String accessorRole;            // FIDUCIARY, PROCESSOR, EMPLOYEE, SYSTEM
    private String purpose;                 // Purpose of access
    private String dataCategory;            // What data category was accessed
    private String dataElements;            // Specific elements (JSON list)
    private String accessType;              // READ, WRITE, UPDATE, DELETE, EXPORT, SHARE
    private String accessChannel;           // API, WEB, MOBILE, BATCH, INTERNAL
    private String ipAddress;
    private String userAgent;
    private boolean consentVerified;        // Whether consent was validated before access
    private String resultStatus;            // SUCCESS, DENIED, PARTIAL
    private String denialReason;            // If denied, why
    private LocalDateTime accessedAt;
    private LocalDateTime createdAt;

    public DataAccessLog() {
        this.id = UUID.randomUUID().toString();
        this.consentVerified = false;
        this.resultStatus = "SUCCESS";
        this.accessedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }

    public String getDataPrincipalId() { return dataPrincipalId; }
    public void setDataPrincipalId(String dataPrincipalId) { this.dataPrincipalId = dataPrincipalId; }

    public String getAccessorId() { return accessorId; }
    public void setAccessorId(String accessorId) { this.accessorId = accessorId; }

    public String getAccessorName() { return accessorName; }
    public void setAccessorName(String accessorName) { this.accessorName = accessorName; }

    public String getAccessorOrganization() { return accessorOrganization; }
    public void setAccessorOrganization(String accessorOrganization) { this.accessorOrganization = accessorOrganization; }

    public String getAccessorRole() { return accessorRole; }
    public void setAccessorRole(String accessorRole) { this.accessorRole = accessorRole; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getDataCategory() { return dataCategory; }
    public void setDataCategory(String dataCategory) { this.dataCategory = dataCategory; }

    public String getDataElements() { return dataElements; }
    public void setDataElements(String dataElements) { this.dataElements = dataElements; }

    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }

    public String getAccessChannel() { return accessChannel; }
    public void setAccessChannel(String accessChannel) { this.accessChannel = accessChannel; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public boolean isConsentVerified() { return consentVerified; }
    public void setConsentVerified(boolean consentVerified) { this.consentVerified = consentVerified; }

    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }

    public String getDenialReason() { return denialReason; }
    public void setDenialReason(String denialReason) { this.denialReason = denialReason; }

    public LocalDateTime getAccessedAt() { return accessedAt; }
    public void setAccessedAt(LocalDateTime accessedAt) { this.accessedAt = accessedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
