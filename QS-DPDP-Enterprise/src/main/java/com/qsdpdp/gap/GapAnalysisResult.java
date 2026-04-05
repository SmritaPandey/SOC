package com.qsdpdp.gap;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Gap Analysis Result model
 * Captures assessment responses, scoring, and gap identification
 * 
 * @version 1.0.0
 * @since Module 3
 */
public class GapAnalysisResult {

    private String id;
    private String assessmentId;
    private String organizationId;
    private String sector;
    private String assessedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String status; // IN_PROGRESS, COMPLETED, CANCELLED

    // Responses
    private Map<String, Integer> responses; // questionId -> selectedOption
    private Map<String, String> comments; // questionId -> comment

    // Scoring
    private double overallScore;
    private double maxPossibleScore;
    private double compliancePercentage;
    private String ragStatus; // RED, AMBER, GREEN

    // Category-wise scores
    private Map<QuestionCategory, Double> categoryScores;
    private Map<QuestionCategory, Double> categoryMaxScores;
    private Map<QuestionCategory, String> categoryRAGStatus;

    // Gaps identified
    private List<ComplianceGap> gaps;

    public GapAnalysisResult() {
        this.id = UUID.randomUUID().toString();
        this.startedAt = LocalDateTime.now();
        this.status = "IN_PROGRESS";
        this.responses = new HashMap<>();
        this.comments = new HashMap<>();
        this.categoryScores = new EnumMap<>(QuestionCategory.class);
        this.categoryMaxScores = new EnumMap<>(QuestionCategory.class);
        this.categoryRAGStatus = new EnumMap<>(QuestionCategory.class);
        this.gaps = new ArrayList<>();
    }

    public void addResponse(AssessmentQuestion question, int selectedOption, String comment) {
        responses.put(question.getId(), selectedOption);
        if (comment != null && !comment.isBlank()) {
            comments.put(question.getId(), comment);
        }

        // Calculate score
        double score = question.calculateScore(selectedOption);
        double maxScore = question.getMaxScore() * question.getCategory().getWeight();

        categoryScores.merge(question.getCategory(), score, Double::sum);
        categoryMaxScores.merge(question.getCategory(), maxScore, Double::sum);

        // Identify gap if not optimal answer
        if (selectedOption != question.getCorrectOptionIndex()) {
            ComplianceGap gap = new ComplianceGap();
            gap.setQuestionId(question.getId());
            gap.setCategory(question.getCategory());
            gap.setDpdpClause(question.getDpdpClause());
            gap.setQuestionText(question.getQuestionText());
            gap.setSelectedAnswer(question.getOptions().get(selectedOption));
            gap.setExpectedAnswer(question.getOptions().get(question.getCorrectOptionIndex()));
            gap.setImpact(question.getImpactExplanation());
            gap.setRemediation(question.getRemediationGuidance());
            gap.setScoreLoss(maxScore - score);
            gap.setSeverity(determineSeverity(question, selectedOption));
            gaps.add(gap);
        }
    }

    private String determineSeverity(AssessmentQuestion question, int selected) {
        int distance = Math.abs(selected - question.getCorrectOptionIndex());
        if (question.isMandatory() && distance >= 3)
            return "CRITICAL";
        if (distance >= 4)
            return "CRITICAL";
        if (distance >= 3)
            return "HIGH";
        if (distance >= 2)
            return "MEDIUM";
        return "LOW";
    }

    public void calculateFinalScores() {
        this.completedAt = LocalDateTime.now();
        this.status = "COMPLETED";

        // Calculate overall scores
        overallScore = categoryScores.values().stream().mapToDouble(Double::doubleValue).sum();
        maxPossibleScore = categoryMaxScores.values().stream().mapToDouble(Double::doubleValue).sum();
        compliancePercentage = maxPossibleScore > 0 ? (overallScore / maxPossibleScore) * 100 : 0;

        // Determine RAG status
        ragStatus = calculateRAG(compliancePercentage);

        // Calculate category RAG
        for (QuestionCategory cat : categoryScores.keySet()) {
            double catScore = categoryScores.getOrDefault(cat, 0.0);
            double catMax = categoryMaxScores.getOrDefault(cat, 1.0);
            double catPercentage = (catScore / catMax) * 100;
            categoryRAGStatus.put(cat, calculateRAG(catPercentage));
        }

        // Sort gaps by severity
        gaps.sort((a, b) -> {
            int severityOrder = getSeverityOrder(a.getSeverity()) - getSeverityOrder(b.getSeverity());
            if (severityOrder != 0)
                return severityOrder;
            return Double.compare(b.getScoreLoss(), a.getScoreLoss());
        });
    }

