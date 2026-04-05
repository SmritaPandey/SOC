package com.qsdpdp.chatbot;

/**
 * LLM Provider Interface for AI Chatbot
 * Abstracts LLM integration to support multiple backends:
 * - Ollama (local, air-gapped deployment)
 * - OpenAI API (cloud)
 * - Azure OpenAI (enterprise)
 *
 * @version 3.0.0
 * @since Phase 1 Upgrade
 */
public interface LLMProvider {

    /**
     * Generate a response from the LLM given a DPDP-contextualized prompt.
     *
     * @param systemPrompt System instructions (DPDP expert persona)
     * @param userQuery    User's actual question
     * @param context      Relevant knowledge base articles for RAG grounding
     * @return LLM-generated response text
     */
    String generateResponse(String systemPrompt, String userQuery, String context);

    /**
     * Check if the LLM provider is available and healthy.
     */
    boolean isAvailable();

    /**
     * Get the name of the LLM model being used.
     */
    String getModelName();
}
