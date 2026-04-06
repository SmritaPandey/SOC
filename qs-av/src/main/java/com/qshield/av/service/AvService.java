package com.qshield.av.service;
import com.qshield.common.audit.AuditService;
import com.qshield.av.model.*; import com.qshield.av.repository.*;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AvService {
    private final ScanResultRepository repo;
    private final AuditService auditService;
    public AvService(ScanResultRepository repo, AuditService auditService) { this.repo = repo; this.auditService = auditService; }
    public ScanResult recordScan(ScanResult result) {
        if (!"CLEAN".equals(result.getVerdict()))
            auditService.log("AV", "THREAT_DETECTED", null, null, result.getThreatName() + " in " + result.getFilePath(), "CRITICAL");
        return repo.save(result);
    }
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalScans", repo.count()); s.put("malwareDetected", repo.countByVerdict("MALWARE"));
        s.put("cleanFiles", repo.countByVerdict("CLEAN")); s.put("suspicious", repo.countByVerdict("SUSPICIOUS"));
        return s;
    }
    public Page<ScanResult> getResults(int page, int size) { return repo.findAll(PageRequest.of(page, size, Sort.by("timestamp").descending())); }
}
