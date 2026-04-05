package com.qsdpdp.dlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Exact Data Matching (EDM) Fingerprinting Engine
 * Creates SHA-256 fingerprints of sensitive data for DLP matching
 * without storing the actual sensitive values.
 *
 * Use cases:
 * - Match PII in transit against known employee/customer databases
 * - Detect exfiltration of specific records without regex
 * - Zero false-positive matching for structured data
 *
 * @version 3.0.0
 * @since Phase 3
 */
@Component
public class EDMFingerprintEngine {

    private static final Logger logger = LoggerFactory.getLogger(EDMFingerprintEngine.class);

    // Fingerprint stores keyed by dataset name
    private final Map<String, FingerprintDataset> datasets = new ConcurrentHashMap<>();
    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        initialized = true;
        logger.info("EDM Fingerprint Engine initialized");
    }

    /**
     * Create fingerprint dataset from structured data (e.g., employee DB export).
     * Each row's fields are individually hashed for partial matching.
     */
    public FingerprintDataset createDataset(String name, String description,
            List<String> columnNames, List<List<String>> rows) {
        FingerprintDataset dataset = new FingerprintDataset(name, description, columnNames);

        for (List<String> row : rows) {
            Map<String, String> rowFingerprints = new LinkedHashMap<>();
            for (int i = 0; i < Math.min(columnNames.size(), row.size()); i++) {
                String value = normalizeValue(row.get(i));
                if (!value.isEmpty()) {
                    rowFingerprints.put(columnNames.get(i), sha256(value));
                }
            }
            // Also create a composite fingerprint for the entire row
            String compositeHash = sha256(String.join("|", row.stream()
                    .map(this::normalizeValue).toList()));
            rowFingerprints.put("__COMPOSITE__", compositeHash);
            dataset.fingerprints.add(rowFingerprints);
        }

        datasets.put(name, dataset);
        logger.info("✅ EDM dataset '{}' created: {} rows, {} columns",
                name, rows.size(), columnNames.size());
        return dataset;
    }

    /**
     * Scan text content against all fingerprint datasets.
     * Returns matches found.
     */
    public List<EDMMatch> scanContent(String content) {
        if (content == null || content.isBlank()) return Collections.emptyList();

        List<EDMMatch> matches = new ArrayList<>();
        String normalized = normalizeValue(content);

        // Extract tokens (words, numbers, email-like, phone-like patterns)
        String[] tokens = normalized.split("[\\s,;|\\t]+");

        for (Map.Entry<String, FingerprintDataset> entry : datasets.entrySet()) {
            FingerprintDataset dataset = entry.getValue();

            for (int rowIdx = 0; rowIdx < dataset.fingerprints.size(); rowIdx++) {
                Map<String, String> rowFP = dataset.fingerprints.get(rowIdx);
                int matchedFields = 0;

                for (Map.Entry<String, String> fieldEntry : rowFP.entrySet()) {
                    if ("__COMPOSITE__".equals(fieldEntry.getKey())) continue;

                    String expectedHash = fieldEntry.getValue();
                    for (String token : tokens) {
                        if (sha256(token).equals(expectedHash)) {
                            matchedFields++;
                            break;
                        }
                    }
                }

                // Require at least 2 field matches to avoid false positives
                int totalFields = rowFP.size() - 1; // exclude composite
                if (matchedFields >= 2 || (totalFields <= 2 && matchedFields >= 1)) {
                    matches.add(new EDMMatch(
                            dataset.name,
                            rowIdx,
                            matchedFields,
                            totalFields,
                            (double) matchedFields / totalFields));
                }
            }
        }

        return matches;
    }

    /**
     * Load fingerprints from a CSV file.
     */
    public FingerprintDataset loadFromCSV(String name, String description, Path csvPath) throws IOException {
        List<String> lines = Files.readAllLines(csvPath);
        if (lines.size() < 2) throw new IOException("CSV must have header + at least 1 row");

        List<String> columns = Arrays.asList(lines.get(0).split(","));
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            rows.add(Arrays.asList(lines.get(i).split(",")));
        }

        return createDataset(name, description, columns, rows);
    }

    // ═══ UTILITIES ═══

    private String normalizeValue(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase()
                .replaceAll("[\\s-]+", "")  // Remove spaces and hyphens
                .replaceAll("[^a-z0-9@._]", ""); // Keep only alphanum + @._
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("datasetCount", datasets.size());
        for (var entry : datasets.entrySet()) {
            stats.put(entry.getKey(), Map.of(
                    "rows", entry.getValue().fingerprints.size(),
                    "columns", entry.getValue().columnNames.size()));
        }
        return stats;
    }

    // ═══ DATA CLASSES ═══

    public static class FingerprintDataset {
        public final String name;
        public final String description;
        public final List<String> columnNames;
        public final List<Map<String, String>> fingerprints = new ArrayList<>();

        public FingerprintDataset(String name, String description, List<String> columnNames) {
            this.name = name;
            this.description = description;
            this.columnNames = columnNames;
        }
    }

    public static class EDMMatch {
        public final String datasetName;
        public final int rowIndex;
        public final int matchedFields;
        public final int totalFields;
        public final double matchScore;

        public EDMMatch(String datasetName, int rowIndex, int matchedFields,
                int totalFields, double matchScore) {
            this.datasetName = datasetName;
            this.rowIndex = rowIndex;
            this.matchedFields = matchedFields;
            this.totalFields = totalFields;
            this.matchScore = matchScore;
        }
    }
}
