package com.qsdpdp.dashboard;

/**
 * Key Performance Indicator
 */
public class KPI {
    private String name;
    private int value;
    private int target;
    private String status;

    public KPI(String name, int value, int target, String status) {
        this.name = name;
        this.value = value;
        this.target = target;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public int getTarget() {
        return target;
    }

    public String getStatus() {
        return status;
    }

    public double getPercentage() {
        return target > 0 ? (double) value / target * 100 : 0;
    }
}
