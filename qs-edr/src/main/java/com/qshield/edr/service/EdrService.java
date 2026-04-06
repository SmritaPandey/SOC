package com.qshield.edr.service;
import com.qshield.common.audit.AuditService;
import com.qshield.edr.model.*;
import com.qshield.edr.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class EdrService {
    private final EndpointRepository endpointRepo;
    private final EndpointEventRepository eventRepo;
    private final AuditService auditService;

    public EdrService(EndpointRepository endpointRepo, EndpointEventRepository eventRepo, AuditService auditService) {
        this.endpointRepo = endpointRepo; this.eventRepo = eventRepo; this.auditService = auditService;
    }

    public Endpoint registerEndpoint(Endpoint ep) {
        auditService.log("EDR", "ENDPOINT_REGISTERED", null, ep.getIpAddress(), ep.getHostname(), "INFO");
        return endpointRepo.save(ep);
    }

    public EndpointEvent ingestEvent(EndpointEvent event) {
        if (event.getTimestamp() == null) event.setTimestamp(Instant.now());
        return eventRepo.save(event);
    }

    public Endpoint isolateEndpoint(Long id) {
        Endpoint ep = endpointRepo.findById(id).orElseThrow();
        ep.setIsolated(true); ep.setStatus("ISOLATED");
        auditService.log("EDR", "ENDPOINT_ISOLATED", null, ep.getIpAddress(), ep.getHostname(), "CRITICAL");
        return endpointRepo.save(ep);
    }

    public Map<String, Object> getDashboardStats() {
        Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalEndpoints", endpointRepo.count());
        stats.put("onlineEndpoints", endpointRepo.countByStatus("ONLINE"));
        stats.put("isolatedEndpoints", endpointRepo.countByStatus("ISOLATED"));
        stats.put("events24h", eventRepo.countByTimestampAfter(last24h));
        return stats;
    }

    public Page<Endpoint> getEndpoints(int page, int size) {
        return endpointRepo.findAll(PageRequest.of(page, size, Sort.by("lastSeen").descending()));
    }

    public Page<EndpointEvent> getEvents(String hostname, int page, int size) {
        if (hostname != null) return eventRepo.findByHostnameOrderByTimestampDesc(hostname, PageRequest.of(page, size));
        return eventRepo.findAll(PageRequest.of(page, size, Sort.by("timestamp").descending()));
    }
}
