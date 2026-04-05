package com.qsdpdp.gap;

import java.util.*;

/**
 * Assessment Question model for Gap Analysis
 * MCQ-based questions with hints, impact explanations, and scoring
 * 
 * @version 1.0.0
 * @since Module 3
 */
public class AssessmentQuestion {

    private String id;
    private QuestionCategory category;
    private String questionText;
    private List<String> options;
    private int correctOptionIndex;
    private String hint;
    private String impactExplanation;
    private String remediationGuidance;
    private int difficultyLevel; // 1-5
    private double maxScore;
    private String dpdpClause;
    private String isoControl;
    private boolean mandatory;
    private String sector; // null = all sectors

    public AssessmentQuestion() {
        this.id = UUID.randomUUID().toString();
        this.options = new ArrayList<>();
        this.difficultyLevel = 3;
        this.maxScore = 10.0;
    }

    public AssessmentQuestion(QuestionCategory category, String questionText) {
        this();
        this.category = category;
        this.questionText = questionText;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AssessmentQuestion q = new AssessmentQuestion();

        public Builder category(QuestionCategory category) {
            q.category = category;
            return this;
        }

        public Builder question(String text) {
            q.questionText = text;
            return this;
        }

        public Builder options(String... opts) {
            q.options.addAll(Arrays.asList(opts));
            return this;
        }

        public Builder correctOption(int index) {
            q.correctOptionIndex = index;
            return this;
        }

        public Builder hint(String hint) {
            q.hint = hint;
            return this;
        }

        public Builder impact(String impact) {
            q.impactExplanation = impact;
            return this;
        }

        public Builder remediation(String remediation) {
            q.remediationGuidance = remediation;
            return this;
        }

        public Builder difficulty(int level) {
            q.difficultyLevel = level;
            return this;
        }

        public Builder score(double score) {
            q.maxScore = score;
            return this;
        }

        public Builder dpdpClause(String clause) {
            q.dpdpClause = clause;
            return this;
        }

        public Builder isoControl(String control) {
            q.isoControl = control;
            return this;
        }

        public Builder mandatory(boolean mandatory) {
            q.mandatory = mandatory;
            return this;
        }

        public Builder sector(String sector) {
            q.sector = sector;
            return this;
        }

        public AssessmentQuestion build() {
            return q;
        }
    }

    public double calculateScore(int selectedOption) {
        if (selectedOption == correctOptionIndex) {
            return maxScore * category.getWeight();
        }

        // Partial credit based on how close the answer is
        int distance = Math.abs(selectedOption - correctOptionIndex);
        if (distance == 1)
            return maxScore * 0.5 * category.getWeight();
        if (distance == 2)
            return maxScore * 0.25 * category.getWeight();
        return 0;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public QuestionCategory getCategory() {
        return category;
    }

    public void setCategory(QuestionCategory category) {
        this.category = category;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String text) {
        this.questionText = text;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int index) {
        this.correctOptionIndex = index;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getImpactExplanation() {
        return impactExplanation;
    }

    public void setImpactExplanation(String impact) {
        this.impactExplanation = impact;
    }

    public String getRemediationGuidance() {
        return remediationGuidance;
    }

    public void setRemediationGuidance(String guidance) {
        this.remediationGuidance = guidance;
    }

    public int getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(int level) {
        this.difficultyLevel = level;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double score) {
        this.maxScore = score;
    }

    public String getDpdpClause() {
        return dpdpClause;
    }

    public void setDpdpClause(String clause) {
        this.dpdpClause = clause;
    }

    public String getIsoControl() {
        return isoControl;
    }

    public void setIsoControl(String control) {
        this.isoControl = control;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }
}
