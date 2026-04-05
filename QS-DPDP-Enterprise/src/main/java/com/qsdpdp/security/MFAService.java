package com.qsdpdp.security;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MFA Service — Database-backed Multi-Factor Authentication
 * Manages TOTP secret storage, MFA enrollment, and verification lifecycle.
 * Uses SecurityManager for cryptographic TOTP operations.
 *
 * @version 1.0.0
 * @since Sprint 7 — MFA Implementation
 */
@Service
public class MFAService {

    private static final Logger logger = LoggerFactory.getLogger(MFAService.class);

    @Autowired
    private DatabaseManager dbManager;

    @Autowired
    private SecurityManager securityManager;

    private static final String ISSUER = "QS-DPDP Enterprise";

    /**
     * Initialize MFA tables
     */
    public void initialize() {
        try (Connection conn = dbManager.getConnection()) {
            conn.createStatement().executeUpdate("""
                CREATE TABLE IF NOT EXISTS mfa_secrets (
                    user_id TEXT PRIMARY KEY,
                    totp_secret TEXT NOT NULL,
                    is_enabled INTEGER DEFAULT 0,
                    backup_codes TEXT,
                    enrolled_at TEXT,
                    last_used_at TEXT,
                    created_at TEXT DEFAULT (datetime('now')),
                    updated_at TEXT DEFAULT (datetime('now'))
                )
            """);
            logger.info("MFA tables initialized");
        } catch (Exception e) {
            logger.error("Failed to initialize MFA tables", e);
        }
    }

    /**
     * Setup MFA for a user — generates secret and provisioning URI
     */
    public Map<String, Object> setupMFA(String userId, String username) {
        String secret = securityManager.generateTOTPSecret();
        String provisioningURI = securityManager.getTOTPProvisioningURI(secret, username, ISSUER);

        // Generate backup codes
        StringBuilder backupCodes = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0) backupCodes.append(",");
            backupCodes.append(securityManager.generateToken(4)); // 8-char hex codes
        }

        try (Connection conn = dbManager.getConnection()) {
            // Upsert MFA secret
            PreparedStatement stmt = conn.prepareStatement("""
                INSERT INTO mfa_secrets (user_id, totp_secret, is_enabled, backup_codes, created_at, updated_at)
                VALUES (?, ?, 0, ?, datetime('now'), datetime('now'))
                ON CONFLICT(user_id) DO UPDATE SET
                    totp_secret = excluded.totp_secret,
                    backup_codes = excluded.backup_codes,
                    is_enabled = 0,
                    updated_at = datetime('now')
            """);
            stmt.setString(1, userId);
            stmt.setString(2, secret);
            stmt.setString(3, backupCodes.toString());
            stmt.executeUpdate();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("secret", secret);
            result.put("provisioningURI", provisioningURI);
            result.put("backupCodes", backupCodes.toString().split(","));
            result.put("message", "Scan QR code with Google Authenticator or Authy. Enter the 6-digit code to verify.");
            return result;

        } catch (Exception e) {
            logger.error("Failed to setup MFA for user: {}", userId, e);
            throw new RuntimeException("MFA setup failed: " + e.getMessage());
        }
    }

    /**
     * Verify and enable MFA — user must provide correct TOTP code to confirm enrollment
     */
    public boolean verifyAndEnableMFA(String userId, String code) {
        try (Connection conn = dbManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT totp_secret FROM mfa_secrets WHERE user_id = ?");
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                logger.warn("No MFA secret found for user: {}", userId);
                return false;
            }

            String secret = rs.getString("totp_secret");
            if (securityManager.verifyTOTP(secret, code)) {
                // Enable MFA
                PreparedStatement update = conn.prepareStatement("""
                    UPDATE mfa_secrets SET is_enabled = 1, enrolled_at = datetime('now'),
                    updated_at = datetime('now') WHERE user_id = ?
                """);
                update.setString(1, userId);
                update.executeUpdate();

                logger.info("MFA enabled for user: {}", userId);
                return true;
            }

            logger.warn("Invalid TOTP code during MFA enrollment for user: {}", userId);
            return false;
        } catch (Exception e) {
            logger.error("MFA verification failed for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Validate TOTP code during login
     */
    public boolean validateCode(String userId, String code) {
        try (Connection conn = dbManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT totp_secret, backup_codes FROM mfa_secrets WHERE user_id = ? AND is_enabled = 1");
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return false; // MFA not enabled
            }

            String secret = rs.getString("totp_secret");
            String backupCodes = rs.getString("backup_codes");

            // Try TOTP first
            if (securityManager.verifyTOTP(secret, code)) {
                // Update last used
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE mfa_secrets SET last_used_at = datetime('now') WHERE user_id = ?");
                update.setString(1, userId);
                update.executeUpdate();
                return true;
            }

            // Try backup codes
            if (backupCodes != null && !backupCodes.isEmpty()) {
                String[] codes = backupCodes.split(",");
                for (int i = 0; i < codes.length; i++) {
                    if (codes[i].equals(code)) {
                        // Consume backup code (replace with empty)
                        codes[i] = "";
                        String updatedCodes = String.join(",", codes);
                        PreparedStatement update = conn.prepareStatement(
                                "UPDATE mfa_secrets SET backup_codes = ?, last_used_at = datetime('now') WHERE user_id = ?");
                        update.setString(1, updatedCodes);
                        update.setString(2, userId);
                        update.executeUpdate();
                        logger.info("Backup code used for user: {}", userId);
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            logger.error("MFA validation failed for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Check if MFA is enabled for a user
     */
    public boolean isMFAEnabled(String userId) {
        try (Connection conn = dbManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT is_enabled FROM mfa_secrets WHERE user_id = ?");
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt("is_enabled") == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Disable MFA for a user (admin action)
     */
    public boolean disableMFA(String userId) {
        try (Connection conn = dbManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE mfa_secrets SET is_enabled = 0, updated_at = datetime('now') WHERE user_id = ?");
            stmt.setString(1, userId);
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                logger.info("MFA disabled for user: {}", userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to disable MFA for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Get MFA status for a user
     */
    public Map<String, Object> getMFAStatus(String userId) {
        Map<String, Object> status = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT is_enabled, enrolled_at, last_used_at FROM mfa_secrets WHERE user_id = ?");
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                status.put("enabled", rs.getInt("is_enabled") == 1);
                status.put("enrolledAt", rs.getString("enrolled_at"));
                status.put("lastUsedAt", rs.getString("last_used_at"));
            } else {
                status.put("enabled", false);
                status.put("enrolledAt", null);
                status.put("lastUsedAt", null);
            }
        } catch (Exception e) {
            status.put("enabled", false);
            status.put("error", e.getMessage());
        }
        return status;
    }
}
