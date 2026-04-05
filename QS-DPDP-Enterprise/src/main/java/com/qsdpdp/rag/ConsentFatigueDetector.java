package com.qsdpdp.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Consent Fatigue Detector — AI Phase 7
 * 
 * Detects when users are experiencing consent fatigue:
 * - Rapid consent acceptance (too fast to read)
 * - Blanket consent patterns
 * - Accept-all behavior
 * - Non-engagement with consent text
 * - Time-of-day consent patterns
 * 
 * @version 1.0.0
 * @since Phase 7 — AI Enhancement
 */
@Service
public class ConsentFatigueDetector {

    private static final Logger logger = LoggerFactory.getLogger(ConsentFatigueDetector.class);

    // Thresholds
    private static final int MIN_READ_TIME_MS = 3000;
    private static final int MAX_CONSENTS_PER_HOUR = 10;
    private static final double ACCEPT_ALL_THRESHOLD = 0.95;

    /**
     * Analyze consent interaction for fatigue indicators
     */
    public Map<String, Object> analyze(String principalId, long timeSpentMs,
            boolean accepted, int totalConsentsToday, int acceptedToday,
            String consentTextLength, Map<String, Object> interactionData) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("principalId", principalId);
        List<Map<String, Object>> indicators = new ArrayList<>();
        int fatigueScore = 0;

        // 1. Speed check — too fast to read
        if (timeSpentMs < MIN_READ_TIME_MS && accepted) {
            fatigueScore += 30;
            indicators.add(Map.of("type", "SPEED_CONSENT", "severity", "HIGH",
                    "details", "Consent accepted in " + timeSpentMs + "ms (min: " + MIN_READ_TIME_MS + "ms)",
                    "recommendation", "Add mandatory reading timer or interactive acknowledgment"));
        }

        // 2. Volume check — too many consents
        if (totalConsentsToday > MAX_CONSENTS_PER_HOUR) {
            fatigueScore += 25;
            indicators.add(Map.of("type", "VOLUME_OVERLOAD", "severity", "MEDIUM",
                    "details", totalConsentsToday + " consent requests today (threshold: " + MAX_CONSENTS_PER_HOUR + ")",
                    "recommendation", "Batch consent requests or reduce granularity"));
        }

        // 3. Accept-all pattern
        if (totalConsentsToday > 3) {
            double acceptRate = (double) acceptedToday / totalConsentsToday;
            if (acceptRate >= ACCEPT_ALL_THRESHOLD) {
                fatigueScore += 20;
                indicators.add(Map.of("type", "ACCEPT_ALL_PATTERN", "severity", "MEDIUM",
                        "details", "Accept rate: " + Math.round(acceptRate * 100) + "% (" + acceptedToday + "/" + totalConsentsToday + ")",
                        "recommendation", "Review consent granularity — user may not be making informed choices"));
            }
        }

        // 4. Scroll engagement
        if (interactionData != null) {
            boolean scrolledToBottom = Boolean.TRUE.equals(interactionData.get("scrolledToBottom"));
            if (!scrolledToBottom && accepted) {
                fatigueScore += 15;
                indicators.add(Map.of("type", "NO_SCROLL", "severity", "LOW",
                        "details", "User accepted without scrolling to bottom",
                        "recommendation", "Require scroll-to-bottom before enabling Accept button"));
            }

            boolean expandedDetails = Boolean.TRUE.equals(interactionData.get("expandedDetails"));
            if (!expandedDetails && accepted) {
                fatigueScore += 10;
                indicators.add(Map.of("type", "NO_DETAIL_VIEW", "severity", "LOW",
                        "details", "User did not expand detailed consent text",
                        "recommendation", "Make key terms more visible in summary"));
            }
        }

        int finalScore = Math.min(100, fatigueScore);
        result.put("fatigueScore", finalScore);
        result.put("fatigueLevel", finalScore >= 60 ? "HIGH" : finalScore >= 30 ? "MEDIUM" : "LOW");
        result.put("indicators", indicators);
        result.put("informed", finalScore < 30);
        result.put("recommendations", indicators.stream()
                .map(i -> i.get("recommendation")).filter(Objects::nonNull).toList());
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    /**
     * Get fatigue detection thresholds
     */
    public Map<String, Object> getThresholds() {
        return Map.of("minReadTimeMs", MIN_READ_TIME_MS, "maxConsentsPerHour", MAX_CONSENTS_PER_HOUR,
                "acceptAllThreshold", ACCEPT_ALL_THRESHOLD,
                "indicators", List.of("SPEED_CONSENT", "VOLUME_OVERLOAD", "ACCEPT_ALL_PATTERN",
                        "NO_SCROLL", "NO_DETAIL_VIEW"));
    }
}
