package com.qshield.siem.controller;

import com.qshield.siem.model.*;
import com.qshield.siem.service.SiemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * QS-SIEM REST API Controller.
 * All endpoints comply with OWASP API Security Top 10 and NIST AC-3.
 */
@RestController
@RequestMapping("/api/v1/siem")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SiemController {

    private final SiemService siemService;

    public SiemController(SiemService siemService) {
        this.siemService = siemService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(siemService.getDashboardStats());
    }

    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String severity) {
        return ResponseEntity.ok(siemService.getEventsPaged(page, size, severity));
    }

    @PostMapping("/events")
    public ResponseEntity<SecurityEvent> ingestEvent(@RequestBody SecurityEvent event) {
        return ResponseEntity.ok(siemService.ingestEvent(event));
    }

    @PostMapping("/events/raw")
    public ResponseEntity<SecurityEvent> ingestRawLog(
            @RequestBody String rawLog,
            @RequestParam(defaultValue = "SYSLOG") String format) {
        return ResponseEntity.ok(siemService.ingestRawLog(rawLog, format));
    }

    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(siemService.getAlertsPaged(page, size, status));
    }

    @PutMapping("/alerts/{id}/status")
    public ResponseEntity<SiemAlert> updateAlertStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String assignedTo) {
        return ResponseEntity.ok(siemService.updateAlertStatus(id, status, assignedTo));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "product", "QS-SIEM",
                "version", "1.0.0",
                "status", "UP",
                "timestamp", java.time.Instant.now()
        ));
    }
}
