package com.qsdpdp.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-Tenant Management Service
 * Enables SaaS delivery model where a single QS-DPDP instance
 * serves multiple organizations with complete data isolation.
 *
 * Features:
 * - Tenant provisioning & lifecycle
 * - Module-level licensing (sell modules independently)
 * - Data isolation per tenant
 * - Usage metering & billing
 * - Tenant branding (white-label)
 *
 * @version 3.0.0
 * @since Phase 6 — Enterprise Distribution
 */
@Service
public class MultiTenantService {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantService.class);

    private final Map<String, Tenant> tenants = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public MultiTenantService() {
        // Register default system tenant
        createTenant("system", "QS-DPDP System",
                "system@qsdpdp.local", "ENTERPRISE",
                List.of("ALL"));
    }

    // ═══════════════════════════════════════════════════════
    // TENANT LIFECYCLE
    // ═══════════════════════════════════════════════════════

    public Tenant createTenant(String id, String name, String adminEmail,
            String plan, List<String> modules) {
        Tenant tenant = new Tenant();
        tenant.id = id;
        tenant.name = name;
        tenant.adminEmail = adminEmail;
        tenant.plan = plan;
        tenant.enabledModules = new HashSet<>(modules);
        tenant.status = "ACTIVE";
        tenant.createdAt = LocalDateTime.now();
        tenant.maxAgents = getPlanAgentLimit(plan);
        tenant.maxUsers = getPlanUserLimit(plan);

        tenants.put(id, tenant);
        logger.info("✅ Tenant created: {} ({}) — Plan: {}, Modules: {}",
                id, name, plan, modules);
        return tenant;
    }

    public Tenant getTenant(String tenantId) {
        return tenants.get(tenantId);
    }

    public List<Tenant> getAllTenants() {
        return new ArrayList<>(tenants.values());
    }

    public void suspendTenant(String tenantId) {
        Tenant t = tenants.get(tenantId);
        if (t != null) {
            t.status = "SUSPENDED";
            logger.info("⏸️ Tenant suspended: {}", tenantId);
        }
    }

    public void activateTenant(String tenantId) {
        Tenant t = tenants.get(tenantId);
        if (t != null) {
            t.status = "ACTIVE";
            logger.info("▶️ Tenant activated: {}", tenantId);
        }
    }

    // ═══════════════════════════════════════════════════════
    // MODULE LICENSING (Independent Product Sales)
    // ═══════════════════════════════════════════════════════

    /**
     * Available modules that can be sold independently.
     */
    public static final Map<String, ModuleInfo> AVAILABLE_MODULES = Map.ofEntries(
            Map.entry("CONSENT", new ModuleInfo("CONSENT", "Consent Management",
                    "DPDP Section 6-7 consent lifecycle", 500)),
            Map.entry("BREACH", new ModuleInfo("BREACH", "Breach Management",
                    "DPBI/CERT-IN notification with 72h/6h automation", 750)),
            Map.entry("DLP", new ModuleInfo("DLP", "Data Loss Prevention",
                    "Content scanning, PII detection, EDM fingerprinting", 1200)),
            Map.entry("SIEM", new ModuleInfo("SIEM", "Security Information & Event Management",
                    "Syslog/CEF parsing, correlation, SOAR", 2000)),
            Map.entry("EDR", new ModuleInfo("EDR", "Endpoint Detection & Response",
                    "Agent monitoring, threat detection, isolation", 1500)),
            Map.entry("XDR", new ModuleInfo("XDR", "Extended Detection & Response",
                    "Cross-layer correlation, attack chain detection", 1800)),
            Map.entry("POLICY", new ModuleInfo("POLICY", "Policy Engine",
                    "Policy lifecycle, approval workflows, versioning", 400)),
            Map.entry("DPIA", new ModuleInfo("DPIA", "Data Protection Impact Assessment",
                    "Impact assessment workflow, risk scoring", 600)),
            Map.entry("RIGHTS", new ModuleInfo("RIGHTS", "Rights Management",
                    "Data principal rights (Section 11-14) workflow", 500)),
            Map.entry("GAP", new ModuleInfo("GAP", "Gap Analysis",
                    "Self-assessment, regulatory gap identification", 300)),
            Map.entry("CHATBOT", new ModuleInfo("CHATBOT", "AI Compliance Assistant",
                    "LLM-powered RAG chatbot for DPDP queries", 800)),
            Map.entry("PQC", new ModuleInfo("PQC", "Quantum-Safe Cryptography",
                    "ML-KEM-1024 + ML-DSA-87 hybrid encryption", 1000))
    );

    /**
     * Enable a module for a tenant.
     */
    public boolean enableModule(String tenantId, String module) {
        Tenant t = tenants.get(tenantId);
        if (t == null) return false;

        if (AVAILABLE_MODULES.containsKey(module)) {
            t.enabledModules.add(module);
            logger.info("✅ Module {} enabled for tenant {}", module, tenantId);
            return true;
        }
        return false;
    }

    /**
     * Check if a module is enabled for current tenant.
     */
    public boolean isModuleEnabled(String tenantId, String module) {
        Tenant t = tenants.get(tenantId);
        if (t == null) return false;
        return t.enabledModules.contains("ALL") || t.enabledModules.contains(module);
    }

    // ═══════════════════════════════════════════════════════
    // PLAN MANAGEMENT
    // ═══════════════════════════════════════════════════════

    public static final Map<String, PlanInfo> PLANS = Map.of(
            "STARTER", new PlanInfo("STARTER", "Starter",
                    List.of("CONSENT", "BREACH", "RIGHTS", "GAP"), 10, 5, 999),
            "PROFESSIONAL", new PlanInfo("PROFESSIONAL", "Professional",
                    List.of("CONSENT", "BREACH", "RIGHTS", "GAP", "DLP", "POLICY", "DPIA", "CHATBOT"),
                    100, 25, 4999),
            "ENTERPRISE", new PlanInfo("ENTERPRISE", "Enterprise",
                    List.of("ALL"), 10000, 500, 14999),
            "SOVEREIGN", new PlanInfo("SOVEREIGN", "Sovereign Cloud",
                    List.of("ALL"), 100000, 5000, 49999)
    );

    // ═══════════════════════════════════════════════════════
    // TENANT CONTEXT (ThreadLocal)
    // ═══════════════════════════════════════════════════════

    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clearCurrentTenant() {
        currentTenant.remove();
    }

    // ═══════════════════════════════════════════════════════
    // USAGE METERING
    // ═══════════════════════════════════════════════════════

    public Map<String, Object> getTenantUsage(String tenantId) {
        Tenant t = tenants.get(tenantId);
        if (t == null) return Map.of("error", "Tenant not found");

        return Map.of(
                "tenantId", tenantId,
                "plan", t.plan,
                "enabledModules", t.enabledModules,
                "maxAgents", t.maxAgents,
                "maxUsers", t.maxUsers,
                "status", t.status,
                "createdAt", t.createdAt.toString());
    }

    public Map<String, Object> getStatistics() {
        return Map.of(
                "totalTenants", tenants.size(),
                "activeTenants", tenants.values().stream()
                        .filter(t -> "ACTIVE".equals(t.status)).count(),
                "availableModules", AVAILABLE_MODULES.size(),
                "availablePlans", PLANS.size());
    }

    // ═══ HELPERS ═══

    private int getPlanAgentLimit(String plan) {
        PlanInfo p = PLANS.get(plan);
        return p != null ? p.maxAgents : 10;
    }

    private int getPlanUserLimit(String plan) {
        PlanInfo p = PLANS.get(plan);
        return p != null ? p.maxUsers : 5;
    }

    // ═══ DATA CLASSES ═══

    public static class Tenant {
        public String id, name, adminEmail, plan, status;
        public Set<String> enabledModules;
        public LocalDateTime createdAt;
        public int maxAgents, maxUsers;
    }

    public record ModuleInfo(String id, String name, String description, int pricePerMonth) {}

    public record PlanInfo(String id, String name, List<String> modules,
            int maxAgents, int maxUsers, int pricePerMonth) {}
}
