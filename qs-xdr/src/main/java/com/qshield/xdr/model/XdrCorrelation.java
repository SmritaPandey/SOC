package com.qshield.xdr.model;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "xdr_correlations")
public class XdrCorrelation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Instant timestamp;
    @Column(nullable = false, length = 256) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 20) private String severity;
    @Column(length = 32) private String status;
    @Column(columnDefinition = "TEXT") private String dataSources; // JSON: SIEM, EDR, IDAM, NETWORK
    @Column(length = 128) private String mitreTactic;
    @Column(length = 128) private String mitreTechnique;
    @Column(columnDefinition = "TEXT") private String attackChain; // JSON kill chain
    @Column private Integer compositeRiskScore;
    @Column(columnDefinition = "TEXT") private String affectedAssets;
    @Column(columnDefinition = "TEXT") private String aiNarrative;
    public XdrCorrelation() {}
    public XdrCorrelation(String title, String severity) { this.timestamp = Instant.now(); this.title = title; this.severity = severity; this.status = "NEW"; }
    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant t) { this.timestamp = t; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getDataSources() { return dataSources; }
    public void setDataSources(String d) { this.dataSources = d; }
    public String getMitreTactic() { return mitreTactic; }
    public void setMitreTactic(String m) { this.mitreTactic = m; }
    public String getMitreTechnique() { return mitreTechnique; }
    public void setMitreTechnique(String m) { this.mitreTechnique = m; }
    public String getAttackChain() { return attackChain; }
    public void setAttackChain(String a) { this.attackChain = a; }
    public Integer getCompositeRiskScore() { return compositeRiskScore; }
    public void setCompositeRiskScore(Integer c) { this.compositeRiskScore = c; }
    public String getAffectedAssets() { return affectedAssets; }
    public void setAffectedAssets(String a) { this.affectedAssets = a; }
    public String getAiNarrative() { return aiNarrative; }
    public void setAiNarrative(String a) { this.aiNarrative = a; }
}
