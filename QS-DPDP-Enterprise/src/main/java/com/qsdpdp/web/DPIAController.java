package com.qsdpdp.web;

import com.qsdpdp.dpia.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DPIA (Data Protection Impact Assessment) REST Controller
 * DPDP Act 2023 — required before high-risk processing
 * Lifecycle: create → assess risks → submit → approve/reject
 *
 * @version 1.0.0
 * @since Sprint 4
 */

@RestController
@RequestMapping("/api/dpias")
public class DPIAController {

    private static final Logger logger = LoggerFactory.getLogger(DPIAController.class);

    @Autowired
    private DPIAService dpiaService;

    // ═══════════════════════════════════════════════════════════
    // CREATE & LIST
    // ═══════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<?> createDPIA(@RequestBody Map<String, String> payload) {
        try {
            DPIARequest request = new DPIARequest();
            request.setTitle(payload.getOrDefault("title", ""));
            request.setDescription(payload.getOrDefault("description", ""));
            request.setProjectName(payload.getOrDefault("projectName", ""));
            request.setDataTypes(payload.getOrDefault("dataTypes", ""));
            request.setAssessor(payload.getOrDefault("assessor", "admin"));

            DPIA dpia = dpiaService.createDPIA(request);
            return ResponseEntity.ok(Map.of(
                    "status", "created",
                    "dpia", dpiaToMap(dpia),
                    "message", "DPIA created. Add risks and submit for review."));
        } catch (Exception e) {
            logger.error("Failed to create DPIA", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create DPIA: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listDPIAs(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<DPIA> dpias = dpiaService.getAllDPIAs(offset, limit);
            return ResponseEntity.ok(Map.of(
                    "dpias", dpias.stream().map(this::dpiaToMap).collect(Collectors.toList()),
                    "total", dpias.size()));
        } catch (Exception e) {
            logger.error("Failed to list DPIAs", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list DPIAs: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDPIA(@PathVariable String id) {
        DPIA dpia = dpiaService.getDPIAById(id);
        if (dpia == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("dpia", dpiaToMap(dpia)));
    }

    // ═══════════════════════════════════════════════════════════
    // RISK ASSESSMENT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{id}/risks")
    public ResponseEntity<?> assessRisks(@PathVariable String id,
            @RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> riskMaps = (List<Map<String, Object>>) payload.get("risks");
            String actorId = (String) payload.getOrDefault("actorId", "admin");

            List<DPIARisk> risks = new ArrayList<>();
            if (riskMaps != null) {
                for (Map<String, Object> rm : riskMaps) {
                    DPIARisk risk = new DPIARisk();
                    risk.setCategory((String) rm.getOrDefault("category", ""));
                    risk.setDescription((String) rm.getOrDefault("description", ""));
                    risk.setLikelihood(rm.get("likelihood") instanceof Number
                            ? ((Number) rm.get("likelihood")).intValue() : 3);
                    risk.setImpact(rm.get("impact") instanceof Number
                            ? ((Number) rm.get("impact")).intValue() : 3);
                    risk.setMitigation((String) rm.getOrDefault("mitigation", ""));
                    risks.add(risk);
                }
            }

            dpiaService.assessRisk(id, risks, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "risks_assessed",
                    "dpiaId", id,
                    "risksAdded", risks.size()));
        } catch (Exception e) {
            logger.error("Failed to assess risks", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to assess risks: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LIFECYCLE: Submit → Approve / Reject
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitForReview(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String findings = payload.getOrDefault("findings", "");
            String mitigationPlan = payload.getOrDefault("mitigationPlan", "");
            String actorId = payload.getOrDefault("actorId", "admin");

            dpiaService.submitForReview(id, findings, mitigationPlan, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "submitted",
                    "dpiaId", id,
                    "message", "DPIA submitted for review"));
        } catch (Exception e) {
            logger.error("Failed to submit DPIA", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to submit: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveDPIA(@PathVariable String id,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String actorId = payload != null ? payload.getOrDefault("actorId", "admin") : "admin";
            dpiaService.approveDPIA(id, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "approved",
                    "dpiaId", id,
                    "message", "DPIA approved. Processing may proceed."));
        } catch (Exception e) {
            logger.error("Failed to approve DPIA", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to approve: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectDPIA(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.getOrDefault("reason", "");
            String actorId = payload.getOrDefault("actorId", "admin");
            dpiaService.rejectDPIA(id, reason, actorId);
            return ResponseEntity.ok(Map.of(
                    "status", "rejected",
                    "dpiaId", id,
                    "reason", reason));
        } catch (Exception e) {
            logger.error("Failed to reject DPIA", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reject: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REVIEW & STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/requiring-review")
    public ResponseEntity<?> getDPIAsRequiringReview() {
        try {
            List<DPIA> dpias = dpiaService.getDPIAsRequiringReview();
            return ResponseEntity.ok(Map.of(
                    "dpias", dpias.stream().map(this::dpiaToMap).collect(Collectors.toList()),
                    "total", dpias.size()));
        } catch (Exception e) {
            logger.error("Failed to get DPIAs requiring review", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get review list: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            DPIAStatistics stats = dpiaService.getStatistics();
            return ResponseEntity.ok(Map.of("statistics", Map.of(
                    "totalDPIAs", stats.getTotalDPIAs(),
                    "approvedDPIAs", stats.getApprovedDPIAs(),
                    "pendingDPIAs", stats.getPendingDPIAs(),
                    "highRiskDPIAs", stats.getHighRiskDPIAs(),
                    "overdueReviews", stats.getOverdueReviews())));
        } catch (Exception e) {
            logger.error("Failed to get DPIA statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> dpiaToMap(DPIA d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", d.getId());
        map.put("title", d.getTitle());
        map.put("description", d.getDescription());
        map.put("status", d.getStatusString());
        map.put("organizationId", d.getOrganizationId());
        map.put("processingActivity", d.getProcessingActivity());
        map.put("processingPurpose", d.getProcessingPurpose());
        map.put("legalBasis", d.getLegalBasis());
        map.put("dataCategories", d.getDataCategories());
        map.put("estimatedDataSubjects", d.getEstimatedDataSubjects());
        map.put("retentionPeriod", d.getRetentionPeriod());
        map.put("totalRisks", d.getTotalRisks());
        map.put("criticalRisks", d.getCriticalRisks());
        map.put("highRisks", d.getHighRisks());
        map.put("mitigationProgress", d.getMitigationProgress());
        map.put("requiresDPBIConsultation", d.requiresDPBIConsultation());
        map.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
        map.put("nextReviewAt", d.getNextReviewAt() != null ? d.getNextReviewAt().toString() : null);
        return map;
    }
}
