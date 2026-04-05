package com.qsdpdp.dpia;

import java.time.LocalDateTime;
import java.util.*;

/**
 * DPIA (Data Protection Impact Assessment) model
 * Complete assessment with risk matrix and mitigation tracking
 * 
 * @version 1.0.0
 * @since Module 9
 */
public class DPIA {

    private String id;
    private String title;
    private String description;
    private String status; // DRAFT, IN_REVIEW, APPROVED, REJECTED, ARCHIVED
    private String organizationId;
    private String departmentId;

    // Processing Activity Details
    private String processingActivity;
    private String processingPurpose;
    private String legalBasis;
    private Set<String> dataCategories;
    private Set<String> dataSources;
    private Set<String> dataRecipients;
    private int estimatedDataSubjects;
    private String retentionPeriod;
    private boolean childrenData;
    private boolean sensitiveData;
    private boolean crossBorderTransfer;

    // Risk Assessment
    private List<DPIARisk> identifiedRisks;
    private DPIARiskLevel overallRiskLevel;
    private double overallRiskScore;

    // Mitigations
    private List<DPIAMitigation> mitigations;
    private DPIARiskLevel residualRiskLevel;

    // Consultation
    private boolean dpbiConsultationRequired;
    private String dpbiConsultationId;
    private LocalDateTime dpbiConsultationDate;

    // Workflow
    private String createdBy;
    private String reviewedBy;
    private String approvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime nextReviewDate;
    private String reviewNotes;
    private String approvalNotes;

    // Extended fields (used by DPIAService)
    private String referenceNumber;
    private String projectName;
    private String dataTypes;
    private String assessor;
    private String approver;
    private String findings;
    private String mitigationPlan;
    private String residualRisks;
    private double riskScore;
    private RiskLevel riskLevel;
    private List<com.qsdpdp.dpia.DPIARisk> risks;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime nextReviewAt;

    public DPIA() {
        this.id = UUID.randomUUID().toString();
        this.status = "DRAFT";
        this.dataCategories = new HashSet<>();
        this.dataSources = new HashSet<>();
        this.dataRecipients = new HashSet<>();
        this.identifiedRisks = new ArrayList<>();
        this.mitigations = new ArrayList<>();
        this.risks = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.riskLevel = RiskLevel.LOW;
    }

    public DPIA(String title, String processingActivity) {
        this();
        this.title = title;
        this.processingActivity = processingActivity;
    }

    public void addRisk(DPIARisk risk) {
        identifiedRisks.add(risk);
        recalculateOverallRisk();
    }

    public void addMitigation(DPIAMitigation mitigation) {
        mitigations.add(mitigation);
        recalculateResidualRisk();
    }

    private void recalculateOverallRisk() {
        if (identifiedRisks.isEmpty()) {
            overallRiskLevel = DPIARiskLevel.LOW;
            overallRiskScore = 0;
            return;
        }

        double totalScore = 0;
        DPIARiskLevel highestLevel = DPIARiskLevel.LOW;

        for (DPIARisk risk : identifiedRisks) {
            totalScore += risk.getRiskScore();
            if (risk.getRiskLevel().getScore() > highestLevel.getScore()) {
                highestLevel = risk.getRiskLevel();
            }
        }

        overallRiskScore = totalScore / identifiedRisks.size();
        overallRiskLevel = highestLevel;

        dpbiConsultationRequired = overallRiskLevel == DPIARiskLevel.CRITICAL ||
                (overallRiskLevel == DPIARiskLevel.HIGH &&
                        (childrenData || crossBorderTransfer));
    }

    private void recalculateResidualRisk() {
        if (identifiedRisks.isEmpty()) {
            residualRiskLevel = DPIARiskLevel.LOW;
            return;
        }

        double mitigationEffectiveness = 0;
        for (DPIAMitigation m : mitigations) {
            if ("IMPLEMENTED".equals(m.getStatus())) {
                mitigationEffectiveness += m.getEffectiveness();
            }
        }

        double residualScore = overallRiskScore * (1 - Math.min(mitigationEffectiveness, 0.9));

        if (residualScore < 4)
            residualRiskLevel = DPIARiskLevel.LOW;
        else if (residualScore < 9)
            residualRiskLevel = DPIARiskLevel.MEDIUM;
        else if (residualScore < 16)
            residualRiskLevel = DPIARiskLevel.HIGH;
        else
            residualRiskLevel = DPIARiskLevel.CRITICAL;
    }

