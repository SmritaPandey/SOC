package com.qsdpdp.web.api;

import com.qsdpdp.creditscore.ConsentCreditScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Consent Credit Score (CCS) REST Controller
 */
@RestController
@RequestMapping("/api/credit-score")
public class CreditScoreController {

    @Autowired
    private ConsentCreditScoreService creditScoreService;

    @GetMapping("/{orgId}")
    public ResponseEntity<?> getScore(@PathVariable String orgId) {
        return ResponseEntity.ok(creditScoreService.calculateScore(orgId));
    }

    @PostMapping("/{orgId}")
    public ResponseEntity<?> calculateScore(@PathVariable String orgId, @RequestBody Map<String, Object> params) {
        return ResponseEntity.ok(creditScoreService.calculateScoreWithParams(orgId, params));
    }

    @GetMapping("/{orgId}/history")
    public ResponseEntity<?> getHistory(@PathVariable String orgId) {
        return ResponseEntity.ok(creditScoreService.getScoreHistory(orgId));
    }

    @GetMapping("/benchmark")
    public ResponseEntity<?> getBenchmarks() {
        return ResponseEntity.ok(creditScoreService.getBenchmarks());
    }
}
