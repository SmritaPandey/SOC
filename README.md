# QShield CSOC — Cyber Security Operations Center
### Enterprise-Grade, AI-Powered, Open-Source Security Platform

---

## 🛡️ Overview

QShield CSOC is a complete Cyber Security Operations Center platform comprising **8 independent, interoperable security modules**. Each product can be deployed standalone or as part of the unified platform.

| # | Product | Port | Description |
|---|---------|------|-------------|
| 1 | **QS-SIEM** | 9001 | Security Information & Event Management |
| 2 | **QS-SOAR** | 9002 | Security Orchestration, Automation & Response |
| 3 | **QS-EDR** | 9003 | Endpoint Detection & Response |
| 4 | **QS-XDR** | 9004 | Extended Detection & Response |
| 5 | **QS-DLP** | 9005 | Data Loss Prevention |
| 6 | **QS-IDAM** | 9006 | Identity & Access Management |
| 7 | **QS-AV** | 9007 | Antivirus & Endpoint Protection |
| 8 | **QS-VAM** | 9008 | Vulnerability Assessment & Management |

## 🚀 Quick Start

### Prerequisites
- Java 21 (JDK)
- Apache Maven 3.9+

### Deploy All 8 Products (Windows)
```powershell
git clone https://github.com/SmritaPandey/SOC.git
cd SOC
mvn clean package -DskipTests
.\deploy-csoc.ps1
```

### Deploy a Single Product
```powershell
cd qs-siem   # or qs-soar, qs-edr, etc.
.\deploy.ps1
```

### Docker Deployment
```bash
docker compose up -d --build
```

### Stop All Services
```powershell
.\stop-csoc.ps1
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Unified SOC Manual](docs/manuals/QShield-CSOC-Unified-Operations-Manual.md) | Complete operations guide |
| [QS-SIEM Manual](docs/manuals/QS-SIEM-Operational-Manual.md) | SIEM operations |
| [QS-SOAR Manual](docs/manuals/QS-SOAR-Operational-Manual.md) | SOAR operations |
| [QS-EDR Manual](docs/manuals/QS-EDR-Operational-Manual.md) | EDR operations |
| [QS-XDR Manual](docs/manuals/QS-XDR-Operational-Manual.md) | XDR operations |
| [QS-DLP Manual](docs/manuals/QS-DLP-Operational-Manual.md) | DLP operations |
| [QS-IDAM Manual](docs/manuals/QS-IDAM-Operational-Manual.md) | IDAM operations |
| [QS-AV Manual](docs/manuals/QS-AV-Operational-Manual.md) | AV operations |
| [QS-VAM Manual](docs/manuals/QS-VAM-Operational-Manual.md) | VAM operations |

## 🏗️ Architecture

```
Java 21 + Spring Boot 3.3 + Maven Multi-Module
├── qs-common/          # Shared: JWT, Audit Trail, AI Engine
├── qs-siem/            # Port 9001
├── qs-soar/            # Port 9002
├── qs-edr/             # Port 9003
├── qs-xdr/             # Port 9004
├── qs-dlp/             # Port 9005
├── qs-idam/            # Port 9006
├── qs-av/              # Port 9007
├── qs-vam/             # Port 9008
├── qs-csoc-portal/     # Unified Single-Pane-of-Glass Portal
├── docs/manuals/       # Operational Manuals
└── website/            # Marketing Website + Download Center
```

## ✅ Compliance

NIST SP 800-53 Rev5 · NIST CSF 2.0 · ISO 27001:2022 · ISO 27035 · MITRE ATT&CK v15 · IS 15408 EAL-3 · IEEE 2807 · OWASP ASVS L2 · CIS Controls v8

## 📄 License

© 2026 NeurQAI Technologies Pvt. Ltd. All rights reserved.
