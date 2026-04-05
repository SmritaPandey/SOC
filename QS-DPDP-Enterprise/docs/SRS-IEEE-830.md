# Software Requirements Specification (IEEE 830)

## QS-DPDP Enterprise — Compliance Operating System

**Version:** 2.0.0  
**Date:** 2026-03-11  
**Publisher:** QualityShield Technologies Pvt. Ltd.

---

## 1. Introduction

### 1.1 Purpose
This SRS defines the functional and non-functional requirements of **QS-DPDP Enterprise**, a comprehensive compliance operating system that enables Indian organisations to achieve, maintain, and demonstrate compliance with the **Digital Personal Data Protection Act, 2023** (DPDP Act) and its associated **Rules 2025**.

### 1.2 Scope
QS-DPDP Enterprise is a self-contained, on-premises Java application providing:
- Real-time compliance scoring across all DPDP Act sections
- Full lifecycle management for consents, data principal rights, breach notifications, and DPIA assessments
- Integrated PII scanning, DLP, and SIEM/SOAR capabilities
- Automated gap analysis with sector-specific benchmarks
- Policy engine with approval workflows
- Audit trail with hash-chain integrity verification
- AI-powered chatbot for contextual DPDP guidance
- Multi-language UI supporting 22+ Indian languages
- MFA/TOTP authentication for enhanced security
- OpenAPI/Swagger API documentation portal

### 1.3 Definitions, Acronyms, and Abbreviations

| Term | Definition |
|------|-----------|
| DPDP | Digital Personal Data Protection Act, 2023 |
| DPO | Data Protection Officer |
| DPBI | Data Protection Board of India |
| DPIA | Data Protection Impact Assessment |
| PII | Personally Identifiable Information |
| DLP | Data Loss Prevention |
| SIEM | Security Information & Event Management |
| SOAR | Security Orchestration, Automation & Response |
| TOTP | Time-based One-Time Password |
| MFA | Multi-Factor Authentication |
| CERT-In | Indian Computer Emergency Response Team |
| STQC | Standardisation Testing & Quality Certification |

### 1.4 References
1. Digital Personal Data Protection Act, 2023 — Gazette of India
2. DPDP Rules, 2025 — Ministry of Electronics & IT
3. OWASP Top Ten 2021
4. IEEE 830-1998 — Recommended Practice for SRS
5. CERT-In Directions of 28 April 2022

### 1.5 Overview
Section 2 provides a general product description. Section 3 specifies individual functional requirements per module. Section 4 covers non-functional requirements.

---

## 2. Overall Description

### 2.1 Product Perspective
QS-DPDP Enterprise operates as a standalone Spring Boot web application with embedded Tomcat server and SQLite database. It requires no external infrastructure beyond a JDK 17+ runtime. The system supports optional connectivity to PostgreSQL, MySQL, Oracle, or SQL Server for enterprise deployments.

#### System Architecture
```
┌──────────────────────────────────────────────┐
│              Browser / JavaFX UI             │
├──────────────────────────────────────────────┤
│         REST API Layer (89+ endpoints)       │
│  ┌─────────┬─────────┬─────────┬─────────┐  │
│  │ Consent │ Breach  │  DPIA   │ Rights  │  │
│  ├─────────┼─────────┼─────────┼─────────┤  │
│  │ Policy  │  SIEM   │   DLP   │   PII   │  │
│  ├─────────┼─────────┼─────────┼─────────┤  │
│  │  Audit  │ Report  │   Gap   │Complianc│  │
│  └─────────┴─────────┴─────────┴─────────┘  │
├──────────────────────────────────────────────┤
│  Security Layer (MFA, AES-256, Hash-chain)   │
├──────────────────────────────────────────────┤
│  Database (SQLite / PostgreSQL / MySQL / etc) │
└──────────────────────────────────────────────┘
```

### 2.2 Product Functions
12 core modules:

