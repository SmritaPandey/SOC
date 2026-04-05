package com.qsdpdp.web.api;

import com.qsdpdp.siem.*;
import com.qsdpdp.siem.EDRService.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * EDR (Endpoint Detection & Response) + XDR REST API
 * Endpoint agent management, threat detection, isolation, and cross-layer correlation.
 *
 * @version 3.0.0
 * @since Phase 2/3
 */
@RestController("edrXdrApiController")
@RequestMapping("/api/v1/edr")
public class EDRXDRController {

    @Autowired(required = false) private EDRService edrService;
    @Autowired(required = false) private XDRService xdrService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "edr-xdr",
                "edrInitialized", edrService.isInitialized(),
                "xdrInitialized", xdrService.isInitialized(),
                "status", "UP"));
    }

    // ═══ EDR AGENTS ═══

    @GetMapping("/agents")
    public ResponseEntity<Object> listAgents(@RequestParam(required = false) String filter) {
        try {
            List<EndpointAgent> agents = switch (filter != null ? filter : "all") {
                case "active" -> edrService.getActiveAgents();
                case "isolated" -> edrService.getIsolatedAgents();
                default -> edrService.getAllAgents();
            };
            return ResponseEntity.ok(Map.of(
                    "agentCount", agents.size(),
                    "filter", filter != null ? filter : "all",
                    "agents", agents));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ EDR THREATS ═══

    @GetMapping("/threats")
    public ResponseEntity<Object> listThreats(@RequestParam(required = false) String agentId) {
        try {
            List<EDRThreat> threats;
            if (agentId != null) {
                threats = edrService.getThreatsByAgent(agentId);
            } else {
                threats = edrService.getActiveThreats();
            }
            return ResponseEntity.ok(Map.of(
                    "threatCount", threats.size(),
                    "threats", threats));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ EDR ACTIONS ═══

    @PostMapping("/agents/{agentId}/isolate")
    public ResponseEntity<Map<String, Object>> isolateEndpoint(
            @PathVariable String agentId,
            @RequestBody Map<String, String> request) {
        try {
            String reason = request.getOrDefault("reason", "Manual isolation");
            String isolatedBy = request.getOrDefault("isolatedBy", "admin");
            edrService.isolateEndpoint(agentId, reason, isolatedBy);
            return ResponseEntity.ok(Map.of(
                    "agentId", agentId,
                    "action", "ISOLATE",
                    "status", "ISOLATED",
                    "message", "Endpoint isolated: " + reason));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ EDR POLICIES ═══

    @GetMapping("/policies")
    public ResponseEntity<Object> listPolicies() {
        try {
            return ResponseEntity.ok(Map.of(
                    "policies", edrService.getAllPolicies()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ EDR STATS & DASHBOARD ═══

    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        try {
            return ResponseEntity.ok(edrService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Object> getDashboard() {
        try {
            return ResponseEntity.ok(edrService.getDashboard());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ XDR INCIDENTS ═══

    @GetMapping("/xdr/incidents")
    public ResponseEntity<Object> xdrIncidents(@RequestParam(required = false) String filter) {
        try {
            List<XDRService.XDRIncident> incidents = "open".equals(filter)
                    ? xdrService.getOpenIncidents()
                    : xdrService.getIncidents();
            return ResponseEntity.ok(Map.of(
                    "incidentCount", incidents.size(),
                    "incidents", incidents));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/xdr/policies")
    public ResponseEntity<Object> xdrPolicies() {
        try {
            return ResponseEntity.ok(Map.of(
                    "correlationPolicies", xdrService.getCorrelationPolicies()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/xdr/stats")
    public ResponseEntity<Object> xdrStats() {
        try {
            return ResponseEntity.ok(xdrService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
