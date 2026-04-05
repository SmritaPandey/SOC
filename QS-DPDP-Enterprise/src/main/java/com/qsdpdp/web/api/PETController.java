package com.qsdpdp.web.api;

import com.qsdpdp.pet.PETService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Privacy Enhancing Technology (PET) REST Controller
 * Differential Privacy, Zero-Knowledge Proofs, Federated Learning, and Secure MPC
 */
@RestController
@RequestMapping("/api/pet")
public class PETController {

    @Autowired
    private PETService petService;

    @PostMapping("/differential-privacy/query")
    public ResponseEntity<?> dpQuery(@RequestBody Map<String, Object> payload) {
        double trueValue = ((Number) payload.getOrDefault("trueValue", 100)).doubleValue();
        double sensitivity = ((Number) payload.getOrDefault("sensitivity", 1.0)).doubleValue();
        double epsilon = ((Number) payload.getOrDefault("epsilon", 1.0)).doubleValue();
        String mechanism = (String) payload.getOrDefault("mechanism", "laplace");

        if ("gaussian".equalsIgnoreCase(mechanism)) {
            double delta = ((Number) payload.getOrDefault("delta", 1e-5)).doubleValue();
            return ResponseEntity.ok(petService.dpGaussianQuery(trueValue, sensitivity, epsilon, delta));
        }
        return ResponseEntity.ok(petService.dpLaplaceQuery(trueValue, sensitivity, epsilon));
    }

    @PostMapping("/zkp/prove")
    public ResponseEntity<?> prove(@RequestBody Map<String, Object> payload) {
        String claim = (String) payload.getOrDefault("claim", "identity_verified");
        Object value = payload.getOrDefault("value", 1);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) payload.getOrDefault("params", Collections.emptyMap());
        return ResponseEntity.ok(petService.generateProof(claim, value, params));
    }

    @PostMapping("/zkp/verify")
    public ResponseEntity<?> verifyProof(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(petService.verifyProof(payload.getOrDefault("proofId", "")));
    }

    @PostMapping("/federated/register")
    public ResponseEntity<?> registerModel(@RequestBody Map<String, Object> payload) {
        String modelId = (String) payload.getOrDefault("modelId", "model-1");
        String participantId = (String) payload.getOrDefault("participantId", "participant-1");
        @SuppressWarnings("unchecked")
        List<Number> weightsList = (List<Number>) payload.getOrDefault("weights", List.of(0.5, 0.3, 0.2));
        double[] weights = weightsList.stream().mapToDouble(Number::doubleValue).toArray();
        return ResponseEntity.ok(petService.registerLocalModel(modelId, participantId, weights));
    }

    @PostMapping("/federated/aggregate")
    public ResponseEntity<?> aggregate(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(petService.aggregateModels(payload.getOrDefault("modelId", "model-1")));
    }

    @PostMapping("/mpc/split")
    public ResponseEntity<?> splitSecret(@RequestBody Map<String, Object> payload) {
        double secret = ((Number) payload.getOrDefault("secret", 42)).doubleValue();
        int numShares = ((Number) payload.getOrDefault("numShares", 5)).intValue();
        int threshold = ((Number) payload.getOrDefault("threshold", 3)).intValue();
        return ResponseEntity.ok(petService.splitSecret(secret, numShares, threshold));
    }

    @PostMapping("/mpc/reconstruct")
    public ResponseEntity<?> reconstruct(@RequestBody Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> shares = (List<Map<String, Object>>) payload.get("shares");
        return ResponseEntity.ok(petService.reconstructSecret(shares));
    }

    @GetMapping("/capabilities")
    public ResponseEntity<?> capabilities() {
        return ResponseEntity.ok(petService.getCapabilities());
    }
}
