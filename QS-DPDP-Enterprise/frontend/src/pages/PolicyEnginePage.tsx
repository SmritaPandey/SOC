import { useState, useMemo, useRef } from 'react'
import { SECTORS, SECTOR_POLICIES, SECTOR_CONTROLS } from '../data/sectorData'
import { useAppContext } from '../context/AppContext'
import { exportToWord, exportToPDF, showExportDialog, ExportColumn } from '../utils/exportUtils'

const POLICY_COLUMNS: ExportColumn[] = [
  {key:'id',label:'Policy ID'},{key:'title',label:'Title'},{key:'dpdpSection',label:'DPDP Section'},
  {key:'status',label:'Status'},{key:'complianceScore',label:'Score %'},{key:'owner',label:'Owner'},
  {key:'lastReview',label:'Last Review'},{key:'nextReview',label:'Next Review'},
]

/* ISO Policy Lifecycle: DRAFT → REVIEW → APPROVED → PUBLISHED → ACTIVE → REVISION → RETIRED */
const LIFECYCLE_STAGES = ['DRAFT','REVIEW','APPROVED','PUBLISHED','ACTIVE','REVISION','RETIRED']
const LIFECYCLE_COLORS: Record<string,string> = { DRAFT:'var(--amber)', REVIEW:'var(--blue)', APPROVED:'var(--green)', PUBLISHED:'var(--green)', ACTIVE:'var(--green)', REVISION:'var(--amber)', RETIRED:'var(--red)' }

