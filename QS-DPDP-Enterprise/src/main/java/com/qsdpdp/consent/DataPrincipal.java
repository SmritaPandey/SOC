package com.qsdpdp.consent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Principal Entity — DPDP Act 2023 Section 2(j)
 * Any individual to whom personal data relates.
 * Aligned with Global Data Model specification.
 *
 * @version 2.0.0
 * @since Module 2
 */
public class DataPrincipal {

    private String id;
    private String externalId;
    private String name;
    private String email;
    private String phone;
    private boolean isChild;
    private String guardianId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Global Data Model Fields ---
    private Map<String, String> identifiers;     // Aadhaar-masked, mobile, email, PAN-masked
    private String consentProfileId;              // Links to consent profile
    private List<String> grievanceHistory;         // Grievance IDs filed by this principal
    private String sector;                         // Primary sector of interaction

    // Statistics
    private int totalConsents;
    private int activeConsents;
    private int pendingRights;

    public DataPrincipal() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.identifiers = new HashMap<>();
        this.grievanceHistory = new ArrayList<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isChild() {
        return isChild;
    }

    public void setChild(boolean child) {
        isChild = child;
    }

    public String getGuardianId() {
        return guardianId;
    }

    public void setGuardianId(String guardianId) {
        this.guardianId = guardianId;
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

    public int getPendingRights() {
        return pendingRights;
    }

    public void setPendingRights(int pendingRights) {
        this.pendingRights = pendingRights;
    }

    // --- Global Data Model Getters/Setters ---

    public Map<String, String> getIdentifiers() { return identifiers; }
    public void setIdentifiers(Map<String, String> identifiers) { this.identifiers = identifiers; }
    public void addIdentifier(String type, String value) {
        if (identifiers == null) identifiers = new HashMap<>();
        identifiers.put(type, value);
    }

    public String getConsentProfileId() { return consentProfileId; }
    public void setConsentProfileId(String consentProfileId) { this.consentProfileId = consentProfileId; }

    public List<String> getGrievanceHistory() { return grievanceHistory; }
    public void setGrievanceHistory(List<String> grievanceHistory) { this.grievanceHistory = grievanceHistory; }
    public void addGrievance(String grievanceId) {
        if (grievanceHistory == null) grievanceHistory = new ArrayList<>();
        grievanceHistory.add(grievanceId);
    }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
}
