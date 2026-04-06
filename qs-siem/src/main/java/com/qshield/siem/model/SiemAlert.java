package com.qshield.siem.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * SIEM Alert — generated when correlation rules or AI detect threats.
 */
@Entity
@Table(name = "siem_alerts")
public class SiemAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(length = 32)
    private String status; // NEW, INVESTIGATING, RESOLVED, FALSE_POSITIVE

    @Column(length = 128)
    private String sourceIp;

    @Column(length = 128)
    private String destinationIp;

    @Column(length = 128)
    private String ruleId;

    @Column(length = 128)
    private String mitreTactic;

    @Column(length = 128)
    private String mitreTechnique;

    @Column
    private Integer threatScore;

    @Column
    private Integer relatedEventCount;

    @Column(length = 128)
    private String assignedTo;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis; // RAG AI threat narrative

    public SiemAlert() {}

    public SiemAlert(String title, String severity, String sourceIp, String ruleId) {
        this.createdAt = Instant.now();
        this.title = title;
        this.severity = severity;
        this.sourceIp = sourceIp;
        this.ruleId = ruleId;
        this.status = "NEW";
    }

    // Getters and setters
    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String s) { this.sourceIp = s; }
    public String getDestinationIp() { return destinationIp; }
    public void setDestinationIp(String d) { this.destinationIp = d; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String r) { this.ruleId = r; }
    public String getMitreTactic() { return mitreTactic; }
    public void setMitreTactic(String m) { this.mitreTactic = m; }
    public String getMitreTechnique() { return mitreTechnique; }
    public void setMitreTechnique(String m) { this.mitreTechnique = m; }
    public Integer getThreatScore() { return threatScore; }
    public void setThreatScore(Integer t) { this.threatScore = t; }
    public Integer getRelatedEventCount() { return relatedEventCount; }
    public void setRelatedEventCount(Integer c) { this.relatedEventCount = c; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String a) { this.assignedTo = a; }
    public String getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(String a) { this.aiAnalysis = a; }
}
