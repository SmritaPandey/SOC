package com.qsdpdp.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Event Bus for QS-DPDP Enterprise
 * Pub/Sub messaging system for decoupled module communication
 * 
 * Extended in Phase 1 with WebSocket sync bridge via SyncService hook.
 * When a SyncService is registered, all dispatched events are also
 * broadcast to WebSocket subscribers for real-time Backend↔Web↔Mobile sync.
 * 
 * @version 1.1.0
 * @since Phase 1
 */
@Service
public class EventBus {

    private static final Logger logger = LoggerFactory.getLogger(EventBus.class);

    private final Map<String, Set<Consumer<ComplianceEvent>>> subscribers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final BlockingQueue<ComplianceEvent> eventQueue = new LinkedBlockingQueue<>(10000);
    private boolean initialized = false;
    private volatile boolean running = false;

    /** Optional sync hook — set by SyncService to bridge events to WebSocket */
    private volatile Consumer<ComplianceEvent> syncHook = null;

    public void initialize() {
        if (initialized) {
            return;
        }

        logger.info("Initializing Event Bus...");

        // Start event processor thread
        running = true;
        executor.submit(this::processEvents);

        initialized = true;
        logger.info("Event Bus initialized");
    }

    /**
     * Subscribe to events matching a pattern
     * Pattern supports wildcards: "consent.*" matches "consent.created",
     * "consent.withdrawn", etc.
     */
    public void subscribe(String pattern, Consumer<ComplianceEvent> handler) {
        subscribers.computeIfAbsent(pattern, k -> ConcurrentHashMap.newKeySet()).add(handler);
        logger.debug("Subscribed to pattern: {}", pattern);
    }

    /**
     * Unsubscribe a handler from a pattern
     */
    public void unsubscribe(String pattern, Consumer<ComplianceEvent> handler) {
        Set<Consumer<ComplianceEvent>> handlers = subscribers.get(pattern);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * Publish an event to the bus
     */
    public void publish(ComplianceEvent event) {
        if (event == null) {
            return;
        }

        logger.debug("Publishing event: {}", event.getType());

        try {
            eventQueue.offer(event, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while publishing event", e);
        }
    }

    /**
     * Publish an event synchronously (blocking)
     */
    public void publishSync(ComplianceEvent event) {
        if (event == null) {
            return;
        }

        logger.debug("Publishing event (sync): {}", event.getType());
        dispatchEvent(event);
    }

    private void processEvents() {
        logger.info("Event processor started");

        while (running) {
            try {
                ComplianceEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) {
                    dispatchEvent(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("Event processor interrupted");
                break;
            } catch (Exception e) {
                logger.error("Error processing event", e);
            }
        }

        logger.info("Event processor stopped");
    }

    private void dispatchEvent(ComplianceEvent event) {
        String eventType = event.getType();

        for (Map.Entry<String, Set<Consumer<ComplianceEvent>>> entry : subscribers.entrySet()) {
            String pattern = entry.getKey();

            if (matchesPattern(eventType, pattern)) {
                for (Consumer<ComplianceEvent> handler : entry.getValue()) {
                    try {
                        handler.accept(event);
                    } catch (Exception e) {
                        logger.error("Error in event handler for pattern: {}", pattern, e);
                    }
                }
            }
        }

        // Phase 1: Bridge to WebSocket via SyncService hook
        if (syncHook != null) {
            try {
                syncHook.accept(event);
            } catch (Exception e) {
                logger.debug("Sync hook error (non-fatal): {}", e.getMessage());
            }
        }
    }

    /**
     * Register a sync hook for WebSocket bridging.
     * Called by SyncService during initialization.
     */
    public void registerSyncHook(Consumer<ComplianceEvent> hook) {
        this.syncHook = hook;
        logger.info("Sync hook registered — events will be bridged to WebSocket");
    }

    private boolean matchesPattern(String eventType, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }

        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return eventType.startsWith(prefix + ".");
        }

        if (pattern.contains("*")) {
            String regex = pattern.replace(".", "\\.").replace("*", ".*");
            return Pattern.matches(regex, eventType);
        }

        return eventType.equals(pattern);
    }

    /**
     * Shutdown the event bus
     */
    public void shutdown() {
        logger.info("Shutting down Event Bus...");
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Event Bus shutdown complete");
    }

    public int getPendingEventCount() {
        return eventQueue.size();
    }

    public int getSubscriberCount() {
        return subscribers.values().stream().mapToInt(Set::size).sum();
    }

    public boolean isInitialized() {
        return initialized;
    }
}
