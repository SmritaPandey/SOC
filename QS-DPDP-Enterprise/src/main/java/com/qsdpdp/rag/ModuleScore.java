package com.qsdpdp.rag;

/**
 * Module Score data class
 */
public class ModuleScore {

    private final String module;
    private final double score;
    private final RAGStatus status;
    private final String details;

    public ModuleScore(String module, double score, RAGStatus status, String details) {
        this.module = module;
        this.score = score;
        this.status = status;
        this.details = details;
    }

    public String getModule() {
        return module;
    }

    public double getScore() {
        return score;
    }

    public RAGStatus getStatus() {
        return status;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return String.format("ModuleScore[%s: %.1f%% %s]", module, score, status);
    }
}
