package com.qshield.idam.controller;
import com.qshield.idam.model.*; import com.qshield.idam.service.IdamService;
import org.springframework.data.domain.Page; import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; import java.util.*;

@RestController @RequestMapping("/api/v1/idam") @CrossOrigin(origins = "*")
public class IdamController {
    private final IdamService idamService;
    public IdamController(IdamService idamService) { this.idamService = idamService; }
    @PostMapping("/auth/login") public ResponseEntity<Map<String,String>> login(@RequestBody Map<String,String> creds) { return ResponseEntity.ok(idamService.authenticate(creds.get("username"), creds.get("password"))); }
    @GetMapping("/dashboard") public ResponseEntity<Map<String,Object>> dashboard() { return ResponseEntity.ok(idamService.getDashboardStats()); }
    @GetMapping("/users") public ResponseEntity<Page<IdamUser>> users(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ResponseEntity.ok(idamService.getUsers(page, size)); }
    @PostMapping("/users") public ResponseEntity<IdamUser> createUser(@RequestBody IdamUser user) { return ResponseEntity.ok(idamService.createUser(user)); }
    @GetMapping("/health") public ResponseEntity<Map<String,Object>> health() { return ResponseEntity.ok(Map.of("product","QS-IDAM","version","1.0.0","status","UP")); }
}