1. **Compliance Dashboard** — Real-time scoring, KPIs, charts
2. **Consent Management** — Collect, withdraw, preference, guardian consent
3. **Rights Management** — Access, correction, erasure, portability, nomination
4. **Breach Notification** — 72-hour SLA, DPBI/CERT-In notification
5. **DPIA Assessment** — Risk matrix, review workflow, approval
6. **Policy Engine** — CRUD lifecycle, approval, versioning
7. **Q-SIEM & Q-SOAR** — Event correlation, incident response, playbooks
8. **Q-DLP** — Data classification, exfiltration detection, policies
9. **PII Scanner** — File/DB/text scanning with regex + ML
10. **Gap Analysis** — Sector-specific questionnaires, benchmarks
11. **Audit Trail** — SHA-256 hash-chain, tamper detection
12. **Reporting & Analytics** — PDF/Excel generation, scheduled reports

### 2.3 User Classes and Characteristics

| Role | Access Level | Description |
|------|-------------|-------------|
| DPO (Data Protection Officer) | Full | Full administrative access to all modules |
| Admin | Administrative | User management, configuration, all modules |
| Auditor | Read-only | Audit trail, reports, compliance score |
| Data Principal (DP) | Self-service | View/manage own consents, submit rights requests |
| Data Fiduciary (DF) | Operational | Manage consents, respond to rights requests |
| Significant DF (SDF) | Enhanced | DF + mandatory DPIA, DPO appointment |

### 2.4 Operating Environment
- **OS:** Windows 10/11 (primary), Linux, macOS
- **Runtime:** JDK 17+
- **Database:** SQLite (embedded, default) or PostgreSQL/MySQL/Oracle/SQL Server
- **Browser:** Chrome 90+, Edge 90+, Firefox 88+, Safari 14+
- **Ports:** HTTP 8080 (configurable)

### 2.5 Design and Implementation Constraints
- All data stored and processed within Indian territory (data localisation)
- AES-256-GCM encryption for data at rest
- TLS 1.3 for data in transit
- Quantum-safe cryptography where applicable (BouncyCastle)
- No cloud dependency — fully air-gapped capable
- OWASP Top 10 mitigations built-in

### 2.6 Assumptions and Dependencies
- Java 17+ installed on target machine
- Minimum 256 MB RAM, 100 MB disk
- Network access required only for external notifications (email/SMS)

---

## 3. Specific Requirements

### 3.1 Consent Management Module (DPDP §§6-7)

| ID | Requirement | Priority |
|----|------------|----------|
| FR-CON-001 | Collect informed consent with purpose, language, notice version | Critical |
| FR-CON-002 | Withdraw consent at any time with reason tracking | Critical |
| FR-CON-003 | Manage consent preferences per data category | Critical |
| FR-CON-004 | Guardian consent for children under 18 (§9) | Critical |
| FR-CON-005 | Maintain consent receipts with timestamp and hash | Critical |
| FR-CON-006 | Consent statistics dashboard (total, active, withdrawn) | Important |
| FR-CON-007 | Export consent records in CSV format | Important |
| FR-CON-008 | Consent audit trail with immutable logging | Critical |

### 3.2 Rights Management Module (DPDP §§11-14)

| ID | Requirement | Priority |
|----|------------|----------|
| FR-RTS-001 | Submit rights requests: access, correction, erasure, portability, nomination, grievance | Critical |
| FR-RTS-002 | Auto-generate unique reference number per request | Critical |
| FR-RTS-003 | Automatic 30-day deadline computation (§12) | Critical |
| FR-RTS-004 | Workflow: submit → acknowledge → assign → complete/reject | Critical |
| FR-RTS-005 | Rights statistics with SLA compliance tracking | Important |
| FR-RTS-006 | Escalation alerts for approaching deadlines | Important |

### 3.3 Breach Notification Module (DPDP §8)

