package com.qsdpdp.core;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

/**
 * Incident Entity — Global Data Model
 * Represents a security/compliance incident per DPDP Act 2023 and CERT-In guidelines.
 * Tracks full lifecycle: Detection → Classification → Response → Notification → Closure.
 *
 * @version 1.0.0
 * @since Module 6
 */
public class Incident {

    public enum IncidentType {
        DATA_BREACH("Personal Data Breach"),
        UNAUTHORIZED_ACCESS("Unauthorized Access"),
        DATA_LOSS("Data Loss"),
        MALWARE("Malware/Ransomware"),
        PHISHING("Phishing Attack"),
        INSIDER_THREAT("Insider Threat"),
        POLICY_VIOLATION("Policy Violation"),
        CONSENT_VIOLATION("Consent Violation"),
        CROSS_BORDER_VIOLATION("Cross-Border Transfer Violation"),
        DLP_TRIGGER("DLP Policy Trigger"),
        SYSTEM_COMPROMISE("System Compromise"),
        THIRD_PARTY_BREACH("Third-Party Vendor Breach");

        private final String description;
        IncidentType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public enum Severity {
        CRITICAL(1, "Immediate response required", 1),      // Report within 1 hour
        HIGH(2, "Urgent response required", 6),              // Report within 6 hours
        MEDIUM(3, "Timely response required", 24),           // Report within 24 hours
        LOW(4, "Scheduled response", 72),                    // Report within 72 hours
        INFORMATIONAL(5, "Awareness only", 168);             // Weekly review

        private final int level;
        private final String responseGuideline;
        private final int reportingHours;  // CERT-In reporting window

        Severity(int level, String responseGuideline, int reportingHours) {
            this.level = level;
            this.responseGuideline = responseGuideline;
            this.reportingHours = reportingHours;
        }

        public int getLevel() { return level; }
        public String getResponseGuideline() { return responseGuideline; }
        public int getReportingHours() { return reportingHours; }
    }

    public enum ClosureStatus {
        OPEN, IN_PROGRESS, CONTAINED, REMEDIATED, CLOSED, REOPENED
    }

    private String incidentId;
    private IncidentType type;
    private Severity severity;
    private int affectedRecords;
    private LocalDateTime detectionTime;
    private LocalDateTime responseTime;
    private LocalDateTime containmentTime;
    private LocalDateTime notificationTime;
    private LocalDateTime closureTime;
    private ClosureStatus closureStatus;
    private String description;
    private String affectedSystems;
    private String affectedDataCategories;
    private String rootCause;
    private String remediationActions;
    private String reportedBy;
    private String assignedTo;
    private String escalatedTo;
    private boolean regulatorNotified;
    private boolean principalsNotified;
    private int principalsNotifiedCount;
    private String certInReferenceId;        // CERT-In incident reference
    private String dpbReferenceId;           // Data Protection Board reference
    private List<String> affectedPrincipalIds;
    private List<String> impactedAssetIds;
    private List<String> timeline;            // Event timeline entries
    private Map<String, String> evidence;     // Evidence artifacts
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Incident() {
        this.incidentId = UUID.randomUUID().toString();
        this.closureStatus = ClosureStatus.OPEN;
        this.detectionTime = LocalDateTime.now();
        this.affectedPrincipalIds = new ArrayList<>();
        this.impactedAssetIds = new ArrayList<>();
        this.timeline = new ArrayList<>();
        this.evidence = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        addTimelineEntry("Incident detected");
    }

