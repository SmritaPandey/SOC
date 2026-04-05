import { useState, useEffect } from 'react'
import api from '../api'
import { showExportDialog, exportToCSV, exportToExcel, exportToWord, exportToPDF, ExportColumn } from '../utils/exportUtils'

const DPIA_COLUMNS: ExportColumn[] = [
  {key:'id',label:'DPIA ID'},{key:'title',label:'Title'},{key:'sector',label:'Sector'},
  {key:'riskLevel',label:'Risk Level'},{key:'status',label:'Status'},{key:'createdAt',label:'Date'},
]

export default function DPIAPage() {
  const [stats, setStats] = useState<any>(null)
  const [dpias, setDpias] = useState<any[]>(demoData)
  const [showCreate, setShowCreate] = useState(false)
  const [selected, setSelected] = useState<any>(null)
  const [form, setForm] = useState({title:'',description:'',category:'DATA_PROCESSING',sector:'Banking'})
  const [auditLogs, setAuditLogs] = useState(demoAudit)

  useEffect(() => {
    api.dpiaStats().then(setStats).catch(()=>{})
    api.dpiaList().then(d=>setDpias(d?.dpias||demoData)).catch(()=>{})
  },[])

  // Export event listener
  useEffect(() => {
    const handleExport = (e: Event) => {
      const format = (e as CustomEvent).detail?.format
      const data = dpias.map(d => ({...d, sector: d.sector||d.category}))
      if (format === 'csv') exportToCSV(data, DPIA_COLUMNS, 'dpia-registry')
      else if (format === 'excel') exportToExcel(data, DPIA_COLUMNS, 'dpia-registry', 'DPIA Registry')
      else if (format === 'word') exportToWord(data, DPIA_COLUMNS, 'dpia-registry', 'DPIA Impact Assessment Registry')
      else if (format === 'pdf') exportToPDF(data, DPIA_COLUMNS, 'dpia-registry', 'DPIA Impact Assessment Registry')
      else showExportDialog(data, DPIA_COLUMNS, 'dpia-registry', 'DPIA Registry')
    }
    window.addEventListener('qs-export', handleExport)
    return () => window.removeEventListener('qs-export', handleExport)
  }, [dpias])

  return (
    <div className="page-container">
      <div className="page-header"><h1>📋 DPIA & Audit Module</h1><p>Data Protection Impact Assessment and compliance audit management</p></div>

      <div className="kpi-grid">
        <div className="kpi-card"><div className="kpi-value">{stats?.totalDPIAs??dpias.length}</div><div className="kpi-label">Total DPIAs</div></div>
        <div className="kpi-card"><div className="kpi-value rag-green">{stats?.completedDPIAs??3}</div><div className="kpi-label">Completed</div></div>
        <div className="kpi-card"><div className="kpi-value rag-amber">{stats?.inProgressDPIAs??4}</div><div className="kpi-label">In Progress</div></div>
        <div className="kpi-card"><div className="kpi-value rag-red">{stats?.highRiskDPIAs??2}</div><div className="kpi-label">High Risk</div></div>
      </div>

      <div style={{display:'flex',gap:12,margin:'24px 0'}}>
        <button className="btn-primary" onClick={()=>setShowCreate(!showCreate)}>+ New DPIA</button>
        <button className="btn-secondary" onClick={()=>setSelected(null)}>🔍 Audit Logs</button>
      </div>

      {showCreate && (
        <div className="glass-card" style={{marginBottom:24}}>
          <h3>📝 Create Impact Assessment</h3>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginTop:16}}>
            <input className="form-input" placeholder="Assessment Title" value={form.title} onChange={e=>setForm({...form,title:e.target.value})} />
            <select className="form-input" value={form.sector} onChange={e=>setForm({...form,sector:e.target.value})}>
              <option>Banking</option><option>Healthcare</option><option>IT</option><option>E-commerce</option><option>Government</option>
            </select>
            <textarea className="form-input" placeholder="Description of data processing activity..." rows={3} style={{gridColumn:'1/3'}} value={form.description} onChange={e=>setForm({...form,description:e.target.value})} />
            <button className="btn-primary" onClick={()=>{setDpias([{id:'DPIA-'+(dpias.length+1),...form,status:'IN_PROGRESS',riskLevel:'MEDIUM',createdAt:new Date().toISOString().split('T')[0]},...dpias]);setShowCreate(false)}}>Create DPIA</button>
            <button className="btn-secondary" onClick={()=>setShowCreate(false)}>Cancel</button>
          </div>
        </div>
      )}

      {/* DPIA TABLE */}
      <div className="glass-card">
        <h3>📊 Impact Assessment Registry</h3>
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>ID</th><th>Title</th><th>Sector</th><th>Risk Level</th><th>Status</th><th>Date</th><th>Actions</th></tr></thead>
            <tbody>
              {dpias.map((d,i)=>(
                <tr key={i} onClick={()=>setSelected(d)} style={{cursor:'pointer'}}>
                  <td style={{fontFamily:'monospace'}}>{d.id}</td>
                  <td><strong>{d.title}</strong></td>
                  <td>{d.sector||d.category}</td>
                  <td><span className={`rag-badge ${d.riskLevel==='HIGH'?'rag-red':d.riskLevel==='MEDIUM'?'rag-amber':'rag-green'}`}>{d.riskLevel}</span></td>
                  <td><span className={`rag-badge ${d.status==='COMPLETED'?'rag-green':'rag-amber'}`}>{d.status}</span></td>
                  <td>{d.createdAt}</td>
                  <td><button className="btn-sm">View</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* AUDIT LOG */}
      <div className="glass-card" style={{marginTop:24}}>
        <h3>🔐 Audit Log Trail</h3>
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>Timestamp</th><th>User</th><th>Action</th><th>Module</th><th>Details</th></tr></thead>
            <tbody>
              {auditLogs.map((a,i)=>(
                <tr key={i}>
                  <td style={{fontFamily:'monospace',fontSize:12}}>{a.timestamp}</td>
                  <td>{a.user}</td>
                  <td><span className={`rag-badge ${a.action.includes('DELETE')?'rag-red':a.action.includes('CREATE')?'rag-green':'rag-amber'}`}>{a.action}</span></td>
                  <td>{a.module}</td>
                  <td style={{fontSize:13,color:'var(--text-muted)'}}>{a.details}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {selected && (
        <div className="glass-card" style={{marginTop:24}}>
          <div style={{display:'flex',justifyContent:'space-between'}}><h3>📄 {selected.title}</h3><button className="btn-sm" onClick={()=>setSelected(null)}>✕</button></div>
          <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:16,marginTop:16}}>
            <div><strong>Risk Level:</strong> {selected.riskLevel}</div>
            <div><strong>Status:</strong> {selected.status}</div>
            <div><strong>Sector:</strong> {selected.sector||selected.category}</div>
          </div>
          <div style={{marginTop:16}}>
            <h4>Risk Matrix</h4>
            <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:8,marginTop:8}}>
              {['Data Volume','Sensitivity','Processing Purpose','Retention Period','Cross-Border','Third-Party Sharing'].map((r,i)=>(
                <div key={i} style={{padding:12,borderRadius:8,background:'var(--bg-primary)',textAlign:'center'}}>
                  <div style={{fontSize:11,color:'var(--text-muted)',marginBottom:4}}>{r}</div>
                  <div className={i<2?'rag-red':i<4?'rag-amber':'rag-green'} style={{fontWeight:700}}>{i<2?'HIGH':i<4?'MEDIUM':'LOW'}</div>
                </div>
              ))}
            </div>
          </div>
          <div style={{display:'flex',gap:8,marginTop:16}}>
            <button className="btn-primary" onClick={()=>showExportDialog(dpias, DPIA_COLUMNS, 'dpia-report', 'DPIA Audit Report')}>📤 Export Audit Report</button>
            <button className="btn-secondary" onClick={()=>{
              alert(`AI Recommendations for ${selected.title}:\n\n1. Implement data minimization for PII fields\n2. Add encryption at rest (AES-256-GCM)\n3. Review data retention schedule (max 3 years)\n4. Conduct annual penetration testing\n5. Implement consent re-validation workflow\n6. Add cross-border transfer impact analysis\n7. Deploy real-time DLP scanning on all channels\n\nRisk Level: ${selected.riskLevel}\nDPDP Section: §8 (Obligations of Data Fiduciary)`)
            }}>📋 Generate Recommendations</button>
          </div>
        </div>
      )}
    </div>
  )
}

