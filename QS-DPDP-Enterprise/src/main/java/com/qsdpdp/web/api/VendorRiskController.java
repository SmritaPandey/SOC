package com.qsdpdp.web.api;

import com.qsdpdp.vendor.VendorRiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Vendor Risk Management REST Controller
 * Vendor CRUD, risk assessments, data sharing records, incidents, and statistics
 *
 * @since DPDP Act — Third-Party Processor Management
 */
@RestController
@RequestMapping("/api/vendors")
public class VendorRiskController {

    private static final Logger logger = LoggerFactory.getLogger(VendorRiskController.class);

    private final VendorRiskService vendorService;

    @Autowired
    public VendorRiskController(VendorRiskService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getVendors(@RequestParam(required = false) String riskTier) {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        List<VendorRiskService.Vendor> vendors = vendorService.getVendorsByRisk(riskTier);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", vendors);
        result.put("total", vendors.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createVendor(@RequestBody Map<String, Object> body) {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        VendorRiskService.Vendor vendor = new VendorRiskService.Vendor();
        vendor.setName((String) body.getOrDefault("name", ""));
        vendor.setCategory((String) body.getOrDefault("category", "PROCESSOR"));
        vendor.setTier((String) body.getOrDefault("tier", "STANDARD"));
        vendor.setDescription((String) body.getOrDefault("description", ""));
        vendor.setCountry((String) body.getOrDefault("country", "India"));
        vendor.setContactName((String) body.getOrDefault("contactName", ""));
        vendor.setContactEmail((String) body.getOrDefault("contactEmail", ""));
        vendor.setContactPhone((String) body.getOrDefault("contactPhone", ""));
        vendor.setRiskTier((String) body.getOrDefault("riskTier", "MEDIUM"));

        String vendorId = vendorService.createVendor(vendor);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", vendorId != null);
        result.put("message", vendorId != null ? "Vendor created" : "Failed to create vendor");
        result.put("vendorId", vendorId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{vendorId}/assessment")
    public ResponseEntity<Map<String, Object>> startAssessment(
            @PathVariable String vendorId,
            @RequestBody Map<String, Object> body) {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        String type = (String) body.getOrDefault("type", "ANNUAL");
        String assessor = (String) body.getOrDefault("assessor", "DPO");

        String assessmentId = vendorService.startAssessment(vendorId, type, assessor);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", assessmentId != null);
        result.put("assessmentId", assessmentId);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{vendorId}/incident")
    public ResponseEntity<Map<String, Object>> reportIncident(
            @PathVariable String vendorId,
            @RequestBody Map<String, Object> body) {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        VendorRiskService.VendorIncident incident = new VendorRiskService.VendorIncident();
        incident.setIncidentType((String) body.getOrDefault("incidentType", "DATA_BREACH"));
        incident.setSeverity((String) body.getOrDefault("severity", "MEDIUM"));
        incident.setDescription((String) body.getOrDefault("description", ""));
        incident.setImpact((String) body.getOrDefault("impact", ""));
        incident.setRootCause((String) body.getOrDefault("rootCause", ""));
        incident.setRemediation((String) body.getOrDefault("remediation", ""));

        vendorService.reportVendorIncident(vendorId, incident);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "Incident reported for vendor: " + vendorId);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        VendorRiskService.VendorStatistics stats = vendorService.getStatistics();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("totalVendors", stats.getTotalVendors());
        result.put("highRiskVendors", stats.getHighRiskVendors());
        result.put("assessmentsDue", stats.getAssessmentsDue());
        result.put("openIncidents", stats.getOpenIncidents());
        result.put("crossBorderTransfers", stats.getCrossBorderTransfers());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/cross-border")
    public ResponseEntity<Map<String, Object>> getCrossBorderTransfers() {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        List<VendorRiskService.DataSharingRecord> records = vendorService.getCrossBorderTransfers();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", records);
        result.put("total", records.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/due-for-assessment")
    public ResponseEntity<Map<String, Object>> getDueForAssessment() {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        List<VendorRiskService.Vendor> vendors = vendorService.getVendorsDueForAssessment();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", vendors);
        result.put("total", vendors.size());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{vendorId}")
    public ResponseEntity<Map<String, Object>> updateVendor(
            @PathVariable String vendorId,
            @RequestBody Map<String, Object> body) {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        VendorRiskService.Vendor updates = new VendorRiskService.Vendor();
        updates.setName((String) body.getOrDefault("name", ""));
        updates.setCategory((String) body.getOrDefault("category", "PROCESSOR"));
        updates.setRiskTier((String) body.getOrDefault("riskTier", "MEDIUM"));
        updates.setCountry((String) body.getOrDefault("country", "India"));
        updates.setContactName((String) body.getOrDefault("contactName", ""));
        updates.setContactEmail((String) body.getOrDefault("contactEmail", ""));
        updates.setDescription((String) body.getOrDefault("description", ""));

        boolean updated = vendorService.updateVendor(vendorId, updates);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", updated);
        result.put("message", updated ? "Vendor updated" : "Vendor not found or inactive");

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{vendorId}")
    public ResponseEntity<Map<String, Object>> deactivateVendor(@PathVariable String vendorId) {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        boolean deactivated = vendorService.deactivateVendor(vendorId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", deactivated);
        result.put("message", deactivated ? "Vendor deactivated (ISO A.5.23)" : "Vendor not found");

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{vendorId}/assessments")
    public ResponseEntity<Map<String, Object>> getAssessmentHistory(@PathVariable String vendorId) {
        if (!vendorService.isInitialized()) {
            vendorService.initialize();
        }

        List<Map<String, Object>> history = vendorService.getAssessmentHistory(vendorId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", history);
        result.put("total", history.size());

        return ResponseEntity.ok(result);
    }
}
