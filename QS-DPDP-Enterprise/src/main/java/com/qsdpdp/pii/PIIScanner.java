package com.qsdpdp.pii;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.events.ComplianceEvent;
import com.qsdpdp.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

/**
 * PII Scanner Service - Core detection engine
 * Scans files, directories, text, and databases for PII
 * Compliant with DPDP Act 2023 data discovery requirements
 * 
 * @version 1.0.0
 * @since Phase 6
 */
@Service
public class PIIScanner {

    private static final Logger logger = LoggerFactory.getLogger(PIIScanner.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private final EventBus eventBus;
    private final SecurityManager securityManager;

    private boolean initialized = false;
    private final List<PIIPattern> activePatterns = new ArrayList<>();

    // Enhanced scanning support (Phase 7)
    private final Map<String, AtomicBoolean> activeScanCancellations = new ConcurrentHashMap<>();
    private final List<ScanProgressListener> progressListeners = new CopyOnWriteArrayList<>();
    private final Map<String, ScanSchedule> scheduledScans = new ConcurrentHashMap<>();
    private ScheduledExecutorService scanScheduler;

    // File extensions to scan
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".txt", ".csv", ".json", ".xml", ".html", ".htm", ".log",
            ".sql", ".md", ".yml", ".yaml", ".properties", ".ini", ".cfg");

    private static final Set<String> OFFICE_EXTENSIONS = Set.of(
            ".doc", ".docx", ".xls", ".xlsx", ".pdf");

