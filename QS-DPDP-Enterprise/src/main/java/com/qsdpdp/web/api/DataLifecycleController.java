package com.qsdpdp.web.api;

import com.qsdpdp.lifecycle.DataLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Lifecycle REST Controller
 * Exposes retention policy, erasure, archival, and statistics APIs
 *
 * @since DPDP Act S.8(7) — Data Retention & Deletion
 */
@RestController
@RequestMapping("/api/lifecycle")
public class DataLifecycleController {

    private static final Logger logger = LoggerFactory.getLogger(DataLifecycleController.class);

    private final DataLifecycleService lifecycleService;

    @Autowired
    public DataLifecycleController(DataLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        if (!lifecycleService.isInitialized()) {
            lifecycleService.initialize();
        }

        DataLifecycleService.LifecycleStatistics stats = lifecycleService.getStatistics();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("activePolicies", stats.getActivePolicies());
        result.put("pendingErasures", stats.getPendingErasures());
        result.put("totalRecordsErased", stats.getTotalRecordsErased());
        result.put("completedArchives", stats.getCompletedArchives());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/policies")
    public ResponseEntity<Map<String, Object>> createPolicy(@RequestBody Map<String, Object> body) {
        if (!lifecycleService.isInitialized()) {
            lifecycleService.initialize();
        }

        String name = (String) body.getOrDefault("name", "Custom Policy");
        String dataCategory = (String) body.getOrDefault("dataCategory", "GENERAL");
        String purpose = (String) body.getOrDefault("purpose", "");
        int retentionDays = body.containsKey("retentionDays") ? ((Number) body.get("retentionDays")).intValue() : 365;
        int archiveAfterDays = body.containsKey("archiveAfterDays") ? ((Number) body.get("archiveAfterDays")).intValue() : 180;
        String legalBasis = (String) body.getOrDefault("legalBasis", "Consent");
        String dpdpReference = (String) body.getOrDefault("dpdpReference", "Section 8(7)");

        DataLifecycleService.RetentionPolicy policy = new DataLifecycleService.RetentionPolicy(
                name, dataCategory, purpose, retentionDays, archiveAfterDays, legalBasis, dpdpReference);

        lifecycleService.createPolicy(policy);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "Retention policy created: " + name);
        result.put("policyId", policy.getId());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/erasure")
    public ResponseEntity<Map<String, Object>> scheduleErasure(@RequestBody Map<String, Object> body) {
        if (!lifecycleService.isInitialized()) {
            lifecycleService.initialize();
        }

        String dataSource = (String) body.getOrDefault("dataSource", "");
        String dataCategory = (String) body.getOrDefault("dataCategory", "");
        String reason = (String) body.getOrDefault("reason", "Data Principal Request");
        int records = body.containsKey("recordsAffected") ? ((Number) body.get("recordsAffected")).intValue() : 0;
        String approvedBy = (String) body.getOrDefault("approvedBy", "system");

        String jobId = lifecycleService.scheduleErasure(dataSource, dataCategory, reason,
                records, LocalDateTime.now().plusHours(1), approvedBy);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", jobId != null);
        result.put("message", jobId != null ? "Erasure scheduled" : "Failed to schedule erasure");
        result.put("jobId", jobId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/erasure/{jobId}/execute")
    public ResponseEntity<Map<String, Object>> executeErasure(@PathVariable String jobId) {
        if (!lifecycleService.isInitialized()) {
            lifecycleService.initialize();
        }

        DataLifecycleService.ErasureResult result = lifecycleService.executeErasure(jobId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        response.put("recordsErased", result.getRecordsErased());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/archive")
    public ResponseEntity<Map<String, Object>> scheduleArchive(@RequestBody Map<String, Object> body) {
        if (!lifecycleService.isInitialized()) {
            lifecycleService.initialize();
        }

        String dataSource = (String) body.getOrDefault("dataSource", "");
        String originalLocation = (String) body.getOrDefault("originalLocation", "");
        String archiveLocation = (String) body.getOrDefault("archiveLocation", "archive/");
        int recordCount = body.containsKey("recordCount") ? ((Number) body.get("recordCount")).intValue() : 0;

        String jobId = lifecycleService.scheduleArchive(dataSource, originalLocation, archiveLocation, recordCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", jobId != null);
        result.put("message", jobId != null ? "Archive scheduled" : "Failed to schedule archive");
        result.put("jobId", jobId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/policies")
    public ResponseEntity<Map<String, Object>> listPolicies() {
        if (!lifecycleService.isInitialized()) {
            lifecycleService.initialize();
        }

        List<Map<String, Object>> policies = lifecycleService.getAllPolicies();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", policies);
        result.put("total", policies.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/erasure-jobs")
    public ResponseEntity<Map<String, Object>> listErasureJobs() {
        if (!lifecycleService.isInitialized()) {
            lifecycleService.initialize();
        }

        List<Map<String, Object>> jobs = lifecycleService.getErasureJobs();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", jobs);
        result.put("total", jobs.size());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/policies/{policyId}")
    public ResponseEntity<Map<String, Object>> deletePolicy(@PathVariable String policyId) {
        if (!lifecycleService.isInitialized()) {
            lifecycleService.initialize();
        }

        boolean deleted = lifecycleService.deletePolicy(policyId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", deleted);
        result.put("message", deleted ? "Policy deactivated" : "Policy not found");

        return ResponseEntity.ok(result);
    }
}
