package com.qshield.common.audit;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Immutable Audit Trail Entity — NIST AU-3 (Content of Audit Records).
 * Hash-chained for tamper evidence (NIST AU-10 Non-Repudiation).
 */
@Entity
@Table(name = "audit_trail")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 32)
    private String product;  // SIEM, SOAR, EDR, XDR, DLP, IDAM, AV, VAM

    @Column(nullable = false, length = 64)
    private String eventType;  // LOGIN, LOGOUT, ALERT_CREATED, INCIDENT_UPDATED, etc.

    @Column(length = 128)
    private String userId;

    @Column(length = 64)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 10)
    private String severity;  // INFO, WARN, ERROR, CRITICAL

    @Column(length = 128)
    private String previousHash;

    @Column(length = 128)
    private String currentHash;

    public AuditEvent() {}

    public AuditEvent(String product, String eventType, String userId, String ipAddress,
                      String details, String severity) {
        this.timestamp = Instant.now();
        this.product = product;
        this.eventType = eventType;
        this.userId = userId;
        this.ipAddress = ipAddress;
        this.details = details;
        this.severity = severity;
    }

    // Getters and setters
    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getProduct() { return product; }
    public String getEventType() { return eventType; }
    public String getUserId() { return userId; }
    public String getIpAddress() { return ipAddress; }
    public String getDetails() { return details; }
    public String getSeverity() { return severity; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String h) { this.previousHash = h; }
    public String getCurrentHash() { return currentHash; }
    public void setCurrentHash(String h) { this.currentHash = h; }
}
