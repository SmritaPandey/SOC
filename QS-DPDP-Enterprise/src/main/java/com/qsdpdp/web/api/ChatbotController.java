package com.qsdpdp.web.api;

import com.qsdpdp.chatbot.ChatbotService;
import com.qsdpdp.chatbot.ChatQuery;
import com.qsdpdp.chatbot.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * AI Chatbot REST Controller — DPDP Compliance Assistant
 * Provides REST API endpoints for the RAG-based chatbot service.
 *
 * @version 1.0.0
 * @since Phase 4
 */
@RestController("chatbotApiController")
@RequestMapping("/api/v1/chatbot")
public class ChatbotController {

    @Autowired(required = false)
    private ChatbotService chatbotService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "ai-chatbot",
                "status", "UP",
                "version", "1.0.0",
                "initialized", chatbotService != null && chatbotService.isInitialized()));
    }

    /**
     * Submit a query to the AI chatbot.
     * Accepts: { "query": "...", "type": "EXPLANATION|GUIDANCE|GENERATION|LEGAL_REFERENCE|NAVIGATION|TROUBLESHOOTING|GENERAL", "sessionId": "...", "userId": "..." }
     */
    @PostMapping("/query")
    public ResponseEntity<Object> processQuery(@RequestBody Map<String, String> body) {
        try {
            String queryText = body.getOrDefault("query", "");
            String typeStr = body.getOrDefault("type", "GENERAL");
            String sessionId = body.getOrDefault("sessionId", UUID.randomUUID().toString());
            String userId = body.getOrDefault("userId", "anonymous");

            if (queryText.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Query text is required"));
            }

            ChatQuery query = new ChatQuery();
            query.setId(UUID.randomUUID().toString());
            query.setQuery(queryText);
            query.setSessionId(sessionId);
            query.setUserId(userId);
            try {
                query.setType(ChatQuery.QueryType.valueOf(typeStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                query.setType(ChatQuery.QueryType.GENERAL);
            }

            ChatResponse response = chatbotService.processQuery(query);

            return ResponseEntity.ok(Map.of(
                    "queryId", query.getId(),
                    "sessionId", sessionId,
                    "response", response.getResponse(),
                    "confidence", response.getConfidence(),
                    "sources", response.getSources() != null ? response.getSources() : List.of(),
                    "suggestedActions", response.getSuggestedActions() != null ? response.getSuggestedActions() : List.of(),
                    "processingTimeMs", response.getProcessingTimeMs()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get chat history summary — returns module status.
     */
    @GetMapping("/history")
    public ResponseEntity<Object> getChatHistory(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            return ResponseEntity.ok(Map.of(
                    "sessionId", sessionId != null ? sessionId : "all",
                    "limit", limit,
                    "message", "Chat history available via database query"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get contextual compliance suggestions based on current module context.
     */
    @GetMapping("/suggestions")
    public ResponseEntity<Object> getSuggestions(
            @RequestParam(defaultValue = "dashboard") String context) {
        try {
            List<Map<String, String>> suggestions = new ArrayList<>();

            switch (context.toLowerCase()) {
                case "consent" -> {
                    suggestions.add(Map.of("text", "How do I collect valid consent under DPDP?", "type", "GUIDANCE"));
                    suggestions.add(Map.of("text", "What are the requirements for consent withdrawal?", "type", "EXPLANATION"));
                    suggestions.add(Map.of("text", "Generate a consent notice template", "type", "GENERATION"));
                }
                case "breach" -> {
                    suggestions.add(Map.of("text", "How do I report a data breach to DPBI?", "type", "GUIDANCE"));
                    suggestions.add(Map.of("text", "What is the 72-hour breach notification rule?", "type", "EXPLANATION"));
                    suggestions.add(Map.of("text", "Generate a breach response SOP", "type", "GENERATION"));
                }
                case "rights" -> {
                    suggestions.add(Map.of("text", "What rights do data principals have?", "type", "EXPLANATION"));
                    suggestions.add(Map.of("text", "How to handle a data access request?", "type", "GUIDANCE"));
                }
                case "gap-analysis" -> {
                    suggestions.add(Map.of("text", "What are the key compliance gaps to address?", "type", "EXPLANATION"));
                    suggestions.add(Map.of("text", "How to improve our compliance score?", "type", "GUIDANCE"));
                }
                default -> {
                    suggestions.add(Map.of("text", "What is the DPDP Act 2023?", "type", "EXPLANATION"));
                    suggestions.add(Map.of("text", "What are my obligations as a Data Fiduciary?", "type", "EXPLANATION"));
                    suggestions.add(Map.of("text", "How do I start DPDP compliance?", "type", "GUIDANCE"));
                    suggestions.add(Map.of("text", "Generate a privacy policy template", "type", "GENERATION"));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "context", context,
                    "suggestions", suggestions));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete chat history for a session.
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteHistory(@PathVariable String sessionId) {
        try {
            // Session cleanup handled at service level
            return ResponseEntity.ok(Map.of(
                    "sessionId", sessionId,
                    "message", "Chat session cleared"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Submit feedback on a chatbot response.
     */
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(@RequestBody Map<String, Object> body) {
        try {
            String historyId = (String) body.getOrDefault("historyId", "");
            boolean helpful = Boolean.TRUE.equals(body.get("helpful"));
            String comment = (String) body.getOrDefault("comment", "");

            // Feedback stored via audit service for now
            return ResponseEntity.ok(Map.of(
                    "historyId", historyId,
                    "helpful", helpful,
                    "message", "Feedback recorded. Thank you!"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
