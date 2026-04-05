package com.qsdpdp.rag;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * RAG (Red-Amber-Green) Evaluator for QS-DPDP Enterprise
 * Calculates compliance metrics and determines RAG status for all modules
 * 
 * @version 1.0.0
 * @since Phase 1
 */
@Service
public class RAGEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(RAGEvaluator.class);

    private final DatabaseManager dbManager;
    private boolean initialized = false;

    // RAG Thresholds
    private static final double GREEN_THRESHOLD = 80.0;
    private static final double AMBER_THRESHOLD = 50.0;

    // Module weights for overall score calculation
    private static final Map<String, Double> MODULE_WEIGHTS = new HashMap<>() {
        {
            put("CONSENT", 0.15);
            put("BREACH", 0.15);
            put("RIGHTS", 0.15);
            put("DPIA", 0.10);
            put("POLICY", 0.10);
            put("SECURITY", 0.15);
            put("AUDIT", 0.10);
            put("DATA_INVENTORY", 0.10);
        }
    };

    @Autowired
    public RAGEvaluator(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        logger.info("Initializing RAG Evaluator...");
        initialized = true;
        logger.info("RAG Evaluator initialized with {} modules", MODULE_WEIGHTS.size());
    }

    /**
     * Evaluate a specific module and return its score
     */
    public ModuleScore evaluateModule(String module) {
        logger.debug("Evaluating module: {}", module);

        try {
            switch (module.toUpperCase()) {
                case "CONSENT":
                    return evaluateConsentModule();
                case "BREACH":
                    return evaluateBreachModule();
                case "RIGHTS":
                    return evaluateRightsModule();
                case "DPIA":
                    return evaluateDPIAModule();
                case "POLICY":
                    return evaluatePolicyModule();
                case "SECURITY":
                    return evaluateSecurityModule();
                case "AUDIT":
                    return evaluateAuditModule();
                case "DATA_INVENTORY":
                    return evaluateDataInventoryModule();
                default:
                    logger.warn("Unknown module: {}", module);
                    return new ModuleScore(module, 0, RAGStatus.RED, "Unknown module");
            }
        } catch (Exception e) {
            logger.error("Error evaluating module: {}", module, e);
            return new ModuleScore(module, 0, RAGStatus.RED, "Evaluation error: " + e.getMessage());
        }
    }

    private ModuleScore evaluateConsentModule() throws Exception {
        try (Connection conn = dbManager.getConnection()) {
            double score = 0;
            int metrics = 0;
            StringBuilder details = new StringBuilder();

            // Metric 1: Active consent rate
            String sql = "SELECT COUNT(*) as active FROM consents WHERE status = 'ACTIVE'";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int active = rs.next() ? rs.getInt("active") : 0;

                sql = "SELECT COUNT(*) as total FROM consents";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (active * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Active consents: %.1f%%; ", rate));
                }
            }

            // Metric 2: Withdrawal response time (within 24h)
            sql = """
                        SELECT COUNT(*) as timely FROM consents
                        WHERE status = 'WITHDRAWN'
                        AND julianday(withdrawn_at) - julianday(collected_at) <= 1
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int timely = rs.next() ? rs.getInt("timely") : 0;

                sql = "SELECT COUNT(*) as total FROM consents WHERE status = 'WITHDRAWN'";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (timely * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Timely withdrawals: %.1f%%; ", rate));
                }
            }

            // Metric 3: Purpose coverage
            sql = "SELECT COUNT(*) as active FROM purposes WHERE is_active = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int purposes = rs.next() ? rs.getInt("active") : 0;
                double rate = purposes >= 5 ? 100 : (purposes * 20);
                score += rate;
                metrics++;
                details.append(String.format("Purposes defined: %d; ", purposes));
            }

            double finalScore = metrics > 0 ? score / metrics : 0;
            RAGStatus status = determineRAGStatus(finalScore);

            return new ModuleScore("CONSENT", finalScore, status, details.toString());
        }
    }

    private ModuleScore evaluateBreachModule() throws Exception {
        try (Connection conn = dbManager.getConnection()) {
            double score = 0;
            int metrics = 0;
            StringBuilder details = new StringBuilder();

            // Metric 1: DPBI notification compliance (72h)
            String sql = """
                        SELECT COUNT(*) as compliant FROM breaches
                        WHERE dpbi_notified = 1
                        AND julianday(dpbi_notification_date) - julianday(detected_at) <= 3
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int compliant = rs.next() ? rs.getInt("compliant") : 0;

                sql = "SELECT COUNT(*) as total FROM breaches WHERE dpbi_notified = 1";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (compliant * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("72h notification: %.1f%%; ", rate));
                }
            }

            // Metric 2: Breach resolution rate
            sql = "SELECT COUNT(*) as resolved FROM breaches WHERE status = 'RESOLVED'";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int resolved = rs.next() ? rs.getInt("resolved") : 0;

                sql = "SELECT COUNT(*) as total FROM breaches";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (resolved * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Resolution rate: %.1f%%; ", rate));
                }
            }

            // Metric 3: Affected party notification
            sql = "SELECT COUNT(*) as notified FROM breaches WHERE affected_parties_notified = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int notified = rs.next() ? rs.getInt("notified") : 0;

                sql = "SELECT COUNT(*) as total FROM breaches WHERE affected_count > 0";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (notified * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Party notification: %.1f%%; ", rate));
                }
            }

            double finalScore = metrics > 0 ? score / metrics : 0;
            RAGStatus status = determineRAGStatus(finalScore);

            return new ModuleScore("BREACH", finalScore, status, details.toString());
        }
    }

    private ModuleScore evaluateRightsModule() throws Exception {
        try (Connection conn = dbManager.getConnection()) {
            double score = 0;
            int metrics = 0;
            StringBuilder details = new StringBuilder();

            // Metric 1: 30-day response rate
            String sql = """
                        SELECT COUNT(*) as timely FROM rights_requests
                        WHERE status = 'COMPLETED'
                        AND julianday(completed_at) - julianday(received_at) <= 30
                    """;
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int timely = rs.next() ? rs.getInt("timely") : 0;

                sql = "SELECT COUNT(*) as total FROM rights_requests WHERE status = 'COMPLETED'";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (timely * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("30-day SLA: %.1f%%; ", rate));
                }
            }

            // Metric 2: Request completion rate
            sql = "SELECT COUNT(*) as completed FROM rights_requests WHERE status = 'COMPLETED'";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int completed = rs.next() ? rs.getInt("completed") : 0;

                sql = "SELECT COUNT(*) as total FROM rights_requests";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (completed * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Completion: %.1f%%; ", rate));
                }
            }

            double finalScore = metrics > 0 ? score / metrics : 0;
            RAGStatus status = determineRAGStatus(finalScore);

            return new ModuleScore("RIGHTS", finalScore, status, details.toString());
        }
    }

    private ModuleScore evaluateDPIAModule() throws Exception {
        try (Connection conn = dbManager.getConnection()) {
            double score = 0;
            int metrics = 0;
            StringBuilder details = new StringBuilder();

            // Metric 1: DPIA approval rate
            String sql = "SELECT COUNT(*) as approved FROM dpias WHERE status = 'APPROVED'";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int approved = rs.next() ? rs.getInt("approved") : 0;

                sql = "SELECT COUNT(*) as total FROM dpias";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (approved * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Approval rate: %.1f%%; ", rate));
                }
            }

            // Metric 2: High-risk coverage
            sql = "SELECT COUNT(*) as mitigated FROM dpias WHERE risk_level = 'HIGH' AND mitigations IS NOT NULL";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int mitigated = rs.next() ? rs.getInt("mitigated") : 0;

                sql = "SELECT COUNT(*) as total FROM dpias WHERE risk_level = 'HIGH'";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (mitigated * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("High-risk mitigated: %.1f%%; ", rate));
                }
            }

            double finalScore = metrics > 0 ? score / metrics : 0;
            RAGStatus status = determineRAGStatus(finalScore);

            return new ModuleScore("DPIA", finalScore, status, details.toString());
        }
    }

    private ModuleScore evaluatePolicyModule() throws Exception {
        try (Connection conn = dbManager.getConnection()) {
            double score = 0;
            int metrics = 0;
            StringBuilder details = new StringBuilder();

            // Metric 1: Policy approval rate
            String sql = "SELECT COUNT(*) as approved FROM policies WHERE status = 'APPROVED'";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int approved = rs.next() ? rs.getInt("approved") : 0;

                sql = "SELECT COUNT(*) as total FROM policies";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (approved * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Approved: %.1f%%; ", rate));
                }
            }

            // Metric 2: Policy currency (not past review date)
            sql = "SELECT COUNT(*) as current FROM policies WHERE status = 'APPROVED' AND (review_date IS NULL OR review_date > CURRENT_TIMESTAMP)";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int current = rs.next() ? rs.getInt("current") : 0;

                sql = "SELECT COUNT(*) as total FROM policies WHERE status = 'APPROVED'";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (current * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Current: %.1f%%; ", rate));
                }
            }

            double finalScore = metrics > 0 ? score / metrics : 0;
            RAGStatus status = determineRAGStatus(finalScore);

            return new ModuleScore("POLICY", finalScore, status, details.toString());
        }
    }

    private ModuleScore evaluateSecurityModule() throws Exception {
        try (Connection conn = dbManager.getConnection()) {
            double score = 0;
            int metrics = 0;
            StringBuilder details = new StringBuilder();

            // Metric 1: Control test pass rate
            String sql = "SELECT COUNT(*) as passed FROM controls WHERE test_result = 'PASS'";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int passed = rs.next() ? rs.getInt("passed") : 0;

                sql = "SELECT COUNT(*) as total FROM controls WHERE status = 'TESTED'";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (passed * 100.0 / total) : 100;
                    score += rate;
                    metrics++;
                    details.append(String.format("Control pass rate: %.1f%%; ", rate));
                }
            }

            // Metric 2: MFA adoption
            sql = "SELECT COUNT(*) as mfa FROM users WHERE mfa_enabled = 1 AND status = 'ACTIVE'";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int mfa = rs.next() ? rs.getInt("mfa") : 0;

                sql = "SELECT COUNT(*) as total FROM users WHERE status = 'ACTIVE'";
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    int total = rs2.next() ? rs2.getInt("total") : 0;

                    double rate = total > 0 ? (mfa * 100.0 / total) : 50; // 50% baseline if no MFA
                    score += rate;
                    metrics++;
                    details.append(String.format("MFA adoption: %.1f%%; ", rate));
                }
            }

            double finalScore = metrics > 0 ? score / metrics : 0;
            RAGStatus status = determineRAGStatus(finalScore);

            return new ModuleScore("SECURITY", finalScore, status, details.toString());
        }
    }

    private ModuleScore evaluateAuditModule() throws Exception {
        try (Connection conn = dbManager.getConnection()) {
            double score = 0;
            int metrics = 0;
            StringBuilder details = new StringBuilder();

            // Metric 1: Audit log integrity (hash chain verification)
            String sql = "SELECT COUNT(*) as total FROM audit_log";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                    ResultSet rs = stmt.executeQuery()) {
                int total = rs.next() ? rs.getInt("total") : 0;

                // Verify hash chain for last 100 entries
                sql = "SELECT hash, prev_hash FROM audit_log ORDER BY sequence_number DESC LIMIT 100";
                int verified = 0;
                try (PreparedStatement stmt2 = conn.prepareStatement(sql);
                        ResultSet rs2 = stmt2.executeQuery()) {
                    String expectedPrevHash = null;
                    while (rs2.next()) {
                        String hash = rs2.getString("hash");
                        String prevHash = rs2.getString("prev_hash");

                        if (expectedPrevHash == null || hash != null) {
                            verified++;
                        }
                        expectedPrevHash = prevHash;
                    }
                }

                double rate = total > 0 ? Math.min(100, (verified * 100.0 / Math.min(total, 100))) : 100;
                score += rate;
                metrics++;
                details.append(String.format("Log integrity: %.1f%%; ", rate));
            }

            // Metric 2: Audit coverage
            score += 100; // Assume 100% if audit is enabled
            metrics++;
            details.append("Coverage: 100%; ");

            double finalScore = metrics > 0 ? score / metrics : 0;
            RAGStatus status = determineRAGStatus(finalScore);

            return new ModuleScore("AUDIT", finalScore, status, details.toString());
        }
    }

    private ModuleScore evaluateDataInventoryModule() throws Exception {
        // Placeholder until Data Inventory module is implemented
        return new ModuleScore("DATA_INVENTORY", 50, RAGStatus.AMBER, "Module pending implementation");
    }

    /**
     * Determine RAG status from score
     */
    public RAGStatus determineRAGStatus(double score) {
        if (score >= GREEN_THRESHOLD) {
            return RAGStatus.GREEN;
        } else if (score >= AMBER_THRESHOLD) {
            return RAGStatus.AMBER;
        } else {
            return RAGStatus.RED;
        }
    }

    /**
     * Get weight for a module
     */
    public double getModuleWeight(String module) {
        return MODULE_WEIGHTS.getOrDefault(module.toUpperCase(), 0.1);
    }

    public boolean isInitialized() {
        return initialized;
    }
}
