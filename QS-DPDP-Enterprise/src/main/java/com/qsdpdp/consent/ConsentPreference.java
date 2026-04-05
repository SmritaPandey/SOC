package com.qsdpdp.consent;

import java.time.LocalDateTime;

/**
 * Granular Consent Preference — per-purpose, per-data-category consent model.
 * DPDP Act 2023 Section 6 — allows data principals to control specific
 * data categories, third-party sharing, and cross-border transfer per purpose.
 *
 * @version 1.0.0
 * @since Phase 2 — Consent Enhancement
 */
public class ConsentPreference {

    private String id;
    private String consentId;
    private String dataPrincipalId;
    private String purposeId;
    private String dataCategory;
    private boolean allowed;
    private String processingBasis;        // consent, legal_obligation, legitimate_interest, contract
    private boolean thirdPartySharing;
    private boolean crossBorderTransfer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConsentPreference() {
        this.allowed = true;
        this.processingBasis = "consent";
        this.thirdPartySharing = false;
        this.crossBorderTransfer = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }

    public String getDataPrincipalId() { return dataPrincipalId; }
    public void setDataPrincipalId(String dataPrincipalId) { this.dataPrincipalId = dataPrincipalId; }

    public String getPurposeId() { return purposeId; }
    public void setPurposeId(String purposeId) { this.purposeId = purposeId; }

    public String getDataCategory() { return dataCategory; }
    public void setDataCategory(String dataCategory) { this.dataCategory = dataCategory; }

    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }

    public String getProcessingBasis() { return processingBasis; }
    public void setProcessingBasis(String processingBasis) { this.processingBasis = processingBasis; }

    public boolean isThirdPartySharing() { return thirdPartySharing; }
    public void setThirdPartySharing(boolean thirdPartySharing) { this.thirdPartySharing = thirdPartySharing; }

    public boolean isCrossBorderTransfer() { return crossBorderTransfer; }
    public void setCrossBorderTransfer(boolean crossBorderTransfer) { this.crossBorderTransfer = crossBorderTransfer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
