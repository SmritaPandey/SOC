package com.qshield.common.audit;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Audit Trail Service with hash-chaining for immutability.
 * NIST AU-3 + AU-10 (Non-Repudiation).
 */
@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public AuditEvent log(String product, String eventType, String userId,
                          String ipAddress, String details, String severity) {
        AuditEvent event = new AuditEvent(product, eventType, userId, ipAddress, details, severity);

        // Hash-chain: link to previous record
        repository.findFirstByOrderByIdDesc().ifPresent(prev -> event.setPreviousHash(prev.getCurrentHash()));

        // Compute hash of this record
        String hashInput = event.getTimestamp() + "|" + product + "|" + eventType + "|" +
                           userId + "|" + details + "|" + event.getPreviousHash();
        event.setCurrentHash(sha256(hashInput));

        return repository.save(event);
    }

    public boolean verifyChainIntegrity() {
        var events = repository.findAll();
        String expectedPrevHash = null;
        for (AuditEvent e : events) {
            if (expectedPrevHash != null && !expectedPrevHash.equals(e.getPreviousHash())) {
                return false; // Chain broken — tampering detected
            }
            expectedPrevHash = e.getCurrentHash();
        }
        return true;
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
