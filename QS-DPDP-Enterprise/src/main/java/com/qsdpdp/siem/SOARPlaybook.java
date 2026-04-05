package com.qsdpdp.siem;

import java.util.*;

/**
 * SOAR Playbook for automated incident response
 * Contains steps to execute when correlation rules trigger
 * 
 * @version 1.0.0
 * @since Module 6
 */
public class SOARPlaybook {

    private String id;
    private String name;
    private String description;
    private boolean enabled;
    private List<PlaybookStep> steps;
    private int priority;
    private String dpdpAlignment;
    private boolean requiresApproval;
    private String approvalRole;

    public SOARPlaybook() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.steps = new ArrayList<>();
        this.priority = 50;
    }

    public SOARPlaybook(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    // ═══════════════════════════════════════════════════════════
    // PREDEFINED PLAYBOOKS
    // ═══════════════════════════════════════════════════════════

    public static Map<String, SOARPlaybook> getDefaultPlaybooks() {
        Map<String, SOARPlaybook> playbooks = new LinkedHashMap<>();

        // Playbook 1: Lockout Source
        playbooks.put("LOCKOUT_SOURCE", new SOARPlaybook("LOCKOUT_SOURCE",
                "Block source IP/system after brute force detection")
                .addStep(new PlaybookStep("LOG", "Log incident details", "Log the security event with full context"))
                .addStep(new PlaybookStep("BLOCK_IP", "Block source IP", "Add source IP to firewall blocklist"))
                .addStep(new PlaybookStep("NOTIFY", "Notify security team", "Send alert to SOC team"))
                .addStep(new PlaybookStep("CREATE_TICKET", "Create incident ticket",
                        "Create incident in ticketing system"))
                .withPriority(90));

        // Playbook 2: Suspend User Access
        playbooks.put("SUSPEND_USER_ACCESS", new SOARPlaybook("SUSPEND_USER_ACCESS",
                "Suspend user access on data exfiltration detection")
                .addStep(new PlaybookStep("LOG", "Log incident", "Log exfiltration attempt details"))
                .addStep(new PlaybookStep("SUSPEND_USER", "Suspend user account", "Disable user account immediately"))
                .addStep(new PlaybookStep("REVOKE_SESSIONS", "Revoke active sessions", "Terminate all active sessions"))
                .addStep(new PlaybookStep("PRESERVE_EVIDENCE", "Preserve evidence", "Snapshot user activity logs"))
                .addStep(new PlaybookStep("NOTIFY_DPO", "Notify DPO", "Alert Data Protection Officer"))
                .addStep(new PlaybookStep("CREATE_BREACH_RECORD", "Create breach record",
                        "Initialize breach investigation"))
                .withDPDPAlignment("Section 8 - Breach Notification")
                .withPriority(100));

        // Playbook 3: Alert DPO
        playbooks.put("ALERT_DPO", new SOARPlaybook("ALERT_DPO",
                "Alert Data Protection Officer for compliance violations")
                .addStep(new PlaybookStep("LOG", "Log violation", "Record compliance violation details"))
                .addStep(new PlaybookStep("NOTIFY_DPO", "Notify DPO", "Send immediate notification to DPO"))
                .addStep(new PlaybookStep("GENERATE_REPORT", "Generate report", "Create detailed incident report"))
                .addStep(new PlaybookStep("CREATE_TICKET", "Create ticket", "Create compliance ticket"))
                .withDPDPAlignment("Section 10 - Data Protection Officer")
                .withPriority(85));

        // Playbook 4: Stop Processing
        playbooks.put("STOP_PROCESSING", new SOARPlaybook("STOP_PROCESSING",
                "Stop processing personal data after consent withdrawal")
                .addStep(new PlaybookStep("LOG", "Log consent change", "Record consent withdrawal"))
                .addStep(new PlaybookStep("PAUSE_PROCESSING", "Pause processing",
                        "Halt all processing for data principal"))
                .addStep(new PlaybookStep("UPDATE_CONSENT_STATUS", "Update consent", "Mark consent as withdrawn"))
                .addStep(new PlaybookStep("NOTIFY_SYSTEMS", "Notify systems",
                        "Propagate consent withdrawal to all systems"))
                .addStep(new PlaybookStep("AUDIT_LOG", "Audit log", "Create audit trail entry"))
                .withDPDPAlignment("Section 6 - Consent")
                .withPriority(80));

        // Playbook 5: Revoke Session
        playbooks.put("REVOKE_SESSION", new SOARPlaybook("REVOKE_SESSION",
                "Revoke user session on privilege escalation detection")
                .addStep(new PlaybookStep("LOG", "Log escalation attempt", "Record privilege escalation details"))
                .addStep(new PlaybookStep("REVOKE_SESSION", "Revoke session", "Terminate current user session"))
                .addStep(new PlaybookStep("FORCE_REAUTH", "Force re-authentication", "Require MFA on next login"))
                .addStep(new PlaybookStep("NOTIFY", "Notify security", "Alert security team"))
                .withPriority(75));

        // Playbook 6: Escalate to DPO
        playbooks.put("ESCALATE_TO_DPO", new SOARPlaybook("ESCALATE_TO_DPO",
                "Escalate SLA breaches to Data Protection Officer")
                .addStep(new PlaybookStep("LOG", "Log SLA breach", "Record SLA violation details"))
                .addStep(new PlaybookStep("CALCULATE_OVERDUE", "Calculate overdue", "Determine days past SLA"))
                .addStep(new PlaybookStep("NOTIFY_DPO", "Notify DPO", "Send escalation to DPO"))
                .addStep(new PlaybookStep("NOTIFY_SUPERVISOR", "Notify supervisor", "Alert responsible supervisor"))
                .addStep(new PlaybookStep("CREATE_REMEDIATION", "Create remediation", "Create remediation task"))
                .withDPDPAlignment("Section 11-14 - Rights")
                .withRequiresApproval(false)
                .withPriority(70));

        // Playbook 7: Block Data Transfer
        playbooks.put("BLOCK_DATA_TRANSFER", new SOARPlaybook("BLOCK_DATA_TRANSFER",
                "Block unauthorized data transfer attempts")
                .addStep(new PlaybookStep("LOG", "Log transfer attempt", "Record data transfer details"))
                .addStep(new PlaybookStep("BLOCK_TRANSFER", "Block transfer", "Prevent data transfer completion"))
                .addStep(new PlaybookStep("QUARANTINE_DATA", "Quarantine data", "Move data to quarantine location"))
                .addStep(new PlaybookStep("NOTIFY_DPO", "Notify DPO", "Alert DPO of DLP violation"))
                .addStep(new PlaybookStep("CREATE_INCIDENT", "Create incident", "Create DLP incident record"))
                .withDPDPAlignment("Section 16 - Cross-Border Transfer")
                .withPriority(90));

        // Playbook 8: Log and Alert
        playbooks.put("LOG_AND_ALERT", new SOARPlaybook("LOG_AND_ALERT",
                "Log suspicious activity and alert relevant parties")
                .addStep(new PlaybookStep("LOG", "Log activity", "Record suspicious activity"))
                .addStep(new PlaybookStep("ANALYZE", "Analyze patterns", "Check for related events"))
                .addStep(new PlaybookStep("NOTIFY_SECURITY", "Notify security", "Send security alert"))
                .withPriority(50));

        // Playbook 9: Suspend User and Investigate
        playbooks.put("SUSPEND_USER_INVESTIGATE", new SOARPlaybook("SUSPEND_USER_INVESTIGATE",
                "Suspend user and initiate forensic investigation")
                .addStep(new PlaybookStep("LOG", "Log incident", "Record incident details"))
                .addStep(new PlaybookStep("SUSPEND_USER", "Suspend user", "Disable user account"))
                .addStep(new PlaybookStep("PRESERVE_EVIDENCE", "Preserve evidence", "Create forensic snapshot"))
                .addStep(new PlaybookStep("INITIATE_INVESTIGATION", "Start investigation", "Create investigation case"))
                .addStep(new PlaybookStep("NOTIFY_LEGAL", "Notify legal", "Alert legal team"))
                .addStep(new PlaybookStep("NOTIFY_DPO", "Notify DPO", "Alert DPO of potential breach"))
                .withDPDPAlignment("Section 8 - Breach Notification")
                .withRequiresApproval(true)
                .withApprovalRole("SECURITY_MANAGER")
                .withPriority(95));

        // Playbook 10: Block and Alert DPO
        playbooks.put("BLOCK_AND_ALERT_DPO", new SOARPlaybook("BLOCK_AND_ALERT_DPO",
                "Block cross-border transfer and alert DPO")
                .addStep(new PlaybookStep("LOG", "Log transfer", "Record cross-border transfer attempt"))
                .addStep(new PlaybookStep("BLOCK_TRANSFER", "Block transfer", "Prevent cross-border data flow"))
                .addStep(new PlaybookStep("IDENTIFY_DESTINATION", "Identify destination",
                        "Determine transfer destination"))
                .addStep(new PlaybookStep("CHECK_ADEQUACY", "Check adequacy", "Verify destination adequacy status"))
                .addStep(new PlaybookStep("NOTIFY_DPO", "Notify DPO", "Alert DPO with details"))
                .addStep(new PlaybookStep("CREATE_COMPLIANCE_REVIEW", "Request review",
                        "Create compliance review request"))
                .withDPDPAlignment("Section 16 - Cross-Border Transfer")
                .withPriority(85));

        return playbooks;
    }

    // ═══════════════════════════════════════════════════════════
    // BUILDER METHODS
    // ═══════════════════════════════════════════════════════════

    public SOARPlaybook addStep(PlaybookStep step) {
        step.setOrder(this.steps.size() + 1);
        this.steps.add(step);
        return this;
    }

    public SOARPlaybook withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public SOARPlaybook withDPDPAlignment(String dpdpAlignment) {
        this.dpdpAlignment = dpdpAlignment;
        return this;
    }

    public SOARPlaybook withRequiresApproval(boolean requires) {
        this.requiresApproval = requires;
        return this;
    }

    public SOARPlaybook withApprovalRole(String role) {
        this.approvalRole = role;
        return this;
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<PlaybookStep> getSteps() {
        return steps;
    }

    public int getPriority() {
        return priority;
    }

    public String getDpdpAlignment() {
        return dpdpAlignment;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public String getApprovalRole() {
        return approvalRole;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // ═══════════════════════════════════════════════════════════
    // PLAYBOOK STEP
    // ═══════════════════════════════════════════════════════════

    public static class PlaybookStep {
        private String action;
        private String name;
        private String description;
        private int order;
        private int timeoutSeconds;
        private boolean required;
        private String status; // PENDING, RUNNING, COMPLETED, FAILED, SKIPPED

        public PlaybookStep(String action, String name, String description) {
            this.action = action;
            this.name = name;
            this.description = description;
            this.timeoutSeconds = 60;
            this.required = true;
            this.status = "PENDING";
        }

        public String getAction() {
            return action;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public boolean isRequired() {
            return required;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
