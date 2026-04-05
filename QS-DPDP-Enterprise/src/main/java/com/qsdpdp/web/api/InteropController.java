package com.qsdpdp.web.api;

import com.qsdpdp.interop.GlobalInteropService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Global Interoperability REST Controller
 * DPDP↔GDPR↔OECD↔ISO 27701 mapping and cross-border consent validation
 */
@RestController
@RequestMapping("/api/interop")
public class InteropController {

    @Autowired
    private GlobalInteropService interopService;

    @PostMapping("/validate-cross-border")
    public ResponseEntity<?> validateCrossBorder(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(interopService.validateCrossBorder(
                payload.getOrDefault("consentId", "C-001"),
                payload.getOrDefault("sourceJurisdiction", "INDIA"),
                payload.getOrDefault("targetJurisdiction", "EU")));
    }

    @GetMapping("/gdpr-mapping/{consentId}")
    public ResponseEntity<?> gdprMapping(@PathVariable String consentId) {
        return ResponseEntity.ok(interopService.getGDPRMapping(consentId));
    }

    @GetMapping("/frameworks")
    public ResponseEntity<?> frameworks() {
        return ResponseEntity.ok(interopService.getSupportedFrameworks());
    }
}
