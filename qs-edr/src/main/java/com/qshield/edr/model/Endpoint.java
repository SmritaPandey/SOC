package com.qshield.edr.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Endpoint — registered device being monitored by EDR. NIST SI-4 + SI-7. */
@Entity
@Table(name = "endpoints")
public class Endpoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 128, unique = true)
    private String hostname;
    @Column(length = 64)
    private String osType; // WINDOWS, LINUX, MACOS
    @Column(length = 64)
    private String osVersion;
    @Column(length = 64)
    private String ipAddress;
    @Column(length = 64)
    private String macAddress;
    @Column(length = 32)
    private String agentVersion;
    @Column(length = 20)
    private String status; // ONLINE, OFFLINE, ISOLATED, COMPROMISED
    @Column
    private Instant lastSeen;
    @Column
    private Integer riskScore;
    @Column
    private Boolean isolated = false;
    @Column(length = 128)
    private String owner;

    public Endpoint() {}
    public Endpoint(String hostname, String osType, String ipAddress) {
        this.hostname = hostname; this.osType = osType; this.ipAddress = ipAddress;
        this.status = "ONLINE"; this.lastSeen = Instant.now(); this.riskScore = 0;
    }

    public Long getId() { return id; }
    public String getHostname() { return hostname; }
    public void setHostname(String h) { this.hostname = h; }
    public String getOsType() { return osType; }
    public void setOsType(String o) { this.osType = o; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String o) { this.osVersion = o; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String i) { this.ipAddress = i; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String m) { this.macAddress = m; }
    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String a) { this.agentVersion = a; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant l) { this.lastSeen = l; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer r) { this.riskScore = r; }
    public Boolean getIsolated() { return isolated; }
    public void setIsolated(Boolean i) { this.isolated = i; }
    public String getOwner() { return owner; }
    public void setOwner(String o) { this.owner = o; }
}
