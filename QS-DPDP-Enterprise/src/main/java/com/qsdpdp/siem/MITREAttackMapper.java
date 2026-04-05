package com.qsdpdp.siem;

import java.util.*;

/**
 * MITRE ATT&CK Framework Mapper
 * Maps events, correlation rules, and alerts to MITRE ATT&CK tactics and techniques
 * Provides coverage heatmap and gap analysis
 */
public class MITREAttackMapper {

    // Tactic → List of techniques
    private final Map<String, List<Technique>> attackMatrix = new LinkedHashMap<>();
    // Technique ID → covered by our detection
    private final Map<String, Boolean> coverageMap = new HashMap<>();

    public MITREAttackMapper() { loadAttackMatrix(); }

    public void initialize() { loadAttackMatrix(); }

    private void loadAttackMatrix() {
        // Initial Access
        addTechnique("TA0001", "Initial Access", "T1078", "Valid Accounts", true);
        addTechnique("TA0001", "Initial Access", "T1566", "Phishing", true);
        addTechnique("TA0001", "Initial Access", "T1190", "Exploit Public-Facing Application", true);
        addTechnique("TA0001", "Initial Access", "T1133", "External Remote Services", true);

        // Execution
        addTechnique("TA0002", "Execution", "T1059", "Command and Scripting Interpreter", true);
        addTechnique("TA0002", "Execution", "T1053", "Scheduled Task/Job", true);
        addTechnique("TA0002", "Execution", "T1204", "User Execution", false);

        // Persistence
        addTechnique("TA0003", "Persistence", "T1098", "Account Manipulation", true);
        addTechnique("TA0003", "Persistence", "T1136", "Create Account", true);
        addTechnique("TA0003", "Persistence", "T1543", "Create/Modify System Process", false);

        // Privilege Escalation
        addTechnique("TA0004", "Privilege Escalation", "T1548", "Abuse Elevation Control", true);
        addTechnique("TA0004", "Privilege Escalation", "T1134", "Access Token Manipulation", false);

        // Defense Evasion
        addTechnique("TA0005", "Defense Evasion", "T1070", "Indicator Removal", true);
        addTechnique("TA0005", "Defense Evasion", "T1036", "Masquerading", false);
        addTechnique("TA0005", "Defense Evasion", "T1027", "Obfuscated Files", false);

        // Credential Access
        addTechnique("TA0006", "Credential Access", "T1110", "Brute Force", true);
        addTechnique("TA0006", "Credential Access", "T1003", "OS Credential Dumping", true);
        addTechnique("TA0006", "Credential Access", "T1552", "Unsecured Credentials", true);

        // Discovery
        addTechnique("TA0007", "Discovery", "T1087", "Account Discovery", true);
        addTechnique("TA0007", "Discovery", "T1046", "Network Service Discovery", true);

        // Lateral Movement
        addTechnique("TA0008", "Lateral Movement", "T1021", "Remote Services", true);
        addTechnique("TA0008", "Lateral Movement", "T1570", "Lateral Tool Transfer", true);

        // Collection
        addTechnique("TA0009", "Collection", "T1005", "Data from Local System", true);
        addTechnique("TA0009", "Collection", "T1039", "Data from Network Shared Drive", true);
        addTechnique("TA0009", "Collection", "T1114", "Email Collection", true);

        // Exfiltration
        addTechnique("TA0010", "Exfiltration", "T1048", "Exfiltration Over Alternative Protocol", true);
        addTechnique("TA0010", "Exfiltration", "T1041", "Exfiltration Over C2 Channel", true);
        addTechnique("TA0010", "Exfiltration", "T1567", "Exfiltration Over Web Service", true);

        // Impact
        addTechnique("TA0040", "Impact", "T1485", "Data Destruction", true);
        addTechnique("TA0040", "Impact", "T1486", "Data Encrypted for Impact", true);
        addTechnique("TA0040", "Impact", "T1565", "Data Manipulation", true);
    }

    private void addTechnique(String tacticId, String tacticName, String techId, String techName, boolean covered) {
        attackMatrix.computeIfAbsent(tacticId + ": " + tacticName, k -> new ArrayList<>())
                    .add(new Technique(techId, techName, covered));
        coverageMap.put(techId, covered);
    }

    /** Map an event category to potential MITRE techniques */
    public List<String> mapEventToTechniques(EventCategory category) {
        if (category == null) return List.of();
        String group = category.getCategory();
        return switch (group) {
            case "Authentication" -> List.of("T1078", "T1110");
            case "Authorization" -> List.of("T1548", "T1098");
            case "Data" -> List.of("T1005", "T1039", "T1565", "T1485");
            case "Network" -> List.of("T1021", "T1046", "T1570");
            case "System" -> List.of("T1059", "T1053");
            case "Audit" -> List.of();
            case "Policy" -> List.of("T1048", "T1567");
            case "Security" -> List.of("T1204", "T1059", "T1078");
            default -> List.of();
        };
    }

    /** Get coverage statistics */
    public CoverageReport getCoverageReport() {
        CoverageReport report = new CoverageReport();
        int total = 0, covered = 0;
        for (Map.Entry<String, List<Technique>> entry : attackMatrix.entrySet()) {
            TacticCoverage tc = new TacticCoverage();
            tc.tacticName = entry.getKey();
            tc.totalTechniques = entry.getValue().size();
            tc.coveredTechniques = (int) entry.getValue().stream().filter(t -> t.covered).count();
            tc.uncoveredTechniques = entry.getValue().stream().filter(t -> !t.covered).map(t -> t.name).toList();
            report.tactics.add(tc);
            total += tc.totalTechniques;
            covered += tc.coveredTechniques;
        }
        report.totalTechniques = total;
        report.coveredTechniques = covered;
        report.coveragePercentage = total > 0 ? (covered * 100.0 / total) : 0;
        return report;
    }

    /** Check if a specific technique is covered */
    public boolean isTechniqueCovered(String techniqueId) {
        return coverageMap.getOrDefault(techniqueId, false);
    }

    /** Get all uncovered techniques (gap analysis) */
    public List<Technique> getUncoveredTechniques() {
        List<Technique> uncovered = new ArrayList<>();
        for (List<Technique> techs : attackMatrix.values()) {
            for (Technique t : techs) { if (!t.covered) uncovered.add(t); }
        }
        return uncovered;
    }

    // Inner classes
    public static class Technique {
        public String id, name; public boolean covered;
        public Technique(String id, String name, boolean covered) { this.id = id; this.name = name; this.covered = covered; }
    }

    public static class CoverageReport {
        public int totalTechniques, coveredTechniques;
        public double coveragePercentage;
        public List<TacticCoverage> tactics = new ArrayList<>();
    }

    public static class TacticCoverage {
        public String tacticName;
        public int totalTechniques, coveredTechniques;
        public List<String> uncoveredTechniques = new ArrayList<>();
    }
}
