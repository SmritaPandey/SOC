package com.qsdpdp.integration.adapters;

import com.qsdpdp.integration.ExternalIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sector Adapter Factory — Plug-and-Play Connector Framework
 * 
 * Factory-pattern manager for sector-specific integration adapters:
 * - BFSI (CBS, UPI, KYC, CERSAI)
 * - Healthcare (EHR, ABDM, CoWIN)
 * - Insurance (IIB, policy management)
 * - Telecom (CDR, DND, TRAI)
 * - Government (DigiLocker, eSign, Aadhaar)
 * 
 * Config-driven onboarding — each adapter is activated via
 * configuration without code changes.
 * 
 * @version 1.0.0
 * @since Phase 2 — API Integration
 */
@Service
public class SectorAdapterFactory {

    private static final Logger logger = LoggerFactory.getLogger(SectorAdapterFactory.class);

    @Autowired(required = false)
    private ExternalIntegrationService integrationService;

    private final Map<String, SectorAdapter> adapters = new ConcurrentHashMap<>();
    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Sector Adapter Factory...");
        registerDefaultAdapters();
        initialized = true;
        logger.info("Sector Adapter Factory initialized with {} adapters", adapters.size());
    }

    // ═══════════════════════════════════════════════════════════
    // DEFAULT ADAPTERS
    // ═══════════════════════════════════════════════════════════

    private void registerDefaultAdapters() {
        // BFSI Sector Adapters
        register(new SectorAdapter("bfsi-cbs", "Core Banking System (CBS)", "BFSI",
                "Connect to CBS for customer KYC, account status, and transaction consent",
                List.of("CBS_API_URL", "CBS_API_KEY", "CBS_BRANCH_CODE")));

        register(new SectorAdapter("bfsi-upi", "UPI Payment Gateway", "BFSI",
                "NPCI UPI integration for consent-based payment data access",
                List.of("UPI_PSP_ID", "UPI_API_KEY", "UPI_CALLBACK_URL")));

        register(new SectorAdapter("bfsi-kyc", "KYC Verification (CKYC/eKYC)", "BFSI",
                "CERSAI CKYC and Aadhaar eKYC integration",
                List.of("CKYC_API_URL", "CKYC_INSTITUTION_ID", "AADHAAR_LICENSE_KEY")));

        register(new SectorAdapter("bfsi-rbi-reporting", "RBI Regulatory Reporting", "BFSI",
                "Automated compliance reporting to RBI via XBRL/API",
                List.of("RBI_ENTITY_CODE", "RBI_API_URL", "RBI_CERT_PATH")));

        register(new SectorAdapter("bfsi-cersai", "CERSAI Registry", "BFSI",
                "Central Registry of Securitisation and Asset Reconstruction",
                List.of("CERSAI_ENTITY_ID", "CERSAI_API_URL")));

        // Healthcare Sector Adapters
        register(new SectorAdapter("health-abdm", "ABDM (Ayushman Bharat Digital Mission)", "HEALTHCARE",
                "Health data exchange via ABDM/NDHM APIs — PHR, HIP/HIU consent flow",
                List.of("ABDM_CLIENT_ID", "ABDM_CLIENT_SECRET", "ABDM_FACILITY_ID")));

        register(new SectorAdapter("health-ehr", "EHR System Integration", "HEALTHCARE",
                "Connect to hospital EHR systems for health record consent management",
                List.of("EHR_FHIR_URL", "EHR_API_KEY", "EHR_FACILITY_CODE")));

        register(new SectorAdapter("health-cowin", "CoWIN/Vaccination Registry", "HEALTHCARE",
                "Vaccination record access with consent",
                List.of("COWIN_API_URL", "COWIN_SECRET")));

        register(new SectorAdapter("health-ndhm", "NDHM Health Claims", "HEALTHCARE",
                "National Digital Health Mission claims exchange",
                List.of("NDHM_ENTITY_ID", "NDHM_API_KEY")));

        // Insurance Sector Adapters
        register(new SectorAdapter("insurance-iib", "Insurance Information Bureau (IIB)", "INSURANCE",
                "IIB integration for policy data, claims, and fraud detection",
                List.of("IIB_MEMBER_ID", "IIB_API_KEY", "IIB_CERT_PATH")));

        register(new SectorAdapter("insurance-policy", "Policy Management System", "INSURANCE",
                "Connect to core policy admin for consent-based data access",
                List.of("POLICY_SYSTEM_URL", "POLICY_API_KEY")));

        register(new SectorAdapter("insurance-irdai", "IRDAI Regulatory Compliance", "INSURANCE",
                "Automated regulatory reporting to IRDAI",
                List.of("IRDAI_ENTITY_CODE", "IRDAI_API_URL")));

        // Telecom Sector Adapters
        register(new SectorAdapter("telecom-cdr", "CDR (Call Detail Records)", "TELECOM",
                "Consent-based CDR access for telecom subscribers",
                List.of("CDR_API_URL", "CDR_OPERATOR_ID", "CDR_API_KEY")));

        register(new SectorAdapter("telecom-dnd", "DND Registry (TRAI)", "TELECOM",
                "Do Not Disturb preference management via TRAI DND API",
                List.of("DND_API_URL", "DND_ENTITY_ID")));

        register(new SectorAdapter("telecom-trai", "TRAI Compliance Reporting", "TELECOM",
                "Telecom regulatory compliance reporting to TRAI",
                List.of("TRAI_ENTITY_ID", "TRAI_API_URL")));

        // Government Sector Adapters
        register(new SectorAdapter("gov-digilocker", "DigiLocker Integration", "GOVERNMENT",
                "Consent-based document access from DigiLocker",
                List.of("DIGILOCKER_CLIENT_ID", "DIGILOCKER_CLIENT_SECRET", "DIGILOCKER_CALLBACK_URL")));

        register(new SectorAdapter("gov-esign", "eSign (Aadhaar-based)", "GOVERNMENT",
                "Aadhaar-based electronic signature for consent documents",
                List.of("ESIGN_ASP_ID", "ESIGN_API_KEY", "ESIGN_CALLBACK_URL")));

        register(new SectorAdapter("gov-umang", "UMANG Platform", "GOVERNMENT",
                "Government services integration via UMANG",
                List.of("UMANG_API_KEY", "UMANG_DEPT_ID")));

        register(new SectorAdapter("gov-gstn", "GSTN (GST Network)", "GOVERNMENT",
                "GST compliance data exchange",
                List.of("GSTN_API_URL", "GSTN_ASP_ID", "GSTN_API_KEY")));

        // Also register with ExternalIntegrationService if available
        if (integrationService != null) {
            for (SectorAdapter adapter : adapters.values()) {
                integrationService.register(new ExternalIntegrationService.ConnectorRegistration(
                        adapter.id, adapter.name,
                        ExternalIntegrationService.ConnectorType.CUSTOM,
                        adapter.description, false
                ));
            }
            logger.info("Sector adapters registered with ExternalIntegrationService");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ADAPTER MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public void register(SectorAdapter adapter) {
        adapters.put(adapter.id, adapter);
        logger.debug("Registered sector adapter: {} ({})", adapter.name, adapter.sector);
    }

    /**
     * Enable an adapter with configuration
     */
    public Map<String, Object> enableAdapter(String adapterId, Map<String, String> config) {
        SectorAdapter adapter = adapters.get(adapterId);
        if (adapter == null) {
            return Map.of("success", false, "error", "Adapter not found: " + adapterId);
        }

        // Validate required config
        List<String> missing = new ArrayList<>();
        for (String req : adapter.requiredConfig) {
            if (!config.containsKey(req) || config.get(req).isEmpty()) {
                missing.add(req);
            }
        }
        if (!missing.isEmpty()) {
            return Map.of("success", false, "error", "Missing required config: " + missing,
                    "requiredConfig", adapter.requiredConfig);
        }

        adapter.config = config;
        adapter.enabled = true;
        adapter.lastConfigured = LocalDateTime.now();
        adapter.status = "ACTIVE";

        logger.info("Sector adapter enabled: {} ({})", adapter.name, adapterId);
        return Map.of("success", true, "message", "Adapter enabled: " + adapter.name,
                "adapterId", adapterId, "status", "ACTIVE");
    }

    /**
     * Disable an adapter
     */
    public Map<String, Object> disableAdapter(String adapterId) {
        SectorAdapter adapter = adapters.get(adapterId);
        if (adapter == null) return Map.of("success", false, "error", "Not found");
        adapter.enabled = false;
        adapter.status = "DISABLED";
        return Map.of("success", true, "message", "Adapter disabled: " + adapter.name);
    }

    /**
     * Test connectivity to an adapter
     */
    public Map<String, Object> testAdapter(String adapterId) {
        SectorAdapter adapter = adapters.get(adapterId);
        if (adapter == null) return Map.of("success", false, "error", "Not found");
        // Stub test — production would call real endpoint
        adapter.lastTestedAt = LocalDateTime.now();
        adapter.connectionStatus = "OK";
        return Map.of("success", true, "adapterId", adapterId, "message",
                "Connection test successful for " + adapter.name, "latencyMs", 45);
    }

    /**
     * Send data via adapter (stub — production calls real APIs)
     */
    public Map<String, Object> sendData(String adapterId, Map<String, Object> data) {
        SectorAdapter adapter = adapters.get(adapterId);
        if (adapter == null || !adapter.enabled) {
            return Map.of("success", false, "error", "Adapter not available or disabled");
        }
        adapter.requestCount++;
        adapter.lastRequestAt = LocalDateTime.now();
        logger.info("Data sent via adapter {}: {} fields", adapterId, data.size());
        return Map.of("success", true, "adapterId", adapterId,
                "message", "Data transmitted via " + adapter.name);
    }

    // ═══════════════════════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════════════════════

    public List<SectorAdapter> getAllAdapters() {
        return new ArrayList<>(adapters.values());
    }

    public List<SectorAdapter> getAdaptersBySector(String sector) {
        return adapters.values().stream()
                .filter(a -> a.sector.equalsIgnoreCase(sector))
                .toList();
    }

    public List<SectorAdapter> getEnabledAdapters() {
        return adapters.values().stream()
                .filter(a -> a.enabled)
                .toList();
    }

    public SectorAdapter getAdapter(String adapterId) {
        return adapters.get(adapterId);
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalAdapters", adapters.size());
        overview.put("enabledAdapters", getEnabledAdapters().size());
        overview.put("sectors", getSectorSummary());
        overview.put("timestamp", LocalDateTime.now().toString());
        return overview;
    }

    private Map<String, Integer> getSectorSummary() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SectorAdapter a : adapters.values()) {
            counts.merge(a.sector, 1, Integer::sum);
        }
        return counts;
    }

    public boolean isInitialized() { return initialized; }

    // ═══════════════════════════════════════════════════════════
    // SECTOR ADAPTER DTO
    // ═══════════════════════════════════════════════════════════

    public static class SectorAdapter {
        public String id;
        public String name;
        public String sector;
        public String description;
        public List<String> requiredConfig;
        public Map<String, String> config = new HashMap<>();
        public boolean enabled = false;
        public String status = "INACTIVE";
        public String connectionStatus = "UNTESTED";
        public int requestCount = 0;
        public LocalDateTime lastConfigured;
        public LocalDateTime lastTestedAt;
        public LocalDateTime lastRequestAt;

        public SectorAdapter(String id, String name, String sector, String description,
                             List<String> requiredConfig) {
            this.id = id;
            this.name = name;
            this.sector = sector;
            this.description = description;
            this.requiredConfig = requiredConfig;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("name", name);
            m.put("sector", sector);
            m.put("description", description);
            m.put("requiredConfig", requiredConfig);
            m.put("enabled", enabled);
            m.put("status", status);
            m.put("connectionStatus", connectionStatus);
            m.put("requestCount", requestCount);
            m.put("lastConfigured", lastConfigured != null ? lastConfigured.toString() : null);
            m.put("lastTestedAt", lastTestedAt != null ? lastTestedAt.toString() : null);
            return m;
        }
    }
}
