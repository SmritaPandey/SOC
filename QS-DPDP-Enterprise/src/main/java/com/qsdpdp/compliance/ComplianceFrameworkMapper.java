package com.qsdpdp.compliance;

import java.util.*;

/**
 * Compliance Framework Mapper - Maps features to compliance standards
 * Covers DPDP Act, ISO 27001, NIST CSF, IEEE 2089, STQC, PCI-DSS, HIPAA
 */
public class ComplianceFrameworkMapper {
    private final Map<String, ComplianceFramework> frameworks = new LinkedHashMap<>();
    private final Map<String, List<ControlMapping>> featureMappings = new LinkedHashMap<>();

    public ComplianceFrameworkMapper() { initialize(); }

    public void initialize() {
        loadFrameworks();
        loadMappings();
    }

    private void loadFrameworks() {
        frameworks.put("DPDP", new ComplianceFramework("DPDP", "Digital Personal Data Protection Act 2023", "India", true));
        frameworks.put("ISO27001", new ComplianceFramework("ISO27001", "ISO/IEC 27001:2022", "International", true));
        frameworks.put("ISO27701", new ComplianceFramework("ISO27701", "ISO/IEC 27701:2019 (PIMS)", "International", true));
        frameworks.put("NIST_CSF", new ComplianceFramework("NIST_CSF", "NIST Cybersecurity Framework 2.0", "USA", true));
        frameworks.put("IEEE2089", new ComplianceFramework("IEEE2089", "IEEE 2089 - Age-Appropriate Design", "International", true));
        frameworks.put("STQC", new ComplianceFramework("STQC", "STQC Certification Standards", "India", true));
        frameworks.put("PCI_DSS", new ComplianceFramework("PCI_DSS", "PCI DSS v4.0", "International", false));
        frameworks.put("HIPAA", new ComplianceFramework("HIPAA", "Health Insurance Portability and Accountability Act", "USA", false));
        frameworks.put("SOC2", new ComplianceFramework("SOC2", "SOC 2 Type II", "USA", false));
        frameworks.put("RBI", new ComplianceFramework("RBI", "RBI IT Governance & Data Guidelines", "India", true));
        frameworks.put("DSCI", new ComplianceFramework("DSCI", "DSCI Privacy Framework 2.0", "India", true));
    }

    private void loadMappings() {
        // PII Scanner
        mapFeature("pii-scanner", "DPDP", "Section 8", "Processing of personal data");
        mapFeature("pii-scanner", "ISO27001", "A.8.10", "Information deletion");
        mapFeature("pii-scanner", "NIST_CSF", "DE.CM-01", "Networks monitored for threats");
        mapFeature("pii-scanner", "STQC", "DQ-2.1", "Data quality assessment");

        // Consent Engine
        mapFeature("consent-engine", "DPDP", "Section 6", "Consent of data principal");
        mapFeature("consent-engine", "DPDP", "Section 7", "Certain legitimate uses");
        mapFeature("consent-engine", "ISO27001", "A.5.34", "Privacy and PII protection");
        mapFeature("consent-engine", "IEEE2089", "Clause 6", "Age-appropriate consent");

        // Breach Manager
        mapFeature("breach-manager", "DPDP", "Section 12", "Breach notification to Board");
        mapFeature("breach-manager", "ISO27001", "A.5.24", "Information security incident planning");
        mapFeature("breach-manager", "NIST_CSF", "RS.CO-02", "Incidents reported to authorities");

        // Rights Engine
        mapFeature("rights-engine", "DPDP", "Section 11", "Rights of data principal");
        mapFeature("rights-engine", "DPDP", "Section 13", "Right to grievance redressal");

        // Audit Service
        mapFeature("audit", "DPDP", "Section 10", "Compliance with duties");
        mapFeature("audit", "ISO27001", "A.5.33", "Protection of records");
        mapFeature("audit", "NIST_CSF", "ID.IM-02", "Improvements based on assessments");
        mapFeature("audit", "STQC", "AUD-1.0", "Audit trail integrity");

        // SIEM
        mapFeature("siem", "ISO27001", "A.8.15", "Logging");
        mapFeature("siem", "ISO27001", "A.8.16", "Monitoring activities");
        mapFeature("siem", "NIST_CSF", "DE.AE-02", "Events analyzed for anomalies");
        mapFeature("siem", "RBI", "IT-GOV-4", "Security operations center");

        // DLP
        mapFeature("dlp", "DPDP", "Section 8(4)", "Safeguard personal data");
        mapFeature("dlp", "ISO27001", "A.8.11", "Data masking");
        mapFeature("dlp", "ISO27001", "A.8.12", "Data leakage prevention");
        mapFeature("dlp", "PCI_DSS", "Req 3", "Protect stored account data");

        // UEBA
        mapFeature("ueba", "ISO27001", "A.8.16", "Monitoring activities");
        mapFeature("ueba", "NIST_CSF", "DE.AE-03", "Event correlation");

        // Threat Intelligence
        mapFeature("threat-intelligence", "NIST_CSF", "ID.RA-02", "Cyber threat intelligence received");
        mapFeature("threat-intelligence", "ISO27001", "A.5.7", "Threat intelligence");

        // Data Classification
        mapFeature("data-classification", "ISO27001", "A.5.12", "Classification of information");
        mapFeature("data-classification", "ISO27001", "A.5.13", "Labelling of information");
        mapFeature("data-classification", "DPDP", "Section 8(7)", "Data classification requirements");
    }

