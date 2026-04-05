package com.qsdpdp.dlp;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.pii.PIIScanner;
import com.qsdpdp.pii.PIIScanResult;
import com.qsdpdp.pii.ScanProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Discovery Scan Service - Periodic data-at-rest scanning
 * Identifies unmonitored data stores and builds data inventory
 */
public class DiscoveryScanService {
    private static final Logger logger = LoggerFactory.getLogger(DiscoveryScanService.class);
    private final DatabaseManager dbManager;
    private final PIIScanner piiScanner;
    private final DataClassificationService classificationService;
    private boolean initialized = false;
    private ScheduledExecutorService scheduler;
    private final Map<String, DiscoveryEndpoint> endpoints = new ConcurrentHashMap<>();
    private final List<DiscoveryResult> results = new CopyOnWriteArrayList<>();

    public DiscoveryScanService(DatabaseManager dbManager, PIIScanner piiScanner, DataClassificationService classificationService) {
        this.dbManager = dbManager;
        this.piiScanner = piiScanner;
        this.classificationService = classificationService;
    }

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS discovery_endpoints (id TEXT PRIMARY KEY, name TEXT, type TEXT, path TEXT, enabled INTEGER DEFAULT 1, last_scan TIMESTAMP, status TEXT DEFAULT 'PENDING', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            stmt.execute("CREATE TABLE IF NOT EXISTS discovery_results (id TEXT PRIMARY KEY, endpoint_id TEXT, scan_time TIMESTAMP, files_scanned INTEGER, pii_findings INTEGER, classification TEXT, status TEXT, duration_ms INTEGER)");
            stmt.execute("CREATE TABLE IF NOT EXISTS data_inventory (id TEXT PRIMARY KEY, endpoint_id TEXT, file_path TEXT, file_type TEXT, size_bytes INTEGER, classification TEXT, pii_types TEXT, last_scanned TIMESTAMP, risk_score REAL)");
        } catch (SQLException e) { logger.error("Failed to create discovery tables", e); }
        loadDefaultEndpoints();
        initialized = true;
        logger.info("DiscoveryScanService initialized with {} endpoints", endpoints.size());
    }

    private void loadDefaultEndpoints() {
        addEndpoint("user-home", "User Home Directory", "FILESYSTEM", System.getProperty("user.home"));
        addEndpoint("temp-dir", "System Temp Directory", "FILESYSTEM", System.getProperty("java.io.tmpdir"));
        addEndpoint("desktop", "Desktop", "FILESYSTEM", System.getProperty("user.home") + "/Desktop");
        addEndpoint("downloads", "Downloads", "FILESYSTEM", System.getProperty("user.home") + "/Downloads");
        addEndpoint("documents", "Documents", "FILESYSTEM", System.getProperty("user.home") + "/Documents");
    }

    /** Add a discovery endpoint to scan */
    public void addEndpoint(String id, String name, String type, String path) {
        DiscoveryEndpoint ep = new DiscoveryEndpoint();
        ep.id = id; ep.name = name; ep.type = type; ep.path = path; ep.enabled = true;
        endpoints.put(id, ep);
    }

    /** Remove an endpoint */
    public boolean removeEndpoint(String id) { return endpoints.remove(id) != null; }

    /** Run discovery scan on a specific endpoint */
    public DiscoveryResult scanEndpoint(String endpointId) {
        DiscoveryEndpoint ep = endpoints.get(endpointId);
        if (ep == null) return null;

        DiscoveryResult result = new DiscoveryResult();
        result.id = UUID.randomUUID().toString();
        result.endpointId = endpointId;
        result.endpointName = ep.name;
        result.scanTime = LocalDateTime.now();

        long startMs = System.currentTimeMillis();
        try {
            ScanProfile profile = ScanProfile.quickScan();
            profile.setTargetPath(ep.path);
            PIIScanResult scanResult = piiScanner.scanWithProfile(profile);

            result.filesScanned = scanResult.getFilesScanned();
            result.piiFindings = scanResult.getTotalFindings();
            result.status = "COMPLETED";

            if (classificationService != null && classificationService.isInitialized()) {
                var classification = classificationService.classifyFromScanResult(scanResult);
                result.classification = classification.level.name();
            }

            ep.lastScan = LocalDateTime.now();
            ep.status = "SCANNED";
        } catch (Exception e) {
            logger.error("Discovery scan failed for endpoint: {}", endpointId, e);
            result.status = "FAILED";
        }
        result.durationMs = (int)(System.currentTimeMillis() - startMs);
        results.add(result);
        persistResult(result);
        return result;
    }

    /** Run discovery on all enabled endpoints */
    public List<DiscoveryResult> scanAllEndpoints() {
        List<DiscoveryResult> allResults = new ArrayList<>();
        for (DiscoveryEndpoint ep : endpoints.values()) {
            if (ep.enabled) {
                DiscoveryResult r = scanEndpoint(ep.id);
                if (r != null) allResults.add(r);
            }
        }
        return allResults;
    }

    /** Schedule periodic discovery scans */
    public void scheduleDiscovery(int intervalHours) {
        if (scheduler != null) scheduler.shutdown();
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::scanAllEndpoints, 0, intervalHours, TimeUnit.HOURS);
        logger.info("Scheduled discovery scans every {} hours", intervalHours);
    }

    /** Get all endpoints */
    public Map<String, DiscoveryEndpoint> getEndpoints() { return Collections.unmodifiableMap(endpoints); }

    /** Get recent results */
    public List<DiscoveryResult> getRecentResults(int limit) {
        int from = Math.max(0, results.size() - limit);
        return results.subList(from, results.size());
    }

    private void persistResult(DiscoveryResult result) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO discovery_results (id, endpoint_id, scan_time, files_scanned, pii_findings, classification, status, duration_ms) VALUES (?,?,?,?,?,?,?,?)")) {
            ps.setString(1, result.id); ps.setString(2, result.endpointId);
            ps.setString(3, result.scanTime.toString()); ps.setInt(4, result.filesScanned);
            ps.setInt(5, result.piiFindings); ps.setString(6, result.classification);
            ps.setString(7, result.status); ps.setInt(8, result.durationMs);
            ps.executeUpdate();
        } catch (SQLException e) { logger.debug("Failed to persist discovery result", e); }
    }

    public boolean isInitialized() { return initialized; }
    public void shutdown() { if (scheduler != null) scheduler.shutdown(); }

    public static class DiscoveryEndpoint {
        public String id, name, type, path, status = "PENDING";
        public boolean enabled = true;
        public LocalDateTime lastScan;
    }

    public static class DiscoveryResult {
        public String id, endpointId, endpointName, classification, status;
        public LocalDateTime scanTime;
        public int filesScanned, piiFindings, durationMs;
    }
}
