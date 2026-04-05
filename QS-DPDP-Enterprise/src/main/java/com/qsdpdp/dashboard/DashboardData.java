package com.qsdpdp.dashboard;

import com.qsdpdp.consent.ConsentStatistics;
import com.qsdpdp.breach.BreachStatistics;
import com.qsdpdp.rights.RightsStatistics;
import com.qsdpdp.dpia.DPIAStatistics;
import com.qsdpdp.policy.PolicyStatistics;
import com.qsdpdp.user.UserStatistics;
import com.qsdpdp.rag.RAGStatus;
import com.qsdpdp.rag.ModuleScore;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Complete dashboard data for executive view
 */
public class DashboardData {

    private double overallScore;
    private RAGStatus overallStatus;
    private Map<String, ModuleScore> moduleScores = new HashMap<>();

    private ConsentStatistics consentStats;
    private BreachStatistics breachStats;
    private RightsStatistics rightsStats;
    private DPIAStatistics dpiaStats;
    private PolicyStatistics policyStats;
    private UserStatistics userStats;

    private List<Alert> alerts = new ArrayList<>();
    private Map<String, KPI> kpis = new LinkedHashMap<>();

    private LocalDateTime generatedAt;

    // Getters and setters
    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public RAGStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(RAGStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public Map<String, ModuleScore> getModuleScores() {
        return moduleScores;
    }

    public void setModuleScores(Map<String, ModuleScore> moduleScores) {
        this.moduleScores = moduleScores;
    }

    public ConsentStatistics getConsentStats() {
        return consentStats;
    }

    public void setConsentStats(ConsentStatistics consentStats) {
        this.consentStats = consentStats;
    }

    public BreachStatistics getBreachStats() {
        return breachStats;
    }

    public void setBreachStats(BreachStatistics breachStats) {
        this.breachStats = breachStats;
    }

    public RightsStatistics getRightsStats() {
        return rightsStats;
    }

    public void setRightsStats(RightsStatistics rightsStats) {
        this.rightsStats = rightsStats;
    }

    public DPIAStatistics getDpiaStats() {
        return dpiaStats;
    }

    public void setDpiaStats(DPIAStatistics dpiaStats) {
        this.dpiaStats = dpiaStats;
    }

    public PolicyStatistics getPolicyStats() {
        return policyStats;
    }

    public void setPolicyStats(PolicyStatistics policyStats) {
        this.policyStats = policyStats;
    }

    public UserStatistics getUserStats() {
        return userStats;
    }

    public void setUserStats(UserStatistics userStats) {
        this.userStats = userStats;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public Map<String, KPI> getKpis() {
        return kpis;
    }

    public void setKpis(Map<String, KPI> kpis) {
        this.kpis = kpis;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
