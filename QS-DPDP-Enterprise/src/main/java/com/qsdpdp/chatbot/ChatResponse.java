package com.qsdpdp.chatbot;

import java.util.*;
import java.time.LocalDateTime;

/**
 * AI Chatbot Response model
 * Captures AI responses with sources, confidence, and actions
 * 
 * @version 1.0.0
 * @since Module 4
 */
public class ChatResponse {

    private String id;
    private String queryId;
    private String response;
    private double confidence;
    private List<Source> sources;
    private List<SuggestedAction> suggestedActions;
    private boolean needsClarification;
    private String clarificationPrompt;
    private LocalDateTime timestamp;
    private long processingTimeMs;

    public ChatResponse() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.sources = new ArrayList<>();
        this.suggestedActions = new ArrayList<>();
    }

    public ChatResponse(String queryId, String response, double confidence) {
        this();
        this.queryId = queryId;
        this.response = response;
        this.confidence = confidence;
    }

    public static ChatResponse lowConfidence(String queryId, String response, String clarification) {
        ChatResponse r = new ChatResponse(queryId, response, 0.3);
        r.setNeedsClarification(true);
        r.setClarificationPrompt(clarification);
        return r;
    }

    public void addSource(String title, String reference, String snippet) {
        sources.add(new Source(title, reference, snippet));
    }

    public void addAction(String label, String actionType, Map<String, Object> params) {
        suggestedActions.add(new SuggestedAction(label, actionType, params));
    }

    // Source class
    public static class Source {
        private final String title;
        private final String reference;
        private final String snippet;

        public Source(String title, String reference, String snippet) {
            this.title = title;
            this.reference = reference;
            this.snippet = snippet;
        }

        public String getTitle() {
            return title;
        }

        public String getReference() {
            return reference;
        }

        public String getSnippet() {
            return snippet;
        }
    }

    // Suggested Action class
    public static class SuggestedAction {
        private final String label;
        private final String actionType;
        private final Map<String, Object> parameters;

        public SuggestedAction(String label, String actionType, Map<String, Object> parameters) {
            this.label = label;
            this.actionType = actionType;
            this.parameters = parameters != null ? parameters : new HashMap<>();
        }

        public String getLabel() {
            return label;
        }

        public String getActionType() {
            return actionType;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public List<Source> getSources() {
        return sources;
    }

    public List<SuggestedAction> getSuggestedActions() {
        return suggestedActions;
    }

    public boolean isNeedsClarification() {
        return needsClarification;
    }

    public void setNeedsClarification(boolean needs) {
        this.needsClarification = needs;
    }

    public String getClarificationPrompt() {
        return clarificationPrompt;
    }

    public void setClarificationPrompt(String prompt) {
        this.clarificationPrompt = prompt;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long ms) {
        this.processingTimeMs = ms;
    }
}
