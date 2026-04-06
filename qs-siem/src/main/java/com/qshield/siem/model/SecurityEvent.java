package com.qshield.siem.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Security Event — core entity for SIEM log ingestion.
 * Supports Syslog RFC 5424, CEF, LEEF, JSON, Windows Event Log.
 */
@Entity
@Table(name = "security_events", indexes = {
    @Index(name = "idx_event_timestamp", columnList = "timestamp"),
    @Index(name = "idx_event_severity", columnList = "severity"),
    @Index(name = "idx_event_source", columnList = "sourceIp"),
    @Index(name = "idx_event_category", columnList = "category")
})
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 20)
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW, INFO

    @Column(length = 64)
    private String category; // AUTH, NETWORK, MALWARE, POLICY, SYSTEM, CUSTOM

    @Column(length = 64)
    private String sourceIp;

    @Column(length = 64)
    private String destinationIp;

    @Column
    private Integer sourcePort;

    @Column
    private Integer destinationPort;

    @Column(length = 128)
    private String protocol;

    @Column(length = 256)
    private String sourceName; // hostname, device name

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rawLog;

    @Column(columnDefinition = "TEXT")
    private String normalizedMessage;

    @Column(length = 128)
    private String userName;

    @Column(length = 64)
    private String action; // ALLOW, DENY, DROP, ALERT, BLOCK

    @Column(length = 128)
    private String ruleId; // correlation rule that matched

    @Column
    private Integer threatScore; // 1-100 AI-computed threat score

    @Column(length = 64)
    private String mitreTactic; // MITRE ATT&CK tactic

    @Column(length = 128)
    private String mitreTechnique; // MITRE ATT&CK technique ID

    @Column
    private Boolean acknowledged = false;

    @Column(length = 32)
    private String logFormat; // SYSLOG, CEF, LEEF, JSON, WINEVENT

    // Constructors
    public SecurityEvent() {}

    public SecurityEvent(String severity, String category, String sourceIp, String rawLog) {
        this.timestamp = Instant.now();
        this.severity = severity;
        this.category = category;
        this.sourceIp = sourceIp;
        this.rawLog = rawLog;
    }

    // Getters and setters
    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant t) { this.timestamp = t; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String s) { this.sourceIp = s; }
    public String getDestinationIp() { return destinationIp; }
    public void setDestinationIp(String d) { this.destinationIp = d; }
    public Integer getSourcePort() { return sourcePort; }
    public void setSourcePort(Integer p) { this.sourcePort = p; }
    public Integer getDestinationPort() { return destinationPort; }
    public void setDestinationPort(Integer p) { this.destinationPort = p; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String p) { this.protocol = p; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String s) { this.sourceName = s; }
    public String getRawLog() { return rawLog; }
    public void setRawLog(String r) { this.rawLog = r; }
    public String getNormalizedMessage() { return normalizedMessage; }
    public void setNormalizedMessage(String n) { this.normalizedMessage = n; }
    public String getUserName() { return userName; }
    public void setUserName(String u) { this.userName = u; }
    public String getAction() { return action; }
    public void setAction(String a) { this.action = a; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String r) { this.ruleId = r; }
    public Integer getThreatScore() { return threatScore; }
    public void setThreatScore(Integer t) { this.threatScore = t; }
    public String getMitreTactic() { return mitreTactic; }
    public void setMitreTactic(String m) { this.mitreTactic = m; }
    public String getMitreTechnique() { return mitreTechnique; }
    public void setMitreTechnique(String m) { this.mitreTechnique = m; }
    public Boolean getAcknowledged() { return acknowledged; }
    public void setAcknowledged(Boolean a) { this.acknowledged = a; }
    public String getLogFormat() { return logFormat; }
    public void setLogFormat(String l) { this.logFormat = l; }
}
