package com.qsdpdp.web;

import com.qsdpdp.dlp.*;
import com.qsdpdp.pii.PIIType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DLP REST Controller — Data Loss Prevention
 * Policies, incidents, evaluate, USB/email/file checks, statistics
 *
 * @version 1.0.0
 * @since Sprint 6
 */

@RestController
@RequestMapping("/api/dlp")
public class DLPController {

    private static final Logger logger = LoggerFactory.getLogger(DLPController.class);

    @Autowired
    private DLPService dlpService;

    // ═══════════════════════════════════════════════════════════
    // POLICY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/policies")
    public ResponseEntity<?> getPolicies() {
        try {
            List<DLPPolicy> policies = dlpService.getAllPolicies();
            List<Map<String, Object>> list = policies.stream()
                    .map(this::policyToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("policies", list, "total", list.size()));
        } catch (Exception e) {
            logger.error("Failed to get DLP policies", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get policies: " + e.getMessage()));
        }
    }

    @PostMapping("/policies")
    public ResponseEntity<?> createPolicy(@RequestBody Map<String, Object> payload) {
        try {
            DLPPolicy policy = new DLPPolicy();
            policy.setName((String) payload.getOrDefault("name", "New Policy"));
            policy.setDescription((String) payload.getOrDefault("description", ""));
            policy.setEnabled(Boolean.TRUE.equals(payload.get("enabled")));
            policy.setPriority(((Number) payload.getOrDefault("priority", 50)).intValue());
            policy.setPrimaryAction(DLPAction.valueOf(
                    (String) payload.getOrDefault("primaryAction", "ALERT")));

            dlpService.addPolicy(policy);
            return ResponseEntity.ok(Map.of(
                    "status", "created",
                    "policyId", policy.getId(),
                    "message", "DLP policy created: " + policy.getName()));
        } catch (Exception e) {
            logger.error("Failed to create DLP policy", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create policy: " + e.getMessage()));
        }
    }

    @PutMapping("/policies/{id}")
    public ResponseEntity<?> updatePolicy(@PathVariable String id,
            @RequestBody Map<String, Object> payload) {
        try {
            List<DLPPolicy> all = dlpService.getAllPolicies();
            DLPPolicy existing = all.stream()
                    .filter(p -> p.getId().equals(id)).findFirst().orElse(null);
            if (existing == null) return ResponseEntity.notFound().build();

            if (payload.containsKey("name")) existing.setName((String) payload.get("name"));
            if (payload.containsKey("description")) existing.setDescription((String) payload.get("description"));
            if (payload.containsKey("enabled")) existing.setEnabled(Boolean.TRUE.equals(payload.get("enabled")));
            if (payload.containsKey("priority")) existing.setPriority(((Number) payload.get("priority")).intValue());
            if (payload.containsKey("primaryAction"))
                existing.setPrimaryAction(DLPAction.valueOf((String) payload.get("primaryAction")));

            dlpService.updatePolicy(existing);
            return ResponseEntity.ok(Map.of(
                    "status", "updated",
                    "policyId", id,
                    "message", "DLP policy updated"));
        } catch (Exception e) {
            logger.error("Failed to update DLP policy: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update policy: " + e.getMessage()));
        }
    }

    @PostMapping("/policies/{id}/toggle")
    public ResponseEntity<?> togglePolicy(@PathVariable String id,
            @RequestBody Map<String, Object> payload) {
        try {
            boolean enabled = Boolean.TRUE.equals(payload.get("enabled"));
            dlpService.enablePolicy(id, enabled);
            return ResponseEntity.ok(Map.of(
                    "status", enabled ? "enabled" : "disabled",
                    "policyId", id));
        } catch (Exception e) {
            logger.error("Failed to toggle DLP policy: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to toggle policy: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // INCIDENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/incidents")
    public ResponseEntity<?> getIncidents() {
        try {
            List<DLPIncident> incidents = dlpService.getOpenIncidents();
            List<Map<String, Object>> list = incidents.stream()
                    .map(this::incidentToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("incidents", list, "total", list.size()));
        } catch (Exception e) {
            logger.error("Failed to get DLP incidents", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get incidents: " + e.getMessage()));
        }
    }

    @PostMapping("/incidents/{id}/resolve")
    public ResponseEntity<?> resolveIncident(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String resolution = payload.getOrDefault("resolution", "Resolved");
            String resolvedBy = payload.getOrDefault("resolvedBy", "admin");
            dlpService.resolveIncident(id, resolution, resolvedBy);
            return ResponseEntity.ok(Map.of(
                    "status", "resolved",
                    "incidentId", id,
                    "message", "Incident resolved"));
        } catch (Exception e) {
            logger.error("Failed to resolve DLP incident: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to resolve incident: " + e.getMessage()));
        }
    }

    @PostMapping("/incidents/{id}/false-positive")
    public ResponseEntity<?> markFalsePositive(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String markedBy = payload.getOrDefault("markedBy", "admin");
            String notes = payload.getOrDefault("notes", "");
            dlpService.markFalsePositive(id, markedBy, notes);
            return ResponseEntity.ok(Map.of(
                    "status", "false_positive",
                    "incidentId", id,
                    "message", "Incident marked as false positive"));
        } catch (Exception e) {
            logger.error("Failed to mark false positive: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to mark false positive: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DLP EVALUATION
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluate(@RequestBody Map<String, String> payload) {
        try {
            String content = payload.getOrDefault("content", "");
            String user = payload.getOrDefault("user", "admin");
            String destination = payload.getOrDefault("destination", "");
            String channel = payload.getOrDefault("channel", "ENDPOINT");

            DLPEvaluationResult result = dlpService.evaluate(content, user, destination, channel);
            return ResponseEntity.ok(evaluationToMap(result));
        } catch (Exception e) {
            logger.error("Failed to evaluate content", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to evaluate: " + e.getMessage()));
        }
    }

    @PostMapping("/evaluate/file")
    public ResponseEntity<?> evaluateFile(@RequestBody Map<String, String> payload) {
        try {
            String filePath = payload.getOrDefault("filePath", "");
            String user = payload.getOrDefault("user", "admin");
            String destination = payload.getOrDefault("destination", "");

            DLPEvaluationResult result = dlpService.evaluateFileTransfer(filePath, user, destination);
            return ResponseEntity.ok(evaluationToMap(result));
        } catch (Exception e) {
            logger.error("Failed to evaluate file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to evaluate file: " + e.getMessage()));
        }
    }

    @PostMapping("/evaluate/email")
    public ResponseEntity<?> evaluateEmail(@RequestBody Map<String, String> payload) {
        try {
            String subject = payload.getOrDefault("subject", "");
            String body = payload.getOrDefault("body", "");
            String recipient = payload.getOrDefault("recipient", "");
            String user = payload.getOrDefault("user", "admin");

            DLPEvaluationResult result = dlpService.evaluateEmail(subject, body, recipient, user);
            return ResponseEntity.ok(evaluationToMap(result));
        } catch (Exception e) {
            logger.error("Failed to evaluate email", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to evaluate email: " + e.getMessage()));
        }
    }

    @PostMapping("/evaluate/usb")
    public ResponseEntity<?> evaluateUsb(@RequestBody Map<String, String> payload) {
        try {
            String filePath = payload.getOrDefault("filePath", "");
            String user = payload.getOrDefault("user", "admin");
            String deviceId = payload.getOrDefault("deviceId", "");

            DLPEvaluationResult result = dlpService.evaluateUSB(filePath, user, deviceId);
            return ResponseEntity.ok(evaluationToMap(result));
        } catch (Exception e) {
            logger.error("Failed to evaluate USB", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to evaluate USB: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            DLPService.DLPStatistics stats = dlpService.getStatistics();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("totalScanned", stats.getTotalScanned());
            result.put("violationsDetected", stats.getViolationsDetected());
            result.put("blockedActions", stats.getBlockedActions());
            result.put("activePolicies", stats.getActivePolicies());
            result.put("openIncidents", stats.getOpenIncidents());
            result.put("criticalIncidents", stats.getCriticalIncidents());
            result.put("incidentsToday", stats.getIncidentsToday());
            return ResponseEntity.ok(Map.of("statistics", result));
        } catch (Exception e) {
            logger.error("Failed to get DLP statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> policyToMap(DLPPolicy p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("description", p.getDescription());
        m.put("enabled", p.isEnabled());
        m.put("priority", p.getPriority());
        m.put("primaryAction", p.getPrimaryAction() != null ? p.getPrimaryAction().name() : null);
        m.put("protectedDataTypes", p.getProtectedDataTypes() != null
                ? p.getProtectedDataTypes().stream().map(PIIType::name).collect(Collectors.toList())
                : List.of());
        m.put("monitorEndpoint", p.isMonitorEndpoint());
        m.put("monitorNetwork", p.isMonitorNetwork());
        m.put("monitorEmail", p.isMonitorEmail());
        m.put("monitorPrint", p.isMonitorPrint());
        m.put("monitorRemovableMedia", p.isMonitorRemovableMedia());
        m.put("dpdpSection", p.getDpdpSection());
        return m;
    }

    private Map<String, Object> incidentToMap(DLPIncident i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("policyId", i.getPolicyId());
        m.put("policyName", i.getPolicyName());
        m.put("actionTaken", i.getActionTaken() != null ? i.getActionTaken().name() : null);
        m.put("severity", i.getSeverity());
        m.put("sourceUser", i.getSourceUser());
        m.put("sourcePath", i.getSourcePath());
        m.put("destinationType", i.getDestinationType());
        m.put("status", i.getStatus());
        return m;
    }

    private Map<String, Object> evaluationToMap(DLPEvaluationResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("allowed", r.isAllowed());
        m.put("action", r.getAction() != null ? r.getAction().name() : null);
        m.put("matchedPolicy", r.getMatchedPolicy() != null ? r.getMatchedPolicy().getName() : null);
        m.put("matchedDataTypes", r.getMatchedDataTypes() != null
                ? r.getMatchedDataTypes().stream().map(PIIType::name).collect(Collectors.toList())
                : List.of());
        if (r.getIncident() != null) {
            m.put("incidentId", r.getIncident().getId());
        }
        return m;
    }
}
