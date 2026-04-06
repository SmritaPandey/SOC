package com.qshield.soar.controller;

import com.qshield.soar.model.*;
import com.qshield.soar.service.SoarService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/soar")
@CrossOrigin(origins = "*")
public class SoarController {
    private final SoarService soarService;
    public SoarController(SoarService soarService) { this.soarService = soarService; }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() { return ResponseEntity.ok(soarService.getDashboardStats()); }

    @GetMapping("/incidents")
    public ResponseEntity<Page<Incident>> getIncidents(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String status) {
        return ResponseEntity.ok(soarService.getIncidents(page, size, status));
    }

    @PostMapping("/incidents")
    public ResponseEntity<Incident> createIncident(@RequestBody Incident incident) {
        return ResponseEntity.ok(soarService.createIncident(incident));
    }

    @PutMapping("/incidents/{incidentId}/status")
    public ResponseEntity<Incident> updateStatus(@PathVariable String incidentId,
            @RequestParam String status, @RequestParam(required = false) String assignedTo) {
        return ResponseEntity.ok(soarService.updateStatus(incidentId, status, assignedTo));
    }

    @GetMapping("/playbooks")
    public ResponseEntity<List<Playbook>> getPlaybooks() { return ResponseEntity.ok(soarService.getPlaybooks()); }

    @PostMapping("/playbooks")
    public ResponseEntity<Playbook> createPlaybook(@RequestBody Playbook pb) { return ResponseEntity.ok(soarService.createPlaybook(pb)); }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of("product", "QS-SOAR", "version", "1.0.0", "status", "UP"));
    }
}
