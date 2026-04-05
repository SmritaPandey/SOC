package com.qsdpdp.web;

import com.qsdpdp.pii.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PII Scanner REST Controller (Web)
 * Scans text, files, directories, databases, drives, and network paths
 * Scheduling, cancellation, statistics, pattern listing
 *
 * @version 1.0.0
 * @since Sprint 6
 */

@RestController
@RequestMapping("/api/pii")
public class PIIScannerWebController {

    private static final Logger logger = LoggerFactory.getLogger(PIIScannerWebController.class);

    @Autowired
    private PIIScanner piiScanner;

    // ═══════════════════════════════════════════════════════════
    // SCANNING
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/scan/text")
    public ResponseEntity<?> scanText(@RequestBody Map<String, String> payload) {
        try {
            String text = payload.getOrDefault("text", "");
            String source = payload.getOrDefault("source", "api-text-scan");
            if (text.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
            }
            PIIScanResult result = piiScanner.scanText(text, source);
            return ResponseEntity.ok(scanResultToMap(result));
        } catch (Exception e) {
            logger.error("Text scan failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Text scan failed: " + e.getMessage()));
        }
    }

    @PostMapping("/scan/file")
    public ResponseEntity<?> scanFile(@RequestBody Map<String, String> payload) {
        try {
            String filePath = payload.getOrDefault("filePath", "");
            if (filePath.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "filePath is required"));
            }
            Path path = Paths.get(filePath);
            PIIScanResult result = piiScanner.scanFile(path);
            return ResponseEntity.ok(scanResultToMap(result));
        } catch (Exception e) {
            logger.error("File scan failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "File scan failed: " + e.getMessage()));
        }
    }

    @PostMapping("/scan/directory")
    public ResponseEntity<?> scanDirectory(@RequestBody Map<String, Object> payload) {
        try {
            String dirPath = (String) payload.getOrDefault("directoryPath", "");
            boolean recursive = Boolean.TRUE.equals(payload.get("recursive"));
            if (dirPath.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "directoryPath is required"));
            }
            Path path = Paths.get(dirPath);
            PIIScanResult result = piiScanner.scanDirectory(path, recursive);
            return ResponseEntity.ok(scanResultToMap(result));
        } catch (Exception e) {
            logger.error("Directory scan failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Directory scan failed: " + e.getMessage()));
        }
    }

    @PostMapping("/scan/database")
    public ResponseEntity<?> scanDatabase(@RequestBody Map<String, Object> payload) {
        try {
            String tableName = (String) payload.getOrDefault("tableName", "");
            if (tableName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "tableName is required"));
            }
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) payload.getOrDefault("columns", List.of());
            PIIScanResult result = piiScanner.scanDatabaseTable(tableName,
                    columns.toArray(new String[0]));
            return ResponseEntity.ok(scanResultToMap(result));
        } catch (Exception e) {
            logger.error("Database scan failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database scan failed: " + e.getMessage()));
        }
    }

    @PostMapping("/scan/drive")
    public ResponseEntity<?> scanDrive(@RequestBody Map<String, String> payload) {
        try {
            String driveLetter = payload.getOrDefault("driveLetter", "C:\\");
            PIIScanResult result = piiScanner.scanDrive(driveLetter);
            return ResponseEntity.ok(scanResultToMap(result));
        } catch (Exception e) {
            logger.error("Drive scan failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Drive scan failed: " + e.getMessage()));
        }
    }

    @PostMapping("/scan/network")
    public ResponseEntity<?> scanNetwork(@RequestBody Map<String, String> payload) {
        try {
            String uncPath = payload.getOrDefault("uncPath", "");
            if (uncPath.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "uncPath is required"));
            }
            PIIScanResult result = piiScanner.scanNetworkPath(uncPath);
            return ResponseEntity.ok(scanResultToMap(result));
        } catch (Exception e) {
            logger.error("Network scan failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Network scan failed: " + e.getMessage()));
        }
    }

    @PostMapping("/scan/profile")
    public ResponseEntity<?> scanWithProfile(@RequestBody Map<String, Object> payload) {
        try {
            String profileName = (String) payload.getOrDefault("profileName", "Custom Scan");
            String targetPath = (String) payload.getOrDefault("targetPath", "");
            String targetType = (String) payload.getOrDefault("targetType", "FOLDER");

            ScanProfile profile = new ScanProfile();
            profile.setName(profileName);
            profile.setTargetPath(targetPath);
            profile.setTargetType(ScanTarget.valueOf(targetType));

            PIIScanResult result = piiScanner.scanWithProfile(profile);
            return ResponseEntity.ok(scanResultToMap(result));
        } catch (Exception e) {
            logger.error("Profile scan failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Profile scan failed: " + e.getMessage()));
        }
    }

    @PostMapping("/scan/{id}/cancel")
    public ResponseEntity<?> cancelScan(@PathVariable String id) {
        try {
            piiScanner.cancelScan(id);
            return ResponseEntity.ok(Map.of(
                    "status", "cancelled",
                    "scanId", id,
                    "message", "Scan cancellation requested"));
        } catch (Exception e) {
            logger.error("Failed to cancel scan: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to cancel scan: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SCHEDULING
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/schedule")
    public ResponseEntity<?> scheduleScan(@RequestBody Map<String, Object> payload) {
        try {
            String profileName = (String) payload.getOrDefault("profileName", "Scheduled Scan");
            String targetPath = (String) payload.getOrDefault("targetPath", "");
            String targetType = (String) payload.getOrDefault("targetType", "FOLDER");
            String frequency = (String) payload.getOrDefault("frequency", "DAILY");

            ScanProfile profile = new ScanProfile();
            profile.setName(profileName);
            profile.setTargetPath(targetPath);
            profile.setTargetType(ScanTarget.valueOf(targetType));

            ScanSchedule schedule = new ScanSchedule();
            schedule.setFrequency(ScanSchedule.Frequency.valueOf(frequency));

            piiScanner.scheduleRecurringScan(profile, schedule);
            return ResponseEntity.ok(Map.of(
                    "status", "scheduled",
                    "scheduleId", schedule.getId(),
                    "frequency", frequency,
                    "message", "Recurring scan scheduled"));
        } catch (Exception e) {
            logger.error("Failed to schedule scan", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to schedule scan: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES & STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/scans")
    public ResponseEntity<?> getRecentScans(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<PIIScanResult> scans = piiScanner.getRecentScans(limit);
            List<Map<String, Object>> list = scans.stream()
                    .map(this::scanResultToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("scans", list, "total", list.size()));
        } catch (Exception e) {
            logger.error("Failed to get recent scans", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get recent scans: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            PIIScanner.PIIScanStatistics stats = piiScanner.getStatistics();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalScans", stats.getTotalScans());
            result.put("activeFindings", stats.getActiveFindings());
            result.put("criticalFindings", stats.getCriticalFindings());
            result.put("affectedSources", stats.getAffectedSources());
            return ResponseEntity.ok(Map.of("statistics", result));
        } catch (Exception e) {
            logger.error("Failed to get PII statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/patterns")
    public ResponseEntity<?> getPatterns() {
        try {
            List<Map<String, Object>> patterns = new ArrayList<>();
            for (PIIPattern p : PIIPattern.ALL_PATTERNS) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("type", p.getType().name());
                m.put("confidence", p.getConfidence());
                m.put("requiresValidation", p.isRequiresValidation());
                m.put("description", p.getDescription());
                patterns.add(m);
            }
            return ResponseEntity.ok(Map.of("patterns", patterns, "total", patterns.size()));
        } catch (Exception e) {
            logger.error("Failed to get PII patterns", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get patterns: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> scanResultToMap(PIIScanResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scanId", r.getScanId());
        m.put("scanType", r.getScanType());
        m.put("source", r.getSource());
        m.put("status", r.getStatus());
        m.put("bytesScanned", r.getBytesScanned());
        m.put("filesScanned", r.getFilesScanned());
        m.put("totalFindings", r.getTotalFindings());
        m.put("criticalFindings", r.getCriticalFindings());
        m.put("highFindings", r.getHighFindings());
        m.put("startTime", r.getStartTime() != null ? r.getStartTime().toString() : null);
        m.put("endTime", r.getEndTime() != null ? r.getEndTime().toString() : null);

        // Include finding summaries (not raw values)
        if (r.getFindings() != null && !r.getFindings().isEmpty()) {
            List<Map<String, Object>> findings = new ArrayList<>();
            for (PIIFinding f : r.getFindings()) {
                Map<String, Object> fm = new LinkedHashMap<>();
                fm.put("id", f.getId());
                fm.put("type", f.getType().name());
                fm.put("maskedValue", f.getMaskedValue());
                fm.put("sourcePath", f.getSourcePath());
                fm.put("lineNumber", f.getLineNumber());
                fm.put("confidence", f.getConfidence());
                fm.put("validated", f.isValidated());
                fm.put("riskLevel", f.getRiskLevel());
                findings.add(fm);
            }
            m.put("findings", findings);
        }

        return m;
    }
}
