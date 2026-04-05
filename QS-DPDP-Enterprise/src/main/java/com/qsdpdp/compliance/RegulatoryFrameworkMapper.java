package com.qsdpdp.compliance;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Regulatory Framework Cross-Mapper
 * Maps DPDP Act 2023 requirements to equivalent controls in:
 * - GDPR (EU General Data Protection Regulation)
 * - ISO 27001:2022 (Information Security Management)
 * - NIST Cybersecurity Framework (CSF 2.0)
 * - SOC 2 Type II
 *
 * Enables multi-framework compliance reporting from a single assessment.
 *
 * @version 3.0.0
 * @since Phase 4
 */
@Component
public class RegulatoryFrameworkMapper {

    private final Map<String, FrameworkMapping> mappings = new LinkedHashMap<>();

    public RegulatoryFrameworkMapper() {
        initializeMappings();
    }

    private void initializeMappings() {
        // Consent Management — DPDP Section 6-7
        addMapping("DPDP-CONSENT", "Consent Management (Section 6-7)",
                "GDPR Art. 6-7", "ISO 27701:A.7.2", "NIST PR.IP-1", "SOC2 CC6.1",
                "Lawful processing requires informed, specific, freely given consent");

        // Children's Data — DPDP Section 9
        addMapping("DPDP-CHILDREN", "Children's Data Protection (Section 9)",
                "GDPR Art. 8", "ISO 27701:A.7.4", "NIST PR.IP-1", "SOC2 CC6.1",
                "Verifiable guardian consent required for processing children's data");

        // Data Principal Rights — DPDP Section 11-14
        addMapping("DPDP-RIGHTS-ACCESS", "Right of Access (Section 11)",
                "GDPR Art. 15", "ISO 27701:A.7.3.2", "NIST PR.AC-1", "SOC2 CC6.3",
                "Data principal can request summary of personal data processing");

        addMapping("DPDP-RIGHTS-CORRECTION", "Right of Correction (Section 12)",
                "GDPR Art. 16", "ISO 27701:A.7.3.6", "NIST PR.AC-1", "SOC2 CC6.3",
                "Right to correct inaccurate or misleading personal data");

        addMapping("DPDP-RIGHTS-ERASURE", "Right of Erasure (Section 12)",
                "GDPR Art. 17", "ISO 27701:A.7.4.5", "NIST PR.IP-6", "SOC2 CC6.5",
                "Right to erasure of personal data when consent withdrawn");

        addMapping("DPDP-RIGHTS-GRIEVANCE", "Right of Grievance (Section 13)",
                "GDPR Art. 77", "ISO 27701:A.7.3.9", "NIST RS.CO-3", "SOC2 CC2.3",
                "Right to register grievance with Data Fiduciary");

        addMapping("DPDP-RIGHTS-NOMINATION", "Right of Nomination (Section 14)",
                "GDPR Art. N/A", "ISO 27701:N/A", "NIST N/A", "SOC2 N/A",
                "Right to nominate another person for rights exercise in case of death/incapacity");

        // Breach Notification — DPDP Section 8
        addMapping("DPDP-BREACH", "Breach Notification (Section 8)",
                "GDPR Art. 33-34", "ISO 27001:A.16.1", "NIST RS.CO-2", "SOC2 CC7.4",
                "Notify DPBI within 72 hours, CERT-IN within 6 hours of data breach");

        // Data Fiduciary Obligations — DPDP Section 8
        addMapping("DPDP-FIDUCIARY", "Data Fiduciary Obligations (Section 8)",
                "GDPR Art. 24-25", "ISO 27001:A.5.1", "NIST ID.GV-1", "SOC2 CC1.1",
                "Ensure accuracy, completeness, consistency of personal data");

        // Significant Data Fiduciary — DPDP Section 10
        addMapping("DPDP-SDF", "Significant Data Fiduciary (Section 10)",
                "GDPR Art. 37-39", "ISO 27001:A.5.2", "NIST ID.GV-2", "SOC2 CC1.2",
                "DPO appointment, periodic audits, DPIA obligation");

        // Cross-Border Transfer — DPDP Section 16
        addMapping("DPDP-CROSSBORDER", "Cross-Border Transfer (Section 16)",
                "GDPR Art. 44-49", "ISO 27701:A.7.5", "NIST PR.DS-5", "SOC2 CC6.6",
                "Transfer allowed to notified countries only");

        // Data Retention & Deletion
        addMapping("DPDP-RETENTION", "Data Retention (Section 8(7))",
                "GDPR Art. 5(1)(e)", "ISO 27001:A.8.10", "NIST PR.IP-6", "SOC2 CC6.5",
                "Erase personal data when consent withdrawn or purpose served");

        // Security Safeguards
        addMapping("DPDP-SECURITY", "Security Safeguards (Section 8(4))",
                "GDPR Art. 32", "ISO 27001:A.8", "NIST PR.DS", "SOC2 CC6.1",
                null, "Reasonable security safeguards to protect personal data");

        // ═══ INDIAN SECTOR-SPECIFIC REGULATORY MAPPINGS ═══

        // RBI — Banking & Financial Services
        addMapping("RBI-ITG", "RBI Master Direction on IT Governance",
                "GDPR Art. 24-25", "ISO 27001:A.5.1", "NIST ID.GV-1", "SOC2 CC1.1",
                "RBI", "IT governance framework for regulated entities");

        addMapping("RBI-CSF", "RBI Cyber Security Framework",
                "GDPR Art. 32", "ISO 27001:A.8", "NIST PR.DS", "SOC2 CC6.1",
                "RBI", "Baseline cyber security controls for banks and NBFCs");

        addMapping("RBI-DL", "RBI Data Localisation Directive",
                "GDPR Art. 44-49", "ISO 27701:A.7.5", "NIST PR.DS-5", "SOC2 CC6.6",
                "RBI", "Payment data must be stored exclusively in India");

        addMapping("RBI-KYC", "RBI KYC Master Direction",
                "GDPR Art. 6-7", "ISO 27701:A.7.2", "NIST PR.IP-1", "SOC2 CC6.1",
                "RBI", "KYC data collection, retention, and deletion requirements");

        // UIDAI — Aadhaar Ecosystem
        addMapping("UIDAI-AUTH", "UIDAI Authentication Circulars",
                "GDPR Art. 6-7", "ISO 27001:A.9", "NIST PR.AC-1", "SOC2 CC6.1",
                "UIDAI", "Aadhaar authentication data handling and vault requirements");

        addMapping("UIDAI-DP", "UIDAI Data Protection Requirements",
                "GDPR Art. 32", "ISO 27001:A.8", "NIST PR.DS", "SOC2 CC6.1",
                "UIDAI", "Aadhaar data vault, HSM, access controls");

        // IRDAI — Insurance Sector
        addMapping("IRDAI-CS", "IRDAI Cyber Security Guidelines",
                "GDPR Art. 32", "ISO 27001:A.8", "NIST PR.DS", "SOC2 CC6.1",
                "IRDAI", "Cyber security framework for insurers and intermediaries");

        addMapping("IRDAI-DP", "IRDAI Data Protection Guidelines 2017",
                "GDPR Art. 24-25", "ISO 27001:A.5.1", "NIST ID.GV-1", "SOC2 CC1.1",
                "IRDAI", "Data protection obligations for insurance companies");

        addMapping("IRDAI-TPA", "IRDAI TPA Regulations",
                "GDPR Art. 28", "ISO 27001:A.15", "NIST ID.SC", "SOC2 CC9.2",
                "IRDAI", "Third-party administrator governance and oversight");

        // ABDM — Healthcare Ecosystem
        addMapping("ABDM-HDP", "ABDM Health Data Management Policy",
                "GDPR Art. 6-7", "ISO 27701:A.7.2", "NIST PR.IP-1", "SOC2 CC6.1",
                "MoHFW/NHA", "Health data consent and management framework");

        addMapping("ABDM-HIE", "ABDM Health Information Exchange Standards",
                "GDPR Art. 44-49", "ISO 27701:A.7.5", "NIST PR.DS-5", "SOC2 CC6.6",
                "NHA", "Interoperability and health information exchange privacy");

        // ICMR — Research & Clinical Trials
        addMapping("ICMR-BE", "ICMR Bioethics Guidelines",
                "GDPR Art. 6-7", "ISO 27701:A.7.2", "NIST PR.IP-1", "SOC2 CC6.1",
                "ICMR", "Informed consent and biobank governance for research");
    }

