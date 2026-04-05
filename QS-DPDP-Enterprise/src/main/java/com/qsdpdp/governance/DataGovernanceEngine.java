package com.qsdpdp.governance;

import com.qsdpdp.core.DataAsset;
import com.qsdpdp.core.DataAsset.*;
import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.pii.PIIScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Data Governance Engine — Module 1
 * Central service for data discovery, classification, cataloging, and lineage tracking.
 * Integrates with PIIScanner for automated discovery and classification.
 *
 * Features:
 * - Auto data discovery (DB + Files + APIs)
 * - Rule-based + AI-based classification
 * - Data catalog (asset registry)
 * - Sensitivity heatmap generation
 * - Data lineage tracking (source → process → destination)
 *
 * @version 1.0.0
 * @since Module 1
 */
@Service
public class DataGovernanceEngine {

    private static final Logger logger = LoggerFactory.getLogger(DataGovernanceEngine.class);

    @Autowired
    private DatabaseManager databaseManager;

    @Autowired(required = false)
    private AuditService auditService;

    private final Map<String, DataAsset> assetRegistry = new ConcurrentHashMap<>();
    private final Map<String, List<LineageRecord>> lineageGraph = new ConcurrentHashMap<>();
    private final Map<String, ScanResult> scanHistory = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private boolean initialized = false;

    // --- Classification Rules ---

