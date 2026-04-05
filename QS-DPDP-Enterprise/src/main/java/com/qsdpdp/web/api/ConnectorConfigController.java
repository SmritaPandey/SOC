package com.qsdpdp.web.api;

import com.qsdpdp.integration.adapters.SectorAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Connector Config Controller — Sector Adapter Management API
 * 
 * REST endpoints for managing sector-specific integration adapters:
 * - GET /api/connectors — List all adapters
 * - GET /api/connectors/sector/{sector} — Filter by sector
 * - POST /api/connectors/{id}/enable — Enable with config
 * - POST /api/connectors/{id}/disable — Disable
 * - POST /api/connectors/{id}/test — Test connectivity
 * - POST /api/connectors/{id}/send — Send data
 * - GET /api/connectors/overview — Dashboard summary
 * 
 * @version 1.0.0
 * @since Phase 2 — API Integration
 */
@RestController
@RequestMapping("/api/connectors")
public class ConnectorConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorConfigController.class);

    @Autowired(required = false)
    private SectorAdapterFactory adapterFactory;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        ensureInitialized();
        List<Map<String, Object>> list = adapterFactory.getAllAdapters().stream()
                .map(SectorAdapterFactory.SectorAdapter::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "total", list.size(),
                "adapters", list
        ));
    }

    @GetMapping("/sector/{sector}")
    public ResponseEntity<Map<String, Object>> bySector(@PathVariable String sector) {
        ensureInitialized();
        List<Map<String, Object>> list = adapterFactory.getAdaptersBySector(sector).stream()
                .map(SectorAdapterFactory.SectorAdapter::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "sector", sector.toUpperCase(),
                "total", list.size(),
                "adapters", list
        ));
    }

    @GetMapping("/enabled")
    public ResponseEntity<Map<String, Object>> enabledAdapters() {
        ensureInitialized();
        List<Map<String, Object>> list = adapterFactory.getEnabledAdapters().stream()
                .map(SectorAdapterFactory.SectorAdapter::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("total", list.size(), "adapters", list));
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        ensureInitialized();
        return ResponseEntity.ok(adapterFactory.getOverview());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAdapter(@PathVariable String id) {
        ensureInitialized();
        SectorAdapterFactory.SectorAdapter adapter = adapterFactory.getAdapter(id);
        if (adapter == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(adapter.toMap());
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<Map<String, Object>> enableAdapter(
            @PathVariable String id,
            @RequestBody Map<String, String> config) {
        ensureInitialized();
        return ResponseEntity.ok(adapterFactory.enableAdapter(id, config));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<Map<String, Object>> disableAdapter(@PathVariable String id) {
        ensureInitialized();
        return ResponseEntity.ok(adapterFactory.disableAdapter(id));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testAdapter(@PathVariable String id) {
        ensureInitialized();
        return ResponseEntity.ok(adapterFactory.testAdapter(id));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Map<String, Object>> sendData(
            @PathVariable String id,
            @RequestBody Map<String, Object> data) {
        ensureInitialized();
        return ResponseEntity.ok(adapterFactory.sendData(id, data));
    }

    private void ensureInitialized() {
        if (adapterFactory != null && !adapterFactory.isInitialized()) {
            try { adapterFactory.initialize(); } catch (Exception e) {
                logger.debug("AdapterFactory init skipped: {}", e.getMessage());
            }
        }
    }
}