    private String calculateRAG(double percentage) {
        if (percentage >= 80)
            return "GREEN";
        if (percentage >= 50)
            return "AMBER";
        return "RED";
    }

    private int getSeverityOrder(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }

    // Statistics
    public int getTotalQuestions() {
        return responses.size();
    }

    public int getGapCount() {
        return gaps.size();
    }

    public long getCriticalGapCount() {
        return gaps.stream().filter(g -> "CRITICAL".equals(g.getSeverity())).count();
    }

    public long getHighGapCount() {
        return gaps.stream().filter(g -> "HIGH".equals(g.getSeverity())).count();
    }

    public List<ComplianceGap> getGapsByCategory(QuestionCategory category) {
        return gaps.stream().filter(g -> g.getCategory() == category).toList();
    }

    public List<ComplianceGap> getCriticalGaps() {
        return gaps.stream().filter(g -> "CRITICAL".equals(g.getSeverity())).toList();
    }

    public List<QuestionCategory> getWeakestCategories(int count) {
        return categoryScores.entrySet().stream()
                .sorted((a, b) -> {
                    double aPercent = a.getValue() / categoryMaxScores.getOrDefault(a.getKey(), 1.0);
                    double bPercent = b.getValue() / categoryMaxScores.getOrDefault(b.getKey(), 1.0);
                    return Double.compare(aPercent, bPercent);
                })
                .limit(count)
                .map(Map.Entry::getKey)
                .toList();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(String id) {
        this.assessmentId = id;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String id) {
        this.organizationId = id;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getAssessedBy() {
        return assessedBy;
    }

    public void setAssessedBy(String assessedBy) {
        this.assessedBy = assessedBy;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Integer> getResponses() {
        return responses;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public double getMaxPossibleScore() {
        return maxPossibleScore;
    }

    public double getCompliancePercentage() {
        return compliancePercentage;
    }

    public String getRagStatus() {
        return ragStatus;
    }

    public Map<QuestionCategory, Double> getCategoryScores() {
        return categoryScores;
    }

    public Map<QuestionCategory, String> getCategoryRAGStatus() {
        return categoryRAGStatus;
    }

    public List<ComplianceGap> getGaps() {
        return gaps;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASS: ComplianceGap
    // ═══════════════════════════════════════════════════════════

    public static class ComplianceGap {
        private String questionId;
        private QuestionCategory category;
        private String dpdpClause;
        private String questionText;
        private String selectedAnswer;
        private String expectedAnswer;
        private String impact;
        private String remediation;
        private double scoreLoss;
        private String severity;
        private String status; // IDENTIFIED, IN_PROGRESS, REMEDIATED
        private String remediationOwner;
        private LocalDateTime targetDate;

        public ComplianceGap() {
            this.status = "IDENTIFIED";
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String id) {
            this.questionId = id;
        }

        public QuestionCategory getCategory() {
            return category;
        }

        public void setCategory(QuestionCategory cat) {
            this.category = cat;
        }

        public String getDpdpClause() {
            return dpdpClause;
        }

        public void setDpdpClause(String clause) {
            this.dpdpClause = clause;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String text) {
            this.questionText = text;
        }

        public String getSelectedAnswer() {
            return selectedAnswer;
        }

        public void setSelectedAnswer(String answer) {
            this.selectedAnswer = answer;
        }

        public String getExpectedAnswer() {
            return expectedAnswer;
        }

        public void setExpectedAnswer(String answer) {
            this.expectedAnswer = answer;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String impact) {
            this.impact = impact;
        }

        public String getRemediation() {
            return remediation;
        }

        public void setRemediation(String remediation) {
            this.remediation = remediation;
        }

        public double getScoreLoss() {
            return scoreLoss;
        }

        public void setScoreLoss(double loss) {
            this.scoreLoss = loss;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getRemediationOwner() {
            return remediationOwner;
        }

        public void setRemediationOwner(String owner) {
            this.remediationOwner = owner;
        }

        public LocalDateTime getTargetDate() {
            return targetDate;
        }

        public void setTargetDate(LocalDateTime date) {
            this.targetDate = date;
        }
    }
}