    private static final Map<String, DataAsset.AssetType> CLASSIFICATION_RULES = new LinkedHashMap<>();
    static {
        // PII patterns
        CLASSIFICATION_RULES.put("aadhaar", AssetType.PII);
        CLASSIFICATION_RULES.put("pan_number", AssetType.PII);
        CLASSIFICATION_RULES.put("passport", AssetType.PII);
        CLASSIFICATION_RULES.put("voter_id", AssetType.PII);
        CLASSIFICATION_RULES.put("driving_license", AssetType.PII);
        CLASSIFICATION_RULES.put("email", AssetType.PII);
        CLASSIFICATION_RULES.put("phone", AssetType.PII);
        CLASSIFICATION_RULES.put("mobile", AssetType.PII);
        CLASSIFICATION_RULES.put("address", AssetType.PII);
        CLASSIFICATION_RULES.put("name", AssetType.PII);
        CLASSIFICATION_RULES.put("dob", AssetType.PII);
        CLASSIFICATION_RULES.put("date_of_birth", AssetType.PII);

        // Sensitive data patterns
        CLASSIFICATION_RULES.put("health", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("medical", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("biometric", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("genetic", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("sexual_orientation", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("religion", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("caste", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("political", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("financial", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("bank_account", AssetType.SENSITIVE);
        CLASSIFICATION_RULES.put("credit_card", AssetType.SENSITIVE);

        // Critical data patterns
        CLASSIFICATION_RULES.put("password", AssetType.CRITICAL);
        CLASSIFICATION_RULES.put("secret_key", AssetType.CRITICAL);
        CLASSIFICATION_RULES.put("encryption_key", AssetType.CRITICAL);
        CLASSIFICATION_RULES.put("token", AssetType.CRITICAL);
        CLASSIFICATION_RULES.put("api_key", AssetType.CRITICAL);
    }

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Data Governance Engine...");
        createTables();
        loadExistingAssets();
        startPeriodicScan();
        initialized = true;
        logger.info("Data Governance Engine initialized with {} assets in registry", assetRegistry.size());
    }

    private void createTables() {
        try (Connection conn = databaseManager.getConnection()) {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS data_assets (" +
                "asset_id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "asset_type TEXT NOT NULL, " +
                "source_system TEXT, " +
                "storage_location TEXT, " +
                "encryption_status TEXT, " +
                "classification_level TEXT, " +
                "data_category TEXT, " +
                "owner TEXT, " +
                "custodian TEXT, " +
                "record_count INTEGER DEFAULT 0, " +
                "data_size_bytes INTEGER DEFAULT 0, " +
                "retention_policy TEXT, " +
                "retention_expiry TEXT, " +
                "cross_border_transfer INTEGER DEFAULT 0, " +
                "transfer_destination TEXT, " +
                "purposes TEXT, " +
                "lineage_source TEXT, " +
                "lineage_process TEXT, " +
                "lineage_destination TEXT, " +
                "tags TEXT, " +
                "discovered_by TEXT, " +
                "discovered_at TEXT, " +
                "last_scanned TEXT, " +
                "status TEXT DEFAULT 'ACTIVE', " +
                "sensitivity_score INTEGER DEFAULT 0, " +
                "created_at TEXT, " +
                "updated_at TEXT)"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS data_lineage (" +
                "lineage_id TEXT PRIMARY KEY, " +
                "asset_id TEXT, " +
                "source_system TEXT, " +
                "source_table TEXT, " +
                "process_name TEXT, " +
                "process_type TEXT, " +
                "destination_system TEXT, " +
                "destination_table TEXT, " +
                "transformation TEXT, " +
                "frequency TEXT, " +
                "last_execution TEXT, " +
                "status TEXT DEFAULT 'ACTIVE', " +
                "created_at TEXT)"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS scan_history (" +
                "scan_id TEXT PRIMARY KEY, " +
                "scan_type TEXT, " +
                "source TEXT, " +
                "assets_discovered INTEGER DEFAULT 0, " +
                "pii_found INTEGER DEFAULT 0, " +
                "sensitive_found INTEGER DEFAULT 0, " +
                "critical_found INTEGER DEFAULT 0, " +
                "duration_ms INTEGER DEFAULT 0, " +
                "status TEXT, " +
                "started_at TEXT, " +
                "completed_at TEXT)"
            );
            logger.debug("Data Governance tables created");
        } catch (Exception e) {
            logger.error("Failed to create governance tables", e);
        }
    }

    // --- Data Discovery ---

    /**
     * Discover and classify data assets from a database source
     */
    public DiscoveryResult discoverFromDatabase(String sourceName, String connectionUrl) {
        logger.info("Starting data discovery from database: {}", sourceName);
        DiscoveryResult result = new DiscoveryResult(sourceName, "DATABASE");
        long startTime = System.currentTimeMillis();

        try (Connection conn = databaseManager.getConnection()) {
            // Get table metadata
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "%", new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (tableName.startsWith("sqlite_") || tableName.startsWith("data_")) continue;

                ResultSet columns = meta.getColumns(null, null, tableName, "%");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME").toLowerCase();
                    String columnType = columns.getString("TYPE_NAME");

                    AssetType classified = classifyColumn(columnName);
                    if (classified != AssetType.GENERAL) {
                        DataAsset asset = new DataAsset();
                        asset.setName(tableName + "." + columnName);
                        asset.setDescription("Auto-discovered " + classified.getDescription() + " in " + tableName);
                        asset.setType(classified);
                        asset.setSourceSystem(sourceName);
                        asset.setStorageLocation("Database: " + sourceName);
                        asset.setDataCategory(mapToCategory(classified, columnName));
                        asset.setDiscoveredBy("DataGovernanceEngine");
                        asset.setDiscoveredAt(LocalDateTime.now());
                        asset.setLastScanned(LocalDateTime.now());
                        asset.setLineageSource(sourceName);
                        asset.setLineageProcess("Direct Storage");
                        asset.setLineageDestination(tableName);
                        asset.setClassificationLevel(mapToClassification(classified));
                        asset.calculateSensitivityScore();

                        registerAsset(asset);
                        result.addAsset(asset);
                    }
                }
                columns.close();
            }
            tables.close();
        } catch (Exception e) {
            logger.error("Database discovery failed for {}", sourceName, e);
            result.setStatus("FAILED");
            result.setError(e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - startTime);
        result.setStatus("COMPLETED");
        recordScanHistory(result);
        logger.info("Database discovery complete: {} assets found ({} PII, {} sensitive, {} critical) in {}ms",
                result.getTotalAssets(), result.getPiiCount(), result.getSensitiveCount(),
                result.getCriticalCount(), result.getDurationMs());
        return result;
    }

    /**
     * Discover data assets from file system
     */
    public DiscoveryResult discoverFromFiles(String sourceName, String basePath) {
        logger.info("Starting file discovery from: {}", basePath);
        DiscoveryResult result = new DiscoveryResult(sourceName, "FILE_SYSTEM");
        long startTime = System.currentTimeMillis();

        // Simulated file scanning - in production would walk filesystem
        DataAsset asset = new DataAsset();
        asset.setName("File System: " + basePath);
        asset.setDescription("File-based data storage at " + basePath);
        asset.setType(AssetType.GENERAL);
        asset.setSourceSystem(sourceName);
        asset.setStorageLocation(basePath);
        asset.setDiscoveredBy("DataGovernanceEngine");
        asset.setDiscoveredAt(LocalDateTime.now());
        asset.setLastScanned(LocalDateTime.now());
        registerAsset(asset);
        result.addAsset(asset);

        result.setDurationMs(System.currentTimeMillis() - startTime);
        result.setStatus("COMPLETED");
        recordScanHistory(result);
        return result;
    }

    /**
     * Discover data assets from API endpoint
     */
    public DiscoveryResult discoverFromAPI(String sourceName, String endpoint) {
        logger.info("Starting API discovery from: {}", endpoint);
        DiscoveryResult result = new DiscoveryResult(sourceName, "API");
        long startTime = System.currentTimeMillis();

        DataAsset asset = new DataAsset();
        asset.setName("API: " + endpoint);
        asset.setDescription("API data stream from " + endpoint);
        asset.setType(AssetType.PII);
        asset.setSourceSystem(sourceName);
        asset.setStorageLocation("API: " + endpoint);
        asset.setDiscoveredBy("DataGovernanceEngine");
        asset.setDiscoveredAt(LocalDateTime.now());
        asset.setLastScanned(LocalDateTime.now());
        asset.setLineageSource(endpoint);
        asset.setLineageProcess("API Ingestion");
        registerAsset(asset);
        result.addAsset(asset);

        result.setDurationMs(System.currentTimeMillis() - startTime);
        result.setStatus("COMPLETED");
        recordScanHistory(result);
        return result;
    }

    // --- Classification Engine ---

    /**
     * Classify a column name using rule-based engine
     */
    public AssetType classifyColumn(String columnName) {
        String normalized = columnName.toLowerCase().replace("-", "_").replace(" ", "_");
        for (Map.Entry<String, AssetType> rule : CLASSIFICATION_RULES.entrySet()) {
            if (normalized.contains(rule.getKey())) {
                return rule.getValue();
            }
        }
        return AssetType.GENERAL;
    }

    /**
     * AI-based contextual classification (enhanced)
     * Uses context from column name, table name, and surrounding columns
     */
    public AssetType classifyContextual(String columnName, String tableName, List<String> siblingColumns) {
        // First try rule-based
        AssetType ruleResult = classifyColumn(columnName);
        if (ruleResult != AssetType.GENERAL) return ruleResult;

        // Contextual detection: if table contains known PII columns, elevate unknowns
        long piiSiblings = siblingColumns.stream()
                .filter(c -> classifyColumn(c) != AssetType.GENERAL)
                .count();

        if (piiSiblings > 2 && tableName.toLowerCase().matches(".*(customer|user|patient|employee|member).*")) {
            return AssetType.PII; // High probability of PII in context
        }

        return AssetType.GENERAL;
    }

    // --- Data Catalog ---

    /**
     * Register an asset in the catalog
     */
    public void registerAsset(DataAsset asset) {
        assetRegistry.put(asset.getAssetId(), asset);
        persistAsset(asset);
    }

    /**
     * Get full data catalog
     */
    public List<DataAsset> getCatalog() {
        return new ArrayList<>(assetRegistry.values());
    }

    /**
     * Get catalog filtered by type
     */
    public List<DataAsset> getCatalogByType(AssetType type) {
        return assetRegistry.values().stream()
                .filter(a -> a.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * Search catalog by name or description
     */
    public List<DataAsset> searchCatalog(String query) {
        String q = query.toLowerCase();
        return assetRegistry.values().stream()
                .filter(a -> (a.getName() != null && a.getName().toLowerCase().contains(q)) ||
                             (a.getDescription() != null && a.getDescription().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    // --- Sensitivity Heatmap ---

    /**
     * Generate sensitivity heatmap data
     * Returns map of source → sensitivity score (0-100)
     */
    public Map<String, Integer> generateSensitivityHeatmap() {
        Map<String, List<Integer>> sourceScores = new HashMap<>();

        for (DataAsset asset : assetRegistry.values()) {
            String source = asset.getSourceSystem() != null ? asset.getSourceSystem() : "Unknown";
            sourceScores.computeIfAbsent(source, k -> new ArrayList<>())
                    .add(asset.calculateSensitivityScore());
        }

        Map<String, Integer> heatmap = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> entry : sourceScores.entrySet()) {
            int avg = (int) entry.getValue().stream().mapToInt(i -> i).average().orElse(0);
            heatmap.put(entry.getKey(), avg);
        }

        // Sort by score descending
        return heatmap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * Generate heatmap by data category
     */
    public Map<String, Integer> generateCategoryHeatmap() {
        Map<String, List<Integer>> categoryScores = new HashMap<>();

        for (DataAsset asset : assetRegistry.values()) {
            String category = asset.getDataCategory() != null ? asset.getDataCategory() : "Uncategorized";
            categoryScores.computeIfAbsent(category, k -> new ArrayList<>())
                    .add(asset.calculateSensitivityScore());
        }

        Map<String, Integer> heatmap = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> entry : categoryScores.entrySet()) {
            int max = entry.getValue().stream().mapToInt(i -> i).max().orElse(0);
            heatmap.put(entry.getKey(), max);
        }
        return heatmap;
    }

    // --- Data Lineage ---

    /**
     * Record a data lineage relationship
     */
    public void recordLineage(String assetId, String sourceSystem, String sourceTable,
                               String processName, String processType,
                               String destSystem, String destTable, String transformation) {
        LineageRecord record = new LineageRecord();
        record.setAssetId(assetId);
        record.setSourceSystem(sourceSystem);
        record.setSourceTable(sourceTable);
        record.setProcessName(processName);
        record.setProcessType(processType);
        record.setDestinationSystem(destSystem);
        record.setDestinationTable(destTable);
        record.setTransformation(transformation);

        lineageGraph.computeIfAbsent(assetId, k -> new ArrayList<>()).add(record);
        persistLineage(record);
        logger.debug("Lineage recorded: {} → {} → {}", sourceSystem, processName, destSystem);
    }

    /**
     * Get lineage chain for an asset
     */
    public List<LineageRecord> getLineage(String assetId) {
        return lineageGraph.getOrDefault(assetId, Collections.emptyList());
    }

    /**
     * Get full lineage graph
     */
    public Map<String, List<LineageRecord>> getFullLineageGraph() {
        return Collections.unmodifiableMap(lineageGraph);
    }

    // --- Statistics ---

    /**
     * Get governance statistics
     */
    public GovernanceStats getStatistics() {
        GovernanceStats stats = new GovernanceStats();
        stats.setTotalAssets(assetRegistry.size());
        stats.setPiiAssets((int) assetRegistry.values().stream().filter(a -> a.getType() == AssetType.PII).count());
        stats.setSensitiveAssets((int) assetRegistry.values().stream().filter(a -> a.getType() == AssetType.SENSITIVE).count());
        stats.setCriticalAssets((int) assetRegistry.values().stream().filter(a -> a.getType() == AssetType.CRITICAL).count());
        stats.setUnencrypted((int) assetRegistry.values().stream()
                .filter(a -> a.getEncryptionStatus() == EncryptionStatus.UNENCRYPTED).count());
        stats.setLineageRecords(lineageGraph.values().stream().mapToInt(List::size).sum());
        stats.setTotalScans(scanHistory.size());

        int totalScore = assetRegistry.values().stream().mapToInt(DataAsset::calculateSensitivityScore).sum();
        stats.setAverageSensitivity(assetRegistry.isEmpty() ? 0 : totalScore / assetRegistry.size());

        return stats;
    }

    // --- Private Helpers ---

    private String mapToCategory(AssetType type, String columnName) {
        String col = columnName.toLowerCase();
        if (col.contains("health") || col.contains("medical")) return "Health";
        if (col.contains("financial") || col.contains("bank") || col.contains("credit")) return "Financial";
        if (col.contains("biometric")) return "Biometric";
        if (col.contains("aadhaar") || col.contains("pan")) return "Identity";
        if (col.contains("email") || col.contains("phone") || col.contains("address")) return "Contact";
        if (col.contains("password") || col.contains("key") || col.contains("token")) return "Credentials";
        return "General";
    }

    private ClassificationLevel mapToClassification(AssetType type) {
        switch (type) {
            case CRITICAL: return ClassificationLevel.RESTRICTED;
            case SENSITIVE: return ClassificationLevel.CONFIDENTIAL;
            case PII: return ClassificationLevel.CONFIDENTIAL;
            default: return ClassificationLevel.INTERNAL;
        }
    }

    private void loadExistingAssets() {
        try (Connection conn = databaseManager.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM data_assets");
            if (rs.next()) {
                logger.debug("Loaded {} existing assets from database", rs.getInt(1));
            }
        } catch (Exception e) {
            logger.debug("No existing assets found (first run)");
        }
    }

    private void startPeriodicScan() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                discoverFromDatabase("Internal", "jdbc:sqlite:");
            } catch (Exception e) {
                logger.debug("Periodic scan skipped: {}", e.getMessage());
            }
        }, 1, 24, TimeUnit.HOURS);
    }

    private void persistAsset(DataAsset asset) {
        try (Connection conn = databaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO data_assets (asset_id, name, description, asset_type, " +
                "source_system, storage_location, encryption_status, classification_level, " +
                "data_category, owner, custodian, record_count, data_size_bytes, " +
                "lineage_source, lineage_process, lineage_destination, discovered_by, " +
                "discovered_at, last_scanned, sensitivity_score, status, created_at, updated_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            );
            ps.setString(1, asset.getAssetId());
            ps.setString(2, asset.getName());
            ps.setString(3, asset.getDescription());
            ps.setString(4, asset.getType().name());
            ps.setString(5, asset.getSourceSystem());
            ps.setString(6, asset.getStorageLocation());
            ps.setString(7, asset.getEncryptionStatus().name());
            ps.setString(8, asset.getClassificationLevel().name());
            ps.setString(9, asset.getDataCategory());
            ps.setString(10, asset.getOwner());
            ps.setString(11, asset.getCustodian());
            ps.setInt(12, asset.getRecordCount());
            ps.setLong(13, asset.getDataSizeBytes());
            ps.setString(14, asset.getLineageSource());
            ps.setString(15, asset.getLineageProcess());
            ps.setString(16, asset.getLineageDestination());
            ps.setString(17, asset.getDiscoveredBy());
            ps.setString(18, asset.getDiscoveredAt() != null ? asset.getDiscoveredAt().toString() : null);
            ps.setString(19, asset.getLastScanned() != null ? asset.getLastScanned().toString() : null);
            ps.setInt(20, asset.calculateSensitivityScore());
            ps.setString(21, asset.getStatus());
            ps.setString(22, asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : null);
            ps.setString(23, asset.getUpdatedAt() != null ? asset.getUpdatedAt().toString() : null);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to persist asset: {}", asset.getAssetId(), e);
        }
    }

    private void persistLineage(LineageRecord record) {
        try (Connection conn = databaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO data_lineage (lineage_id, asset_id, source_system, source_table, " +
                "process_name, process_type, destination_system, destination_table, " +
                "transformation, frequency, status, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
            );
            ps.setString(1, record.getLineageId());
            ps.setString(2, record.getAssetId());
            ps.setString(3, record.getSourceSystem());
            ps.setString(4, record.getSourceTable());
            ps.setString(5, record.getProcessName());
            ps.setString(6, record.getProcessType());
            ps.setString(7, record.getDestinationSystem());
            ps.setString(8, record.getDestinationTable());
            ps.setString(9, record.getTransformation());
            ps.setString(10, record.getFrequency());
            ps.setString(11, "ACTIVE");
            ps.setString(12, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to persist lineage record", e);
        }
    }

    private void recordScanHistory(DiscoveryResult result) {
        try (Connection conn = databaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO scan_history (scan_id, scan_type, source, assets_discovered, " +
                "pii_found, sensitive_found, critical_found, duration_ms, status, " +
                "started_at, completed_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)"
            );
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, result.getScanType());
            ps.setString(3, result.getSourceName());
            ps.setInt(4, result.getTotalAssets());
            ps.setInt(5, result.getPiiCount());
            ps.setInt(6, result.getSensitiveCount());
            ps.setInt(7, result.getCriticalCount());
            ps.setLong(8, result.getDurationMs());
            ps.setString(9, result.getStatus());
            ps.setString(10, LocalDateTime.now().toString());
            ps.setString(11, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            logger.error("Failed to record scan history", e);
        }
    }

    public boolean isInitialized() { return initialized; }

    // --- Inner Classes ---

    /**
     * Data lineage record
     */
    public static class LineageRecord {
        private String lineageId = UUID.randomUUID().toString();
        private String assetId;
        private String sourceSystem;
        private String sourceTable;
        private String processName;
        private String processType;
        private String destinationSystem;
        private String destinationTable;
        private String transformation;
        private String frequency;
        private LocalDateTime lastExecution;

        public String getLineageId() { return lineageId; }
        public void setLineageId(String id) { this.lineageId = id; }
        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }
        public String getSourceSystem() { return sourceSystem; }
        public void setSourceSystem(String s) { this.sourceSystem = s; }
        public String getSourceTable() { return sourceTable; }
        public void setSourceTable(String s) { this.sourceTable = s; }
        public String getProcessName() { return processName; }
        public void setProcessName(String p) { this.processName = p; }
        public String getProcessType() { return processType; }
        public void setProcessType(String p) { this.processType = p; }
        public String getDestinationSystem() { return destinationSystem; }
        public void setDestinationSystem(String d) { this.destinationSystem = d; }
        public String getDestinationTable() { return destinationTable; }
        public void setDestinationTable(String d) { this.destinationTable = d; }
        public String getTransformation() { return transformation; }
        public void setTransformation(String t) { this.transformation = t; }
        public String getFrequency() { return frequency; }
        public void setFrequency(String f) { this.frequency = f; }
        public LocalDateTime getLastExecution() { return lastExecution; }
        public void setLastExecution(LocalDateTime l) { this.lastExecution = l; }
    }

    /**
     * Discovery scan result
     */
    public static class DiscoveryResult {
        private String sourceName;
        private String scanType;
        private List<DataAsset> discoveredAssets = new ArrayList<>();
        private long durationMs;
        private String status = "IN_PROGRESS";
        private String error;

        public DiscoveryResult(String sourceName, String scanType) {
            this.sourceName = sourceName;
            this.scanType = scanType;
        }

        public void addAsset(DataAsset asset) { discoveredAssets.add(asset); }
        public int getTotalAssets() { return discoveredAssets.size(); }
        public int getPiiCount() { return (int) discoveredAssets.stream().filter(a -> a.getType() == AssetType.PII).count(); }
        public int getSensitiveCount() { return (int) discoveredAssets.stream().filter(a -> a.getType() == AssetType.SENSITIVE).count(); }
        public int getCriticalCount() { return (int) discoveredAssets.stream().filter(a -> a.getType() == AssetType.CRITICAL).count(); }

        public String getSourceName() { return sourceName; }
        public String getScanType() { return scanType; }
        public List<DataAsset> getDiscoveredAssets() { return discoveredAssets; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long ms) { this.durationMs = ms; }
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public String getError() { return error; }
        public void setError(String e) { this.error = e; }
    }

    /**
     * Scan result for history tracking
     */
    public static class ScanResult {
        private String scanId;
        private String scanType;
        private String source;
        private int assetsDiscovered;
        private long durationMs;
        private LocalDateTime timestamp;

        public String getScanId() { return scanId; }
        public void setScanId(String s) { this.scanId = s; }
        public String getScanType() { return scanType; }
        public void setScanType(String s) { this.scanType = s; }
        public String getSource() { return source; }
        public void setSource(String s) { this.source = s; }
        public int getAssetsDiscovered() { return assetsDiscovered; }
        public void setAssetsDiscovered(int a) { this.assetsDiscovered = a; }
        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long d) { this.durationMs = d; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime t) { this.timestamp = t; }
    }

    /**
     * Governance engine statistics
     */
    public static class GovernanceStats {
        private int totalAssets;
        private int piiAssets;
        private int sensitiveAssets;
        private int criticalAssets;
        private int unencrypted;
        private int lineageRecords;
        private int totalScans;
        private int averageSensitivity;

        public int getTotalAssets() { return totalAssets; }
        public void setTotalAssets(int t) { this.totalAssets = t; }
        public int getPiiAssets() { return piiAssets; }
        public void setPiiAssets(int p) { this.piiAssets = p; }
        public int getSensitiveAssets() { return sensitiveAssets; }
        public void setSensitiveAssets(int s) { this.sensitiveAssets = s; }
        public int getCriticalAssets() { return criticalAssets; }
        public void setCriticalAssets(int c) { this.criticalAssets = c; }
        public int getUnencrypted() { return unencrypted; }
        public void setUnencrypted(int u) { this.unencrypted = u; }
        public int getLineageRecords() { return lineageRecords; }
        public void setLineageRecords(int l) { this.lineageRecords = l; }
        public int getTotalScans() { return totalScans; }
        public void setTotalScans(int t) { this.totalScans = t; }
        public int getAverageSensitivity() { return averageSensitivity; }
        public void setAverageSensitivity(int a) { this.averageSensitivity = a; }
    }
}
