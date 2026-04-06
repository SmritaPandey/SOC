package com.qshield.av.model;
import jakarta.persistence.*; import java.time.Instant;

@Entity @Table(name = "av_scan_results")
public class ScanResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Instant timestamp;
    @Column(nullable = false, length = 512) private String filePath;
    @Column(length = 128) private String fileHash;
    @Column private Long fileSize;
    @Column(nullable = false, length = 20) private String verdict; // CLEAN, MALWARE, SUSPICIOUS, PUP
    @Column(length = 128) private String threatName;
    @Column(length = 64) private String detectionEngine; // SIGNATURE, NEURAL_NET, HEURISTIC, RANDOM_FOREST, YARA
    @Column private Integer confidenceScore;
    @Column(length = 32) private String action; // QUARANTINED, DELETED, ALLOWED, BLOCKED
    @Column(length = 128) private String hostname;
    public ScanResult() {}
    public ScanResult(String filePath, String verdict, String detectionEngine) {
        this.timestamp = Instant.now(); this.filePath = filePath; this.verdict = verdict; this.detectionEngine = detectionEngine;
    }
    public Long getId() { return id; } public Instant getTimestamp() { return timestamp; }
    public String getFilePath() { return filePath; } public void setFilePath(String f) { this.filePath = f; }
    public String getFileHash() { return fileHash; } public void setFileHash(String f) { this.fileHash = f; }
    public Long getFileSize() { return fileSize; } public void setFileSize(Long f) { this.fileSize = f; }
    public String getVerdict() { return verdict; } public void setVerdict(String v) { this.verdict = v; }
    public String getThreatName() { return threatName; } public void setThreatName(String t) { this.threatName = t; }
    public String getDetectionEngine() { return detectionEngine; } public void setDetectionEngine(String d) { this.detectionEngine = d; }
    public Integer getConfidenceScore() { return confidenceScore; } public void setConfidenceScore(Integer c) { this.confidenceScore = c; }
    public String getAction() { return action; } public void setAction(String a) { this.action = a; }
    public String getHostname() { return hostname; } public void setHostname(String h) { this.hostname = h; }
}
