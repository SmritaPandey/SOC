package com.qsdpdp.web.api;

import com.qsdpdp.sector.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Unified Sector Compliance REST Controller
 * Routes sector-specific compliance requests to the appropriate
 * sector service based on sector code. All 18 DPDP-defined sectors
 * are supported (BFSI, Healthcare, Insurance + 15 new).
 *
 * @version 1.0.0
 * @since Phase 7 — Unified Sector API
 */
@RestController("sectorComplianceApiController")
@RequestMapping("/api/v1/sectors")
public class SectorComplianceController {

    // Existing 3 sectors
    @Autowired(required = false) private BFSIComplianceService bfsiService;
    @Autowired(required = false) private HealthcareComplianceService healthcareService;
    @Autowired(required = false) private InsuranceComplianceService insuranceService;

    // New 15 sectors
    @Autowired(required = false) private FintechComplianceService fintechService;
    @Autowired(required = false) private TelecomComplianceService telecomService;
    @Autowired(required = false) private GovernmentComplianceService governmentService;
    @Autowired(required = false) private EducationComplianceService educationService;
    @Autowired(required = false) private EcommerceComplianceService ecommerceService;
    @Autowired(required = false) private ManufacturingComplianceService manufacturingService;
    @Autowired(required = false) private EnergyUtilityComplianceService energyService;
    @Autowired(required = false) private TransportLogisticsComplianceService transportService;
    @Autowired(required = false) private MediaDigitalComplianceService mediaService;
    @Autowired(required = false) private AgriRuralComplianceService agriService;
    @Autowired(required = false) private PharmaComplianceService pharmaService;
    @Autowired(required = false) private RealEstateComplianceService realEstateService;
    @Autowired(required = false) private LegalComplianceService legalService;
    @Autowired(required = false) private HospitalityTravelComplianceService hospitalityService;
    @Autowired(required = false) private SocialMediaComplianceService socialMediaService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "module", "unified-sector-compliance",
                "status", "UP",
                "version", "1.0.0",
                "totalSectors", 18,
                "dpdpAct", "Digital Personal Data Protection Act, 2023"));
    }

    /**
     * List all supported sectors with their initialization status.
     */
    @GetMapping
    public ResponseEntity<Object> listSectors() {
        List<Map<String, Object>> sectors = new ArrayList<>();

        sectors.add(sectorInfo("BFSI", "Banking, Financial Services & Insurance", bfsiService != null));
        sectors.add(sectorInfo("HEALTHCARE", "Healthcare & Hospitals", healthcareService != null));
        sectors.add(sectorInfo("INSURANCE", "Insurance (General & Life)", insuranceService != null));
        sectors.add(sectorInfo("FINTECH", "Fintech / Digital Lending", fintechService != null));
        sectors.add(sectorInfo("TELECOM", "Telecom & ISP", telecomService != null));
        sectors.add(sectorInfo("GOVERNMENT", "Government / e-Governance", governmentService != null));
        sectors.add(sectorInfo("EDUCATION", "Education / EdTech", educationService != null));
        sectors.add(sectorInfo("ECOMMERCE", "E-Commerce / Retail", ecommerceService != null));
        sectors.add(sectorInfo("MANUFACTURING", "Manufacturing / Industrial", manufacturingService != null));
        sectors.add(sectorInfo("ENERGY", "Energy / Utilities", energyService != null));
        sectors.add(sectorInfo("TRANSPORT", "Transport / Logistics", transportService != null));
        sectors.add(sectorInfo("MEDIA", "Media / Digital Content / OTT", mediaService != null));
        sectors.add(sectorInfo("AGRI", "Agriculture / Rural", agriService != null));
        sectors.add(sectorInfo("PHARMA", "Pharmaceutical / Life Sciences", pharmaService != null));
        sectors.add(sectorInfo("REALESTATE", "Real Estate / Property", realEstateService != null));
        sectors.add(sectorInfo("LEGAL", "Legal & Professional Services", legalService != null));
        sectors.add(sectorInfo("HOSPITALITY", "Hospitality / Travel", hospitalityService != null));
        sectors.add(sectorInfo("SOCIALMEDIA", "Social Media Platforms", socialMediaService != null));

        return ResponseEntity.ok(Map.of(
                "totalSectors", sectors.size(),
                "sectors", sectors));
    }

    /**
     * Get sector-specific statistics by sector code.
     */
    @GetMapping("/{sectorCode}/stats")
    public ResponseEntity<Object> getSectorStats(@PathVariable String sectorCode) {
        try {
            Object stats = switch (sectorCode.toUpperCase()) {
                case "BFSI" -> bfsiService != null ? bfsiService.getStatistics() : notAvailable();
                case "HEALTHCARE" -> healthcareService != null ? healthcareService.getStatistics() : notAvailable();
                case "INSURANCE" -> insuranceService != null ? insuranceService.getStatistics() : notAvailable();
                case "FINTECH" -> fintechService != null ? fintechService.getStatistics() : notAvailable();
                case "TELECOM" -> telecomService != null ? telecomService.getStatistics() : notAvailable();
                case "GOVERNMENT" -> governmentService != null ? governmentService.getStatistics() : notAvailable();
                case "EDUCATION" -> educationService != null ? educationService.getStatistics() : notAvailable();
                case "ECOMMERCE" -> ecommerceService != null ? ecommerceService.getStatistics() : notAvailable();
                case "MANUFACTURING" -> manufacturingService != null ? manufacturingService.getStatistics() : notAvailable();
                case "ENERGY" -> energyService != null ? energyService.getStatistics() : notAvailable();
                case "TRANSPORT" -> transportService != null ? transportService.getStatistics() : notAvailable();
                case "MEDIA" -> mediaService != null ? mediaService.getStatistics() : notAvailable();
                case "AGRI" -> agriService != null ? agriService.getStatistics() : notAvailable();
                case "PHARMA" -> pharmaService != null ? pharmaService.getStatistics() : notAvailable();
                case "REALESTATE" -> realEstateService != null ? realEstateService.getStatistics() : notAvailable();
                case "LEGAL" -> legalService != null ? legalService.getStatistics() : notAvailable();
                case "HOSPITALITY" -> hospitalityService != null ? hospitalityService.getStatistics() : notAvailable();
                case "SOCIALMEDIA" -> socialMediaService != null ? socialMediaService.getStatistics() : notAvailable();
                default -> Map.of("error", "Unknown sector: " + sectorCode);
            };

            // Wrap result with sector code metadata
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("sectorCode", sectorCode.toUpperCase());
            result.put("statistics", stats);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get aggregate compliance dashboard across all sectors.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Object> getDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("totalSectors", 18);

        int active = 0;
        List<String> activeSectors = new ArrayList<>();
        List<String> inactiveSectors = new ArrayList<>();

        record SectorEntry(String code, String name, boolean available) {}
        List<SectorEntry> entries = List.of(
                new SectorEntry("BFSI", "BFSI", bfsiService != null),
                new SectorEntry("HEALTHCARE", "Healthcare", healthcareService != null),
                new SectorEntry("INSURANCE", "Insurance", insuranceService != null),
                new SectorEntry("FINTECH", "Fintech", fintechService != null),
                new SectorEntry("TELECOM", "Telecom", telecomService != null),
                new SectorEntry("GOVERNMENT", "Government", governmentService != null),
                new SectorEntry("EDUCATION", "Education", educationService != null),
                new SectorEntry("ECOMMERCE", "E-Commerce", ecommerceService != null),
                new SectorEntry("MANUFACTURING", "Manufacturing", manufacturingService != null),
                new SectorEntry("ENERGY", "Energy", energyService != null),
                new SectorEntry("TRANSPORT", "Transport", transportService != null),
                new SectorEntry("MEDIA", "Media", mediaService != null),
                new SectorEntry("AGRI", "Agriculture", agriService != null),
                new SectorEntry("PHARMA", "Pharma", pharmaService != null),
                new SectorEntry("REALESTATE", "Real Estate", realEstateService != null),
                new SectorEntry("LEGAL", "Legal", legalService != null),
                new SectorEntry("HOSPITALITY", "Hospitality", hospitalityService != null),
                new SectorEntry("SOCIALMEDIA", "Social Media", socialMediaService != null)
        );

        for (SectorEntry e : entries) {
            if (e.available()) { active++; activeSectors.add(e.code()); }
            else { inactiveSectors.add(e.code()); }
        }

        dashboard.put("activeSectors", active);
        dashboard.put("activeSectorCodes", activeSectors);
        dashboard.put("inactiveSectorCodes", inactiveSectors);
        dashboard.put("coveragePercent", (double) active / 18 * 100);

        return ResponseEntity.ok(dashboard);
    }

    private Map<String, Object> sectorInfo(String code, String name, boolean available) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("code", code);
        info.put("name", name);
        info.put("available", available);
        info.put("status", available ? "ACTIVE" : "NOT_LOADED");
        return info;
    }

    private Map<String, Object> notAvailable() {
        return new LinkedHashMap<>(Map.of("status", "SERVICE_NOT_AVAILABLE"));
    }
}