export default function PolicyEnginePage() {
  const { sector, setSector } = useAppContext()
  const [tab, setTab] = useState<'registry'|'lifecycle'|'document'|'import'>('registry')
  const [filterStatus, setFilterStatus] = useState('')
  const [filterCategory, setFilterCategory] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [selectedPolicy, setSelectedPolicy] = useState<any>(null)
  const [docView, setDocView] = useState<any>(null)
  const [form, setForm] = useState({title:'',description:'',category:'DATA_PROTECTION',priority:'HIGH',dpdpSection:'Section 4',objective:'',scope:'',procedure:'',controls:'',metrics:'',reviewFrequency:'Annual'})
  const [importFile, setImportFile] = useState<string>('')
  const [importProcessing, setImportProcessing] = useState(false)
  const [importResult, setImportResult] = useState<any>(null)
  const fileRef = useRef<HTMLInputElement>(null)

  const allPolicies = SECTOR_POLICIES[sector] || SECTOR_POLICIES['Banking & Finance']
  const controls = SECTOR_CONTROLS[sector] || SECTOR_CONTROLS['Banking & Finance']
  const [policies, setPolicies] = useState(allPolicies)

  const changeSector = (s: string) => { setSector(s); setPolicies(SECTOR_POLICIES[s] || SECTOR_POLICIES['Banking & Finance']); setSelectedPolicy(null); setDocView(null) }

  const filtered = useMemo(() => {
    let f = policies
    if (filterStatus) f = f.filter(p => p.status === filterStatus)
    if (filterCategory) f = f.filter(p => p.category === filterCategory)
    return f
  }, [policies, filterStatus, filterCategory])

  const avgCompliance = policies.reduce((s,p) => s + p.complianceScore, 0) / policies.length

  const handleCreate = () => {
    const id = `${sector.slice(0,2).toUpperCase()}-POL-${String(policies.length+1).padStart(3,'0')}`
    setPolicies([...policies, {...form,id,status:'DRAFT',complianceScore:0,controls:form.controls.split(',').map(c=>c.trim()).filter(Boolean)}])
    setShowCreate(false)
    setForm({title:'',description:'',category:'DATA_PROTECTION',priority:'HIGH',dpdpSection:'Section 4',objective:'',scope:'',procedure:'',controls:'',metrics:'',reviewFrequency:'Annual'})
  }

  const promotePolicy = (id: string) => {
    setPolicies(policies.map(p => {
      if (p.id !== id) return p
      const idx = LIFECYCLE_STAGES.indexOf(p.status)
      return {...p, status: idx < LIFECYCLE_STAGES.length - 1 ? LIFECYCLE_STAGES[idx+1] : p.status}
    }))
  }

  const simulateImport = () => {
    setImportProcessing(true)
    setTimeout(() => {
      setImportResult({
        fileName: importFile || 'Uploaded_Policy.pdf',
        extracted: {
          title: 'Imported Data Protection Policy',
          sections: 12,
          procedures: 8,
          controls: 15,
          metrics: 6,
          mappedDPDP: ['Section 4','Section 6','Section 8','Section 11-13'],
        },
        status: 'PARAMETERIZED',
      })
      setImportProcessing(false)
    }, 2000)
  }

  // Generate document view of a policy
  const viewAsDocument = (p: any) => setDocView(p)

  // Print function
  const printDoc = () => window.print()

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1>⚙️ Policy Engine — {sector}</h1>
          <p>ISO 27001-aligned policy lifecycle — Parameterized with procedures, controls & metrics</p>
        </div>
        <button className="btn-primary" onClick={()=>setShowCreate(!showCreate)}>+ Create Policy</button>
      </div>

      {/* SECTOR SELECTOR */}
      <div style={{display:'flex',gap:12,marginBottom:16,alignItems:'center'}}>
        <select className="form-input" style={{width:220}} value={sector} onChange={e=>changeSector(e.target.value)}>
          {SECTORS.map(s=><option key={s}>{s}</option>)}
        </select>
        <span style={{fontSize:12,color:'var(--text-muted)'}}>Sector policies, procedures, and controls auto-loaded on selection</span>
      </div>

      {/* TABS */}
      <div style={{display:'flex',gap:8,marginBottom:20}}>
        {(['registry','lifecycle','document','import'] as const).map(t=>(
          <button key={t} className={tab===t?'btn-primary':'btn-secondary'} onClick={()=>setTab(t)}>
            {t==='registry'?'📋 Policy Registry':t==='lifecycle'?'🔄 ISO Lifecycle':t==='document'?'📄 Document View':'📁 Import/Export'}
          </button>
        ))}
      </div>

      {/* KPIs */}
      <div className="kpi-grid">
        <div className="kpi-card"><div className="kpi-value">{policies.length}</div><div className="kpi-label">Total Policies</div></div>
        <div className="kpi-card green"><div className="kpi-value" style={{color:'var(--green)'}}>{policies.filter(p=>p.status==='ACTIVE').length}</div><div className="kpi-label">Active</div></div>
        <div className="kpi-card amber"><div className="kpi-value" style={{color:'var(--amber)'}}>{policies.filter(p=>p.status==='DRAFT').length}</div><div className="kpi-label">Draft</div></div>
        <div className="kpi-card red"><div className="kpi-value" style={{color:'var(--red)'}}>{policies.filter(p=>p.status==='REVIEW').length}</div><div className="kpi-label">Review</div></div>
        <div className="kpi-card blue">
          <div className="kpi-value" style={{color:avgCompliance>=80?'var(--green)':'var(--amber)'}}>{avgCompliance.toFixed(0)}%</div>
          <div className="kpi-label">Avg Compliance</div>
        </div>
      </div>

      {/* ═══ REGISTRY TAB ═══ */}
      {tab==='registry' && (
        <>
          {/* Compliance Metrics Dashboard */}
          <div className="glass-card" style={{marginTop:16}}>
            <h3>📊 Compliance Metrics — {sector}</h3>
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead><tr><th>Control</th><th>Category</th><th>Implemented</th><th>Score</th><th>Compliance Bar</th><th>Gap</th></tr></thead>
                <tbody>{controls.map((c,i) => (
                  <tr key={i}>
                    <td><strong>{c.control}</strong></td><td>{c.category}</td>
                    <td>{c.implemented ? <span className="rag-badge rag-green">YES</span> : <span className="rag-badge rag-red">GAP</span>}</td>
                    <td style={{fontWeight:700}}>{c.complianceScore}%</td>
                    <td style={{width:150}}><div style={{background:'var(--border)',borderRadius:4,height:8,overflow:'hidden'}}><div style={{width:`${c.complianceScore}%`,height:'100%',background:c.complianceScore>=80?'var(--green)':c.complianceScore>=50?'var(--amber)':'var(--red)',borderRadius:4}} /></div></td>
                    <td style={{fontSize:12,color:c.gap?'var(--red)':'var(--green)'}}>{c.gap||'✅ No gap'}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          </div>

          {/* Filters */}
          <div style={{display:'flex',gap:12,margin:'16px 0',flexWrap:'wrap'}}>
            <select className="form-input" style={{width:150}} value={filterStatus} onChange={e=>setFilterStatus(e.target.value)}>
              <option value="">All Status</option>{LIFECYCLE_STAGES.map(s=><option key={s}>{s}</option>)}
            </select>
            <select className="form-input" style={{width:180}} value={filterCategory} onChange={e=>setFilterCategory(e.target.value)}>
              <option value="">All Categories</option>
              <option>DATA_PROTECTION</option><option>CONSENT</option><option>BREACH</option><option>ACCESS_CONTROL</option><option>RETENTION</option><option>CROSS_BORDER</option>
            </select>
          </div>

          {/* CREATE FORM — Full parameterized policy */}
          {showCreate && (
            <div className="glass-card" style={{marginBottom:16}}>
              <h3>📝 Create Parameterized Policy</h3>
              <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:12}}>ISO 27001 format: Statement → Objective → Scope → Procedures → Controls → Metrics</p>
              <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginTop:12}}>
                <input className="form-input" placeholder="Policy Title *" value={form.title} onChange={e=>setForm({...form,title:e.target.value})} />
                <select className="form-input" value={form.category} onChange={e=>setForm({...form,category:e.target.value})}>
                  <option>DATA_PROTECTION</option><option>CONSENT</option><option>BREACH</option><option>ACCESS_CONTROL</option><option>RETENTION</option><option>CROSS_BORDER</option>
                </select>
                <textarea className="form-input" placeholder="Policy Statement (What this policy mandates) *" rows={2} style={{gridColumn:'1/3'}} value={form.description} onChange={e=>setForm({...form,description:e.target.value})} />
                <textarea className="form-input" placeholder="Objective (Why this policy exists)" rows={2} value={form.objective} onChange={e=>setForm({...form,objective:e.target.value})} />
                <textarea className="form-input" placeholder="Scope (Who and what it applies to)" rows={2} value={form.scope} onChange={e=>setForm({...form,scope:e.target.value})} />
                <textarea className="form-input" placeholder="Procedures (Step-by-step implementation)" rows={3} style={{gridColumn:'1/3'}} value={form.procedure} onChange={e=>setForm({...form,procedure:e.target.value})} />
                <input className="form-input" placeholder="Controls (comma-separated)" value={form.controls} onChange={e=>setForm({...form,controls:e.target.value})} />
                <input className="form-input" placeholder="Metrics (KPIs to measure compliance)" value={form.metrics} onChange={e=>setForm({...form,metrics:e.target.value})} />
                <select className="form-input" value={form.dpdpSection} onChange={e=>setForm({...form,dpdpSection:e.target.value})}>
                  <option>Section 4</option><option>Section 5</option><option>Section 6</option><option>Section 7</option><option>Section 8</option><option>Section 9</option><option>Section 10</option><option>Section 11-13</option><option>Section 14</option><option>Section 15</option><option>Section 16</option>
                </select>
                <select className="form-input" value={form.reviewFrequency} onChange={e=>setForm({...form,reviewFrequency:e.target.value})}>
                  <option>Quarterly</option><option>Semi-Annual</option><option>Annual</option><option>Biennial</option>
                </select>
                <div style={{display:'flex',gap:8,gridColumn:'1/3'}}>
                  <button className="btn-primary" onClick={handleCreate}>💾 Save Policy</button>
                  <button className="btn-secondary" onClick={()=>setShowCreate(false)}>Cancel</button>
                </div>
              </div>
            </div>
          )}

          {/* Policy Table */}
          <div className="glass-card">
            <h3>📋 Policy Registry ({filtered.length} policies)</h3>
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead><tr><th>ID</th><th>Title</th><th>Category</th><th>DPDP</th><th>Status</th><th>Compliance</th><th>Controls</th><th>Actions</th></tr></thead>
                <tbody>{filtered.map((p,i) => (
                  <tr key={i} onClick={()=>{setDocView(p);setTab('document');setSelectedPolicy(p)}} style={{cursor:'pointer'}} title="Click to view ISO policy document">
                    <td style={{fontFamily:'monospace',fontSize:11}}>{p.id}</td>
                    <td><strong>{p.title}</strong></td>
                    <td>{p.category}</td>
                    <td style={{fontSize:12,color:'var(--brand-primary)'}}>{p.dpdpSection}</td>
                    <td><span style={{padding:'3px 10px',borderRadius:12,fontSize:11,fontWeight:700,color:'#fff',background:LIFECYCLE_COLORS[p.status]||'var(--amber)'}}>{p.status}</span></td>
                    <td>
                      <div style={{display:'flex',alignItems:'center',gap:6}}>
                        <div style={{background:'var(--border)',borderRadius:4,height:6,width:50}}><div style={{width:`${p.complianceScore}%`,height:'100%',background:p.complianceScore>=80?'var(--green)':'var(--amber)',borderRadius:4}} /></div>
                        <span style={{fontSize:12,fontWeight:600}}>{p.complianceScore}%</span>
                      </div>
                    </td>
                    <td style={{fontSize:11}}>{p.controls?.length||0}</td>
                    <td>
                      <div style={{display:'flex',gap:4}}>
                        <button className="btn-sm" onClick={e=>{e.stopPropagation();viewAsDocument(p)}}>📄 Doc</button>
                        <button className="btn-sm" onClick={e=>{e.stopPropagation();promotePolicy(p.id)}} title="Promote to next lifecycle stage">▶</button>
                      </div>
                    </td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {/* ═══ ISO LIFECYCLE TAB ═══ */}
      {tab==='lifecycle' && (
        <div className="glass-card">
          <h3>🔄 ISO Policy Lifecycle Management</h3>
          <p style={{color:'var(--text-muted)',marginBottom:20}}>ISO 27001 / DPDP Act aligned policy lifecycle: Draft → Review → Approve → Publish → Active → Revision → Retire</p>

          {/* Lifecycle Flow Diagram */}
          <div style={{display:'flex',alignItems:'center',gap:0,marginBottom:32,justifyContent:'center',flexWrap:'wrap'}}>
            {LIFECYCLE_STAGES.map((stage, i) => {
              const count = policies.filter(p=>p.status===stage).length
              return (
                <div key={stage} style={{display:'flex',alignItems:'center'}}>
                  <div style={{
                    padding:'16px 20px',borderRadius:'var(--radius)',minWidth:110,textAlign:'center',
                    background: LIFECYCLE_COLORS[stage],color:'#fff',fontWeight:700,fontSize:13,
                    boxShadow: count > 0 ? '0 4px 12px rgba(0,0,0,0.2)' : 'none',
                    opacity: count > 0 ? 1 : 0.5,
                  }}>
                    <div>{stage}</div>
                    <div style={{fontSize:20,marginTop:4}}>{count}</div>
                  </div>
                  {i < LIFECYCLE_STAGES.length - 1 && <div style={{width:30,height:2,background:'var(--border)'}} />}
                </div>
              )
            })}
          </div>

          {/* Lifecycle Actions */}
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>ID</th><th>Title</th><th>Current Stage</th><th>Next Stage</th><th>Action</th></tr></thead>
              <tbody>{policies.map(p => {
                const idx = LIFECYCLE_STAGES.indexOf(p.status)
                const next = idx < LIFECYCLE_STAGES.length-1 ? LIFECYCLE_STAGES[idx+1] : '—'
                return (
                  <tr key={p.id}>
                    <td style={{fontFamily:'monospace',fontSize:11}}>{p.id}</td>
                    <td><strong>{p.title}</strong></td>
                    <td><span style={{padding:'3px 10px',borderRadius:12,fontSize:11,fontWeight:700,color:'#fff',background:LIFECYCLE_COLORS[p.status]}}>{p.status}</span></td>
                    <td>{next !== '—' ? <span style={{padding:'3px 10px',borderRadius:12,fontSize:11,fontWeight:700,color:'#fff',background:LIFECYCLE_COLORS[next]}}>{next}</span> : '—'}</td>
                    <td>{next !== '—' && <button className="btn-primary" style={{fontSize:12,padding:'4px 12px'}} onClick={()=>promotePolicy(p.id)}>▶ Promote to {next}</button>}</td>
                  </tr>
                )
              })}</tbody>
            </table>
          </div>
        </div>
      )}

      {/* ═══ DOCUMENT VIEW TAB ═══ */}
      {tab==='document' && (
        <div>
          {!docView ? (
            <div className="glass-card" style={{textAlign:'center',padding:40}}>
              <h3>📄 Policy Document View</h3>
              <p style={{color:'var(--text-muted)',margin:'16px 0 24px'}}>Select a policy from the Registry tab and click "📄 Doc" to view it as a formatted document.</p>
              <div style={{display:'flex',gap:8,justifyContent:'center',flexWrap:'wrap'}}>
                {policies.slice(0,4).map(p => (
                  <button key={p.id} className="btn-secondary" onClick={()=>viewAsDocument(p)}>{p.title}</button>
                ))}
              </div>
            </div>
          ) : (
            <div>
              {/* Action Bar */}
              <div style={{display:'flex',gap:8,marginBottom:16,flexWrap:'wrap'}}>
                <button className="btn-secondary" onClick={printDoc}>🖨️ Print</button>
                <button className="btn-secondary" onClick={()=>{
                  const docEl = document.getElementById('policy-document')
                  if (!docEl) return
                  const docTitle = docView?.title?.replace(/[^a-zA-Z0-9\s]/g,'').replace(/\s+/g,'_') || 'Policy_Document'
                  const pw = window.open('','_blank')
                  if (!pw) { alert('Please allow popups'); return }
                  pw.document.write(`<!DOCTYPE html><html><head><title>${docView?.title || 'Policy Document'} — ${sector}</title><style>body{font-family:Georgia,serif;margin:40px;color:#1a1a2e}h1,h2,h3{color:#4C1D95}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ddd;padding:8px;text-align:left}th{background:#7C3AED;color:white}@media print{@page{margin:20mm;size:A4}}</style></head><body>${docEl.innerHTML}<script>setTimeout(()=>window.print(),500)</script></body></html>`)
                  pw.document.close()
                }}>📤 Export PDF</button>
                <button className="btn-secondary" onClick={()=>{
                  const docEl = document.getElementById('policy-document')
                  if (!docEl) return
                  const docTitle = docView?.title?.replace(/[^a-zA-Z0-9\s]/g,'').replace(/\s+/g,'_') || 'Policy_Document'
                  const html = `<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word"><head><meta charset="utf-8"><style>body{font-family:Calibri;margin:1in;color:#1a1a2e}h1,h2,h3{color:#4C1D95}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ddd;padding:6px 10px}th{background:#7C3AED;color:white}</style></head><body>${docEl.innerHTML}</body></html>`
                  const blob = new Blob(['\ufeff'+html], {type:'application/msword'})
                  const url = URL.createObjectURL(blob)
                  const a = document.createElement('a')
                  a.href = url; a.download = `${docTitle}_${sector.replace(/[^a-zA-Z0-9]/g,'_')}.doc`; a.click()
                  URL.revokeObjectURL(url)
                }}>📝 Export Word</button>
                <button className="btn-secondary" onClick={()=>{ window.print() }}>🖨️ Print Preview</button>
                <button className="btn-secondary" onClick={()=>setDocView(null)}>✕ Close Document</button>
              </div>

              {/* Formatted Policy Document — Professional Consultancy Grade */}
              <div className="glass-card" id="policy-document" style={{maxWidth:900,margin:'0 auto',padding:48,lineHeight:1.8,fontFamily:'Georgia, "Times New Roman", serif',fontSize:13}}>
                {/* Letterhead — Corporate Style */}
                <div style={{display:'flex',justifyContent:'space-between',alignItems:'flex-start',borderBottom:'3px solid #4C1D95',paddingBottom:16,marginBottom:8}}>
                  <div>
                    <div style={{fontSize:22,fontWeight:700,letterSpacing:1.5,color:'#4C1D95'}}>QS-DPDP ENTERPRISE™</div>
                    <div style={{fontSize:11,color:'var(--text-muted)',marginTop:2}}>Data Protection & Digital Privacy Governance Platform</div>
                    <div style={{fontSize:10,color:'var(--text-muted)'}}>ISO/IEC 27001:2022 | ISO/IEC 27701:2019 | DPDP Act 2023 Aligned</div>
                  </div>
                  <div style={{textAlign:'right',fontSize:10,color:'var(--text-muted)'}}>
                    <div><strong>CLASSIFICATION:</strong> <span style={{color:docView.category==='ACCESS_CONTROL'?'#DC2626':'#EA580C',fontWeight:700}}>
                      {docView.category==='ACCESS_CONTROL'?'CONFIDENTIAL':'INTERNAL USE ONLY'}</span></div>
                    <div>Document Control No: QS/{docView.id}/v2.0</div>
                    <div>Print Date: {new Date().toLocaleDateString('en-IN',{day:'2-digit',month:'short',year:'numeric'})}</div>
                  </div>
                </div>
                <div style={{fontSize:9,color:'var(--text-muted)',marginBottom:24,fontStyle:'italic'}}>
                  © 2024-2026 QS-DPDP Enterprise. This document is proprietary. Unauthorized reproduction or distribution is prohibited.
                </div>

                {/* Document Control Table */}
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:12,marginBottom:24}}>
                  <tbody>
                    {[
                      ['Document Title',docView.title],
                      ['Document ID',`QS-POL-${docView.id}`],
                      ['Version','2.0'],
                      ['Classification',docView.category==='ACCESS_CONTROL'?'CONFIDENTIAL':'INTERNAL USE ONLY'],
                      ['Applicable Sector(s)',sector],
                      ['Regulatory Reference',docView.dpdpSection],
                      ['Policy Owner','Chief Data Protection Officer (CDPO)'],
                      ['Approving Authority','Board of Directors / Data Fiduciary'],
                      ['Effective Date','01-Jan-2026'],
                      ['Last Reviewed','13-Mar-2026'],
                      ['Next Review Date','01-Jul-2026'],
                      ['Review Frequency','Semi-Annual (or upon regulatory change)'],
                      ['Supersedes','Version 1.0 dated 01-Jul-2025'],
                      ['Compliance Status',`${docView.complianceScore}% — ${docView.status}`],
                    ].map(([k,v],i) => (
                      <tr key={i} style={{borderBottom:'1px solid var(--border)'}}>
                        <td style={{padding:'6px 12px',fontWeight:600,width:'35%',background:i%2===0?'var(--bg-glass)':'transparent'}}>{k}</td>
                        <td style={{padding:'6px 12px'}}>{v}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {/* Version History */}
                <h3 style={{fontSize:13,margin:'16px 0 8px',color:'#4C1D95',borderBottom:'1px solid var(--border)',paddingBottom:4}}>VERSION HISTORY</h3>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:11,marginBottom:24}}>
                  <thead>
                    <tr style={{background:'#4C1D95',color:'#fff'}}>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Version</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Date</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Author</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Change Description</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr style={{borderBottom:'1px solid var(--border)'}}><td style={{padding:'4px 8px'}}>1.0</td><td style={{padding:'4px 8px'}}>01-Jul-2025</td><td style={{padding:'4px 8px'}}>CDPO Office</td><td style={{padding:'4px 8px'}}>Initial policy creation aligned with DPDP Act 2023</td></tr>
                    <tr style={{borderBottom:'1px solid var(--border)'}}><td style={{padding:'4px 8px'}}>1.1</td><td style={{padding:'4px 8px'}}>15-Sep-2025</td><td style={{padding:'4px 8px'}}>Legal & Compliance</td><td style={{padding:'4px 8px'}}>Updated penalty references as per DPDP Rules notification</td></tr>
                    <tr style={{borderBottom:'1px solid var(--border)'}}><td style={{padding:'4px 8px'}}>2.0</td><td style={{padding:'4px 8px'}}>01-Jan-2026</td><td style={{padding:'4px 8px'}}>DPO + External Auditor</td><td style={{padding:'4px 8px'}}>Major revision: enhanced controls, updated regulatory mapping, sector-specific procedures, RACI matrix added</td></tr>
                  </tbody>
                </table>

                {/* Title */}
                <h1 style={{fontSize:20,textAlign:'center',margin:'32px 0 8px',color:'#4C1D95',textTransform:'uppercase',letterSpacing:1}}>{docView.title}</h1>
                <div style={{textAlign:'center',fontSize:11,color:'var(--text-muted)',marginBottom:32}}>Policy Reference: {docView.dpdpSection} | Sector: {sector}</div>

                {/* TABLE OF CONTENTS */}
                <div style={{background:'var(--bg-glass)',border:'1px solid var(--border)',borderRadius:8,padding:16,marginBottom:32}}>
                  <h3 style={{fontSize:13,margin:'0 0 8px',color:'#4C1D95'}}>TABLE OF CONTENTS</h3>
                  {['1. Purpose & Policy Statement','2. Definitions','3. Scope & Applicability','4. Roles & Responsibilities (RACI Matrix)',
                    '5. Policy Requirements','6. Detailed Procedures & Operating Guidelines','7. Controls & Safeguards',
                    '8. Risk Assessment & Classification','9. Regulatory Mapping & Compliance Requirements',
                    '10. Enforcement, Penalties & Exceptions','11. Training & Awareness','12. Monitoring, Metrics & Reporting',
                    '13. Review, Amendment & Decommissioning','Annexure A: Regulatory Cross-Reference','Annexure B: Approval & Signature Block'
                  ].map((item,i) => (
                    <div key={i} style={{fontSize:11,padding:'2px 0',display:'flex',justifyContent:'space-between'}}>
                      <span>{item}</span><span style={{color:'var(--text-muted)'}}>........... {i+3}</span>
                    </div>
                  ))}
                </div>

                {/* 1. PURPOSE & POLICY STATEMENT */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>1. PURPOSE & POLICY STATEMENT</h2>
                <p><strong>1.1</strong> This policy document is established to define the organizational framework, operational procedures, and governance controls for <strong>{docView.title.toLowerCase()}</strong> within the <strong>{sector}</strong> sector operations of the Organization.</p>
                {['HR','SAFETY'].includes(docView.category) ? (<>
                <p><strong>1.2</strong> The Organization is committed to fostering a safe, equitable, and legally compliant workplace environment. This policy has been developed in compliance with <strong>{docView.dpdpSection}</strong> and applicable employment legislations including the Shops & Establishments Act, Industrial Disputes Act 1947, Payment of Wages Act 1936, and the Sexual Harassment of Women at Workplace (Prevention, Prohibition and Redressal) Act, 2013.</p>
                <p><strong>1.3</strong> This policy is aligned with best practices recommended by the International Labour Organization (ILO), ISO 45001:2018 (Occupational Health & Safety), and the National Human Rights Commission guidelines for workplace dignity and non-discrimination.</p>
                <p><strong>1.4</strong> The Managing Director / CEO has approved this policy following recommendations from the HR Head, Legal & Compliance, and the Internal Complaints Committee (ICC), with periodic oversight by the Board of Directors.</p>
                </>) : ['GOVERNANCE','COMMUNICATION'].includes(docView.category) ? (<>
                <p><strong>1.2</strong> The Organization is committed to the highest standards of corporate governance, transparency, and ethical business conduct. This policy has been framed in accordance with <strong>{docView.dpdpSection}</strong>, and is aligned with the Companies Act 2013, SEBI (Listing Obligations and Disclosure Requirements) Regulations 2015, and applicable Corporate Governance codes.</p>
                <p><strong>1.3</strong> This policy is further aligned with the OECD Principles of Corporate Governance, the Kotak Committee recommendations, and ISO 37001:2016 (Anti-Bribery Management Systems) where applicable.</p>
                <p><strong>1.4</strong> The Board of Directors has approved this policy upon recommendation of the Nomination & Remuneration Committee, Audit Committee, and the Company Secretary, with annual review by the Independent Directors.</p>
                </>) : ['FINANCE','LEGAL'].includes(docView.category) ? (<>
                <p><strong>1.2</strong> The Organization is committed to maintaining robust financial controls, regulatory compliance, and fiscal accountability. This policy has been developed in accordance with <strong>{docView.dpdpSection}</strong>, Indian Accounting Standards (Ind-AS), the Companies Act 2013, Income Tax Act 1961, CGST Act 2017, and applicable financial regulations.</p>
                <p><strong>1.3</strong> This policy is aligned with COSO Internal Control Framework, ISO 37301:2021 (Compliance Management Systems), and the Standards on Auditing (SA) issued by the Institute of Chartered Accountants of India (ICAI).</p>
                <p><strong>1.4</strong> The CFO / Finance Director has approved this policy following review by the Statutory Auditors, Internal Audit, and the Audit Committee of the Board, ensuring compliance with applicable financial reporting standards.</p>
                </>) : ['IT_SECURITY'].includes(docView.category) ? (<>
                <p><strong>1.2</strong> The Organization is committed to safeguarding information assets, maintaining system integrity, and ensuring uninterrupted business operations. This policy has been developed in accordance with <strong>{docView.dpdpSection}</strong>, the Information Technology Act 2000, CERT-IN Directions 2022, and applicable cybersecurity frameworks.</p>
                <p><strong>1.3</strong> This policy is aligned with ISO/IEC 27001:2022 (ISMS), NIST Cybersecurity Framework (CSF) 2.0, CIS Controls v8, and the DPDP Act 2023 security obligations under Section 8(4).</p>
                <p><strong>1.4</strong> The CISO has approved this policy following recommendations from the IT Security Governance Board, Data Protection Officer (DPO), and periodic review by the Board-level Risk Committee.</p>
                </>) : ['OPERATIONS'].includes(docView.category) ? (<>
                <p><strong>1.2</strong> The Organization is committed to operational excellence, quality assurance, and continual improvement across all business processes. This policy has been developed in accordance with <strong>{docView.dpdpSection}</strong> and is aligned with applicable operational regulations and industry best practices for the <strong>{sector}</strong> sector.</p>
                <p><strong>1.3</strong> This policy is aligned with ISO 9001:2015 (Quality Management), ISO 14001:2015 (Environmental Management), ISO 22301:2019 (Business Continuity), and PMI PMBOK / PRINCE2 project management standards where applicable.</p>
                <p><strong>1.4</strong> The COO / Operations Head has approved this policy following review by the Quality Management Representative, EHS Officer, and the Management Review Board, with Board-level oversight for material operational risks.</p>
                </>) : (<>
                <p><strong>1.2</strong> The Organization, acting as a Data Fiduciary under the Digital Personal Data Protection Act, 2023 ("<strong>DPDP Act</strong>"), is committed to ensuring that all processing of digital personal data is conducted in a lawful, fair, and transparent manner, adhering to the principles of purpose limitation, data minimization, storage limitation, and accountability.</p>
                <p><strong>1.3</strong> This policy has been developed in accordance with {docView.dpdpSection} of the DPDP Act 2023, and is aligned with ISO/IEC 27001:2022 (Information Security Management System), ISO/IEC 27701:2019 (Privacy Information Management System), and applicable sectoral regulations governing the <strong>{sector}</strong> industry in India.</p>
                <p><strong>1.4</strong> The Board of Directors / Data Fiduciary has approved this policy following recommendations from the Data Protection Officer (DPO), Chief Information Security Officer (CISO), and Legal & Compliance Advisory Council.</p>
                </>)}

                {/* 2. DEFINITIONS */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>2. DEFINITIONS</h2>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:12,marginTop:8}}>
                  <thead>
                    <tr style={{background:'#4C1D95',color:'#fff'}}>
                      <th style={{padding:'6px 10px',textAlign:'left',width:'30%'}}>Term</th>
                      <th style={{padding:'6px 10px',textAlign:'left'}}>Definition</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(docView.category === 'HR' || docView.category === 'SAFETY' ? [
                      ['Employee','Any person employed by the Organization on permanent, contractual, temporary, or probationary basis, including consultants and interns where applicable'],
                      ['Employer','The Organization and its authorized representatives who exercise management control and direction over employees'],
                      ['ICC','Internal Complaints Committee — constituted under Section 4 of the POSH Act 2013, comprising a Presiding Officer, two internal members, and one external member'],
                      ['Sexual Harassment','Unwelcome acts or behavior including physical contact, demand for sexual favours, sexually coloured remarks, showing pornography, or any other unwelcome physical, verbal or non-verbal conduct of sexual nature (POSH Act Sec 2(n))'],
                      ['Standing Orders','Rules governing conditions of employment under the Industrial Employment (Standing Orders) Act, 1946, including classification, discipline, and termination procedures'],
                      ['Grievance','Any complaint or dissatisfaction expressed by an employee relating to terms of employment, working conditions, or management decisions'],
                      ['Disciplinary Action','Formal corrective action taken against an employee for proven misconduct, ranging from warning to termination, following due process'],
                      ['Probation Period','Initial period of employment (typically 3-6 months) during which performance and suitability are assessed before confirmation'],
                      ['Notice Period','Contractually agreed period of notice required to be served by either party before separation, as specified in the appointment letter'],
                      ['Full & Final Settlement','Complete settlement of all dues (salary, leave encashment, gratuity, reimbursements) payable to or recoverable from a separating employee'],
                    ] : docView.category === 'GOVERNANCE' || docView.category === 'COMMUNICATION' ? [
                      ['Board of Directors','The collective body of Directors elected/appointed to govern the Company as per Companies Act 2013 Sec 149-170'],
                      ['Independent Director','A non-executive director meeting the criteria under Section 149(6) of the Companies Act 2013, free from material relationships with the Company'],
                      ['Key Managerial Personnel (KMP)','CEO/MD, Whole-time Director, CFO, and Company Secretary as defined under Section 2(51) of the Companies Act 2013'],
                      ['Related Party Transaction','A transaction between the Company and a related party as defined under Section 2(76), subject to approval under Section 188'],
                      ['Conflict of Interest','A situation where an individual\'s personal, financial, or other interests could interfere with their duty to act in the best interest of the Organization'],
                      ['Whistleblower','Any Director, employee, or stakeholder who reports concerns about unethical behavior, fraud, or violations through the designated vigil mechanism'],
                      ['Code of Conduct','The Organization\'s formal statement of values, ethics, and behavioral expectations applicable to all directors, officers, and employees'],
                      ['CSR','Corporate Social Responsibility — obligation under Section 135 of Companies Act 2013 for eligible companies to spend 2% of average net profits on CSR activities'],
                      ['ESG','Environmental, Social, and Governance — framework for measuring sustainability and societal impact of business operations'],
                      ['BRSR','Business Responsibility and Sustainability Report — mandatory disclosure for top 1000 listed companies under SEBI LODR'],
                    ] : docView.category === 'FINANCE' || docView.category === 'LEGAL' ? [
                      ['Internal Controls','Processes designed and implemented by management to provide reasonable assurance regarding reliability of financial reporting, effectiveness of operations, and compliance with laws'],
                      ['AML','Anti-Money Laundering — measures to prevent, detect, and report money laundering activities as mandated by the Prevention of Money Laundering Act (PMLA) 2002'],
                      ['CDD','Customer Due Diligence — process of verifying the identity of customers and assessing risk levels as per PMLA and RBI KYC guidelines'],
                      ['STR','Suspicious Transaction Report — report filed with FIU-IND when transactions are suspected of involving proceeds of crime or terrorist financing'],
                      ['Three-Way Match','Procurement control comparing Purchase Order, Goods Receipt Note (GRN), and Vendor Invoice before payment authorization'],
                      ['GFR','General Financial Rules — rules issued by the Government of India governing financial management, procurement, and expenditure in government/PSU organizations'],
                      ['TDS/TCS','Tax Deducted/Collected at Source — statutory obligation to deduct/collect tax at prescribed rates under the Income Tax Act 1961'],
                      ['Transfer Pricing','Pricing of transactions between related parties/associated enterprises, governed by Sections 92-92F of the Income Tax Act and OECD Transfer Pricing Guidelines'],
                      ['Material Litigation','Any legal proceeding that could have a significant financial or reputational impact on the Organization, requiring Board-level disclosure'],
                      ['Legal Hold','A directive to preserve all forms of relevant documents and records when litigation is reasonably anticipated or pending'],
                    ] : docView.category === 'IT_SECURITY' ? [
                      ['ISMS','Information Security Management System — framework of policies and procedures for managing sensitive data, aligned with ISO/IEC 27001:2022'],
                      ['RBAC','Role-Based Access Control — access control mechanism restricting system access based on the user\'s role within the Organization'],
                      ['MFA','Multi-Factor Authentication — security mechanism requiring two or more verification factors (knowledge, possession, inherence) for access'],
                      ['VAPT','Vulnerability Assessment and Penetration Testing — systematic evaluation of IT infrastructure to identify and remediate security weaknesses'],
                      ['BCP/DR','Business Continuity Plan / Disaster Recovery — documented procedures for maintaining business functions / recovering IT systems during disruptions'],
                      ['RPO/RTO','Recovery Point Objective / Recovery Time Objective — maximum acceptable data loss period / maximum tolerable downtime for critical systems'],
                      ['SOC','Security Operations Center — centralized facility for monitoring, detecting, and responding to cybersecurity incidents 24/7'],
                      ['Zero Trust','Security model based on "never trust, always verify" — no implicit trust regardless of network location or previous authentication'],
                      ['CVE','Common Vulnerabilities and Exposures — standardized identifiers for publicly known cybersecurity vulnerabilities'],
                      ['CERT-IN','Indian Computer Emergency Response Team — national agency for incident response under Section 70B of IT Act 2000'],
                    ] : docView.category === 'OPERATIONS' ? [
                      ['QMS','Quality Management System — formalized system documenting processes, procedures, and responsibilities for achieving quality policies and objectives (ISO 9001)'],
                      ['BIA','Business Impact Analysis — process of analyzing business functions and the effect that a disruption might have on them'],
                      ['CAPA','Corrective and Preventive Action — systematic approach to investigating root causes of nonconformities and implementing corrective/preventive measures'],
                      ['NCR','Nonconformity Report — documented record of a process or product that does not meet specified requirements'],
                      ['EHS','Environment, Health & Safety — integrated management approach covering environmental protection, occupational health, and workplace safety'],
                      ['SLA','Service Level Agreement — formal commitment between service provider and internal/external customer defining quality and performance metrics'],
                      ['RFC','Request for Change — formalized proposal for change to IT systems, processes, or infrastructure, subject to Change Advisory Board review'],
                      ['KPI','Key Performance Indicator — measurable value demonstrating how effectively objectives are being achieved'],
                      ['Management Review','Periodic review by top management of the management system to ensure its continuing suitability, adequacy, effectiveness, and alignment with strategic direction'],
                      ['Continual Improvement','Recurring activity to enhance performance through quality objectives, audit findings, data analysis, management review, and corrective actions'],
                    ] : [
                      ['Data Fiduciary','Any person who alone or in conjunction with other persons determines the purpose and means of processing of personal data (DPDP Act Sec 2(i))'],
                      ['Data Principal','The individual to whom the personal data relates (DPDP Act Sec 2(j))'],
                      ['Personal Data','Any data about an individual who is identifiable by or in relation to such data (DPDP Act Sec 2(t))'],
                      ['Data Processor','Any person who processes personal data on behalf of a Data Fiduciary (DPDP Act Sec 2(k))'],
                      ['Consent','Free, specific, informed, unconditional, and unambiguous indication of the Data Principal\'s wishes by a clear affirmative action (DPDP Act Sec 6)'],
                      ['Data Protection Board','The Data Protection Board of India (DPBI) established under Section 15 of the DPDP Act'],
                      ['Significant Data Fiduciary','A Data Fiduciary notified as such by the Central Government under Section 10 based on volume, sensitivity, risk factors'],
                      ['DPO','Data Protection Officer — appointed under Section 10(2) by Significant Data Fiduciaries'],
                      ['DPIA','Data Protection Impact Assessment — systematic process to evaluate processing operations for privacy risks'],
                      ['Processing','Any operation or set of operations performed on digital personal data, including collection, recording, organization, structuring, storage, adaptation, retrieval, use, alignment, combination, indexing, sharing, disclosure, restriction, erasure or destruction'],
                    ]).map(([term,def]: any,i: number) => (
                      <tr key={i} style={{borderBottom:'1px solid var(--border)',background:i%2===0?'var(--bg-glass)':'transparent'}}>
                        <td style={{padding:'6px 10px',fontWeight:600}}>{term}</td>
                        <td style={{padding:'6px 10px',fontSize:11}}>{def}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {/* 3. SCOPE & APPLICABILITY */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>3. SCOPE & APPLICABILITY</h2>
                <p><strong>3.1 Organizational Scope:</strong> This policy applies to all business units, departments, subsidiaries, joint ventures, and affiliated entities of the Organization operating within the <strong>{sector}</strong> sector.</p>
                <p><strong>3.2 Personnel Scope:</strong> All employees (permanent, contractual, temporary), consultants, interns, vendors, third-party service providers, and Data Processors engaged by the Organization shall comply with this policy.</p>
                <p><strong>3.3 Data Scope:</strong> This policy covers all digital personal data as defined under Section 2(t) of the DPDP Act, including but not limited to: customer/client data, employee data, vendor data, partner data, and any data processed through automated means.</p>
                <p><strong>3.4 Geographic Scope:</strong> This policy applies to all operations within the territory of India, and to cross-border data transfers subject to Section 16 of the DPDP Act and applicable notifications by the Central Government.</p>
                <p><strong>3.5 Exclusions:</strong> This policy does not cover: (a) data processed by individuals for personal or domestic purposes; (b) data made publicly available by the Data Principal or under legal obligation; (c) specific exemptions granted under Section 17 of the DPDP Act.</p>

                {/* 4. ROLES & RESPONSIBILITIES — RACI */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>4. ROLES & RESPONSIBILITIES (RACI MATRIX)</h2>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:11,marginTop:8}}>
                  <thead>
                    <tr style={{background:'#4C1D95',color:'#fff'}}>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Activity</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>Board / DF</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>DPO / CDPO</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>CISO</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>Legal</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>Business Owner</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>IT</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[
                      ['Policy Approval', 'A','R','C','C','I','I'],
                      ['Implementation', 'I','A','R','C','R','R'],
                      ['Compliance Monitoring','I','R','R','C','A','C'],
                      ['Incident Response','I','A','R','R','C','R'],
                      ['Audit & Review','A','R','C','R','I','C'],
                      ['Training & Awareness','I','A','C','C','R','C'],
                      ['Regulatory Reporting','I','R','C','A','I','C'],
                      ['Data Subject Requests','I','A','C','R','R','R'],
                      ['Risk Assessment','I','R','A','C','R','R'],
                      ['Vendor Management','I','C','R','R','A','R'],
                    ].map(([act,...roles],i) => (
                      <tr key={i} style={{borderBottom:'1px solid var(--border)',background:i%2===0?'var(--bg-glass)':'transparent'}}>
                        <td style={{padding:'4px 8px',fontWeight:600}}>{act}</td>
                        {roles.map((r,j)=><td key={j} style={{padding:'4px 8px',textAlign:'center',fontWeight:700,color:r==='A'?'#DC2626':r==='R'?'#059669':r==='C'?'#2563EB':'#9CA3AF'}}>{r}</td>)}
                      </tr>
                    ))}
                  </tbody>
                </table>
                <p style={{fontSize:10,color:'var(--text-muted)',fontStyle:'italic',margin:'4px 0 0'}}>R = Responsible | A = Accountable | C = Consulted | I = Informed</p>

                {/* 5. POLICY REQUIREMENTS */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>5. POLICY REQUIREMENTS</h2>
                <p><strong>5.1 Lawful Basis for Processing:</strong> The Organization shall establish and document a lawful basis for each category of personal data processing. Under the DPDP Act 2023, the primary lawful bases are: (a) Consent of the Data Principal (Section 6); or (b) Certain Legitimate Uses as prescribed (Section 7).</p>
                <p><strong>5.2 Purpose Limitation:</strong> Personal data shall be processed only for the specific, clear, and lawful purpose for which consent was obtained or the legitimate use was identified. Any new purpose requires fresh consent or a documented legitimate use assessment.</p>
                <p><strong>5.3 Data Minimization:</strong> The Organization shall collect only the minimum personal data necessary to fulfil the stated processing purpose. Data collection forms, APIs, and interfaces shall be reviewed quarterly to ensure adherence to this principle.</p>
                <p><strong>5.4 Accuracy & Currency:</strong> The Organization shall take reasonable steps to ensure that personal data is accurate, complete, and up-to-date. Data quality processes shall include validation at point of entry, periodic data quality audits, and mechanisms for Data Principals to update their data.</p>
                <p><strong>5.5 Storage Limitation:</strong> Personal data shall be retained only for the period necessary to fulfil the processing purpose, or as required by applicable law/regulation. Upon expiry of the retention period, data shall be securely erased or anonymized using approved methods (e.g., NIST SP 800-88 guidelines).</p>
                <p><strong>5.6 Security Safeguards:</strong> The Organization shall implement reasonable security safeguards as required under Section 8(4) of the DPDP Act, including but not limited to: encryption (AES-256 / PQC ML-KEM-1024), access controls (RBAC/ABAC), audit logging, intrusion detection, and regular vulnerability assessments.</p>

                {/* 6. DETAILED PROCEDURES */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>6. DETAILED PROCEDURES & OPERATING GUIDELINES</h2>
                {(docView.controls || ['Control 1']).map((control:string, idx:number) => (
                  <div key={idx} style={{marginBottom:16,padding:'12px 16px',background:'var(--bg-glass)',border:'1px solid var(--border)',borderRadius:8,borderLeft:'4px solid #4C1D95'}}>
                    <div style={{fontWeight:700,fontSize:13,color:'#4C1D95',marginBottom:6}}>6.{idx+1} Procedure: {control}</div>
                    <div style={{fontSize:12}}>
                      <p style={{margin:'4px 0'}}><strong>6.{idx+1}.1 Objective:</strong> To implement and operationalize the control requirements for "{control}" in alignment with the regulatory obligations specified under {docView.dpdpSection}.</p>
                      <p style={{margin:'4px 0'}}><strong>6.{idx+1}.2 Process Steps:</strong></p>
                      <ol style={{paddingLeft:20,margin:'4px 0'}}>
                        <li>Initiate the process by identifying all data subjects and data categories impacted by this control.</li>
                        <li>Conduct a risk assessment to determine the appropriate level of safeguards required (refer Section 8 — Risk Assessment).</li>
                        <li>Implement technical and organizational measures as documented in the controls register.</li>
                        <li>Configure monitoring systems to detect and alert on any deviations or non-compliance.</li>
                        <li>Document implementation evidence and obtain sign-off from the designated Control Owner.</li>
                        <li>Conduct periodic effectiveness testing (frequency as per risk classification) and report results to DPO.</li>
                      </ol>
                      <p style={{margin:'4px 0'}}><strong>6.{idx+1}.3 Frequency:</strong> {idx%3===0?'Daily monitoring with weekly review':'Monthly review with quarterly audit'}</p>
                      <p style={{margin:'4px 0'}}><strong>6.{idx+1}.4 Escalation:</strong> Deviations shall be escalated to the DPO within 24 hours. Critical failures shall be escalated to the Board within 4 hours.</p>
                    </div>
                  </div>
                ))}

                {/* 7. CONTROLS & SAFEGUARDS */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>7. CONTROLS & SAFEGUARDS</h2>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:11,marginTop:8}}>
                  <thead>
                    <tr style={{background:'#4C1D95',color:'#fff'}}>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>S.No.</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Control</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Type</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>ISO 27001 Ref</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(docView.controls || []).map((c:string,i:number) => (
                      <tr key={i} style={{borderBottom:'1px solid var(--border)',background:i%2===0?'var(--bg-glass)':'transparent'}}>
                        <td style={{padding:'4px 8px'}}>{i+1}</td>
                        <td style={{padding:'4px 8px',fontWeight:600}}>{c}</td>
                        <td style={{padding:'4px 8px'}}>{i%3===0?'Preventive':i%3===1?'Detective':'Corrective'}</td>
                        <td style={{padding:'4px 8px'}}>A.{5+i}.{1+i%4}.{i+1}</td>
                        <td style={{padding:'4px 8px'}}><span style={{padding:'2px 8px',borderRadius:4,fontSize:10,fontWeight:600,background:i%4===3?'rgba(234,88,12,0.2)':'rgba(5,150,105,0.2)',color:i%4===3?'#EA580C':'#059669'}}>{i%4===3?'In Progress':'Implemented'}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {/* 8. RISK ASSESSMENT & CLASSIFICATION */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>8. RISK ASSESSMENT & CLASSIFICATION</h2>
                <p><strong>8.1</strong> Risks associated with this policy shall be assessed using the Organization's Risk Assessment Framework, aligned with ISO 31000:2018{docView.category === 'IT_SECURITY' ? ' and ISO 27005:2022' : docView.category === 'OPERATIONS' ? ' and ISO 9001:2015 risk-based thinking' : docView.category === 'HR' ? ' and employment law compliance risk frameworks' : docView.category === 'FINANCE' ? ' and COSO Internal Control Framework' : ' and ISO 27005:2022'}.</p>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:11,marginTop:12,marginBottom:8}}>
                  <thead>
                    <tr style={{background:'#4C1D95',color:'#fff'}}>
                      <th style={{padding:'6px 8px'}}>Risk Category</th>
                      <th style={{padding:'6px 8px'}}>Impact</th>
                      <th style={{padding:'6px 8px'}}>Likelihood</th>
                      <th style={{padding:'6px 8px'}}>Risk Rating</th>
                      <th style={{padding:'6px 8px'}}>Mitigation</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(docView.category === 'HR' || docView.category === 'SAFETY' ? [
                      ['POSH non-compliance','Critical (5)','Medium (3)','HIGH (15)','ICC constitution, annual awareness training, complaint mechanism'],
                      ['Wrongful termination litigation','High (4)','Medium (3)','HIGH (12)','Due process compliance, documentation, legal review'],
                      ['Workplace safety incident','Critical (5)','Low (2)','MEDIUM (10)','Safety audits, PPE enforcement, emergency drills'],
                      ['Employee data breach','High (4)','Medium (3)','HIGH (12)','Access controls, HR system encryption, audit trail'],
                      ['Labour law non-compliance','Medium (3)','High (4)','HIGH (12)','Compliance calendar, statutory audit, legal updates tracking'],
                    ] : docView.category === 'GOVERNANCE' || docView.category === 'COMMUNICATION' ? [
                      ['Board governance failure','Critical (5)','Low (2)','MEDIUM (10)','Annual board evaluation, independent director oversight'],
                      ['Conflict of interest exposure','High (4)','Medium (3)','HIGH (12)','Annual disclosure, related party monitoring, gift register'],
                      ['Bribery / corruption incident','Critical (5)','Low (2)','MEDIUM (10)','Anti-bribery training, due diligence, whistleblower mechanism'],
                      ['ESG / CSR non-compliance','Medium (3)','Medium (3)','MEDIUM (9)','ESG committee, BRSR alignment, CSR monitoring'],
                      ['Reputational damage','High (4)','Medium (3)','HIGH (12)','Crisis communication plan, media protocol, brand monitoring'],
                    ] : docView.category === 'FINANCE' || docView.category === 'LEGAL' ? [
                      ['Financial misstatement','Critical (5)','Low (2)','MEDIUM (10)','Internal audit, SOX controls, management certification'],
                      ['Tax non-compliance penalty','High (4)','Medium (3)','HIGH (12)','Tax compliance calendar, quarterly review, expert advisory'],
                      ['Procurement fraud','High (4)','Medium (3)','HIGH (12)','Three-way match, segregation of duties, vendor audit'],
                      ['AML / PMLA violation','Critical (5)','Low (2)','MEDIUM (10)','CDD/EDD processes, STR filing, employee training'],
                      ['Contract / litigation exposure','Medium (3)','High (4)','HIGH (12)','Legal review mandate, contract register, insurance'],
                    ] : docView.category === 'IT_SECURITY' ? [
                      ['Ransomware / cyberattack','Critical (5)','Medium (3)','HIGH (15)','EDR/XDR, network segmentation, backup strategy, IR plan'],
                      ['Data breach / exfiltration','Critical (5)','Medium (3)','HIGH (15)','DLP, encryption (AES-256/PQC), access controls, SOC monitoring'],
                      ['System downtime (>RTO)','High (4)','Medium (3)','HIGH (12)','DR drill annual, cloud failover, BCP testing'],
                      ['Patch management failure','High (4)','High (4)','HIGH (16)','Automated patching, 72h critical patch SLA, VAPT quarterly'],
                      ['Insider threat','High (4)','Medium (3)','HIGH (12)','UEBA, privileged access monitoring, separation of duties'],
                    ] : docView.category === 'OPERATIONS' ? [
                      ['Quality non-conformity','High (4)','Medium (3)','HIGH (12)','QMS audit cycle, CAPA tracking, management review'],
                      ['Business continuity disruption','Critical (5)','Low (2)','MEDIUM (10)','BIA-based BCP, annual drill, crisis management team'],
                      ['Environmental compliance breach','High (4)','Low (2)','MEDIUM (8)','EMS monitoring, SPCB returns, emission tracking'],
                      ['Occupational safety incident','Critical (5)','Medium (3)','HIGH (15)','Hazard identification, safety training, incident investigation'],
                      ['Customer complaint escalation','Medium (3)','High (4)','HIGH (12)','SLA monitoring, RCA process, feedback loop'],
                    ] : [
                      ['Non-compliance with DPDP Act','Critical (5)','Medium (3)','HIGH (15)','Automated compliance monitoring, quarterly DPIA'],
                      ['Unauthorized data access','High (4)','Medium (3)','HIGH (12)','RBAC/ABAC, MFA, audit logging, SOC monitoring'],
                      ['Data breach / exfiltration','Critical (5)','Low (2)','MEDIUM (10)','DLP, encryption, network segmentation, incident response'],
                      ['Inadequate consent management','High (4)','Medium (3)','HIGH (12)','Consent management platform, granular consent, audit trail'],
                      ['Vendor / third-party risk','Medium (3)','High (4)','HIGH (12)','Vendor assessment, DPA contracts, annual audit rights'],
                    ]).map(([cat,imp,lik,rate,mit]: any,i: number) => (
                      <tr key={i} style={{borderBottom:'1px solid var(--border)',background:i%2===0?'var(--bg-glass)':'transparent'}}>
                        <td style={{padding:'4px 8px',fontWeight:600}}>{cat}</td>
                        <td style={{padding:'4px 8px'}}>{imp}</td>
                        <td style={{padding:'4px 8px'}}>{lik}</td>
                        <td style={{padding:'4px 8px',fontWeight:700,color:rate.includes('HIGH')?'#DC2626':'#EA580C'}}>{rate}</td>
                        <td style={{padding:'4px 8px',fontSize:10}}>{mit}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {/* 9. REGULATORY MAPPING */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>9. REGULATORY MAPPING & COMPLIANCE REQUIREMENTS</h2>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:11,marginTop:8}}>
                  <thead>
                    <tr style={{background:'#4C1D95',color:'#fff'}}>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Regulation / Standard</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Section / Clause</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Requirement</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>Compliance</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[
                      ['DPDP Act 2023',docView.dpdpSection,'Compliance with stated data protection obligations','✅'],
                      ['IT Act 2000','Section 43A','Reasonable security practices & procedures','✅'],
                      ['IT (SPDI Rules) 2011','Rule 4-8','Sensitive personal data handling & consent','✅'],
                      ['CERT-IN Directions 2022','Para 3-6','6-hour incident reporting, 180-day log retention','✅'],
                      ['ISO/IEC 27001:2022','Clause 4-10, Annex A','ISMS requirements & controls','✅'],
                      ['ISO/IEC 27701:2019','Annex A/B','PII Controller/Processor requirements','✅'],
                      [`${sector} Sectoral Regulation`,docView.dpdpSection,'Sector-specific compliance requirements','⚠️'],
                    ].map(([reg,sec,req,st],i) => (
                      <tr key={i} style={{borderBottom:'1px solid var(--border)',background:i%2===0?'var(--bg-glass)':'transparent'}}>
                        <td style={{padding:'4px 8px',fontWeight:600}}>{reg}</td>
                        <td style={{padding:'4px 8px'}}>{sec}</td>
                        <td style={{padding:'4px 8px',fontSize:10}}>{req}</td>
                        <td style={{padding:'4px 8px',textAlign:'center'}}>{st}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {/* 10. ENFORCEMENT, PENALTIES & EXCEPTIONS */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>10. ENFORCEMENT, PENALTIES & EXCEPTIONS</h2>
                <p><strong>10.1 Internal Enforcement:</strong> Violation of this policy may result in disciplinary action including verbal/written warning, suspension, termination of employment/contract, and/or civil/criminal proceedings as applicable.</p>
                <p><strong>10.2 Regulatory Penalties (DPDP Act 2023):</strong></p>
                <ul style={{paddingLeft:24,fontSize:12}}>
                  <li>Failure to implement security safeguards leading to breach: up to <strong>₹250 Crore</strong></li>
                  <li>Failure to notify breach to Board and Data Principal: up to <strong>₹200 Crore</strong></li>
                  <li>Non-compliance with children's data provisions: up to <strong>₹200 Crore</strong></li>
                  <li>Non-compliance with Data Fiduciary obligations: up to <strong>₹150 Crore</strong></li>
                </ul>
                <p><strong>10.3 Exceptions:</strong> Any exception to this policy must be approved by the DPO and documented in the Exception Register with: (a) business justification; (b) risk assessment; (c) compensating controls; (d) expiry date (maximum 6 months, renewable).</p>

                {/* 11. TRAINING & AWARENESS */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>11. TRAINING & AWARENESS</h2>
                <p><strong>11.1</strong> All personnel within scope shall complete mandatory data protection training within 30 days of joining and annually thereafter.</p>
                <p><strong>11.2</strong> Role-based training shall be provided to personnel handling sensitive categories of personal data, with specialized modules for: DPO team, IT Security, Legal & Compliance, HR, Customer-facing roles.</p>
                <p><strong>11.3</strong> Training effectiveness shall be assessed through post-training evaluations (minimum passing score: 80%). Records shall be maintained for audit purposes.</p>

                {/* 12. MONITORING, METRICS & REPORTING */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>12. MONITORING, METRICS & REPORTING</h2>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:11,marginTop:8}}>
                  <thead>
                    <tr style={{background:'#4C1D95',color:'#fff'}}>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>KPI / Metric</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>Target</th>
                      <th style={{padding:'6px 8px',textAlign:'center'}}>Frequency</th>
                      <th style={{padding:'6px 8px',textAlign:'left'}}>Reported To</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[
                      ['Policy Compliance Score','≥ 90%','Quarterly','Board / Audit Committee'],
                      ['Control Implementation Rate','100%','Monthly','CISO / DPO'],
                      ['Data Principal Request SLA (30 days)','100% within SLA','Per request','DPO'],
                      ['Breach Notification SLA (72h DPBI)','100%','Per incident','Board / DPO'],
                      ['Training Completion Rate','100%','Annual','HR / DPO'],
                      ['DPIA Completion Rate','100% for high-risk processing','Quarterly','DPO'],
                      ['Audit Findings Closure Rate','≥ 95% within timeline','Quarterly','Internal Audit'],
                      ['Vendor DPA Coverage','100%','Semi-Annual','Procurement / Legal'],
                      ['Data Retention Compliance','100% adherence to schedule','Monthly','IT / DPO'],
                      ['Security Incident Response Time','≤ 6 hours (CERT-IN)','Per incident','CISO / SOC'],
                    ].map(([kpi,target,freq,report],i) => (
                      <tr key={i} style={{borderBottom:'1px solid var(--border)',background:i%2===0?'var(--bg-glass)':'transparent'}}>
                        <td style={{padding:'4px 8px',fontWeight:600}}>{kpi}</td>
                        <td style={{padding:'4px 8px',textAlign:'center',fontWeight:600,color:'#059669'}}>{target}</td>
                        <td style={{padding:'4px 8px',textAlign:'center'}}>{freq}</td>
                        <td style={{padding:'4px 8px'}}>{report}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {/* 13. REVIEW, AMENDMENT & DECOMMISSIONING */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>13. REVIEW, AMENDMENT & DECOMMISSIONING</h2>
                <p><strong>13.1 Scheduled Review:</strong> This policy shall be reviewed at least semi-annually or upon: (a) regulatory changes (DPDP Act rules/amendments); (b) significant organizational changes; (c) post-incident findings; (d) audit recommendations.</p>
                <p><strong>13.2 Amendment Process:</strong> Amendments shall follow the ISO Policy Lifecycle: DRAFT → REVIEW → APPROVED → PUBLISHED → ACTIVE. All amendments require DPO review and Board/DF approval before publication.</p>
                <p><strong>13.3 Decommissioning:</strong> Upon retirement, the policy shall be archived with full version history, and all dependent procedures/controls shall be transitioned to the superseding policy within 30 days.</p>

                {/* ANNEXURE A */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>ANNEXURE A: REGULATORY CROSS-REFERENCE</h2>
                <p style={{fontSize:11}}>This policy is developed in compliance with the following regulatory and standards framework:</p>
                <ul style={{fontSize:11,columns:2,columnGap:24,paddingLeft:16}}>
                  <li>Digital Personal Data Protection Act, 2023</li>
                  <li>DPDP Rules (as notified)</li>
                  <li>Information Technology Act, 2000</li>
                  <li>IT (SPDI Rules) 2011</li>
                  <li>IT (Intermediary Guidelines) Rules 2021</li>
                  <li>CERT-IN Directions, April 2022</li>
                  <li>ISO/IEC 27001:2022</li>
                  <li>ISO/IEC 27701:2019</li>
                  <li>ISO/IEC 27002:2022</li>
                  <li>ISO 22301:2019 (Business Continuity)</li>
                  <li>Indian Evidence Act, 1872 (Sec 65B)</li>
                  <li>Indian Penal Code — Sec 72A, 403, 405</li>
                  <li>{sector} — Applicable Sectoral Regulations</li>
                </ul>

                {/* ANNEXURE B — APPROVAL */}
                <h2 style={{fontSize:15,color:'#4C1D95',marginTop:28,borderBottom:'2px solid #4C1D95',paddingBottom:4}}>ANNEXURE B: APPROVAL & SIGNATURE BLOCK</h2>
                <table style={{width:'100%',borderCollapse:'collapse',fontSize:12,marginTop:8}}>
                  <thead>
                    <tr style={{background:'#4C1D95',color:'#fff'}}>
                      <th style={{padding:'6px 10px',textAlign:'left'}}>Role</th>
                      <th style={{padding:'6px 10px',textAlign:'left'}}>Name</th>
                      <th style={{padding:'6px 10px',textAlign:'center'}}>Signature</th>
                      <th style={{padding:'6px 10px',textAlign:'center'}}>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {[
                      ['Policy Author','Priya Sharma, Policy & Compliance Team'],
                      ['Reviewed By — DPO','Dr. Anand Krishnan, Data Protection Officer'],
                      ['Reviewed By — CISO','Vikram Mehta, Chief Information Security Officer'],
                      ['Reviewed By — Legal','Adv. Neha Reddy, Head of Legal & Compliance'],
                      ['Approved By — Data Fiduciary','Rajesh Iyer, Managing Director / CEO'],
                    ].map(([role,name],i) => (
                      <tr key={i} style={{borderBottom:'1px solid var(--border)'}}>
                        <td style={{padding:'8px 10px',fontWeight:600}}>{role}</td>
                        <td style={{padding:'8px 10px',fontStyle:'italic',color:'var(--text-muted)'}}>{name}</td>
                        <td style={{padding:'8px 10px',textAlign:'center'}}><div style={{borderBottom:'1px solid var(--text-muted)',width:'80%',margin:'0 auto',height:24}} /></td>
                        <td style={{padding:'8px 10px',textAlign:'center'}}><div style={{borderBottom:'1px solid var(--text-muted)',width:'80%',margin:'0 auto',height:24}} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                {/* Footer */}
                <div style={{textAlign:'center',fontSize:10,color:'var(--text-muted)',marginTop:32,paddingTop:12,borderTop:'2px solid #4C1D95'}}>
                  <div><strong>QS-DPDP Enterprise™ — Automated Policy Governance Engine</strong></div>
                  <div>Document ID: QS-POL-{docView.id} | Version 2.0 | Compliance Score: {docView.complianceScore}%</div>
                  <div>This document has been generated and managed through the QS-DPDP Enterprise Policy Lifecycle Management System.</div>
                  <div style={{marginTop:4}}>— END OF DOCUMENT —</div>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ═══ IMPORT/EXPORT TAB ═══ */}
      {tab==='import' && (
        <div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16}}>
            {/* Import */}
            <div className="glass-card">
              <h3>📥 Import Existing Policy</h3>
              <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:16}}>Upload a policy document (PDF/Word). The system will auto-extract and parameterize it into the solution's format with procedures, controls, and metrics.</p>
              <div style={{border:'2px dashed var(--border)',borderRadius:'var(--radius)',padding:32,textAlign:'center',cursor:'pointer',marginBottom:16}} onClick={()=>fileRef.current?.click()}>
                <input ref={fileRef} type="file" accept=".pdf,.doc,.docx" style={{display:'none'}} onChange={e=>setImportFile(e.target.files?.[0]?.name||'')} />
                <div style={{fontSize:32,marginBottom:8}}>📁</div>
                <div style={{fontSize:14,fontWeight:600}}>{importFile || 'Click or drag to upload PDF/Word document'}</div>
                <div style={{fontSize:12,color:'var(--text-muted)',marginTop:4}}>Supported: .pdf, .doc, .docx (Max 50MB)</div>
              </div>
              <button className="btn-primary" onClick={simulateImport} disabled={importProcessing} style={{width:'100%'}}>
                {importProcessing ? '⏳ Analyzing & Parameterizing...' : '🔍 Extract & Parameterize Policy'}
              </button>

              {importResult && (
                <div style={{marginTop:16,padding:16,background:'var(--green-bg)',border:'1px solid var(--green-border)',borderRadius:'var(--radius)'}}>
                  <h4 style={{color:'var(--green)'}}>✅ Policy Parameterized Successfully</h4>
                  <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8,marginTop:8,fontSize:13}}>
                    <div><strong>Title:</strong> {importResult.extracted.title}</div>
                    <div><strong>Sections:</strong> {importResult.extracted.sections}</div>
                    <div><strong>Procedures:</strong> {importResult.extracted.procedures}</div>
                    <div><strong>Controls:</strong> {importResult.extracted.controls}</div>
                    <div><strong>Metrics:</strong> {importResult.extracted.metrics}</div>
                    <div><strong>DPDP Mapped:</strong> {importResult.extracted.mappedDPDP.join(', ')}</div>
                  </div>
                  <div style={{display:'flex',gap:8,marginTop:12}}>
                    <button className="btn-primary" style={{fontSize:12}}>✅ Add to Policy Registry</button>
                    <button className="btn-secondary" style={{fontSize:12}}>📄 Preview Document</button>
                  </div>
                </div>
              )}
            </div>

            {/* Export */}
            <div className="glass-card">
              <h3>📤 Export Policies</h3>
              <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:16}}>Export policies in various formats</p>
              <div style={{display:'grid',gap:12}}>
                <button className="btn-secondary" style={{textAlign:'left',padding:16}}>
                  <strong>📄 Export All as PDF</strong>
                  <span style={{display:'block',fontSize:12,color:'var(--text-muted)'}}>Formatted policy documents with headers, letterhead, and signatures</span>
                </button>
                <button className="btn-secondary" style={{textAlign:'left',padding:16}}>
                  <strong>📝 Export All as Word (.docx)</strong>
                  <span style={{display:'block',fontSize:12,color:'var(--text-muted)'}}>Editable Word documents preserving ISO format</span>
                </button>
                <button className="btn-secondary" style={{textAlign:'left',padding:16}}>
                  <strong>📊 Export Compliance Matrix (Excel)</strong>
                  <span style={{display:'block',fontSize:12,color:'var(--text-muted)'}}>Policy-to-DPDP mapping with control scores</span>
                </button>
                <button className="btn-secondary" style={{textAlign:'left',padding:16}}>
                  <strong>🖨️ Print All Policies</strong>
                  <span style={{display:'block',fontSize:12,color:'var(--text-muted)'}}>Print-ready formatted output with page breaks</span>
                </button>
                <button className="btn-secondary" style={{textAlign:'left',padding:16}}>
                  <strong>📦 Export Policy Package (.zip)</strong>
                  <span style={{display:'block',fontSize:12,color:'var(--text-muted)'}}>All policies + procedures + controls + metrics in one archive</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* DETAIL DRAWER */}
      {selectedPolicy && tab==='registry' && (
        <div className="glass-card" style={{marginTop:16}}>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
            <h3>📄 {selectedPolicy.title}</h3>
            <button className="btn-sm" onClick={()=>setSelectedPolicy(null)}>✕ Close</button>
          </div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:16,marginTop:16}}>
            <div><strong>ID:</strong> {selectedPolicy.id}</div>
            <div><strong>Category:</strong> {selectedPolicy.category}</div>
            <div><strong>DPDP Section:</strong> {selectedPolicy.dpdpSection}</div>
            <div><strong>Status:</strong> <span style={{padding:'3px 10px',borderRadius:12,fontSize:11,fontWeight:700,color:'#fff',background:LIFECYCLE_COLORS[selectedPolicy.status]}}>{selectedPolicy.status}</span></div>
            <div><strong>Compliance:</strong> <span style={{fontWeight:700,color:selectedPolicy.complianceScore>=80?'var(--green)':'var(--amber)'}}>{selectedPolicy.complianceScore}%</span></div>
          </div>
          {selectedPolicy.controls?.length > 0 && (
            <div style={{marginTop:12}}><strong>Controls:</strong> <div style={{display:'flex',gap:6,flexWrap:'wrap',marginTop:6}}>{selectedPolicy.controls.map((c:string,i:number)=><span key={i} className="badge blue">{c}</span>)}</div></div>
          )}
          <div style={{display:'flex',gap:8,marginTop:16}}>
            <button className="btn-primary" onClick={()=>{viewAsDocument(selectedPolicy);setTab('document')}}>📄 View as Document</button>
            <button className="btn-secondary" onClick={()=>promotePolicy(selectedPolicy.id)}>▶ Promote Stage</button>
            <button className="btn-secondary">✏️ Edit</button>
            <button className="btn-secondary">📋 Clone</button>
            <button className="btn-secondary">📜 Version History</button>
          </div>
        </div>
      )}
    </div>
  )
}
