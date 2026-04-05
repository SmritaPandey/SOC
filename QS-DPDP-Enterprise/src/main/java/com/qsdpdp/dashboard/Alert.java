package com.qsdpdp.dashboard;

/**
 * Alert for dashboard
 */
public class Alert {
    private String severity;
    private String module;
    private String message;
    private String recommendation;

    public Alert(String severity, String module, String message, String recommendation) {
        this.severity = severity;
        this.module = module;
        this.message = message;
        this.recommendation = recommendation;
    }

    public String getSeverity() {
        return severity;
    }

    public String getModule() {
        return module;
    }

    public String getMessage() {
        return message;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
