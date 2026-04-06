package com.qshield.soar.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Incident — core entity for SOAR case management.
 * Lifecycle: DETECTED → TRIAGED → INVESTIGATING → CONTAINED → REMEDIATED → CLOSED
 * Implements NIST IR-4, IR-5, IR-6, IR-8 and ISO 27035.
 */
@Entity
@Table(name = "incidents")
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32, unique = true)
    private String incidentId; // INC-YYYYMMDD-XXXX

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(nullable = false, length = 32)
    private String status; // DETECTED, TRIAGED, INVESTIGATING, CONTAINED, REMEDIATED, CLOSED, FALSE_POSITIVE

    @Column(length = 64)
    private String category; // MALWARE, PHISHING, DATA_BREACH, INSIDER_THREAT, DDOS, UNAUTHORIZED_ACCESS

    @Column(length = 128)
    private String assignedTo;

    @Column(length = 128)
    private String sourceAlertId;

    @Column(length = 128)
    private String playbookId;

    @Column(columnDefinition = "TEXT")
    private String timeline; // JSON array of timeline events

    @Column(columnDefinition = "TEXT")
    private String responseActions; // JSON array of actions taken

    @Column
    private Integer impactScore;

    @Column(length = 128)
    private String mitreTactic;

    @Column(length = 128)
    private String mitreTechnique;

    @Column(columnDefinition = "TEXT")
    private String aiRecommendation;

    public Incident() {}

    public Incident(String title, String severity, String category) {
        this.incidentId = "INC-" + java.time.LocalDate.now().toString().replace("-","") + "-" +
                String.format("%04d", (int)(Math.random()*9999));
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.title = title;
        this.severity = severity;
        this.category = category;
        this.status = "DETECTED";
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getIncidentId() { return incidentId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant u) { this.updatedAt = u; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; this.updatedAt = Instant.now(); }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String a) { this.assignedTo = a; }
    public String getSourceAlertId() { return sourceAlertId; }
    public void setSourceAlertId(String s) { this.sourceAlertId = s; }
    public String getPlaybookId() { return playbookId; }
    public void setPlaybookId(String p) { this.playbookId = p; }
    public String getTimeline() { return timeline; }
    public void setTimeline(String t) { this.timeline = t; }
    public String getResponseActions() { return responseActions; }
    public void setResponseActions(String r) { this.responseActions = r; }
    public Integer getImpactScore() { return impactScore; }
    public void setImpactScore(Integer i) { this.impactScore = i; }
    public String getMitreTactic() { return mitreTactic; }
    public void setMitreTactic(String m) { this.mitreTactic = m; }
    public String getMitreTechnique() { return mitreTechnique; }
    public void setMitreTechnique(String m) { this.mitreTechnique = m; }
    public String getAiRecommendation() { return aiRecommendation; }
    public void setAiRecommendation(String a) { this.aiRecommendation = a; }
}
