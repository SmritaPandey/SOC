package com.qsdpdp.web;

import com.qsdpdp.reporting.ReportDefinition;
import com.qsdpdp.reporting.ReportExportService;
import com.qsdpdp.reporting.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reporting & Analytics REST Controller
 * Compliance reports, exports (PDF/CSV/JSON/XLSX/DOCX), scheduling, email sharing
 *
 * @version 2.0.0
 * @since Sprint 2
 */

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportingService reportingService;

    @Autowired(required = false)
    private ReportExportService exportService;

    // ═══════════════════════════════════════════════════════════
    // REPORT DEFINITIONS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/definitions")
    public ResponseEntity<?> getAvailableReports() {
        try {
            List<ReportDefinition> reports = reportingService.getAvailableReports();
            List<Map<String, Object>> reportList = reports.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.getId());
                m.put("name", r.getName());
                m.put("description", r.getDescription());
                m.put("type", r.getType() != null ? r.getType().name() : null);
                m.put("category", r.getCategory() != null ? r.getCategory().name() : null);
                m.put("dpdpClause", r.getDpdpClause());
                m.put("schedulable", r.isSchedulable());
                m.put("exportable", r.isExportable());
                m.put("active", r.isActive());
                return m;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "reports", reportList,
                    "total", reportList.size()));
        } catch (Exception e) {
            logger.error("Failed to get report definitions", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get reports: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GENERATE REPORT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(@RequestBody Map<String, Object> payload) {
        try {
            String definitionId = (String) payload.getOrDefault("definitionId", "");
            String outputFormat = (String) payload.getOrDefault("format", "json");
            String generatedBy = (String) payload.getOrDefault("generatedBy", "admin");

            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) payload.getOrDefault("parameters", new HashMap<>());

            ReportingService.ReportResult result = reportingService.generateReport(
                    definitionId, parameters, outputFormat, generatedBy);

            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "status", "generated",
                        "executionId", result.getExecutionId(),
                        "outputPath", result.getOutputPath(),
                        "rowCount", result.getRowCount(),
                        "format", outputFormat));
            } else {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", result.getError()));
            }
        } catch (Exception e) {
            logger.error("Failed to generate report", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FILE EXPORT ENDPOINTS (Sprint 2 — Req #24)
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/export/{reportType}/xlsx")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String reportType) {
        try {
            Map<String, Object> config = exportService.getReportQueryConfig(reportType);
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) config.get("columns");
            String title = (String) config.get("title");
            String query = (String) config.get("query");

            byte[] data = exportService.exportToExcel(title, query, columns);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("QS-DPDP_" + reportType + ".xlsx").build());

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Excel export failed for: " + reportType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/{reportType}/csv")
    public ResponseEntity<byte[]> exportCSV(@PathVariable String reportType) {
        try {
            Map<String, Object> config = exportService.getReportQueryConfig(reportType);
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) config.get("columns");
            String query = (String) config.get("query");

            byte[] data = exportService.exportToCSV(query, columns);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("QS-DPDP_" + reportType + ".csv").build());

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("CSV export failed for: " + reportType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/{reportType}/docx")
    public ResponseEntity<byte[]> exportWord(@PathVariable String reportType) {
        try {
            Map<String, Object> config = exportService.getReportQueryConfig(reportType);
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) config.get("columns");
            String title = (String) config.get("title");
            String query = (String) config.get("query");

            byte[] data = exportService.exportToWord(title, query, columns);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("QS-DPDP_" + reportType + ".docx").build());

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Word export failed for: " + reportType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/{reportType}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable String reportType) {
        try {
            Map<String, Object> config = exportService.getReportQueryConfig(reportType);
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) config.get("columns");
            String title = (String) config.get("title");
            String query = (String) config.get("query");

            byte[] data = exportService.exportToPdf(title, query, columns);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("QS-DPDP_" + reportType + ".pdf").build());

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("PDF export failed for: " + reportType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/{reportType}/json")
    public ResponseEntity<byte[]> exportJson(@PathVariable String reportType) {
        try {
            Map<String, Object> config = exportService.getReportQueryConfig(reportType);
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) config.get("columns");
            String title = (String) config.get("title");
            String query = (String) config.get("query");

            byte[] data = exportService.exportToJson(title, query, columns);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("QS-DPDP_" + reportType + ".json").build());

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("JSON export failed for: " + reportType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // QUICK GENERATE SHORTCUTS
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/generate/compliance-scorecard")
    public ResponseEntity<?> generateComplianceScorecard(
            @RequestBody(required = false) Map<String, Object> payload) {
        return generateQuickReport("compliance_scorecard", "json", payload);
    }

    @PostMapping("/generate/breach-summary")
    public ResponseEntity<?> generateBreachSummary(
            @RequestBody(required = false) Map<String, Object> payload) {
        return generateQuickReport("breach_summary", "json", payload);
    }

    @PostMapping("/generate/consent-analytics")
    public ResponseEntity<?> generateConsentAnalytics(
            @RequestBody(required = false) Map<String, Object> payload) {
        return generateQuickReport("consent_analytics", "json", payload);
    }

    @PostMapping("/generate/rights-requests")
    public ResponseEntity<?> generateRightsReport(
            @RequestBody(required = false) Map<String, Object> payload) {
        return generateQuickReport("rights_requests", "json", payload);
    }

    @PostMapping("/generate/audit-trail")
    public ResponseEntity<?> generateAuditTrail(
            @RequestBody(required = false) Map<String, Object> payload) {
        return generateQuickReport("audit_trail", "json", payload);
    }

    @PostMapping("/generate/dpbi-submission")
    public ResponseEntity<?> generateDpbiSubmission(
            @RequestBody(required = false) Map<String, Object> payload) {
        return generateQuickReport("dpbi_submission", "pdf", payload);
    }

    // ═══════════════════════════════════════════════════════════
    // SCHEDULE REPORT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleReport(@RequestBody Map<String, Object> payload) {
        try {
            String definitionId = (String) payload.getOrDefault("definitionId", "");
            String name = (String) payload.getOrDefault("name", "Scheduled Report");
            String cronExpression = (String) payload.getOrDefault("cronExpression", "0 0 8 * * MON");
            String outputFormat = (String) payload.getOrDefault("format", "pdf");
            String createdBy = (String) payload.getOrDefault("createdBy", "admin");

            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) payload.getOrDefault("parameters", new HashMap<>());
            @SuppressWarnings("unchecked")
            List<String> recipients = (List<String>) payload.getOrDefault("recipients", List.of());

            reportingService.scheduleReport(
                    definitionId, name, cronExpression, parameters, outputFormat, recipients, createdBy);

            return ResponseEntity.ok(Map.of(
                    "status", "scheduled",
                    "definitionId", definitionId,
                    "cronExpression", cronExpression,
                    "message", "Report scheduled: " + name));
        } catch (Exception e) {
            logger.error("Failed to schedule report", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to schedule: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REPORT TYPES REFERENCE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/types")
    public ResponseEntity<?> getReportTypes() {
        return ResponseEntity.ok(Map.of(
                "reportTypes", Arrays.stream(ReportDefinition.ReportType.values())
                        .map(Enum::name).collect(Collectors.toList()),
                "categories", Arrays.stream(ReportDefinition.ReportCategory.values())
                        .map(c -> Map.of("id", c.name(),
                                "name", c.getDisplayName(),
                                "description", c.getDescription()))
                        .collect(Collectors.toList()),
                "outputFormats", List.of("PDF", "CSV", "JSON", "EXCEL", "DOCX")));
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private ResponseEntity<?> generateQuickReport(String defId, String format,
            Map<String, Object> payload) {
        try {
            Map<String, Object> params = payload != null
                    ? (Map<String, Object>) payload.getOrDefault("parameters", new HashMap<>())
                    : new HashMap<>();
            String generatedBy = payload != null
                    ? (String) payload.getOrDefault("generatedBy", "admin") : "admin";

            ReportingService.ReportResult result =
                    reportingService.generateReport(defId, params, format, generatedBy);

            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "status", "generated",
                        "executionId", result.getExecutionId(),
                        "outputPath", result.getOutputPath(),
                        "rowCount", result.getRowCount()));
            } else {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", result.getError()));
            }
        } catch (Exception e) {
            logger.error("Failed to generate quick report: " + defId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate: " + e.getMessage()));
        }
    }
}
