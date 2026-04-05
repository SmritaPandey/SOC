package com.qsdpdp.consent;

import java.time.LocalDateTime;

/**
 * Consent Audit Trail Entry — Immutable Hash-Chained Ledger
 * Creates a tamper-proof blockchain-style audit trail for all consent events.
 * Each entry links to the previous via SHA-256 hash chain.
 *
 * Actions tracked: CONSENT_CREATED, CONSENT_WITHDRAWN, CONSENT_RENEWED,
 *                  GUARDIAN_CONSENT, PREFERENCE_UPDATED, CONSENT_EXPIRED
 *
 * @version 1.0.0
 * @since Phase 2 — Consent Enhancement
 */
public class ConsentAuditEntry {

    private int id;
    private int blockNumber;
    private String consentId;
    private String dataPrincipalId;
    private String action;
    private String actionBy;
    private String details;
    private String previousHash;
    private String currentHash;
    private LocalDateTime timestamp;

    public ConsentAuditEntry() {
        this.actionBy = "SYSTEM";
        this.timestamp = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBlockNumber() { return blockNumber; }
    public void setBlockNumber(int blockNumber) { this.blockNumber = blockNumber; }

    public String getConsentId() { return consentId; }
    public void setConsentId(String consentId) { this.consentId = consentId; }

    public String getDataPrincipalId() { return dataPrincipalId; }
    public void setDataPrincipalId(String dataPrincipalId) { this.dataPrincipalId = dataPrincipalId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActionBy() { return actionBy; }
    public void setActionBy(String actionBy) { this.actionBy = actionBy; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String currentHash) { this.currentHash = currentHash; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    /** Check whether this block correctly chains to the given previous hash */
    public boolean isChainValid(String expectedPrevHash) {
        return previousHash != null && previousHash.equals(expectedPrevHash);
    }
}
