package com.qsdpdp.web.api;

import com.qsdpdp.governance.GRCEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * GRC Dashboard Controller — Policy + Risk + Control API
 * 
 * @version 1.0.0
 * @since Phase 6
 */
@RestController
@RequestMapping("/api/grc")
public class GRCDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(GRCDashboardController.class);

    @Autowired(required = false)
    private GRCEngine grcEngine;

    /** GET /api/grc/overview */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        ensureInit(); return ResponseEntity.ok(grcEngine.getOverview());
    }

    /** GET /api/grc/templates — All policy templates */
    @GetMapping("/templates")
    public ResponseEntity<?> templates() {
        ensureInit();
        return ResponseEntity.ok(Map.of("templates", grcEngine.getAllTemplates()));
    }

    /** GET /api/grc/controls — All controls */
    @GetMapping("/controls")
    public ResponseEntity<?> controls() {
        ensureInit();
        return ResponseEntity.ok(Map.of("controls", grcEngine.getAllControls()));
    }

    /** POST /api/grc/policies — Create policy from template */
    @PostMapping("/policies")
    public ResponseEntity<Map<String, Object>> createPolicy(@RequestBody Map<String, String> body) {
        ensureInit();
        return ResponseEntity.ok(grcEngine.createPolicy(
                body.getOrDefault("templateId", ""),
                body.getOrDefault("name", ""),
                body.getOrDefault("owner", "admin"),
                body.getOrDefault("content", "")
        ));
    }

    /** GET /api/grc/policies?status=DRAFT */
    @GetMapping("/policies")
    public ResponseEntity<?> listPolicies(@RequestParam(required = false) String status) {
        ensureInit();
        return ResponseEntity.ok(Map.of("policies", grcEngine.getPolicies(status)));
    }

    /** POST /api/grc/policies/{id}/transition */
    @PostMapping("/policies/{id}/transition")
    public ResponseEntity<Map<String, Object>> transitionPolicy(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        ensureInit();
        return ResponseEntity.ok(grcEngine.transitionPolicy(id,
                body.getOrDefault("status", ""), body.getOrDefault("actor", "admin")));
    }

    /** POST /api/grc/risks — Add risk */
    @PostMapping("/risks")
    public ResponseEntity<Map<String, Object>> addRisk(@RequestBody Map<String, Object> body) {
        ensureInit();
        return ResponseEntity.ok(grcEngine.addRisk(
                (String) body.getOrDefault("name", ""),
                (String) body.getOrDefault("category", ""),
                (String) body.getOrDefault("description", ""),
                body.containsKey("likelihood") ? ((Number) body.get("likelihood")).intValue() : 3,
                body.containsKey("impact") ? ((Number) body.get("impact")).intValue() : 3,
                (String) body.getOrDefault("owner", "admin")
        ));
    }

    /** GET /api/grc/risks */
    @GetMapping("/risks")
    public ResponseEntity<?> getRisks() {
        ensureInit();
        return ResponseEntity.ok(Map.of("risks", grcEngine.getRisks()));
    }

    private void ensureInit() {
        if (grcEngine != null && !grcEngine.isInitialized()) {
            try { grcEngine.initialize(); } catch (Exception e) {
                logger.debug("GRCEngine init skipped: {}", e.getMessage());
            }
        }
    }
}
