package com.qsdpdp.reporting;

import java.util.*;
import java.time.LocalDateTime;

/**
 * Report Definition model - defines report structure and parameters
 * 
 * @version 1.0.0
 * @since Module 14
 */
public class ReportDefinition {

    private String id;
    private String name;
    private String description;
    private ReportType type;
    private ReportCategory category;
    private String templateId;
    private Set<String> requiredPermissions;
    private List<ReportParameter> parameters;
    private List<ReportSection> sections;
    private String outputFormat; // PDF, EXCEL, CSV, JSON
    private boolean schedulable;
    private boolean exportable;
    private String dpdpClause;
    private String createdBy;
    private LocalDateTime createdAt;
    private boolean active;

    public ReportDefinition() {
        this.id = UUID.randomUUID().toString();
        this.requiredPermissions = new HashSet<>();
        this.parameters = new ArrayList<>();
        this.sections = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    public ReportDefinition(String name, ReportType type, ReportCategory category) {
        this();
        this.name = name;
        this.type = type;
        this.category = category;
    }

    public enum ReportType {
        COMPLIANCE_SCORECARD,
        GAP_ANALYSIS,
        BREACH_SUMMARY,
        CONSENT_ANALYTICS,
        RIGHTS_REQUESTS,
        DPIA_STATUS,
        AUDIT_TRAIL,
        SECURITY_EVENTS,
        DLP_INCIDENTS,
        PII_DISCOVERY,
        POLICY_STATUS,
        EXECUTIVE_DASHBOARD,
        REGULATORY_SUBMISSION,
        CUSTOM
    }

    public enum ReportCategory {
        COMPLIANCE("Compliance Reports", "DPDP compliance status and metrics"),
        PRIVACY("Privacy Reports", "Data privacy and consent analytics"),
        SECURITY("Security Reports", "Security events and incidents"),
        OPERATIONAL("Operational Reports", "Day-to-day operations metrics"),
        REGULATORY("Regulatory Reports", "Reports for regulatory submission"),
        EXECUTIVE("Executive Reports", "High-level executive dashboards");

        private final String displayName;
        private final String description;

        ReportCategory(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class ReportParameter {
        private String name;
        private String displayName;
        private String type; // DATE, DATE_RANGE, SELECT, MULTI_SELECT, TEXT
        private boolean required;
        private Object defaultValue;
        private List<Object> options;

        public ReportParameter() {
        }

        public ReportParameter(String name, String displayName, String type, boolean required) {
            this.name = name;
            this.displayName = displayName;
            this.type = type;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        public List<Object> getOptions() {
            return options;
        }

        public void setOptions(List<Object> options) {
            this.options = options;
        }
    }

    public static class ReportSection {
        private String id;
        private String title;
        private int orderIndex;
        private String dataSource;
        private String chartType; // TABLE, BAR, PIE, LINE, AREA, HEATMAP
        private List<String> columns;
        private String aggregation;
        private String filter;

        public ReportSection() {
            this.id = UUID.randomUUID().toString();
        }

        public ReportSection(String title, int orderIndex, String dataSource) {
            this();
            this.title = title;
            this.orderIndex = orderIndex;
            this.dataSource = dataSource;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getOrderIndex() {
            return orderIndex;
        }

        public void setOrderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
        }

        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }

        public String getChartType() {
            return chartType;
        }

        public void setChartType(String chartType) {
            this.chartType = chartType;
        }

        public List<String> getColumns() {
            return columns;
        }

        public void setColumns(List<String> columns) {
            this.columns = columns;
        }

        public String getAggregation() {
            return aggregation;
        }

        public void setAggregation(String aggregation) {
            this.aggregation = aggregation;
        }

        public String getFilter() {
            return filter;
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }
    }

    // Pre-defined report templates
    public static ReportDefinition complianceScorecard() {
        ReportDefinition def = new ReportDefinition("DPDP Compliance Scorecard",
                ReportType.COMPLIANCE_SCORECARD, ReportCategory.COMPLIANCE);
        def.setDescription("Overall DPDP compliance status with RAG indicators");
        def.setDpdpClause("All Sections");
        def.addParameter(new ReportParameter("startDate", "Start Date", "DATE", true));
        def.addParameter(new ReportParameter("endDate", "End Date", "DATE", true));
        def.addSection(new ReportSection("Executive Summary", 1, "compliance_scores"));
        def.addSection(new ReportSection("Module-wise Scores", 2, "module_scores"));
        def.addSection(new ReportSection("Gap Summary", 3, "compliance_gaps"));
        def.addSection(new ReportSection("Trend Analysis", 4, "compliance_trends"));
        return def;
    }

    public static ReportDefinition breachSummary() {
        ReportDefinition def = new ReportDefinition("Breach Summary Report",
                ReportType.BREACH_SUMMARY, ReportCategory.SECURITY);
        def.setDescription("Summary of data breaches and response status");
        def.setDpdpClause("Section 8(6)");
        def.addParameter(new ReportParameter("dateRange", "Date Range", "DATE_RANGE", true));
        def.addParameter(new ReportParameter("severity", "Severity", "MULTI_SELECT", false));
        def.addSection(new ReportSection("Breach Overview", 1, "breaches"));
        def.addSection(new ReportSection("Notification Status", 2, "breach_notifications"));
        def.addSection(new ReportSection("Response Timeline", 3, "breach_timeline"));
        return def;
    }

    public static ReportDefinition consentAnalytics() {
        ReportDefinition def = new ReportDefinition("Consent Analytics",
                ReportType.CONSENT_ANALYTICS, ReportCategory.PRIVACY);
        def.setDescription("Consent collection and management analytics");
        def.setDpdpClause("Section 6");
        def.addParameter(new ReportParameter("dateRange", "Date Range", "DATE_RANGE", true));
        def.addParameter(new ReportParameter("purpose", "Purpose", "MULTI_SELECT", false));
        def.addSection(new ReportSection("Consent Summary", 1, "consents"));
        def.addSection(new ReportSection("Collection Trends", 2, "consent_trends"));
        def.addSection(new ReportSection("Withdrawal Analysis", 3, "consent_withdrawals"));
        return def;
    }

    public static ReportDefinition rightsRequests() {
        ReportDefinition def = new ReportDefinition("Rights Requests Report",
                ReportType.RIGHTS_REQUESTS, ReportCategory.PRIVACY);
        def.setDescription("Data subject rights requests and fulfillment");
        def.setDpdpClause("Sections 11-14");
        def.addParameter(new ReportParameter("dateRange", "Date Range", "DATE_RANGE", true));
        def.addParameter(new ReportParameter("requestType", "Request Type", "MULTI_SELECT", false));
        def.addSection(new ReportSection("Request Summary", 1, "rights_requests"));
        def.addSection(new ReportSection("SLA Compliance", 2, "rights_sla"));
        def.addSection(new ReportSection("Request Trends", 3, "rights_trends"));
        return def;
    }

    public static ReportDefinition auditTrail() {
        ReportDefinition def = new ReportDefinition("Audit Trail Report",
                ReportType.AUDIT_TRAIL, ReportCategory.COMPLIANCE);
        def.setDescription("Comprehensive audit trail for compliance verification");
        def.setDpdpClause("All Sections");
        def.addParameter(new ReportParameter("dateRange", "Date Range", "DATE_RANGE", true));
        def.addParameter(new ReportParameter("module", "Module", "MULTI_SELECT", false));
        def.addParameter(new ReportParameter("action", "Action Type", "MULTI_SELECT", false));
        def.addSection(new ReportSection("Audit Summary", 1, "audit_summary"));
        def.addSection(new ReportSection("Detailed Log", 2, "audit_logs"));
        return def;
    }

    public static ReportDefinition dpbiSubmission() {
        ReportDefinition def = new ReportDefinition("DPBI Regulatory Submission",
                ReportType.REGULATORY_SUBMISSION, ReportCategory.REGULATORY);
        def.setDescription("Report formatted for DPBI regulatory submission");
        def.setDpdpClause("Section 8(6)");
        def.addParameter(new ReportParameter("breachId", "Breach ID", "TEXT", true));
        def.addSection(new ReportSection("Breach Details", 1, "breach_details"));
        def.addSection(new ReportSection("Impact Assessment", 2, "breach_impact"));
        def.addSection(new ReportSection("Notification Evidence", 3, "breach_notifications"));
        def.addSection(new ReportSection("Remediation Measures", 4, "breach_remediation"));
        return def;
    }

    public static List<ReportDefinition> getDefaultReports() {
        return Arrays.asList(
                complianceScorecard(),
                breachSummary(),
                consentAnalytics(),
                rightsRequests(),
                auditTrail(),
                dpbiSubmission());
    }

    public void addParameter(ReportParameter param) {
        this.parameters.add(param);
    }

    public void addSection(ReportSection section) {
        this.sections.add(section);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public ReportCategory getCategory() {
        return category;
    }

    public void setCategory(ReportCategory category) {
        this.category = category;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public Set<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    public List<ReportParameter> getParameters() {
        return parameters;
    }

    public List<ReportSection> getSections() {
        return sections;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public boolean isSchedulable() {
        return schedulable;
    }

    public void setSchedulable(boolean schedulable) {
        this.schedulable = schedulable;
    }

    public boolean isExportable() {
        return exportable;
    }

    public void setExportable(boolean exportable) {
        this.exportable = exportable;
    }

    public String getDpdpClause() {
        return dpdpClause;
    }

    public void setDpdpClause(String dpdpClause) {
        this.dpdpClause = dpdpClause;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
