package com.qsdpdp.consent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Consent Object Entity — DPDP Act 2023 Section 6
 * Represents a data principal's consent with full lifecycle.
 * Aligned with Global Data Model specification.
 *
 * @version 2.0.0
 * @since Module 2
 */
public class Consent {

    private String id;
    private String dataPrincipalId;
    private String purposeId;
    private ConsentStatus status;
    private String consentMethod;
    private String noticeVersion;
    private String language;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime collectedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime withdrawnAt;
    private String withdrawalReason;
    private String withdrawnBy;
    private String hash;
    private String prevHash;
    private String createdBy;

    // --- Universal Consent Manager Fields (DPDP Act 2023 + Rules 2025) ---
    private List<String> dataCategories;     // Health, Financial, Biometric, Contact, etc.
    private String channel;                   // web, mobile, api, desktop, offline
    private String dataFiduciaryId;           // S.8 — Data Fiduciary accountability
    private String dataFiduciaryName;         // Org name for display
    private String consentType;               // EXPLICIT, DEEMED, LEGITIMATE_INTEREST
    private String retentionPeriod;           // e.g. "365 days", "7 years"
    private String digitalSignature;          // SHA-256 signature for legal evidence
    private String noticeId;                  // S.5 — linked consent notice version
    private LocalDateTime modifiedAt;         // Modification timestamp
    private String modifiedBy;                // Who modified the consent

    // --- Global-Grade Consent Orchestration Fields ---
    private String processingActions;         // read/write/share/profile
    private String jurisdictionTag;           // IN, cross-border
    private String retentionPolicyId;         // Linked retention policy
    private String uxSnapshotHash;            // Hash of UX at time of consent
    private int versionNumber;                // Consent version (immutable chain)
    private String bundleId;                  // Purpose bundle reference
    private String parentConsentId;           // Parent for version chain

    // Joined data
    private String dataPrincipalName;
    private String purposeName;

