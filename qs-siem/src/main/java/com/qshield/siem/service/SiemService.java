package com.qshield.siem.service;

import com.qshield.common.ai.AIAnalyticsEngine;
import com.qshield.common.audit.AuditService;
import com.qshield.siem.model.*;
import com.qshield.siem.repository.*;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/**
 * QS-SIEM Core Service — log ingestion, correlation, UEBA, and threat intelligence.
 * Implements: NIST SI-4 (System Monitoring), NIST AU-6 (Audit Review),
 * NIST IR-4 (Incident Handling), MITRE ATT&CK mapping.
 */
@Service
public class SiemService {

    private final SecurityEventRepository eventRepo;
    private final SiemAlertRepository alertRepo;
    private final AuditService auditService;
    private final AIAnalyticsEngine aiEngine;

    // UEBA baselines: IP -> { metric -> value }
    private final Map<String, Map<String, Double>> uebaBaselines = new ConcurrentHashMap<>();

    // Built-in correlation rules
    private final List<CorrelationRule> correlationRules = new ArrayList<>();

    public SiemService(SecurityEventRepository eventRepo, SiemAlertRepository alertRepo,
                       AuditService auditService, AIAnalyticsEngine aiEngine) {
        this.eventRepo = eventRepo;
        this.alertRepo = alertRepo;
        this.auditService = auditService;
        this.aiEngine = aiEngine;
        initCorrelationRules();
    }

    // ═══════════ LOG INGESTION ═══════════

    public SecurityEvent ingestEvent(SecurityEvent event) {
        if (event.getTimestamp() == null) event.setTimestamp(Instant.now());
        normalizeEvent(event);
        mapMitreAttack(event);
        event.setThreatScore(computeThreatScore(event));
        SecurityEvent saved = eventRepo.save(event);
        runCorrelation(saved);
        updateUEBABaseline(saved);
        return saved;
    }

    public SecurityEvent ingestRawLog(String rawLog, String format) {
        SecurityEvent event = parseLog(rawLog, format);
        return ingestEvent(event);
    }

    // ═══════════ CORRELATION ENGINE ═══════════

    private void runCorrelation(SecurityEvent event) {
        for (CorrelationRule rule : correlationRules) {
            if (rule.matches(event)) {
                // Check threshold
                long count = eventRepo.findBySourceIpAndTimestampAfter(
                        event.getSourceIp(),
                        Instant.now().minus(rule.windowMinutes, ChronoUnit.MINUTES)
                ).size();

                if (count >= rule.threshold) {
                    createAlert(rule, event, (int) count);
                }
            }
        }
    }

    private void createAlert(CorrelationRule rule, SecurityEvent event, int eventCount) {
        SiemAlert alert = new SiemAlert(
                rule.alertTitle + " from " + event.getSourceIp(),
                rule.severity,
                event.getSourceIp(),
                rule.ruleId
        );
        alert.setDescription(rule.description);
        alert.setRelatedEventCount(eventCount);
        alert.setMitreTactic(rule.mitreTactic);
        alert.setMitreTechnique(rule.mitreTechnique);
        alert.setThreatScore(event.getThreatScore());

        // AI-generated threat narrative
        String narrative = aiEngine.query(
                "Analyze this security threat: " + rule.alertTitle +
                " from IP " + event.getSourceIp() + " with " + eventCount + " events",
                "SIEM Alert Context: " + event.getRawLog()
        );
        alert.setAiAnalysis(narrative);

        alertRepo.save(alert);
        auditService.log("SIEM", "ALERT_CREATED", null, event.getSourceIp(),
                "Alert: " + alert.getTitle() + " | Score: " + event.getThreatScore(), rule.severity);
    }

    // ═══════════ UEBA ENGINE ═══════════

