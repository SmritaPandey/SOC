# QS-EDR — Operational Manual
## Endpoint Detection & Response v1.0.0

---

## 1. Product Overview

**QS-EDR** provides continuous endpoint monitoring, threat detection, and automated containment. It collects process, file, network, and registry telemetry from endpoints, correlates behaviors against MITRE ATT&CK techniques, and enables one-click endpoint isolation.

### Key Capabilities
| Capability | Description | Standard |
|------------|-------------|----------|
| Endpoint Monitoring | Real-time OS/process/file/network telemetry | NIST SI-4 |
| Threat Detection | Behavioral and signature-based detection | NIST SI-7 |
| Endpoint Isolation | One-click network quarantine of compromised hosts | NIST IR-4 |
| Process Tracking | Full process tree with parent-child visibility | MITRE T1059 |
| File Integrity | Hash-based file monitoring and alerting | NIST SI-7(1) |

---

## 2. System Requirements
| Component | Specification |
|-----------|--------------|
| **Server OS** | Windows 10/11, Ubuntu 22.04+, RHEL 9+ |
| **CPU** | 4 cores |
| **RAM** | 4 GB (8 GB recommended for 500+ endpoints) |
| **Disk** | 50 GB (telemetry storage) |
| **Java** | JDK 21+ |
| **Port** | 9003 |

---

## 3. Installation

```powershell
# Windows
cd D:\SOC\qs-edr
.\deploy.ps1

# Docker
docker compose up -d

# Verify
curl http://localhost:9003/api/v1/edr/health
```

---

## 4. Operations Guide

### 4.1 Registering Endpoints
```bash
curl -X POST http://localhost:9003/api/v1/edr/endpoints \
  -H "Content-Type: application/json" \
  -d '{
    "hostname": "WORKSTATION-042",
    "osType": "WINDOWS",
    "osVersion": "Windows 11 23H2",
    "ipAddress": "192.168.1.42",
    "macAddress": "AA:BB:CC:DD:EE:FF",
    "agentVersion": "1.0.0",
    "owner": "john.doe@company.com"
  }'
```

### 4.2 Ingesting Endpoint Events
```bash
curl -X POST http://localhost:9003/api/v1/edr/events \
  -H "Content-Type: application/json" \
  -d '{
    "hostname": "WORKSTATION-042",
    "eventType": "PROCESS_CREATE",
    "processName": "powershell.exe",
    "processId": 4532,
    "parentProcess": "cmd.exe",
    "filePath": "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
    "fileHash": "abc123def456...",
    "severity": "MEDIUM",
    "mitreTechnique": "T1059.001"
  }'
```

### 4.3 Event Types
| Type | Description | Risk |
|------|-------------|------|
| PROCESS_CREATE | New process spawned | Varies |
| FILE_MODIFY | File created/modified/deleted | Medium |
| NETWORK_CONNECT | Outbound network connection | Low-High |
| REGISTRY_MODIFY | Windows Registry change | High |
| DRIVER_LOAD | Kernel driver loaded | Critical |

### 4.4 Isolating a Compromised Endpoint
```bash
# Isolate endpoint by ID
curl -X POST http://localhost:9003/api/v1/edr/endpoints/42/isolate

# Endpoint status changes: ONLINE → ISOLATED
# All network traffic except EDR heartbeat is blocked
```

### 4.5 Endpoint Statuses
```
ONLINE → OFFLINE (agent not reporting)
       → ISOLATED (network quarantined)
       → COMPROMISED (active threat detected)
```

---

## 5. API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/edr/health` | Health check |
| GET | `/api/v1/edr/dashboard` | Dashboard stats |
| GET | `/api/v1/edr/endpoints` | List endpoints |
| POST | `/api/v1/edr/endpoints` | Register endpoint |
| POST | `/api/v1/edr/endpoints/{id}/isolate` | Isolate endpoint |
| GET | `/api/v1/edr/events` | List events |
| POST | `/api/v1/edr/events` | Ingest event |

---

## 6. Comparable Products
| Feature | QS-EDR | CrowdStrike Falcon | Carbon Black | SentinelOne |
|---------|--------|-------------------|--------------|-------------|
| Process Telemetry | ✅ | ✅ | ✅ | ✅ |
| Network Isolation | ✅ | ✅ | ✅ | ✅ |
| MITRE ATT&CK Mapping | ✅ | ✅ | ✅ | ✅ |
| AI Analysis | ✅ | ✅ | ❌ | ✅ |
| Open Source Backend | ✅ | ❌ | ❌ | ❌ |
| On-Premises Option | ✅ | ❌ | ✅ | ❌ |

---

*© 2026 NeurQAI Technologies — QShield CSOC Platform*