| ID | Requirement | Priority |
|----|------------|----------|
| FR-BRN-001 | Report breach with severity classification (LOW to CRITICAL) | Critical |
| FR-BRN-002 | 72-hour SLA countdown from detection to DPBI notification | Critical |
| FR-BRN-003 | Record DPBI notification reference | Critical |
| FR-BRN-004 | Record CERT-In 6-hour notification compliance | Critical |
| FR-BRN-005 | Breach lifecycle: OPEN → CONTAINED → INVESTIGATING → RESOLVED → CLOSED | Critical |
| FR-BRN-006 | Affected principal notification tracking | Important |
| FR-BRN-007 | Breach statistics and trend analysis | Important |

### 3.4 DPIA Assessment Module (DPDP §24)

| ID | Requirement | Priority |
|----|------------|----------|
| FR-DPA-001 | Create DPIA with data types, processing purpose, assessor | Critical |
| FR-DPA-002 | Risk assessment with likelihood × impact matrix | Critical |
| FR-DPA-003 | Multi-level review workflow: submit → review → approve | Critical |
| FR-DPA-004 | DPIA statistics with risk distribution | Important |
| FR-DPA-005 | Mandatory DPIA for Significant Data Fiduciaries | Critical |

### 3.5 Policy Engine Module

| ID | Requirement | Priority |
|----|------------|----------|
| FR-POL-001 | Create policies: code, name, description, category, content, owner | Critical |
| FR-POL-002 | Policy lifecycle: DRAFT → REVIEW → ACTIVE → ARCHIVED | Critical |
| FR-POL-003 | Policy version control with change history | Important |
| FR-POL-004 | Policy statistics and compliance metrics | Important |

### 3.6 Q-SIEM & SOAR Module

| ID | Requirement | Priority |
|----|------------|----------|
| FR-SIM-001 | Real-time security event ingestion and correlation | Critical |
| FR-SIM-002 | Alert generation based on configurable rules | Critical |
| FR-SIM-003 | Automated incident response playbooks (SOAR) | Important |
| FR-SIM-004 | SIEM dashboard with event timelines | Important |
| FR-SIM-005 | Threat intelligence feed integration | Enhancement |

### 3.7 Q-DLP Module

| ID | Requirement | Priority |
|----|------------|----------|
| FR-DLP-001 | Data classification engine (Public, Internal, Confidential, Restricted) | Critical |
| FR-DLP-002 | Policy-based data movement monitoring | Critical |
| FR-DLP-003 | Exfiltration detection and blocking | Critical |
| FR-DLP-004 | DLP incident reporting and remediation tracking | Important |

### 3.8 PII Scanner Module

| ID | Requirement | Priority |
|----|------------|----------|
| FR-PII-001 | Scan files for PII patterns (Aadhaar, PAN, passport, email, phone) | Critical |
| FR-PII-002 | Scan database tables for PII columns | Critical |
| FR-PII-003 | Scan free-text content with regex + NLP | Important |
| FR-PII-004 | PII scan report generation | Important |
| FR-PII-005 | Scheduled automated scans | Enhancement |

### 3.9 Gap Analysis Module

| ID | Requirement | Priority |
|----|------------|----------|
| FR-GAP-001 | Sector-specific assessment questionnaires (BFSI, Healthcare, IT, etc.) | Critical |
| FR-GAP-002 | Automated gap identification against DPDP Act sections | Critical |
| FR-GAP-003 | Remediation recommendations with priority | Important |
| FR-GAP-004 | Gap analysis report export | Important |

### 3.10 Compliance Engine Module

| ID | Requirement | Priority |
|----|------------|----------|
| FR-CMP-001 | Calculate overall compliance score (0-100) using RAG evaluator | Critical |
| FR-CMP-002 | Identify compliance gaps with section references | Critical |
| FR-CMP-003 | Rule engine for automated compliance checks | Critical |
| FR-CMP-004 | Compliance trend tracking over time | Important |

### 3.11 Audit Trail Module

