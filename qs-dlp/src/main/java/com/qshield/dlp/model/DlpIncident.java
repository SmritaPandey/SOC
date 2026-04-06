package com.qshield.dlp.model;
import jakarta.persistence.*; import java.time.Instant;

@Entity @Table(name = "dlp_incidents")
public class DlpIncident {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Instant timestamp;
    @Column(nullable = false, length = 256) private String title;
    @Column(length = 20) private String severity;
    @Column(length = 32) private String status; // DETECTED, BLOCKED, QUARANTINED, RESOLVED
    @Column(length = 64) private String dataType; // PII, PHI, PCI, CONFIDENTIAL, SECRET
    @Column(columnDefinition = "TEXT") private String matchedPatterns;
    @Column(length = 256) private String sourceUser;
    @Column(length = 256) private String sourcePath;
    @Column(length = 256) private String destination;
    @Column(length = 32) private String channel; // EMAIL, USB, CLOUD, PRINTER, CLIPBOARD
    @Column(length = 32) private String action; // BLOCK, ENCRYPT, MASK, ALERT, QUARANTINE
    @Column(length = 128) private String policyId;
    @Column private Integer confidenceScore;
    public DlpIncident() {}
    public DlpIncident(String title, String severity, String dataType, String channel) {
        this.timestamp = Instant.now(); this.title = title; this.severity = severity; this.dataType = dataType; this.channel = channel; this.status = "DETECTED";
    }
    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getDataType() { return dataType; }
    public void setDataType(String d) { this.dataType = d; }
    public String getMatchedPatterns() { return matchedPatterns; }
    public void setMatchedPatterns(String m) { this.matchedPatterns = m; }
    public String getSourceUser() { return sourceUser; }
    public void setSourceUser(String s) { this.sourceUser = s; }
    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String s) { this.sourcePath = s; }
    public String getDestination() { return destination; }
    public void setDestination(String d) { this.destination = d; }
    public String getChannel() { return channel; }
    public void setChannel(String c) { this.channel = c; }
    public String getAction() { return action; }
    public void setAction(String a) { this.action = a; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String p) { this.policyId = p; }
    public Integer getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Integer c) { this.confidenceScore = c; }
}
