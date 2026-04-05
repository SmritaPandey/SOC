package com.qsdpdp.pii;

/**
 * PII Types as per DPDP Act 2023 and Indian regulatory requirements
 * 
 * @version 1.0.0
 * @since Phase 6
 */
public enum PIIType {
    // Identity Documents
    AADHAAR("Aadhaar Number", "CRITICAL", "12-digit unique identification number", true),
    PAN("PAN Number", "HIGH", "Permanent Account Number", true),
    PASSPORT("Passport Number", "HIGH", "Indian passport number", true),
    VOTER_ID("Voter ID", "MEDIUM", "Electoral Photo Identity Card", true),
    DRIVING_LICENSE("Driving License", "MEDIUM", "DL number", true),

    // Contact Information
    EMAIL("Email Address", "MEDIUM", "Email address", false),
    PHONE("Phone Number", "MEDIUM", "Mobile/landline number", false),
    ADDRESS("Physical Address", "LOW", "Residential/office address", false),

    // Financial Information
    BANK_ACCOUNT("Bank Account", "CRITICAL", "Bank account number", true),
    CREDIT_CARD("Credit Card", "CRITICAL", "Credit/debit card number", true),
    IFSC_CODE("IFSC Code", "LOW", "Indian Financial System Code", false),
    UPI_ID("UPI ID", "MEDIUM", "Unified Payments Interface ID", false),

    // Health Information (Sensitive per DPDP)
    HEALTH_ID("Health ID", "CRITICAL", "Ayushman Bharat Health Account", true),
    MEDICAL_RECORD("Medical Record Number", "CRITICAL", "Hospital/clinic MRN", true),

    // Biometric (Sensitive per DPDP)
    FINGERPRINT("Fingerprint", "CRITICAL", "Fingerprint biometric data", true),
    IRIS("Iris Scan", "CRITICAL", "Iris biometric data", true),
    FACIAL("Facial Data", "CRITICAL", "Facial recognition data", true),

    // Other
    NAME("Person Name", "LOW", "Full name of individual", false),
    DATE_OF_BIRTH("Date of Birth", "MEDIUM", "Birth date", false),
    GENDER("Gender", "LOW", "Gender identity", false),
    GST_NUMBER("GST Number", "LOW", "Goods and Services Tax ID", false),

    // Generic
    UNKNOWN("Unknown PII", "MEDIUM", "Unclassified PII", false);

    private final String displayName;
    private final String riskLevel;
    private final String description;
    private final boolean sensitive;

    PIIType(String displayName, String riskLevel, String description, boolean sensitive) {
        this.displayName = displayName;
        this.riskLevel = riskLevel;
        this.description = description;
        this.sensitive = sensitive;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    /**
     * Get DPDP section reference for this PII type
     */
    public String getDPDPSection() {
        return switch (this) {
            case AADHAAR, PAN, PASSPORT, VOTER_ID, DRIVING_LICENSE -> "Section 2(t) - Personal Data";
            case HEALTH_ID, MEDICAL_RECORD -> "Section 2(t) - Sensitive Personal Data";
            case FINGERPRINT, IRIS, FACIAL -> "Section 2(t) - Sensitive Personal Data (Biometric)";
            case BANK_ACCOUNT, CREDIT_CARD -> "Section 2(t) - Financial Personal Data";
            default -> "Section 2(t) - Personal Data";
        };
    }

    /**
     * Get retention period in days as per DPDP guidelines
     */
    public int getRetentionDays() {
        return switch (this.riskLevel) {
            case "CRITICAL" -> 365 * 7; // 7 years
            case "HIGH" -> 365 * 5; // 5 years
            case "MEDIUM" -> 365 * 3; // 3 years
            default -> 365; // 1 year
        };
    }
}
