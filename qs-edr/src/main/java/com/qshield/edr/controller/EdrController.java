package com.qshield.edr.controller;
import com.qshield.edr.model.*;
import com.qshield.edr.service.EdrService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/edr") @CrossOrigin(origins = "*")
public class EdrController {
    private final EdrService edrService;
    public EdrController(EdrService edrService) { this.edrService = edrService; }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() { return ResponseEntity.ok(edrService.getDashboardStats()); }
    @GetMapping("/endpoints")
    public ResponseEntity<Page<Endpoint>> endpoints(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ResponseEntity.ok(edrService.getEndpoints(page, size)); }
    @PostMapping("/endpoints")
    public ResponseEntity<Endpoint> register(@RequestBody Endpoint ep) { return ResponseEntity.ok(edrService.registerEndpoint(ep)); }
    @PostMapping("/endpoints/{id}/isolate")
    public ResponseEntity<Endpoint> isolate(@PathVariable Long id) { return ResponseEntity.ok(edrService.isolateEndpoint(id)); }
    @GetMapping("/events")
    public ResponseEntity<Page<EndpointEvent>> events(@RequestParam(required=false) String hostname, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) { return ResponseEntity.ok(edrService.getEvents(hostname, page, size)); }
    @PostMapping("/events")
    public ResponseEntity<EndpointEvent> ingest(@RequestBody EndpointEvent event) { return ResponseEntity.ok(edrService.ingestEvent(event)); }
    @GetMapping("/health")
    public ResponseEntity<Map<String,Object>> health() { return ResponseEntity.ok(Map.of("product","QS-EDR","version","1.0.0","status","UP")); }
}
