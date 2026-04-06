package com.qshield.vam.service;
import com.qshield.common.audit.AuditService;
import com.qshield.vam.model.*; import com.qshield.vam.repository.*;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class VamService {
    private final VulnerabilityRepository repo;
    private final AuditService auditService;
    public VamService(VulnerabilityRepository repo, AuditService auditService) { this.repo = repo; this.auditService = auditService; }
    public Vulnerability reportVulnerability(Vulnerability v) {
        auditService.log("VAM", "VULN_REPORTED", null, null, v.getCveId() + ": " + v.getTitle(), v.getSeverity());
        return repo.save(v);
    }
    public Vulnerability updateStatus(Long id, String status) {
        Vulnerability v = repo.findById(id).orElseThrow(); v.setStatus(status); return repo.save(v);
    }
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalVulnerabilities", repo.count()); s.put("openVulnerabilities", repo.countByStatus("OPEN"));
        s.put("criticalVulnerabilities", repo.countBySeverity("CRITICAL")); s.put("patchedVulnerabilities", repo.countByStatus("PATCHED"));
        return s;
    }
    public Page<Vulnerability> getVulnerabilities(int page, int size) { return repo.findAll(PageRequest.of(page, size, Sort.by("discoveredAt").descending())); }
}
