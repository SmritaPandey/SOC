package com.qsdpdp.pii;

import java.util.regex.Pattern;

/**
 * PII Pattern definitions for scanning
 * Contains regex patterns for Indian PII detection
 * 
 * @version 1.0.0
 * @since Phase 6
 */
public class PIIPattern {

    private final String id;
    private final PIIType type;
    private final Pattern pattern;
    private final String description;
    private final double confidence;
    private final boolean requiresValidation;

    public PIIPattern(String id, PIIType type, String regex, String description,
            double confidence, boolean requiresValidation) {
        this.id = id;
        this.type = type;
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        this.description = description;
        this.confidence = confidence;
        this.requiresValidation = requiresValidation;
    }

    // ═══════════════════════════════════════════════════════════
    // STATIC PATTERN DEFINITIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Aadhaar number pattern - 12 digits with optional spaces/dashes
     * Format: XXXX XXXX XXXX or XXXX-XXXX-XXXX
     */
    public static final PIIPattern AADHAAR = new PIIPattern(
            "PAT-AADHAAR-001",
            PIIType.AADHAAR,
            "\\b[2-9]\\d{3}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b",
            "12-digit Aadhaar number (cannot start with 0 or 1)",
            0.95,
            true // Requires Verhoeff checksum validation
    );

    /**
     * PAN number pattern - 10 alphanumeric characters
     * Format: AAAAA9999A (5 letters + 4 digits + 1 letter)
     */
    public static final PIIPattern PAN = new PIIPattern(
            "PAT-PAN-001",
            PIIType.PAN,
            "\\b[A-Z]{3}[A-T][A-Z]\\d{4}[A-Z]\\b",
            "10-character PAN (5 letters + 4 digits + 1 letter)",
            0.98,
            true);

    /**
     * Indian passport pattern
     * Format: A1234567 (1 letter + 7 digits)
     */
    public static final PIIPattern PASSPORT = new PIIPattern(
            "PAT-PASS-001",
            PIIType.PASSPORT,
            "\\b[A-Z][0-9]{7}\\b",
            "Indian passport number",
            0.85,
            false);

    /**
     * Email pattern
     */
    public static final PIIPattern EMAIL = new PIIPattern(
            "PAT-EMAIL-001",
            PIIType.EMAIL,
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b",
            "Email address",
            0.99,
            false);

    /**
     * Indian phone number pattern
     * Formats: +91 XXXXX XXXXX, 91-XXXXXXXXXX, XXXXXXXXXX (10 digits starting with
     * 6-9)
     */
    public static final PIIPattern PHONE = new PIIPattern(
            "PAT-PHONE-001",
            PIIType.PHONE,
            "\\b(?:\\+91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}\\b",
            "Indian mobile number",
            0.90,
            false);

    /**
     * Credit/Debit card pattern - 15 or 16 digits
     * Formats: XXXX XXXX XXXX XXXX, XXXX-XXXX-XXXX-XXXX, Amex 15-digit
     */
    public static final PIIPattern CREDIT_CARD = new PIIPattern(
            "PAT-CC-001",
            PIIType.CREDIT_CARD,
            "\\b(?:3[47]\\d{13}|(?:4[0-9]{3}|5[1-5][0-9]{2}|6(?:011|5[0-9]{2}))[\\s-]?[0-9]{4}[\\s-]?[0-9]{4}[\\s-]?[0-9]{4})\\b",
            "Credit/Debit card number (Visa, MC, Amex, Discover)",
            0.95,
            true // Requires Luhn validation
    );

    /**
     * Bank account number pattern
     * 9-18 digits
     */
    public static final PIIPattern BANK_ACCOUNT = new PIIPattern(
            "PAT-BANK-001",
            PIIType.BANK_ACCOUNT,
            "\\b\\d{9,18}\\b",
            "Bank account number",
            0.60, // Lower confidence as many numbers could match
            false);

    /**
     * IFSC Code pattern
     * Format: AAAA0XXXXXX (4 letters + 0 + 6 alphanumeric)
     */
    public static final PIIPattern IFSC = new PIIPattern(
            "PAT-IFSC-001",
            PIIType.IFSC_CODE,
            "\\b[A-Z]{4}0[A-Z0-9]{6}\\b",
            "IFSC code",
            0.98,
            false);

    /**
     * UPI ID pattern
     * Format: username@bankname
     */
    public static final PIIPattern UPI_ID = new PIIPattern(
            "PAT-UPI-001",
            PIIType.UPI_ID,
            "\\b[a-zA-Z0-9._-]+@[a-zA-Z]{2,}\\b",
            "UPI ID",
            0.70,
            false);

    /**
     * GST Number pattern
     * Format: XX AAAAA9999A 9 Z 9 (15 characters)
     */
    public static final PIIPattern GST = new PIIPattern(
            "PAT-GST-001",
            PIIType.GST_NUMBER,
            "\\b\\d{2}[A-Z]{5}\\d{4}[A-Z]\\d[A-Z0-9]Z[A-Z0-9]\\b",
            "GST Number",
            0.98,
            true);

    /**
     * Voter ID pattern
     * Format: AAA9999999 (3 letters + 7 digits)
     */
    public static final PIIPattern VOTER_ID = new PIIPattern(
            "PAT-VOTER-001",
            PIIType.VOTER_ID,
            "\\b[A-Z]{3}\\d{7}\\b",
            "Voter ID (EPIC)",
            0.85,
            false);

    /**
     * Driving License pattern (varies by state)
     * General format: XX-XX-XXXX-XXXXXXX
     */
    public static final PIIPattern DRIVING_LICENSE = new PIIPattern(
            "PAT-DL-001",
            PIIType.DRIVING_LICENSE,
            "\\b[A-Z]{2}[\\s-]?\\d{2}[\\s-]?(?:19|20)\\d{2}[\\s-]?\\d{7}\\b",
            "Driving License number",
            0.80,
            false);

    /**
     * Date of Birth pattern
     * Formats: DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD
     */
    public static final PIIPattern DOB = new PIIPattern(
            "PAT-DOB-001",
            PIIType.DATE_OF_BIRTH,
            "\\b(?:0[1-9]|[12][0-9]|3[01])[/-](?:0[1-9]|1[012])[/-](?:19|20)\\d{2}\\b|\\b(?:19|20)\\d{2}[/-](?:0[1-9]|1[012])[/-](?:0[1-9]|[12][0-9]|3[01])\\b",
            "Date of Birth",
            0.70,
            false);

    /**
     * Health ID (Ayushman Bharat) pattern
     * Format: XX-XXXX-XXXX-XXXX (14 digits)
     */
    public static final PIIPattern HEALTH_ID = new PIIPattern(
            "PAT-HEALTH-001",
            PIIType.HEALTH_ID,
            "\\b\\d{2}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b",
            "ABHA (Health ID) number",
            0.75,
            true);

    // ═══════════════════════════════════════════════════════════
    // ALL PATTERNS ARRAY
    // ═══════════════════════════════════════════════════════════

    public static final PIIPattern[] ALL_PATTERNS = {
            AADHAAR, PAN, PASSPORT, EMAIL, PHONE, CREDIT_CARD,
            BANK_ACCOUNT, IFSC, UPI_ID, GST, VOTER_ID,
            DRIVING_LICENSE, DOB, HEALTH_ID
    };

    // ═══════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public PIIType getType() {
        return type;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getDescription() {
        return description;
    }

    public double getConfidence() {
        return confidence;
    }

    public boolean isRequiresValidation() {
        return requiresValidation;
    }
}
