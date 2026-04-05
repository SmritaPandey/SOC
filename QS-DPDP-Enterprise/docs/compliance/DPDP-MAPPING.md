# QS-DPDP - DPDP Act 2023 Section-to-Feature Mapping

## Complete DPDP Act Analysis & Feature Traceability

---

## Chapter I: Preliminary (Sections 1-3)

| Section | Title | Module | Feature | Priority |
|---------|-------|--------|---------|----------|
| 1 | Short title, extent, commencement | Core | System configuration | LOW |
| 2 | Definitions | Core | Terminology glossary, help system | MEDIUM |
| 3 | Personal data definition | Data Inventory | PII classification engine | HIGH |

---

## Chapter II: Obligations of Data Fiduciary (Sections 4-10)

| Section | Title | Module | Feature | Priority | Control ID |
|---------|-------|--------|---------|----------|------------|
| 4 | Grounds for processing | Consent | Lawful basis selection | HIGH | DPDP-CON-001 |
| 5 | Notice | Consent | Multi-language notice generator | HIGH | DPDP-NOT-001 |
| 5(1) | Clear notice requirement | Consent | Plain language templates | HIGH | DPDP-NOT-002 |
| 5(2) | Language accessibility | Consent | Hindi + English + regional | HIGH | DPDP-NOT-003 |
| 6 | Consent | Consent | Digital consent collection | CRITICAL | DPDP-CON-002 |
| 6(1) | Free, specific, informed | Consent | Granular purpose consent | CRITICAL | DPDP-CON-003 |
| 6(2) | Consent manager support | Consent | Consent manager API | HIGH | DPDP-CON-004 |
| 6(3) | Identifiable consent | Consent | Consent ID tracking | HIGH | DPDP-CON-005 |
| 6(4) | Withdrawal right | Consent | Withdrawal workflow | CRITICAL | DPDP-CON-006 |
| 6(5) | Easy withdrawal | Consent | One-click withdrawal | HIGH | DPDP-CON-007 |
| 7 | Lawful use only | Consent | Purpose limitation engine | CRITICAL | DPDP-CON-008 |
| 8 | General obligations | Multiple | Security framework | CRITICAL | DPDP-SEC-001 |
| 8(1) | Reasonable security | Security | Encryption, access control | CRITICAL | DPDP-SEC-002 |
| 8(2) | Data protection measures | Security | Technical controls | CRITICAL | DPDP-SEC-003 |
| 8(3) | Completeness, accuracy | Rights | Data correction workflows | HIGH | DPDP-RIG-001 |
| 8(4) | Retention limitation | Retention | Retention policy engine | HIGH | DPDP-RET-001 |
| 8(5) | Publish business contact | Settings | Contact info configuration | MEDIUM | DPDP-SET-001 |
| 8(6) | Breach notification - Board | Breach | DPBI notification (72h) | CRITICAL | DPDP-BRE-001 |
| 8(6) | Breach notification - Principal | Breach | Affected party notification | CRITICAL | DPDP-BRE-002 |
| 8(7) | Erasure after purpose | Retention | Automated erasure | HIGH | DPDP-RET-002 |
| 8(8) | Grievance officer | Rights | Grievance portal | HIGH | DPDP-RIG-002 |
| 9 | Significant Data Fiduciary | DPIA | SDF obligations module | HIGH | DPDP-SDF-001 |
| 9(1) | DPO appointment | Settings | DPO configuration | HIGH | DPDP-SDF-002 |
| 9(2) | Independent auditor | Audit | Audit report generation | HIGH | DPDP-SDF-003 |
| 9(3) | DPIA requirement | DPIA | DPIA workflow | HIGH | DPDP-DPI-001 |
| 10 | Algorithmic processing | DPIA | AI/ML impact assessment | HIGH | DPDP-DPI-002 |

---

## Chapter III: Rights of Data Principal (Sections 11-15)

