package com.qsdpdp.web;

import com.qsdpdp.breach.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Breach Management REST Controller
 * DPDP Act 2023 Section 8 — mandatory DPBI notification within 72 hours
 * Full lifecycle: report → contain → notify → resolve
 *
 * @version 1.0.0
 * @since Sprint 4
 */

@RestController
@RequestMapping("/api/breaches")
public class BreachController {

    private static final Logger logger = LoggerFactory.getLogger(BreachController.class);

    @Autowired
    private BreachService breachService;

    // ═══════════════════════════════════════════════════════════
    // REPORT & LIST
    // ═══════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> reportBreach(@RequestBody Map<String, Object> payload) {
        try {
            BreachRequest request = BreachRequest.builder()
                    .title((String) payload.getOrDefault("title", ""))
                    .description((String) payload.getOrDefault("description", ""))
                    .severity(BreachSeverity.valueOf(
                            ((String) payload.getOrDefault("severity", "MEDIUM")).toUpperCase()))
                    .breachType((String) payload.getOrDefault("breachType", ""))
                    .dataCategories((String) payload.getOrDefault("dataCategories", ""))
                    .affectedCount(payload.get("affectedCount") instanceof Number
                            ? ((Number) payload.get("affectedCount")).intValue() : 0)
                    .reportedBy((String) payload.getOrDefault("reportedBy", "admin"))
                    .assignedTo((String) payload.getOrDefault("assignedTo", ""))
                    .build();

            String detectedAt = (String) payload.get("detectedAt");
            if (detectedAt != null && !detectedAt.isBlank()) {
                request.setDetectedAt(LocalDateTime.parse(detectedAt));
            }

            Breach breach = breachService.reportBreach(request);
            return ResponseEntity.ok(Map.of(
                    "status", "reported",
                    "breach", breachToMap(breach),
                    "message", "Breach reported. Reference: " + breach.getReferenceNumber()
                            + ". DPBI notification deadline: 72 hours."));
        } catch (Exception e) {
            logger.error("Failed to report breach", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to report breach: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listBreaches(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String status) {
        try {
            List<Breach> breaches;
            if (status != null && !status.isBlank()) {
                breaches = breachService.getBreachesByStatus(
                        List.of(BreachStatus.valueOf(status.toUpperCase())));
            } else {
                breaches = breachService.getAllBreaches(offset, limit);
            }
            return ResponseEntity.ok(Map.of(
                    "breaches", breaches.stream().map(this::breachToMap).collect(Collectors.toList()),
                    "total", breaches.size()));
        } catch (Exception e) {
            logger.error("Failed to list breaches", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list breaches: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBreach(@PathVariable String id) {
        Breach breach = breachService.getBreachById(id);
        if (breach == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("breach", breachToMap(breach)));
    }

    // ═══════════════════════════════════════════════════════════
    // LIFECYCLE: Contain → Investigate → Notify → Resolve
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            BreachStatus newStatus = BreachStatus.valueOf(
                    payload.getOrDefault("status", "CONTAINED").toUpperCase());
            String actorId = payload.getOrDefault("actorId", "admin");

            Breach breach = breachService.updateStatus(id, newStatus, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "updated",
                    "breach", breachToMap(breach),
                    "newStatus", newStatus.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage(),
                            "validStatuses", Arrays.toString(BreachStatus.values())));
        } catch (Exception e) {
            logger.error("Failed to update breach status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DPBI & CERT-IN NOTIFICATIONS (mandatory per DPDP Act)
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{id}/notify-dpbi")
    public ResponseEntity<?> notifyDPBI(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String reference = payload.getOrDefault("dpbiReference", "");
            String actorId = payload.getOrDefault("actorId", "admin");
            breachService.recordDpbiNotification(id, reference, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "dpbi_notified",
                    "breachId", id,
                    "dpbiReference", reference,
                    "message", "DPBI notification recorded per DPDP Act Section 8"));
        } catch (Exception e) {
            logger.error("Failed to record DPBI notification", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to notify DPBI: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/notify-certin")
    public ResponseEntity<?> notifyCERTIN(@PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String actorId = payload != null ? payload.getOrDefault("actorId", "admin") : "admin";
            breachService.recordCertinNotification(id, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "certin_notified",
                    "breachId", id,
                    "message", "CERT-IN notification recorded (critical breaches: 6h deadline)"));
        } catch (Exception e) {
            logger.error("Failed to record CERT-IN notification", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to notify CERT-IN: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SLA MONITORING
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/open")
    public ResponseEntity<?> getOpenBreaches() {
        try {
            List<Breach> open = breachService.getOpenBreaches();
            return ResponseEntity.ok(Map.of(
                    "breaches", open.stream().map(this::breachToMap).collect(Collectors.toList()),
                    "total", open.size()));
        } catch (Exception e) {
            logger.error("Failed to get open breaches", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get open breaches: " + e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueBreaches() {
        try {
            List<Breach> overdue = breachService.getOverdueBreaches();
            return ResponseEntity.ok(Map.of(
                    "breaches", overdue.stream().map(this::breachToMap).collect(Collectors.toList()),
                    "total", overdue.size(),
                    "slaInfo", "DPBI: 72 hours, CERT-IN: 6 hours (critical)"));
        } catch (Exception e) {
            logger.error("Failed to get overdue breaches", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get overdue: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            BreachStatistics stats = breachService.getStatistics();
            return ResponseEntity.ok(Map.of("statistics", Map.of(
                    "totalBreaches", stats.getTotalBreaches(),
                    "openBreaches", stats.getOpenBreaches(),
                    "criticalBreaches", stats.getCriticalBreaches(),
                    "notifiedBreaches", stats.getNotifiedBreaches(),
                    "totalAffected", stats.getTotalAffected(),
                    "avgResolutionHours", stats.getAvgResolutionHours(),
                    "dpbiComplianceRate", stats.getDpbiComplianceRate())));
        } catch (Exception e) {
            logger.error("Failed to get breach statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> breachToMap(Breach b) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", b.getId());
        map.put("referenceNumber", b.getReferenceNumber());
        map.put("title", b.getTitle());
        map.put("description", b.getDescription());
        map.put("severity", b.getSeverity().name());
        map.put("breachType", b.getBreachType());
        map.put("dataCategories", b.getDataCategories());
        map.put("affectedCount", b.getAffectedCount());
        map.put("status", b.getStatus().name());
        map.put("detectedAt", b.getDetectedAt() != null ? b.getDetectedAt().toString() : null);
        map.put("reportedAt", b.getReportedAt() != null ? b.getReportedAt().toString() : null);
        map.put("containedAt", b.getContainedAt() != null ? b.getContainedAt().toString() : null);
        map.put("reportedBy", b.getReportedBy());
        map.put("assignedTo", b.getAssignedTo());
        map.put("isDpbiOverdue", b.isDpbiOverdue());
        map.put("isCertinOverdue", b.isCertinOverdue());
        map.put("hoursSinceDetection", b.getHoursSinceDetection());
        return map;
    }
}
