package com.qsdpdp.module;

import java.util.*;

/**
 * Module Definition - Represents a deployable module in the QS-DPDP platform
 */
public class ModuleDefinition {
    private String id;
    private String name;
    private String description;
    private String version;
    private boolean enabled;
    private boolean licensed;
    private String licenseKey;
    private Set<String> dependencies;
    private Set<String> features;
    private ModuleHealth health;
    private java.time.LocalDateTime enabledAt;
    private java.time.LocalDateTime disabledAt;

    public enum ModuleHealth { HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN }

    // Predefined module IDs
    public static final String PII_SCANNER = "pii-scanner";
    public static final String SIEM = "siem";
    public static final String DLP = "dlp";
    public static final String CONSENT_ENGINE = "consent-engine";
    public static final String BREACH_MANAGER = "breach-manager";
    public static final String RIGHTS_ENGINE = "rights-engine";
    public static final String AUDIT = "audit";
    public static final String API_GATEWAY = "api-gateway";
    public static final String RAG = "rag-evaluator";
    public static final String COMPLIANCE_ENGINE = "compliance-engine";
    public static final String THREAT_INTEL = "threat-intelligence";
    public static final String UEBA = "ueba";
    public static final String DATA_CLASSIFICATION = "data-classification";
    public static final String MODULE_MANAGER = "module-manager";

    public ModuleDefinition() {
        this.dependencies = new HashSet<>();
        this.features = new HashSet<>();
        this.health = ModuleHealth.UNKNOWN;
        this.enabled = true;
        this.licensed = true;
    }

    public ModuleDefinition(String id, String name, String description, String version) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
    }

    /** Get all predefined modules */
    public static List<ModuleDefinition> getDefaultModules() {
        List<ModuleDefinition> modules = new ArrayList<>();

        ModuleDefinition core = new ModuleDefinition(COMPLIANCE_ENGINE, "Compliance Engine", "Core compliance scoring and gap analysis engine", "2.0.0");
        core.getFeatures().addAll(Set.of("compliance-scoring", "gap-analysis", "framework-mapping"));
        modules.add(core);

        ModuleDefinition audit = new ModuleDefinition(AUDIT, "Audit Service", "Hash-chained audit ledger with tamper detection", "2.0.0");
        audit.getFeatures().addAll(Set.of("audit-logging", "tamper-detection", "report-generation"));
        modules.add(audit);

        ModuleDefinition pii = new ModuleDefinition(PII_SCANNER, "PII Scanner", "Enterprise PII detection with system/drive/network scanning", "2.0.0");
        pii.getDependencies().add(AUDIT);
        pii.getFeatures().addAll(Set.of("text-scan", "file-scan", "directory-scan", "database-scan", "system-scan", "drive-scan", "network-scan", "scheduled-scan"));
        modules.add(pii);

        ModuleDefinition siem = new ModuleDefinition(SIEM, "SIEM Engine", "Security event correlation, SOAR, threat intelligence, UEBA", "2.0.0");
        siem.getDependencies().addAll(Set.of(AUDIT));
        siem.getFeatures().addAll(Set.of("event-correlation", "soar-playbooks", "threat-intel", "ueba", "threat-hunting", "forensic-timeline", "mitre-mapping"));
        modules.add(siem);

        ModuleDefinition dlp = new ModuleDefinition(DLP, "DLP Engine", "Data Loss Prevention with classification, inspection, lineage tracking", "2.0.0");
        dlp.getDependencies().addAll(Set.of(PII_SCANNER, SIEM, AUDIT));
        dlp.getFeatures().addAll(Set.of("policy-engine", "file-monitor", "network-monitor", "email-monitor", "usb-monitor", "clipboard-monitor", "data-classification", "content-inspection", "data-lineage", "discovery-scan"));
        modules.add(dlp);

        ModuleDefinition consent = new ModuleDefinition(CONSENT_ENGINE, "Consent Manager", "Data principal consent collection and lifecycle management", "2.0.0");
        consent.getDependencies().add(AUDIT);
        consent.getFeatures().addAll(Set.of("consent-collection", "consent-withdrawal", "consent-lifecycle", "granular-consent"));
        modules.add(consent);

        ModuleDefinition breach = new ModuleDefinition(BREACH_MANAGER, "Breach Manager", "Breach detection, notification, and response workflow", "2.0.0");
        breach.getDependencies().addAll(Set.of(AUDIT, SIEM));
        breach.getFeatures().addAll(Set.of("breach-detection", "dpb-notification", "72-hour-tracking", "impact-assessment"));
        modules.add(breach);

        ModuleDefinition rights = new ModuleDefinition(RIGHTS_ENGINE, "Rights Engine", "Data principal rights management (access, correction, erasure)", "2.0.0");
        rights.getDependencies().addAll(Set.of(CONSENT_ENGINE, AUDIT));
        rights.getFeatures().addAll(Set.of("right-to-access", "right-to-correction", "right-to-erasure", "right-to-grievance"));
        modules.add(rights);

        ModuleDefinition api = new ModuleDefinition(API_GATEWAY, "API Gateway", "Secure REST API with rate limiting and webhooks", "2.0.0");
        api.getFeatures().addAll(Set.of("api-keys", "rate-limiting", "webhooks", "request-logging"));
        modules.add(api);

        ModuleDefinition rag = new ModuleDefinition(RAG, "RAG Evaluator", "Retrieval-Augmented Generation compliance evaluator", "2.0.0");
        rag.getFeatures().addAll(Set.of("rag-evaluation", "checklist-generation"));
        modules.add(rag);

        ModuleDefinition ti = new ModuleDefinition(THREAT_INTEL, "Threat Intelligence", "IoC feed management and reputation checking", "1.0.0");
        ti.getDependencies().add(SIEM);
        ti.getFeatures().addAll(Set.of("ioc-feeds", "reputation-check", "event-enrichment"));
        modules.add(ti);

        ModuleDefinition ueba = new ModuleDefinition(UEBA, "UEBA", "User & Entity Behavior Analytics", "1.0.0");
        ueba.getDependencies().add(SIEM);
        ueba.getFeatures().addAll(Set.of("behavioral-baseline", "anomaly-detection", "risk-scoring"));
        modules.add(ueba);

        ModuleDefinition dc = new ModuleDefinition(DATA_CLASSIFICATION, "Data Classification", "Automatic data labeling and classification", "1.0.0");
        dc.getDependencies().add(PII_SCANNER);
        dc.getFeatures().addAll(Set.of("auto-classification", "label-management", "compliance-mapping"));
        modules.add(dc);

        return modules;
    }

    // Getters and Setters
    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public String getVersion() { return version; } public void setVersion(String v) { this.version = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean e) { this.enabled = e; }
    public boolean isLicensed() { return licensed; } public void setLicensed(boolean l) { this.licensed = l; }
    public String getLicenseKey() { return licenseKey; } public void setLicenseKey(String k) { this.licenseKey = k; }
    public Set<String> getDependencies() { return dependencies; }
    public Set<String> getFeatures() { return features; }
    public ModuleHealth getHealth() { return health; } public void setHealth(ModuleHealth h) { this.health = h; }
    public java.time.LocalDateTime getEnabledAt() { return enabledAt; } public void setEnabledAt(java.time.LocalDateTime t) { this.enabledAt = t; }
    public java.time.LocalDateTime getDisabledAt() { return disabledAt; } public void setDisabledAt(java.time.LocalDateTime t) { this.disabledAt = t; }
}
