# QS-VAM — Operational Manual
## Vulnerability Assessment & Management v1.0.0

---

## 1. Product Overview

**QS-VAM** provides enterprise vulnerability lifecycle management — from discovery through prioritization, remediation tracking, and verification. It supports CVE tracking, CVSS scoring, exploit intelligence, and SLA-based remediation workflows.

### Key Capabilities
| Capability | Description | Standard |
|------------|-------------|----------|
| CVE Tracking | Full CVE database integration with enrichment | NIST RA-5 |
| CVSS Scoring | v3.1 base/temporal/environmental scoring | NIST RA-5(2) |
| Remediation Workflow | Assignment, due dates, SLA tracking | ISO 27001 A.12 |
| Exploit Intelligence | Tracks known exploit availability per vulnerability | NIST SI-5 |
| Asset Mapping | Links vulnerabilities to specific assets/products | NIST CM-8 |

---

## 2. System Requirements
| Component | Specification |
|-----------|--------------|
| **Port** | 9008 |
| **CPU** | 2 cores |
| **RAM** | 2 GB |
| **Java** | JDK 21+ |

## 3. Installation
```powershell
cd D:\SOC\qs-vam && .\deploy.ps1
# Docker: docker compose up -d
# Verify: curl http://localhost:9008/api/v1/vam/health
```

---

## 4. Operations Guide

### 4.1 Reporting Vulnerabilities
```bash
curl -X POST http://localhost:9008/api/v1/vam/vulnerabilities \
  -H "Content-Type: application/json" \
  -d '{
    "cveId": "CVE-2024-3094",
    "title": "XZ Utils Backdoor — liblzma Supply Chain Compromise",
    "description": "Malicious code in xz/liblzma allows unauthorized SSH access",
    "severity": "CRITICAL",
    "cvssScore": 10.0,
    "affectedAsset": "LINUX-SRV-01",
    "affectedProduct": "xz-utils",
    "affectedVersion": "5.6.0-5.6.1",
    "exploitAvailable": true,
    "remediationAction": "Downgrade to xz-utils 5.4.x or apply vendor patch"
  }'
```

### 4.2 Vulnerability Lifecycle
```
OPEN → IN_PROGRESS → PATCHED → VERIFIED
                   → ACCEPTED_RISK (with justification)
                   → FALSE_POSITIVE
```

### 4.3 Updating Vulnerability Status
```bash
curl -X PUT "http://localhost:9008/api/v1/vam/vulnerabilities/1/status?status=PATCHED"
```

### 4.4 Severity Prioritization Matrix
| CVSS Score | Severity | SLA (Remediation) | Action |
|------------|----------|-------------------|--------|
| 9.0–10.0 | CRITICAL | 24 hours | Immediate patching, SOC alert |
| 7.0–8.9 | HIGH | 7 days | Priority patching |
| 4.0–6.9 | MEDIUM | 30 days | Scheduled maintenance |
| 0.1–3.9 | LOW | 90 days | Next maintenance window |

### 4.5 Dashboard Metrics
- **Total Vulnerabilities**: All discovered CVEs
- **Open**: Awaiting remediation
- **Critical**: CVSS ≥ 9.0
- **Patched**: Successfully remediated

---

## 5. API Reference
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/vam/health` | Health check |
| GET | `/api/v1/vam/dashboard` | Dashboard stats |
| GET | `/api/v1/vam/vulnerabilities` | List vulnerabilities |
| POST | `/api/v1/vam/vulnerabilities` | Report vulnerability |
| PUT | `/api/v1/vam/vulnerabilities/{id}/status` | Update status |

## 6. Comparable Products
| Feature | QS-VAM | Tenable Nessus | Qualys VMDR | Rapid7 InsightVM |
|---------|--------|---------------|-------------|-----------------|
| CVE Tracking | ✅ | ✅ | ✅ | ✅ |
| CVSS Scoring | ✅ | ✅ | ✅ | ✅ |
| Exploit Intel | ✅ | ✅ | ✅ | ✅ |
| SLA Workflow | ✅ | ❌ | ✅ | ✅ |
| On-Premises | ✅ | ✅ | ❌ | ✅ |
| Open Source | ✅ | ❌ | ❌ | ❌ |

---

*© 2026 NeurQAI Technologies — QShield CSOC Platform*
