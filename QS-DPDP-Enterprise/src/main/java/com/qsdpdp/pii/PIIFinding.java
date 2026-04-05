package com.qsdpdp.pii;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single PII finding from a scan
 * 
 * @version 1.0.0
 * @since Phase 6
 */
public class PIIFinding {

    private String id;
    private String scanId;
    private PIIType type;
    private String patternId;
    private String maskedValue; // Partially masked value for display
    private String hash; // SHA-256 hash of actual value
    private String sourcePath; // File path or table.column
    private int lineNumber;
    private int columnStart;
    private int columnEnd;
    private double confidence;
    private boolean validated;
    private String context; // Surrounding text (masked)
    private String riskLevel;
    private LocalDateTime foundAt;
    private String status; // ACTIVE, REMEDIATED, FALSE_POSITIVE

    public PIIFinding() {
        this.id = UUID.randomUUID().toString();
        this.foundAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    // Builder pattern for convenient construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PIIFinding finding = new PIIFinding();

        public Builder type(PIIType type) {
            finding.type = type;
            finding.riskLevel = type.getRiskLevel();
            return this;
        }

        public Builder scanId(String scanId) {
            finding.scanId = scanId;
            return this;
        }

        public Builder patternId(String patternId) {
            finding.patternId = patternId;
            return this;
        }

        public Builder value(String value, String hash) {
            finding.maskedValue = maskValue(value, finding.type);
            finding.hash = hash;
            return this;
        }

        public Builder location(String sourcePath, int lineNumber, int columnStart, int columnEnd) {
            finding.sourcePath = sourcePath;
            finding.lineNumber = lineNumber;
            finding.columnStart = columnStart;
            finding.columnEnd = columnEnd;
            return this;
        }

        public Builder confidence(double confidence) {
            finding.confidence = confidence;
            return this;
        }

        public Builder validated(boolean validated) {
            finding.validated = validated;
            return this;
        }

        public Builder context(String context) {
            finding.context = context;
            return this;
        }

        public PIIFinding build() {
            return finding;
        }

        /**
         * Mask PII value for safe display
         */
        private String maskValue(String value, PIIType type) {
            if (value == null || value.length() < 4) {
                return "****";
            }

            String cleanValue = value.replaceAll("[\\s-]", "");
            int len = cleanValue.length();

            return switch (type) {
                case AADHAAR -> "XXXX-XXXX-" + cleanValue.substring(len - 4);
                case PAN -> cleanValue.substring(0, 2) + "XXXXX" + cleanValue.substring(len - 3);
                case CREDIT_CARD -> "XXXX-XXXX-XXXX-" + cleanValue.substring(len - 4);
                case PHONE -> "+91-XXXXX-" + cleanValue.substring(len - 5);
                case EMAIL -> {
                    int atIndex = value.indexOf('@');
                    if (atIndex > 2) {
                        yield value.substring(0, 2) + "***" + value.substring(atIndex);
                    }
                    yield "***" + value.substring(atIndex);
                }
                case BANK_ACCOUNT -> "XXXXXXXX" + cleanValue.substring(len - 4);
                default -> {
                    if (len > 6) {
                        yield cleanValue.substring(0, 2) + "X".repeat(len - 4) + cleanValue.substring(len - 2);
                    }
                    yield "****";
                }
            };
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS AND SETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScanId() {
        return scanId;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public PIIType getType() {
        return type;
    }

    public void setType(PIIType type) {
        this.type = type;
    }

    public String getPatternId() {
        return patternId;
    }

    public void setPatternId(String patternId) {
        this.patternId = patternId;
    }

    public String getMaskedValue() {
        return maskedValue;
    }

    public void setMaskedValue(String maskedValue) {
        this.maskedValue = maskedValue;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getColumnStart() {
        return columnStart;
    }

    public void setColumnStart(int columnStart) {
        this.columnStart = columnStart;
    }

    public int getColumnEnd() {
        return columnEnd;
    }

    public void setColumnEnd(int columnEnd) {
        this.columnEnd = columnEnd;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public LocalDateTime getFoundAt() {
        return foundAt;
    }

    public void setFoundAt(LocalDateTime foundAt) {
        this.foundAt = foundAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("PIIFinding{type=%s, masked=%s, source=%s:%d, confidence=%.2f, risk=%s}",
                type, maskedValue, sourcePath, lineNumber, confidence, riskLevel);
    }
}
