package com.qsdpdp.policy;

/**
 * Policy statistics
 */
public class PolicyStatistics {
    private int totalPolicies;
    private int activePolicies;
    private int pendingPolicies;
    private int overdueReviews;

    public int getTotalPolicies() {
        return totalPolicies;
    }

    public void setTotalPolicies(int totalPolicies) {
        this.totalPolicies = totalPolicies;
    }

    public int getActivePolicies() {
        return activePolicies;
    }

    public void setActivePolicies(int activePolicies) {
        this.activePolicies = activePolicies;
    }

    public int getPendingPolicies() {
        return pendingPolicies;
    }

    public void setPendingPolicies(int pendingPolicies) {
        this.pendingPolicies = pendingPolicies;
    }

    public int getOverdueReviews() {
        return overdueReviews;
    }

    public void setOverdueReviews(int overdueReviews) {
        this.overdueReviews = overdueReviews;
    }
}