| ID | Requirement | Priority |
|----|------------|----------|
| FR-AUD-001 | Log all system actions with timestamp, user, entity | Critical |
| FR-AUD-002 | SHA-256 hash-chain for tamper detection | Critical |
| FR-AUD-003 | Integrity verification with invalid entry reporting | Critical |
| FR-AUD-004 | Export audit trail for regulatory review | Important |

### 3.12 Reporting & Analytics Module

| ID | Requirement | Priority |
|----|------------|----------|
| FR-RPT-001 | Generate compliance reports in PDF format | Critical |
| FR-RPT-002 | Generate data export in Excel/CSV format | Critical |
| FR-RPT-003 | CERT-In submission-ready report format | Important |
| FR-RPT-004 | Dashboard analytics with charts (Chart.js) | Important |

### 3.13 Authentication & Security

| ID | Requirement | Priority |
|----|------------|----------|
| FR-SEC-001 | User authentication with role-based access control | Critical |
| FR-SEC-002 | MFA/TOTP support via authenticator apps | Critical |
| FR-SEC-003 | Session management with configurable timeout | Critical |
| FR-SEC-004 | API token authentication for programmatic access | Important |
| FR-SEC-005 | Password hashing with BCrypt | Critical |

### 3.14 Settings & Configuration

| ID | Requirement | Priority |
|----|------------|----------|
| FR-SET-001 | 6-step setup wizard: Sector → Language → Org Details → Hierarchy → DB → Employees | Critical |
| FR-SET-002 | Multi-language UI with 22+ Indian languages | Important |
| FR-SET-003 | Database configuration for 5 DB types | Important |
| FR-SET-004 | Organization details form with 16 fields | Important |

### 3.15 User Interface

| ID | Requirement | Priority |
|----|------------|----------|
| FR-UI-001 | Desktop-style File/Edit/View/Tools/Help menu bar | Important |
| FR-UI-002 | AI chatbot with slide-out panel | Important |
| FR-UI-003 | Keyboard shortcuts (Ctrl+S, Ctrl+P, F1, F5, Escape) | Important |
| FR-UI-004 | Search, sort, and filter on all data tables | Important |
| FR-UI-005 | CSV export and print support | Important |
| FR-UI-006 | OpenAPI/Swagger API documentation at `/swagger-ui.html` | Important |

---

## 4. Non-Functional Requirements

### 4.1 Performance

| ID | Requirement | Target |
|----|------------|--------|
| NFR-PRF-001 | Dashboard load time | < 2 seconds |
| NFR-PRF-002 | API response time (95th percentile) | < 500ms |
| NFR-PRF-003 | Concurrent users supported | 50+ |
| NFR-PRF-004 | Database query response | < 100ms |

### 4.2 Security

| ID | Requirement |
|----|------------|
| NFR-SEC-001 | AES-256-GCM encryption for data at rest |
| NFR-SEC-002 | TLS 1.3 for data in transit |
| NFR-SEC-003 | OWASP Top 10 2021 mitigations |
| NFR-SEC-004 | CSRF protection on all state-changing endpoints |
| NFR-SEC-005 | Security headers (X-Content-Type, X-Frame-Options, CSP) |
| NFR-SEC-006 | Rate limiting on authentication endpoints |

### 4.3 Reliability

| ID | Requirement | Target |
|----|------------|--------|
| NFR-REL-001 | System availability | 99.9% uptime |
| NFR-REL-002 | Automatic database backup | Configurable schedule |
| NFR-REL-003 | Graceful degradation on component failure | Required |

### 4.4 Maintainability

| ID | Requirement |
|----|------------|
| NFR-MNT-001 | Modular service-layer architecture with 12 independent modules |
| NFR-MNT-002 | Comprehensive JUnit 5 test suite (12 test classes, 100+ tests) |
| NFR-MNT-003 | OpenAPI 3.0 documentation for all REST endpoints |
| NFR-MNT-004 | Code follows Spring Boot conventions and Java 17 features |

