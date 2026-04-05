package com.qsdpdp.chatbot;

import java.util.*;

/**
 * DPDP Knowledge Base - RAG (Retrieval Augmented Generation) data source
 * Contains DPDP Act 2023 sections, explanations, and practical guidance
 * 
 * @version 1.0.0
 * @since Module 4
 */
public class DPDPKnowledgeBase {

    private static final Map<String, KnowledgeArticle> ARTICLES = new HashMap<>();

    static {
        initializeKnowledge();
    }

    private static void initializeKnowledge() {
        // ═══════════════════════════════════════════════════════════
        // DPDP ACT 2023 - KEY DEFINITIONS (Section 2)
        // ═══════════════════════════════════════════════════════════

        addArticle("personal_data", "Personal Data Definition",
                "Section 2(t)",
                """
                        Personal data means any data about an individual who is identifiable by or in relation
                        to such data. This includes:
                        - Name, address, date of birth
                        - Identification numbers (Aadhaar, PAN, Passport)
                        - Financial information
                        - Biometric data
                        - Health records
                        - Location data
                        - Any data that can identify a person directly or indirectly
                        """,
                Arrays.asList("What is personal data", "PII definition", "personal information"));

        addArticle("data_principal", "Data Principal",
                "Section 2(j)",
                """
                        Data Principal means the individual to whom the personal data relates. In case of:
                        - A child: The parent or lawful guardian
                        - A person with disability: The lawful guardian acting on their behalf

                        Data Principals have specific rights under the DPDP Act including:
                        - Right to access their data
                        - Right to correct inaccurate data
                        - Right to erasure
                        - Right to grievance redressal
                        - Right to nominate
                        """,
                Arrays.asList("data subject", "individual rights", "data owner"));

        addArticle("data_fiduciary", "Data Fiduciary",
                "Section 2(i)",
                """
                        Data Fiduciary means any person who alone or in conjunction with other persons
                        determines the purpose and means of processing of personal data.

                        Obligations of Data Fiduciary:
                        - Obtain free, specific, informed consent
                        - Process data only for lawful purposes
                        - Implement security safeguards
                        - Notify breaches to DPBI and affected individuals
                        - Appoint Data Protection Officer (for significant fiduciaries)
                        - Conduct DPIA (for significant fiduciaries)
                        """,
                Arrays.asList("data controller", "fiduciary duties", "data processor"));

        addArticle("consent", "Consent Requirements",
                "Section 6",
                """
                        Consent under DPDP Act must be:
                        - FREE: Given voluntarily without coercion
                        - SPECIFIC: For particular, clearly defined purposes
                        - INFORMED: Data Principal knows what they're consenting to
                        - UNCONDITIONAL: Not bundled with unrelated services
                        - UNAMBIGUOUS: Clear positive action (no pre-ticked boxes)

                        Consent must be:
                        - Presented in clear, plain language
                        - Available in multiple Indian languages
                        - Separate from other terms
                        - Easily withdrawable (as easy as giving)

                        Valid consent records must include:
                        - Timestamp of consent
                        - Purpose of processing
                        - Identity verification
                        - Method of consent collection
                        """,
                Arrays.asList("how to collect consent", "consent form", "consent requirements"));

        addArticle("legitimate_uses", "Legitimate Uses Without Consent",
                "Section 7",
                """
                        Personal data may be processed without consent for:

                        (a) Specified purpose for which data was voluntarily provided
                        (b) State functions and services
                        (c) Medical emergency involving threat to life/health
                        (d) Employment purposes
                        (e) Public interest in catastrophic situations
                        (f) Legal obligations and court orders

                        Even for legitimate uses:
                        - Notice must still be provided
                        - Processing must be limited to stated purpose
                        - Data security remains mandatory
                        """,
                Arrays.asList("processing without consent", "legitimate basis", "lawful grounds"));

        // ═══════════════════════════════════════════════════════════
        // BREACH NOTIFICATION (Section 8(6))
        // ═══════════════════════════════════════════════════════════

        addArticle("breach_notification", "Breach Notification Requirements",
                "Section 8(6)",
                """
                        Data Fiduciary must notify personal data breach to:

                        1. DATA PROTECTION BOARD OF INDIA (DPBI)
                           - Within 72 hours of becoming aware
                           - Include: Nature of breach, categories of data, number affected
                           - Include: DPO contact, likely consequences, mitigation measures

                        2. EACH AFFECTED DATA PRINCIPAL
                           - Without undue delay
                           - In clear, plain language
                           - Include: Nature of breach and data affected
                           - Include: Mitigation measures taken
                           - Include: Steps they can take

                        3. CERT-In (for cyber incidents)
                           - Within 6 hours as per IT Act
                           - Mandatory for targeted attacks, data theft, ransomware

                        Failure to notify: Up to ₹200 crores penalty
                        """,
                Arrays.asList("breach response", "data breach", "notification timeline", "DPBI"));

        // ═══════════════════════════════════════════════════════════
        // DATA PRINCIPAL RIGHTS (Chapter III)
        // ═══════════════════════════════════════════════════════════

        addArticle("right_to_access", "Right to Access",
                "Section 11",
                """
                        Every Data Principal has the right to:

                        (a) SUMMARY OF PROCESSING
                            - What personal data is being processed
                            - Processing activities undertaken

                        (b) IDENTITIES
                            - Identity of all Data Fiduciaries with whom data shared
                            - Description of shared data

                        (c) OTHER INFORMATION
                            - Any other information as prescribed by rules

                        Response Timeline:
                        - As prescribed by rules (expected: 30 days)
                        - Free of charge for reasonable requests
                        - May charge for manifestly unfounded/excessive requests
                        """,
                Arrays.asList("access request", "subject access", "data access"));

        addArticle("right_to_correction", "Right to Correction",
                "Section 12",
                """
                        Data Principal has the right to:

                        (a) CORRECTION
                            - Correct inaccurate or misleading personal data
                            - Update incomplete data

                        (b) COMPLETION
                            - Complete information that is incomplete

                        (c) ERASURE
                            - Erase personal data no longer necessary
                            - When consent is withdrawn
                            - When purpose is fulfilled

                        Exceptions:
                        - Legal obligations requiring retention
                        - Archiving in public interest
                        - Scientific/historical research
                        - Statistical purposes
                        """,
                Arrays.asList("data correction", "right to erasure", "delete my data"));

        addArticle("grievance_redressal", "Grievance Redressal",
                "Section 13",
                """
                        Data Fiduciary must:

                        1. PUBLISH DETAILS
                           - Contact information for grievance submission
                           - Clear process for lodging complaints

                        2. ACKNOWLEDGE RECEIPT
                           - Timely acknowledgment of grievance

                        3. RESPOND
                           - Within timeframe prescribed by rules
                           - With resolution or explanation

                        If unsatisfied, Data Principal may:
                        - Escalate to Data Protection Board of India
                        - File complaint under Section 14

                        DPO must ensure:
                        - Grievance mechanism is accessible
                        - Responses are timely and adequate
                        """,
                Arrays.asList("complaint", "grievance", "DPO contact"));

        // ═══════════════════════════════════════════════════════════
        // CHILDREN'S DATA (Section 9)
        // ═══════════════════════════════════════════════════════════

        addArticle("children_data", "Processing Children's Personal Data",
                "Section 9",
                """
                        Special requirements for children (under 18 years):

                        CONSENT REQUIREMENTS:
                        - Must be obtained from parent or lawful guardian
                        - Verifiable parental consent is mandatory
                        - Identity of parent/guardian must be verified

                        PROHIBITIONS:
                        - No tracking or behavioral monitoring
                        - No targeted advertising
                        - No processing likely to cause harm

                        AGE VERIFICATION:
                        - Must implement reliable age-gating
                        - Cannot rely on self-declaration alone

                        Penalties:
                        - Up to ₹200 crores for violations
                        - Higher scrutiny from regulators

                        Exception classes may be notified by Central Government
                        """,
                Arrays.asList("minor data", "parental consent", "child protection"));

        // ═══════════════════════════════════════════════════════════
        // SIGNIFICANT DATA FIDUCIARY (Section 10)
        // ═══════════════════════════════════════════════════════════

        addArticle("significant_fiduciary", "Significant Data Fiduciary Obligations",
                "Section 10",
                """
                        Central Government may notify certain fiduciaries as
                        Significant Data Fiduciary based on:
                        - Volume and sensitivity of data
                        - Risk to rights of Data Principals
                        - Impact on sovereignty and integrity
                        - Risk to electoral democracy
                        - Other factors

                        ADDITIONAL OBLIGATIONS:

                        (a) DATA PROTECTION OFFICER
                            - Appoint DPO based in India
                            - DPO represents fiduciary to DPBI
                            - Must be senior management

                        (b) INDEPENDENT DATA AUDITOR
                            - Periodic audits by appointed auditor
                            - Audit reports to DPBI

                        (c) DATA PROTECTION IMPACT ASSESSMENT
                            - Before high-risk processing
                            - Document risks and mitigations
                        """,
                Arrays.asList("SDF", "DPO appointment", "DPIA requirement"));

        // ═══════════════════════════════════════════════════════════
        // CROSS-BORDER TRANSFER (Section 16)
        // ═══════════════════════════════════════════════════════════

        addArticle("cross_border", "Cross-Border Data Transfer",
                "Section 16",
                """
                        Key requirements for international transfers:

                        GENERAL RULE:
                        - Transfer allowed except to notified restricted countries
                        - Central Government maintains restricted country list

                        RESTRICTIONS:
                        - Government may restrict transfer to specified countries
                        - May require specific safeguards

                        SAFEGUARDS (for transfers):
                        - Contractual clauses (similar to SCCs)
                        - Adequacy assessment where applicable
                        - Technical measures to ensure protection

                        AUDIT REQUIREMENTS:
                        - Document all cross-border transfers
                        - Maintain records of recipient countries
                        - Record legal basis for transfer

                        Sensitive sectors may have additional restrictions.
                        """,
                Arrays.asList("international transfer", "data localization", "overseas transfer"));

        // ═══════════════════════════════════════════════════════════
        // PENALTIES (Section 33)
        // ═══════════════════════════════════════════════════════════

        addArticle("penalties", "Penalties Under DPDP Act",
                "Section 33",
                """
                        PENALTY SCHEDULE:

                        ┌─────────────────────────────────────────┬──────────────────┐
                        │ Violation                               │ Maximum Penalty  │
                        ├─────────────────────────────────────────┼──────────────────┤
                        │ Non-compliance with provisions          │ ₹50 crores       │
                        │ Failure to notify breach                │ ₹200 crores      │
                        │ Children's data violations              │ ₹200 crores      │
                        │ Non-compliance with Board's order       │ ₹150 crores      │
                        │ Breach of additional SDF obligations    │ ₹150 crores      │
                        │ Any other violation                     │ ₹10,000          │
                        └─────────────────────────────────────────┴──────────────────┘

                        MAXIMUM AGGREGATE: ₹250 crores

                        FACTORS CONSIDERED:
                        - Nature, gravity, duration of violation
                        - Type and purpose of processing
                        - Actions taken to mitigate damage
                        - Previous violations
                        - Financial benefits from violation
                        """,
                Arrays.asList("fine", "penalty amount", "violation cost"));

        // ═══════════════════════════════════════════════════════════
        // PRACTICAL GUIDANCE
        // ═══════════════════════════════════════════════════════════

        addArticle("consent_form_template", "How to Create Consent Form",
                "Practical Guidance",
                """
                        CONSENT FORM CHECKLIST:

                        ☐ Organization identity and contact
                        ☐ DPO contact information
                        ☐ Categories of personal data collected
                        ☐ Specific purposes (itemized)
                        ☐ Retention period for each purpose
                        ☐ Third parties with whom data shared
                        ☐ Cross-border transfer disclosure
                        ☐ Rights of Data Principal
                        ☐ How to withdraw consent
                        ☐ Grievance mechanism

                        LANGUAGE REQUIREMENTS:
                        - Plain, clear language
                        - Avoid legal jargon
                        - Multi-language support
                        - Separate from T&C

                        COLLECTION METHOD:
                        - Active checkbox (no pre-ticked)
                        - Double opt-in for sensitive data
                        - Record timestamp and IP
                        - Store consent proof immutably
                        """,
                Arrays.asList("consent template", "consent form example", "create consent"));

        addArticle("breach_response_playbook", "Breach Response Procedure",
                "Practical Guidance",
                """
                        IMMEDIATE ACTIONS (0-6 hours):

                        1. CONTAIN
                           - Isolate affected systems
                           - Preserve evidence
                           - Activate incident response team

                        2. ASSESS
                           - Determine scope and impact
                           - Identify affected data types
                           - Count affected individuals

                        3. NOTIFY CERT-In (within 6 hours)
                           - If cyber incident
                           - Use CERT-In portal

                        SHORT-TERM (6-72 hours):

                        4. INVESTIGATE
                           - Root cause analysis
                           - Timeline construction
                           - Attack vector identification

                        5. NOTIFY DPBI (within 72 hours)
                           - Complete Form DPB-001
                           - Include all required details

                        6. NOTIFY AFFECTED INDIVIDUALS
                           - Clear communication
                           - Advice on protective steps

                        RECOVERY & REMEDIATION:
                        - Patch vulnerabilities
                        - Update controls
                        - Document lessons learned
                        """,
                Arrays.asList("breach response", "incident response", "breach steps"));

        addArticle("dpia_template", "How to Conduct DPIA",
                "Practical Guidance",
                """
                        DATA PROTECTION IMPACT ASSESSMENT STEPS:

                        1. DESCRIBE PROCESSING
                           - What data is processed
                           - How data flows
                           - Who has access
                           - Retention periods

                        2. ASSESS NECESSITY
                           - Is processing necessary?
                           - Is it proportionate?
                           - What is the legal basis?

                        3. IDENTIFY RISKS
                           - Unauthorized access
                           - Data loss/corruption
                           - Purpose creep
                           - Re-identification risks

                        4. RISK ASSESSMENT
                           For each risk:
                           - Likelihood (1-5)
                           - Impact (1-5)
                           - Risk score = L × I

                        5. MITIGATION MEASURES
                           For each risk:
                           - Technical controls
                           - Organizational controls
                           - Residual risk

                        6. APPROVAL
                           - DPO review
                           - Management sign-off
                           - DPBI consultation if high risk
                        """,
                Arrays.asList("DPIA steps", "impact assessment", "risk assessment"));

        addArticle("security_checklist", "Security Safeguards Checklist",
                "Practical Guidance",
                """
                        TECHNICAL CONTROLS:

                        ☐ Encryption at rest (AES-256)
                        ☐ Encryption in transit (TLS 1.3)
                        ☐ Access control (RBAC)
                        ☐ Multi-factor authentication
                        ☐ Audit logging
                        ☐ Intrusion detection
                        ☐ Data masking/pseudonymization
                        ☐ Backup and recovery
                        ☐ Vulnerability management
                        ☐ DLP controls

                        ORGANIZATIONAL CONTROLS:

                        ☐ Security policies
                        ☐ Access review procedures
                        ☐ Background checks
                        ☐ Security awareness training
                        ☐ Incident response plan
                        ☐ Third-party assessments
                        ☐ Business continuity plan
                        ☐ Regular security audits

                        QUANTUM-SAFE PREPARATION:

                        ☐ Inventory cryptographic assets
                        ☐ Plan PQC migration
                        ☐ Test hybrid algorithms
                        """,
                Arrays.asList("security controls", "technical measures", "safeguards"));
    }

