package com.qsdpdp.dlp;

import com.qsdpdp.pii.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Data Classification Service - Automatic data labeling engine
 * Classifies data into protection levels based on PII content analysis
 */
public class DataClassificationService {
    private static final Logger logger = LoggerFactory.getLogger(DataClassificationService.class);
    private boolean initialized = false;
    private final Map<String, ClassificationResult> classificationCache = new LinkedHashMap<>();

    public enum ClassificationLevel {
        PUBLIC("Public", 0, "No restrictions"),
        INTERNAL("Internal", 1, "Internal use only"),
        CONFIDENTIAL("Confidential", 2, "Restricted access, encryption required"),
        RESTRICTED("Restricted", 3, "Strict access controls, encryption mandatory"),
        TOP_SECRET("Top Secret", 4, "Maximum security, need-to-know basis, full audit");

        public final String displayName; public final int severity; public final String description;
        ClassificationLevel(String d, int s, String desc) { this.displayName = d; this.severity = s; this.description = desc; }
    }

    public void initialize() {
        if (initialized) return;
        initialized = true;
        logger.info("DataClassificationService initialized");
    }

    /** Classify data based on PII scan results */
    public ClassificationResult classifyFromScanResult(PIIScanResult scanResult) {
        ClassificationResult result = new ClassificationResult();
        result.id = UUID.randomUUID().toString();
        result.source = scanResult.getSource();
        result.scanTime = java.time.LocalDateTime.now();

        if (scanResult.getTotalFindings() == 0) {
            result.level = ClassificationLevel.PUBLIC;
            result.reason = "No PII detected";
            return result;
        }

        // Determine classification based on highest risk PII found
        boolean hasCritical = scanResult.getCriticalFindings() > 0;
        boolean hasSensitive = false;
        boolean hasFinancial = false;
        boolean hasBiometric = false;

        for (PIIFinding finding : scanResult.getFindings()) {
            PIIType type = finding.getType();
            if (type == PIIType.AADHAAR || type == PIIType.CREDIT_CARD || type == PIIType.BANK_ACCOUNT) hasFinancial = true;
            if (type.isSensitive()) hasSensitive = true;
            if (type == PIIType.FINGERPRINT || type == PIIType.FACIAL) hasBiometric = true;
        }

        if (hasBiometric || (hasCritical && hasFinancial)) {
            result.level = ClassificationLevel.TOP_SECRET;
            result.reason = "Contains biometric data or critical financial PII";
        } else if (hasCritical) {
            result.level = ClassificationLevel.RESTRICTED;
            result.reason = "Contains critical PII (Aadhaar/financial data)";
        } else if (hasSensitive || hasFinancial) {
            result.level = ClassificationLevel.CONFIDENTIAL;
            result.reason = "Contains sensitive or financial PII";
        } else {
            result.level = ClassificationLevel.INTERNAL;
            result.reason = "Contains non-critical PII (email/phone/name)";
        }

        result.piiTypesFound = new ArrayList<>();
        scanResult.getFindingsByType().keySet().forEach(t -> result.piiTypesFound.add(t.name()));
        result.totalFindings = scanResult.getTotalFindings();

        classificationCache.put(result.source, result);
        return result;
    }

    /** Classify text content directly */
    public ClassificationResult classifyText(String text, String source, PIIScanner scanner) {
        PIIScanResult scanResult = scanner.scanText(text, source);
        return classifyFromScanResult(scanResult);
    }

    /** Get all classifications */
    public Map<String, ClassificationResult> getAllClassifications() {
        return Collections.unmodifiableMap(classificationCache);
    }

    /** Get required protections for a classification level */
    public List<String> getRequiredProtections(ClassificationLevel level) {
        return switch (level) {
            case PUBLIC -> List.of("No special protections required");
            case INTERNAL -> List.of("Access control", "Data retention policy");
            case CONFIDENTIAL -> List.of("Encryption at rest", "Encryption in transit", "Access logging", "DLP monitoring");
            case RESTRICTED -> List.of("AES-256 encryption", "MFA required", "Full audit trail", "DLP blocking", "Need-to-know access");
            case TOP_SECRET -> List.of("Hardware security module", "Zero-trust access", "Real-time monitoring", "Tamper-proof audit", "Data masking", "Secure enclave processing");
        };
    }

    /** Get compliance frameworks applicable to a classification */
    public List<String> getApplicableFrameworks(ClassificationLevel level) {
        List<String> frameworks = new ArrayList<>(List.of("DPDP Act 2023"));
        if (level.severity >= 2) frameworks.addAll(List.of("ISO 27001", "NIST CSF"));
        if (level.severity >= 3) frameworks.addAll(List.of("RBI Guidelines", "PCI-DSS", "HIPAA"));
        if (level.severity >= 4) frameworks.addAll(List.of("STQC", "IEEE 2089", "SOC 2 Type II"));
        return frameworks;
    }

    public boolean isInitialized() { return initialized; }

    public static class ClassificationResult {
        public String id, source, reason;
        public ClassificationLevel level;
        public List<String> piiTypesFound = new ArrayList<>();
        public int totalFindings;
        public java.time.LocalDateTime scanTime;
    }
}
