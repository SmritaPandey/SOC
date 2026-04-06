package com.qshield.dlp.service;
import com.qshield.common.audit.AuditService;
import com.qshield.dlp.model.*; import com.qshield.dlp.repository.*;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service;
import java.util.*; import java.util.regex.*;

@Service
public class DlpService {
    private final DlpIncidentRepository repo;
    private final AuditService auditService;
    private static final Map<String, Pattern> PATTERNS = Map.of(
        "AADHAAR", Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"),
        "PAN", Pattern.compile("\\b[A-Z]{5}\\d{4}[A-Z]\\b"),
        "CREDIT_CARD", Pattern.compile("\\b(?:\\d{4}[\\s-]?){3}\\d{4}\\b"),
        "EMAIL", Pattern.compile("\\b[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}\\b"),
        "PHONE", Pattern.compile("\\b(?:\\+91)?[\\s-]?\\d{10}\\b"),
        "SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b")
    );
    public DlpService(DlpIncidentRepository repo, AuditService auditService) { this.repo = repo; this.auditService = auditService; }

    public Map<String, List<String>> scanContent(String content) {
        Map<String, List<String>> findings = new LinkedHashMap<>();
        PATTERNS.forEach((type, pattern) -> {
            Matcher m = pattern.matcher(content);
            List<String> matches = new ArrayList<>();
            while (m.find()) matches.add(m.group().replaceAll("(?<=.{4}).", "*"));
            if (!matches.isEmpty()) findings.put(type, matches);
        });
        return findings;
    }

    public DlpIncident createIncident(DlpIncident incident) {
        auditService.log("DLP", "INCIDENT_CREATED", incident.getSourceUser(), null, incident.getTitle(), incident.getSeverity());
        return repo.save(incident);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalIncidents", repo.count());
        s.put("blockedIncidents", repo.countByStatus("BLOCKED"));
        s.put("activeIncidents", repo.countByStatus("DETECTED"));
        return s;
    }
    public Page<DlpIncident> getIncidents(int page, int size) { return repo.findAll(PageRequest.of(page, size, Sort.by("timestamp").descending())); }
}
