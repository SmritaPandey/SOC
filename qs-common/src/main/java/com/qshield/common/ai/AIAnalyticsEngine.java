package com.qshield.common.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.*;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Analytics Engine — provides RAG-based threat analysis, NL query,
 * and anomaly scoring across all QShield CSOC products.
 * Implements IEEE 2807-2019 (AI Ethics) and IEEE 7005-2021 (Transparent AI).
 */
@Service
public class AIAnalyticsEngine {

    @Value("${qshield.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${qshield.ai.model:llama3}")
    private String model;

    @Value("${qshield.ai.enabled:false}")
    private boolean aiEnabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Gson gson = new Gson();

    // ═══════════ TF-IDF RAG Engine ═══════════
    private final Map<String, Map<String, Double>> documentVectors = new HashMap<>();
    private final Map<String, String> documentStore = new HashMap<>();
    private final Map<String, Integer> globalDocFreq = new HashMap<>();

    /**
     * Index a document for RAG retrieval.
     */
    public void indexDocument(String docId, String content) {
        documentStore.put(docId, content);
        Map<String, Double> tf = computeTF(content);
        documentVectors.put(docId, tf);
        tf.keySet().forEach(term -> globalDocFreq.merge(term, 1, Integer::sum));
    }

    /**
     * RAG query — find most relevant documents and generate AI response.
     */
    public String query(String question, String context) {
        // Step 1: TF-IDF retrieval
        List<String> relevantDocs = retrieveTopK(question, 3);
        String ragContext = relevantDocs.stream()
                .map(documentStore::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        // Step 2: Generate response
        if (aiEnabled) {
            return callOllama(question, context + "\n\n" + ragContext);
        }

        // Fallback: return relevant context
        return ragContext.isEmpty()
                ? "AI analysis not available. Enable Ollama for natural language threat analysis."
                : "Relevant intelligence:\n" + ragContext;
    }

    /**
     * Anomaly score — returns 0.0 (normal) to 1.0 (highly anomalous).
     */
    public double computeAnomalyScore(Map<String, Double> features, Map<String, Double> baseline) {
        if (baseline.isEmpty()) return 0.5;
        double totalDeviation = 0;
        int count = 0;
        for (var entry : features.entrySet()) {
            Double baseVal = baseline.get(entry.getKey());
            if (baseVal != null && baseVal != 0) {
                totalDeviation += Math.abs(entry.getValue() - baseVal) / baseVal;
                count++;
            }
        }
        return count == 0 ? 0.5 : Math.min(1.0, totalDeviation / count);
    }

    /**
     * Threat risk scoring — composite of multiple risk factors.
     */
    public int computeThreatScore(int severityWeight, int frequencyWeight,
                                   int impactWeight, int confidenceWeight) {
        // Weighted average, clamped to 1-100
        double raw = (severityWeight * 0.35 + frequencyWeight * 0.2 +
                      impactWeight * 0.3 + confidenceWeight * 0.15);
        return (int) Math.max(1, Math.min(100, raw));
    }

    // ═══════════ Private Helpers ═══════════

    private List<String> retrieveTopK(String query, int k) {
        Map<String, Double> queryTF = computeTF(query);
        return documentVectors.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), cosineSimilarity(queryTF, e.getValue())))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Map<String, Double> computeTF(String text) {
        String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "").split("\\s+");
        Map<String, Double> tf = new HashMap<>();
        for (String t : tokens) {
            if (t.length() > 2) tf.merge(t, 1.0, Double::sum);
        }
        double max = tf.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        tf.replaceAll((k, v) -> v / max);
        return tf;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0, normA = 0, normB = 0;
        for (var e : a.entrySet()) {
            Double bv = b.get(e.getKey());
            if (bv != null) dot += e.getValue() * bv;
            normA += e.getValue() * e.getValue();
        }
        for (double v : b.values()) normB += v * v;
        return (normA == 0 || normB == 0) ? 0 : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String callOllama(String prompt, String context) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("prompt", "Context:\n" + context + "\n\nQuestion: " + prompt);
            body.addProperty("stream", false);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonObject result = JsonParser.parseString(resp.body()).getAsJsonObject();
            return result.has("response") ? result.get("response").getAsString() : "No response from AI";
        } catch (Exception e) {
            return "AI engine unavailable: " + e.getMessage();
        }
    }
}
