package com.qsdpdp.gap;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Gap Analysis Service - Self-Assessment and Gap Identification Engine
 * MCQ-based compliance assessment with real-time RAG analytics
 * 
 * @version 1.0.0
 * @since Module 3
 */
@Service
public class GapAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(GapAnalysisService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;

    private boolean initialized = false;

    @Autowired
    public GapAnalysisService(DatabaseManager dbManager, AuditService auditService, EventBus eventBus) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (initialized) return;

        logger.info("Initializing Gap Analysis Service...");
        createTables();

        initialized = true;
        logger.info("Gap Analysis Service initialized with {} questions available",
                QuestionBank.getTotalQuestionCount());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gap_assessments (
                    id TEXT PRIMARY KEY,
                    organization_id TEXT,
                    sector TEXT,
                    assessed_by TEXT,
                    status TEXT DEFAULT 'IN_PROGRESS',
                    total_questions INTEGER,
                    answered_questions INTEGER DEFAULT 0,
                    overall_score REAL,
                    max_score REAL,
                    compliance_percentage REAL,
                    rag_status TEXT,
                    gap_count INTEGER DEFAULT 0,
                    critical_gaps INTEGER DEFAULT 0,
                    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    completed_at TIMESTAMP,
                    category_scores TEXT,
                    category_rag TEXT
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gap_responses (
                    id TEXT PRIMARY KEY,
                    assessment_id TEXT NOT NULL,
                    question_id TEXT NOT NULL,
                    selected_option INTEGER,
                    score REAL,
                    max_score REAL,
                    is_gap INTEGER DEFAULT 0,
                    comment TEXT,
                    responded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (assessment_id) REFERENCES gap_assessments(id)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS gap_items (
                    id TEXT PRIMARY KEY,
                    assessment_id TEXT NOT NULL,
                    question_id TEXT NOT NULL,
                    category TEXT,
                    dpdp_clause TEXT,
                    severity TEXT,
                    impact TEXT,
                    remediation TEXT,
                    score_loss REAL,
                    status TEXT DEFAULT 'IDENTIFIED',
                    owner TEXT,
                    target_date TIMESTAMP,
                    remediated_at TIMESTAMP,
                    notes TEXT,
                    FOREIGN KEY (assessment_id) REFERENCES gap_assessments(id)
                )
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_gap_responses_assessment ON gap_responses(assessment_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_gap_items_assessment ON gap_items(assessment_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_gap_items_status ON gap_items(status)");

            logger.info("Gap Analysis tables created");

        } catch (SQLException e) {
            logger.error("Failed to create Gap Analysis tables", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ASSESSMENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public GapAnalysisResult startAssessment(String organizationId, String sector, String assessedBy) {
        GapAnalysisResult result = new GapAnalysisResult();
        result.setOrganizationId(organizationId);
        result.setSector(sector);
        result.setAssessedBy(assessedBy);

        persistAssessment(result);

        auditService.log("GAP_ASSESSMENT_STARTED", "GAP_ANALYSIS", assessedBy,
                "Started gap assessment for sector: " + sector);

        eventBus.publish(new ComplianceEvent("gap.assessment.started",
                Map.of("assessmentId", result.getId(), "sector", sector)));

        logger.info("Started gap assessment {} for sector: {}", result.getId(), sector);
        return result;
    }

    public List<AssessmentQuestion> getQuestionsForAssessment(String sector, QuestionCategory category) {
        List<AssessmentQuestion> questions = new ArrayList<>();

        if (category != null) {
            questions.addAll(QuestionBank.getByCategory(category));
        } else if (sector != null) {
            questions.addAll(QuestionBank.getBySector(sector));
        } else {
            questions.addAll(QuestionBank.getAllQuestions());
        }

        return questions;
    }

    public List<AssessmentQuestion> getMandatoryQuestions() {
        return QuestionBank.getMandatory();
    }

    public void submitResponse(GapAnalysisResult result, AssessmentQuestion question,
                               int selectedOption, String comment) {
        result.addResponse(question, selectedOption, comment);
        persistResponse(result.getId(), question, selectedOption, 
                       question.calculateScore(selectedOption),
                       question.getMaxScore() * question.getCategory().getWeight(),
                       selectedOption != question.getCorrectOptionIndex(),
                       comment);

        // Real-time RAG update after each response
        updateAssessmentProgress(result);
    }

    public GapAnalysisResult completeAssessment(GapAnalysisResult result) {
        result.calculateFinalScores();

        // Persist final results
        updateAssessmentCompletion(result);

        // Persist gaps
        for (GapAnalysisResult.ComplianceGap gap : result.getGaps()) {
            persistGap(result.getId(), gap);
        }

        auditService.log("GAP_ASSESSMENT_COMPLETED", "GAP_ANALYSIS", result.getAssessedBy(),
                String.format("Assessment completed. Score: %.1f%%, RAG: %s, Gaps: %d",
                        result.getCompliancePercentage(), result.getRagStatus(), result.getGapCount()));

        eventBus.publish(new ComplianceEvent("gap.assessment.completed", Map.of(
                "assessmentId", result.getId(),
                "score", result.getCompliancePercentage(),
                "ragStatus", result.getRagStatus(),
                "gapCount", result.getGapCount()
        )));

        logger.info("Completed assessment {} - Score: {:.1f}%, RAG: {}",
                result.getId(), result.getCompliancePercentage(), result.getRagStatus());

        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // REMEDIATION MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public void assignRemediationOwner(String assessmentId, String gapId, String owner, LocalDateTime targetDate) {
        String sql = "UPDATE gap_items SET owner = ?, target_date = ?, status = 'IN_PROGRESS' WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, owner);
            stmt.setString(2, targetDate.toString());
            stmt.setString(3, gapId);
            stmt.executeUpdate();

            auditService.log("GAP_REMEDIATION_ASSIGNED", "GAP_ANALYSIS", owner,
                    "Gap " + gapId + " assigned to " + owner);

        } catch (SQLException e) {
            logger.error("Failed to assign remediation owner", e);
        }
    }

    public void markGapRemediated(String gapId, String remediatedBy, String notes) {
        String sql = "UPDATE gap_items SET status = 'REMEDIATED', remediated_at = ?, notes = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, notes);
            stmt.setString(3, gapId);
            stmt.executeUpdate();

            auditService.log("GAP_REMEDIATED", "GAP_ANALYSIS", remediatedBy,
                    "Gap " + gapId + " marked as remediated");

        } catch (SQLException e) {
            logger.error("Failed to mark gap as remediated", e);
        }
    }

    public List<GapAnalysisResult.ComplianceGap> getOpenGaps(String assessmentId) {
        List<GapAnalysisResult.ComplianceGap> gaps = new ArrayList<>();
        String sql = "SELECT * FROM gap_items WHERE assessment_id = ? AND status != 'REMEDIATED' ORDER BY severity, score_loss DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, assessmentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                GapAnalysisResult.ComplianceGap gap = new GapAnalysisResult.ComplianceGap();
                gap.setQuestionId(rs.getString("question_id"));
                gap.setCategory(QuestionCategory.valueOf(rs.getString("category")));
                gap.setDpdpClause(rs.getString("dpdp_clause"));
                gap.setSeverity(rs.getString("severity"));
                gap.setImpact(rs.getString("impact"));
                gap.setRemediation(rs.getString("remediation"));
                gap.setScoreLoss(rs.getDouble("score_loss"));
                gap.setStatus(rs.getString("status"));
                gap.setRemediationOwner(rs.getString("owner"));
                gaps.add(gap);
            }
        } catch (SQLException e) {
            logger.error("Failed to get open gaps", e);
        }

        return gaps;
    }

    // ═══════════════════════════════════════════════════════════
    // REPORTS & ANALYTICS
    // ═══════════════════════════════════════════════════════════

    public GapHeatmap generateHeatmap(String assessmentId) {
        GapHeatmap heatmap = new GapHeatmap();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT category, COUNT(*) as count, AVG(score_loss) as avg_loss " +
                     "FROM gap_items WHERE assessment_id = ? GROUP BY category")) {
            stmt.setString(1, assessmentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");
                int count = rs.getInt("count");
                double avgLoss = rs.getDouble("avg_loss");
                heatmap.addCategory(QuestionCategory.valueOf(category), count, avgLoss);
            }
        } catch (SQLException e) {
            logger.error("Failed to generate heatmap", e);
        }

        return heatmap;
    }

    public RemediationPlan generateRemediationPlan(String assessmentId) {
        RemediationPlan plan = new RemediationPlan();
        plan.setAssessmentId(assessmentId);

        List<GapAnalysisResult.ComplianceGap> gaps = getOpenGaps(assessmentId);

        // Group by severity and timeline
        Map<String, List<GapAnalysisResult.ComplianceGap>> bySeverity = new LinkedHashMap<>();
        bySeverity.put("CRITICAL", new ArrayList<>());
        bySeverity.put("HIGH", new ArrayList<>());
        bySeverity.put("MEDIUM", new ArrayList<>());
        bySeverity.put("LOW", new ArrayList<>());

        for (GapAnalysisResult.ComplianceGap gap : gaps) {
            bySeverity.get(gap.getSeverity()).add(gap);
        }

        // Create phases
        int phase = 1;
        for (Map.Entry<String, List<GapAnalysisResult.ComplianceGap>> entry : bySeverity.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                RemediationPhase remPhase = new RemediationPhase();
                remPhase.setPhaseNumber(phase++);
                remPhase.setName("Phase " + remPhase.getPhaseNumber() + ": " + entry.getKey() + " Priority");
                remPhase.setGaps(entry.getValue());
                remPhase.setTimelineWeeks(getTimelineForSeverity(entry.getKey()));
                plan.addPhase(remPhase);
            }
        }

        return plan;
    }

    private int getTimelineForSeverity(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 2;   // 2 weeks
            case "HIGH" -> 4;       // 4 weeks
            case "MEDIUM" -> 8;     // 8 weeks
            case "LOW" -> 12;       // 12 weeks
            default -> 12;
        };
    }

    public GapStatistics getStatistics() {
        GapStatistics stats = new GapStatistics();

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM gap_assessments");
            if (rs.next()) stats.setTotalAssessments(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM gap_assessments WHERE status = 'COMPLETED'");
            if (rs.next()) stats.setCompletedAssessments(rs.getInt(1));

            rs = stmt.executeQuery("SELECT AVG(compliance_percentage) FROM gap_assessments WHERE status = 'COMPLETED'");
            if (rs.next()) stats.setAverageCompliance(rs.getDouble(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM gap_items WHERE status != 'REMEDIATED'");
            if (rs.next()) stats.setOpenGaps(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM gap_items WHERE severity = 'CRITICAL' AND status != 'REMEDIATED'");
            if (rs.next()) stats.setCriticalGaps(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM gap_items WHERE status = 'REMEDIATED'");
            if (rs.next()) stats.setRemediatedGaps(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get gap statistics", e);
        }

        return stats;
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    private void persistAssessment(GapAnalysisResult result) {
        String sql = """
            INSERT INTO gap_assessments (id, organization_id, sector, assessed_by, status, 
                total_questions, started_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, result.getId());
            stmt.setString(2, result.getOrganizationId());
            stmt.setString(3, result.getSector());
            stmt.setString(4, result.getAssessedBy());
            stmt.setString(5, result.getStatus());
            stmt.setInt(6, QuestionBank.getTotalQuestionCount());
            stmt.setString(7, result.getStartedAt().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist assessment", e);
        }
    }

    private void persistResponse(String assessmentId, AssessmentQuestion question,
                                 int selectedOption, double score, double maxScore,
                                 boolean isGap, String comment) {
        String sql = """
            INSERT INTO gap_responses (id, assessment_id, question_id, selected_option, 
                score, max_score, is_gap, comment, responded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, assessmentId);
            stmt.setString(3, question.getId());
            stmt.setInt(4, selectedOption);
            stmt.setDouble(5, score);
            stmt.setDouble(6, maxScore);
            stmt.setInt(7, isGap ? 1 : 0);
            stmt.setString(8, comment);
            stmt.setString(9, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist response", e);
        }
    }

    private void updateAssessmentProgress(GapAnalysisResult result) {
        String sql = "UPDATE gap_assessments SET answered_questions = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, result.getResponses().size());
            stmt.setString(2, result.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update assessment progress", e);
        }
    }

    private void updateAssessmentCompletion(GapAnalysisResult result) {
        String sql = """
            UPDATE gap_assessments SET status = 'COMPLETED', completed_at = ?,
                overall_score = ?, max_score = ?, compliance_percentage = ?,
                rag_status = ?, gap_count = ?, critical_gaps = ?
            WHERE id = ?
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, result.getCompletedAt().toString());
            stmt.setDouble(2, result.getOverallScore());
            stmt.setDouble(3, result.getMaxPossibleScore());
            stmt.setDouble(4, result.getCompliancePercentage());
            stmt.setString(5, result.getRagStatus());
            stmt.setInt(6, result.getGapCount());
            stmt.setLong(7, result.getCriticalGapCount());
            stmt.setString(8, result.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update assessment completion", e);
        }
    }

    private void persistGap(String assessmentId, GapAnalysisResult.ComplianceGap gap) {
        String sql = """
            INSERT INTO gap_items (id, assessment_id, question_id, category, dpdp_clause,
                severity, impact, remediation, score_loss, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, assessmentId);
            stmt.setString(3, gap.getQuestionId());
            stmt.setString(4, gap.getCategory().name());
            stmt.setString(5, gap.getDpdpClause());
            stmt.setString(6, gap.getSeverity());
            stmt.setString(7, gap.getImpact());
            stmt.setString(8, gap.getRemediation());
            stmt.setDouble(9, gap.getScoreLoss());
            stmt.setString(10, gap.getStatus());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist gap", e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class GapHeatmap {
        private Map<QuestionCategory, HeatmapCell> cells = new EnumMap<>(QuestionCategory.class);

        public void addCategory(QuestionCategory category, int count, double avgLoss) {
            cells.put(category, new HeatmapCell(count, avgLoss));
        }

        public Map<QuestionCategory, HeatmapCell> getCells() { return cells; }

        public static class HeatmapCell {
            private final int gapCount;
            private final double avgScoreLoss;
            private final String intensity;

            public HeatmapCell(int gapCount, double avgScoreLoss) {
                this.gapCount = gapCount;
                this.avgScoreLoss = avgScoreLoss;
                this.intensity = gapCount > 5 ? "HIGH" : gapCount > 2 ? "MEDIUM" : "LOW";
            }

            public int getGapCount() { return gapCount; }
            public double getAvgScoreLoss() { return avgScoreLoss; }
            public String getIntensity() { return intensity; }
        }
    }

    public static class RemediationPlan {
        private String assessmentId;
        private List<RemediationPhase> phases = new ArrayList<>();

        public void addPhase(RemediationPhase phase) { phases.add(phase); }

        public String getAssessmentId() { return assessmentId; }
        public void setAssessmentId(String id) { this.assessmentId = id; }
        public List<RemediationPhase> getPhases() { return phases; }
    }

    public static class RemediationPhase {
        private int phaseNumber;
        private String name;
        private int timelineWeeks;
        private List<GapAnalysisResult.ComplianceGap> gaps = new ArrayList<>();

        public int getPhaseNumber() { return phaseNumber; }
        public void setPhaseNumber(int num) { this.phaseNumber = num; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getTimelineWeeks() { return timelineWeeks; }
        public void setTimelineWeeks(int weeks) { this.timelineWeeks = weeks; }
        public List<GapAnalysisResult.ComplianceGap> getGaps() { return gaps; }
        public void setGaps(List<GapAnalysisResult.ComplianceGap> gaps) { this.gaps = gaps; }
    }

    public static class GapStatistics {
        private int totalAssessments;
        private int completedAssessments;
        private double averageCompliance;
        private int openGaps;
        private int criticalGaps;
        private int remediatedGaps;

        public int getTotalAssessments() { return totalAssessments; }
        public void setTotalAssessments(int v) { this.totalAssessments = v; }
        public int getCompletedAssessments() { return completedAssessments; }
        public void setCompletedAssessments(int v) { this.completedAssessments = v; }
        public double getAverageCompliance() { return averageCompliance; }
        public void setAverageCompliance(double v) { this.averageCompliance = v; }
        public int getOpenGaps() { return openGaps; }
        public void setOpenGaps(int v) { this.openGaps = v; }
        public int getCriticalGaps() { return criticalGaps; }
        public void setCriticalGaps(int v) { this.criticalGaps = v; }
        public int getRemediatedGaps() { return remediatedGaps; }
        public void setRemediatedGaps(int v) { this.remediatedGaps = v; }
    }
}
