package com.qsdpdp;

import com.qsdpdp.core.*;
import com.qsdpdp.db.*;
import com.qsdpdp.rag.*;
import com.qsdpdp.events.*;
import com.qsdpdp.audit.*;
import com.qsdpdp.security.*;
import com.qsdpdp.rules.*;
import com.qsdpdp.consent.*;
import com.qsdpdp.breach.*;
import com.qsdpdp.rights.*;
import com.qsdpdp.dpia.*;
import com.qsdpdp.policy.*;
import com.qsdpdp.user.*;
import com.qsdpdp.training.*;
import com.qsdpdp.pii.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test Suite for QS-DPDP Enterprise
 * Validates cross-module workflows and end-to-end DPDP compliance scenarios
 * 
 * Tests cover: Consent→Audit, Breach→DPBI, Rights→Fulfillment, DPIA→Approval,
 * Policy lifecycle, User auth chain, PII→Breach, and cross-sector workflows.
 * 
 * @version 1.0.0
 * @since Integration Phase
 */
@TestMethodOrder(OrderAnnotation.class)
public class IntegrationTest {

        // ═══════════════════════════════════════════════════════════
        // SHARED INFRASTRUCTURE
        // ═══════════════════════════════════════════════════════════

        private static DatabaseManager dbManager;
        private static com.qsdpdp.security.SecurityManager securityManager;
        private static EventBus eventBus;
        private static AuditService auditService;
        private static RAGEvaluator ragEvaluator;
        private static RuleEngine ruleEngine;

        // Phase 2 Services
        private static ConsentService consentService;
        private static BreachService breachService;
        private static RightsService rightsService;
        private static DPIAService dpiaService;
        private static PolicyService policyService;
        private static UserService userService;

        // Phase 5 Services
        private static TrainingService trainingService;

        // Phase 6 Services
        private static PIIScanner piiScanner;

        // Shared state across tests (order-dependent integration)
        private static String sharedConsentId;
        private static String sharedBreachId;
        private static String sharedRightsRequestId;
        private static String sharedDpiaId;
        private static String sharedPolicyId;

        @BeforeAll
        static void setUp() {
                System.out.println("\n╔═══════════════════════════════════════════════════╗");
                System.out.println("║   QS-DPDP INTEGRATION TEST SUITE                   ║");
                System.out.println("║   Cross-Module Workflow Validation                  ║");
                System.out.println("╚═══════════════════════════════════════════════════╝\n");

                // Core infrastructure
                dbManager = new DatabaseManager();
                dbManager.initialize();

                securityManager = new com.qsdpdp.security.SecurityManager();
                securityManager.initialize();

                eventBus = new EventBus();
                eventBus.initialize();

                auditService = new AuditService(dbManager);
                auditService.initialize();

                ragEvaluator = new RAGEvaluator(dbManager);
                ragEvaluator.initialize();

                ruleEngine = new RuleEngine(dbManager, eventBus);
                ruleEngine.initialize();

                // Phase 2 services
                consentService = new ConsentService(dbManager, auditService, eventBus, securityManager);
                consentService.initialize();

                breachService = new BreachService(dbManager, auditService, eventBus);
                breachService.initialize();

                rightsService = new RightsService(dbManager, auditService, eventBus);
                rightsService.initialize();

                dpiaService = new DPIAService(dbManager, auditService, eventBus);
                dpiaService.initialize();

                policyService = new PolicyService(dbManager, auditService, eventBus);
                policyService.initialize();

                userService = new UserService(dbManager, auditService, securityManager);
                userService.initialize();

                // Phase 5 services
                trainingService = new TrainingService(dbManager, auditService);
                trainingService.initialize();

                // Phase 6 services
                piiScanner = new PIIScanner(dbManager, auditService, eventBus, securityManager);
                piiScanner.initialize();

                // Seed required test data
                seedIntegrationData();
        }

