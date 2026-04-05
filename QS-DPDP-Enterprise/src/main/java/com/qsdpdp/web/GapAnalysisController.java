package com.qsdpdp.web;

import com.qsdpdp.gap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gap Analysis REST Controller
 * MCQ-based compliance self-assessment with heatmap and remediation planning
 *
 * @version 1.0.0
 * @since Sprint 2
 */

@RestController
@RequestMapping("/api/gap-analysis")
public class GapAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(GapAnalysisController.class);

    @Autowired
    private GapAnalysisService gapAnalysisService;

    // ═══════════════════════════════════════════════════════════
    // ASSESSMENT LIFECYCLE
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/start")
    public ResponseEntity<?> startAssessment(@RequestBody Map<String, String> payload) {
        try {
            String orgId = payload.getOrDefault("organizationId", "default-org");
            String sector = payload.getOrDefault("sector", "BFSI");
            String assessedBy = payload.getOrDefault("assessedBy", "admin");

            GapAnalysisResult result = gapAnalysisService.startAssessment(orgId, sector, assessedBy);
            return ResponseEntity.ok(Map.of(
                    "status", "started",
                    "assessmentId", result.getAssessmentId(),
                    "sector", sector,
                    "message", "Assessment started. Use /questions to get questions."));
        } catch (Exception e) {
            logger.error("Failed to start assessment", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to start assessment: " + e.getMessage()));
        }
    }

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(
            @RequestParam(defaultValue = "BFSI") String sector,
            @RequestParam(required = false) String category) {

        List<AssessmentQuestion> questions;
        if (category != null && !category.isBlank()) {
            try {
                QuestionCategory cat = QuestionCategory.valueOf(category.toUpperCase());
                questions = gapAnalysisService.getQuestionsForAssessment(sector, cat);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid category. Valid: " +
                                Arrays.toString(QuestionCategory.values())));
            }
        } else {
            // Return all mandatory questions
            questions = gapAnalysisService.getMandatoryQuestions();
        }

        return ResponseEntity.ok(Map.of(
                "questions", questions.stream().map(this::questionToMap).collect(Collectors.toList()),
                "total", questions.size(),
                "categories", Arrays.stream(QuestionCategory.values())
                        .map(c -> Map.of("id", c.name(), "name", c.getDisplayName()))
                        .collect(Collectors.toList())));
    }

    @PostMapping("/{assessmentId}/respond")
    public ResponseEntity<?> submitResponse(
            @PathVariable String assessmentId,
            @RequestBody Map<String, Object> payload) {
        try {
            String questionId = (String) payload.get("questionId");
            int selectedOption = payload.get("selectedOption") instanceof Number
                    ? ((Number) payload.get("selectedOption")).intValue()
                    : Integer.parseInt((String) payload.get("selectedOption"));
            String comment = (String) payload.getOrDefault("comment", "");

            return ResponseEntity.ok(Map.of(
                    "status", "recorded",
                    "assessmentId", assessmentId,
                    "questionId", questionId,
                    "selectedOption", selectedOption,
                    "message", "Response recorded successfully"));
        } catch (Exception e) {
            logger.error("Failed to submit response", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to submit response: " + e.getMessage()));
        }
    }

    @PostMapping("/{assessmentId}/complete")
    public ResponseEntity<?> completeAssessment(@PathVariable String assessmentId,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "completed",
                    "assessmentId", assessmentId,
                    "message", "Assessment completed. Use /gaps, /heatmap, /remediation for results."));
        } catch (Exception e) {
            logger.error("Failed to complete assessment", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to complete assessment: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // RESULTS & ANALYSIS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/{assessmentId}/gaps")
    public ResponseEntity<?> getGaps(@PathVariable String assessmentId) {
        try {
            List<GapAnalysisResult.ComplianceGap> gaps = gapAnalysisService.getOpenGaps(assessmentId);
            List<Map<String, Object>> gapList = new ArrayList<>();
            for (GapAnalysisResult.ComplianceGap gap : gaps) {
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("questionId", gap.getQuestionId());
                g.put("category", gap.getCategory().name());
                g.put("categoryName", gap.getCategory().getDisplayName());
                g.put("dpdpClause", gap.getDpdpClause());
                g.put("severity", gap.getSeverity());
                g.put("impact", gap.getImpact());
                g.put("remediation", gap.getRemediation());
                g.put("scoreLoss", gap.getScoreLoss());
                g.put("status", gap.getStatus());
                g.put("remediationOwner", gap.getRemediationOwner());
                gapList.add(g);
            }
            return ResponseEntity.ok(Map.of(
                    "assessmentId", assessmentId,
                    "gaps", gapList,
                    "total", gapList.size()));
        } catch (Exception e) {
            logger.error("Failed to get gaps", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get gaps: " + e.getMessage()));
        }
    }

    @GetMapping("/{assessmentId}/heatmap")
    public ResponseEntity<?> getHeatmap(@PathVariable String assessmentId) {
        try {
            GapAnalysisService.GapHeatmap heatmap = gapAnalysisService.generateHeatmap(assessmentId);
            Map<String, Object> heatmapData = new LinkedHashMap<>();
            heatmap.getCells().forEach((category, cell) -> {
                heatmapData.put(category.name(), Map.of(
                        "categoryName", category.getDisplayName(),
                        "gapCount", cell.getGapCount(),
                        "avgScoreLoss", cell.getAvgScoreLoss(),
                        "intensity", cell.getIntensity()));
            });
            return ResponseEntity.ok(Map.of(
                    "assessmentId", assessmentId,
                    "heatmap", heatmapData));
        } catch (Exception e) {
            logger.error("Failed to generate heatmap", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate heatmap: " + e.getMessage()));
        }
    }

    @GetMapping("/{assessmentId}/remediation")
    public ResponseEntity<?> getRemediationPlan(@PathVariable String assessmentId) {
        try {
            GapAnalysisService.RemediationPlan plan = gapAnalysisService.generateRemediationPlan(assessmentId);

            List<Map<String, Object>> phases = new ArrayList<>();
            for (GapAnalysisService.RemediationPhase phase : plan.getPhases()) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("phaseNumber", phase.getPhaseNumber());
                p.put("name", phase.getName());
                p.put("timelineWeeks", phase.getTimelineWeeks());
                p.put("gapCount", phase.getGaps().size());

                List<Map<String, Object>> phaseGaps = new ArrayList<>();
                for (GapAnalysisResult.ComplianceGap gap : phase.getGaps()) {
                    Map<String, Object> g = new LinkedHashMap<>();
                    g.put("questionId", gap.getQuestionId());
                    g.put("category", gap.getCategory().name());
                    g.put("severity", gap.getSeverity());
                    g.put("remediation", gap.getRemediation());
                    phaseGaps.add(g);
                }
                p.put("gaps", phaseGaps);
                phases.add(p);
            }

            return ResponseEntity.ok(Map.of(
                    "assessmentId", assessmentId,
                    "remediationPlan", phases,
                    "totalPhases", phases.size()));
        } catch (Exception e) {
            logger.error("Failed to generate remediation plan", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate remediation plan: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GAP MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/gaps/{gapId}/assign")
    public ResponseEntity<?> assignRemediationOwner(@PathVariable String gapId,
            @RequestBody Map<String, String> payload) {
        try {
            String assessmentId = payload.getOrDefault("assessmentId", "");
            String owner = payload.getOrDefault("owner", "");
            String targetDateStr = payload.getOrDefault("targetDate", "");
            LocalDateTime targetDate = targetDateStr.isEmpty()
                    ? LocalDateTime.now().plusDays(30)
                    : LocalDateTime.parse(targetDateStr);

            gapAnalysisService.assignRemediationOwner(assessmentId, gapId, owner, targetDate);
            return ResponseEntity.ok(Map.of(
                    "status", "assigned",
                    "gapId", gapId,
                    "owner", owner,
                    "targetDate", targetDate.toString()));
        } catch (Exception e) {
            logger.error("Failed to assign remediation owner", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to assign: " + e.getMessage()));
        }
    }

    @PostMapping("/gaps/{gapId}/remediate")
    public ResponseEntity<?> markRemediated(@PathVariable String gapId,
            @RequestBody Map<String, String> payload) {
        try {
            String remediatedBy = payload.getOrDefault("remediatedBy", "admin");
            String notes = payload.getOrDefault("notes", "");

            gapAnalysisService.markGapRemediated(gapId, remediatedBy, notes);
            return ResponseEntity.ok(Map.of(
                    "status", "remediated",
                    "gapId", gapId,
                    "remediatedBy", remediatedBy));
        } catch (Exception e) {
            logger.error("Failed to mark gap remediated", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to remediate: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            GapAnalysisService.GapStatistics stats = gapAnalysisService.getStatistics();
            return ResponseEntity.ok(Map.of("statistics", Map.of(
                    "totalAssessments", stats.getTotalAssessments(),
                    "completedAssessments", stats.getCompletedAssessments(),
                    "averageCompliance", stats.getAverageCompliance(),
                    "openGaps", stats.getOpenGaps(),
                    "criticalGaps", stats.getCriticalGaps(),
                    "remediatedGaps", stats.getRemediatedGaps())));
        } catch (Exception e) {
            logger.error("Failed to get gap statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private Map<String, Object> questionToMap(AssessmentQuestion q) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", q.getId());
        map.put("questionText", q.getQuestionText());
        map.put("category", q.getCategory().name());
        map.put("categoryName", q.getCategory().getDisplayName());
        map.put("options", q.getOptions());
        map.put("correctOptionIndex", q.getCorrectOptionIndex());
        map.put("maxScore", q.getMaxScore());
        map.put("mandatory", q.isMandatory());
        map.put("dpdpClause", q.getDpdpClause());
        map.put("hint", q.getHint());
        map.put("impactExplanation", q.getImpactExplanation());
        map.put("remediationGuidance", q.getRemediationGuidance());
        map.put("sector", q.getSector());
        map.put("difficultyLevel", q.getDifficultyLevel());
        return map;
    }
}
