import { useState, useMemo, useEffect } from 'react'
import { generateRightsRequests, SECTORS } from '../data/sectorData'
import { useAppContext } from '../context/AppContext'
import { showExportDialog, exportToCSV, exportToExcel, exportToWord, exportToPDF, ExportColumn } from '../utils/exportUtils'

const RIGHTS_COLUMNS: ExportColumn[] = [
  {key:'id',label:'Request ID'},{key:'principal',label:'Data Principal'},{key:'type',label:'Type'},
  {key:'dpdpSection',label:'DPDP Section'},{key:'status',label:'Status'},
  {key:'submittedAt',label:'Submitted'},{key:'slaDeadline',label:'SLA Deadline'},
]

export default function RightsPage() {
  const { sector, setSector } = useAppContext()
  const [filterType, setFilterType] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [page, setPage] = useState(0)
  const [showCreate, setShowCreate] = useState(false)
  const [selected, setSelected] = useState<any>(null)
  const [form, setForm] = useState({principal:'',type:'ACCESS',dpdpSection:'Section 11'})

  const allRequests = useMemo(() => generateRightsRequests(sector, 200), [sector])
  const [requests, setRequests] = useState(allRequests)

  // Export event listener
  useEffect(() => {
    const handleExport = (e: Event) => {
      const format = (e as CustomEvent).detail?.format
      if (format === 'csv') exportToCSV(requests, RIGHTS_COLUMNS, `rights-requests-${sector}`)
      else if (format === 'excel') exportToExcel(requests, RIGHTS_COLUMNS, `rights-requests-${sector}`, `Rights Requests — ${sector}`)
      else if (format === 'word') exportToWord(requests, RIGHTS_COLUMNS, `rights-requests-${sector}`, `Data Subject Rights Report — ${sector}`, sector)
      else if (format === 'pdf') exportToPDF(requests, RIGHTS_COLUMNS, `rights-requests-${sector}`, `Rights Requests — ${sector}`, sector)
      else showExportDialog(requests, RIGHTS_COLUMNS, `rights-requests-${sector}`, `Rights Requests — ${sector}`, sector)
    }
    window.addEventListener('qs-export', handleExport)
    return () => window.removeEventListener('qs-export', handleExport)
  }, [requests, sector])

  const changeSector = (s: string) => { setSector(s); setRequests(generateRightsRequests(s, 200)); setPage(0) }

  const stats = useMemo(() => ({
    total: requests.length,
    pending: requests.filter(r=>r.status==='PENDING').length,
    inProgress: requests.filter(r=>r.status==='IN_PROGRESS').length,
    completed: requests.filter(r=>r.status==='COMPLETED').length,
    access: requests.filter(r=>r.type==='ACCESS').length,
    correction: requests.filter(r=>r.type==='CORRECTION').length,
    erasure: requests.filter(r=>r.type==='ERASURE').length,
    grievance: requests.filter(r=>r.type==='GRIEVANCE').length,
  }), [requests])

  const filtered = useMemo(() => {
    let f = requests
    if (filterType) f = f.filter(r => r.type === filterType)
    if (filterStatus) f = f.filter(r => r.status === filterStatus)
    return f
  }, [requests, filterType, filterStatus])

  const PAGE_SIZE = 20
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page*PAGE_SIZE,(page+1)*PAGE_SIZE)

  const slaColor = (deadline: string) => {
    const days = Math.floor((new Date(deadline).getTime() - Date.now()) / 86400000)
    return days < 0 ? 'var(--red)' : days < 7 ? 'var(--amber)' : 'var(--green)'
  }

  const handleCreate = () => {
    const now = new Date()
    setRequests([{
      id:`RR-${String(requests.length+1).padStart(4,'0')}`,
      ...form,status:'PENDING',sector,
      submittedAt:now.toISOString().split('T')[0],
      slaDeadline:new Date(now.getTime()+30*86400000).toISOString().split('T')[0],
    },...requests])
    setShowCreate(false)
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div><h1>👤 Rights Management</h1><p>DPDP Act Sections 11-14 — Access, Correction, Erasure, Grievance with 30-day SLA</p></div>
        <button className="btn-primary" onClick={()=>setShowCreate(!showCreate)}>+ New Request</button>
      </div>

      <div style={{display:'flex',gap:12,marginBottom:16,alignItems:'center'}}>
        <select className="form-input" style={{width:220}} value={sector} onChange={e=>changeSector(e.target.value)}>
          {SECTORS.map(s=><option key={s}>{s}</option>)}
        </select>
      </div>

      <div className="kpi-grid">
        <div className="kpi-card"><div className="kpi-value">{stats.total}</div><div className="kpi-label">Total Requests</div></div>
        <div className="kpi-card amber"><div className="kpi-value" style={{color:'var(--amber)'}}>{stats.pending}</div><div className="kpi-label">Pending</div></div>
        <div className="kpi-card blue"><div className="kpi-value" style={{color:'var(--blue)'}}>{stats.inProgress}</div><div className="kpi-label">In Progress</div></div>
        <div className="kpi-card green"><div className="kpi-value" style={{color:'var(--green)'}}>{stats.completed}</div><div className="kpi-label">Completed</div></div>
        <div className="kpi-card"><div className="kpi-value">{stats.access}</div><div className="kpi-label">Access (Sec 11)</div></div>
        <div className="kpi-card"><div className="kpi-value">{stats.erasure}</div><div className="kpi-label">Erasure (Sec 13)</div></div>
      </div>

      {showCreate && (
        <div className="glass-card" style={{marginBottom:16}}>
          <h3>📝 New Rights Request</h3>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:12,marginTop:12}}>
            <input className="form-input" placeholder="Data Principal Name" value={form.principal} onChange={e=>setForm({...form,principal:e.target.value})} />
            <select className="form-input" value={form.type} onChange={e=>setForm({...form,type:e.target.value,dpdpSection:e.target.value==='ACCESS'?'Section 11':e.target.value==='CORRECTION'?'Section 12':e.target.value==='ERASURE'?'Section 13':'Section 14'})}>
              <option value="ACCESS">Right to Access (Section 11)</option>
              <option value="CORRECTION">Right to Correction (Section 12)</option>
              <option value="ERASURE">Right to Erasure (Section 13)</option>
              <option value="GRIEVANCE">Grievance Redressal (Section 14)</option>
              <option value="PORTABILITY">Data Portability</option>
              <option value="RESTRICT">Restrict Processing</option>
            </select>
            <div style={{display:'flex',gap:8}}>
              <button className="btn-primary" onClick={handleCreate}>Submit Request</button>
              <button className="btn-secondary" onClick={()=>setShowCreate(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      <div style={{display:'flex',gap:12,marginBottom:16}}>
        <select className="form-input" style={{width:160}} value={filterType} onChange={e=>{setFilterType(e.target.value);setPage(0)}}>
          <option value="">All Types</option><option>ACCESS</option><option>CORRECTION</option><option>ERASURE</option><option>GRIEVANCE</option><option>PORTABILITY</option>
        </select>
        <select className="form-input" style={{width:160}} value={filterStatus} onChange={e=>{setFilterStatus(e.target.value);setPage(0)}}>
          <option value="">All Status</option><option>PENDING</option><option>IN_PROGRESS</option><option>COMPLETED</option>
        </select>
        <span style={{fontSize:12,color:'var(--text-muted)',alignSelf:'center'}}>{filtered.length} requests</span>
      </div>

      <div className="glass-card">
        <h3>📋 Rights Requests ({filtered.length})</h3>
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>ID</th><th>Principal</th><th>Type</th><th>DPDP Section</th><th>Status</th><th>Submitted</th><th>SLA Deadline</th><th>Actions</th></tr></thead>
            <tbody>{paged.map(r=>(
              <tr key={r.id} onClick={()=>setSelected(r)} style={{cursor:'pointer'}}>
                <td style={{fontFamily:'monospace',fontSize:11}}>{r.id}</td>
                <td><strong>{r.principal}</strong></td>
                <td><span className="badge blue">{r.type}</span></td>
                <td style={{fontSize:12,color:'var(--brand-primary)'}}>{r.dpdpSection}</td>
                <td><span className={`rag-badge ${r.status==='COMPLETED'?'rag-green':r.status==='PENDING'?'rag-amber':'rag-red'}`}>{r.status}</span></td>
                <td style={{fontSize:12}}>{r.submittedAt}</td>
                <td style={{fontSize:12,fontWeight:600,color:slaColor(r.slaDeadline)}}>{r.slaDeadline}</td>
                <td><button className="btn-sm">View</button></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginTop:12}}>
          <span style={{fontSize:12,color:'var(--text-muted)'}}>Page {page+1} of {totalPages}</span>
          <div style={{display:'flex',gap:4}}>
            <button className="btn-sm" disabled={page===0} onClick={()=>setPage(p=>p-1)}>◀ Prev</button>
            <button className="btn-sm" disabled={page>=totalPages-1} onClick={()=>setPage(p=>p+1)}>Next ▶</button>
          </div>
        </div>
      </div>

      {selected && (
        <div className="glass-card" style={{marginTop:16}}>
          <div style={{display:'flex',justifyContent:'space-between'}}><h3>📄 Request — {selected.id}</h3><button className="btn-sm" onClick={()=>setSelected(null)}>✕</button></div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:16,marginTop:16}}>
            <div><strong>Principal:</strong><br/>{selected.principal}</div>
            <div><strong>Type:</strong><br/>{selected.type}</div>
            <div><strong>DPDP Section:</strong><br/>{selected.dpdpSection}</div>
            <div><strong>Status:</strong><br/>{selected.status}</div>
            <div><strong>SLA:</strong><br/><span style={{color:slaColor(selected.slaDeadline),fontWeight:700}}>{selected.slaDeadline}</span></div>
          </div>
          <div style={{display:'flex',gap:8,marginTop:16}}>
            <button className="btn-primary">✅ Complete Request</button>
            <button className="btn-secondary">📤 Export</button>
          </div>
        </div>
      )}
    </div>
  )
}
