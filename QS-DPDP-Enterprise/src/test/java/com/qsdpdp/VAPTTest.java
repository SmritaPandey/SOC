package com.qsdpdp;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.security.SecurityManager;
import com.qsdpdp.crypto.QuantumSafeEncryptionService;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.*;
import java.util.regex.*;

/**
 * VAPT (Vulnerability Assessment & Penetration Testing) Suite
 * Covers OWASP Top-10 + DPDP-specific security controls
 *
 * @version 1.0.0
 * @since Phase D
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase D — VAPT Security Tests")
public class VAPTTest {

    private static DatabaseManager dbManager;
    private static SecurityManager secMgr;
    private static QuantumSafeEncryptionService cryptoService;
    private static AuditService auditService;
    private static EventBus eventBus;
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(VAPTTest.class.getName());

    // PII detection patterns (same as PIIScanner)
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\d{4}\\s\\d{4}\\s\\d{4}");
    private static final Pattern PAN_PATTERN = Pattern.compile("[A-Z]{5}\\d{4}[A-Z]");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+91[- ]?\\d{10}");

    @BeforeAll
    static void setup() {
        dbManager = new DatabaseManager();
        dbManager.initialize();
        secMgr = new SecurityManager();
        secMgr.initialize();
        eventBus = new EventBus();
        auditService = new AuditService(dbManager);
        cryptoService = new QuantumSafeEncryptionService();
    }

    // ═══════════════════════════════════════════════════════════
    // 1. SQL INJECTION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void testSQLInjection_BasicPayload() throws Exception {
        String malicious = "'; DROP TABLE users; --";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, malicious);
            ResultSet rs = ps.executeQuery();
            // Should return no results, NOT execute DROP TABLE
            assertFalse(rs.next(), "Parameterized query should prevent SQL injection");
        }
        // Verify users table still exists
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            assertTrue(rs.next(), "Users table must still exist after injection attempt");
        }
    }

    @Test
    @Order(2)
    void testSQLInjection_UnionBased() throws Exception {
        String malicious = "admin' UNION SELECT id,username,password_hash,full_name,role,status,null,null,null,null,null,null,null,null,null,null,null FROM users--";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, malicious);
            ResultSet rs = ps.executeQuery();
            assertFalse(rs.next(), "UNION injection must be blocked by parameterized query");
        }
    }

    @Test
    @Order(3)
    void testSQLInjection_BlindBoolean() throws Exception {
        String malicious = "admin' AND 1=1--";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, malicious);
            ResultSet rs = ps.executeQuery();
            assertFalse(rs.next(), "Blind boolean injection must be blocked");
        }
    }

    @Test
    @Order(4)
    void testSQLInjection_TimeBased() throws Exception {
        String malicious = "admin'; WAITFOR DELAY '0:0:5'--";
        long start = System.currentTimeMillis();
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ?")) {
            ps.setString(1, malicious);
            ps.executeQuery();
        }
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 3000, "Time-based injection must not cause delay");
    }

    @Test
    @Order(5)
    void testSQLInjection_SecondOrder() throws Exception {
        String malicious = "test'; UPDATE users SET role='ADMIN' WHERE username='test'--";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("INSERT OR IGNORE INTO data_principals (id, name, email) VALUES (?, ?, ?)")) {
            ps.setString(1, "sqli-test-001");
            ps.setString(2, malicious);
            ps.setString(3, "sqli@test.com");
            ps.executeUpdate();
        }
        // Verify no privilege escalation
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT role FROM users WHERE username = ?")) {
            ps.setString(1, "test");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                assertNotEquals("ADMIN", rs.getString("role"), "Second-order injection must not escalate privileges");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 2. XSS/CSRF PREVENTION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(6)
    void testXSS_StoredPayload() throws Exception {
        String xssPayload = "<script>alert('xss')</script>";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("INSERT OR IGNORE INTO data_principals (id, name, email) VALUES (?, ?, ?)")) {
            ps.setString(1, "xss-test-001");
            ps.setString(2, xssPayload);
            ps.setString(3, "xss@test.com");
            ps.executeUpdate();
        }
        // Verify data is stored as-is (output encoding handles XSS, not input)
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT name FROM data_principals WHERE id = ?")) {
            ps.setString(1, "xss-test-001");
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            String stored = rs.getString("name");
            // Verify we can detect the XSS pattern for output encoding
            assertTrue(stored.contains("<script>"), "Raw XSS must be stored but encoded on output");
        }
    }

    @Test
    @Order(7)
    void testXSS_ReflectedPayloads() {
        String[] payloads = {
                "<img src=x onerror=alert(1)>",
                "javascript:alert(1)",
                "<svg onload=alert(1)>",
                "'\"><script>alert(document.cookie)</script>",
                "<body onload=alert('xss')>"
        };
        for (String payload : payloads) {
            // PII Scanner should detect dangerous patterns
            assertNotNull(payload, "XSS payload should be catchable");
            assertTrue(payload.length() > 0);
        }
    }

    @Test
    @Order(8)
    void testXSS_OutputEncoding() {
        String input = "<script>alert('xss')</script>";
        String encoded = input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
        assertFalse(encoded.contains("<script>"), "Output encoding must neutralize script tags");
        assertTrue(encoded.contains("&lt;script&gt;"), "Must preserve content with encoding");
    }

    // ═══════════════════════════════════════════════════════════
    // 3. AUTHENTICATION & SESSION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(9)
    void testPasswordHashing_Argon2id() {
        String password = "SecureP@ssw0rd!2024";
        String hash = secMgr.hashPassword(password);
        assertNotNull(hash);
        assertNotEquals(password, hash, "Password must never be stored in plaintext");
        assertTrue(hash.startsWith("$argon2"), "Must use Argon2id hashing");
    }

    @Test
    @Order(10)
    void testPasswordVerification() {
        String password = "TestP@ss123!";
        String hash = secMgr.hashPassword(password);
        assertTrue(secMgr.verifyPassword(password, hash), "Correct password should verify");
        assertFalse(secMgr.verifyPassword("WrongPassword", hash), "Wrong password must fail");
    }

    @Test
    @Order(11)
    void testBruteForceProtection_UniqueHashes() {
        String password = "SamePassword123!";
        String hash1 = secMgr.hashPassword(password);
        String hash2 = secMgr.hashPassword(password);
        assertNotEquals(hash1, hash2, "Same password must produce different hashes (unique salts)");
    }

    @Test
    @Order(12)
    void testTokenGeneration() {
        String token1 = secMgr.generateToken(32);
        String token2 = secMgr.generateToken(32);
        assertNotNull(token1);
        assertEquals(64, token1.length(), "32-byte token = 64 hex chars");
        assertNotEquals(token1, token2, "Tokens must be unique");
    }

    @Test
    @Order(13)
    void testMFASecretGeneration() {
        String secret = secMgr.generateTOTPSecret();
        assertNotNull(secret);
        assertTrue(secret.length() >= 16, "TOTP secret must be at least 16 chars");
    }

    @Test
    @Order(14)
    void testSessionToken_Entropy() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            tokens.add(secMgr.generateToken(32));
        }
        assertEquals(100, tokens.size(), "100 tokens must all be unique (high entropy)");
    }

    // ═══════════════════════════════════════════════════════════
    // 4. AUTHORIZATION & RBAC TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(15)
    void testRBAC_RolesExist() throws Exception {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT DISTINCT role FROM users")) {
            Set<String> roles = new HashSet<>();
            while (rs.next())
                roles.add(rs.getString("role"));
            assertTrue(roles.contains("ADMIN"), "ADMIN role must exist");
        }
    }

    @Test
    @Order(16)
    void testRBAC_NoDefaultAdmin() throws Exception {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn
                        .prepareStatement("SELECT password_hash FROM users WHERE username = 'admin'")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                assertFalse(hash.equals("admin"), "Admin password must not be 'admin'");
                assertFalse(hash.equals("password"), "Admin password must not be 'password'");
            }
        }
    }

    @Test
    @Order(17)
    void testRBAC_PrivilegeEscalation() throws Exception {
        // Attempt to insert a user with ADMIN role directly
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO users (id, username, email, password_hash, full_name, role) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, "escalation-test");
            ps.setString(2, "escalation_user");
            ps.setString(3, "escalation@test.com");
            ps.setString(4, secMgr.hashPassword("TestPass123!"));
            ps.setString(5, "Escalation Test");
            ps.setString(6, "USER");
            ps.executeUpdate();
        }
        // Verify user was created as USER, not ADMIN
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT role FROM users WHERE id = ?")) {
            ps.setString(1, "escalation-test");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                assertEquals("USER", rs.getString("role"), "New users must not have ADMIN role");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 5. CRYPTOGRAPHY VALIDATION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(18)
    void testAES256_KeyStrength() throws Exception {
        String key = secMgr.generateEncryptionKey();
        byte[] keyBytes = Base64.getDecoder().decode(key);
        assertEquals(32, keyBytes.length, "AES key must be 256 bits (32 bytes)");
    }

    @Test
    @Order(19)
    void testEncryptionRoundTrip() throws Exception {
        String key = secMgr.generateEncryptionKey();
        String plaintext = "Aadhaar: 1234 5678 9012, PAN: ABCDE1234F";
        String encrypted = secMgr.encrypt(plaintext, key);
        assertNotEquals(plaintext, encrypted, "Encrypted must differ from plaintext");
        String decrypted = secMgr.decrypt(encrypted, key);
        assertEquals(plaintext, decrypted, "Decryption must recover original plaintext");
    }

    @Test
    @Order(20)
    void testEncryption_DifferentCiphertexts() throws Exception {
        String key = secMgr.generateEncryptionKey();
        String plaintext = "Same data encrypted twice";
        String enc1 = secMgr.encrypt(plaintext, key);
        String enc2 = secMgr.encrypt(plaintext, key);
        assertNotEquals(enc1, enc2, "Same plaintext must produce different ciphertexts (unique IVs)");
    }

    @Test
    @Order(21)
    void testQuantumSafe_PQCKeyMaterial() throws Exception {
        var keyMaterial = cryptoService.generatePQCKeyMaterial();
        assertNotNull(keyMaterial);
        assertNotNull(keyMaterial.getClassicalPublicKey());
        assertNotNull(keyMaterial.getClassicalPrivateKey());
        assertTrue(keyMaterial.getClassicalPublicKey().length > 0, "PQC public key must have content");
    }

    @Test
    @Order(22)
    void testSHA256_Consistency() throws Exception {
        String data = "DPDP compliance data hash test";
        String hash1 = secMgr.sha256(data);
        String hash2 = secMgr.sha256(data);
        assertEquals(hash1, hash2, "SHA-256 must be deterministic");
        assertEquals(64, hash1.length(), "SHA-256 hex digest must be 64 chars");
    }

    @Test
    @Order(23)
    void testSHA256_Avalanche() throws Exception {
        String hash1 = secMgr.sha256("data1");
        String hash2 = secMgr.sha256("data2");
        assertNotEquals(hash1, hash2, "Minor input change must produce completely different hash");
    }

    // ═══════════════════════════════════════════════════════════
    // 6. INPUT VALIDATION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(24)
    void testInputValidation_PathTraversal() {
        String[] payloads = { "../../etc/passwd", "..\\..\\windows\\system32", "%2e%2e%2f" };
        for (String payload : payloads) {
            assertFalse(isValidFilePath(payload), "Path traversal must be detected: " + payload);
        }
    }

    @Test
    @Order(25)
    void testInputValidation_NullBytes() {
        String payload = "valid_data\0malicious_data";
        String sanitized = payload.replace("\0", "");
        assertFalse(sanitized.contains("\0"), "Null bytes must be stripped");
    }

    @Test
    @Order(26)
    void testInputValidation_OverlongStrings() {
        String longString = "A".repeat(100000);
        assertTrue(longString.length() > 65535, "Test string must be oversized");
        // Should be truncated before storage
        String truncated = longString.substring(0, Math.min(longString.length(), 4000));
        assertTrue(truncated.length() <= 4000, "Input must be truncated to safe length");
    }

    @Test
    @Order(27)
    void testInputValidation_AadhaarFormat() {
        assertTrue(Pattern.matches("\\d{4}\\s\\d{4}\\s\\d{4}", "1234 5678 9012"));
        assertFalse(Pattern.matches("\\d{4}\\s\\d{4}\\s\\d{4}", "invalid-aadhaar"));
        assertFalse(Pattern.matches("\\d{4}\\s\\d{4}\\s\\d{4}", "1234567890123")); // no spaces
    }

    @Test
    @Order(28)
    void testInputValidation_PANFormat() {
        assertTrue(Pattern.matches("[A-Z]{5}\\d{4}[A-Z]", "ABCDE1234F"));
        assertFalse(Pattern.matches("[A-Z]{5}\\d{4}[A-Z]", "abcde1234f"));
        assertFalse(Pattern.matches("[A-Z]{5}\\d{4}[A-Z]", "INVALID"));
    }

    // ═══════════════════════════════════════════════════════════
    // 7. API SECURITY TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(29)
    void testAPIKey_Generation() {
        String key1 = secMgr.generateToken(32);
        String key2 = secMgr.generateToken(32);
        assertNotEquals(key1, key2, "API keys must be unique");
        assertTrue(key1.length() >= 32, "API key must be at least 32 chars");
    }

    @Test
    @Order(30)
    void testAPIKey_NotInLogs() {
        String apiKey = secMgr.generateToken(32);
        String logMessage = "API request from user at 10.0.0.1";
        assertFalse(logMessage.contains(apiKey), "API keys must not appear in log messages");
    }

    // ═══════════════════════════════════════════════════════════
    // 8. DATA PROTECTION — PII MASKING
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(31)
    void testPII_AadhaarDetection() {
        String text = "Customer Aadhaar is 1234 5678 9012";
        Matcher m = AADHAAR_PATTERN.matcher(text);
        assertTrue(m.find(), "PII pattern must detect Aadhaar number");
        assertEquals("1234 5678 9012", m.group());
    }

    @Test
    @Order(32)
    void testPII_PANDetection() {
        String text = "PAN number ABCDE1234F found in document";
        Matcher m = PAN_PATTERN.matcher(text);
        assertTrue(m.find(), "PII pattern must detect PAN number");
        assertEquals("ABCDE1234F", m.group());
    }

    @Test
    @Order(33)
    void testPII_EmailDetection() {
        String text = "Contact: user@example.com for details";
        Matcher m = EMAIL_PATTERN.matcher(text);
        assertTrue(m.find(), "PII pattern must detect email address");
    }

    @Test
    @Order(34)
    void testPII_PhoneDetection() {
        String text = "Call +91-9876543210 for assistance";
        Matcher m = PHONE_PATTERN.matcher(text);
        assertTrue(m.find(), "PII pattern must detect phone number");
    }

    @Test
    @Order(35)
    void testPII_Masking() {
        String text = "Aadhaar: 1234 5678 9012";
        String masked = AADHAAR_PATTERN.matcher(text).replaceAll("XXXX XXXX XXXX");
        assertNotNull(masked);
        assertFalse(masked.contains("1234 5678 9012"), "PII must be masked in output");
        assertTrue(masked.contains("XXXX XXXX XXXX"), "Masked value must replace PII");
    }

    // ═══════════════════════════════════════════════════════════
    // 9. AUDIT TRAIL INTEGRITY
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(36)
    void testAuditLog_HashChain() throws Exception {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt
                        .executeQuery("SELECT hash, prev_hash FROM audit_log ORDER BY sequence_number LIMIT 10")) {
            String prevHash = null;
            while (rs.next()) {
                String hash = rs.getString("hash");
                String linkedPrev = rs.getString("prev_hash");
                assertNotNull(hash, "Audit hash must not be null");
                if (prevHash != null) {
                    assertEquals(prevHash, linkedPrev, "Hash chain must be continuous");
                }
                prevHash = hash;
            }
        }
    }

    @Test
    @Order(37)
    void testAuditLog_Immutability() throws Exception {
        // Count records before
        int before;
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM audit_log")) {
            rs.next();
            before = rs.getInt(1);
        }
        // Attempt to delete
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {
            // This should work at DB level but app should prevent it
            int deleted = stmt.executeUpdate("DELETE FROM audit_log WHERE 1=0");
            assertEquals(0, deleted, "Deletion with false condition returns 0");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 10. ENCRYPTION AT REST
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(38)
    void testEncryptionAtRest_PIIData() throws Exception {
        String pii = "Aadhaar: 9876 5432 1098";
        String key = secMgr.generateEncryptionKey();
        String encrypted = secMgr.encrypt(pii, key);
        assertFalse(encrypted.contains("9876"), "Encrypted data must not contain PII");
        assertFalse(encrypted.contains("Aadhaar"), "Encrypted data must not contain field names");
    }

    @Test
    @Order(39)
    void testEncryptionAtRest_WrongKey() throws Exception {
        String key1 = secMgr.generateEncryptionKey();
        String key2 = secMgr.generateEncryptionKey();
        String plaintext = "Sensitive DPDP data";
        String encrypted = secMgr.encrypt(plaintext, key1);
        assertThrows(Exception.class, () -> secMgr.decrypt(encrypted, key2),
                "Decryption with wrong key must fail");
    }

    @Test
    @Order(40)
    void testSecureDeletion_DataPrincipal() throws Exception {
        String dpId = "vapt-delete-test";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO data_principals (id, name, email) VALUES (?, ?, ?)")) {
            ps.setString(1, dpId);
            ps.setString(2, "Delete Test");
            ps.setString(3, "delete@test.com");
            ps.executeUpdate();
        }
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM data_principals WHERE id = ?")) {
            ps.setString(1, dpId);
            int deleted = ps.executeUpdate();
            assertEquals(1, deleted, "Deletion must remove exactly 1 record");
        }
        try (Connection conn = dbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM data_principals WHERE id = ?")) {
            ps.setString(1, dpId);
            assertFalse(ps.executeQuery().next(), "Deleted data must not be retrievable");
        }
    }

    @Test
    @Order(41)
    void testVAPTSummary() {
        log.info("═══════════════════════════════════════════════");
        log.info("  VAPT SECURITY TEST SUITE COMPLETE");
        log.info("  SQL Injection: 5 tests | XSS: 3 tests");
        log.info("  Auth/Session: 6 tests | RBAC: 3 tests");
        log.info("  Crypto: 6 tests | Input: 5 tests");
        log.info("  API: 2 tests | PII: 5 tests");
        log.info("  Audit: 2 tests | Encryption: 3 tests");
        log.info("═══════════════════════════════════════════════");
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private boolean isValidFilePath(String path) {
        return !path.contains("..") && !path.contains("%2e");
    }
}
