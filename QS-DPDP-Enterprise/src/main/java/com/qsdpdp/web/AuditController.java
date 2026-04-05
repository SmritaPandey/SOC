package com.qsdpdp.web;

import com.qsdpdp.audit.AuditIntegrityReport;
import com.qsdpdp.audit.AuditLogEntry;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Audit Trail REST Controller
 * Hash-chained immutable audit logging for compliance evidence
 *
 * @version 1.0.0
 * @since Sprint 5
 */

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    @Autowired
    private AuditService auditService;

    // ═══════════════════════════════════════════════════════════
    // AUDIT LOG ENTRIES
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/entries")
    public ResponseEntity<?> getRecentEntries(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<AuditLogEntry> entries = auditService.getRecentEntries(limit);
            List<Map<String, Object>> entryList = entries.stream().map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", e.getId());
                m.put("sequenceNumber", e.getSequenceNumber());
                m.put("timestamp", e.getTimestamp());
                m.put("eventType", e.getEventType());
                m.put("module", e.getModule());
                m.put("action", e.getAction());
                m.put("actor", e.getActor());
                m.put("details", e.getDetails());
                return m;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "entries", entryList,
                    "total", entryList.size()));
        } catch (Exception e) {
            logger.error("Failed to get audit entries", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get entries: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // INTEGRITY VERIFICATION
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/integrity")
    public ResponseEntity<?> verifyIntegrity() {
        try {
            AuditIntegrityReport report = auditService.verifyIntegrity();
            return ResponseEntity.ok(Map.of(
                    "integrity", Map.of(
                            "valid", report.isValid(),
                            "totalEntries", report.getTotalEntries(),
                            "validEntries", report.getValidEntries(),
                            "invalidEntries", report.getInvalidEntries(),
                            "firstInvalidId", report.getFirstInvalidId() != null
                                    ? report.getFirstInvalidId() : "none"),
                    "message", report.isValid()
                            ? "Audit chain integrity verified. All entries valid."
                            : "INTEGRITY VIOLATION DETECTED! " + report.getInvalidEntries()
                                    + " invalid entries found."));
        } catch (Exception e) {
            logger.error("Failed to verify audit integrity", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to verify integrity: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LOG MANUAL ENTRY
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/log")
    public ResponseEntity<?> logEntry(@RequestBody Map<String, String> payload) {
        try {
            String action = payload.getOrDefault("action", "");
            String module = payload.getOrDefault("module", "");
            String actor = payload.getOrDefault("actor", "admin");
            String details = payload.getOrDefault("details", "");

            auditService.log(action, module, actor, details);
            return ResponseEntity.ok(Map.of(
                    "status", "logged",
                    "action", action,
                    "module", module,
                    "message", "Audit entry queued for hash-chained persistence"));
        } catch (Exception e) {
            logger.error("Failed to log audit entry", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to log: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of(
                "initialized", auditService.isInitialized(),
                "currentSequence", auditService.getSequenceNumber(),
                "hashChainEnabled", true));
    }
}
