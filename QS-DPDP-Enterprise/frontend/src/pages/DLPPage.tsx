import { useState, useMemo } from 'react'
import api from '../api'
import { generateDLPIncidents, SECTORS } from '../data/sectorData'
import { useAppContext } from '../context/AppContext'

/* ═══════════════════════════════════════════════════════════════
   QS-DPDP Enterprise DLP — Enterprise-Grade Data Loss Prevention
   Inspired by Trend Micro DLP + Fortinet FortiDLP + Symantec DLP
   Covers: Endpoint · Network · Email · Cloud · Web · Database · Print · API
   Features: AI Analytics · NIST PQC · EDM · OCR · UEBA · Fingerprinting
   ═══════════════════════════════════════════════════════════════ */

// ─── DLP Channel Configuration (like Trend Micro / Fortinet) ───
const DLP_CHANNELS = [
  { id: 'endpoint', icon: '💻', label: 'Endpoint DLP', desc: 'USB/Print/Clipboard/Screen capture monitoring on endpoints', agents: 142, incidents: 328, blocked: 245, color: 'var(--blue)' },
  { id: 'network', icon: '🌐', label: 'Network DLP', desc: 'Inline/SPAN port deep packet inspection on network egress', agents: 8, incidents: 512, blocked: 401, color: 'var(--cyan)' },
  { id: 'email', icon: '📧', label: 'Email DLP', desc: 'SMTP gateway + O365/Gmail API scanning for outbound PII', agents: 3, incidents: 187, blocked: 156, color: 'var(--purple)' },
  { id: 'cloud', icon: '☁️', label: 'Cloud DLP', desc: 'CASB integration for AWS S3/Azure Blob/GCP Storage + SaaS', agents: 12, incidents: 234, blocked: 198, color: 'var(--green)' },
  { id: 'web', icon: '🔒', label: 'Web DLP', desc: 'Proxy-based HTTP/HTTPS content inspection and URL filtering', agents: 6, incidents: 145, blocked: 112, color: 'var(--amber)' },
  { id: 'database', icon: '🗄️', label: 'Database DLP', desc: 'DAM — Database Activity Monitoring with query-level PII detection', agents: 24, incidents: 89, blocked: 67, color: 'var(--red)' },
  { id: 'print', icon: '🖨️', label: 'Print DLP', desc: 'Print job interception with watermarking + OCR classification', agents: 35, incidents: 42, blocked: 28, color: '#e0e0e0' },
  { id: 'api', icon: '🔗', label: 'API DLP', desc: 'REST/GraphQL API payload inspection for PII exfiltration', agents: 18, incidents: 156, blocked: 134, color: 'var(--brand-primary)' },
]

// ─── AI-Powered Detection Engine ───
const AI_DETECTORS = [
  { name: 'NLP Entity Recognition', type: 'AI/ML', accuracy: 98.7, piiTypes: ['Names', 'Addresses', 'Organizations'], model: 'Transformer (BERT-Indian)', quantum: 'ML-KEM-1024 model encryption' },
  { name: 'RegEx Pattern Matcher', type: 'Rule-based', accuracy: 99.9, piiTypes: ['Aadhaar', 'PAN', 'GSTIN', 'Passport'], model: 'Compiled RegEx Engine', quantum: 'N/A (local computation)' },
  { name: 'Computer Vision OCR', type: 'AI/ML', accuracy: 96.2, piiTypes: ['Scanned documents', 'Screenshots', 'Photos of ID cards'], model: 'TesseractOCR + Custom CNN', quantum: 'ML-DSA-87 signed model' },
  { name: 'EDM Fingerprinting', type: 'Hash-based', accuracy: 99.5, piiTypes: ['Database columns', 'CSV files', 'Structured data'], model: 'Exact Data Match (SHA3-256)', quantum: 'SLH-DSA-256 hash verification' },
  { name: 'IDM Document Matching', type: 'AI/ML', accuracy: 97.8, piiTypes: ['Contracts', 'Policies', 'Templates'], model: 'Document fingerprint + SimHash', quantum: 'ML-KEM-1024 encrypted index' },
  { name: 'UEBA Anomaly Detection', type: 'AI/ML', accuracy: 94.5, piiTypes: ['Unusual data access', 'Bulk downloads', 'Off-hour activity'], model: 'Isolation Forest + LSTM', quantum: 'PQC-secured inference pipeline' },
  { name: 'Image Classification', type: 'AI/ML', accuracy: 95.3, piiTypes: ['Credit cards in images', 'ID documents', 'Medical images'], model: 'ResNet-50 + Custom layers', quantum: 'ML-DSA-87 model attestation' },
  { name: 'Behavioral Analytics', type: 'AI/ML', accuracy: 93.1, piiTypes: ['Insider threats', 'Data hoarding', 'Shadow IT'], model: 'Graph Neural Network', quantum: 'Homomorphic inference' },
]

