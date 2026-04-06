package com.qshield.soar.service;

import com.qshield.common.ai.AIAnalyticsEngine;
import com.qshield.common.audit.AuditService;
import com.qshield.soar.model.*;
import com.qshield.soar.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SoarService {

    private final IncidentRepository incidentRepo;
    private final PlaybookRepository playbookRepo;
    private final AuditService auditService;
    private final AIAnalyticsEngine aiEngine;

    public SoarService(IncidentRepository incidentRepo, PlaybookRepository playbookRepo,
                       AuditService auditService, AIAnalyticsEngine aiEngine) {
        this.incidentRepo = incidentRepo;
        this.playbookRepo = playbookRepo;
        this.auditService = auditService;
        this.aiEngine = aiEngine;
        initDefaultPlaybooks();
    }

    public Incident createIncident(Incident incident) {
        if (incident.getIncidentId() == null) {
            incident = new Incident(incident.getTitle(), incident.getSeverity(), incident.getCategory());
        }
        String aiRec = aiEngine.query(
                "Recommend response actions for: " + incident.getTitle() + " (Category: " + incident.getCategory() + ")",
                "SOAR Incident Response Context"
        );
        incident.setAiRecommendation(aiRec);
        Incident saved = incidentRepo.save(incident);
        auditService.log("SOAR", "INCIDENT_CREATED", null, null,
                "Incident " + saved.getIncidentId() + ": " + saved.getTitle(), saved.getSeverity());
        autoTriage(saved);
        return saved;
    }

    public Incident updateStatus(String incidentId, String status, String assignedTo) {
        Incident inc = incidentRepo.findByIncidentId(incidentId).orElseThrow();
        inc.setStatus(status);
        if (assignedTo != null) inc.setAssignedTo(assignedTo);
        auditService.log("SOAR", "INCIDENT_STATUS_CHANGE", assignedTo, null,
                incidentId + " → " + status, "INFO");
        return incidentRepo.save(inc);
    }

    public Page<Incident> getIncidents(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null) return incidentRepo.findByStatusOrderByCreatedAtDesc(status, pageable);
        return incidentRepo.findAll(pageable);
    }

    public Map<String, Object> getDashboardStats() {
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalIncidents", incidentRepo.count());
        stats.put("openIncidents", incidentRepo.countByStatus("DETECTED") + incidentRepo.countByStatus("TRIAGED") + incidentRepo.countByStatus("INVESTIGATING"));
        stats.put("criticalIncidents", incidentRepo.countBySeverityAndCreatedAtAfter("CRITICAL", last24h));
        stats.put("statusBreakdown", incidentRepo.countByStatus());
        stats.put("categoryBreakdown", incidentRepo.countByCategory());
        stats.put("activePlaybooks", playbookRepo.findByEnabledTrue().size());
        return stats;
    }

    public List<Playbook> getPlaybooks() { return playbookRepo.findAll(); }

    public Playbook createPlaybook(Playbook pb) { return playbookRepo.save(pb); }

    private void autoTriage(Incident incident) {
        if ("CRITICAL".equals(incident.getSeverity())) {
            incident.setStatus("TRIAGED");
            incident.setImpactScore(90);
            incidentRepo.save(incident);
        }
    }

    private void initDefaultPlaybooks() {
        if (playbookRepo.count() == 0) {
            playbookRepo.save(new Playbook("PB-MALWARE-01", "Malware Containment",
                    "ALERT_SEVERITY", "[{\"step\":1,\"action\":\"ISOLATE_ENDPOINT\"},{\"step\":2,\"action\":\"QUARANTINE_FILE\"},{\"step\":3,\"action\":\"BLOCK_HASH\"},{\"step\":4,\"action\":\"NOTIFY_SOC_TEAM\"}]"));
            playbookRepo.save(new Playbook("PB-BRUTEFORCE-01", "Brute Force Response",
                    "CATEGORY", "[{\"step\":1,\"action\":\"BLOCK_SOURCE_IP\"},{\"step\":2,\"action\":\"LOCK_ACCOUNT\"},{\"step\":3,\"action\":\"FORCE_MFA\"},{\"step\":4,\"action\":\"ALERT_ADMIN\"}]"));
            playbookRepo.save(new Playbook("PB-PHISHING-01", "Phishing Response",
                    "CATEGORY", "[{\"step\":1,\"action\":\"QUARANTINE_EMAIL\"},{\"step\":2,\"action\":\"BLOCK_SENDER_DOMAIN\"},{\"step\":3,\"action\":\"SCAN_RECIPIENTS\"},{\"step\":4,\"action\":\"NOTIFY_USERS\"}]"));
            playbookRepo.save(new Playbook("PB-EXFIL-01", "Data Exfiltration Response",
                    "CATEGORY", "[{\"step\":1,\"action\":\"BLOCK_DESTINATION\"},{\"step\":2,\"action\":\"CAPTURE_NETFLOW\"},{\"step\":3,\"action\":\"FREEZE_ACCOUNT\"},{\"step\":4,\"action\":\"ALERT_DLP_TEAM\"}]"));
        }
    }
}
