# QS-DPDP Control Catalogue

## Policy → Procedure → Control → Evidence Framework

---

## 1. Consent Management Controls

### DPDP-CON-001: Valid Consent Collection
| Attribute | Value |
|-----------|-------|
| **Policy** | Data Protection Policy |
| **Procedure** | Consent Collection Procedure |
| **Control Type** | Preventive |
| **DPDP Section** | Section 6 |
| **Control Statement** | All personal data processing shall be based on valid consent obtained in clear, plain language |
| **Implementation** | Digital consent form with timestamp, purpose selection, acknowledgment |
| **Evidence Required** | Consent record (JSON), timestamp, purpose ID, data principal ID |
| **Test Method** | Verify consent record exists before any data processing |
| **Frequency** | Real-time |
| **Owner** | DPO |

### DPDP-CON-002: Consent Withdrawal
| Attribute | Value |
|-----------|-------|
| **Policy** | Data Protection Policy |
| **Procedure** | Consent Withdrawal Procedure |
| **Control Type** | Detective |
| **DPDP Section** | Section 6(4) |
| **Control Statement** | Data principals shall be able to withdraw consent at any time |
| **Implementation** | One-click withdrawal, immediate processing cessation, audit trail |
| **Evidence Required** | Withdrawal timestamp, reason (optional), confirmation |
| **Test Method** | Attempt withdrawal, verify processing stops |
| **Frequency** | On-demand |
| **Owner** | DPO |

### DPDP-CON-003: Purpose Limitation
| Attribute | Value |
|-----------|-------|
| **Policy** | Data Protection Policy |
| **Procedure** | Purpose Tracking Procedure |
| **Control Type** | Preventive |
| **DPDP Section** | Section 7 |
| **Control Statement** | Personal data shall only be processed for consented purposes |
| **Implementation** | Purpose-consent linking, processing validation |
| **Evidence Required** | Purpose audit log, consent-purpose mapping |
| **Test Method** | Verify all processing has valid consent-purpose link |
| **Frequency** | Real-time |
| **Owner** | DPO |

---

## 2. Breach Notification Controls

### DPDP-BRE-001: 72-Hour Board Notification
| Attribute | Value |
|-----------|-------|
| **Policy** | Breach Response Policy |
| **Procedure** | Breach Notification Procedure |
| **Control Type** | Detective/Corrective |
| **DPDP Section** | Section 8(6) |
| **Control Statement** | Personal data breaches shall be notified to the Board within 72 hours |
| **Implementation** | Breach detection, countdown timer, DPBI notification API |
| **Evidence Required** | Breach report, notification timestamp, acknowledgment |
| **Test Method** | Simulate breach, verify 72-hour timer activates |
| **Frequency** | Event-driven |
| **Owner** | DPO |

### DPDP-BRE-002: Affected Party Notification
| Attribute | Value |
|-----------|-------|
| **Policy** | Breach Response Policy |
| **Procedure** | Affected Party Notification Procedure |
| **Control Type** | Corrective |
| **DPDP Section** | Section 8(6) |
| **Control Statement** | Affected data principals shall be notified of breaches |
| **Implementation** | Multi-channel notification (email, SMS, portal) |
| **Evidence Required** | Notification logs, delivery receipts |
| **Test Method** | Verify notification delivery to sample affected parties |
| **Frequency** | Event-driven |
| **Owner** | DPO |

---

## 3. Data Principal Rights Controls

### DPDP-RIG-001: Access Request Fulfillment
| Attribute | Value |
|-----------|-------|
| **Policy** | Data Principal Rights Policy |
| **Procedure** | Access Request Procedure |
| **Control Type** | Detective |
| **DPDP Section** | Section 11 |
| **Control Statement** | Access requests shall be fulfilled within 30 days |
| **Implementation** | Request portal, data collection, structured export |
| **Evidence Required** | Request ID, response timestamp, data export |
| **Test Method** | Submit access request, verify 30-day compliance |
| **Frequency** | On-demand |
| **Owner** | DPO |

### DPDP-RIG-002: Correction Processing
| Attribute | Value |
|-----------|-------|
| **Policy** | Data Principal Rights Policy |
| **Procedure** | Correction Request Procedure |
| **Control Type** | Corrective |
| **DPDP Section** | Section 12 |
| **Control Statement** | Correction requests shall be processed within 30 days |
| **Implementation** | Correction form, verification, update workflow |
| **Evidence Required** | Request ID, before/after data, update timestamp |
| **Test Method** | Submit correction, verify data updated |
| **Frequency** | On-demand |
| **Owner** | DPO |

### DPDP-RIG-003: Erasure Execution
| Attribute | Value |
|-----------|-------|
| **Policy** | Data Principal Rights Policy |
| **Procedure** | Erasure Request Procedure |
| **Control Type** | Corrective |
| **DPDP Section** | Section 13 |
| **Control Statement** | Erasure requests shall be executed within 30 days |
| **Implementation** | Deletion workflow, verification, certificate |
| **Evidence Required** | Erasure certificate, deletion logs |
| **Test Method** | Submit erasure, verify data removed |
| **Frequency** | On-demand |
| **Owner** | DPO |