    private void addMapping(String dpdpId, String dpdpDescription,
            String gdpr, String iso27001, String nistCsf, String soc2, String notes) {
        mappings.put(dpdpId, new FrameworkMapping(
                dpdpId, dpdpDescription, gdpr, iso27001, nistCsf, soc2, null, notes));
    }

    private void addMapping(String dpdpId, String dpdpDescription,
            String gdpr, String iso27001, String nistCsf, String soc2,
            String sectorRegulator, String notes) {
        mappings.put(dpdpId, new FrameworkMapping(
                dpdpId, dpdpDescription, gdpr, iso27001, nistCsf, soc2, sectorRegulator, notes));
    }

    /**
     * Get all DPDP → multi-framework mappings
     */
    public List<FrameworkMapping> getAllMappings() {
        return new ArrayList<>(mappings.values());
    }

    /**
     * Get mapping for a specific DPDP requirement
     */
    public FrameworkMapping getMapping(String dpdpId) {
        return mappings.get(dpdpId);
    }

    /**
     * Get all DPDP requirements that map to a specific GDPR article
     */
    public List<FrameworkMapping> findByGDPR(String gdprReference) {
        return mappings.values().stream()
                .filter(m -> m.gdpr.contains(gdprReference))
                .toList();
    }

    /**
     * Get all DPDP requirements that map to an ISO 27001 control
     */
    public List<FrameworkMapping> findByISO27001(String isoControl) {
        return mappings.values().stream()
                .filter(m -> m.iso27001.contains(isoControl))
                .toList();
    }

