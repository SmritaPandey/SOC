# QS-AV — Operational Manual
## Antivirus & Endpoint Protection v1.0.0

---

## 1. Product Overview

**QS-AV** provides multi-engine malware detection combining signature-based scanning, neural network analysis, heuristic detection, random forest classifiers, and YARA rules. It supports real-time file scanning, quarantine management, and threat intelligence integration.

### Key Capabilities
| Capability | Description | Standard |
|------------|-------------|----------|
| Multi-Engine Detection | Signature, Neural Net, Heuristic, Random Forest, YARA | NIST SI-3 |
| Real-Time Scanning | On-access and on-demand file scanning | NIST SI-3(1) |
| Quarantine | Isolated storage for detected threats | NIST SI-3(2) |
| Threat Intelligence | Hash-based IOC matching | MITRE ATT&CK |
| Confidence Scoring | 0-100 detection confidence for each verdict | NIST SI-4 |

---

## 2. System Requirements
| Component | Specification |
|-----------|--------------|
| **Port** | 9007 |
| **CPU** | 2 cores (4 for high-throughput scanning) |
| **RAM** | 2 GB (4 GB recommended) |
| **Java** | JDK 21+ |

## 3. Installation
```powershell
cd D:\SOC\qs-av && .\deploy.ps1
# Docker: docker compose up -d
# Verify: curl http://localhost:9007/api/v1/av/health
```

---

## 4. Operations Guide

### 4.1 Recording Scan Results
```bash
curl -X POST http://localhost:9007/api/v1/av/scans \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "C:\\Users\\john\\Downloads\\invoice.exe",
    "fileHash": "a1b2c3d4e5f6789012345678abcdef01",
    "fileSize": 2048576,
    "verdict": "MALWARE",
    "threatName": "Trojan.GenericKD.46542315",
    "detectionEngine": "NEURAL_NET",
    "confidenceScore": 97,
    "action": "QUARANTINED",
    "hostname": "WORKSTATION-042"
  }'
```

### 4.2 Verdicts
| Verdict | Description | Action |
|---------|-------------|--------|
| **CLEAN** | No threats detected | Allow |
| **MALWARE** | Confirmed malicious | Quarantine/Delete |
| **SUSPICIOUS** | Potentially unwanted behavior | Alert + Monitor |
| **PUP** | Potentially Unwanted Program | Alert user |

### 4.3 Detection Engines
| Engine | Method | Strength |
|--------|--------|----------|
| **SIGNATURE** | Hash & pattern matching | Known threats — fast |
| **NEURAL_NET** | Deep learning classification | Zero-day threats |
| **HEURISTIC** | Behavioral analysis | Polymorphic malware |
| **RANDOM_FOREST** | ML ensemble classification | Low false-positive rate |
| **YARA** | Custom rule-based detection | Targeted threat hunting |

### 4.4 Viewing Scan History
```bash
curl "http://localhost:9007/api/v1/av/scans?page=0&size=50"
```

---

## 5. API Reference
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/av/health` | Health check |
| GET | `/api/v1/av/dashboard` | Dashboard stats |
| GET | `/api/v1/av/scans` | List scan results |
| POST | `/api/v1/av/scans` | Record scan result |

## 6. Comparable Products
| Feature | QS-AV | Trend Micro | Kaspersky | Bitdefender | Windows Defender |
|---------|-------|-------------|-----------|-------------|-----------------|
| Multi-Engine | ✅ 5 engines | 3 | 3 | 3 | 2 |
| AI Detection | ✅ Neural Net + RF | ✅ | ✅ | ✅ | ✅ |
| YARA Rules | ✅ | ❌ | ✅ | ❌ | ❌ |
| On-Premises | ✅ | ✅ | ✅ | ✅ | ✅ |
| Open Source | ✅ | ❌ | ❌ | ❌ | ❌ |
| Confidence Score | ✅ | ❌ | ❌ | ❌ | ❌ |

---

*© 2026 NeurQAI Technologies — QShield CSOC Platform*
