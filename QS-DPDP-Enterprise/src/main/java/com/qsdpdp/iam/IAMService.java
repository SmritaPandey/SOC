package com.qsdpdp.iam;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Identity and Access Management Service
 * Handles authentication, authorization, and purpose-based access control
 * 
 * @version 1.0.0
 * @since Module 11
 */
@Service
public class IAMService {

    private static final Logger logger = LoggerFactory.getLogger(IAMService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final SecurityManager securityManager;

    private boolean initialized = false;
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<Permission>> rolePermissions = new ConcurrentHashMap<>();

    @Autowired
    public IAMService(DatabaseManager dbManager, AuditService auditService,
            SecurityManager securityManager) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.securityManager = securityManager;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing IAM Service...");
        createTables();
        initializeRolePermissions();

        initialized = true;
        logger.info("IAM Service initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS iam_users (
                            id TEXT PRIMARY KEY,
                            username TEXT UNIQUE NOT NULL,
                            email TEXT UNIQUE NOT NULL,
                            password_hash TEXT NOT NULL,
                            full_name TEXT,
                            department TEXT,
                            phone TEXT,
                            active INTEGER DEFAULT 1,
                            mfa_enabled INTEGER DEFAULT 0,
                            mfa_secret TEXT,
                            failed_attempts INTEGER DEFAULT 0,
                            locked_until TIMESTAMP,
                            last_login TIMESTAMP,
                            password_changed_at TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS iam_user_roles (
                            id TEXT PRIMARY KEY,
                            user_id TEXT NOT NULL,
                            role TEXT NOT NULL,
                            granted_by TEXT,
                            granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            expires_at TIMESTAMP,
                            purpose TEXT,
                            FOREIGN KEY (user_id) REFERENCES iam_users(id),
                            UNIQUE (user_id, role)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS iam_sessions (
                            id TEXT PRIMARY KEY,
                            user_id TEXT NOT NULL,
                            token_hash TEXT NOT NULL,
                            ip_address TEXT,
                            user_agent TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            expires_at TIMESTAMP,
                            last_activity TIMESTAMP,
                            active INTEGER DEFAULT 1,
                            FOREIGN KEY (user_id) REFERENCES iam_users(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS iam_purpose_grants (
                            id TEXT PRIMARY KEY,
                            user_id TEXT NOT NULL,
                            purpose TEXT NOT NULL,
                            resource_type TEXT,
                            resource_id TEXT,
                            granted_by TEXT,
                            granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            expires_at TIMESTAMP,
                            reason TEXT,
                            active INTEGER DEFAULT 1,
                            FOREIGN KEY (user_id) REFERENCES iam_users(id)
                        )
                    """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_iam_sessions_token ON iam_sessions(token_hash)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_iam_purpose_user ON iam_purpose_grants(user_id)");

            logger.info("IAM tables created");

        } catch (SQLException e) {
            logger.error("Failed to create IAM tables", e);
        }
    }

    private void initializeRolePermissions() {
        rolePermissions.put(Role.SUPER_ADMIN.name(), new HashSet<>(Arrays.asList(Permission.values())));
        rolePermissions.put(Role.DPO.name(), Permission.getDPOPermissions());
        rolePermissions.put(Role.SECURITY_ANALYST.name(), Permission.getSecurityAnalystPermissions());
        rolePermissions.put(Role.COMPLIANCE_OFFICER.name(), Permission.getComplianceOfficerPermissions());
        rolePermissions.put(Role.AUDITOR.name(), Permission.getAuditorPermissions());

        // Additional role mappings
        rolePermissions.put(Role.CISO.name(), new HashSet<>(Permission.getSecurityAnalystPermissions()) {
            {
                add(Permission.SIEM_CORRELATE);
                add(Permission.DLP_POLICY_CREATE);
                add(Permission.BREACH_CLOSE);
            }
        });

        rolePermissions.put(Role.PRIVACY_MANAGER.name(), new HashSet<>(Permission.getComplianceOfficerPermissions()) {
            {
                add(Permission.CONSENT_CREATE);
                add(Permission.CONSENT_UPDATE);
                add(Permission.RIGHTS_APPROVE);
            }
        });

        rolePermissions.put(Role.VIEWER.name(), Set.of(
                Permission.CONSENT_READ, Permission.BREACH_READ, Permission.RIGHTS_READ,
                Permission.DPIA_READ, Permission.POLICY_READ, Permission.GAP_READ,
                Permission.REPORT_VIEW));
    }

    // ═══════════════════════════════════════════════════════════
    // AUTHENTICATION
    // ═══════════════════════════════════════════════════════════

    public AuthResult authenticate(String username, String password, String ipAddress, String userAgent) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT * FROM iam_users WHERE username = ? AND active = 1")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                auditService.log("AUTH_FAILED", "IAM", username, "User not found");
                return AuthResult.failed("Invalid credentials");
            }

            String userId = rs.getString("id");
            String passwordHash = rs.getString("password_hash");
            int failedAttempts = rs.getInt("failed_attempts");
            String lockedUntil = rs.getString("locked_until");

            // Check lockout
            if (lockedUntil != null && LocalDateTime.parse(lockedUntil).isAfter(LocalDateTime.now())) {
                auditService.log("AUTH_LOCKED", "IAM", username, "Account locked");
                return AuthResult.failed("Account locked. Try again later.");
            }

            // Verify password
            if (!securityManager.verifyPassword(password, passwordHash)) {
                incrementFailedAttempts(userId, failedAttempts + 1);
                auditService.log("AUTH_FAILED", "IAM", username, "Invalid password");
                return AuthResult.failed("Invalid credentials");
            }

            // Success - create session
            resetFailedAttempts(userId);
            updateLastLogin(userId);

            String sessionId = UUID.randomUUID().toString();
            String token = securityManager.generateToken(64);
            String tokenHash = securityManager.sha256(token);

            createSession(sessionId, userId, tokenHash, ipAddress, userAgent);

            // Get user roles
            Set<Role> roles = getUserRoles(userId);
            Set<Permission> permissions = getEffectivePermissions(roles);

            UserSession session = new UserSession(sessionId, userId, username,
                    rs.getString("full_name"), roles, permissions);
            activeSessions.put(sessionId, session);

            auditService.log("AUTH_SUCCESS", "IAM", username, "Login from " + ipAddress);

            return AuthResult.success(sessionId, token, session);

        } catch (SQLException e) {
            logger.error("Authentication error", e);
            return AuthResult.failed("System error");
        }
    }

    public void logout(String sessionId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE iam_sessions SET active = 0 WHERE id = ?")) {
            stmt.setString(1, sessionId);
            stmt.executeUpdate();

            UserSession session = activeSessions.remove(sessionId);
            if (session != null) {
                auditService.log("LOGOUT", "IAM", session.getUsername(), "Session ended");
            }
        } catch (SQLException e) {
            logger.error("Logout error", e);
        }
    }

    public UserSession validateSession(String sessionId, String token) {
        UserSession cached = activeSessions.get(sessionId);
        if (cached != null && cached.isValid()) {
            return cached;
        }

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement("""
                        SELECT s.*, u.username, u.full_name FROM iam_sessions s
                        JOIN iam_users u ON s.user_id = u.id
                        WHERE s.id = ? AND s.active = 1 AND s.expires_at > ?
                        """)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, LocalDateTime.now().toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String tokenHash = rs.getString("token_hash");
                if (securityManager.sha256(token).equals(tokenHash)) {
                    String userId = rs.getString("user_id");
                    Set<Role> roles = getUserRoles(userId);
                    Set<Permission> permissions = getEffectivePermissions(roles);

                    UserSession session = new UserSession(sessionId, userId,
                            rs.getString("username"), rs.getString("full_name"),
                            roles, permissions);
                    activeSessions.put(sessionId, session);

                    updateSessionActivity(sessionId);
                    return session;
                }
            }
        } catch (SQLException e) {
            logger.error("Session validation error", e);
        }

        return null;
    }

    // ═══════════════════════════════════════════════════════════
    // AUTHORIZATION
    // ═══════════════════════════════════════════════════════════

    public boolean hasPermission(String sessionId, Permission permission) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null)
            return false;
        return session.getPermissions().contains(permission);
    }

    public boolean hasRole(String sessionId, Role role) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null)
            return false;
        return session.getRoles().contains(role);
    }

    public boolean hasAnyPermission(String sessionId, Permission... permissions) {
        UserSession session = activeSessions.get(sessionId);
        if (session == null)
            return false;
        for (Permission p : permissions) {
            if (session.getPermissions().contains(p))
                return true;
        }
        return false;
    }

    public void checkPermission(String sessionId, Permission permission) throws AccessDeniedException {
        if (!hasPermission(sessionId, permission)) {
            UserSession session = activeSessions.get(sessionId);
            String user = session != null ? session.getUsername() : "UNKNOWN";
            auditService.log("ACCESS_DENIED", "IAM", user, "Missing permission: " + permission.name());
            throw new AccessDeniedException("Permission denied: " + permission.getDisplayName());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PURPOSE-BASED ACCESS CONTROL
    // ═══════════════════════════════════════════════════════════

    public void grantPurposeAccess(String userId, String purpose, String resourceType,
            String resourceId, String grantedBy, LocalDateTime expiresAt, String reason) {
        String sql = """
                    INSERT INTO iam_purpose_grants (id, user_id, purpose, resource_type, resource_id,
                        granted_by, expires_at, reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, userId);
            stmt.setString(3, purpose);
            stmt.setString(4, resourceType);
            stmt.setString(5, resourceId);
            stmt.setString(6, grantedBy);
            stmt.setString(7, expiresAt != null ? expiresAt.toString() : null);
            stmt.setString(8, reason);
            stmt.executeUpdate();

            auditService.log("PURPOSE_GRANTED", "IAM", grantedBy,
                    "Purpose '" + purpose + "' granted to user " + userId);

        } catch (SQLException e) {
            logger.error("Failed to grant purpose access", e);
        }
    }

    public boolean hasPurposeAccess(String userId, String purpose, String resourceType, String resourceId) {
        String sql = """
                    SELECT 1 FROM iam_purpose_grants
                    WHERE user_id = ? AND purpose = ? AND active = 1
                    AND (expires_at IS NULL OR expires_at > ?)
                    AND (resource_type IS NULL OR resource_type = ?)
                    AND (resource_id IS NULL OR resource_id = ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, purpose);
            stmt.setString(3, LocalDateTime.now().toString());
            stmt.setString(4, resourceType);
            stmt.setString(5, resourceId);
            return stmt.executeQuery().next();

        } catch (SQLException e) {
            logger.error("Failed to check purpose access", e);
            return false;
        }
    }

    public void revokePurposeAccess(String grantId, String revokedBy) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE iam_purpose_grants SET active = 0 WHERE id = ?")) {
            stmt.setString(1, grantId);
            stmt.executeUpdate();

            auditService.log("PURPOSE_REVOKED", "IAM", revokedBy, "Grant " + grantId + " revoked");

        } catch (SQLException e) {
            logger.error("Failed to revoke purpose access", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // USER MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public String createUser(String username, String email, String password, String fullName,
            String department, String createdBy) {
        String userId = UUID.randomUUID().toString();
        String passwordHash = securityManager.hashPassword(password);

        String sql = """
                    INSERT INTO iam_users (id, username, email, password_hash, full_name,
                        department, password_changed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, email);
            stmt.setString(4, passwordHash);
            stmt.setString(5, fullName);
            stmt.setString(6, department);
            stmt.setString(7, LocalDateTime.now().toString());
            stmt.executeUpdate();

            auditService.log("USER_CREATED", "IAM", createdBy, "Created user: " + username);
            return userId;

        } catch (SQLException e) {
            logger.error("Failed to create user", e);
            return null;
        }
    }

    public void assignRole(String userId, Role role, String grantedBy, String purpose) {
        String sql = """
                    INSERT OR REPLACE INTO iam_user_roles (id, user_id, role, granted_by, purpose)
                    VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, userId);
            stmt.setString(3, role.name());
            stmt.setString(4, grantedBy);
            stmt.setString(5, purpose);
            stmt.executeUpdate();

            auditService.log("ROLE_ASSIGNED", "IAM", grantedBy,
                    "Role " + role.name() + " assigned to " + userId);

        } catch (SQLException e) {
            logger.error("Failed to assign role", e);
        }
    }

    public void removeRole(String userId, Role role, String removedBy) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM iam_user_roles WHERE user_id = ? AND role = ?")) {
            stmt.setString(1, userId);
            stmt.setString(2, role.name());
            stmt.executeUpdate();

            auditService.log("ROLE_REMOVED", "IAM", removedBy,
                    "Role " + role.name() + " removed from " + userId);

        } catch (SQLException e) {
            logger.error("Failed to remove role", e);
        }
    }

    public Set<Role> getUserRoles(String userId) {
        Set<Role> roles = new HashSet<>();
        String sql = "SELECT role FROM iam_user_roles WHERE user_id = ? " +
                "AND (expires_at IS NULL OR expires_at > ?)";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setString(2, LocalDateTime.now().toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                try {
                    roles.add(Role.valueOf(rs.getString("role")));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get user roles", e);
        }

        return roles;
    }

    private Set<Permission> getEffectivePermissions(Set<Role> roles) {
        Set<Permission> permissions = new HashSet<>();
        for (Role role : roles) {
            Set<Permission> rolePerms = rolePermissions.get(role.name());
            if (rolePerms != null) {
                permissions.addAll(rolePerms);
            }
        }
        return permissions;
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private void incrementFailedAttempts(String userId, int attempts) {
        String lockedUntil = attempts >= 5 ? LocalDateTime.now().plusMinutes(30).toString() : null;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE iam_users SET failed_attempts = ?, locked_until = ? WHERE id = ?")) {
            stmt.setInt(1, attempts);
            stmt.setString(2, lockedUntil);
            stmt.setString(3, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update failed attempts", e);
        }
    }

    private void resetFailedAttempts(String userId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE iam_users SET failed_attempts = 0, locked_until = NULL WHERE id = ?")) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to reset failed attempts", e);
        }
    }

    private void updateLastLogin(String userId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE iam_users SET last_login = ? WHERE id = ?")) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update last login", e);
        }
    }

    private void createSession(String sessionId, String userId, String tokenHash,
            String ipAddress, String userAgent) {
        String sql = """
                    INSERT INTO iam_sessions (id, user_id, token_hash, ip_address, user_agent,
                        expires_at, last_activity)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(8);

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessionId);
            stmt.setString(2, userId);
            stmt.setString(3, tokenHash);
            stmt.setString(4, ipAddress);
            stmt.setString(5, userAgent);
            stmt.setString(6, expiresAt.toString());
            stmt.setString(7, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to create session", e);
        }
    }

    private void updateSessionActivity(String sessionId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE iam_sessions SET last_activity = ? WHERE id = ?")) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update session activity", e);
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final String sessionId;
        private final String token;
        private final UserSession session;

        private AuthResult(boolean success, String message, String sessionId,
                String token, UserSession session) {
            this.success = success;
            this.message = message;
            this.sessionId = sessionId;
            this.token = token;
            this.session = session;
        }

        public static AuthResult success(String sessionId, String token, UserSession session) {
            return new AuthResult(true, "Success", sessionId, token, session);
        }

        public static AuthResult failed(String message) {
            return new AuthResult(false, message, null, null, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getToken() {
            return token;
        }

        public UserSession getSession() {
            return session;
        }
    }

    public static class UserSession {
        private final String sessionId;
        private final String userId;
        private final String username;
        private final String fullName;
        private final Set<Role> roles;
        private final Set<Permission> permissions;
        private final LocalDateTime createdAt;

        public UserSession(String sessionId, String userId, String username,
                String fullName, Set<Role> roles, Set<Permission> permissions) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.roles = roles;
            this.permissions = permissions;
            this.createdAt = LocalDateTime.now();
        }

        public boolean isValid() {
            return createdAt.plusHours(8).isAfter(LocalDateTime.now());
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getFullName() {
            return fullName;
        }

        public Set<Role> getRoles() {
            return roles;
        }

        public Set<Permission> getPermissions() {
            return permissions;
        }
    }

    public static class AccessDeniedException extends Exception {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