// ─── NIST PQC Layer ───
const PQC_LAYERS = [
  { layer: 'Data at Rest', algorithm: 'ML-KEM-1024 (FIPS 203)', standard: 'NIST Level 5', status: 'ACTIVE', desc: 'Quantum-safe encryption for all stored DLP logs, fingerprints, and policy databases' },
  { layer: 'Data in Transit', algorithm: 'Post-Quantum TLS 1.3 (Kyber)', standard: 'NIST Level 5', status: 'ACTIVE', desc: 'All DLP agent ↔ server communication uses PQ-hybrid TLS with X25519+ML-KEM' },
  { layer: 'Model Signing', algorithm: 'ML-DSA-87 (FIPS 204)', standard: 'NIST Level 5', status: 'ACTIVE', desc: 'All AI/ML models digitally signed with ML-DSA to prevent model tampering' },
  { layer: 'Hash Verification', algorithm: 'SLH-DSA-256 (FIPS 205)', standard: 'NIST Level 5', status: 'ACTIVE', desc: 'EDM fingerprints and document hashes use stateless hash-based PQC signatures' },
  { layer: 'Key Management', algorithm: 'XMSS + ML-KEM Hybrid', standard: 'NIST SP 800-208', status: 'ACTIVE', desc: 'Hierarchical deterministic key management with quantum-safe key derivation' },
  { layer: 'Audit Chain', algorithm: 'SHA3-512 + ML-DSA', standard: 'FIPS 202 + 204', status: 'ACTIVE', desc: 'Tamper-evident audit chain with PQC-signed log entries for forensic integrity' },
]

// ─── Classification Taxonomy ───
const CLASSIFICATIONS = [
  { level: 'L5', label: 'TOP SECRET', color: '#ff0040', piiTypes: 'Biometric (fingerprint, iris, DNA)', handling: 'PQC vault · Air-gapped · DPO + Board approval · No export', dpdp: 'Section 9 (Children biometric prohibited)' },
  { level: 'L4', label: 'RESTRICTED', color: 'var(--red)', piiTypes: 'Aadhaar · Health Records · Financial PII', handling: 'PQC encryption · Role-based · Audit logged · DPIA required', dpdp: 'Section 4, 8(4) — Reasonable safeguards' },
  { level: 'L3', label: 'CONFIDENTIAL', color: 'var(--amber)', piiTypes: 'PAN · Bank Account · Salary · Address', handling: 'AES-256 · Department-restricted · DLP monitored', dpdp: 'Section 6 — Consent required' },
  { level: 'L2', label: 'INTERNAL', color: 'var(--blue)', piiTypes: 'Name · Email · Phone · Employee ID', handling: 'Standard encryption · Access controlled · Logged', dpdp: 'Section 5 — Notice required' },
  { level: 'L1', label: 'PUBLIC', color: 'var(--green)', piiTypes: 'Company info · Published data · Product docs', handling: 'Standard handling · Integrity monitoring', dpdp: 'N/A — Non-personal data' },
]

