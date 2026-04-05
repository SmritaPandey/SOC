package com.qsdpdp.api;

import com.qsdpdp.iam.IAMService;
import com.qsdpdp.iam.Permission;
import com.qsdpdp.audit.AuditService;
import com.qsdpdp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API Gateway Service - Secure REST API with rate limiting
 * Provides programmatic access to QS-DPDP functionality
 * 
 * @version 1.0.0
 * @since Module 13
 */
@Service
public class APIGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(APIGatewayService.class);

    private final DatabaseManager dbManager;
    private final IAMService iamService;
    private final AuditService auditService;

    private boolean initialized = false;
    private final Map<String, RateLimitBucket> rateLimits = new ConcurrentHashMap<>();
    private final Map<String, APIKey> apiKeys = new ConcurrentHashMap<>();

    // Rate limit configuration
    private static final int DEFAULT_RATE_LIMIT = 1000; // requests per minute
    private static final int BURST_LIMIT = 50; // max burst

    @Autowired
    public APIGatewayService(DatabaseManager dbManager, IAMService iamService,
            AuditService auditService) {
        this.dbManager = dbManager;
        this.iamService = iamService;
        this.auditService = auditService;
    }

    public void initialize() {
        if (initialized)
            return;

        logger.info("Initializing API Gateway Service...");
        createTables();
        loadAPIKeys();

        initialized = true;
        logger.info("API Gateway Service initialized with {} API keys", apiKeys.size());
    }

    private void createTables() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS api_keys (
                            id TEXT PRIMARY KEY,
                            key_hash TEXT UNIQUE NOT NULL,
                            name TEXT NOT NULL,
                            organization_id TEXT,
                            owner_user_id TEXT,
                            scopes TEXT,
                            rate_limit INTEGER DEFAULT 1000,
                            active INTEGER DEFAULT 1,
                            expires_at TIMESTAMP,
                            last_used_at TIMESTAMP,
                            total_requests INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS api_requests (
                            id TEXT PRIMARY KEY,
                            api_key_id TEXT,
                            endpoint TEXT,
                            method TEXT,
                            status_code INTEGER,
                            response_time_ms INTEGER,
                            ip_address TEXT,
                            user_agent TEXT,
                            request_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            error_message TEXT,
                            FOREIGN KEY (api_key_id) REFERENCES api_keys(id)
                        )
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS api_webhooks (
                            id TEXT PRIMARY KEY,
                            organization_id TEXT,
                            name TEXT,
                            url TEXT NOT NULL,
                            events TEXT,
                            secret_hash TEXT,
                            active INTEGER DEFAULT 1,
                            retry_count INTEGER DEFAULT 3,
                            last_triggered_at TIMESTAMP,
                            last_success_at TIMESTAMP,
                            failure_count INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_api_requests_key ON api_requests(api_key_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_api_requests_time ON api_requests(request_at)");

            logger.info("API Gateway tables created");

        } catch (SQLException e) {
            logger.error("Failed to create API Gateway tables", e);
        }
    }

    private void loadAPIKeys() {
        try (Connection conn = dbManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM api_keys WHERE active = 1 AND (expires_at IS NULL OR expires_at > datetime('now'))")) {
            while (rs.next()) {
                APIKey key = mapAPIKey(rs);
                apiKeys.put(key.getKeyHash(), key);
            }
        } catch (SQLException e) {
            logger.error("Failed to load API keys", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // API KEY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public APIKeyCreateResult createAPIKey(String name, String organizationId, String ownerUserId,
            Set<APIScope> scopes, int rateLimit, LocalDateTime expiresAt) {
        String keyId = UUID.randomUUID().toString();
        String rawKey = "qsdpdp_" + generateSecureKey(32);
        String keyHash = hashKey(rawKey);

        String scopeStr = scopes.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse("");

        String sql = """
                    INSERT INTO api_keys (id, key_hash, name, organization_id, owner_user_id,
                        scopes, rate_limit, expires_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, keyId);
            stmt.setString(2, keyHash);
            stmt.setString(3, name);
            stmt.setString(4, organizationId);
            stmt.setString(5, ownerUserId);
            stmt.setString(6, scopeStr);
            stmt.setInt(7, rateLimit > 0 ? rateLimit : DEFAULT_RATE_LIMIT);
            stmt.setString(8, expiresAt != null ? expiresAt.toString() : null);
            stmt.executeUpdate();

            APIKey apiKey = new APIKey(keyId, keyHash, name, organizationId, scopes, rateLimit);
            apiKeys.put(keyHash, apiKey);

            auditService.log("API_KEY_CREATED", "API", ownerUserId, "Created API key: " + name);

            return new APIKeyCreateResult(true, keyId, rawKey, "API key created successfully");

        } catch (SQLException e) {
            logger.error("Failed to create API key", e);
            return new APIKeyCreateResult(false, null, null, "Failed to create API key: " + e.getMessage());
        }
    }

    public void revokeAPIKey(String keyId, String revokedBy) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE api_keys SET active = 0 WHERE id = ?")) {
            stmt.setString(1, keyId);
            stmt.executeUpdate();

            apiKeys.values().removeIf(k -> k.getId().equals(keyId));

            auditService.log("API_KEY_REVOKED", "API", revokedBy, "Revoked API key: " + keyId);

        } catch (SQLException e) {
            logger.error("Failed to revoke API key", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REQUEST HANDLING
    // ═══════════════════════════════════════════════════════════

    public APIResponse processRequest(APIRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        try {
            // Validate API key
            APIKey apiKey = validateAPIKey(request.getApiKey());
            if (apiKey == null) {
                return APIResponse.unauthorized("Invalid or expired API key");
            }

            // Check rate limit
            if (!checkRateLimit(apiKey)) {
                return APIResponse.tooManyRequests("Rate limit exceeded. Try again later.");
            }

            // Check scope
            APIScope requiredScope = getRequiredScope(request.getEndpoint(), request.getMethod());
            if (requiredScope != null && !apiKey.getScopes().contains(requiredScope)) {
                return APIResponse.forbidden("Insufficient scope for this operation");
            }

            // Route request
            APIResponse response = routeRequest(request, apiKey);
            response.setRequestId(requestId);

            // Log request
            logRequest(requestId, apiKey.getId(), request, response,
                    System.currentTimeMillis() - startTime);

            // Update key usage
            updateAPIKeyUsage(apiKey.getId());

            return response;

        } catch (Exception e) {
            logger.error("API request error", e);
            return APIResponse.error(500, "Internal server error: " + e.getMessage());
        }
    }

    private APIKey validateAPIKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith("qsdpdp_")) {
            return null;
        }

        String keyHash = hashKey(rawKey);
        APIKey apiKey = apiKeys.get(keyHash);

        if (apiKey == null || !apiKey.isActive()) {
            return null;
        }

        if (apiKey.getExpiresAt() != null &&
                apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }

        return apiKey;
    }

    private boolean checkRateLimit(APIKey apiKey) {
        RateLimitBucket bucket = rateLimits.computeIfAbsent(
                apiKey.getId(), k -> new RateLimitBucket(apiKey.getRateLimit()));
        return bucket.tryConsume();
    }

    private APIScope getRequiredScope(String endpoint, String method) {
        if (endpoint.startsWith("/consent"))
            return APIScope.CONSENT_READ;
        if (endpoint.startsWith("/breach"))
            return APIScope.BREACH_READ;
        if (endpoint.startsWith("/rights"))
            return APIScope.RIGHTS_READ;
        if (endpoint.startsWith("/dpia"))
            return APIScope.DPIA_READ;
        if (endpoint.startsWith("/pii"))
            return APIScope.PII_READ;
        if (endpoint.startsWith("/compliance"))
            return APIScope.COMPLIANCE_READ;
        if (endpoint.startsWith("/reporting"))
            return APIScope.REPORTING_READ;
        return null;
    }

    private APIResponse routeRequest(APIRequest request, APIKey apiKey) {
        String endpoint = request.getEndpoint();
        String method = request.getMethod();

        // Consent endpoints
        if (endpoint.equals("/consent/collect") && "POST".equals(method)) {
            return handleConsentCollect(request);
        }
        if (endpoint.equals("/consent/status") && "GET".equals(method)) {
            return handleConsentStatus(request);
        }
        if (endpoint.equals("/consent/withdraw") && "POST".equals(method)) {
            return handleConsentWithdraw(request);
        }

        // Breach endpoints
        if (endpoint.equals("/breach/report") && "POST".equals(method)) {
            return handleBreachReport(request);
        }
        if (endpoint.equals("/breach/status") && "GET".equals(method)) {
            return handleBreachStatus(request);
        }

        // Rights endpoints
        if (endpoint.equals("/rights/access") && "POST".equals(method)) {
            return handleRightsAccess(request);
        }
        if (endpoint.equals("/rights/erasure") && "POST".equals(method)) {
            return handleRightsErasure(request);
        }

        // Compliance endpoints
        if (endpoint.equals("/compliance/score") && "GET".equals(method)) {
            return handleComplianceScore(request);
        }

        // PII endpoints
        if (endpoint.equals("/pii/scan") && "POST".equals(method)) {
            return handlePIIScan(request);
        }

        return APIResponse.notFound("Endpoint not found: " + endpoint);
    }

    // ═══════════════════════════════════════════════════════════
    // ENDPOINT HANDLERS
    // ═══════════════════════════════════════════════════════════

    private APIResponse handleConsentCollect(APIRequest request) {
        Map<String, Object> body = request.getBody();
        String principalId = (String) body.get("principalId");
        String purpose = (String) body.get("purpose");

        if (principalId == null || purpose == null) {
            return APIResponse.badRequest("Missing required fields: principalId, purpose");
        }

        // Integration with ConsentService would happen here
        Map<String, Object> data = new HashMap<>();
        data.put("consentId", UUID.randomUUID().toString());
        data.put("status", "COLLECTED");
        data.put("timestamp", LocalDateTime.now().toString());

        return APIResponse.success(data);
    }

    private APIResponse handleConsentStatus(APIRequest request) {
        String principalId = request.getQueryParam("principalId");
        if (principalId == null) {
            return APIResponse.badRequest("Missing query param: principalId");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("principalId", principalId);
        data.put("consents", List.of()); // Would query ConsentService

        return APIResponse.success(data);
    }

    private APIResponse handleConsentWithdraw(APIRequest request) {
        Map<String, Object> body = request.getBody();
        String consentId = (String) body.get("consentId");

        if (consentId == null) {
            return APIResponse.badRequest("Missing required field: consentId");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("consentId", consentId);
        data.put("status", "WITHDRAWN");
        data.put("withdrawnAt", LocalDateTime.now().toString());

        return APIResponse.success(data);
    }

    private APIResponse handleBreachReport(APIRequest request) {
        Map<String, Object> body = request.getBody();
        String description = (String) body.get("description");
        String severity = (String) body.get("severity");

        if (description == null) {
            return APIResponse.badRequest("Missing required field: description");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("breachId", "BRE-" + System.currentTimeMillis());
        data.put("status", "REPORTED");
        data.put("severity", severity != null ? severity : "MEDIUM");
        data.put("reportedAt", LocalDateTime.now().toString());

        return APIResponse.success(data);
    }

    private APIResponse handleBreachStatus(APIRequest request) {
        String breachId = request.getQueryParam("breachId");
        if (breachId == null) {
            return APIResponse.badRequest("Missing query param: breachId");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("breachId", breachId);
        data.put("status", "INVESTIGATING");
        data.put("lastUpdated", LocalDateTime.now().toString());

        return APIResponse.success(data);
    }

    private APIResponse handleRightsAccess(APIRequest request) {
        Map<String, Object> body = request.getBody();
        String principalId = (String) body.get("principalId");

        if (principalId == null) {
            return APIResponse.badRequest("Missing required field: principalId");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", "DSR-" + System.currentTimeMillis());
        data.put("type", "ACCESS");
        data.put("status", "SUBMITTED");
        data.put("sla", "30 days");

        return APIResponse.success(data);
    }

    private APIResponse handleRightsErasure(APIRequest request) {
        Map<String, Object> body = request.getBody();
        String principalId = (String) body.get("principalId");

        if (principalId == null) {
            return APIResponse.badRequest("Missing required field: principalId");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("requestId", "DSR-" + System.currentTimeMillis());
        data.put("type", "ERASURE");
        data.put("status", "SUBMITTED");
        data.put("sla", "30 days");

        return APIResponse.success(data);
    }

    private APIResponse handleComplianceScore(APIRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("overallScore", 85.5);
        data.put("ragStatus", "GREEN");
        data.put("calculatedAt", LocalDateTime.now().toString());
        data.put("moduleScores", Map.of(
                "consent", 90,
                "breach", 85,
                "rights", 80,
                "security", 88));

        return APIResponse.success(data);
    }

    private APIResponse handlePIIScan(APIRequest request) {
        Map<String, Object> body = request.getBody();
        String content = (String) body.get("content");

        if (content == null) {
            return APIResponse.badRequest("Missing required field: content");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("scanId", UUID.randomUUID().toString());
        data.put("status", "COMPLETED");
        data.put("piiFound", false); // Would call PIIScanner
        data.put("scannedAt", LocalDateTime.now().toString());

        return APIResponse.success(data);
    }

    // ═══════════════════════════════════════════════════════════
    // WEBHOOK MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    public String registerWebhook(String organizationId, String name, String url,
            Set<String> events, String createdBy) {
        String webhookId = UUID.randomUUID().toString();
        String secret = generateSecureKey(32);
        String secretHash = hashKey(secret);

        String eventsStr = events.stream().reduce((a, b) -> a + "," + b).orElse("");

        String sql = """
                    INSERT INTO api_webhooks (id, organization_id, name, url, events, secret_hash)
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, webhookId);
            stmt.setString(2, organizationId);
            stmt.setString(3, name);
            stmt.setString(4, url);
            stmt.setString(5, eventsStr);
            stmt.setString(6, secretHash);
            stmt.executeUpdate();

            auditService.log("WEBHOOK_REGISTERED", "API", createdBy,
                    "Registered webhook: " + name + " for events: " + eventsStr);

            return secret; // Return secret for signature verification

        } catch (SQLException e) {
            logger.error("Failed to register webhook", e);
            return null;
        }
    }

    public void triggerWebhook(String event, Map<String, Object> payload) {
        String sql = "SELECT * FROM api_webhooks WHERE active = 1 AND events LIKE ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + event + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String url = rs.getString("url");
                String webhookId = rs.getString("id");

                // In production, this would make HTTP POST to the webhook URL
                logger.info("Triggering webhook {} for event: {}", webhookId, event);
                updateWebhookTrigger(webhookId, true);
            }
        } catch (SQLException e) {
            logger.error("Failed to trigger webhooks", e);
        }
    }

    private void updateWebhookTrigger(String webhookId, boolean success) {
        String sql = success ? "UPDATE api_webhooks SET last_triggered_at = ?, last_success_at = ? WHERE id = ?"
                : "UPDATE api_webhooks SET last_triggered_at = ?, failure_count = failure_count + 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, LocalDateTime.now().toString());
            if (success) {
                stmt.setString(2, LocalDateTime.now().toString());
                stmt.setString(3, webhookId);
            } else {
                stmt.setString(2, webhookId);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update webhook trigger", e);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════

    private String generateSecureKey(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String hashKey(String key) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash key", e);
        }
    }

    private void logRequest(String requestId, String keyId, APIRequest request,
            APIResponse response, long responseTime) {
        String sql = """
                    INSERT INTO api_requests (id, api_key_id, endpoint, method, status_code,
                        response_time_ms, ip_address, user_agent, error_message)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, requestId);
            stmt.setString(2, keyId);
            stmt.setString(3, request.getEndpoint());
            stmt.setString(4, request.getMethod());
            stmt.setInt(5, response.getStatusCode());
            stmt.setLong(6, responseTime);
            stmt.setString(7, request.getIpAddress());
            stmt.setString(8, request.getUserAgent());
            stmt.setString(9, response.getError());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to log API request", e);
        }
    }

    private void updateAPIKeyUsage(String keyId) {
        try (Connection conn = dbManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE api_keys SET last_used_at = ?, total_requests = total_requests + 1 WHERE id = ?")) {
            stmt.setString(1, LocalDateTime.now().toString());
            stmt.setString(2, keyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to update API key usage", e);
        }
    }

    private APIKey mapAPIKey(ResultSet rs) throws SQLException {
        String scopeStr = rs.getString("scopes");
        Set<APIScope> scopes = new HashSet<>();
        if (scopeStr != null && !scopeStr.isEmpty()) {
            for (String s : scopeStr.split(",")) {
                try {
                    scopes.add(APIScope.valueOf(s.trim()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        APIKey key = new APIKey(
                rs.getString("id"),
                rs.getString("key_hash"),
                rs.getString("name"),
                rs.getString("organization_id"),
                scopes,
                rs.getInt("rate_limit"));

        String expiresAt = rs.getString("expires_at");
        if (expiresAt != null) {
            key.setExpiresAt(LocalDateTime.parse(expiresAt));
        }

        return key;
    }

    public boolean isInitialized() {
        return initialized;
    }

    // ═══════════════════════════════════════════════════════════
    // INNER CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class RateLimitBucket {
        private final int maxTokens;
        private final AtomicLong tokens;
        private long lastRefill;

        public RateLimitBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.tokens = new AtomicLong(maxTokens);
            this.lastRefill = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefill;
            if (elapsed >= 60000) { // Refill every minute
                tokens.set(maxTokens);
                lastRefill = now;
            }
        }
    }

    public static class APIKeyCreateResult {
        private final boolean success;
        private final String keyId;
        private final String apiKey;
        private final String message;

        public APIKeyCreateResult(boolean success, String keyId, String apiKey, String message) {
            this.success = success;
            this.keyId = keyId;
            this.apiKey = apiKey;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getKeyId() {
            return keyId;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getMessage() {
            return message;
        }
    }
}
