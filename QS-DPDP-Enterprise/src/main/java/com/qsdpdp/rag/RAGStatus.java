package com.qsdpdp.rag;

/**
 * RAG (Red-Amber-Green) Status enum
 */
public enum RAGStatus {
    GREEN("Compliant", "#22C55E", 80, 100),
    AMBER("Partial Compliance", "#F59E0B", 50, 79),
    RED("Non-Compliant", "#EF4444", 0, 49);

    private final String description;
    private final String hexColor;
    private final int minScore;
    private final int maxScore;

    RAGStatus(String description, String hexColor, int minScore, int maxScore) {
        this.description = description;
        this.hexColor = hexColor;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getDescription() {
        return description;
    }

    public String getHexColor() {
        return hexColor;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public static RAGStatus fromScore(double score) {
        if (score >= 80)
            return GREEN;
        if (score >= 50)
            return AMBER;
        return RED;
    }
}
