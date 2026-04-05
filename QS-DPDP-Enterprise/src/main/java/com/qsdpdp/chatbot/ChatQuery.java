package com.qsdpdp.chatbot;

import java.util.*;
import java.time.LocalDateTime;

/**
 * AI Chatbot Query model
 * Captures user queries with context and metadata
 * 
 * @version 1.0.0
 * @since Module 4
 */
public class ChatQuery {

    private String id;
    private String sessionId;
    private String userId;
    private String query;
    private QueryType type;
    private String context; // Current module/page
    private Map<String, Object> metadata;
    private LocalDateTime timestamp;

    public ChatQuery() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }

    public ChatQuery(String userId, String query) {
        this();
        this.userId = userId;
        this.query = query;
        this.type = classifyQuery(query);
    }

    private QueryType classifyQuery(String query) {
        String lower = query.toLowerCase();

        if (lower.contains("what is") || lower.contains("explain") || lower.contains("define")) {
            return QueryType.EXPLANATION;
        }
        if (lower.contains("how to") || lower.contains("how do") || lower.contains("steps")) {
            return QueryType.GUIDANCE;
        }
        if (lower.contains("generate") || lower.contains("create") || lower.contains("draft")) {
            return QueryType.GENERATION;
        }
        if (lower.contains("section") || lower.contains("clause") || lower.contains("dpdp")) {
            return QueryType.LEGAL_REFERENCE;
        }
        if (lower.contains("help") || lower.contains("where") || lower.contains("navigate")) {
            return QueryType.NAVIGATION;
        }
        if (lower.contains("error") || lower.contains("problem") || lower.contains("issue")) {
            return QueryType.TROUBLESHOOTING;
        }

        return QueryType.GENERAL;
    }

    public enum QueryType {
        EXPLANATION, // What is X?
        GUIDANCE, // How to do X?
        GENERATION, // Generate policy/SOP
        LEGAL_REFERENCE, // DPDP clause info
        NAVIGATION, // Where is X?
        TROUBLESHOOTING, // Fix problem
        GENERAL // Other queries
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public QueryType getType() {
        return type;
    }

    public void setType(QueryType type) {
        this.type = type;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
