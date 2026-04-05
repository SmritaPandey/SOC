package com.qsdpdp.web;

import com.qsdpdp.rights.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rights Request (DSR) REST Controller
 * DPDP Act 2023 — mandatory 30-day response deadline
 * Full workflow: submit → acknowledge → assign → complete/reject
 *
 * @version 1.0.0
 * @since Sprint 3
 */

@RestController
@RequestMapping("/api/rights")
public class RightsController {

    private static final Logger logger = LoggerFactory.getLogger(RightsController.class);

    @Autowired
    private RightsService rightsService;

    // ═══════════════════════════════════════════════════════════
    // SUBMIT & LIST
    // ═══════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> submitRequest(@RequestBody Map<String, String> payload) {
        try {
            RightsRequestDTO dto = RightsRequestDTO.builder()
                    .dataPrincipalId(payload.getOrDefault("dataPrincipalId", ""))
                    .requestType(RightType.valueOf(payload.getOrDefault("requestType", "ACCESS").toUpperCase()))
                    .description(payload.getOrDefault("description", ""))
                    .priority(RequestPriority.valueOf(payload.getOrDefault("priority", "NORMAL").toUpperCase()))
                    .actorId(payload.getOrDefault("actorId", "admin"))
                    .build();

            RightsRequest request = rightsService.submitRequest(dto);
            return ResponseEntity.ok(Map.of(
                    "status", "submitted",
                    "request", requestToMap(request),
                    "message", "Rights request submitted. Reference: " + request.getReferenceNumber()
                            + ". Deadline: " + request.getDeadline().toLocalDate()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "validTypes", Arrays.stream(RightType.values())
                            .map(t -> t.name() + " (" + t.getName() + ")")
                            .collect(Collectors.toList())));
        } catch (Exception e) {
            logger.error("Failed to submit rights request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to submit request: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listRequests(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String status) {
        try {
            List<RightsRequest> requests;
            if (status != null && !status.isBlank()) {
                requests = rightsService.getRequestsByStatus(
                        List.of(RequestStatus.valueOf(status.toUpperCase())));
            } else {
                requests = rightsService.getAllRequests(offset, limit);
            }
            return ResponseEntity.ok(Map.of(
                    "requests", requests.stream().map(this::requestToMap).collect(Collectors.toList()),
                    "total", requests.size()));
        } catch (Exception e) {
            logger.error("Failed to list rights requests", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list requests: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequest(@PathVariable String id) {
        RightsRequest request = rightsService.getRequestById(id);
        if (request == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("request", requestToMap(request)));
    }

    // ═══════════════════════════════════════════════════════════
    // WORKFLOW: Acknowledge → Assign → Complete / Reject
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<?> acknowledgeRequest(@PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String actorId = payload != null ? payload.getOrDefault("actorId", "admin") : "admin";
            RightsRequest request = rightsService.acknowledgeRequest(id, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "acknowledged",
                    "request", requestToMap(request),
                    "message", "Request acknowledged. Deadline: " + request.getDeadline().toLocalDate()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to acknowledge request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to acknowledge: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assignRequest(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String assignee = payload.getOrDefault("assignee", "");
            String actorId = payload.getOrDefault("actorId", "admin");
            RightsRequest request = rightsService.assignRequest(id, assignee, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "assigned",
                    "request", requestToMap(request),
                    "message", "Request assigned to " + assignee));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to assign request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to assign: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeRequest(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String response = payload.getOrDefault("response", "");
            String evidencePackage = payload.getOrDefault("evidencePackage", "");
            String actorId = payload.getOrDefault("actorId", "admin");
            RightsRequest request = rightsService.completeRequest(id, response, evidencePackage, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "completed",
                    "request", requestToMap(request),
                    "message", "Rights request completed. Reference: " + request.getReferenceNumber()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to complete request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to complete: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.getOrDefault("reason", "");
            String actorId = payload.getOrDefault("actorId", "admin");
            RightsRequest request = rightsService.rejectRequest(id, reason, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "rejected",
                    "request", requestToMap(request),
                    "message", "Request rejected: " + reason));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to reject request", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reject: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SLA MONITORING & OVERDUE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests() {
        try {
            List<RightsRequest> pending = rightsService.getPendingRequests();
            return ResponseEntity.ok(Map.of(
                    "pending", pending.stream().map(this::requestToMap).collect(Collectors.toList()),
                    "total", pending.size()));
        } catch (Exception e) {
            logger.error("Failed to get pending requests", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get pending: " + e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<?> getOverdueRequests() {
        try {
            List<RightsRequest> overdue = rightsService.getOverdueRequests();
            return ResponseEntity.ok(Map.of(
                    "overdue", overdue.stream().map(this::requestToMap).collect(Collectors.toList()),
                    "total", overdue.size(),
                    "slaDeadline", "30 days per DPDP Act 2023"));
        } catch (Exception e) {
            logger.error("Failed to get overdue requests", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get overdue: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // RIGHT TYPES & STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/types")
    public ResponseEntity<?> getRightTypes() {
        List<Map<String, String>> types = Arrays.stream(RightType.values())
                .map(t -> Map.of("id", t.name(), "name", t.getName(), "description", t.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("rightTypes", types));
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            RightsStatistics stats = rightsService.getStatistics();
            return ResponseEntity.ok(Map.of("statistics", Map.of(
                    "totalRequests", stats.getTotalRequests(),
                    "pendingRequests", stats.getPendingRequests(),
                    "completedRequests", stats.getCompletedRequests(),
                    "overdueRequests", stats.getOverdueRequests(),
                    "complianceRate", stats.getComplianceRate(),
                    "avgResolutionDays", stats.getAvgResolutionDays(),
                    "requestsByType", stats.getRequestsByType())));
        } catch (Exception e) {
            logger.error("Failed to get rights statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> requestToMap(RightsRequest r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("referenceNumber", r.getReferenceNumber());
        map.put("dataPrincipalId", r.getDataPrincipalId());
        map.put("dataPrincipalName", r.getDataPrincipalName());
        map.put("requestType", r.getRequestType().name());
        map.put("requestTypeName", r.getRequestType().getName());
        map.put("description", r.getDescription());
        map.put("status", r.getStatus().name());
        map.put("statusDescription", r.getStatus().getDescription());
        map.put("priority", r.getPriority().name());
        map.put("assignedTo", r.getAssignedTo());
        map.put("receivedAt", r.getReceivedAt() != null ? r.getReceivedAt().toString() : null);
        map.put("acknowledgedAt", r.getAcknowledgedAt() != null ? r.getAcknowledgedAt().toString() : null);
        map.put("deadline", r.getDeadline() != null ? r.getDeadline().toString() : null);
        map.put("completedAt", r.getCompletedAt() != null ? r.getCompletedAt().toString() : null);
        map.put("response", r.getResponse());
        map.put("isOverdue", r.isOverdue());
        map.put("daysRemaining", r.getDaysRemaining());
        map.put("isApproachingDeadline", r.isApproachingDeadline());
        return map;
    }
}
