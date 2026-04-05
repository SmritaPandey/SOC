package com.qsdpdp.web.api;

import com.qsdpdp.ndce.NDCEService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * National DPDP Compliance Exchange (NDCE) REST Controller
 */
@RestController
@RequestMapping("/api/ndce")
public class NDCEController {

    @Autowired
    private NDCEService ndceService;

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> payload) {
        String consentId = payload.getOrDefault("consentId", "");
        String requestingOrg = payload.getOrDefault("requestingOrg", "");
        return ResponseEntity.ok(ndceService.verifyConsent(consentId, requestingOrg));
    }

    @PostMapping("/share")
    public ResponseEntity<?> share(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(ndceService.shareConsent(
                payload.getOrDefault("consentId", ""),
                payload.getOrDefault("fromOrg", ""),
                payload.getOrDefault("toOrg", ""),
                payload.getOrDefault("purpose", "data-processing")));
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(ndceService.revokeConsent(
                payload.getOrDefault("consentId", ""),
                payload.getOrDefault("revokedBy", "data-principal")));
    }

    @GetMapping("/registry")
    public ResponseEntity<?> registry() {
        return ResponseEntity.ok(ndceService.getRegistryStatus());
    }
}
