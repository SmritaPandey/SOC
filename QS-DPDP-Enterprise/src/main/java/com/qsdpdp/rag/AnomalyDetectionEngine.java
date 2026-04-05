package com.qsdpdp.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Anomaly Detection Engine — Explainable AI Phase 7
 * 
 * Multi-signal anomaly detection for data processing:
 * - Volume anomalies (unusual data access patterns)
 * - Temporal anomalies (off-hours processing)
 * - Scope anomalies (cross-purpose data usage)
 * - Geographic anomalies (unusual access locations)
 * - Velocity anomalies (sudden spikes)
 * 
 * All outputs include explainability metadata.
 * 
 * @version 1.0.0
 * @since Phase 7 — AI Enhancement
 */
@Service
public class AnomalyDetectionEngine {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionEngine.class);

    /**
     * Detect anomalies across multiple signals
     */
    public Map<String, Object> detect(Map<String, Object> signals) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> anomalies = new ArrayList<>();
        int overallScore = 0;

        // Volume anomaly
        if (signals.containsKey("currentVolume") && signals.containsKey("avgVolume")) {
            double current = ((Number) signals.get("currentVolume")).doubleValue();
            double avg = ((Number) signals.get("avgVolume")).doubleValue();
            if (avg > 0 && current > avg * 3) {
                int score = Math.min(40, (int) ((current / avg - 1) * 10));
                overallScore += score;
                anomalies.add(Map.of("type", "VOLUME_SPIKE", "score", score,
                        "explanation", String.format("Current volume (%.0f) is %.1fx the average (%.0f)", current, current / avg, avg),
                        "action", "Investigate source of increased data access"));
            }
        }

        // Temporal anomaly
        if (signals.containsKey("accessHour")) {
            int hour = ((Number) signals.get("accessHour")).intValue();
            if (hour < 6 || hour > 22) {
                overallScore += 20;
                anomalies.add(Map.of("type", "OFF_HOURS_ACCESS", "score", 20,
                        "explanation", "Data accessed at hour " + hour + " (outside normal hours 06:00-22:00)",
                        "action", "Verify if legitimate — may indicate compromised credentials"));
            }
        }

        // Scope anomaly
        if (signals.containsKey("requestedCategory") && signals.containsKey("allowedCategories")) {
            String requested = (String) signals.get("requestedCategory");
            @SuppressWarnings("unchecked")
            List<String> allowed = (List<String>) signals.get("allowedCategories");
            if (allowed != null && !allowed.contains(requested)) {
                overallScore += 30;
                anomalies.add(Map.of("type", "SCOPE_VIOLATION", "score", 30,
                        "explanation", "Category '" + requested + "' not in allowed set: " + allowed,
                        "action", "Block access and alert DPO — potential purpose violation"));
            }
        }

        // Geographic anomaly
        if (signals.containsKey("accessCountry") && signals.containsKey("registeredCountry")) {
            String access = (String) signals.get("accessCountry");
            String registered = (String) signals.get("registeredCountry");
            if (!access.equalsIgnoreCase(registered)) {
                overallScore += 25;
                anomalies.add(Map.of("type", "GEO_ANOMALY", "score", 25,
                        "explanation", "Access from " + access + " but registered in " + registered,
                        "action", "Check for cross-border transfer compliance (DPDP S.16)"));
            }
        }

        // Velocity anomaly
        if (signals.containsKey("requestsPerMinute")) {
            int rpm = ((Number) signals.get("requestsPerMinute")).intValue();
            if (rpm > 100) {
                overallScore += 25;
                anomalies.add(Map.of("type", "VELOCITY_SPIKE", "score", 25,
                        "explanation", "Request rate: " + rpm + "/min (threshold: 100/min)",
                        "action", "Possible automated scraping — throttle and investigate"));
            }
        }

        int finalScore = Math.min(100, overallScore);
        result.put("anomalyScore", finalScore);
        result.put("riskLevel", finalScore >= 70 ? "CRITICAL" : finalScore >= 50 ? "HIGH" : finalScore >= 25 ? "MEDIUM" : "LOW");
        result.put("anomalies", anomalies);
        result.put("anomalyCount", anomalies.size());
        result.put("explainability", Map.of(
                "model", "Rule-based multi-signal anomaly detection",
                "version", "1.0.0",
                "signals", signals.keySet(),
                "reasoning", "Each signal evaluated independently with weighted scoring"
        ));
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * Get supported detection signals
     */
    public Map<String, Object> getSignals() {
        return Map.of("signals", List.of(
            Map.of("name", "VOLUME_SPIKE", "inputs", List.of("currentVolume", "avgVolume"), "maxScore", 40),
            Map.of("name", "OFF_HOURS_ACCESS", "inputs", List.of("accessHour"), "maxScore", 20),
            Map.of("name", "SCOPE_VIOLATION", "inputs", List.of("requestedCategory", "allowedCategories"), "maxScore", 30),
            Map.of("name", "GEO_ANOMALY", "inputs", List.of("accessCountry", "registeredCountry"), "maxScore", 25),
            Map.of("name", "VELOCITY_SPIKE", "inputs", List.of("requestsPerMinute"), "maxScore", 25)
        ), "maxTotalScore", 100);
    }
}