    // Maximum file size to scan (50MB)
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Autowired
    public PIIScanner(DatabaseManager dbManager, AuditService auditService,
            EventBus eventBus, SecurityManager securityManager) {
        this.dbManager = dbManager;
        this.auditService = auditService;
        this.eventBus = eventBus;
        this.securityManager = securityManager;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing PII Scanner...");

        // Load all patterns
        Collections.addAll(activePatterns, PIIPattern.ALL_PATTERNS);
        logger.info("Loaded {} PII patterns", activePatterns.size());

        // Create database tables
        createTables();

        // Subscribe to events
        eventBus.subscribe("pii.*", e -> logger.debug("PII event: {}", e.getType()));

        initialized = true;
        logger.info("PII Scanner initialized");
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            // PII Scans table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS pii_scans (
                            id TEXT PRIMARY KEY,
                            scan_type TEXT NOT NULL,
                            source TEXT NOT NULL,
                            status TEXT NOT NULL,
                            bytes_scanned INTEGER DEFAULT 0,
                            files_scanned INTEGER DEFAULT 0,
                            total_findings INTEGER DEFAULT 0,
                            critical_findings INTEGER DEFAULT 0,
                            high_findings INTEGER DEFAULT 0,
                            started_at TIMESTAMP,
                            completed_at TIMESTAMP,
                            scanned_by TEXT,
                            error TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // PII Findings table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS pii_findings (
                            id TEXT PRIMARY KEY,
                            scan_id TEXT NOT NULL,
                            pii_type TEXT NOT NULL,
                            pattern_id TEXT,
                            masked_value TEXT,
                            value_hash TEXT NOT NULL,
                            source_path TEXT,
                            line_number INTEGER,
                            column_start INTEGER,
                            column_end INTEGER,
                            confidence REAL,
                            validated INTEGER DEFAULT 0,
                            context TEXT,
                            risk_level TEXT,
                            status TEXT DEFAULT 'ACTIVE',
                            found_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            remediated_at TIMESTAMP,
                            remediated_by TEXT,
                            FOREIGN KEY (scan_id) REFERENCES pii_scans(id)
                        )
                    """);

            // PII Inventory table (aggregated view)
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS pii_inventory (
                            id TEXT PRIMARY KEY,
                            pii_type TEXT NOT NULL,
                            source_type TEXT NOT NULL,
                            source_path TEXT NOT NULL,
                            first_detected TIMESTAMP,
                            last_detected TIMESTAMP,
                            occurrence_count INTEGER DEFAULT 1,
                            risk_level TEXT,
                            status TEXT DEFAULT 'ACTIVE',
                            owner TEXT,
                            notes TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Indexes
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_findings_scan ON pii_findings(scan_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_findings_type ON pii_findings(pii_type)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_findings_risk ON pii_findings(risk_level)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_inventory_type ON pii_inventory(pii_type)");

            logger.info("PII Scanner tables created");

        } catch (SQLException e) {
            logger.error("Failed to create PII tables", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SCANNING METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Scan a text string for PII
     */
    public PIIScanResult scanText(String text, String source) {
        logger.info("Scanning text for PII: {} bytes", text.length());

        PIIScanResult result = new PIIScanResult(source, "TEXT");
        result.setBytesScanned(text.length());

        try {
            scanContent(text, source, 1, result);
            result.complete();

            auditService.log("PII_SCAN_TEXT", "PII", "SYSTEM",
                    String.format("Text scan completed: %d findings", result.getTotalFindings()));

        } catch (Exception e) {
            logger.error("Text scan failed", e);
            result.fail(e.getMessage());
        }

        return result;
    }

    /**
     * Scan a single file for PII
     */
    public PIIScanResult scanFile(Path filePath) {
        logger.info("Scanning file: {}", filePath);

        PIIScanResult result = new PIIScanResult(filePath.toString(), "FILE");

        try {
            if (!Files.exists(filePath)) {
                result.fail("File not found: " + filePath);
                return result;
            }

            long fileSize = Files.size(filePath);
            if (fileSize > MAX_FILE_SIZE) {
                result.fail("File too large: " + fileSize + " bytes (max: " + MAX_FILE_SIZE + ")");
                return result;
            }

            result.setBytesScanned(fileSize);
            result.setFilesScanned(1);

            // Read and scan file
            String extension = getFileExtension(filePath.toString()).toLowerCase();
            if (TEXT_EXTENSIONS.contains(extension)) {
                scanTextFile(filePath, result);
            } else if (OFFICE_EXTENSIONS.contains(extension)) {
                // Office files need special handling - skip for now
                logger.warn("Office file scanning not implemented: {}", extension);
            } else {
                // Try as text anyway
                scanTextFile(filePath, result);
            }

            result.complete();
            persistScanResult(result);

            auditService.log("PII_SCAN_FILE", "PII", "SYSTEM",
                    String.format("File scan: %s - %d findings", filePath.getFileName(), result.getTotalFindings()));

            eventBus.publish(new ComplianceEvent("pii.scan.completed",
                    Map.of("scanId", result.getScanId(), "findings", result.getTotalFindings())));

        } catch (Exception e) {
            logger.error("File scan failed: {}", filePath, e);
            result.fail(e.getMessage());
        }

        return result;
    }

    /**
     * Scan a directory recursively for PII
     */
    public PIIScanResult scanDirectory(Path directory, boolean recursive) {
        logger.info("Scanning directory: {} (recursive: {})", directory, recursive);

        PIIScanResult result = new PIIScanResult(directory.toString(), "DIRECTORY");

        try {
            if (!Files.isDirectory(directory)) {
                result.fail("Not a directory: " + directory);
                return result;
            }

            int depth = recursive ? Integer.MAX_VALUE : 1;
            long totalBytes = 0;
            int fileCount = 0;

            try (var stream = Files.walk(directory, depth)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .filter(this::isScannableFile)
                        .toList();

                for (Path file : files) {
                    try {
                        long fileSize = Files.size(file);
                        if (fileSize <= MAX_FILE_SIZE) {
                            scanTextFile(file, result);
                            totalBytes += fileSize;
                            fileCount++;
                        }
                    } catch (Exception e) {
                        logger.warn("Error scanning file: {}", file, e);
                    }
                }
            }

            result.setBytesScanned(totalBytes);
            result.setFilesScanned(fileCount);
            result.complete();
            persistScanResult(result);

            auditService.log("PII_SCAN_DIRECTORY", "PII", "SYSTEM",
                    String.format("Directory scan: %s - %d files, %d findings",
                            directory, fileCount, result.getTotalFindings()));

        } catch (Exception e) {
            logger.error("Directory scan failed: {}", directory, e);
            result.fail(e.getMessage());
        }

        return result;
    }

    /**
     * Scan a database table column for PII
     */
    public PIIScanResult scanDatabaseTable(String tableName, String... columns) {
        logger.info("Scanning database table: {} columns: {}", tableName, Arrays.toString(columns));

        PIIScanResult result = new PIIScanResult(tableName, "DATABASE");

        try (Connection conn = dbManager.getConnection()) {
            String columnList = columns.length > 0 ? String.join(", ", columns) : "*";
            String sql = "SELECT rowid, " + columnList + " FROM " + tableName;

            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {

                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                int rowCount = 0;

                while (rs.next()) {
                    rowCount++;
                    long rowId = rs.getLong(1);

                    for (int i = 2; i <= colCount; i++) {
                        String value = rs.getString(i);
                        if (value != null && !value.isEmpty()) {
                            String colName = meta.getColumnName(i);
                            String sourcePath = tableName + "." + colName + "[row:" + rowId + "]";
                            scanContent(value, sourcePath, (int) rowId, result);
                        }
                    }
                }

                result.setFilesScanned(rowCount);
            }

            result.complete();
            persistScanResult(result);

            auditService.log("PII_SCAN_DATABASE", "PII", "SYSTEM",
                    String.format("Table scan: %s - %d findings", tableName, result.getTotalFindings()));

        } catch (Exception e) {
            logger.error("Database scan failed: {}", tableName, e);
            result.fail(e.getMessage());
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL SCANNING METHODS
    // ═══════════════════════════════════════════════════════════

    private void scanTextFile(Path filePath, PIIScanResult result) throws IOException {
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
            String line = lines.get(lineNum);
            scanContent(line, filePath.toString(), lineNum + 1, result);
        }
    }

    private void scanContent(String content, String source, int lineNumber, PIIScanResult result) {
        if (content == null || content.isEmpty())
            return;

        for (PIIPattern pattern : activePatterns) {
            Matcher matcher = pattern.getPattern().matcher(content);

            while (matcher.find()) {
                String match = matcher.group();
                int start = matcher.start();
                int end = matcher.end();

                // Calculate confidence
                double confidence = pattern.getConfidence();

                // Validate if required
                boolean validated = false;
                if (pattern.isRequiresValidation()) {
                    validated = validatePII(pattern.getType(), match);
                    if (!validated) {
                        confidence *= 0.5; // Reduce confidence if validation fails
                    }
                }

                // Only report if confidence is above threshold
                if (confidence >= 0.5) {
                    String valueHash = securityManager.sha256(match);
                    String context = extractContext(content, start, end);

                    PIIFinding finding = PIIFinding.builder()
                            .type(pattern.getType())
                            .patternId(pattern.getId())
                            .value(match, valueHash)
                            .location(source, lineNumber, start, end)
                            .confidence(confidence)
                            .validated(validated)
                            .context(context)
                            .build();

                    result.addFinding(finding);
                }
            }
        }
    }

    private String extractContext(String content, int start, int end) {
        int contextStart = Math.max(0, start - 20);
        int contextEnd = Math.min(content.length(), end + 20);
        String context = content.substring(contextStart, contextEnd);

        // Mask the actual PII value in context
        String masked = content.substring(start, end);
        return context.replace(masked, "[REDACTED]");
    }

    private boolean validatePII(PIIType type, String value) {
        String cleanValue = value.replaceAll("[\\s-]", "");

        return switch (type) {
            case AADHAAR -> validateAadhaar(cleanValue);
            case PAN -> validatePAN(cleanValue);
            case CREDIT_CARD -> validateLuhn(cleanValue);
            case GST_NUMBER -> validateGST(cleanValue);
            default -> true;
        };
    }

    /**
     * Validate Aadhaar using Verhoeff algorithm
     */
    private boolean validateAadhaar(String aadhaar) {
        if (aadhaar.length() != 12)
            return false;
        if (!aadhaar.matches("\\d{12}"))
            return false;

        // Verhoeff algorithm tables
        int[][] d = {
                { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                { 1, 2, 3, 4, 0, 6, 7, 8, 9, 5 },
                { 2, 3, 4, 0, 1, 7, 8, 9, 5, 6 },
                { 3, 4, 0, 1, 2, 8, 9, 5, 6, 7 },
                { 4, 0, 1, 2, 3, 9, 5, 6, 7, 8 },
                { 5, 9, 8, 7, 6, 0, 4, 3, 2, 1 },
                { 6, 5, 9, 8, 7, 1, 0, 4, 3, 2 },
                { 7, 6, 5, 9, 8, 2, 1, 0, 4, 3 },
                { 8, 7, 6, 5, 9, 3, 2, 1, 0, 4 },
                { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0 }
        };

        int[][] p = {
                { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 },
                { 1, 5, 7, 6, 2, 8, 3, 0, 9, 4 },
                { 5, 8, 0, 3, 7, 9, 6, 1, 4, 2 },
                { 8, 9, 1, 6, 0, 4, 3, 5, 2, 7 },
                { 9, 4, 5, 3, 1, 2, 6, 8, 7, 0 },
                { 4, 2, 8, 6, 5, 7, 3, 9, 0, 1 },
                { 2, 7, 9, 3, 8, 0, 6, 4, 1, 5 },
                { 7, 0, 4, 6, 9, 1, 3, 2, 5, 8 }
        };

        int c = 0;
        int[] digits = new int[12];
        for (int i = 0; i < 12; i++) {
            digits[i] = aadhaar.charAt(11 - i) - '0';
        }

        for (int i = 0; i < 12; i++) {
            c = d[c][p[i % 8][digits[i]]];
        }

        return c == 0;
    }

    /**
     * Validate PAN format
     */
    private boolean validatePAN(String pan) {
        return pan.length() == 10 && pan.matches("[A-Z]{3}[ABCFGHLJPTK][A-Z][0-9]{4}[A-Z]");
    }

    /**
     * Validate credit card using Luhn algorithm
     */
    private boolean validateLuhn(String number) {
        if (number.length() < 13 || number.length() > 19)
            return false;

        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9)
                    n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }

        return sum % 10 == 0;
    }

    /**
     * Validate GST number format
     */
    private boolean validateGST(String gst) {
        if (gst.length() != 15)
            return false;

        // First 2 digits: state code (01-37)
        int stateCode = Integer.parseInt(gst.substring(0, 2));
        if (stateCode < 1 || stateCode > 37)
            return false;

        // Next 10 chars: PAN
        String pan = gst.substring(2, 12);
        if (!validatePAN(pan))
            return false;

        // 13th char: entity code (1-9 or Z)
        char entityCode = gst.charAt(12);
        if (!Character.isDigit(entityCode) && entityCode != 'Z')
            return false;

        // 14th char must be Z
        if (gst.charAt(13) != 'Z')
            return false;

        return true;
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    private boolean isScannableFile(Path path) {
        String ext = getFileExtension(path.toString()).toLowerCase();
        return TEXT_EXTENSIONS.contains(ext);
    }

    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot) : "";
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    private void persistScanResult(PIIScanResult result) {
        try (Connection conn = dbManager.getConnection()) {
            // Insert scan record
            String scanSql = """
                        INSERT INTO pii_scans (id, scan_type, source, status, bytes_scanned, files_scanned,
                            total_findings, critical_findings, high_findings, started_at, completed_at, scanned_by, error)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(scanSql)) {
                stmt.setString(1, result.getScanId());
                stmt.setString(2, result.getScanType());
                stmt.setString(3, result.getSource());
                stmt.setString(4, result.getStatus());
                stmt.setLong(5, result.getBytesScanned());
                stmt.setInt(6, result.getFilesScanned());
                stmt.setInt(7, result.getTotalFindings());
                stmt.setInt(8, result.getCriticalFindings());
                stmt.setInt(9, result.getHighFindings());
                stmt.setString(10, result.getStartTime().toString());
                stmt.setString(11, result.getEndTime() != null ? result.getEndTime().toString() : null);
                stmt.setString(12, result.getScannedBy());
                stmt.setString(13, result.getError());
                stmt.executeUpdate();
            }

            // Insert findings
            String findingSql = """
                        INSERT INTO pii_findings (id, scan_id, pii_type, pattern_id, masked_value, value_hash,
                            source_path, line_number, column_start, column_end, confidence, validated,
                            context, risk_level, status, found_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement stmt = conn.prepareStatement(findingSql)) {
                for (PIIFinding finding : result.getFindings()) {
                    stmt.setString(1, finding.getId());
                    stmt.setString(2, result.getScanId());
                    stmt.setString(3, finding.getType().name());
                    stmt.setString(4, finding.getPatternId());
                    stmt.setString(5, finding.getMaskedValue());
                    stmt.setString(6, finding.getHash());
                    stmt.setString(7, finding.getSourcePath());
                    stmt.setInt(8, finding.getLineNumber());
                    stmt.setInt(9, finding.getColumnStart());
                    stmt.setInt(10, finding.getColumnEnd());
                    stmt.setDouble(11, finding.getConfidence());
                    stmt.setInt(12, finding.isValidated() ? 1 : 0);
                    stmt.setString(13, finding.getContext());
                    stmt.setString(14, finding.getRiskLevel());
                    stmt.setString(15, finding.getStatus());
                    stmt.setString(16, finding.getFoundAt().toString());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

        } catch (SQLException e) {
            logger.error("Failed to persist scan result", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // QUERY METHODS
    // ═══════════════════════════════════════════════════════════

    public List<PIIScanResult> getRecentScans(int limit) {
        List<PIIScanResult> scans = new ArrayList<>();
        String sql = "SELECT * FROM pii_scans ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                PIIScanResult result = new PIIScanResult();
                result.setScanId(rs.getString("id"));
                result.setScanType(rs.getString("scan_type"));
                result.setSource(rs.getString("source"));
                result.setStatus(rs.getString("status"));
                result.setBytesScanned(rs.getLong("bytes_scanned"));
                result.setFilesScanned(rs.getInt("files_scanned"));
                scans.add(result);
            }
        } catch (SQLException e) {
            logger.error("Failed to get recent scans", e);
        }

        return scans;
    }

    public PIIScanStatistics getStatistics() {
        PIIScanStatistics stats = new PIIScanStatistics();

        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pii_scans");
            if (rs.next())
                stats.setTotalScans(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM pii_findings WHERE status = 'ACTIVE'");
            if (rs.next())
                stats.setActiveFindings(rs.getInt(1));

            rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM pii_findings WHERE risk_level = 'CRITICAL' AND status = 'ACTIVE'");
            if (rs.next())
                stats.setCriticalFindings(rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(DISTINCT source_path) FROM pii_findings WHERE status = 'ACTIVE'");
            if (rs.next())
                stats.setAffectedSources(rs.getInt(1));

        } catch (SQLException e) {
            logger.error("Failed to get PII statistics", e);
        }

        return stats;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // ENHANCED SCANNING METHODS (Phase 7 - Antivirus-style)
    // ═══════════════════════════════════════════════════════════

    /**
     * Add a progress listener for scan notifications
     */
    public void addProgressListener(ScanProgressListener listener) {
        progressListeners.add(listener);
    }

    /**
     * Remove a progress listener
     */
    public void removeProgressListener(ScanProgressListener listener) {
        progressListeners.remove(listener);
    }

    /**
     * Scan an entire logical drive (e.g. "C:\", "D:\")
     */
    public PIIScanResult scanDrive(String driveLetter) {
        logger.info("Starting drive scan: {}", driveLetter);
        ScanProfile profile = ScanProfile.driveScan(driveLetter);
        return scanWithProfile(profile);
    }

    /**
     * Scan all local drives on the system (full system scan)
     */
    public PIIScanResult scanSystem() {
        logger.info("Starting full system scan");
        ScanProfile profile = ScanProfile.fullSystemScan();
        return scanWithProfile(profile);
    }

    /**
     * Scan a UNC/SMB network path (e.g. \\\\server\\share\\folder)
     */
    public PIIScanResult scanNetworkPath(String uncPath) {
        logger.info("Starting network path scan: {}", uncPath);
        ScanProfile profile = ScanProfile.networkScan(uncPath);
        return scanWithProfile(profile);
    }

    /**
     * Enumerate and scan all network shares on a host
     */
    public PIIScanResult scanNetworkShare(String host) {
        logger.info("Starting network share scan for host: {}", host);

        PIIScanResult combinedResult = new PIIScanResult(host, "NETWORK_SHARE");
        List<String> shares = getNetworkShares(host);

        for (String share : shares) {
            try {
                String sharePath = "\\\\" + host + "\\" + share;
                Path path = Paths.get(sharePath);
                if (Files.isDirectory(path)) {
                    PIIScanResult shareResult = scanNetworkPath(sharePath);
                    mergeResults(combinedResult, shareResult);
                }
            } catch (Exception e) {
                logger.warn("Cannot access share: {} on {}: {}", share, host, e.getMessage());
            }
        }

        combinedResult.complete();
        persistScanResult(combinedResult);
        return combinedResult;
    }

    /**
     * Execute scan based on a configurable ScanProfile
     */
    public PIIScanResult scanWithProfile(ScanProfile profile) {
        logger.info("Executing scan with profile: {} (type: {})", profile.getName(), profile.getTargetType());

        String scanId = UUID.randomUUID().toString();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        activeScanCancellations.put(scanId, cancelled);

        // Notify listeners
        for (ScanProgressListener listener : progressListeners) {
            listener.onScanStarted(profile);
        }

        PIIScanResult result;
        try {
            result = switch (profile.getTargetType()) {
                case SYSTEM -> executeSystemScan(profile, scanId, cancelled);
                case DRIVE -> executeDriveScan(profile, scanId, cancelled);
                case FOLDER -> executeFolderScan(profile, scanId, cancelled);
                case NETWORK_PATH -> executeNetworkPathScan(profile, scanId, cancelled);
                case FILE -> {
                    Path filePath = Paths.get(profile.getTargetPath());
                    yield scanFile(filePath);
                }
                case DATABASE -> {
                    yield scanDatabaseTable(profile.getTargetPath());
                }
                default -> {
                    PIIScanResult r = new PIIScanResult(profile.getTargetPath(), profile.getTargetType().name());
                    r.complete();
                    yield r;
                }
            };
        } finally {
            activeScanCancellations.remove(scanId);
        }

        // Notify completion
        for (ScanProgressListener listener : progressListeners) {
            listener.onComplete(result);
        }

        auditService.log("PII_SCAN_PROFILE", "PII", "SYSTEM",
                String.format("Profile scan [%s]: %s - %d findings, %d files",
                        profile.getName(), profile.getTargetType(),
                        result.getTotalFindings(), result.getFilesScanned()));

        return result;
    }

    /**
     * Cancel an in-progress scan
     */
    public boolean cancelScan(String scanId) {
        AtomicBoolean cancellation = activeScanCancellations.get(scanId);
        if (cancellation != null) {
            cancellation.set(true);
            logger.info("Scan cancellation requested: {}", scanId);
            for (ScanProgressListener listener : progressListeners) {
                listener.onScanCancelled(scanId, "User requested cancellation");
            }
            return true;
        }
        return false;
    }

    /**
     * Schedule a recurring scan
     */
    public String scheduleRecurringScan(ScanProfile profile, ScanSchedule schedule) {
        if (scanScheduler == null || scanScheduler.isShutdown()) {
            scanScheduler = Executors.newScheduledThreadPool(1);
        }

        profile.setSchedule(schedule);
        String scheduleId = schedule.getId();
        scheduledScans.put(scheduleId, schedule);

        long initialDelay = Math.max(1, java.time.Duration.between(
                LocalDateTime.now(),
                schedule.computeNextRun(LocalDateTime.now())).toMinutes());

        scanScheduler.scheduleAtFixedRate(() -> {
            ScanSchedule sched = scheduledScans.get(scheduleId);
            if (sched != null && sched.isDue(LocalDateTime.now())) {
                logger.info("Executing scheduled scan: {}", profile.getName());
                try {
                    scanWithProfile(profile);
                    sched.markExecuted();
                } catch (Exception e) {
                    logger.error("Scheduled scan failed: {}", profile.getName(), e);
                }
            }
        }, initialDelay, 60, TimeUnit.MINUTES);

        logger.info("Scheduled recurring scan: {} ({}) - next run in {} minutes",
                profile.getName(), schedule.getFrequency(), initialDelay);
        return scheduleId;
    }

    /**
     * Cancel a scheduled scan
     */
    public boolean cancelScheduledScan(String scheduleId) {
        ScanSchedule removed = scheduledScans.remove(scheduleId);
        if (removed != null) {
            removed.setEnabled(false);
            logger.info("Cancelled scheduled scan: {}", scheduleId);
            return true;
        }
        return false;
    }

    /**
     * Get all available system drives
     */
    public List<Map<String, Object>> getSystemDrives() {
        List<Map<String, Object>> drives = new ArrayList<>();
        for (File root : File.listRoots()) {
            Map<String, Object> driveInfo = new LinkedHashMap<>();
            driveInfo.put("path", root.getAbsolutePath());
            driveInfo.put("totalSpace", root.getTotalSpace());
            driveInfo.put("usableSpace", root.getUsableSpace());
            driveInfo.put("freeSpace", root.getFreeSpace());
            driveInfo.put("readable", root.canRead());
            driveInfo.put("writable", root.canWrite());
            drives.add(driveInfo);
        }
        return drives;
    }

    /**
     * Get network shares on a host (via net view or SMB enumeration)
     */
    public List<String> getNetworkShares(String host) {
        List<String> shares = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("net", "view", "\\\\" + host, "/all");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean inShareSection = false;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("---")) {
                        inShareSection = true;
                        continue;
                    }
                    if (inShareSection && !line.isBlank() && !line.startsWith("The command")) {
                        String shareName = line.split("\\s+")[0];
                        if (!shareName.isEmpty()) {
                            shares.add(shareName);
                        }
                    }
                }
            }
            process.waitFor(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Failed to enumerate shares on {}: {}", host, e.getMessage());
        }
        return shares;
    }

    /**
     * Get all predefined scan profile templates
     */
    public List<ScanProfile> getScanProfileTemplates() {
        return ScanProfile.getProfileTemplates();
    }

    /**
     * Get all active scheduled scans
     */
    public Map<String, ScanSchedule> getScheduledScans() {
        return Collections.unmodifiableMap(scheduledScans);
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL ENHANCED SCAN EXECUTION
    // ═══════════════════════════════════════════════════════════

    private PIIScanResult executeSystemScan(ScanProfile profile, String scanId, AtomicBoolean cancelled) {
        PIIScanResult combinedResult = new PIIScanResult("FULL_SYSTEM", "SYSTEM");

        for (File root : File.listRoots()) {
            if (cancelled.get()) {
                combinedResult.fail("Scan cancelled");
                return combinedResult;
            }
            if (root.canRead()) {
                ScanProfile driveProfile = ScanProfile.driveScan(root.getAbsolutePath());
                driveProfile.getExcludePaths().addAll(profile.getExcludePaths());
                driveProfile.setMaxFileSizeBytes(profile.getMaxFileSizeBytes());
                PIIScanResult driveResult = executeDriveScan(driveProfile, scanId, cancelled);
                mergeResults(combinedResult, driveResult);
            }
        }

        combinedResult.complete();
        persistScanResult(combinedResult);
        return combinedResult;
    }

    private PIIScanResult executeDriveScan(ScanProfile profile, String scanId, AtomicBoolean cancelled) {
        Path drivePath = Paths.get(profile.getTargetPath());
        return executePathScan(drivePath, profile, scanId, cancelled, "DRIVE");
    }

    private PIIScanResult executeFolderScan(ScanProfile profile, String scanId, AtomicBoolean cancelled) {
        Path folderPath = Paths.get(profile.getTargetPath());
        return executePathScan(folderPath, profile, scanId, cancelled, "FOLDER");
    }

    private PIIScanResult executeNetworkPathScan(ScanProfile profile, String scanId, AtomicBoolean cancelled) {
        Path networkPath = Paths.get(profile.getTargetPath());
        return executePathScan(networkPath, profile, scanId, cancelled, "NETWORK_PATH");
    }

    private PIIScanResult executePathScan(Path basePath, ScanProfile profile,
                                           String scanId, AtomicBoolean cancelled, String sourceType) {
        PIIScanResult result = new PIIScanResult(basePath.toString(), sourceType);

        try {
            if (!Files.isDirectory(basePath)) {
                result.fail("Not a directory: " + basePath);
                return result;
            }

            // Count files first for progress reporting
            AtomicLong totalFiles = new AtomicLong(0);
            AtomicLong scannedFiles = new AtomicLong(0);
            long totalBytesScanned = 0;

            int depth = profile.isRecursive() ? profile.getMaxDepth() : 1;

            // Walk and collect scannable files
            List<Path> filesToScan = new ArrayList<>();
            try (var stream = Files.walk(basePath, depth, FileVisitOption.FOLLOW_LINKS)) {
                stream.filter(Files::isRegularFile)
                      .filter(p -> isScannableWithProfile(p, profile))
                      .forEach(filesToScan::add);
            }
            totalFiles.set(filesToScan.size());

            // Scan each file with progress
            for (Path file : filesToScan) {
                if (cancelled.get()) {
                    result.fail("Scan cancelled by user");
                    return result;
                }

                try {
                    long fileSize = Files.size(file);
                    if (fileSize <= profile.getMaxFileSizeBytes()) {
                        scanTextFile(file, result);
                        totalBytesScanned += fileSize;
                    }
                } catch (Exception e) {
                    logger.warn("Error scanning file: {}", file, e);
                    for (ScanProgressListener listener : progressListeners) {
                        listener.onError(file.toString(), e);
                    }
                }

                long scanned = scannedFiles.incrementAndGet();
                int percent = totalFiles.get() > 0 ? (int) (scanned * 100 / totalFiles.get()) : 0;

                // Notify progress every 100 files or every 10%
                if (scanned % 100 == 0 || percent % 10 == 0) {
                    for (ScanProgressListener listener : progressListeners) {
                        listener.onProgress(percent, scanned, totalFiles.get(), file.toString());
                    }
                }
            }

            result.setBytesScanned(totalBytesScanned);
            result.setFilesScanned((int) scannedFiles.get());
            result.complete();
            persistScanResult(result);

        } catch (Exception e) {
            logger.error("Path scan failed: {}", basePath, e);
            result.fail(e.getMessage());
        }

        return result;
    }

    private boolean isScannableWithProfile(Path file, ScanProfile profile) {
        if (!isScannableFile(file)) return false;

        String fileName = file.getFileName().toString().toLowerCase();
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";

        // Check exclusion paths
        String pathStr = file.toString();
        for (String excludePath : profile.getExcludePaths()) {
            if (pathStr.contains(excludePath)) return false;
        }

        // Check extension filters
        if (!profile.getIncludeExtensions().isEmpty()) {
            if (!profile.getIncludeExtensions().contains(ext)) return false;
        }
        if (profile.getExcludeExtensions().contains(ext)) return false;

        // Check hidden files
        try {
            if (!profile.isScanHiddenFiles() && Files.isHidden(file)) return false;
        } catch (IOException ignored) {}

        return true;
    }

    private void mergeResults(PIIScanResult target, PIIScanResult source) {
        for (PIIFinding finding : source.getFindings()) {
            target.addFinding(finding);
        }
        target.setFilesScanned(target.getFilesScanned() + source.getFilesScanned());
        target.setBytesScanned(target.getBytesScanned() + source.getBytesScanned());
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS CLASS
    // ═══════════════════════════════════════════════════════════

    public static class PIIScanStatistics {
        private int totalScans;
        private int activeFindings;
        private int criticalFindings;
        private int affectedSources;

        public int getTotalScans() {
            return totalScans;
        }

        public void setTotalScans(int totalScans) {
            this.totalScans = totalScans;
        }

        public int getActiveFindings() {
            return activeFindings;
        }

        public void setActiveFindings(int activeFindings) {
            this.activeFindings = activeFindings;
        }

        public int getCriticalFindings() {
            return criticalFindings;
        }

        public void setCriticalFindings(int criticalFindings) {
            this.criticalFindings = criticalFindings;
        }

        public int getAffectedSources() {
            return affectedSources;
        }

        public void setAffectedSources(int affectedSources) {
            this.affectedSources = affectedSources;
        }
    }
}
