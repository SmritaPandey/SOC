# QS-DLP — Operational Manual
## Data Loss Prevention v1.0.0

---

## 1. Product Overview

**QS-DLP** prevents unauthorized data exfiltration by detecting sensitive information (PII, PHI, PCI, classified data) across email, USB, cloud, printer, and clipboard channels. It includes real-time content scanning with regex-based pattern matching for Indian and international data patterns.

### Key Capabilities
| Capability | Description | Standard |
|------------|-------------|----------|
| Content Scanning | Real-time PII/PHI/PCI detection in data streams | NIST SC-7 |
| Channel Monitoring | Email, USB, Cloud, Printer, Clipboard | ISO 27001 A.8 |
| Policy Engine | Data classification rules with auto-action | NIST SC-28 |
| Pattern Library | Aadhaar, PAN, SSN, Credit Card, Email, Phone | DPDP Act 2023 |
| Incident Management | Block, Encrypt, Mask, Alert, Quarantine actions | NIST IR-4 |

---

## 2. System Requirements
| Component | Specification |
|-----------|--------------|
| **Port** | 9005 |
| **CPU** | 2 cores (4 recommended) |
| **RAM** | 2 GB (4 GB for high-volume scanning) |
| **Java** | JDK 21+ |

## 3. Installation
```powershell
cd D:\SOC\qs-dlp && .\deploy.ps1
# Docker: docker compose up -d
# Verify: curl http://localhost:9005/api/v1/dlp/health
```

---

## 4. Operations Guide

### 4.1 Real-Time Content Scanning
```bash
curl -X POST http://localhost:9005/api/v1/dlp/scan \
  -H "Content-Type: text/plain" \
  -d "Please send payment to account holder Rajesh Kumar, 
      Aadhaar: 1234 5678 9012, PAN: ABCDE1234F, 
      Credit Card: 4111-1111-1111-1111, 
      Email: rajesh@company.com, Phone: +91 9876543210"
```

**Response:**
```json
{
  "AADHAAR": ["1234 5678 ****"],
  "PAN": ["ABCDE****"],
  "CREDIT_CARD": ["4111-1111-****-****"],
  "EMAIL": ["raje****@company.com"],
  "PHONE": ["+91 98765*****"]
}
```

### 4.2 Supported Detection Patterns
| Pattern | Regex | Region |
|---------|-------|--------|
| **Aadhaar** | `\d{4}[\s-]?\d{4}[\s-]?\d{4}` | India |
| **PAN** | `[A-Z]{5}\d{4}[A-Z]` | India |
| **Credit Card** | `(?:\d{4}[\s-]?){3}\d{4}` | Global |
| **Email** | `[\w.+-]+@[\w-]+\.[a-zA-Z]{2,}` | Global |
| **Phone** | `(?:\+91)?[\s-]?\d{10}` | India |
| **SSN** | `\d{3}-\d{2}-\d{4}` | USA |

### 4.3 Creating DLP Incidents
```bash
curl -X POST http://localhost:9005/api/v1/dlp/incidents \
  -H "Content-Type: application/json" \
  -d '{
    "title": "PII detected in outbound email to external recipient",
    "severity": "HIGH",
    "dataType": "PII",
    "channel": "EMAIL",
    "sourceUser": "john.doe@company.com",
    "destination": "external@gmail.com",
    "action": "BLOCK",
    "policyId": "POL-PII-001",
    "confidenceScore": 95
  }'
```

### 4.4 DLP Actions
| Action | Description | Use Case |
|--------|-------------|----------|
| **BLOCK** | Prevent data transfer | High-confidence PII to external |
| **ENCRYPT** | Force encryption on transfer | Partner communications |
| **MASK** | Replace sensitive data with masks | Logging and audit |
| **ALERT** | Notify SOC but allow transfer | Low-risk classifications |
| **QUARANTINE** | Hold for manual review | Unknown or suspicious patterns |

### 4.5 Incident Statuses
```
DETECTED → BLOCKED → RESOLVED
         → QUARANTINED → RESOLVED
         → RESOLVED (false positive)
```

---

## 5. API Reference
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/dlp/health` | Health check |
| GET | `/api/v1/dlp/dashboard` | Dashboard stats |
| POST | `/api/v1/dlp/scan` | Scan content for sensitive data |
| GET | `/api/v1/dlp/incidents` | List incidents |
| POST | `/api/v1/dlp/incidents` | Create incident |

## 6. Comparable Products
| Feature | QS-DLP | Symantec DLP | Forcepoint DLP | Digital Guardian |
|---------|--------|-------------|----------------|-----------------|
| Content Inspection | ✅ | ✅ | ✅ | ✅ |
| Indian PII (Aadhaar/PAN) | ✅ | ❌ | ❌ | ❌ |
| Channel Coverage | 5 | 5 | 4 | 4 |
| On-Premises | ✅ | ✅ | ✅ | ✅ |
| Open Source | ✅ | ❌ | ❌ | ❌ |

---

*© 2026 NeurQAI Technologies — QShield CSOC Platform*
