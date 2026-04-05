package com.qsdpdp.web.api;

import com.qsdpdp.siem.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SIEM + SOAR + Threat Intelligence REST API
 * Unified security operations controller.
 *
 * @version 3.0.0
 * @since Phase 2
 */
@RestController("siemApiController")
@RequestMapping("/api/v1/siem")
public class SIEMController {

    @Autowired(required = false) private SIEMService siemService;
    @Autowired(required = false) private ThreatIntelligenceService threatIntelService;
    @Autowired(required = false) private ThreatFeedClient threatFeedClient;
    @Autowired(required = false) private UEBAService uebaService;
    @Autowired(required = false) private MLAnomalyDetector mlDetector;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "siem",
                "status", "UP",
                "siemInitialized", siemService.isInitialized(),
                "threatIntelInitialized", threatIntelService.isInitialized(),
                "mlModelsLoaded", mlDetector.getTrainedModelCount()));
    }

    // ═══ EVENTS ═══

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> ingestEvent(@RequestBody Map<String, Object> request) {
        try {
            SecurityEvent event = new SecurityEvent();
            event.setId(UUID.randomUUID().toString());
            event.setSource((String) request.getOrDefault("type", "GENERIC"));
            event.setSourceIP((String) request.get("sourceIP"));
            event.setDestinationIP((String) request.get("destinationIP"));
            event.setUserId((String) request.get("userId"));
            event.setMessage((String) request.get("message"));
            event.setTimestamp(LocalDateTime.now());

            String severity = (String) request.getOrDefault("severity", "MEDIUM");
            event.setSeverity(EventSeverity.valueOf(severity.toUpperCase()));

            // Enrich with threat intelligence
            SecurityEvent enriched = threatIntelService.enrichEvent(event);

            // Ingest into SIEM
            siemService.ingestEvent(enriched);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "eventId", event.getId(),
                    "enriched", enriched.getMetadata().containsKey("threat_intel_source_ip_risk"),
                    "message", "Event ingested successfully"));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ THREAT INTELLIGENCE ═══

    @PostMapping("/threat-intel/lookup")
    public ResponseEntity<Object> lookupIndicator(@RequestBody Map<String, String> request) {
        try {
            String indicator = request.get("indicator");
            if (indicator == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "indicator is required"));
            }

            ThreatIntelligenceService.ReputationResult result =
                    threatIntelService.checkReputation(indicator);

            return ResponseEntity.ok(Map.of(
                    "indicator", result.indicator,
                    "type", result.type,
                    "matched", result.matched,
                    "riskScore", result.riskScore,
                    "source", result.source != null ? result.source : "",
                    "description", result.description,
                    "isThreat", result.isThreat(),
                    "isCriticalThreat", result.isCriticalThreat()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/threat-intel/bulk-lookup")
    public ResponseEntity<Object> bulkLookup(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> indicators = request.get("indicators");
            if (indicators == null || indicators.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "indicators list is required"));
            }

            Map<String, ThreatIntelligenceService.ReputationResult> results =
                    threatIntelService.checkBulk(indicators);

            return ResponseEntity.ok(Map.of(
                    "totalChecked", results.size(),
                    "results", results));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/threat-intel/refresh-feeds")
    public ResponseEntity<Object> refreshFeeds() {
        try {
            ThreatFeedClient.FeedRefreshSummary summary = threatFeedClient.refreshAllFeeds();

            // Add fetched indicators to the TI service
            for (ThreatFeedClient.ThreatIndicatorDTO dto : summary.spamhausIndicators) {
                threatIntelService.addIndicator(dto.value, dto.type, dto.riskScore,
                        dto.source, dto.description, dto.tags);
            }
            for (ThreatFeedClient.ThreatIndicatorDTO dto : summary.abuseChIndicators) {
                threatIntelService.addIndicator(dto.value, dto.type, dto.riskScore,
                        dto.source, dto.description, dto.tags);
            }
            for (ThreatFeedClient.ThreatIndicatorDTO dto : summary.urlhausIndicators) {
                threatIntelService.addIndicator(dto.value, dto.type, dto.riskScore,
                        dto.source, dto.description, dto.tags);
            }

            return ResponseEntity.ok(Map.of(
                    "success", summary.success,
                    "totalIndicatorsAdded", summary.totalIndicators,
                    "feeds", threatFeedClient.getLastResults()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/threat-intel/stats")
    public ResponseEntity<Object> threatIntelStats() {
        try {
            return ResponseEntity.ok(threatIntelService.getStatistics());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ UEBA ═══

    @GetMapping("/ueba/risk-users")
    public ResponseEntity<Object> getTopRiskUsers(@RequestParam(defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(Map.of(
                    "topRiskUsers", uebaService.getTopRiskUsers(limit),
                    "mlEnabled", mlDetector.isMLEnabled(),
                    "trainedModels", mlDetector.getTrainedModelCount()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══ DASHBOARD ═══

    @GetMapping("/stats")
    public ResponseEntity<Object> getStats() {
        try {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("threatIntel", threatIntelService.getStatistics());
            stats.put("ueba", Map.of(
                    "mlEnabled", mlDetector.isMLEnabled(),
                    "trainedModels", mlDetector.getTrainedModelCount()));
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
