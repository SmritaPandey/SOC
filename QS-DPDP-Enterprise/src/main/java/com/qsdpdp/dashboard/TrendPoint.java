package com.qsdpdp.dashboard;

import java.time.LocalDateTime;

/**
 * Trend data point for charts
 */
public class TrendPoint {
    private LocalDateTime date;
    private int value;

    public TrendPoint(LocalDateTime date, int value) {
        this.date = date;
        this.value = value;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public int getValue() {
        return value;
    }
}
