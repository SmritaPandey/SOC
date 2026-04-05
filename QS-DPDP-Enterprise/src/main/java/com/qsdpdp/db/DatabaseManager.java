package com.qsdpdp.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Database Manager for QS-DPDP Enterprise
 * Handles SQLite database creation, initialization, and connection management
 * Supports multi-database configuration (SQLite, Oracle, PostgreSQL, MS SQL)
 * 
 * @version 1.0.0
 * @since Phase 1
 */
@Component
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private String dbPath;
    private String connectionUrl;
    private boolean initialized = false;
    private final ReentrantLock lock = new ReentrantLock();

    // Database configuration
    private static final String DB_NAME = "qsdpdp_enterprise.db";
    private static final int SCHEMA_VERSION = 1;

    public void initialize() {
        lock.lock();
        try {
            if (initialized) {
                logger.debug("Database already initialized");
                return;
            }

            logger.info("Initializing database...");

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Determine database path
            dbPath = getDataDirectory() + File.separator + DB_NAME;
            connectionUrl = "jdbc:sqlite:" + dbPath;

            // Create database file if not exists
            createDatabaseIfNotExists();

            // Initialize schema
            initializeSchema();

            initialized = true;
            logger.info("Database initialized at: {}", dbPath);

        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        } finally {
            lock.unlock();
        }
    }

    private String getDataDirectory() {
        String dataDir = System.getenv("QSDPDP_DATA_DIR");
        if (dataDir != null && !dataDir.isEmpty()) {
            return dataDir;
        }

        String userHome = System.getProperty("user.home");
        Path appDataPath = Paths.get(userHome, ".qsdpdp-enterprise", "data");

        try {
            Files.createDirectories(appDataPath);
        } catch (IOException e) {
            logger.warn("Could not create data directory, using current directory");
            return ".";
        }

        return appDataPath.toString();
    }

    private void createDatabaseIfNotExists() throws Exception {
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            logger.info("Creating new database at: {}", dbPath);
            if (dbFile.getParentFile() != null) {
                dbFile.getParentFile().mkdirs();
            }
            dbFile.createNewFile();
        }
    }

    private void initializeSchema() throws Exception {
        logger.info("Initializing database schema v{}...", SCHEMA_VERSION);

        try (Connection conn = getDirectConnection(); Statement stmt = conn.createStatement()) {

            // Enable foreign keys and WAL mode for better performance
            stmt.executeUpdate("PRAGMA foreign_keys = ON");
            stmt.executeUpdate("PRAGMA journal_mode = WAL");

            // Schema version table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS schema_version (
                            version INTEGER PRIMARY KEY,
                            applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // CORE TABLES
            // ═══════════════════════════════════════════════════════════

            // Users table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS users (
                            id TEXT PRIMARY KEY,
                            username TEXT UNIQUE NOT NULL,
                            email TEXT UNIQUE NOT NULL,
                            password_hash TEXT NOT NULL,
                            full_name TEXT NOT NULL,
                            role TEXT NOT NULL DEFAULT 'USER',
                            department TEXT,
                            phone TEXT,
                            status TEXT NOT NULL DEFAULT 'ACTIVE',
                            mfa_enabled INTEGER DEFAULT 0,
                            mfa_secret TEXT,
                            last_login TIMESTAMP,
                            failed_login_attempts INTEGER DEFAULT 0,
                            locked_until TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            created_by TEXT,
                            updated_by TEXT
                        )
                    """);

            // Migration: add phone column if missing (for existing databases)
            try {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN phone TEXT");
            } catch (SQLException ignored) {
                // Column already exists
            };

            // Sessions table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS sessions (
                            id TEXT PRIMARY KEY,
                            user_id TEXT NOT NULL,
                            token_hash TEXT NOT NULL,
                            ip_address TEXT,
                            user_agent TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            expires_at TIMESTAMP NOT NULL,
                            revoked INTEGER DEFAULT 0,
                            FOREIGN KEY (user_id) REFERENCES users(id)
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // CONSENT MANAGEMENT TABLES
            // ═══════════════════════════════════════════════════════════

            // Purposes table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS purposes (
                            id TEXT PRIMARY KEY,
                            code TEXT UNIQUE NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT,
                            legal_basis TEXT NOT NULL,
                            data_categories TEXT,
                            retention_period_days INTEGER,
                            is_active INTEGER DEFAULT 1,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Data Principals table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS data_principals (
                            id TEXT PRIMARY KEY,
                            external_id TEXT,
                            name TEXT NOT NULL,
                            email TEXT,
                            phone TEXT,
                            is_child INTEGER DEFAULT 0,
                            guardian_id TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (guardian_id) REFERENCES data_principals(id)
                        )
                    """);

            // Consents table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS consents (
                            id TEXT PRIMARY KEY,
                            data_principal_id TEXT NOT NULL,
                            purpose_id TEXT NOT NULL,
                            status TEXT NOT NULL DEFAULT 'ACTIVE',
                            consent_method TEXT NOT NULL,
                            notice_version TEXT,
                            language TEXT DEFAULT 'en',
                            ip_address TEXT,
                            user_agent TEXT,
                            collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            expires_at TIMESTAMP,
                            withdrawn_at TIMESTAMP,
                            withdrawal_reason TEXT,
                            withdrawn_by TEXT,
                            hash TEXT NOT NULL,
                            prev_hash TEXT,
                            created_by TEXT,
                            FOREIGN KEY (data_principal_id) REFERENCES data_principals(id),
                            FOREIGN KEY (purpose_id) REFERENCES purposes(id)
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // CONSENT ENHANCEMENT TABLES (Phase 2)
            // ═══════════════════════════════════════════════════════════

            // Consent Preferences — granular per-purpose, per-data-category
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS consent_preferences (
                            id TEXT PRIMARY KEY,
                            consent_id TEXT NOT NULL,
                            data_principal_id TEXT NOT NULL,
                            purpose_id TEXT NOT NULL,
                            data_category TEXT NOT NULL,
                            allowed INTEGER DEFAULT 1,
                            processing_basis TEXT DEFAULT 'consent',
                            third_party_sharing INTEGER DEFAULT 0,
                            cross_border_transfer INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (consent_id) REFERENCES consents(id),
                            FOREIGN KEY (data_principal_id) REFERENCES data_principals(id),
                            FOREIGN KEY (purpose_id) REFERENCES purposes(id)
                        )
                    """);

            // Guardian Consents — DPDP S.9, Rule 11
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS guardian_consents (
                            id TEXT PRIMARY KEY,
                            child_principal_id TEXT NOT NULL,
                            guardian_principal_id TEXT NOT NULL,
                            child_name TEXT NOT NULL,
                            child_age INTEGER NOT NULL,
                            guardian_name TEXT NOT NULL,
                            guardian_relationship TEXT NOT NULL,
                            guardian_id_type TEXT NOT NULL,
                            guardian_id_number TEXT NOT NULL,
                            guardian_kyc_verified INTEGER DEFAULT 0,
                            guardian_kyc_date TIMESTAMP,
                            is_disability INTEGER DEFAULT 0,
                            disability_type TEXT,
                            consent_id TEXT,
                            purpose_id TEXT,
                            status TEXT DEFAULT 'pending',
                            verification_method TEXT DEFAULT 'OTP',
                            verification_date TIMESTAMP,
                            notes TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (child_principal_id) REFERENCES data_principals(id),
                            FOREIGN KEY (guardian_principal_id) REFERENCES data_principals(id),
                            FOREIGN KEY (consent_id) REFERENCES consents(id)
                        )
                    """);

            // Consent Audit Trail — hash-chained immutable ledger
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS consent_audit_trail (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            block_number INTEGER NOT NULL,
                            consent_id TEXT NOT NULL,
                            data_principal_id TEXT NOT NULL,
                            action TEXT NOT NULL,
                            action_by TEXT DEFAULT 'SYSTEM',
                            details TEXT,
                            previous_hash TEXT,
                            current_hash TEXT NOT NULL,
                            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (consent_id) REFERENCES consents(id)
                        )
                    """);

            // Sector Purpose Templates — pre-built sector-specific
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS sector_purpose_templates (
                            id TEXT PRIMARY KEY,
                            sector TEXT NOT NULL,
                            code TEXT NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT,
                            legal_basis TEXT NOT NULL,
                            data_categories TEXT,
                            retention_period TEXT,
                            mandatory INTEGER DEFAULT 0,
                            regulatory_reference TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // UNIVERSAL CONSENT MANAGER TABLES (UCM Enhancement)
            // ═══════════════════════════════════════════════════════════

            // Migration: add UCM columns to consents table
            String[] consentMigrations = {
                "ALTER TABLE consents ADD COLUMN data_fiduciary_id TEXT",
                "ALTER TABLE consents ADD COLUMN consent_type TEXT DEFAULT 'EXPLICIT'",
                "ALTER TABLE consents ADD COLUMN retention_period TEXT",
                "ALTER TABLE consents ADD COLUMN digital_signature TEXT",
                "ALTER TABLE consents ADD COLUMN notice_id TEXT",
                "ALTER TABLE consents ADD COLUMN modified_at TIMESTAMP",
                "ALTER TABLE consents ADD COLUMN modified_by TEXT",
                "ALTER TABLE consents ADD COLUMN channel TEXT DEFAULT 'web'",
                "ALTER TABLE consents ADD COLUMN data_categories TEXT"
            };
            for (String migration : consentMigrations) {
                try { stmt.executeUpdate(migration); } catch (SQLException ignored) {}
            }

            // Data Category Registry — standalone classification per DPDP
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS data_categories (
                            id TEXT PRIMARY KEY,
                            code TEXT UNIQUE NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT,
                            sensitivity_level TEXT DEFAULT 'MEDIUM',
                            dpdp_classification TEXT DEFAULT 'PERSONAL',
                            sector_applicability TEXT,
                            data_elements TEXT,
                            retention_guideline TEXT,
                            regulatory_reference TEXT,
                            is_active INTEGER DEFAULT 1,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Consent Delegations — S.9 guardian + authorized representatives
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS consent_delegations (
                            id TEXT PRIMARY KEY,
                            delegator_id TEXT NOT NULL,
                            delegator_name TEXT,
                            delegate_id TEXT NOT NULL,
                            delegate_name TEXT,
                            delegate_type TEXT NOT NULL,
                            scope TEXT DEFAULT 'ALL',
                            scope_details TEXT,
                            relationship TEXT,
                            id_proof_type TEXT,
                            id_proof_number TEXT,
                            kyc_verified INTEGER DEFAULT 0,
                            kyc_verified_at TIMESTAMP,
                            verification_method TEXT DEFAULT 'OTP',
                            valid_from TIMESTAMP,
                            valid_to TIMESTAMP,
                            status TEXT DEFAULT 'PENDING_VERIFICATION',
                            revoked_by TEXT,
                            revoked_reason TEXT,
                            revoked_at TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (delegator_id) REFERENCES data_principals(id),
                            FOREIGN KEY (delegate_id) REFERENCES data_principals(id)
                        )
                    """);

            // Legitimate Uses — S.7 processing without explicit consent
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS legitimate_uses (
                            id TEXT PRIMARY KEY,
                            data_fiduciary_id TEXT,
                            data_fiduciary_name TEXT,
                            data_principal_id TEXT NOT NULL,
                            data_principal_name TEXT,
                            lawful_basis TEXT NOT NULL,
                            purpose_description TEXT,
                            data_categories TEXT,
                            legal_reference TEXT,
                            start_date TIMESTAMP,
                            end_date TIMESTAMP,
                            retention_period TEXT,
                            status TEXT DEFAULT 'ACTIVE',
                            reviewed_by TEXT,
                            reviewed_at TIMESTAMP,
                            review_notes TEXT,
                            evidence_reference TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (data_principal_id) REFERENCES data_principals(id)
                        )
                    """);

            // Consent Notices — S.5 privacy notices
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS consent_notices (
                            id TEXT PRIMARY KEY,
                            version TEXT DEFAULT '1.0',
                            organization_id TEXT,
                            organization_name TEXT,
                            title TEXT NOT NULL,
                            sector_code TEXT,
                            purposes TEXT,
                            data_categories TEXT,
                            retention_policy TEXT,
                            dpo_name TEXT,
                            dpo_email TEXT,
                            dpo_phone TEXT,
                            grievance_officer_name TEXT,
                            grievance_officer_email TEXT,
                            withdrawal_url TEXT,
                            rights_url TEXT,
                            dpbi_complaint_url TEXT,
                            language TEXT DEFAULT 'en',
                            content TEXT,
                            content_plain TEXT,
                            is_active INTEGER DEFAULT 1,
                            is_current_version INTEGER DEFAULT 1,
                            effective_from TIMESTAMP,
                            effective_to TIMESTAMP,
                            approved_by TEXT,
                            approved_at TIMESTAMP,
                            created_by TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Data Access Log — tracks who accessed data under consent
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS data_access_log (
                            id TEXT PRIMARY KEY,
                            consent_id TEXT,
                            data_principal_id TEXT NOT NULL,
                            accessor_id TEXT,
                            accessor_name TEXT,
                            accessor_organization TEXT,
                            accessor_role TEXT,
                            purpose TEXT,
                            data_category TEXT,
                            data_elements TEXT,
                            access_type TEXT NOT NULL,
                            access_channel TEXT DEFAULT 'API',
                            ip_address TEXT,
                            user_agent TEXT,
                            consent_verified INTEGER DEFAULT 0,
                            result_status TEXT DEFAULT 'SUCCESS',
                            denial_reason TEXT,
                            accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (consent_id) REFERENCES consents(id),
                            FOREIGN KEY (data_principal_id) REFERENCES data_principals(id)
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // POLICY MANAGEMENT TABLES
            // ═══════════════════════════════════════════════════════════

            // Policies table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS policies (
                            id TEXT PRIMARY KEY,
                            code TEXT UNIQUE NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT,
                            category TEXT NOT NULL,
                            content TEXT,
                            version TEXT NOT NULL DEFAULT '1.0',
                            status TEXT NOT NULL DEFAULT 'DRAFT',
                            effective_date TIMESTAMP,
                            review_date TIMESTAMP,
                            owner TEXT,
                            approver TEXT,
                            approved_at TIMESTAMP,
                            expiry_date TIMESTAMP,
                            iso_controls TEXT,
                            dpdp_sections TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // BREACH MANAGEMENT TABLES
            // ═══════════════════════════════════════════════════════════

            // Breaches table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS breaches (
                            id TEXT PRIMARY KEY,
                            reference_number TEXT UNIQUE NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT,
                            severity TEXT NOT NULL DEFAULT 'MEDIUM',
                            breach_type TEXT,
                            data_categories TEXT,
                            affected_count INTEGER DEFAULT 0,
                            detected_at TIMESTAMP NOT NULL,
                            reported_at TIMESTAMP,
                            contained_at TIMESTAMP,
                            resolved_at TIMESTAMP,
                            status TEXT NOT NULL DEFAULT 'OPEN',
                            root_cause TEXT,
                            remediation_steps TEXT,
                            dpbi_notified INTEGER DEFAULT 0,
                            dpbi_notification_date TIMESTAMP,
                            dpbi_deadline TIMESTAMP,
                            dpbi_reference TEXT,
                            certin_notified INTEGER DEFAULT 0,
                            certin_notification_date TIMESTAMP,
                            certin_deadline TIMESTAMP,
                            affected_parties_notified INTEGER DEFAULT 0,
                            reported_by TEXT,
                            assigned_to TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // DPIA TABLES
            // ═══════════════════════════════════════════════════════════

            // DPIAs table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS dpias (
                            id TEXT PRIMARY KEY,
                            reference_number TEXT UNIQUE NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT,
                            project_name TEXT,
                            processing_activity TEXT,
                            data_types TEXT,
                            data_volume TEXT,
                            involves_sensitive_data INTEGER DEFAULT 0,
                            involves_children_data INTEGER DEFAULT 0,
                            cross_border_transfer INTEGER DEFAULT 0,
                            automated_decision_making INTEGER DEFAULT 0,
                            risk_score REAL DEFAULT 0,
                            risk_level TEXT DEFAULT 'LOW',
                            status TEXT NOT NULL DEFAULT 'DRAFT',
                            findings TEXT,
                            mitigation_plan TEXT,
                            residual_risks TEXT,
                            assessment_data TEXT,
                            mitigations TEXT,
                            assessor TEXT,
                            reviewer TEXT,
                            approver TEXT,
                            started_at TIMESTAMP,
                            completed_at TIMESTAMP,
                            reviewed_at TIMESTAMP,
                            approved_at TIMESTAMP,
                            next_review_at TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // RIGHTS REQUEST TABLES
            // ═══════════════════════════════════════════════════════════

            // Rights Requests table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rights_requests (
                            id TEXT PRIMARY KEY,
                            reference_number TEXT UNIQUE NOT NULL,
                            data_principal_id TEXT NOT NULL,
                            request_type TEXT NOT NULL,
                            description TEXT,
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            priority TEXT DEFAULT 'NORMAL',
                            assigned_to TEXT,
                            received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            acknowledged_at TIMESTAMP,
                            deadline TIMESTAMP,
                            completed_at TIMESTAMP,
                            response TEXT,
                            evidence_package TEXT,
                            notes TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (data_principal_id) REFERENCES data_principals(id)
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // GAP ANALYSIS TABLES
            // ═══════════════════════════════════════════════════════════

            // Gap Assessments table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS gap_assessments (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            sector TEXT,
                            assessment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            assessor TEXT,
                            overall_score REAL DEFAULT 0,
                            status TEXT DEFAULT 'IN_PROGRESS',
                            rag_status TEXT,
                            responses TEXT,
                            gaps TEXT,
                            recommendations TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // AUDIT TABLES
            // ═══════════════════════════════════════════════════════════

            // Audit Log table (immutable, hash-chained)
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS audit_log (
                            id TEXT PRIMARY KEY,
                            sequence_number INTEGER NOT NULL,
                            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            event_type TEXT NOT NULL,
                            module TEXT NOT NULL,
                            action TEXT NOT NULL,
                            actor TEXT NOT NULL,
                            actor_role TEXT,
                            entity_type TEXT,
                            entity_id TEXT,
                            old_value TEXT,
                            new_value TEXT,
                            details TEXT,
                            ip_address TEXT,
                            user_agent TEXT,
                            session_id TEXT,
                            dpdp_section TEXT,
                            control_id TEXT,
                            hash TEXT NOT NULL,
                            prev_hash TEXT NOT NULL
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // COMPLIANCE SCORING TABLES
            // ═══════════════════════════════════════════════════════════

            // Compliance Scores table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS compliance_scores (
                            id TEXT PRIMARY KEY,
                            module TEXT NOT NULL,
                            metric_name TEXT NOT NULL,
                            metric_value REAL NOT NULL,
                            rag_status TEXT NOT NULL,
                            calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            details TEXT
                        )
                    """);

            // Controls table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS controls (
                            id TEXT PRIMARY KEY,
                            control_id TEXT UNIQUE NOT NULL,
                            name TEXT NOT NULL,
                            description TEXT,
                            category TEXT NOT NULL,
                            control_type TEXT NOT NULL,
                            dpdp_section TEXT,
                            iso_control TEXT,
                            evidence_type TEXT,
                            test_frequency TEXT,
                            owner TEXT,
                            status TEXT DEFAULT 'NOT_TESTED',
                            last_tested_at TIMESTAMP,
                            test_result TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // SETTINGS & LICENSE TABLES
            // ═══════════════════════════════════════════════════════════

            // Settings table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS settings (
                            key TEXT PRIMARY KEY,
                            value TEXT,
                            category TEXT,
                            description TEXT,
                            is_encrypted INTEGER DEFAULT 0,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_by TEXT
                        )
                    """);

            // License table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS license (
                            id INTEGER PRIMARY KEY,
                            license_key TEXT,
                            license_type TEXT DEFAULT 'TRIAL',
                            organization TEXT,
                            contact_email TEXT,
                            activated_at TIMESTAMP,
                            expires_at TIMESTAMP,
                            features TEXT,
                            max_users INTEGER,
                            is_valid INTEGER DEFAULT 1
                        )
                    """);

            // ═══════════════════════════════════════════════════════════
            // ORGANIZATION & HIERARCHY TABLES (Sprint 1)
            // ═══════════════════════════════════════════════════════════

            // Organizations table — full org details as per DPDP Act
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS organizations (
                            id TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            legal_name TEXT,
                            sector TEXT,
                            industry TEXT,
                            address_line1 TEXT,
                            address_line2 TEXT,
                            city TEXT,
                            state TEXT,
                            pin_code TEXT,
                            country TEXT DEFAULT 'India',
                            phone TEXT,
                            alternate_phone TEXT,
                            email TEXT,
                            alternate_email TEXT,
                            website TEXT,
                            gst_number TEXT,
                            pan_number TEXT,
                            cin_number TEXT,
                            duns_number TEXT,
                            incorporation_date TEXT,
                            employee_count INTEGER DEFAULT 0,
                            annual_turnover TEXT,
                            data_fiduciary_registered INTEGER DEFAULT 0,
                            dpo_name TEXT,
                            dpo_email TEXT,
                            dpo_phone TEXT,
                            consent_manager_name TEXT,
                            consent_manager_email TEXT,
                            grievance_officer_name TEXT,
                            grievance_officer_email TEXT,
                            dpbi_registration_number TEXT,
                            logo_path TEXT,
                            is_significant_data_fiduciary INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Org Hierarchy Levels — configurable L0..Ln with DPDP role mapping
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS org_hierarchy_levels (
                            id TEXT PRIMARY KEY,
                            org_id TEXT NOT NULL,
                            level_number INTEGER NOT NULL,
                            level_code TEXT NOT NULL,
                            level_name TEXT NOT NULL,
                            description TEXT,
                            dpdp_role_mapping TEXT,
                            can_approve_consent INTEGER DEFAULT 0,
                            can_approve_breach INTEGER DEFAULT 0,
                            can_approve_dpia INTEGER DEFAULT 0,
                            can_view_reports INTEGER DEFAULT 0,
                            can_manage_policy INTEGER DEFAULT 0,
                            max_positions INTEGER DEFAULT 0,
                            is_active INTEGER DEFAULT 1,
                            sort_order INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (org_id) REFERENCES organizations(id),
                            UNIQUE (org_id, level_number)
                        )
                    """);

            // Employees — mapped to hierarchy levels and DPDP roles
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS employees (
                            id TEXT PRIMARY KEY,
                            org_id TEXT NOT NULL,
                            employee_code TEXT,
                            full_name TEXT NOT NULL,
                            designation TEXT,
                            department TEXT,
                            hierarchy_level_id TEXT,
                            reporting_to_id TEXT,
                            email TEXT,
                            phone TEXT,
                            dpdp_role TEXT,
                            is_dpo INTEGER DEFAULT 0,
                            is_consent_manager INTEGER DEFAULT 0,
                            is_grievance_officer INTEGER DEFAULT 0,
                            is_data_fiduciary INTEGER DEFAULT 0,
                            date_of_joining TEXT,
                            status TEXT DEFAULT 'ACTIVE',
                            user_id TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (org_id) REFERENCES organizations(id),
                            FOREIGN KEY (hierarchy_level_id) REFERENCES org_hierarchy_levels(id),
                            FOREIGN KEY (reporting_to_id) REFERENCES employees(id),
                            FOREIGN KEY (user_id) REFERENCES users(id)
                        )
                    """);

            // Supported Languages table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS supported_languages (
                            code TEXT PRIMARY KEY,
                            name TEXT NOT NULL,
                            native_name TEXT NOT NULL,
                            script TEXT,
                            is_enabled INTEGER DEFAULT 1,
                            sort_order INTEGER DEFAULT 0
                        )
                    """);

            // Insert default Indian languages (22 scheduled + English)
            insertDefaultLanguages(stmt);

            // ═══════════════════════════════════════════════════════════
            // INDEXES
            // ═══════════════════════════════════════════════════════════

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_consents_principal ON consents(data_principal_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_consents_purpose ON consents(purpose_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_consents_status ON consents(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_breaches_status ON breaches(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_breaches_severity ON breaches(severity)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_dpias_status ON dpias(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_rights_status ON rights_requests(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_rights_principal ON rights_requests(data_principal_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_log(timestamp)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_log(actor)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_module ON audit_log(module)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_sequence ON audit_log(sequence_number)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_policies_status ON policies(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions(expires_at)");

            // Phase 2 — Consent Enhancement Indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_consent_prefs_consent ON consent_preferences(consent_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_consent_prefs_principal ON consent_preferences(data_principal_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_guardian_child ON guardian_consents(child_principal_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_guardian_status ON guardian_consents(status)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_consent_audit_consent ON consent_audit_trail(consent_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_consent_audit_block ON consent_audit_trail(block_number)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sector_templates_sector ON sector_purpose_templates(sector)");

            // Sprint 1 — Organization & Hierarchy Indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_org_hierarchy_org ON org_hierarchy_levels(org_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_org_hierarchy_level ON org_hierarchy_levels(level_number)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_employees_org ON employees(org_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_employees_hierarchy ON employees(hierarchy_level_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_employees_reporting ON employees(reporting_to_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_employees_dpdp_role ON employees(dpdp_role)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status)");

            // ═══════════════════════════════════════════════════════════
            // DEFAULT DATA
            // ═══════════════════════════════════════════════════════════

            // Insert schema version
            stmt.executeUpdate("INSERT OR IGNORE INTO schema_version (version) VALUES (" + SCHEMA_VERSION + ")");

            // Note: Default admin user is seeded by DataSeeder with properly hashed password

            // Insert default settings
            insertDefaultSettings(stmt);

            // Insert trial license
            stmt.executeUpdate("""
                        INSERT OR IGNORE INTO license (id, license_type, activated_at, expires_at, max_users)
                        VALUES (1, 'TRIAL', CURRENT_TIMESTAMP, datetime(CURRENT_TIMESTAMP, '+30 days'), 5)
                    """);

            logger.info("Database schema v{} initialized successfully", SCHEMA_VERSION);
        }
    }

    private void insertDefaultSettings(Statement stmt) throws SQLException {
        String[][] settings = {
                { "org_name", "Your Organization", "organization", "Organization name" },
                { "org_address", "", "organization", "Organization address" },
                { "dpo_name", "Data Protection Officer", "organization", "DPO name" },
                { "dpo_email", "dpo@organization.com", "organization", "DPO email" },
                { "dpo_phone", "", "organization", "DPO phone" },
                { "default_language", "en", "preferences", "Default language" },
                { "consent_retention_years", "7", "retention", "Consent retention period" },
                { "audit_retention_years", "10", "retention", "Audit log retention period" },
                { "breach_notification_hours", "72", "breach", "DPBI notification deadline" },
                { "certin_notification_hours", "6", "breach", "CERT-IN notification deadline" },
                { "rights_response_days", "30", "rights", "Rights request response deadline" },
                { "mfa_required", "false", "security", "MFA requirement" },
                { "session_timeout_minutes", "30", "security", "Session timeout" },
                { "password_min_length", "12", "security", "Minimum password length" },
                { "selected_sector", "", "sector", "Currently selected industry sector" }
        };

        for (String[] setting : settings) {
            stmt.executeUpdate(String.format(
                    "INSERT OR IGNORE INTO settings (key, value, category, description) VALUES ('%s', '%s', '%s', '%s')",
                    setting[0], setting[1], setting[2], setting[3]));
        }
    }

    private void insertDefaultLanguages(Statement stmt) throws SQLException {
        // 22 Scheduled Indian Languages (Eighth Schedule) + English
        String[][] languages = {
                { "en", "English", "English", "Latin", "1" },
                { "hi", "Hindi", "\u0939\u093f\u0928\u094d\u0926\u0940", "Devanagari", "1" },
                { "bn", "Bengali", "\u09ac\u09be\u0982\u09b2\u09be", "Bengali", "1" },
                { "te", "Telugu", "\u0c24\u0c46\u0c32\u0c41\u0c17\u0c41", "Telugu", "1" },
                { "mr", "Marathi", "\u092e\u0930\u093e\u0920\u0940", "Devanagari", "1" },
                { "ta", "Tamil", "\u0ba4\u0bae\u0bbf\u0bb4\u0bcd", "Tamil", "1" },
                { "gu", "Gujarati", "\u0a97\u0ac1\u0a9c\u0ab0\u0abe\u0aa4\u0ac0", "Gujarati", "1" },
                { "ur", "Urdu", "\u0627\u0631\u062f\u0648", "Perso-Arabic", "1" },
                { "kn", "Kannada", "\u0c95\u0ca8\u0ccd\u0ca8\u0ca1", "Kannada", "1" },
                { "or", "Odia", "\u0b13\u0b21\u0b3c\u0b3f\u0b06", "Odia", "1" },
                { "ml", "Malayalam", "\u0d2e\u0d32\u0d2f\u0d3e\u0d33\u0d02", "Malayalam", "1" },
                { "pa", "Punjabi", "\u0a2a\u0a70\u0a1c\u0a3e\u0a2c\u0a40", "Gurmukhi", "1" },
                { "as", "Assamese", "\u0985\u09b8\u09ae\u09c0\u09af\u09bc\u09be", "Bengali", "1" },
                { "mai", "Maithili", "\u092e\u0948\u0925\u093f\u0932\u0940", "Devanagari", "0" },
                { "sa", "Sanskrit", "\u0938\u0902\u0938\u094d\u0915\u0943\u0924\u092e\u094d", "Devanagari", "0" },
                { "kok", "Konkani", "\u0915\u094b\u0902\u0915\u0923\u0940", "Devanagari", "0" },
                { "ne", "Nepali", "\u0928\u0947\u092a\u093e\u0932\u0940", "Devanagari", "0" },
                { "sd", "Sindhi", "\u0938\u093f\u0928\u094d\u0927\u0940", "Devanagari", "0" },
                { "ks", "Kashmiri", "\u0915\u0936\u094d\u092e\u0940\u0930\u0940", "Perso-Arabic", "0" },
                { "doi", "Dogri", "\u0921\u094b\u0917\u0930\u0940", "Devanagari", "0" },
                { "mni", "Manipuri", "\u09ae\u09c8\u09a4\u09c8\u09b2\u09cb\u09a8\u09cd", "Bengali", "0" },
                { "bo", "Bodo", "\u092c\u094b\u0921\u094b", "Devanagari", "0" },
                { "sat", "Santali", "\u1c65\u1c5f\u1c71\u1c5b\u1c5f\u1c62\u1c64", "Ol Chiki", "0" }
        };

        for (int i = 0; i < languages.length; i++) {
            String[] lang = languages[i];
            stmt.executeUpdate(String.format(
                    "INSERT OR IGNORE INTO supported_languages (code, name, native_name, script, is_enabled, sort_order) " +
                    "VALUES ('%s', '%s', '%s', '%s', %s, %d)",
                    lang[0], lang[1], lang[2], lang[3], lang[4], i));
        }
    }

    /**
     * Get direct connection without initialization check (for internal use during
     * init)
     */
    private Connection getDirectConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(connectionUrl);
        conn.setAutoCommit(true);
        // Enable WAL mode for concurrent read/write access (critical for CRUD during data seeding)
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=30000"); // Wait up to 30 seconds for locks
        }
        return conn;
    }

    /**
     * Get a new database connection
     * Caller is responsible for closing the connection
     */
    public Connection getConnection() throws SQLException {
        if (!initialized) {
            throw new SQLException("Database not initialized. Call initialize() first.");
        }
        return getDirectConnection();
    }

    /**
     * Get the database file path
     */
    public String getDbPath() {
        return dbPath;
    }

    /**
     * Check if database is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get current schema version
     */
    public int getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down database manager...");
        initialized = false;
        logger.info("Database manager shutdown complete");
    }
}
