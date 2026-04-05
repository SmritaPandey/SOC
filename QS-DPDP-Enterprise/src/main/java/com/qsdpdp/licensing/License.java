package com.qsdpdp.licensing;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * License entity — represents a product license
 *
 * @version 1.0.0
 * @since Sprint 6
 */
public class License {

    private String id;
    private String licenseKey;
    private LicenseType type;
    private LicenseStatus status;
    private String organizationName;
    private String organizationId;
    private String contactEmail;
    private LocalDateTime activatedAt;
    private LocalDateTime expiresAt;
    private int maxUsers;
    private int currentUsers;
    private String features;    // JSON string of enabled features
    private String agreementId;
    private String activatedBy;
    private String hardwareFingerprint;
    private String signature;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public License() {
        this.id = UUID.randomUUID().toString();
        this.status = LicenseStatus.DEMO;
        this.type = LicenseType.DEMO;
        this.maxUsers = 5;
        this.currentUsers = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ═══════════════════════════════════════════════════════════
    // ENUMS
    // ═══════════════════════════════════════════════════════════

    public enum LicenseType {
        DEMO, STANDARD, PROFESSIONAL, ENTERPRISE
    }

    public enum LicenseStatus {
        DEMO, ACTIVE, EXPIRED, SUSPENDED, REVOKED
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

    public LicenseType getType() { return type; }
    public void setType(LicenseType type) { this.type = type; }

    public LicenseStatus getStatus() { return status; }
    public void setStatus(LicenseStatus status) { this.status = status; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

    public int getCurrentUsers() { return currentUsers; }
    public void setCurrentUsers(int currentUsers) { this.currentUsers = currentUsers; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }

    public String getActivatedBy() { return activatedBy; }
    public void setActivatedBy(String activatedBy) { this.activatedBy = activatedBy; }

    public String getHardwareFingerprint() { return hardwareFingerprint; }
    public void setHardwareFingerprint(String hardwareFingerprint) { this.hardwareFingerprint = hardwareFingerprint; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Business logic
    public boolean isActive() {
        return status == LicenseStatus.ACTIVE && !isExpired();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isDemo() {
        return type == LicenseType.DEMO || status == LicenseStatus.DEMO;
    }

    public int getRemainingDays() {
        if (expiresAt == null) return 0;
        return (int) java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
    }
}
