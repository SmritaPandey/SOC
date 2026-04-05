package com.qsdpdp.training;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Training & Awareness Service
 * Manages DPDP compliance training and employee awareness programs
 * 
 * @version 1.0.0
 * @since Module 17
 */
@Service
public class TrainingService {

    private static final Logger logger = LoggerFactory.getLogger(TrainingService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;

    private boolean initialized = false;
    private final List<TrainingModule> defaultModules = new ArrayList<>();

    @Autowired
    public TrainingService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing Training Service...");
        createTables();
        initializeDefaultModules();

        initialized = true;
        logger.info("Training Service initialized with {} default modules", defaultModules.size());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS training_modules (
                            id TEXT PRIMARY KEY,
                            title TEXT NOT NULL,
                            description TEXT,
                            category TEXT,
                            dpdp_section TEXT,
                            target_audience TEXT,
                            duration_minutes INTEGER,
                            passing_score INTEGER DEFAULT 70,
                            mandatory INTEGER DEFAULT 0,
                            refresh_period_days INTEGER DEFAULT 365,
                            content_type TEXT,
                            content_url TEXT,
                            active INTEGER DEFAULT 1,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS training_enrollments (
                            id TEXT PRIMARY KEY,
                            user_id TEXT NOT NULL,
                            module_id TEXT NOT NULL,
                            status TEXT DEFAULT 'ENROLLED',
                            enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            started_at TIMESTAMP,
                            completed_at TIMESTAMP,
                            score INTEGER,
                            attempts INTEGER DEFAULT 0,
                            certificate_id TEXT,
                            expires_at TIMESTAMP,
                            FOREIGN KEY (module_id) REFERENCES training_modules(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS training_quiz_questions (
                            id TEXT PRIMARY KEY,
                            module_id TEXT NOT NULL,
                            question_text TEXT NOT NULL,
                            question_type TEXT DEFAULT 'MCQ',
                            options TEXT,
                            correct_answer TEXT,
                            explanation TEXT,
                            points INTEGER DEFAULT 1,
                            order_index INTEGER,
                            FOREIGN KEY (module_id) REFERENCES training_modules(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS training_campaigns (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            description TEXT,
                            modules TEXT,
                            target_departments TEXT,
                            start_date TIMESTAMP,
                            end_date TIMESTAMP,
                            reminder_frequency TEXT,
                            status TEXT DEFAULT 'DRAFT',
                            created_by TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_training_enroll_user ON training_enrollments(user_id)");
            stmt.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_training_enroll_module ON training_enrollments(module_id)");

            logger.info("Training tables created");

        } catch (SQLException e) {
            logger.error("Failed to create Training tables", e);
        }
    }

    private void initializeDefaultModules() {
        defaultModules.add(new TrainingModule(
                "DPDP Act Overview",
                "Comprehensive introduction to India's Digital Personal Data Protection Act, 2023",
                "FOUNDATIONAL", "All Sections", "ALL", 60, true));

        defaultModules.add(new TrainingModule(
                "Data Principal Rights",
                "Understanding and handling data principal rights requests",
                "RIGHTS", "Sections 11-14", "OPERATIONS", 45, true));

        defaultModules.add(new TrainingModule(
                "Consent Management Best Practices",
                "How to collect, manage, and withdraw consent properly",
                "CONSENT", "Section 6", "OPERATIONS", 30, true));

        defaultModules.add(new TrainingModule(
                "Data Breach Response",
                "Procedures for identifying, reporting, and responding to data breaches",
                "SECURITY", "Section 8(6)", "SECURITY", 45, true));

        defaultModules.add(new TrainingModule(
                "Data Protection Impact Assessments",
                "When and how to conduct DPIAs for high-risk processing",
                "DPIA", "Section 10", "PRIVACY_TEAM", 60, true));

        defaultModules.add(new TrainingModule(
                "Children's Data Protection",
                "Special safeguards for processing children's personal data",
                "SPECIAL_CATEGORIES", "Section 9", "ALL", 30, true));

        defaultModules.add(new TrainingModule(
                "Cross-Border Data Transfers",
                "Rules and restrictions for international data transfers",
                "DATA_TRANSFER", "Section 16", "LEGAL", 45, false));

        defaultModules.add(new TrainingModule(
                "DPO Roles and Responsibilities",
                "Deep dive into Data Protection Officer duties",
                "GOVERNANCE", "Section 8", "DPO", 90, true));

        defaultModules.add(new TrainingModule(
                "Personal Data Security",
                "Technical and organizational measures for data protection",
                "SECURITY", "Section 8(4)", "IT", 60, true));

        defaultModules.add(new TrainingModule(
                "Data Retention and Deletion",
                "Managing data lifecycle and erasure obligations",
                "DATA_LIFECYCLE", "Section 8(7)", "OPERATIONS", 30, true));

        // Add quiz questions for each module
        for (TrainingModule module : defaultModules) {
            addDefaultQuizQuestions(module);
        }

        // Persist default modules to the database
        persistDefaultModules();
    }

    private void persistDefaultModules() {
        String sql = """
                    INSERT OR IGNORE INTO training_modules (id, title, description, category, dpdp_section,
                        target_audience, duration_minutes, passing_score, mandatory, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 70, ?, 1)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (TrainingModule module : defaultModules) {
                stmt.setString(1, module.getId());
                stmt.setString(2, module.getTitle());
                stmt.setString(3, module.getDescription());
                stmt.setString(4, module.getCategory());
                stmt.setString(5, module.getDpdpSection());
                stmt.setString(6, module.getTargetAudience());
                stmt.setInt(7, module.getDurationMinutes());
                stmt.setInt(8, module.isMandatory() ? 1 : 0);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Failed to persist default training modules", e);
        }
    }

    private void addDefaultQuizQuestions(TrainingModule module) {
        List<QuizQuestion> questions = new ArrayList<>();

        switch (module.getCategory()) {
            case "FOUNDATIONAL" -> {
                questions.add(new QuizQuestion(
                        "What is the main objective of the DPDP Act, 2023?",
                        "MCQ",
                        List.of("Tax collection", "Protect digital personal data", "Promote e-commerce",
                                "Border security"),
                        "Protect digital personal data",
                        "The DPDP Act 2023 primarily aims to protect the digital personal data of individuals"));
                questions.add(new QuizQuestion(
                        "Who is a 'Data Fiduciary' under the DPDP Act?",
                        "MCQ",
                        List.of("Data Principal", "Person who determines purpose and means of processing",
                                "Government regulator", "IT vendor"),
                        "Person who determines purpose and means of processing",
                        "Data Fiduciary determines the purpose and means of processing personal data"));
            }
            case "CONSENT" -> {
                questions.add(new QuizQuestion(
                        "Which of these is NOT a requirement for valid consent under DPDP?",
                        "MCQ",
                        List.of("Free", "Informed", "In writing on paper", "Specific"),
                        "In writing on paper",
                        "Consent can be digital - it does not need to be on paper"));
                questions.add(new QuizQuestion(
                        "Can consent for processing children's data be given by the child?",
                        "MCQ",
                        List.of("Yes, always", "No, only by parent/guardian", "Yes, if over 16", "Only for education"),
                        "No, only by parent/guardian",
                        "For children, verifiable consent of parent/guardian is required"));
            }
            case "SECURITY" -> {
                questions.add(new QuizQuestion(
                        "Within what timeframe must a significant breach be reported to DPBI?",
                        "MCQ",
                        List.of("24 hours", "72 hours", "7 days", "1 month"),
                        "72 hours",
                        "Significant data breaches must be reported to DPBI within 72 hours"));
            }
        }

        module.setQuizQuestions(questions);
    }

    // ═══════════════════════════════════════════════════════════
    // ENROLLMENT MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public String enrollUser(String userId, String moduleId) {
        String enrollmentId = UUID.randomUUID().toString();

        String sql = """
                    INSERT INTO training_enrollments (id, user_id, module_id, status, expires_at)
                    VALUES (?, ?, ?, 'ENROLLED', ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, enrollmentId);
            stmt.setString(2, userId);
            stmt.setString(3, moduleId);
            stmt.setString(4, LocalDateTime.now().plusDays(365).toString());
            stmt.executeUpdate();

            auditService.log("TRAINING_ENROLLED", "TRAINING", userId, "Enrolled in module: " + moduleId);
            return enrollmentId;

        } catch (SQLException e) {
            logger.error("Failed to enroll user", e);
            return null;
        }
    }

    public void startModule(String enrollmentId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE training_enrollments SET status = 'IN_PROGRESS', started_at = ? WHERE id = ?")) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, enrollmentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to start module", e);
        }
    }

    public void completeModule(String enrollmentId, int score) {
        String sql = """
                    UPDATE training_enrollments SET status = ?, score = ?, completed_at = ?,
                        attempts = attempts + 1, certificate_id = ?
                    WHERE id = ?
                """;

        // Check passing score
        int passingScore = getPassingScore(enrollmentId);
        String status = score >= passingScore ? "COMPLETED" : "FAILED";
        String certificateId = status.equals("COMPLETED") ? generateCertificate(enrollmentId) : null;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, score);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.setString(4, certificateId);
            stmt.setString(5, enrollmentId);
            stmt.executeUpdate();

            auditService.log("TRAINING_COMPLETED", "TRAINING", null,
                    "Completed module: " + enrollmentId + ", Score: " + score + "%, Status: " + status);

        } catch (SQLException e) {
            logger.error("Failed to complete module", e);
        }
    }

