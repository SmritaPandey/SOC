package com.qsdpdp.siem;

import java.util.*;
import java.util.function.Predicate;

/**
 * Correlation Rule for SIEM event analysis
 * Detects patterns across multiple events that indicate security incidents
 * 
 * @version 1.0.0
 * @since Module 6
 */
public class CorrelationRule {

    private String id;
    private String name;
    private String description;
    private boolean enabled;
    private EventSeverity outputSeverity;
    private EventCategory outputCategory;

    // Rule conditions
    private List<EventCategory> triggerCategories;
    private EventSeverity minimumSeverity;
    private int eventThreshold; // Number of events
    private int timeWindowSeconds; // Time window for correlation
    private boolean sameSource; // Events must be from same source
    private boolean sameUser; // Events must be from same user
    private boolean sameSession; // Events must be in same session

    // DPDP-specific conditions
    private boolean requiresPersonalData;
    private boolean requiresSensitiveData;
    private String dpdpSectionTarget;

    // SOAR response
    private String soarPlaybook; // Playbook to execute
    private boolean autoExecutePlaybook;
    private int priority;

    public CorrelationRule() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.triggerCategories = new ArrayList<>();
        this.eventThreshold = 1;
        this.timeWindowSeconds = 300; // 5 minutes default
        this.priority = 50;
    }

    // ═══════════════════════════════════════════════════════════
    // PREDEFINED DPDP CORRELATION RULES
    // ═══════════════════════════════════════════════════════════

    public static List<CorrelationRule> getDefaultRules() {
        List<CorrelationRule> rules = new ArrayList<>();

        // Rule 1: Brute Force Detection
        rules.add(new CorrelationRule()
                .withName("BRUTE_FORCE_DETECTION")
                .withDescription("Multiple failed login attempts from same source")
                .withTriggerCategories(EventCategory.AUTH_FAILURE)
                .withEventThreshold(5)
                .withTimeWindow(300) // 5 minutes
                .withSameSource(true)
                .withOutputSeverity(EventSeverity.HIGH)
                .withOutputCategory(EventCategory.INTRUSION_ATTEMPT)
                .withSOARPlaybook("LOCKOUT_SOURCE", true)
                .withPriority(90));

        // Rule 2: Personal Data Exfiltration
        rules.add(new CorrelationRule()
                .withName("DATA_EXFILTRATION")
                .withDescription("Large volume of personal data exported by single user")
                .withTriggerCategories(EventCategory.DATA_EXPORT, EventCategory.DATA_ACCESS)
                .withEventThreshold(10)
                .withTimeWindow(600)
                .withSameUser(true)
                .withRequiresPersonalData(true)
                .withOutputSeverity(EventSeverity.CRITICAL)
                .withOutputCategory(EventCategory.BREACH_SUSPECTED)
                .withSOARPlaybook("SUSPEND_USER_ACCESS", true)
                .withDPDPSection("Section 8 - Breach Notification")
                .withPriority(100));

        // Rule 3: Unauthorized Sensitive Data Access
        rules.add(new CorrelationRule()
                .withName("UNAUTHORIZED_SENSITIVE_ACCESS")
                .withDescription("Access to sensitive personal data without proper authorization")
                .withTriggerCategories(EventCategory.SENSITIVE_DATA_ACCESS, EventCategory.ACCESS_DENIED)
                .withEventThreshold(3)
                .withTimeWindow(300)
                .withSameUser(true)
                .withRequiresSensitiveData(true)
                .withOutputSeverity(EventSeverity.CRITICAL)
                .withOutputCategory(EventCategory.POLICY_VIOLATION)
                .withSOARPlaybook("ALERT_DPO", true)
                .withDPDPSection("Section 6 - Consent")
                .withPriority(95));

        // Rule 4: Consent Violation Pattern
        rules.add(new CorrelationRule()
                .withName("CONSENT_VIOLATION")
                .withDescription("Processing personal data after consent withdrawal")
                .withTriggerCategories(EventCategory.CONSENT_WITHDRAWN, EventCategory.DATA_ACCESS)
                .withEventThreshold(1)
                .withTimeWindow(86400) // 24 hours
                .withRequiresPersonalData(true)
                .withOutputSeverity(EventSeverity.HIGH)
                .withOutputCategory(EventCategory.POLICY_VIOLATION)
                .withSOARPlaybook("STOP_PROCESSING", true)
                .withDPDPSection("Section 6 - Consent")
                .withPriority(85));

        // Rule 5: Privilege Escalation Chain
        rules.add(new CorrelationRule()
                .withName("PRIVILEGE_ESCALATION_CHAIN")
                .withDescription("User accessing resources beyond their role")
                .withTriggerCategories(EventCategory.ACCESS_DENIED, EventCategory.PRIVILEGE_ESCALATION)
                .withEventThreshold(3)
                .withTimeWindow(600)
                .withSameUser(true)
                .withOutputSeverity(EventSeverity.HIGH)
                .withOutputCategory(EventCategory.INTRUSION_ATTEMPT)
                .withSOARPlaybook("REVOKE_SESSION", true)
                .withPriority(80));

        // Rule 6: Rights Request Processing Delay
        rules.add(new CorrelationRule()
                .withName("RIGHTS_REQUEST_SLA_BREACH")
                .withDescription("Rights request not processed within regulatory timeline")
                .withTriggerCategories(EventCategory.RIGHTS_REQUEST)
                .withEventThreshold(1)
                .withTimeWindow(2592000) // 30 days
                .withOutputSeverity(EventSeverity.MEDIUM)
                .withOutputCategory(EventCategory.POLICY_VIOLATION)
                .withSOARPlaybook("ESCALATE_TO_DPO", false)
                .withDPDPSection("Section 11-14 - Rights")
                .withPriority(70));

        // Rule 7: DLP Violation Pattern
        rules.add(new CorrelationRule()
                .withName("DLP_VIOLATION_PATTERN")
                .withDescription("Multiple DLP violations from same source")
                .withTriggerCategories(EventCategory.DLP_VIOLATION)
                .withEventThreshold(3)
                .withTimeWindow(3600)
                .withSameSource(true)
                .withOutputSeverity(EventSeverity.HIGH)
                .withOutputCategory(EventCategory.PII_EXPOSURE)
                .withSOARPlaybook("BLOCK_DATA_TRANSFER", true)
                .withPriority(90));

        // Rule 8: Off-Hours Access
        rules.add(new CorrelationRule()
                .withName("OFF_HOURS_SENSITIVE_ACCESS")
                .withDescription("Sensitive data access outside business hours")
                .withTriggerCategories(EventCategory.SENSITIVE_DATA_ACCESS)
                .withEventThreshold(1)
                .withTimeWindow(60)
                .withRequiresSensitiveData(true)
                .withOutputSeverity(EventSeverity.MEDIUM)
                .withOutputCategory(EventCategory.POLICY_VIOLATION)
                .withSOARPlaybook("LOG_AND_ALERT", false)
                .withPriority(60));

        // Rule 9: Mass Data Deletion
        rules.add(new CorrelationRule()
                .withName("MASS_DATA_DELETION")
                .withDescription("Large scale data deletion activity")
                .withTriggerCategories(EventCategory.DATA_DELETION)
                .withEventThreshold(50)
                .withTimeWindow(300)
                .withSameUser(true)
                .withOutputSeverity(EventSeverity.CRITICAL)
                .withOutputCategory(EventCategory.BREACH_SUSPECTED)
                .withSOARPlaybook("SUSPEND_USER_INVESTIGATE", true)
                .withPriority(95));

        // Rule 10: Cross-Border Transfer
        rules.add(new CorrelationRule()
                .withName("CROSS_BORDER_TRANSFER")
                .withDescription("Personal data transfer to non-approved destination")
                .withTriggerCategories(EventCategory.DATA_TRANSFER)
                .withEventThreshold(1)
                .withTimeWindow(60)
                .withRequiresPersonalData(true)
                .withOutputSeverity(EventSeverity.HIGH)
                .withOutputCategory(EventCategory.POLICY_VIOLATION)
                .withSOARPlaybook("BLOCK_AND_ALERT_DPO", true)
                .withDPDPSection("Section 16 - Cross-Border Transfer")
                .withPriority(85));

        return rules;
    }

    // ═══════════════════════════════════════════════════════════
    // BUILDER METHODS
    // ═══════════════════════════════════════════════════════════

    public CorrelationRule withName(String name) {
        this.name = name;
        return this;
    }

    public CorrelationRule withDescription(String description) {
        this.description = description;
        return this;
    }

    public CorrelationRule withTriggerCategories(EventCategory... categories) {
        this.triggerCategories.addAll(Arrays.asList(categories));
        return this;
    }

    public CorrelationRule withMinimumSeverity(EventSeverity severity) {
        this.minimumSeverity = severity;
        return this;
    }

    public CorrelationRule withEventThreshold(int threshold) {
        this.eventThreshold = threshold;
        return this;
    }

    public CorrelationRule withTimeWindow(int seconds) {
        this.timeWindowSeconds = seconds;
        return this;
    }

    public CorrelationRule withSameSource(boolean sameSource) {
        this.sameSource = sameSource;
        return this;
    }

    public CorrelationRule withSameUser(boolean sameUser) {
        this.sameUser = sameUser;
        return this;
    }

    public CorrelationRule withSameSession(boolean sameSession) {
        this.sameSession = sameSession;
        return this;
    }

    public CorrelationRule withRequiresPersonalData(boolean required) {
        this.requiresPersonalData = required;
        return this;
    }

    public CorrelationRule withRequiresSensitiveData(boolean required) {
        this.requiresSensitiveData = required;
        return this;
    }

    public CorrelationRule withOutputSeverity(EventSeverity severity) {
        this.outputSeverity = severity;
        return this;
    }

    public CorrelationRule withOutputCategory(EventCategory category) {
        this.outputCategory = category;
        return this;
    }

    public CorrelationRule withSOARPlaybook(String playbook, boolean autoExecute) {
        this.soarPlaybook = playbook;
        this.autoExecutePlaybook = autoExecute;
        return this;
    }

    public CorrelationRule withDPDPSection(String section) {
        this.dpdpSectionTarget = section;
        return this;
    }

    public CorrelationRule withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    // ═══════════════════════════════════════════════════════════
    // EVALUATION
    // ═══════════════════════════════════════════════════════════

    public boolean matches(SecurityEvent event) {
        // Check category match
        if (!triggerCategories.isEmpty() && !triggerCategories.contains(event.getCategory())) {
            return false;
        }

        // Check minimum severity
        if (minimumSeverity != null &&
                event.getSeverity().getPriority() > minimumSeverity.getPriority()) {
            return false;
        }

        // Check personal data requirement
        if (requiresPersonalData && !event.isPersonalDataInvolved()) {
            return false;
        }

        // Check sensitive data requirement
        if (requiresSensitiveData && !event.isSensitiveDataInvolved()) {
            return false;
        }

        return true;
    }

    public Predicate<SecurityEvent> getMatchPredicate() {
        return this::matches;
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

    public EventSeverity getOutputSeverity() {
        return outputSeverity;
    }

    public EventCategory getOutputCategory() {
        return outputCategory;
    }

    public List<EventCategory> getTriggerCategories() {
        return triggerCategories;
    }

    public EventSeverity getMinimumSeverity() {
        return minimumSeverity;
    }

    public int getEventThreshold() {
        return eventThreshold;
    }

    public int getTimeWindowSeconds() {
        return timeWindowSeconds;
    }

    public boolean isSameSource() {
        return sameSource;
    }

    public boolean isSameUser() {
        return sameUser;
    }

    public boolean isSameSession() {
        return sameSession;
    }

    public boolean isRequiresPersonalData() {
        return requiresPersonalData;
    }

    public boolean isRequiresSensitiveData() {
        return requiresSensitiveData;
    }

    public String getDpdpSectionTarget() {
        return dpdpSectionTarget;
    }

    public String getSoarPlaybook() {
        return soarPlaybook;
    }

    public boolean isAutoExecutePlaybook() {
        return autoExecutePlaybook;
    }

    public int getPriority() {
        return priority;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
