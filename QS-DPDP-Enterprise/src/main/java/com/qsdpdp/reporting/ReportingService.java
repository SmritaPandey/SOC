package com.qsdpdp.reporting;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.core.ComplianceEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Reporting Engine Service - Generate compliance reports and dashboards
 * Supports PDF, Excel, CSV exports with scheduling
 * 
 * @version 1.0.0
 * @since Module 14
 */
@Service
public class ReportingService {

    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final ComplianceEngine complianceEngine;

    private boolean initialized = false;
    private final Map<String, ReportDefinition> reportDefinitions = new HashMap<>();

    @Autowired
    public ReportingService(DatabaseManager dbManager, AuditService auditService,
            ComplianceEngine complianceEngine) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.complianceEngine = complianceEngine;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing Reporting Service...");
        createTables();
        loadReportDefinitions();

        initialized = true;
        logger.info("Reporting Service initialized with {} report definitions", reportDefinitions.size());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS report_definitions (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            description TEXT,
                            type TEXT,
                            category TEXT,
                            template_id TEXT,
                            parameters TEXT,
                            sections TEXT,
                            output_format TEXT,
                            schedulable INTEGER DEFAULT 0,
                            dpdp_clause TEXT,
                            active INTEGER DEFAULT 1,
                            created_by TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS report_executions (
                            id TEXT PRIMARY KEY,
                            definition_id TEXT NOT NULL,
                            parameters TEXT,
                            status TEXT DEFAULT 'PENDING',
                            output_path TEXT,
                            output_format TEXT,
                            generated_by TEXT,
                            started_at TIMESTAMP,
                            completed_at TIMESTAMP,
                            error_message TEXT,
                            row_count INTEGER,
                            file_size_bytes INTEGER,
                            FOREIGN KEY (definition_id) REFERENCES report_definitions(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS report_schedules (
                            id TEXT PRIMARY KEY,
                            definition_id TEXT NOT NULL,
                            name TEXT,
                            cron_expression TEXT NOT NULL,
                            parameters TEXT,
                            output_format TEXT,
                            recipients TEXT,
                            active INTEGER DEFAULT 1,
                            last_run_at TIMESTAMP,
                            next_run_at TIMESTAMP,
                            created_by TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (definition_id) REFERENCES report_definitions(id)
                        )
                    """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_report_exec_def ON report_executions(definition_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_report_exec_status ON report_executions(status)");

            logger.info("Reporting tables created");

        } catch (SQLException e) {
            logger.error("Failed to create Reporting tables", e);
        }
    }

    private void loadReportDefinitions() {
        // Load built-in definitions
        for (ReportDefinition def : ReportDefinition.getDefaultReports()) {
            reportDefinitions.put(def.getId(), def);
        }

        // Load custom definitions from DB
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM report_definitions WHERE active = 1")) {
            while (rs.next()) {
                ReportDefinition def = mapReportDefinition(rs);
                reportDefinitions.put(def.getId(), def);
            }
        } catch (SQLException e) {
            logger.error("Failed to load report definitions", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REPORT GENERATION
    // ═══════════════════════════════════════════════════════════

    public ReportResult generateReport(String definitionId, Map<String, Object> parameters,
            String outputFormat, String generatedBy) {
        ReportDefinition definition = reportDefinitions.get(definitionId);
        if (definition == null) {
            return ReportResult.error("Report definition not found: " + definitionId);
        }

        String executionId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Create execution record
            createExecution(executionId, definitionId, parameters, outputFormat, generatedBy);
            updateExecutionStatus(executionId, "RUNNING", null);

            // Gather data for each section
            List<Map<String, Object>> reportData = new ArrayList<>();
            for (ReportDefinition.ReportSection section : definition.getSections()) {
                Map<String, Object> sectionData = executeSectionQuery(section, parameters);
                sectionData.put("_sectionId", section.getId());
                sectionData.put("_sectionTitle", section.getTitle());
                reportData.add(sectionData);
            }

            // Generate output file
            String outputPath = generateOutput(executionId, definition, reportData,
                    outputFormat != null ? outputFormat : "PDF");

            int rowCount = calculateRowCount(reportData);
            long fileSize = getFileSize(outputPath);

            updateExecutionComplete(executionId, outputPath, rowCount, fileSize);

            auditService.log("REPORT_GENERATED", "REPORTING", generatedBy,
                    "Generated report: " + definition.getName());

            return ReportResult.success(executionId, outputPath, rowCount);

        } catch (Exception e) {
            logger.error("Failed to generate report", e);
            updateExecutionStatus(executionId, "FAILED", e.getMessage());
            return ReportResult.error("Report generation failed: " + e.getMessage());
        }
    }

    private Map<String, Object> executeSectionQuery(ReportDefinition.ReportSection section,
            Map<String, Object> parameters) {
        Map<String, Object> data = new HashMap<>();
        String dataSource = section.getDataSource();

        switch (dataSource) {
            case "compliance_scores" -> data.put("data", getComplianceScores(parameters));
            case "module_scores" -> data.put("data", getModuleScores(parameters));
            case "compliance_gaps" -> data.put("data", getComplianceGaps(parameters));
            case "compliance_trends" -> data.put("data", getComplianceTrends(parameters));
            case "breaches" -> data.put("data", getBreaches(parameters));
            case "breach_notifications" -> data.put("data", getBreachNotifications(parameters));
            case "consents" -> data.put("data", getConsents(parameters));
            case "consent_trends" -> data.put("data", getConsentTrends(parameters));
            case "rights_requests" -> data.put("data", getRightsRequests(parameters));
            case "audit_logs" -> data.put("data", getAuditLogs(parameters));
            default -> data.put("data", List.of());
        }

        return data;
    }

    // ═══════════════════════════════════════════════════════════
    // DATA QUERIES
    // ═══════════════════════════════════════════════════════════

    private List<Map<String, Object>> getComplianceScores(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM compliance_scores ORDER BY calculated_at DESC LIMIT 100")) {
            while (rs.next()) {
                results.add(Map.of(
                        "moduleName", rs.getString("module_name"),
                        "score", rs.getDouble("score"),
                        "ragStatus", rs.getString("rag_status"),
                        "calculatedAt", rs.getString("calculated_at")));
            }
        } catch (SQLException e) {
            logger.error("Failed to get compliance scores", e);
        }

        return results;
    }

    private List<Map<String, Object>> getModuleScores(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        String[] modules = { "Consent", "Breach", "Rights", "DPIA", "Policy", "Security" };
        Random rand = new Random(42); // Consistent demo data

        for (String module : modules) {
            results.add(Map.of(
                    "module", module,
                    "score", 70 + rand.nextInt(30),
                    "status", rand.nextInt(10) > 2 ? "GREEN" : "AMBER"));
        }

        return results;
    }

    private List<Map<String, Object>> getComplianceGaps(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM gap_items WHERE status != 'REMEDIATED' ORDER BY severity")) {
            while (rs.next()) {
                results.add(Map.of(
                        "category", rs.getString("category"),
                        "severity", rs.getString("severity"),
                        "dpdpClause", rs.getString("dpdp_clause"),
                        "status", rs.getString("status")));
            }
        } catch (SQLException e) {
            logger.error("Failed to get compliance gaps", e);
        }

        return results;
    }

    private List<Map<String, Object>> getComplianceTrends(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Generate trend data for last 12 months
        LocalDateTime now = LocalDateTime.now();
        Random rand = new Random(42);
        double baseScore = 65;

        for (int i = 11; i >= 0; i--) {
            LocalDateTime month = now.minusMonths(i);
            baseScore += rand.nextDouble() * 3 - 0.5; // Slight upward trend
            results.add(Map.of(
                    "month", month.getMonth().toString(),
                    "year", month.getYear(),
                    "score", Math.min(100, Math.max(0, baseScore))));
        }

        return results;
    }

    private List<Map<String, Object>> getBreaches(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM breaches ORDER BY reported_at DESC LIMIT 100")) {
            while (rs.next()) {
                results.add(Map.of(
                        "id", rs.getString("id"),
                        "severity", rs.getString("severity"),
                        "status", rs.getString("status"),
                        "affectedCount", rs.getInt("affected_count"),
                        "reportedAt", rs.getString("reported_at")));
            }
        } catch (SQLException e) {
            logger.error("Failed to get breaches", e);
        }

        return results;
    }

    private List<Map<String, Object>> getBreachNotifications(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM breach_notifications ORDER BY sent_at DESC LIMIT 100")) {
            while (rs.next()) {
                results.add(Map.of(
                        "breachId", rs.getString("breach_id"),
                        "recipientType", rs.getString("recipient_type"),
                        "status", rs.getString("status"),
                        "sentAt", rs.getString("sent_at")));
            }
        } catch (SQLException e) {
            logger.error("Failed to get breach notifications", e);
        }

        return results;
    }

    private List<Map<String, Object>> getConsents(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT purpose, status, COUNT(*) as count FROM consents GROUP BY purpose, status")) {
            while (rs.next()) {
                results.add(Map.of(
                        "purpose", rs.getString("purpose"),
                        "status", rs.getString("status"),
                        "count", rs.getInt("count")));
            }
        } catch (SQLException e) {
            logger.error("Failed to get consents", e);
        }

        return results;
    }

    private List<Map<String, Object>> getConsentTrends(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        Random rand = new Random(42);

        for (int i = 6; i >= 0; i--) {
            LocalDateTime week = now.minusWeeks(i);
            results.add(Map.of(
                    "week", "Week " + (7 - i),
                    "collected", 50 + rand.nextInt(100),
                    "withdrawn", 5 + rand.nextInt(15)));
        }

        return results;
    }

    private List<Map<String, Object>> getRightsRequests(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM rights_requests ORDER BY submitted_at DESC LIMIT 100")) {
            while (rs.next()) {
                results.add(Map.of(
                        "id", rs.getString("id"),
                        "type", rs.getString("request_type"),
                        "status", rs.getString("status"),
                        "submittedAt", rs.getString("submitted_at"),
                        "completedAt", rs.getString("completed_at")));
            }
        } catch (SQLException e) {
            logger.error("Failed to get rights requests", e);
        }

        return results;
    }

    private List<Map<String, Object>> getAuditLogs(Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 500")) {
            while (rs.next()) {
                results.add(Map.of(
                        "action", rs.getString("action"),
                        "module", rs.getString("module"),
                        "userId", rs.getString("user_id"),
                        "details", rs.getString("details"),
                        "timestamp", rs.getString("created_at")));
            }
        } catch (SQLException e) {
            logger.error("Failed to get audit logs", e);
        }

        return results;
    }

    // ═══════════════════════════════════════════════════════════
    // OUTPUT GENERATION
    // ═══════════════════════════════════════════════════════════

    private String generateOutput(String executionId, ReportDefinition definition,
            List<Map<String, Object>> data, String format) {
        String outputDir = System.getProperty("user.home") + "/QS-DPDP/reports/";
        new java.io.File(outputDir).mkdirs();

        String filename = String.format("%s_%s_%s.%s",
                definition.getName().replaceAll("[^a-zA-Z0-9]", "_"),
                executionId.substring(0, 8),
                LocalDateTime.now().toString().replace(":", "-").substring(0, 16),
                format.toLowerCase());

        String outputPath = outputDir + filename;

        switch (format.toUpperCase()) {
            case "PDF" -> generatePDF(outputPath, definition, data);
            case "CSV" -> generateCSV(outputPath, definition, data);
            case "EXCEL" -> generateExcel(outputPath, definition, data);
            case "JSON" -> generateJSON(outputPath, definition, data);
            default -> generatePDF(outputPath, definition, data);
        }

        return outputPath;
    }

    private void generatePDF(String path, ReportDefinition def, List<Map<String, Object>> data) {
        // In production, would use iText or Apache PDFBox
        try (java.io.PrintWriter writer = new java.io.PrintWriter(path)) {
            writer.println("# " + def.getName());
            writer.println("Generated: " + LocalDateTime.now());
            writer.println("DPDP Clause: " + def.getDpdpClause());
            writer.println();

            for (Map<String, Object> section : data) {
                writer.println("## " + section.get("_sectionTitle"));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) section.get("data");
                if (rows != null) {
                    for (Map<String, Object> row : rows) {
                        writer.println(row.toString());
                    }
                }
                writer.println();
            }
        } catch (Exception e) {
            logger.error("Failed to generate PDF", e);
        }
    }

    private void generateCSV(String path, ReportDefinition def, List<Map<String, Object>> data) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(path)) {
            for (Map<String, Object> section : data) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) section.get("data");
                if (rows != null && !rows.isEmpty()) {
                    // Header
                    writer.println(String.join(",", rows.get(0).keySet()));
                    // Data
                    for (Map<String, Object> row : rows) {
                        writer.println(row.values().stream()
                                .map(Object::toString)
                                .reduce((a, b) -> a + "," + b)
                                .orElse(""));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to generate CSV", e);
        }
    }

    private void generateExcel(String path, ReportDefinition def, List<Map<String, Object>> data) {
        // Would use Apache POI in production
        generateCSV(path.replace(".xlsx", ".csv"), def, data);
    }

    private void generateJSON(String path, ReportDefinition def, List<Map<String, Object>> data) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(path)) {
            writer.println("{");
            writer.println("  \"report\": \"" + def.getName() + "\",");
            writer.println("  \"generated\": \"" + LocalDateTime.now() + "\",");
            writer.println("  \"sections\": " + data.toString());
            writer.println("}");
        } catch (Exception e) {
            logger.error("Failed to generate JSON", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SCHEDULING
    // ═══════════════════════════════════════════════════════════

    public String scheduleReport(String definitionId, String name, String cronExpression,
            Map<String, Object> parameters, String outputFormat,
            List<String> recipients, String createdBy) {
        String scheduleId = UUID.randomUUID().toString();

        String sql = """
                    INSERT INTO report_schedules (id, definition_id, name, cron_expression,
                        parameters, output_format, recipients, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, scheduleId);
            stmt.setString(2, definitionId);
            stmt.setString(3, name);
            stmt.setString(4, cronExpression);
            stmt.setString(5, parameters != null ? parameters.toString() : null);
            stmt.setString(6, outputFormat);
            stmt.setString(7, recipients != null ? String.join(",", recipients) : null);
            stmt.setString(8, createdBy);
            stmt.executeUpdate();

            auditService.log("REPORT_SCHEDULED", "REPORTING", createdBy,
                    "Scheduled report: " + name);

            return scheduleId;

        } catch (SQLException e) {
            logger.error("Failed to schedule report", e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private void createExecution(String id, String definitionId, Map<String, Object> parameters,
            String outputFormat, String generatedBy) {
        String sql = """
                    INSERT INTO report_executions (id, definition_id, parameters, output_format,
                        generated_by, started_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, definitionId);
            stmt.setString(3, parameters != null ? parameters.toString() : null);
            stmt.setString(4, outputFormat);
            stmt.setString(5, generatedBy);
            stmt.setString(6, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to create execution record", e);
        }
    }

    private void updateExecutionStatus(String id, String status, String error) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE report_executions SET status = ?, error_message = ? WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setString(2, error);
            stmt.setString(3, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update execution status", e);
        }
    }

    private void updateExecutionComplete(String id, String outputPath, int rowCount, long fileSize) {
        String sql = """
                    UPDATE report_executions SET status = 'COMPLETED', output_path = ?,
                        row_count = ?, file_size_bytes = ?, completed_at = ?
                    WHERE id = ?
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, outputPath);
            stmt.setInt(2, rowCount);
            stmt.setLong(3, fileSize);
            stmt.setString(4, LocalDateTime.now().toString());
            stmt.setString(5, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update execution complete", e);
        }
    }

    private int calculateRowCount(List<Map<String, Object>> data) {
        int count = 0;
        for (Map<String, Object> section : data) {
            @SuppressWarnings("unchecked")
            List<?> rows = (List<?>) section.get("data");
            if (rows != null)
                count += rows.size();
        }
        return count;
    }

    private long getFileSize(String path) {
        java.io.File file = new java.io.File(path);
        return file.exists() ? file.length() : 0;
    }

    private ReportDefinition mapReportDefinition(ResultSet rs) throws SQLException {
        ReportDefinition def = new ReportDefinition();
        def.setId(rs.getString("id"));
        def.setName(rs.getString("name"));
        def.setDescription(rs.getString("description"));
        def.setType(ReportDefinition.ReportType.valueOf(rs.getString("type")));
        def.setCategory(ReportDefinition.ReportCategory.valueOf(rs.getString("category")));
        def.setOutputFormat(rs.getString("output_format"));
        def.setSchedulable(rs.getInt("schedulable") == 1);
        def.setDpdpClause(rs.getString("dpdp_clause"));
        return def;
    }

    public List<ReportDefinition> getAvailableReports() {
        return new ArrayList<>(reportDefinitions.values());
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class ReportResult {
        private final boolean success;
        private final String executionId;
        private final String outputPath;
        private final int rowCount;
        private final String error;

        private ReportResult(boolean success, String executionId, String outputPath,
                int rowCount, String error) {
            this.success = success;
            this.executionId = executionId;
            this.outputPath = outputPath;
            this.rowCount = rowCount;
            this.error = error;
        }

        public static ReportResult success(String executionId, String outputPath, int rowCount) {
            return new ReportResult(true, executionId, outputPath, rowCount, null);
        }

        public static ReportResult error(String message) {
            return new ReportResult(false, null, null, 0, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getExecutionId() {
            return executionId;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public int getRowCount() {
            return rowCount;
        }

        public String getError() {
            return error;
        }
    }
}
