package com.qsdpdp.web.api;

import com.qsdpdp.consent.VoiceConsentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Voice Consent Controller — Multi-language voice consent API
 * 
 * @version 1.0.0
 * @since Phase 9
 */
@RestController
@RequestMapping("/api/voice-consent")
public class VoiceConsentController {

    private static final Logger logger = LoggerFactory.getLogger(VoiceConsentController.class);

    @Autowired(required = false) private VoiceConsentService voiceService;

    /** GET /api/voice-consent/languages — Supported languages */
    @GetMapping("/languages")
    public ResponseEntity<?> languages() {
        ensureInit();
        return ResponseEntity.ok(voiceService.getSupportedLanguages());
    }

    /** GET /api/voice-consent/prompt?language=hi&purpose=data+collection */
    @GetMapping("/prompt")
    public ResponseEntity<Map<String, Object>> prompt(
            @RequestParam(defaultValue = "en") String language,
            @RequestParam(defaultValue = "data collection") String purpose) {
        ensureInit();
        return ResponseEntity.ok(voiceService.getConsentPrompt(language, purpose));
    }

    /** POST /api/voice-consent/record — Record voice consent */
    @PostMapping("/record")
    public ResponseEntity<Map<String, Object>> record(@RequestBody Map<String, String> body) {
        ensureInit();
        return ResponseEntity.ok(voiceService.recordVoiceConsent(
                body.getOrDefault("principalId", ""),
                body.getOrDefault("consentType", "GRANT"),
                body.getOrDefault("language", "en"),
                body.getOrDefault("audioReference", ""),
                body.getOrDefault("transcription", ""),
                body.getOrDefault("purpose", "")
        ));
    }

    /** POST /api/voice-consent/{id}/verify — Verify voice consent */
    @PostMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable String id) {
        ensureInit();
        return ResponseEntity.ok(voiceService.verifyConsent(id));
    }

    private void ensureInit() {
        if (voiceService != null && !voiceService.isInitialized()) {
            try { voiceService.initialize(); } catch (Exception e) {
                logger.debug("VoiceConsentService init skipped: {}", e.getMessage());
            }
        }
    }
}
