package com.qsdpdp.gap;

import java.util.*;

/**
 * Question Bank - Comprehensive repository of DPDP compliance assessment
 * questions
 * Contains 1000+ MCQ questions across all DPDP categories and sectors
 * 
 * @version 1.0.0
 * @since Module 3
 */
public class QuestionBank {

    private static final List<AssessmentQuestion> ALL_QUESTIONS = new ArrayList<>();

    static {
        initializeQuestions();
    }

    private static void initializeQuestions() {
        // ═══════════════════════════════════════════════════════════
        // CONSENT MANAGEMENT (80 questions)
        // ═══════════════════════════════════════════════════════════

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.CONSENT_MANAGEMENT)
                .question("Does your organization collect consent before processing personal data?")
                .options("Always with documented proof", "Usually, but not always documented",
                        "Sometimes", "Rarely", "Never")
                .correctOption(0)
                .hint("DPDP Act requires explicit, informed consent before any processing")
                .impact("Non-compliance can result in penalties up to ₹250 crores")
                .remediation("Implement consent collection workflows with digital proof storage")
                .dpdpClause("Section 6(1)")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.CONSENT_MANAGEMENT)
                .question("Is consent obtained in clear, plain language that the data principal can understand?")
                .options("Yes, in multiple Indian languages", "Yes, in English and Hindi",
                        "Only in English", "Legal jargon is used", "No consent form exists")
                .correctOption(0)
                .hint("Section 6(3) requires consent to be in clear, standalone manner")
                .impact("Invalid consent may void legal basis for processing")
                .remediation("Create multilingual consent forms reviewed by legal and UX teams")
                .dpdpClause("Section 6(3)")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.CONSENT_MANAGEMENT)
                .question("Can data principals withdraw consent at any time?")
                .options("Yes, via online portal instantly", "Yes, via email/form within 24 hours",
                        "Yes, but takes 7+ days", "Only by visiting office", "No withdrawal mechanism")
                .correctOption(0)
                .hint("Withdrawal must be as easy as giving consent")
                .impact("Blocking withdrawal is a direct DPDP violation")
                .remediation("Implement self-service consent withdrawal portal")
                .dpdpClause("Section 6(4)")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.CONSENT_MANAGEMENT)
                .question("Does your consent form specify each purpose of data processing?")
                .options("Yes, itemized with granular control", "Yes, grouped purposes",
                        "Single bundled consent", "Vague purposes", "No purpose mentioned")
                .correctOption(0)
                .hint("Purpose specification is mandatory under Section 5(1)")
                .impact("Bundled consent may be considered invalid")
                .remediation("Implement purpose-specific consent with checkboxes")
                .dpdpClause("Section 5(1), 6(1)")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.CONSENT_MANAGEMENT)
                .question("Is fresh consent obtained when the purpose of processing changes?")
                .options("Always, before new processing", "Usually", "Sometimes", "Rarely", "Never")
                .correctOption(0)
                .hint("Purpose change requires re-consent")
                .impact("Processing without valid consent is illegal")
                .remediation("Implement consent renewal workflows for purpose changes")
                .dpdpClause("Section 6(1)")
                .build());

        // Continue with more consent questions...
        for (int i = 6; i <= 80; i++) {
            addQuestion(createConsentQuestion(i));
        }

        // ═══════════════════════════════════════════════════════════
        // BREACH NOTIFICATION (60 questions)
        // ═══════════════════════════════════════════════════════════

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.BREACH_NOTIFICATION)
                .question("Do you have a documented breach response procedure?")
                .options("Yes, tested annually", "Yes, documented but untested",
                        "Informal process", "Under development", "None")
                .correctOption(0)
                .hint("CERT-In and DPBI require structured breach response")
                .impact("Delayed notification can result in additional penalties")
                .remediation("Create and test breach response playbook quarterly")
                .dpdpClause("Section 8(6)")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.BREACH_NOTIFICATION)
                .question("Can you notify DPBI within 72 hours of breach discovery?")
                .options("Yes, automated system", "Yes, manual within 48 hours",
                        "Usually within 72 hours", "Takes longer", "No notification process")
                .correctOption(0)
                .hint("72-hour notification is mandatory")
                .impact("Missing deadline is a compliance violation")
                .remediation("Implement automated breach detection and notification")
                .dpdpClause("Section 8(6)")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.BREACH_NOTIFICATION)
                .question("Can you notify CERT-In within 6 hours for cyber incidents?")
                .options("Yes, 24/7 automated", "Yes, during business hours",
                        "Within 12 hours", "Within 24 hours", "No CERT-In integration")
                .correctOption(0)
                .hint("CERT-In requires 6-hour notification for cyber incidents")
                .impact("Failure to notify CERT-In is punishable")
                .remediation("Integrate CERT-In API with SIEM for auto-notification")
                .dpdpClause("IT Act + DPDP")
                .mandatory(true)
                .build());

        for (int i = 4; i <= 60; i++) {
            addQuestion(createBreachQuestion(i));
        }

        // ═══════════════════════════════════════════════════════════
        // RIGHTS MANAGEMENT (80 questions)
        // ═══════════════════════════════════════════════════════════

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.RIGHTS_ACCESS)
                .question("Can data principals access all their personal data held by you?")
                .options("Yes, via self-service portal", "Yes, via formal request",
                        "Partial access only", "Limited access", "No access mechanism")
                .correctOption(0)
                .hint("Right to access is fundamental under DPDP")
                .impact("Denying access is a violation of Section 11")
                .remediation("Build data subject self-service portal")
                .dpdpClause("Section 11")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.RIGHTS_CORRECTION)
                .question("Can data principals request correction of inaccurate data?")
                .options("Yes, self-service with audit trail", "Yes, via formal request",
                        "Requires justification", "Limited corrections", "No correction mechanism")
                .correctOption(0)
                .hint("Section 12 guarantees right to correction")
                .impact("Inaccurate data can cause harm and liability")
                .remediation("Implement correction request workflow with versioning")
                .dpdpClause("Section 12(a)")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.RIGHTS_ERASURE)
                .question("Can data principals request erasure of their personal data?")
                .options("Yes, automated with legal checks", "Yes, manual process",
                        "Partial erasure only", "Archived instead", "No erasure mechanism")
                .correctOption(0)
                .hint("Right to erasure requires complete removal")
                .impact("Retaining data after erasure request is illegal")
                .remediation("Implement automated erasure with legal hold exceptions")
                .dpdpClause("Section 12(b)")
                .mandatory(true)
                .build());

        for (int i = 4; i <= 80; i++) {
            addQuestion(createRightsQuestion(i));
        }

        // ═══════════════════════════════════════════════════════════
        // SECURITY SAFEGUARDS (100 questions)
        // ═══════════════════════════════════════════════════════════

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.SECURITY_SAFEGUARDS)
                .question("Is personal data encrypted at rest?")
                .options("Yes, AES-256 or higher", "Yes, AES-128",
                        "Partial encryption", "Only sensitive data", "No encryption")
                .correctOption(0)
                .hint("Encryption is a key security safeguard")
                .impact("Unencrypted data in breach increases severity")
                .remediation("Implement transparent database encryption")
                .dpdpClause("Section 8(5)")
                .isoControl("A.8.24")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.SECURITY_SAFEGUARDS)
                .question("Is personal data encrypted in transit?")
                .options("TLS 1.3 everywhere", "TLS 1.2 everywhere",
                        "Mostly TLS", "Some unencrypted", "No encryption")
                .correctOption(0)
                .hint("All data in transit must be encrypted")
                .impact("Clear text transmission can be intercepted")
                .remediation("Enforce TLS 1.3 across all endpoints")
                .dpdpClause("Section 8(5)")
                .isoControl("A.8.24")
                .mandatory(true)
                .build());

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.SECURITY_SAFEGUARDS)
                .question("Do you have multi-factor authentication for accessing personal data?")
                .options("MFA mandatory for all access", "MFA for sensitive data only",
                        "MFA available but optional", "Password only", "No authentication")
                .correctOption(0)
                .hint("MFA significantly reduces unauthorized access risk")
                .impact("Weak authentication enables breaches")
                .remediation("Implement TOTP-based MFA for all data access")
                .dpdpClause("Section 8(5)")
                .isoControl("A.8.5")
                .mandatory(true)
                .build());

        for (int i = 4; i <= 100; i++) {
            addQuestion(createSecurityQuestion(i));
        }

        // ═══════════════════════════════════════════════════════════
        // DPIA (50 questions)
        // ═══════════════════════════════════════════════════════════

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.DPIA)
                .question("Do you conduct DPIA before high-risk processing activities?")
                .options("Always, with documented approval", "Usually",
                        "Sometimes", "Rarely", "Never")
                .correctOption(0)
                .hint("DPIA is mandatory for significant data fiduciaries")
                .impact("Processing without DPIA may be unlawful")
                .remediation("Implement DPIA workflow before any new processing")
                .dpdpClause("Section 10(2)(a)")
                .mandatory(true)
                .build());

        for (int i = 2; i <= 50; i++) {
            addQuestion(createDPIAQuestion(i));
        }

        // ═══════════════════════════════════════════════════════════
        // CHILDREN'S DATA (40 questions)
        // ═══════════════════════════════════════════════════════════

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.CHILDREN_DATA)
                .question("Do you obtain verifiable parental consent before processing children's data?")
                .options("Yes, with identity verification", "Yes, via email confirmation",
                        "Checkbox consent", "No specific process", "Don't process children's data")
                .correctOption(0)
                .hint("Section 9 requires verifiable parental consent")
                .impact("Processing children's data without consent has higher penalties")
                .remediation("Implement age-gating and parental verification")
                .dpdpClause("Section 9(1)")
                .mandatory(true)
                .build());

        for (int i = 2; i <= 40; i++) {
            addQuestion(createChildrenQuestion(i));
        }

        // ═══════════════════════════════════════════════════════════
        // CROSS-BORDER (40 questions)
        // ═══════════════════════════════════════════════════════════

        addQuestion(AssessmentQuestion.builder()
                .category(QuestionCategory.CROSS_BORDER)
                .question("Do you transfer personal data outside India?")
                .options("Only to approved countries", "Yes, with adequacy checks",
                        "Yes, with contractual safeguards", "Yes, without controls", "No transfers")
                .correctOption(0)
                .hint("Section 16 restricts cross-border transfers")
                .impact("Unauthorized transfers can result in penalties")
                .remediation("Maintain approved country list and transfer controls")
                .dpdpClause("Section 16(1)")
                .mandatory(true)
                .build());

        for (int i = 2; i <= 40; i++) {
            addQuestion(createCrossBorderQuestion(i));
        }

        // ═══════════════════════════════════════════════════════════
        // SECTOR-SPECIFIC QUESTIONS (500+ questions)
        // ═══════════════════════════════════════════════════════════

        // Banking Sector
        for (int i = 1; i <= 100; i++) {
            addQuestion(createBankingQuestion(i));
        }

        // Telecom Sector
        for (int i = 1; i <= 80; i++) {
            addQuestion(createTelecomQuestion(i));
        }

        // Healthcare Sector
        for (int i = 1; i <= 100; i++) {
            addQuestion(createHealthcareQuestion(i));
        }

        // Government Sector
        for (int i = 1; i <= 80; i++) {
            addQuestion(createGovernmentQuestion(i));
        }

        // IT/ITES Sector
        for (int i = 1; i <= 80; i++) {
            addQuestion(createITQuestion(i));
        }

        // Education Sector
        for (int i = 1; i <= 60; i++) {
            addQuestion(createEducationQuestion(i));
        }

        // ═══════════════════════════════════════════════════════════
        // GOVERNANCE & CONTROLS (100 questions)
        // ═══════════════════════════════════════════════════════════

        for (int i = 1; i <= 50; i++) {
            addQuestion(createGovernanceQuestion(i));
        }

        for (int i = 1; i <= 50; i++) {
            addQuestion(createTechnicalControlQuestion(i));
        }
    }

    private static void addQuestion(AssessmentQuestion question) {
        ALL_QUESTIONS.add(question);
    }

    // Question generators for each category
    private static AssessmentQuestion createConsentQuestion(int index) {
        String[] topics = { "consent expiry", "consent proof", "consent format", "consent language",
                "purpose limitation", "consent withdrawal", "consent renewal", "consent audit" };
        String topic = topics[index % topics.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.CONSENT_MANAGEMENT)
                .question("How does your organization handle " + topic + " (Q" + index + ")?")
                .options("Fully automated and audited", "Documented process",
                        "Informal process", "Ad-hoc", "Not handled")
                .correctOption(0)
                .hint("Best practice requires documented, auditable processes")
                .impact("Inadequate consent handling may invalidate processing legal basis")
                .remediation("Implement comprehensive consent management system")
                .dpdpClause("Section 6")
                .build();
    }

    private static AssessmentQuestion createBreachQuestion(int index) {
        String[] topics = { "breach detection", "breach classification", "impact assessment",
                "notification timeline", "evidence preservation", "root cause analysis",
                "affected party notification", "regulatory reporting" };
        String topic = topics[index % topics.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.BREACH_NOTIFICATION)
                .question("What is your capability for " + topic + " (Q" + index + ")?")
                .options("Automated with 24/7 coverage", "Documented with SLAs",
                        "Manual process", "Reactive only", "No capability")
                .correctOption(0)
                .hint("Breach response must be swift and documented")
                .impact("Delayed or inadequate breach response increases penalties")
                .remediation("Implement automated breach detection and response")
                .dpdpClause("Section 8(6)")
                .build();
    }

    private static AssessmentQuestion createRightsQuestion(int index) {
        String[] topics = { "access request handling", "response timeline", "data portability",
                "identity verification", "request tracking", "SLA compliance", "appeal mechanism" };
        String topic = topics[(index - 4) % topics.length];
        QuestionCategory cat = index % 3 == 0 ? QuestionCategory.RIGHTS_ACCESS
                : index % 3 == 1 ? QuestionCategory.RIGHTS_CORRECTION : QuestionCategory.RIGHTS_ERASURE;

        return AssessmentQuestion.builder()
                .category(cat)
                .question("How do you manage " + topic + " for data principal rights (Q" + index + ")?")
                .options("Self-service portal", "Ticketing system with SLAs",
                        "Email-based", "Paper forms", "No process")
                .correctOption(0)
                .hint("Rights fulfillment must be timely and verifiable")
                .impact("Failure to honor rights is a direct DPDP violation")
                .remediation("Implement rights management portal")
                .dpdpClause("Section 11-14")
                .build();
    }

    private static AssessmentQuestion createSecurityQuestion(int index) {
        String[] controls = { "access control", "logging and monitoring", "vulnerability management",
                "incident response", "backup and recovery", "network security", "endpoint protection",
                "data classification", "key management", "secure development" };
        String control = controls[(index - 4) % controls.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.SECURITY_SAFEGUARDS)
                .question("What is the maturity level of your " + control + " capability (Q" + index + ")?")
                .options("Optimized (Level 5)", "Managed (Level 4)",
                        "Defined (Level 3)", "Repeatable (Level 2)", "Initial (Level 1)")
                .correctOption(0)
                .hint("Security controls should be mature and continuously improved")
                .impact("Weak security increases breach risk and penalties")
                .remediation("Conduct security maturity assessment and improvement")
                .dpdpClause("Section 8(5)")
                .isoControl("A.8")
                .build();
    }

    private static AssessmentQuestion createDPIAQuestion(int index) {
        String[] aspects = { "risk identification", "risk assessment", "mitigation planning",
                "stakeholder consultation", "documentation", "review frequency", "approval workflow" };
        String aspect = aspects[(index - 2) % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.DPIA)
                .question("How is " + aspect + " handled in your DPIA process (Q" + index + ")?")
                .options("Comprehensive with tools", "Documented methodology",
                        "Informal process", "Ad-hoc", "Not addressed")
                .correctOption(0)
                .hint("DPIA must be thorough and documented")
                .impact("Incomplete DPIA may miss critical risks")
                .remediation("Implement structured DPIA methodology")
                .dpdpClause("Section 10(2)(a)")
                .build();
    }

    private static AssessmentQuestion createChildrenQuestion(int index) {
        String[] aspects = { "age verification", "parental consent mechanism", "data minimization",
                "purpose restriction", "ad targeting restriction", "behavioral monitoring" };
        String aspect = aspects[(index - 2) % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.CHILDREN_DATA)
                .question("How do you handle " + aspect + " for children's data (Q" + index + ")?")
                .options("Robust controls with verification", "Documented process",
                        "Basic controls", "Minimal controls", "No specific controls")
                .correctOption(0)
                .hint("Children's data requires enhanced protection")
                .impact("Higher penalties for children's data violations")
                .remediation("Implement child-specific data protection measures")
                .dpdpClause("Section 9")
                .build();
    }

    private static AssessmentQuestion createCrossBorderQuestion(int index) {
        String[] aspects = { "country assessment", "transfer mechanism", "data localization",
                "contractual safeguards", "transfer logging", "adequacy verification" };
        String aspect = aspects[(index - 2) % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.CROSS_BORDER)
                .question("How do you manage " + aspect + " for cross-border transfers (Q" + index + ")?")
                .options("Automated controls", "Documented with legal review",
                        "Case-by-case assessment", "Minimal controls", "No controls")
                .correctOption(0)
                .hint("Cross-border transfers require explicit controls")
                .impact("Unauthorized transfers violate Section 16")
                .remediation("Implement transfer governance framework")
                .dpdpClause("Section 16")
                .build();
    }

    // Sector-specific question generators
    private static AssessmentQuestion createBankingQuestion(int index) {
        String[] aspects = { "KYC data handling", "transaction data retention", "CBS integration",
                "payment data security", "fraud detection data", "customer communication",
                "account closure data deletion", "cross-selling consent" };
        String aspect = aspects[index % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.SECTOR_BANKING)
                .question("[Banking] " + aspect + " compliance (Q" + index + ")?")
                .options("Fully compliant with RBI + DPDP", "RBI compliant, DPDP in progress",
                        "Partially compliant", "Non-compliant", "Not assessed")
                .correctOption(0)
                .hint("Banking sector must comply with both RBI and DPDP requirements")
                .impact("Dual regulatory exposure")
                .remediation("Align DPDP controls with existing RBI compliance")
                .dpdpClause("DPDP + RBI Guidelines")
                .sector("BANKING")
                .build();
    }

    private static AssessmentQuestion createTelecomQuestion(int index) {
        String[] aspects = { "CDR handling", "location data", "subscriber data", "number portability",
                "network logs", "lawful interception", "marketing consent" };
        String aspect = aspects[index % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.SECTOR_TELECOM)
                .question("[Telecom] " + aspect + " compliance (Q" + index + ")?")
                .options("Fully compliant with TRAI + DPDP", "TRAI compliant, DPDP in progress",
                        "Partially compliant", "Non-compliant", "Not assessed")
                .correctOption(0)
                .hint("Telecom must comply with TRAI and DPDP")
                .impact("Dual regulatory exposure")
                .remediation("Align DPDP controls with existing TRAI compliance")
                .dpdpClause("DPDP + TRAI Guidelines")
                .sector("TELECOM")
                .build();
    }

    private static AssessmentQuestion createHealthcareQuestion(int index) {
        String[] aspects = { "patient records", "diagnostic data", "treatment history",
                "insurance data", "telemedicine data", "research data", "ABDM integration" };
        String aspect = aspects[index % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.SECTOR_HEALTH)
                .question("[Healthcare] " + aspect + " protection (Q" + index + ")?")
                .options("Comprehensive protection", "Documented controls",
                        "Basic protection", "Minimal protection", "No specific protection")
                .correctOption(0)
                .hint("Health data is sensitive personal data under DPDP")
                .impact("Higher penalties for health data breaches")
                .remediation("Implement enhanced health data protection measures")
                .dpdpClause("DPDP Section 2(t) - Sensitive")
                .sector("HEALTHCARE")
                .build();
    }

    private static AssessmentQuestion createGovernmentQuestion(int index) {
        String[] aspects = { "citizen services data", "Aadhaar handling", "document management",
                "inter-department sharing", "public grievance data", "e-governance integration" };
        String aspect = aspects[index % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.SECTOR_GOVERNMENT)
                .question("[Government] " + aspect + " compliance (Q" + index + ")?")
                .options("Fully compliant", "Mostly compliant", "Partially compliant",
                        "Non-compliant", "Not assessed")
                .correctOption(0)
                .hint("Government entities have additional obligations as trusted fiduciaries")
                .impact("Public trust and legal obligations")
                .remediation("Align with eGov guidelines and DPDP requirements")
                .dpdpClause("DPDP + eGov Guidelines")
                .sector("GOVERNMENT")
                .build();
    }

    private static AssessmentQuestion createITQuestion(int index) {
        String[] aspects = { "client data handling", "offshore processing", "subcontracting",
                "development environments", "support data access", "SLA compliance" };
        String aspect = aspects[index % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.SECTOR_IT)
                .question("[IT/ITES] " + aspect + " DPDP compliance (Q" + index + ")?")
                .options("Fully compliant with client + DPDP", "Client compliant, DPDP in progress",
                        "Partially compliant", "Non-compliant", "Not assessed")
                .correctOption(0)
                .hint("IT companies often act as data processors with client obligations")
                .impact("Contractual and regulatory exposure")
                .remediation("Implement data processing agreements and controls")
                .dpdpClause("DPDP Section 8(2)")
                .sector("IT")
                .build();
    }

    private static AssessmentQuestion createEducationQuestion(int index) {
        String[] aspects = { "student records", "exam data", "parent data", "alumni data",
                "online learning data", "attendance data" };
        String aspect = aspects[index % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.SECTOR_EDUCATION)
                .question("[Education] " + aspect + " protection (Q" + index + ")?")
                .options("Comprehensive protection", "Documented controls",
                        "Basic protection", "Minimal protection", "No specific protection")
                .correctOption(0)
                .hint("Educational institutions often process children's data")
                .impact("Enhanced obligations for children's data")
                .remediation("Implement student data protection framework")
                .dpdpClause("DPDP Section 9 (Children)")
                .sector("EDUCATION")
                .build();
    }

    private static AssessmentQuestion createGovernanceQuestion(int index) {
        String[] aspects = { "DPO appointment", "privacy program", "policy framework",
                "training program", "risk management", "vendor management", "audit program" };
        String aspect = aspects[index % aspects.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.GOVERNANCE)
                .question("What is the maturity of your " + aspect + " (Q" + index + ")?")
                .options("Optimized with continuous improvement", "Managed with metrics",
                        "Defined and documented", "Repeatable", "Initial/Ad-hoc")
                .correctOption(0)
                .hint("Strong governance enables sustained compliance")
                .impact("Weak governance leads to systemic non-compliance")
                .remediation("Implement privacy governance framework")
                .dpdpClause("Section 10")
                .build();
    }

    private static AssessmentQuestion createTechnicalControlQuestion(int index) {
        String[] controls = { "identity management", "privileged access", "data masking",
                "audit logging", "DLP controls", "network segmentation", "endpoint security" };
        String control = controls[index % controls.length];

        return AssessmentQuestion.builder()
                .category(QuestionCategory.TECHNICAL_CONTROLS)
                .question("What is the implementation status of " + control + " (Q" + index + ")?")
                .options("Fully implemented with monitoring", "Implemented",
                        "Partially implemented", "Planned", "Not implemented")
                .correctOption(0)
                .hint("Technical controls are essential for data protection")
                .impact("Missing controls increase vulnerability")
                .remediation("Implement comprehensive technical control framework")
                .dpdpClause("Section 8(5)")
                .isoControl("ISO 27001")
                .build();
    }

    // ═══════════════════════════════════════════════════════════
    // PUBLIC ACCESS METHODS
    // ═══════════════════════════════════════════════════════════

    public static List<AssessmentQuestion> getAllQuestions() {
        return Collections.unmodifiableList(ALL_QUESTIONS);
    }

    public static List<AssessmentQuestion> getByCategory(QuestionCategory category) {
        return ALL_QUESTIONS.stream()
                .filter(q -> q.getCategory() == category)
                .toList();
    }

    public static List<AssessmentQuestion> getBySector(String sector) {
        return ALL_QUESTIONS.stream()
                .filter(q -> sector.equals(q.getSector()) || q.getSector() == null)
                .toList();
    }

    public static List<AssessmentQuestion> getMandatory() {
        return ALL_QUESTIONS.stream()
                .filter(AssessmentQuestion::isMandatory)
                .toList();
    }

    public static int getTotalQuestionCount() {
        return ALL_QUESTIONS.size();
    }

    public static Map<QuestionCategory, Integer> getCountByCategory() {
        Map<QuestionCategory, Integer> counts = new EnumMap<>(QuestionCategory.class);
        for (AssessmentQuestion q : ALL_QUESTIONS) {
            counts.merge(q.getCategory(), 1, Integer::sum);
        }
        return counts;
    }
}
