# QS-DPDP Enterprise — Administrator Guide

## 1. Overview

QS-DPDP Enterprise is a comprehensive, production-ready compliance platform for India's Digital Personal Data Protection Act, 2023. This guide covers deployment, configuration, and administration of all modules.

## 2. System Requirements

| Component | Minimum | Recommended |
|---|---|---|
| Java | JDK 17 LTS | JDK 21 LTS |
| RAM | 4 GB | 8 GB |
| Disk | 2 GB | 10 GB |
| OS | Windows 10+ / Linux | Windows Server 2022 / Ubuntu 22+ |

## 3. Installation & Setup

### 3.1 Build from Source
```bash
cd QS-DPDP-Enterprise
mvn clean package -DskipTests
```

### 3.2 Run the Application
```bash
java -jar target/qs-dpdp-enterprise-1.0.0.jar
```
The application starts on port **8080** by default. The SQLite database is auto-created at `./qsdpdp.db`.

### 3.3 Configuration
Key properties in `application.properties`:
- `server.port` — HTTP port (default: 8080)
- `qsdpdp.db.path` — Database file path
- `qsdpdp.security.encryption-key` — AES-256 master key
- `spring.autoconfigure.exclude` — Exclude Redis if not used

## 4. Module Administration

### 4.1 Consent Management
- **API Base**: `/api/v1/consent`
- Collect, withdraw, modify, renew consents
- Guardian consent for children (DPDP §9/§33)
- Sector-specific purpose templates
- Consent withdrawal propagation to all processors (DPDP §6)
- Hash-chained audit trail for tamper detection

### 4.2 Breach Management
- **API Base**: `/api/v1/breach`
- 72-hour DPBI notification workflow
- 6-hour CERT-In notification for cyber incidents
- Severity classification and impact assessment

### 4.3 SIEM (Security Information & Event Management)
- **API Base**: `/api/v1/siem`
- Real-time security event monitoring
- Threat detection and alerting
- Compliance-relevant event correlation

### 4.4 DLP (Data Loss Prevention)
- **API Base**: `/api/v1/dlp`
- Content inspection and PII detection
- Data exfiltration prevention
- Policy-based blocking and alerting

### 4.5 Training & Awareness
- **API Base**: `/api/v1/training`
- 10 built-in DPDP training modules
- Quiz engine with MCQ assessment
- Campaign management for department-wide training
- Certificate generation on passing

### 4.6 AI Chatbot
- **API Base**: `/api/v1/chatbot`
- RAG-based (Retrieval-Augmented Generation) DPDP advisor
- Supports Ollama LLM backend with keyword-matching fallback
- Context-aware suggestions per module

### 4.7 Gap Analysis
- Automated compliance gap identification
- Sector-specific gap questionnaires
- Risk scoring and remediation roadmap

### 4.8 Sector Compliance (18 Sectors)
- **API Base**: `/api/v1/sectors`
- BFSI, Healthcare, Insurance, Fintech, Telecom, Government,
  Education, E-Commerce, Manufacturing, Energy, Transport,
  Media, Agriculture, Pharma, Real Estate, Legal, Hospitality,
  Social Media
- Each sector includes: database tables, regulatory mappings,
  compliance tracking, and statistics

## 5. Security Configuration

### 5.1 Access Control
- Role-Based Access Control (RBAC) for admin/user/auditor
- Attribute-Based Access Control (ABAC) for sector-level policies
- MFA support with OTP/TOTP

### 5.2 Encryption
- AES-256 encryption at rest
- TLS 1.3 in transit
- Quantum-safe key exchange (Kyber) readiness

### 5.3 Audit Logging
- All operations logged to `audit_log` table
- Hash-chained audit ledger for tamper detection
- Exportable for DPBI/CERT-In submissions

## 6. Multilingual Support

The platform supports 23 languages:
English, Hindi, Bengali, Telugu, Marathi, Tamil, Urdu, Gujarati, Kannada,
Malayalam, Odia, Punjabi, Assamese, Maithili, Sanskrit, Santali, Kashmiri,
Nepali, Sindhi, Dogri, Konkani, Manipuri, Bodo.

## 7. API Documentation

All REST APIs follow the pattern `/api/v1/{module}` and return JSON responses.
Use the `/health` endpoint on any module to verify its status.

## 8. Backup & Recovery

- **Database**: Copy `qsdpdp.db` file for backup
- **Configuration**: Back up `application.properties`
- **Audit Logs**: Export via `/api/v1/consent/audit-trail`

## 9. Troubleshooting

| Issue | Solution |
|---|---|
| Port conflict | Change `server.port` in application.properties |
| Database locked | Ensure no other process accesses the SQLite file |
| LLM unavailable | Chatbot falls back to keyword matching automatically |
| Redis error | Excluded by default; add `spring.autoconfigure.exclude` property |
