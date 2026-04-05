import { useState } from 'react'

const TEMPLATES = [
  {id:'RPT-01',name:'DPDP Compliance Summary',desc:'Overall compliance status with RAG scoring across all modules',category:'Compliance'},
  {id:'RPT-02',name:'Consent Analytics Report',desc:'Consent collection, withdrawal rates, and purpose analytics',category:'Consent'},
  {id:'RPT-03',name:'Breach Notification Report',desc:'Breach timeline, notifications sent, DPBI/CERT-IN status',category:'Breach'},
  {id:'RPT-04',name:'DPIA Summary Report',desc:'Impact assessment results with risk matrix',category:'DPIA'},
  {id:'RPT-05',name:'Policy Compliance Report',desc:'Policy lifecycle status, review schedule, version history',category:'Policy'},
  {id:'RPT-06',name:'Rights Request Report',desc:'Data principal rights requests, SLA compliance, resolution',category:'Rights'},
  {id:'RPT-07',name:'SIEM Incident Report',desc:'Security events, alerts, threat intelligence summary',category:'Security'},
  {id:'RPT-08',name:'DLP Activity Report',desc:'PII detections, DLP incidents, data classifications',category:'DLP'},
  {id:'RPT-09',name:'Audit Trail Report',desc:'Complete system audit log with integrity verification',category:'Audit'},
  {id:'RPT-10',name:'Gap Analysis Report',desc:'Self-assessment results, gaps identified, remediation plan',category:'Gap'},
  {id:'RPT-11',name:'Executive Dashboard Report',desc:'Board-ready summary of DPDP compliance posture',category:'Executive'},
  {id:'RPT-12',name:'Sector Compliance Report',desc:'Sector-specific compliance assessment with benchmarks',category:'Sector'},
]

export default function ReportsPage() {
  const [selectedReport, setSelectedReport] = useState<any>(null)
  const [customQuery, setCustomQuery] = useState('')
  const [generating, setGenerating] = useState(false)
  const [generated, setGenerated] = useState<any>(null)
  const [filterCat, setFilterCat] = useState('')

  const generateReport = () => {
    setGenerating(true)
    setTimeout(()=>{
      setGenerated({title:selectedReport?.name||'Custom Report',generatedAt:new Date().toLocaleString(),pages:12,format:'PDF'})
      setGenerating(false)
    },1500)
  }

  const generateCustom = () => {
    setGenerating(true)
    setTimeout(()=>{
      setGenerated({title:'Custom: '+customQuery,generatedAt:new Date().toLocaleString(),pages:5,format:'PDF'})
      setGenerating(false)
    },2000)
  }

  const filtered = filterCat ? TEMPLATES.filter(t=>t.category===filterCat) : TEMPLATES

  return (
    <div className="page-container">
      <div className="page-header"><h1>📊 Reports Engine</h1><p>Generate, export, and share DPDP compliance reports</p></div>

      <div className="kpi-grid">
        <div className="kpi-card"><div className="kpi-value">{TEMPLATES.length}</div><div className="kpi-label">Report Templates</div></div>
        <div className="kpi-card"><div className="kpi-value">4</div><div className="kpi-label">Export Formats</div></div>
        <div className="kpi-card"><div className="kpi-value rag-green">AI</div><div className="kpi-label">Custom Report Builder</div></div>
        <div className="kpi-card"><div className="kpi-value">✉️</div><div className="kpi-label">Email Sharing</div></div>
      </div>

      {/* AI CUSTOM REPORT BUILDER */}
      <div className="glass-card" style={{marginTop:24}}>
        <h3>🤖 AI Custom Report Builder</h3>
        <p style={{color:'var(--text-muted)',marginBottom:12}}>Describe the report you need in plain English. Our AI will generate it from your data.</p>
        <div style={{display:'flex',gap:12}}>
          <input className="form-input" style={{flex:1}} placeholder="E.g.: Show me all consent withdrawals in the last 30 days by sector with trend analysis..." value={customQuery} onChange={e=>setCustomQuery(e.target.value)} />
          <button className="btn-primary" onClick={generateCustom} disabled={!customQuery||generating}>{generating?'Generating...':'🚀 Generate'}</button>
        </div>
      </div>

      {/* GENERATED REPORT */}
      {generated && (
        <div className="glass-card" style={{marginTop:16,border:'2px solid var(--accent)'}}>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
            <h3>✅ Report Generated: {generated.title}</h3>
            <button className="btn-sm" onClick={()=>setGenerated(null)}>✕</button>
          </div>
          <p style={{color:'var(--text-muted)'}}>Generated at: {generated.generatedAt} • {generated.pages} pages</p>
          <div style={{display:'flex',gap:8,marginTop:12}}>
            <button className="btn-primary">📄 Export PDF</button>
            <button className="btn-secondary">📊 Export Excel</button>
            <button className="btn-secondary">📝 Export Word</button>
            <button className="btn-secondary">📋 Export CSV</button>
            <button className="btn-secondary">🖨️ Print Preview</button>
            <button className="btn-secondary">✉️ Email Report</button>
          </div>
        </div>
      )}

      {/* FILTER */}
      <div style={{display:'flex',gap:8,margin:'24px 0',flexWrap:'wrap'}}>
        <button className={!filterCat?'btn-primary':'btn-secondary'} onClick={()=>setFilterCat('')}>All</button>
        {[...new Set(TEMPLATES.map(t=>t.category))].map(c=>(
          <button key={c} className={filterCat===c?'btn-primary':'btn-secondary'} onClick={()=>setFilterCat(c)}>{c}</button>
        ))}
      </div>

      {/* TEMPLATE TABLE */}
      <div className="glass-card">
        <h3>📋 Report Templates ({filtered.length})</h3>
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>ID</th><th>Report Name</th><th>Category</th><th>Description</th><th>Actions</th></tr></thead>
            <tbody>
              {filtered.map((t,i)=>(
                <tr key={i}>
                  <td style={{fontFamily:'monospace'}}>{t.id}</td>
                  <td><strong>{t.name}</strong></td>
                  <td><span className="rag-badge rag-green">{t.category}</span></td>
                  <td style={{fontSize:13,color:'var(--text-muted)'}}>{t.desc}</td>
                  <td>
                    <button className="btn-sm" style={{marginRight:4}} onClick={()=>{setSelectedReport(t);generateReport()}}>Generate</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
