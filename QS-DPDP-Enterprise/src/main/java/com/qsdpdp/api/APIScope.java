package com.qsdpdp.api;

import java.util.Set;
import java.time.LocalDateTime;

/**
 * API Access Scopes for programmatic access control
 * 
 * @version 1.0.0
 * @since Module 13
 */
public enum APIScope {
    // Read Scopes
    CONSENT_READ("consent:read", "Read consent records"),
    CONSENT_WRITE("consent:write", "Create and update consents"),
    BREACH_READ("breach:read", "Read breach information"),
    BREACH_WRITE("breach:write", "Report and update breaches"),
    RIGHTS_READ("rights:read", "View rights requests"),
    RIGHTS_WRITE("rights:write", "Submit rights requests"),
    DPIA_READ("dpia:read", "View DPIAs"),
    DPIA_WRITE("dpia:write", "Create and manage DPIAs"),
    POLICY_READ("policy:read", "View policies"),
    PII_READ("pii:read", "View PII scan results"),
    PII_WRITE("pii:write", "Run PII scans"),
    SIEM_READ("siem:read", "View security events"),
    DLP_READ("dlp:read", "View DLP incidents"),
    COMPLIANCE_READ("compliance:read", "View compliance scores"),
    REPORTING_READ("reporting:read", "View reports"),
    REPORTING_WRITE("reporting:write", "Generate reports"),

    // Admin Scopes
    ADMIN("admin", "Full administrative access"),
    WEBHOOK_MANAGE("webhook:manage", "Manage webhooks");

    private final String value;
    private final String description;

    APIScope(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
