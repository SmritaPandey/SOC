package com.qsdpdp.core;

import com.qsdpdp.rag.RAGStatus;
import com.qsdpdp.rag.ModuleScore;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Compliance Score representing overall and module-level compliance status
 */
public class ComplianceScore {

    private final double overallScore;
    private final RAGStatus overallStatus;
    private final Map<String, ModuleScore> moduleScores;
    private final LocalDateTime calculatedAt;

    public ComplianceScore(double overallScore, RAGStatus overallStatus,
            Map<String, ModuleScore> moduleScores, LocalDateTime calculatedAt) {
        this.overallScore = overallScore;
        this.overallStatus = overallStatus;
        this.moduleScores = moduleScores;
        this.calculatedAt = calculatedAt;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public RAGStatus getOverallStatus() {
        return overallStatus;
    }

    public Map<String, ModuleScore> getModuleScores() {
        return moduleScores;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public ModuleScore getModuleScore(String module) {
        return moduleScores.get(module);
    }

    public int getGreenModuleCount() {
        return (int) moduleScores.values().stream()
                .filter(s -> s.getStatus() == RAGStatus.GREEN)
                .count();
    }

    public int getAmberModuleCount() {
        return (int) moduleScores.values().stream()
                .filter(s -> s.getStatus() == RAGStatus.AMBER)
                .count();
    }

    public int getRedModuleCount() {
        return (int) moduleScores.values().stream()
                .filter(s -> s.getStatus() == RAGStatus.RED)
                .count();
    }

    @Override
    public String toString() {
        return String.format("ComplianceScore[%.1f%% %s, modules=%d]",
                overallScore, overallStatus, moduleScores.size());
    }
}
