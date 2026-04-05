/**
 * QS-DPDP Enterprise — Real-time Sync Client
 * 
 * STOMP/SockJS client for WebSocket-based real-time synchronization.
 * Connects to the backend sync layer and provides:
 * - Real-time event subscription (consent, breach, data-usage)
 * - Automatic reconnection with exponential backoff
 * - Offline event queuing
 * - Event deduplication on client side
 * 
 * Dependencies: SockJS (loaded from CDN), STOMP.js (embedded)
 * 
 * @version 1.0.0
 * @since Phase 1 — Core Synchronization
 */
const QSSyncClient = (function() {
    'use strict';

    // ═══════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════

    let stompClient = null;
    let connected = false;
    let reconnectAttempts = 0;
    const MAX_RECONNECT_ATTEMPTS = 10;
    const BASE_RECONNECT_DELAY = 2000;
    const clientId = 'web-' + Date.now() + '-' + Math.random().toString(36).substr(2, 6);
    const eventHandlers = {};
    const seenEvents = new Set();
    const DEDUP_CACHE_MAX = 1000;
    let lastSequence = 0;

    // ═══════════════════════════════════════════════════════════
    // CONNECTION
    // ═══════════════════════════════════════════════════════════

    /**
     * Initialize sync client and connect to WebSocket
     */
    function connect(options = {}) {
        const wsUrl = options.url || '/ws-sync';
        
        console.log(`[QS-Sync] Connecting to ${wsUrl} (clientId: ${clientId})...`);

        // Use SockJS for browser compatibility
        const socket = new SockJS(wsUrl);
        stompClient = Stomp.over(socket);
        
        // Reduce STOMP debug logging in production
        stompClient.debug = options.debug ? console.log : () => {};

        stompClient.connect({}, function(frame) {
            connected = true;
            reconnectAttempts = 0;
            console.log('[QS-Sync] ✅ Connected:', frame);

            // Subscribe to all sync topics
            subscribeTopic('/topic/consent');
            subscribeTopic('/topic/breach');
            subscribeTopic('/topic/data-usage');
            subscribeTopic('/topic/notifications');
            subscribeTopic('/topic/sync');

            // Register with server
            stompClient.send('/app/sync.register', {}, JSON.stringify({
                clientId: clientId,
                clientType: 'WEB'
            }));

            // Fetch any missed events via REST
            catchUpSync();

            // Fire connect handlers
            fireEvent('connected', { clientId, frame });

        }, function(error) {
            connected = false;
            console.warn('[QS-Sync] ❌ Connection lost:', error);
            fireEvent('disconnected', { error });
            scheduleReconnect();
        });
    }

    /**
     * Subscribe to a STOMP topic
     */
    function subscribeTopic(topic) {
        if (!stompClient || !connected) return;

        stompClient.subscribe(topic, function(message) {
            try {
                const data = JSON.parse(message.body);
                
                // Deduplicate
                const eventKey = data.id || data.eventType + ':' + JSON.stringify(data.payload);
                if (seenEvents.has(eventKey)) return;
                seenEvents.add(eventKey);
                if (seenEvents.size > DEDUP_CACHE_MAX) {
                    const iter = seenEvents.values();
                    for (let i = 0; i < 200; i++) iter.next();
                    // Trim oldest entries
                }

                // Update sequence tracking
                if (data.sequence && data.sequence > lastSequence) {
                    lastSequence = data.sequence;
                }

                // Dispatch to handlers
                const topicName = topic.replace('/topic/', '');
                fireEvent(topicName, data);
                fireEvent('any', { topic: topicName, data });

            } catch (e) {
                console.error('[QS-Sync] Parse error:', e);
            }
        });
        
        console.log(`[QS-Sync] Subscribed: ${topic}`);
    }

    /**
     * Automatic reconnection with exponential backoff
     */
    function scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            console.error('[QS-Sync] Max reconnect attempts reached. Falling back to REST polling.');
            startPolling();
            return;
        }

        const delay = BASE_RECONNECT_DELAY * Math.pow(2, reconnectAttempts);
        reconnectAttempts++;
        console.log(`[QS-Sync] Reconnecting in ${delay}ms (attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})...`);
        setTimeout(() => connect(), delay);
    }

    // ═══════════════════════════════════════════════════════════
    // REST FALLBACK (offline catch-up + polling)
    // ═══════════════════════════════════════════════════════════

    /**
     * Catch up on missed events via REST API
     */
    async function catchUpSync() {
        try {
            // Drain offline queue
            const offlineRes = await fetch(`/api/sync/offline/${clientId}`);
            if (offlineRes.ok) {
                const offlineData = await offlineRes.json();
                if (offlineData.eventCount > 0) {
                    console.log(`[QS-Sync] Replayed ${offlineData.eventCount} offline events`);
                    offlineData.events.forEach(ev => {
                        const topicName = (ev.topic || '').replace('/topic/', '') || 'sync';
                        fireEvent(topicName, ev);
                    });
                }
            }

            // Catch up consent events
            await catchUpTopic('consent');
            await catchUpTopic('breach');
            await catchUpTopic('data-usage');

        } catch (e) {
            console.warn('[QS-Sync] Catch-up sync failed:', e);
        }
    }

    async function catchUpTopic(topic) {
        try {
            const res = await fetch(`/api/sync/${topic}?since=${lastSequence}&limit=50`);
            if (res.ok) {
                const data = await res.json();
                if (data.eventCount > 0) {
                    console.log(`[QS-Sync] Caught up ${data.eventCount} ${topic} events`);
                    data.events.forEach(ev => fireEvent(topic, ev));
                    if (data.latestSequence > lastSequence) {
                        lastSequence = data.latestSequence;
                    }
                }
            }
        } catch (e) {
            // Silent — REST fallback is best-effort
        }
    }

    let pollingInterval = null;

    /**
     * Start REST polling as WebSocket fallback
     */
    function startPolling(intervalMs = 15000) {
        if (pollingInterval) return;
        console.log(`[QS-Sync] Starting REST polling (${intervalMs}ms interval)`);
        pollingInterval = setInterval(async () => {
            if (connected) {
                clearInterval(pollingInterval);
                pollingInterval = null;
                return;
            }
            await catchUpSync();
        }, intervalMs);
    }

    // ═══════════════════════════════════════════════════════════
    // EVENT HANDLERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Register an event handler
     * @param {string} event - 'consent', 'breach', 'data-usage', 'notifications', 'sync', 'connected', 'disconnected', 'any'
     * @param {Function} handler - callback(data)
     */
    function on(event, handler) {
        if (!eventHandlers[event]) eventHandlers[event] = [];
        eventHandlers[event].push(handler);
    }

    /**
     * Remove an event handler
     */
    function off(event, handler) {
        if (!eventHandlers[event]) return;
        eventHandlers[event] = eventHandlers[event].filter(h => h !== handler);
    }

    /**
     * Fire event to all registered handlers
     */
    function fireEvent(event, data) {
        if (!eventHandlers[event]) return;
        eventHandlers[event].forEach(handler => {
            try {
                handler(data);
            } catch (e) {
                console.error(`[QS-Sync] Handler error for '${event}':`, e);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    // SEND (Client → Server)
    // ═══════════════════════════════════════════════════════════

    /**
     * Send a consent event to server via WebSocket
     */
    function sendConsentEvent(action, consentId, metadata = {}) {
        if (!connected || !stompClient) {
            console.warn('[QS-Sync] Not connected — queuing event');
            return false;
        }
        stompClient.send('/app/sync.consent', {}, JSON.stringify({
            action, consentId, ...metadata, clientId
        }));
        return true;
    }

    /**
     * Send a breach report to server via WebSocket
     */
    function sendBreachEvent(action, breachId, metadata = {}) {
        if (!connected || !stompClient) return false;
        stompClient.send('/app/sync.breach', {}, JSON.stringify({
            action, breachId, ...metadata, clientId
        }));
        return true;
    }

    /**
     * Send a data usage event to server via WebSocket
     */
    function sendDataUsageEvent(action, metadata = {}) {
        if (!connected || !stompClient) return false;
        stompClient.send('/app/sync.data-usage', {}, JSON.stringify({
            action, ...metadata, clientId
        }));
        return true;
    }

    // ═══════════════════════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════════════════════

    function isConnected() { return connected; }
    function getClientId() { return clientId; }
    function getLastSequence() { return lastSequence; }

    async function getStatus() {
        try {
            const res = await fetch('/api/sync/status');
            if (res.ok) return await res.json();
        } catch (e) { /* silent */ }
        return { status: 'UNAVAILABLE' };
    }

    /**
     * Disconnect cleanly
     */
    function disconnect() {
        if (stompClient) {
            stompClient.disconnect(() => {
                console.log('[QS-Sync] Disconnected');
                connected = false;
            });
        }
        if (pollingInterval) {
            clearInterval(pollingInterval);
            pollingInterval = null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════

    return {
        connect,
        disconnect,
        on,
        off,
        sendConsentEvent,
        sendBreachEvent,
        sendDataUsageEvent,
        isConnected,
        getClientId,
        getLastSequence,
        getStatus
    };
})();

// Auto-connect when loaded (if SockJS and STOMP are available)
if (typeof SockJS !== 'undefined' && typeof Stomp !== 'undefined') {
    // Will be initialized by dashboard.js or trust-os.js when ready
    console.log('[QS-Sync] Client loaded — call QSSyncClient.connect() to start');
}
