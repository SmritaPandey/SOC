package com.qsdpdp.compliance.rbi;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * RBI Advisory 3/2026 Compliance Engine — 12 Domain Coverage
 * 
 * Implements all 12 cybersecurity and data governance domains
 * mandated by RBI for regulated entities:
 * 
 * 1.  IT Governance          7.  Data Loss Prevention
 * 2.  Data Classification     8.  Incident Response
 * 3.  Access Control          9.  Audit & Logging
 * 4.  Network Security       10.  Business Continuity
 * 5.  Application Security   11.  Third-Party Risk
 * 6.  Vulnerability Mgmt     12.  Retention & Disposal
 * 
 * Each domain has controls, maturity levels, RACI matrix,
 * and compliance scoring (0-100).
 * 
 * @version 1.0.0
 * @since Phase 5 — RBI Advisory Engine
 */
@Service
public class RBIComplianceEngine {

    private static final Logger logger = LoggerFactory.getLogger(RBIComplianceEngine.class);

    @Autowired(required = false) private DatabaseManager dbManager;

    private boolean initialized = false;
    private final Map<String, RBIDomain> domains = new LinkedHashMap<>();

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing RBI Compliance Engine (12 domains)...");
        createTables();
        registerDomains();
        initialized = true;
        logger.info("RBI Compliance Engine initialized — {} domains, {} total controls",
                domains.size(), domains.values().stream().mapToInt(d -> d.controls.size()).sum());
    }

    private void createTables() {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rbi_compliance_scores (
                    id TEXT PRIMARY KEY,
                    domain_id TEXT NOT NULL,
                    control_id TEXT NOT NULL,
                    score INTEGER DEFAULT 0,
                    maturity_level TEXT DEFAULT 'INITIAL',
                    evidence TEXT,
                    assessed_by TEXT,
                    assessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS rbi_raci_matrix (
                    id TEXT PRIMARY KEY,
                    domain_id TEXT NOT NULL,
                    control_id TEXT NOT NULL,
                    responsible TEXT,
                    accountable TEXT,
                    consulted TEXT,
                    informed TEXT
                )
            """);
        } catch (SQLException e) {
            logger.error("Failed to create RBI compliance tables", e);
        }
    }

    private void registerDomains() {
        addDomain("GOV", "IT Governance", "Board-level oversight of IT strategy, risk, and compliance", List.of(
                new RBIControl("GOV-01", "Board Cyber Policy", "Board-approved cybersecurity policy", "CRITICAL"),
                new RBIControl("GOV-02", "CISO Appointment", "Dedicated Chief Information Security Officer", "CRITICAL"),
                new RBIControl("GOV-03", "IT Risk Committee", "Quarterly IT risk review by board committee", "HIGH"),
                new RBIControl("GOV-04", "Cyber Budget", "Dedicated budget for cybersecurity (>5% IT spend)", "HIGH"),
                new RBIControl("GOV-05", "IT Strategy", "3-year IT strategy aligned with business goals", "MEDIUM")));

        addDomain("DCLS", "Data Classification", "Classification of data assets by sensitivity", List.of(
                new RBIControl("DCLS-01", "Classification Policy", "Formal data classification policy (Public/Internal/Confidential/Restricted)", "CRITICAL"),
                new RBIControl("DCLS-02", "Data Inventory", "Complete inventory of all data assets and owners", "CRITICAL"),
                new RBIControl("DCLS-03", "Labeling Standards", "Automated data labeling per classification", "HIGH"),
                new RBIControl("DCLS-04", "Handling Procedures", "Handling procedures per classification level", "HIGH")));

        addDomain("ACL", "Access Control", "Identity and access management controls", List.of(
                new RBIControl("ACL-01", "MFA Enforcement", "Multi-factor authentication for all critical systems", "CRITICAL"),
                new RBIControl("ACL-02", "RBAC Implementation", "Role-based access control with least privilege", "CRITICAL"),
                new RBIControl("ACL-03", "PAM Solution", "Privileged Access Management for admin accounts", "HIGH"),
                new RBIControl("ACL-04", "Access Review", "Quarterly access review and recertification", "HIGH"),
                new RBIControl("ACL-05", "SSO Implementation", "Single Sign-On across enterprise applications", "MEDIUM")));

        addDomain("NSC", "Network Security", "Network protection and segmentation", List.of(
                new RBIControl("NSC-01", "Network Segmentation", "Zone-based network with DMZ, Trust, Restricted", "CRITICAL"),
                new RBIControl("NSC-02", "Firewall Rules", "Documented and reviewed firewall rules", "HIGH"),
                new RBIControl("NSC-03", "IDS/IPS", "Network intrusion detection and prevention", "HIGH"),
                new RBIControl("NSC-04", "VPN Standards", "Encrypted VPN for all remote access", "HIGH")));

        addDomain("ASC", "Application Security", "Secure SDLC and application protection", List.of(
                new RBIControl("ASC-01", "Secure SDLC", "Security integrated in development lifecycle", "CRITICAL"),
                new RBIControl("ASC-02", "Code Review", "Mandatory code review before production deployment", "HIGH"),
                new RBIControl("ASC-03", "DAST/SAST", "Automated security testing (static + dynamic)", "HIGH"),
                new RBIControl("ASC-04", "WAF", "Web Application Firewall for all internet-facing apps", "HIGH")));

        addDomain("VUL", "Vulnerability Management", "Proactive vulnerability identification and remediation", List.of(
                new RBIControl("VUL-01", "VA Schedule", "Monthly vulnerability assessments", "CRITICAL"),
                new RBIControl("VUL-02", "Patch Management", "Critical patches within 48 hours, others within 30 days", "CRITICAL"),
                new RBIControl("VUL-03", "Pen Testing", "Annual penetration testing by certified firm", "HIGH"),
                new RBIControl("VUL-04", "Bug Bounty", "Responsible disclosure / bug bounty program", "MEDIUM")));

        addDomain("DLP", "Data Loss Prevention", "Controls to prevent data exfiltration", List.of(
                new RBIControl("DLP-01", "DLP Policy", "Formal data loss prevention policy", "CRITICAL"),
                new RBIControl("DLP-02", "Endpoint DLP", "DLP agents on all endpoints", "HIGH"),
                new RBIControl("DLP-03", "Email Gateway", "Email DLP scanning for sensitive data", "HIGH"),
                new RBIControl("DLP-04", "USB Control", "USB/removable media access control", "HIGH")));

        addDomain("IR", "Incident Response", "Cyber incident detection, response, and recovery", List.of(
                new RBIControl("IR-01", "IR Plan", "Documented incident response plan", "CRITICAL"),
                new RBIControl("IR-02", "CERT Reporting", "CERT-In reporting within 6 hours", "CRITICAL"),
                new RBIControl("IR-03", "SOC 24x7", "24/7 Security Operations Center", "HIGH"),
                new RBIControl("IR-04", "Forensics", "Digital forensics capability", "HIGH"),
                new RBIControl("IR-05", "Tabletop Drills", "Quarterly incident response exercises", "MEDIUM")));

        addDomain("AUD", "Audit & Logging", "Comprehensive audit trails and monitoring", List.of(
                new RBIControl("AUD-01", "Centralized Logging", "SIEM with centralized log aggregation", "CRITICAL"),
                new RBIControl("AUD-02", "Log Retention", "Minimum 5-year log retention", "CRITICAL"),
                new RBIControl("AUD-03", "Tamper-proof Logs", "Immutable, tamper-evident audit logs", "HIGH"),
                new RBIControl("AUD-04", "Real-time Alerts", "Automated alerts for suspicious activity", "HIGH")));

        addDomain("BCP", "Business Continuity", "Resilience and disaster recovery planning", List.of(
                new RBIControl("BCP-01", "BCP Plan", "Documented Business Continuity Plan", "CRITICAL"),
                new RBIControl("BCP-02", "DR Site", "Active-active or active-passive DR site", "CRITICAL"),
                new RBIControl("BCP-03", "RTO/RPO", "RTO <4 hours, RPO <1 hour for critical systems", "HIGH"),
                new RBIControl("BCP-04", "DR Drills", "Semi-annual DR drills", "HIGH")));

        addDomain("TPR", "Third-Party Risk", "Vendor and third-party risk management", List.of(
                new RBIControl("TPR-01", "Vendor Assessment", "Annual security assessment of critical vendors", "CRITICAL"),
                new RBIControl("TPR-02", "SLA Enforcement", "Security SLAs with all IT vendors", "HIGH"),
                new RBIControl("TPR-03", "Cloud Security", "Cloud provider security assessment (CSA STAR)", "HIGH"),
                new RBIControl("TPR-04", "Data Processing Agreement", "DPA with all data processors", "HIGH")));

        addDomain("RET", "Retention & Disposal", "Data retention and secure disposal policies", List.of(
                new RBIControl("RET-01", "Retention Policy", "Data retention policy per regulation", "CRITICAL"),
                new RBIControl("RET-02", "Secure Deletion", "Cryptographic erasure for digital data", "HIGH"),
                new RBIControl("RET-03", "Physical Disposal", "Secure shredding for physical media", "HIGH"),
                new RBIControl("RET-04", "Disposal Audit", "Certificate of destruction for disposed data", "MEDIUM")));
    }

    private void addDomain(String id, String name, String description, List<RBIControl> controls) {
        domains.put(id, new RBIDomain(id, name, description, controls));
    }

    // ═══════════════════════════════════════════════════════════
    // SCORING
    // ═══════════════════════════════════════════════════════════

    /** Score a control */
    public Map<String, Object> scoreControl(String domainId, String controlId, int score,
            String maturityLevel, String evidence, String assessedBy) {
        String id = UUID.randomUUID().toString();
        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO rbi_compliance_scores (id, domain_id, control_id, score, maturity_level, evidence, assessed_by) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, id);
                ps.setString(2, domainId);
                ps.setString(3, controlId);
                ps.setInt(4, Math.min(100, Math.max(0, score)));
                ps.setString(5, maturityLevel);
                ps.setString(6, evidence);
                ps.setString(7, assessedBy);
                ps.executeUpdate();
            } catch (SQLException e) { logger.error("Failed to score", e); }
        }
        return Map.of("id", id, "domainId", domainId, "controlId", controlId, "score", score, "status", "SCORED");
    }

    /** Get overall compliance score */
    public Map<String, Object> getOverallScore() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> domainScores = new LinkedHashMap<>();
        int totalScore = 0, totalControls = 0;

        for (RBIDomain domain : domains.values()) {
            Map<String, Object> ds = new LinkedHashMap<>();
            ds.put("name", domain.name);
            ds.put("controlCount", domain.controls.size());
            ds.put("score", getAverageDomainScore(domain.id));
            domainScores.put(domain.id, ds);
            totalScore += (int) ds.get("score");
            totalControls++;
        }

        result.put("overallScore", totalControls > 0 ? totalScore / totalControls : 0);
        result.put("domainCount", domains.size());
        result.put("totalControls", domains.values().stream().mapToInt(d -> d.controls.size()).sum());
        result.put("domainScores", domainScores);
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    private int getAverageDomainScore(String domainId) {
        if (dbManager == null || !dbManager.isInitialized()) return 0;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT AVG(score) as avg_score FROM rbi_compliance_scores WHERE domain_id = ?")) {
            ps.setString(1, domainId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("avg_score");
            }
        } catch (SQLException e) { /* silent */ }
        return 0;
    }

    /** Get all domains with controls */
    public List<Map<String, Object>> getAllDomains() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (RBIDomain d : domains.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id);
            m.put("name", d.name);
            m.put("description", d.description);
            m.put("controlCount", d.controls.size());
            m.put("controls", d.controls.stream().map(c -> Map.of(
                    "id", c.id, "name", c.name, "description", c.description, "priority", c.priority
            )).toList());
            list.add(m);
        }
        return list;
    }

    /** Set RACI for a control */
    public Map<String, Object> setRACI(String domainId, String controlId,
            String responsible, String accountable, String consulted, String informed) {
        String id = UUID.randomUUID().toString();
        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR REPLACE INTO rbi_raci_matrix (id, domain_id, control_id, responsible, accountable, consulted, informed) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, id);
                ps.setString(2, domainId);
                ps.setString(3, controlId);
                ps.setString(4, responsible);
                ps.setString(5, accountable);
                ps.setString(6, consulted);
                ps.setString(7, informed);
                ps.executeUpdate();
            } catch (SQLException e) { logger.error("Failed to set RACI", e); }
        }
        return Map.of("success", true, "domainId", domainId, "controlId", controlId);
    }

    public boolean isInitialized() { return initialized; }

    // DTOs
    public static class RBIDomain {
        public String id, name, description;
        public List<RBIControl> controls;
        public RBIDomain(String id, String name, String desc, List<RBIControl> controls) {
            this.id = id; this.name = name; this.description = desc; this.controls = controls;
        }
    }

    public static class RBIControl {
        public String id, name, description, priority;
        public RBIControl(String id, String name, String desc, String priority) {
            this.id = id; this.name = name; this.description = desc; this.priority = priority;
        }
    }
}
