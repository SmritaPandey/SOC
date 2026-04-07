# QS-SOAR — Operational Manual
## Security Orchestration, Automation & Response v1.0.0

---

## 1. Product Overview

**QS-SOAR** automates security incident response through playbook-driven workflows, AI-powered recommendations, and integration with all QShield CSOC modules. It provides full incident lifecycle management from detection through containment, eradication, and recovery.

### Key Capabilities
| Capability | Description | Standard |
|------------|-------------|----------|
| Incident Management | Full lifecycle (Detect → Triage → Investigate → Contain → Resolve) | NIST IR-4 |
| Playbook Engine | 4 pre-built automated response playbooks | ISO 27035 |
| AI Recommendations | LLM-powered response action suggestions | IEEE 2807 |
| Auto-Triage | CRITICAL incidents auto-escalated with risk scoring | NIST IR-4(1) |
| Audit Trail | All actions logged with hash-chain integrity | NIST AU-10 |

---

## 2. System Requirements
| Component | Specification |
|-----------|--------------|
| **OS** | Windows 10/11, Ubuntu 22.04+, RHEL 9+, macOS 14+ |
| **CPU** | 2 cores (4 recommended) |
| **RAM** | 2 GB (4 GB recommended) |
| **Disk** | 10 GB |
| **Java** | JDK 21+ |
| **Database** | H2 (dev) / PostgreSQL 16 (prod) |
| **Port** | 9002 |

---

## 3. Installation

### Standalone (Windows)
```powershell
cd D:\SOC\qs-soar
.\deploy.ps1
```

### Docker
```bash
cd qs-soar && docker compose up -d
```

### Verify
```bash
curl http://localhost:9002/api/v1/soar/health
# {"product":"QS-SOAR","version":"1.0.0","status":"UP"}
```

---

## 4. Operations Guide

### 4.1 Dashboard
Open: **http://localhost:9002**

Displays: Total Incidents, Open Incidents, Critical (24h), Active Playbooks

### 4.2 Creating Incidents

```bash
curl -X POST http://localhost:9002/api/v1/soar/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Ransomware detected on WORKSTATION-042",
    "severity": "CRITICAL",
    "category": "MALWARE",
    "description": "CryptoLocker variant detected by EDR agent"
  }'
```

### 4.3 Incident Lifecycle
```
DETECTED → TRIAGED → INVESTIGATING → CONTAINED → ERADICATED → RECOVERED → CLOSED
                                                                         → POST_MORTEM
```

### 4.4 Updating Incident Status
```bash
curl -X PUT "http://localhost:9002/api/v1/soar/incidents/INC-20260407-0001/status?status=INVESTIGATING&assignedTo=analyst01"
```

### 4.5 Pre-Built Playbooks

| Playbook ID | Name | Trigger | Actions |
|------------|------|---------|---------|
| PB-MALWARE-01 | Malware Containment | CRITICAL Alert | Isolate Endpoint → Quarantine File → Block Hash → Notify SOC |
| PB-BRUTEFORCE-01 | Brute Force Response | AUTH Category | Block Source IP → Lock Account → Force MFA → Alert Admin |
| PB-PHISHING-01 | Phishing Response | EMAIL Category | Quarantine Email → Block Sender → Scan Recipients → Notify Users |
| PB-EXFIL-01 | Data Exfiltration | DLP Alert | Block Destination → Capture Netflow → Freeze Account → Alert DLP Team |

### 4.6 Creating Custom Playbooks
```bash
curl -X POST http://localhost:9002/api/v1/soar/playbooks \
  -H "Content-Type: application/json" \
  -d '{
    "playbookId": "PB-CUSTOM-01",
    "name": "Insider Threat Response",
    "triggerType": "CATEGORY",
    "triggerCondition": "category=INSIDER_THREAT",
    "actionsJson": "[{\"step\":1,\"action\":\"MONITOR_USER\"},{\"step\":2,\"action\":\"CAPTURE_FORENSICS\"},{\"step\":3,\"action\":\"ALERT_HR\"}]"
  }'
```

---

## 5. API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/soar/health` | Health check |
| GET | `/api/v1/soar/dashboard` | Dashboard stats |
| GET | `/api/v1/soar/incidents` | List incidents |
| POST | `/api/v1/soar/incidents` | Create incident |
| PUT | `/api/v1/soar/incidents/{id}/status` | Update status |
| GET | `/api/v1/soar/playbooks` | List playbooks |
| POST | `/api/v1/soar/playbooks` | Create playbook |

---

## 6. Integration with SIEM
SOAR receives alerts from QS-SIEM and automatically creates incidents:
```
QS-SIEM Alert (CRITICAL) → QS-SOAR Incident → Playbook Execution → Containment
```

---

## 7. Troubleshooting

| Issue | Solution |
|-------|----------|
| Playbooks not executing | Verify `enabled=true` on playbook |
| AI recommendations empty | Check `qshield.ai.enabled` setting |
| Incident not auto-triaged | Only CRITICAL severity triggers auto-triage |

---

*© 2026 NeurQAI Technologies — QShield CSOC Platform*