    public void submitForReview() {
        if (identifiedRisks.isEmpty()) {
            throw new IllegalStateException("Cannot submit DPIA without risk assessment");
        }
        this.status = "IN_REVIEW";
    }

    public void approve(String approver, String notes) {
        this.status = "APPROVED";
        this.approvedBy = approver;
        this.approvalNotes = notes;
        this.approvedAt = LocalDateTime.now();
        this.nextReviewDate = LocalDateTime.now().plusYears(1);
    }

    public void reject(String reviewer, String notes) {
        this.status = "REJECTED";
        this.reviewedBy = reviewer;
        this.reviewNotes = notes;
        this.reviewedAt = LocalDateTime.now();
    }

    public int getTotalRisks() {
        return identifiedRisks.size();
    }

    public long getCriticalRisks() {
        return identifiedRisks.stream()
                .filter(r -> r.getRiskLevel() == DPIARiskLevel.CRITICAL)
                .count();
    }

    public long getHighRisks() {
        return identifiedRisks.stream()
                .filter(r -> r.getRiskLevel() == DPIARiskLevel.HIGH)
                .count();
    }

    public long getImplementedMitigations() {
        return mitigations.stream()
                .filter(m -> "IMPLEMENTED".equals(m.getStatus()))
                .count();
    }

    public double getMitigationProgress() {
        if (mitigations.isEmpty())
            return 0;
        return (double) getImplementedMitigations() / mitigations.size() * 100;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DPIAStatus getStatus() {
        return DPIAStatus.valueOf(status);
    }

    public String getStatusString() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String id) {
        this.organizationId = id;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String id) {
        this.departmentId = id;
    }

    public String getProcessingActivity() {
        return processingActivity;
    }

    public void setProcessingActivity(String activity) {
        this.processingActivity = activity;
    }

    public String getProcessingPurpose() {
        return processingPurpose;
    }

    public void setProcessingPurpose(String purpose) {
        this.processingPurpose = purpose;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String basis) {
        this.legalBasis = basis;
    }

    public Set<String> getDataCategories() {
        return dataCategories;
    }

    public Set<String> getDataSources() {
        return dataSources;
    }

    public Set<String> getDataRecipients() {
        return dataRecipients;
    }

    public int getEstimatedDataSubjects() {
        return estimatedDataSubjects;
    }

    public void setEstimatedDataSubjects(int count) {
        this.estimatedDataSubjects = count;
    }

    public String getRetentionPeriod() {
        return retentionPeriod;
    }

    public void setRetentionPeriod(String period) {
        this.retentionPeriod = period;
    }

    public boolean isChildrenData() {
        return childrenData;
    }

    public void setChildrenData(boolean children) {
        this.childrenData = children;
    }

    public boolean isSensitiveData() {
        return sensitiveData;
    }

    public void setSensitiveData(boolean sensitive) {
        this.sensitiveData = sensitive;
    }

    public boolean isCrossBorderTransfer() {
        return crossBorderTransfer;
    }

    public void setCrossBorderTransfer(boolean crossBorder) {
        this.crossBorderTransfer = crossBorder;
    }

    public List<DPIARisk> getIdentifiedRisks() {
        return identifiedRisks;
    }

    public DPIARiskLevel getOverallRiskLevel() {
        return overallRiskLevel;
    }

    public double getOverallRiskScore() {
        return overallRiskScore;
    }

    public List<DPIAMitigation> getMitigations() {
        return mitigations;
    }

    public DPIARiskLevel getResidualRiskLevel() {
        return residualRiskLevel;
    }

