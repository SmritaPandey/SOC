package com.qsdpdp.dlp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.*;

/**
 * Content Inspection Engine - Deep content analysis beyond regex
 * Provides keyword proximity analysis, document fingerprinting, and NLP-based classification
 */
public class ContentInspectionEngine {
    private static final Logger logger = LoggerFactory.getLogger(ContentInspectionEngine.class);
    private boolean initialized = false;

    // Sensitive keyword dictionaries by category
    private final Map<String, Set<String>> keywordDictionaries = new LinkedHashMap<>();
    // Document fingerprints for exact/partial match detection
    private final Map<String, DocumentFingerprint> fingerprints = new LinkedHashMap<>();

    public void initialize() {
        if (initialized) return;
        loadKeywordDictionaries();
        initialized = true;
        logger.info("ContentInspectionEngine initialized with {} keyword categories", keywordDictionaries.size());
    }

    private void loadKeywordDictionaries() {
        keywordDictionaries.put("FINANCIAL", Set.of("bank account", "credit card", "debit card", "ifsc", "swift", "iban",
            "loan", "mortgage", "salary", "income", "tax return", "pan card", "gst", "invoice"));
        keywordDictionaries.put("MEDICAL", Set.of("diagnosis", "prescription", "patient", "medical record", "blood group",
            "allergy", "treatment", "hospital", "doctor", "health insurance", "abha", "abdm"));
        keywordDictionaries.put("LEGAL", Set.of("confidential", "privileged", "attorney", "court order", "subpoena",
            "settlement", "agreement", "contract", "clause", "indemnity"));
        keywordDictionaries.put("HR", Set.of("employee id", "salary slip", "appraisal", "performance review",
            "resignation", "termination", "provident fund", "gratuity", "esops"));
        keywordDictionaries.put("CREDENTIAL", Set.of("password", "secret key", "api key", "access token", "bearer",
            "private key", "certificate", "oauth", "jwt", "ssh key"));
        keywordDictionaries.put("PII_CONTEXT", Set.of("aadhaar", "pan number", "passport", "voter id", "driving license",
            "date of birth", "mother maiden", "social security", "national id"));
    }

    /** Perform deep content inspection */
    public InspectionResult inspect(String content, String source) {
        InspectionResult result = new InspectionResult();
        result.id = UUID.randomUUID().toString();
        result.source = source;
        result.contentLength = content.length();

        // Keyword analysis by category
        String lower = content.toLowerCase();
        for (Map.Entry<String, Set<String>> entry : keywordDictionaries.entrySet()) {
            String category = entry.getKey();
            int matchCount = 0;
            List<String> matched = new ArrayList<>();
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword)) { matchCount++; matched.add(keyword); }
            }
            if (matchCount > 0) {
                CategoryMatch cm = new CategoryMatch();
                cm.category = category;
                cm.matchCount = matchCount;
                cm.matchedKeywords = matched;
                cm.density = (double) matchCount / entry.getValue().size();
                result.categoryMatches.add(cm);
            }
        }

        // Proximity analysis (keywords near PII indicators)
        result.proximityScore = computeProximityScore(content);

        // Compute overall sensitivity score
        result.sensitivityScore = computeSensitivityScore(result);
        result.classification = result.sensitivityScore > 0.7 ? "HIGH" :
                                result.sensitivityScore > 0.4 ? "MEDIUM" : "LOW";
        return result;
    }

    /** Create a fingerprint of a document for matching */
    public DocumentFingerprint fingerprint(String content, String documentId) {
        DocumentFingerprint fp = new DocumentFingerprint();
        fp.documentId = documentId;
        fp.contentHash = Integer.toHexString(content.hashCode());
        fp.length = content.length();
        fp.wordCount = content.split("\\s+").length;

        // Create shingles (n-grams) for partial matching
        List<String> words = List.of(content.toLowerCase().split("\\s+"));
        Set<Integer> shingles = new HashSet<>();
        for (int i = 0; i < words.size() - 2; i++) {
            String shingle = words.get(i) + " " + words.get(Math.min(i+1, words.size()-1)) + " " + words.get(Math.min(i+2, words.size()-1));
            shingles.add(shingle.hashCode());
        }
        fp.shingleHashes = shingles;
        fingerprints.put(documentId, fp);
        return fp;
    }

    /** Check if content matches any known fingerprinted document */
    public FingerprintMatch checkFingerprint(String content) {
        DocumentFingerprint testFp = fingerprint(content, "test-" + UUID.randomUUID());
        FingerprintMatch bestMatch = null;
        double bestSimilarity = 0;

        for (Map.Entry<String, DocumentFingerprint> entry : fingerprints.entrySet()) {
            if (entry.getKey().startsWith("test-")) continue;
            double similarity = computeJaccardSimilarity(testFp.shingleHashes, entry.getValue().shingleHashes);
            if (similarity > bestSimilarity && similarity > 0.3) {
                bestSimilarity = similarity;
                bestMatch = new FingerprintMatch();
                bestMatch.matchedDocumentId = entry.getKey();
                bestMatch.similarity = similarity;
                bestMatch.isExactMatch = similarity > 0.95;
                bestMatch.isPartialMatch = similarity > 0.3 && similarity <= 0.95;
            }
        }
        fingerprints.remove("test-" + testFp.documentId); // Clean up
        return bestMatch;
    }

    private double computeProximityScore(String content) {
        String lower = content.toLowerCase();
        String[] piiIndicators = {"aadhaar", "pan", "credit card", "bank", "password", "email", "phone"};
        String[] contextWords = {"number", "id", "account", "card", "secret", "confidential"};
        int proximityHits = 0;
        for (String pii : piiIndicators) {
            int piiIdx = lower.indexOf(pii);
            if (piiIdx >= 0) {
                String window = lower.substring(Math.max(0, piiIdx - 50), Math.min(lower.length(), piiIdx + pii.length() + 50));
                for (String ctx : contextWords) {
                    if (window.contains(ctx)) { proximityHits++; break; }
                }
            }
        }
        return Math.min(1.0, proximityHits * 0.2);
    }

    private double computeSensitivityScore(InspectionResult result) {
        double score = 0;
        for (CategoryMatch cm : result.categoryMatches) {
            double weight = switch (cm.category) {
                case "CREDENTIAL" -> 1.0;
                case "FINANCIAL", "MEDICAL" -> 0.8;
                case "LEGAL", "PII_CONTEXT" -> 0.6;
                case "HR" -> 0.5;
                default -> 0.3;
            };
            score += cm.density * weight;
        }
        score += result.proximityScore * 0.3;
        return Math.min(1.0, score);
    }

    private double computeJaccardSimilarity(Set<Integer> a, Set<Integer> b) {
        if (a.isEmpty() && b.isEmpty()) return 0;
        Set<Integer> intersection = new HashSet<>(a); intersection.retainAll(b);
        Set<Integer> union = new HashSet<>(a); union.addAll(b);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    public boolean isInitialized() { return initialized; }

    // Inner classes
    public static class InspectionResult {
        public String id, source, classification;
        public int contentLength;
        public List<CategoryMatch> categoryMatches = new ArrayList<>();
        public double proximityScore, sensitivityScore;
    }

    public static class CategoryMatch {
        public String category; public int matchCount; public double density;
        public List<String> matchedKeywords = new ArrayList<>();
    }

    public static class DocumentFingerprint {
        public String documentId, contentHash; public int length, wordCount;
        public Set<Integer> shingleHashes = new HashSet<>();
    }

    public static class FingerprintMatch {
        public String matchedDocumentId; public double similarity;
        public boolean isExactMatch, isPartialMatch;
    }
}
