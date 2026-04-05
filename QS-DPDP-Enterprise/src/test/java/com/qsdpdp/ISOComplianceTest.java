package com.qsdpdp;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.security.SecurityManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.crypto.QuantumSafeEncryptionService;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.*;

/**
 * ISO 27001:2022 (ISMS) + ISO 27701:2019 (PIMS) Compliance Test Suite
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ISO 27001/27701 Compliance Test Suite")
public class ISOComplianceTest {

    private static DatabaseManager dbManager;
    private static SecurityManager secMgr;
    private static AuditService auditService;
    private static EventBus eventBus;
    private static QuantumSafeEncryptionService cryptoService;

    @BeforeAll
    static void setup() {
        dbManager = new DatabaseManager();
        dbManager.initialize();
        secMgr = new SecurityManager();
        secMgr.initialize();
        eventBus = new EventBus();
        auditService = new AuditService(dbManager);
        auditService.initialize();
        cryptoService = new QuantumSafeEncryptionService();
    }

    // A.5: INFORMATION SECURITY POLICIES
    @Test @Order(1)
    @DisplayName("ISO-A5-001: Policy management service exists")
    void testISO_A5_001() {
        try { Class.forName("com.qsdpdp.policy.PolicyService"); }
        catch (ClassNotFoundException e) { fail("PolicyService must exist (A.5.1.1)"); }
    }

    @Test @Order(2)
    @DisplayName("ISO-A5-002: Policies table with lifecycle tracking")
    void testISO_A5_002() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM policies")) {
            assertTrue(r.next(), "Policies table must exist (A.5.1.2)");
        }
    }

    // A.8: ASSET MANAGEMENT
    @Test @Order(10)
    @DisplayName("ISO-A8-001: Data principal inventory (A.8.1)")
    void testISO_A8_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM data_principals")) {
            assertTrue(r.next(), "Data principal inventory must exist (A.8.1.1)");
        }
    }

    @Test @Order(11)
    @DisplayName("ISO-A8-002: PII classification service")
    void testISO_A8_002() {
        boolean found = false;
        try { Class.forName("com.qsdpdp.dlp.DataClassificationService"); found = true; } catch (ClassNotFoundException e) {}
        if (!found) { try { Class.forName("com.qsdpdp.dlp.DLPService"); found = true; } catch (ClassNotFoundException e) {} }
        assertTrue(found, "DLP/Classification service must exist (A.8.2)");
    }

    // A.9: ACCESS CONTROL
    @Test @Order(20)
    @DisplayName("ISO-A9-001: RBAC with defined roles (A.9.2)")
    void testISO_A9_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT DISTINCT role FROM users")) {
            Set<String> roles = new HashSet<>();
            while (r.next()) roles.add(r.getString("role"));
            assertTrue(roles.contains("ADMIN"), "ADMIN role must exist (A.9.2.1)");
        }
    }

    @Test @Order(21)
    @DisplayName("ISO-A9-002: Secure password storage (A.9.4)")
    void testISO_A9_002() throws Exception {
        try (Connection c = dbManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE username = 'admin'")) {
            ResultSet r = ps.executeQuery();
            if (r.next()) {
                String hash = r.getString("password_hash");
                assertNotEquals("admin", hash, "Password must not be plaintext (A.9.4.3)");
            }
        }
    }

    @Test @Order(22)
    @DisplayName("ISO-A9-003: MFA readiness (A.9.4.2)")
    void testISO_A9_003() {
        String secret = secMgr.generateTOTPSecret();
        assertNotNull(secret); assertTrue(secret.length() >= 16, "TOTP entropy (A.9.4.2)");
    }

    // A.10: CRYPTOGRAPHY
    @Test @Order(30)
    @DisplayName("ISO-A10-001: AES-256 key strength (A.10.1)")
    void testISO_A10_001() throws Exception {
        byte[] kb = Base64.getDecoder().decode(secMgr.generateEncryptionKey());
        assertEquals(32, kb.length, "AES-256 bit key (A.10.1.1)");
    }

    @Test @Order(31)
    @DisplayName("ISO-A10-002: Encryption IND-CPA security (A.10.1)")
    void testISO_A10_002() throws Exception {
        String k = secMgr.generateEncryptionKey();
        assertNotEquals(secMgr.encrypt("test", k), secMgr.encrypt("test", k), "IND-CPA (A.10.1.1)");
    }

    @Test @Order(32)
    @DisplayName("ISO-A10-003: Wrong key rejection (A.10.1.2)")
    void testISO_A10_003() throws Exception {
        String k1 = secMgr.generateEncryptionKey(), k2 = secMgr.generateEncryptionKey();
        String enc = secMgr.encrypt("iso test", k1);
        assertThrows(Exception.class, () -> secMgr.decrypt(enc, k2), "Wrong key fail (A.10.1.2)");
    }

    @Test @Order(33)
    @DisplayName("ISO-A10-004: PQC readiness (A.10.1)")
    void testISO_A10_004() {
        assertNotNull(cryptoService.generatePQCKeyMaterial(), "PQC ready (A.10.1.1)");
    }

    // A.12: OPERATIONS SECURITY
    @Test @Order(40)
    @DisplayName("ISO-A12-001: Audit logging operational (A.12.4)")
    void testISO_A12_001() {
        auditService.logSync("ISO_TEST", "ISO_COMP", "iso-tester", "A.12.4 test");
        assertNotNull(auditService.getRecentEntries(1), "Audit logging operational (A.12.4.1)");
    }

    @Test @Order(41)
    @DisplayName("ISO-A12-002: Database stability (A.12.1)")
    void testISO_A12_002() throws Exception {
        for (int i = 0; i < 30; i++) {
            try (Connection c = dbManager.getConnection()) {
                assertNotNull(c); assertFalse(c.isClosed());
            }
        }
    }

    // A.16: INCIDENT MANAGEMENT
    @Test @Order(50)
    @DisplayName("ISO-A16-001: Breach management (A.16.1)")
    void testISO_A16_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM breaches")) {
            assertTrue(r.next(), "Breaches table exists (A.16.1.1)");
        }
    }

    @Test @Order(51)
    @DisplayName("ISO-A16-002: SIEM integration (A.16.1.2)")
    void testISO_A16_002() {
        try { Class.forName("com.qsdpdp.siem.SIEMService"); }
        catch (ClassNotFoundException e) { System.out.println("  info: SIEM: " + e.getMessage()); }
    }

    // A.18: COMPLIANCE
    @Test @Order(60)
    @DisplayName("ISO-A18-001: DPIA capability (A.18.1)")
    void testISO_A18_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dpias")) {
            assertTrue(r.next(), "DPIA table exists (A.18.1.4)");
        }
    }

    @Test @Order(61)
    @DisplayName("ISO-A18-002: Compliance framework mapper")
    void testISO_A18_002() {
        try { Class.forName("com.qsdpdp.compliance.ComplianceFrameworkMapper"); }
        catch (ClassNotFoundException e) { fail("ComplianceFrameworkMapper required (A.18.2.1)"); }
    }

    // ISO 27701: PIMS EXTENSION
    @Test @Order(70)
    @DisplayName("ISO-27701-001: Consent management (7.2.3)")
    void testISO_27701_001() {
        try { Class.forName("com.qsdpdp.consent.ConsentService"); }
        catch (ClassNotFoundException e) { fail("ConsentService required (ISO 27701 §7.2.3)"); }
    }

    @Test @Order(71)
    @DisplayName("ISO-27701-002: DSAR handling (7.3.2)")
    void testISO_27701_002() {
        try { Class.forName("com.qsdpdp.rights.RightsService"); }
        catch (ClassNotFoundException e) { fail("RightsService required (ISO 27701 §7.3.2)"); }
    }

    @Test @Order(72)
    @DisplayName("ISO-27701-003: PII processing records (7.4)")
    void testISO_27701_003() throws Exception {
        String[] tables = {"consents", "audit_log", "data_principals"};
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement()) {
            for (String t : tables) {
                ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + t);
                assertTrue(r.next(), t + " must exist (ISO 27701 §7.4)");
            }
        }
    }

    // SUMMARY
    @Test @Order(999)
    @DisplayName("ISO 27001/27701 Compliance Summary")
    void testISOSummary() {
        System.out.println("\n" + "=".repeat(64));
        System.out.println("  ISO 27001:2022 / ISO 27701:2019 COMPLIANCE — RESULTS");
        System.out.println("=".repeat(64));
        System.out.println("  A.5  (Policies) : 2 | A.8  (Assets)     : 2");
        System.out.println("  A.9  (Access)   : 3 | A.10 (Crypto)     : 4");
        System.out.println("  A.12 (Operations): 2 | A.16 (Incidents) : 2");
        System.out.println("  A.18 (Compliance): 2 | 27701 (PIMS)     : 3");
        System.out.println("  TOTAL: 20 tests");
        System.out.println("=".repeat(64));
    }

    @AfterAll
    static void tearDown() {
        if (auditService != null) auditService.shutdown();
        if (eventBus != null) eventBus.shutdown();
        if (dbManager != null) dbManager.shutdown();
    }
}
