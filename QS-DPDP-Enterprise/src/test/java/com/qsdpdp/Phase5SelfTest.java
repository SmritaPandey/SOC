package com.qsdpdp;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.security.SecurityManager;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.crypto.QuantumSafeEncryptionService;
import com.qsdpdp.crypto.QuantumSafeEncryptionService.*;
import com.qsdpdp.lifecycle.DataLifecycleService;
import com.qsdpdp.lifecycle.DataLifecycleService.*;
import com.qsdpdp.training.TrainingService;
import com.qsdpdp.training.TrainingService.*;
import com.qsdpdp.vendor.VendorRiskService;
import com.qsdpdp.vendor.VendorRiskService.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Phase 5 Self-Test Suite — Crypto, Lifecycle, Training, Vendor Risk
 * Validates quantum-safe encryption, data lifecycle management,
 * training/awareness, and vendor risk assessment.
 *
 * @version 1.0.0
 * @since Phase 5
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase5SelfTest {

    private static DatabaseManager dbManager;
    private static SecurityManager securityManager;
    private static EventBus eventBus;
    private static AuditService auditService;
    private static QuantumSafeEncryptionService cryptoService;
    private static DataLifecycleService lifecycleService;
    private static TrainingService trainingService;
    private static VendorRiskService vendorService;

    private static int passed = 0;
    private static int failed = 0;

    @BeforeAll
    static void setup() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   PHASE 5 SELF-TEST — Crypto / Lifecycle / Train / Vendor ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        dbManager = new DatabaseManager();
        dbManager.initialize();

        securityManager = new SecurityManager();
        securityManager.initialize();

        eventBus = new EventBus();
        eventBus.initialize();

        auditService = new AuditService(dbManager);
        auditService.initialize();

        cryptoService = new QuantumSafeEncryptionService();
        cryptoService.initialize();

        lifecycleService = new DataLifecycleService(dbManager, auditService);
        lifecycleService.initialize();

        trainingService = new TrainingService(dbManager, auditService);
        trainingService.initialize();

        vendorService = new VendorRiskService(dbManager, auditService);
        vendorService.initialize();

        System.out.println("✓ All Phase 5 services initialized");
    }

    @AfterAll
    static void teardown() {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  Phase 5 Results: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("  Total: " + (passed + failed) + " tests");
        System.out.println("═══════════════════════════════════════════════════════════");
        if (lifecycleService != null)
            lifecycleService.shutdown();
        if (dbManager != null)
            dbManager.shutdown();
    }

    private void pass(String name) {
        passed++;
        System.out.println("  ✅ " + name);
    }

    private void fail(String name, Exception e) {
        failed++;
        System.out.println("  ❌ " + name + " — " + e.getMessage());
    }

    // ═══════════════════════════════════════════════════════════
    // QUANTUM-SAFE ENCRYPTION TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void testCryptoServiceInitialization() {
        String test = "Crypto Service Initialization";
        try {
            assertNotNull(cryptoService, "QuantumSafeEncryptionService must not be null");
            assertTrue(cryptoService.isInitialized(), "CryptoService must be initialized");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(2)
    void testAESEncryptDecrypt() throws Exception {
        String test = "AES-256-GCM Encrypt/Decrypt Roundtrip";
        try {
            SecretKey key = cryptoService.generateAESKey();
            assertNotNull(key, "AES key must not be null");

            String plaintext = "Aadhaar: 1234-5678-9012, PAN: ABCDE1234F";
            EncryptedData encrypted = cryptoService.encryptAES(plaintext, key);
            assertNotNull(encrypted);
            assertNotNull(encrypted.getCiphertext());
            assertNotNull(encrypted.getIv());
            assertEquals("AES-256-GCM", encrypted.getAlgorithm());

            String decrypted = cryptoService.decryptAES(encrypted, key);
            assertEquals(plaintext, decrypted, "Decrypted text must match original");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(3)
    void testHybridEncryption() throws Exception {
        String test = "Hybrid Classical + PQC Encryption";
        try {
            String sensitive = "Patient Health ID: ABHA-123456789012, Diagnosis: Hypertension";
            HybridEncryptedData hybrid = cryptoService.encryptHybrid(sensitive);
            assertNotNull(hybrid);
            assertNotNull(hybrid.getCiphertext());
            assertNotNull(hybrid.getIv());
            assertNotNull(hybrid.getRsaWrappedKey());
            assertNotNull(hybrid.getAlgorithm());

            String decrypted = cryptoService.decryptHybrid(hybrid);
            assertEquals(sensitive, decrypted, "Hybrid decryption must recover original");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(4)
    void testPQCKeyMaterial() throws Exception {
        String test = "PQC Key Material Generation (ML-KEM prep)";
        try {
            PQCKeyMaterial keyMaterial = cryptoService.generatePQCKeyMaterial();
            assertNotNull(keyMaterial, "PQC key material must not be null");
            assertNotNull(keyMaterial.getClassicalPublicKey());
            assertNotNull(keyMaterial.getClassicalPrivateKey());
            assertNotNull(keyMaterial.getPqcComponent());
            assertNotNull(keyMaterial.getAlgorithm());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(5)
    void testQuantumSafeEncryption() throws Exception {
        String test = "Quantum-Safe Encryption — Full Pipeline";
        try {
            String plaintext = "Sensitive DPDP data requiring quantum-safe protection";
            HybridEncryptedData qsEncrypted = cryptoService.encryptHybrid(plaintext);
            assertNotNull(qsEncrypted);
            assertNotNull(qsEncrypted.getCiphertext());
            assertTrue(qsEncrypted.getCiphertext().length > 0, "Quantum-safe ciphertext must have content");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(6)
    void testSHAHashing() throws Exception {
        String test = "SHA-256 & SHA-512 Hashing";
        try {
            String input = "data_principal_id:DP001:consent:active";
            String hash256 = cryptoService.hashSHA256(input);
            assertNotNull(hash256);
            assertEquals(64, hash256.length(), "SHA-256 hex must be 64 chars");

            String hash512 = cryptoService.hashSHA512(input);
            assertNotNull(hash512);
            assertEquals(128, hash512.length(), "SHA-512 hex must be 128 chars");

            // Deterministic hashing
            assertEquals(hash256, cryptoService.hashSHA256(input), "Same input must produce same hash");
            assertNotEquals(hash256, hash512, "SHA-256 and SHA-512 must differ");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(7)
    void testKeyDerivation() throws Exception {
        String test = "PBKDF2 Key Derivation";
        try {
            byte[] salt = cryptoService.generateSalt(16);
            assertNotNull(salt);
            assertEquals(16, salt.length);

            byte[] derived = cryptoService.deriveKey("StrongPassword@123", salt, 100000, 256);
            assertNotNull(derived, "Derived key must not be null");
            assertTrue(derived.length > 0, "Derived key must have length");

            // Same params must produce same key
            byte[] derived2 = cryptoService.deriveKey("StrongPassword@123", salt, 100000, 256);
            assertArrayEquals(derived, derived2,
                    "Same password + salt must produce same key");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(8)
    void testPIIEncryption() {
        String test = "PII Encryption — Double Encryption";
        try {
            String pii = "1234-5678-9012"; // Aadhaar number
            String encrypted = cryptoService.encryptPII(pii);
            assertNotNull(encrypted, "Encrypted PII string must not be null");
            assertTrue(encrypted.contains(":"), "Encrypted PII must contain keyId:ciphertext format");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(9)
    void testPIITokenization() {
        String test = "PII Tokenization";
        try {
            String pii = "ABCDE1234F"; // PAN
            String token = cryptoService.tokenizePII(pii, "KYC_VERIFICATION");
            assertNotNull(token);
            assertNotEquals(pii, token, "Token must not be the original PII");
            assertTrue(token.length() > 0);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(10)
    void testPIIMasking() {
        String test = "PII Masking — Multiple Types";
        try {
            // Aadhaar masking
            String aadhaarMasked = cryptoService.maskPII("123456789012", PIIMaskType.AADHAAR);
            assertNotNull(aadhaarMasked);
            assertNotEquals("123456789012", aadhaarMasked);

            // PAN masking
            String panMasked = cryptoService.maskPII("ABCDE1234F", PIIMaskType.PAN);
            assertNotNull(panMasked);
            assertNotEquals("ABCDE1234F", panMasked);

            // Email masking
            String emailMasked = cryptoService.maskPII("user@example.com", PIIMaskType.EMAIL);
            assertNotNull(emailMasked);
            assertNotEquals("user@example.com", emailMasked);

            // Phone masking
            String phoneMasked = cryptoService.maskPII("9876543210", PIIMaskType.PHONE);
            assertNotNull(phoneMasked);
            assertNotEquals("9876543210", phoneMasked);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(11)
    void testPublicKeyInfrastructure() throws Exception {
        String test = "Hybrid RSA+AES Public Key Infrastructure";
        try {
            // generateRSAKeyPair is private; test via encryptHybrid which uses it
            // internally
            var hybridData = cryptoService.encryptHybrid("Sensitive DPDP data for hybrid encryption test");
            assertNotNull(hybridData, "Hybrid encrypted data must not be null");
            assertNotNull(hybridData.getCiphertext(), "Hybrid ciphertext must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DATA LIFECYCLE MANAGEMENT TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(12)
    void testLifecycleServiceInitialization() {
        String test = "Data Lifecycle Service Initialization";
        try {
            assertNotNull(lifecycleService, "DataLifecycleService must not be null");
            assertTrue(lifecycleService.isInitialized());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(13)
    void testRetentionPolicyCreation() {
        String test = "Retention Policy — DPDP Compliant";
        try {
            RetentionPolicy policy = new RetentionPolicy(
                    "Consent Records", "CONSENT_DATA", "Legal Compliance",
                    1825, 365, "DPDP Section 6",
                    "Section 6 - Consent Management");
            assertNotNull(policy.getId());
            assertEquals("Consent Records", policy.getName());
            assertEquals("CONSENT_DATA", policy.getDataCategory());
            assertEquals(1825, policy.getRetentionDays()); // ~5 years
            assertEquals("DPDP Section 6", policy.getLegalBasis());

            lifecycleService.createPolicy(policy);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(14)
    void testErasureScheduling() {
        String test = "Data Erasure — Schedule & Execute";
        try {
            String jobId = lifecycleService.scheduleErasure(
                    "customer_db", "EXPIRED_CONSENT", "Consent withdrawn by data principal",
                    150, LocalDateTime.now().plusHours(1), "dpo@organization.com");
            assertNotNull(jobId, "Erasure job ID must not be null");

            // Execute erasure
            DataLifecycleService.ErasureResult result = lifecycleService.executeErasure(jobId);
            assertNotNull(result, "Erasure result must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(15)
    void testArchiveScheduling() {
        String test = "Data Archival — Schedule";
        try {
            String archiveId = lifecycleService.scheduleArchive(
                    "audit_logs", "/data/active/audit_2025",
                    "/archive/audit_2025", 50000);
            assertNotNull(archiveId, "Archive ID must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(16)
    void testLifecycleStatistics() {
        String test = "Lifecycle Statistics";
        try {
            DataLifecycleService.LifecycleStatistics stats = lifecycleService.getStatistics();
            assertNotNull(stats, "Statistics must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(17)
    void testRetentionPolicyLookup() {
        String test = "Retention Policy — Lookup by Category";
        try {
            // Create and then lookup
            RetentionPolicy policy = new RetentionPolicy(
                    "Healthcare Records", "HEALTH_DATA", "Regulatory",
                    3650, 730, "Section 9 - Children's Data",
                    "Section 8(7) - Storage Limitation");
            lifecycleService.createPolicy(policy);

            RetentionPolicy found = lifecycleService.getPolicy("HEALTH_DATA");
            assertNotNull(found, "Policy must be found by category");
            assertEquals("HEALTH_DATA", found.getDataCategory());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TRAINING & AWARENESS TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(18)
    void testTrainingServiceInitialization() {
        String test = "Training Service Initialization";
        try {
            assertNotNull(trainingService, "TrainingService must not be null");
            assertTrue(trainingService.isInitialized());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(19)
    void testDefaultTrainingModules() {
        String test = "Default Training Modules";
        try {
            List<TrainingModule> modules = trainingService.getDefaultModules();
            assertNotNull(modules, "Default modules must not be null");
            assertFalse(modules.isEmpty(), "Must have default training modules");

            // Each module must have mandatory fields
            for (TrainingModule module : modules) {
                assertNotNull(module.getId());
                assertNotNull(module.getTitle());
                assertNotNull(module.getCategory());
                assertNotNull(module.getDpdpSection());
                assertTrue(module.getDurationMinutes() > 0);
            }
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(20)
    void testUserEnrollment() {
        String test = "Training — User Enrollment";
        try {
            List<TrainingModule> modules = trainingService.getDefaultModules();
            assertFalse(modules.isEmpty());

            String moduleId = modules.get(0).getId();
            String enrollmentId = trainingService.enrollUser("EMP001", moduleId);
            assertNotNull(enrollmentId, "Enrollment ID must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(21)
    void testTrainingModuleCompletion() {
        String test = "Training — Module Start & Completion";
        try {
            List<TrainingModule> modules = trainingService.getDefaultModules();
            String moduleId = modules.get(0).getId();
            String enrollmentId = trainingService.enrollUser("EMP002", moduleId);

            // Start module
            trainingService.startModule(enrollmentId);

            // Complete with passing score
            trainingService.completeModule(enrollmentId, 85);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(22)
    void testTrainingCampaign() {
        String test = "Training — Campaign Creation";
        try {
            List<TrainingModule> modules = trainingService.getDefaultModules();
            List<String> moduleIds = new ArrayList<>();
            for (int i = 0; i < Math.min(3, modules.size()); i++) {
                moduleIds.add(modules.get(i).getId());
            }

            String campaignId = trainingService.createCampaign(
                    "DPDP Compliance 2026", "Mandatory DPDP training for all employees",
                    moduleIds, List.of("IT", "HR", "Legal", "Finance"),
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                    "hr@organization.com");
            assertNotNull(campaignId, "Campaign ID must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(23)
    void testTrainingStatistics() {
        String test = "Training Statistics";
        try {
            TrainingService.TrainingStats stats = trainingService.getStatistics();
            assertNotNull(stats, "Training statistics must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(24)
    void testUserEnrollmentHistory() {
        String test = "Training — User Enrollment History";
        try {
            List<TrainingService.EnrollmentStatus> enrollments = trainingService.getUserEnrollments("EMP002");
            assertNotNull(enrollments);
            assertFalse(enrollments.isEmpty(), "EMP002 must have enrollments");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // VENDOR RISK MANAGEMENT TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(25)
    void testVendorServiceInitialization() {
        String test = "Vendor Risk Service Initialization";
        try {
            assertNotNull(vendorService, "VendorRiskService must not be null");
            assertTrue(vendorService.isInitialized());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(26)
    void testVendorCreation() {
        String test = "Vendor Creation — With DPA";
        try {
            Vendor vendor = new Vendor();
            vendor.setName("CloudHost India Pvt Ltd");
            vendor.setCategory("CLOUD_HOSTING");
            vendor.setTier("TIER_1");
            vendor.setDescription("Primary cloud hosting provider for DPDP data");
            vendor.setCountry("India");
            vendor.setContactName("Rahul Sharma");
            vendor.setContactEmail("rahul@cloudhost.in");
            vendor.setContactPhone("+91-98765-43210");
            vendor.setDataProcessingAgreement(true);
            vendor.setDpaExpiryDate(LocalDateTime.now().plusYears(2));
            vendor.setStatus("ACTIVE");
            vendor.setRiskTier("HIGH");

            vendorService.createVendor(vendor);

            assertNotNull(vendor.getId());
            assertEquals("CloudHost India Pvt Ltd", vendor.getName());
            assertTrue(vendor.isDataProcessingAgreement());
            assertEquals("HIGH", vendor.getRiskTier());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(27)
    void testVendorRiskAssessment() {
        String test = "Vendor Risk Assessment — Start & Complete";
        try {
            // Create a vendor first
            Vendor vendor = new Vendor();
            vendor.setName("DataProcessor LLC");
            vendor.setCategory("DATA_PROCESSING");
            vendor.setTier("TIER_2");
            vendor.setCountry("India");
            vendor.setContactEmail("contact@processor.com");
            vendor.setDataProcessingAgreement(true);
            vendor.setRiskTier("MEDIUM");
            vendorService.createVendor(vendor);

            // Start assessment
            String assessmentId = vendorService.startAssessment(
                    vendor.getId(), "DPDP_COMPLIANCE", "assessor@org.com");
            assertNotNull(assessmentId, "Assessment ID must not be null");

            // Complete assessment
            vendorService.completeAssessment(assessmentId, 78.5,
                    "MEDIUM", 5, 1);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(28)
    void testVendorDataSharingRecord() {
        String test = "Vendor Data Sharing — Cross-Border Tracking";
        try {
            Vendor vendor = new Vendor();
            vendor.setName("Analytics Partner SG");
            vendor.setCategory("ANALYTICS");
            vendor.setTier("TIER_1");
            vendor.setCountry("Singapore");
            vendor.setContactEmail("partner@analytics.sg");
            vendor.setDataProcessingAgreement(true);
            vendor.setRiskTier("HIGH");
            vendorService.createVendor(vendor);

            DataSharingRecord record = new DataSharingRecord();
            record.setDataCategory("CUSTOMER_ANALYTICS");
            record.setDataTypes(List.of("Name", "Email", "Transaction History"));
            record.setDataVolume("10000 records/month");
            record.setPurpose("Business analytics and reporting");
            record.setTransferFrequency("DAILY");
            record.setCrossBorder(true);
            record.setDestinationCountry("Singapore");
            record.setEncryptionRequired(true);
            record.setLegalBasis("Section 16 - Cross-Border Transfer");

            vendorService.recordDataSharing(vendor.getId(), record);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(29)
    void testVendorIncidentReporting() {
        String test = "Vendor Incident Reporting";
        try {
            Vendor vendor = new Vendor();
            vendor.setName("Payment Gateway Corp");
            vendor.setCategory("PAYMENT_PROCESSING");
            vendor.setTier("TIER_1");
            vendor.setCountry("India");
            vendor.setContactEmail("sec@paygate.com");
            vendor.setDataProcessingAgreement(true);
            vendor.setRiskTier("HIGH");
            vendorService.createVendor(vendor);

            VendorIncident incident = new VendorIncident();
            incident.setIncidentType("DATA_BREACH");
            incident.setSeverity("HIGH");
            incident.setDescription("Vendor API vulnerability allowed unauthorized PII access — 500 records affected");
            incident.setImpact("Customer PII - 500 records exposed, Section 8(6) notification required");
            incident.setRootCause("Unauthorized data access via API vulnerability");
            incident.setRemediation("Patch applied, access revoked, DPBI notified");

            vendorService.reportVendorIncident(vendor.getId(), incident);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(30)
    void testVendorsByRiskTier() {
        String test = "Vendors — Filter by Risk Tier";
        try {
            List<Vendor> highRisk = vendorService.getVendorsByRisk("HIGH");
            assertNotNull(highRisk, "High-risk vendor list must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(31)
    void testCrossBorderTransfers() {
        String test = "Vendor — Cross-Border Transfer Tracking";
        try {
            List<DataSharingRecord> crossBorder = vendorService.getCrossBorderTransfers();
            assertNotNull(crossBorder, "Cross-border transfers must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(32)
    void testVendorStatistics() {
        String test = "Vendor Risk Statistics";
        try {
            VendorRiskService.VendorStatistics stats = vendorService.getStatistics();
            assertNotNull(stats, "Vendor statistics must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(33)
    void testSummary() {
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("  PHASE 5 TEST SUMMARY");
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.println("  Crypto:           11 tests (AES, RSA, hybrid, PQC, PII, hash)");
        System.out.println("  Lifecycle:         6 tests (retention, erasure, archive)");
        System.out.println("  Training:          7 tests (modules, enrollment, campaigns)");
        System.out.println("  Vendor Risk:       8 tests (CRUD, assessment, data sharing)");
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf("  Total: %d PASSED, %d FAILED%n", passed, failed);
        System.out.println("══════════════════════════════════════════════════════════");
        assertEquals(0, failed, "All Phase 5 tests must pass");
    }
}
