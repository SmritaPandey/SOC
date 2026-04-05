package com.qsdpdp.consent;

import java.time.LocalDateTime;

/**
 * Purpose entity for consent purposes
 */
public class Purpose {

    private String id;
    private String code;
    private String name;
    private String description;
    private String legalBasis;
    private String dataCategories;
    private int retentionPeriodDays;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Statistics
    private int totalConsents;
    private int activeConsents;

    public Purpose() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }

    public String getDataCategories() {
        return dataCategories;
    }

    public void setDataCategories(String dataCategories) {
        this.dataCategories = dataCategories;
    }

    public int getRetentionPeriodDays() {
        return retentionPeriodDays;
    }

    public void setRetentionPeriodDays(int retentionPeriodDays) {
        this.retentionPeriodDays = retentionPeriodDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getTotalConsents() {
        return totalConsents;
    }

    public void setTotalConsents(int totalConsents) {
        this.totalConsents = totalConsents;
    }

    public int getActiveConsents() {
        return activeConsents;
    }

    public void setActiveConsents(int activeConsents) {
        this.activeConsents = activeConsents;
    }
}
