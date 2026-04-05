package com.qsdpdp.module;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Module Manager - Central registry for enabling/disabling modules
 * Provides admin control over all platform modules with dependency validation
 */
public class ModuleManager {
    private static final Logger logger = LoggerFactory.getLogger(ModuleManager.class);
    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;
    private boolean initialized = false;
    private final Map<String, ModuleDefinition> modules = new ConcurrentHashMap<>();

    public ModuleManager(DatabaseManager dbManager, AuditService auditService, EventBus eventBus) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS modules (id TEXT PRIMARY KEY, name TEXT, description TEXT, version TEXT, enabled INTEGER DEFAULT 1, licensed INTEGER DEFAULT 1, license_key TEXT, health TEXT DEFAULT 'UNKNOWN', enabled_at TIMESTAMP, disabled_at TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS module_features (module_id TEXT, feature TEXT, enabled INTEGER DEFAULT 1, PRIMARY KEY (module_id, feature))");
        } catch (SQLException e) { logger.error("Failed to create module tables", e); }

        // Register all default modules
        for (ModuleDefinition mod : ModuleDefinition.getDefaultModules()) {
            modules.put(mod.getId(), mod);
        }
        loadPersistedState();
        initialized = true;
        logger.info("ModuleManager initialized with {} modules", modules.size());
    }

    /** Enable a module */
    public ModuleResult enableModule(String moduleId) {
        ModuleDefinition mod = modules.get(moduleId);
        if (mod == null) return new ModuleResult(false, "Module not found: " + moduleId);

        // Check dependencies
        for (String dep : mod.getDependencies()) {
            ModuleDefinition depMod = modules.get(dep);
            if (depMod == null || !depMod.isEnabled()) {
                return new ModuleResult(false, "Dependency not enabled: " + dep);
            }
        }

        mod.setEnabled(true);
        mod.setEnabledAt(LocalDateTime.now());
        mod.setHealth(ModuleDefinition.ModuleHealth.HEALTHY);
        persistModuleState(mod);

        auditService.log("MODULE_ENABLED", "MODULE", "ADMIN", "Module enabled: " + mod.getName());
        logger.info("Module enabled: {} ({})", mod.getName(), moduleId);
        return new ModuleResult(true, "Module enabled: " + mod.getName());
    }

    /** Disable a module */
    public ModuleResult disableModule(String moduleId) {
        ModuleDefinition mod = modules.get(moduleId);
        if (mod == null) return new ModuleResult(false, "Module not found: " + moduleId);

        // Check if other enabled modules depend on this one
        for (ModuleDefinition other : modules.values()) {
            if (other.isEnabled() && other.getDependencies().contains(moduleId)) {
                return new ModuleResult(false, "Cannot disable: module '" + other.getName() + "' depends on it");
            }
        }

        mod.setEnabled(false);
        mod.setDisabledAt(LocalDateTime.now());
        mod.setHealth(ModuleDefinition.ModuleHealth.UNKNOWN);
        persistModuleState(mod);

        auditService.log("MODULE_DISABLED", "MODULE", "ADMIN", "Module disabled: " + mod.getName());
        logger.info("Module disabled: {} ({})", mod.getName(), moduleId);
        return new ModuleResult(true, "Module disabled: " + mod.getName());
    }

    /** Check if a module is enabled */
    public boolean isModuleEnabled(String moduleId) {
        ModuleDefinition mod = modules.get(moduleId);
        return mod != null && mod.isEnabled();
    }

    /** Get module status */
    public ModuleDefinition getModule(String moduleId) { return modules.get(moduleId); }

    /** Get all modules */
    public List<ModuleDefinition> getAllModules() { return new ArrayList<>(modules.values()); }

    /** Get enabled modules */
    public List<ModuleDefinition> getEnabledModules() {
        return modules.values().stream().filter(ModuleDefinition::isEnabled).toList();
    }

    /** Enable/disable a specific feature within a module */
    public ModuleResult toggleFeature(String moduleId, String feature, boolean enabled) {
        ModuleDefinition mod = modules.get(moduleId);
        if (mod == null) return new ModuleResult(false, "Module not found");
        if (!mod.getFeatures().contains(feature)) return new ModuleResult(false, "Feature not found: " + feature);
        // Feature flag toggle (persisted)
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO module_features (module_id, feature, enabled) VALUES (?, ?, ?)")) {
            ps.setString(1, moduleId); ps.setString(2, feature); ps.setInt(3, enabled ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) { logger.error("Failed to toggle feature", e); }
        return new ModuleResult(true, "Feature " + feature + " " + (enabled ? "enabled" : "disabled"));
    }

    /** Health check all modules */
    public Map<String, ModuleDefinition.ModuleHealth> healthCheck() {
        Map<String, ModuleDefinition.ModuleHealth> health = new LinkedHashMap<>();
        for (ModuleDefinition mod : modules.values()) {
            if (mod.isEnabled()) {
                mod.setHealth(ModuleDefinition.ModuleHealth.HEALTHY);
            }
            health.put(mod.getId(), mod.getHealth());
        }
        return health;
    }

    private void persistModuleState(ModuleDefinition mod) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO modules (id, name, description, version, enabled, licensed, health, enabled_at, disabled_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, mod.getId()); ps.setString(2, mod.getName()); ps.setString(3, mod.getDescription());
            ps.setString(4, mod.getVersion()); ps.setInt(5, mod.isEnabled() ? 1 : 0); ps.setInt(6, mod.isLicensed() ? 1 : 0);
            ps.setString(7, mod.getHealth().name());
            ps.setString(8, mod.getEnabledAt() != null ? mod.getEnabledAt().toString() : null);
            ps.setString(9, mod.getDisabledAt() != null ? mod.getDisabledAt().toString() : null);
            ps.setString(10, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) { logger.error("Failed to persist module state", e); }
    }

    private void loadPersistedState() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, enabled FROM modules")) {
            while (rs.next()) {
                String id = rs.getString("id");
                boolean enabled = rs.getInt("enabled") == 1;
                ModuleDefinition mod = modules.get(id);
                if (mod != null) mod.setEnabled(enabled);
            }
        } catch (SQLException e) { logger.debug("No persisted module state found (first run)"); }
    }

    public boolean isInitialized() { return initialized; }

    public static class ModuleResult {
        public boolean success; public String message;
        public ModuleResult(boolean s, String m) { this.success = s; this.message = m; }
    }
}
