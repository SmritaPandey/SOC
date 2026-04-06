package com.qshield.xdr.service;
import com.qshield.common.ai.AIAnalyticsEngine;
import com.qshield.common.audit.AuditService;
import com.qshield.xdr.model.*; import com.qshield.xdr.repository.*;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service;
import java.time.Instant; import java.time.temporal.ChronoUnit; import java.util.*;

@Service
public class XdrService {
    private final XdrCorrelationRepository corrRepo;
    private final AuditService auditService;
    private final AIAnalyticsEngine aiEngine;
    public XdrService(XdrCorrelationRepository corrRepo, AuditService auditService, AIAnalyticsEngine aiEngine) {
        this.corrRepo = corrRepo; this.auditService = auditService; this.aiEngine = aiEngine;
    }
    public XdrCorrelation createCorrelation(XdrCorrelation corr) {
        if (corr.getTimestamp() == null) corr.setTimestamp(Instant.now());
        String narrative = aiEngine.query("Analyze cross-layer threat: " + corr.getTitle(), "XDR Context");
        corr.setAiNarrative(narrative);
        auditService.log("XDR", "CORRELATION_CREATED", null, null, corr.getTitle(), corr.getSeverity());
        return corrRepo.save(corr);
    }
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalCorrelations", corrRepo.count());
        s.put("activeCorrelations", corrRepo.countByStatus("NEW") + corrRepo.countByStatus("INVESTIGATING"));
        return s;
    }
    public Page<XdrCorrelation> getCorrelations(int page, int size) {
        return corrRepo.findAll(PageRequest.of(page, size, Sort.by("timestamp").descending()));
    }
}