const demoData = [
  {id:'DPIA-001',title:'Customer Onboarding KYC Processing',sector:'Banking',riskLevel:'HIGH',status:'IN_PROGRESS',createdAt:'2026-02-15'},
  {id:'DPIA-002',title:'Employee Biometric Attendance System',sector:'IT',riskLevel:'HIGH',status:'COMPLETED',createdAt:'2026-01-20'},
  {id:'DPIA-003',title:'Patient Medical Records Digitization',sector:'Healthcare',riskLevel:'CRITICAL',status:'IN_PROGRESS',createdAt:'2026-03-01'},
  {id:'DPIA-004',title:'Marketing Campaign Analytics',sector:'E-commerce',riskLevel:'MEDIUM',status:'COMPLETED',createdAt:'2025-12-10'},
  {id:'DPIA-005',title:'Cross-Border Data Transfer Assessment',sector:'IT',riskLevel:'HIGH',status:'IN_PROGRESS',createdAt:'2026-02-28'},
  {id:'DPIA-006',title:'Aadhaar Integration for eKYC',sector:'Banking',riskLevel:'CRITICAL',status:'REVIEW',createdAt:'2026-03-05'},
  {id:'DPIA-007',title:'Student Performance Analytics',sector:'Education',riskLevel:'MEDIUM',status:'COMPLETED',createdAt:'2026-01-15'},
]

const demoAudit = [
  {timestamp:'2026-03-13 14:30:22',user:'admin',action:'CREATE',module:'Consent',details:'New consent record CON-4521 created for principal DP-0089'},
  {timestamp:'2026-03-13 14:28:15',user:'admin',action:'UPDATE',module:'Policy',details:'Policy POL-003 updated to version 1.3'},
  {timestamp:'2026-03-13 14:25:00',user:'system',action:'ALERT',module:'SIEM',details:'Threat intelligence feed refresh: 342 new indicators added'},
  {timestamp:'2026-03-13 14:20:45',user:'admin',action:'CREATE',module:'DPIA',details:'New DPIA assessment initiated for KYC processing'},
  {timestamp:'2026-03-13 14:15:30',user:'system',action:'SCAN',module:'DLP',details:'PII scan completed: 3 Aadhaar numbers detected in uploaded document'},
  {timestamp:'2026-03-13 14:10:00',user:'admin',action:'DELETE',module:'Rights',details:'Rights request RR-0234 marked as completed and archived'},
]