    private void updateUEBABaseline(SecurityEvent event) {
        if (event.getSourceIp() == null) return;
        uebaBaselines.computeIfAbsent(event.getSourceIp(), k -> new ConcurrentHashMap<>());
        Map<String, Double> baseline = uebaBaselines.get(event.getSourceIp());
        baseline.merge("eventCount", 1.0, Double::sum);
        if ("AUTH".equals(event.getCategory())) baseline.merge("authEvents", 1.0, Double::sum);
        if ("DENY".equals(event.getAction())) baseline.merge("deniedEvents", 1.0, Double::sum);
    }

    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void runUEBAAnalysis() {
        uebaBaselines.forEach((ip, features) -> {
            Map<String, Double> baseline = new HashMap<>();
            baseline.put("eventCount", 10.0);
            baseline.put("authEvents", 2.0);
            baseline.put("deniedEvents", 0.5);

            double anomalyScore = aiEngine.computeAnomalyScore(features, baseline);
            if (anomalyScore > 0.7) {
                SiemAlert alert = new SiemAlert(
                        "UEBA Anomaly Detected: " + ip,
                        anomalyScore > 0.9 ? "CRITICAL" : "HIGH",
                        ip, "UEBA-ANOMALY"
                );
                alert.setDescription("Behavioral anomaly score: " + String.format("%.2f", anomalyScore));
                alert.setThreatScore((int) (anomalyScore * 100));
                alert.setMitreTactic("TA0001");
                alertRepo.save(alert);
            }
        });
    }

    // ═══════════ DASHBOARD ANALYTICS ═══════════

    public Map<String, Object> getDashboardStats() {
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEvents24h", eventRepo.countByTimestampAfter(last24h));
        stats.put("criticalAlerts", alertRepo.countBySeverityAndCreatedAtAfter("CRITICAL", last24h));
        stats.put("highAlerts", alertRepo.countBySeverityAndCreatedAtAfter("HIGH", last24h));
        stats.put("activeAlerts", alertRepo.countByStatus("NEW"));
        stats.put("severityBreakdown", eventRepo.countBySeverityAfter(last24h));
        stats.put("categoryBreakdown", eventRepo.countByCategoryAfter(last24h));
        stats.put("topSourceIps", eventRepo.topSourceIps(last24h, PageRequest.of(0, 10)));
        stats.put("alertsByStatus", alertRepo.countByStatus());
        return stats;
    }

    public Map<String, Object> getEventsPaged(int page, int size, String severity) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<SecurityEvent> result;
        if (severity != null && !severity.isEmpty()) {
            result = eventRepo.findBySeverityOrderByTimestampDesc(severity, pageable);
        } else {
            result = eventRepo.findAll(pageable);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", result.getContent());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("number", result.getNumber());
        return response;
    }

    public Map<String, Object> getAlertsPaged(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<SiemAlert> result;
        if (status != null && !status.isEmpty()) {
            result = alertRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            result = alertRepo.findAll(pageable);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", result.getContent());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("number", result.getNumber());
        return response;
    }

    public SiemAlert updateAlertStatus(Long alertId, String status, String assignedTo) {
        SiemAlert alert = alertRepo.findById(alertId).orElseThrow();
        alert.setStatus(status);
        if (assignedTo != null) alert.setAssignedTo(assignedTo);
        auditService.log("SIEM", "ALERT_UPDATED", assignedTo, null,
                "Alert #" + alertId + " status → " + status, "INFO");
        return alertRepo.save(alert);
    }

    // ═══════════ LOG PARSING ═══════════

    private SecurityEvent parseLog(String rawLog, String format) {
        SecurityEvent event = new SecurityEvent();
        event.setRawLog(rawLog);
        event.setLogFormat(format);
        event.setTimestamp(Instant.now());

        // Extract IPs from raw log
        Matcher ipMatcher = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})").matcher(rawLog);
        if (ipMatcher.find()) event.setSourceIp(ipMatcher.group(1));
        if (ipMatcher.find()) event.setDestinationIp(ipMatcher.group(1));

        // Detect severity from keywords
        String lower = rawLog.toLowerCase();
        if (lower.contains("critical") || lower.contains("emergency")) event.setSeverity("CRITICAL");
        else if (lower.contains("error") || lower.contains("fail")) event.setSeverity("HIGH");
        else if (lower.contains("warn")) event.setSeverity("MEDIUM");
        else if (lower.contains("denied") || lower.contains("block")) event.setSeverity("HIGH");
        else event.setSeverity("LOW");

        // Detect category
        if (lower.contains("auth") || lower.contains("login") || lower.contains("password")) event.setCategory("AUTH");
        else if (lower.contains("malware") || lower.contains("virus") || lower.contains("trojan")) event.setCategory("MALWARE");
        else if (lower.contains("firewall") || lower.contains("network") || lower.contains("connection")) event.setCategory("NETWORK");
        else if (lower.contains("policy") || lower.contains("compliance")) event.setCategory("POLICY");
        else event.setCategory("SYSTEM");

