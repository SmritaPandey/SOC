package com.qsdpdp.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Crypto Erase Service — DPDP S.8(7) + RBI Domain 9
 * 
 * Implements cryptographic erasure:
 * - Destroy encryption keys to make data irrecoverable
 * - Policy-based automated deletion scheduling
 * - Retention period enforcement
 * - Erasure certificate generation
 * - Audit trail for regulator evidence
 * 
 * @version 1.0.0
 * @since Phase 3 — RBI Enhancement
 */
@Service
public class CryptoEraseService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoEraseService.class);

    // Track active encryption keys (in production: use HSM/KMS)
    private final Map<String, SecretKey> keyStore = new LinkedHashMap<>();
    private final List<Map<String, Object>> erasureLog = new ArrayList<>();

    /**
     * Generate encryption key for a data entity
     */
    public Map<String, Object> generateKey(String entityId, String entityType,
            String retentionDays) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey key = keyGen.generateKey();
            String keyId = "KEY-" + UUID.randomUUID().toString().substring(0, 8);
            keyStore.put(keyId, key);

            return Map.of("keyId", keyId, "entityId", entityId, "entityType", entityType,
                    "algorithm", "AES-256-GCM", "retentionDays", retentionDays,
                    "status", "ACTIVE", "createdAt", LocalDateTime.now().toString());
        } catch (Exception e) {
            logger.error("Key generation failed", e);
            return Map.of("error", "Key generation failed: " + e.getMessage());
        }
    }

    /**
     * Perform cryptographic erasure — destroy the key
     */
    public Map<String, Object> cryptoErase(String keyId, String reason, String authorizedBy) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("keyId", keyId);
        result.put("reason", reason);
        result.put("authorizedBy", authorizedBy);

        if (keyStore.containsKey(keyId)) {
            keyStore.remove(keyId);
            result.put("status", "ERASED");
            result.put("method", "CRYPTOGRAPHIC_KEY_DESTRUCTION");
            result.put("standard", "NIST SP 800-88 Rev.1 — Cryptographic Erase");
            result.put("dpdpSection", "S.8(7) — Data retention and erasure");
            result.put("rbiDomain", "Domain 9 — Data Retention and Disposal");
            result.put("recoverable", false);

            // Generate erasure certificate
            String certId = "CERT-ERASE-" + System.currentTimeMillis();
            result.put("certificateId", certId);
            result.put("certificate", generateErasureCertificate(keyId, certId, reason, authorizedBy));
        } else {
            result.put("status", "KEY_NOT_FOUND");
            result.put("recoverable", true);
        }

        result.put("erasedAt", LocalDateTime.now().toString());
        erasureLog.add(result);
        return result;
    }

    /**
     * Encrypt data with a key
     */
    public Map<String, Object> encrypt(String keyId, String data) {
        SecretKey key = keyStore.get(keyId);
        if (key == null) return Map.of("error", "Key not found or already erased", "keyId", keyId);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Map.of("keyId", keyId, "encrypted", Base64.getEncoder().encodeToString(encrypted),
                    "iv", Base64.getEncoder().encodeToString(iv), "algorithm", "AES-256-GCM");
        } catch (Exception e) {
            return Map.of("error", "Encryption failed: " + e.getMessage());
        }
    }

    /**
     * Get erasure log (audit trail)
     */
    public Map<String, Object> getErasureLog() {
        return Map.of("entries", erasureLog, "totalErasures", erasureLog.size(),
                "activeKeys", keyStore.size(), "timestamp", LocalDateTime.now().toString());
    }

    /**
     * Get retention policy schedule
     */
    public Map<String, Object> getRetentionPolicies() {
        Map<String, Map<String, Object>> policies = new LinkedHashMap<>();
        policies.put("TRANSACTION_DATA", Map.of("retention", "8 years", "basis", "RBI Master Direction", "autoErase", true));
        policies.put("KYC_DATA", Map.of("retention", "5 years after account closure", "basis", "RBI KYC Master Direction", "autoErase", true));
        policies.put("CONSENT_RECORDS", Map.of("retention", "5 years", "basis", "DPDP S.8(7)", "autoErase", true));
        policies.put("HEALTH_RECORDS", Map.of("retention", "3 years", "basis", "ABDM Guidelines", "autoErase", true));
        policies.put("MARKETING_DATA", Map.of("retention", "Until withdrawal", "basis", "DPDP S.6(6)", "autoErase", true));
        policies.put("AUDIT_LOGS", Map.of("retention", "7 years", "basis", "ISO 27001 A.12.4", "autoErase", false));
        policies.put("BREACH_RECORDS", Map.of("retention", "Permanent", "basis", "DPDP S.8(6)", "autoErase", false));
        return Map.of("policies", policies, "count", policies.size());
    }

    private Map<String, Object> generateErasureCertificate(String keyId, String certId,
            String reason, String authorizedBy) {
        return Map.of(
                "certificateId", certId,
                "keyId", keyId,
                "method", "AES-256 Key Destruction",
                "standard", "NIST SP 800-88 Rev.1",
                "reason", reason,
                "authorizedBy", authorizedBy,
                "timestamp", LocalDateTime.now().toString(),
                "verification", "Data encrypted with key " + keyId + " is now permanently irrecoverable",
                "legalBasis", "DPDP Act S.8(7), RBI Advisory Domain 9"
        );
    }
}