    private void mapFeature(String featureId, String frameworkId, String controlId, String controlName) {
        featureMappings.computeIfAbsent(featureId, k -> new ArrayList<>())
            .add(new ControlMapping(frameworkId, controlId, controlName));
    }

    /** Get all compliance controls mapped to a feature */
    public List<ControlMapping> getControlsForFeature(String featureId) {
        return featureMappings.getOrDefault(featureId, List.of());
    }

    /** Get compliance coverage report */
    public ComplianceCoverageReport getCoverageReport() {
        ComplianceCoverageReport report = new ComplianceCoverageReport();
        Map<String, Set<String>> frameworkControls = new LinkedHashMap<>();

        for (var entry : featureMappings.entrySet()) {
            for (ControlMapping mapping : entry.getValue()) {
                frameworkControls.computeIfAbsent(mapping.frameworkId, k -> new LinkedHashSet<>())
                    .add(mapping.controlId);
            }
        }

        for (var entry : frameworkControls.entrySet()) {
            FrameworkCoverage fc = new FrameworkCoverage();
            fc.frameworkId = entry.getKey();
            fc.frameworkName = frameworks.containsKey(entry.getKey()) ? frameworks.get(entry.getKey()).name : entry.getKey();
            fc.controlsCovered = entry.getValue().size();
            fc.controls = new ArrayList<>(entry.getValue());
            report.frameworkCoverage.add(fc);
        }

        report.totalFeaturesMapped = featureMappings.size();
        report.totalFrameworks = frameworks.size();
        return report;
    }

    /** Get all frameworks */
    public List<ComplianceFramework> getFrameworks() { return new ArrayList<>(frameworks.values()); }

    /** Get active frameworks */
    public List<ComplianceFramework> getActiveFrameworks() {
        return frameworks.values().stream().filter(f -> f.active).toList();
    }

    // Inner classes
    public static class ComplianceFramework {
        public String id, name, jurisdiction; public boolean active;
        public ComplianceFramework(String id, String name, String jurisdiction, boolean active) {
            this.id = id; this.name = name; this.jurisdiction = jurisdiction; this.active = active;
        }
    }

    public static class ControlMapping {
        public String frameworkId, controlId, controlName;
        public ControlMapping(String fid, String cid, String cn) { this.frameworkId = fid; this.controlId = cid; this.controlName = cn; }
    }

    public static class ComplianceCoverageReport {
        public int totalFeaturesMapped, totalFrameworks;
        public List<FrameworkCoverage> frameworkCoverage = new ArrayList<>();
    }

    public static class FrameworkCoverage {
        public String frameworkId, frameworkName;
        public int controlsCovered;
        public List<String> controls = new ArrayList<>();
    }
}