### 4.5 Portability

| ID | Requirement |
|----|------------|
| NFR-PRT-001 | Cross-platform: Windows, Linux, macOS |
| NFR-PRT-002 | Dockerized deployment support (Dockerfile included) |
| NFR-PRT-003 | Cloud-ready with Render, Railway, Heroku support |
| NFR-PRT-004 | Air-gapped on-premises deployment capability |

### 4.6 Regulatory Compliance

| ID | Requirement |
|----|------------|
| NFR-REG-001 | Full DPDP Act 2023 compliance (Sections 4-33) |
| NFR-REG-002 | DPDP Rules 2025 compliance |
| NFR-REG-003 | CERT-In 6-hour incident reporting capability |
| NFR-REG-004 | STQC quality assurance readiness |
| NFR-REG-005 | DSCI best practices alignment |

### 4.7 Usability

| ID | Requirement |
|----|------------|
| NFR-USB-001 | Responsive UI (desktop + tablet) |
| NFR-USB-002 | Multi-language support (22+ Indian languages + English) |
| NFR-USB-003 | Keyboard navigation and shortcuts |
| NFR-USB-004 | Consistent visual design with Inter font family |

---

## 5. Appendices

### Appendix A: DPDP Act Section Mapping

| DPDP Section | Module | Status |
|-------------|--------|--------|
| §4 — Data Processing | Consent Management | ✅ Implemented |
| §5 — Notice | Consent Management | ✅ Implemented |
| §6 — Consent | Consent Management | ✅ Implemented |
| §7 — Consent Withdrawal | Consent Management | ✅ Implemented |
| §8 — Personal Data Breach | Breach Notification | ✅ Implemented |
| §9 — Children's Data | Consent (Guardian) | ✅ Implemented |
| §10 — Data Fiduciary Duties | Compliance Engine | ✅ Implemented |
| §11 — Right to Access | Rights Management | ✅ Implemented |
| §12 — Right to Correction/Erasure | Rights Management | ✅ Implemented |
| §13 — Right to Portability | Rights Management | ✅ Implemented |
| §14 — Right of Nomination | Rights Management | ✅ Implemented |
| §15 — Right to Grievance | Rights Management | ✅ Implemented |
| §17 — Data Protection Board | Breach Notification | ✅ Implemented |
| §24 — DPIA | DPIA Assessment | ✅ Implemented |

### Appendix B: API Endpoint Summary

| Controller | Endpoints | Tag |
|-----------|---------:|-----|
| DashboardController | 25+ | Dashboard & MFA |
| ConsentController | 10+ | Consent Management |
| BreachController | 8+ | Breach Notification |
| RightsController | 8+ | Rights Requests |
| DPIAController | 6+ | DPIA Assessments |
| PolicyController | 6+ | Policy Engine |
| SIEMController | 8+ | SIEM & SOAR |
| DLPController | 6+ | Data Loss Prevention |
| PIIScannerWebController | 5+ | PII Scanner |
| GapAnalysisController | 5+ | Gap Analysis |
| ComplianceController | 4+ | Compliance Engine |
| AuditController | 4+ | Audit Trail |
| ReportController | 6+ | Reporting & Analytics |
| SettingsController | 8+ | Settings |
| LicensingController | 3+ | Licensing |
| **Total** | **89+** | |

### Appendix C: Technology Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 3.2.0 + Java 17 |
| Frontend | HTML5 + JavaScript + CSS3 |
| Database | SQLite 3.44 (default) |
| Cryptography | BouncyCastle 1.77 |
| PDF Generation | OpenPDF 1.3.30 |
| Excel Export | Apache POI 5.2.5 |
| API Docs | SpringDoc OpenAPI 2.3.0 |
| Charts | Chart.js 4.4.1 |
| Icons | Font Awesome 6.4.2 |
| Testing | JUnit 5.10.1 |
