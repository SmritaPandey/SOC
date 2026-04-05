package com.qsdpdp.web.api;

import com.qsdpdp.audit.ComplianceLedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Compliance Ledger Controller — Immutable audit trail API
 * 
 * @version 1.0.0
 * @since Phase 12
 */
@RestController
@RequestMapping("/api/compliance-ledger")
public class ComplianceLedgerController {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceLedgerController.class);

    @Autowired(required = false) private ComplianceLedgerService ledgerService;

    /** POST /api/compliance-ledger/record — Add immutable entry */
    @PostMapping("/record")
    public ResponseEntity<Map<String, Object>> record(@RequestBody Map<String, String> body) {
        ensureInit();
        return ResponseEntity.ok(ledgerService.record(
                body.getOrDefault("action", ""),
                body.getOrDefault("category", ""),
                body.getOrDefault("entityType", ""),
                body.getOrDefault("entityId", ""),
                body.getOrDefault("actor", "system"),
                body.getOrDefault("details", "")
        ));
    }

    /** GET /api/compliance-ledger/entries?limit=50 */
    @GetMapping("/entries")
    public ResponseEntity<?> getEntries(@RequestParam(defaultValue = "50") int limit) {
        ensureInit();
        return ResponseEntity.ok(Map.of("entries", ledgerService.getRecentEntries(limit)));
    }

    /** GET /api/compliance-ledger/validate — Verify chain integrity */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate() {
        ensureInit();
        return ResponseEntity.ok(ledgerService.validateChain());
    }

    /** GET /api/compliance-ledger/statistics */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics() {
        ensureInit();
        return ResponseEntity.ok(ledgerService.getStatistics());
    }

    private void ensureInit() {
        if (ledgerService != null && !ledgerService.isInitialized()) {
            try { ledgerService.initialize(); } catch (Exception e) {
                logger.debug("ComplianceLedgerService init skipped: {}", e.getMessage());
            }
        }
    }
}
