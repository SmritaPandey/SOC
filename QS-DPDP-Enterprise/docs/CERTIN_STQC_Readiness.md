# QS-DPDP Enterprise — CERT-In / STQC / DSCI Readiness Checklist

## CERT-In Compliance (Indian Computer Emergency Response Team)

### Incident Reporting (CERT-In Directive 28 April 2022)

| # | Requirement | QS-DPDP Status | Module |
|---|---|---|---|
| 1 | Report cyber incidents within 6 hours | ✅ Done | Breach Module — CERT-In 6-hour notification flow |
| 2 | Maintain logs for 180 days (India jurisdiction) | ✅ Done | Audit Trail with hash-chain (configurable retention) |
| 3 | Synchronize ICT clocks to NTP | ⚡ Config | Server-side NTP; app uses system clock |
| 4 | Report the following incident types: | | |
| 4a | Targeted scanning/probing of critical networks | ✅ Done | SIEM event detection & auto-alerts |
| 4b | Compromise of critical systems | ✅ Done | SIEM — high-severity event workflows |
| 4c | Unauthorized access to IT systems/data | ✅ Done | SIEM + DLP incident tracking |
| 4d | Website defacement | ⚡ Partial | DLP monitoring (external scan not included) |
| 4e | Malicious code attacks (ransomware, etc.) | ✅ Done | SIEM event classification |
| 4f | Attacks on servers, networks, applications | ✅ Done | SIEM correlation rules |
| 4g | Data breach / data leak | ✅ Done | Breach Notification — full lifecycle |
| 4h | Attacks on IoT devices | ⚡ Partial | Framework exists; IoT profiles TBD |
| 5 | Designate Point of Contact (PoC) | ✅ Done | Settings → Organization → DPO/Grievance Officer |
| 6 | Enable logs of all ICT systems | ✅ Done | Audit Service — comprehensive logging |

### CERT-In Mandatory Fields for Incident Report

| Field | QS-DPDP Source |
|---|---|
| Organization Name | Settings → Organization |
| Sector | Settings → Sector |
| Incident Date/Time | Breach → `discovered_at` timestamp |
| Incident Type | Breach → severity and category |
| Systems Affected | Breach → `affected_systems` |
| Impact Summary | Breach → `impact_description` |
| Remediation Steps | Breach → `actions_taken`, `remediation_plan` |
| PoC Details | Settings → DPO name, email, phone |

## STQC (Standardisation Testing and Quality Certification)

| # | STQC Area | QS-DPDP Coverage |
|---|---|---|
| 1 | Secure coding practices | ✅ Argon2id, AES-256-GCM, prepared statements, input validation |
| 2 | OWASP Top 10 mitigation | ✅ See separate OWASP document |
| 3 | Data encryption at rest | ✅ AES-256-GCM via SecurityManager |
| 4 | Data encryption in transit | ✅ HTTPS (configurable), TLS 1.2+ |
| 5 | Access control mechanisms | ✅ Role-based with DPDP hierarchy mapping |
| 6 | Session management | ✅ Token-based with expiry |
| 7 | Input validation | ✅ Server-side validation, parameterized queries |
| 8 | Error handling | ✅ Structured error responses, no stack traces |
| 9 | Logging & monitoring | ✅ Hash-chained audit trail + SIEM |
| 10 | Configuration management | ✅ Settings wizard, DB-backed config |

## DSCI (Data Security Council of India) Best Practices

| Practice Area | QS-DPDP Implementation |
|---|---|
| Privacy by Design | Module-level consent checks, purpose limitation |
| Data Minimization | PII Scanner identifies unnecessary data collection |
| Storage Limitation | Configurable retention policies |
| Right to be Forgotten | Erasure request workflow with audit trail |
| Data Portability | JSON/CSV export for data principal requests |
| Cross-border Transfer | DLP policies for international data flow |
| Breach Response Plan | Automated DPBI + CERT-In notification with SLAs |
| DPO Appointment | Mandatory for SDF; tracked in org hierarchy |
| Awareness & Training | Policy acceptance tracking per employee |