    private static void addArticle(String id, String title, String reference,
            String content, List<String> keywords) {
        ARTICLES.put(id, new KnowledgeArticle(id, title, reference, content, keywords));
    }

    // ═══════════════════════════════════════════════════════════
    // SEARCH & RETRIEVAL
    // ═══════════════════════════════════════════════════════════

    public static List<KnowledgeArticle> search(String query) {
        String lowerQuery = query.toLowerCase();
        List<KnowledgeArticle> results = new ArrayList<>();

        for (KnowledgeArticle article : ARTICLES.values()) {
            double score = calculateRelevance(article, lowerQuery);
            if (score > 0) {
                article.setRelevanceScore(score);
                results.add(article);
            }
        }

        results.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        return results;
    }

    private static double calculateRelevance(KnowledgeArticle article, String query) {
        double score = 0;

        // Title match (high weight)
        if (article.getTitle().toLowerCase().contains(query)) {
            score += 10;
        }

        // Keyword match (medium weight)
        for (String keyword : article.getKeywords()) {
            if (query.contains(keyword.toLowerCase())) {
                score += 5;
            }
        }

        // Content match (lower weight but important)
        String lowerContent = article.getContent().toLowerCase();
        if (lowerContent.contains(query)) {
            score += 3;
        }

        // Individual word matches
        for (String word : query.split("\\s+")) {
            if (word.length() > 3 && lowerContent.contains(word)) {
                score += 1;
            }
        }

        return score;
    }

    public static KnowledgeArticle getById(String id) {
        return ARTICLES.get(id);
    }

    public static Collection<KnowledgeArticle> getAll() {
        return ARTICLES.values();
    }

    public static int getTotalArticles() {
        return ARTICLES.size();
    }

    // ═══════════════════════════════════════════════════════════
    // KNOWLEDGE ARTICLE CLASS
    // ═══════════════════════════════════════════════════════════

    public static class KnowledgeArticle {
        private final String id;
        private final String title;
        private final String reference;
        private final String content;
        private final List<String> keywords;
        private double relevanceScore;

        public KnowledgeArticle(String id, String title, String reference,
                String content, List<String> keywords) {
            this.id = id;
            this.title = title;
            this.reference = reference;
            this.content = content.trim();
            this.keywords = keywords;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getReference() {
            return reference;
        }

        public String getContent() {
            return content;
        }

        public List<String> getKeywords() {
            return keywords;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(double score) {
            this.relevanceScore = score;
        }

        public String getSummary(int maxLength) {
            if (content.length() <= maxLength)
                return content;
            return content.substring(0, maxLength) + "...";
        }
    }
}
