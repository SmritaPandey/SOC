package com.qsdpdp.iam;

import java.util.*;

/**
 * Permission enumeration for fine-grained access control
 * Mapped to DPDP compliance operations
 * 
 * @version 1.0.0
 * @since Module 11
 */
public enum Permission {

    // System Permissions
    SYSTEM_ADMIN("System Administration", "system"),
    SYSTEM_CONFIG("System Configuration", "system"),
    SYSTEM_AUDIT("View Audit Logs", "audit"),
    SYSTEM_LOGS("View System Logs", "system"),

    // User Management
    USER_CREATE("Create Users", "users"),
    USER_READ("View Users", "users"),
    USER_UPDATE("Update Users", "users"),
    USER_DELETE("Delete Users", "users"),
    USER_ROLE_ASSIGN("Assign Roles", "users"),

    // Consent Management
    CONSENT_CREATE("Create Consent Records", "consent"),
    CONSENT_READ("View Consent Records", "consent"),
    CONSENT_UPDATE("Update Consent", "consent"),
    CONSENT_WITHDRAW("Withdraw Consent", "consent"),
    CONSENT_EXPORT("Export Consent Data", "consent"),
    CONSENT_ANALYTICS("View Consent Analytics", "consent"),

    // Breach Management
    BREACH_REPORT("Report Breach", "breach"),
    BREACH_READ("View Breaches", "breach"),
    BREACH_UPDATE("Update Breach", "breach"),
    BREACH_NOTIFY("Send Breach Notifications", "breach"),
    BREACH_CLOSE("Close Breach", "breach"),
    BREACH_DPBI_SUBMIT("Submit to DPBI", "breach"),
    BREACH_CERTIN_SUBMIT("Submit to CERT-In", "breach"),

    // Rights Management
    RIGHTS_CREATE("Create Rights Requests", "rights"),
    RIGHTS_READ("View Rights Requests", "rights"),
    RIGHTS_PROCESS("Process Rights Requests", "rights"),
    RIGHTS_APPROVE("Approve Rights", "rights"),
    RIGHTS_REJECT("Reject Rights", "rights"),

    // DPIA
    DPIA_CREATE("Create DPIA", "dpia"),
    DPIA_READ("View DPIA", "dpia"),
    DPIA_UPDATE("Update DPIA", "dpia"),
    DPIA_APPROVE("Approve DPIA", "dpia"),
    DPIA_DPBI_CONSULT("DPBI Consultation", "dpia"),

    // Policy Management
    POLICY_CREATE("Create Policies", "policy"),
    POLICY_READ("View Policies", "policy"),
    POLICY_UPDATE("Update Policies", "policy"),
    POLICY_APPROVE("Approve Policies", "policy"),
    POLICY_PUBLISH("Publish Policies", "policy"),
    POLICY_RETIRE("Retire Policies", "policy"),

    // Gap Analysis
    GAP_CREATE("Create Assessments", "gap"),
    GAP_READ("View Assessments", "gap"),
    GAP_RESPOND("Submit Responses", "gap"),
    GAP_REMEDIATE("Manage Remediation", "gap"),

    // PII Scanner
    PII_SCAN("Run PII Scans", "pii"),
    PII_READ("View PII Findings", "pii"),
    PII_REMEDIATE("Remediate PII Issues", "pii"),

    // SIEM
    SIEM_READ("View Security Events", "siem"),
    SIEM_INVESTIGATE("Investigate Events", "siem"),
    SIEM_CORRELATE("Manage Correlation Rules", "siem"),
    SIEM_ALERT_ACK("Acknowledge Alerts", "siem"),

    // DLP
    DLP_POLICY_CREATE("Create DLP Policies", "dlp"),
    DLP_POLICY_READ("View DLP Policies", "dlp"),
    DLP_INCIDENT_READ("View DLP Incidents", "dlp"),
    DLP_INCIDENT_RESOLVE("Resolve DLP Incidents", "dlp"),

    // Reporting
    REPORT_VIEW("View Reports", "reports"),
    REPORT_CREATE("Create Reports", "reports"),
    REPORT_EXPORT("Export Reports", "reports"),
    REPORT_SCHEDULE("Schedule Reports", "reports"),

    // API Access
    API_READ("API Read Access", "api"),
    API_WRITE("API Write Access", "api"),
    API_ADMIN("API Administration", "api");

    private final String displayName;
    private final String module;

    Permission(String displayName, String module) {
        this.displayName = displayName;
        this.module = module;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getModule() {
        return module;
    }

    public static List<Permission> getByModule(String module) {
        return Arrays.stream(values())
                .filter(p -> p.module.equals(module))
                .toList();
    }

    // Pre-defined permission sets for roles
    public static Set<Permission> getDPOPermissions() {
        return Set.of(
                CONSENT_READ, CONSENT_ANALYTICS,
                BREACH_READ, BREACH_UPDATE, BREACH_NOTIFY, BREACH_DPBI_SUBMIT, BREACH_CERTIN_SUBMIT,
                RIGHTS_READ, RIGHTS_APPROVE, RIGHTS_REJECT,
                DPIA_READ, DPIA_APPROVE, DPIA_DPBI_CONSULT,
                POLICY_READ, POLICY_APPROVE, POLICY_PUBLISH,
                GAP_READ, GAP_REMEDIATE,
                PII_READ, PII_REMEDIATE,
                SIEM_READ, DLP_INCIDENT_READ,
                REPORT_VIEW, REPORT_CREATE, REPORT_EXPORT,
                SYSTEM_AUDIT);
    }

    public static Set<Permission> getSecurityAnalystPermissions() {
        return Set.of(
                SIEM_READ, SIEM_INVESTIGATE, SIEM_ALERT_ACK,
                DLP_POLICY_READ, DLP_INCIDENT_READ, DLP_INCIDENT_RESOLVE,
                PII_READ, BREACH_READ, BREACH_REPORT,
                REPORT_VIEW, SYSTEM_LOGS);
    }

    public static Set<Permission> getComplianceOfficerPermissions() {
        return Set.of(
                CONSENT_READ, CONSENT_ANALYTICS,
                BREACH_READ, RIGHTS_READ, RIGHTS_PROCESS,
                DPIA_READ, DPIA_CREATE, DPIA_UPDATE,
                POLICY_READ, POLICY_CREATE,
                GAP_READ, GAP_CREATE, GAP_RESPOND,
                REPORT_VIEW, REPORT_CREATE);
    }

    public static Set<Permission> getAuditorPermissions() {
        return Set.of(
                CONSENT_READ, BREACH_READ, RIGHTS_READ,
                DPIA_READ, POLICY_READ, GAP_READ,
                PII_READ, SIEM_READ, DLP_POLICY_READ, DLP_INCIDENT_READ,
                REPORT_VIEW, SYSTEM_AUDIT, SYSTEM_LOGS);
    }
}
