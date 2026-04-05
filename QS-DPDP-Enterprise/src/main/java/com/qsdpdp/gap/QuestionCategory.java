package com.qsdpdp.gap;

/**
 * GAP Assessment Question Category aligned with DPDP Act 2023 sections
 * 
 * @version 1.0.0
 * @since Module 3
 */
public enum QuestionCategory {

    // DPDP Act 2023 Chapter/Section mappings
    LAWFUL_PROCESSING("Lawful Processing", "Chapter II - Section 4-5",
            "Questions related to lawful grounds for processing personal data"),

    CONSENT_MANAGEMENT("Consent Management", "Chapter II - Section 6",
            "Questions about consent collection, withdrawal, and management"),

    NOTICE_AND_TRANSPARENCY("Notice & Transparency", "Chapter II - Section 5",
            "Questions about privacy notices and transparency obligations"),

    PURPOSE_LIMITATION("Purpose Limitation", "Chapter II - Section 5(a)",
            "Questions about limiting processing to declared purposes"),

    DATA_MINIMIZATION("Data Minimization", "Chapter II - Section 5(c)",
            "Questions about collecting only necessary data"),

    ACCURACY("Data Accuracy", "Chapter II - Section 8(4)",
            "Questions about maintaining accurate personal data"),

    STORAGE_LIMITATION("Storage Limitation", "Chapter II - Section 8(7)",
            "Questions about data retention and deletion"),

    SECURITY_SAFEGUARDS("Security Safeguards", "Chapter II - Section 8(5)",
            "Questions about technical and organizational security measures"),

    BREACH_NOTIFICATION("Breach Notification", "Chapter II - Section 8(6)",
            "Questions about breach detection and notification procedures"),

    RIGHTS_ACCESS("Right to Access", "Chapter III - Section 11",
            "Questions about data principal's right to access their data"),

    RIGHTS_CORRECTION("Right to Correction", "Chapter III - Section 12",
            "Questions about data principal's right to correction"),

    RIGHTS_ERASURE("Right to Erasure", "Chapter III - Section 12",
            "Questions about data principal's right to erasure"),

    RIGHTS_GRIEVANCE("Grievance Redressal", "Chapter III - Section 13",
            "Questions about grievance handling mechanisms"),

    RIGHTS_NOMINATION("Right to Nomination", "Chapter III - Section 14",
            "Questions about data principal's right to nominate"),

    CHILDREN_DATA("Children's Data", "Chapter II - Section 9",
            "Questions about processing children's personal data"),

    SIGNIFICANT_DPO("DPO & Significant Fiduciary", "Chapter II - Section 10",
            "Questions about DPO appointment and significant data fiduciary obligations"),

    CROSS_BORDER("Cross-Border Transfer", "Chapter IV - Section 16",
            "Questions about international data transfers"),

    DPIA("Data Protection Impact Assessment", "Chapter II - Section 10(2)(a)",
            "Questions about DPIA processes and documentation"),

    THIRD_PARTY("Third Party & Processors", "Chapter II - Section 8(2)",
            "Questions about data processor relationships"),

    GOVERNANCE("Governance & Accountability", "Chapter II - Section 8",
            "Questions about organizational governance and accountability"),

    SECTOR_BANKING("Banking Sector", "Sectoral - RBI Guidelines",
            "Questions specific to banking and financial services"),

    SECTOR_TELECOM("Telecom Sector", "Sectoral - TRAI Guidelines",
            "Questions specific to telecommunications"),

    SECTOR_HEALTH("Healthcare Sector", "Sectoral - Health Data Management",
            "Questions specific to healthcare and medical data"),

    SECTOR_GOVERNMENT("Government Sector", "Sectoral - eGov Guidelines",
            "Questions specific to government organizations"),

    SECTOR_IT("IT Services Sector", "Sectoral - IT/ITeS Guidelines",
            "Questions specific to IT and ITES organizations"),

    SECTOR_EDUCATION("Education Sector", "Sectoral - Education Guidelines",
            "Questions specific to educational institutions"),

    TECHNICAL_CONTROLS("Technical Controls", "ISO 27001 - Annex A",
            "Questions about technical security controls"),

    ORGANIZATIONAL_MEASURES("Organizational Measures", "ISO 27001 - Clauses",
            "Questions about organizational security measures");

    private final String displayName;
    private final String dpdpReference;
    private final String description;

    QuestionCategory(String displayName, String dpdpReference, String description) {
        this.displayName = displayName;
        this.dpdpReference = dpdpReference;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDpdpReference() {
        return dpdpReference;
    }

    public String getDescription() {
        return description;
    }

    public double getWeight() {
        return switch (this) {
            case CONSENT_MANAGEMENT, BREACH_NOTIFICATION, CHILDREN_DATA, CROSS_BORDER -> 1.5; // Critical
            case SECURITY_SAFEGUARDS, RIGHTS_ACCESS, RIGHTS_ERASURE, DPIA -> 1.3; // High priority
            case LAWFUL_PROCESSING, NOTICE_AND_TRANSPARENCY, PURPOSE_LIMITATION -> 1.2; // Important
            default -> 1.0; // Standard weight
        };
    }
}
