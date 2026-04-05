package com.qsdpdp.core;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Risk Object Entity — Global Data Model
 * Represents a quantifiable risk with the formula: Risk Score = Likelihood × Impact × Exposure.
 * Supports sector-based risk libraries and AI prediction of emerging risks.
 *
 * @version 1.0.0
 * @since Module 8
 */
public class RiskObject {

    public enum RiskCategory {
        COMPLIANCE("Regulatory Compliance Risk"),
        DATA_BREACH("Data Breach Risk"),
        CONSENT("Consent Management Risk"),
        CROSS_BORDER("Cross-Border Transfer Risk"),
        VENDOR("Third-Party Vendor Risk"),
        OPERATIONAL("Operational Risk"),
        TECHNOLOGY("Technology/Infrastructure Risk"),
        PRIVACY("Privacy Impact Risk"),
        LEGAL("Legal/Litigation Risk"),
        REPUTATIONAL("Reputational Risk");

        private final String description;
        RiskCategory(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public enum MitigationStatus {
        IDENTIFIED, ASSESSED, TREATMENT_PLANNED, IN_PROGRESS, MITIGATED, ACCEPTED, TRANSFERRED, CLOSED
    }

    public enum RiskLevel {
        CRITICAL(4), HIGH(3), MEDIUM(2), LOW(1), NEGLIGIBLE(0);

        private final int value;
        RiskLevel(int value) { this.value = value; }
        public int getValue() { return value; }

        public static RiskLevel fromScore(double score) {
            if (score >= 80) return CRITICAL;
            if (score >= 60) return HIGH;
            if (score >= 40) return MEDIUM;
            if (score >= 20) return LOW;
            return NEGLIGIBLE;
        }
    }

    private String riskId;
    private String name;
    private String description;
    private RiskCategory category;
    private double likelihood;              // 0.0 to 10.0
    private double impact;                  // 0.0 to 10.0
    private double exposure;                // 0.0 to 10.0 (data volume, sensitivity factor)
    private double riskScore;               // Calculated: L × I × E (0 to 1000)
    private double normalizedScore;         // Normalized to 0-100
    private RiskLevel riskLevel;
    private String sector;                  // Sector this risk applies to
    private String dpdpSection;             // DPDP Act section reference
    private List<String> controlMapping;    // Mapped controls to mitigate
    private MitigationStatus mitigationStatus;
    private String mitigationPlan;
    private String owner;                   // Risk owner
    private String assessedBy;
    private LocalDateTime identifiedAt;
    private LocalDateTime assessedAt;
    private LocalDateTime reviewDate;
    private LocalDateTime nextReviewDate;
    private String treatmentStrategy;       // Mitigate, Accept, Transfer, Avoid
    private double residualRisk;            // Risk remaining after treatment
    private Map<String, Double> historicalScores;  // Score history for trend analysis
    private boolean aiPredicted;            // Was this risk predicted by AI
    private double aiConfidence;            // AI prediction confidence (0-1)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RiskObject() {
        this.riskId = UUID.randomUUID().toString();
        this.mitigationStatus = MitigationStatus.IDENTIFIED;
        this.controlMapping = new ArrayList<>();
        this.historicalScores = new LinkedHashMap<>();
        this.identifiedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- Core Risk Calculation ---

    /**
     * Calculate Risk Score = Likelihood × Impact × Exposure
     * Normalizes to 0-100 scale
     */
    public double calculateRiskScore() {
        this.riskScore = likelihood * impact * exposure;
        this.normalizedScore = (riskScore / 1000.0) * 100.0;
        this.normalizedScore = Math.min(normalizedScore, 100.0);
        this.riskLevel = RiskLevel.fromScore(normalizedScore);
        this.updatedAt = LocalDateTime.now();
        this.historicalScores.put(LocalDateTime.now().toString(), normalizedScore);
        return normalizedScore;
    }

    /**
     * Calculate residual risk after controls are applied
     * Residual = Original × (1 - control effectiveness)
     */
    public double calculateResidualRisk(double controlEffectiveness) {
        this.residualRisk = normalizedScore * (1.0 - controlEffectiveness);
        return residualRisk;
    }

    /**
     * Predict risk trend based on historical scores
     * Returns: INCREASING, STABLE, DECREASING
     */
    public String predictTrend() {
        if (historicalScores.size() < 2) return "INSUFFICIENT_DATA";
        List<Double> scores = new ArrayList<>(historicalScores.values());
        int n = scores.size();
        double recent = scores.get(n - 1);
        double previous = scores.get(n - 2);
        double delta = recent - previous;
        if (delta > 5) return "INCREASING";
        if (delta < -5) return "DECREASING";
        return "STABLE";
    }

    // --- Getters and Setters ---

    public String getRiskId() { return riskId; }
    public void setRiskId(String riskId) { this.riskId = riskId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public RiskCategory getCategory() { return category; }
    public void setCategory(RiskCategory category) { this.category = category; }

    public double getLikelihood() { return likelihood; }
    public void setLikelihood(double likelihood) { this.likelihood = Math.max(0, Math.min(10, likelihood)); }

    public double getImpact() { return impact; }
    public void setImpact(double impact) { this.impact = Math.max(0, Math.min(10, impact)); }

    public double getExposure() { return exposure; }
    public void setExposure(double exposure) { this.exposure = Math.max(0, Math.min(10, exposure)); }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public double getNormalizedScore() { return normalizedScore; }
    public void setNormalizedScore(double normalizedScore) { this.normalizedScore = normalizedScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getDpdpSection() { return dpdpSection; }
    public void setDpdpSection(String dpdpSection) { this.dpdpSection = dpdpSection; }

    public List<String> getControlMapping() { return controlMapping; }
    public void setControlMapping(List<String> controlMapping) { this.controlMapping = controlMapping; }

    public MitigationStatus getMitigationStatus() { return mitigationStatus; }
    public void setMitigationStatus(MitigationStatus mitigationStatus) { this.mitigationStatus = mitigationStatus; }

    public String getMitigationPlan() { return mitigationPlan; }
    public void setMitigationPlan(String mitigationPlan) { this.mitigationPlan = mitigationPlan; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getAssessedBy() { return assessedBy; }
    public void setAssessedBy(String assessedBy) { this.assessedBy = assessedBy; }

    public LocalDateTime getIdentifiedAt() { return identifiedAt; }
    public void setIdentifiedAt(LocalDateTime identifiedAt) { this.identifiedAt = identifiedAt; }

    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }

    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }

    public LocalDateTime getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDateTime nextReviewDate) { this.nextReviewDate = nextReviewDate; }

    public String getTreatmentStrategy() { return treatmentStrategy; }
    public void setTreatmentStrategy(String treatmentStrategy) { this.treatmentStrategy = treatmentStrategy; }

    public double getResidualRisk() { return residualRisk; }
    public void setResidualRisk(double residualRisk) { this.residualRisk = residualRisk; }

    public Map<String, Double> getHistoricalScores() { return historicalScores; }
    public void setHistoricalScores(Map<String, Double> historicalScores) { this.historicalScores = historicalScores; }

    public boolean isAiPredicted() { return aiPredicted; }
    public void setAiPredicted(boolean aiPredicted) { this.aiPredicted = aiPredicted; }

    public double getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(double aiConfidence) { this.aiConfidence = aiConfidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // --- Business Logic ---

    /**
     * Check if risk needs immediate attention
     */
    public boolean requiresImmediateAction() {
        return riskLevel == RiskLevel.CRITICAL ||
               (riskLevel == RiskLevel.HIGH && mitigationStatus == MitigationStatus.IDENTIFIED);
    }

    /**
     * Check if review is overdue
     */
    public boolean isReviewOverdue() {
        return nextReviewDate != null && nextReviewDate.isBefore(LocalDateTime.now());
    }

    /**
     * Add a control to the mapping
     */
    public void addControl(String controlId) {
        if (!controlMapping.contains(controlId)) {
            controlMapping.add(controlId);
        }
    }

    /**
     * Create a sector-specific risk from template
     */
    public static RiskObject createSectorRisk(String sector, String name, String description,
                                               RiskCategory category, double likelihood,
                                               double impact, double exposure, String dpdpSection) {
        RiskObject risk = new RiskObject();
        risk.setSector(sector);
        risk.setName(name);
        risk.setDescription(description);
        risk.setCategory(category);
        risk.setLikelihood(likelihood);
        risk.setImpact(impact);
        risk.setExposure(exposure);
        risk.setDpdpSection(dpdpSection);
        risk.calculateRiskScore();
        return risk;
    }
}
