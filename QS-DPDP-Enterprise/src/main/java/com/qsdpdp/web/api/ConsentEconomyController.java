package com.qsdpdp.web.api;

import com.qsdpdp.economy.ConsentEconomyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Consent Economy REST Controller
 * Consent Wallet, Marketplace, and Notification Engine
 */
@RestController
@RequestMapping("/api")
public class ConsentEconomyController {

    @Autowired
    private ConsentEconomyService economyService;

    @GetMapping("/wallet/{principalId}")
    public ResponseEntity<?> getWallet(@PathVariable String principalId) {
        return ResponseEntity.ok(economyService.getWallet(principalId));
    }

    @PostMapping("/wallet/{principalId}/revoke-all")
    public ResponseEntity<?> revokeAll(@PathVariable String principalId) {
        return ResponseEntity.ok(economyService.revokeAll(principalId));
    }

    @GetMapping("/marketplace/listings")
    public ResponseEntity<?> listings() {
        return ResponseEntity.ok(economyService.getListings());
    }

    @PostMapping("/notifications/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody Map<String, Object> payload) {
        String principalId = (String) payload.getOrDefault("principalId", "unknown");
        return ResponseEntity.ok(economyService.subscribe(principalId, payload));
    }

    @GetMapping("/notifications/{principalId}")
    public ResponseEntity<?> getNotifications(@PathVariable String principalId) {
        return ResponseEntity.ok(economyService.getNotifications(principalId));
    }
}
