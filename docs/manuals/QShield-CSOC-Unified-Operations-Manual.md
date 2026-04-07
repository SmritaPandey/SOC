# QShield CSOC — Unified Operations Manual
## Cyber Security Operations Center Platform v1.0.0

---

## 1. Executive Summary

**QShield CSOC** is an enterprise-grade, AI-powered Cyber Security Operations Center platform comprising 8 independent yet interoperable security modules. Each module can be deployed standalone or as part of the unified platform, providing complete security coverage from endpoint to cloud.

### Platform Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                   QShield CSOC Unified Portal                │
│              (Single Pane of Glass — All Modules)            │
├───────────┬──────────┬──────────┬──────────┬────────────────┤
│  QS-SIEM  │ QS-SOAR  │  QS-EDR  │  QS-XDR  │   QS-DLP      │
│   :9001   │  :9002   │  :9003   │  :9004   │    :9005       │
├───────────┼──────────┼──────────┼──────────┤────────────────┤
│  QS-IDAM  │  QS-AV   │  QS-VAM  │ qs-common (shared lib)   │
│   :9006   │  :9007   │  :9008   │ JWT + Audit + AI Engine   │
└───────────┴──────────┴──────────┴──────────┴────────────────┘
│                    PostgreSQL 16 / H2                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Product Portfolio

| # | Product | Port | Function | Comparable To |
|---|---------|------|----------|---------------|
| 1 | **QS-SIEM** | 9001 | Log collection, correlation, UEBA | IBM QRadar, Splunk |
| 2 | **QS-SOAR** | 9002 | Incident response automation | Palo Alto XSOAR, Splunk SOAR |
| 3 | **QS-EDR** | 9003 | Endpoint monitoring & containment | CrowdStrike Falcon, Carbon Black |
| 4 | **QS-XDR** | 9004 | Cross-layer threat correlation | Palo Alto Cortex XDR, Trend Micro XDR |
| 5 | **QS-DLP** | 9005 | Data loss prevention | Symantec DLP, Forcepoint |
| 6 | **QS-IDAM** | 9006 | Identity & access management | Okta, CyberArk, ForgeRock |
| 7 | **QS-AV** | 9007 | Antivirus & endpoint protection | Trend Micro, Kaspersky, Bitdefender |
| 8 | **QS-VAM** | 9008 | Vulnerability management | Tenable Nessus, Qualys VMDR |

---

## 3. Deployment Options

### Option A: Full CSOC Platform (All 8 Modules)
```powershell
# Windows — One-command deployment
cd D:\SOC
.\deploy-csoc.ps1 -BuildFirst

# Launches all 8 products + unified portal
# Stop all: .\stop-csoc.ps1
```

### Option B: Individual Product (Standalone)
```powershell
# Example: Deploy only QS-SIEM
cd D:\SOC\qs-siem
.\deploy.ps1

# Example: Deploy only QS-AV
cd D:\SOC\qs-av
.\deploy.ps1
```

### Option C: Docker Deployment
```bash
# Full platform
cd D:\SOC
docker compose up -d

# Individual product
cd D:\SOC/qs-siem
docker compose up -d
```

### Option D: Linux/Mac Deployment
```bash
cd /opt/qshield/qs-siem
chmod +x deploy.sh
./deploy.sh
```

---

## 4. System Requirements

### Unified Platform (All 8 Modules)
| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 8 cores | 16 cores |
| **RAM** | 16 GB | 32 GB |
| **Disk** | 100 GB SSD | 500 GB SSD |
| **OS** | Windows 10/Ubuntu 22.04/RHEL 9 | Same |
| **Java** | JDK 21 | JDK 21 |
| **Database** | H2 (dev) | PostgreSQL 16 |
| **Ports** | 9001-9008 | Same |

### Single Product (Standalone)
| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **CPU** | 2 cores | 4 cores |
| **RAM** | 2 GB | 4 GB |
| **Disk** | 10 GB | 50 GB |

---

## 5. Unified Installation Procedure

### Step 1: Prerequisites
```powershell
# Verify Java 21
java -version
# Should show: openjdk version "21.x.x"

# Verify Maven (if building from source)
mvn -version
# Should show: Apache Maven 3.9.x
```

### Step 2: Build All Modules
```powershell
cd D:\SOC
$env:Path += ";C:\apache-maven-3.9.6\bin"
mvn clean package -DskipTests
```

### Step 3: Deploy
```powershell
.\deploy-csoc.ps1
```

### Step 4: Verify
Open browser to each product dashboard or use the unified portal:
- **Unified Portal**: `D:\SOC\qs-csoc-portal\index.html`
- **QS-SIEM**: http://localhost:9001
- **QS-SOAR**: http://localhost:9002
- **QS-EDR**: http://localhost:9003
- **QS-XDR**: http://localhost:9004
- **QS-DLP**: http://localhost:9005
- **QS-IDAM**: http://localhost:9006
- **QS-AV**: http://localhost:9007
- **QS-VAM**: http://localhost:9008

---

## 6. Module Integration Map