    private int getPassingScore(String enrollmentId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                        SELECT m.passing_score FROM training_modules m
                        JOIN training_enrollments e ON e.module_id = m.id
                        WHERE e.id = ?
                        """)) {
            stmt.setString(1, enrollmentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Failed to get passing score", e);
        }
        return 70; // Default
    }

    private String generateCertificate(String enrollmentId) {
        return "CERT-" + System.currentTimeMillis() + "-" + enrollmentId.substring(0, 8).toUpperCase();
    }

    // ═══════════════════════════════════════════════════════════
    // CAMPAIGN MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public String createCampaign(String name, String description, List<String> moduleIds,
            List<String> departments, LocalDateTime startDate,
            LocalDateTime endDate, String createdBy) {
        String campaignId = UUID.randomUUID().toString();

        String sql = """
                    INSERT INTO training_campaigns (id, name, description, modules, target_departments,
                        start_date, end_date, status, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, campaignId);
            stmt.setString(2, name);
            stmt.setString(3, description);
            stmt.setString(4, String.join(",", moduleIds));
            stmt.setString(5, String.join(",", departments));
            stmt.setString(6, startDate.toString());
            stmt.setString(7, endDate.toString());
            stmt.setString(8, createdBy);
            stmt.executeUpdate();

            auditService.log("TRAINING_CAMPAIGN_CREATED", "TRAINING", createdBy,
                    "Created campaign: " + name);

            return campaignId;

        } catch (SQLException e) {
            logger.error("Failed to create campaign", e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REPORTING
    // ═══════════════════════════════════════════════════════════

    public TrainingStats getStatistics() {
        TrainingStats stats = new TrainingStats();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT user_id) FROM training_enrollments");
            if (rs.next())
                stats.setTotalEnrollees(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM training_enrollments WHERE status = 'COMPLETED'");
            if (rs.next())
                stats.setCompletions(rs.getInt(1));

            rs = stmt.executeQuery("SELECT AVG(score) FROM training_enrollments WHERE status = 'COMPLETED'");
            if (rs.next())
                stats.setAverageScore(rs.getDouble(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM training_enrollments WHERE status = 'IN_PROGRESS'");
            if (rs.next())
                stats.setInProgress(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM training_enrollments WHERE expires_at <= datetime('now', '+30 days')");
            if (rs.next())
                stats.setExpiringCertificates(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get training statistics", e);
        }

        return stats;
    }

    public List<EnrollmentStatus> getUserEnrollments(String userId) {
        List<EnrollmentStatus> enrollments = new ArrayList<>();

        String sql = """
                    SELECT e.*, m.title, m.category FROM training_enrollments e
                    JOIN training_modules m ON e.module_id = m.id
                    WHERE e.user_id = ?
                    ORDER BY e.enrolled_at DESC
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                EnrollmentStatus status = new EnrollmentStatus();
                status.setEnrollmentId(rs.getString("id"));
                status.setModuleTitle(rs.getString("title"));
                status.setCategory(rs.getString("category"));
                status.setStatus(rs.getString("status"));
                status.setScore(rs.getInt("score"));
                status.setCertificateId(rs.getString("certificate_id"));
                enrollments.add(status);
            }
        } catch (SQLException e) {
            logger.error("Failed to get user enrollments", e);
        }

        return enrollments;
    }

    public List<TrainingModule> getDefaultModules() {
        return defaultModules;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class TrainingModule {
        private String id = UUID.randomUUID().toString();
        private String title;
        private String description;
        private String category;
        private String dpdpSection;
        private String targetAudience;
        private int durationMinutes;
        private boolean mandatory;
        private List<QuizQuestion> quizQuestions = new ArrayList<>();

        public TrainingModule() {
        }

        public TrainingModule(String title, String description, String category,
                String dpdpSection, String targetAudience, int duration, boolean mandatory) {
            this.title = title;
            this.description = description;
            this.category = category;
            this.dpdpSection = dpdpSection;
            this.targetAudience = targetAudience;
            this.durationMinutes = duration;
            this.mandatory = mandatory;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getCategory() {
            return category;
        }

        public String getDpdpSection() {
            return dpdpSection;
        }

        public String getTargetAudience() {
            return targetAudience;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public boolean isMandatory() {
            return mandatory;
        }

        public List<QuizQuestion> getQuizQuestions() {
            return quizQuestions;
        }

        public void setQuizQuestions(List<QuizQuestion> questions) {
            this.quizQuestions = questions;
        }
    }

    public static class QuizQuestion {
        private String id = UUID.randomUUID().toString();
        private String questionText;
        private String questionType;
        private List<String> options;
        private String correctAnswer;
        private String explanation;

        public QuizQuestion() {
        }

        public QuizQuestion(String questionText, String questionType, List<String> options,
                String correctAnswer, String explanation) {
            this.questionText = questionText;
            this.questionType = questionType;
            this.options = options;
            this.correctAnswer = correctAnswer;
            this.explanation = explanation;
        }

        public String getId() {
            return id;
        }

        public String getQuestionText() {
            return questionText;
        }

        public String getQuestionType() {
            return questionType;
        }

        public List<String> getOptions() {
            return options;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public String getExplanation() {
            return explanation;
        }
    }

    public static class TrainingStats {
        private int totalEnrollees;
        private int completions;
        private double averageScore;
        private int inProgress;
        private int expiringCertificates;

        public int getTotalEnrollees() {
            return totalEnrollees;
        }

        public void setTotalEnrollees(int v) {
            this.totalEnrollees = v;
        }

        public int getCompletions() {
            return completions;
        }

        public void setCompletions(int v) {
            this.completions = v;
        }

        public double getAverageScore() {
            return averageScore;
        }

        public void setAverageScore(double v) {
            this.averageScore = v;
        }

        public int getInProgress() {
            return inProgress;
        }

        public void setInProgress(int v) {
            this.inProgress = v;
        }

        public int getExpiringCertificates() {
            return expiringCertificates;
        }

        public void setExpiringCertificates(int v) {
            this.expiringCertificates = v;
        }
    }

    public static class EnrollmentStatus {
        private String enrollmentId;
        private String moduleTitle;
        private String category;
        private String status;
        private int score;
        private String certificateId;

        public String getEnrollmentId() {
            return enrollmentId;
        }

        public void setEnrollmentId(String id) {
            this.enrollmentId = id;
        }

        public String getModuleTitle() {
            return moduleTitle;
        }

        public void setModuleTitle(String title) {
            this.moduleTitle = title;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String cat) {
            this.category = cat;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public String getCertificateId() {
            return certificateId;
        }

        public void setCertificateId(String id) {
            this.certificateId = id;
        }
    }
}
