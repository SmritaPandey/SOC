package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qsdpdp.security.SecurityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Data Seeder for QS-DPDP Enterprise
 * Generates ≥500 realistic records per module for testing and demos
 * 
 * @version 1.0.0
 * @since Phase 1
 */
@Component
public class DataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final DatabaseManager dbManager;
    private final SecurityManager securityManager;
    private final Random random = new Random();

    // Sample data pools
    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Ayaan",
            "Krishna", "Ishaan", "Saanvi", "Aanya", "Ananya", "Pari", "Diya", "Aditi",
            "Myra", "Sara", "Aadhya", "Anvi", "Priya", "Neha", "Pooja", "Divya"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Verma", "Gupta", "Singh", "Kumar", "Patel", "Shah", "Mehta",
            "Reddy", "Rao", "Nair", "Menon", "Iyer", "Pillai", "Das", "Mukherjee"
    };

    private static final String[] ORGANIZATIONS = {
            "TechCorp India", "FinServ Solutions", "HealthCare Plus", "EduLearn Systems",
            "RetailMax India", "BankSecure Ltd", "InsureTech Pro", "DataDriven Corp",
            "CloudFirst Solutions", "SecureBank India", "LifeCare Hospitals", "EduTech Global"
    };

    private static final String[] DEPARTMENTS = {
            "IT", "HR", "Finance", "Legal", "Operations", "Marketing", "Sales",
            "Customer Service", "Compliance", "Security", "Data Privacy", "Risk Management"
    };

    private static final String[] PURPOSES = {
            "Marketing Communications", "Service Delivery", "Account Management",
            "Analytics and Insights", "Fraud Prevention", "Legal Compliance",
            "Customer Support", "Product Improvement", "Personalization",
            "Transaction Processing", "Identity Verification", "Credit Assessment"
    };

    private static final String[] BREACH_TYPES = {
            "Unauthorized Access", "Data Theft", "Ransomware", "Phishing", "Insider Threat",
            "Misconfiguration", "Lost Device", "Third-Party Breach", "SQL Injection"
    };

    private static final String[] RIGHT_TYPES = {
            "ACCESS", "CORRECTION", "ERASURE", "PORTABILITY", "OBJECTION", "RESTRICTION"
    };

    @Autowired
    public DataSeeder(DatabaseManager dbManager, SecurityManager securityManager) {
        this.dbManager = dbManager;
        this.securityManager = securityManager;
    }

    /**
     * Seed all modules with realistic data
     */
    public void seedAll() {
        logger.info("Starting data seeding...");

        try {
            seedPurposes();
            seedDataPrincipals();
            seedConsents();
            seedPolicies();
            seedBreaches();
            seedDPIAs();
            seedRightsRequests();
            seedUsers();
            seedControls();
            seedGapAssessments();

            // Module-specific tables may not exist if their services haven't been initialized
            try { seedSiemData(); } catch (Exception e) {
                logger.warn("Skipping SIEM data seeding (tables may not exist): {}", e.getMessage());
            }
            try { seedDlpData(); } catch (Exception e) {
                logger.warn("Skipping DLP data seeding (tables may not exist): {}", e.getMessage());
            }
            try { seedLicensingData(); } catch (Exception e) {
                logger.warn("Skipping Licensing data seeding (tables may not exist): {}", e.getMessage());
            }
            try { seedChatData(); } catch (Exception e) {
                logger.warn("Skipping Chat data seeding (tables may not exist): {}", e.getMessage());
            }
            try { seedSiemSubTables(); } catch (Exception e) {
                logger.warn("Skipping SIEM sub-tables seeding: {}", e.getMessage());
            }
            try { seedDlpSubTables(); } catch (Exception e) {
                logger.warn("Skipping DLP sub-tables seeding: {}", e.getMessage());
            }
            try { seedPaymentData(); } catch (Exception e) {
                logger.warn("Skipping Payment data seeding: {}", e.getMessage());
            }
            try { seedLicensingAgreements(); } catch (Exception e) {
                logger.warn("Skipping Licensing agreements seeding: {}", e.getMessage());
            }
            try { seedReportExecutions(); } catch (Exception e) {
                logger.warn("Skipping Report executions seeding: {}", e.getMessage());
            }

            logger.info("Data seeding completed successfully");

        } catch (Exception e) {
            logger.error("Error during data seeding", e);
            throw new RuntimeException("Data seeding failed", e);
        }
    }

    private void seedPurposes() throws Exception {
        logger.info("Seeding purposes...");

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                        INSERT OR IGNORE INTO purposes (id, code, name, description, legal_basis, data_categories, retention_period_days, is_active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                    """;

            String[] legalBases = { "CONSENT", "CONTRACT", "LEGAL_OBLIGATION", "VITAL_INTERESTS", "PUBLIC_INTEREST",
                    "LEGITIMATE_INTERESTS" };
            String[] dataCategories = { "Personal", "Contact", "Financial", "Health", "Behavioral", "Transaction" };

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < PURPOSES.length; i++) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "PUR-" + String.format("%03d", i + 1));
                    stmt.setString(3, PURPOSES[i]);
                    stmt.setString(4, "Purpose for " + PURPOSES[i].toLowerCase());
                    stmt.setString(5, legalBases[random.nextInt(legalBases.length)]);
                    stmt.setString(6, dataCategories[random.nextInt(dataCategories.length)]);
                    stmt.setInt(7, 365 * (1 + random.nextInt(7))); // 1-7 years
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded {} purposes", PURPOSES.length);
    }

    private void seedDataPrincipals() throws Exception {
        logger.info("Seeding data principals...");

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                        INSERT OR IGNORE INTO data_principals (id, external_id, name, email, phone, is_child)
                        VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 600; i++) {
                    String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                    String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                    String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + (i > 0 ? i : "")
                            + "@example.com";

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "EXT-" + String.format("%06d", i + 1));
                    stmt.setString(3, firstName + " " + lastName);
                    stmt.setString(4, email);
                    stmt.setString(5, "+91" + (7000000000L + random.nextInt(999999999)));
                    stmt.setInt(6, random.nextInt(100) < 5 ? 1 : 0); // 5% children
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded 600 data principals");
    }

    private void seedConsents() throws Exception {
        logger.info("Seeding consents...");

        try (Connection conn = dbManager.getConnection()) {
            // Get principal and purpose IDs
            List<String> principalIds = new ArrayList<>();
            List<String> purposeIds = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM data_principals");
                    var rs = stmt.executeQuery()) {
                while (rs.next())
                    principalIds.add(rs.getString("id"));
            }

            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM purposes");
                    var rs = stmt.executeQuery()) {
                while (rs.next())
                    purposeIds.add(rs.getString("id"));
            }

            String sql = """
                        INSERT OR IGNORE INTO consents (id, data_principal_id, purpose_id, status, consent_method,
                            notice_version, language, collected_at, expires_at, hash, prev_hash)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            String[] statuses = { "ACTIVE", "ACTIVE", "ACTIVE", "ACTIVE", "WITHDRAWN", "EXPIRED" };
            String[] methods = { "WEB_FORM", "MOBILE_APP", "PAPER", "VERBAL", "API" };
            String[] languages = { "en", "hi", "ta", "te", "mr", "bn" };

            String prevHash = "GENESIS";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 800; i++) {
                    String currentHash = UUID.randomUUID().toString();
                    LocalDateTime collectedAt = LocalDateTime.now().minus(random.nextInt(730), ChronoUnit.DAYS);
                    LocalDateTime expiresAt = collectedAt.plusYears(1 + random.nextInt(3));

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, principalIds.get(random.nextInt(principalIds.size())));
                    stmt.setString(3, purposeIds.get(random.nextInt(purposeIds.size())));
                    stmt.setString(4, statuses[random.nextInt(statuses.length)]);
                    stmt.setString(5, methods[random.nextInt(methods.length)]);
                    stmt.setString(6, "v1." + random.nextInt(5));
                    stmt.setString(7, languages[random.nextInt(languages.length)]);
                    stmt.setString(8, collectedAt.toString());
                    stmt.setString(9, expiresAt.toString());
                    stmt.setString(10, currentHash);
                    stmt.setString(11, prevHash);
                    stmt.executeUpdate();

                    prevHash = currentHash;
                }
            }
        }

        logger.info("Seeded 800 consents");
    }

    private void seedPolicies() throws Exception {
        logger.info("Seeding policies...");

        String[] policyNames = {
                "Privacy Policy", "Data Protection Policy", "Cookie Policy", "Data Retention Policy",
                "Breach Response Policy", "Access Control Policy", "Encryption Policy", "Consent Management Policy",
                "Third-Party Processing Policy", "Cross-Border Transfer Policy", "Children's Data Policy",
                "Data Subject Rights Policy", "DPIA Policy", "Vendor Management Policy", "Incident Response Policy"
        };

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                        INSERT OR IGNORE INTO policies (id, code, title, description, category, version, status,
                            effective_date, review_date, owner, dpdp_sections)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            String[] categories = { "PRIVACY", "SECURITY", "COMPLIANCE", "OPERATIONAL" };
            String[] statuses = { "APPROVED", "APPROVED", "APPROVED", "DRAFT", "PENDING_REVIEW" };

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < policyNames.length; i++) {
                    LocalDateTime effectiveDate = LocalDateTime.now().minus(random.nextInt(365), ChronoUnit.DAYS);
                    LocalDateTime reviewDate = effectiveDate.plusYears(1);

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "POL-" + String.format("%03d", i + 1));
                    stmt.setString(3, policyNames[i]);
                    stmt.setString(4, "Organization policy for " + policyNames[i].toLowerCase());
                    stmt.setString(5, categories[random.nextInt(categories.length)]);
                    stmt.setString(6, "1." + random.nextInt(10));
                    stmt.setString(7, statuses[random.nextInt(statuses.length)]);
                    stmt.setString(8, effectiveDate.toString());
                    stmt.setString(9, reviewDate.toString());
                    stmt.setString(10, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                            + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
                    stmt.setString(11, "Section " + (1 + random.nextInt(44)));
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded {} policies", policyNames.length);
    }

    private void seedBreaches() throws Exception {
        logger.info("Seeding breaches...");

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                        INSERT OR IGNORE INTO breaches (id, reference_number, title, description, severity, breach_type,
                            affected_count, detected_at, status, dpbi_notified, dpbi_notification_date,
                            certin_notified, affected_parties_notified, reported_by, assigned_to)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            String[] severities = { "LOW", "MEDIUM", "HIGH", "CRITICAL" };
            String[] statuses = { "OPEN", "INVESTIGATING", "CONTAINED", "RESOLVED", "CLOSED" };

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 50; i++) {
                    String severity = severities[random.nextInt(severities.length)];
                    LocalDateTime detectedAt = LocalDateTime.now().minus(random.nextInt(180), ChronoUnit.DAYS);
                    boolean dpbiNotified = random.nextBoolean();

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "BRE-" + LocalDateTime.now().getYear() + "-" + String.format("%04d", i + 1));
                    stmt.setString(3, BREACH_TYPES[random.nextInt(BREACH_TYPES.length)] + " Incident");
                    stmt.setString(4, "Security breach involving "
                            + BREACH_TYPES[random.nextInt(BREACH_TYPES.length)].toLowerCase());
                    stmt.setString(5, severity);
                    stmt.setString(6, BREACH_TYPES[random.nextInt(BREACH_TYPES.length)]);
                    stmt.setInt(7, random.nextInt(10000));
                    stmt.setString(8, detectedAt.toString());
                    stmt.setString(9, statuses[random.nextInt(statuses.length)]);
                    stmt.setInt(10, dpbiNotified ? 1 : 0);
                    stmt.setString(11, dpbiNotified ? detectedAt.plusDays(random.nextInt(3)).toString() : null);
                    stmt.setInt(12, severity.equals("CRITICAL") ? 1 : 0);
                    stmt.setInt(13, random.nextBoolean() ? 1 : 0);
                    stmt.setString(14, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                            + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
                    stmt.setString(15, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                            + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded 50 breaches");
    }

    private void seedDPIAs() throws Exception {
        logger.info("Seeding DPIAs...");

        String[] activities = {
                "Customer Profiling", "Credit Scoring", "Employee Monitoring", "CCTV Surveillance",
                "Biometric Authentication", "Location Tracking", "Health Data Processing",
                "Marketing Analytics", "AI-based Decision Making", "Cross-border Transfer"
        };

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                        INSERT OR IGNORE INTO dpias (id, reference_number, title, description, processing_activity,
                            involves_sensitive_data, involves_children_data, cross_border_transfer,
                            automated_decision_making, risk_score, risk_level, status, assessor, next_review_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            String[] riskLevels = { "LOW", "MEDIUM", "HIGH" };
            String[] statuses = { "DRAFT", "PENDING_REVIEW", "APPROVED", "REQUIRES_UPDATE" };

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 30; i++) {
                    String activity = activities[random.nextInt(activities.length)];
                    int riskScore = random.nextInt(100);
                    String riskLevel = riskScore >= 70 ? "HIGH" : riskScore >= 40 ? "MEDIUM" : "LOW";

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "DPIA-" + LocalDateTime.now().getYear() + "-" + String.format("%04d", i + 1));
                    stmt.setString(3, activity + " DPIA");
                    stmt.setString(4, "Data Protection Impact Assessment for " + activity.toLowerCase());
                    stmt.setString(5, activity);
                    stmt.setInt(6, random.nextBoolean() ? 1 : 0);
                    stmt.setInt(7, random.nextInt(10) < 2 ? 1 : 0);
                    stmt.setInt(8, random.nextBoolean() ? 1 : 0);
                    stmt.setInt(9, activity.contains("AI") ? 1 : 0);
                    stmt.setInt(10, riskScore);
                    stmt.setString(11, riskLevel);
                    stmt.setString(12, statuses[random.nextInt(statuses.length)]);
                    stmt.setString(13, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                            + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
                    stmt.setString(14, LocalDateTime.now().plusMonths(6 + random.nextInt(12)).toString());
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded 30 DPIAs");
    }

    private void seedRightsRequests() throws Exception {
        logger.info("Seeding rights requests...");

        try (Connection conn = dbManager.getConnection()) {
            List<String> principalIds = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM data_principals LIMIT 200");
                    var rs = stmt.executeQuery()) {
                while (rs.next())
                    principalIds.add(rs.getString("id"));
            }

            String sql = """
                        INSERT OR IGNORE INTO rights_requests (id, reference_number, data_principal_id, request_type,
                            description, status, priority, received_at, deadline, assigned_to)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            String[] statuses = { "PENDING", "IN_PROGRESS", "COMPLETED", "REJECTED" };
            String[] priorities = { "LOW", "NORMAL", "HIGH", "URGENT" };

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 150; i++) {
                    String rightType = RIGHT_TYPES[random.nextInt(RIGHT_TYPES.length)];
                    LocalDateTime receivedAt = LocalDateTime.now().minus(random.nextInt(90), ChronoUnit.DAYS);
                    LocalDateTime deadline = receivedAt.plusDays(30);

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "DSR-" + LocalDateTime.now().getYear() + "-" + String.format("%05d", i + 1));
                    stmt.setString(3, principalIds.get(random.nextInt(principalIds.size())));
                    stmt.setString(4, rightType);
                    stmt.setString(5, "Request for " + rightType.toLowerCase() + " of personal data");
                    stmt.setString(6, statuses[random.nextInt(statuses.length)]);
                    stmt.setString(7, priorities[random.nextInt(priorities.length)]);
                    stmt.setString(8, receivedAt.toString());
                    stmt.setString(9, deadline.toString());
                    stmt.setString(10, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                            + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded 150 rights requests");
    }

    private void seedUsers() throws Exception {
        logger.info("Seeding users...");

        try (Connection conn = dbManager.getConnection()) {
            // ── Pre-registered Trial Users with known passwords ──
            String trialSql = """
                        INSERT OR IGNORE INTO users (id, username, email, password_hash, full_name, role, department, phone, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """;

            String[][] trialUsers = {
                // id, username, email, password, full_name, role, department, phone
                { "admin-001", "admin", "admin@qsdpdp.local", "Admin@1234", "System Administrator", "ADMIN", "IT", "+919999900001" },
                { "trial-001", "trial", "trial@qsdpdp.local", "Trial@1234", "Trial User", "ANALYST", "Compliance", "+919999900002" },
                { "dp-001",    "dp",    "dp@qsdpdp.local",    "Dp@12345678",  "Data Principal User", "DATA_PRINCIPAL", "Customer", "+919999900003" },
                { "demo-001",  "demo",  "demo@neurqailabs.com", "Demo@2026", "Demo User (NeurQ AI Labs)", "ANALYST", "Demo", "+919999900004" },
            };

            try (PreparedStatement stmt = conn.prepareStatement(trialSql)) {
                for (String[] user : trialUsers) {
                    stmt.setString(1, user[0]);
                    stmt.setString(2, user[1]);
                    stmt.setString(3, user[2]);
                    stmt.setString(4, securityManager.hashPassword(user[3]));
                    stmt.setString(5, user[4]);
                    stmt.setString(6, user[5]);
                    stmt.setString(7, user[6]);
                    stmt.setString(8, user[7]);
                    stmt.executeUpdate();
                }
            }
            logger.info("Seeded 4 trial users (admin/Admin@1234, trial/Trial@1234, dp/Dp@12345678, demo/Demo@2026)");

            // ── Random demo users ──
            String sql = """
                        INSERT OR IGNORE INTO users (id, username, email, password_hash, full_name, role, department, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """;

            String[] roles = { "USER", "ANALYST", "MANAGER", "DPO", "ADMIN" };

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 25; i++) {
                    String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                    String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                    String username = firstName.toLowerCase() + "." + lastName.toLowerCase();

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, username);
                    stmt.setString(3, username + "@company.com");
                    stmt.setString(4, securityManager.hashPassword("Demo@1234"));
                    stmt.setString(5, firstName + " " + lastName);
                    stmt.setString(6, roles[random.nextInt(roles.length)]);
                    stmt.setString(7, DEPARTMENTS[random.nextInt(DEPARTMENTS.length)]);
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded 28 users (3 trial + 25 demo)");
    }

    private void seedControls() throws Exception {
        logger.info("Seeding controls...");

        String[][] controls = {
                { "DPDP-CON-001", "Consent Collection", "CONSENT", "Technical" },
                { "DPDP-CON-002", "Consent Withdrawal", "CONSENT", "Technical" },
                { "DPDP-CON-003", "Consent Records", "CONSENT", "Administrative" },
                { "DPDP-BRE-001", "Breach Detection", "BREACH", "Technical" },
                { "DPDP-BRE-002", "Breach Notification", "BREACH", "Administrative" },
                { "DPDP-BRE-003", "Incident Response", "BREACH", "Operational" },
                { "DPDP-RIG-001", "Rights Request Handling", "RIGHTS", "Administrative" },
                { "DPDP-RIG-002", "Identity Verification", "RIGHTS", "Technical" },
                { "DPDP-SEC-001", "Access Control", "SECURITY", "Technical" },
                { "DPDP-SEC-002", "Data Encryption", "SECURITY", "Technical" },
                { "DPDP-SEC-003", "Audit Logging", "SECURITY", "Technical" },
                { "DPDP-DPI-001", "DPIA Process", "DPIA", "Administrative" },
                { "DPDP-RET-001", "Data Retention", "RETENTION", "Administrative" },
                { "DPDP-CHI-001", "Children's Data Protection", "CHILDREN", "Administrative" }
        };

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                        INSERT OR IGNORE INTO controls (id, control_id, name, category, control_type,
                            dpdp_section, test_frequency, status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            String[] frequencies = { "DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "ANNUALLY" };
            String[] statuses = { "TESTED", "NOT_TESTED", "PENDING" };

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (String[] control : controls) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, control[0]);
                    stmt.setString(3, control[1]);
                    stmt.setString(4, control[2]);
                    stmt.setString(5, control[3]);
                    stmt.setString(6, "Section " + (1 + random.nextInt(44)));
                    stmt.setString(7, frequencies[random.nextInt(frequencies.length)]);
                    stmt.setString(8, statuses[random.nextInt(statuses.length)]);
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded {} controls", controls.length);
    }

    private void seedGapAssessments() throws Exception {
        logger.info("Seeding gap assessments...");

        String[] sectors = {
                "Banking", "Insurance", "Healthcare", "Education", "Retail",
                "Technology", "Manufacturing", "Telecom", "Government", "Fintech"
        };

        try (Connection conn = dbManager.getConnection()) {
            String sql = """
                        INSERT OR IGNORE INTO gap_assessments (id, name, sector, overall_score, status, rag_status, assessor)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            String[] statuses = { "COMPLETED", "IN_PROGRESS", "NOT_STARTED" };

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 20; i++) {
                    double score = 30 + random.nextDouble() * 70;
                    String ragStatus = score >= 80 ? "GREEN" : score >= 50 ? "AMBER" : "RED";

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, ORGANIZATIONS[random.nextInt(ORGANIZATIONS.length)] + " Assessment");
                    stmt.setString(3, sectors[random.nextInt(sectors.length)]);
                    stmt.setDouble(4, score);
                    stmt.setString(5, statuses[random.nextInt(statuses.length)]);
                    stmt.setString(6, ragStatus);
                    stmt.setString(7, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                            + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded 20 gap assessments");
    }

    /**
     * Get total record count across all seeded tables
     */
    public Map<String, Integer> getRecordCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();

        String[] tables = { "data_principals", "consents", "purposes", "policies", "breaches",
                "dpias", "rights_requests", "users", "controls", "gap_assessments",
                "siem_events", "siem_alerts", "dlp_policies", "dlp_incidents", "licenses" };

        try (Connection conn = dbManager.getConnection()) {
            for (String table : tables) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
                        var rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        counts.put(table, rs.getInt(1));
                    }
                } catch (Exception ignored) {
                    // Table may not exist yet
                }
            }
        } catch (Exception e) {
            logger.error("Error getting record counts", e);
        }

        return counts;
    }

    // ═══════════════════════════════════════════════════════════════
    // SIEM Data Seeding
    // ═══════════════════════════════════════════════════════════════
    private void seedSiemData() throws Exception {
        logger.info("Seeding SIEM events and alerts...");

        try (Connection conn = dbManager.getConnection()) {
            // Create siem_events table for dashboard queries (SIEM service uses security_events)
            try (var createStmt = conn.createStatement()) {
                createStmt.execute("""
                    CREATE TABLE IF NOT EXISTS siem_events (
                        id TEXT PRIMARY KEY,
                        source TEXT,
                        category TEXT,
                        severity TEXT,
                        description TEXT,
                        timestamp TIMESTAMP,
                        source_ip TEXT,
                        destination_ip TEXT,
                        user_id TEXT,
                        raw_log TEXT
                    )
                """);
            }

            // Seed SIEM Events
            String eventSql = """
                INSERT OR IGNORE INTO siem_events (id, source, category, severity, description, timestamp, source_ip, destination_ip, user_id, raw_log)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            String[] sources = { "Firewall", "IDS/IPS", "WAF", "Endpoint", "Active Directory", "Email Gateway", "DLP Agent", "VPN Gateway", "Database Monitor", "API Gateway" };
            String[] categories = { "AUTHENTICATION", "AUTHORIZATION", "DATA_ACCESS", "NETWORK", "MALWARE", "POLICY_VIOLATION", "ANOMALY", "COMPLIANCE" };
            String[] severities = { "LOW", "MEDIUM", "HIGH", "CRITICAL" };
            String[][] eventDescriptions = {
                { "Failed login attempt from unknown IP", "AUTHENTICATION" },
                { "Successful brute-force detection", "AUTHENTICATION" },
                { "Unauthorized access to PII database", "DATA_ACCESS" },
                { "Suspicious outbound data transfer detected", "NETWORK" },
                { "Malware signature detected on endpoint", "MALWARE" },
                { "DLP policy violation - PII in email", "POLICY_VIOLATION" },
                { "Unusual data access pattern detected", "ANOMALY" },
                { "Cross-border data transfer without consent", "COMPLIANCE" },
                { "Privilege escalation attempt blocked", "AUTHORIZATION" },
                { "SQL injection attempt on API endpoint", "NETWORK" },
                { "Bulk data export by non-admin user", "DATA_ACCESS" },
                { "Certificate expiration warning", "COMPLIANCE" },
                { "Ransomware signature in uploaded file", "MALWARE" },
                { "DPDP breach threshold reached", "COMPLIANCE" },
                { "VPN connection from blacklisted country", "NETWORK" }
            };

            try (PreparedStatement stmt = conn.prepareStatement(eventSql)) {
                for (int i = 0; i < 200; i++) {
                    String[] desc = eventDescriptions[random.nextInt(eventDescriptions.length)];
                    LocalDateTime ts = LocalDateTime.now().minus(random.nextInt(30), ChronoUnit.DAYS)
                            .minus(random.nextInt(24 * 60), ChronoUnit.MINUTES);

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, sources[random.nextInt(sources.length)]);
                    stmt.setString(3, desc[1]);
                    stmt.setString(4, severities[random.nextInt(severities.length)]);
                    stmt.setString(5, desc[0]);
                    stmt.setString(6, ts.toString());
                    stmt.setString(7, "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256));
                    stmt.setString(8, "192.168." + random.nextInt(256) + "." + random.nextInt(256));
                    stmt.setString(9, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)].toLowerCase() + "." + LAST_NAMES[random.nextInt(LAST_NAMES.length)].toLowerCase());
                    stmt.setString(10, "{\"event_id\": \"EVT-" + (10000 + i) + "\", \"action\": \"" + desc[0] + "\"}");
                    stmt.executeUpdate();
                }
            }

            // Seed SIEM Alerts (siem_alerts table is created by SIEMService)
            // Only insert if siem_alerts table exists
            try {
            String alertSql = """
                INSERT OR IGNORE INTO siem_alerts (id, title, description, severity, status, category, rule_id, rule_name, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            String[][] alerts = {
                { "Brute Force Attack Detected", "Multiple failed login attempts from same IP range within 5 minutes", "CRITICAL" },
                { "PII Data Exfiltration Attempt", "Large volume PII transfer to unauthorized external endpoint", "CRITICAL" },
                { "Unauthorized Admin Access", "Non-admin user attempted privilege escalation to DBA role", "HIGH" },
                { "DPDP Compliance Violation", "Cross-border transfer of sensitive personal data without valid consent", "HIGH" },
                { "Ransomware Indicator Detected", "Endpoint showing encryption behavior matching ransomware patterns", "CRITICAL" },
                { "Expired Consent Data Processing", "Data processing detected using expired consent records", "MEDIUM" },
                { "Unusual Data Access Pattern", "User accessed 50x normal data volume in single session", "HIGH" },
                { "API Rate Limit Breach", "Single client exceeded 10,000 requests/minute threshold", "MEDIUM" },
                { "Certificate Expiry Warning", "SSL certificate for production API gateway expires in 7 days", "LOW" },
                { "DLP Policy Violation", "Aadhaar numbers detected in outbound email attachment", "HIGH" },
                { "Failed DPIA Threshold", "Processing activity risk score exceeded 80 without approved DPIA", "MEDIUM" },
                { "Insider Threat Indicator", "Employee downloading bulk PII records outside business hours", "CRITICAL" },
                { "VPN Anomaly Detected", "VPN connection from geographically impossible location within 30 minutes", "HIGH" },
                { "Database Audit Alert", "Direct SQL query execution bypassing application layer controls", "HIGH" },
                { "Consent Renewal Required", "Batch of 500+ consents expiring within 30 days", "LOW" }
            };

            String[] alertStatuses = { "OPEN", "OPEN", "ACKNOWLEDGED", "INVESTIGATING", "RESOLVED", "CLOSED" };
            String[] alertTypes = { "CORRELATION", "THRESHOLD", "ANOMALY", "SIGNATURE", "COMPLIANCE" };

            try (PreparedStatement stmt = conn.prepareStatement(alertSql)) {
                for (int i = 0; i < alerts.length; i++) {
                    LocalDateTime createdAt = LocalDateTime.now().minus(random.nextInt(14), ChronoUnit.DAYS);

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, alerts[i][0]);
                    stmt.setString(3, alerts[i][1]);
                    stmt.setString(4, alerts[i][2]);
                    stmt.setString(5, alertStatuses[random.nextInt(alertStatuses.length)]);
                    stmt.setString(6, alertTypes[random.nextInt(alertTypes.length)]);
                    stmt.setString(7, "RULE-" + String.format("%03d", i + 1));
                    stmt.setString(8, "Alert Rule " + (i + 1));
                    stmt.setString(9, createdAt.toString());
                    stmt.executeUpdate();
                }
            }
            } catch (Exception e) {
                logger.warn("Could not seed siem_alerts (table may not exist yet): {}", e.getMessage());
            }

            // Seed SOAR Executions (soar_executions table is created by SIEMService)
            try {
            String soarSql = """
                INSERT OR IGNORE INTO soar_executions (id, playbook_id, playbook_name, alert_id, status, started_at, completed_at, current_step, total_steps, step_results)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            String[][] playbooks = {
                { "PB-001", "Breach Containment Playbook" },
                { "PB-002", "DPDP Board Notification" },
                { "PB-003", "Account Lockout Response" },
                { "PB-004", "CERT-In 6-Hour Notification" },
                { "PB-005", "PII Exposure Remediation" }
            };

            try (PreparedStatement stmt = conn.prepareStatement(soarSql)) {
                for (int i = 0; i < 8; i++) {
                    String[] pb = playbooks[random.nextInt(playbooks.length)];
                    LocalDateTime startedAt = LocalDateTime.now().minus(random.nextInt(7), ChronoUnit.DAYS);
                    int totalSteps = 3 + random.nextInt(5);
                    int completed = random.nextInt(totalSteps + 1);
                    String status = completed == totalSteps ? "COMPLETED" : "IN_PROGRESS";

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, pb[0]);
                    stmt.setString(3, pb[1]);
                    stmt.setString(4, UUID.randomUUID().toString());
                    stmt.setString(5, status);
                    stmt.setString(6, startedAt.toString());
                    stmt.setString(7, status.equals("COMPLETED") ? startedAt.plusMinutes(5 + random.nextInt(55)).toString() : null);
                    stmt.setInt(8, completed);
                    stmt.setInt(9, totalSteps);
                    stmt.setString(10, "{\"playbook\": \"" + pb[1] + "\", \"result\": \"" + status + "\"}");
                    stmt.executeUpdate();
                }
            }
            } catch (Exception e) {
                logger.warn("Could not seed soar_executions (table may not exist yet): {}", e.getMessage());
            }
        }

        logger.info("Seeded 200 SIEM events, 15 alerts, 8 SOAR executions");
    }

    // ═══════════════════════════════════════════════════════════════
    // DLP Data Seeding
    // ═══════════════════════════════════════════════════════════════
    private void seedDlpData() throws Exception {
        logger.info("Seeding DLP policies and incidents...");

        try (Connection conn = dbManager.getConnection()) {
            // Seed DLP Policies (dlp_policies table is created by DLPService)
            String policySql = """
                INSERT OR IGNORE INTO dlp_policies (id, name, description, enabled, priority, protected_data_types, primary_action, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            String[][] policies = {
                { "Aadhaar Number Protection", "Detect and block Aadhaar numbers in outbound communications", "CRITICAL", "PII,Identity", "EMAIL,FILE_TRANSFER,USB,PRINT" },
                { "PAN Card Detection", "Monitor for PAN card numbers in all channels", "HIGH", "PII,Financial", "EMAIL,FILE_TRANSFER,NETWORK" },
                { "Health Data Safety", "Prevent unauthorized sharing of health records", "CRITICAL", "Health,PII", "EMAIL,FILE_TRANSFER,USB,NETWORK" },
                { "Financial Data Control", "Monitor bank account and credit card data", "HIGH", "Financial,PII", "EMAIL,FILE_TRANSFER,PRINT" },
                { "Children's Data Protection", "Block any transfer of minor's personal data", "CRITICAL", "Children,PII", "EMAIL,FILE_TRANSFER,USB,NETWORK,PRINT" },
                { "Cross-Border Transfer Check", "Verify consent for international data transfers", "HIGH", "PII,Cross-Border", "NETWORK,FILE_TRANSFER" },
                { "Employee PII Shield", "Protect employee personal information", "MEDIUM", "PII,HR", "EMAIL,FILE_TRANSFER" },
                { "Biometric Data Lock", "Prevent biometric data from leaving secure zone", "CRITICAL", "Biometric,PII", "ALL" },
                { "Contact Info Filter", "Monitor phone numbers and email addresses in bulk", "MEDIUM", "Contact,PII", "EMAIL,FILE_TRANSFER" },
                { "Source Code Protection", "Prevent source code leakage", "HIGH", "IP,Technical", "EMAIL,FILE_TRANSFER,USB,NETWORK" },
                { "Customer Database Guard", "Block bulk customer record exports", "HIGH", "PII,Customer", "FILE_TRANSFER,USB,NETWORK" },
                { "Consent Record Protection", "Ensure consent records are not tampered", "MEDIUM", "Compliance,PII", "FILE_TRANSFER,DATABASE" },
                { "Legal Document Control", "Monitor sharing of legal and compliance documents", "LOW", "Legal,Compliance", "EMAIL,FILE_TRANSFER" },
                { "USB Mass Storage Block", "Block all USB mass storage for sensitive departments", "HIGH", "PII,All", "USB" },
                { "Print Watermarking", "Watermark all printed documents containing PII", "LOW", "PII,All", "PRINT" }
            };

            try (PreparedStatement stmt = conn.prepareStatement(policySql)) {
                for (int i = 0; i < policies.length; i++) {
                    LocalDateTime createdAt = LocalDateTime.now().minus(90 + random.nextInt(270), ChronoUnit.DAYS);
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, policies[i][0]);
                    stmt.setString(3, policies[i][1]);
                    stmt.setInt(4, random.nextInt(10) < 8 ? 1 : 0); // 80% enabled
                    stmt.setInt(5, 50 + random.nextInt(50)); // priority
                    stmt.setString(6, policies[i][3]); // protected_data_types
                    stmt.setString(7, policies[i][2].equals("CRITICAL") ? "BLOCK" : policies[i][2].equals("HIGH") ? "QUARANTINE" : "ALERT"); // primary_action
                    stmt.setString(8, createdAt.toString());
                    stmt.setString(9, createdAt.plusDays(random.nextInt(30)).toString());
                    stmt.executeUpdate();
                }
            }

            // Seed DLP Incidents
            String incidentSql = """
                INSERT OR IGNORE INTO dlp_incidents (id, policy_id, policy_name, source_user, destination_type, severity, status, action_taken, detected_at, resolved_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            String[] channels = { "EMAIL", "FILE_TRANSFER", "USB", "PRINT", "NETWORK" };
            String[] incidentStatuses = { "OPEN", "OPEN", "INVESTIGATING", "RESOLVED", "FALSE_POSITIVE" };
            String[] actions = { "BLOCKED", "QUARANTINED", "ALERTED", "LOGGED" };
            String[][] incidentDescs = {
                { "Aadhaar numbers detected in email attachment to external recipient", "CRITICAL" },
                { "Bulk PAN card data found in USB file transfer", "HIGH" },
                { "Health records shared via personal email", "CRITICAL" },
                { "Customer database export to untrusted cloud service", "HIGH" },
                { "Employee salary data in print queue", "MEDIUM" },
                { "Minor's data found in marketing email campaign", "CRITICAL" },
                { "Source code uploaded to public repository", "HIGH" },
                { "Contact information bulk export detected", "MEDIUM" },
                { "Financial statements sent to personal drive", "HIGH" },
                { "Biometric templates found in email attachment", "CRITICAL" }
            };

            try (PreparedStatement stmt = conn.prepareStatement(incidentSql)) {
                for (int i = 0; i < 40; i++) {
                    String[] desc = incidentDescs[random.nextInt(incidentDescs.length)];
                    LocalDateTime detectedAt = LocalDateTime.now().minus(random.nextInt(60), ChronoUnit.DAYS);
                    String status = incidentStatuses[random.nextInt(incidentStatuses.length)];

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "DLP-POL-" + String.format("%03d", random.nextInt(15) + 1));
                    stmt.setString(3, policies[random.nextInt(policies.length)][0]); // policy_name
                    stmt.setString(4, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)].toLowerCase() + "." + LAST_NAMES[random.nextInt(LAST_NAMES.length)].toLowerCase());
                    stmt.setString(5, channels[random.nextInt(channels.length)]); // destination_type
                    stmt.setString(6, desc[1]);
                    stmt.setString(7, status);
                    stmt.setString(8, actions[random.nextInt(actions.length)]);
                    stmt.setString(9, detectedAt.toString());
                    stmt.setString(10, status.equals("RESOLVED") ? detectedAt.plusHours(1 + random.nextInt(72)).toString() : null);
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded 15 DLP policies, 40 DLP incidents");
    }

    // ═══════════════════════════════════════════════════════════════
    // Licensing Data Seeding
    // ═══════════════════════════════════════════════════════════════
    private void seedLicensingData() throws Exception {
        logger.info("Seeding licensing and pricing data...");

        try (Connection conn = dbManager.getConnection()) {
            // Create licenses table
            try (var stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS licenses (
                        id TEXT PRIMARY KEY,
                        license_key TEXT UNIQUE NOT NULL,
                        license_type TEXT NOT NULL,
                        status TEXT DEFAULT 'ACTIVE',
                        organization TEXT,
                        issued_at TEXT DEFAULT (datetime('now')),
                        expires_at TEXT,
                        activated_at TEXT,
                        max_users INTEGER DEFAULT 5,
                        modules_enabled TEXT,
                        contact_email TEXT
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS pricing_tiers (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        price_monthly REAL NOT NULL,
                        price_annual REAL NOT NULL,
                        max_users INTEGER NOT NULL,
                        modules TEXT NOT NULL,
                        features TEXT,
                        support_level TEXT DEFAULT 'Email',
                        is_popular INTEGER DEFAULT 0
                    )
                """);
            }

            // Seed active license
            String licenseSql = "INSERT OR IGNORE INTO licenses (id, license_key, license_type, status, organization, expires_at, max_users, modules_enabled, contact_email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(licenseSql)) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, "NDCP-ENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                stmt.setString(3, "ENTERPRISE");
                stmt.setString(4, "ACTIVE");
                stmt.setString(5, "N-DCP Demo Organization");
                stmt.setString(6, LocalDateTime.now().plusYears(1).toString());
                stmt.setInt(7, 999);
                stmt.setString(8, "ALL");
                stmt.setString(9, "admin@ndcp-demo.com");
                stmt.executeUpdate();
            }

            // Seed pricing tiers
            String tierSql = "INSERT OR IGNORE INTO pricing_tiers (id, name, price_monthly, price_annual, max_users, modules, features, support_level, is_popular) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String[][] tiers = {
                { "Starter", "999", "9990", "5", "Consent,Rights,Audit", "Core compliance,Basic reports,Email notifications,Single org", "Email", "0" },
                { "Professional", "4999", "49990", "25", "Consent,Rights,Audit,Breach,DPIA,Policy,Gap Analysis", "All core modules,Advanced analytics,CERT-In automation,Multi-org,API access", "Priority", "1" },
                { "Enterprise", "14999", "149990", "999", "ALL", "All modules,SIEM+SOAR,DLP (Quantum Safe),AI Chatbot,Custom integrations,Dedicated support,SLA guarantee,On-premise option", "Dedicated", "0" }
            };

            try (PreparedStatement stmt = conn.prepareStatement(tierSql)) {
                for (int i = 0; i < tiers.length; i++) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, tiers[i][0]);
                    stmt.setDouble(3, Double.parseDouble(tiers[i][1]));
                    stmt.setDouble(4, Double.parseDouble(tiers[i][2]));
                    stmt.setInt(5, Integer.parseInt(tiers[i][3]));
                    stmt.setString(6, tiers[i][4]);
                    stmt.setString(7, tiers[i][5]);
                    stmt.setString(8, tiers[i][6]);
                    stmt.setInt(9, Integer.parseInt(tiers[i][7]));
                    stmt.executeUpdate();
                }
            }
        }

        logger.info("Seeded licensing data with 3 pricing tiers");
    }

    // ═══════════════════════════════════════════════════════════════
    // Chat Data Seeding
    // ═══════════════════════════════════════════════════════════════
    private void seedChatData() throws Exception {
        logger.info("Seeding chat history...");

        try (Connection conn = dbManager.getConnection()) {
            // Create chat_history table
            try (var stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS chat_history (
                        id TEXT PRIMARY KEY,
                        session_id TEXT,
                        user_id TEXT,
                        query TEXT NOT NULL,
                        query_type TEXT,
                        response TEXT NOT NULL,
                        confidence REAL,
                        sources TEXT,
                        processing_time_ms INTEGER,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """);
            }

            String chatSql = "INSERT OR IGNORE INTO chat_history (id, session_id, query, response, query_type, confidence, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";

            String[][] chats = {
                { "What are the key requirements of DPDP Act 2023?", "The DPDP Act 2023 establishes a comprehensive data protection framework...", "EXPLANATION", "0.95" },
                { "How do I report a data breach?", "Under Section 8(6), you must notify the Data Protection Board within 72 hours...", "GUIDANCE", "0.93" },
                { "What penalties can be imposed?", "Penalties range from ₹50 crore to ₹250 crore depending on the violation...", "EXPLANATION", "0.97" },
                { "How to obtain valid consent?", "Valid consent under Section 6 must be free, specific, informed, and unambiguous...", "GUIDANCE", "0.91" },
                { "What is a DPIA and when is it required?", "A Data Protection Impact Assessment is required for Significant Data Fiduciaries...", "EXPLANATION", "0.94" },
                { "How does cross-border data transfer work?", "Cross-border transfers require government notification and adequate data protection...", "GUIDANCE", "0.89" },
                { "What rights do data principals have?", "Data principals have right to access, correction, erasure, grievance redressal, and nomination...", "EXPLANATION", "0.96" },
                { "How is children's data protected?", "Processing children's data requires verifiable parental consent. Section 9 prohibits tracking...", "GUIDANCE", "0.92" }
            };

            try (PreparedStatement stmt = conn.prepareStatement(chatSql)) {
                String sessionId = UUID.randomUUID().toString();
                for (int i = 0; i < chats.length; i++) {
                    LocalDateTime ts = LocalDateTime.now().minus(i, ChronoUnit.HOURS);
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, sessionId);
                    stmt.setString(3, chats[i][0]);
                    stmt.setString(4, chats[i][1]);
                    stmt.setString(5, chats[i][2]);
                    stmt.setDouble(6, Double.parseDouble(chats[i][3]));
                    stmt.setString(7, ts.toString());
                    stmt.executeUpdate();
                }
            }
        }
        logger.info("Seeded 30 chat history entries");
    }

    // ═══════════════════════════════════════════════════════════════
    // SIEM Sub-Tables Seeding (Threat Intel, MITRE, UEBA, Forensics)
    // ═══════════════════════════════════════════════════════════════
    private void seedSiemSubTables() throws Exception {
        logger.info("Seeding SIEM sub-tables...");
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            // Threat Intel
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS threat_intel (
                    id TEXT PRIMARY KEY, indicator TEXT, indicator_type TEXT, threat_type TEXT,
                    confidence INTEGER, source TEXT, severity TEXT, first_seen TIMESTAMP,
                    last_seen TIMESTAMP, status TEXT, tags TEXT
                )
            """);
            // MITRE ATT&CK
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS mitre_mappings (
                    id TEXT PRIMARY KEY, technique_id TEXT, technique_name TEXT, tactic TEXT,
                    description TEXT, detection_count INTEGER, severity TEXT, platforms TEXT, last_detected TIMESTAMP
                )
            """);
            // UEBA
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ueba_anomalies (
                    id TEXT PRIMARY KEY, user_id TEXT, anomaly_type TEXT, risk_score INTEGER,
                    description TEXT, baseline TEXT, actual TEXT, severity TEXT, detected_at TIMESTAMP, status TEXT
                )
            """);
            // Forensics
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS forensic_events (
                    id TEXT PRIMARY KEY, case_id TEXT, event_type TEXT, source TEXT,
                    description TEXT, evidence_hash TEXT, collected_by TEXT, severity TEXT,
                    timestamp TIMESTAMP, status TEXT
                )
            """);

            // Seed Threat Intel
            String[] indicators = {"185.220.101.42","45.33.32.156","evil-domain.xyz","d4e0f5c2a1.onion","malware-c2.net",
                "phishing-kit.ru","192.168.99.100","ransomware-drop.com","data-exfil.cc","crypto-mine.io"};
            String[] indTypes = {"IP","IP","DOMAIN","URL","DOMAIN","DOMAIN","IP","DOMAIN","DOMAIN","DOMAIN"};
            String[] threatTypes = {"C2_SERVER","SCANNER","PHISHING","TOR_EXIT","C2_SERVER","PHISHING","BOTNET","RANSOMWARE","EXFILTRATION","CRYPTOMINER"};
            String[] sources = {"AlienVault OTX","MISP","VirusTotal","ThreatFox","AbuseIPDB","CERT-In","Custom Feed"};
            for (int i = 0; i < indicators.length; i++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO threat_intel VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, indicators[i]);
                    ps.setString(3, indTypes[i]);
                    ps.setString(4, threatTypes[i]);
                    ps.setInt(5, 60 + random.nextInt(40));
                    ps.setString(6, sources[random.nextInt(sources.length)]);
                    ps.setString(7, new String[]{"LOW","MEDIUM","HIGH","CRITICAL"}[random.nextInt(4)]);
                    ps.setString(8, LocalDateTime.now().minus(random.nextInt(90), ChronoUnit.DAYS).toString());
                    ps.setString(9, LocalDateTime.now().minus(random.nextInt(7), ChronoUnit.DAYS).toString());
                    ps.setString(10, new String[]{"ACTIVE","ACTIVE","EXPIRED","WHITELISTED"}[random.nextInt(4)]);
                    ps.setString(11, threatTypes[i]);
                    ps.executeUpdate();
                }
            }

            // Seed MITRE ATT&CK
            String[][] mitre = {
                {"T1566.001","Spearphishing Attachment","Initial Access","Email-based phishing with malicious attachments"},
                {"T1078","Valid Accounts","Persistence","Adversary obtains and uses legitimate credentials"},
                {"T1059","Command and Scripting Interpreter","Execution","Scripts to execute commands on victim systems"},
                {"T1090","Proxy","Command and Control","Using proxy to direct network traffic through intermediary"},
                {"T1486","Data Encrypted for Impact","Impact","Ransomware encrypting files for extortion"},
                {"T1048","Exfiltration Over Alternative Protocol","Exfiltration","Using DNS/ICMP/other protocols for data theft"},
                {"T1110","Brute Force","Credential Access","Trying many passwords or keys to gain access"},
                {"T1053","Scheduled Task/Job","Execution","Scheduling tasks/jobs to execute malicious code"},
                {"T1071","Application Layer Protocol","Command and Control","Using HTTP/S, DNS for C2 communication"},
                {"T1027","Obfuscated Files or Information","Defense Evasion","Encoding/encrypting payloads to evade detection"}
            };
            for (String[] m : mitre) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO mitre_mappings VALUES (?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, m[0]); ps.setString(3, m[1]); ps.setString(4, m[2]); ps.setString(5, m[3]);
                    ps.setInt(6, random.nextInt(50));
                    ps.setString(7, new String[]{"LOW","MEDIUM","HIGH","CRITICAL"}[random.nextInt(4)]);
                    ps.setString(8, "Windows, Linux, macOS");
                    ps.setString(9, LocalDateTime.now().minus(random.nextInt(30), ChronoUnit.DAYS).toString());
                    ps.executeUpdate();
                }
            }

            // Seed UEBA Anomalies
            String[][] uebaData = {
                {"admin.patel","EXCESSIVE_ACCESS","Accessed 500+ records in 5 minutes vs baseline of 20"},
                {"ravi.kumar","OFF_HOURS_ACCESS","Logged in at 2:30 AM IST — never accessed after 8 PM"},
                {"priya.sharma","GEO_ANOMALY","Login from Mumbai then Delhi within 30 minutes"},
                {"vijay.singh","PRIVILEGE_ESCALATION","Attempted admin panel access without admin role"},
                {"neha.gupta","DATA_HOARDING","Downloaded 2GB of PII data, 100x normal volume"},
                {"amit.patel","UNUSUAL_ENDPOINT","Accessed from unregistered device ID"},
                {"sanjay.joshi","BULK_EXPORT","Exported all customer records in CSV format"},
                {"deepa.nair","FAILED_AUTH_SPIKE","15 failed login attempts in 2 minutes"}
            };
            for (String[] u : uebaData) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO ueba_anomalies VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, u[0]); ps.setString(3, u[1]);
                    ps.setInt(4, 50 + random.nextInt(50));
                    ps.setString(5, u[2]);
                    ps.setString(6, "Normal pattern"); ps.setString(7, "Anomalous pattern");
                    ps.setString(8, new String[]{"MEDIUM","HIGH","CRITICAL"}[random.nextInt(3)]);
                    ps.setString(9, LocalDateTime.now().minus(random.nextInt(14), ChronoUnit.DAYS).toString());
                    ps.setString(10, new String[]{"OPEN","INVESTIGATING","RESOLVED"}[random.nextInt(3)]);
                    ps.executeUpdate();
                }
            }

            // Seed Forensic Events
            String[][] forensicData = {
                {"CASE-2025-001","FILE_ACCESS","Endpoint Agent","Suspicious file access to customer PII database"},
                {"CASE-2025-001","NETWORK_CAPTURE","Network TAP","Outbound data transfer to unknown external IP"},
                {"CASE-2025-002","MEMORY_DUMP","Endpoint Agent","Process injection detected in memory dump analysis"},
                {"CASE-2025-002","DISK_IMAGE","Forensic Workstation","Recovered deleted files from user workstation"},
                {"CASE-2025-003","EMAIL_HEADER","Email Gateway","Phishing email header analysis showing spoofed sender"},
                {"CASE-2025-003","LOG_ANALYSIS","SIEM Collector","Correlated authentication logs showing lateral movement"},
                {"CASE-2025-004","MALWARE_SAMPLE","Sandbox","Static analysis of ransomware payload"},
                {"CASE-2025-004","REGISTRY_ARTIFACT","Endpoint Agent","Registry keys modified for persistence"}
            };
            for (String[] f : forensicData) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO forensic_events VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, f[0]); ps.setString(3, f[1]); ps.setString(4, f[2]); ps.setString(5, f[3]);
                    ps.setString(6, UUID.randomUUID().toString().replace("-","").substring(0,16));
                    ps.setString(7, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)].toLowerCase());
                    ps.setString(8, new String[]{"MEDIUM","HIGH","CRITICAL"}[random.nextInt(3)]);
                    ps.setString(9, LocalDateTime.now().minus(random.nextInt(14), ChronoUnit.DAYS).toString());
                    ps.setString(10, new String[]{"COLLECTED","ANALYZING","PRESERVED","COMPLETE"}[random.nextInt(4)]);
                    ps.executeUpdate();
                }
            }
        }
        logger.info("Seeded SIEM sub-tables: 10 threat_intel, 10 MITRE, 8 UEBA, 8 forensics");
    }

    // ═══════════════════════════════════════════════════════════════
    // DLP Sub-Tables Seeding (Classification, Lineage, Scans)
    // ═══════════════════════════════════════════════════════════════
    private void seedDlpSubTables() throws Exception {
        logger.info("Seeding DLP sub-tables...");
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS data_classification (
                    id TEXT PRIMARY KEY, data_source TEXT, field_name TEXT, classification TEXT,
                    sensitivity TEXT, pii_type TEXT, record_count INTEGER, confidence REAL,
                    classified_at TIMESTAMP, status TEXT
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS data_lineage (
                    id TEXT PRIMARY KEY, source_system TEXT, destination_system TEXT, data_type TEXT,
                    transfer_method TEXT, records_transferred INTEGER, consent_verified TEXT,
                    purpose TEXT, initiated_by TEXT, timestamp TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS discovery_scans (
                    id TEXT PRIMARY KEY, scan_name TEXT, target TEXT, scan_type TEXT,
                    status TEXT, pii_found INTEGER, phi_found INTEGER, pfi_found INTEGER,
                    total_files INTEGER, started_at TIMESTAMP, completed_at TIMESTAMP
                )
            """);

            // Seed Data Classification
            String[][] classData = {
                {"customer_db","aadhaar_number","PII","CRITICAL","AADHAAR"},
                {"customer_db","pan_number","PII","HIGH","PAN"},
                {"customer_db","email","PII","MEDIUM","EMAIL"},
                {"customer_db","phone","PII","MEDIUM","PHONE"},
                {"customer_db","name","PII","LOW","NAME"},
                {"medical_records","diagnosis","PHI","CRITICAL","HEALTH_DATA"},
                {"medical_records","prescription","PHI","HIGH","HEALTH_DATA"},
                {"financial_db","account_number","PFI","CRITICAL","FINANCIAL"},
                {"financial_db","credit_score","PFI","HIGH","FINANCIAL"},
                {"hr_system","salary","PII","HIGH","FINANCIAL"},
                {"hr_system","dob","PII","MEDIUM","DOB"},
                {"crm_system","address","PII","MEDIUM","ADDRESS"},
                {"kyc_store","passport_number","PII","CRITICAL","IDENTITY"},
                {"kyc_store","voter_id","PII","HIGH","IDENTITY"},
                {"log_store","ip_address","PII","LOW","NETWORK"}
            };
            for (String[] c : classData) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO data_classification VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, c[0]); ps.setString(3, c[1]); ps.setString(4, c[2]);
                    ps.setString(5, c[3]); ps.setString(6, c[4]);
                    ps.setInt(7, 100 + random.nextInt(50000));
                    ps.setDouble(8, 0.85 + random.nextDouble() * 0.15);
                    ps.setString(9, LocalDateTime.now().minus(random.nextInt(30), ChronoUnit.DAYS).toString());
                    ps.setString(10, "CLASSIFIED");
                    ps.executeUpdate();
                }
            }

            // Seed Data Lineage
            String[][] lineageData = {
                {"Customer DB","Analytics DW","PII","ETL","Analytics"},
                {"KYC Store","Compliance Report","PII","Export","Regulatory"},
                {"CRM System","Marketing Platform","PII","API","Marketing"},
                {"HR System","Payroll Provider","PII","SFTP","Payroll"},
                {"Medical Records","Insurance Claim","PHI","API","Insurance"},
                {"Financial DB","Credit Bureau","PFI","API","Credit Check"},
                {"Customer DB","Backup Server","PII","Replication","DR"},
                {"Application Logs","SIEM","PII","Syslog","Security"}
            };
            for (String[] l : lineageData) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO data_lineage VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, l[0]); ps.setString(3, l[1]); ps.setString(4, l[2]);
                    ps.setString(5, l[3]);
                    ps.setInt(6, 500 + random.nextInt(10000));
                    ps.setString(7, random.nextBoolean() ? "YES" : "PENDING");
                    ps.setString(8, l[4]);
                    ps.setString(9, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)].toLowerCase());
                    ps.setString(10, LocalDateTime.now().minus(random.nextInt(14), ChronoUnit.DAYS).toString());
                    ps.executeUpdate();
                }
            }

            // Seed Discovery Scans
            String[][] scanData = {
                {"Full PII Scan - Q1","customer_db","FULL_SCAN"},
                {"Endpoint DLP Scan","endpoint_shares","ENDPOINT"},
                {"Email Attachment Scan","email_attachments","EMAIL"},
                {"Cloud Storage Scan","s3_buckets","CLOUD"},
                {"Database Schema Scan","all_databases","SCHEMA"},
                {"File Server Scan","shared_drives","FILE_SYSTEM"}
            };
            for (String[] s : scanData) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO discovery_scans VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, s[0]); ps.setString(3, s[1]); ps.setString(4, s[2]);
                    ps.setString(5, new String[]{"COMPLETED","COMPLETED","IN_PROGRESS","SCHEDULED"}[random.nextInt(4)]);
                    ps.setInt(6, random.nextInt(2000)); ps.setInt(7, random.nextInt(500));
                    ps.setInt(8, random.nextInt(800)); ps.setInt(9, 1000 + random.nextInt(50000));
                    ps.setString(10, LocalDateTime.now().minus(random.nextInt(30), ChronoUnit.DAYS).toString());
                    ps.setString(11, LocalDateTime.now().minus(random.nextInt(7), ChronoUnit.DAYS).toString());
                    ps.executeUpdate();
                }
            }
        }
        logger.info("Seeded DLP sub-tables: 15 classifications, 8 lineage, 6 scans");
    }

    // ═══════════════════════════════════════════════════════════════
    // Payment Gateway Seeding
    // ═══════════════════════════════════════════════════════════════
    private void seedPaymentData() throws Exception {
        logger.info("Seeding payment gateway data...");
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_gateways (
                    id TEXT PRIMARY KEY, name TEXT, provider TEXT, status TEXT,
                    api_endpoint TEXT, environment TEXT, configured_at TIMESTAMP, last_tested TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_transactions (
                    id TEXT PRIMARY KEY, gateway TEXT, transaction_type TEXT, amount REAL,
                    currency TEXT, status TEXT, reference_id TEXT, customer_id TEXT,
                    description TEXT, created_at TIMESTAMP
                )
            """);

            // Seed Gateways
            String[][] gateways = {
                {"Razorpay","Razorpay Software Pvt Ltd","https://api.razorpay.com/v1"},
                {"PayU","PayU Payments Pvt Ltd","https://secure.payu.in/_payment"}
            };
            for (String[] g : gateways) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO payment_gateways VALUES (?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, g[0]); ps.setString(3, g[1]); ps.setString(4, "ACTIVE");
                    ps.setString(5, g[2]); ps.setString(6, "SANDBOX");
                    ps.setString(7, LocalDateTime.now().minus(30, ChronoUnit.DAYS).toString());
                    ps.setString(8, LocalDateTime.now().minus(1, ChronoUnit.DAYS).toString());
                    ps.executeUpdate();
                }
            }

            // Seed Transactions
            String[] txnTypes = {"LICENSE_PURCHASE","SUBSCRIPTION_RENEWAL","ADD_ON_PURCHASE","UPGRADE"};
            String[] txnStatuses = {"SUCCESS","SUCCESS","SUCCESS","PENDING","FAILED"};
            for (int i = 0; i < 20; i++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO payment_transactions VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, gateways[random.nextInt(gateways.length)][0]);
                    ps.setString(3, txnTypes[random.nextInt(txnTypes.length)]);
                    ps.setDouble(4, (10 + random.nextInt(90)) * 1000.0);
                    ps.setString(5, "INR");
                    ps.setString(6, txnStatuses[random.nextInt(txnStatuses.length)]);
                    ps.setString(7, "TXN-" + String.format("%06d", 100000 + i));
                    ps.setString(8, "CUST-" + String.format("%04d", 1000 + i));
                    ps.setString(9, txnTypes[random.nextInt(txnTypes.length)] + " for QS-DPDP");
                    ps.setString(10, LocalDateTime.now().minus(random.nextInt(90), ChronoUnit.DAYS).toString());
                    ps.executeUpdate();
                }
            }
        }
        logger.info("Seeded payment data: 2 gateways, 20 transactions");
    }

    // ═══════════════════════════════════════════════════════════════
    // Licensing Agreements Seeding
    // ═══════════════════════════════════════════════════════════════
    private void seedLicensingAgreements() throws Exception {
        logger.info("Seeding licensing agreements...");
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS licensing_agreements (
                    id TEXT PRIMARY KEY, agreement_type TEXT, customer TEXT, tier TEXT,
                    start_date TEXT, end_date TEXT, value REAL, status TEXT,
                    auto_renew TEXT, signed_by TEXT
                )
            """);

            String[][] agreements = {
                {"Enterprise License","Axis Bank","ENTERPRISE","7500000"},
                {"Enterprise License","SBI","ENTERPRISE","9000000"},
                {"Professional License","HDFC Securities","PROFESSIONAL","2400000"},
                {"Professional License","Kotak Mahindra","PROFESSIONAL","2400000"},
                {"Starter License","Paytm Payments Bank","STARTER","600000"},
                {"Enterprise License","ICICI Bank","ENTERPRISE","8500000"},
                {"Professional License","Bajaj Finance","PROFESSIONAL","1800000"},
                {"Starter License","PhonePe","STARTER","480000"}
            };
            for (String[] a : agreements) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO licensing_agreements VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, a[0]); ps.setString(3, a[1]); ps.setString(4, a[2]);
                    ps.setString(5, "2025-01-01"); ps.setString(6, "2026-12-31");
                    ps.setDouble(7, Double.parseDouble(a[3]));
                    ps.setString(8, new String[]{"ACTIVE","ACTIVE","ACTIVE","PENDING_RENEWAL"}[random.nextInt(4)]);
                    ps.setString(9, random.nextBoolean() ? "YES" : "NO");
                    ps.setString(10, FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[random.nextInt(LAST_NAMES.length)]);
                    ps.executeUpdate();
                }
            }
        }
        logger.info("Seeded 8 licensing agreements");
    }

    // ═══════════════════════════════════════════════════════════════
    // Report Executions Seeding
    // ═══════════════════════════════════════════════════════════════
    private void seedReportExecutions() throws Exception {
        logger.info("Seeding report execution history...");
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS report_executions (
                    id TEXT PRIMARY KEY, report_type TEXT, format TEXT, status TEXT,
                    generated_by TEXT, row_count INTEGER, file_size TEXT,
                    created_at TIMESTAMP, completed_at TIMESTAMP
                )
            """);

            String[] types = {"compliance-summary","consent-report","breach-report","gap-report",
                "dpia-report","rights-report","audit-trail","policy-report","siem-report","executive-dashboard"};
            String[] formats = {"PDF","EXCEL","CSV","WORD","PDF"};
            for (int i = 0; i < 15; i++) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO report_executions VALUES (?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, types[random.nextInt(types.length)]);
                    ps.setString(3, formats[random.nextInt(formats.length)]);
                    ps.setString(4, "COMPLETED");
                    ps.setString(5, "admin");
                    ps.setInt(6, 50 + random.nextInt(500));
                    ps.setString(7, (50 + random.nextInt(950)) + " KB");
                    ps.setString(8, LocalDateTime.now().minus(random.nextInt(30), ChronoUnit.DAYS).toString());
                    ps.setString(9, LocalDateTime.now().minus(random.nextInt(30), ChronoUnit.DAYS).toString());
                    ps.executeUpdate();
                }
            }
        }
        logger.info("Seeded 15 report executions");
    }
}
