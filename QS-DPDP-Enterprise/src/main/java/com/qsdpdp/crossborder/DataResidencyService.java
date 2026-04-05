package com.qsdpdp.crossborder;

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
 * Data Residency Service — Cloud & Data Localisation Enforcement
 * 
 * Implements DPDP Act §16 cross-border data transfer restrictions:
 * - Data residency tagging with sovereign classification
 * - Hybrid cloud routing rules
 * - Residency violation detection and blocking
 * - Data localisation compliance dashboard
 *
 * @version 1.0.0
 * @since Phase 7 — Data Localisation Enhancement
 */
@Service
public class DataResidencyService {

    private static final Logger logger = LoggerFactory.getLogger(DataResidencyService.class);

    private final DatabaseManager dbManager;
    private final AuditService auditService;
    private boolean initialized = false;

    @Autowired
    public DataResidencyService(DatabaseManager dbManager, AuditService auditService) {
        this.dbManager = dbManager;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS data_residency_tags (
                    id TEXT PRIMARY KEY,
                    data_id TEXT NOT NULL,
                    data_type TEXT,
                    current_region TEXT NOT NULL,
                    required_region TEXT,
                    sovereign_classification TEXT DEFAULT 'STANDARD',
                    sector TEXT,
                    regulatory_reference TEXT,
                    tagged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    tagged_by TEXT,
                    UNIQUE(data_id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS residency_rules (
                    id TEXT PRIMARY KEY,
                    sector TEXT NOT NULL,
                    data_category TEXT NOT NULL,
                    required_region TEXT NOT NULL,
                    allowed_regions TEXT,
                    blocked_regions TEXT,
                    regulatory_reference TEXT,
                    enforcement_level TEXT DEFAULT 'STRICT',
                    active INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS residency_violations (
                    id TEXT PRIMARY KEY,
                    data_id TEXT,
                    source_region TEXT,
                    destination_region TEXT,
                    rule_id TEXT,
                    violation_type TEXT,
                    severity TEXT DEFAULT 'HIGH',
                    blocked INTEGER DEFAULT 1,
                    description TEXT,
                    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    resolved_at TIMESTAMP,
                    status TEXT DEFAULT 'OPEN'
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cloud_routing_config (
                    id TEXT PRIMARY KEY,
                    provider TEXT NOT NULL,
                    region TEXT NOT NULL,
                    data_classification TEXT,
                    is_sovereign INTEGER DEFAULT 0,
                    latency_ms INTEGER,
                    compliance_certified INTEGER DEFAULT 0,
                    active INTEGER DEFAULT 1
                )
            """);

            seedResidencyRules(stmt);
            seedCloudRouting(stmt);
            initialized = true;
            logger.info("DataResidencyService initialized");
        } catch (SQLException e) {
            logger.error("Failed to initialize DataResidencyService", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // RESIDENCY TAGGING
    // ═══════════════════════════════════════════════════════════

    public String tagDataResidency(String dataId, String dataType, String region,
                                    String sovereignClassification, String sector) {
        String id = UUID.randomUUID().toString();
        String sql = """
            INSERT OR REPLACE INTO data_residency_tags 
            (id, data_id, data_type, current_region, sovereign_classification, sector, tagged_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, dataId);
            ps.setString(3, dataType);
            ps.setString(4, region);
            ps.setString(5, sovereignClassification != null ? sovereignClassification : "STANDARD");
            ps.setString(6, sector);
            ps.setString(7, LocalDateTime.now().toString());
            ps.executeUpdate();
            auditService.log("DATA_RESIDENCY_TAGGED", "DATA_LOCALISATION", null,
                    "Tagged data " + dataId + " region=" + region + " sovereign=" + sovereignClassification);
        } catch (SQLException e) {
            logger.error("Failed to tag data residency", e);
        }
        return id;
    }

    /**
     * Enforce residency rules before data transfer.
     * Returns null if allowed, or a violation description if blocked.
     */
    public ResidencyCheckResult enforceResidencyRules(String dataId, String destinationRegion) {
        ResidencyCheckResult result = new ResidencyCheckResult();
        result.dataId = dataId;
        result.destinationRegion = destinationRegion;

        // Get data's current tag
        String tagSql = "SELECT * FROM data_residency_tags WHERE data_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(tagSql)) {
            ps.setString(1, dataId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String currentRegion = rs.getString("current_region");
                String sovereign = rs.getString("sovereign_classification");
                String sector = rs.getString("sector");
                result.sourceRegion = currentRegion;

                // Check if sovereign data is leaving India
                if ("SOVEREIGN".equalsIgnoreCase(sovereign) && !"IN".equalsIgnoreCase(destinationRegion)) {
                    result.allowed = false;
                    result.violationType = "SOVEREIGN_DATA_EXIT";
                    result.description = "Sovereign data cannot leave India — DPDP §16 restriction";
                    logViolation(dataId, currentRegion, destinationRegion, "SOVEREIGN_DATA_EXIT", result.description);
                    return result;
                }

                // Check sector-specific rules
                String ruleSql = "SELECT * FROM residency_rules WHERE sector = ? AND active = 1";
                try (PreparedStatement rps = conn.prepareStatement(ruleSql)) {
                    rps.setString(1, sector);
                    ResultSet rrs = rps.executeQuery();
                    while (rrs.next()) {
                        String blockedRegions = rrs.getString("blocked_regions");
                        if (blockedRegions != null && blockedRegions.contains(destinationRegion)) {
                            result.allowed = false;
                            result.violationType = "SECTOR_RESTRICTION";
                            result.description = "Sector " + sector + " blocks transfer to " + destinationRegion
                                    + " — " + rrs.getString("regulatory_reference");
                            logViolation(dataId, currentRegion, destinationRegion, "SECTOR_RESTRICTION", result.description);
                            return result;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to enforce residency rules", e);
        }

        result.allowed = true;
        result.description = "Transfer permitted";
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    // DASHBOARD & STATISTICS
    // ═══════════════════════════════════════════════════════════

    public Map<String, Object> getResidencyDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM data_residency_tags");
            if (rs.next()) dashboard.put("totalTaggedData", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM data_residency_tags WHERE sovereign_classification = 'SOVEREIGN'");
            if (rs.next()) dashboard.put("sovereignData", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM residency_violations WHERE status = 'OPEN'");
            if (rs.next()) dashboard.put("openViolations", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM residency_violations WHERE blocked = 1");
            if (rs.next()) dashboard.put("blockedTransfers", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM residency_rules WHERE active = 1");
            if (rs.next()) dashboard.put("activeRules", rs.getInt(1));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM cloud_routing_config WHERE is_sovereign = 1 AND active = 1");
            if (rs.next()) dashboard.put("sovereignCloudRegions", rs.getInt(1));

            // Region breakdown
            List<Map<String, Object>> byRegion = new ArrayList<>();
            rs = stmt.executeQuery("SELECT current_region, COUNT(*) as cnt FROM data_residency_tags GROUP BY current_region");
            while (rs.next()) {
                byRegion.add(Map.of("region", rs.getString(1), "count", rs.getInt(2)));
            }
            dashboard.put("dataByRegion", byRegion);

            dashboard.put("status", "OPERATIONAL");
        } catch (SQLException e) {
            logger.error("Failed to get residency dashboard", e);
            dashboard.put("status", "ERROR");
        }
        return dashboard;
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════════════════════

    private void logViolation(String dataId, String source, String dest, String type, String desc) {
        String sql = "INSERT INTO residency_violations (id, data_id, source_region, destination_region, violation_type, description) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, dataId);
            ps.setString(3, source);
            ps.setString(4, dest);
            ps.setString(5, type);
            ps.setString(6, desc);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to log residency violation", e);
        }
        auditService.log("RESIDENCY_VIOLATION", "DATA_LOCALISATION", null,
                "Violation: " + type + " dataId=" + dataId + " dest=" + dest);
    }

    private void seedResidencyRules(Statement stmt) throws SQLException {
        String sql = "INSERT OR IGNORE INTO residency_rules (id, sector, data_category, required_region, blocked_regions, regulatory_reference, enforcement_level) VALUES (?,?,?,?,?,?,?)";
        String[][] rules = {
            {"BFSI", "PAYMENT_DATA", "IN", "CN,RU,KP", "RBI Data Localisation Directive 2018", "STRICT"},
            {"BFSI", "AADHAAR_DATA", "IN", "ALL_NON_IN", "Aadhaar Act 2016 §8", "STRICT"},
            {"HEALTHCARE", "EHR_DATA", "IN", "CN,RU,KP", "ABDM Data Protection Policy", "STRICT"},
            {"GOVERNMENT", "CITIZEN_DATA", "IN", "ALL_NON_IN", "MeitY Guidelines 2022", "STRICT"},
            {"TELECOM", "CDR_DATA", "IN", "ALL_NON_IN", "DoT License Agreement", "STRICT"},
            {"INSURANCE", "CLAIMS_DATA", "IN", "CN,RU,KP", "IRDAI Guidelines", "MODERATE"},
            {"EDUCATION", "STUDENT_DATA", "IN", "CN,RU,KP", "NEP 2020 Data Norms", "MODERATE"},
        };
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(sql)) {
            for (String[] r : rules) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]);
                ps.setString(5, r[3]); ps.setString(6, r[4]); ps.setString(7, r[5]);
                ps.executeUpdate();
            }
        }
        logger.info("Seeded {} residency rules", rules.length);
    }

    private void seedCloudRouting(Statement stmt) throws SQLException {
        String sql = "INSERT OR IGNORE INTO cloud_routing_config (id, provider, region, data_classification, is_sovereign, compliance_certified) VALUES (?,?,?,?,?,?)";
        String[][] routes = {
            {"AWS", "ap-south-1", "SOVEREIGN", "1", "1"},
            {"AWS", "ap-south-2", "SOVEREIGN", "1", "1"},
            {"AZURE", "centralindia", "SOVEREIGN", "1", "1"},
            {"AZURE", "southindia", "SOVEREIGN", "1", "1"},
            {"GCP", "asia-south1", "SOVEREIGN", "1", "1"},
            {"GCP", "asia-south2", "SOVEREIGN", "1", "1"},
            {"MEGHRAJ", "delhi-dc", "SOVEREIGN", "1", "1"},
            {"MEGHRAJ", "hyderabad-dc", "SOVEREIGN", "1", "1"},
        };
        try (PreparedStatement ps = stmt.getConnection().prepareStatement(sql)) {
            for (String[] r : routes) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, r[0]); ps.setString(3, r[1]); ps.setString(4, r[2]);
                ps.setInt(5, Integer.parseInt(r[3])); ps.setInt(6, Integer.parseInt(r[4]));
                ps.executeUpdate();
            }
        }
        logger.info("Seeded {} cloud routing configs", routes.length);
    }

    public boolean isInitialized() { return initialized; }

    // DATA CLASS
    public static class ResidencyCheckResult {
        public String dataId, sourceRegion, destinationRegion, violationType, description;
        public boolean allowed = true;
    }
}