    // --- Getters and Setters ---

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public IncidentType getType() { return type; }
    public void setType(IncidentType type) { this.type = type; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public int getAffectedRecords() { return affectedRecords; }
    public void setAffectedRecords(int affectedRecords) { this.affectedRecords = affectedRecords; }

    public LocalDateTime getDetectionTime() { return detectionTime; }
    public void setDetectionTime(LocalDateTime detectionTime) { this.detectionTime = detectionTime; }

    public LocalDateTime getResponseTime() { return responseTime; }
    public void setResponseTime(LocalDateTime responseTime) { this.responseTime = responseTime; }

    public LocalDateTime getContainmentTime() { return containmentTime; }
    public void setContainmentTime(LocalDateTime containmentTime) { this.containmentTime = containmentTime; }

    public LocalDateTime getNotificationTime() { return notificationTime; }
    public void setNotificationTime(LocalDateTime notificationTime) { this.notificationTime = notificationTime; }

    public LocalDateTime getClosureTime() { return closureTime; }
    public void setClosureTime(LocalDateTime closureTime) { this.closureTime = closureTime; }

    public ClosureStatus getClosureStatus() { return closureStatus; }
    public void setClosureStatus(ClosureStatus closureStatus) { this.closureStatus = closureStatus; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAffectedSystems() { return affectedSystems; }
    public void setAffectedSystems(String affectedSystems) { this.affectedSystems = affectedSystems; }

    public String getAffectedDataCategories() { return affectedDataCategories; }
    public void setAffectedDataCategories(String affectedDataCategories) { this.affectedDataCategories = affectedDataCategories; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getRemediationActions() { return remediationActions; }
    public void setRemediationActions(String remediationActions) { this.remediationActions = remediationActions; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getEscalatedTo() { return escalatedTo; }
    public void setEscalatedTo(String escalatedTo) { this.escalatedTo = escalatedTo; }

    public boolean isRegulatorNotified() { return regulatorNotified; }
    public void setRegulatorNotified(boolean regulatorNotified) { this.regulatorNotified = regulatorNotified; }

    public boolean isPrincipalsNotified() { return principalsNotified; }
    public void setPrincipalsNotified(boolean principalsNotified) { this.principalsNotified = principalsNotified; }

    public int getPrincipalsNotifiedCount() { return principalsNotifiedCount; }
    public void setPrincipalsNotifiedCount(int principalsNotifiedCount) { this.principalsNotifiedCount = principalsNotifiedCount; }

    public String getCertInReferenceId() { return certInReferenceId; }
    public void setCertInReferenceId(String certInReferenceId) { this.certInReferenceId = certInReferenceId; }

    public String getDpbReferenceId() { return dpbReferenceId; }
    public void setDpbReferenceId(String dpbReferenceId) { this.dpbReferenceId = dpbReferenceId; }

    public List<String> getAffectedPrincipalIds() { return affectedPrincipalIds; }
    public void setAffectedPrincipalIds(List<String> affectedPrincipalIds) { this.affectedPrincipalIds = affectedPrincipalIds; }

    public List<String> getImpactedAssetIds() { return impactedAssetIds; }
    public void setImpactedAssetIds(List<String> impactedAssetIds) { this.impactedAssetIds = impactedAssetIds; }

    public List<String> getTimeline() { return timeline; }
    public void setTimeline(List<String> timeline) { this.timeline = timeline; }

    public Map<String, String> getEvidence() { return evidence; }
    public void setEvidence(Map<String, String> evidence) { this.evidence = evidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // --- Business Logic ---

    /**
     * Add a timeline entry with timestamp
     */
    public void addTimelineEntry(String entry) {
        timeline.add(LocalDateTime.now() + ": " + entry);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Add evidence artifact
     */
    public void addEvidence(String key, String value) {
        evidence.put(key, value);
    }

    /**
     * Record response initiation
     */
    public void recordResponse(String assignee) {
        this.responseTime = LocalDateTime.now();
        this.assignedTo = assignee;
        this.closureStatus = ClosureStatus.IN_PROGRESS;
        addTimelineEntry("Response initiated by " + assignee);
    }

    /**
     * Record containment
     */
    public void recordContainment(String actions) {
        this.containmentTime = LocalDateTime.now();
        this.closureStatus = ClosureStatus.CONTAINED;
        this.remediationActions = actions;
        addTimelineEntry("Incident contained: " + actions);
    }

    /**
     * Complete incident closure
     */
    public void closeIncident(String rootCause, String remediation) {
        this.closureTime = LocalDateTime.now();
        this.closureStatus = ClosureStatus.CLOSED;
        this.rootCause = rootCause;
        this.remediationActions = remediation;
        addTimelineEntry("Incident closed");
    }

    /**
     * Calculate response time in minutes
     */
    public long getResponseTimeMinutes() {
        if (responseTime == null || detectionTime == null) return -1;
        return Duration.between(detectionTime, responseTime).toMinutes();
    }

    /**
     * Calculate total incident duration in hours
     */
    public long getIncidentDurationHours() {
        LocalDateTime endTime = closureTime != null ? closureTime : LocalDateTime.now();
        return Duration.between(detectionTime, endTime).toHours();
    }

    /**
     * Check if CERT-In reporting deadline is breached
     */
    public boolean isReportingDeadlineBreached() {
        if (severity == null) return false;
        long hoursElapsed = Duration.between(detectionTime, LocalDateTime.now()).toHours();
        return !regulatorNotified && hoursElapsed > severity.getReportingHours();
    }

    /**
     * Check if this incident requires DPDP Board notification per Sections 8(6)
     */
    public boolean requiresDPBNotification() {
        return severity == Severity.CRITICAL || severity == Severity.HIGH || affectedRecords > 100;
    }

    /**
     * Check if this incident requires CERT-In notification
     */
    public boolean requiresCertInNotification() {
        return type == IncidentType.DATA_BREACH ||
               type == IncidentType.MALWARE ||
               type == IncidentType.SYSTEM_COMPROMISE ||
               type == IncidentType.UNAUTHORIZED_ACCESS;
    }
}
