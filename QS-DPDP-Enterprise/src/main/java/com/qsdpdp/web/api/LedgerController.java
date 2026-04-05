package com.qsdpdp.web.api;

import com.qsdpdp.ledger.ConsentBlock;
import com.qsdpdp.ledger.ConsentLedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Consent Ledger Network (CLN) REST Controller
 * Tamper-proof consent ledger with hash chaining, Merkle proofs, and quantum-safe signatures.
 */
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    @Autowired
    private ConsentLedgerService ledgerService;

    @PostMapping("/add-consent")
    public ResponseEntity<?> addConsent(@RequestBody Map<String, Object> payload) {
        try {
            String consentId = (String) payload.getOrDefault("consentId", "C-" + UUID.randomUUID().toString().substring(0, 8));
            String dataPrincipal = (String) payload.getOrDefault("dataPrincipal", "anonymous");
            String fiduciaryId = (String) payload.getOrDefault("fiduciary", "default-org");
            String purpose = (String) payload.getOrDefault("purpose", "data-processing");
            String action = (String) payload.getOrDefault("action", "GRANT");

            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) payload.getOrDefault("metadata", Collections.emptyMap());

            ConsentBlock block = ledgerService.addConsent(consentId, dataPrincipal, fiduciaryId, purpose, action, metadata);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "ADDED");
            response.put("message", "Consent record added to tamper-proof ledger");
            response.put("block", block.toMap());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify-consent/{consentId}")
    public ResponseEntity<?> verifyConsent(@PathVariable String consentId) {
        return ResponseEntity.ok(ledgerService.verifyConsent(consentId));
    }

    @GetMapping("/audit-proof/{consentId}")
    public ResponseEntity<?> auditProof(@PathVariable String consentId) {
        return ResponseEntity.ok(ledgerService.generateAuditProof(consentId));
    }

    @GetMapping("/chain-status")
    public ResponseEntity<?> chainStatus() {
        return ResponseEntity.ok(ledgerService.getChainStatus());
    }

    @GetMapping("/fiduciary/{fiduciaryId}")
    public ResponseEntity<?> getByFiduciary(@PathVariable String fiduciaryId) {
        List<Map<String, Object>> blocks = ledgerService.getBlocksByFiduciary(fiduciaryId);
        return ResponseEntity.ok(Map.of("fiduciaryId", fiduciaryId, "blocks", blocks, "count", blocks.size()));
    }
}
