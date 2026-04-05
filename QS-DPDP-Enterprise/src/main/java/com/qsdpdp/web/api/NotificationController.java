package com.qsdpdp.web.api;

import com.qsdpdp.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Notification REST API — DPDP Act 2023
 * Endpoints for sending notifications and viewing notification history.
 *
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @Autowired(required = false) private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<Object> sendNotification(@RequestBody Map<String, Object> request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(request));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Object> getNotifications(
            @RequestParam(required = false) String principalId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            return ResponseEntity.ok(notificationService.getNotifications(principalId, limit));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/breach/{breachId}/notify")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Object> sendBreachNotification(
            @PathVariable String breachId,
            @RequestBody Map<String, Object> request) {
        try {
            List<String> principalIds = (List<String>) request.getOrDefault("principalIds", List.of());
            String language = (String) request.getOrDefault("language", "en");
            return ResponseEntity.ok(notificationService.sendBreachNotification(breachId, principalIds, language));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
