package com.qsdpdp.core;

/**
 * Represents a compliance gap identified during assessment
 */
public class ComplianceGap {

    private final String gapId;
    private final String title;
    private final String description;
    private final String module;
    private final String controlId;
    private final GapSeverity severity;
    private final String recommendation;

    public ComplianceGap(String gapId, String title, String description,
            String module, String controlId, GapSeverity severity,
            String recommendation) {
        this.gapId = gapId;
        this.title = title;
        this.description = description;
        this.module = module;
        this.controlId = controlId;
        this.severity = severity;
        this.recommendation = recommendation;
    }

    public String getGapId() {
        return gapId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getModule() {
        return module;
    }

    public String getControlId() {
        return controlId;
    }

    public GapSeverity getSeverity() {
        return severity;
    }

    public String getRecommendation() {
        return recommendation;
    }

    @Override
    public String toString() {
        return String.format("Gap[%s: %s (%s)]", gapId, title, severity);
    }
}
