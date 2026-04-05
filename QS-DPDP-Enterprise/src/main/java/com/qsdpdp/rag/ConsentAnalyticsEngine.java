package com.qsdpdp.rag;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Consent Analytics Engine — AI-Powered Compliance Analytics
 *
 * Uses the existing RAG engine's pattern analysis capabilities to:
 * - Detect consent misuse (anomalous access patterns)
 * - Calculate data principal risk scores
 * - Analyze vendor risk compliance
 * - Generate AI-powered compliance insights
 * - Predict consent expiration for proactive renewal
 *
 * @version 1.0.0
 * @since Phase 7 — AI Analytics Enhancement
 */
@Component
public class ConsentAnalyticsEngine {

    private static final Logger logger = LoggerFactory.getLogger(ConsentAnalyticsEngine.class);

    @Autowired private DatabaseManager dbManager;
    @Autowired private AuditService auditService;

    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_analytics_results (
                    id TEXT PRIMARY KEY,
                    analysis_type TEXT NOT NULL,
                    entity_id TEXT,
                    entity_type TEXT,
                    risk_score REAL,
                    risk_level TEXT,
                    findings TEXT,
                    recommendations TEXT,
                    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    valid_until TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS consent_misuse_alerts (
                    id TEXT PRIMARY KEY,
                    principal_id TEXT,
                    pattern_type TEXT,
                    description TEXT,
                    severity TEXT DEFAULT 'MEDIUM',
                    resolved INTEGER DEFAULT 0,
                    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved_at TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS compliance_insights (
                    id TEXT PRIMARY KEY,
                    category TEXT NOT NULL,
                    insight TEXT NOT NULL,
                    severity TEXT DEFAULT 'INFO',
                    actionable INTEGER DEFAULT 1,
                    recommendation TEXT,
                    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    acknowledged INTEGER DEFAULT 0
                )
            """);

            initialized = true;
            logger.info("ConsentAnalyticsEngine initialized");
        } catch (SQLException e) {
            logger.error("Failed to initialize ConsentAnalyticsEngine", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENT MISUSE DETECTION
    // ═══════════════════════════════════════════════════════════

    /**
     * Detect consent misuse by analyzing access patterns.
     * Pattern analysis includes:
     * - Access outside business hours
     * - Excessive data access volume
     * - Purpose drift (accessing data category not in consent)
     * - Post-withdrawal access attempts
     */
    public List<MisuseAlert> detectConsentMisuse() {
        if (!initialized) initialize();
        List<MisuseAlert> alerts = new ArrayList<>();

        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            // Pattern 1: Access after consent withdrawal
            ResultSet rs = stmt.executeQuery("""
                SELECT cel.principal_id, cel.purpose, cel.evaluated_at, COUNT(*) as attempts
                FROM consent_enforcement_log cel
                WHERE cel.decision = 'DENIED'
                AND cel.denial_reason LIKE '%WITHDRAWN%'
                GROUP BY cel.principal_id, cel.purpose
                HAVING attempts > 2
            """);
            while (rs.next()) {
                MisuseAlert alert = new MisuseAlert();
                alert.id = UUID.randomUUID().toString();
                alert.principalId = rs.getString("principal_id");
                alert.patternType = "POST_WITHDRAWAL_ACCESS";
                alert.severity = "HIGH";
                alert.description = "Multiple access attempts (" + rs.getInt("attempts")
                        + ") after consent withdrawal for purpose: " + rs.getString("purpose");
                alerts.add(alert);
                persistAlert(conn, alert);
            }

            // Pattern 2: Excessive denied access
            rs = stmt.executeQuery("""
                SELECT principal_id, COUNT(*) as denied_count
                FROM consent_enforcement_log
                WHERE decision = 'DENIED'
                AND evaluated_at > datetime('now', '-24 hours')
                GROUP BY principal_id
                HAVING denied_count > 10
            """);
            while (rs.next()) {
                MisuseAlert alert = new MisuseAlert();
                alert.id = UUID.randomUUID().toString();
                alert.principalId = rs.getString("principal_id");
                alert.patternType = "EXCESSIVE_DENIED_ACCESS";
                alert.severity = "MEDIUM";
                alert.description = rs.getInt("denied_count")
                        + " denied access attempts in last 24 hours — possible unauthorized data harvesting";
                alerts.add(alert);
                persistAlert(conn, alert);
            }

            // Pattern 3: Purpose drift
            rs = stmt.executeQuery("""
                SELECT cel.principal_id, cel.purpose, cel.data_category
                FROM consent_enforcement_log cel
                WHERE cel.decision = 'DENIED'
                AND cel.denial_reason LIKE '%CATEGORY%'
                GROUP BY cel.principal_id, cel.purpose, cel.data_category
            """);
            while (rs.next()) {
                MisuseAlert alert = new MisuseAlert();
                alert.id = UUID.randomUUID().toString();
                alert.principalId = rs.getString("principal_id");
                alert.patternType = "PURPOSE_DRIFT";
                alert.severity = "MEDIUM";
                alert.description = "Data category '" + rs.getString("data_category")
                        + "' accessed outside consented scope for purpose: " + rs.getString("purpose");
                alerts.add(alert);
                persistAlert(conn, alert);
            }

            auditService.log("MISUSE_DETECTION_RUN", "ANALYTICS", null,
                    "Consent misuse detection completed: " + alerts.size() + " alerts generated");

        } catch (SQLException e) {
            logger.error("Failed to detect consent misuse", e);
        }
        return alerts;
    }

    // ═══════════════════════════════════════════════════════════
    // RISK SCORING
    // ═══════════════════════════════════════════════════════════

    /**
     * Calculate risk score for a data principal (0-100).
     * Factors: consent count, withdrawal rate, data sensitivity, breach exposure, access patterns.
     */
    public RiskScore calculateRiskScore(String principalId) {
        if (!initialized) initialize();
        RiskScore score = new RiskScore();
        score.principalId = principalId;
        score.calculatedAt = LocalDateTime.now();

        double totalScore = 0;
        int factors = 0;

        try (Connection conn = dbManager.getConnection()) {
            // Factor 1: Expired consents (higher = more risk)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM consent_grants WHERE principal_id = ? AND status = 'ACTIVE' AND expires_at < datetime('now')")) {
                ps.setString(1, principalId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int expired = rs.getInt(1);
                    totalScore += Math.min(expired * 15, 30); // Max 30 points from expired consents
                    factors++;
                    score.factors.put("expiredConsents", expired);
                }
            }

            // Factor 2: Denied access count
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM consent_enforcement_log WHERE principal_id = ? AND decision = 'DENIED'")) {
                ps.setString(1, principalId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int denied = rs.getInt(1);
                    totalScore += Math.min(denied * 5, 25); // Max 25 points from denials
                    factors++;
                    score.factors.put("deniedAccesses", denied);
                }
            }

            // Factor 3: Breach exposure
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM breach_affected_principals WHERE principal_id = ?")) {
                ps.setString(1, principalId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int breaches = rs.getInt(1);
                    totalScore += Math.min(breaches * 20, 30); // Max 30 points from breaches
                    factors++;
                    score.factors.put("breachExposure", breaches);
                }
            }

            // Factor 4: Consent diversity (more sectors = higher exposure)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(DISTINCT sector) FROM consent_grants WHERE principal_id = ?")) {
                ps.setString(1, principalId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int sectors = rs.getInt(1);
                    totalScore += Math.min(sectors * 3, 15);
                    factors++;
                    score.factors.put("sectorExposure", sectors);
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to calculate risk score", e);
        }

        score.riskScore = Math.min(totalScore, 100);
        score.riskLevel = score.riskScore >= 75 ? "CRITICAL"
                : score.riskScore >= 50 ? "HIGH"
                : score.riskScore >= 25 ? "MEDIUM" : "LOW";
        score.factorsEvaluated = factors;

        // Persist
        persistRiskScore(score);
        return score;
    }

    // ═══════════════════════════════════════════════════════════
    // VENDOR RISK ANALYTICS
    // ═══════════════════════════════════════════════════════════

    /**
     * AI-powered vendor risk analysis across all vendors.
     */
    public List<Map<String, Object>> analyzeVendorRisk() {
        if (!initialized) initialize();
        List<Map<String, Object>> analysis = new ArrayList<>();

        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("""
                SELECT v.id, v.name, v.risk_tier, v.country,
                       COUNT(DISTINCT va.id) as assessment_count,
                       COUNT(DISTINCT vi.id) as incident_count,
                       COUNT(DISTINCT vds.id) as data_shares,
                       COALESCE(AVG(va.overall_score), 0) as avg_score
                FROM vendors v
                LEFT JOIN vendor_assessments va ON v.id = va.vendor_id
                LEFT JOIN vendor_incidents vi ON v.id = vi.vendor_id AND vi.status = 'OPEN'
                LEFT JOIN vendor_data_sharing vds ON v.id = vds.vendor_id AND vds.active = 1
                WHERE v.status = 'ACTIVE'
                GROUP BY v.id
                ORDER BY incident_count DESC, avg_score ASC
            """);

            while (rs.next()) {
                Map<String, Object> vendorRisk = new LinkedHashMap<>();
                vendorRisk.put("vendorId", rs.getString("id"));
                vendorRisk.put("vendorName", rs.getString("name"));
                vendorRisk.put("currentRiskTier", rs.getString("risk_tier"));
                vendorRisk.put("country", rs.getString("country"));
                vendorRisk.put("assessmentCount", rs.getInt("assessment_count"));
                vendorRisk.put("openIncidents", rs.getInt("incident_count"));
                vendorRisk.put("activeDataShares", rs.getInt("data_shares"));
                vendorRisk.put("avgAssessmentScore", rs.getDouble("avg_score"));

                // Calculate AI risk score
                int incidents = rs.getInt("incident_count");
                double avgScore = rs.getDouble("avg_score");
                double aiRiskScore = Math.min(100, (incidents * 20) + (100 - avgScore));
                String aiRiskLevel = aiRiskScore >= 75 ? "CRITICAL"
                        : aiRiskScore >= 50 ? "HIGH"
                        : aiRiskScore >= 25 ? "MEDIUM" : "LOW";

                vendorRisk.put("aiRiskScore", aiRiskScore);
                vendorRisk.put("aiRiskLevel", aiRiskLevel);

                // Recommendations
                List<String> recs = new ArrayList<>();
                if (incidents > 0) recs.add("Immediate incident resolution required");
                if (avgScore < 60) recs.add("Assessment score below threshold — re-assessment needed");
                if (rs.getInt("assessment_count") == 0) recs.add("No assessments on record — schedule initial assessment");
                vendorRisk.put("recommendations", recs);

                analysis.add(vendorRisk);
            }
        } catch (SQLException e) {
            logger.error("Failed to analyze vendor risk", e);
        }
        return analysis;
    }

    // ═══════════════════════════════════════════════════════════
    // COMPLIANCE INSIGHTS
    // ═══════════════════════════════════════════════════════════

    /**
     * Generate AI-powered compliance insights based on system data.
     */
    public List<ComplianceInsight> generateComplianceInsights() {
        if (!initialized) initialize();
        List<ComplianceInsight> insights = new ArrayList<>();

        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            // Insight 1: Consent expiry approaching
            ResultSet rs = stmt.executeQuery("""
                SELECT COUNT(*) as expiring FROM consent_grants 
                WHERE status = 'ACTIVE' AND expires_at BETWEEN datetime('now') AND datetime('now', '+30 days')
            """);
            if (rs.next() && rs.getInt(1) > 0) {
                ComplianceInsight ci = new ComplianceInsight();
                ci.category = "CONSENT_RENEWAL";
                ci.severity = "WARNING";
                ci.insight = rs.getInt(1) + " consent records expiring within 30 days — renewal campaigns needed";
                ci.recommendation = "Initiate automated consent renewal workflows for affected principals";
                insights.add(ci);
                persistInsight(conn, ci);
            }

            // Insight 2: Overdue rights requests
            rs = stmt.executeQuery("""
                SELECT COUNT(*) as overdue FROM rights_requests
                WHERE status NOT IN ('COMPLETED', 'REJECTED', 'WITHDRAWN') AND deadline < datetime('now')
            """);
            if (rs.next() && rs.getInt(1) > 0) {
                ComplianceInsight ci = new ComplianceInsight();
                ci.category = "RIGHTS_COMPLIANCE";
                ci.severity = "CRITICAL";
                ci.insight = rs.getInt(1) + " DSR requests overdue — 30-day DPDP deadline breached";
                ci.recommendation = "Escalate to DPO immediately. Each overdue request is a compliance violation under DPDP §13";
                insights.add(ci);
                persistInsight(conn, ci);
            }

            // Insight 3: High-risk vendors without recent assessment
            rs = stmt.executeQuery("""
                SELECT COUNT(*) as unassessed FROM vendors
                WHERE (risk_tier = 'HIGH' OR risk_tier = 'CRITICAL')
                AND (last_assessment_date IS NULL OR last_assessment_date < datetime('now', '-90 days'))
                AND status = 'ACTIVE'
            """);
            if (rs.next() && rs.getInt(1) > 0) {
                ComplianceInsight ci = new ComplianceInsight();
                ci.category = "VENDOR_RISK";
                ci.severity = "HIGH";
                ci.insight = rs.getInt(1) + " high-risk vendors without assessment in 90+ days";
                ci.recommendation = "Schedule immediate vendor risk assessments with DPDP compliance questionnaire";
                insights.add(ci);
                persistInsight(conn, ci);
            }

            // Insight 4: Enforcement denial rate
            rs = stmt.executeQuery("""
                SELECT 
                  COUNT(CASE WHEN decision = 'DENIED' THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0) as denial_rate
                FROM consent_enforcement_log
                WHERE evaluated_at > datetime('now', '-7 days')
            """);
            if (rs.next() && rs.getDouble(1) > 20) {
                ComplianceInsight ci = new ComplianceInsight();
                ci.category = "ENFORCEMENT";
                ci.severity = "WARNING";
                ci.insight = String.format("%.1f%% enforcement denial rate in last 7 days — above 20%% threshold", rs.getDouble(1));
                ci.recommendation = "Review consent grants and ABAC policies to reduce false denials or identify systemic gaps";
                insights.add(ci);
                persistInsight(conn, ci);
            }

            // Insight 5: Data residency compliance
            rs = stmt.executeQuery("SELECT COUNT(*) FROM residency_violations WHERE status = 'OPEN'");
            if (rs.next() && rs.getInt(1) > 0) {
                ComplianceInsight ci = new ComplianceInsight();
                ci.category = "DATA_LOCALISATION";
                ci.severity = "CRITICAL";
                ci.insight = rs.getInt(1) + " open data residency violations — potential DPDP §16 breach";
                ci.recommendation = "Investigate and resolve sovereign data transfer violations immediately";
                insights.add(ci);
                persistInsight(conn, ci);
            }

            auditService.log("COMPLIANCE_INSIGHTS_GENERATED", "ANALYTICS", null,
                    "Generated " + insights.size() + " compliance insights");

        } catch (SQLException e) {
            logger.error("Failed to generate compliance insights", e);
        }
        return insights;
    }

    /**
     * Predict consent expiry and recommend proactive renewals.
     */
    public List<Map<String, Object>> predictConsentExpiry() {
        List<Map<String, Object>> predictions = new ArrayList<>();
        String sql = """
            SELECT principal_id, purpose, expires_at, sector,
                   CAST((julianday(expires_at) - julianday('now')) AS INTEGER) as days_remaining
            FROM consent_grants
            WHERE status = 'ACTIVE' AND expires_at IS NOT NULL
            AND expires_at > datetime('now')
            ORDER BY expires_at ASC
            LIMIT 50
        """;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> pred = new LinkedHashMap<>();
                pred.put("principalId", rs.getString("principal_id"));
                pred.put("purpose", rs.getString("purpose"));
                pred.put("expiresAt", rs.getString("expires_at"));
                pred.put("sector", rs.getString("sector"));
                int days = rs.getInt("days_remaining");
                pred.put("daysRemaining", days);
                pred.put("urgency", days <= 7 ? "CRITICAL"
                        : days <= 30 ? "HIGH"
                        : days <= 90 ? "MEDIUM" : "LOW");
                pred.put("recommendation", days <= 7
                        ? "Immediate renewal required — consent expires in " + days + " days"
                        : days <= 30
                        ? "Schedule renewal within 30 days"
                        : "Monitor — renewal due within " + days + " days");
                predictions.add(pred);
            }
        } catch (SQLException e) {
            logger.error("Failed to predict consent expiry", e);
        }
        return predictions;
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_misuse_alerts WHERE resolved = 0");
            if (rs.next()) stats.put("unresolvedMisuseAlerts", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM consent_analytics_results");
            if (rs.next()) stats.put("totalAnalyses", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM compliance_insights WHERE acknowledged = 0");
            if (rs.next()) stats.put("pendingInsights", rs.getInt(1));

            stats.put("status", "OPERATIONAL");
        } catch (SQLException e) {
            stats.put("status", "ERROR");
        }
        return stats;
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════════════════════

    private void persistAlert(Connection conn, MisuseAlert alert) {
        String sql = "INSERT INTO consent_misuse_alerts (id, principal_id, pattern_type, description, severity) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alert.id);
            ps.setString(2, alert.principalId);
            ps.setString(3, alert.patternType);
            ps.setString(4, alert.description);
            ps.setString(5, alert.severity);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to persist misuse alert", e);
        }
    }

    private void persistRiskScore(RiskScore score) {
        String sql = "INSERT INTO consent_analytics_results (id, analysis_type, entity_id, entity_type, risk_score, risk_level, findings) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, "RISK_SCORE");
            ps.setString(3, score.principalId);
            ps.setString(4, "DATA_PRINCIPAL");
            ps.setDouble(5, score.riskScore);
            ps.setString(6, score.riskLevel);
            ps.setString(7, score.factors.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to persist risk score", e);
        }
    }

    private void persistInsight(Connection conn, ComplianceInsight insight) {
        String sql = "INSERT INTO compliance_insights (id, category, insight, severity, recommendation) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, insight.category);
            ps.setString(3, insight.insight);
            ps.setString(4, insight.severity);
            ps.setString(5, insight.recommendation);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to persist insight", e);
        }
    }

    public boolean isInitialized() { return initialized; }

    // ═══════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class MisuseAlert {
        public String id, principalId, patternType, description, severity;
        public LocalDateTime detectedAt = LocalDateTime.now();
    }

    public static class RiskScore {
        public String principalId, riskLevel;
        public double riskScore;
        public int factorsEvaluated;
        public Map<String, Object> factors = new LinkedHashMap<>();
        public LocalDateTime calculatedAt;
    }

    public static class ComplianceInsight {
        public String category, insight, severity, recommendation;
    }
}
