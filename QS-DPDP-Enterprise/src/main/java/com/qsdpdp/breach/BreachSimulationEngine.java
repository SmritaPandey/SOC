package com.qsdpdp.breach;

import com.qsdpdp.events.ComplianceEvent;
import com.qsdpdp.events.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Breach Simulation Engine — DPDP Board Audit Simulation
 * 
 * Provides:
 * - Predefined breach templates (sector-wise)
 * - Full breach lifecycle simulation
 * - Response time measurement
 * - DPDP Board audit drill
 * - CERT-In notification simulation
 * - Team readiness scoring
 * 
 * @version 1.0.0
 * @since Phase 6 — Breach Enhancement
 */
@Service
public class BreachSimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(BreachSimulationEngine.class);

    @Autowired(required = false) private EventBus eventBus;

    // Sector-wise breach templates
    private static final Map<String, List<Map<String, Object>>> TEMPLATES = new LinkedHashMap<>();
    static {
        TEMPLATES.put("BFSI", List.of(
            breachTemplate("BFSI-001", "Card data breach via compromised POS", "CRITICAL", 50000,
                    "Payment card data exposed through compromised point-of-sale system",
                    List.of("PAN", "CARD_NUMBER", "CVV"), 6),
            breachTemplate("BFSI-002", "Insider access to customer accounts", "HIGH", 5000,
                    "Employee accessed customer accounts without authorization",
                    List.of("ACCOUNT_NUMBER", "BALANCE", "TRANSACTION_DATA"), 6),
            breachTemplate("BFSI-003", "UPI ID harvesting attack", "HIGH", 100000,
                    "Automated scraping of UPI IDs through API vulnerability",
                    List.of("UPI_ID", "PHONE", "NAME"), 6),
            breachTemplate("BFSI-004", "KYC documents leak", "CRITICAL", 25000,
                    "KYC documents including Aadhaar exposed via misconfigured storage",
                    List.of("AADHAAR", "PAN", "PHOTO", "ADDRESS"), 6)
        ));

        TEMPLATES.put("HEALTHCARE", List.of(
            breachTemplate("HC-001", "EHR system ransomware", "CRITICAL", 100000,
                    "Electronic Health Records encrypted by ransomware",
                    List.of("HEALTH_RECORDS", "MEDICAL_HISTORY", "PRESCRIPTIONS"), 6),
            breachTemplate("HC-002", "Lab report portal exposure", "HIGH", 10000,
                    "Lab reports accessible without authentication",
                    List.of("HEALTH_RECORDS", "NAME", "DOB"), 6)
        ));

        TEMPLATES.put("INSURANCE", List.of(
            breachTemplate("INS-001", "Policy data exfiltration", "HIGH", 30000,
                    "Policy holder data exfiltrated via SQL injection",
                    List.of("NAME", "DOB", "HEALTH_RECORDS", "INCOME"), 6),
            breachTemplate("INS-002", "Claims fraud ring data breach", "MEDIUM", 5000,
                    "Claims processing data accessed by fraud ring",
                    List.of("NAME", "ACCOUNT_NUMBER", "CLAIM_DATA"), 6)
        ));

        TEMPLATES.put("TELECOM", List.of(
            breachTemplate("TEL-001", "CDR mass exposure", "CRITICAL", 500000,
                    "Call Detail Records exposed via API vulnerability",
                    List.of("PHONE", "CALL_RECORDS", "LOCATION"), 6),
            breachTemplate("TEL-002", "SIM swap fraud", "HIGH", 1000,
                    "SIM swap attack enabling account takeover",
                    List.of("PHONE", "AADHAAR", "NAME"), 6)
        ));

        TEMPLATES.put("GOVERNMENT", List.of(
            breachTemplate("GOV-001", "Citizen database leak", "CRITICAL", 1000000,
                    "Citizen database exposed via misconfigured API",
                    List.of("AADHAAR", "NAME", "ADDRESS", "DOB"), 6),
            breachTemplate("GOV-002", "Document forgery detection", "HIGH", 5000,
                    "Forged government documents detected in system",
                    List.of("AADHAAR", "PAN", "DRIVING_LICENSE"), 6)
        ));
    }

    /**
     * Get breach templates by sector
     */
    public Map<String, Object> getTemplates(String sector) {
        if (sector != null && !sector.isEmpty()) {
            List<Map<String, Object>> templates = TEMPLATES.getOrDefault(sector.toUpperCase(), List.of());
            return Map.of("sector", sector, "templates", templates, "count", templates.size());
        }
        Map<String, Object> all = new LinkedHashMap<>();
        TEMPLATES.forEach((s, t) -> all.put(s, Map.of("templates", t, "count", t.size())));
        all.put("totalSectors", TEMPLATES.size());
        all.put("totalTemplates", TEMPLATES.values().stream().mapToInt(List::size).sum());
        return all;
    }

    /**
     * Start a breach simulation
     */
    public Map<String, Object> startSimulation(String templateId, String teamLead) {
        // Find template
        Map<String, Object> template = null;
        for (List<Map<String, Object>> templates : TEMPLATES.values()) {
            for (Map<String, Object> t : templates) {
                if (templateId.equals(t.get("templateId"))) { template = t; break; }
            }
            if (template != null) break;
        }

        if (template == null) {
            return Map.of("error", "Template not found: " + templateId);
        }

        String simId = "SIM-" + System.currentTimeMillis();
        Map<String, Object> simulation = new LinkedHashMap<>();
        simulation.put("simulationId", simId);
        simulation.put("templateId", templateId);
        simulation.put("status", "IN_PROGRESS");
        simulation.put("teamLead", teamLead);
        simulation.put("startedAt", LocalDateTime.now().toString());
        simulation.put("template", template);

        // Generate checklist
        simulation.put("checklist", List.of(
            Map.of("step", 1, "action", "DETECT", "description", "Identify and classify the breach",
                    "deadline", "T+0h", "status", "PENDING"),
            Map.of("step", 2, "action", "CONTAIN", "description", "Isolate affected systems",
                    "deadline", "T+1h", "status", "PENDING"),
            Map.of("step", 3, "action", "ASSESS", "description", "Determine scope and impact",
                    "deadline", "T+2h", "status", "PENDING"),
            Map.of("step", 4, "action", "NOTIFY_DPO", "description", "Notify Data Protection Officer",
                    "deadline", "T+2h", "status", "PENDING"),
            Map.of("step", 5, "action", "NOTIFY_CERTIN", "description", "Report to CERT-In (mandatory within 6h)",
                    "deadline", "T+6h", "status", "PENDING"),
            Map.of("step", 6, "action", "NOTIFY_BOARD", "description", "Report to DPDP Board",
                    "deadline", "T+72h", "status", "PENDING"),
            Map.of("step", 7, "action", "NOTIFY_PRINCIPALS", "description", "Notify affected data principals",
                    "deadline", "T+72h", "status", "PENDING"),
            Map.of("step", 8, "action", "REMEDIATE", "description", "Implement fixes and patches",
                    "deadline", "T+7d", "status", "PENDING"),
            Map.of("step", 9, "action", "RCA", "description", "Root Cause Analysis report",
                    "deadline", "T+14d", "status", "PENDING"),
            Map.of("step", 10, "action", "REVIEW", "description", "Post-incident review and lessons learned",
                    "deadline", "T+30d", "status", "PENDING")
        ));

        // Publish event
        if (eventBus != null && eventBus.isInitialized()) {
            eventBus.publish(new ComplianceEvent("breach.simulation.started", simulation, "BREACH_SIM"));
        }

        return simulation;
    }

    private static Map<String, Object> breachTemplate(String id, String name, String severity,
            int estimatedAffected, String description, List<String> dataCategories, int certInDeadlineHours) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("templateId", id); t.put("name", name); t.put("severity", severity);
        t.put("estimatedAffected", estimatedAffected); t.put("description", description);
        t.put("dataCategories", dataCategories); t.put("certInDeadlineHours", certInDeadlineHours);
        return t;
    }
}
