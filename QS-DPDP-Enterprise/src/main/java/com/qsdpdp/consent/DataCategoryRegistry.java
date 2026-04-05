package com.qsdpdp.consent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Category Registry — DPDP Act 2023
 * Defines categories of personal data (identity, financial, biometric, health, etc.)
 * Consent must explicitly map to these categories for purpose limitation.
 *
 * Reference: DPDP Rules 2025, KPMG SARAL data classification
 * @version 1.0.0
 */
public class DataCategoryRegistry {

    private String id;
    private String code;                    // e.g. "IDENTITY", "FINANCIAL", "BIOMETRIC"
    private String name;                    // e.g. "Identity Data"
    private String description;             // Detailed description
    private String sensitivityLevel;        // LOW, MEDIUM, HIGH, CRITICAL
    private String dpdpClassification;      // PERSONAL, SENSITIVE_PERSONAL, CHILD
    private List<String> sectorApplicability; // ["banking","healthcare","telecom"]
    private List<String> dataElements;      // ["name","aadhaar","pan","email"]
    private String retentionGuideline;      // Recommended retention period
    private String regulatoryReference;     // DPDP section reference
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DataCategoryRegistry() {
        this.id = UUID.randomUUID().toString();
        this.isActive = true;
        this.sensitivityLevel = "MEDIUM";
        this.dpdpClassification = "PERSONAL";
        this.sectorApplicability = new ArrayList<>();
        this.dataElements = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSensitivityLevel() { return sensitivityLevel; }
    public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }

    public String getDpdpClassification() { return dpdpClassification; }
    public void setDpdpClassification(String dpdpClassification) { this.dpdpClassification = dpdpClassification; }

    public List<String> getSectorApplicability() { return sectorApplicability; }
    public void setSectorApplicability(List<String> sectorApplicability) { this.sectorApplicability = sectorApplicability; }

    public List<String> getDataElements() { return dataElements; }
    public void setDataElements(List<String> dataElements) { this.dataElements = dataElements; }

    public String getRetentionGuideline() { return retentionGuideline; }
    public void setRetentionGuideline(String retentionGuideline) { this.retentionGuideline = retentionGuideline; }

    public String getRegulatoryReference() { return regulatoryReference; }
    public void setRegulatoryReference(String regulatoryReference) { this.regulatoryReference = regulatoryReference; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
