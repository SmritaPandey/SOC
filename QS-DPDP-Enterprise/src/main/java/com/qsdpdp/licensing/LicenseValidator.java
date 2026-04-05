package com.qsdpdp.licensing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.*;

/**
 * License Validator — Cryptographic License File Verification
 *
 * Validates license files by:
 * 1. Verifying RSA-2048 digital signature (using embedded public key)
 * 2. Checking hardware fingerprint match
 * 3. Verifying expiry date
 * 4. Parsing license attributes (org, type, modules, max users)
 *
 * The NeurQ public key is embedded at compile time. Only NeurQ's private key
 * can produce valid signatures, making license forgery computationally infeasible.
 *
 * @version 1.0.0
 * @since Enterprise Deployment
 */
@Component
public class LicenseValidator {

    private static final Logger logger = LoggerFactory.getLogger(LicenseValidator.class);
    private static final String ALGORITHM = "SHA256withRSA";

    /**
     * NeurQ AI Labs RSA-2048 Public Key (Base64-encoded).
     * 
     * IMPORTANT: Replace this with your actual public key generated via
     * LicenseKeyGenerator.main(new String[]{"keygen"})
     * 
     * This placeholder key will cause validation to fail until replaced.
     * After generation, update this constant and rebuild.
     */
    private static final String NEURQ_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0PLACEHOLDER_REPLACE_WITH_ACTUAL_KEY" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIDAQAB";

    /**
     * Validation result container.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String error;
        private final Map<String, String> licenseData;

        private ValidationResult(boolean valid, String error, Map<String, String> licenseData) {
            this.valid = valid;
            this.error = error;
            this.licenseData = licenseData;
        }

        public static ValidationResult success(Map<String, String> data) {
            return new ValidationResult(true, null, data);
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, error, null);
        }

        public boolean isValid() { return valid; }
        public String getError() { return error; }
        public Map<String, String> getLicenseData() { return licenseData; }
    }

    /**
     * Validate a license file content string.
     *
     * @param licenseFileContent The license file content (Base64payload.Base64signature)
     * @param checkHardware      Whether to verify hardware fingerprint match
     * @return ValidationResult with parsed license data or error
     */
    public ValidationResult validate(String licenseFileContent, boolean checkHardware) {
        if (licenseFileContent == null || licenseFileContent.isBlank()) {
            return ValidationResult.failure("License file is empty");
        }

        // Split payload and signature
        String[] parts = licenseFileContent.trim().split("\\.");
        if (parts.length != 2) {
            return ValidationResult.failure("Invalid license file format — expected payload.signature");
        }

        String payloadB64 = parts[0];
        String signatureB64 = parts[1];

        // Decode payload
        String payloadJson;
        try {
            payloadJson = new String(Base64.getDecoder().decode(payloadB64), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ValidationResult.failure("Failed to decode license payload: " + e.getMessage());
        }

        // Verify RSA signature
        try {
            if (!verifySignature(payloadJson, signatureB64)) {
                return ValidationResult.failure("Invalid license signature — license file may be tampered");
            }
        } catch (Exception e) {
            return ValidationResult.failure("Signature verification failed: " + e.getMessage());
        }

        // Parse license data from JSON (simple parser — no external dependency)
        Map<String, String> data = parseSimpleJson(payloadJson);

        // Check expiry
        String expiresAt = data.get("expiresAt");
        if (expiresAt != null) {
            try {
                LocalDateTime expiry = LocalDateTime.parse(expiresAt);
                if (LocalDateTime.now().isAfter(expiry)) {
                    return ValidationResult.failure("License expired on " + expiresAt);
                }
            } catch (Exception e) {
                return ValidationResult.failure("Invalid expiry date format: " + expiresAt);
            }
        }

        // Check hardware fingerprint
        if (checkHardware) {
            String licensedFingerprint = data.get("hardwareFingerprint");
            if (licensedFingerprint != null && !licensedFingerprint.isBlank()) {
                String currentFingerprint = HardwareFingerprint.generate();
                if (!licensedFingerprint.equals(currentFingerprint)) {
                    logger.warn("Hardware fingerprint mismatch! Licensed: {}...{}, Current: {}...{}",
                            licensedFingerprint.substring(0, 8),
                            licensedFingerprint.substring(licensedFingerprint.length() - 4),
                            currentFingerprint.substring(0, 8),
                            currentFingerprint.substring(currentFingerprint.length() - 4));
                    return ValidationResult.failure(
                            "Hardware fingerprint mismatch — this license is bound to a different machine");
                }
            }
        }

        logger.info("License validated: org={}, type={}, expires={}, modules={}",
                data.get("organizationName"), data.get("licenseType"),
                data.get("expiresAt"), data.get("modules"));

        return ValidationResult.success(data);
    }

    /**
     * Verify RSA-2048 signature.
     */
    private boolean verifySignature(String payload, String signatureB64) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(NEURQ_PUBLIC_KEY);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(keySpec);

        Signature sig = Signature.getInstance(ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(payload.getBytes(StandardCharsets.UTF_8));

        byte[] signatureBytes = Base64.getDecoder().decode(signatureB64);
        return sig.verify(signatureBytes);
    }

    /**
     * Simple JSON parser for license payload (no external dependency).
     * Handles flat JSON objects with string/number values.
     */
    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        // Strip braces
        String content = json.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);

        // Split by commas (respecting quoted strings)
        List<String> pairs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaped = false;

        for (char c : content.toCharArray()) {
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }
            if (c == ',' && !inQuotes) {
                pairs.add(current.toString().trim());
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            pairs.add(current.toString().trim());
        }

        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx > 0) {
                String key = pair.substring(0, colonIdx).trim().replaceAll("^\"|\"$", "");
                String value = pair.substring(colonIdx + 1).trim().replaceAll("^\"|\"$", "");
                map.put(key, value);
            }
        }

        return map;
    }

    /**
     * Check if this validator has a real public key configured (not placeholder).
     */
    public boolean isPublicKeyConfigured() {
        return !NEURQ_PUBLIC_KEY.contains("PLACEHOLDER");
    }
}
