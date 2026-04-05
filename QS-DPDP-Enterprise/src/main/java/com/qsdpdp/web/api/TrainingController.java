package com.qsdpdp.web.api;

import com.qsdpdp.training.TrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Training & Awareness REST Controller
 * Provides REST API for DPDP compliance training programs,
 * enrollments, campaigns, and reporting.
 *
 * @version 1.0.0
 * @since Phase 4
 */
@RestController("trainingApiController")
@RequestMapping("/api/v1/training")
public class TrainingController {

    @Autowired(required = false)
    private TrainingService trainingService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "training-awareness",
                "status", "UP",
                "version", "1.0.0",
                "initialized", trainingService != null && trainingService.isInitialized()));
    }

    // ═══════════════════════════════════════════════════════════
    // TRAINING PROGRAMS (MODULES)
    // ═══════════════════════════════════════════════════════════

    /** List all training programs */
    @GetMapping("/programs")
    public ResponseEntity<Object> listPrograms() {
        try {
            var modules = trainingService.getDefaultModules();
            List<Map<String, Object>> result = new ArrayList<>();
            for (var m : modules) {
                result.add(Map.of(
                        "id", m.getId(),
                        "title", m.getTitle(),
                        "description", m.getDescription(),
                        "category", m.getCategory(),
                        "dpdpSection", m.getDpdpSection(),
                        "targetAudience", m.getTargetAudience(),
                        "durationMinutes", m.getDurationMinutes(),
                        "mandatory", m.isMandatory(),
                        "quizQuestionCount", m.getQuizQuestions().size()));
            }
            return ResponseEntity.ok(Map.of(
                    "totalPrograms", result.size(),
                    "programs", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get training module details including quiz questions */
    @GetMapping("/programs/{moduleId}")
    public ResponseEntity<Object> getProgram(@PathVariable String moduleId) {
        try {
            var modules = trainingService.getDefaultModules();
            var module = modules.stream()
                    .filter(m -> m.getId().equals(moduleId))
                    .findFirst()
                    .orElse(null);

            if (module == null) return ResponseEntity.notFound().build();

            List<Map<String, Object>> questions = new ArrayList<>();
            for (var q : module.getQuizQuestions()) {
                questions.add(Map.of(
                        "id", q.getId(),
                        "questionText", q.getQuestionText(),
                        "questionType", q.getQuestionType(),
                        "options", q.getOptions() != null ? q.getOptions() : List.of(),
                        "explanation", q.getExplanation() != null ? q.getExplanation() : ""));
            }

            return ResponseEntity.ok(Map.of(
                    "id", module.getId(),
                    "title", module.getTitle(),
                    "description", module.getDescription(),
                    "category", module.getCategory(),
                    "dpdpSection", module.getDpdpSection(),
                    "targetAudience", module.getTargetAudience(),
                    "durationMinutes", module.getDurationMinutes(),
                    "mandatory", module.isMandatory(),
                    "quizQuestions", questions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ENROLLMENT
    // ═══════════════════════════════════════════════════════════

    /** Enroll a user into a training module */
    @PostMapping("/enroll")
    public ResponseEntity<Map<String, Object>> enroll(@RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String moduleId = body.get("moduleId");

            if (userId == null || moduleId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId and moduleId are required"));
            }

            String enrollmentId = trainingService.enrollUser(userId, moduleId);
            if (enrollmentId == null) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Enrollment failed"));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "enrollmentId", enrollmentId,
                    "userId", userId,
                    "moduleId", moduleId,
                    "status", "ENROLLED",
                    "message", "User enrolled successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Start a module (mark enrollment as IN_PROGRESS) */
    @PostMapping("/enrollments/{enrollmentId}/start")
    public ResponseEntity<Map<String, Object>> startModule(@PathVariable String enrollmentId) {
        try {
            trainingService.startModule(enrollmentId);
            return ResponseEntity.ok(Map.of(
                    "enrollmentId", enrollmentId,
                    "status", "IN_PROGRESS",
                    "message", "Module started"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Complete a module with score */
    @PostMapping("/enrollments/{enrollmentId}/complete")
    public ResponseEntity<Map<String, Object>> completeModule(
            @PathVariable String enrollmentId,
            @RequestBody Map<String, Object> body) {
        try {
            int score = (int) body.getOrDefault("score", 0);
            trainingService.completeModule(enrollmentId, score);
            return ResponseEntity.ok(Map.of(
                    "enrollmentId", enrollmentId,
                    "score", score,
                    "message", "Module completion recorded"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get user's enrollments */
    @GetMapping("/enrollments")
    public ResponseEntity<Object> getUserEnrollments(@RequestParam String userId) {
        try {
            var enrollments = trainingService.getUserEnrollments(userId);
            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "totalEnrollments", enrollments.size(),
                    "enrollments", enrollments));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CAMPAIGNS
    // ═══════════════════════════════════════════════════════════

    /** Create a training campaign */
    @PostMapping("/campaigns")
    public ResponseEntity<Map<String, Object>> createCampaign(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String description = (String) body.getOrDefault("description", "");
            @SuppressWarnings("unchecked")
            List<String> moduleIds = (List<String>) body.getOrDefault("moduleIds", List.of());
            @SuppressWarnings("unchecked")
            List<String> departments = (List<String>) body.getOrDefault("departments", List.of());
            String createdBy = (String) body.getOrDefault("createdBy", "ADMIN");

            if (name == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Campaign name is required"));
            }

            String campaignId = trainingService.createCampaign(
                    name, description, moduleIds, departments,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(90), createdBy);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "campaignId", campaignId,
                    "name", name,
                    "status", "ACTIVE",
                    "message", "Training campaign created"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS & REPORTING
    // ═══════════════════════════════════════════════════════════

    /** Get training dashboard statistics */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Object> getStats() {
        try {
            var stats = trainingService.getStatistics();
            return ResponseEntity.ok(Map.of(
                    "totalEnrollees", stats.getTotalEnrollees(),
                    "completions", stats.getCompletions(),
                    "averageScore", stats.getAverageScore(),
                    "inProgress", stats.getInProgress(),
                    "expiringCertificates", stats.getExpiringCertificates(),
                    "totalModules", trainingService.getDefaultModules().size()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Get completion summary for reporting */
    @GetMapping("/completions")
    public ResponseEntity<Object> getCompletionReport(
            @RequestParam(defaultValue = "30") int days) {
        try {
            var stats = trainingService.getStatistics();
            return ResponseEntity.ok(Map.of(
                    "reportPeriodDays", days,
                    "totalCompletions", stats.getCompletions(),
                    "averageScore", stats.getAverageScore(),
                    "totalEnrollees", stats.getTotalEnrollees(),
                    "completionRate", stats.getTotalEnrollees() > 0
                            ? (double) stats.getCompletions() / stats.getTotalEnrollees() * 100 : 0));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
