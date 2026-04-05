package com.qsdpdp.api;

import java.util.*;
import java.time.LocalDateTime;

/**
 * API Key model for programmatic access
 * 
 * @version 1.0.0
 * @since Module 13
 */
public class APIKey {

    private final String id;
    private final String keyHash;
    private final String name;
    private final String organizationId;
    private final Set<APIScope> scopes;
    private final int rateLimit;
    private boolean active;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private long totalRequests;

    public APIKey(String id, String keyHash, String name, String organizationId,
            Set<APIScope> scopes, int rateLimit) {
        this.id = id;
        this.keyHash = keyHash;
        this.name = name;
        this.organizationId = organizationId;
        this.scopes = scopes != null ? scopes : new HashSet<>();
        this.rateLimit = rateLimit;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getName() {
        return name;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public Set<APIScope> getScopes() {
        return scopes;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }
}