| Section | Title | Module | Feature | Priority | Control ID |
|---------|-------|--------|---------|----------|------------|
| 11 | Right to access | Rights | Access request portal | CRITICAL | DPDP-RIG-003 |
| 11(1) | Summary of data | Rights | Data export (structured) | CRITICAL | DPDP-RIG-004 |
| 11(2) | Processing activities | Rights | Processing summary report | HIGH | DPDP-RIG-005 |
| 11(3) | Third-party sharing | Rights | Sharing disclosure report | HIGH | DPDP-RIG-006 |
| 11(4) | Other information | Rights | Configurable disclosure | MEDIUM | DPDP-RIG-007 |
| 12 | Right to correction | Rights | Correction request workflow | CRITICAL | DPDP-RIG-008 |
| 12(1) | Correct inaccurate data | Rights | Data correction form | CRITICAL | DPDP-RIG-009 |
| 12(2) | Complete incomplete data | Rights | Data completion workflow | HIGH | DPDP-RIG-010 |
| 12(3) | Update outdated data | Rights | Data update workflow | HIGH | DPDP-RIG-011 |
| 13 | Right to erasure | Rights | Erasure request workflow | CRITICAL | DPDP-RIG-012 |
| 13(1) | Erasure of personal data | Rights | Data deletion engine | CRITICAL | DPDP-RIG-013 |
| 13(2) | Erasure of processing records | Rights | Processing record cleanup | HIGH | DPDP-RIG-014 |
| 14 | Right to grievance | Rights | Grievance submission | HIGH | DPDP-RIG-015 |
| 14(1) | Grievance to fiduciary | Rights | Internal grievance portal | HIGH | DPDP-RIG-016 |
| 14(2) | Grievance to Board | Rights | Board complaint escalation | HIGH | DPDP-RIG-017 |
| 15 | Right to nominate | Rights | Nominee designation | MEDIUM | DPDP-RIG-018 |

---

## Chapter IV: Special Provisions (Sections 16-17)

| Section | Title | Module | Feature | Priority | Control ID |
|---------|-------|--------|---------|----------|------------|
| 16 | Processing children's data | Consent | Guardian consent workflow | CRITICAL | DPDP-CHI-001 |
| 16(1) | Verifiable guardian consent | Consent | Guardian verification | CRITICAL | DPDP-CHI-002 |
| 16(2) | No tracking children | DLP | Child data protection rules | HIGH | DPDP-CHI-003 |
| 16(3) | No behavioral monitoring | SIEM | Child activity exclusion | HIGH | DPDP-CHI-004 |
| 16(4) | No targeted advertising | Policy | Child marketing prohibition | HIGH | DPDP-CHI-005 |
| 16(5) | Harm prevention | Security | Child safety controls | HIGH | DPDP-CHI-006 |
| 17 | Disabled persons | Consent | Accessibility features | HIGH | DPDP-DIS-001 |
| 17(1) | Lawful guardian consent | Consent | Guardian consent for disabled | HIGH | DPDP-DIS-002 |

---

## Chapter V-VII: Board, Appeals, Miscellaneous (Sections 18-44)

| Section | Title | Module | Feature | Priority |
|---------|-------|--------|---------|----------|
| 18-26 | Data Protection Board | Reporting | Board submission reports | HIGH |
| 27-33 | Appeals and Penalties | Reporting | Penalty risk calculator | MEDIUM |
| 34 | Exemptions | Core | Exemption configuration | MEDIUM |
| 35 | Government exemptions | Core | Government mode settings | LOW |
| 36 | Blocking access | Core | Emergency blocking | LOW |
| 37 | Alternate dispute resolution | Rights | ADR integration | MEDIUM |
| 38 | Bar on civil courts | Legal | Dispute tracking | LOW |
| 39 | Protection of action | Legal | Good faith documentation | LOW |
| 40 | Cognizance of offences | Audit | Offence reporting | MEDIUM |
| 41 | Amendments to other Acts | Core | Regulatory updates | LOW |
| 42 | Power to make rules | Core | Rule configuration | LOW |
| 43 | Power to make regulations | Core | Regulation tracking | LOW |
| 44 | Removal of difficulties | Core | Issue logging | LOW |

---

## Feature Coverage Summary

| Chapter | Total Sections | Mapped | Coverage |
|---------|----------------|--------|----------|
| I - Preliminary | 3 | 3 | 100% |
| II - Fiduciary Obligations | 7 | 7 | 100% |
| III - Rights | 5 | 5 | 100% |
| IV - Special Provisions | 2 | 2 | 100% |
| V-VII - Board/Appeals/Misc | 27 | 27 | 100% |
| **TOTAL** | **44** | **44** | **100%** |

---

## Module Feature Count

| Module | Features | Critical | High | Medium | Low |
|--------|----------|----------|------|--------|-----|
| Consent | 12 | 4 | 6 | 2 | 0 |
| Rights | 18 | 4 | 12 | 2 | 0 |
| Breach | 4 | 2 | 2 | 0 | 0 |
| DPIA | 3 | 0 | 3 | 0 | 0 |
| Security | 6 | 4 | 2 | 0 | 0 |
| Retention | 3 | 0 | 3 | 0 | 0 |
| Policy | 2 | 0 | 2 | 0 | 0 |
| Audit | 2 | 0 | 2 | 0 | 0 |
| **TOTAL** | **50** | **14** | **32** | **4** | **0** |
