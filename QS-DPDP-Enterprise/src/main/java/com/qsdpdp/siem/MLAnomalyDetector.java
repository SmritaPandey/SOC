package com.qsdpdp.siem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import smile.anomaly.IsolationForest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ML-based Anomaly Detector using Smile Isolation Forest
 * Replaces basic z-score (mean ± σ) with trained ML model for UEBA.
 *
 * Isolation Forest advantages over z-score:
 * - Works with high-dimensional data
 * - Doesn't assume normal distribution
 * - Fewer false positives
 * - Detects novel attack patterns
 *
 * @version 3.0.0
 * @since Phase 1 Upgrade
 */
@Component
public class MLAnomalyDetector {

    private static final Logger logger = LoggerFactory.getLogger(MLAnomalyDetector.class);

    @Value("${qsdpdp.ueba.ml.enabled:true}")
    private boolean mlEnabled;

    @Value("${qsdpdp.ueba.ml.isolation-forest-threshold:0.6}")
    private double anomalyThreshold;

    @Value("${qsdpdp.ueba.ml.min-observations:50}")
    private int minObservationsForTraining;

    // Per-user trained models
    private final Map<String, IsolationForest> userModels = new ConcurrentHashMap<>();
    // Per-user observation buffers
    private final Map<String, List<double[]>> userObservations = new ConcurrentHashMap<>();

    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        initialized = true;
        logger.info("ML Anomaly Detector initialized [enabled={}, threshold={}, minObs={}]",
                mlEnabled, anomalyThreshold, minObservationsForTraining);
    }

    /**
     * Record a behavioral observation for a user.
     * Features: [loginHour, eventsPerDay, dataAccessVolume, uniqueIPs, failedLogins]
     */
    public void recordObservation(String userId, double[] features) {
        if (!mlEnabled) return;

        userObservations
                .computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(features);

        // Auto-train when enough observations collected
        List<double[]> observations = userObservations.get(userId);
        if (observations != null && observations.size() >= minObservationsForTraining
                && observations.size() % minObservationsForTraining == 0) {
            trainModel(userId);
        }
    }

    /**
     * Train an Isolation Forest model for a specific user.
     */
    public void trainModel(String userId) {
        if (!mlEnabled) return;

        List<double[]> observations = userObservations.get(userId);
        if (observations == null || observations.size() < minObservationsForTraining) {
            logger.debug("Not enough observations for user {} ({}/{})",
                    userId, observations != null ? observations.size() : 0, minObservationsForTraining);
            return;
        }

        try {
            // Convert to 2D array for Smile
            double[][] data = observations.toArray(new double[0][]);

            // Train Isolation Forest with default parameters
            IsolationForest model = IsolationForest.fit(data);

            userModels.put(userId, model);
            logger.info("✅ Isolation Forest trained for user {} with {} observations",
                    userId, data.length);

        } catch (Exception e) {
            logger.error("Failed to train Isolation Forest for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Score a behavioral observation for anomaly.
     *
     * @param userId   User to evaluate
     * @param features [loginHour, eventsPerDay, dataAccessVolume, uniqueIPs, failedLogins]
     * @return AnomalyResult with score (0.0 = normal, 1.0 = highly anomalous) and method used
     */
    public AnomalyResult scoreAnomaly(String userId, double[] features) {
        IsolationForest model = userModels.get(userId);

        if (model != null && mlEnabled) {
            // Use trained Isolation Forest
            double rawScore = model.score(features);

            // Isolation Forest scores: closer to 1.0 = more anomalous
            boolean isAnomaly = rawScore > anomalyThreshold;

            return new AnomalyResult(
                    rawScore,
                    isAnomaly,
                    "ISOLATION_FOREST",
                    String.format("IF score=%.3f (threshold=%.3f)", rawScore, anomalyThreshold));
        }

        // Fallback: basic z-score when ML model not yet trained
        return fallbackZScore(userId, features);
    }

    /**
     * Z-score fallback for users without enough training data.
     * This is the original UEBA logic — kept as fallback.
     */
    private AnomalyResult fallbackZScore(String userId, double[] features) {
        List<double[]> observations = userObservations.get(userId);

        if (observations == null || observations.size() < 5) {
            return new AnomalyResult(0.0, false, "INSUFFICIENT_DATA",
                    "Less than 5 observations — cannot score");
        }

        // Calculate mean and stddev per feature
        int nFeatures = features.length;
        double[] mean = new double[nFeatures];
        double[] stddev = new double[nFeatures];

        for (double[] obs : observations) {
            for (int i = 0; i < Math.min(nFeatures, obs.length); i++) {
                mean[i] += obs[i];
            }
        }
        for (int i = 0; i < nFeatures; i++) mean[i] /= observations.size();

        for (double[] obs : observations) {
            for (int i = 0; i < Math.min(nFeatures, obs.length); i++) {
                stddev[i] += Math.pow(obs[i] - mean[i], 2);
            }
        }
        for (int i = 0; i < nFeatures; i++) stddev[i] = Math.sqrt(stddev[i] / observations.size());

        // Max z-score across features
        double maxZ = 0;
        for (int i = 0; i < nFeatures; i++) {
            if (stddev[i] > 0) {
                double z = Math.abs(features[i] - mean[i]) / stddev[i];
                maxZ = Math.max(maxZ, z);
            }
        }

        // Normalize z-score to 0-1 range: z=3 → score=0.75, z=4 → score=0.87
        double normalizedScore = 1.0 - (1.0 / (1.0 + maxZ / 3.0));
        boolean isAnomaly = maxZ > 3.0;

        return new AnomalyResult(
                normalizedScore,
                isAnomaly,
                "Z_SCORE_FALLBACK",
                String.format("z=%.2f (threshold=3.0), normalized=%.3f", maxZ, normalizedScore));
    }

    /**
     * Get ML model status for a user
     */
    public ModelStatus getModelStatus(String userId) {
        ModelStatus status = new ModelStatus();
        status.userId = userId;
        status.mlEnabled = this.mlEnabled;
        status.hasTrainedModel = userModels.containsKey(userId);
        List<double[]> obs = userObservations.get(userId);
        status.observationCount = obs != null ? obs.size() : 0;
        status.minObservationsRequired = minObservationsForTraining;
        status.anomalyThreshold = anomalyThreshold;
        return status;
    }

    public boolean isMLEnabled() { return mlEnabled; }
    public int getTrainedModelCount() { return userModels.size(); }

    // ═══════════════════════════════════════════════════════════
    // RESULT CLASSES
    // ═══════════════════════════════════════════════════════════

    public static class AnomalyResult {
        private final double score;
        private final boolean anomaly;
        private final String method;
        private final String detail;

        public AnomalyResult(double score, boolean anomaly, String method, String detail) {
            this.score = score;
            this.anomaly = anomaly;
            this.method = method;
            this.detail = detail;
        }

        public double getScore() { return score; }
        public boolean isAnomaly() { return anomaly; }
        public String getMethod() { return method; }
        public String getDetail() { return detail; }
    }

    public static class ModelStatus {
        public String userId;
        public boolean mlEnabled;
        public boolean hasTrainedModel;
        public int observationCount;
        public int minObservationsRequired;
        public double anomalyThreshold;
    }
}
