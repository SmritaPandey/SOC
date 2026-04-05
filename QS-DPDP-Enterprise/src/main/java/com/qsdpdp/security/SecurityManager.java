package com.qsdpdp.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Security Manager for QS-DPDP Enterprise
 * Handles encryption, hashing, and security operations
 * Implements NIST-compliant cryptography with PQC readiness
 * 
 * ENHANCED in Phase 2:
 * - H-01: Added TOTP verification (RFC 6238)
 * - H-09: Added password complexity policy enforcement
 * - Added password policy validation
 * 
 * @version 2.0.0
 * @since Phase 1
 */
@Service
public class SecurityManager {

    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);

    // Encryption constants
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    // Argon2id parameters (OWASP recommended)
    private static final int ARGON2_MEMORY = 65536; // 64 MB
    private static final int ARGON2_ITERATIONS = 3;
    private static final int ARGON2_PARALLELISM = 4;
    private static final int ARGON2_HASH_LENGTH = 32;
    private static final int ARGON2_SALT_LENGTH = 16;

    // Password policy constants
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    // TOTP constants (RFC 6238)
    private static final int TOTP_DIGITS = 6;
    private static final int TOTP_PERIOD_SECONDS = 30;
    private static final int TOTP_WINDOW = 1; // Allow ±1 time step

    private boolean initialized = false;
    private SecureRandom secureRandom;

    static {
        // Add Bouncy Castle provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public void initialize() {
        if (initialized) {
            return;
        }

        logger.info("Initializing Security Manager...");

        try {
            secureRandom = SecureRandom.getInstanceStrong();

            // Verify crypto availability
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            if (maxKeyLen < AES_KEY_SIZE) {
                logger.warn("JCE unlimited strength policy not installed, max key length: {}", maxKeyLen);
            }

            // List available algorithms
            int algorithmCount = 0;
            for (Provider provider : Security.getProviders()) {
                algorithmCount += provider.getServices().size();
            }

            initialized = true;
            logger.info("Security Manager initialized with {} algorithms available", algorithmCount);

        } catch (Exception e) {
            logger.error("Failed to initialize Security Manager", e);
            throw new RuntimeException("Security initialization failed", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PASSWORD POLICY (H-09 FIX)
    // ═══════════════════════════════════════════════════════════

    /**
     * Validate password against enterprise security policy.
     * Requirements:
     * - Minimum 12 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     * - Maximum 128 characters
     *
     * @return List of validation error messages (empty = valid)
     */
    public List<String> validatePasswordPolicy(String password) {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();
        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
            return errors;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            errors.add("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            errors.add("Password must not exceed " + MAX_PASSWORD_LENGTH + " characters");
        }
        if (!HAS_UPPERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }
        if (!HAS_LOWERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }
        if (!HAS_DIGIT.matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }
        if (!HAS_SPECIAL.matcher(password).find()) {
            errors.add("Password must contain at least one special character");
        }
        return errors;
    }

    /**
     * Check if a password meets the enterprise security policy.
     */
    public boolean isPasswordCompliant(String password) {
        return validatePasswordPolicy(password).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════
    // PASSWORD HASHING (Argon2id)
    // ═══════════════════════════════════════════════════════════

    /**
     * Hash a password using Argon2id
     */
    public String hashPassword(String password) {
        byte[] salt = new byte[ARGON2_SALT_LENGTH];
        secureRandom.nextBytes(salt);

        byte[] hash = argon2Hash(password.toCharArray(), salt);

        // Format: $argon2id$v=19$m=65536,t=3,p=4$<base64_salt>$<base64_hash>
        return String.format("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s",
                ARGON2_MEMORY, ARGON2_ITERATIONS, ARGON2_PARALLELISM,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash));
    }

    /**
     * Verify a password against a hash
     */
    public boolean verifyPassword(String password, String storedHash) {
        try {
            // Parse stored hash
            String[] parts = storedHash.split("\\$");
            if (parts.length < 6 || !parts[1].equals("argon2id")) {
                return false;
            }

            String params = parts[3];
            byte[] salt = Base64.getDecoder().decode(parts[4]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[5]);

            // Recompute hash
            byte[] computedHash = argon2Hash(password.toCharArray(), salt);

            // Constant-time comparison
            return MessageDigest.isEqual(expectedHash, computedHash);

        } catch (Exception e) {
            logger.error("Error verifying password", e);
            return false;
        }
    }

    private byte[] argon2Hash(char[] password, byte[] salt) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withMemoryAsKB(ARGON2_MEMORY)
                .withIterations(ARGON2_ITERATIONS)
                .withParallelism(ARGON2_PARALLELISM)
                .withSalt(salt)
                .build();

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);

        byte[] hash = new byte[ARGON2_HASH_LENGTH];
        generator.generateBytes(new String(password).getBytes(), hash);

        return hash;
    }

    // ═══════════════════════════════════════════════════════════
    // DATA ENCRYPTION (AES-256-GCM)
    // ═══════════════════════════════════════════════════════════

    /**
     * Generate a new AES-256 key
     */
    public String generateEncryptionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE, secureRandom);
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            logger.error("Error generating encryption key", e);
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Encrypt data using AES-256-GCM
     */
    public String encrypt(String plaintext, String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // Combine IV + ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            logger.error("Encryption error", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt data using AES-256-GCM
     */
    public String decrypt(String encryptedData, String base64Key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] combined = Base64.getDecoder().decode(encryptedData);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");

        } catch (Exception e) {
            logger.error("Decryption error", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HASHING (SHA-256)
    // ═══════════════════════════════════════════════════════════

    /**
     * Calculate SHA-256 hash
     */
    public String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            logger.error("Hashing error", e);
            throw new RuntimeException("Hashing failed", e);
        }
    }

    /**
     * Generate a secure random token
     */
    public String generateToken(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    // ═══════════════════════════════════════════════════════════
    // MFA / TOTP (H-01 FIX — RFC 6238)
    // ═══════════════════════════════════════════════════════════

    /**
     * Generate TOTP secret for MFA (Base32-encoded)
     */
    public String generateTOTPSecret() {
        byte[] secret = new byte[20]; // 160-bit secret per RFC 4226
        secureRandom.nextBytes(secret);
        return base32Encode(secret);
    }

    /**
     * Verify a TOTP code against a secret.
     * Implements RFC 6238 TOTP with a configurable time window.
     *
     * @param secret Base32-encoded secret
     * @param code   6-digit TOTP code provided by user
     * @return true if the code is valid within the allowed time window
     */
    public boolean verifyTOTP(String secret, String code) {
        if (secret == null || code == null || code.length() != TOTP_DIGITS) {
            return false;
        }

        try {
            byte[] secretBytes = base32Decode(secret);
            long currentTimeStep = System.currentTimeMillis() / 1000 / TOTP_PERIOD_SECONDS;

            // Check current time step and ±TOTP_WINDOW adjacent steps
            for (int i = -TOTP_WINDOW; i <= TOTP_WINDOW; i++) {
                String generatedCode = generateTOTPCode(secretBytes, currentTimeStep + i);
                if (MessageDigest.isEqual(generatedCode.getBytes(), code.getBytes())) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("TOTP verification error", e);
        }
        return false;
    }

    /**
     * Generate TOTP code for a given secret and time step.
     * Implements HOTP (RFC 4226) as the base for TOTP.
     */
    private String generateTOTPCode(byte[] secret, long timeStep) throws Exception {
        byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

        Mac hmac = Mac.getInstance("HmacSHA1");
        hmac.init(new SecretKeySpec(secret, "HmacSHA1"));
        byte[] hash = hmac.doFinal(timeBytes);

        // Dynamic truncation per RFC 4226
        int offset = hash[hash.length - 1] & 0x0F;
        int truncated = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = truncated % (int) Math.pow(10, TOTP_DIGITS);
        return String.format("%0" + TOTP_DIGITS + "d", otp);
    }

    /**
     * Generate a TOTP provisioning URI for QR code generation.
     * Compatible with Google Authenticator, Authy, etc.
     */
    public String getTOTPProvisioningURI(String secret, String username, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                issuer, username, secret, issuer, TOTP_DIGITS, TOTP_PERIOD_SECONDS);
    }

    // ═══════════════════════════════════════════════════════════
    // BASE32 ENCODING/DECODING (for TOTP secrets)
    // ═══════════════════════════════════════════════════════════

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                result.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

    private byte[] base32Decode(String encoded) {
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int outputLength = encoded.length() * 5 / 8;
        byte[] result = new byte[outputLength];
        int buffer = 0, bitsLeft = 0, index = 0;
        for (char c : encoded.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0)
                continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                result[index++] = (byte) ((buffer >> bitsLeft) & 0xFF);
            }
        }
        return result;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