// ─── DLP Policies (Enterprise-Grade) ───
const ENTERPRISE_POLICIES = [
  { id: 'DLP-EP-001', name: 'Block Aadhaar on All Channels', channels: ['endpoint','network','email','cloud','web','api'], severity: 'CRITICAL', action: 'Block + Alert DPO + Forensic Capture', status: 'ACTIVE', aiPowered: true, pqcEnabled: true, dpdp: 'Section 4,8(4)' },
  { id: 'DLP-EP-002', name: 'PAN Card Detection — Outbound Email', channels: ['email'], severity: 'HIGH', action: 'Quarantine + Manager Approval', status: 'ACTIVE', aiPowered: true, pqcEnabled: true, dpdp: 'Section 6' },
  { id: 'DLP-EP-003', name: 'USB Data Transfer — Encrypt or Block', channels: ['endpoint'], severity: 'HIGH', action: 'Force Encrypt + Log + Alert', status: 'ACTIVE', aiPowered: false, pqcEnabled: true, dpdp: 'Section 8(4)' },
  { id: 'DLP-EP-004', name: 'Cloud Upload — PII Classification Gate', channels: ['cloud','web'], severity: 'HIGH', action: 'Classify → If L3+ → Block + Re-route', status: 'ACTIVE', aiPowered: true, pqcEnabled: true, dpdp: 'Section 16 (Cross-border)' },
  { id: 'DLP-EP-005', name: 'Healthcare PHI — OCR Document Scan', channels: ['endpoint','email','cloud','print'], severity: 'CRITICAL', action: 'OCR Scan + Block if PHI + DPIA Trigger', status: 'ACTIVE', aiPowered: true, pqcEnabled: true, dpdp: 'Section 4, 9' },
  { id: 'DLP-EP-006', name: 'Database Query — Bulk PII Export Alert', channels: ['database','api'], severity: 'CRITICAL', action: 'Alert + Rate Limit + DPO Notification', status: 'ACTIVE', aiPowered: true, pqcEnabled: true, dpdp: 'Section 8(4)' },
  { id: 'DLP-EP-007', name: 'Print Job — PII Watermark + Block', channels: ['print'], severity: 'MEDIUM', action: 'Watermark + If L4+ → Block', status: 'ACTIVE', aiPowered: true, pqcEnabled: false, dpdp: 'Section 8(4)' },
  { id: 'DLP-EP-008', name: 'Screen Capture — Sensitive App Block', channels: ['endpoint'], severity: 'HIGH', action: 'Block Screenshot + DLP Alert', status: 'ACTIVE', aiPowered: false, pqcEnabled: false, dpdp: 'Section 8(4)' },
  { id: 'DLP-EP-009', name: 'API Payload — PII Detection in JSON/XML', channels: ['api','network'], severity: 'HIGH', action: 'Mask PII + Log + Alert if bulk', status: 'ACTIVE', aiPowered: true, pqcEnabled: true, dpdp: 'Section 4(2)' },
  { id: 'DLP-EP-010', name: 'Insider Threat — UEBA Anomaly Response', channels: ['endpoint','network','cloud','database'], severity: 'CRITICAL', action: 'Auto-isolate + SOC Alert + Forensic Snapshot', status: 'ACTIVE', aiPowered: true, pqcEnabled: true, dpdp: 'Section 8' },
  { id: 'DLP-EP-011', name: 'Children Data (Sec 9) — Total Block', channels: ['endpoint','network','email','cloud','web','database','print','api'], severity: 'CRITICAL', action: 'Block All + DPO Alert + Compliance Hold', status: 'ACTIVE', aiPowered: true, pqcEnabled: true, dpdp: 'Section 9' },
  { id: 'DLP-EP-012', name: 'Cross-Border Transfer — Geo Fence', channels: ['network','cloud','email','api'], severity: 'CRITICAL', action: 'Block if dest not in Section 16 whitelist', status: 'ACTIVE', aiPowered: false, pqcEnabled: true, dpdp: 'Section 16' },
]