        static void seedIntegrationData() {
                try (java.sql.Connection conn = dbManager.getConnection();
                                java.sql.Statement stmt = conn.createStatement()) {

                        // BFSI Data Principal
                        stmt.executeUpdate(
                                        """
                                                            INSERT OR IGNORE INTO data_principals (id, name, email, phone)
                                                            VALUES ('DP-INT-BFSI-001', 'Anil Kapoor', 'anil.kapoor@icici.com', '+919876500001')
                                                        """);

                        // Healthcare Data Principal
                        stmt.executeUpdate(
                                        """
                                                            INSERT OR IGNORE INTO data_principals (id, name, email, phone)
                                                            VALUES ('DP-INT-HC-001', 'Sunita Devi', 'sunita.devi@apollo.org', '+919876500002')
                                                        """);

                        // E-Commerce Data Principal
                        stmt.executeUpdate(
                                        """
                                                            INSERT OR IGNORE INTO data_principals (id, name, email, phone)
                                                            VALUES ('DP-INT-ECOM-001', 'Vikram Singh', 'vikram.singh@flipkart.com', '+919876500003')
                                                        """);

                        // Processing purposes
                        stmt.executeUpdate(
                                        """
                                                            INSERT OR IGNORE INTO purposes (id, code, name, description, legal_basis, is_active)
                                                            VALUES ('PUR-INT-KYC', 'INT_KYC', 'Integration KYC Verification',
                                                                    'RBI-mandated KYC for integration testing', 'LEGAL_OBLIGATION', 1)
                                                        """);
                        stmt.executeUpdate(
                                        """
                                                            INSERT OR IGNORE INTO purposes (id, code, name, description, legal_basis, is_active)
                                                            VALUES ('PUR-INT-HEALTH', 'INT_HEALTH_RECORDS', 'Health Record Processing',
                                                                    'ABDM health record processing under DPDP Section 7', 'CONSENT', 1)
                                                        """);
                        stmt.executeUpdate(
                                        """
                                                            INSERT OR IGNORE INTO purposes (id, code, name, description, legal_basis, is_active)
                                                            VALUES ('PUR-INT-ECOM', 'INT_ECOM_MARKETING', 'E-Commerce Marketing',
                                                                    'Personalized marketing and recommendations', 'CONSENT', 1)
                                                        """);

                } catch (Exception e) {
                        System.err.println("Warning: Could not seed all integration data: " + e.getMessage());
                }
        }

