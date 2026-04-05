package com.qsdpdp.pii;

import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Lineage Graph Service — End-to-End Data Flow Tracking
 * 
 * Tracks the complete journey of personal data:
 * Source → Ingestion → Processing → Storage → Sharing → Deletion
 * 
 * Provides:
 * - Visual-ready lineage graphs (nodes + edges)
 * - Cross-system data flow mapping
 * - DPDP S.8(9) compliance evidence
 * - Impact analysis for breach response
 * 
 * @version 1.0.0
 * @since Phase 2 — Data Integration Enhancement
 */
@Service
public class DataLineageGraphService {

    private static final Logger logger = LoggerFactory.getLogger(DataLineageGraphService.class);

    @Autowired(required = false) private DatabaseManager dbManager;

    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Data Lineage Graph Service...");
        createTables();
        initialized = true;
    }

    private void createTables() {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lineage_nodes (
                    id TEXT PRIMARY KEY,
                    node_type TEXT NOT NULL,
                    name TEXT NOT NULL,
                    system_name TEXT,
                    data_categories TEXT,
                    metadata TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS lineage_edges (
                    id TEXT PRIMARY KEY,
                    source_node TEXT NOT NULL,
                    target_node TEXT NOT NULL,
                    edge_type TEXT NOT NULL,
                    data_categories TEXT,
                    purpose TEXT,
                    consent_id TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        } catch (SQLException e) {
            logger.error("Failed to create lineage tables", e);
        }
    }

    /**
     * Record a lineage node (system, process, storage, etc.)
     */
    public Map<String, Object> addNode(String nodeType, String name, String systemName,
            List<String> dataCategories, Map<String, String> metadata) {
        String id = UUID.randomUUID().toString();
        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO lineage_nodes (id, node_type, name, system_name, data_categories, metadata) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, id);
                ps.setString(2, nodeType);
                ps.setString(3, name);
                ps.setString(4, systemName);
                ps.setString(5, dataCategories != null ? String.join(",", dataCategories) : "");
                ps.setString(6, metadata != null ? metadata.toString() : "{}");
                ps.executeUpdate();
            } catch (SQLException e) { logger.error("Failed to add lineage node", e); }
        }
        return Map.of("id", id, "nodeType", nodeType, "name", name, "systemName", systemName != null ? systemName : "");
    }

    /**
     * Record a lineage edge (data flow between nodes)
     */
    public Map<String, Object> addEdge(String sourceNode, String targetNode, String edgeType,
            List<String> dataCategories, String purpose, String consentId) {
        String id = UUID.randomUUID().toString();
        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO lineage_edges (id, source_node, target_node, edge_type, data_categories, purpose, consent_id) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, id);
                ps.setString(2, sourceNode);
                ps.setString(3, targetNode);
                ps.setString(4, edgeType);
                ps.setString(5, dataCategories != null ? String.join(",", dataCategories) : "");
                ps.setString(6, purpose);
                ps.setString(7, consentId);
                ps.executeUpdate();
            } catch (SQLException e) { logger.error("Failed to add lineage edge", e); }
        }
        return Map.of("id", id, "source", sourceNode, "target", targetNode, "type", edgeType);
    }

    /**
     * Get full lineage graph (nodes + edges)
     */
    public Map<String, Object> getGraph() {
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM lineage_nodes ORDER BY created_at")) {
                    while (rs.next()) {
                        nodes.add(Map.of("id", rs.getString("id"), "type", rs.getString("node_type"),
                                "name", rs.getString("name"), "system", rs.getString("system_name") != null ? rs.getString("system_name") : "",
                                "categories", rs.getString("data_categories")));
                    }
                }
                try (ResultSet rs = stmt.executeQuery("SELECT * FROM lineage_edges ORDER BY created_at")) {
                    while (rs.next()) {
                        edges.add(Map.of("id", rs.getString("id"), "source", rs.getString("source_node"),
                                "target", rs.getString("target_node"), "type", rs.getString("edge_type"),
                                "categories", rs.getString("data_categories"),
                                "purpose", rs.getString("purpose") != null ? rs.getString("purpose") : ""));
                    }
                }
            } catch (SQLException e) { logger.error("Failed to get lineage graph", e); }
        }

        // If no data, provide template graph
        if (nodes.isEmpty()) {
            nodes = getTemplateNodes();
            edges = getTemplateEdges();
        }

        return Map.of("nodes", nodes, "edges", edges,
                "nodeCount", nodes.size(), "edgeCount", edges.size(),
                "timestamp", LocalDateTime.now().toString());
    }

    /**
     * Get impact analysis for a specific data category (for breach response)
     */
    public Map<String, Object> getImpactAnalysis(String dataCategory) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataCategory", dataCategory);

        List<Map<String, Object>> affectedNodes = new ArrayList<>();
        List<Map<String, Object>> affectedEdges = new ArrayList<>();

        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM lineage_nodes WHERE data_categories LIKE ?")) {
                    ps.setString(1, "%" + dataCategory + "%");
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            affectedNodes.add(Map.of("id", rs.getString("id"),
                                    "name", rs.getString("name"), "type", rs.getString("node_type")));
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT * FROM lineage_edges WHERE data_categories LIKE ?")) {
                    ps.setString(1, "%" + dataCategory + "%");
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            affectedEdges.add(Map.of("source", rs.getString("source_node"),
                                    "target", rs.getString("target_node"), "type", rs.getString("edge_type")));
                        }
                    }
                }
            } catch (SQLException e) { /* silent */ }
        }

        result.put("affectedSystems", affectedNodes);
        result.put("affectedFlows", affectedEdges);
        result.put("totalAffected", affectedNodes.size());
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    private List<Map<String, Object>> getTemplateNodes() {
        return List.of(
            Map.of("id", "src-web", "type", "SOURCE", "name", "Web Application", "system", "Frontend", "categories", "NAME,EMAIL,PHONE"),
            Map.of("id", "src-mobile", "type", "SOURCE", "name", "Mobile App", "system", "Mobile", "categories", "NAME,PHONE,LOCATION"),
            Map.of("id", "src-api", "type", "SOURCE", "name", "Partner API", "system", "API Gateway", "categories", "PAN,AADHAAR,ACCOUNT"),
            Map.of("id", "proc-consent", "type", "PROCESSING", "name", "Consent Engine", "system", "UCCMP", "categories", "ALL"),
            Map.of("id", "proc-pii", "type", "PROCESSING", "name", "PII Scanner", "system", "UCCMP", "categories", "ALL"),
            Map.of("id", "store-db", "type", "STORAGE", "name", "Primary Database", "system", "SQLite/PostgreSQL", "categories", "ALL"),
            Map.of("id", "store-backup", "type", "STORAGE", "name", "Encrypted Backup", "system", "AES-256", "categories", "ALL"),
            Map.of("id", "share-rbi", "type", "SHARING", "name", "RBI Reporting", "system", "RBI Gateway", "categories", "PAN,ACCOUNT"),
            Map.of("id", "share-certin", "type", "SHARING", "name", "CERT-In Reporting", "system", "CERT-In", "categories", "BREACH_DATA"),
            Map.of("id", "del-crypto", "type", "DELETION", "name", "Crypto Erase", "system", "CryptoErase Module", "categories", "ALL")
        );
    }

    private List<Map<String, Object>> getTemplateEdges() {
        return List.of(
            Map.of("id", "e1", "source", "src-web", "target", "proc-consent", "type", "INGESTION", "categories", "NAME,EMAIL", "purpose", "Registration"),
            Map.of("id", "e2", "source", "src-mobile", "target", "proc-consent", "type", "INGESTION", "categories", "NAME,PHONE", "purpose", "Mobile KYC"),
            Map.of("id", "e3", "source", "src-api", "target", "proc-pii", "type", "INGESTION", "categories", "PAN,AADHAAR", "purpose", "KYC Verification"),
            Map.of("id", "e4", "source", "proc-consent", "target", "store-db", "type", "STORAGE", "categories", "ALL", "purpose", "Persistence"),
            Map.of("id", "e5", "source", "store-db", "target", "store-backup", "type", "BACKUP", "categories", "ALL", "purpose", "Disaster Recovery"),
            Map.of("id", "e6", "source", "store-db", "target", "share-rbi", "type", "REPORTING", "categories", "PAN,ACCOUNT", "purpose", "Regulatory"),
            Map.of("id", "e7", "source", "store-db", "target", "share-certin", "type", "REPORTING", "categories", "BREACH_DATA", "purpose", "Breach Notification"),
            Map.of("id", "e8", "source", "store-db", "target", "del-crypto", "type", "DELETION", "categories", "ALL", "purpose", "Right to Erasure")
        );
    }

    public boolean isInitialized() { return initialized; }
}