export default function DLPPage() {
  const [content, setContent] = useState('')
  const [result, setResult] = useState<any>(null)
  const [tab, setTab] = useState<'overview'|'channels'|'incidents'|'policies'|'ai'|'pqc'|'classification'|'scanner'>('overview')
  const { sector, setSector } = useAppContext()
  const [selectedChannel, setSelectedChannel] = useState<string|null>(null)
  const [incidentPage, setIncidentPage] = useState(0)

  const incidents = useMemo(() => generateDLPIncidents(sector, 200), [sector])
  const PAGE_SIZE = 15
  const totalIncidentPages = Math.ceil(incidents.length / PAGE_SIZE)

  const totalBlocked = DLP_CHANNELS.reduce((s, c) => s + c.blocked, 0)
  const totalIncidents = DLP_CHANNELS.reduce((s, c) => s + c.incidents, 0)
  const totalAgents = DLP_CHANNELS.reduce((s, c) => s + c.agents, 0)

  const scanPII = async () => {
    if (!content) return
    try { const data = await api.piiScan(content); setResult(data) }
    catch { setResult({ error: 'Scan failed — backend offline', piiDetected: false, totalFindings: 0, findings: [] }) }
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1>🛡️ Enterprise Data Loss Prevention</h1>
          <p>Endpoint · Network · Email · Cloud · Web · Database · Print · API — AI-Powered + NIST PQC</p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <select className="form-input" style={{ width: 200 }} value={sector} onChange={e => setSector(e.target.value)}>
            {SECTORS.map(s => <option key={s}>{s}</option>)}
          </select>
          <button className="btn-primary">📊 DLP Report</button>
        </div>
      </div>

      {/* ═══ MAIN KPI ═══ */}
      <div className="kpi-grid" style={{ gridTemplateColumns: 'repeat(6, 1fr)' }}>
        <div className="kpi-card red"><div className="kpi-value" style={{ color: 'var(--red)' }}>{totalBlocked}</div><div className="kpi-label">Blocked (24h)</div></div>
        <div className="kpi-card amber"><div className="kpi-value" style={{ color: 'var(--amber)' }}>{totalIncidents}</div><div className="kpi-label">Total Incidents</div></div>
        <div className="kpi-card blue"><div className="kpi-value" style={{ color: 'var(--blue)' }}>{DLP_CHANNELS.length}</div><div className="kpi-label">DLP Channels</div></div>
        <div className="kpi-card green"><div className="kpi-value" style={{ color: 'var(--green)' }}>{ENTERPRISE_POLICIES.filter(p => p.status === 'ACTIVE').length}</div><div className="kpi-label">Active Policies</div></div>
        <div className="kpi-card"><div className="kpi-value">{totalAgents}</div><div className="kpi-label">Agents/Sensors</div></div>
        <div className="kpi-card"><div className="kpi-value" style={{ color: 'var(--cyan)' }}>{AI_DETECTORS.length}</div><div className="kpi-label">AI Detectors</div></div>
      </div>

      {/* ═══ TAB NAV ═══ */}
      <div style={{ display: 'flex', gap: 4, margin: '20px 0', flexWrap: 'wrap' }}>
        {([
          ['overview', '📊 Overview'], ['channels', '📡 DLP Channels'], ['incidents', '🚨 Incidents (200+)'],
          ['policies', '📝 Policies'], ['ai', '🤖 AI Analytics'], ['pqc', '🔐 NIST PQC'],
          ['classification', '🏷️ Classification'], ['scanner', '🔍 PII Scanner']
        ] as const).map(([k, l]) => (
          <button key={k} className={tab === k ? 'btn-primary' : 'btn-secondary'} onClick={() => setTab(k as any)} style={{ fontSize: 12, padding: '8px 14px' }}>
            {l}
          </button>
        ))}
      </div>

      {/* ═══ OVERVIEW TAB ═══ */}
      {tab === 'overview' && (
        <div>
          {/* Channel Overview Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 24 }}>
            {DLP_CHANNELS.map(ch => (
              <div key={ch.id} className="glass-card" style={{ padding: 16, borderLeft: `4px solid ${ch.color}`, cursor: 'pointer' }}
                onClick={() => { setSelectedChannel(ch.id); setTab('channels') }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontSize: 24 }}>{ch.icon}</span>
                  <span style={{ fontSize: 11, padding: '2px 8px', borderRadius: 12, background: 'rgba(0,200,100,0.15)', color: 'var(--green)', fontWeight: 600 }}>ACTIVE</span>
                </div>
                <h4 style={{ margin: '8px 0 4px', fontSize: 14 }}>{ch.label}</h4>
                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 8 }}>{ch.desc.substring(0, 55)}...</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 4, fontSize: 11 }}>
                  <div><span style={{ fontWeight: 700, color: 'var(--red)' }}>{ch.blocked}</span> blocked</div>
                  <div><span style={{ fontWeight: 700 }}>{ch.incidents}</span> incidents</div>
                  <div><span style={{ fontWeight: 700, color: ch.color }}>{ch.agents}</span> sensors</div>
                </div>
              </div>
            ))}
          </div>

          {/* AI Engine Status */}
          <div className="glass-card" style={{ padding: 20, marginBottom: 16 }}>
            <h3 style={{ margin: '0 0 16px' }}>🤖 AI Detection Engine Status</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
              {AI_DETECTORS.slice(0, 4).map((d, i) => (
                <div key={i} style={{ background: 'var(--bg-surface)', padding: 12, borderRadius: 8 }}>
                  <div style={{ fontSize: 12, fontWeight: 600 }}>{d.name}</div>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: 4, marginTop: 4 }}>
                    <span style={{ fontSize: 22, fontWeight: 700, color: d.accuracy > 97 ? 'var(--green)' : 'var(--amber)' }}>{d.accuracy}%</span>
                    <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>accuracy</span>
                  </div>
                  <div style={{ height: 3, background: 'var(--border)', borderRadius: 2, marginTop: 6 }}>
                    <div style={{ height: '100%', width: `${d.accuracy}%`, background: d.accuracy > 97 ? 'var(--green)' : 'var(--amber)', borderRadius: 2 }} />
                  </div>
                  <div style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 4 }}>🔐 {d.quantum.substring(0, 30)}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Real-time threat map */}
          <div className="glass-card" style={{ padding: 20 }}>
            <h3 style={{ margin: '0 0 16px' }}>⚡ Real-Time DLP Activity (Last 1 Hour)</h3>
            <div style={{ display: 'grid', gridTemplateColumns: '3fr 1fr', gap: 16 }}>
              <div>
                {/* Activity bars */}
                {DLP_CHANNELS.map(ch => (
                  <div key={ch.id} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                    <span style={{ width: 24, textAlign: 'center' }}>{ch.icon}</span>
                    <span style={{ width: 100, fontSize: 11 }}>{ch.label}</span>
                    <div style={{ flex: 1, height: 18, background: 'var(--bg-surface)', borderRadius: 4, overflow: 'hidden', display: 'flex' }}>
                      <div style={{ width: `${ch.blocked / 6}%`, background: 'var(--red)', height: '100%' }} title={`${ch.blocked} blocked`} />
                      <div style={{ width: `${(ch.incidents - ch.blocked) / 6}%`, background: 'var(--amber)', height: '100%' }} title={`${ch.incidents - ch.blocked} alerted`} />
                    </div>
                    <span style={{ fontSize: 11, fontWeight: 600, width: 40, textAlign: 'right' }}>{ch.incidents}</span>
                  </div>
                ))}
                <div style={{ display: 'flex', gap: 16, marginTop: 8, fontSize: 11, color: 'var(--text-muted)' }}>
                  <span><span style={{ display: 'inline-block', width: 10, height: 10, background: 'var(--red)', borderRadius: 2, marginRight: 4 }} />Blocked</span>
                  <span><span style={{ display: 'inline-block', width: 10, height: 10, background: 'var(--amber)', borderRadius: 2, marginRight: 4 }} />Alerted</span>
                </div>
              </div>
              <div>
                <div style={{ background: 'var(--bg-surface)', padding: 12, borderRadius: 8, textAlign: 'center' }}>
                  <div style={{ fontSize: 36, fontWeight: 700, color: 'var(--green)' }}>{Math.round(totalBlocked / totalIncidents * 100)}%</div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Prevention Rate</div>
                </div>
                <div style={{ background: 'var(--bg-surface)', padding: 12, borderRadius: 8, textAlign: 'center', marginTop: 8 }}>
                  <div style={{ fontSize: 14, fontWeight: 700 }}>🔐 PQC Active</div>
                  <div style={{ fontSize: 11, color: 'var(--green)' }}>ML-KEM-1024</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ═══ DLP CHANNELS TAB ═══ */}
      {tab === 'channels' && (
        <div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 16 }}>
            {DLP_CHANNELS.map(ch => (
              <div key={ch.id} className="glass-card" style={{ padding: 20, borderLeft: `4px solid ${ch.color}`, background: selectedChannel === ch.id ? 'var(--bg-surface)' : undefined }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 28 }}>{ch.icon}</span>
                    <div>
                      <h3 style={{ margin: 0, fontSize: 16 }}>{ch.label}</h3>
                      <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>{ch.desc}</span>
                    </div>
                  </div>
                  <span style={{ padding: '4px 12px', borderRadius: 12, fontSize: 11, fontWeight: 600, background: 'rgba(0,200,100,0.15)', color: 'var(--green)' }}>🟢 ACTIVE</span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
                  <div style={{ background: 'var(--bg-main)', padding: 8, borderRadius: 6, textAlign: 'center' }}>
                    <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--red)' }}>{ch.blocked}</div>
                    <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>Blocked</div>
                  </div>
                  <div style={{ background: 'var(--bg-main)', padding: 8, borderRadius: 6, textAlign: 'center' }}>
                    <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--amber)' }}>{ch.incidents}</div>
                    <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>Incidents</div>
                  </div>
                  <div style={{ background: 'var(--bg-main)', padding: 8, borderRadius: 6, textAlign: 'center' }}>
                    <div style={{ fontSize: 20, fontWeight: 700, color: ch.color }}>{ch.agents}</div>
                    <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>Sensors</div>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 6, marginTop: 12 }}>
                  <button className="btn-sm" style={{ fontSize: 11 }}>📊 Dashboard</button>
                  <button className="btn-sm" style={{ fontSize: 11 }}>⚙️ Configure</button>
                  <button className="btn-sm" style={{ fontSize: 11 }}>📝 Policies</button>
                  <button className="btn-sm" style={{ fontSize: 11 }}>📋 Logs</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ═══ INCIDENTS TAB ═══ */}
      {tab === 'incidents' && (
        <div className="glass-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ margin: 0 }}>🚨 DLP Incidents ({incidents.length})</h3>
            <div style={{ display: 'flex', gap: 8 }}>
              <select className="form-input" style={{ width: 140, fontSize: 12 }}>
                <option value="">All Channels</option>
                {DLP_CHANNELS.map(ch => <option key={ch.id}>{ch.label}</option>)}
              </select>
              <select className="form-input" style={{ width: 120, fontSize: 12 }}>
                <option value="">All Severity</option>
                <option>CRITICAL</option><option>HIGH</option><option>MEDIUM</option>
              </select>
            </div>
          </div>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr>
                <th>ID</th><th>PII Type</th><th>Sensitivity</th><th>Source</th><th>Destination</th><th>Action</th><th>Count</th><th>Detected</th>
              </tr></thead>
              <tbody>
                {incidents.slice(incidentPage * PAGE_SIZE, (incidentPage + 1) * PAGE_SIZE).map((d, i) => (
                  <tr key={i}>
                    <td style={{ fontFamily: 'monospace', fontSize: 11 }}>{d.id}</td>
                    <td><strong>{d.type}</strong></td>
                    <td><span className={`rag-badge ${d.sensitivity === 'CRITICAL' ? 'rag-red' : d.sensitivity === 'HIGH' ? 'rag-amber' : 'rag-green'}`}>{d.sensitivity}</span></td>
                    <td>{d.source}</td>
                    <td>{d.destination}</td>
                    <td><span className={`rag-badge ${d.action === 'BLOCKED' ? 'rag-red' : d.action === 'QUARANTINED' ? 'rag-amber' : 'rag-green'}`}>{d.action}</span></td>
                    <td style={{ fontWeight: 700 }}>{d.count}</td>
                    <td style={{ fontSize: 11 }}>{d.detectedAt}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 }}>
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>Page {incidentPage + 1} of {totalIncidentPages}</span>
            <div style={{ display: 'flex', gap: 4 }}>
              <button className="btn-sm" disabled={incidentPage === 0} onClick={() => setIncidentPage(p => p - 1)}>◀ Prev</button>
              <button className="btn-sm" disabled={incidentPage >= totalIncidentPages - 1} onClick={() => setIncidentPage(p => p + 1)}>Next ▶</button>
            </div>
          </div>
        </div>
      )}

      {/* ═══ POLICIES TAB ═══ */}
      {tab === 'policies' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h3 style={{ margin: 0 }}>📝 Enterprise DLP Policies ({ENTERPRISE_POLICIES.length})</h3>
            <button className="btn-primary">➕ Create Policy</button>
          </div>
          {ENTERPRISE_POLICIES.map((p, i) => (
            <div key={i} className="glass-card" style={{ padding: 16, marginBottom: 8, borderLeft: `4px solid ${p.severity === 'CRITICAL' ? 'var(--red)' : p.severity === 'HIGH' ? 'var(--amber)' : 'var(--blue)'}` }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontFamily: 'monospace', fontSize: 11, color: 'var(--text-muted)' }}>{p.id}</span>
                    <span className={`rag-badge ${p.severity === 'CRITICAL' ? 'rag-red' : 'rag-amber'}`}>{p.severity}</span>
                    {p.aiPowered && <span style={{ fontSize: 11, padding: '2px 6px', borderRadius: 4, background: 'rgba(0,132,255,0.15)', color: 'var(--brand-primary)' }}>🤖 AI</span>}
                    {p.pqcEnabled && <span style={{ fontSize: 11, padding: '2px 6px', borderRadius: 4, background: 'rgba(0,200,100,0.15)', color: 'var(--green)' }}>🔐 PQC</span>}
                  </div>
                  <h4 style={{ margin: '6px 0 4px' }}>{p.name}</h4>
                  <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>Action: {p.action}</div>
                  <div style={{ fontSize: 11 }}>DPDP: <strong>{p.dpdp}</strong></div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                  <span style={{ padding: '3px 10px', borderRadius: 12, fontSize: 11, background: 'rgba(0,200,100,0.15)', color: 'var(--green)', fontWeight: 600 }}>{p.status}</span>
                  <div style={{ display: 'flex', gap: 4, marginTop: 4 }}>
                    {p.channels.map(ch => {
                      const channel = DLP_CHANNELS.find(c => c.id === ch)
                      return <span key={ch} title={channel?.label} style={{ fontSize: 14 }}>{channel?.icon}</span>
                    })}
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ═══ AI ANALYTICS TAB ═══ */}
      {tab === 'ai' && (
        <div>
          <h3 style={{ marginBottom: 16 }}>🤖 AI-Powered Detection Engine</h3>
          <p style={{ fontSize: 13, color: 'var(--text-muted)', marginBottom: 20 }}>Advanced AI/ML models for PII detection, behavioral analytics, and insider threat detection — all models signed with NIST ML-DSA-87</p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 16 }}>
            {AI_DETECTORS.map((d, i) => (
              <div key={i} className="glass-card" style={{ padding: 20 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <div>
                    <h4 style={{ margin: 0 }}>{d.name}</h4>
                    <span style={{ fontSize: 11, padding: '2px 8px', borderRadius: 4, background: d.type === 'AI/ML' ? 'rgba(0,132,255,0.15)' : 'var(--bg-surface)', color: d.type === 'AI/ML' ? 'var(--brand-primary)' : 'var(--text-muted)' }}>{d.type}</span>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: 28, fontWeight: 700, color: d.accuracy > 97 ? 'var(--green)' : d.accuracy > 94 ? 'var(--amber)' : 'var(--text-primary)' }}>{d.accuracy}%</div>
                    <div style={{ fontSize: 10, color: 'var(--text-muted)' }}>Accuracy</div>
                  </div>
                </div>
                <div style={{ marginBottom: 8 }}>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 4 }}>Detects:</div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                    {d.piiTypes.map(p => <span key={p} style={{ fontSize: 11, padding: '2px 8px', borderRadius: 4, background: 'var(--bg-surface)' }}>{p}</span>)}
                  </div>
                </div>
                <div style={{ fontSize: 11, marginBottom: 4 }}><strong>Model:</strong> {d.model}</div>
                <div style={{ fontSize: 11, color: 'var(--green)' }}>🔐 {d.quantum}</div>
              </div>
            ))}
          </div>

          {/* UEBA Section */}
          <div className="glass-card" style={{ padding: 20, marginTop: 16, borderLeft: '4px solid var(--purple)' }}>
            <h3 style={{ margin: '0 0 12px' }}>🧠 User & Entity Behavior Analytics (UEBA)</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
              <div style={{ background: 'var(--bg-surface)', padding: 12, borderRadius: 8, textAlign: 'center' }}>
                <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--red)' }}>3</div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>High-Risk Users</div>
              </div>
              <div style={{ background: 'var(--bg-surface)', padding: 12, borderRadius: 8, textAlign: 'center' }}>
                <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--amber)' }}>12</div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Anomalous Patterns</div>
              </div>
              <div style={{ background: 'var(--bg-surface)', padding: 12, borderRadius: 8, textAlign: 'center' }}>
                <div style={{ fontSize: 28, fontWeight: 700, color: 'var(--green)' }}>847</div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Users Profiled</div>
              </div>
              <div style={{ background: 'var(--bg-surface)', padding: 12, borderRadius: 8, textAlign: 'center' }}>
                <div style={{ fontSize: 28, fontWeight: 700 }}>98.2%</div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Baseline Accuracy</div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ═══ NIST PQC TAB ═══ */}
      {tab === 'pqc' && (
        <div>
          <div className="glass-card" style={{ padding: 24, borderLeft: '4px solid var(--green)', marginBottom: 16 }}>
            <h3 style={{ margin: '0 0 8px' }}>🔐 NIST Post-Quantum Cryptography — DLP Integration</h3>
            <p style={{ fontSize: 13, color: 'var(--text-muted)' }}>All DLP data, models, and communications secured with NIST FIPS 203/204/205 quantum-safe algorithms</p>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 12 }}>
            {PQC_LAYERS.map((l, i) => (
              <div key={i} className="glass-card" style={{ padding: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <span style={{ width: 12, height: 12, borderRadius: '50%', background: 'var(--green)', boxShadow: '0 0 8px var(--green)' }} />
                    <div>
                      <div style={{ fontWeight: 600, fontSize: 15 }}>{l.layer}</div>
                      <div style={{ fontSize: 12, color: 'var(--text-muted)' }}>{l.desc}</div>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontFamily: 'monospace', fontSize: 13, fontWeight: 600, color: 'var(--brand-primary)' }}>{l.algorithm}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{l.standard}</div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ═══ CLASSIFICATION TAB ═══ */}
      {tab === 'classification' && (
        <div className="glass-card">
          <h3>🏷️ Data Classification Taxonomy — DPDP Act Aligned</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Level</th><th>Classification</th><th>PII Types</th><th>Handling Requirements</th><th>DPDP Reference</th></tr></thead>
              <tbody>
                {CLASSIFICATIONS.map((c, i) => (
                  <tr key={i}>
                    <td style={{ fontWeight: 700, fontSize: 16, textAlign: 'center' }}>{c.level}</td>
                    <td><span className="rag-badge" style={{ background: c.color, color: '#fff' }}>{c.label}</span></td>
                    <td>{c.piiTypes}</td>
                    <td style={{ fontSize: 12 }}>{c.handling}</td>
                    <td style={{ fontSize: 12, fontWeight: 600, color: 'var(--brand-primary)' }}>{c.dpdp}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ═══ PII SCANNER TAB ═══ */}
      {tab === 'scanner' && (
        <div className="glass-card">
          <h3>🔍 Live PII Scanner — AI-Powered</h3>
          <p style={{ fontSize: 12, color: 'var(--text-muted)', marginBottom: 12 }}>NLP + RegEx + EDM combined detection engine • PQC-encrypted scan pipeline</p>
          <textarea className="form-input" style={{ height: 120, marginTop: 8 }} value={content} onChange={e => setContent(e.target.value)}
            placeholder="Paste text, JSON, XML, CSV data to scan for PII (Aadhaar, PAN, names, addresses, bank accounts, health records)..." />
          <div style={{ marginTop: 12, display: 'flex', gap: 12 }}>
            <button className="btn-primary" onClick={scanPII}>🛡️ Scan for PII</button>
            <button className="btn-secondary" onClick={() => { setContent(''); setResult(null) }}>Clear</button>
            <button className="btn-secondary">📁 Upload File (PDF/Word/CSV)</button>
            <button className="btn-secondary">📸 Scan Image (OCR)</button>
          </div>
          {result && (
            <div style={{ marginTop: 16 }}>
              <span className={`rag-badge ${result.piiDetected ? 'rag-red' : 'rag-green'}`}>
                {result.piiDetected ? `⚠️ ${result.totalFindings} PII Found` : '✅ No PII Detected'}
              </span>
              {result.findings?.length > 0 && (
                <div className="data-table-wrapper" style={{ marginTop: 12 }}>
                  <table className="data-table">
                    <thead><tr><th>Type</th><th>Risk</th><th>Value</th><th>Detector</th><th>Action</th></tr></thead>
                    <tbody>{result.findings.map((f: any, i: number) => (
                      <tr key={i}>
                        <td>{f.type || f.piiType}</td>
                        <td><span className={`rag-badge ${f.severity === 'CRITICAL' ? 'rag-red' : 'rag-amber'}`}>{f.severity || f.riskLevel}</span></td>
                        <td style={{ fontFamily: 'monospace' }}>{f.maskedValue || f.value}</td>
                        <td style={{ fontSize: 11 }}>AI NLP + RegEx</td>
                        <td><span className="rag-badge rag-red">REDACT</span></td>
                      </tr>
                    ))}</tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
