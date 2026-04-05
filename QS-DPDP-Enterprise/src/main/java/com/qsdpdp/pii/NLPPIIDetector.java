package com.qsdpdp.pii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NLP-Enhanced Named Entity Recognition (NER) PII Detector
 * Uses pattern-based NER with contextual rules to detect PII entities
 * that regex alone would miss (names, addresses, organizations).
 *
 * Enhancement over basic regex PII scanning:
 * - Indian name detection with title/suffix patterns
 * - Address detection with Indian pin code + state patterns
 * - Organization name extraction
 * - Contextual clue analysis (keywords like "name:", "address:", etc.)
 *
 * @version 3.0.0
 * @since Phase 3
 */
@Component
public class NLPPIIDetector {

    private static final Logger logger = LoggerFactory.getLogger(NLPPIIDetector.class);

    // Indian name patterns with titles
    private static final Pattern INDIAN_NAME = Pattern.compile(
            "(?:(?:Mr|Mrs|Ms|Dr|Shri|Smt|Sri|Prof|Sh)\\.?\\s+)?" +
            "([A-Z][a-z]{1,20}(?:\\s+[A-Z][a-z]{1,20}){1,3})"
    );

    // Contextual name indicators
    private static final Pattern NAME_CONTEXT = Pattern.compile(
            "(?:name|naam|नाम|patient|employee|applicant|customer|client|member)\\s*[:=]\\s*([A-Za-z\\s]{3,50})",
            Pattern.CASE_INSENSITIVE
    );

    // Indian address pattern
    private static final Pattern INDIAN_ADDRESS = Pattern.compile(
            "(?:(?:H\\.?\\s*No|House|Flat|Plot|Floor|Building|Block|Ward|Village|Mohalla|Gali)\\s*[.:,-]?\\s*" +
            "[\\w\\s,.-]{5,80}\\s*(?:PIN|Pin|Pincode)?\\s*[-:]?\\s*\\d{6})",
            Pattern.CASE_INSENSITIVE
    );

    // Indian PIN code
    private static final Pattern PIN_CODE = Pattern.compile("\\b[1-9]\\d{5}\\b");

    // Date of birth patterns
    private static final Pattern DOB = Pattern.compile(
            "(?:DOB|D\\.O\\.B|Date\\s+of\\s+Birth|जन्म\\s*तिथि)\\s*[:=]?\\s*(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4})",
            Pattern.CASE_INSENSITIVE
    );

    // Vehicle registration (Indian)
    private static final Pattern VEHICLE_REG = Pattern.compile(
            "\\b[A-Z]{2}\\s*\\d{1,2}\\s*[A-Z]{1,3}\\s*\\d{4}\\b"
    );

    // Bank account number (Indian)
    private static final Pattern BANK_ACCOUNT = Pattern.compile(
            "(?:A/C|Account|Acct)\\s*(?:No|Number|#)?\\s*[:=]?\\s*(\\d{9,18})",
            Pattern.CASE_INSENSITIVE
    );

    // IFSC Code
    private static final Pattern IFSC = Pattern.compile("\\b[A-Z]{4}0[A-Z0-9]{6}\\b");

    // UPI ID
    private static final Pattern UPI_ID = Pattern.compile(
            "\\b[a-zA-Z0-9._-]+@(?:upi|paytm|oksbi|okicici|okaxis|ybl|ibl|apl|okhdfcbank)\\b"
    );

    // Religion, Caste (sensitive under DPDP)
    private static final Pattern SENSITIVE_CATEGORY = Pattern.compile(
            "(?:religion|caste|tribe|जाति|धर्म|community)\\s*[:=]\\s*([\\w\\s]{2,30})",
            Pattern.CASE_INSENSITIVE
    );

    // Disability status
    private static final Pattern DISABILITY = Pattern.compile(
            "(?:disability|handicap|differently.?abled|विकलांग)\\s*[:=]?\\s*([\\w\\s]{2,30})",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Scan text using NLP-enhanced NER patterns.
     * Returns findings that supplement basic regex PII scanning.
     */
    public List<NERFinding> detectEntities(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        List<NERFinding> findings = new ArrayList<>();

        // Name detection with context
        findMatches(text, NAME_CONTEXT, "PERSON_NAME", "HIGH", "Contextual name detection", findings);
        findMatches(text, INDIAN_NAME, "PERSON_NAME", "MEDIUM", "Indian name pattern", findings);

        // Address detection
        findMatches(text, INDIAN_ADDRESS, "ADDRESS", "HIGH", "Indian address with PIN", findings);

        // Date of Birth
        findMatches(text, DOB, "DATE_OF_BIRTH", "HIGH", "Date of birth", findings);

        // Financial identifiers
        findMatches(text, BANK_ACCOUNT, "BANK_ACCOUNT", "CRITICAL", "Bank account number", findings);
        findMatches(text, IFSC, "IFSC_CODE", "HIGH", "IFSC bank code", findings);
        findMatches(text, UPI_ID, "UPI_ID", "HIGH", "UPI payment ID", findings);

        // Indian-specific identifiers
        findMatches(text, VEHICLE_REG, "VEHICLE_REGISTRATION", "MEDIUM", "Vehicle registration", findings);
        findMatches(text, PIN_CODE, "PIN_CODE", "MEDIUM", "Indian PIN code", findings);

        // Sensitive personal data (Section 3(f), DPDP Act)
        findMatches(text, SENSITIVE_CATEGORY, "SENSITIVE_RELIGION_CASTE", "CRITICAL", "Religion/caste data", findings);
        findMatches(text, DISABILITY, "SENSITIVE_DISABILITY", "CRITICAL", "Disability status", findings);

        // Deduplicate overlapping findings
        findings = deduplicateFindings(findings);

        logger.debug("NER scan found {} entities in {} chars", findings.size(), text.length());
        return findings;
    }

    private void findMatches(String text, Pattern pattern, String entityType,
                            String riskLevel, String description, List<NERFinding> findings) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(matcher.groupCount() > 0 ? 1 : 0).trim();
            if (value.length() >= 2) {
                findings.add(new NERFinding(
                        entityType, value, riskLevel, description,
                        matcher.start(), matcher.end()));
            }
        }
    }

    private List<NERFinding> deduplicateFindings(List<NERFinding> findings) {
        List<NERFinding> deduplicated = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (NERFinding f : findings) {
            String key = f.entityType + ":" + f.value;
            if (seen.add(key)) {
                deduplicated.add(f);
            }
        }
        return deduplicated;
    }

    // ═══ RESULT CLASS ═══

    public static class NERFinding {
        public final String entityType;
        public final String value;
        public final String riskLevel;
        public final String description;
        public final int startPos;
        public final int endPos;

        public NERFinding(String entityType, String value, String riskLevel,
                String description, int startPos, int endPos) {
            this.entityType = entityType;
            this.value = value;
            this.riskLevel = riskLevel;
            this.description = description;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }
}
