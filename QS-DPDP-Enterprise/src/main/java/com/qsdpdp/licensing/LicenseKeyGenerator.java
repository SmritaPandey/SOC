package com.qsdpdp.licensing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.*;

/**
 * License Key Generator — ADMIN TOOL ONLY
 * 
 * This class is used by NeurQ AI Labs to generate signed license files
 * for enterprise clients. It is NOT shipped to clients in production builds.
 * ProGuard will strip this class in production profile.
 *
 * Workflow:
 * 1. Client sends their hardware fingerprint (from GET /api/licensing/fingerprint)
 * 2. NeurQ admin runs this tool with client details
 * 3. Generated license.key file is sent to the client
 * 4. Client uploads license.key via POST /api/licensing/activate-file
 *
 * @version 1.0.0
 * @since Enterprise Deployment
 */
public class LicenseKeyGenerator {

    private static final Logger logger = LoggerFactory.getLogger(LicenseKeyGenerator.class);
    private static final String ALGORITHM = "SHA256withRSA";
    private static final int KEY_SIZE = 2048;

    /**
     * Generate an RSA-2048 key pair for license signing.
     * The PRIVATE key is kept by NeurQ. The PUBLIC key is embedded in the application.
     *
     * @return Map with "privateKey" and "publicKey" as Base64-encoded strings
     */
    public static Map<String, String> generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(KEY_SIZE, new SecureRandom());
            KeyPair kp = kpg.generateKeyPair();

            Map<String, String> keys = new LinkedHashMap<>();
            keys.put("privateKey", Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
            keys.put("publicKey", Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));

            logger.info("RSA-2048 key pair generated for license signing");
            return keys;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA not available", e);
        }
    }

    /**
     * Generate a signed license file content.
     *
     * @param privateKeyBase64     NeurQ's RSA private key (Base64)
     * @param organizationName     Client organization name
     * @param organizationId       Client organization unique ID
     * @param licenseType          STANDARD, PROFESSIONAL, or ENTERPRISE
     * @param hardwareFingerprint  Client's hardware fingerprint hash
     * @param maxUsers             Maximum allowed users
     * @param validDays            License validity in days from now
     * @param enabledModules       Comma-separated list of enabled modules
     * @return Base64-encoded license file content (JSON payload + signature)
     */
    public static String generateLicense(
            String privateKeyBase64,
            String organizationName,
            String organizationId,
            String licenseType,
            String hardwareFingerprint,
            int maxUsers,
            int validDays,
            String enabledModules) {

        try {
            // Build license payload as JSON
            String licenseId = UUID.randomUUID().toString();
            LocalDateTime issuedAt = LocalDateTime.now();
            LocalDateTime expiresAt = issuedAt.plusDays(validDays);

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"licenseId\":\"").append(licenseId).append("\",");
            json.append("\"organizationName\":\"").append(escape(organizationName)).append("\",");
            json.append("\"organizationId\":\"").append(escape(organizationId)).append("\",");
            json.append("\"licenseType\":\"").append(licenseType).append("\",");
            json.append("\"hardwareFingerprint\":\"").append(hardwareFingerprint).append("\",");
            json.append("\"maxUsers\":").append(maxUsers).append(",");
            json.append("\"issuedAt\":\"").append(issuedAt).append("\",");
            json.append("\"expiresAt\":\"").append(expiresAt).append("\",");
            json.append("\"modules\":\"").append(enabledModules).append("\",");
            json.append("\"issuer\":\"NeurQ AI Labs\",");
            json.append("\"version\":\"1.0\"");
            json.append("}");

            String payload = json.toString();

            // Sign the payload
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(keySpec);

            Signature sig = Signature.getInstance(ALGORITHM);
            sig.initSign(privateKey);
            sig.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] signature = sig.sign();

            // Combine: Base64(payload) + "." + Base64(signature)
            String payloadB64 = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            String signatureB64 = Base64.getEncoder().encodeToString(signature);

            String licenseFile = payloadB64 + "." + signatureB64;

            logger.info("License generated: id={}, org={}, type={}, expires={}, modules={}",
                    licenseId, organizationName, licenseType, expiresAt, enabledModules);

            return licenseFile;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate license: " + e.getMessage(), e);
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * CLI entry point for generating licenses.
     * Usage: java -cp app.jar com.qsdpdp.licensing.LicenseKeyGenerator <command> [args...]
     *
     * Commands:
     *   keygen                          — Generate a new RSA key pair
     *   generate <privateKey> <org> <orgId> <type> <fingerprint> <maxUsers> <validDays> <modules>
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("QS-DPDP License Key Generator");
            System.out.println("Commands:");
            System.out.println("  keygen                          — Generate RSA-2048 key pair");
            System.out.println("  generate <privateKey> <org> <orgId> <type> <fingerprint> <maxUsers> <validDays> <modules>");
            return;
        }

        switch (args[0]) {
            case "keygen" -> {
                Map<String, String> keys = generateKeyPair();
                System.out.println("═══ PRIVATE KEY (keep secret!) ═══");
                System.out.println(keys.get("privateKey"));
                System.out.println();
                System.out.println("═══ PUBLIC KEY (embed in app) ═══");
                System.out.println(keys.get("publicKey"));
            }
            case "generate" -> {
                if (args.length < 9) {
                    System.err.println("Usage: generate <privateKey> <org> <orgId> <type> <fingerprint> <maxUsers> <validDays> <modules>");
                    System.exit(1);
                }
                String license = generateLicense(
                        args[1], args[2], args[3], args[4], args[5],
                        Integer.parseInt(args[6]), Integer.parseInt(args[7]), args[8]);
                System.out.println("═══ LICENSE FILE CONTENT ═══");
                System.out.println(license);
            }
            default -> System.err.println("Unknown command: " + args[0]);
        }
    }
}