    public boolean isDpbiConsultationRequired() {
        return dpbiConsultationRequired;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String user) {
        this.createdBy = user;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getNextReviewDate() {
        return nextReviewDate;
    }

    // Extended getters/setters for DPIAService compatibility
    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(String dataTypes) {
        this.dataTypes = dataTypes;
    }

    public String getAssessor() {
        return assessor;
    }

    public void setAssessor(String assessor) {
        this.assessor = assessor;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }

    public String getMitigationPlan() {
        return mitigationPlan;
    }

    public void setMitigationPlan(String mitigationPlan) {
        this.mitigationPlan = mitigationPlan;
    }

    public String getResidualRisks() {
        return residualRisks;
    }

    public void setResidualRisks(String residualRisks) {
        this.residualRisks = residualRisks;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<com.qsdpdp.dpia.DPIARisk> getRisks() {
        return risks;
    }

    public void setRisks(List<com.qsdpdp.dpia.DPIARisk> risks) {
        this.risks = risks;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public void setStatus(DPIAStatus dpiaStatus) {
        this.status = dpiaStatus.name();
    }



    public boolean requiresDPBIConsultation() {
        return dpbiConsultationRequired || riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }

    public static DPIABuilder builder() {
        return new DPIABuilder();
    }

    public static class DPIABuilder {
        private final DPIA dpia = new DPIA();

        public DPIABuilder id(String id) {
            dpia.id = id;
            return this;
        }

        public DPIABuilder referenceNumber(String ref) {
            dpia.referenceNumber = ref;
            return this;
        }

        public DPIABuilder title(String title) {
            dpia.title = title;
            return this;
        }

        public DPIABuilder description(String desc) {
            dpia.description = desc;
            return this;
        }

        public DPIABuilder projectName(String name) {
            dpia.projectName = name;
            return this;
        }

        public DPIABuilder dataTypes(String types) {
            dpia.dataTypes = types;
            return this;
        }

        public DPIABuilder assessor(String assessor) {
            dpia.assessor = assessor;
            return this;
        }

        public DPIA build() {
            return dpia;
        }
    }

    public static class DPIARisk {
        private String id;
        private String category;
        private String description;
        private int likelihood;
        private int impact;
        private DPIARiskLevel riskLevel;
        private double riskScore;
        private String dpdpClause;
        private String consequences;

        public DPIARisk() {
            this.id = UUID.randomUUID().toString();
        }

        public DPIARisk(String category, String description, int likelihood, int impact) {
            this();
            this.category = category;
            this.description = description;
            this.likelihood = likelihood;
            this.impact = impact;
            this.riskScore = likelihood * impact;
            this.riskLevel = DPIARiskLevel.fromScore(likelihood, impact);
        }

        public String getId() {
            return id;
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

        public DPIARiskLevel getRiskLevel() {
            return riskLevel;
        }

        public double getRiskScore() {
            return riskScore;
        }

        public String getDpdpClause() {
            return dpdpClause;
        }

        public void setDpdpClause(String clause) {
            this.dpdpClause = clause;
        }

        public String getConsequences() {
            return consequences;
        }

        public void setConsequences(String consequences) {
            this.consequences = consequences;
        }
    }

    public static class DPIAMitigation {
        private String id;
        private String riskId;
        private String controlType;
        private String description;
        private String status;
        private String owner;
        private LocalDateTime targetDate;
        private double effectiveness;

        public DPIAMitigation() {
            this.id = UUID.randomUUID().toString();
            this.status = "PLANNED";
        }

        public DPIAMitigation(String riskId, String controlType, String description) {
            this();
            this.riskId = riskId;
            this.controlType = controlType;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getRiskId() {
            return riskId;
        }

        public void setRiskId(String riskId) {
            this.riskId = riskId;
        }

        public String getControlType() {
            return controlType;
        }

        public void setControlType(String type) {
            this.controlType = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public LocalDateTime getTargetDate() {
            return targetDate;
        }

        public void setTargetDate(LocalDateTime date) {
            this.targetDate = date;
        }

        public double getEffectiveness() {
            return effectiveness;
        }

        public void setEffectiveness(double effectiveness) {
            this.effectiveness = effectiveness;
        }
    }
}
