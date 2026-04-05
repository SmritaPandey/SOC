import { useState, useMemo } from 'react'
import { generateSIEMEvents } from '../data/sectorData'

const PLAYBOOKS = [
  {id:'PB-001',name:'Brute Force Response',trigger:'AUTH_FAILURE > 5/min',status:'ACTIVE',runs:142,lastRun:'2026-03-13 14:20',actions:['Block source IP','Alert SOC','Create ticket','Quarantine account']},
  {id:'PB-002',name:'Malware Containment',trigger:'MALWARE_DETECTED severity=CRITICAL',status:'ACTIVE',runs:38,lastRun:'2026-03-13 11:45',actions:['Isolate endpoint','Collect forensics','Notify CERT-IN','Update IOC']},
  {id:'PB-003',name:'Data Exfiltration Block',trigger:'DATA_EXFILTRATION any severity',status:'ACTIVE',runs:23,lastRun:'2026-03-12 22:10',actions:['Block destination','Alert DPO','Assess PII impact','Report to DPBI if PII']},
  {id:'PB-004',name:'Ransomware Response',trigger:'RANSOMWARE detection',status:'ACTIVE',runs:5,lastRun:'2026-03-10 09:30',actions:['Network isolation','Backup verification','Law enforcement','Forensic imaging']},
  {id:'PB-005',name:'Phishing Email Triage',trigger:'PHISHING_EMAIL inbound',status:'ACTIVE',runs:215,lastRun:'2026-03-13 16:00',actions:['Quarantine email','Block sender domain','User notification','IOC extraction']},
  {id:'PB-006',name:'Insider Threat Detection',trigger:'ANOMALOUS_BEHAVIOR + data access',status:'ACTIVE',runs:12,lastRun:'2026-03-11 15:22',actions:['Session monitoring','Manager alert','Access review','HR notification']},
]

const UEBA_ANOMALIES = [
  {user:'admin@corp.local',riskScore:92,anomaly:'Bulk data download (500GB) outside business hours',baseline:'Avg daily: 2.3GB',triggered:'2026-03-13 02:15',status:'CRITICAL'},
  {user:'analyst3@corp.local',riskScore:78,anomaly:'Access to 3 restricted databases in 10 minutes',baseline:'Avg: 0.5 DB/day',triggered:'2026-03-13 09:30',status:'HIGH'},
  {user:'hr-bot@corp.local',riskScore:65,anomaly:'API key used from new geo-location (Dubai)',baseline:'Always Mumbai',triggered:'2026-03-12 23:45',status:'MEDIUM'},
  {user:'dev5@corp.local',riskScore:55,anomaly:'Source code repository clone after resignation notice',baseline:'No prior full clone',triggered:'2026-03-12 18:20',status:'HIGH'},
  {user:'vendor-api@ext.com',riskScore:45,anomaly:'Rate limit exceeded: 5000 req/min',baseline:'Avg: 200 req/min',triggered:'2026-03-13 14:00',status:'MEDIUM'},
]