### Data Flow Between Modules
```
                    ┌──────────┐
    Syslog/CEF ────►│ QS-SIEM  │── Alert ──►┌──────────┐
                    │  :9001   │             │ QS-SOAR  │── Contain ──► QS-EDR
    Endpoints ─────►│          │             │  :9002   │              :9003
                    └──────────┘             └──────────┘
                         │                        │
                    Correlation              Playbook
                         │                   Execution
                         ▼                        │
                    ┌──────────┐                  ▼
                    │ QS-XDR   │◄──── Cross-layer correlation
                    │  :9004   │
                    └──────────┘

    Data Scan ─────►┌──────────┐     Auth ──►┌──────────┐
                    │ QS-DLP   │             │ QS-IDAM  │
                    │  :9005   │             │  :9006   │
                    └──────────┘             └──────────┘

    File Scan ─────►┌──────────┐    CVE ────►┌──────────┐
                    │  QS-AV   │             │ QS-VAM   │
                    │  :9007   │             │  :9008   │
                    └──────────┘             └──────────┘
```

### Integration Scenarios

| Scenario | Flow |
|----------|------|
| **Malware Response** | EDR detects → SIEM correlates → SOAR isolates endpoint → AV quarantines file |
| **Brute Force** | SIEM detects auth failures → IDAM locks account → SOAR blocks IP → XDR correlates attack chain |
| **Data Exfil** | DLP detects PII → SIEM logs event → SOAR creates incident → IDAM freezes user |
| **Vulnerability** | VAM discovers CVE → SIEM monitors exploit attempts → EDR watches for exploitation |

---

## 7. Shared Infrastructure

### 7.1 JWT Authentication (qs-common)
All products share stateless JWT authentication:
```
Access Token:  15 minutes TTL
Refresh Token: 7 days TTL
Algorithm:     HMAC-SHA256
```

### 7.2 Audit Trail (qs-common)
Every action across all modules is logged to a hash-chained audit trail:
```
Record N: hash(timestamp | product | event | details | hash_of_record_N-1)
```
This guarantees tamper evidence per NIST AU-10.

### 7.3 AI Analytics Engine (qs-common)
All modules share the AI engine for:
- **TF-IDF RAG**: Threat intelligence retrieval
- **Anomaly Scoring**: Statistical deviation detection
- **LLM Integration**: Ollama-based threat narrative generation

---

## 8. Compliance Mapping

| Standard | Controls Implemented | Modules |
|----------|---------------------|---------|
| **NIST SP 800-53 Rev5** | AC-2, AC-3, AC-7, AU-6, AU-10, IA-2, IR-4, RA-3, RA-5, SC-7, SC-28, SI-3, SI-4, SI-7 | All |
| **NIST CSF 2.0** | ID.AM, PR.AC, PR.DS, DE.AE, DE.CM, RS.AN, RS.MI, RC.RP | All |
| **ISO 27001:2022** | A.5-A.8 (Information security policies through Asset management) | All |
| **ISO 27035** | Incident management lifecycle | SOAR |
| **MITRE ATT&CK v15** | 14 Tactics, 200+ Techniques mapped | SIEM, XDR, EDR |
| **IS 15408 EAL-3** | Indian Common Criteria | All |
| **IEEE 2807** | AI Ethics in security analytics | AI Engine |
| **OWASP ASVS L2** | API security, input validation | All APIs |
| **CIS Controls v8** | Controls 1-18 | All |

---

## 9. Production Hardening Checklist

- [ ] Change default IDAM admin password
- [ ] Set `JWT_SECRET` environment variable (256-bit random)
- [ ] Switch database to PostgreSQL 16 (`SPRING_PROFILES_ACTIVE=prod`)
- [ ] Enable TLS/HTTPS on all product ports
- [ ] Configure firewall rules (restrict 9001-9008 to SOC network)
- [ ] Set up log rotation for application logs
- [ ] Configure database backups (daily)
- [ ] Enable audit trail integrity verification (weekly cron)
- [ ] Set up monitoring alerts for service health endpoints
- [ ] Review and customize SIEM correlation rules
- [ ] Configure SOAR playbooks for your environment
- [ ] Register all endpoints with EDR
- [ ] Import vulnerability scan data into VAM
- [ ] Configure DLP policies for your data classification

---

## 10. Troubleshooting

| Issue | Solution |
|-------|----------|
| Port conflict | Change port in `application.yml` |
| Database locked (H2) | Stop all instances; delete `data/` directory |
| Out of memory | Increase JVM heap: `-Xmx2g` |
| JWT token expired | Re-authenticate via IDAM login endpoint |
| Service won't start | Check Java 21 installed: `java -version` |
| Build fails | Run `mvn install -pl qs-common -DskipTests` first |
| Audit trail corrupt | Run integrity verification; restore from backup |

---

## 11. Support & Documentation

| Resource | Location |
|----------|----------|
| Individual Manuals | `D:\SOC\docs\manuals\QS-{PRODUCT}-Operational-Manual.md` |
| API Health Checks | `http://localhost:{port}/api/v1/{product}/health` |
| H2 Database Console | `http://localhost:{port}/h2-console` |
| Source Code | `https://github.com/SmritaPandey/SOC` |
| Issue Tracker | `https://github.com/SmritaPandey/SOC/issues` |

---

*© 2026 NeurQAI Technologies Pvt. Ltd. — QShield CSOC Platform*
*All rights reserved. Unauthorized reproduction prohibited.*
