package com.qsdpdp.dpia;

/**
 * DPIA Risk entity
 */
public class DPIARisk {

    private String id;
    private String dpiaId;
    private String category;
    private String description;
    private int likelihood;
    private int impact;
    private double score;
    private RiskLevel level;
    private String mitigation;
    private String status;

    public DPIARisk() {
    }

    public DPIARisk(String category, String description, int likelihood, int impact) {
        this.category = category;
        this.description = description;
        this.likelihood = likelihood;
        this.impact = impact;
        this.score = likelihood * impact;
        this.level = RiskLevel.fromScore(score * 4); // Scale to 100
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDpiaId() {
        return dpiaId;
    }

    public void setDpiaId(String dpiaId) {
        this.dpiaId = dpiaId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getLikelihood() {
        return likelihood;
    }

    public void setLikelihood(int likelihood) {
        this.likelihood = likelihood;
    }

    public int getImpact() {
        return impact;
    }

    public void setImpact(int impact) {
        this.impact = impact;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public void setLevel(RiskLevel level) {
        this.level = level;
    }

    public String getMitigation() {
        return mitigation;
    }

    public void setMitigation(String mitigation) {
        this.mitigation = mitigation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
