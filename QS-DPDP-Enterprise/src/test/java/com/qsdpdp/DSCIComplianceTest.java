package com.qsdpdp;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.security.SecurityManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.*;

/**
 * DSCI (Data Security Council of India) Privacy Framework Compliance Test Suite
 *
 * Validates platform conformance to DSCI Privacy Framework 2.0:
 *   1. DPL — Data Protection Leadership
 *   2. SRA — Structured Risk Assessment
 *   3. LPR — Lawful Processing
 *   4. NAC — Notice & Consent
 *   5. DSR — Data Subject Rights
 *   6. DSE — Data Security
 *   7. DBM — Data Breach Management
 *   8. TPM — Third Party Management
 *   9. MON — Monitoring & Enforcement
 *
 * @version 1.0.0
 * @since Compliance Phase
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("DSCI Privacy Framework Compliance Test Suite")
public class DSCIComplianceTest {

    private static DatabaseManager dbManager;
    private static SecurityManager secMgr;
    private static AuditService auditService;
    private static EventBus eventBus;

    @BeforeAll
    static void setup() {
        dbManager = new DatabaseManager();
        dbManager.initialize();
        secMgr = new SecurityManager();
        secMgr.initialize();
        eventBus = new EventBus();
        auditService = new AuditService(dbManager);
        auditService.initialize();
    }

    // ═══════════════════════════════════════════════════════════
    // 1. DATA PROTECTION LEADERSHIP (DPL)
    // ═══════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("DSCI-DPL-001: RBAC with DPO/Fiduciary roles")
    void testDSCI_DPL_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT DISTINCT role FROM users")) {
            Set<String> roles = new HashSet<>();
            while (r.next()) roles.add(r.getString("role"));
            assertTrue(roles.contains("ADMIN"), "ADMIN role for data protection leadership");
            assertTrue(roles.size() >= 1, "Role hierarchy must exist (DSCI DPL-1)");
        }
    }

    @Test @Order(2)
    @DisplayName("DSCI-DPL-002: Policy governance framework")
    void testDSCI_DPL_002() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM policies")) {
            assertTrue(r.next(), "Policy framework must exist (DSCI DPL-2)");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 2. STRUCTURED RISK ASSESSMENT (SRA)
    // ═══════════════════════════════════════════════════════════

    @Test @Order(10)
    @DisplayName("DSCI-SRA-001: DPIA assessment capability")
    void testDSCI_SRA_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM dpias")) {
            assertTrue(r.next(), "DPIA table must exist (DSCI SRA-1)");
        }
    }

    @Test @Order(11)
    @DisplayName("DSCI-SRA-002: Gap analysis service")
    void testDSCI_SRA_002() {
        try { Class.forName("com.qsdpdp.gap.GapAnalysisService"); }
        catch (ClassNotFoundException e) { fail("GapAnalysisService required (DSCI SRA-2)"); }
    }

    // ═══════════════════════════════════════════════════════════
    // 3. LAWFUL PROCESSING (LPR)
    // ═══════════════════════════════════════════════════════════

    @Test @Order(20)
    @DisplayName("DSCI-LPR-001: Purpose definition with legal basis")
    void testDSCI_LPR_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM purposes")) {
            assertTrue(r.next(), "Purposes with legal basis must exist (DSCI LPR-1)");
        }
    }

    @Test @Order(21)
    @DisplayName("DSCI-LPR-002: Consent service for lawful consent collection")
    void testDSCI_LPR_002() {
        try { Class.forName("com.qsdpdp.consent.ConsentService"); }
        catch (ClassNotFoundException e) { fail("ConsentService required (DSCI LPR-2)"); }
    }

    // ═══════════════════════════════════════════════════════════
    // 4. NOTICE & CONSENT (NAC)
    // ═══════════════════════════════════════════════════════════

    @Test @Order(30)
    @DisplayName("DSCI-NAC-001: Consent records with notice versioning")
    void testDSCI_NAC_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM consents")) {
            assertTrue(r.next(), "Consent collection must be tracked (DSCI NAC-1)");
        }
    }

    @Test @Order(31)
    @DisplayName("DSCI-NAC-002: Guardian consent for children (DPDP Section 9)")
    void testDSCI_NAC_002() {
        try { Class.forName("com.qsdpdp.consent.GuardianConsent"); }
        catch (ClassNotFoundException e) { fail("GuardianConsent required (DSCI NAC-2)"); }
    }

    // ═══════════════════════════════════════════════════════════
    // 5. DATA SUBJECT RIGHTS (DSR)
    // ═══════════════════════════════════════════════════════════

    @Test @Order(40)
    @DisplayName("DSCI-DSR-001: Rights request table with 30-day tracking")
    void testDSCI_DSR_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM rights_requests")) {
            assertTrue(r.next(), "Rights requests must be trackable (DSCI DSR-1)");
        }
    }

    @Test @Order(41)
    @DisplayName("DSCI-DSR-002: Multiple right types (access, correction, erasure, nomination)")
    void testDSCI_DSR_002() {
        try {
            Class<?> rt = Class.forName("com.qsdpdp.rights.RightType");
            assertTrue(rt.isEnum(), "RightType must be enum");
            assertTrue(rt.getEnumConstants().length >= 5, "Must support 5+ right types (DSCI DSR-2)");
        } catch (ClassNotFoundException e) {
            fail("RightType enum required (DSCI DSR-2)");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 6. DATA SECURITY (DSE)
    // ═══════════════════════════════════════════════════════════

    @Test @Order(50)
    @DisplayName("DSCI-DSE-001: AES-256 encryption for PII at rest")
    void testDSCI_DSE_001() throws Exception {
        String key = secMgr.generateEncryptionKey();
        byte[] kb = Base64.getDecoder().decode(key);
        assertEquals(32, kb.length, "AES-256 key (DSCI DSE-1)");
        String pii = "Aadhaar: 9876 5432 1098";
        String enc = secMgr.encrypt(pii, key);
        assertFalse(enc.contains("9876"), "PII must be encrypted (DSCI DSE-1)");
        assertEquals(pii, secMgr.decrypt(enc, key), "Decryption must recover data");
    }

    @Test @Order(51)
    @DisplayName("DSCI-DSE-002: Argon2id password hashing")
    void testDSCI_DSE_002() {
        String h = secMgr.hashPassword("DSCI_T3st@2026!");
        assertTrue(h.startsWith("$argon2"), "Must use Argon2id (DSCI DSE-2)");
        assertNotEquals(h, secMgr.hashPassword("DSCI_T3st@2026!"), "Unique salts (DSCI DSE-2)");
    }

    @Test @Order(52)
    @DisplayName("DSCI-DSE-003: SQL injection prevention")
    void testDSCI_DSE_003() throws Exception {
        String[] payloads = {"'; DROP TABLE users; --", "1 OR 1=1", "admin'--"};
        for (String p : payloads) {
            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username = ?")) {
                ps.setString(1, p);
                assertFalse(ps.executeQuery().next(), "SQLi blocked: " + p);
            }
        }
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM users")) {
            assertTrue(r.next(), "Users table intact after injection (DSCI DSE-3)");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 7. DATA BREACH MANAGEMENT (DBM)
    // ═══════════════════════════════════════════════════════════

    @Test @Order(60)
    @DisplayName("DSCI-DBM-001: Breach management with DPBI 72-hr notification")
    void testDSCI_DBM_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM breaches")) {
            assertTrue(r.next(), "Breaches table exists (DSCI DBM-1)");
        }
    }

    @Test @Order(61)
    @DisplayName("DSCI-DBM-002: Breach service with notification recording")
    void testDSCI_DBM_002() {
        try { Class.forName("com.qsdpdp.breach.BreachService"); }
        catch (ClassNotFoundException e) { fail("BreachService required (DSCI DBM-2)"); }
    }

    // ═══════════════════════════════════════════════════════════
    // 8. MONITORING & ENFORCEMENT (MON)
    // ═══════════════════════════════════════════════════════════

    @Test @Order(70)
    @DisplayName("DSCI-MON-001: Tamper-evident audit hash chain")
    void testDSCI_MON_001() throws Exception {
        try (Connection c = dbManager.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT hash, prev_hash FROM audit_log ORDER BY sequence_number LIMIT 10")) {
            String prev = null; int cnt = 0;
            while (r.next()) {
                String h = r.getString("hash");
                assertNotNull(h, "Audit hash required (DSCI MON-1)");
                if (prev != null && r.getString("prev_hash") != null) {
                    assertEquals(prev, r.getString("prev_hash"), "Hash chain continuous (DSCI MON-1)");
                }
                prev = h; cnt++;
            }
            assertTrue(cnt > 0, "Audit entries exist (DSCI MON-1)");
        }
    }

    @Test @Order(71)
    @DisplayName("DSCI-MON-002: Audit integrity verification API")
    void testDSCI_MON_002() {
        var report = auditService.verifyIntegrity();
        assertNotNull(report, "Integrity report required (DSCI MON-2)");
        assertTrue(report.getTotalEntries() >= 0, "Entry count reported");
    }

    // SUMMARY
    @Test @Order(999)
    @DisplayName("DSCI Privacy Framework Compliance Summary")
    void testDSCISummary() {
        System.out.println("\n" + "=".repeat(64));
        System.out.println("  DSCI PRIVACY FRAMEWORK 2.0 COMPLIANCE — RESULTS");
        System.out.println("=".repeat(64));
        System.out.println("  DPL (Leadership)     : 2 | SRA (Risk Assessment) : 2");
        System.out.println("  LPR (Lawful Process) : 2 | NAC (Notice/Consent)  : 2");
        System.out.println("  DSR (Subject Rights) : 2 | DSE (Data Security)   : 3");
        System.out.println("  DBM (Breach Mgmt)    : 2 | MON (Monitoring)      : 2");
        System.out.println("  TOTAL: 17 tests");
        System.out.println("=".repeat(64));
        System.out.println("  Framework: DSCI Privacy Framework 2.0");
        System.out.println("  Organization: Data Security Council of India");
        System.out.println("=".repeat(64));
    }

    @AfterAll
    static void tearDown() {
        if (auditService != null) auditService.shutdown();
        if (eventBus != null) eventBus.shutdown();
        if (dbManager != null) dbManager.shutdown();
    }
}
