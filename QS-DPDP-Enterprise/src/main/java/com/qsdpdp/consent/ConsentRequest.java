package com.qsdpdp.consent;

import java.time.LocalDateTime;

/**
 * Consent collection request DTO
 */
public class ConsentRequest {

    private String dataPrincipalId;
    private String purposeId;
    private String consentMethod;
    private String noticeVersion;
    private String language;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime expiresAt;
    private String actorId;
    private String dataPrincipalName;
    private String purposeName;
    private String consentType;
    private String sector;
    private String retentionPeriod;

    public ConsentRequest() {
        this.language = "en";
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ConsentRequest request = new ConsentRequest();

        public Builder dataPrincipalId(String id) {
            request.dataPrincipalId = id;
            return this;
        }

        public Builder purposeId(String id) {
            request.purposeId = id;
            return this;
        }

        public Builder consentMethod(String method) {
            request.consentMethod = method;
            return this;
        }

        public Builder noticeVersion(String version) {
            request.noticeVersion = version;
            return this;
        }

        public Builder language(String lang) {
            request.language = lang;
            return this;
        }

        public Builder ipAddress(String ip) {
            request.ipAddress = ip;
            return this;
        }

        public Builder userAgent(String ua) {
            request.userAgent = ua;
            return this;
        }

        public Builder expiresAt(LocalDateTime dt) {
            request.expiresAt = dt;
            return this;
        }

        public Builder actorId(String id) {
            request.actorId = id;
            return this;
        }

        public ConsentRequest build() {
            return request;
        }
    }

    // Getters and setters
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
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

    public String getConsentType() {
        return consentType;
    }

    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getRetentionPeriod() {
        return retentionPeriod;
    }

    public void setRetentionPeriod(String retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }
}
