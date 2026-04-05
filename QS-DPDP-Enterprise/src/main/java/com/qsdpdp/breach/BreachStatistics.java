package com.qsdpdp.breach;

/**
 * Breach statistics for dashboard
 */
public class BreachStatistics {

    private int totalBreaches;
    private int openBreaches;
    private int criticalBreaches;
    private int notifiedBreaches;
    private int totalAffected;
    private double avgResolutionHours;
    private double dpbiComplianceRate;

    public int getTotalBreaches() {
        return totalBreaches;
    }

    public void setTotalBreaches(int totalBreaches) {
        this.totalBreaches = totalBreaches;
    }

    public int getOpenBreaches() {
        return openBreaches;
    }

    public void setOpenBreaches(int openBreaches) {
        this.openBreaches = openBreaches;
    }

    public int getCriticalBreaches() {
        return criticalBreaches;
    }

    public void setCriticalBreaches(int criticalBreaches) {
        this.criticalBreaches = criticalBreaches;
    }

    public int getNotifiedBreaches() {
        return notifiedBreaches;
    }

    public void setNotifiedBreaches(int notifiedBreaches) {
        this.notifiedBreaches = notifiedBreaches;
    }

    public int getTotalAffected() {
        return totalAffected;
    }

    public void setTotalAffected(int totalAffected) {
        this.totalAffected = totalAffected;
    }

    public double getAvgResolutionHours() {
        return avgResolutionHours;
    }

    public void setAvgResolutionHours(double avgResolutionHours) {
        this.avgResolutionHours = avgResolutionHours;
    }

    public double getDpbiComplianceRate() {
        return dpbiComplianceRate;
    }

    public void setDpbiComplianceRate(double dpbiComplianceRate) {
        this.dpbiComplianceRate = dpbiComplianceRate;
    }
}
