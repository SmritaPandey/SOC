package com.qsdpdp.core;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Asset Entity — Global Data Model
 * Represents any data element tracked by the governance engine.
 * Classified by type (PII / Sensitive / Critical) with full lineage metadata.
 *
 * @version 1.0.0
 * @since Module 1
 */
public class DataAsset {

    public enum AssetType {
        PII("Personally Identifiable Information"),
        SENSITIVE("Sensitive Personal Data"),
        CRITICAL("Critical/Regulated Data"),
        GENERAL("General Business Data"),
        METADATA("System Metadata");

        private final String description;
        AssetType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public enum ClassificationLevel {
        PUBLIC, INTERNAL, CONFIDENTIAL, RESTRICTED, TOP_SECRET
    }

    public enum EncryptionStatus {
        ENCRYPTED, UNENCRYPTED, PARTIALLY_ENCRYPTED, TOKENIZED, MASKED
    }

    private String assetId;
    private String name;
    private String description;
    private AssetType type;
    private String sourceSystem;            // DB name, API endpoint, file path
    private String storageLocation;         // Cloud region, data center, local
    private EncryptionStatus encryptionStatus;
    private ClassificationLevel classificationLevel;
    private String dataCategory;            // Health, Financial, Biometric, etc.
    private String owner;                   // Department or individual
    private String custodian;               // Technical custodian
    private int recordCount;
    private long dataSizeBytes;
    private String retentionPolicy;
    private LocalDateTime retentionExpiry;
    private boolean crossBorderTransfer;
    private String transferDestination;
    private List<String> purposes;          // Linked consent purposes
    private List<String> processingActivities;
    private String lineageSource;           // Where data originated
    private String lineageProcess;          // How data was transformed
    private String lineageDestination;      // Where data flows to
    private Map<String, String> tags;       // Custom metadata tags
    private String discoveredBy;            // Scanner that found this asset
    private LocalDateTime discoveredAt;
    private LocalDateTime lastScanned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;                  // ACTIVE, ARCHIVED, DELETED, QUARANTINED

    public DataAsset() {
        this.assetId = UUID.randomUUID().toString();
        this.type = AssetType.GENERAL;
        this.encryptionStatus = EncryptionStatus.UNENCRYPTED;
        this.classificationLevel = ClassificationLevel.INTERNAL;
        this.status = "ACTIVE";
        this.purposes = new ArrayList<>();
        this.processingActivities = new ArrayList<>();
        this.tags = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public AssetType getType() { return type; }
    public void setType(AssetType type) { this.type = type; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }

    public EncryptionStatus getEncryptionStatus() { return encryptionStatus; }
    public void setEncryptionStatus(EncryptionStatus encryptionStatus) { this.encryptionStatus = encryptionStatus; }

    public ClassificationLevel getClassificationLevel() { return classificationLevel; }
    public void setClassificationLevel(ClassificationLevel classificationLevel) { this.classificationLevel = classificationLevel; }

    public String getDataCategory() { return dataCategory; }
    public void setDataCategory(String dataCategory) { this.dataCategory = dataCategory; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getCustodian() { return custodian; }
    public void setCustodian(String custodian) { this.custodian = custodian; }

    public int getRecordCount() { return recordCount; }
    public void setRecordCount(int recordCount) { this.recordCount = recordCount; }

    public long getDataSizeBytes() { return dataSizeBytes; }
    public void setDataSizeBytes(long dataSizeBytes) { this.dataSizeBytes = dataSizeBytes; }

    public String getRetentionPolicy() { return retentionPolicy; }
    public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }

    public LocalDateTime getRetentionExpiry() { return retentionExpiry; }
    public void setRetentionExpiry(LocalDateTime retentionExpiry) { this.retentionExpiry = retentionExpiry; }

    public boolean isCrossBorderTransfer() { return crossBorderTransfer; }
    public void setCrossBorderTransfer(boolean crossBorderTransfer) { this.crossBorderTransfer = crossBorderTransfer; }

    public String getTransferDestination() { return transferDestination; }
    public void setTransferDestination(String transferDestination) { this.transferDestination = transferDestination; }

    public List<String> getPurposes() { return purposes; }
    public void setPurposes(List<String> purposes) { this.purposes = purposes; }

    public List<String> getProcessingActivities() { return processingActivities; }
    public void setProcessingActivities(List<String> processingActivities) { this.processingActivities = processingActivities; }

    public String getLineageSource() { return lineageSource; }
    public void setLineageSource(String lineageSource) { this.lineageSource = lineageSource; }

    public String getLineageProcess() { return lineageProcess; }
    public void setLineageProcess(String lineageProcess) { this.lineageProcess = lineageProcess; }

    public String getLineageDestination() { return lineageDestination; }
    public void setLineageDestination(String lineageDestination) { this.lineageDestination = lineageDestination; }

    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags; }

    public String getDiscoveredBy() { return discoveredBy; }
    public void setDiscoveredBy(String discoveredBy) { this.discoveredBy = discoveredBy; }

    public LocalDateTime getDiscoveredAt() { return discoveredAt; }
    public void setDiscoveredAt(LocalDateTime discoveredAt) { this.discoveredAt = discoveredAt; }

    public LocalDateTime getLastScanned() { return lastScanned; }
    public void setLastScanned(LocalDateTime lastScanned) { this.lastScanned = lastScanned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // --- Business Logic ---

    /**
     * Check if this asset requires consent for processing per DPDP
     */
    public boolean requiresConsent() {
        return type == AssetType.PII || type == AssetType.SENSITIVE || type == AssetType.CRITICAL;
    }

    /**
     * Check if this asset is adequately protected
     */
    public boolean isAdequatelyProtected() {
        if (type == AssetType.CRITICAL || type == AssetType.SENSITIVE) {
            return encryptionStatus == EncryptionStatus.ENCRYPTED ||
                   encryptionStatus == EncryptionStatus.TOKENIZED;
        }
        return encryptionStatus != EncryptionStatus.UNENCRYPTED;
    }

    /**
     * Check if retention has expired
     */
    public boolean isRetentionExpired() {
        return retentionExpiry != null && retentionExpiry.isBefore(LocalDateTime.now());
    }

    /**
     * Get data lineage as a formatted string
     */
    public String getLineageSummary() {
        return String.format("[%s] → [%s] → [%s]",
                lineageSource != null ? lineageSource : "Unknown",
                lineageProcess != null ? lineageProcess : "Direct",
                lineageDestination != null ? lineageDestination : "Current");
    }

    /**
     * Calculate sensitivity score (0-100) based on type, classification, and encryption
     */
    public int calculateSensitivityScore() {
        int score = 0;
        switch (type) {
            case CRITICAL: score += 40; break;
            case SENSITIVE: score += 30; break;
            case PII: score += 20; break;
            default: score += 5;
        }
        switch (classificationLevel) {
            case TOP_SECRET: score += 40; break;
            case RESTRICTED: score += 30; break;
            case CONFIDENTIAL: score += 20; break;
            case INTERNAL: score += 10; break;
            default: score += 0;
        }
        if (encryptionStatus == EncryptionStatus.UNENCRYPTED) score += 20;
        else if (encryptionStatus == EncryptionStatus.PARTIALLY_ENCRYPTED) score += 10;
        return Math.min(score, 100);
    }
}
