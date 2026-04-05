package com.qsdpdp.consent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Sector Purpose Template — Pre-built DPDP-Compliant Purpose Templates
 * Provides ready-to-use consent purpose templates for 6 Indian sectors:
 *   - BFSI (Banking, Financial Services & Insurance)
 *   - Healthcare (Hospitals, Health-tech, ABDM)
 *   - Telecom (TSPs, ISPs, OTT)
 *   - E-Commerce (Online retail, digital marketplace)
 *   - Education (EdTech, Universities, Schools)
 *   - Government (Aadhaar, DBT, DigiLocker, welfare)
 *
 * Each template specifies legal basis, data categories, retention periods,
 * and regulatory references aligned with sector-specific Indian regulations.
 *
 * @version 1.0.0
 * @since Phase 2 — Consent Enhancement
 */
public class SectorPurposeTemplate {

    private String id;
    private String sector;
    private String code;
    private String name;
    private String description;
    private String legalBasis;              // consent, legal_obligation, legitimate_interest, contract
    private List<String> dataCategories;
    private String retentionPeriod;
    private boolean mandatory;
    private String regulatoryReference;
    private LocalDateTime createdAt;

    public SectorPurposeTemplate() {
        this.mandatory = false;
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLegalBasis() { return legalBasis; }
    public void setLegalBasis(String legalBasis) { this.legalBasis = legalBasis; }

    public List<String> getDataCategories() { return dataCategories; }
    public void setDataCategories(List<String> dataCategories) { this.dataCategories = dataCategories; }

    public String getRetentionPeriod() { return retentionPeriod; }
    public void setRetentionPeriod(String retentionPeriod) { this.retentionPeriod = retentionPeriod; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public String getRegulatoryReference() { return regulatoryReference; }
    public void setRegulatoryReference(String regulatoryReference) { this.regulatoryReference = regulatoryReference; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ═══ ARTICLE-DRIVEN SECTOR PURPOSE TEMPLATES ═══

    /**
     * Creates all sector-specific consent purpose templates as recommended by
     * Vinod Shah's DPDP operationalisation framework for BFSI, Healthcare & Insurance.
     */
    public static List<SectorPurposeTemplate> createAllSectorTemplates() {
        List<SectorPurposeTemplate> templates = new ArrayList<>();
        templates.addAll(createBFSITemplates());
        templates.addAll(createHealthcareTemplates());
        templates.addAll(createInsuranceTemplates());
        return templates;
    }

    public static List<SectorPurposeTemplate> createBFSITemplates() {
        return List.of(
            build("BFSI", "BFSI-ADV-01", "Aadhaar Data Vault Storage",
                "Storage and processing of Aadhaar data in UIDAI-mandated isolated vault",
                "legal_obligation", List.of("AADHAAR", "IDENTITY"), "As per UIDAI mandate", true, "UIDAI Authentication Circulars"),
            build("BFSI", "BFSI-UPI-01", "UPI Transaction Processing",
                "Processing UPI payment transactions with tokenised card data",
                "contract", List.of("PAYMENT", "TRANSACTION"), "As per RBI mandate", true, "RBI UPI/NPCI Guidelines"),
            build("BFSI", "BFSI-KYC-01", "KYC Verification & Retention",
                "Collection and storage of KYC documents for identity verification",
                "legal_obligation", List.of("IDENTITY", "ADDRESS", "FINANCIAL"), "5 years post relationship end", true, "RBI KYC Master Direction"),
            build("BFSI", "BFSI-AML-01", "AML/CFT Monitoring",
                "Transaction monitoring for anti-money laundering compliance",
                "legal_obligation", List.of("TRANSACTION", "FINANCIAL", "IDENTITY"), "10 years", true, "PMLA 2002, RBI AML Directions"),
            build("BFSI", "BFSI-CRD-01", "Credit Scoring & Assessment",
                "Sharing data with CICs for credit scoring and loan assessment",
                "consent", List.of("FINANCIAL", "IDENTITY", "EMPLOYMENT"), "Duration of credit facility", false, "RBI CIC Regulations"),
            build("BFSI", "BFSI-FRD-01", "Fraud Detection",
                "Processing transaction data for fraud detection and prevention",
                "legitimate_interest", List.of("TRANSACTION", "DEVICE", "LOCATION"), "3 years", true, "RBI Fraud Monitoring Framework"),
            build("BFSI", "BFSI-LOC-01", "Data Localisation Compliance",
                "Ensuring all payment data is stored within India per RBI mandate",
                "legal_obligation", List.of("PAYMENT", "TRANSACTION", "CARD"), "As required", true, "RBI Data Localisation Directive"),
            build("BFSI", "BFSI-DBT-01", "Direct Benefit Transfer",
                "Processing bank account data for government DBT disbursement",
                "legal_obligation", List.of("FINANCIAL", "IDENTITY", "AADHAAR"), "As per scheme guidelines", true, "DBT Mission Guidelines")
        );
    }

    public static List<SectorPurposeTemplate> createHealthcareTemplates() {
        return List.of(
            build("HEALTHCARE", "HC-EHR-01", "EHR Clinical Access",
                "Accessing electronic health records for patient treatment",
                "contract", List.of("HEALTH", "MEDICAL", "DIAGNOSTIC"), "As per Clinical Establishments Act", true, "ABDM Health Data Mgmt Policy"),
            build("HEALTHCARE", "HC-ABDM-01", "ABDM Health Information Exchange",
                "Sharing health data through ABDM network with patient consent",
                "consent", List.of("HEALTH", "MEDICAL", "PRESCRIPTIONS"), "As per ABDM policy", false, "ABDM HIE Standards"),
            build("HEALTHCARE", "HC-TM-01", "Telemedicine Consultation",
                "Recording and processing telemedicine consultation data",
                "consent", List.of("HEALTH", "VIDEO", "PRESCRIPTIONS"), "3 years post consultation", false, "MoHFW Telemedicine Guidelines"),
            build("HEALTHCARE", "HC-CT-01", "Clinical Trial Data",
                "Processing participant data for clinical trial research",
                "consent", List.of("HEALTH", "GENETIC", "BIOMETRIC", "DEMOGRAPHIC"), "As per trial protocol + 15 years", false, "ICMR Bioethics Guidelines"),
            build("HEALTHCARE", "HC-GEN-01", "Genetic Data Research",
                "Processing genetic and genomic data for research purposes",
                "consent", List.of("GENETIC", "BIOMETRIC", "HEALTH"), "Duration of research + 10 years", false, "PCPNDT Act, ICMR Guidelines"),
            build("HEALTHCARE", "HC-WRB-01", "Wearable Health Data",
                "Collecting and processing health data from wearable devices",
                "consent", List.of("HEALTH", "BIOMETRIC", "LOCATION"), "Duration of service", false, "ABDM HDP"),
            build("HEALTHCARE", "HC-MH-01", "Mental Health Records",
                "Processing mental health records with enhanced safeguards",
                "consent", List.of("HEALTH", "MENTAL_HEALTH"), "As per Mental Healthcare Act", false, "Mental Healthcare Act 2017"),
            build("HEALTHCARE", "HC-AI-01", "AI Diagnostic Analysis",
                "Using anonymised health data for AI diagnostic model training",
                "consent", List.of("HEALTH", "DIAGNOSTIC", "IMAGING"), "Duration of model lifecycle", false, "ABDM HDP, DPDP S.8"),
            build("HEALTHCARE", "HC-ABHA-01", "ABHA Number Management",
                "Management and protection of Ayushman Bharat Health Account numbers",
                "legal_obligation", List.of("IDENTITY", "HEALTH"), "Duration of ABHA lifecycle", true, "ABDM ABHA Guidelines")
        );
    }

    public static List<SectorPurposeTemplate> createInsuranceTemplates() {
        return List.of(
            build("INSURANCE", "INS-UW-01", "Underwriting Risk Assessment",
                "Collecting proportionate data for underwriting risk evaluation",
                "contract", List.of("HEALTH", "FINANCIAL", "DEMOGRAPHIC", "LIFESTYLE"), "Duration of policy + 8 years", true, "IRDAI Underwriting Guidelines"),
            build("INSURANCE", "INS-CLM-01", "Claims Processing",
                "Processing personal data for insurance claims settlement",
                "contract", List.of("HEALTH", "FINANCIAL", "IDENTITY", "MEDICAL"), "8 years post settlement", true, "IRDAI Claims Settlement Guidelines"),
            build("INSURANCE", "INS-TPA-01", "TPA Administration",
                "Sharing policyholder data with Third Party Administrators",
                "contract", List.of("HEALTH", "IDENTITY", "POLICY"), "Duration of TPA agreement", true, "IRDAI TPA Regulations"),
            build("INSURANCE", "INS-FRD-01", "Insurance Fraud Investigation",
                "Processing data for fraud detection in claims and applications",
                "legitimate_interest", List.of("IDENTITY", "FINANCIAL", "HEALTH", "LOCATION"), "Duration of investigation + 3 years", true, "Insurance Act 1938"),
            build("INSURANCE", "INS-RNW-01", "Policy Renewal Communications",
                "Contacting policyholders for policy renewal and service",
                "contract", List.of("CONTACT", "POLICY"), "Duration of policy", false, "IRDAI Marketing Guidelines"),
            build("INSURANCE", "INS-XSL-01", "Cross-Selling Products",
                "Marketing additional insurance products to existing customers",
                "consent", List.of("CONTACT", "DEMOGRAPHIC", "POLICY"), "Until consent withdrawn", false, "IRDAI Distribution Guidelines"),
            build("INSURANCE", "INS-AGT-01", "Agent/Broker Data Handling",
                "Managing policyholder data through agents and brokers",
                "contract", List.of("IDENTITY", "CONTACT", "POLICY"), "Duration of agency agreement", true, "IRDAI Agent Regulations"),
            build("INSURANCE", "INS-AML-01", "Insurance AML Compliance",
                "Processing data for anti-money laundering in insurance",
                "legal_obligation", List.of("IDENTITY", "FINANCIAL", "TRANSACTION"), "10 years", true, "IRDAI AML Guidelines")
        );
    }

    private static SectorPurposeTemplate build(String sector, String code, String name, String desc,
            String legalBasis, List<String> cats, String retention, boolean mandatory, String regRef) {
        SectorPurposeTemplate t = new SectorPurposeTemplate();
        t.setId(java.util.UUID.randomUUID().toString());
        t.setSector(sector); t.setCode(code); t.setName(name); t.setDescription(desc);
        t.setLegalBasis(legalBasis); t.setDataCategories(cats); t.setRetentionPeriod(retention);
        t.setMandatory(mandatory); t.setRegulatoryReference(regRef);
        return t;
    }
}
