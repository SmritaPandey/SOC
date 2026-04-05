package com.qsdpdp.dlp;

import com.qsdpdp.pii.PIIType;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DLP Incident model for policy violation events
 * 
 * @version 1.0.0
 * @since Module 7
 */
public class DLPIncident {

    private String id;
    private String policyId;
    private String policyName;
    private DLPAction actionTaken;
    private String severity;

    // Source
    private String sourceUser;
    private String sourceSystem;
    private String sourcePath;
    private String sourceApplication;

    // Destination
    private String destinationType; // EMAIL, USB, NETWORK, CLOUD, PRINT, CLIPBOARD
    private String destinationAddress;
    private String destinationCountry;

    // Data
    private Set<PIIType> detectedDataTypes;
    private int matchCount;
    private double confidenceScore;
    private String dataSnippet; // Masked
    private long dataSize;

    // Context
    private String processName;
    private String fileName;
    private String fileHash;

    // Status
    private String status; // OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE
    private String assignedTo;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String resolution;
    private String notes;

    // DPDP
    private String dpdpSection;
    private boolean breachIndicator;
    private boolean notificationRequired;

    public DLPIncident() {
        this.id = UUID.randomUUID().toString();
        this.detectedAt = LocalDateTime.now();
        this.status = "OPEN";
        this.detectedDataTypes = new HashSet<>();
    }

    public DLPIncident(DLPPolicy policy, String sourceUser, String sourcePath) {
        this();
        this.policyId = policy.getId();
        this.policyName = policy.getName();
        this.actionTaken = policy.getPrimaryAction();
        this.sourceUser = sourceUser;
        this.sourcePath = sourcePath;
        this.dpdpSection = policy.getDpdpSection();
        this.severity = determineSeverity(policy);
    }

    private String determineSeverity(DLPPolicy policy) {
        if (policy.isSensitiveDataProtection())
            return "CRITICAL";
        if (policy.isCrossBorderRestriction())
            return "CRITICAL";
        if (policy.getPrimaryAction() == DLPAction.BLOCK)
            return "HIGH";
        if (policy.getPrimaryAction() == DLPAction.QUARANTINE)
            return "HIGH";
        if (policy.getPrimaryAction() == DLPAction.WARN)
            return "MEDIUM";
        return "LOW";
    }

    public boolean isSensitiveData() {
        return detectedDataTypes.stream().anyMatch(PIIType::isSensitive);
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

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public DLPAction getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(DLPAction actionTaken) {
        this.actionTaken = actionTaken;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSourceUser() {
        return sourceUser;
    }

    public void setSourceUser(String sourceUser) {
        this.sourceUser = sourceUser;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getSourceApplication() {
        return sourceApplication;
    }

    public void setSourceApplication(String app) {
        this.sourceApplication = app;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String type) {
        this.destinationType = type;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String address) {
        this.destinationAddress = address;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public void setDestinationCountry(String country) {
        this.destinationCountry = country;
    }

    public Set<PIIType> getDetectedDataTypes() {
        return detectedDataTypes;
    }

    public void setDetectedDataTypes(Set<PIIType> types) {
        this.detectedDataTypes = types;
    }

    public int getMatchCount() {
        return matchCount;
    }

    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double score) {
        this.confidenceScore = score;
    }

    public String getDataSnippet() {
        return dataSnippet;
    }

    public void setDataSnippet(String snippet) {
        this.dataSnippet = snippet;
    }

    public long getDataSize() {
        return dataSize;
    }

    public void setDataSize(long dataSize) {
        this.dataSize = dataSize;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDpdpSection() {
        return dpdpSection;
    }

    public void setDpdpSection(String section) {
        this.dpdpSection = section;
    }

    public boolean isBreachIndicator() {
        return breachIndicator;
    }

    public void setBreachIndicator(boolean indicator) {
        this.breachIndicator = indicator;
    }

    public boolean isNotificationRequired() {
        return notificationRequired;
    }

    public void setNotificationRequired(boolean required) {
        this.notificationRequired = required;
    }
}
