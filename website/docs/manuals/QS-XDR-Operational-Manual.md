# QS-XDR — Operational Manual
## Extended Detection & Response v1.0.0

---

## 1. Product Overview

**QS-XDR** provides cross-layer threat detection by correlating data from SIEM, EDR, network, cloud, and identity sources. It generates AI-powered attack narratives and composite risk scores across the entire kill chain.

### Key Capabilities
| Capability | Description | Standard |
|------------|-------------|----------|
| Cross-Layer Correlation | Connects events from SIEM, EDR, IDAM, network | NIST SI-4(4) |
| Attack Chain Analysis | Maps multi-stage attacks across the kill chain | MITRE ATT&CK |
| AI Narratives | LLM-generated human-readable threat stories | IEEE 2807 |
| Composite Risk Scoring | Weighted risk from all data sources | NIST RA-3 |
| Multi-Source Ingestion | SIEM, EDR, IDAM, NETWORK, CLOUD | ISO 27001 A.12 |

---

## 2. System Requirements
| Component | Specification |
|-----------|--------------|
| **Port** | 9004 |
| **CPU** | 4 cores (8 recommended) |
| **RAM** | 4 GB (8 GB recommended) |
| **Java** | JDK 21+ |

## 3. Installation
```powershell
cd D:\SOC\qs-xdr && .\deploy.ps1
# or: docker compose up -d
# Verify: curl http://localhost:9004/api/v1/xdr/health
```

## 4. Operations Guide

### 4.1 Creating Cross-Layer Correlations
```bash
curl -X POST http://localhost:9004/api/v1/xdr/correlations \
  -H "Content-Type: application/json" \
  -d '{
    "title": "APT29 Multi-Stage Attack — Phishing to Lateral Movement",
    "severity": "CRITICAL",
    "dataSources": "[\"SIEM\",\"EDR\",\"IDAM\"]",
    "mitreTactic": "TA0001,TA0003,TA0008",
    "mitreTechnique": "T1566.001,T1059.001,T1021.002",
    "attackChain": "[{\"stage\":1,\"tactic\":\"Initial Access\",\"detail\":\"Spear-phishing email\"},{\"stage\":2,\"tactic\":\"Execution\",\"detail\":\"PowerShell payload\"},{\"stage\":3,\"tactic\":\"Lateral Movement\",\"detail\":\"SMB relay\"}]",
    "affectedAssets": "[\"MAIL-SRV-01\",\"WORKSTATION-042\",\"DC-01\"]",
    "compositeRiskScore": 95
  }'
```

### 4.2 Correlation Statuses
```
NEW → INVESTIGATING → CONFIRMED → REMEDIATED → CLOSED
                   → FALSE_POSITIVE
```

## 5. API Reference
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/xdr/health` | Health check |
| GET | `/api/v1/xdr/dashboard` | Dashboard stats |
| GET | `/api/v1/xdr/correlations` | List correlations |
| POST | `/api/v1/xdr/correlations` | Create correlation |

## 6. How XDR Differs from SIEM
| Aspect | QS-SIEM | QS-XDR |
|--------|---------|--------|
| Data Sources | Logs only | Logs + Endpoint + Network + Identity + Cloud |
| Correlation | Single-source rules | Cross-layer attack chains |
| Output | Event alerts | Attack narratives with kill chain stage |
| Use Case | Log monitoring | Advanced threat hunting |

---

*© 2026 NeurQAI Technologies — QShield CSOC Platform*
