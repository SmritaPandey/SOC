package com.qsdpdp.pii;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents the result of a PII scan operation
 * 
 * @version 1.0.0
 * @since Phase 6
 */
public class PIIScanResult {

    private String scanId;
    private String scanType; // FILE, DATABASE, TEXT, DIRECTORY
    private String source; // Path or identifier
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // IN_PROGRESS, COMPLETED, FAILED
    private long bytesScanned;
    private int filesScanned;
    private List<PIIFinding> findings;
    private Map<PIIType, Integer> findingsByType;
    private Map<String, Integer> findingsByRisk;
    private String error;
    private String scannedBy;

    public PIIScanResult() {
        this.scanId = UUID.randomUUID().toString();
        this.startTime = LocalDateTime.now();
        this.status = "IN_PROGRESS";
        this.findings = new ArrayList<>();
        this.findingsByType = new EnumMap<>(PIIType.class);
        this.findingsByRisk = new LinkedHashMap<>();
    }

    public PIIScanResult(String source, String scanType) {
        this();
        this.source = source;
        this.scanType = scanType;
    }

    // ═══════════════════════════════════════════════════════════
    // RESULT BUILDERS
    // ═══════════════════════════════════════════════════════════

    public void addFinding(PIIFinding finding) {
        finding.setScanId(this.scanId);
        findings.add(finding);

        // Update type counts
        findingsByType.merge(finding.getType(), 1, Integer::sum);

        // Update risk counts
        findingsByRisk.merge(finding.getRiskLevel(), 1, Integer::sum);
    }

    public void complete() {
        this.endTime = LocalDateTime.now();
        this.status = "COMPLETED";
    }

    public void fail(String error) {
        this.endTime = LocalDateTime.now();
        this.status = "FAILED";
        this.error = error;
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public int getTotalFindings() {
        return findings.size();
    }

    public int getCriticalFindings() {
        return findingsByRisk.getOrDefault("CRITICAL", 0);
    }

    public int getHighFindings() {
        return findingsByRisk.getOrDefault("HIGH", 0);
    }

    public int getMediumFindings() {
        return findingsByRisk.getOrDefault("MEDIUM", 0);
    }

    public int getLowFindings() {
        return findingsByRisk.getOrDefault("LOW", 0);
    }

    public int getSensitiveDataCount() {
        return (int) findings.stream()
                .filter(f -> f.getType().isSensitive())
                .count();
    }

    public double getScanDurationSeconds() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMillis() / 1000.0;
    }

    public String getRiskSummary() {
        return String.format("CRITICAL:%d, HIGH:%d, MEDIUM:%d, LOW:%d",
                getCriticalFindings(), getHighFindings(),
                getMediumFindings(), getLowFindings());
    }

    // ═══════════════════════════════════════════════════════════
    // FILTERING
    // ═══════════════════════════════════════════════════════════

    public List<PIIFinding> getFindingsByType(PIIType type) {
        return findings.stream()
                .filter(f -> f.getType() == type)
                .toList();
    }

    public List<PIIFinding> getFindingsByRisk(String riskLevel) {
        return findings.stream()
                .filter(f -> riskLevel.equals(f.getRiskLevel()))
                .toList();
    }

    public List<PIIFinding> getSensitiveFindings() {
        return findings.stream()
                .filter(f -> f.getType().isSensitive())
                .toList();
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public String getScanType() {
        return scanType;
    }

    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getBytesScanned() {
        return bytesScanned;
    }

    public void setBytesScanned(long bytesScanned) {
        this.bytesScanned = bytesScanned;
    }

    public int getFilesScanned() {
        return filesScanned;
    }

    public void setFilesScanned(int filesScanned) {
        this.filesScanned = filesScanned;
    }

    public List<PIIFinding> getFindings() {
        return findings;
    }

    public void setFindings(List<PIIFinding> findings) {
        this.findings = findings;
    }

    public Map<PIIType, Integer> getFindingsByType() {
        return findingsByType;
    }

    public Map<String, Integer> getFindingsByRiskLevel() {
        return findingsByRisk;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }

    @Override
    public String toString() {
        return String.format("PIIScanResult{scanId=%s, source=%s, status=%s, findings=%d, risk=[%s], duration=%.2fs}",
                scanId, source, status, getTotalFindings(), getRiskSummary(), getScanDurationSeconds());
    }
}
