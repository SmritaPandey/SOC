package com.qshield.edr.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Endpoint Event — telemetry collected from EDR agents. */
@Entity
@Table(name = "endpoint_events", indexes = {
    @Index(name = "idx_ep_event_time", columnList = "timestamp"),
    @Index(name = "idx_ep_event_host", columnList = "hostname")
})
public class EndpointEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Instant timestamp;
    @Column(nullable = false, length = 128)
    private String hostname;
    @Column(nullable = false, length = 32)
    private String eventType; // PROCESS_CREATE, FILE_MODIFY, NETWORK_CONNECT, REGISTRY_MODIFY, DRIVER_LOAD
    @Column(length = 256)
    private String processName;
    @Column
    private Integer processId;
    @Column(length = 256)
    private String parentProcess;
    @Column(length = 512)
    private String filePath;
    @Column(length = 128)
    private String fileHash;
    @Column(length = 64)
    private String remoteIp;
    @Column
    private Integer remotePort;
    @Column(length = 20)
    private String severity;
    @Column
    private Integer riskScore;
    @Column(length = 128)
    private String mitreTechnique;
    @Column(columnDefinition = "TEXT")
    private String rawData;

    public EndpointEvent() {}
    public EndpointEvent(String hostname, String eventType, String processName) {
        this.timestamp = Instant.now(); this.hostname = hostname; this.eventType = eventType; this.processName = processName;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant t) { this.timestamp = t; }
    public String getHostname() { return hostname; }
    public void setHostname(String h) { this.hostname = h; }
    public String getEventType() { return eventType; }
    public void setEventType(String e) { this.eventType = e; }
    public String getProcessName() { return processName; }
    public void setProcessName(String p) { this.processName = p; }
    public Integer getProcessId() { return processId; }
    public void setProcessId(Integer p) { this.processId = p; }
    public String getParentProcess() { return parentProcess; }
    public void setParentProcess(String p) { this.parentProcess = p; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String f) { this.filePath = f; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String f) { this.fileHash = f; }
    public String getRemoteIp() { return remoteIp; }
    public void setRemoteIp(String r) { this.remoteIp = r; }
    public Integer getRemotePort() { return remotePort; }
    public void setRemotePort(Integer r) { this.remotePort = r; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer r) { this.riskScore = r; }
    public String getMitreTechnique() { return mitreTechnique; }
    public void setMitreTechnique(String m) { this.mitreTechnique = m; }
    public String getRawData() { return rawData; }
    public void setRawData(String r) { this.rawData = r; }
}
