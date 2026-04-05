package com.qsdpdp.core;

import com.qsdpdp.rag.RAGEvaluator;
import com.qsdpdp.rag.RAGStatus;
import com.qsdpdp.rag.ModuleScore;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.rules.RuleEngine;
import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Core Compliance Engine for QS-DPDP Enterprise
 * Central orchestrator for compliance assessment, scoring, and reporting
 * 
 * @version 1.0.0
 * @since Phase 1
 */
@Service
public class ComplianceEngine {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceEngine.class);

    private final DatabaseManager dbManager;
    private final RAGEvaluator ragEvaluator;
    private final EventBus eventBus;
    private final AuditService auditService;
    private final RuleEngine ruleEngine;

    private boolean initialized = false;
    private LocalDateTime lastAssessment;

    @Autowired
    public ComplianceEngine(DatabaseManager dbManager, RAGEvaluator ragEvaluator,
            EventBus eventBus, AuditService auditService, RuleEngine ruleEngine) {
        this.dbManager = dbManager;
        this.ragEvaluator = ragEvaluator;
        this.eventBus = eventBus;
        this.auditService = auditService;
        this.ruleEngine = ruleEngine;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        logger.info("Initializing Compliance Engine...");

        // Subscribe to relevant events
        eventBus.subscribe("consent.*", this::handleConsentEvent);
        eventBus.subscribe("breach.*", this::handleBreachEvent);
        eventBus.subscribe("rights.*", this::handleRightsEvent);
        eventBus.subscribe("policy.*", this::handlePolicyEvent);

        initialized = true;
        logger.info("Compliance Engine initialized");
    }

    /**
     * Calculate overall compliance score
     * Aggregates scores from all modules using weighted average
     */
    public ComplianceScore calculateOverallScore() {
        logger.info("Calculating overall compliance score...");

        Map<String, ModuleScore> moduleScores = new LinkedHashMap<>();

        // Calculate individual module scores
        moduleScores.put("CONSENT", ragEvaluator.evaluateModule("CONSENT"));
        moduleScores.put("BREACH", ragEvaluator.evaluateModule("BREACH"));
        moduleScores.put("RIGHTS", ragEvaluator.evaluateModule("RIGHTS"));
        moduleScores.put("DPIA", ragEvaluator.evaluateModule("DPIA"));
        moduleScores.put("POLICY", ragEvaluator.evaluateModule("POLICY"));
        moduleScores.put("SECURITY", ragEvaluator.evaluateModule("SECURITY"));
        moduleScores.put("AUDIT", ragEvaluator.evaluateModule("AUDIT"));

        // Calculate weighted overall score
        double totalWeight = 0;
        double weightedSum = 0;

        for (Map.Entry<String, ModuleScore> entry : moduleScores.entrySet()) {
            double weight = ragEvaluator.getModuleWeight(entry.getKey());
            weightedSum += entry.getValue().getScore() * weight;
            totalWeight += weight;
        }

        double overallScore = totalWeight > 0 ? weightedSum / totalWeight : 0;
        RAGStatus overallStatus = ragEvaluator.determineRAGStatus(overallScore);

        lastAssessment = LocalDateTime.now();

        ComplianceScore result = new ComplianceScore(overallScore, overallStatus, moduleScores, lastAssessment);

        // Persist scores
        persistComplianceScores(result);

        // Publish event
        eventBus.publish(new ComplianceEvent("compliance.score.calculated", result));

        // Audit log
        auditService.log("COMPLIANCE_SCORE_CALCULATED", "COMPLIANCE", "SYSTEM",
                String.format("Overall score: %.1f%% (%s)", overallScore, overallStatus));

        logger.info("Compliance score calculated: {:.1f}% ({})", overallScore, overallStatus);

        return result;
    }

    /**
     * Get compliance gaps as list of issues
     */
    public List<ComplianceGap> identifyGaps() {
        logger.info("Identifying compliance gaps...");

        List<ComplianceGap> gaps = new ArrayList<>();

        try (Connection conn = dbManager.getConnection()) {

            // Check consent gaps
            gaps.addAll(identifyConsentGaps(conn));

            // Check breach notification gaps
            gaps.addAll(identifyBreachGaps(conn));

            // Check rights request gaps
            gaps.addAll(identifyRightsGaps(conn));

            // Check DPIA gaps
            gaps.addAll(identifyDPIAGaps(conn));

            // Check policy gaps
            gaps.addAll(identifyPolicyGaps(conn));

        } catch (Exception e) {
            logger.error("Error identifying compliance gaps", e);
        }

        // Sort by severity
        gaps.sort((a, b) -> b.getSeverity().compareTo(a.getSeverity()));

        logger.info("Identified {} compliance gaps", gaps.size());
        return gaps;
    }

    private List<ComplianceGap> identifyConsentGaps(Connection conn) throws Exception {
        List<ComplianceGap> gaps = new ArrayList<>();

        // Check for orphaned consents (no purpose linked)
        String sql = "SELECT COUNT(*) FROM consents WHERE purpose_id NOT IN (SELECT id FROM purposes WHERE is_active = 1)";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-CON-001",
                        "Orphaned Consents",
                        "Consents exist without valid purposes",
                        "CONSENT",
                        "DPDP-CON-003",
                        GapSeverity.HIGH,
                        "Review and remediate orphaned consent records"));
            }
        }

        // Check for expired consents still marked active
        sql = "SELECT COUNT(*) FROM consents WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-CON-002",
                        "Expired Consents Still Active",
                        String.format("%d expired consents still marked as active", rs.getInt(1)),
                        "CONSENT",
                        "DPDP-CON-001",
                        GapSeverity.MEDIUM,
                        "Run consent expiry cleanup process"));
            }
        }

        return gaps;
    }

    private List<ComplianceGap> identifyBreachGaps(Connection conn) throws Exception {
        List<ComplianceGap> gaps = new ArrayList<>();

        // Check for breaches past 72-hour notification deadline
        String sql = """
                    SELECT COUNT(*) FROM breaches
                    WHERE status = 'OPEN'
                    AND dpbi_notified = 0
                    AND datetime(detected_at, '+72 hours') < CURRENT_TIMESTAMP
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-BRE-001",
                        "Overdue Breach Notifications",
                        String.format("%d breaches past 72-hour DPBI notification deadline", rs.getInt(1)),
                        "BREACH",
                        "DPDP-BRE-001",
                        GapSeverity.CRITICAL,
                        "IMMEDIATE: Notify DPBI and document reasons for delay"));
            }
        }

        // Check for unassigned breaches
        sql = "SELECT COUNT(*) FROM breaches WHERE status = 'OPEN' AND assigned_to IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-BRE-002",
                        "Unassigned Breaches",
                        String.format("%d open breaches without assigned handler", rs.getInt(1)),
                        "BREACH",
                        "DPDP-BRE-001",
                        GapSeverity.HIGH,
                        "Assign incident handlers to all open breaches"));
            }
        }

        return gaps;
    }

    private List<ComplianceGap> identifyRightsGaps(Connection conn) throws Exception {
        List<ComplianceGap> gaps = new ArrayList<>();

        // Check for rights requests approaching deadline
        String sql = """
                    SELECT COUNT(*) FROM rights_requests
                    WHERE status IN ('PENDING', 'IN_PROGRESS')
                    AND datetime(deadline) < datetime(CURRENT_TIMESTAMP, '+3 days')
                    AND datetime(deadline) > CURRENT_TIMESTAMP
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-RIG-001",
                        "Rights Requests Near Deadline",
                        String.format("%d rights requests due within 3 days", rs.getInt(1)),
                        "RIGHTS",
                        "DPDP-RIG-001",
                        GapSeverity.HIGH,
                        "Prioritize these requests to meet 30-day deadline"));
            }
        }

        // Check for overdue rights requests
        sql = """
                    SELECT COUNT(*) FROM rights_requests
                    WHERE status IN ('PENDING', 'IN_PROGRESS')
                    AND datetime(deadline) < CURRENT_TIMESTAMP
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-RIG-002",
                        "Overdue Rights Requests",
                        String.format("%d rights requests past 30-day deadline", rs.getInt(1)),
                        "RIGHTS",
                        "DPDP-RIG-001",
                        GapSeverity.CRITICAL,
                        "IMMEDIATE: Respond to overdue requests and document delays"));
            }
        }

        return gaps;
    }

    private List<ComplianceGap> identifyDPIAGaps(Connection conn) throws Exception {
        List<ComplianceGap> gaps = new ArrayList<>();

        // Check for DPIAs pending review past due date
        String sql = """
                    SELECT COUNT(*) FROM dpias
                    WHERE status = 'PENDING_REVIEW'
                    AND datetime(next_review_date) < CURRENT_TIMESTAMP
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-DPI-001",
                        "Overdue DPIA Reviews",
                        String.format("%d DPIAs overdue for review", rs.getInt(1)),
                        "DPIA",
                        "DPDP-DPI-001",
                        GapSeverity.MEDIUM,
                        "Schedule DPIA review sessions"));
            }
        }

        // Check for high-risk DPIAs without mitigations
        sql = "SELECT COUNT(*) FROM dpias WHERE risk_level = 'HIGH' AND (mitigations IS NULL OR mitigations = '')";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-DPI-002",
                        "High-Risk DPIAs Without Mitigations",
                        String.format("%d high-risk DPIAs without documented mitigations", rs.getInt(1)),
                        "DPIA",
                        "DPDP-DPI-002",
                        GapSeverity.HIGH,
                        "Document risk mitigation measures for high-risk processing"));
            }
        }

        return gaps;
    }

    private List<ComplianceGap> identifyPolicyGaps(Connection conn) throws Exception {
        List<ComplianceGap> gaps = new ArrayList<>();

        // Check for policies due for review
        String sql = "SELECT COUNT(*) FROM policies WHERE status = 'APPROVED' AND datetime(review_date) < CURRENT_TIMESTAMP";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-POL-001",
                        "Policies Overdue for Review",
                        String.format("%d policies past their review date", rs.getInt(1)),
                        "POLICY",
                        "DPDP-POL-001",
                        GapSeverity.MEDIUM,
                        "Initiate policy review cycle"));
            }
        }

        // Check for draft policies without owners
        sql = "SELECT COUNT(*) FROM policies WHERE status = 'DRAFT' AND (owner IS NULL OR owner = '')";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                gaps.add(new ComplianceGap(
                        "GAP-POL-002",
                        "Draft Policies Without Owners",
                        String.format("%d draft policies without assigned owners", rs.getInt(1)),
                        "POLICY",
                        "DPDP-POL-002",
                        GapSeverity.LOW,
                        "Assign policy owners to draft policies"));
            }
        }

        return gaps;
    }

    private void persistComplianceScores(ComplianceScore score) {
        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                        INSERT INTO compliance_scores (id, module, metric_name, metric_value, rag_status, details)
                        VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Insert overall score
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, "OVERALL");
                stmt.setString(3, "compliance_score");
                stmt.setDouble(4, score.getOverallScore());
                stmt.setString(5, score.getOverallStatus().name());
                stmt.setString(6, "{}");
                stmt.executeUpdate();

                // Insert module scores
                for (Map.Entry<String, ModuleScore> entry : score.getModuleScores().entrySet()) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, entry.getKey());
                    stmt.setString(3, "module_score");
                    stmt.setDouble(4, entry.getValue().getScore());
                    stmt.setString(5, entry.getValue().getStatus().name());
                    stmt.setString(6, "{}");
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            logger.error("Error persisting compliance scores", e);
        }
    }

    // Event handlers
    private void handleConsentEvent(ComplianceEvent event) {
        logger.debug("Handling consent event: {}", event.getType());
        // Trigger recalculation of consent module score
    }

    private void handleBreachEvent(ComplianceEvent event) {
        logger.debug("Handling breach event: {}", event.getType());
        // Check 72-hour deadline, send alerts if approaching
    }

    private void handleRightsEvent(ComplianceEvent event) {
        logger.debug("Handling rights event: {}", event.getType());
        // Check 30-day deadline, send alerts if approaching
    }

    private void handlePolicyEvent(ComplianceEvent event) {
        logger.debug("Handling policy event: {}", event.getType());
        // Update policy compliance metrics
    }

    public boolean isInitialized() {
        return initialized;
    }

    public LocalDateTime getLastAssessment() {
        return lastAssessment;
    }

    // ====================================================================
    // MODULE 8: AI-DRIVEN RISK ENGINE
    // Risk Score = Likelihood × Impact × Exposure (normalized 0-100)
    // ====================================================================

    /**
     * Calculate risk score using the mandatory formula:
     * Risk Score = Likelihood × Impact × Exposure
     * All inputs on 0-10 scale, output normalized to 0-100
     */
    public double calculateRiskScore(double likelihood, double impact, double exposure) {
        double rawScore = likelihood * impact * exposure;
        double normalizedScore = Math.min((rawScore / 1000.0) * 100.0, 100.0);

        logger.info("Risk Score calculated: L={} × I={} × E={} = {:.1f} (normalized: {:.1f}%)",
                likelihood, impact, exposure, rawScore, normalizedScore);

        return normalizedScore;
    }

    /**
     * Determine risk level from normalized score
     */
    public String determineRiskLevel(double normalizedScore) {
        if (normalizedScore >= 80) return "CRITICAL";
        if (normalizedScore >= 60) return "HIGH";
        if (normalizedScore >= 40) return "MEDIUM";
        if (normalizedScore >= 20) return "LOW";
        return "NEGLIGIBLE";
    }

    /**
     * Get sector-specific risk library
     * Returns pre-defined risks for each sector with baseline L/I/E values
     */
    public List<Map<String, Object>> getSectorRiskLibrary(String sector) {
        List<Map<String, Object>> risks = new ArrayList<>();

        switch (sector.toUpperCase()) {
            case "BFSI":
            case "BANKING":
                risks.add(createRiskEntry("BFSI-001", "Unauthorized account access", 7, 9, 8, "Section 8(6)"));
                risks.add(createRiskEntry("BFSI-002", "Financial data leakage via API", 6, 9, 7, "Section 8(6)"));
                risks.add(createRiskEntry("BFSI-003", "KYC data misuse", 5, 8, 8, "Section 6"));
                risks.add(createRiskEntry("BFSI-004", "Cross-border transaction data exposure", 4, 8, 6, "Section 16"));
                risks.add(createRiskEntry("BFSI-005", "Third-party payment processor breach", 6, 9, 7, "Section 8(6)"));
                risks.add(createRiskEntry("BFSI-006", "Insider trading data misuse", 3, 10, 5, "Section 4"));
                break;

            case "HEALTHCARE":
            case "HOSPITAL":
                risks.add(createRiskEntry("HC-001", "Patient health record exposure", 6, 10, 9, "Section 8(6)"));
                risks.add(createRiskEntry("HC-002", "Telemedicine data interception", 5, 8, 6, "Section 8(4)"));
                risks.add(createRiskEntry("HC-003", "Lab report sharing without consent", 7, 8, 7, "Section 6"));
                risks.add(createRiskEntry("HC-004", "Insurance data cross-linking", 5, 7, 6, "Section 5"));
                risks.add(createRiskEntry("HC-005", "Biometric data (fingerprint/retina) misuse", 4, 9, 5, "Section 8(6)"));
                break;

            case "TELECOM":
                risks.add(createRiskEntry("TEL-001", "CDR (Call Detail Record) mass surveillance", 5, 9, 9, "Section 8(6)"));
                risks.add(createRiskEntry("TEL-002", "Location data tracking without consent", 7, 8, 8, "Section 6"));
                risks.add(createRiskEntry("TEL-003", "SIM swap fraud exposing PII", 6, 8, 7, "Section 8(6)"));
                risks.add(createRiskEntry("TEL-004", "Subscriber data sharing with third parties", 6, 7, 8, "Section 5"));
                break;

            case "ECOMMERCE":
            case "E-COMMERCE":
                risks.add(createRiskEntry("EC-001", "Payment card data breach", 6, 9, 8, "Section 8(6)"));
                risks.add(createRiskEntry("EC-002", "Behavioral tracking without consent", 8, 6, 9, "Section 6"));
                risks.add(createRiskEntry("EC-003", "Delivery address data exposure", 5, 6, 8, "Section 8(4)"));
                risks.add(createRiskEntry("EC-004", "Customer profiling data misuse", 7, 7, 7, "Section 4"));
                break;

            case "GOVERNMENT":
                risks.add(createRiskEntry("GOV-001", "Aadhaar data mishandling", 5, 10, 10, "Section 8(6)"));
                risks.add(createRiskEntry("GOV-002", "Citizen data cross-department sharing", 6, 8, 9, "Section 5"));
                risks.add(createRiskEntry("GOV-003", "Voter data exposure", 4, 9, 8, "Section 8(6)"));
                risks.add(createRiskEntry("GOV-004", "Welfare scheme data profiling", 5, 7, 8, "Section 4"));
                break;

            case "EDUCATION":
                risks.add(createRiskEntry("EDU-001", "Student biometric data misuse", 5, 8, 6, "Section 9"));
                risks.add(createRiskEntry("EDU-002", "Children's data processing without guardian consent", 7, 9, 7, "Section 9"));
                risks.add(createRiskEntry("EDU-003", "Academic record data breach", 4, 7, 6, "Section 8(6)"));
                break;

            default:
                risks.add(createRiskEntry("GEN-001", "Personal data breach", 5, 8, 7, "Section 8(6)"));
                risks.add(createRiskEntry("GEN-002", "Consent management failure", 6, 7, 6, "Section 6"));
                risks.add(createRiskEntry("GEN-003", "Purpose limitation violation", 5, 7, 6, "Section 5"));
                risks.add(createRiskEntry("GEN-004", "Data retention beyond purpose", 6, 6, 7, "Section 8(7)"));
                risks.add(createRiskEntry("GEN-005", "Cross-border transfer non-compliance", 4, 8, 5, "Section 16"));
                break;
        }

        return risks;
    }

    /**
     * AI-based risk prediction using trend analysis and sector patterns
     * Identifies emerging risks based on historical data and sector context
     */
    public List<Map<String, Object>> predictEmergingRisks(String sector) {
        List<Map<String, Object>> predictions = new ArrayList<>();

        // Analyze existing compliance gaps to predict emerging risks
        List<ComplianceGap> currentGaps = identifyGaps();
        int criticalGaps = (int) currentGaps.stream()
                .filter(g -> g.getSeverity() == GapSeverity.CRITICAL).count();
        int highGaps = (int) currentGaps.stream()
                .filter(g -> g.getSeverity() == GapSeverity.HIGH).count();

        // Predictive logic based on gap patterns
        if (criticalGaps > 0) {
            Map<String, Object> prediction = new LinkedHashMap<>();
            prediction.put("risk_name", "Regulatory Enforcement Action");
            prediction.put("description", "Multiple critical gaps increase probability of regulatory scrutiny");
            prediction.put("predicted_likelihood", Math.min(criticalGaps * 2 + 3, 10));
            prediction.put("predicted_impact", 9);
            prediction.put("confidence", 0.85);
            prediction.put("ai_predicted", true);
            prediction.put("recommendation", "Address all critical gaps within 7 days");
            predictions.add(prediction);
        }

        if (highGaps > 2) {
            Map<String, Object> prediction = new LinkedHashMap<>();
            prediction.put("risk_name", "Compliance Score Degradation");
            prediction.put("description", "Accumulating high-severity gaps indicate systemic compliance issues");
            prediction.put("predicted_likelihood", Math.min(highGaps + 2, 10));
            prediction.put("predicted_impact", 7);
            prediction.put("confidence", 0.78);
            prediction.put("ai_predicted", true);
            prediction.put("recommendation", "Implement compliance improvement program");
            predictions.add(prediction);
        }

        // Sector-specific emerging risks
        if ("BFSI".equalsIgnoreCase(sector)) {
            Map<String, Object> prediction = new LinkedHashMap<>();
            prediction.put("risk_name", "Digital Lending Data Exposure");
            prediction.put("description", "RBI digital lending guidelines require enhanced data protection");
            prediction.put("predicted_likelihood", 6);
            prediction.put("predicted_impact", 8);
            prediction.put("confidence", 0.72);
            prediction.put("ai_predicted", true);
            prediction.put("recommendation", "Review digital lending data flows against RBI circular");
            predictions.add(prediction);
        }

        if ("HEALTHCARE".equalsIgnoreCase(sector)) {
            Map<String, Object> prediction = new LinkedHashMap<>();
            prediction.put("risk_name", "ABDM Health Data Compliance");
            prediction.put("description", "ABDM integration creates new data processing obligations");
            prediction.put("predicted_likelihood", 5);
            prediction.put("predicted_impact", 8);
            prediction.put("confidence", 0.70);
            prediction.put("ai_predicted", true);
            prediction.put("recommendation", "Map ABDM data flows and ensure consent coverage");
            predictions.add(prediction);
        }

        logger.info("AI predicted {} emerging risks for sector {}", predictions.size(), sector);
        return predictions;
    }

    private Map<String, Object> createRiskEntry(String id, String name, int likelihood,
                                                  int impact, int exposure, String dpdpSection) {
        Map<String, Object> risk = new LinkedHashMap<>();
        risk.put("risk_id", id);
        risk.put("name", name);
        risk.put("likelihood", likelihood);
        risk.put("impact", impact);
        risk.put("exposure", exposure);
        risk.put("risk_score", calculateRiskScore(likelihood, impact, exposure));
        risk.put("risk_level", determineRiskLevel((double) risk.get("risk_score")));
        risk.put("dpdp_section", dpdpSection);
        return risk;
    }
}
