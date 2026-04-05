package com.qsdpdp.dpia;

import java.time.LocalDateTime;
import java.util.*;

/**
 * DPIA Risk Level enumeration
 * 
 * @version 1.0.0
 * @since Module 9
 */
public enum DPIARiskLevel {
    LOW("Low", 1, "green", "Acceptable risk, standard controls sufficient"),
    MEDIUM("Medium", 2, "amber", "Enhanced controls recommended"),
    HIGH("High", 3, "orange", "Significant controls required, DPO review mandatory"),
    CRITICAL("Critical", 4, "red", "Unacceptable risk, processing may be prohibited, DPBI consultation required");

    private final String displayName;
    private final int score;
    private final String color;
    private final String description;

    DPIARiskLevel(String displayName, int score, String color, String description) {
        this.displayName = displayName;
        this.score = score;
        this.color = color;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getScore() {
        return score;
    }

    public String getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public static DPIARiskLevel fromScore(int likelihood, int impact) {
        int total = likelihood * impact;
        if (total >= 16)
            return CRITICAL;
        if (total >= 9)
            return HIGH;
        if (total >= 4)
            return MEDIUM;
        return LOW;
    }
}
