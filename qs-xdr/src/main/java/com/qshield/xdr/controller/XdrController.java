package com.qshield.xdr.controller;
import com.qshield.xdr.model.*; import com.qshield.xdr.service.XdrService;
import org.springframework.data.domain.Page; import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; import java.util.Map;

@RestController @RequestMapping("/api/v1/xdr") @CrossOrigin(origins = "*")
public class XdrController {
    private final XdrService xdrService;
    public XdrController(XdrService xdrService) { this.xdrService = xdrService; }
    @GetMapping("/dashboard") public ResponseEntity<Map<String,Object>> dashboard() { return ResponseEntity.ok(xdrService.getDashboardStats()); }
    @GetMapping("/correlations") public ResponseEntity<Page<XdrCorrelation>> correlations(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return ResponseEntity.ok(xdrService.getCorrelations(page, size)); }
    @PostMapping("/correlations") public ResponseEntity<XdrCorrelation> create(@RequestBody XdrCorrelation corr) { return ResponseEntity.ok(xdrService.createCorrelation(corr)); }
    @GetMapping("/health") public ResponseEntity<Map<String,Object>> health() { return ResponseEntity.ok(Map.of("product","QS-XDR","version","1.0.0","status","UP")); }
}