export default function SIEMPage() {
  const [tab, setTab] = useState<'events'|'soar'|'ueba'|'intel'>('events')
  const [selected, setSelected] = useState<any>(null)
  const [page, setPage] = useState(0)
  const [filterSeverity, setFilterSeverity] = useState('')
  const [iocQuery, setIocQuery] = useState('')
  const [iocResults, setIocResults] = useState<any[]>([])

  const allEvents = useMemo(() => generateSIEMEvents(500), [])
  const [events] = useState(allEvents)

  const filtered = useMemo(() => filterSeverity ? events.filter(e=>e.severity===filterSeverity) : events, [events, filterSeverity])
  const PAGE_SIZE = 20
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE)
  const paged = filtered.slice(page*PAGE_SIZE, (page+1)*PAGE_SIZE)

  const stats = useMemo(() => ({
    total: events.length,
    critical: events.filter(e=>e.severity==='CRITICAL').length,
    high: events.filter(e=>e.severity==='HIGH').length,
    open: events.filter(e=>e.status==='OPEN').length,
    contained: events.filter(e=>e.status==='CONTAINED').length,
  }), [events])

  const lookupIOC = () => {
    if (!iocQuery.trim()) return
    setIocResults([
      {ioc:iocQuery,type:iocQuery.includes('.')?'IP':'HASH',reputation:'MALICIOUS',source:'OTX AlienVault',firstSeen:'2026-01-15',lastSeen:'2026-03-13',tags:['APT28','Phishing','CnC']},
      {ioc:iocQuery,type:'associatedDomain',reputation:'SUSPICIOUS',source:'Abuse.ch',firstSeen:'2026-02-20',lastSeen:'2026-03-12',tags:['Malware','Dropper']},
    ])
  }

  return (
    <div className="page-container">
      <div className="page-header"><h1>🛡️ Security Operations Center</h1><p>QS-SIEM • SOAR • Threat Intelligence • UEBA • MITRE ATT&CK</p></div>

      <div className="kpi-grid">
        <div className="kpi-card red"><div className="kpi-value" style={{color:'var(--red)'}}>{stats.critical}</div><div className="kpi-label">Critical Alerts</div></div>
        <div className="kpi-card amber"><div className="kpi-value" style={{color:'var(--amber)'}}>{stats.high}</div><div className="kpi-label">High Alerts</div></div>
        <div className="kpi-card"><div className="kpi-value">{stats.open}</div><div className="kpi-label">Open Events</div></div>
        <div className="kpi-card green"><div className="kpi-value" style={{color:'var(--green)'}}>{stats.contained}</div><div className="kpi-label">Contained</div></div>
        <div className="kpi-card blue"><div className="kpi-value" style={{color:'var(--blue)'}}>{PLAYBOOKS.length}</div><div className="kpi-label">SOAR Playbooks</div></div>
        <div className="kpi-card"><div className="kpi-value">{stats.total}</div><div className="kpi-label">Total Events</div></div>
      </div>

      <div style={{display:'flex',gap:8,marginBottom:20}}>
        {(['events','soar','ueba','intel'] as const).map(t=>(
          <button key={t} className={tab===t?'btn-primary':'btn-secondary'} onClick={()=>setTab(t)}>
            {t==='events'?'🚨 Events & Alerts':t==='soar'?'🤖 SOAR Playbooks':t==='ueba'?'🧠 UEBA Analytics':'🔍 Threat Intel'}
          </button>
        ))}
      </div>

      {/* EVENTS TAB */}
      {tab==='events' && (
        <div className="glass-card">
          <h3>🚨 Security Events ({filtered.length})</h3>
          <div style={{display:'flex',gap:12,margin:'12px 0'}}>
            <select className="form-input" style={{width:160}} value={filterSeverity} onChange={e=>{setFilterSeverity(e.target.value);setPage(0)}}>
              <option value="">All Severity</option><option>CRITICAL</option><option>HIGH</option><option>MEDIUM</option><option>LOW</option>
            </select>
          </div>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>ID</th><th>Time</th><th>Source</th><th>Type</th><th>Severity</th><th>MITRE</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>{paged.map(e=>(
                <tr key={e.id} onClick={()=>setSelected(e)} style={{cursor:'pointer'}}>
                  <td style={{fontFamily:'monospace',fontSize:11}}>{e.id}</td>
                  <td style={{fontFamily:'monospace',fontSize:12}}>{e.time}</td>
                  <td style={{fontFamily:'monospace',fontSize:12}}>{e.source}</td>
                  <td>{e.type}</td>
                  <td><span className={`rag-badge ${e.severity==='CRITICAL'?'rag-red':e.severity==='HIGH'?'rag-amber':'rag-green'}`}>{e.severity}</span></td>
                  <td style={{fontFamily:'monospace',fontSize:12,color:'var(--brand-primary)'}}>{e.mitre}</td>
                  <td><span className={`rag-badge ${e.status==='OPEN'?'rag-red':e.status==='INVESTIGATING'?'rag-amber':'rag-green'}`}>{e.status}</span></td>
                  <td><button className="btn-sm">Investigate</button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginTop:12}}>
            <span style={{fontSize:12,color:'var(--text-muted)'}}>Page {page+1} of {totalPages} ({filtered.length} events)</span>
            <div style={{display:'flex',gap:4}}>
              <button className="btn-sm" disabled={page===0} onClick={()=>setPage(p=>p-1)}>◀ Prev</button>
              <button className="btn-sm" disabled={page>=totalPages-1} onClick={()=>setPage(p=>p+1)}>Next ▶</button>
            </div>
          </div>
        </div>
      )}

      {/* SOAR TAB */}
      {tab==='soar' && (
        <div className="glass-card">
          <h3>🤖 SOAR Playbooks ({PLAYBOOKS.length} active)</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>ID</th><th>Playbook Name</th><th>Trigger Condition</th><th>Status</th><th>Total Runs</th><th>Last Run</th><th>Actions</th></tr></thead>
              <tbody>{PLAYBOOKS.map(p=>(
                <tr key={p.id}>
                  <td style={{fontFamily:'monospace',fontSize:11}}>{p.id}</td>
                  <td><strong>{p.name}</strong></td>
                  <td style={{fontSize:12,fontFamily:'monospace'}}>{p.trigger}</td>
                  <td><span className="rag-badge rag-green">{p.status}</span></td>
                  <td style={{fontWeight:700}}>{p.runs}</td>
                  <td style={{fontSize:12}}>{p.lastRun}</td>
                  <td>
                    <div style={{display:'flex',gap:4}}>
                      <button className="btn-sm">▶ Run</button>
                      <button className="btn-sm">✏️ Edit</button>
                    </div>
                  </td>
                </tr>
              ))}</tbody>
            </table>
          </div>
          {PLAYBOOKS.map(p=>(
            <div key={p.id} className="glass-card" style={{marginTop:12}}>
              <strong>{p.name}</strong> — Response Actions:
              <div style={{display:'flex',gap:6,marginTop:6,flexWrap:'wrap'}}>
                {p.actions.map((a,i)=><span key={i} className="badge blue">{i+1}. {a}</span>)}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* UEBA TAB */}
      {tab==='ueba' && (
        <div className="glass-card">
          <h3>🧠 UEBA — User & Entity Behavior Analytics (ML-Powered)</h3>
          <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:16}}>Isolation Forest + Random Forest anomaly detection (Smile ML)</p>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>User/Entity</th><th>Risk Score</th><th>Anomaly</th><th>Baseline</th><th>Triggered</th><th>Status</th><th>Actions</th></tr></thead>
              <tbody>{UEBA_ANOMALIES.map((u,i)=>(
                <tr key={i}>
                  <td style={{fontFamily:'monospace',fontSize:12}}>{u.user}</td>
                  <td>
                    <div style={{display:'flex',alignItems:'center',gap:6}}>
                      <div style={{background:'var(--border)',borderRadius:4,height:6,width:50,overflow:'hidden'}}>
                        <div style={{width:`${u.riskScore}%`,height:'100%',background:u.riskScore>=80?'var(--red)':u.riskScore>=50?'var(--amber)':'var(--green)',borderRadius:4}} />
                      </div>
                      <span style={{fontWeight:700,fontSize:12}}>{u.riskScore}</span>
                    </div>
                  </td>
                  <td style={{fontSize:12,maxWidth:250}}>{u.anomaly}</td>
                  <td style={{fontSize:11,color:'var(--text-muted)'}}>{u.baseline}</td>
                  <td style={{fontSize:12}}>{u.triggered}</td>
                  <td><span className={`rag-badge ${u.status==='CRITICAL'?'rag-red':u.status==='HIGH'?'rag-amber':'rag-green'}`}>{u.status}</span></td>
                  <td><button className="btn-sm">Investigate</button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        </div>
      )}

      {/* THREAT INTEL TAB */}
      {tab==='intel' && (
        <div className="glass-card">
          <h3>🔍 Threat Intelligence — IOC Lookup</h3>
          <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:16}}>Search against OTX AlienVault, Abuse.ch, Spamhaus DROP feeds</p>
          <div style={{display:'flex',gap:8,marginBottom:20}}>
            <input className="form-input" style={{flex:1}} placeholder="Enter IP, domain, hash, or URL to lookup..." value={iocQuery} onChange={e=>setIocQuery(e.target.value)} />
            <button className="btn-primary" onClick={lookupIOC}>🔍 Lookup</button>
            <button className="btn-secondary">🔄 Refresh Feeds</button>
          </div>
          {iocResults.length > 0 && (
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead><tr><th>IOC</th><th>Type</th><th>Reputation</th><th>Source</th><th>First Seen</th><th>Last Seen</th><th>Tags</th></tr></thead>
                <tbody>{iocResults.map((r,i)=>(
                  <tr key={i}>
                    <td style={{fontFamily:'monospace',fontSize:12}}>{r.ioc}</td>
                    <td>{r.type}</td>
                    <td><span className={`rag-badge ${r.reputation==='MALICIOUS'?'rag-red':'rag-amber'}`}>{r.reputation}</span></td>
                    <td>{r.source}</td>
                    <td style={{fontSize:12}}>{r.firstSeen}</td>
                    <td style={{fontSize:12}}>{r.lastSeen}</td>
                    <td>{r.tags.map((t:string,j:number)=><span key={j} className="badge red" style={{marginRight:4}}>{t}</span>)}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {selected && (
        <div className="glass-card" style={{marginTop:16}}>
          <div style={{display:'flex',justifyContent:'space-between'}}><h3>🔍 Event Detail — {selected.id}</h3><button className="btn-sm" onClick={()=>setSelected(null)}>✕</button></div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:16,marginTop:16}}>
            <div><strong>Type:</strong><br/>{selected.type}</div>
            <div><strong>Source:</strong><br/>{selected.source}</div>
            <div><strong>MITRE:</strong><br/>{selected.mitre}</div>
            <div><strong>Severity:</strong><br/><span className={`rag-badge ${selected.severity==='CRITICAL'?'rag-red':'rag-amber'}`}>{selected.severity}</span></div>
            <div><strong>Status:</strong><br/>{selected.status}</div>
            <div><strong>Details:</strong><br/>{selected.details}</div>
          </div>
          <div style={{display:'flex',gap:8,marginTop:16}}>
            <button className="btn-primary">🔍 Investigate</button>
            <button className="btn-secondary">📋 Timeline</button>
            <button className="btn-secondary">🤖 Run SOAR Playbook</button>
          </div>
        </div>
      )}
    </div>
  )
}
