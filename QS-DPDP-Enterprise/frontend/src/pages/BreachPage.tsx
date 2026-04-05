import { useState, useMemo, useEffect } from 'react'
import { generateBreaches, SECTORS, DPDP_LANGUAGES, BREACH_NOTICE_TEMPLATES, DELIVERY_CHANNELS } from '../data/sectorData'
import { useAppContext } from '../context/AppContext'
import { showExportDialog, exportToCSV, exportToExcel, exportToWord, exportToPDF, ExportColumn } from '../utils/exportUtils'

const BREACH_COLUMNS: ExportColumn[] = [
  {key:'id',label:'Breach ID'},{key:'title',label:'Title'},{key:'severity',label:'Severity'},
  {key:'recordsAffected',label:'Records Affected'},{key:'status',label:'Status'},
  {key:'detectedAt',label:'Detected'},{key:'dpbiNotified',label:'DPBI Notified'},
]

export default function BreachPage() {
  const { sector, setSector } = useAppContext()
  const [showCreate, setShowCreate] = useState(false)
  const [selected, setSelected] = useState<any>(null)
  const [filterSeverity, setFilterSeverity] = useState('')
  const [page, setPage] = useState(0)
  const [form, setForm] = useState({title:'',severity:'HIGH',vector:'External API'})

  // ─── Notice Composer State ───
  const [showNotice, setShowNotice] = useState(false)
  const [noticeLang, setNoticeLang] = useState('en')
  const [noticeChannel, setNoticeChannel] = useState('email')
  const [noticeRecipient, setNoticeRecipient] = useState('')
  const [noticeSending, setNoticeSending] = useState(false)
  const [noticeSent, setNoticeSent] = useState(false)

  const allBreaches = useMemo(() => generateBreaches(sector, 50), [sector])
  const [breaches, setBreaches] = useState(allBreaches)

  // Export event listener
  useEffect(() => {
    const handleExport = (e: Event) => {
      const format = (e as CustomEvent).detail?.format
      if (format === 'csv') exportToCSV(breaches, BREACH_COLUMNS, `breach-incidents-${sector}`)
      else if (format === 'excel') exportToExcel(breaches, BREACH_COLUMNS, `breach-incidents-${sector}`, `Breach Incidents — ${sector}`)
      else if (format === 'word') exportToWord(breaches, BREACH_COLUMNS, `breach-incidents-${sector}`, `Breach Incident Report — ${sector}`, sector)
      else if (format === 'pdf') exportToPDF(breaches, BREACH_COLUMNS, `breach-incidents-${sector}`, `Breach Incidents — ${sector}`, sector)
      else showExportDialog(breaches, BREACH_COLUMNS, `breach-incidents-${sector}`, `Breach Incidents — ${sector}`, sector)
    }
    window.addEventListener('qs-export', handleExport)
    return () => window.removeEventListener('qs-export', handleExport)
  }, [breaches, sector])

  const changeSector = (s: string) => { setSector(s); setBreaches(generateBreaches(s, 50)); setPage(0) }

  const stats = useMemo(() => ({
    total: breaches.length,
    critical: breaches.filter(b=>b.severity==='CRITICAL').length,
    investigating: breaches.filter(b=>b.status==='INVESTIGATING').length,
    contained: breaches.filter(b=>b.status==='CONTAINED').length,
    resolved: breaches.filter(b=>b.status==='RESOLVED').length,
    notifiedDPBI: breaches.filter(b=>b.notifiedDPBI).length,
    notifiedCERTIN: breaches.filter(b=>b.notifiedCERTIN).length,
  }), [breaches])

  const filtered = useMemo(() => filterSeverity ? breaches.filter(b=>b.severity===filterSeverity) : breaches, [breaches, filterSeverity])
  const PAGE_SIZE = 15
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page*PAGE_SIZE, (page+1)*PAGE_SIZE)

  const handleCreate = () => {
    const now = new Date()
    setBreaches([{
      id:`BRR-${now.getFullYear()}-${String(breaches.length+1).padStart(3,'0')}`,
      ...form,status:'INVESTIGATING',recordsAffected:0,
      detectedAt:now.toISOString().split('T')[0],
      dpbiDeadline:new Date(now.getTime()+72*3600000).toISOString().split('T')[0],
      certinDeadline:new Date(now.getTime()+6*3600000).toISOString(),
      notifiedDPBI:false,notifiedCERTIN:false,dpdpSection:'Section 8'
    },...breaches])
    setShowCreate(false)
  }

  // ─── Get notice template ───
  const getSectorKey = () => {
    if (sector.includes('Bank')) return 'Banking & Finance'
    if (sector.includes('Health') || sector.includes('Pharma')) return 'Healthcare'
    if (sector.includes('Telecom')) return 'Telecom'
    if (sector.includes('Edu')) return 'Education'
    if (sector.includes('commerce')) return 'E-commerce'
    return 'Banking & Finance'
  }

  const noticeTemplate = BREACH_NOTICE_TEMPLATES[noticeLang]?.[getSectorKey()] || BREACH_NOTICE_TEMPLATES['en']['Banking & Finance']

  const fillTemplate = (tmpl: string) => {
    return tmpl
      .replace(/\{dataFiduciaryName\}/g, 'QS-DPDP Organization')
      .replace(/\{principalName\}/g, 'Data Principal')
      .replace(/\{breachDate\}/g, selected?.detectedAt || new Date().toISOString().split('T')[0])
      .replace(/\{dataCategories\}/g, 'Personal Identifiers, Financial Records')
      .replace(/\{breachDescription\}/g, selected?.title || 'Unauthorized access detected')
      .replace(/\{consequences\}/g, 'Potential unauthorized access to personal data')
      .replace(/\{remedialSteps\}/g, 'Affected systems isolated, forensic analysis initiated, encryption keys rotated')
      .replace(/\{fraudHelpline\}/g, '1800-309-4567')
      .replace(/\{dpbiRef\}/g, selected?.id || 'DPBI-REF')
      .replace(/\{certinRef\}/g, 'CERTIN-' + (selected?.id || 'REF'))
      .replace(/\{dpoName\}/g, 'Chief Data Protection Officer')
      .replace(/\{dpoEmail\}/g, 'dpo@qsdpdp.com')
      .replace(/\{dpoPhone\}/g, '+91-11-4096-7890')
  }

  const handleSendNotice = () => {
    setNoticeSending(true)
    setTimeout(() => {
      setNoticeSending(false)
      setNoticeSent(true)
      setTimeout(() => setNoticeSent(false), 3000)
    }, 2000)
  }

  const langInfo = DPDP_LANGUAGES.find(l => l.code === noticeLang) || DPDP_LANGUAGES[0]
  const channelInfo = DELIVERY_CHANNELS.find(c => c.id === noticeChannel) || DELIVERY_CHANNELS[0]

  return (
    <div className="page-container">
      <div className="page-header">
        <div><h1>🚨 Breach Notification Engine</h1><p>DPDP Act Section 8 — 72h DPBI / 6h CERT-IN compliance tracking</p></div>
        <div style={{display:'flex',gap:8}}>
          <button className="btn-primary" onClick={()=>setShowCreate(!showCreate)}>+ Report Breach</button>
          <button className="btn-secondary" onClick={()=>{setShowNotice(true);setNoticeSent(false)}} style={{background:'var(--amber)',borderColor:'var(--amber)',color:'#000'}}>📩 Compose Notice</button>
        </div>
      </div>

      <div style={{display:'flex',gap:12,marginBottom:16,alignItems:'center'}}>
        <select className="form-input" style={{width:220}} value={sector} onChange={e=>changeSector(e.target.value)}>
          {SECTORS.map(s=><option key={s}>{s}</option>)}
        </select>
        <span style={{fontSize:12,color:'var(--brand-primary)',fontWeight:600}}>📢 Notices available in {DPDP_LANGUAGES.length} languages</span>
      </div>

      <div className="kpi-grid">
        <div className="kpi-card red"><div className="kpi-value" style={{color:'var(--red)'}}>{stats.critical}</div><div className="kpi-label">Critical</div></div>
        <div className="kpi-card amber"><div className="kpi-value" style={{color:'var(--amber)'}}>{stats.investigating}</div><div className="kpi-label">Investigating</div></div>
        <div className="kpi-card blue"><div className="kpi-value" style={{color:'var(--blue)'}}>{stats.contained}</div><div className="kpi-label">Contained</div></div>
        <div className="kpi-card green"><div className="kpi-value" style={{color:'var(--green)'}}>{stats.resolved}</div><div className="kpi-label">Resolved</div></div>
        <div className="kpi-card"><div className="kpi-value">{stats.notifiedDPBI}/{stats.total}</div><div className="kpi-label">DPBI Notified (72h)</div></div>
        <div className="kpi-card"><div className="kpi-value">{stats.notifiedCERTIN}/{stats.total}</div><div className="kpi-label">CERT-IN Notified (6h)</div></div>
      </div>

      {showCreate && (
        <div className="glass-card" style={{marginBottom:16}}>
          <h3>📝 Report New Breach — Section 8</h3>
          <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:12}}>72-hour notification deadline to DPBI starts now. CERT-IN 6-hour deadline auto-calculated.</p>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:12}}>
            <input className="form-input" placeholder="Breach Title/Description" value={form.title} onChange={e=>setForm({...form,title:e.target.value})} />
            <select className="form-input" value={form.severity} onChange={e=>setForm({...form,severity:e.target.value})}>
              <option>CRITICAL</option><option>HIGH</option><option>MEDIUM</option><option>LOW</option>
            </select>
            <select className="form-input" value={form.vector} onChange={e=>setForm({...form,vector:e.target.value})}>
              <option>External API</option><option>Insider</option><option>Malware</option><option>Phishing</option><option>Misconfiguration</option><option>Social Engineering</option><option>Supply Chain</option><option>Ransomware</option>
            </select>
            <div style={{display:'flex',gap:8}}><button className="btn-primary" onClick={handleCreate}>Report Breach</button><button className="btn-secondary" onClick={()=>setShowCreate(false)}>Cancel</button></div>
          </div>
        </div>
      )}

      {/* ═══ MULTILINGUAL NOTICE COMPOSER ═══ */}
      {showNotice && (
        <div className="glass-card" style={{marginBottom:16,border:'2px solid var(--amber)'}}>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:16}}>
            <h3 style={{margin:0}}>📩 Compose Breach Notice — Data Principal Notification (Sec 8.6)</h3>
            <button className="btn-sm" onClick={()=>setShowNotice(false)}>✕</button>
          </div>

          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:24}}>
            {/* Left: Controls */}
            <div>
              <div style={{marginBottom:16}}>
                <label style={{fontSize:12,fontWeight:600,display:'block',marginBottom:6}}>🌐 Language (Eighth Schedule — {DPDP_LANGUAGES.length} languages)</label>
                <select value={noticeLang} onChange={e=>setNoticeLang(e.target.value)} style={{width:'100%',padding:10,background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:8,color:'var(--text-primary)',fontSize:14}}>
                  {DPDP_LANGUAGES.map(l => <option key={l.code} value={l.code}>{l.nativeName} — {l.name}</option>)}
                </select>
                <span style={{fontSize:11,color:'var(--text-muted)'}}>Selected: {langInfo.nativeName} ({langInfo.name})</span>
              </div>

              <div style={{marginBottom:16}}>
                <label style={{fontSize:12,fontWeight:600,display:'block',marginBottom:6}}>📤 Delivery Channel</label>
                <div style={{display:'flex',flexWrap:'wrap',gap:6}}>
                  {DELIVERY_CHANNELS.map(ch => (
                    <button key={ch.id} onClick={()=>setNoticeChannel(ch.id)} style={{padding:'8px 14px',background:noticeChannel===ch.id?'var(--brand-primary)':'var(--bg-surface)',color:noticeChannel===ch.id?'#fff':'var(--text-primary)',border:'1px solid var(--border)',borderRadius:8,cursor:'pointer',fontSize:13}}>
                      {ch.label}
                    </button>
                  ))}
                </div>
              </div>

              <div style={{marginBottom:16}}>
                <label style={{fontSize:12,fontWeight:600,display:'block',marginBottom:6}}>{channelInfo.label} — {channelInfo.field}</label>
                <input value={noticeRecipient} onChange={e=>setNoticeRecipient(e.target.value)} placeholder={channelInfo.placeholder} style={{width:'100%',padding:10,background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:8,color:'var(--text-primary)'}} />
              </div>

              <div style={{marginBottom:16}}>
                <label style={{fontSize:12,fontWeight:600,display:'block',marginBottom:6}}>📋 Linked Breach</label>
                <select onChange={e=>{const b=breaches.find(x=>x.id===e.target.value);if(b)setSelected(b)}} style={{width:'100%',padding:10,background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:8,color:'var(--text-primary)'}}>
                  <option value="">Select a breach incident...</option>
                  {breaches.slice(0,20).map(b => <option key={b.id} value={b.id}>{b.id} — {b.title.substring(0,60)}</option>)}
                </select>
              </div>

              <div style={{display:'flex',gap:8}}>
                <button className="btn-primary" onClick={handleSendNotice} disabled={noticeSending || !noticeRecipient} style={{flex:1}}>
                  {noticeSending ? '⏳ Sending...' : noticeSent ? '✅ Sent!' : `📤 Send via ${channelInfo.label}`}
                </button>
                <button className="btn-secondary" style={{background:'var(--purple)',borderColor:'var(--purple)',color:'#fff'}}>📤 Send to All Affected</button>
              </div>

              {noticeSent && (
                <div style={{marginTop:12,padding:12,background:'rgba(0,200,100,0.1)',borderRadius:8,border:'1px solid var(--green)',fontSize:13}}>
                  ✅ Notice sent successfully via {channelInfo.label} in {langInfo.name} to {noticeRecipient}
                </div>
              )}
            </div>

            {/* Right: Preview */}
            <div>
              <div style={{fontSize:12,fontWeight:600,marginBottom:8}}>📄 Notice Preview ({langInfo.nativeName})</div>
              <div style={{background:'var(--bg-surface)',borderRadius:8,padding:16,border:'1px solid var(--border)',maxHeight:400,overflowY:'auto'}}>
                <div style={{borderBottom:'1px solid var(--border)',paddingBottom:8,marginBottom:12}}>
                  <div style={{fontSize:11,color:'var(--text-muted)'}}>Subject:</div>
                  <div style={{fontWeight:700,fontSize:14}}>{fillTemplate(noticeTemplate.subject)}</div>
                </div>
                {noticeChannel === 'sms' || noticeChannel === 'whatsapp' ? (
                  <div style={{fontSize:13,lineHeight:1.6}}>
                    <div style={{background:'var(--bg-main)',padding:12,borderRadius:12,borderTopLeftRadius:0,maxWidth:'90%'}}>
                      {fillTemplate(noticeTemplate.sms)}
                    </div>
                    <div style={{fontSize:10,color:'var(--text-muted)',marginTop:4}}>
                      {noticeTemplate.sms.length} characters • {noticeChannel === 'whatsapp' ? 'WhatsApp Business API' : `${Math.ceil(noticeTemplate.sms.length/160)} SMS`}
                    </div>
                  </div>
                ) : (
                  <pre style={{whiteSpace:'pre-wrap',fontSize:12,lineHeight:1.7,fontFamily:'Inter,sans-serif',margin:0,color:'var(--text-primary)'}}>
                    {fillTemplate(noticeTemplate.body)}
                  </pre>
                )}
              </div>
              <div style={{display:'flex',gap:8,marginTop:8}}>
                <span style={{fontSize:11,padding:'3px 8px',borderRadius:4,background:'var(--bg-surface)',border:'1px solid var(--border)'}}>DPDP Section 8(6)</span>
                <span style={{fontSize:11,padding:'3px 8px',borderRadius:4,background:'var(--bg-surface)',border:'1px solid var(--border)'}}>Eighth Schedule Language</span>
                <span style={{fontSize:11,padding:'3px 8px',borderRadius:4,background:'var(--bg-surface)',border:'1px solid var(--border)'}}>Sector: {getSectorKey()}</span>
              </div>
            </div>
          </div>
        </div>
      )}

      <div style={{display:'flex',gap:12,marginBottom:16}}>
        <select className="form-input" style={{width:160}} value={filterSeverity} onChange={e=>{setFilterSeverity(e.target.value);setPage(0)}}>
          <option value="">All Severity</option><option>CRITICAL</option><option>HIGH</option><option>MEDIUM</option><option>LOW</option>
        </select>
        <span style={{fontSize:12,color:'var(--text-muted)',alignSelf:'center'}}>Showing {filtered.length} incidents</span>
      </div>

      <div className="glass-card">
        <h3>📋 Breach Registry ({filtered.length} incidents)</h3>
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>ID</th><th>Title</th><th>Severity</th><th>Status</th><th>Vector</th><th>Records</th><th>Detected</th><th>DPBI 72h</th><th>CERT-IN 6h</th><th>Actions</th></tr></thead>
            <tbody>{paged.map(b=>(
              <tr key={b.id} onClick={()=>setSelected(b)} style={{cursor:'pointer'}}>
                <td style={{fontFamily:'monospace',fontSize:11}}>{b.id}</td>
                <td style={{maxWidth:200}}><strong>{b.title}</strong></td>
                <td><span className={`rag-badge ${b.severity==='CRITICAL'?'rag-red':b.severity==='HIGH'?'rag-amber':'rag-green'}`}>{b.severity}</span></td>
                <td><span className={`rag-badge ${b.status==='INVESTIGATING'?'rag-red':b.status==='CONTAINED'?'rag-amber':'rag-green'}`}>{b.status}</span></td>
                <td>{b.vector}</td>
                <td>{b.recordsAffected.toLocaleString()}</td>
                <td style={{fontSize:12}}>{b.detectedAt}</td>
                <td>{b.notifiedDPBI?<span className="rag-badge rag-green">SENT</span>:<span className="rag-badge rag-red">PENDING</span>}</td>
                <td>{b.notifiedCERTIN?<span className="rag-badge rag-green">SENT</span>:<span className="rag-badge rag-red">PENDING</span>}</td>
                <td>
                  <div style={{display:'flex',gap:4}}>
                    <button className="btn-sm" onClick={e=>{e.stopPropagation();setSelected(b)}}>View</button>
                    <button className="btn-sm" onClick={e=>{e.stopPropagation();setSelected(b);setShowNotice(true);setNoticeSent(false)}} style={{background:'var(--amber)',color:'#000',border:'none'}}>📩</button>
                  </div>
                </td>
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

      {selected && !showNotice && (
        <div className="glass-card" style={{marginTop:16}}>
          <div style={{display:'flex',justifyContent:'space-between'}}><h3>📄 Breach Detail — {selected.id}</h3><button className="btn-sm" onClick={()=>setSelected(null)}>✕</button></div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:16,marginTop:16}}>
            <div><strong>Severity:</strong><br/><span className={`rag-badge ${selected.severity==='CRITICAL'?'rag-red':'rag-amber'}`}>{selected.severity}</span></div>
            <div><strong>Status:</strong><br/>{selected.status}</div>
            <div><strong>Vector:</strong><br/>{selected.vector}</div>
            <div><strong>Records Affected:</strong><br/>{selected.recordsAffected.toLocaleString()}</div>
            <div><strong>DPBI Deadline:</strong><br/>{selected.dpbiDeadline}</div>
            <div><strong>DPDP Section:</strong><br/>{selected.dpdpSection}</div>
          </div>
          <div style={{display:'flex',gap:8,marginTop:16}}>
            {!selected.notifiedDPBI && <button className="btn-primary">📨 Notify DPBI (72h)</button>}
            {!selected.notifiedCERTIN && <button className="btn-primary" style={{background:'var(--red)',borderColor:'var(--red)'}}>🚨 Notify CERT-IN (6h)</button>}
            <button className="btn-secondary" onClick={()=>{setShowNotice(true);setNoticeSent(false)}}>📩 Send Notice to Data Principal</button>
            <button className="btn-secondary">📋 Forensic Report</button>
            <button className="btn-secondary">📤 Export Incident</button>
          </div>
        </div>
      )}
    </div>
  )
}
