package com.qsdpdp.update;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Auto-Update Service
 * Manages OTA (Over-The-Air) updates for both:
 * - Management Console (server-side)
 * - Endpoint Sensor Agents (push updates)
 *
 * Update flow (similar to CrowdStrike):
 * 1. Agent checks /api/v1/agents/updates periodically
 * 2. Server responds with latest version info
 * 3. Agent downloads update package
 * 4. Agent verifies SHA-256 checksum
 * 5. Agent applies update and restarts
 *
 * Console admins can:
 * - Upload new agent versions
 * - Schedule rollout (staged deployment)
 * - Monitor update progress
 * - Rollback failed updates
 *
 * @version 3.0.0
 * @since Phase 6 — Enterprise Distribution
 */
@Service
public class AutoUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(AutoUpdateService.class);

    private final HttpClient httpClient;
    private final Map<String, UpdatePackage> availableUpdates = new LinkedHashMap<>();
    private final Map<String, UpdateStatus> agentUpdateStatus = new LinkedHashMap<>();
    private String currentVersion = "3.0.0";

    public AutoUpdateService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // UPDATE PACKAGE MANAGEMENT
    // ═══════════════════════════════════════════════════════

    /**
     * Register a new update package for distribution.
     */
    public UpdatePackage registerUpdate(String version, String downloadUrl,
            String sha256, String releaseNotes, boolean critical) {
        UpdatePackage pkg = new UpdatePackage();
        pkg.version = version;
        pkg.downloadUrl = downloadUrl;
        pkg.sha256Checksum = sha256;
        pkg.releaseNotes = releaseNotes;
        pkg.critical = critical;
        pkg.publishedAt = LocalDateTime.now();
        pkg.status = "STAGED";

        availableUpdates.put(version, pkg);
        logger.info("📦 Update package registered: v{} (critical: {})", version, critical);
        return pkg;
    }

    /**
     * Activate an update for rollout.
     */
    public void activateUpdate(String version, int rolloutPercentage) {
        UpdatePackage pkg = availableUpdates.get(version);
        if (pkg != null) {
            pkg.status = "ACTIVE";
            pkg.rolloutPercentage = rolloutPercentage;
            logger.info("🚀 Update v{} activated — rollout: {}%", version, rolloutPercentage);
        }
    }

    /**
     * Check if an update is available for a specific agent version.
     */
    public Map<String, Object> checkForUpdate(String agentVersion) {
        Optional<UpdatePackage> latest = availableUpdates.values().stream()
                .filter(pkg -> "ACTIVE".equals(pkg.status))
                .filter(pkg -> compareVersions(pkg.version, agentVersion) > 0)
                .max(Comparator.comparing(pkg -> pkg.version));

        if (latest.isPresent()) {
            UpdatePackage pkg = latest.get();
            return Map.of(
                    "updateAvailable", true,
                    "currentVersion", agentVersion,
                    "latestVersion", pkg.version,
                    "downloadUrl", pkg.downloadUrl,
                    "sha256", pkg.sha256Checksum,
                    "releaseNotes", pkg.releaseNotes,
                    "critical", pkg.critical);
        }

        return Map.of("updateAvailable", false, "currentVersion", agentVersion);
    }

    /**
     * Record agent update status.
     */
    public void reportUpdateStatus(String agentId, String version, String status, String message) {
        UpdateStatus us = new UpdateStatus();
        us.agentId = agentId;
        us.version = version;
        us.status = status;
        us.message = message;
        us.timestamp = LocalDateTime.now();
        agentUpdateStatus.put(agentId, us);

        logger.info("📊 Agent {} update to v{}: {} — {}", agentId, version, status, message);
    }

    /**
     * Download an update package to local path.
     */
    public boolean downloadUpdate(String downloadUrl, Path destination) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .timeout(Duration.ofMinutes(10))
                    .GET().build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                Files.copy(response.body(), destination, StandardCopyOption.REPLACE_EXISTING);
                logger.info("✅ Update downloaded to: {}", destination);
                return true;
            }
        } catch (Exception e) {
            logger.error("Download failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Verify downloaded file checksum.
     */
    public boolean verifyChecksum(Path file, String expectedSha256) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = Files.readAllBytes(file);
            byte[] hash = md.digest(data);
            StringBuilder actual = new StringBuilder();
            for (byte b : hash) actual.append(String.format("%02x", b));
            return actual.toString().equalsIgnoreCase(expectedSha256);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Rollback to previous version.
     */
    public void rollbackUpdate(String version) {
        UpdatePackage pkg = availableUpdates.get(version);
        if (pkg != null) {
            pkg.status = "ROLLED_BACK";
            logger.info("↩️ Update v{} rolled back", version);
        }
    }

    // ═══════════════════════════════════════════════════════
    // STATISTICS & MONITORING
    // ═══════════════════════════════════════════════════════

    public Map<String, Object> getRolloutStatus(String version) {
        long total = agentUpdateStatus.size();
        long updated = agentUpdateStatus.values().stream()
                .filter(s -> version.equals(s.version) && "SUCCESS".equals(s.status)).count();
        long failed = agentUpdateStatus.values().stream()
                .filter(s -> version.equals(s.version) && "FAILED".equals(s.status)).count();

        return Map.of(
                "version", version,
                "totalAgents", total,
                "updated", updated,
                "failed", failed,
                "pending", total - updated - failed,
                "progressPercent", total > 0 ? (updated * 100.0 / total) : 0);
    }

    public Map<String, Object> getStatistics() {
        return Map.of(
                "currentVersion", currentVersion,
                "availableUpdates", availableUpdates.size(),
                "agentsTracked", agentUpdateStatus.size());
    }

    private int compareVersions(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        for (int i = 0; i < Math.max(p1.length, p2.length); i++) {
            int a = i < p1.length ? Integer.parseInt(p1[i]) : 0;
            int b = i < p2.length ? Integer.parseInt(p2[i]) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }

    // ═══ DATA CLASSES ═══

    public static class UpdatePackage {
        public String version, downloadUrl, sha256Checksum, releaseNotes, status;
        public boolean critical;
        public int rolloutPercentage;
        public LocalDateTime publishedAt;
    }

    public static class UpdateStatus {
        public String agentId, version, status, message;
        public LocalDateTime timestamp;
    }
}
