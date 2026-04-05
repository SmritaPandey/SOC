package com.qsdpdp.web;

import com.qsdpdp.audit.AuditService;
import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.db.SectorDataSeeder;
import com.qsdpdp.db.SectorDataSeeder.Sector;
import com.qsdpdp.i18n.I18nService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

/**
 * Settings Controller for QS-DPDP Enterprise
 * Manages Organization, Hierarchy, Employees, Language, Sector, and Database settings.
 * Full CRUD with audit logging and DPDP role mapping.
 *
 * @version 2.0.0
 * @since Sprint 1
 */

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @Autowired
    private DatabaseManager dbManager;

    @Autowired
    private SectorDataSeeder sectorDataSeeder;

    @Autowired
    private AuditService auditService;

    @Autowired
    private I18nService i18nService;

    // ═══════════════════════════════════════════════════════════
    // GENERAL SETTINGS
    // ═══════════════════════════════════════════════════════════

    @GetMapping
    public ResponseEntity<?> getSettings() {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT key, value, category, description FROM settings ORDER BY category, key")) {
            ResultSet rs = stmt.executeQuery();
            List<Map<String, String>> settings = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> setting = new LinkedHashMap<>();
                setting.put("key", rs.getString("key"));
                setting.put("value", rs.getString("value"));
                setting.put("category", rs.getString("category"));
                setting.put("description", rs.getString("description"));
                settings.add(setting);
            }
            return ResponseEntity.ok(Map.of("settings", settings));
        } catch (SQLException e) {
            logger.error("Failed to load settings", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load settings"));
        }
    }

    @PutMapping
    public ResponseEntity<?> updateSetting(@RequestBody Map<String, String> payload) {
        String key = payload.get("key");
        String value = payload.get("value");
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Key is required"));
        }
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE settings SET value = ?, updated_at = CURRENT_TIMESTAMP WHERE key = ?")) {
            stmt.setString(1, value);
            stmt.setString(2, key);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                return ResponseEntity.notFound().build();
            }
            auditService.log("SETTING_UPDATED", "SETTINGS", "admin",
                    "Setting '" + key + "' updated to '" + value + "'");
            return ResponseEntity.ok(Map.of("status", "updated", "key", key, "value", value));
        } catch (SQLException e) {
            logger.error("Failed to update setting: {}", key, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update setting"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ORGANIZATION CRUD
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/organization")
    public ResponseEntity<?> getOrganization() {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM organizations LIMIT 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return ResponseEntity.ok(Map.of("organization", resultSetToMap(rs)));
            }
            return ResponseEntity.ok(Map.of("organization", Map.of()));
        } catch (SQLException e) {
            logger.error("Failed to load organization", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load organization"));
        }
    }

    @PostMapping("/organization")
    public ResponseEntity<?> createOrganization(@RequestBody Map<String, String> payload) {
        String id = UUID.randomUUID().toString();
        String sql = """
                INSERT INTO organizations (id, name, legal_name, sector, industry,
                    address_line1, address_line2, city, state, pin_code, country,
                    phone, alternate_phone, email, alternate_email, website,
                    gst_number, pan_number, cin_number, duns_number, incorporation_date,
                    employee_count, annual_turnover, data_fiduciary_registered,
                    dpo_name, dpo_email, dpo_phone,
                    consent_manager_name, consent_manager_email,
                    grievance_officer_name, grievance_officer_email,
                    dpbi_registration_number, is_significant_data_fiduciary)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, payload.getOrDefault("name", ""));
            stmt.setString(3, payload.getOrDefault("legalName", ""));
            stmt.setString(4, payload.getOrDefault("sector", ""));
            stmt.setString(5, payload.getOrDefault("industry", ""));
            stmt.setString(6, payload.getOrDefault("addressLine1", ""));
            stmt.setString(7, payload.getOrDefault("addressLine2", ""));
            stmt.setString(8, payload.getOrDefault("city", ""));
            stmt.setString(9, payload.getOrDefault("state", ""));
            stmt.setString(10, payload.getOrDefault("pinCode", ""));
            stmt.setString(11, payload.getOrDefault("country", "India"));
            stmt.setString(12, payload.getOrDefault("phone", ""));
            stmt.setString(13, payload.getOrDefault("alternatePhone", ""));
            stmt.setString(14, payload.getOrDefault("email", ""));
            stmt.setString(15, payload.getOrDefault("alternateEmail", ""));
            stmt.setString(16, payload.getOrDefault("website", ""));
            stmt.setString(17, payload.getOrDefault("gstNumber", ""));
            stmt.setString(18, payload.getOrDefault("panNumber", ""));
            stmt.setString(19, payload.getOrDefault("cinNumber", ""));
            stmt.setString(20, payload.getOrDefault("dunsNumber", ""));
            stmt.setString(21, payload.getOrDefault("incorporationDate", ""));
            stmt.setString(22, payload.getOrDefault("employeeCount", "0"));
            stmt.setString(23, payload.getOrDefault("annualTurnover", ""));
            stmt.setInt(24, "true".equalsIgnoreCase(payload.getOrDefault("dataFiduciaryRegistered", "false")) ? 1 : 0);
            stmt.setString(25, payload.getOrDefault("dpoName", ""));
            stmt.setString(26, payload.getOrDefault("dpoEmail", ""));
            stmt.setString(27, payload.getOrDefault("dpoPhone", ""));
            stmt.setString(28, payload.getOrDefault("consentManagerName", ""));
            stmt.setString(29, payload.getOrDefault("consentManagerEmail", ""));
            stmt.setString(30, payload.getOrDefault("grievanceOfficerName", ""));
            stmt.setString(31, payload.getOrDefault("grievanceOfficerEmail", ""));
            stmt.setString(32, payload.getOrDefault("dpbiRegistrationNumber", ""));
            stmt.setInt(33, "true".equalsIgnoreCase(payload.getOrDefault("isSignificantDataFiduciary", "false")) ? 1 : 0);
            stmt.executeUpdate();

            auditService.log("ORG_CREATED", "SETTINGS", "admin",
                    "Organization created: " + payload.getOrDefault("name", ""));
            return ResponseEntity.ok(Map.of("status", "created", "id", id));
        } catch (SQLException e) {
            logger.error("Failed to create organization", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create organization"));
        }
    }

    @PutMapping("/organization/{id}")
    public ResponseEntity<?> updateOrganization(@PathVariable String id, @RequestBody Map<String, String> payload) {
        StringBuilder sql = new StringBuilder("UPDATE organizations SET updated_at = CURRENT_TIMESTAMP");
        List<String> values = new ArrayList<>();
        String[] fields = { "name", "legal_name", "sector", "industry", "address_line1", "address_line2",
                "city", "state", "pin_code", "country", "phone", "alternate_phone", "email",
                "alternate_email", "website", "gst_number", "pan_number", "cin_number", "duns_number",
                "incorporation_date", "employee_count", "annual_turnover",
                "dpo_name", "dpo_email", "dpo_phone", "consent_manager_name", "consent_manager_email",
                "grievance_officer_name", "grievance_officer_email", "dpbi_registration_number" };
        String[] keys = { "name", "legalName", "sector", "industry", "addressLine1", "addressLine2",
                "city", "state", "pinCode", "country", "phone", "alternatePhone", "email",
                "alternateEmail", "website", "gstNumber", "panNumber", "cinNumber", "dunsNumber",
                "incorporationDate", "employeeCount", "annualTurnover",
                "dpoName", "dpoEmail", "dpoPhone", "consentManagerName", "consentManagerEmail",
                "grievanceOfficerName", "grievanceOfficerEmail", "dpbiRegistrationNumber" };

        for (int i = 0; i < fields.length; i++) {
            if (payload.containsKey(keys[i])) {
                sql.append(", ").append(fields[i]).append(" = ?");
                values.add(payload.get(keys[i]));
            }
        }
        sql.append(" WHERE id = ?");
        values.add(id);

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setString(i + 1, values.get(i));
            }
            int rows = stmt.executeUpdate();
            if (rows == 0) return ResponseEntity.notFound().build();

            auditService.log("ORG_UPDATED", "SETTINGS", "admin", "Organization updated: " + id);
            return ResponseEntity.ok(Map.of("status", "updated", "id", id));
        } catch (SQLException e) {
            logger.error("Failed to update organization", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update organization"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HIERARCHY CRUD
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/hierarchy")
    public ResponseEntity<?> getHierarchy() {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM org_hierarchy_levels ORDER BY level_number ASC")) {
            ResultSet rs = stmt.executeQuery();
            List<Map<String, Object>> levels = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> level = new LinkedHashMap<>();
                level.put("id", rs.getString("id"));
                level.put("orgId", rs.getString("org_id"));
                level.put("levelNumber", rs.getInt("level_number"));
                level.put("levelCode", rs.getString("level_code"));
                level.put("levelName", rs.getString("level_name"));
                level.put("description", rs.getString("description"));
                level.put("dpdpRoleMapping", rs.getString("dpdp_role_mapping"));
                level.put("canApproveConsent", rs.getInt("can_approve_consent") == 1);
                level.put("canApproveBreach", rs.getInt("can_approve_breach") == 1);
                level.put("canApproveDpia", rs.getInt("can_approve_dpia") == 1);
                level.put("canViewReports", rs.getInt("can_view_reports") == 1);
                level.put("canManagePolicy", rs.getInt("can_manage_policy") == 1);
                level.put("maxPositions", rs.getInt("max_positions"));
                level.put("isActive", rs.getInt("is_active") == 1);
                levels.add(level);
            }
            return ResponseEntity.ok(Map.of("hierarchy", levels));
        } catch (SQLException e) {
            logger.error("Failed to load hierarchy", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load hierarchy"));
        }
    }

    @PostMapping("/hierarchy")
    public ResponseEntity<?> createHierarchyLevel(@RequestBody Map<String, Object> payload) {
        String id = UUID.randomUUID().toString();
        String sql = """
                INSERT INTO org_hierarchy_levels (id, org_id, level_number, level_code, level_name,
                    description, dpdp_role_mapping, can_approve_consent, can_approve_breach,
                    can_approve_dpia, can_view_reports, can_manage_policy, max_positions, sort_order)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, getString(payload, "orgId", "default"));
            stmt.setInt(3, getInt(payload, "levelNumber", 0));
            stmt.setString(4, getString(payload, "levelCode", ""));
            stmt.setString(5, getString(payload, "levelName", ""));
            stmt.setString(6, getString(payload, "description", ""));
            stmt.setString(7, getString(payload, "dpdpRoleMapping", ""));
            stmt.setInt(8, getBool(payload, "canApproveConsent") ? 1 : 0);
            stmt.setInt(9, getBool(payload, "canApproveBreach") ? 1 : 0);
            stmt.setInt(10, getBool(payload, "canApproveDpia") ? 1 : 0);
            stmt.setInt(11, getBool(payload, "canViewReports") ? 1 : 0);
            stmt.setInt(12, getBool(payload, "canManagePolicy") ? 1 : 0);
            stmt.setInt(13, getInt(payload, "maxPositions", 0));
            stmt.setInt(14, getInt(payload, "levelNumber", 0));
            stmt.executeUpdate();

            auditService.log("HIERARCHY_CREATED", "SETTINGS", "admin",
                    "Hierarchy level created: " + getString(payload, "levelName", ""));
            return ResponseEntity.ok(Map.of("status", "created", "id", id));
        } catch (SQLException e) {
            logger.error("Failed to create hierarchy level", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create hierarchy level: " + e.getMessage()));
        }
    }

    @PutMapping("/hierarchy/{id}")
    public ResponseEntity<?> updateHierarchyLevel(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        String sql = """
                UPDATE org_hierarchy_levels SET level_name = ?, level_code = ?, description = ?,
                    dpdp_role_mapping = ?, can_approve_consent = ?, can_approve_breach = ?,
                    can_approve_dpia = ?, can_view_reports = ?, can_manage_policy = ?,
                    max_positions = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getString(payload, "levelName", ""));
            stmt.setString(2, getString(payload, "levelCode", ""));
            stmt.setString(3, getString(payload, "description", ""));
            stmt.setString(4, getString(payload, "dpdpRoleMapping", ""));
            stmt.setInt(5, getBool(payload, "canApproveConsent") ? 1 : 0);
            stmt.setInt(6, getBool(payload, "canApproveBreach") ? 1 : 0);
            stmt.setInt(7, getBool(payload, "canApproveDpia") ? 1 : 0);
            stmt.setInt(8, getBool(payload, "canViewReports") ? 1 : 0);
            stmt.setInt(9, getBool(payload, "canManagePolicy") ? 1 : 0);
            stmt.setInt(10, getInt(payload, "maxPositions", 0));
            stmt.setString(11, id);
            int rows = stmt.executeUpdate();
            if (rows == 0) return ResponseEntity.notFound().build();

            auditService.log("HIERARCHY_UPDATED", "SETTINGS", "admin", "Hierarchy level updated: " + id);
            return ResponseEntity.ok(Map.of("status", "updated", "id", id));
        } catch (SQLException e) {
            logger.error("Failed to update hierarchy level", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update hierarchy level"));
        }
    }

    @DeleteMapping("/hierarchy/{id}")
    public ResponseEntity<?> deleteHierarchyLevel(@PathVariable String id) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM org_hierarchy_levels WHERE id = ?")) {
            stmt.setString(1, id);
            int rows = stmt.executeUpdate();
            if (rows == 0) return ResponseEntity.notFound().build();

            auditService.log("HIERARCHY_DELETED", "SETTINGS", "admin", "Hierarchy level deleted: " + id);
            return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
        } catch (SQLException e) {
            logger.error("Failed to delete hierarchy level", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete hierarchy level"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // EMPLOYEE CRUD
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/employees")
    public ResponseEntity<?> getEmployees(@RequestParam(required = false) String orgId,
                                           @RequestParam(required = false) String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT e.*, h.level_name, h.level_code FROM employees e " +
                "LEFT JOIN org_hierarchy_levels h ON e.hierarchy_level_id = h.id WHERE 1=1");
        List<String> params = new ArrayList<>();
        if (orgId != null && !orgId.isBlank()) {
            sql.append(" AND e.org_id = ?");
            params.add(orgId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND e.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY e.full_name");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            List<Map<String, Object>> employees = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> emp = new LinkedHashMap<>();
                emp.put("id", rs.getString("id"));
                emp.put("orgId", rs.getString("org_id"));
                emp.put("employeeCode", rs.getString("employee_code"));
                emp.put("fullName", rs.getString("full_name"));
                emp.put("designation", rs.getString("designation"));
                emp.put("department", rs.getString("department"));
                emp.put("hierarchyLevelId", rs.getString("hierarchy_level_id"));
                emp.put("hierarchyLevelName", rs.getString("level_name"));
                emp.put("hierarchyLevelCode", rs.getString("level_code"));
                emp.put("reportingToId", rs.getString("reporting_to_id"));
                emp.put("email", rs.getString("email"));
                emp.put("phone", rs.getString("phone"));
                emp.put("dpdpRole", rs.getString("dpdp_role"));
                emp.put("isDpo", rs.getInt("is_dpo") == 1);
                emp.put("isConsentManager", rs.getInt("is_consent_manager") == 1);
                emp.put("isGrievanceOfficer", rs.getInt("is_grievance_officer") == 1);
                emp.put("isDataFiduciary", rs.getInt("is_data_fiduciary") == 1);
                emp.put("dateOfJoining", rs.getString("date_of_joining"));
                emp.put("status", rs.getString("status"));
                employees.add(emp);
            }
            return ResponseEntity.ok(Map.of("employees", employees, "total", employees.size()));
        } catch (SQLException e) {
            logger.error("Failed to load employees", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load employees"));
        }
    }

    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@RequestBody Map<String, Object> payload) {
        String id = UUID.randomUUID().toString();
        String sql = """
                INSERT INTO employees (id, org_id, employee_code, full_name, designation, department,
                    hierarchy_level_id, reporting_to_id, email, phone, dpdp_role,
                    is_dpo, is_consent_manager, is_grievance_officer, is_data_fiduciary,
                    date_of_joining, status)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.setString(2, getString(payload, "orgId", "default"));
            stmt.setString(3, getString(payload, "employeeCode", ""));
            stmt.setString(4, getString(payload, "fullName", ""));
            stmt.setString(5, getString(payload, "designation", ""));
            stmt.setString(6, getString(payload, "department", ""));
            stmt.setString(7, getString(payload, "hierarchyLevelId", null));
            stmt.setString(8, getString(payload, "reportingToId", null));
            stmt.setString(9, getString(payload, "email", ""));
            stmt.setString(10, getString(payload, "phone", ""));
            stmt.setString(11, getString(payload, "dpdpRole", ""));
            stmt.setInt(12, getBool(payload, "isDpo") ? 1 : 0);
            stmt.setInt(13, getBool(payload, "isConsentManager") ? 1 : 0);
            stmt.setInt(14, getBool(payload, "isGrievanceOfficer") ? 1 : 0);
            stmt.setInt(15, getBool(payload, "isDataFiduciary") ? 1 : 0);
            stmt.setString(16, getString(payload, "dateOfJoining", ""));
            stmt.setString(17, getString(payload, "status", "ACTIVE"));
            stmt.executeUpdate();

            auditService.log("EMPLOYEE_CREATED", "SETTINGS", "admin",
                    "Employee created: " + getString(payload, "fullName", ""));
            return ResponseEntity.ok(Map.of("status", "created", "id", id));
        } catch (SQLException e) {
            logger.error("Failed to create employee", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create employee"));
        }
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        String sql = """
                UPDATE employees SET full_name = ?, designation = ?, department = ?,
                    hierarchy_level_id = ?, reporting_to_id = ?, email = ?, phone = ?,
                    dpdp_role = ?, is_dpo = ?, is_consent_manager = ?, is_grievance_officer = ?,
                    is_data_fiduciary = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, getString(payload, "fullName", ""));
            stmt.setString(2, getString(payload, "designation", ""));
            stmt.setString(3, getString(payload, "department", ""));
            stmt.setString(4, getString(payload, "hierarchyLevelId", null));
            stmt.setString(5, getString(payload, "reportingToId", null));
            stmt.setString(6, getString(payload, "email", ""));
            stmt.setString(7, getString(payload, "phone", ""));
            stmt.setString(8, getString(payload, "dpdpRole", ""));
            stmt.setInt(9, getBool(payload, "isDpo") ? 1 : 0);
            stmt.setInt(10, getBool(payload, "isConsentManager") ? 1 : 0);
            stmt.setInt(11, getBool(payload, "isGrievanceOfficer") ? 1 : 0);
            stmt.setInt(12, getBool(payload, "isDataFiduciary") ? 1 : 0);
            stmt.setString(13, getString(payload, "status", "ACTIVE"));
            stmt.setString(14, id);
            int rows = stmt.executeUpdate();
            if (rows == 0) return ResponseEntity.notFound().build();

            auditService.log("EMPLOYEE_UPDATED", "SETTINGS", "admin", "Employee updated: " + id);
            return ResponseEntity.ok(Map.of("status", "updated", "id", id));
        } catch (SQLException e) {
            logger.error("Failed to update employee", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update employee"));
        }
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable String id) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE employees SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            stmt.setString(1, id);
            int rows = stmt.executeUpdate();
            if (rows == 0) return ResponseEntity.notFound().build();

            auditService.log("EMPLOYEE_DEACTIVATED", "SETTINGS", "admin", "Employee deactivated: " + id);
            return ResponseEntity.ok(Map.of("status", "deactivated", "id", id));
        } catch (SQLException e) {
            logger.error("Failed to deactivate employee", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to deactivate employee"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LANGUAGE SETTINGS
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/languages")
    public ResponseEntity<?> getLanguages() {
        return ResponseEntity.ok(Map.of(
                "languages", i18nService.getAvailableLanguages(),
                "currentLanguage", i18nService.getCurrentLanguage(),
                "totalSupported", i18nService.getSupportedLanguageCount()));
    }

    @GetMapping("/languages/translations/{lang}")
    public ResponseEntity<?> getTranslations(@PathVariable String lang) {
        return ResponseEntity.ok(Map.of(
                "language", lang,
                "translations", i18nService.getTranslations(lang)));
    }

    @PutMapping("/languages")
    public ResponseEntity<?> updateLanguageSettings(@RequestBody Map<String, Object> payload) {
        String defaultLang = (String) payload.get("defaultLanguage");
        if (defaultLang != null) {
            i18nService.setCurrentLanguage(defaultLang);
            try {
                upsertSetting("default_language", defaultLang, "preferences", "Default language");
            } catch (SQLException e) {
                logger.error("Failed to save language setting", e);
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> langUpdates = (List<Map<String, Object>>) payload.get("languages");
        if (langUpdates != null) {
            for (Map<String, Object> update : langUpdates) {
                String code = (String) update.get("code");
                Boolean enabled = (Boolean) update.get("enabled");
                if (code != null && enabled != null) {
                    i18nService.setLanguageEnabled(code, enabled);
                }
            }
        }

        auditService.log("LANGUAGE_UPDATED", "SETTINGS", "admin", "Language settings updated");
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    // ═══════════════════════════════════════════════════════════
    // SECTOR MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/sectors")
    public ResponseEntity<?> getSectors() {
        List<Map<String, String>> sectors = new ArrayList<>();
        for (Sector s : Sector.values()) {
            Map<String, String> sector = new LinkedHashMap<>();
            sector.put("id", s.name());
            sector.put("name", s.displayName);
            sectors.add(sector);
        }
        String currentSector = getSettingValue("selected_sector");
        return ResponseEntity.ok(Map.of("sectors", sectors, "currentSector",
                currentSector != null ? currentSector : ""));
    }

    @PostMapping("/select-sector")
    public ResponseEntity<?> selectSector(@RequestBody Map<String, String> payload) {
        String sectorId = payload.get("sector");
        if (sectorId == null || sectorId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sector ID is required"));
        }

        Sector sector;
        try {
            sector = Sector.valueOf(sectorId.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Invalid sector. Choose from: " + java.util.Arrays.stream(Sector.values())
                            .map(Enum::name).collect(java.util.stream.Collectors.joining(", "))));
        }

        logger.info("Selecting sector: {} — clearing old sector data and seeding new...", sector.displayName);

        try {
            clearAllSectorData();
            int count = sectorDataSeeder.seedSector(sector);

            // Also seed default hierarchy for this sector
            int hierarchyCount = seedSectorDefaultHierarchy(sector);

            upsertSetting("selected_sector", sectorId.toUpperCase(), "sector",
                    "Currently selected industry sector");

            auditService.log("SECTOR_SELECTED", "SETTINGS", "admin",
                    "Sector changed to " + sector.displayName + " — " + count + " records + "
                            + hierarchyCount + " hierarchy levels seeded");

            logger.info("✅ Sector {} selected — {} data records + {} hierarchy levels seeded",
                    sector.displayName, count, hierarchyCount);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "sector", sector.displayName,
                    "sectorId", sector.name(),
                    "recordsSeeded", count,
                    "hierarchyLevelsCreated", hierarchyCount,
                    "message", sector.displayName + " demo data loaded successfully (" + count
                            + " records + " + hierarchyCount + " hierarchy levels)."));
        } catch (Exception e) {
            logger.error("Failed to select sector: {}", sectorId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load sector data: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // DATABASE INFO
    // ═══════════════════════════════════════════════════════════

    @GetMapping("/database-info")
    public ResponseEntity<?> getDatabaseInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("dbPath", dbManager.getDbPath());
        info.put("schemaVersion", dbManager.getSchemaVersion());
        info.put("initialized", dbManager.isInitialized());

        // Get table row counts
        Map<String, Integer> tableCounts = new LinkedHashMap<>();
        String[] tables = { "users", "consents", "data_principals", "purposes", "breaches",
                "dpias", "policies", "rights_requests", "gap_assessments", "audit_log",
                "organizations", "org_hierarchy_levels", "employees", "supported_languages" };
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                try {
                    ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table);
                    if (rs.next()) tableCounts.put(table, rs.getInt(1));
                } catch (SQLException ignored) {
                    tableCounts.put(table, -1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get table counts", e);
        }
        info.put("tableCounts", tableCounts);

        return ResponseEntity.ok(Map.of("database", info));
    }

    // ═══════════════════════════════════════════════════════════
    // DATABASE RESET
    // ═══════════════════════════════════════════════════════════

    @PostMapping("/reset-database")
    public ResponseEntity<?> resetDatabase(@RequestBody(required = false) Map<String, String> payload) {
        String confirm = payload != null ? payload.get("confirm") : null;
        if (!"RESET".equals(confirm)) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Send {\"confirm\": \"RESET\"} to confirm database reset. This action is irreversible."));
        }

        logger.warn("⚠️ DATABASE RESET INITIATED by admin");

        try {
            String[] dataTables = {
                    "consents", "data_principals", "purposes",
                    "breaches", "dpias", "rights_requests",
                    "policies", "gap_assessments", "controls",
                    "compliance_scores", "audit_log",
                    "siem_events", "siem_alerts", "soar_playbooks",
                    "dlp_policies", "dlp_incidents",
                    "chat_messages", "chat_sessions",
                    "api_keys", "api_request_logs", "api_webhooks",
                    "iam_users", "iam_sessions", "iam_role_assignments",
                    "employees", "org_hierarchy_levels", "organizations"
            };

            int totalCleared = 0;
            try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
                for (String table : dataTables) {
                    try {
                        int deleted = stmt.executeUpdate("DELETE FROM " + table);
                        totalCleared += deleted;
                        logger.info("  Cleared {} rows from {}", deleted, table);
                    } catch (SQLException e) {
                        logger.debug("Table {} not found, skipping", table);
                    }
                }
                stmt.executeUpdate("UPDATE settings SET value = '' WHERE key = 'selected_sector'");
            }

            logger.info("✅ Database reset complete — {} total records cleared", totalCleared);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "recordsCleared", totalCleared,
                    "message", "Database reset complete. " + totalCleared
                            + " records cleared. Schema and admin account preserved."));
        } catch (Exception e) {
            logger.error("Failed to reset database", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database reset failed: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SECTOR-DEFAULT HIERARCHY SEEDING
    // ═══════════════════════════════════════════════════════════

    private int seedSectorDefaultHierarchy(Sector sector) throws SQLException {
        // Clear existing hierarchy
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM employees");
            stmt.executeUpdate("DELETE FROM org_hierarchy_levels");
        }

        String[][] hierarchy = switch (sector) {
            case BFSI -> new String[][]{
                    {"L0", "Assistant", "Data Entry / Support Staff", ""},
                    {"L1", "Officer", "Branch Officer / Field Staff", ""},
                    {"L2", "Assistant Manager", "Team Lead / Supervisor", ""},
                    {"L3", "Manager", "Branch / Department Manager", "Consent Manager"},
                    {"L4", "Senior Manager", "Regional Manager", ""},
                    {"L5", "Assistant General Manager", "Zonal Head", ""},
                    {"L6", "Deputy General Manager", "Division Head", "Grievance Officer"},
                    {"L7", "General Manager", "Function Head", "Data Protection Officer"},
                    {"L8", "Chief General Manager", "Vertical Head", ""},
                    {"L9", "Executive Director", "Board Member", "Data Fiduciary"},
                    {"L10", "Managing Director & CEO", "Chief Executive", "Significant Data Fiduciary"}
            };
            case HEALTHCARE -> new String[][]{
                    {"L0", "Technician", "Lab / Medical Technician", ""},
                    {"L1", "Nurse / Staff", "Nursing & Support Staff", ""},
                    {"L2", "Resident Doctor", "Junior Doctor", ""},
                    {"L3", "Consultant", "Specialist Doctor", "Consent Manager"},
                    {"L4", "HOD", "Head of Department", ""},
                    {"L5", "Medical Superintendent", "Hospital Admin Head", "Grievance Officer"},
                    {"L6", "Chief Medical Officer", "Chief Medical Officer", "Data Protection Officer"},
                    {"L7", "Director / CEO", "Hospital Director", "Data Fiduciary"}
            };
            case ECOMMERCE -> new String[][]{
                    {"L0", "Associate", "Customer Support / Ops", ""},
                    {"L1", "Senior Associate", "Team Member", ""},
                    {"L2", "Team Lead", "Team Lead", ""},
                    {"L3", "Manager", "Department Manager", "Consent Manager"},
                    {"L4", "Senior Manager", "Senior Manager", ""},
                    {"L5", "Director", "Function Director", "Grievance Officer"},
                    {"L6", "VP", "Vice President", "Data Protection Officer"},
                    {"L7", "SVP / CTO", "Senior VP / CTO", ""},
                    {"L8", "CEO / Founder", "Chief Executive", "Data Fiduciary"}
            };
            case GOVERNMENT -> new String[][]{
                    {"L0", "Clerk / LDC", "Lower Division Clerk", ""},
                    {"L1", "UDC / Assistant", "Upper Division Clerk", ""},
                    {"L2", "Section Officer", "Section Officer", ""},
                    {"L3", "Under Secretary", "Under Secretary", "Consent Manager"},
                    {"L4", "Deputy Secretary", "Deputy Secretary", "Grievance Officer"},
                    {"L5", "Joint Secretary", "Joint Secretary", "Data Protection Officer"},
                    {"L6", "Additional Secretary", "Additional Secretary", ""},
                    {"L7", "Secretary", "Department Secretary", "Data Fiduciary"}
            };
            case EDUCATION -> new String[][]{
                    {"L0", "Peon / Support Staff", "Administrative Support", ""},
                    {"L1", "Clerk / Lab Assistant", "Office Staff", ""},
                    {"L2", "Assistant Professor", "Faculty", ""},
                    {"L3", "Associate Professor", "Senior Faculty", "Consent Manager"},
                    {"L4", "Professor / HOD", "Department Head", ""},
                    {"L5", "Dean", "Faculty Dean", "Grievance Officer"},
                    {"L6", "Registrar", "University Registrar", "Data Protection Officer"},
                    {"L7", "Vice Chancellor", "University Head", "Data Fiduciary"}
            };
            case TELECOM -> new String[][]{
                    {"L0", "Executive", "Field / Support Executive", ""},
                    {"L1", "Senior Executive", "Senior Executive", ""},
                    {"L2", "Assistant Manager", "Team Lead", ""},
                    {"L3", "Manager", "Circle Manager", "Consent Manager"},
                    {"L4", "Senior Manager", "Regional Manager", ""},
                    {"L5", "AGM", "Assistant General Manager", "Grievance Officer"},
                    {"L6", "DGM / GM", "Deputy/General Manager", "Data Protection Officer"},
                    {"L7", "VP / Director", "Vice President", ""},
                    {"L8", "CEO / MD", "Chief Executive", "Data Fiduciary"}
            };
            // Generic hierarchy for all other sectors
            default -> new String[][]{
                    {"L0", "Executive", "Support / Entry Level", ""},
                    {"L1", "Senior Executive", "Individual Contributor", ""},
                    {"L2", "Team Lead", "Team Lead / Supervisor", ""},
                    {"L3", "Manager", "Department Manager", "Consent Manager"},
                    {"L4", "Senior Manager", "Senior Manager", ""},
                    {"L5", "Director", "Function Director", "Grievance Officer"},
                    {"L6", "VP / DPO", "Vice President", "Data Protection Officer"},
                    {"L7", "CEO / MD", "Chief Executive", "Data Fiduciary"}
            };
        };

        int count = 0;
        String orgId = getOrCreateDefaultOrgId();
        String sql = """
                INSERT INTO org_hierarchy_levels (id, org_id, level_number, level_code, level_name,
                    description, dpdp_role_mapping, can_approve_consent, can_approve_breach,
                    can_approve_dpia, can_view_reports, can_manage_policy, sort_order)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < hierarchy.length; i++) {
                String[] h = hierarchy[i];
                String dpdpRole = h[3];
                boolean isLeadership = i >= hierarchy.length - 3;
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, orgId);
                stmt.setInt(3, i);
                stmt.setString(4, h[0]);
                stmt.setString(5, h[1]);
                stmt.setString(6, h[2]);
                stmt.setString(7, dpdpRole);
                stmt.setInt(8, !dpdpRole.isEmpty() ? 1 : 0);
                stmt.setInt(9, isLeadership ? 1 : 0);
                stmt.setInt(10, isLeadership ? 1 : 0);
                stmt.setInt(11, i >= 3 ? 1 : 0);
                stmt.setInt(12, isLeadership ? 1 : 0);
                stmt.setInt(13, i);
                stmt.addBatch();
                count++;
            }
            stmt.executeBatch();
        }

        logger.info("Seeded {} hierarchy levels for sector {}", count, sector.displayName);
        return count;
    }

    private String getOrCreateDefaultOrgId() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM organizations LIMIT 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("id");
        }
        // Create default org
        String id = "default-org";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO organizations (id, name) VALUES (?, ?)")) {
            stmt.setString(1, id);
            stmt.setString(2, "Your Organization");
            stmt.executeUpdate();
        }
        return id;
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    private void clearAllSectorData() throws SQLException {
        String[] sectorPrefixes = { "BFSI-", "HEALTHCARE-", "ECOMMERCE-", "GOVERNMENT-", "EDUCATION-", "TELECOM-" };
        String[] tables = { "consents", "data_principals", "purposes", "breaches", "dpias", "policies",
                "gap_assessments", "siem_events", "siem_alerts", "dlp_policies", "dlp_incidents",
                "rights_requests", "audit_log", "soar_executions" };

        try (Connection conn = dbManager.getConnection()) {
            for (String table : tables) {
                for (String prefix : sectorPrefixes) {
                    try (PreparedStatement ps = conn
                            .prepareStatement("DELETE FROM " + table + " WHERE id LIKE ?")) {
                        ps.setString(1, prefix + "%");
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        // ignore if table doesn't exist
                    }
                }
            }
        }
        logger.info("Cleared all existing sector-specific data (12 tables)");
    }

    private String getSettingValue(String key) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("value") : null;
        } catch (SQLException e) {
            return null;
        }
    }

    private void upsertSetting(String key, String value, String category, String description) throws SQLException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO settings (key, value, category, description, updated_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) "
                             + "ON CONFLICT(key) DO UPDATE SET value = ?, updated_at = CURRENT_TIMESTAMP")) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.setString(3, category);
            stmt.setString(4, description);
            stmt.setString(5, value);
            stmt.executeUpdate();
        }
    }

    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> map = new LinkedHashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String col = meta.getColumnName(i);
            // Convert snake_case to camelCase for JSON
            String camelCol = snakeToCamel(col);
            map.put(camelCol, rs.getObject(i));
        }
        return map;
    }

    private String snakeToCamel(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private boolean getBool(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return "true".equalsIgnoreCase((String) val);
        if (val instanceof Number) return ((Number) val).intValue() != 0;
        return false;
    }
}
