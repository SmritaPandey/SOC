package com.qsdpdp.web.api;

import com.qsdpdp.dpia.*;
import com.qsdpdp.gap.GapAnalysisService;
import com.qsdpdp.policy.*;
import com.qsdpdp.rights.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Policy Engine, DPIA, Rights Management, and Gap Analysis REST API
 * Full CRUD for DPDP compliance governance modules.
 *
 * @version 3.0.0
 * @since Phase 2/3
 */
@RestController("governanceApiController")
@RequestMapping("/api/v1")
public class GovernanceController {

    @Autowired(required = false) private PolicyService policyService;
    @Autowired(required = false) private DPIAService dpiaService;
    @Autowired(required = false) private RightsService rightsService;
    @Autowired(required = false) private GapAnalysisService gapService;

    // ════════════════════════════════════════════════════════
    // POLICY ENGINE
    // ════════════════════════════════════════════════════════

    @PostMapping("/policies")
    public ResponseEntity<Object> createPolicy(@RequestBody PolicyRequest request) {
        try {
            Policy policy = policyService.createPolicy(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "policyId", policy.getId(),
                    "status", policy.getStatus(),
                    "message", "Policy created successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/policies/{policyId}")
    public ResponseEntity<Object> getPolicy(@PathVariable String policyId) {
        try {
            Policy policy = policyService.getPolicyById(policyId);
            if (policy == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(policy);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/policies")
    public ResponseEntity<Object> listPolicies(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        try {
            List<Policy> policies;
            if (status != null) {
                policies = policyService.getPoliciesByStatus(status);
            } else if (category != null) {
                policies = policyService.getPoliciesByCategory(category);
            } else {
                policies = policyService.getAllPolicies(offset, limit);
            }
            return ResponseEntity.ok(Map.of(
                    "policyCount", policies.size(),
                    "policies", policies));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/policies/active")
    public ResponseEntity<Object> getActivePolicies() {
        try {
            return ResponseEntity.ok(Map.of(
                    "policies", policyService.getActivePolicies()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/policies/review-required")
    public ResponseEntity<Object> getReviewRequired() {
        try {
            return ResponseEntity.ok(Map.of(
                    "policies", policyService.getPoliciesRequiringReview()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/policies/{policyId}/versions")
    public ResponseEntity<Object> getPolicyVersions(@PathVariable String policyId) {
        try {
            return ResponseEntity.ok(Map.of(
                    "versions", policyService.getVersionHistory(policyId)));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/policies/stats")
    public ResponseEntity<Object> getPolicyStats() {
        try {
            return ResponseEntity.ok(policyService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════
    // DPIA (Data Protection Impact Assessment)
    // ════════════════════════════════════════════════════════

    @PostMapping("/dpia")
    public ResponseEntity<Object> createDPIA(@RequestBody DPIARequest request) {
        try {
            DPIA dpia = dpiaService.createDPIA(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "dpiaId", dpia.getId(),
                    "status", dpia.getStatus(),
                    "message", "DPIA created successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dpia/{dpiaId}")
    public ResponseEntity<Object> getDPIA(@PathVariable String dpiaId) {
        try {
            DPIA dpia = dpiaService.getDPIAById(dpiaId);
            if (dpia == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(dpia);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dpia")
    public ResponseEntity<Object> listDPIAs(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<DPIA> dpias = dpiaService.getAllDPIAs(offset, limit);
            return ResponseEntity.ok(Map.of(
                    "dpiaCount", dpias.size(),
                    "dpias", dpias));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dpia/review-required")
    public ResponseEntity<Object> getDPIAsReviewRequired() {
        try {
            return ResponseEntity.ok(Map.of(
                    "dpias", dpiaService.getDPIAsRequiringReview()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dpia/stats")
    public ResponseEntity<Object> getDPIAStats() {
        try {
            return ResponseEntity.ok(dpiaService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════
    // RIGHTS MANAGEMENT (Section 11-14, DPDP Act)
    // ════════════════════════════════════════════════════════

    @PostMapping("/rights/requests")
    public ResponseEntity<Object> submitRightsRequest(@RequestBody RightsRequestDTO dto) {
        try {
            RightsRequest request = rightsService.submitRequest(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "requestId", request.getId(),
                    "status", request.getStatus(),
                    "message", "Rights request submitted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rights/requests/{requestId}")
    public ResponseEntity<Object> getRightsRequest(@PathVariable String requestId) {
        try {
            RightsRequest request = rightsService.getRequestById(requestId);
            if (request == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rights/requests")
    public ResponseEntity<Object> listRightsRequests(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<RightsRequest> requests = rightsService.getAllRequests(offset, limit);
            return ResponseEntity.ok(Map.of(
                    "requestCount", requests.size(),
                    "requests", requests));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rights/requests/pending")
    public ResponseEntity<Object> getPendingRequests() {
        try {
            return ResponseEntity.ok(Map.of(
                    "requests", rightsService.getPendingRequests()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rights/requests/overdue")
    public ResponseEntity<Object> getOverdueRequests() {
        try {
            return ResponseEntity.ok(Map.of(
                    "requests", rightsService.getOverdueRequests()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rights/stats")
    public ResponseEntity<Object> getRightsStats() {
        try {
            return ResponseEntity.ok(rightsService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════
    // GAP ANALYSIS
    // ════════════════════════════════════════════════════════

    @GetMapping("/gap/stats")
    public ResponseEntity<Object> getGapStats() {
        try {
            return ResponseEntity.ok(gapService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
