package com.qsdpdp.user;

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

/**
 * User Management Service
 * Handles authentication, authorization, and user lifecycle
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final SecurityManager securityManager;

    private boolean initialized = false;

    @Autowired
    public UserService(DatabaseManager dbManager, AuditService auditService, SecurityManager securityManager) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.securityManager = securityManager;
    }

    public void initialize() {
        if (initialized)
            return;
        logger.info("Initializing User Service...");
        ensureAdminExists();
        initialized = true;
        logger.info("User Service initialized");
    }

    private void ensureAdminExists() {
        if (getUserByUsername("admin") == null) {
            createUser(new UserRequest("admin", "admin@qsdpdp.local",
                    "Admin@123", "System Administrator", "IT", "ADMIN"));
            logger.info("Default admin user created");
        }
    }

    public User createUser(UserRequest request) {
        logger.info("Creating user: {}", request.getUsername());

        if (getUserByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        String passwordHash = securityManager.hashPassword(request.getPassword());

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordHash)
                .fullName(request.getFullName())
                .department(request.getDepartment())
                .role(request.getRole())
                .build();

        user.setPasswordChangedAt(LocalDateTime.now());

        saveUser(user);

        auditService.log("USER_CREATED", "USER", "SYSTEM",
                String.format("User created: %s (%s)", request.getUsername(), request.getRole()));

        return user;
    }

    private void saveUser(User user) {
        String sql = """
                    INSERT INTO users (id, username, email, password_hash, full_name, department,
                        role, status, mfa_enabled)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPasswordHash());
            stmt.setString(5, user.getFullName());
            stmt.setString(6, user.getDepartment());
            stmt.setString(7, user.getRole());
            stmt.setString(8, user.isActive() ? "ACTIVE" : "INACTIVE");
            stmt.setInt(9, user.isMfaEnabled() ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public AuthResult authenticate(String username, String password) {
        logger.debug("Authenticating user: {}", username);

        User user = getUserByUsername(username);
        if (user == null) {
            auditService.log("LOGIN_FAILED", "AUTH", username, "User not found: " + username);
            return new AuthResult(false, "Invalid credentials", null);
        }

        if (!user.isActive()) {
            auditService.log("LOGIN_FAILED", "AUTH", username, "Account disabled");
            return new AuthResult(false, "Account disabled", null);
        }

        if (user.isLocked()) {
            auditService.log("LOGIN_FAILED", "AUTH", username, "Account locked");
            return new AuthResult(false, "Account locked", null);
        }

        if (!securityManager.verifyPassword(password, user.getPasswordHash())) {
            handleFailedLogin(user);
            auditService.log("LOGIN_FAILED", "AUTH", username, "Invalid password");
            return new AuthResult(false, "Invalid credentials", null);
        }

        // Success
        resetFailedAttempts(user);
        updateLastLogin(user);

        String sessionToken = securityManager.generateToken(32);

        auditService.log("LOGIN_SUCCESS", "AUTH", username, "User logged in");

        return new AuthResult(true, "Login successful", sessionToken);
    }

    private void handleFailedLogin(User user) {
        user.setFailedAttempts(user.getFailedAttempts() + 1);

        if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            logger.warn("Account locked: {} after {} failed attempts", user.getUsername(), user.getFailedAttempts());
        }

        String sql = "UPDATE users SET failed_login_attempts = ?, locked_until = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, user.getFailedAttempts());
            stmt.setString(2, user.getLockedUntil() != null ? user.getLockedUntil().toString() : null);
            stmt.setString(3, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update failed attempts", e);
        }
    }

    private void resetFailedAttempts(User user) {
        String sql = "UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to reset failed attempts", e);
        }
    }

    private void updateLastLogin(User user) {
        String sql = "UPDATE users SET last_login = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update last login", e);
        }
    }

    public boolean changePassword(String userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null)
            return false;

        if (!securityManager.verifyPassword(oldPassword, user.getPasswordHash())) {
            auditService.log("PASSWORD_CHANGE_FAILED", "USER", user.getUsername(), "Invalid old password");
            return false;
        }

        String newHash = securityManager.hashPassword(newPassword);
        String sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newHash);
            stmt.setString(2, LocalDateTime.now().toString());
            stmt.setString(3, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to change password", e);
        }

        auditService.log("PASSWORD_CHANGED", "USER", user.getUsername(), "Password changed");
        return true;
    }

    public User getUserById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapUser(rs);
        } catch (SQLException e) {
            logger.error("Failed to get user", e);
        }
        return null;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return mapUser(rs);
        } catch (SQLException e) {
            logger.error("Failed to get user by username", e);
        }
        return null;
    }

    public List<User> getAllUsers(int offset, int limit) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            ResultSet rs = stmt.executeQuery();
            while (rs.next())
                users.add(mapUser(rs));
        } catch (SQLException e) {
            logger.error("Failed to get users", e);
        }
        return users;
    }

    public UserStatistics getStatistics() {
        UserStatistics stats = new UserStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next())
                stats.setTotalUsers(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");
            if (rs.next())
                stats.setActiveUsers(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE locked_until > datetime('now')");
            if (rs.next())
                stats.setLockedUsers(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE mfa_enabled = 1");
            if (rs.next())
                stats.setMfaEnabledUsers(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get user statistics", e);
        }

        return stats;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setDepartment(rs.getString("department"));
        user.setRole(rs.getString("role"));
        user.setActive("ACTIVE".equals(rs.getString("status")));
        user.setMfaEnabled(rs.getInt("mfa_enabled") == 1);
        user.setMfaSecret(rs.getString("mfa_secret"));

        String lastLogin = rs.getString("last_login");
        if (lastLogin != null)
            user.setLastLogin(LocalDateTime.parse(lastLogin));

        user.setFailedAttempts(rs.getInt("failed_login_attempts"));

        String lockedUntil = rs.getString("locked_until");
        if (lockedUntil != null)
            user.setLockedUntil(LocalDateTime.parse(lockedUntil));

        return user;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
