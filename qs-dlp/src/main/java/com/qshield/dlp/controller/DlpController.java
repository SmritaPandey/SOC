package com.qshield.dlp.controller;
import com.qshield.dlp.model.*; import com.qshield.dlp.service.DlpService;
import org.springframework.data.domain.Page; import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; import java.util.*;

@RestController @RequestMapping("/api/v1/dlp") @CrossOrigin(origins = "*")
public class DlpController {
    private final DlpService dlpService;
    public DlpController(DlpService dlpService) { this.dlpService = dlpService; }
    @GetMapping("/dashboard") public ResponseEntity<Map<String,Object>> dashboard() { return ResponseEntity.ok(dlpService.getDashboardStats()); }
    @GetMapping("/incidents") public ResponseEntity<Page<DlpIncident>> incidents(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ResponseEntity.ok(dlpService.getIncidents(page, size)); }
    @PostMapping("/incidents") public ResponseEntity<DlpIncident> create(@RequestBody DlpIncident inc) { return ResponseEntity.ok(dlpService.createIncident(inc)); }
    @PostMapping("/scan") public ResponseEntity<Map<String,List<String>>> scan(@RequestBody String content) { return ResponseEntity.ok(dlpService.scanContent(content)); }
    @GetMapping("/health") public ResponseEntity<Map<String,Object>> health() { return ResponseEntity.ok(Map.of("product","QS-DLP","version","1.0.0","status","UP")); }
}
