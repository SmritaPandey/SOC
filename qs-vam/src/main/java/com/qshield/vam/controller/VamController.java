package com.qshield.vam.controller;
import com.qshield.vam.model.*; import com.qshield.vam.service.VamService;
import org.springframework.data.domain.Page; import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; import java.util.*;

@RestController @RequestMapping("/api/v1/vam") @CrossOrigin(origins = "*")
public class VamController {
    private final VamService vamService;
    public VamController(VamService vamService) { this.vamService = vamService; }
    @GetMapping("/dashboard") public ResponseEntity<Map<String,Object>> dashboard() { return ResponseEntity.ok(vamService.getDashboardStats()); }
    @GetMapping("/vulnerabilities") public ResponseEntity<Page<Vulnerability>> vulns(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ResponseEntity.ok(vamService.getVulnerabilities(page, size)); }
    @PostMapping("/vulnerabilities") public ResponseEntity<Vulnerability> report(@RequestBody Vulnerability v) { return ResponseEntity.ok(vamService.reportVulnerability(v)); }
    @PutMapping("/vulnerabilities/{id}/status") public ResponseEntity<Vulnerability> update(@PathVariable Long id, @RequestParam String status) { return ResponseEntity.ok(vamService.updateStatus(id, status)); }
    @GetMapping("/health") public ResponseEntity<Map<String,Object>> health() { return ResponseEntity.ok(Map.of("product","QS-VAM","version","1.0.0","status","UP")); }
}
