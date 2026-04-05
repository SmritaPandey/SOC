package com.qsdpdp.licensing;

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
 * Licensing Service — manages license lifecycle, activation, validation,
 * pricing tiers, agreements, and usage tracking.
 * Admin portal for NeurQ AI Labs only.
 *
 * @version 1.0.0
 * @since Sprint 6
 */
@Service
public class LicensingService {

    private static final Logger logger = LoggerFactory.getLogger(LicensingService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final LicenseValidator licenseValidator;

    private boolean initialized = false;
    private License currentLicense;

    @Autowired
    public LicensingService(DatabaseManager dbManager, AuditService auditService,
                            LicenseValidator licenseValidator) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.licenseValidator = licenseValidator;
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Licensing Service...");
        createTables();
        loadCurrentLicense();
        initialized = true;
        logger.info("Licensing Service initialized — mode: {}", currentLicense.getType());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS licenses (
                    id TEXT PRIMARY KEY,
                    license_key TEXT UNIQUE,
                    type TEXT NOT NULL DEFAULT 'DEMO',
                    status TEXT NOT NULL DEFAULT 'DEMO',
                    organization_name TEXT,
                    organization_id TEXT,
                    contact_email TEXT,
                    activated_at TIMESTAMP,
                    expires_at TIMESTAMP,
                    max_users INTEGER DEFAULT 5,
                    current_users INTEGER DEFAULT 0,
                    features TEXT,
                    hardware_fingerprint TEXT,
                    signature TEXT,
                    agreement_id TEXT,
                    activated_by TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS license_agreements (
                    id TEXT PRIMARY KEY,
                    license_id TEXT,
                    agreement_type TEXT NOT NULL,
                    organization_name TEXT,
                    signed_by TEXT,
                    signed_at TIMESTAMP,
                    document TEXT,
                    status TEXT DEFAULT 'PENDING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (license_id) REFERENCES licenses(id)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS license_history (
                    id TEXT PRIMARY KEY,
                    license_id TEXT NOT NULL,
                    action TEXT NOT NULL,
                    details TEXT,
                    performed_by TEXT,
                    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (license_id) REFERENCES licenses(id)
                )
            """);

            logger.info("Licensing tables created");
        } catch (SQLException e) {
            logger.error("Failed to create licensing tables", e);
        }
    }

    private void loadCurrentLicense() {
        String sql = "SELECT * FROM licenses ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                currentLicense = mapLicense(rs);
                // Auto-expire
                if (currentLicense.isExpired() && currentLicense.getStatus() != License.LicenseStatus.EXPIRED) {
                    currentLicense.setStatus(License.LicenseStatus.EXPIRED);
                    updateLicense(currentLicense);
                }
            } else {
                // Create default DEMO license (14 days)
                currentLicense = new License();
                currentLicense.setType(License.LicenseType.DEMO);
                currentLicense.setStatus(License.LicenseStatus.DEMO);
                currentLicense.setOrganizationName("Demo Organization");
                currentLicense.setExpiresAt(LocalDateTime.now().plusDays(14));
                currentLicense.setFeatures("{\"modules\":[\"core\",\"consent\",\"policy\",\"breach\",\"dpia\",\"rights\",\"gap\",\"audit\",\"reporting\"]}");
                persistLicense(currentLicense);
            }
        } catch (SQLException e) {
            logger.error("Failed to load license", e);
            currentLicense = new License(); // Fallback to DEMO
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HARDWARE FINGERPRINT
    // ═══════════════════════════════════════════════════════════

    /**
     * Get the hardware fingerprint of the current machine.
     */
    public Map<String, Object> getHardwareFingerprint() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fingerprint", HardwareFingerprint.generate());
        result.put("components", HardwareFingerprint.getComponents());
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // ACTIVATION & VALIDATION
    // ═══════════════════════════════════════════════════════════

    public License activate(String licenseKey, String orgName, String email, String activatedBy) {
        // Validate license key format: QSDPDP-XXXX-XXXX-XXXX-XXXX
        if (!isValidKeyFormat(licenseKey)) {
            throw new IllegalArgumentException("Invalid license key format. Expected: QSDPDP-XXXX-XXXX-XXXX-XXXX");
        }

        License.LicenseType type = detectLicenseType(licenseKey);
        int maxUsers = getMaxUsersForType(type);
        int validDays = getValidDaysForType(type);

        License license = new License();
        license.setLicenseKey(licenseKey);
        license.setType(type);
        license.setStatus(License.LicenseStatus.ACTIVE);
        license.setOrganizationName(orgName);
        license.setContactEmail(email);
        license.setActivatedAt(LocalDateTime.now());
        license.setExpiresAt(LocalDateTime.now().plusDays(validDays));
        license.setMaxUsers(maxUsers);
        license.setActivatedBy(activatedBy);
        license.setFeatures(getFeaturesForType(type));

        persistLicense(license);
        recordHistory(license.getId(), "ACTIVATED", "License activated: " + type, activatedBy);
        currentLicense = license;

        auditService.log("LICENSE_ACTIVATED", "LICENSING", activatedBy,
                String.format("License activated: %s (%s) for %s", type, licenseKey, orgName));

        return license;
    }

    /**
     * Activate license using a signed license file (hardware-bound).
     */
    public License activateWithLicenseFile(String licenseFileContent, String activatedBy) {
        LicenseValidator.ValidationResult result = licenseValidator.validate(licenseFileContent, true);

        if (!result.isValid()) {
            throw new IllegalArgumentException("License file validation failed: " + result.getError());
        }

        Map<String, String> data = result.getLicenseData();

        License license = new License();
        license.setLicenseKey("FILE-" + data.getOrDefault("licenseId", UUID.randomUUID().toString()).substring(0, 8).toUpperCase());
        license.setType(License.LicenseType.valueOf(data.getOrDefault("licenseType", "STANDARD")));
        license.setStatus(License.LicenseStatus.ACTIVE);
        license.setOrganizationName(data.getOrDefault("organizationName", ""));
        license.setOrganizationId(data.getOrDefault("organizationId", ""));
        license.setContactEmail(data.getOrDefault("contactEmail", ""));
        license.setActivatedAt(LocalDateTime.now());
        license.setHardwareFingerprint(HardwareFingerprint.generate());
        license.setSignature(licenseFileContent.split("\\.")[1]); // Store signature portion

        String expiresAt = data.get("expiresAt");
        if (expiresAt != null) {
            license.setExpiresAt(LocalDateTime.parse(expiresAt));
        } else {
            license.setExpiresAt(LocalDateTime.now().plusDays(365));
        }

        try {
            license.setMaxUsers(Integer.parseInt(data.getOrDefault("maxUsers", "25")));
        } catch (NumberFormatException e) {
            license.setMaxUsers(25);
        }

        String modules = data.getOrDefault("modules", "");
        license.setFeatures("{\"modules\":[" +
                String.join(",", java.util.Arrays.stream(modules.split(","))
                        .map(m -> "\"" + m.trim() + "\"")
                        .toArray(String[]::new)) + "]}");

        license.setActivatedBy(activatedBy);

        persistLicense(license);
        recordHistory(license.getId(), "ACTIVATED_FILE",
                "License activated via signed file: " + license.getType() + " (hardware-bound)", activatedBy);
        currentLicense = license;

        auditService.log("LICENSE_FILE_ACTIVATED", "LICENSING", activatedBy,
                String.format("License file activated: %s for %s (hardware-bound)",
                        license.getType(), license.getOrganizationName()));

        return license;
    }

    public void deactivate(String deactivatedBy) {
        if (currentLicense != null) {
            currentLicense.setStatus(License.LicenseStatus.DEMO);
            currentLicense.setType(License.LicenseType.DEMO);
            currentLicense.setExpiresAt(LocalDateTime.now().plusDays(14));
            currentLicense.setMaxUsers(5);
            currentLicense.setFeatures("{\"modules\":[\"core\",\"consent\",\"policy\"]}");
            updateLicense(currentLicense);
            recordHistory(currentLicense.getId(), "DEACTIVATED", "Reverted to demo mode", deactivatedBy);

            auditService.log("LICENSE_DEACTIVATED", "LICENSING", deactivatedBy, "License deactivated — reverted to demo");
        }
    }

    public boolean isValidKeyFormat(String key) {
        return key != null && key.matches("QSDPDP-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}");
    }

    private License.LicenseType detectLicenseType(String key) {
        if (key.startsWith("QSDPDP-ENT")) return License.LicenseType.ENTERPRISE;
        if (key.startsWith("QSDPDP-PRO")) return License.LicenseType.PROFESSIONAL;
        if (key.startsWith("QSDPDP-STD")) return License.LicenseType.STANDARD;
        return License.LicenseType.STANDARD;
    }

    private int getMaxUsersForType(License.LicenseType type) {
        return switch (type) {
            case DEMO -> 5;
            case STANDARD -> 25;
            case PROFESSIONAL -> 100;
            case ENTERPRISE -> 999;
        };
    }

    private int getValidDaysForType(License.LicenseType type) {
        return switch (type) {
            case DEMO -> 14;
            case STANDARD -> 365;
            case PROFESSIONAL -> 365;
            case ENTERPRISE -> 730;
        };
    }

    private String getFeaturesForType(License.LicenseType type) {
        return switch (type) {
            case DEMO -> "{\"modules\":[\"core\",\"consent\",\"policy\",\"breach\",\"dpia\",\"rights\",\"gap\",\"audit\",\"reporting\"]}";
            case STANDARD -> "{\"modules\":[\"core\",\"consent\",\"policy\",\"breach\",\"dpia\",\"rights\",\"gap\",\"audit\",\"reporting\",\"settings\",\"chatbot\"]}";
            case PROFESSIONAL -> "{\"modules\":[\"core\",\"consent\",\"policy\",\"breach\",\"dpia\",\"rights\",\"gap\",\"audit\",\"reporting\",\"settings\",\"chatbot\",\"siem\",\"dlp\",\"pii\"]}";
            case ENTERPRISE -> "{\"modules\":[\"core\",\"consent\",\"policy\",\"breach\",\"dpia\",\"rights\",\"gap\",\"audit\",\"reporting\",\"settings\",\"chatbot\",\"siem\",\"dlp\",\"pii\",\"vendor\",\"governance\",\"lifecycle\",\"crossborder\",\"training\",\"api\"]}";
        };
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════

    public License getCurrentLicense() {
        return currentLicense;
    }

    public List<Map<String, Object>> getPricingTiers() {
        List<Map<String, Object>> tiers = new ArrayList<>();
        for (License.LicenseType type : License.LicenseType.values()) {
            Map<String, Object> tier = new LinkedHashMap<>();
            tier.put("type", type.name());
            tier.put("maxUsers", getMaxUsersForType(type));
            tier.put("validDays", getValidDaysForType(type));
            tier.put("features", getFeaturesForType(type));
            tier.put("price", getPriceForType(type));
            tiers.add(tier);
        }
        return tiers;
    }

    private String getPriceForType(License.LicenseType type) {
        return switch (type) {
            case DEMO -> "Free (14 days)";
            case STANDARD -> "₹1,50,000/year";
            case PROFESSIONAL -> "₹4,50,000/year";
            case ENTERPRISE -> "Contact Sales";
        };
    }

    public Map<String, Object> getUsageStats() {
        Map<String, Object> usage = new LinkedHashMap<>();
        if (currentLicense != null) {
            usage.put("maxUsers", currentLicense.getMaxUsers());
            usage.put("currentUsers", currentLicense.getCurrentUsers());
            usage.put("remainingDays", currentLicense.getRemainingDays());
            usage.put("isExpired", currentLicense.isExpired());
            usage.put("licenseType", currentLicense.getType().name());
        }

        // Count module usage from DB
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'");
            if (rs.next()) usage.put("activeUsers", rs.getInt(1));
        } catch (SQLException e) {
            usage.put("activeUsers", 0);
        }

        return usage;
    }

    // ═══════════════════════════════════════════════════════════
    // AGREEMENTS
    // ═══════════════════════════════════════════════════════════

    public List<Map<String, Object>> getAgreements() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM license_agreements ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getString("id"));
                m.put("licenseId", rs.getString("license_id"));
                m.put("agreementType", rs.getString("agreement_type"));
                m.put("organizationName", rs.getString("organization_name"));
                m.put("signedBy", rs.getString("signed_by"));
                m.put("signedAt", rs.getString("signed_at"));
                m.put("status", rs.getString("status"));
                m.put("createdAt", rs.getString("created_at"));
                list.add(m);
            }
        } catch (SQLException e) {
            logger.error("Failed to get agreements", e);
        }
        return list;
    }

    public Map<String, Object> getAgreement(String id) {
        String sql = "SELECT * FROM license_agreements WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getString("id"));
                m.put("licenseId", rs.getString("license_id"));
                m.put("agreementType", rs.getString("agreement_type"));
                m.put("organizationName", rs.getString("organization_name"));
                m.put("signedBy", rs.getString("signed_by"));
                m.put("signedAt", rs.getString("signed_at"));
                m.put("document", rs.getString("document"));
                m.put("status", rs.getString("status"));
                m.put("createdAt", rs.getString("created_at"));
                return m;
            }
        } catch (SQLException e) {
            logger.error("Failed to get agreement: {}", id, e);
        }
        return null;
    }

    public String createAgreement(String agreementType, String orgName, String signedBy, String document) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT INTO license_agreements (id, license_id, agreement_type, organization_name,
                signed_by, signed_at, document, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'SIGNED')
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, currentLicense != null ? currentLicense.getId() : null);
            stmt.setString(3, agreementType);
            stmt.setString(4, orgName);
            stmt.setString(5, signedBy);
            stmt.setString(6, LocalDateTime.now().toString());
            stmt.setString(7, document);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to create agreement", e);
        }
        return id;
    }

    // ═══════════════════════════════════════════════════════════
    // HISTORY
    // ═══════════════════════════════════════════════════════════

    public List<Map<String, Object>> getHistory() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT * FROM license_history ORDER BY performed_at DESC LIMIT 100";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getString("id"));
                m.put("licenseId", rs.getString("license_id"));
                m.put("action", rs.getString("action"));
                m.put("details", rs.getString("details"));
                m.put("performedBy", rs.getString("performed_by"));
                m.put("performedAt", rs.getString("performed_at"));
                list.add(m);
            }
        } catch (SQLException e) {
            logger.error("Failed to get history", e);
        }
        return list;
    }

    private void recordHistory(String licenseId, String action, String details, String performedBy) {
        String sql = "INSERT INTO license_history (id, license_id, action, details, performed_by) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, licenseId);
            stmt.setString(3, action);
            stmt.setString(4, details);
            stmt.setString(5, performedBy);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to record history", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    private void persistLicense(License license) {
        String sql = """
            INSERT OR REPLACE INTO licenses (id, license_key, type, status, organization_name,
                organization_id, contact_email, activated_at, expires_at, max_users, current_users,
                features, hardware_fingerprint, signature, agreement_id, activated_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, license.getId());
            stmt.setString(2, license.getLicenseKey());
            stmt.setString(3, license.getType().name());
            stmt.setString(4, license.getStatus().name());
            stmt.setString(5, license.getOrganizationName());
            stmt.setString(6, license.getOrganizationId());
            stmt.setString(7, license.getContactEmail());
            stmt.setString(8, license.getActivatedAt() != null ? license.getActivatedAt().toString() : null);
            stmt.setString(9, license.getExpiresAt() != null ? license.getExpiresAt().toString() : null);
            stmt.setInt(10, license.getMaxUsers());
            stmt.setInt(11, license.getCurrentUsers());
            stmt.setString(12, license.getFeatures());
            stmt.setString(13, license.getHardwareFingerprint());
            stmt.setString(14, license.getSignature());
            stmt.setString(15, license.getAgreementId());
            stmt.setString(16, license.getActivatedBy());
            stmt.setString(17, license.getCreatedAt().toString());
            stmt.setString(18, LocalDateTime.now().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to persist license", e);
        }
    }

    private void updateLicense(License license) {
        license.setUpdatedAt(LocalDateTime.now());
        persistLicense(license);
    }

    private License mapLicense(ResultSet rs) throws SQLException {
        License l = new License();
        l.setId(rs.getString("id"));
        l.setLicenseKey(rs.getString("license_key"));
        l.setType(License.LicenseType.valueOf(rs.getString("type")));
        l.setStatus(License.LicenseStatus.valueOf(rs.getString("status")));
        l.setOrganizationName(rs.getString("organization_name"));
        l.setOrganizationId(rs.getString("organization_id"));
        l.setContactEmail(rs.getString("contact_email"));
        String activated = rs.getString("activated_at");
        if (activated != null) l.setActivatedAt(LocalDateTime.parse(activated));
        String expires = rs.getString("expires_at");
        if (expires != null) l.setExpiresAt(LocalDateTime.parse(expires));
        l.setMaxUsers(rs.getInt("max_users"));
        l.setCurrentUsers(rs.getInt("current_users"));
        l.setFeatures(rs.getString("features"));
        l.setAgreementId(rs.getString("agreement_id"));
        l.setActivatedBy(rs.getString("activated_by"));
        return l;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
