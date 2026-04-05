package com.qsdpdp.consent;

import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced Consent Statistics — Dashboard Analytics
 * Provides comprehensive compliance scoring, sector breakdown,
 * language distribution, and trend data for the consent module.
 *
 * @version 2.0.0
 * @since Phase 2 — Consent Enhancement
 */
public class ConsentStatistics {

    // --- Core Counts ---
    private int totalConsents;
    private int activeConsents;
    private int withdrawnConsents;
    private int expiredConsents;

    // --- Rates ---
    private double activeRate;
    private double withdrawalRate;
    private double complianceScore;

    // --- Recent Activity (30 days) ---
    private int consentsLast30Days;
    private int withdrawalsLast30Days;
    private int renewalsLast30Days;

    // --- Guardian Consent ---
    private int totalGuardianConsents;
    private int pendingGuardianConsents;
    private int verifiedGuardianConsents;

    // --- Granular Preferences ---
    private int totalPreferences;
    private int allowedPreferences;
    private int deniedPreferences;
    private double thirdPartySharingRate;
    private double crossBorderRate;

    // --- Sector Breakdown ---
    private Map<String, Integer> sectorDistribution;

    // --- Language Distribution ---
    private Map<String, Integer> languageDistribution;

    // --- Audit Trail ---
    private int totalAuditEntries;
    private boolean chainIntegrity;

    // --- Renewal ---
    private int expiringIn30Days;
    private int expiringIn7Days;
    private int renewedCount;

    public ConsentStatistics() {
        this.sectorDistribution = new HashMap<>();
        this.languageDistribution = new HashMap<>();
        this.chainIntegrity = true;
    }

    // ══════════════════════════════════════════
    // Core Getters/Setters
    // ══════════════════════════════════════════

    public int getTotalConsents() { return totalConsents; }
    public void setTotalConsents(int totalConsents) { this.totalConsents = totalConsents; }

    public int getActiveConsents() { return activeConsents; }
    public void setActiveConsents(int activeConsents) { this.activeConsents = activeConsents; }

    public int getWithdrawnConsents() { return withdrawnConsents; }
    public void setWithdrawnConsents(int withdrawnConsents) { this.withdrawnConsents = withdrawnConsents; }

    public int getExpiredConsents() { return expiredConsents; }
    public void setExpiredConsents(int expiredConsents) { this.expiredConsents = expiredConsents; }

    public double getActiveRate() { return activeRate; }
    public void setActiveRate(double activeRate) { this.activeRate = activeRate; }

    public double getWithdrawalRate() { return withdrawalRate; }
    public void setWithdrawalRate(double withdrawalRate) { this.withdrawalRate = withdrawalRate; }

    public double getComplianceScore() { return complianceScore; }
    public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }

    public int getConsentsLast30Days() { return consentsLast30Days; }
    public void setConsentsLast30Days(int consentsLast30Days) { this.consentsLast30Days = consentsLast30Days; }

    public int getWithdrawalsLast30Days() { return withdrawalsLast30Days; }
    public void setWithdrawalsLast30Days(int withdrawalsLast30Days) { this.withdrawalsLast30Days = withdrawalsLast30Days; }

    public int getRenewalsLast30Days() { return renewalsLast30Days; }
    public void setRenewalsLast30Days(int renewalsLast30Days) { this.renewalsLast30Days = renewalsLast30Days; }

    // ══════════════════════════════════════════
    // Guardian Getters/Setters
    // ══════════════════════════════════════════

    public int getTotalGuardianConsents() { return totalGuardianConsents; }
    public void setTotalGuardianConsents(int totalGuardianConsents) { this.totalGuardianConsents = totalGuardianConsents; }

    public int getPendingGuardianConsents() { return pendingGuardianConsents; }
    public void setPendingGuardianConsents(int pendingGuardianConsents) { this.pendingGuardianConsents = pendingGuardianConsents; }

    public int getVerifiedGuardianConsents() { return verifiedGuardianConsents; }
    public void setVerifiedGuardianConsents(int verifiedGuardianConsents) { this.verifiedGuardianConsents = verifiedGuardianConsents; }

    // ══════════════════════════════════════════
    // Preference Getters/Setters
    // ══════════════════════════════════════════

    public int getTotalPreferences() { return totalPreferences; }
    public void setTotalPreferences(int totalPreferences) { this.totalPreferences = totalPreferences; }

    public int getAllowedPreferences() { return allowedPreferences; }
    public void setAllowedPreferences(int allowedPreferences) { this.allowedPreferences = allowedPreferences; }

    public int getDeniedPreferences() { return deniedPreferences; }
    public void setDeniedPreferences(int deniedPreferences) { this.deniedPreferences = deniedPreferences; }

    public double getThirdPartySharingRate() { return thirdPartySharingRate; }
    public void setThirdPartySharingRate(double thirdPartySharingRate) { this.thirdPartySharingRate = thirdPartySharingRate; }

    public double getCrossBorderRate() { return crossBorderRate; }
    public void setCrossBorderRate(double crossBorderRate) { this.crossBorderRate = crossBorderRate; }

    // ══════════════════════════════════════════
    // Sector & Language Getters/Setters
    // ══════════════════════════════════════════

    public Map<String, Integer> getSectorDistribution() { return sectorDistribution; }
    public void setSectorDistribution(Map<String, Integer> sectorDistribution) { this.sectorDistribution = sectorDistribution; }

    public Map<String, Integer> getLanguageDistribution() { return languageDistribution; }
    public void setLanguageDistribution(Map<String, Integer> languageDistribution) { this.languageDistribution = languageDistribution; }

    // ══════════════════════════════════════════
    // Audit & Renewal Getters/Setters
    // ══════════════════════════════════════════

    public int getTotalAuditEntries() { return totalAuditEntries; }
    public void setTotalAuditEntries(int totalAuditEntries) { this.totalAuditEntries = totalAuditEntries; }

    public boolean isChainIntegrity() { return chainIntegrity; }
    public void setChainIntegrity(boolean chainIntegrity) { this.chainIntegrity = chainIntegrity; }

    public int getExpiringIn30Days() { return expiringIn30Days; }
    public void setExpiringIn30Days(int expiringIn30Days) { this.expiringIn30Days = expiringIn30Days; }

    public int getExpiringIn7Days() { return expiringIn7Days; }
    public void setExpiringIn7Days(int expiringIn7Days) { this.expiringIn7Days = expiringIn7Days; }

    public int getRenewedCount() { return renewedCount; }
    public void setRenewedCount(int renewedCount) { this.renewedCount = renewedCount; }
}
