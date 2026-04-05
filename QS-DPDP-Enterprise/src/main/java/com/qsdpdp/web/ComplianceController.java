package com.qsdpdp.web;

import com.qsdpdp.core.ComplianceEngine;
import com.qsdpdp.core.ComplianceScore;
import com.qsdpdp.core.ComplianceGap;
import com.qsdpdp.core.GapSeverity;
import com.qsdpdp.rag.RAGEvaluator;
import com.qsdpdp.rag.RAGStatus;
import com.qsdpdp.rag.ModuleScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Compliance Engine REST Controller
 * Overall scoring, RAG dashboard, gap listing, risk prediction, and compliance trend
 *
 * @version 1.0.0
 * @since Sprint 2
 */

@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceController.class);

    @Autowired
    private ComplianceEngine complianceEngine;

    @Autowired
    private RAGEvaluator ragEvaluator;

    // ═══════════════════════════════════════════════════════════
    // OVERALL COMPLIANCE SCORE
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/score")
    public ResponseEntity<?> getComplianceScore() {
        try {
            ComplianceScore score = complianceEngine.calculateOverallScore();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("overallScore", score.getOverallScore());
            result.put("ragStatus", score.getOverallStatus().name());
            result.put("calculatedAt", score.getCalculatedAt().toString());

            // Module breakdowns
            Map<String, Object> modules = new LinkedHashMap<>();
            score.getModuleScores().forEach((name, ms) -> {
                Map<String, Object> mod = new LinkedHashMap<>();
                mod.put("score", ms.getScore());
                mod.put("ragStatus", ms.getStatus().name());
                mod.put("details", ms.getDetails());
                modules.put(name, mod);
            });
            result.put("modules", modules);
            result.put("totalModules", modules.size());
            result.put("greenModules", score.getGreenModuleCount());
            result.put("amberModules", score.getAmberModuleCount());
            result.put("redModules", score.getRedModuleCount());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to calculate compliance score", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to calculate compliance score: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // RAG DASHBOARD
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/rag")
    public ResponseEntity<?> getRAGDashboard() {
        try {
            String[] moduleNames = {
                    "consent", "breach", "rights", "dpia",
                    "policy", "security", "audit"
            };

            Map<String, Object> ragDashboard = new LinkedHashMap<>();
            int greenCount = 0, amberCount = 0, redCount = 0;
            double totalScore = 0;

            for (String module : moduleNames) {
                ModuleScore ms = ragEvaluator.evaluateModule(module);
                Map<String, Object> moduleData = new LinkedHashMap<>();
                moduleData.put("score", ms.getScore());
                moduleData.put("ragStatus", ms.getStatus().name());
                moduleData.put("details", ms.getDetails());
                ragDashboard.put(module, moduleData);

                totalScore += ms.getScore();
                switch (ms.getStatus()) {
                    case GREEN -> greenCount++;
                    case AMBER -> amberCount++;
                    case RED -> redCount++;
                }
            }

            double avgScore = totalScore / moduleNames.length;
            RAGStatus overall = ragEvaluator.determineRAGStatus(avgScore);

            return ResponseEntity.ok(Map.of(
                    "modules", ragDashboard,
                    "summary", Map.of(
                            "overallRAG", overall.name(),
                            "averageScore", Math.round(avgScore * 100.0) / 100.0,
                            "greenModules", greenCount,
                            "amberModules", amberCount,
                            "redModules", redCount,
                            "totalModules", moduleNames.length)));
        } catch (Exception e) {
            logger.error("Failed to generate RAG dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate RAG dashboard: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // COMPLIANCE GAPS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/gaps")
    public ResponseEntity<?> getComplianceGaps() {
        try {
            List<ComplianceGap> gaps = complianceEngine.identifyGaps();

            // Map to JSON-friendly format
            List<Map<String, Object>> gapList = new ArrayList<>();
            long critical = 0, high = 0, medium = 0, low = 0;

            for (ComplianceGap gap : gaps) {
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("gapId", gap.getGapId());
                g.put("title", gap.getTitle());
                g.put("description", gap.getDescription());
                g.put("module", gap.getModule());
                g.put("controlId", gap.getControlId());
                g.put("severity", gap.getSeverity().name());
                g.put("recommendation", gap.getRecommendation());
                gapList.add(g);

                switch (gap.getSeverity()) {
                    case CRITICAL -> critical++;
                    case HIGH -> high++;
                    case MEDIUM -> medium++;
                    case LOW -> low++;
                }
            }

            return ResponseEntity.ok(Map.of(
                    "gaps", gapList,
                    "total", gapList.size(),
                    "bySeverity", Map.of(
                            "critical", critical,
                            "high", high,
                            "medium", medium,
                            "low", low)));
        } catch (Exception e) {
            logger.error("Failed to identify compliance gaps", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to identify gaps: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // RISK LIBRARY & PREDICTION
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/risks/{sector}")
    public ResponseEntity<?> getSectorRisks(@PathVariable String sector) {
        try {
            List<Map<String, Object>> risks = complianceEngine.getSectorRiskLibrary(sector.toUpperCase());
            return ResponseEntity.ok(Map.of(
                    "sector", sector.toUpperCase(),
                    "risks", risks,
                    "totalRisks", risks.size()));
        } catch (Exception e) {
            logger.error("Failed to get sector risks", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get risks: " + e.getMessage()));
        }
    }

    @GetMapping("/risks/{sector}/predict")
    public ResponseEntity<?> predictRisks(@PathVariable String sector) {
        try {
            List<Map<String, Object>> predictions = complianceEngine.predictEmergingRisks(sector.toUpperCase());
            return ResponseEntity.ok(Map.of(
                    "sector", sector.toUpperCase(),
                    "predictions", predictions,
                    "totalPredictions", predictions.size(),
                    "methodology", "AI-based trend analysis using historical breach data, " +
                            "sector patterns, and DPDP Act penalty structures"));
        } catch (Exception e) {
            logger.error("Failed to predict risks", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to predict risks: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // COMPLIANCE TREND
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/trend")
    public ResponseEntity<?> getComplianceTrend(
            @RequestParam(defaultValue = "30") int days) {
        try {
            ComplianceScore currentScore = complianceEngine.calculateOverallScore();

            // Build trend data — current assessment point
            List<Map<String, Object>> trend = new ArrayList<>();
            Map<String, Object> current = new LinkedHashMap<>();
            current.put("date", currentScore.getCalculatedAt().toLocalDate().toString());
            current.put("overallScore", currentScore.getOverallScore());
            current.put("ragStatus", currentScore.getOverallStatus().name());
            trend.add(current);

            return ResponseEntity.ok(Map.of(
                    "trend", trend,
                    "days", days,
                    "currentScore", currentScore.getOverallScore(),
                    "currentRAG", currentScore.getOverallStatus().name()));
        } catch (Exception e) {
            logger.error("Failed to get compliance trend", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get trend: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TRIGGER ASSESSMENT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/assess")
    public ResponseEntity<?> triggerAssessment(
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            ComplianceScore score = complianceEngine.calculateOverallScore();
            List<ComplianceGap> gaps = complianceEngine.identifyGaps();

            return ResponseEntity.ok(Map.of(
                    "status", "assessment_complete",
                    "overallScore", score.getOverallScore(),
                    "ragStatus", score.getOverallStatus().name(),
                    "gapCount", gaps.size(),
                    "calculatedAt", score.getCalculatedAt().toString(),
                    "message", "Compliance assessment completed. Overall score: "
                            + String.format("%.1f%%", score.getOverallScore())
                            + " (" + score.getOverallStatus().name() + ") with "
                            + gaps.size() + " gaps identified."));
        } catch (Exception e) {
            logger.error("Failed to trigger assessment", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to trigger assessment: " + e.getMessage()));
        }
    }
}
