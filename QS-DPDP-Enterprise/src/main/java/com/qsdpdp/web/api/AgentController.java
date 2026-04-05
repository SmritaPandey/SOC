package com.qsdpdp.web.api;

import com.qsdpdp.agent.AgentManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Agent Management REST API
 * Endpoints for endpoint sensor agent communication and console management.
 *
 * Agent-facing endpoints (called by deployed sensors):
 * - POST /register — agent registration
 * - POST /heartbeat — periodic health check
 * - POST /events — event batch upload
 * - GET /updates — check for agent updates
 * - GET /policy — fetch assigned policies
 *
 * Console-facing endpoints (called by admin UI):
 * - GET /agents — list all agents
 * - POST /agents/{id}/isolate — isolate endpoint
 * - POST /agents/bulk — bulk actions
 * - GET /dashboard — agent management dashboard
 *
 * @version 3.0.0
 */
@RestController
@RequestMapping("/api/v1/agents")
public class AgentController {

    private final AgentManagementService agentService;

    @Autowired
    public AgentController(AgentManagementService agentService) {
        this.agentService = agentService;
    }

    // ═══ AGENT-FACING ENDPOINTS ═══

    @PostMapping("/register")
    public ResponseEntity<Object> registerAgent(@RequestBody Map<String, Object> registration) {
        try {
            var agent = agentService.registerAgent(registration);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "agentId", agent.agentId,
                    "status", "REGISTERED",
                    "message", "Agent registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Object> heartbeat(
            @RequestHeader("X-Agent-ID") String agentId,
            @RequestBody Map<String, Object> heartbeat) {
        try {
            agentService.processHeartbeat(agentId, heartbeat);
            return ResponseEntity.ok(Map.of("status", "OK", "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/events")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Object> receiveEvents(
            @RequestHeader("X-Agent-ID") String agentId,
            @RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> events = (List<Map<String, Object>>) payload.get("events");
            if (events != null) {
                agentService.processEvents(agentId, events);
            }
            return ResponseEntity.ok(Map.of("received", events != null ? events.size() : 0));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/updates")
    public ResponseEntity<Object> checkUpdates(
            @RequestParam(defaultValue = "3.0.0") String version) {
        // Simulated update check
        return ResponseEntity.ok(Map.of(
                "currentVersion", version,
                "latestVersion", "3.0.0",
                "updateAvailable", false));
    }

    @GetMapping("/policy")
    public ResponseEntity<Object> getAssignedPolicies(
            @RequestHeader(value = "X-Agent-ID", required = false) String agentId) {
        return ResponseEntity.ok(Map.of(
                "policies", agentService.getAllPolicies()));
    }

    // ═══ CONSOLE-FACING ENDPOINTS ═══

    @GetMapping
    public ResponseEntity<Object> listAgents(
            @RequestParam(required = false) String filter) {
        try {
            var agents = switch (filter != null ? filter : "all") {
                case "active" -> agentService.getActiveAgents();
                case "stale" -> agentService.getStaleAgents(5);
                default -> agentService.getAllAgents();
            };
            return ResponseEntity.ok(Map.of(
                    "agentCount", agents.size(),
                    "agents", agents));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{agentId}/isolate")
    public ResponseEntity<Object> isolateAgent(
            @PathVariable String agentId,
            @RequestBody Map<String, String> request) {
        try {
            agentService.isolateAgent(agentId, request.getOrDefault("reason", "Manual isolation"));
            return ResponseEntity.ok(Map.of("agentId", agentId, "status", "ISOLATED"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{agentId}/unisolate")
    public ResponseEntity<Object> unisolateAgent(@PathVariable String agentId) {
        try {
            agentService.unisolateAgent(agentId);
            return ResponseEntity.ok(Map.of("agentId", agentId, "status", "ACTIVE"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{agentId}")
    public ResponseEntity<Object> removeAgent(@PathVariable String agentId) {
        try {
            agentService.removeAgent(agentId);
            return ResponseEntity.ok(Map.of("agentId", agentId, "status", "REMOVED"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bulk")
    public ResponseEntity<Object> bulkAction(@RequestBody Map<String, Object> request) {
        try {
            String action = request.getOrDefault("action", "").toString();
            @SuppressWarnings("unchecked")
            List<String> agentIds = (List<String>) request.get("agentIds");
            Map<String, String> results = agentService.bulkAction(action, agentIds);
            return ResponseEntity.ok(Map.of("results", results));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Object> agentDashboard() {
        try {
            return ResponseEntity.ok(agentService.getDashboard());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> agentStats() {
        try {
            return ResponseEntity.ok(agentService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