    public Consent() {
        this.status = ConsentStatus.ACTIVE;
        this.language = "en";
        this.consentType = "EXPLICIT";
        this.collectedAt = LocalDateTime.now();
        this.dataCategories = new ArrayList<>();
        this.jurisdictionTag = "IN";
        this.processingActions = "read";
        this.versionNumber = 1;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Consent consent = new Consent();

        public Builder id(String id) {
            consent.id = id;
            return this;
        }

        public Builder dataPrincipalId(String id) {
            consent.dataPrincipalId = id;
            return this;
        }

        public Builder purposeId(String id) {
            consent.purposeId = id;
            return this;
        }

        public Builder status(ConsentStatus status) {
            consent.status = status;
            return this;
        }

        public Builder consentMethod(String method) {
            consent.consentMethod = method;
            return this;
        }

        public Builder noticeVersion(String version) {
            consent.noticeVersion = version;
            return this;
        }

        public Builder language(String lang) {
            consent.language = lang;
            return this;
        }

        public Builder ipAddress(String ip) {
            consent.ipAddress = ip;
            return this;
        }

        public Builder userAgent(String ua) {
            consent.userAgent = ua;
            return this;
        }

        public Builder expiresAt(LocalDateTime dt) {
            consent.expiresAt = dt;
            return this;
        }

        public Builder hash(String hash) {
            consent.hash = hash;
            return this;
        }

        public Builder prevHash(String hash) {
            consent.prevHash = hash;
            return this;
        }

        public Builder createdBy(String user) {
            consent.createdBy = user;
            return this;
        }

        public Builder dataFiduciaryId(String id) {
            consent.dataFiduciaryId = id;
            return this;
        }

        public Builder consentType(String type) {
            consent.consentType = type;
            return this;
        }

        public Builder retentionPeriod(String period) {
            consent.retentionPeriod = period;
            return this;
        }

        public Builder digitalSignature(String sig) {
            consent.digitalSignature = sig;
            return this;
        }

        public Builder noticeId(String id) {
            consent.noticeId = id;
            return this;
        }

        public Builder channel(String ch) {
            consent.channel = ch;
            return this;
        }

        public Builder processingActions(String actions) {
            consent.processingActions = actions;
            return this;
        }

        public Builder jurisdictionTag(String tag) {
            consent.jurisdictionTag = tag;
            return this;
        }

        public Builder retentionPolicyId(String id) {
            consent.retentionPolicyId = id;
            return this;
        }

        public Builder uxSnapshotHash(String hash) {
            consent.uxSnapshotHash = hash;
            return this;
        }

        public Builder versionNumber(int v) {
            consent.versionNumber = v;
            return this;
        }

        public Builder bundleId(String id) {
            consent.bundleId = id;
            return this;
        }

        public Builder parentConsentId(String id) {
            consent.parentConsentId = id;
            return this;
        }

        public Builder dataPrincipalName(String name) {
            consent.dataPrincipalName = name;
            return this;
        }

        public Builder purposeName(String name) {
            consent.purposeName = name;
            return this;
        }

        public Consent build() {
            return consent;
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDataPrincipalId() {
        return dataPrincipalId;
    }

    public void setDataPrincipalId(String dataPrincipalId) {
        this.dataPrincipalId = dataPrincipalId;
    }

    public String getPurposeId() {
        return purposeId;
    }

    public void setPurposeId(String purposeId) {
        this.purposeId = purposeId;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public void setStatus(ConsentStatus status) {
        this.status = status;
    }

    public String getConsentMethod() {
        return consentMethod;
    }

    public void setConsentMethod(String consentMethod) {
        this.consentMethod = consentMethod;
    }

    public String getNoticeVersion() {
        return noticeVersion;
    }

    public void setNoticeVersion(String noticeVersion) {
        this.noticeVersion = noticeVersion;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public String getWithdrawalReason() {
        return withdrawalReason;
    }

    public void setWithdrawalReason(String withdrawalReason) {
        this.withdrawalReason = withdrawalReason;
    }

    public String getWithdrawnBy() {
        return withdrawnBy;
    }

    public void setWithdrawnBy(String withdrawnBy) {
        this.withdrawnBy = withdrawnBy;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getDataPrincipalName() {
        return dataPrincipalName;
    }

    public void setDataPrincipalName(String dataPrincipalName) {
        this.dataPrincipalName = dataPrincipalName;
    }

    public String getPurposeName() {
        return purposeName;
    }

    public void setPurposeName(String purposeName) {
        this.purposeName = purposeName;
    }

    public boolean isActive() {
        return status == ConsentStatus.ACTIVE;
    }

    public boolean isWithdrawn() {
        return status == ConsentStatus.WITHDRAWN;
    }

    public boolean isExpired() {
        return status == ConsentStatus.EXPIRED || (expiresAt != null && expiresAt.isBefore(LocalDateTime.now()));
    }

    // --- Universal Consent Manager Getters/Setters ---

    public List<String> getDataCategories() { return dataCategories; }
    public void setDataCategories(List<String> dataCategories) { this.dataCategories = dataCategories; }
    public void addDataCategory(String category) {
        if (dataCategories == null) dataCategories = new ArrayList<>();
        dataCategories.add(category);
    }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getDataFiduciaryId() { return dataFiduciaryId; }
    public void setDataFiduciaryId(String dataFiduciaryId) { this.dataFiduciaryId = dataFiduciaryId; }

    public String getDataFiduciaryName() { return dataFiduciaryName; }
    public void setDataFiduciaryName(String dataFiduciaryName) { this.dataFiduciaryName = dataFiduciaryName; }

    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }

    public String getRetentionPeriod() { return retentionPeriod; }
    public void setRetentionPeriod(String retentionPeriod) { this.retentionPeriod = retentionPeriod; }

    public String getDigitalSignature() { return digitalSignature; }
    public void setDigitalSignature(String digitalSignature) { this.digitalSignature = digitalSignature; }

    public String getNoticeId() { return noticeId; }
    public void setNoticeId(String noticeId) { this.noticeId = noticeId; }

    public LocalDateTime getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(LocalDateTime modifiedAt) { this.modifiedAt = modifiedAt; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }

    // --- Global-Grade Consent Orchestration Getters/Setters ---

    public String getProcessingActions() { return processingActions; }
    public void setProcessingActions(String processingActions) { this.processingActions = processingActions; }

    public String getJurisdictionTag() { return jurisdictionTag; }
    public void setJurisdictionTag(String jurisdictionTag) { this.jurisdictionTag = jurisdictionTag; }

    public String getRetentionPolicyId() { return retentionPolicyId; }
    public void setRetentionPolicyId(String retentionPolicyId) { this.retentionPolicyId = retentionPolicyId; }

    public String getUxSnapshotHash() { return uxSnapshotHash; }
    public void setUxSnapshotHash(String uxSnapshotHash) { this.uxSnapshotHash = uxSnapshotHash; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getBundleId() { return bundleId; }
    public void setBundleId(String bundleId) { this.bundleId = bundleId; }

    public String getParentConsentId() { return parentConsentId; }
    public void setParentConsentId(String parentConsentId) { this.parentConsentId = parentConsentId; }
}
