package com.qsdpdp.dlp;

import com.qsdpdp.pii.PIIType;
import java.util.*;

/**
 * DLP Policy model for data protection rules
 * Aligned with DPDP Act 2023 data protection requirements
 * 
 * @version 1.0.0
 * @since Module 7
 */
public class DLPPolicy {

    private String id;
    private String name;
    private String description;
    private boolean enabled;
    private int priority;

    // Scope
    private Set<String> targetSystems;
    private Set<String> targetUsers;
    private Set<String> targetGroups;
    private Set<String> excludedUsers;
    private Set<String> excludedPaths;

    // Detection
    private Set<PIIType> protectedDataTypes;
    private Set<String> customPatterns;
    private int minMatchCount;
    private double confidenceThreshold;

    // Channels
    private boolean monitorEndpoint;
    private boolean monitorNetwork;
    private boolean monitorCloud;
    private boolean monitorEmail;
    private boolean monitorPrint;
    private boolean monitorRemovableMedia;
    private boolean monitorClipboard;

    // Actions
    private DLPAction primaryAction;
    private DLPAction fallbackAction;
    private boolean notifyUser;
    private boolean notifyManager;
    private boolean notifyDPO;
    private String notificationTemplate;

    // DPDP mapping
    private String dpdpSection;
    private boolean crossBorderRestriction;
    private boolean sensitiveDataProtection;

