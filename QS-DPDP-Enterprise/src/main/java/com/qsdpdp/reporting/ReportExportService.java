package com.qsdpdp.reporting;

import com.qsdpdp.db.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Report Export Service
 * Generates actual file exports: PDF, Excel (XLSX), Word (DOCX), CSV, JSON
 * Requirement #24: Reports must be exported to word, excel, csv, pdf, json
 *
 * @version 2.0.0
 * @since Sprint 2
 */
@Service
public class ReportExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportService.class);

    @Autowired
    private DatabaseManager dbManager;

    private static final String EXPORT_DIR = "exports";

    /**
     * Export query results to Excel XLSX format
     */
    public byte[] exportToExcel(String reportTitle, String query, List<String> columns) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(reportTitle);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Title row
            Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("QS-DPDP Enterprise - " + reportTitle);
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Timestamp row
            Row tsRow = sheet.createRow(1);
            tsRow.createCell(0).setCellValue("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));

            // Header row
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < columns.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).toUpperCase());
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 4;
            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < columns.size(); i++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
                        String val = rs.getString(columns.get(i));
                        cell.setCellValue(val != null ? val : "");
                    }
                }
            }

            // Auto-size columns
            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            // Footer
            Row footerRow = sheet.createRow(rowNum + 1);
            footerRow.createCell(0).setCellValue("Total Records: " + (rowNum - 4));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Excel export complete: {} rows", rowNum - 4);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel export failed", e);
            return createErrorExcel(reportTitle, e.getMessage());
        }
    }

    /**
     * Export query results to CSV format
     */
    public byte[] exportToCSV(String query, List<String> columns) {
        StringBuilder sb = new StringBuilder();
        // BOM for Excel compatibility
        sb.append('\ufeff');
        // Header
        sb.append(String.join(",", columns)).append("\r\n");

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                List<String> values = new ArrayList<>();
                for (String col : columns) {
                    String val = rs.getString(col);
                    if (val != null && (val.contains(",") || val.contains("\"") || val.contains("\n"))) {
                        val = "\"" + val.replace("\"", "\"\"") + "\"";
                    }
                    values.add(val != null ? val : "");
                }
                sb.append(String.join(",", values)).append("\r\n");
            }
        } catch (Exception e) {
            log.error("CSV export failed", e);
            sb.append("ERROR: ").append(e.getMessage());
        }

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Export query results to Word DOCX format
     */
    public byte[] exportToWord(String reportTitle, String query, List<String> columns) {
        try (XWPFDocument document = new XWPFDocument()) {
            // Title
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText("QS-DPDP Enterprise");
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setColor("C62828");

            // Subtitle
            XWPFParagraph subtitle = document.createParagraph();
            subtitle.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subRun = subtitle.createRun();
            subRun.setText(reportTitle);
            subRun.setFontSize(14);
            subRun.setItalic(true);

            // Timestamp
            XWPFParagraph ts = document.createParagraph();
            XWPFRun tsRun = ts.createRun();
            tsRun.setText("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm:ss")));
            tsRun.setFontSize(10);
            tsRun.setColor("666666");

            document.createParagraph(); // Spacer

            // Create table
            int rowCount = 0;
            List<String[]> data = new ArrayList<>();

            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    String[] row = new String[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        String val = rs.getString(columns.get(i));
                        row[i] = val != null ? val : "";
                    }
                    data.add(row);
                    rowCount++;
                }
            }

            XWPFTable table = document.createTable(rowCount + 1, columns.size());
            table.setWidth("100%");

            // Header row
            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < columns.size(); i++) {
                XWPFRun run = headerRow.getCell(i).getParagraphs().get(0).createRun();
                run.setText(columns.get(i).toUpperCase().replace("_", " "));
                run.setBold(true);
                run.setFontSize(10);
            }

            // Data rows
            for (int r = 0; r < data.size(); r++) {
                XWPFTableRow row = table.getRow(r + 1);
                for (int c = 0; c < columns.size(); c++) {
                    row.getCell(c).setText(data.get(r)[c]);
                }
            }

            // Footer
            document.createParagraph();
            XWPFParagraph footer = document.createParagraph();
            XWPFRun footerRun = footer.createRun();
            footerRun.setText("Total Records: " + rowCount + " | DPDP Act 2023 Compliance Report | NeurQ AI Labs Pvt. Ltd.");
            footerRun.setFontSize(9);
            footerRun.setColor("999999");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            log.info("Word export complete: {} rows", rowCount);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Word export failed", e);
            return new byte[0];
        }
    }

    /**
     * Export query results to PDF format using OpenPDF
     */
    public byte[] exportToPdf(String reportTitle, String query, List<String> columns) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // --- Branded Header ---
            Font brandFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(198, 40, 40));
            Paragraph brand = new Paragraph("QS-DPDP Enterprise", brandFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            document.add(brand);

            Font titleFont = new Font(Font.HELVETICA, 14, Font.ITALIC, new Color(26, 26, 46));
            Paragraph titlePara = new Paragraph(reportTitle, titleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(4);
            document.add(titlePara);

            Font tsFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(102, 102, 102));
            Paragraph tsPara = new Paragraph("Generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm:ss")) +
                    " | DPDP Act 2023 Compliance Report", tsFont);
            tsPara.setAlignment(Element.ALIGN_CENTER);
            tsPara.setSpacingAfter(16);
            document.add(tsPara);

            // --- Data Table ---
            PdfPTable table = new PdfPTable(columns.size());
            table.setWidthPercentage(100);
            table.setSpacingBefore(8);

            // Header cells
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Color headerBg = new Color(26, 26, 26);
            for (String col : columns) {
                PdfPCell cell = new PdfPCell(new Phrase(col.toUpperCase().replace("_", " "), headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Data rows
            Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.BLACK);
            Color altBg = new Color(248, 249, 250);
            int rowCount = 0;

            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    for (String col : columns) {
                        String val = rs.getString(col);
                        PdfPCell cell = new PdfPCell(new Phrase(val != null ? val : "—", dataFont));
                        cell.setPadding(5);
                        if (rowCount % 2 == 1) cell.setBackgroundColor(altBg);
                        table.addCell(cell);
                    }
                    rowCount++;
                }
            }

            document.add(table);

            // --- Footer ---
            Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(153, 153, 153));
            Paragraph footer = new Paragraph(
                    "Total Records: " + rowCount + " | Confidential — NeurQ AI Labs Pvt. Ltd.", footerFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            footer.setSpacingBefore(12);
            document.add(footer);

            document.close();
            log.info("PDF export complete: {} rows", rowCount);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF export failed", e);
            return createErrorPdf(reportTitle, e.getMessage());
        }
    }

    /**
     * Export query results to JSON format
     */
    public byte[] exportToJson(String reportTitle, String query, List<String> columns) {
        try {
            List<Map<String, Object>> records = new ArrayList<>();

            try (Connection conn = dbManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String col : columns) {
                        row.put(col, rs.getString(col));
                    }
                    records.add(row);
                }
            }

            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("report", reportTitle);
            wrapper.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            wrapper.put("totalRecords", records.size());
            wrapper.put("data", records);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(wrapper);

            log.info("JSON export complete: {} records", records.size());
            return json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("JSON export failed", e);
            Map<String, String> error = Map.of("error", e.getMessage(), "report", reportTitle);
            return new Gson().toJson(error).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Get the SQL query and columns for a given report type
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getReportQueryConfig(String reportType) {
        Map<String, Object> config = new HashMap<>();
        switch (reportType.toLowerCase()) {
            case "compliance_scorecard":
                config.put("query", "SELECT id, module, score, max_score, percentage, status, assessed_at FROM compliance_scores ORDER BY assessed_at DESC LIMIT 200");
                config.put("columns", List.of("id", "module", "score", "max_score", "percentage", "status", "assessed_at"));
                config.put("title", "Compliance Scorecard Report");
                break;
            case "consent_analytics":
                config.put("query", "SELECT id, principal_name, purpose, status, collected_at, expires_at FROM consents ORDER BY collected_at DESC LIMIT 200");
                config.put("columns", List.of("id", "principal_name", "purpose", "status", "collected_at", "expires_at"));
                config.put("title", "Consent Analytics Report");
                break;
            case "breach_summary":
                config.put("query", "SELECT id, title, severity, status, detected_at, notified_at, affected_count FROM breaches ORDER BY detected_at DESC LIMIT 200");
                config.put("columns", List.of("id", "title", "severity", "status", "detected_at", "notified_at", "affected_count"));
                config.put("title", "Breach Summary Report");
                break;
            case "audit_trail":
                config.put("query", "SELECT id, action, entity_type, entity_id, performed_by, ip_address, created_at FROM audit_log ORDER BY created_at DESC LIMIT 200");
                config.put("columns", List.of("id", "action", "entity_type", "entity_id", "performed_by", "ip_address", "created_at"));
                config.put("title", "Audit Trail Report");
                break;
            case "policy_report":
                config.put("query", "SELECT id, title, category, status, version, created_by, created_at, review_date FROM policies ORDER BY created_at DESC LIMIT 200");
                config.put("columns", List.of("id", "title", "category", "status", "version", "created_by", "created_at", "review_date"));
                config.put("title", "Policy Engine Report");
                break;
            case "dpia_report":
                config.put("query", "SELECT id, title, status, risk_level, assessor, created_at, completed_at FROM dpias ORDER BY created_at DESC LIMIT 200");
                config.put("columns", List.of("id", "title", "status", "risk_level", "assessor", "created_at", "completed_at"));
                config.put("title", "DPIA Assessment Report");
                break;
            case "rights_report":
                config.put("query", "SELECT id, request_type, principal_name, status, requested_at, resolved_at FROM rights_requests ORDER BY requested_at DESC LIMIT 200");
                config.put("columns", List.of("id", "request_type", "principal_name", "status", "requested_at", "resolved_at"));
                config.put("title", "Data Principal Rights Report");
                break;
            default:
                // Generic: export from any table
                config.put("query", "SELECT * FROM " + reportType.replaceAll("[^a-zA-Z_]", "") + " LIMIT 200");
                config.put("columns", List.of("id"));
                config.put("title", reportType + " Report");
        }
        return config;
    }

    private byte[] createErrorExcel(String title, String error) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Error");
            s.createRow(0).createCell(0).setCellValue("Error generating " + title);
            s.createRow(1).createCell(0).setCellValue(error);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e2) {
            return new byte[0];
        }
    }

    private byte[] createErrorPdf(String title, String error) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document();
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("Error generating " + title + ": " + error));
            doc.close();
            return out.toByteArray();
        } catch (Exception e2) {
            return new byte[0];
        }
    }
}