---

## 4. Security Controls

### DPDP-SEC-001: Encryption at Rest
| Attribute | Value |
|-----------|-------|
| **Policy** | Information Security Policy |
| **Procedure** | Encryption Procedure |
| **Control Type** | Preventive |
| **DPDP Section** | Section 8(4) |
| **Control Statement** | All personal data at rest shall be encrypted using AES-256 |
| **Implementation** | Database encryption, file encryption |
| **Evidence Required** | Encryption status report, key management logs |
| **Test Method** | Verify encryption enabled, attempt access without key |
| **Frequency** | Continuous |
| **Owner** | Security Officer |

### DPDP-SEC-002: Access Control
| Attribute | Value |
|-----------|-------|
| **Policy** | Access Control Policy |
| **Procedure** | Access Management Procedure |
| **Control Type** | Preventive |
| **DPDP Section** | Section 8(4) |
| **Control Statement** | Access to personal data shall be role-based and logged |
| **Implementation** | RBAC, purpose-based access, audit logging |
| **Evidence Required** | Access logs, role assignments, permission matrix |
| **Test Method** | Attempt unauthorized access, verify denial |
| **Frequency** | Continuous |
| **Owner** | Security Officer |

### DPDP-SEC-003: Audit Trail Integrity
| Attribute | Value |
|-----------|-------|
| **Policy** | Audit Policy |
| **Procedure** | Audit Logging Procedure |
| **Control Type** | Detective |
| **DPDP Section** | Section 8(5) |
| **Control Statement** | All actions on personal data shall be immutably logged |
| **Implementation** | Cryptographic hash chain, tamper detection |
| **Evidence Required** | Audit logs with hashes, integrity verification |
| **Test Method** | Verify hash chain integrity |
| **Frequency** | Continuous |
| **Owner** | Security Officer |

---

## 5. DPIA Controls

### DPDP-DPI-001: Impact Assessment
| Attribute | Value |
|-----------|-------|
| **Policy** | DPIA Policy |
| **Procedure** | DPIA Procedure |
| **Control Type** | Preventive |
| **DPDP Section** | Section 10 |
| **Control Statement** | High-risk processing shall require DPIA before commencement |
| **Implementation** | DPIA questionnaire, risk scoring, approval workflow |
| **Evidence Required** | DPIA report, risk score, approval record |
| **Test Method** | Verify DPIA completed before high-risk processing |
| **Frequency** | Project-based |
| **Owner** | DPO |

### DPDP-DPI-002: Risk Mitigation
| Attribute | Value |
|-----------|-------|
| **Policy** | DPIA Policy |
| **Procedure** | Risk Mitigation Procedure |
| **Control Type** | Preventive |
| **DPDP Section** | Section 10 |
| **Control Statement** | Identified risks shall have documented mitigation measures |
| **Implementation** | Mitigation tracking, implementation verification |
| **Evidence Required** | Mitigation plan, implementation evidence |
| **Test Method** | Verify mitigations implemented |
| **Frequency** | Project-based |
| **Owner** | DPO |

---

## 6. Children's Data Controls

### DPDP-CHI-001: Guardian Consent
| Attribute | Value |
|-----------|-------|
| **Policy** | Children's Data Policy |
| **Procedure** | Guardian Consent Procedure |
| **Control Type** | Preventive |
| **DPDP Section** | Section 16 |
| **Control Statement** | Processing children's data requires verifiable guardian consent |
| **Implementation** | Age verification, guardian verification, consent collection |
| **Evidence Required** | Age verification record, guardian verification, consent |
| **Test Method** | Attempt child data processing without guardian consent |
| **Frequency** | Event-driven |
| **Owner** | DPO |

---

## Control Summary

| Category | Total Controls | Preventive | Detective | Corrective |
|----------|----------------|------------|-----------|------------|
| Consent Management | 3 | 2 | 1 | 0 |
| Breach Notification | 2 | 0 | 1 | 1 |
| Data Principal Rights | 3 | 0 | 1 | 2 |
| Security | 3 | 2 | 1 | 0 |
| DPIA | 2 | 2 | 0 | 0 |
| Children's Data | 1 | 1 | 0 | 0 |
| **TOTAL** | **14** | **7** | **4** | **3** |

---

## Evidence Retention Schedule

| Evidence Type | Retention Period | Storage | Integrity |
|---------------|------------------|---------|-----------|
| Consent Records | 7 years post-withdrawal | Encrypted DB | Hash chain |
| Audit Logs | 10 years | Encrypted + Archive | Hash chain |
| Breach Reports | Permanent | Encrypted + Archive | Digital signature |
| DPIA Documents | 7 years post-project | Document store | Hash |
| Erasure Certificates | 10 years | Encrypted + Archive | Digital signature |
| Access Request Logs | 7 years | Encrypted DB | Hash chain |
