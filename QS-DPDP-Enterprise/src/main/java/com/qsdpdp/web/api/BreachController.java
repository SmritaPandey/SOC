package com.qsdpdp.web.api;

import com.qsdpdp.breach.Breach;
import com.qsdpdp.breach.BreachRequest;
import com.qsdpdp.breach.BreachService;
import com.qsdpdp.breach.BreachStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Breach Management REST API
 * Full CRUD for DPDP Act breach notification (Section 8).
 * Enforces DPBI (72h) and CERT-IN (6h) timelines.
 *
 * @version 3.0.0
 * @since Phase 2
 */
@RestController("breachApiController")
@RequestMapping("/api/v1/breach")
public class BreachController {

    @Autowired(required = false) private BreachService breachService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "breach",
                "status", "UP",
                "initialized", breachService.isInitialized()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> reportBreach(@RequestBody BreachRequest request) {
        try {
            if (request.getTitle() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "title is required"));
            }

            Breach breach = breachService.reportBreach(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "breachId", breach.getId(),
                    "status", breach.getStatus().name(),
                    "message", "Breach reported. DPBI notification due within 72 hours."));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{breachId}")
    public ResponseEntity<Object> getBreach(@PathVariable String breachId) {
        try {
            Breach breach = breachService.getBreachById(breachId);
            if (breach == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(breach);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/overdue")
    public ResponseEntity<Object> getOverdueBreaches() {
        try {
            List<Breach> overdue = breachService.getOverdueBreaches();
            return ResponseEntity.ok(Map.of(
                    "overdueCount", overdue.size(),
                    "breaches", overdue));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Object> listBreaches(
            @RequestParam(required = false) String status) {
        try {
            List<Breach> breaches;
            if (status != null) {
                breaches = breachService.getBreachesByStatus(
                        List.of(BreachStatus.valueOf(status.toUpperCase())));
            } else {
                breaches = breachService.getBreachesByStatus(
                        List.of(BreachStatus.values()));
            }
            return ResponseEntity.ok(Map.of(
                    "totalBreaches", breaches.size(),
                    "breaches", breaches));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        try {
            return ResponseEntity.ok(breachService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
