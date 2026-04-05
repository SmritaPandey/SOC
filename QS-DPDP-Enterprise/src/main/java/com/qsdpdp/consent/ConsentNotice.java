package com.qsdpdp.consent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Consent Notice — DPDP Act 2023 Section 5
 * Every consent must be preceded by a notice itemizing:
 *   - Personal data collected and purpose
 *   - How to exercise rights (access, correction, erasure, grievance)
 *   - How to withdraw consent
 *   - How to file a complaint with the DPBI
 *
 * Notices must be in clear, plain language; available in English and
 * any of the 22 Eighth Schedule languages.
 *
 * Reference: DPDP Rules 2025 Rule 3, EY implementation guidelines
 * @version 1.0.0
 */
public class ConsentNotice {

    private String id;
    private String version;                 // e.g. "2.1"
    private String organizationId;
    private String organizationName;
    private String title;                   // Notice title
    private String sectorCode;             // Banking, Healthcare, etc.
    private List<String> purposes;          // Purpose IDs covered
    private List<String> dataCategories;    // Data categories collected
    private String retentionPolicy;         // How long data will be retained
    private String dpoName;
    private String dpoEmail;
    private String dpoPhone;
    private String grievanceOfficerName;
    private String grievanceOfficerEmail;
    private String withdrawalUrl;           // URL/mechanism for withdrawal
    private String rightsUrl;               // URL for exercising rights
    private String dpbiComplaintUrl;        // URL for DPBI complaint
    private String language;                // Language code
    private String content;                 // Full notice text (HTML/Markdown)
    private String contentPlain;            // Plain text version for accessibility
    private boolean isActive;
    private boolean isCurrentVersion;       // Whether this is the latest version
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConsentNotice() {
        this.id = UUID.randomUUID().toString();
        this.version = "1.0";
        this.isActive = true;
        this.isCurrentVersion = true;
        this.language = "en";
        this.purposes = new ArrayList<>();
        this.dataCategories = new ArrayList<>();
        this.effectiveFrom = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSectorCode() { return sectorCode; }
    public void setSectorCode(String sectorCode) { this.sectorCode = sectorCode; }

    public List<String> getPurposes() { return purposes; }
    public void setPurposes(List<String> purposes) { this.purposes = purposes; }

    public List<String> getDataCategories() { return dataCategories; }
    public void setDataCategories(List<String> dataCategories) { this.dataCategories = dataCategories; }

    public String getRetentionPolicy() { return retentionPolicy; }
    public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }

    public String getDpoName() { return dpoName; }
    public void setDpoName(String dpoName) { this.dpoName = dpoName; }

    public String getDpoEmail() { return dpoEmail; }
    public void setDpoEmail(String dpoEmail) { this.dpoEmail = dpoEmail; }

    public String getDpoPhone() { return dpoPhone; }
    public void setDpoPhone(String dpoPhone) { this.dpoPhone = dpoPhone; }

    public String getGrievanceOfficerName() { return grievanceOfficerName; }
    public void setGrievanceOfficerName(String grievanceOfficerName) { this.grievanceOfficerName = grievanceOfficerName; }

    public String getGrievanceOfficerEmail() { return grievanceOfficerEmail; }
    public void setGrievanceOfficerEmail(String grievanceOfficerEmail) { this.grievanceOfficerEmail = grievanceOfficerEmail; }

    public String getWithdrawalUrl() { return withdrawalUrl; }
    public void setWithdrawalUrl(String withdrawalUrl) { this.withdrawalUrl = withdrawalUrl; }

    public String getRightsUrl() { return rightsUrl; }
    public void setRightsUrl(String rightsUrl) { this.rightsUrl = rightsUrl; }

    public String getDpbiComplaintUrl() { return dpbiComplaintUrl; }
    public void setDpbiComplaintUrl(String dpbiComplaintUrl) { this.dpbiComplaintUrl = dpbiComplaintUrl; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getContentPlain() { return contentPlain; }
    public void setContentPlain(String contentPlain) { this.contentPlain = contentPlain; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isCurrentVersion() { return isCurrentVersion; }
    public void setCurrentVersion(boolean currentVersion) { isCurrentVersion = currentVersion; }

    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
