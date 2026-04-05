package com.qsdpdp.rights;

import java.util.HashMap;
import java.util.Map;

/**
 * Rights statistics for dashboard
 */
public class RightsStatistics {

    private int totalRequests;
    private int pendingRequests;
    private int completedRequests;
    private int overdueRequests;
    private double complianceRate;
    private double avgResolutionDays;
    private Map<String, Integer> requestsByType = new HashMap<>();

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getPendingRequests() {
        return pendingRequests;
    }

    public void setPendingRequests(int pendingRequests) {
        this.pendingRequests = pendingRequests;
    }

    public int getCompletedRequests() {
        return completedRequests;
    }

    public void setCompletedRequests(int completedRequests) {
        this.completedRequests = completedRequests;
    }

    public int getOverdueRequests() {
        return overdueRequests;
    }

    public void setOverdueRequests(int overdueRequests) {
        this.overdueRequests = overdueRequests;
    }

    public double getComplianceRate() {
        return complianceRate;
    }

    public void setComplianceRate(double complianceRate) {
        this.complianceRate = complianceRate;
    }

    public double getAvgResolutionDays() {
        return avgResolutionDays;
    }

    public void setAvgResolutionDays(double avgResolutionDays) {
        this.avgResolutionDays = avgResolutionDays;
    }

    public Map<String, Integer> getRequestsByType() {
        return requestsByType;
    }

    public void setRequestsByType(Map<String, Integer> requestsByType) {
        this.requestsByType = requestsByType;
    }
}
