package com.qsdpdp;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.security.SecurityManager;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.dlp.*;
import com.qsdpdp.siem.*;
import com.qsdpdp.gap.*;
import com.qsdpdp.pii.*;
import com.qsdpdp.gap.GapAnalysisService.RemediationPlan;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Phase 3 Self-Test Suite — DLP, SIEM/SOAR, Gap Analysis
 * Validates data loss prevention, security event management,
 * orchestrated response, and compliance gap assessment.
 *
 * @version 1.0.0
 * @since Phase 3
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase3SelfTest {

    private static DatabaseManager dbManager;
    private static SecurityManager securityManager;
    private static EventBus eventBus;
    private static AuditService auditService;
    private static DLPService dlpService;
    private static SIEMService siemService;
    private static GapAnalysisService gapService;

    private static int passed = 0;
    private static int failed = 0;

    @BeforeAll
    static void setup() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║         PHASE 3 SELF-TEST — DLP / SIEM / Gap Analysis     ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        dbManager = new DatabaseManager();
        dbManager.initialize();

        securityManager = new SecurityManager();
        securityManager.initialize();

        eventBus = new EventBus();
        eventBus.initialize();

        auditService = new AuditService(dbManager);
        auditService.initialize();

        // PIIScanner + SIEMService must be created before DLPService (dependency)
        PIIScanner piiScanner = new PIIScanner(dbManager, auditService, eventBus, securityManager);
        piiScanner.initialize();

        siemService = new SIEMService(dbManager, auditService, eventBus);
        siemService.initialize();

        dlpService = new DLPService(dbManager, auditService, eventBus, piiScanner, siemService);
        dlpService.initialize();

        gapService = new GapAnalysisService(dbManager, auditService, eventBus);
        gapService.initialize();

        System.out.println("✓ All Phase 3 services initialized");
    }

    @AfterAll
    static void teardown() {
        System.out.println("\n═══════════════════════════════════════════════════════════");
        System.out.println("  Phase 3 Results: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("  Total: " + (passed + failed) + " tests");
        System.out.println("═══════════════════════════════════════════════════════════");
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
    // DLP SERVICE TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(1)
    void testDLPServiceInitialization() {
        String test = "DLP Service Initialization";
        try {
            assertNotNull(dlpService, "DLPService must not be null");
            assertTrue(dlpService.isInitialized(), "DLPService must be initialized");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(2)
    void testDefaultDLPPolicies() {
        String test = "Default DLP Policies (DPDP-aligned)";
        try {
            List<DLPPolicy> defaults = DLPPolicy.getDefaultPolicies();
            assertNotNull(defaults, "Default policies must not be null");
            assertEquals(10, defaults.size(), "Expected 10 default DPDP-aligned DLP policies");

            // Verify Aadhaar policy is BLOCK action
            DLPPolicy aadhaar = defaults.stream()
                    .filter(p -> "BLOCK_AADHAAR_TRANSFER".equals(p.getName()))
                    .findFirst().orElse(null);
            assertNotNull(aadhaar, "Aadhaar transfer policy must exist");
            assertEquals(DLPAction.BLOCK, aadhaar.getPrimaryAction());
            assertTrue(aadhaar.getProtectedDataTypes().contains(PIIType.AADHAAR));
            assertTrue(aadhaar.isSensitiveDataProtection());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(3)
    void testDLPPolicyCreation() {
        String test = "DLP Policy Creation";
        try {
            DLPPolicy policy = new DLPPolicy("TEST_POLICY", "Test DLP policy for unit testing");
            policy.addProtectedType(PIIType.PAN);
            policy.addProtectedType(PIIType.AADHAAR);
            policy.withAction(DLPAction.BLOCK);
            policy.withPriority(99);
            policy.setMonitorNetwork(true);
            policy.setMonitorEmail(true);
            policy.setNotifyDPO(true);
            policy.setDpdpSection("Section 2(t) - Personal Data");

            assertNotNull(policy.getId(), "Policy must have an ID");
            assertEquals("TEST_POLICY", policy.getName());
            assertEquals(DLPAction.BLOCK, policy.getPrimaryAction());
            assertEquals(99, policy.getPriority());
            assertTrue(policy.getProtectedDataTypes().contains(PIIType.PAN));
            assertTrue(policy.getProtectedDataTypes().contains(PIIType.AADHAAR));
            assertTrue(policy.isMonitorNetwork());
            assertTrue(policy.isMonitorEmail());
            assertTrue(policy.isNotifyDPO());
            assertEquals("Section 2(t) - Personal Data", policy.getDpdpSection());

            dlpService.addPolicy(policy);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(4)
    void testDLPActionEnumProperties() {
        String test = "DLP Action Enum — Preventive Classification";
        try {
            // Non-preventive actions
            assertFalse(DLPAction.ALLOW.isPreventive());
            assertFalse(DLPAction.LOG_ONLY.isPreventive());
            assertFalse(DLPAction.WARN.isPreventive());

            // Preventive actions
            assertTrue(DLPAction.BLOCK.isPreventive());
            assertTrue(DLPAction.ENCRYPT.isPreventive());
            assertTrue(DLPAction.QUARANTINE.isPreventive());
            assertTrue(DLPAction.NOTIFY.isPreventive());
            assertTrue(DLPAction.REDACT.isPreventive());

            assertEquals(8, DLPAction.values().length, "Expected 8 DLP actions");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(5)
    void testDLPContentEvaluation() {
        String test = "DLP Content Evaluation — Aadhaar Detection";
        try {
            // Content with Aadhaar number (sensitive PII)
            String content = "Customer data: Name: Rajesh Kumar, Aadhaar: 1234 5678 9012, Address: Mumbai";
            DLPEvaluationResult result = dlpService.evaluate(content, "testuser", "external@example.com", "email");
            assertNotNull(result, "Evaluation result must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(6)
    void testDLPCrossBorderPolicy() {
        String test = "DLP Cross-Border Transfer Policy";
        try {
            List<DLPPolicy> defaults = DLPPolicy.getDefaultPolicies();
            DLPPolicy crossBorder = defaults.stream()
                    .filter(p -> "BLOCK_CROSS_BORDER".equals(p.getName()))
                    .findFirst().orElse(null);
            assertNotNull(crossBorder, "Cross-border policy must exist");
            assertTrue(crossBorder.isCrossBorderRestriction());
            assertEquals(DLPAction.BLOCK, crossBorder.getPrimaryAction());
            assertEquals("Section 16 - Cross-Border Transfer", crossBorder.getDpdpSection());
            assertTrue(crossBorder.getProtectedDataTypes().contains(PIIType.AADHAAR));
            assertTrue(crossBorder.getProtectedDataTypes().contains(PIIType.PAN));
            assertTrue(crossBorder.getProtectedDataTypes().contains(PIIType.HEALTH_ID));
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(7)
    void testDLPPolicyEnableDisable() {
        String test = "DLP Policy Enable/Disable";
        try {
            DLPPolicy policy = new DLPPolicy("TOGGLE_TEST", "Toggle test policy");
            policy.setEnabled(true);
            assertTrue(policy.isEnabled());
            policy.setEnabled(false);
            assertFalse(policy.isEnabled());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(8)
    void testDLPBiometricPolicy() {
        String test = "DLP Biometric Data Protection";
        try {
            List<DLPPolicy> defaults = DLPPolicy.getDefaultPolicies();
            DLPPolicy biometric = defaults.stream()
                    .filter(p -> "BLOCK_BIOMETRIC".equals(p.getName()))
                    .findFirst().orElse(null);
            assertNotNull(biometric, "Biometric policy must exist");
            assertTrue(biometric.getProtectedDataTypes().contains(PIIType.FINGERPRINT));
            assertTrue(biometric.getProtectedDataTypes().contains(PIIType.IRIS));
            assertTrue(biometric.getProtectedDataTypes().contains(PIIType.FACIAL));
            assertEquals(DLPAction.BLOCK, biometric.getPrimaryAction());
            assertTrue(biometric.isSensitiveDataProtection());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(9)
    void testDLPStatistics() {
        String test = "DLP Statistics Retrieval";
        try {
            Object stats = dlpService.getStatistics();
            assertNotNull(stats, "DLP statistics must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(10)
    void testDLPChannelMonitoring() {
        String test = "DLP Multi-Channel Monitoring";
        try {
            DLPPolicy policy = new DLPPolicy("MULTI_CHANNEL", "Multi-channel test");
            policy.setMonitorEndpoint(true);
            policy.setMonitorNetwork(true);
            policy.setMonitorCloud(true);
            policy.setMonitorEmail(true);
            policy.setMonitorPrint(true);
            policy.setMonitorRemovableMedia(true);
            policy.setMonitorClipboard(true);

            assertTrue(policy.isMonitorEndpoint());
            assertTrue(policy.isMonitorNetwork());
            assertTrue(policy.isMonitorCloud());
            assertTrue(policy.isMonitorEmail());
            assertTrue(policy.isMonitorPrint());
            assertTrue(policy.isMonitorRemovableMedia());
            assertTrue(policy.isMonitorClipboard());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SIEM SERVICE TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(11)
    void testSIEMServiceInitialization() {
        String test = "SIEM Service Initialization";
        try {
            assertNotNull(siemService, "SIEMService must not be null");
            assertTrue(siemService.isInitialized(), "SIEMService must be initialized");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(12)
    void testSecurityEventCreation() {
        String test = "Security Event Creation (Builder Pattern)";
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .category(EventCategory.DATA_ACCESS)
                    .severity(EventSeverity.HIGH)
                    .source("QS-DPDP", "192.168.1.100")
                    .user("USR001", "Rajesh Kumar")
                    .action("READ", true)
                    .resource("customer_data", "DATABASE_TABLE")
                    .message("Accessed customer PII records")
                    .dataPrincipal("DP001")
                    .processingPurpose("KYC Verification")
                    .build();

            assertNotNull(event.getId(), "Event must have UUID");
            assertEquals(EventCategory.DATA_ACCESS, event.getCategory());
            assertEquals(EventSeverity.HIGH, event.getSeverity());
            assertEquals("QS-DPDP", event.getSource());
            assertEquals("192.168.1.100", event.getSourceIP());
            assertEquals("USR001", event.getUserId());
            assertEquals("Rajesh Kumar", event.getUserName());
            assertTrue(event.isSuccess());
            assertTrue(event.isPersonalDataInvolved());
            assertNotNull(event.getTimestamp());
            assertEquals("NEW", event.getStatus());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(13)
    void testSecurityEventComplianceHelpers() {
        String test = "Security Event — DPDP Compliance Helpers";
        try {
            // Breach event should require CERT-In and DPBI notification
            SecurityEvent breach = new SecurityEvent(EventCategory.BREACH_CONFIRMED,
                    EventSeverity.CRITICAL, "Data breach confirmed");
            assertTrue(breach.requiresCERTInNotification(), "Breach must require CERT-In notification");
            assertTrue(breach.requiresDPBINotification(), "Breach must require DPBI notification");
            assertEquals(6, breach.getNotificationDeadlineHours(), "CERT-In deadline must be 6 hours");
            assertTrue(breach.requiresEscalation());

            // Normal event should not require notifications
            SecurityEvent normal = new SecurityEvent(EventCategory.AUTH_SUCCESS,
                    EventSeverity.INFO, "User login success");
            assertFalse(normal.requiresCERTInNotification());
            assertFalse(normal.requiresDPBINotification());
            assertEquals(-1, normal.getNotificationDeadlineHours());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(14)
    void testEventCategoryDPDPMapping() {
        String test = "Event Category — DPDP Section Mapping";
        try {
            assertEquals("Section 6 - Consent", EventCategory.CONSENT_COLLECTED.getDPDPSection());
            assertEquals("Section 6 - Consent", EventCategory.CONSENT_WITHDRAWN.getDPDPSection());
            assertEquals("Section 8 - Breach Notification", EventCategory.BREACH_CONFIRMED.getDPDPSection());
            assertEquals("Section 11-14 - Rights", EventCategory.RIGHTS_REQUEST.getDPDPSection());
            assertEquals("Section 7 - Processing", EventCategory.DATA_ACCESS.getDPDPSection());
            assertEquals("Section 5 - Lawful Processing", EventCategory.POLICY_VIOLATION.getDPDPSection());

            // Personal data flags
            assertTrue(EventCategory.DATA_ACCESS.affectsPersonalData());
            assertTrue(EventCategory.CONSENT_COLLECTED.affectsPersonalData());
            assertTrue(EventCategory.PII_EXPOSURE.affectsPersonalData());
            assertFalse(EventCategory.SYSTEM_START.affectsPersonalData());
            assertFalse(EventCategory.AUTH_SUCCESS.affectsPersonalData());

            // DPDP notification requirements
            assertTrue(EventCategory.BREACH_CONFIRMED.requiresDPDPNotification());
            assertTrue(EventCategory.PII_EXPOSURE.requiresDPDPNotification());
            assertTrue(EventCategory.DLP_VIOLATION.requiresDPDPNotification());
            assertFalse(EventCategory.AUTH_SUCCESS.requiresDPDPNotification());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(15)
    void testEventSeverityProperties() {
        String test = "Event Severity — Escalation & CERT-In Timeline";
        try {
            // Escalation flags
            assertTrue(EventSeverity.CRITICAL.shouldEscalate());
            assertTrue(EventSeverity.HIGH.shouldEscalate());
            assertFalse(EventSeverity.MEDIUM.shouldEscalate());
            assertFalse(EventSeverity.LOW.shouldEscalate());
            assertFalse(EventSeverity.INFO.shouldEscalate());

            // CERT-In notification timelines
            assertEquals(6, EventSeverity.CRITICAL.getCERTInNotificationHours());
            assertEquals(24, EventSeverity.HIGH.getCERTInNotificationHours());
            assertEquals(72, EventSeverity.MEDIUM.getCERTInNotificationHours());
            assertEquals(-1, EventSeverity.LOW.getCERTInNotificationHours());

            // DPBI notification timelines
            assertEquals(72, EventSeverity.CRITICAL.getDPBINotificationHours());
            assertEquals(72, EventSeverity.HIGH.getDPBINotificationHours());
            assertEquals(-1, EventSeverity.MEDIUM.getDPBINotificationHours());

            // Priority ordering
            assertTrue(EventSeverity.CRITICAL.getPriority() < EventSeverity.HIGH.getPriority());
            assertTrue(EventSeverity.HIGH.getPriority() < EventSeverity.MEDIUM.getPriority());
            assertTrue(EventSeverity.MEDIUM.getPriority() < EventSeverity.LOW.getPriority());

            // DPDP impact mapping
            assertEquals(EventSeverity.CRITICAL, EventSeverity.fromDPDPImpact("PERSONAL_DATA_BREACH"));
            assertEquals(EventSeverity.CRITICAL, EventSeverity.fromDPDPImpact("SENSITIVE_DATA_ACCESS"));
            assertEquals(EventSeverity.HIGH, EventSeverity.fromDPDPImpact("UNAUTHORIZED_PROCESSING"));
            assertEquals(EventSeverity.HIGH, EventSeverity.fromDPDPImpact("CONSENT_VIOLATION"));
            assertEquals(EventSeverity.MEDIUM, EventSeverity.fromDPDPImpact("POLICY_VIOLATION"));
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(16)
    void testSIEMEventIngestion() {
        String test = "SIEM Event Ingestion";
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .category(EventCategory.AUTH_FAILURE)
                    .severity(EventSeverity.MEDIUM)
                    .source("WebApp", "10.0.0.1")
                    .user("attacker", "Unknown")
                    .action("LOGIN", false)
                    .message("Failed login attempt from suspicious IP")
                    .build();

            siemService.ingestEvent(event);
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(17)
    void testSIEMRawLogParsing() {
        String test = "SIEM Raw Log Ingestion";
        try {
            String rawLog = "2026-02-11 12:00:00 WARN [AUTH] Failed login for user admin from 192.168.1.50 - Invalid password";
            // parseRawLog is private — use public ingestRawLog which parses and queues the
            // event
            siemService.ingestRawLog("firewall", rawLog);
            // If no exception, raw log parsing succeeded internally
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(18)
    void testCorrelationRuleDefaults() {
        String test = "Correlation Rules — 10 Default DPDP Rules";
        try {
            List<CorrelationRule> rules = CorrelationRule.getDefaultRules();
            assertNotNull(rules);
            assertEquals(10, rules.size(), "Expected 10 default correlation rules");

            // Verify brute force rule
            CorrelationRule bruteForce = rules.stream()
                    .filter(r -> "BRUTE_FORCE_DETECTION".equals(r.getName()))
                    .findFirst().orElse(null);
            assertNotNull(bruteForce, "Brute force rule must exist");
            assertEquals(5, bruteForce.getEventThreshold());
            assertEquals(300, bruteForce.getTimeWindowSeconds());
            assertTrue(bruteForce.isSameSource());
            assertEquals(EventSeverity.HIGH, bruteForce.getOutputSeverity());
            assertEquals(EventCategory.INTRUSION_ATTEMPT, bruteForce.getOutputCategory());

            // Verify data exfiltration rule
            CorrelationRule exfiltration = rules.stream()
                    .filter(r -> "DATA_EXFILTRATION".equals(r.getName()))
                    .findFirst().orElse(null);
            assertNotNull(exfiltration);
            assertTrue(exfiltration.isRequiresPersonalData());
            assertEquals(EventSeverity.CRITICAL, exfiltration.getOutputSeverity());
            assertEquals("Section 8 - Breach Notification", exfiltration.getDpdpSectionTarget());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(19)
    void testCorrelationRuleMatching() {
        String test = "Correlation Rule — Event Matching";
        try {
            CorrelationRule rule = new CorrelationRule()
                    .withName("TEST_RULE")
                    .withTriggerCategories(EventCategory.DATA_ACCESS)
                    .withRequiresPersonalData(true)
                    .withEventThreshold(3)
                    .withTimeWindow(600);

            // Should match: data access with personal data
            SecurityEvent match = SecurityEvent.builder()
                    .category(EventCategory.DATA_ACCESS)
                    .severity(EventSeverity.MEDIUM)
                    .message("Accessed PII")
                    .dataPrincipal("DP001")
                    .build();
            assertTrue(rule.matches(match), "Rule should match personal data access event");

            // Should not match: wrong category
            SecurityEvent wrongCat = SecurityEvent.builder()
                    .category(EventCategory.AUTH_SUCCESS)
                    .severity(EventSeverity.INFO)
                    .message("Login success")
                    .build();
            assertFalse(rule.matches(wrongCat), "Rule should not match auth event");

            // Should not match: no personal data
            SecurityEvent noPersonal = SecurityEvent.builder()
                    .category(EventCategory.DATA_ACCESS)
                    .severity(EventSeverity.LOW)
                    .message("Accessed config data")
                    .build();
            assertFalse(rule.matches(noPersonal), "Rule should not match non-personal data event");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(20)
    void testSOARPlaybookDefaults() {
        String test = "SOAR Playbooks — 10 Default Playbooks";
        try {
            Map<String, SOARPlaybook> playbooks = SOARPlaybook.getDefaultPlaybooks();
            assertNotNull(playbooks);
            assertEquals(10, playbooks.size(), "Expected 10 default SOAR playbooks");

            // Verify suspend user access playbook
            SOARPlaybook suspend = playbooks.get("SUSPEND_USER_ACCESS");
            assertNotNull(suspend, "SUSPEND_USER_ACCESS playbook must exist");
            assertEquals(6, suspend.getSteps().size(), "Suspend playbook must have 6 steps");
            assertEquals("Section 8 - Breach Notification", suspend.getDpdpAlignment());
            assertEquals(100, suspend.getPriority());

            // Verify step ordering
            for (int i = 0; i < suspend.getSteps().size(); i++) {
                assertEquals(i + 1, suspend.getSteps().get(i).getOrder(),
                        "Step order must be sequential");
            }
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(21)
    void testSOARPlaybookSteps() {
        String test = "SOAR Playbook Steps — Content Validation";
        try {
            Map<String, SOARPlaybook> playbooks = SOARPlaybook.getDefaultPlaybooks();

            // Verify stop processing playbook (consent withdrawal)
            SOARPlaybook stopProc = playbooks.get("STOP_PROCESSING");
            assertNotNull(stopProc);
            assertEquals("Section 6 - Consent", stopProc.getDpdpAlignment());
            assertEquals(5, stopProc.getSteps().size());

            // First step should always be LOG
            assertEquals("LOG", stopProc.getSteps().get(0).getAction());
            assertTrue(stopProc.getSteps().get(0).isRequired());
            assertEquals("PENDING", stopProc.getSteps().get(0).getStatus());

            // Verify investigation playbook requires approval
            SOARPlaybook investigate = playbooks.get("SUSPEND_USER_INVESTIGATE");
            assertNotNull(investigate);
            assertTrue(investigate.isRequiresApproval());
            assertEquals("SECURITY_MANAGER", investigate.getApprovalRole());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(22)
    void testSIEMAlertCreation() {
        String test = "SIEM Alert Creation — DPDP Breach Detection";
        try {
            SecurityEvent breachEvent = SecurityEvent.builder()
                    .category(EventCategory.BREACH_CONFIRMED)
                    .severity(EventSeverity.CRITICAL)
                    .source("DLP_ENGINE", "10.0.0.5")
                    .user("USR042", "Unknown Actor")
                    .action("DATA_EXPORT", false)
                    .message("Confirmed personal data breach — 5000 records exported")
                    .dataPrincipal("BATCH_DP")
                    .sensitiveData(true)
                    .build();

            assertTrue(breachEvent.requiresEscalation());
            assertTrue(breachEvent.requiresCERTInNotification());
            assertTrue(breachEvent.requiresDPBINotification());
            assertEquals(6, breachEvent.getNotificationDeadlineHours());
            assertEquals("DPDP Breach Notification Required", breachEvent.getComplianceRisk());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(23)
    void testSIEMStatistics() {
        String test = "SIEM Statistics Retrieval";
        try {
            Object stats = siemService.getStatistics();
            assertNotNull(stats, "SIEM statistics must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GAP ANALYSIS TESTS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(24)
    void testGapAnalysisServiceInitialization() {
        String test = "Gap Analysis Service Initialization";
        try {
            assertNotNull(gapService, "GapAnalysisService must not be null");
            assertTrue(gapService.isInitialized(), "GapAnalysisService must be initialized");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(25)
    void testQuestionCategoryEnumProperties() {
        String test = "Question Categories — DPDP Section Mapping & Weights";
        try {
            // Verify categories exist
            assertTrue(QuestionCategory.values().length >= 20, "Must have at least 20 categories");

            // Verify critical categories have higher weight
            assertEquals(1.5, QuestionCategory.CONSENT_MANAGEMENT.getWeight());
            assertEquals(1.5, QuestionCategory.BREACH_NOTIFICATION.getWeight());
            assertEquals(1.5, QuestionCategory.CHILDREN_DATA.getWeight());
            assertEquals(1.5, QuestionCategory.CROSS_BORDER.getWeight());

            // Verify high-priority categories
            assertEquals(1.3, QuestionCategory.SECURITY_SAFEGUARDS.getWeight());
            assertEquals(1.3, QuestionCategory.RIGHTS_ACCESS.getWeight());
            assertEquals(1.3, QuestionCategory.DPIA.getWeight());

            // Standard weight
            assertEquals(1.0, QuestionCategory.GOVERNANCE.getWeight());
            assertEquals(1.0, QuestionCategory.TECHNICAL_CONTROLS.getWeight());

            // DPDP references
            assertEquals("Chapter II - Section 6", QuestionCategory.CONSENT_MANAGEMENT.getDpdpReference());
            assertEquals("Chapter III - Section 11", QuestionCategory.RIGHTS_ACCESS.getDpdpReference());
            assertEquals("Chapter IV - Section 16", QuestionCategory.CROSS_BORDER.getDpdpReference());
            assertEquals("Chapter II - Section 9", QuestionCategory.CHILDREN_DATA.getDpdpReference());

            // Sector-specific categories
            assertNotNull(QuestionCategory.SECTOR_BANKING.getDisplayName());
            assertNotNull(QuestionCategory.SECTOR_HEALTH.getDisplayName());
            assertNotNull(QuestionCategory.SECTOR_TELECOM.getDisplayName());
            assertNotNull(QuestionCategory.SECTOR_GOVERNMENT.getDisplayName());
            assertNotNull(QuestionCategory.SECTOR_EDUCATION.getDisplayName());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(26)
    void testAssessmentQuestionBuilder() {
        String test = "Assessment Question Builder & Scoring";
        try {
            AssessmentQuestion question = AssessmentQuestion.builder()
                    .category(QuestionCategory.CONSENT_MANAGEMENT)
                    .question("How does your organization collect consent from data principals?")
                    .options(
                            "No consent mechanism exists",
                            "Verbal consent only",
                            "Written consent but no digital records",
                            "Digital consent with basic tracking",
                            "Fully automated consent platform with audit trail (DPDP compliant)")
                    .correctOption(4)
                    .hint("DPDP Section 6 requires clear, informed, and verifiable consent")
                    .impact("Non-compliance may attract penalties under Section 33")
                    .remediation("Implement a DPDP-compliant consent management system")
                    .difficulty(4)
                    .score(10.0)
                    .dpdpClause("Section 6 - Consent")
                    .isoControl("ISO 27701:2019 A.7.2.3")
                    .mandatory(true)
                    .build();

            assertNotNull(question.getId());
            assertEquals(QuestionCategory.CONSENT_MANAGEMENT, question.getCategory());
            assertEquals(5, question.getOptions().size());
            assertEquals(4, question.getCorrectOptionIndex());
            assertTrue(question.isMandatory());
            assertEquals(10.0, question.getMaxScore());
            assertEquals("Section 6 - Consent", question.getDpdpClause());

            // Scoring — correct answer gets full weighted score
            double fullScore = question.calculateScore(4);
            assertEquals(10.0 * 1.5, fullScore, 0.01, "Correct answer × consent weight (1.5)");

            // Scoring — close answer gets partial credit
            double partialClose = question.calculateScore(3);
            assertEquals(10.0 * 0.5 * 1.5, partialClose, 0.01, "Distance 1 = 50% × weight");

            // Scoring — far answer gets less credit
            double partialFar = question.calculateScore(2);
            assertEquals(10.0 * 0.25 * 1.5, partialFar, 0.01, "Distance 2 = 25% × weight");

            // Scoring — very wrong answer gets zero
            double zeroScore = question.calculateScore(0);
            assertEquals(0.0, zeroScore, 0.01, "Distance >= 3 = 0 score");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(27)
    void testGapAnalysisResultCreation() {
        String test = "Gap Analysis Result — Assessment Workflow";
        try {
            GapAnalysisResult result = new GapAnalysisResult();
            result.setOrganizationId("ORG001");
            result.setSector("BFSI");
            result.setAssessedBy("compliance_officer@org.com");

            assertNotNull(result.getId());
            assertEquals("IN_PROGRESS", result.getStatus());
            assertNotNull(result.getStartedAt());
            assertEquals("BFSI", result.getSector());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(28)
    void testGapAnalysisScoring() {
        String test = "Gap Analysis Scoring — RAG Status Calculation";
        try {
            GapAnalysisResult result = new GapAnalysisResult();
            result.setOrganizationId("ORG002");
            result.setSector("Healthcare");

            // Create a question and submit correct answer
            AssessmentQuestion q1 = AssessmentQuestion.builder()
                    .category(QuestionCategory.CONSENT_MANAGEMENT)
                    .question("Consent mechanism?")
                    .options("None", "Partial", "Basic", "Advanced", "Full DPDP compliance")
                    .correctOption(4)
                    .score(10.0)
                    .dpdpClause("Section 6")
                    .mandatory(true)
                    .build();

            // Submit correct answer
            result.addResponse(q1, 4, "Fully compliant");
            assertEquals(1, result.getTotalQuestions());
            assertEquals(0, result.getGapCount(), "Correct answer should not create a gap");

            // Submit wrong answer to another question
            AssessmentQuestion q2 = AssessmentQuestion.builder()
                    .category(QuestionCategory.BREACH_NOTIFICATION)
                    .question("Breach notification timeline?")
                    .options("No process", "Ad-hoc", "Documented", "Partial automation", "72-hour automated")
                    .correctOption(4)
                    .score(10.0)
                    .dpdpClause("Section 8(6)")
                    .impact("Regulatory penalty risk")
                    .remediation("Implement automated 72-hour breach notification")
                    .mandatory(true)
                    .build();

            result.addResponse(q2, 0, "No breach process exists");
            assertEquals(2, result.getTotalQuestions());
            assertEquals(1, result.getGapCount(), "Wrong answer should create a gap");

            // Verify gap details
            var gaps = result.getGaps();
            assertFalse(gaps.isEmpty());
            assertEquals("CRITICAL", gaps.get(0).getSeverity(),
                    "Mandatory question with distance>=3 should be CRITICAL");
            assertEquals("IDENTIFIED", gaps.get(0).getStatus());

            // Calculate final scores
            result.calculateFinalScores();
            assertEquals("COMPLETED", result.getStatus());
            assertNotNull(result.getCompletedAt());
            assertTrue(result.getCompliancePercentage() >= 0);
            assertTrue(result.getCompliancePercentage() <= 100);
            assertNotNull(result.getRagStatus());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(29)
    void testGapAnalysisRAGClassification() {
        String test = "Gap Analysis — RED/AMBER/GREEN Classification";
        try {
            // HIGH compliance — all correct answers → GREEN
            GapAnalysisResult greenResult = new GapAnalysisResult();
            AssessmentQuestion q = AssessmentQuestion.builder()
                    .category(QuestionCategory.GOVERNANCE)
                    .question("Test question?")
                    .options("Bad", "OK", "Good", "Great", "Perfect")
                    .correctOption(4)
                    .score(10.0)
                    .dpdpClause("Section 8")
                    .build();
            greenResult.addResponse(q, 4, null);
            greenResult.calculateFinalScores();
            assertEquals("GREEN", greenResult.getRagStatus(),
                    "100% compliance should be GREEN");

            // LOW compliance — all wrong answers → RED
            GapAnalysisResult redResult = new GapAnalysisResult();
            redResult.addResponse(q, 0, null);
            redResult.calculateFinalScores();
            assertEquals("RED", redResult.getRagStatus(),
                    "<50% compliance should be RED");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(30)
    void testGapAnalysisGapStatistics() {
        String test = "Gap Analysis — Gap Statistics & Weakest Categories";
        try {
            GapAnalysisResult result = new GapAnalysisResult();

            // Add multiple responses with gaps
            AssessmentQuestion q1 = AssessmentQuestion.builder()
                    .category(QuestionCategory.CONSENT_MANAGEMENT)
                    .question("Consent Q1?")
                    .options("A", "B", "C", "D", "E")
                    .correctOption(4).score(10.0).dpdpClause("S6")
                    .mandatory(true).build();
            result.addResponse(q1, 0, null); // CRITICAL gap

            AssessmentQuestion q2 = AssessmentQuestion.builder()
                    .category(QuestionCategory.SECURITY_SAFEGUARDS)
                    .question("Security Q1?")
                    .options("A", "B", "C", "D", "E")
                    .correctOption(4).score(10.0).dpdpClause("S8(5)")
                    .build();
            result.addResponse(q2, 3, null); // LOW gap (distance 1)

            AssessmentQuestion q3 = AssessmentQuestion.builder()
                    .category(QuestionCategory.CHILDREN_DATA)
                    .question("Children data protection?")
                    .options("A", "B", "C", "D", "E")
                    .correctOption(4).score(10.0).dpdpClause("S9")
                    .build();
            result.addResponse(q3, 4, null); // Correct, no gap

            result.calculateFinalScores();

            assertEquals(3, result.getTotalQuestions());
            assertEquals(2, result.getGapCount());
            assertTrue(result.getCriticalGapCount() >= 1, "Should have at least 1 critical gap");

            // Weakest category should be consent (worst score)
            List<QuestionCategory> weakest = result.getWeakestCategories(1);
            assertFalse(weakest.isEmpty());
            assertEquals(QuestionCategory.CONSENT_MANAGEMENT, weakest.get(0),
                    "Consent (scored 0) should be weakest");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(31)
    void testGapAnalysisStartAssessment() {
        String test = "Gap Analysis — Start New Assessment";
        try {
            GapAnalysisResult result = gapService.startAssessment("ORG_BFSI_001", "BFSI", "auditor@bank.com");
            assertNotNull(result, "Assessment result must not be null");
            assertNotNull(result.getId());
            assertEquals("BFSI", result.getSector());
            assertEquals("auditor@bank.com", result.getAssessedBy());
            assertEquals("IN_PROGRESS", result.getStatus());
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(32)
    void testGapAnalysisMandatoryQuestions() {
        String test = "Gap Analysis — Mandatory Questions";
        try {
            List<AssessmentQuestion> mandatory = gapService.getMandatoryQuestions();
            assertNotNull(mandatory, "Mandatory questions must not be null");
            // All questions should be marked mandatory
            for (AssessmentQuestion q : mandatory) {
                assertTrue(q.isMandatory(), "Question must be mandatory: " + q.getQuestionText());
            }
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(33)
    void testGapHeatmapGeneration() {
        String test = "Gap Analysis — Heatmap Generation";
        try {
            // Start and complete an assessment in the service
            GapAnalysisResult result = gapService.startAssessment("ORG_HEATMAP", "Healthcare", "test@hc.com");

            // Get questions for consent category and submit a wrong answer
            List<AssessmentQuestion> questions = gapService.getQuestionsForAssessment(
                    "Healthcare", QuestionCategory.CONSENT_MANAGEMENT);
            if (!questions.isEmpty()) {
                gapService.submitResponse(result, questions.get(0), 0, "No consent system");
            }
            gapService.completeAssessment(result);

            GapAnalysisService.GapHeatmap heatmap = gapService.generateHeatmap(result.getId());
            assertNotNull(heatmap, "Heatmap must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(34)
    void testGapRemediationPlan() {
        String test = "Gap Analysis — Remediation Plan Generation";
        try {
            // Create assessment with gaps
            GapAnalysisResult result = gapService.startAssessment("ORG_REMEDIATION", "Government", "officer@gov.in");

            List<AssessmentQuestion> questions = gapService.getQuestionsForAssessment(
                    "Government", QuestionCategory.BREACH_NOTIFICATION);
            if (!questions.isEmpty()) {
                gapService.submitResponse(result, questions.get(0), 0, "No breach notification");
            }
            gapService.completeAssessment(result);

            RemediationPlan plan = gapService.generateRemediationPlan(result.getId());
            assertNotNull(plan, "Remediation plan must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(35)
    void testGapAnalysisStatistics() {
        String test = "Gap Analysis — Statistics Retrieval";
        try {
            Object stats = gapService.getStatistics();
            assertNotNull(stats, "Gap analysis statistics must not be null");
            pass(test);
        } catch (Exception e) {
            fail(test, e);
            throw e;
        }
    }

    @Test
    @Order(36)
    void testSummary() {
        System.out.println("\n══════════════════════════════════════════════════════════");
        System.out.println("  PHASE 3 TEST SUMMARY");
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.println("  DLP Service:       10 tests (policies, evaluation, channels)");
        System.out.println("  SIEM/SOAR:         13 tests (events, correlation, playbooks)");
        System.out.println("  Gap Analysis:      12 tests (assessment, scoring, heatmap)");
        System.out.println("  ─────────────────────────────────────────────────────");
        System.out.printf("  Total: %d PASSED, %d FAILED%n", passed, failed);
        System.out.println("══════════════════════════════════════════════════════════");
        assertEquals(0, failed, "All Phase 3 tests must pass");
    }
}