        event.setNormalizedMessage(rawLog.substring(0, Math.min(256, rawLog.length())));
        return event;
    }

    private void normalizeEvent(SecurityEvent event) {
        if (event.getNormalizedMessage() == null && event.getRawLog() != null) {
            event.setNormalizedMessage(event.getRawLog().substring(0, Math.min(256, event.getRawLog().length())));
        }
    }

    // ═══════════ MITRE ATT&CK MAPPING ═══════════

    private void mapMitreAttack(SecurityEvent event) {
        if (event.getCategory() == null) return;
        switch (event.getCategory()) {
            case "AUTH":
                if ("DENY".equals(event.getAction())) {
                    event.setMitreTactic("TA0006"); // Credential Access
                    event.setMitreTechnique("T1110"); // Brute Force
                } else {
                    event.setMitreTactic("TA0001"); // Initial Access
                    event.setMitreTechnique("T1078"); // Valid Accounts
                }
                break;
            case "MALWARE":
                event.setMitreTactic("TA0002"); // Execution
                event.setMitreTechnique("T1204"); // User Execution
                break;
            case "NETWORK":
                event.setMitreTactic("TA0011"); // Command and Control
                event.setMitreTechnique("T1071"); // Application Layer Protocol
                break;
            case "POLICY":
                event.setMitreTactic("TA0010"); // Exfiltration
                event.setMitreTechnique("T1048"); // Exfiltration Over Alternative Protocol
                break;
        }
    }

    private int computeThreatScore(SecurityEvent event) {
        int score = 0;
        switch (event.getSeverity()) {
            case "CRITICAL": score += 40; break;
            case "HIGH": score += 30; break;
            case "MEDIUM": score += 15; break;
            case "LOW": score += 5; break;
        }
        if ("AUTH".equals(event.getCategory()) && "DENY".equals(event.getAction())) score += 20;
        if ("MALWARE".equals(event.getCategory())) score += 30;
        if (event.getSourceIp() != null && event.getSourceIp().startsWith("10.")) score -= 5;
        return Math.max(1, Math.min(100, score));
    }

    // ═══════════ CORRELATION RULES ═══════════

    private void initCorrelationRules() {
        correlationRules.add(new CorrelationRule("CR-001", "Brute Force Attack Detected",
                "Multiple failed authentication attempts from single IP",
                "AUTH", "DENY", 5, 5, "CRITICAL", "TA0006", "T1110"));
        correlationRules.add(new CorrelationRule("CR-002", "Port Scan Detected",
                "Multiple connection attempts to different ports",
                "NETWORK", null, 20, 2, "HIGH", "TA0043", "T1046"));
        correlationRules.add(new CorrelationRule("CR-003", "Malware Outbreak",
                "Multiple malware detections across endpoints",
                "MALWARE", null, 3, 10, "CRITICAL", "TA0002", "T1204"));
        correlationRules.add(new CorrelationRule("CR-004", "Data Exfiltration Attempt",
                "Abnormal outbound data transfer volume",
                "NETWORK", null, 50, 15, "HIGH", "TA0010", "T1048"));
        correlationRules.add(new CorrelationRule("CR-005", "Privilege Escalation",
                "Unauthorized privilege change detected",
                "AUTH", "ALLOW", 3, 5, "CRITICAL", "TA0004", "T1068"));
    }

    static class CorrelationRule {
        String ruleId, alertTitle, description, category, action;
        int threshold, windowMinutes;
        String severity, mitreTactic, mitreTechnique;

        CorrelationRule(String ruleId, String alertTitle, String description,
                       String category, String action, int threshold, int windowMinutes,
                       String severity, String mitreTactic, String mitreTechnique) {
            this.ruleId = ruleId; this.alertTitle = alertTitle; this.description = description;
            this.category = category; this.action = action; this.threshold = threshold;
            this.windowMinutes = windowMinutes; this.severity = severity;
            this.mitreTactic = mitreTactic; this.mitreTechnique = mitreTechnique;
        }

        boolean matches(SecurityEvent event) {
            if (!category.equals(event.getCategory())) return false;
            return action == null || action.equals(event.getAction());
        }
    }
}