    // Audit
    private String createdBy;
    private String approvedBy;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public DLPPolicy() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.priority = 50;
        this.targetSystems = new HashSet<>();
        this.targetUsers = new HashSet<>();
        this.targetGroups = new HashSet<>();
        this.excludedUsers = new HashSet<>();
        this.excludedPaths = new HashSet<>();
        this.protectedDataTypes = new HashSet<>();
        this.customPatterns = new HashSet<>();
        this.minMatchCount = 1;
        this.confidenceThreshold = 0.7;
        this.createdAt = java.time.LocalDateTime.now();
        this.primaryAction = DLPAction.LOG_ONLY;
    }

    public DLPPolicy(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    // ═══════════════════════════════════════════════════════════
    // PREDEFINED DPDP-ALIGNED POLICIES
    // ═══════════════════════════════════════════════════════════

    public static List<DLPPolicy> getDefaultPolicies() {
        List<DLPPolicy> policies = new ArrayList<>();

        // Policy 1: Block Aadhaar External Transfer
        DLPPolicy aadhaarPolicy = new DLPPolicy("BLOCK_AADHAAR_TRANSFER",
                "Block external transfer of Aadhaar numbers");
        aadhaarPolicy.setPriority(100);
        aadhaarPolicy.getProtectedDataTypes().add(PIIType.AADHAAR);
        aadhaarPolicy.setMonitorNetwork(true);
        aadhaarPolicy.setMonitorEmail(true);
        aadhaarPolicy.setMonitorRemovableMedia(true);
        aadhaarPolicy.setPrimaryAction(DLPAction.BLOCK);
        aadhaarPolicy.setNotifyDPO(true);
        aadhaarPolicy.setDpdpSection("Section 2(t) - Personal Data");
        aadhaarPolicy.setSensitiveDataProtection(true);
        policies.add(aadhaarPolicy);

        // Policy 2: Encrypt PAN Data
        DLPPolicy panPolicy = new DLPPolicy("ENCRYPT_PAN_TRANSFER",
                "Encrypt PAN numbers before external transfer");
        panPolicy.setPriority(95);
        panPolicy.getProtectedDataTypes().add(PIIType.PAN);
        panPolicy.setMonitorNetwork(true);
        panPolicy.setMonitorEmail(true);
        panPolicy.setPrimaryAction(DLPAction.ENCRYPT);
        panPolicy.setNotifyUser(true);
        panPolicy.setDpdpSection("Section 2(t) - Personal Data");
        policies.add(panPolicy);

        // Policy 3: Block Cross-Border Personal Data
        DLPPolicy crossBorderPolicy = new DLPPolicy("BLOCK_CROSS_BORDER",
                "Block personal data transfer to non-approved countries");
        crossBorderPolicy.setPriority(100);
        crossBorderPolicy.getProtectedDataTypes().addAll(Arrays.asList(
                PIIType.AADHAAR, PIIType.PAN, PIIType.HEALTH_ID, PIIType.BANK_ACCOUNT));
        crossBorderPolicy.setMonitorNetwork(true);
        crossBorderPolicy.setPrimaryAction(DLPAction.BLOCK);
        crossBorderPolicy.setNotifyDPO(true);
        crossBorderPolicy.setDpdpSection("Section 16 - Cross-Border Transfer");
        crossBorderPolicy.setCrossBorderRestriction(true);
        policies.add(crossBorderPolicy);

        // Policy 4: Protect Health Records
        DLPPolicy healthPolicy = new DLPPolicy("PROTECT_HEALTH_DATA",
                "Block unauthorized health data transfer");
        healthPolicy.setPriority(100);
        healthPolicy.getProtectedDataTypes().addAll(Arrays.asList(
                PIIType.HEALTH_ID, PIIType.MEDICAL_RECORD));
        healthPolicy.setMonitorEndpoint(true);
        healthPolicy.setMonitorNetwork(true);
        healthPolicy.setMonitorEmail(true);
        healthPolicy.setPrimaryAction(DLPAction.BLOCK);
        healthPolicy.setNotifyDPO(true);
        healthPolicy.setDpdpSection("Section 2(t) - Sensitive Personal Data");
        healthPolicy.setSensitiveDataProtection(true);
        policies.add(healthPolicy);

        // Policy 5: Warn on Credit Card
        DLPPolicy creditCardPolicy = new DLPPolicy("WARN_CREDIT_CARD",
                "Warn when credit card data is being transferred");
        creditCardPolicy.setPriority(80);
        creditCardPolicy.getProtectedDataTypes().add(PIIType.CREDIT_CARD);
        creditCardPolicy.setMonitorEndpoint(true);
        creditCardPolicy.setMonitorNetwork(true);
        creditCardPolicy.setMonitorEmail(true);
        creditCardPolicy.setMinMatchCount(1);
        creditCardPolicy.setPrimaryAction(DLPAction.WARN);
        creditCardPolicy.setNotifyUser(true);
        creditCardPolicy.setNotifyManager(true);
        policies.add(creditCardPolicy);

        // Policy 6: Block Biometric Data
        DLPPolicy biometricPolicy = new DLPPolicy("BLOCK_BIOMETRIC",
                "Block all biometric data transfer");
        biometricPolicy.setPriority(100);
        biometricPolicy.getProtectedDataTypes().addAll(Arrays.asList(
                PIIType.FINGERPRINT, PIIType.IRIS, PIIType.FACIAL));
        biometricPolicy.setMonitorEndpoint(true);
        biometricPolicy.setMonitorNetwork(true);
        biometricPolicy.setMonitorRemovableMedia(true);
        biometricPolicy.setPrimaryAction(DLPAction.BLOCK);
        biometricPolicy.setNotifyDPO(true);
        biometricPolicy.setDpdpSection("Section 2(t) - Sensitive Personal Data (Biometric)");
        biometricPolicy.setSensitiveDataProtection(true);
        policies.add(biometricPolicy);

        // Policy 7: Quarantine Bulk PII Export
        DLPPolicy bulkExportPolicy = new DLPPolicy("QUARANTINE_BULK_EXPORT",
                "Quarantine files with high PII concentration");
        bulkExportPolicy.setPriority(90);
        bulkExportPolicy.getProtectedDataTypes().addAll(Arrays.asList(
                PIIType.EMAIL, PIIType.PHONE, PIIType.NAME, PIIType.ADDRESS));
        bulkExportPolicy.setMonitorEndpoint(true);
        bulkExportPolicy.setMonitorNetwork(true);
        bulkExportPolicy.setMinMatchCount(50); // 50+ matches
        bulkExportPolicy.setPrimaryAction(DLPAction.QUARANTINE);
        bulkExportPolicy.setNotifyDPO(true);
        bulkExportPolicy.setNotifyManager(true);
        policies.add(bulkExportPolicy);

        // Policy 8: Log All Bank Account Access
        DLPPolicy bankPolicy = new DLPPolicy("LOG_BANK_ACCOUNT",
                "Log all bank account number processing");
        bankPolicy.setPriority(60);
        bankPolicy.getProtectedDataTypes().add(PIIType.BANK_ACCOUNT);
        bankPolicy.setMonitorEndpoint(true);
        bankPolicy.setMonitorNetwork(true);
        bankPolicy.setPrimaryAction(DLPAction.LOG_ONLY);
        bankPolicy.setDpdpSection("Section 2(t) - Financial Personal Data");
        policies.add(bankPolicy);

        // Policy 9: Block USB for Sensitive Data
        DLPPolicy usbPolicy = new DLPPolicy("BLOCK_USB_SENSITIVE",
                "Block sensitive data copy to USB devices");
        usbPolicy.setPriority(95);
        usbPolicy.getProtectedDataTypes().addAll(Arrays.asList(
                PIIType.AADHAAR, PIIType.HEALTH_ID, PIIType.CREDIT_CARD, PIIType.BANK_ACCOUNT));
        usbPolicy.setMonitorRemovableMedia(true);
        usbPolicy.setPrimaryAction(DLPAction.BLOCK);
        usbPolicy.setNotifyUser(true);
        usbPolicy.setNotifyManager(true);
        policies.add(usbPolicy);

        // Policy 10: Redact PII in Print
        DLPPolicy printPolicy = new DLPPolicy("REDACT_PRINT_PII",
                "Redact PII when printing documents");
        printPolicy.setPriority(70);
        printPolicy.getProtectedDataTypes().addAll(Arrays.asList(
                PIIType.AADHAAR, PIIType.PAN, PIIType.CREDIT_CARD));
        printPolicy.setMonitorPrint(true);
        printPolicy.setPrimaryAction(DLPAction.REDACT);
        printPolicy.setNotifyUser(true);
        policies.add(printPolicy);

        return policies;
    }

    // ═══════════════════════════════════════════════════════════
    // BUILDER METHODS
    // ═══════════════════════════════════════════════════════════

    public DLPPolicy withPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public DLPPolicy addProtectedType(PIIType type) {
        this.protectedDataTypes.add(type);
        return this;
    }

    public DLPPolicy withAction(DLPAction action) {
        this.primaryAction = action;
        return this;
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Set<String> getTargetSystems() {
        return targetSystems;
    }

    public Set<String> getTargetUsers() {
        return targetUsers;
    }

    public Set<String> getTargetGroups() {
        return targetGroups;
    }

    public Set<String> getExcludedUsers() {
        return excludedUsers;
    }

    public Set<String> getExcludedPaths() {
        return excludedPaths;
    }

    public Set<PIIType> getProtectedDataTypes() {
        return protectedDataTypes;
    }

    public Set<String> getCustomPatterns() {
        return customPatterns;
    }

    public int getMinMatchCount() {
        return minMatchCount;
    }

    public void setMinMatchCount(int minMatchCount) {
        this.minMatchCount = minMatchCount;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double threshold) {
        this.confidenceThreshold = threshold;
    }

    public boolean isMonitorEndpoint() {
        return monitorEndpoint;
    }

    public void setMonitorEndpoint(boolean monitor) {
        this.monitorEndpoint = monitor;
    }

    public boolean isMonitorNetwork() {
        return monitorNetwork;
    }

    public void setMonitorNetwork(boolean monitor) {
        this.monitorNetwork = monitor;
    }

    public boolean isMonitorCloud() {
        return monitorCloud;
    }

    public void setMonitorCloud(boolean monitor) {
        this.monitorCloud = monitor;
    }

    public boolean isMonitorEmail() {
        return monitorEmail;
    }

    public void setMonitorEmail(boolean monitor) {
        this.monitorEmail = monitor;
    }

    public boolean isMonitorPrint() {
        return monitorPrint;
    }

    public void setMonitorPrint(boolean monitor) {
        this.monitorPrint = monitor;
    }

    public boolean isMonitorRemovableMedia() {
        return monitorRemovableMedia;
    }

    public void setMonitorRemovableMedia(boolean monitor) {
        this.monitorRemovableMedia = monitor;
    }

    public boolean isMonitorClipboard() {
        return monitorClipboard;
    }

    public void setMonitorClipboard(boolean monitor) {
        this.monitorClipboard = monitor;
    }

    public DLPAction getPrimaryAction() {
        return primaryAction;
    }

    public void setPrimaryAction(DLPAction action) {
        this.primaryAction = action;
    }

    public DLPAction getFallbackAction() {
        return fallbackAction;
    }

    public void setFallbackAction(DLPAction action) {
        this.fallbackAction = action;
    }

    public boolean isNotifyUser() {
        return notifyUser;
    }

    public void setNotifyUser(boolean notify) {
        this.notifyUser = notify;
    }

    public boolean isNotifyManager() {
        return notifyManager;
    }

    public void setNotifyManager(boolean notify) {
        this.notifyManager = notify;
    }

    public boolean isNotifyDPO() {
        return notifyDPO;
    }

    public void setNotifyDPO(boolean notify) {
        this.notifyDPO = notify;
    }

    public String getNotificationTemplate() {
        return notificationTemplate;
    }

    public void setNotificationTemplate(String template) {
        this.notificationTemplate = template;
    }

    public String getDpdpSection() {
        return dpdpSection;
    }

    public void setDpdpSection(String section) {
        this.dpdpSection = section;
    }

    public boolean isCrossBorderRestriction() {
        return crossBorderRestriction;
    }

    public void setCrossBorderRestriction(boolean restriction) {
        this.crossBorderRestriction = restriction;
    }

    public boolean isSensitiveDataProtection() {
        return sensitiveDataProtection;
    }

    public void setSensitiveDataProtection(boolean protection) {
        this.sensitiveDataProtection = protection;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
