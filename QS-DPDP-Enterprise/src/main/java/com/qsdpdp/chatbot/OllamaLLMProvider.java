package com.qsdpdp.chatbot;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.utils.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ollama-based LLM Provider for air-gapped / on-premise deployments.
 * Connects to local Ollama instance running models like llama3, mistral, phi3.
 *
 * Configuration in application.properties:
 *   chatbot.llm.provider=ollama
 *   chatbot.llm.ollama.host=http://localhost:11434
 *   chatbot.llm.ollama.model=llama3
 *
 * @version 3.0.0
 * @since Phase 1 Upgrade
 */
@Component
public class OllamaLLMProvider implements LLMProvider {

    private static final Logger logger = LoggerFactory.getLogger(OllamaLLMProvider.class);

    @Value("${chatbot.llm.ollama.host:http://localhost:11434}")
    private String ollamaHost;

    @Value("${chatbot.llm.ollama.model:llama3}")
    private String modelName;

    @Value("${chatbot.llm.ollama.timeout:120}")
    private int timeoutSeconds;

    private OllamaAPI ollamaAPI;
    private boolean available = false;

    public void initialize() {
        try {
            ollamaAPI = new OllamaAPI(ollamaHost);
            ollamaAPI.setRequestTimeoutSeconds(timeoutSeconds);

            // Check connectivity
            if (ollamaAPI.ping()) {
                available = true;
                logger.info("✅ Ollama LLM connected at {} with model '{}'", ollamaHost, modelName);
            } else {
                available = false;
                logger.warn("⚠️ Ollama not reachable at {}. Chatbot will use keyword-matching fallback.", ollamaHost);
            }
        } catch (Exception e) {
            available = false;
            logger.warn("⚠️ Ollama initialization failed: {}. Chatbot will use keyword-matching fallback.",
                    e.getMessage());
        }
    }

    @Override
    public String generateResponse(String systemPrompt, String userQuery, String context) {
        if (!available || ollamaAPI == null) {
            return null; // Signal to caller to use fallback
        }

        try {
            // Build the full prompt with system context + RAG knowledge
            String fullPrompt = buildPrompt(systemPrompt, userQuery, context);

            Options options = new Options(new java.util.HashMap<>());

            // Generate response — using Object to avoid version-specific OllamaResult import
            var result = ollamaAPI.generate(modelName, fullPrompt, false, options);

            if (result != null && result.getResponse() != null) {
                logger.debug("LLM response generated ({} chars, model: {})",
                        result.getResponse().length(), modelName);
                return result.getResponse();
            }

            return null;
        } catch (Exception e) {
            logger.error("LLM generation failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String systemPrompt, String userQuery, String context) {
        StringBuilder prompt = new StringBuilder();

        // System context
        prompt.append("### System Instructions ###\n");
        prompt.append(systemPrompt).append("\n\n");

        // RAG Context (knowledge base articles)
        if (context != null && !context.isEmpty()) {
            prompt.append("### Reference Knowledge ###\n");
            prompt.append(context).append("\n\n");
        }

        // User query
        prompt.append("### User Question ###\n");
        prompt.append(userQuery).append("\n\n");

        prompt.append("### Your Response ###\n");

        return prompt.toString();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
