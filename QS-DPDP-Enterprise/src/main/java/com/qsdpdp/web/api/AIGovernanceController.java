package com.qsdpdp.web.api;

import com.qsdpdp.aigovernance.AIGovernanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI Governance & Accountability REST Controller
 * Model registry, decision logging, explainability, and bias detection
 */
@RestController
@RequestMapping("/api/ai")
public class AIGovernanceController {

    @Autowired
    private AIGovernanceService aiService;

    @PostMapping("/register-model")
    public ResponseEntity<?> registerModel(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiService.registerModel(payload));
    }

    @GetMapping("/models")
    public ResponseEntity<?> listModels() {
        return ResponseEntity.ok(aiService.listModels());
    }

    @PostMapping("/log-decision")
    public ResponseEntity<?> logDecision(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(aiService.logDecision(payload));
    }

    @GetMapping("/explain/{decisionId}")
    public ResponseEntity<?> explain(@PathVariable String decisionId) {
        return ResponseEntity.ok(aiService.explainDecision(decisionId));
    }

    @GetMapping("/bias-report/{modelId}")
    public ResponseEntity<?> biasReport(@PathVariable String modelId) {
        return ResponseEntity.ok(aiService.detectBias(modelId));
    }

    @GetMapping("/audit")
    public ResponseEntity<?> audit(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(aiService.getAuditTrail(page, size));
    }
}