    /**
     * Get all mappings for a specific Indian sector regulator
     */
    public List<FrameworkMapping> findBySectorRegulator(String regulator) {
        return mappings.values().stream()
                .filter(m -> m.sectorRegulator != null && m.sectorRegulator.contains(regulator))
                .toList();
    }

    /**
     * Generate cross-framework compliance matrix
     */
    public Map<String, Object> getComplianceMatrix() {
        Map<String, Object> matrix = new LinkedHashMap<>();
        matrix.put("totalMappings", mappings.size());
        matrix.put("frameworks", List.of("DPDP 2023", "GDPR", "ISO 27001", "NIST CSF 2.0", "SOC 2",
                "RBI", "IRDAI", "ABDM", "UIDAI", "ICMR"));
        matrix.put("mappings", mappings.values().stream().map(m -> {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("dpdpId", m.dpdpId);
            map.put("dpdp", m.dpdpDescription);
            map.put("gdpr", m.gdpr);
            map.put("iso27001", m.iso27001);
            map.put("nistCsf", m.nistCsf);
            map.put("soc2", m.soc2);
            if (m.sectorRegulator != null) map.put("sectorRegulator", m.sectorRegulator);
            return map;
        }).toList());
        return matrix;
    }

    // ═══ DATA CLASS ═══
    public static class FrameworkMapping {
        public final String dpdpId;
        public final String dpdpDescription;
        public final String gdpr;
        public final String iso27001;
        public final String nistCsf;
        public final String soc2;
        public final String sectorRegulator;
        public final String notes;

        public FrameworkMapping(String dpdpId, String dpdpDescription,
                String gdpr, String iso27001, String nistCsf, String soc2,
                String sectorRegulator, String notes) {
            this.dpdpId = dpdpId;
            this.dpdpDescription = dpdpDescription;
            this.gdpr = gdpr;
            this.iso27001 = iso27001;
            this.nistCsf = nistCsf;
            this.soc2 = soc2;
            this.sectorRegulator = sectorRegulator;
            this.notes = notes;
        }
    }
}
