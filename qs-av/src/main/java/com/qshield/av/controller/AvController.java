package com.qshield.av.controller;
import com.qshield.av.model.*; import com.qshield.av.service.AvService;
import org.springframework.data.domain.Page; import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; import java.util.*;

@RestController @RequestMapping("/api/v1/av") @CrossOrigin(origins = "*")
public class AvController {
    private final AvService avService;
    public AvController(AvService avService) { this.avService = avService; }
    @GetMapping("/dashboard") public ResponseEntity<Map<String,Object>> dashboard() { return ResponseEntity.ok(avService.getDashboardStats()); }
    @GetMapping("/scans") public ResponseEntity<Page<ScanResult>> scans(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ResponseEntity.ok(avService.getResults(page, size)); }
    @PostMapping("/scans") public ResponseEntity<ScanResult> record(@RequestBody ScanResult r) { return ResponseEntity.ok(avService.recordScan(r)); }
    @GetMapping("/health") public ResponseEntity<Map<String,Object>> health() { return ResponseEntity.ok(Map.of("product","QS-AV","version","1.0.0","status","UP")); }
}
