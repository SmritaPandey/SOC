package com.qsdpdp.dpia;

/**
 * DPIA statistics
 */
public class DPIAStatistics {
    private int totalDPIAs;
    private int approvedDPIAs;
    private int pendingDPIAs;
    private int highRiskDPIAs;
    private int overdueReviews;

    public int getTotalDPIAs() {
        return totalDPIAs;
    }

    public void setTotalDPIAs(int totalDPIAs) {
        this.totalDPIAs = totalDPIAs;
    }

    public int getApprovedDPIAs() {
        return approvedDPIAs;
    }

    public void setApprovedDPIAs(int approvedDPIAs) {
        this.approvedDPIAs = approvedDPIAs;
    }

    public int getPendingDPIAs() {
        return pendingDPIAs;
    }

    public void setPendingDPIAs(int pendingDPIAs) {
        this.pendingDPIAs = pendingDPIAs;
    }

    public int getHighRiskDPIAs() {
        return highRiskDPIAs;
    }

    public void setHighRiskDPIAs(int highRiskDPIAs) {
        this.highRiskDPIAs = highRiskDPIAs;
    }

    public int getOverdueReviews() {
        return overdueReviews;
    }

    public void setOverdueReviews(int overdueReviews) {
        this.overdueReviews = overdueReviews;
    }
}
