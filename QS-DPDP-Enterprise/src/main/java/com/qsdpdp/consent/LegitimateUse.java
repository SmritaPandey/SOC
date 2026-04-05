package com.qsdpdp.consent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Legitimate Use — DPDP Act 2023 Section 7
 * Records where personal data is processed WITHOUT explicit consent
 * under legally permitted "legitimate uses" as defined in S.7.
 *
 * Lawful bases per S.7:
 *   - VOLUNTARY: Data principal voluntarily provided data for a specified purpose
 *   - EMPLOYMENT: Purpose related to employment relationship
 *   - STATE_FUNCTION: Performance of state functions under law
 *   - LEGAL_OBLIGATION: Fulfilling obligation to disclose to State
 *   - MEDICAL_EMERGENCY: Medical emergency or public health
 *   - SAFETY: Reasonable purpose to prevent/detect fraud, threats to life
 *
 * @version 1.0.0
 */
public class LegitimateUse {

    private String id;
    private String dataFiduciaryId;
    private String dataFiduciaryName;
    private String dataPrincipalId;
    private String dataPrincipalName;
    private String lawfulBasis;             // VOLUNTARY, EMPLOYMENT, STATE_FUNCTION, LEGAL_OBLIGATION, MEDICAL_EMERGENCY, SAFETY
    private String purposeDescription;      // Detailed purpose of processing
    private List<String> dataCategories;    // What data is being processed
    private String legalReference;          // Specific law/regulation reference
    private LocalDateTime startDate;        // When processing began
    private LocalDateTime endDate;          // When processing should end
    private String retentionPeriod;         // How long data will be retained
    private String status;                  // ACTIVE, EXPIRED, CEASED, CHALLENGED
    private String reviewedBy;              // DPO who reviewed legitimacy
    private LocalDateTime reviewedAt;
    private String reviewNotes;
    private String evidenceReference;       // Link to supporting evidence/documentation
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LegitimateUse() {
        this.id = UUID.randomUUID().toString();
        this.status = "ACTIVE";
        this.dataCategories = new ArrayList<>();
        this.startDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDataFiduciaryId() { return dataFiduciaryId; }
    public void setDataFiduciaryId(String dataFiduciaryId) { this.dataFiduciaryId = dataFiduciaryId; }

    public String getDataFiduciaryName() { return dataFiduciaryName; }
    public void setDataFiduciaryName(String dataFiduciaryName) { this.dataFiduciaryName = dataFiduciaryName; }

    public String getDataPrincipalId() { return dataPrincipalId; }
    public void setDataPrincipalId(String dataPrincipalId) { this.dataPrincipalId = dataPrincipalId; }

    public String getDataPrincipalName() { return dataPrincipalName; }
    public void setDataPrincipalName(String dataPrincipalName) { this.dataPrincipalName = dataPrincipalName; }

    public String getLawfulBasis() { return lawfulBasis; }
    public void setLawfulBasis(String lawfulBasis) { this.lawfulBasis = lawfulBasis; }

    public String getPurposeDescription() { return purposeDescription; }
    public void setPurposeDescription(String purposeDescription) { this.purposeDescription = purposeDescription; }

    public List<String> getDataCategories() { return dataCategories; }
    public void setDataCategories(List<String> dataCategories) { this.dataCategories = dataCategories; }

    public String getLegalReference() { return legalReference; }
    public void setLegalReference(String legalReference) { this.legalReference = legalReference; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public String getRetentionPeriod() { return retentionPeriod; }
    public void setRetentionPeriod(String retentionPeriod) { this.retentionPeriod = retentionPeriod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public String getEvidenceReference() { return evidenceReference; }
    public void setEvidenceReference(String evidenceReference) { this.evidenceReference = evidenceReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