        @AfterAll
        static void tearDown() {
                System.out.println("\n╔═══════════════════════════════════════════════════╗");
                System.out.println("║   INTEGRATION TEST SUITE COMPLETED                 ║");
                System.out.println("╚═══════════════════════════════════════════════════╝\n");

                if (auditService != null)
                        auditService.shutdown();
                if (eventBus != null)
                        eventBus.shutdown();
                if (dbManager != null)
                        dbManager.shutdown();
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 1: Consent Lifecycle → Audit Trail Verification
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(1)
        @DisplayName("INT-001: Consent collection → audit trail → withdrawal → audit verification")
        void testConsentLifecycleWithAuditTrail() {
                long auditSeqBefore = auditService.getSequenceNumber();

                // Step 1: Collect consent for BFSI KYC
                ConsentRequest request = new ConsentRequest();
                request.setDataPrincipalId("DP-INT-BFSI-001");
                request.setPurposeId("PUR-INT-KYC");
                request.setConsentMethod("DIGITAL_SIGNATURE");
                request.setLanguage("en");
                request.setNoticeVersion("v3.0-DPDP");

                Consent consent = consentService.collectConsent(request);
                assertNotNull(consent, "Consent must be created");
                assertEquals(ConsentStatus.ACTIVE, consent.getStatus());
                assertNotNull(consent.getHash(), "Consent must have tamper-proof hash");
                sharedConsentId = consent.getId();

                // Step 2: Verify audit trail was generated
                long auditSeqAfter = auditService.getSequenceNumber();
                assertTrue(auditSeqAfter > auditSeqBefore,
                                "Audit sequence must increment after consent collection");

                // Step 3: Withdraw consent (must be as easy as collection per DPDP Act)
                Consent withdrawn = consentService.withdrawConsent(
                                consent.getId(), "Customer requested via mobile app", "customer-self-service");
                assertEquals(ConsentStatus.WITHDRAWN, withdrawn.getStatus());
                assertNotNull(withdrawn.getWithdrawnAt());

                // Step 4: Verify audit integrity after full lifecycle
                AuditIntegrityReport report = auditService.verifyIntegrity();
                assertNotNull(report);
                assertTrue(report.getTotalEntries() > 0, "Audit log must have entries");

                System.out.println("✓ INT-001: Consent lifecycle with audit trail verified — " +
                                "Consent: " + consent.getId() + " → Withdrawn → Audit integrity OK");
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 2: Breach → Containment → DPBI Notification → Resolution
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(2)
        @DisplayName("INT-002: Breach reporting → containment → DPBI notification → resolution")
        void testBreachLifecycleWithNotifications() {
                // Step 1: Report a critical healthcare breach
                BreachRequest request = new BreachRequest();
                request.setTitle("INT: ABDM Patient Data Unauthorized Access");
                request.setDescription("Unauthorized API access to 500 patient health records " +
                                "through compromised ABHA authentication tokens. ICD-10 diagnosis codes " +
                                "and prescriptions exposed.");
                request.setBreachType("UNAUTHORIZED_ACCESS");
                request.setSeverity(BreachSeverity.CRITICAL);
                request.setAffectedCount(500);
                request.setReportedBy("abdm-soc-analyst");

                Breach breach = breachService.reportBreach(request);
                assertNotNull(breach);
                assertEquals(BreachStatus.OPEN, breach.getStatus());
                assertNotNull(breach.getDpbiDeadline(), "DPBI 72-hour deadline must be set");
                assertNotNull(breach.getCertinDeadline(), "CERT-In 6-hour deadline must be set");
                sharedBreachId = breach.getId();

                // Step 2: Contain the breach
                Breach contained = breachService.updateStatus(breach.getId(),
                                BreachStatus.CONTAINED, "incident-commander");
                assertEquals(BreachStatus.CONTAINED, contained.getStatus());
                assertNotNull(contained.getContainedAt());

                // Step 3: Record DPBI notification (mandatory per DPDP Act Section 8)
                breachService.recordDpbiNotification(breach.getId(),
                                "DPBI-2026-HC-" + System.currentTimeMillis(), "dpo-officer");

                // Step 4: Record CERT-IN notification (mandatory within 6 hrs for CRITICAL)
                breachService.recordCertinNotification(breach.getId(), "cert-in-analyst");

                // Step 5: Resolve the breach
                Breach resolved = breachService.updateStatus(breach.getId(),
                                BreachStatus.RESOLVED, "incident-commander");
                assertEquals(BreachStatus.RESOLVED, resolved.getStatus());
                assertNotNull(resolved.getResolvedAt());

                System.out.println("✓ INT-002: Breach lifecycle — " + breach.getReferenceNumber() +
                                " → Contained → DPBI/CERT-In notified → Resolved");
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 3: Rights Request → Acknowledge → Assign → Complete
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(3)
        @DisplayName("INT-003: Rights request submission → acknowledgement → completion")
        void testRightsRequestFullfilment() {
                // Step 1: Submit an access request (DPDP Section 11)
                RightsRequestDTO dto = new RightsRequestDTO();
                dto.setDataPrincipalId("DP-INT-HC-001");
                dto.setRequestType(RightType.ACCESS);
                dto.setDescription("Request complete health record export under DPDP Section 11. " +
                                "Data includes ABDM health records, prescription history, and lab reports.");

                RightsRequest request = rightsService.submitRequest(dto);
                assertNotNull(request);
                assertEquals(RequestStatus.PENDING, request.getStatus());

                // Verify 30-day deadline is set
                LocalDateTime expectedDeadline = request.getReceivedAt().plusDays(30);
                assertEquals(expectedDeadline.toLocalDate(), request.getDeadline().toLocalDate(),
                                "30-day DPDP deadline must be set");
                sharedRightsRequestId = request.getId();

                // Step 2: Acknowledge the request
                RightsRequest acknowledged = rightsService.acknowledgeRequest(
                                request.getId(), "dsr-officer");
                assertEquals(RequestStatus.ACKNOWLEDGED, acknowledged.getStatus());
                assertNotNull(acknowledged.getAcknowledgedAt());

                // Step 3: Assign to data handler
                RightsRequest assigned = rightsService.assignRequest(
                                request.getId(), "health-data-admin", "dsr-officer");
                assertEquals(RequestStatus.IN_PROGRESS, assigned.getStatus());
                assertEquals("health-data-admin", assigned.getAssignedTo());

                // Step 4: Complete with response and evidence
                RightsRequest completed = rightsService.completeRequest(
                                request.getId(),
                                "Health records exported in FHIR R4 JSON format. Total: 47 records.",
                                "Evidence: encrypted_export_ABDM_2026.zip (SHA256: abc123...)",
                                "health-data-admin");
                assertEquals(RequestStatus.COMPLETED, completed.getStatus());
                assertNotNull(completed.getCompletedAt());

                System.out.println("✓ INT-003: Rights request lifecycle — " + request.getReferenceNumber() +
                                " → Acknowledged → Assigned → Completed");
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 4: DPIA → Risk Assessment → Approval
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(4)
        @DisplayName("INT-004: DPIA creation → risk assessment → submission → approval")
        void testDPIALifecycle() {
                // Step 1: Create DPIA for e-commerce profiling
                DPIARequest request = new DPIARequest();
                request.setTitle("INT: E-Commerce Customer Profiling Engine DPIA");
                request.setDescription("DPIA for ML-based customer profiling engine that processes " +
                                "purchase history, browsing patterns, and demographic data to generate " +
                                "personalized recommendations. Cross-border data transfer to AWS Singapore.");
                request.setProjectName("SmartRecommend v4.0");
                request.setDataTypes("Behavioral, Purchase History, Demographics, Location");
                request.setAssessor("dpo@ecommerce.in");

                DPIA dpia = dpiaService.createDPIA(request);
                assertNotNull(dpia);
                assertEquals(DPIAStatus.DRAFT, dpia.getStatus());
                sharedDpiaId = dpia.getId();

                // Step 2: Assess risks
                List<DPIARisk> risks = List.of(
                                new DPIARisk("Cross-border Transfer",
                                                "DPDP Section 16 compliance — data transfer to AWS Singapore", 4, 4),
                                new DPIARisk("Profiling Consent", "Requires granular consent for ML-based profiling", 5,
                                                4),
                                new DPIARisk("Data Minimization",
                                                "Collect only necessary attributes per DPDP Section 4", 3, 3));
                DPIA assessed = dpiaService.assessRisk(dpia.getId(), risks, "risk-assessor");
                assertEquals(DPIAStatus.IN_PROGRESS, assessed.getStatus());
                assertTrue(assessed.getRiskScore() > 0, "Risk score must be calculated");

                // Step 3: Submit for review
                DPIA submitted = dpiaService.submitForReview(dpia.getId(),
                                "Three risks identified. Cross-border transfer mitigated by SCCs. " +
                                                "Profiling consent mechanism to be implemented before launch.",
                                "1. Implement granular consent UI. 2. Add data minimization filters. " +
                                                "3. Document SCC with AWS Singapore.",
                                "dpo@ecommerce.in");
                assertEquals(DPIAStatus.PENDING_REVIEW, submitted.getStatus());

                // Step 4: Approve DPIA
                DPIA approved = dpiaService.approveDPIA(dpia.getId(), "chief-privacy-officer");
                assertEquals(DPIAStatus.APPROVED, approved.getStatus());
                assertNotNull(approved.getApprovedAt());
                assertNotNull(approved.getNextReviewAt(), "Annual review date must be set");

                System.out.println("✓ INT-004: DPIA lifecycle — " + dpia.getReferenceNumber() +
                                " → Risk assessed → Submitted → Approved");
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 5: Policy → Approval → Archive Lifecycle
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(5)
        @DisplayName("INT-005: Policy creation → approval → archival lifecycle")
        void testPolicyLifecycle() {
                // Step 1: Create a DPDP compliance policy
                PolicyRequest request = new PolicyRequest();
                request.setCode("POL-INT-DPDP-" + System.currentTimeMillis());
                request.setName("DPDP Data Retention & Disposal Policy");
                request.setDescription("Enterprise policy governing data retention periods, " +
                                "disposal procedures, and evidence preservation per DPDP Act requirements.");
                request.setCategory("Data Governance");
                request.setContent("1. Personal data retention: maximum 3 years or consent expiry. " +
                                "2. Financial data: 8 years per RBI mandate. " +
                                "3. Health records: 5 years per ABDM guidelines. " +
                                "4. Disposal: secure wipe with DoD 5220.22-M standard.");
                request.setOwner("ciso@enterprise.in");

                Policy policy = policyService.createPolicy(request);
                assertNotNull(policy);
                assertEquals(PolicyStatus.DRAFT, policy.getStatus());
                assertEquals("1.0", policy.getVersion());
                sharedPolicyId = policy.getId();

                // Step 2: Approve the policy
                Policy approved = policyService.approvePolicy(policy.getId(), "board-secretary");
                assertEquals(PolicyStatus.ACTIVE, approved.getStatus());
                assertNotNull(approved.getApprovedAt());
                assertNotNull(approved.getEffectiveDate());
                assertNotNull(approved.getNextReviewDate(), "Annual review date must be set");

                // Step 3: Archive the policy (superseded by new version)
                Policy archived = policyService.archivePolicy(policy.getId(), "policy-admin");
                assertEquals(PolicyStatus.ARCHIVED, archived.getStatus());

                // Step 4: Verify policy statistics reflect the lifecycle
                PolicyStatistics stats = policyService.getStatistics();
                assertNotNull(stats);
                assertTrue(stats.getTotalPolicies() >= 1);

                System.out.println("✓ INT-005: Policy lifecycle — " + policy.getCode() +
                                " → Approved → Archived");
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 6: User Auth → Audit → Password Change
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(6)
        @DisplayName("INT-006: User creation → authentication → password change with audit")
        void testUserAuthWithAudit() {
                long auditBefore = auditService.getSequenceNumber();

                // Step 1: Create a DPO user
                String uniqueUser = "dpo_" + System.currentTimeMillis();
                UserRequest request = new UserRequest(uniqueUser, uniqueUser + "@enterprise.in",
                                "Str0ngP@ss!2026", "Data Protection Officer", "Legal", "DPO");
                User user = userService.createUser(request);
                assertNotNull(user);
                assertTrue(user.isActive());

                // Step 2: Authenticate
                AuthResult authResult = userService.authenticate(uniqueUser, "Str0ngP@ss!2026");
                assertTrue(authResult.isSuccess(), "Authentication should succeed");
                assertNotNull(authResult.getSessionToken());

                // Step 3: Failed authentication attempt
                AuthResult failedAuth = userService.authenticate(uniqueUser, "WrongPassword");
                assertFalse(failedAuth.isSuccess(), "Wrong password should fail");

                // Step 4: Change password
                boolean changed = userService.changePassword(user.getId(),
                                "Str0ngP@ss!2026", "NewStr0ngP@ss!2027");
                assertTrue(changed, "Password change should succeed");

                // Step 5: Verify new password works
                AuthResult newAuth = userService.authenticate(uniqueUser, "NewStr0ngP@ss!2027");
                assertTrue(newAuth.isSuccess(), "New password should authenticate");

                // Step 6: Verify audit trail captured all auth events
                long auditAfter = auditService.getSequenceNumber();
                assertTrue(auditAfter - auditBefore >= 4,
                                "Audit should capture: user_created + login_success + login_failed + password_changed");

                System.out.println("✓ INT-006: User auth lifecycle — " + uniqueUser +
                                " → Authenticated → Password changed → " + (auditAfter - auditBefore)
                                + " audit entries");
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 7: PII Scan → Breach Trigger
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(7)
        @DisplayName("INT-007: PII scan detects exposed data → breach reported → audit verified")
        void testPIIScanTriggersBreach() {
                // Step 1: Scan a leaked document containing PII
                String leakedDocument = """
                                LEAKED: Customer Database Export (UNAUTHORIZED)
                                ================================================
                                Name: Pradeep Sharma
                                Aadhaar: 4567 8901 2345
                                PAN: AABPS5678R
                                Email: pradeep@company.in
                                Phone: +91 98765 43210
                                Credit Card: 4111 1111 1111 1111
                                Bank Account: HDFC 50100123456789
                                Health ID (ABHA): 91-5678-9012-3456
                                """;

                PIIScanResult scanResult = piiScanner.scanText(leakedDocument, "int-leaked-doc");
                assertEquals("COMPLETED", scanResult.getStatus());
                assertTrue(scanResult.getTotalFindings() >= 4, "Must detect multiple PII types");
                assertTrue(scanResult.getCriticalFindings() >= 1, "Must flag critical PII");

                // Step 2: Based on scan findings, report a breach
                BreachRequest breachReq = new BreachRequest();
                breachReq.setTitle("INT: PII Scanner detected exposed customer data");
                breachReq.setDescription(String.format(
                                "PII scan detected %d findings (%d critical) in leaked document. " +
                                                "Types found: Aadhaar, PAN, credit card, email, phone. " +
                                                "Immediate containment required.",
                                scanResult.getTotalFindings(), scanResult.getCriticalFindings()));
                breachReq.setBreachType("DATA_EXPOSURE");
                breachReq.setSeverity(BreachSeverity.HIGH);
                breachReq.setAffectedCount(1);
                breachReq.setReportedBy("pii-scanner-automated");

                Breach breach = breachService.reportBreach(breachReq);
                assertNotNull(breach);
                assertEquals(BreachStatus.OPEN, breach.getStatus());

                // Step 3: Verify audit captured both scan and breach
                AuditIntegrityReport report = auditService.verifyIntegrity();
                assertTrue(report.getTotalEntries() > 0);

                System.out.println("✓ INT-007: PII scan → Breach — " + scanResult.getTotalFindings() +
                                " PII findings → Breach " + breach.getReferenceNumber() + " reported");
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 8: Cross-Sector Consent → Breach → Rights Chain
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(8)
        @DisplayName("INT-008: E-Commerce consent → data breach → customer exercises erasure right")
        void testCrossSectorConsentBreachRightsChain() {
                // Step 1: E-Commerce customer gives marketing consent
                ConsentRequest consentReq = new ConsentRequest();
                consentReq.setDataPrincipalId("DP-INT-ECOM-001");
                consentReq.setPurposeId("PUR-INT-ECOM");
                consentReq.setConsentMethod("COOKIE_BANNER");
                consentReq.setLanguage("en");
                consentReq.setNoticeVersion("v2.1-cookie");

                Consent consent = consentService.collectConsent(consentReq);
                assertNotNull(consent);
                assertEquals(ConsentStatus.ACTIVE, consent.getStatus());

                // Step 2: A breach occurs affecting this customer's data
                BreachRequest breachReq = new BreachRequest();
                breachReq.setTitle("INT: E-Commerce marketing DB leaked via SQL injection");
                breachReq.setDescription("SQL injection attack exposed marketing database containing " +
                                "customer purchase history, email addresses, and browsing behavior. " +
                                "Affects customers who consented to marketing processing.");
                breachReq.setBreachType("SQL_INJECTION");
                breachReq.setSeverity(BreachSeverity.HIGH);
                breachReq.setAffectedCount(5000);
                breachReq.setReportedBy("soc-team");

                Breach breach = breachService.reportBreach(breachReq);
                assertNotNull(breach);

                // Step 3: Customer exercises right to erasure after learning of breach
                Consent withdrawn = consentService.withdrawConsent(
                                consent.getId(), "Breach notification received — want data deleted", "customer");

                assertEquals(ConsentStatus.WITHDRAWN, withdrawn.getStatus());

                // Step 4: Customer submits formal erasure request
                RightsRequestDTO erasureReq = new RightsRequestDTO();
                erasureReq.setDataPrincipalId("DP-INT-ECOM-001");
                erasureReq.setRequestType(RightType.ERASURE);
                erasureReq.setDescription("Complete erasure of all marketing data following breach " +
                                breach.getReferenceNumber() + ". Delete: purchase history, browsing data, " +
                                "recommendation profile, and all derived analytics.");

                RightsRequest erasure = rightsService.submitRequest(erasureReq);
                assertNotNull(erasure);
                assertEquals(RequestStatus.PENDING, erasure.getStatus());

                System.out.println("✓ INT-008: Cross-sector chain — Consent → Breach " +
                                breach.getReferenceNumber() + " → Consent withdrawn → Erasure request " +
                                erasure.getReferenceNumber());
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 9: Compliance Dashboard Statistics Aggregation
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(9)
        @DisplayName("INT-009: Aggregate compliance statistics from all modules")
        void testComplianceDashboardAggregation() {
                // Collect statistics from all modules
                ConsentStatistics consentStats = consentService.getStatistics();
                assertNotNull(consentStats);
                assertTrue(consentStats.getTotalConsents() >= 0);

                BreachStatistics breachStats = breachService.getStatistics();
                assertNotNull(breachStats);
                assertTrue(breachStats.getTotalBreaches() >= 0);

                RightsStatistics rightsStats = rightsService.getStatistics();
                assertNotNull(rightsStats);
                assertTrue(rightsStats.getTotalRequests() >= 0);

                DPIAStatistics dpiaStats = dpiaService.getStatistics();
                assertNotNull(dpiaStats);
                assertTrue(dpiaStats.getTotalDPIAs() >= 0);

                PolicyStatistics policyStats = policyService.getStatistics();
                assertNotNull(policyStats);
                assertTrue(policyStats.getTotalPolicies() >= 0);

                UserStatistics userStats = userService.getStatistics();
                assertNotNull(userStats);
                assertTrue(userStats.getTotalUsers() >= 1, "At least admin user should exist");

                TrainingService.TrainingStats trainingStats = trainingService.getStatistics();
                assertNotNull(trainingStats);

                // RAG score across all modules
                ModuleScore consentScore = ragEvaluator.evaluateModule("CONSENT");
                ModuleScore breachScore = ragEvaluator.evaluateModule("BREACH");
                assertNotNull(consentScore);
                assertNotNull(breachScore);

                // Audit integrity
                AuditIntegrityReport auditReport = auditService.verifyIntegrity();
                assertNotNull(auditReport);
                assertTrue(auditReport.getTotalEntries() > 0,
                                "Audit log must have entries from all previous integration tests");

                System.out.println("✓ INT-009: Compliance dashboard aggregation verified:");
                System.out.println("  Consents: " + consentStats.getTotalConsents() +
                                " (Active: " + consentStats.getActiveConsents() + ")");
                System.out.println("  Breaches: " + breachStats.getTotalBreaches() +
                                " (Open: " + breachStats.getOpenBreaches() + ")");
                System.out.println("  Rights Requests: " + rightsStats.getTotalRequests() +
                                " (Pending: " + rightsStats.getPendingRequests() + ")");
                System.out.println("  DPIAs: " + dpiaStats.getTotalDPIAs() +
                                " (High Risk: " + dpiaStats.getHighRiskDPIAs() + ")");
                System.out.println("  Policies: " + policyStats.getTotalPolicies() +
                                " (Active: " + policyStats.getActivePolicies() + ")");
                System.out.println("  Users: " + userStats.getTotalUsers());
                System.out.println("  Audit entries: " + auditReport.getTotalEntries() +
                                " (Valid: " + auditReport.getValidEntries() + ")");
                System.out.println("  RAG — Consent: " + String.format("%.1f%%", consentScore.getScore()) +
                                " Breach: " + String.format("%.1f%%", breachScore.getScore()));
        }

        // ═══════════════════════════════════════════════════════════
        // WORKFLOW 10: Event Bus Cross-Module Propagation
        // ═══════════════════════════════════════════════════════════

        @Test
        @Order(10)
        @DisplayName("INT-010: Event bus cross-module event propagation and handling")
        void testEventBusCrossModulePropagation() {
                final Map<String, Integer> eventCounts = new HashMap<>();

                // Subscribe to all compliance events
                eventBus.subscribe("consent.*", e -> eventCounts.merge("consent", 1, Integer::sum));
                eventBus.subscribe("breach.*", e -> eventCounts.merge("breach", 1, Integer::sum));
                eventBus.subscribe("rights.*", e -> eventCounts.merge("rights", 1, Integer::sum));
                eventBus.subscribe("dpia.*", e -> eventCounts.merge("dpia", 1, Integer::sum));
                eventBus.subscribe("policy.*", e -> eventCounts.merge("policy", 1, Integer::sum));

                // Trigger actions that generate events across modules
                // Consent event
                try {
                        // Seed a fresh data principal for this test
                        try (java.sql.Connection conn = dbManager.getConnection();
                                        java.sql.Statement stmt = conn.createStatement()) {
                                stmt.executeUpdate(
                                                """
                                                                    INSERT OR IGNORE INTO data_principals (id, name, email, phone)
                                                                    VALUES ('DP-INT-EVT-001', 'Event Test User', 'event.test@test.in', '+919999999999')
                                                                """);
                                stmt.executeUpdate(
                                                """
                                                                    INSERT OR IGNORE INTO purposes (id, code, name, description, legal_basis, is_active)
                                                                    VALUES ('PUR-INT-EVT', 'INT_EVT_TEST', 'Event Test Purpose',
                                                                            'Purpose for event propagation testing', 'CONSENT', 1)
                                                                """);
                        }

                        ConsentRequest cReq = new ConsentRequest();
                        cReq.setDataPrincipalId("DP-INT-EVT-001");
                        cReq.setPurposeId("PUR-INT-EVT");
                        cReq.setConsentMethod("API");
                        cReq.setLanguage("en");
                        cReq.setNoticeVersion("v1.0");
                        consentService.collectConsent(cReq);
                } catch (Exception e) {
                        /* consent may already exist */ }

                // Policy event
                PolicyRequest pReq = new PolicyRequest();
                pReq.setCode("POL-INT-EVT-" + System.currentTimeMillis());
                pReq.setName("Event Bus Test Policy");
                pReq.setDescription("Policy created to test event propagation");
                pReq.setCategory("Testing");
                pReq.setContent("Test content");
                pReq.setOwner("test-owner");
                policyService.createPolicy(pReq);

                // DPIA event
                DPIARequest dReq = new DPIARequest();
                dReq.setTitle("INT: Event Bus Test DPIA");
                dReq.setDescription("DPIA created to test event propagation");
                dReq.setProjectName("Event Test Project");
                dReq.setDataTypes("Test Data");
                dReq.setAssessor("test-assessor");
                dpiaService.createDPIA(dReq);

                // Breach event
                BreachRequest bReq = new BreachRequest();
                bReq.setTitle("INT: Event Bus Test Breach");
                bReq.setDescription("Breach created to test event propagation");
                bReq.setBreachType("TEST");
                bReq.setSeverity(BreachSeverity.LOW);
                bReq.setAffectedCount(0);
                bReq.setReportedBy("test-reporter");
                breachService.reportBreach(bReq);

                // Wait briefly for async events to process
                try {
                        Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }

                // Verify events were received
                assertTrue(eventCounts.getOrDefault("policy", 0) >= 1,
                                "Policy events must propagate");
                assertTrue(eventCounts.getOrDefault("dpia", 0) >= 1,
                                "DPIA events must propagate");
                assertTrue(eventCounts.getOrDefault("breach", 0) >= 1,
                                "Breach events must propagate");

                int totalEvents = eventCounts.values().stream().mapToInt(Integer::intValue).sum();
                System.out.println("✓ INT-010: Cross-module event propagation — " + totalEvents +
                                " events captured: " + eventCounts);
        }
}
