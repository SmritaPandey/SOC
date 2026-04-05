import { useEffect, useState } from 'react'
import api from '../api'

export default function GovernancePage() {
  const [policyStats, setPolicyStats] = useState<any>(null)
  const [dpiaStats, setDpiaStats] = useState<any>(null)
  const [rightsStats, setRightsStats] = useState<any>(null)

  useEffect(() => {
    api.policyStats().then(setPolicyStats).catch(() => {})
    api.dpiaStats().then(setDpiaStats).catch(() => {})
    api.rightsStats().then(setRightsStats).catch(() => {})
  }, [])

  return (
    <div className="animate-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">Governance & Compliance</h1>
          <p className="page-subtitle">Policy Engine • DPIA • Rights Management • Gap Analysis</p>
        </div>
      </div>

      {/* Policy Engine */}
      <div className="card" style={{ marginBottom: 24 }}>
        <div className="card-header">
          <h3 className="card-title">📋 Policy Engine</h3>
          <button className="btn btn-primary" style={{ padding: '8px 16px', fontSize: 12 }}>➕ New Policy</button>
        </div>
        <div className="kpi-grid">
          <div className="kpi-card green"><div className="kpi-label">Active Policies</div><div className="kpi-value">{policyStats?.activePolicies ?? '—'}</div></div>
          <div className="kpi-card amber"><div className="kpi-label">Review Required</div><div className="kpi-value">{policyStats?.reviewRequired ?? '—'}</div></div>
          <div className="kpi-card blue"><div className="kpi-label">Total Policies</div><div className="kpi-value">{policyStats?.totalPolicies ?? '—'}</div></div>
        </div>
      </div>

      {/* DPIA */}
      <div className="card" style={{ marginBottom: 24 }}>
        <div className="card-header">
          <h3 className="card-title">📊 Data Protection Impact Assessment</h3>
          <button className="btn btn-primary" style={{ padding: '8px 16px', fontSize: 12 }}>➕ New DPIA</button>
        </div>
        <div className="kpi-grid">
          <div className="kpi-card green"><div className="kpi-label">Completed DPIAs</div><div className="kpi-value">{dpiaStats?.completed ?? '—'}</div></div>
          <div className="kpi-card amber"><div className="kpi-label">In Progress</div><div className="kpi-value">{dpiaStats?.inProgress ?? '—'}</div></div>
          <div className="kpi-card red"><div className="kpi-label">High Risk</div><div className="kpi-value">{dpiaStats?.highRisk ?? '—'}</div></div>
        </div>
      </div>

      {/* Rights Management */}
      <div className="card" style={{ marginBottom: 24 }}>
        <div className="card-header">
          <h3 className="card-title">⚖️ Data Principal Rights (Section 11-14)</h3>
        </div>
        <div className="kpi-grid">
          <div className="kpi-card blue"><div className="kpi-label">Total Requests</div><div className="kpi-value">{rightsStats?.totalRequests ?? '—'}</div></div>
          <div className="kpi-card amber"><div className="kpi-label">Pending</div><div className="kpi-value">{rightsStats?.pendingRequests ?? '—'}</div></div>
          <div className="kpi-card red"><div className="kpi-label">Overdue</div><div className="kpi-value">{rightsStats?.overdueRequests ?? '—'}</div></div>
          <div className="kpi-card green"><div className="kpi-label">Completed</div><div className="kpi-value">{rightsStats?.completedRequests ?? '—'}</div></div>
        </div>
      </div>

      {/* Regulatory Mapping */}
      <div className="card">
        <h3 className="card-title">🗺️ Regulatory Framework Mapping</h3>
        <table className="data-table" style={{ marginTop: 12 }}>
          <thead><tr><th>DPDP Requirement</th><th>GDPR</th><th>ISO 27001</th><th>NIST CSF</th></tr></thead>
          <tbody>
            <tr><td>Consent (S.6-7)</td><td>Art. 6-7</td><td>A.7.2</td><td>PR.IP-1</td></tr>
            <tr><td>Children (S.9)</td><td>Art. 8</td><td>A.7.4</td><td>PR.IP-1</td></tr>
            <tr><td>Breach (S.8)</td><td>Art. 33-34</td><td>A.16.1</td><td>RS.CO-2</td></tr>
            <tr><td>Access Rights (S.11)</td><td>Art. 15</td><td>A.7.3.2</td><td>PR.AC-1</td></tr>
            <tr><td>Erasure (S.12)</td><td>Art. 17</td><td>A.7.4.5</td><td>PR.IP-6</td></tr>
            <tr><td>Cross-Border (S.16)</td><td>Art. 44-49</td><td>A.7.5</td><td>PR.DS-5</td></tr>
          </tbody>
        </table>
      </div>
    </div>
  )
}
