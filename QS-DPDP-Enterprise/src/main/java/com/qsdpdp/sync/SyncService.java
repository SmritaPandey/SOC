package com.qsdpdp.sync;

import com.qsdpdp.db.DatabaseManager;
import com.qsdpdp.events.ComplianceEvent;
import com.qsdpdp.events.EventBus;
import com.qsdpdp.audit.AuditService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unified Sync Service — Core Synchronization Engine
 * 
 * Bridges EventBus → WebSocket for real-time sync between:
 * - Backend services
 * - Web dashboard (SockJS)
 * - Mobile apps (native WS)
 * 
 * Features:
 * - Event deduplication (idempotency keys)
 * - Offline queue for mobile reconnection
 * - Sync state tracking per client
 * - Event replay for catch-up sync
 * 
 * @version 1.0.0
 * @since Phase 1 — Core Synchronization
 */
@Service
public class SyncService {

    private static final Logger logger = LoggerFactory.getLogger(SyncService.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Maximum offline queue size per client */
    private static final int MAX_OFFLINE_QUEUE = 500;

    /** Deduplication window: ignore events with same idempotency key within 60s */
    private static final long DEDUP_WINDOW_MS = 60_000;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private EventBus eventBus;

    @Autowired(required = false)
    private DatabaseManager dbManager;

    @Autowired(required = false)
    private AuditService auditService;

    // Sync state tracking
    private final Map<String, Long> clientLastSync = new ConcurrentHashMap<>();
    private final Map<String, Queue<SyncEvent>> offlineQueues = new ConcurrentHashMap<>();
    private final Map<String, Long> deduplicationCache = new ConcurrentHashMap<>();
    private final AtomicLong syncSequence = new AtomicLong(0);
    private boolean initialized = false;

    // ═══════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════

    public void initialize() {
        if (initialized) return;
        logger.info("Initializing Sync Service...");

        createTables();
        subscribeToEventBus();
        startDeduplicationCleanup();

        initialized = true;
        logger.info("Sync Service initialized — WebSocket bridge active");
    }

    private void createTables() {
        if (dbManager == null || !dbManager.isInitialized()) return;
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sync_events (
                    id TEXT PRIMARY KEY,
                    sequence BIGINT NOT NULL,
                    event_type TEXT NOT NULL,
                    topic TEXT NOT NULL,
                    payload TEXT,
                    source TEXT DEFAULT 'SYSTEM',
                    idempotency_key TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sync_client_state (
                    client_id TEXT PRIMARY KEY,
                    last_sequence BIGINT DEFAULT 0,
                    last_sync_at TIMESTAMP,
                    client_type TEXT,
                    status TEXT DEFAULT 'ONLINE'
                )
            """);

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_sync_events_sequence ON sync_events(sequence)
            """);
            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_sync_events_topic ON sync_events(topic)
            """);

            logger.info("Sync tables created");
        } catch (SQLException e) {
            logger.error("Failed to create sync tables", e);
        }
    }

    /**
     * Subscribe to EventBus patterns and bridge to WebSocket topics
     */
    private void subscribeToEventBus() {
        if (eventBus == null) {
            logger.warn("EventBus not available — sync will work via REST only");
            return;
        }

        // Consent events → /topic/consent
        eventBus.subscribe("consent.*", event -> broadcastToTopic("/topic/consent", event));

        // Breach events → /topic/breach
        eventBus.subscribe("breach.*", event -> broadcastToTopic("/topic/breach", event));

        // Data usage events → /topic/data-usage
        eventBus.subscribe("data-usage.*", event -> broadcastToTopic("/topic/data-usage", event));

        // Notification events → /topic/notifications
        eventBus.subscribe("notification.*", event -> broadcastToTopic("/topic/notifications", event));

        // All events → /topic/sync (system-wide feed)
        eventBus.subscribe("*", event -> broadcastToTopic("/topic/sync", event));

        logger.info("EventBus → WebSocket bridge configured (5 topic subscriptions)");
    }

    private void startDeduplicationCleanup() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sync-dedup-cleanup");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> {
            long cutoff = System.currentTimeMillis() - DEDUP_WINDOW_MS;
            deduplicationCache.entrySet().removeIf(e -> e.getValue() < cutoff);
        }, 60, 60, TimeUnit.SECONDS);
    }

    // ═══════════════════════════════════════════════════════════
    // BROADCAST (EventBus → WebSocket)
    // ═══════════════════════════════════════════════════════════

    /**
     * Broadcast a ComplianceEvent to a WebSocket topic
     */
    public void broadcastToTopic(String topic, ComplianceEvent event) {
        long seq = syncSequence.incrementAndGet();

        SyncEvent syncEvent = new SyncEvent(
                event.getId(),
                seq,
                event.getType(),
                topic,
                event.getPayload(),
                event.getSource(),
                event.getTimestamp()
        );

        // Deduplicate
        String dedupKey = event.getType() + ":" + (event.getPayload() != null ? event.getPayload().hashCode() : 0);
        if (deduplicationCache.putIfAbsent(dedupKey, System.currentTimeMillis()) != null) {
            logger.debug("Duplicate event suppressed: {}", dedupKey);
            return;
        }

        // Persist for replay
        persistSyncEvent(syncEvent);

        // Push to WebSocket subscribers
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend(topic, syncEvent.toMap());
                logger.debug("WS broadcast: {} → {} (seq={})", event.getType(), topic, seq);
            } catch (Exception e) {
                logger.warn("WebSocket broadcast failed for {}: {}", topic, e.getMessage());
            }
        }

        // Queue for offline clients
        for (Map.Entry<String, Queue<SyncEvent>> entry : offlineQueues.entrySet()) {
            Queue<SyncEvent> queue = entry.getValue();
            if (queue.size() < MAX_OFFLINE_QUEUE) {
                queue.offer(syncEvent);
            }
        }
    }

    /**
     * Broadcast a raw map payload to a topic (for direct use without EventBus)
     */
    public void broadcastDirect(String topic, Map<String, Object> payload) {
        long seq = syncSequence.incrementAndGet();
        if (messagingTemplate != null) {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("sequence", seq);
            envelope.put("topic", topic);
            envelope.put("payload", payload);
            envelope.put("timestamp", LocalDateTime.now().toString());
            messagingTemplate.convertAndSend(topic, envelope);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SYNC ENDPOINTS (REST — for mobile offline catch-up)
    // ═══════════════════════════════════════════════════════════

    /**
     * Get consent sync data since a given sequence number
     */
    public Map<String, Object> syncConsent(long sinceSequence, int limit) {
        return syncByTopic("/topic/consent", sinceSequence, limit);
    }

    /**
     * Get data-usage sync data since a given sequence number
     */
    public Map<String, Object> syncDataUsage(long sinceSequence, int limit) {
        return syncByTopic("/topic/data-usage", sinceSequence, limit);
    }

    /**
     * Get breach sync data since a given sequence number
     */
    public Map<String, Object> syncBreach(long sinceSequence, int limit) {
        return syncByTopic("/topic/breach", sinceSequence, limit);
    }

    /**
     * Generic topic sync with sequence-based pagination
     */
    public Map<String, Object> syncByTopic(String topic, long sinceSequence, int limit) {
        List<Map<String, Object>> events = new ArrayList<>();
        long latestSequence = sinceSequence;

        if (dbManager != null && dbManager.isInitialized()) {
            String sql = "SELECT * FROM sync_events WHERE topic = ? AND sequence > ? ORDER BY sequence ASC LIMIT ?";
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, topic);
                ps.setLong(2, sinceSequence);
                ps.setInt(3, limit > 0 ? limit : 100);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> ev = new LinkedHashMap<>();
                        ev.put("id", rs.getString("id"));
                        ev.put("sequence", rs.getLong("sequence"));
                        ev.put("eventType", rs.getString("event_type"));
                        ev.put("topic", rs.getString("topic"));
                        ev.put("payload", rs.getString("payload"));
                        ev.put("source", rs.getString("source"));
                        ev.put("createdAt", rs.getString("created_at"));
                        events.add(ev);
                        latestSequence = Math.max(latestSequence, rs.getLong("sequence"));
                    }
                }
            } catch (SQLException e) {
                logger.error("Sync query failed for topic {}", topic, e);
            }
        }

        return Map.of(
                "topic", topic,
                "sinceSequence", sinceSequence,
                "latestSequence", latestSequence,
                "eventCount", events.size(),
                "hasMore", events.size() >= (limit > 0 ? limit : 100),
                "events", events,
                "syncedAt", LocalDateTime.now().toString()
        );
    }

    // ═══════════════════════════════════════════════════════════
    // OFFLINE QUEUE MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Register a client for offline queuing
     */
    public void registerClient(String clientId, String clientType) {
        offlineQueues.putIfAbsent(clientId, new ConcurrentLinkedQueue<>());
        clientLastSync.put(clientId, syncSequence.get());

        if (dbManager != null && dbManager.isInitialized()) {
            try (Connection conn = dbManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT OR REPLACE INTO sync_client_state (client_id, last_sequence, last_sync_at, client_type, status) VALUES (?, ?, ?, ?, 'ONLINE')")) {
                ps.setString(1, clientId);
                ps.setLong(2, syncSequence.get());
                ps.setString(3, LocalDateTime.now().toString());
                ps.setString(4, clientType);
                ps.executeUpdate();
            } catch (SQLException e) {
                logger.error("Failed to register sync client: {}", clientId, e);
            }
        }
        logger.info("Sync client registered: {} ({})", clientId, clientType);
    }

    /**
     * Drain offline queue for a reconnecting client
     */
    public List<Map<String, Object>> drainOfflineQueue(String clientId) {
        Queue<SyncEvent> queue = offlineQueues.get(clientId);
        if (queue == null || queue.isEmpty()) return List.of();

        List<Map<String, Object>> events = new ArrayList<>();
        SyncEvent event;
        while ((event = queue.poll()) != null) {
            events.add(event.toMap());
        }
        clientLastSync.put(clientId, syncSequence.get());
        logger.info("Drained {} offline events for client: {}", events.size(), clientId);
        return events;
    }

    // ═══════════════════════════════════════════════════════════
    // SYNC STATUS
    // ═══════════════════════════════════════════════════════════

    /**
     * Get overall sync status
     */
    public Map<String, Object> getSyncStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("currentSequence", syncSequence.get());
        status.put("registeredClients", offlineQueues.size());
        status.put("websocketAvailable", messagingTemplate != null);
        status.put("eventBusConnected", eventBus != null && eventBus.isInitialized());
        status.put("topics", List.of("/topic/consent", "/topic/breach", "/topic/data-usage", "/topic/notifications", "/topic/sync"));
        status.put("offlineQueueSizes", getOfflineQueueSizes());
        status.put("deduplicationCacheSize", deduplicationCache.size());
        status.put("timestamp", LocalDateTime.now().toString());
        return status;
    }

    private Map<String, Integer> getOfflineQueueSizes() {
        Map<String, Integer> sizes = new LinkedHashMap<>();
        for (Map.Entry<String, Queue<SyncEvent>> entry : offlineQueues.entrySet()) {
            sizes.put(entry.getKey(), entry.getValue().size());
        }
        return sizes;
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    private void persistSyncEvent(SyncEvent event) {
        if (dbManager == null || !dbManager.isInitialized()) return;

        String sql = "INSERT INTO sync_events (id, sequence, event_type, topic, payload, source, idempotency_key) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.id);
            ps.setLong(2, event.sequence);
            ps.setString(3, event.eventType);
            ps.setString(4, event.topic);
            ps.setString(5, event.payload != null ? gson.toJson(event.payload) : null);
            ps.setString(6, event.source);
            ps.setString(7, event.eventType + ":" + event.sequence);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.debug("Failed to persist sync event (may be duplicate): {}", e.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // SYNC EVENT DTO
    // ═══════════════════════════════════════════════════════════

    public static class SyncEvent {
        public final String id;
        public final long sequence;
        public final String eventType;
        public final String topic;
        public final Object payload;
        public final String source;
        public final LocalDateTime timestamp;

        public SyncEvent(String id, long sequence, String eventType, String topic,
                         Object payload, String source, LocalDateTime timestamp) {
            this.id = id;
            this.sequence = sequence;
            this.eventType = eventType;
            this.topic = topic;
            this.payload = payload;
            this.source = source;
            this.timestamp = timestamp;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("sequence", sequence);
            m.put("eventType", eventType);
            m.put("topic", topic);
            m.put("payload", payload);
            m.put("source", source);
            m.put("timestamp", timestamp != null ? timestamp.toString() : null);
            return m;
        }
    }
}
