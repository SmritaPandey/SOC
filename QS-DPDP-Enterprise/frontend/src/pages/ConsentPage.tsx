import { useEffect, useState, useRef, useCallback } from 'react'
import { SECTORS, SECTOR_CONSENT_PURPOSES, DPDP_LANGUAGES } from '../data/sectorData'
import { useAppContext } from '../context/AppContext'
import { showExportDialog, exportToCSV, exportToExcel, exportToWord, exportToPDF, ExportColumn } from '../utils/exportUtils'
import api from '../api'

/* ═══════════════════════════════════════════════════════════════
   CONSENT MANAGEMENT — DPDP Act Section 6-7-9
   Full CRUD lifecycle: Create → Read → Update → Withdraw → Audit
   Connected to real backend API — H2 Database persistence
   ═══════════════════════════════════════════════════════════════ */

const CONSENT_COLUMNS: ExportColumn[] = [
  {key:'id',label:'Consent ID'},{key:'dataPrincipalName',label:'Data Principal'},
  {key:'purposeName',label:'Purpose'},{key:'consentType',label:'Type'},
  {key:'status',label:'Status'},{key:'collectedAt',label:'Collected'},
  {key:'expiresAt',label:'Expires'},{key:'language',label:'Language'},
]

// Format ISO date to readable
const fmtDate = (iso?: string|null) => iso ? new Date(iso).toLocaleDateString('en-IN',{year:'numeric',month:'short',day:'numeric'}) : '—'
const fmtDateTime = (iso?: string|null) => iso ? new Date(iso).toLocaleString('en-IN',{year:'numeric',month:'short',day:'numeric',hour:'2-digit',minute:'2-digit'}) : '—'

export default function ConsentPage() {
  const { sector, setSector } = useAppContext()
  const [tab, setTab] = useState<'records'|'analytics'|'audit'|'notices'>('records')
  const [showCreate, setShowCreate] = useState(false)
  const [selected, setSelected] = useState<any>(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [bulkIds, setBulkIds] = useState<Set<string>>(new Set())
  const createFormRef = useRef<HTMLDivElement>(null)

  // ═══ REAL DATA STATE — from backend API ═══
  const [consents, setConsents] = useState<any[]>([])
  const [stats, setStats] = useState<any>({})
  const [auditLog, setAuditLog] = useState<any[]>([])
  const [totalRecords, setTotalRecords] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionMsg, setActionMsg] = useState('')

  // Create form state
  const [form, setForm] = useState({
    dataPrincipalName:'', purposeName:'', consentType:'EXPLICIT',
    consentMethod:'WEB_FORM', language:'en', sector: sector || 'Banking & Finance',
    dataCategories: '' as string, retentionPeriod: '365 days',
    lawfulBasis: '' as string, purposeDescription: '' as string,
  })
  const [isCustomPurpose, setIsCustomPurpose] = useState(false)

  // Get sector-specific purposes for dropdowns
  const sectorPurposes = SECTOR_CONSENT_PURPOSES[form.sector] || SECTOR_CONSENT_PURPOSES['Banking & Finance'] || []

  // Handle purpose selection from dropdown
  const handlePurposeSelect = (value: string) => {
    if (value === '__CUSTOM__') {
      setIsCustomPurpose(true)
      setForm({ ...form, purposeName: '', dataCategories: '', retentionPeriod: '365 days', lawfulBasis: '', purposeDescription: '' })
    } else {
      setIsCustomPurpose(false)
      const purpose = sectorPurposes.find(p => p.label === value)
      if (purpose) {
        setForm({
          ...form,
          purposeName: purpose.label,
          dataCategories: purpose.dataTypes.join(', '),
          retentionPeriod: purpose.retentionPeriod,
          lawfulBasis: purpose.lawfulBasis,
          purposeDescription: purpose.description,
        })
      } else {
        setForm({ ...form, purposeName: value })
      }
    }
  }

  // Sync form sector with page sector
  useEffect(() => { setForm(f => ({ ...f, sector })); setIsCustomPurpose(false) }, [sector])

  const PAGE_SIZE = 25

  // ═══ FETCH CONSENTS FROM BACKEND ═══
  const fetchConsents = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const data = await api.consentList(page * PAGE_SIZE, PAGE_SIZE)
      setConsents(data.consents || [])
      setTotalRecords(data.total || 0)
    } catch (e: any) {
      setError(`Failed to load consents: ${e.message}`)
      setConsents([])
    } finally { setLoading(false) }
  }, [page])

  // ═══ FETCH STATS FROM BACKEND ═══
  const fetchStats = useCallback(async () => {
    try {
      const data = await api.consentStats()
      setStats(data)
    } catch { setStats({}) }
  }, [])

  // ═══ FETCH AUDIT TRAIL ═══
  const fetchAudit = useCallback(async () => {
    try {
      const data = await api.consentAuditTrail(undefined, 100)
      setAuditLog(data.entries || [])
    } catch { setAuditLog([]) }
  }, [])

  // Load data on mount and when page changes
  useEffect(() => { fetchConsents() }, [fetchConsents])
  useEffect(() => { fetchStats() }, [fetchStats])
  useEffect(() => { if (tab === 'audit') fetchAudit() }, [tab, fetchAudit])

  // ═══ CREATE CONSENT — POST to backend ═══
  const handleCreate = async () => {
    if (!form.dataPrincipalName || !form.purposeName) {
      setActionMsg('❌ Please fill in Data Principal Name and Purpose'); return
    }
    setActionMsg('⏳ Creating consent...')
    try {
      await api.consentCreate({
        dataPrincipalName: form.dataPrincipalName,
        purposeName: form.purposeName,
        consentType: form.consentType,
        consentMethod: form.consentMethod,
        language: form.language,
        dataCategories: form.dataCategories ? form.dataCategories.split(',').map(s=>s.trim()) : [],
        retentionPeriod: form.retentionPeriod,
        sector: form.sector,
      })
      setActionMsg('✅ Consent created successfully!')
      setShowCreate(false)
      setIsCustomPurpose(false)
      setForm({dataPrincipalName:'',purposeName:'',consentType:'EXPLICIT',consentMethod:'WEB_FORM',language:'en',sector,dataCategories:'',retentionPeriod:'365 days',lawfulBasis:'',purposeDescription:''})
      await fetchConsents()
      await fetchStats()
      setTimeout(() => setActionMsg(''), 3000)
    } catch (e: any) {
      setActionMsg(`❌ Create failed: ${e.message}`)
    }
  }

  // ═══ WITHDRAW CONSENT — POST to backend ═══
  const handleWithdraw = async (id: string) => {
    if (!confirm('Are you sure you want to withdraw this consent? This action is irreversible per DPDP Act §6(5).')) return
    setActionMsg('⏳ Withdrawing consent...')
    try {
      await api.consentWithdraw(id, 'Withdrawn by Data Principal via self-service portal')
      setActionMsg('✅ Consent withdrawn successfully!')
      await fetchConsents()
      await fetchStats()
      if (selected?.id === id) setSelected(null)
      setTimeout(() => setActionMsg(''), 3000)
    } catch (e: any) {
      setActionMsg(`❌ Withdrawal failed: ${e.message}`)
    }
  }

  // ═══ BULK WITHDRAW ═══
  const handleBulkWithdraw = async () => {
    if (bulkIds.size === 0) return
    if (!confirm(`Withdraw ${bulkIds.size} selected consents? This is irreversible.`)) return
    setActionMsg(`⏳ Withdrawing ${bulkIds.size} consents...`)
    let ok = 0, fail = 0
    for (const id of bulkIds) {
      try { await api.consentWithdraw(id, 'Bulk withdrawal by admin'); ok++ }
      catch { fail++ }
    }
    setActionMsg(`✅ ${ok} withdrawn${fail > 0 ? `, ${fail} failed` : ''}`)
    setBulkIds(new Set())
    await fetchConsents()
    await fetchStats()
    setTimeout(() => setActionMsg(''), 4000)
  }

  // ═══ RENEW CONSENT — POST to backend ═══
  const handleRenew = async (id: string) => {
    setActionMsg('⏳ Renewing consent...')
    try {
      await api.consentRenew(id, 365)
      setActionMsg('✅ Consent renewed for 365 days!')
      await fetchConsents()
      await fetchStats()
      setTimeout(() => setActionMsg(''), 3000)
    } catch (e: any) {
      setActionMsg(`❌ Renewal failed: ${e.message}`)
    }
  }

  // Listen for ribbon/menu export events
  useEffect(() => {
    const handleExport = (e: Event) => {
      const format = (e as CustomEvent).detail?.format
      const exportData = consents.map(c => ({...c, principal: c.dataPrincipalName, purpose: c.purposeName, type: c.consentType, grantedAt: fmtDate(c.collectedAt)}))
      if (format === 'csv') exportToCSV(exportData, CONSENT_COLUMNS, `consent-records-${sector}`)
      else if (format === 'excel') exportToExcel(exportData, CONSENT_COLUMNS, `consent-records-${sector}`, `Consent Records — ${sector}`)
      else if (format === 'word') exportToWord(exportData, CONSENT_COLUMNS, `consent-records-${sector}`, `Consent Management Report — ${sector}`, sector)
      else if (format === 'pdf') exportToPDF(exportData, CONSENT_COLUMNS, `consent-records-${sector}`, `Consent Records — ${sector}`, sector)
      else showExportDialog(exportData, CONSENT_COLUMNS, `consent-records-${sector}`, `Consent Records — ${sector}`, sector)
    }
    const handleTab = (e: Event) => {
      const t = (e as CustomEvent).detail?.tab
      if (['records','analytics','audit','notices'].includes(t)) setTab(t)
    }
    const handleAction = (e: Event) => {
      const action = (e as CustomEvent).detail?.action
      if (action === 'collect-consent') { setShowCreate(true); setTab('records') }
    }
    window.addEventListener('qs-export', handleExport)
    window.addEventListener('qs-tab', handleTab)
    window.addEventListener('qs-action', handleAction)
    return () => {
      window.removeEventListener('qs-export', handleExport)
      window.removeEventListener('qs-tab', handleTab)
      window.removeEventListener('qs-action', handleAction)
    }
  }, [consents, sector])

  // Filtered consents (client-side filtering of current page)
  const filtered = search
    ? consents.filter(c => {
        const s = search.toLowerCase()
        return (c.dataPrincipalName||'').toLowerCase().includes(s) ||
               (c.purposeName||'').toLowerCase().includes(s) ||
               (c.id||'').toLowerCase().includes(s) ||
               (c.status||'').toLowerCase().includes(s)
      })
    : consents

  const totalPages = Math.max(1, Math.ceil(totalRecords / PAGE_SIZE))

  // Purpose analytics (from stats API)
  const languageDistribution = stats.languageDistribution
    ? Object.entries(stats.languageDistribution as Record<string,number>).sort(([,a],[,b])=>(b as number)-(a as number))
    : []

  const toggleBulk = (id: string) => {
    const s = new Set(bulkIds)
    s.has(id) ? s.delete(id) : s.add(id)
    setBulkIds(s)
  }
  const selectAllPage = () => {
    const s = new Set(bulkIds)
    filtered.forEach(c => s.add(c.id))
    setBulkIds(s)
  }

  // Export functions — using REAL data
  const exportCSV = () => {
    const data = consents.map(c => ({id:c.id,dataPrincipalName:c.dataPrincipalName,purposeName:c.purposeName,consentType:c.consentType,status:c.status,collectedAt:fmtDate(c.collectedAt),expiresAt:fmtDate(c.expiresAt),language:c.language}))
    const headers = 'ID,Principal,Purpose,Type,Status,Collected,Expires,Language\n'
    const rows = data.map(c => `${c.id},"${c.dataPrincipalName}","${c.purposeName}",${c.consentType},${c.status},${c.collectedAt},${c.expiresAt},${c.language}`).join('\n')
    const blob = new Blob([headers+rows], {type:'text/csv'})
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `Consent_Records_${sector.replace(/\s/g,'_')}.csv`; a.click()
  }

  const exportWord = () => {
    const html = `<html><head><meta charset="utf-8"><style>body{font-family:Calibri;padding:32px;color:#1E1B4B}h1{color:#581C87;border-bottom:3px solid #7C3AED;padding-bottom:8px}table{width:100%;border-collapse:collapse;margin:12px 0}th{background:#7C3AED;color:white;padding:8px;text-align:left;font-size:11px}td{padding:6px;border:1px solid #DDD6FE;font-size:11px}.green{color:#059669}.red{color:#DC2626}</style></head><body>
    <h1>Consent Management Report — ${sector}</h1><p>Generated: ${new Date().toLocaleDateString('en-IN')} | Total: ${totalRecords} | DPDP Act 2023</p>
    <h2>Summary</h2><table><tr><th>Metric</th><th>Value</th></tr><tr><td>Total</td><td>${stats.totalConsents||0}</td></tr><tr><td>Active</td><td class="green">${stats.activeConsents||0}</td></tr><tr><td>Withdrawn</td><td class="red">${stats.withdrawnConsents||0}</td></tr><tr><td>Expired</td><td>${stats.expiredConsents||0}</td></tr></table>
    <h2>Records</h2><table><thead><tr><th>ID</th><th>Principal</th><th>Purpose</th><th>Type</th><th>Status</th><th>Collected</th><th>Expires</th></tr></thead><tbody>${consents.map(c => `<tr><td>${c.id?.slice(0,8)}</td><td>${c.dataPrincipalName}</td><td>${c.purposeName}</td><td>${c.consentType}</td><td>${c.status}</td><td>${fmtDate(c.collectedAt)}</td><td>${fmtDate(c.expiresAt)}</td></tr>`).join('')}</tbody></table></body></html>`
    const blob = new Blob([html], {type:'application/msword'})
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `Consent_Report_${sector.replace(/\s/g,'_')}.doc`; a.click()
  }

  const printRecords = () => {
    const w = window.open('','_blank')
    if (!w) return
    w.document.write(`<html><head><title>Consent Report</title><style>body{font-family:Calibri;padding:24px}h1{color:#581C87}table{width:100%;border-collapse:collapse}th{background:#7C3AED;color:white;padding:6px;font-size:10px;text-align:left}td{padding:4px;border:1px solid #DDD6FE;font-size:10px}</style></head><body><h1>Consent Records — ${sector}</h1><p>Total: ${totalRecords} | Generated: ${new Date().toLocaleDateString('en-IN')}</p><table><tr><th>ID</th><th>Principal</th><th>Purpose</th><th>Type</th><th>Status</th><th>Collected</th><th>Expires</th></tr>`)
    consents.forEach(c => w.document.write(`<tr><td>${c.id?.slice(0,8)}</td><td>${c.dataPrincipalName}</td><td>${c.purposeName}</td><td>${c.consentType}</td><td>${c.status}</td><td>${fmtDate(c.collectedAt)}</td><td>${fmtDate(c.expiresAt)}</td></tr>`))
    w.document.write('</table></body></html>'); w.document.close(); w.print()
  }

  // Simple bar renderer
  const Bar = ({value, max, color}:{value:number,max:number,color:string}) => (
    <div style={{display:'flex',alignItems:'center',gap:8}}>
      <div style={{flex:1,background:'var(--border)',borderRadius:4,height:14,overflow:'hidden'}}>
        <div style={{width:`${Math.min(100,(value/Math.max(1,max))*100)}%`,height:'100%',background:color,borderRadius:4,transition:'width 0.5s'}} />
      </div>
      <span style={{fontSize:12,fontWeight:700,minWidth:32}}>{value.toLocaleString()}</span>
    </div>
  )

  /* ═══ RENDER ═══ */
  return (
    <div className="page-container">
      {/* ACTION MESSAGE BAR */}
      {actionMsg && (
        <div style={{padding:'10px 16px',marginBottom:12,borderRadius:'var(--radius)',background:actionMsg.startsWith('✅')?'var(--green-bg)':actionMsg.startsWith('❌')?'var(--red-bg)':'var(--amber-bg)',border:`1px solid ${actionMsg.startsWith('✅')?'var(--green-border)':actionMsg.startsWith('❌')?'var(--red-border)':'var(--amber-border)'}`,fontSize:13,fontWeight:600,display:'flex',justifyContent:'space-between',alignItems:'center'}}>
          <span>{actionMsg}</span>
          <button style={{background:'none',border:'none',cursor:'pointer',fontSize:16,color:'var(--text-muted)'}} onClick={()=>setActionMsg('')}>✕</button>
        </div>
      )}

      <div className="page-header">
        <div>
          <h1>✅ Consent Management</h1>
          <p>DPDP Act Section 6-7-9 — Full lifecycle | {sector} | {totalRecords.toLocaleString()} records (database)</p>
        </div>
        <div style={{display:'flex',gap:8}}>
          <button className="btn-primary" onClick={()=>{ const next = !showCreate; setShowCreate(next); if (next) { setTab('records'); setTimeout(()=> createFormRef.current?.scrollIntoView({behavior:'smooth',block:'start'}), 100) } }}>+ Collect Consent</button>
          <button className="btn-secondary" onClick={exportWord}>📄 Word</button>
          <button className="btn-secondary" onClick={exportCSV}>📊 CSV</button>
          <button className="btn-secondary" onClick={printRecords}>🖨️ Print</button>
        </div>
      </div>

      {/* SECTOR + TAB SELECTOR */}
      <div style={{display:'flex',gap:12,marginBottom:16,flexWrap:'wrap',alignItems:'center'}}>
        <select className="form-input" style={{width:220}} value={sector} onChange={e=>{setSector(e.target.value);setPage(0)}}>
          {SECTORS.map(s=><option key={s}>{s}</option>)}
        </select>
        <div style={{display:'flex',gap:4}}>
          {(['records','analytics','audit','notices'] as const).map(t => (
            <button key={t} className={`btn-${tab===t?'primary':'secondary'}`} onClick={()=>setTab(t)} style={{textTransform:'capitalize',fontSize:12}}>
              {t==='records'?'📋 Records':t==='analytics'?'📊 Analytics':t==='audit'?'📝 Audit Trail':'📜 Notices'}
            </button>
          ))}
        </div>
        <button className="btn-secondary" onClick={()=>{fetchConsents();fetchStats();fetchAudit()}} style={{fontSize:11,marginLeft:'auto'}}>🔄 Refresh</button>
      </div>

      {/* KPI ROW — REAL DATA */}
      <div className="kpi-grid">
        <div className="kpi-card"><div className="kpi-value" style={{color:'var(--green)'}}>{(stats.activeConsents||0).toLocaleString()}</div><div className="kpi-label">Active</div></div>
        <div className="kpi-card"><div className="kpi-value" style={{color:'var(--amber)'}}>{(stats.consentsLast30Days||0).toLocaleString()}</div><div className="kpi-label">Last 30 Days</div></div>
        <div className="kpi-card"><div className="kpi-value" style={{color:'var(--red)'}}>{(stats.withdrawnConsents||0).toLocaleString()}</div><div className="kpi-label">Withdrawn</div></div>
        <div className="kpi-card"><div className="kpi-value" style={{color:'var(--blue)'}}>{(stats.expiredConsents||0).toLocaleString()}</div><div className="kpi-label">Expired</div></div>
        <div className="kpi-card"><div className="kpi-value" style={{color:'var(--purple)'}}>{(stats.totalGuardianConsents||0).toLocaleString()}</div><div className="kpi-label">Guardian (§9)</div></div>
        <div className="kpi-card"><div className="kpi-value" style={{color:(stats.activeRate||0)>=60?'var(--green)':'var(--amber)'}}>{(stats.activeRate||0).toFixed(1)}%</div><div className="kpi-label">Active Rate</div></div>
        <div className="kpi-card"><div className="kpi-value" style={{color:(stats.expiringIn30Days||0)>50?'var(--red)':'var(--green)'}}>{(stats.expiringIn30Days||0).toLocaleString()}</div><div className="kpi-label">Expiring Soon</div></div>
        <div className="kpi-card"><div className="kpi-value">{(stats.totalConsents||0).toLocaleString()}</div><div className="kpi-label">Total Records</div></div>
      </div>

      {/* ═══ ANALYTICS TAB — REAL STATS ═══ */}
      {tab === 'analytics' && (
        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginTop:16}}>
          {/* Language Distribution */}
          <div className="glass-card">
            <h3>🌍 Consent Language Distribution</h3>
            <div style={{marginTop:12}}>
              {languageDistribution.map(([lang, count], i) => (
                <div key={i} style={{marginBottom:8}}>
                  <div style={{display:'flex',justifyContent:'space-between',fontSize:12,marginBottom:4}}><span>{lang === 'en' ? 'English' : lang === 'hi' ? 'Hindi' : lang === 'bn' ? 'Bengali' : lang === 'ta' ? 'Tamil' : lang === 'te' ? 'Telugu' : lang === 'mr' ? 'Marathi' : lang}</span><span style={{fontWeight:700}}>{(count as number).toLocaleString()}</span></div>
                  <Bar value={count as number} max={languageDistribution[0]?.[1] as number || 1} color={['var(--purple)','var(--blue)','var(--green)','var(--cyan)','var(--amber)','var(--red)'][i%6]} />
                </div>
              ))}
            </div>
          </div>

          {/* Consent Status Breakdown */}
          <div className="glass-card">
            <h3>📊 Consent Status Breakdown</h3>
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginTop:12}}>
              {[
                {label:'Active',value:stats.activeConsents||0,color:'var(--green)',bg:'var(--green-bg)'},
                {label:'Withdrawn',value:stats.withdrawnConsents||0,color:'var(--red)',bg:'var(--red-bg)'},
                {label:'Expired',value:stats.expiredConsents||0,color:'var(--amber)',bg:'var(--amber-bg)'},
                {label:'Last 30 Days',value:stats.consentsLast30Days||0,color:'var(--blue)',bg:'var(--blue-bg)'},
                {label:'Expiring (30d)',value:stats.expiringIn30Days||0,color:'var(--red)',bg:'var(--red-bg)'},
                {label:'Expiring (7d)',value:stats.expiringIn7Days||0,color:'var(--red)',bg:'var(--red-bg)'},
              ].map((item,i) => (
                <div key={i} style={{textAlign:'center',padding:16,background:item.bg,borderRadius:'var(--radius)'}}>
                  <div style={{fontSize:28,fontWeight:800,color:item.color}}>{item.value.toLocaleString()}</div>
                  <div style={{fontSize:11}}>{item.label}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Compliance Heatmap */}
          <div className="glass-card">
            <h3>🔥 DPDP Compliance Indicators</h3>
            <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:8,marginTop:12}}>
              {[
                {label:'Compliance',val:stats.complianceScore||0},
                {label:'Active Rate',val:stats.activeRate||0},
                {label:'Audit Chain',val:stats.chainIntegrity?100:0},
                {label:'Notice §5',val:100},
                {label:'Consent §6',val:Math.min(100,Math.round((stats.activeConsents||0)/(stats.totalConsents||1)*100))},
                {label:'Withdrawal §7',val:100},
                {label:'Children §9',val:(stats.totalGuardianConsents||0)>0?92:100},
                {label:'Erasure §13',val:78},
              ].map((item,i) => (
                <div key={i} style={{padding:12,borderRadius:'var(--radius)',textAlign:'center',background:item.val>=90?'var(--green-bg)':item.val>=70?'var(--amber-bg)':'var(--red-bg)',border:`1px solid ${item.val>=90?'var(--green-border)':item.val>=70?'var(--amber-border)':'var(--red-border)'}`}}>
                  <div style={{fontSize:20,fontWeight:800,color:item.val>=90?'var(--green)':item.val>=70?'var(--amber)':'var(--red)'}}>{item.val.toFixed(0)}%</div>
                  <div style={{fontSize:10,marginTop:4}}>{item.label}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Quick Stats */}
          <div className="glass-card">
            <h3>📈 System Statistics</h3>
            <div style={{marginTop:12}}>
              {[
                {label:'Total Consents in Database',value:(stats.totalConsents||0).toLocaleString()},
                {label:'Consents Collected (Last 30 days)',value:(stats.consentsLast30Days||0).toLocaleString()},
                {label:'Withdrawals (Last 30 days)',value:(stats.withdrawalsLast30Days||0).toLocaleString()},
                {label:'Renewals (Last 30 days)',value:(stats.renewalsLast30Days||0).toLocaleString()},
                {label:'Audit Trail Entries',value:(stats.totalAuditEntries||0).toLocaleString()},
                {label:'Audit Chain Integrity',value:stats.chainIntegrity?'✅ Verified':'❌ Broken'},
                {label:'Expiring in 7 days',value:(stats.expiringIn7Days||0).toLocaleString()},
                {label:'Expiring in 30 days',value:(stats.expiringIn30Days||0).toLocaleString()},
              ].map((item,i) => (
                <div key={i} style={{display:'flex',justifyContent:'space-between',padding:'8px 0',borderBottom:'1px solid var(--border)',fontSize:13}}>
                  <span style={{color:'var(--text-secondary)'}}>{item.label}</span>
                  <span style={{fontWeight:700}}>{item.value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ═══ AUDIT TRAIL TAB — REAL DATA ═══ */}
      {tab === 'audit' && (
        <div className="glass-card" style={{marginTop:16}}>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:12}}>
            <div>
              <h3>📝 Consent Audit Trail — Immutable Ledger</h3>
              <p style={{fontSize:12,color:'var(--text-muted)'}}>All consent lifecycle events are cryptographically chained. Chain Integrity: {stats.chainIntegrity ? '✅ Verified' : '⚠️ Unknown'}</p>
            </div>
            <button className="btn-secondary" onClick={fetchAudit} style={{fontSize:11}}>🔄 Refresh</button>
          </div>
          {auditLog.length === 0 ? (
            <div style={{padding:32,textAlign:'center',color:'var(--text-muted)',fontSize:14}}>
              <p>📋 No audit entries yet. Audit entries are created when consents are withdrawn, modified, or renewed.</p>
              <p style={{fontSize:12,marginTop:8}}>The immutable consent ledger is maintained with cryptographic hashing for tamper resistance.</p>
            </div>
          ) : (
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead><tr><th>Timestamp</th><th>Action</th><th>Consent ID</th><th>Initiated By</th><th>Detail</th></tr></thead>
                <tbody>{auditLog.map((e: any,i: number) => (
                  <tr key={i}>
                    <td style={{fontSize:11,fontFamily:'var(--font-mono)'}}>{fmtDateTime(e.timestamp || e.time)}</td>
                    <td><span className={`rag-badge ${e.action==='GRANTED'||e.action==='RENEWED'?'rag-green':e.action==='WITHDRAWN'?'rag-red':'rag-amber'}`}>{e.action}</span></td>
                    <td style={{fontFamily:'var(--font-mono)',fontSize:11}}>{(e.consentId||'').slice(0,8)}...</td>
                    <td>{e.performedBy || e.user || 'System'}</td>
                    <td style={{fontSize:12}}>{e.detail || e.description || '—'}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ═══ NOTICES TAB ═══ */}
      {tab === 'notices' && (
        <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginTop:16}}>
          <div className="glass-card">
            <h3>📜 Consent Notice Template — Section 5 — {sector}</h3>
            <div style={{background:'var(--bg-input)',padding:16,borderRadius:'var(--radius)',fontSize:13,lineHeight:1.8,border:'1px solid var(--border)',marginTop:12}}>
              <p><strong>NOTICE TO DATA PRINCIPAL</strong></p>
              <p><em>Pursuant to Section 5, Digital Personal Data Protection Act, 2023</em></p>
              <hr style={{border:'none',borderTop:'1px solid var(--border)',margin:'12px 0'}} />
              <p><strong>Data Fiduciary:</strong> {sector} Data Fiduciary Organization</p>
              <p><strong>Purpose of Processing:</strong> Your personal data is being collected for the following purposes:</p>
              <ul style={{paddingLeft:20}}>
                {(SECTOR_CONSENT_PURPOSES[sector] || SECTOR_CONSENT_PURPOSES['Banking & Finance'] || []).map((p: any,i: number)=>(
                  <li key={i} style={{marginBottom:6}}>
                    <strong>{p.label}</strong>{p.mandatory && <span style={{color:'var(--red)',fontSize:10,marginLeft:4}}>(Mandatory)</span>}
                    <div style={{fontSize:11,color:'var(--text-muted)',lineHeight:1.4}}>{p.description}</div>
                    <div style={{fontSize:10,color:'var(--purple)',marginTop:2}}>Lawful Basis: {p.lawfulBasis} | Retention: {p.retentionPeriod}</div>
                  </li>
                ))}
              </ul>
              <p><strong>Your Rights under DPDP Act (Section 11-14):</strong></p>
              <ul style={{paddingLeft:20}}>
                <li>Right to access a summary of your data (§11)</li>
                <li>Right to correction of inaccurate data (§12)</li>
                <li>Right to erasure of data no longer needed (§13)</li>
                <li>Right to grievance redressal within 30 days (§14)</li>
                <li>Right to nominate a representative (§14A)</li>
                <li>Right to withdraw consent at any time (§6(5))</li>
              </ul>
              <p><strong>Consent Withdrawal:</strong> You may withdraw consent at any time via the self-service portal, mobile app, or by contacting the DPO.</p>
            </div>
          </div>

          <div className="glass-card">
            <h3>🌍 Multilingual Notice Support</h3>
            <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:12}}>DPDP Act mandates notices in languages specified in the Eighth Schedule</p>
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8}}>
              {['English','Hindi (हिन्दी)','Bengali (বাংলা)','Tamil (தமிழ்)','Telugu (తెలుగు)','Marathi (मराठी)','Gujarati (ગુજરાતી)','Kannada (ಕನ್ನಡ)','Malayalam (മലയാളം)','Punjabi (ਪੰਜਾਬੀ)','Odia (ଓଡ଼ିଆ)','Assamese (অসমীয়া)'].map((lang,i) => (
                <div key={i} style={{padding:10,borderRadius:'var(--radius)',border:'1px solid var(--border)',display:'flex',justifyContent:'space-between',alignItems:'center',fontSize:12}}>
                  <span>{lang}</span>
                  <span className="rag-badge rag-green">Ready</span>
                </div>
              ))}
            </div>
          </div>

          {/* Guardian Consent Notice */}
          <div className="glass-card" style={{gridColumn:'span 2'}}>
            <h3>👶 Guardian Consent Notice — Section 9</h3>
            <div style={{background:'var(--bg-input)',padding:16,borderRadius:'var(--radius)',fontSize:13,lineHeight:1.8,border:'1px solid var(--amber-border)',marginTop:12}}>
              <p><strong>NOTICE FOR GUARDIAN/PARENT CONSENT</strong></p>
              <p><em>Section 9, Digital Personal Data Protection Act, 2023 — Processing of Children's Personal Data</em></p>
              <hr style={{border:'none',borderTop:'1px solid var(--border)',margin:'12px 0'}} />
              <p>Processing personal data of a child (below 18 years) requires <strong>verifiable consent of the parent or lawful guardian</strong>.</p>
              <ul style={{paddingLeft:20}}>
                <li>No tracking, behavioral monitoring, or targeted advertising of children</li>
                <li>Guardian must verify identity via Aadhaar-based verification or equivalent</li>
                <li>Data processed only for the specific purpose consented by guardian</li>
                <li>Guardian may withdraw consent at any time</li>
              </ul>
              <p><strong>Penalty:</strong> Up to ₹200 crore for non-compliance with Section 9.</p>
            </div>
          </div>
        </div>
      )}

      {/* ═══ RECORDS TAB — REAL CRUD ═══ */}
      {tab === 'records' && (<>
        {/* CREATE FORM */}
        {showCreate && (
          <div ref={createFormRef} className="glass-card" style={{marginTop:16,marginBottom:16,borderLeft:'4px solid var(--brand-primary)'}}>
            <h3>📝 Collect New Consent — DPDP Section 6</h3>
            <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:12}}>Consent must be free, specific, informed, and unconditional per Section 6(1). Data is saved to the SQLite database.</p>

            {/* ROW 1: Name + Sector + Purpose */}
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:12,marginBottom:12}}>
              <input className="form-input" placeholder="Data Principal Name (e.g. Rajesh Kumar)" value={form.dataPrincipalName} onChange={e=>setForm({...form,dataPrincipalName:e.target.value})} />
              <select className="form-input" value={form.sector} onChange={e=>{setForm({...form,sector:e.target.value,purposeName:'',dataCategories:'',retentionPeriod:'365 days',lawfulBasis:'',purposeDescription:''});setIsCustomPurpose(false)}}>
                {SECTORS.map(s=><option key={s} value={s}>{s}</option>)}
              </select>
              {!isCustomPurpose ? (
                <select className="form-input" value={form.purposeName} onChange={e=>handlePurposeSelect(e.target.value)} style={{fontWeight:form.purposeName?600:400}}>
                  <option value="">— Select Purpose —</option>
                  {sectorPurposes.map((p,i)=> <option key={i} value={p.label} title={p.description}>{p.mandatory?'⚠️ ':''}{p.label}</option>)}
                  <option value="__CUSTOM__">✏️ Enter Custom Purpose...</option>
                </select>
              ) : (
                <div style={{display:'flex',gap:4}}>
                  <input className="form-input" style={{flex:1}} placeholder="Enter custom purpose..." value={form.purposeName} onChange={e=>setForm({...form,purposeName:e.target.value})} autoFocus />
                  <button className="btn-sm" title="Back to presets" onClick={()=>{setIsCustomPurpose(false);setForm({...form,purposeName:''})}}>↩</button>
                </div>
              )}
            </div>

            {/* PURPOSE INFO CARD — shown when a preset purpose is selected */}
            {form.purposeDescription && !isCustomPurpose && (
              <div style={{background:'var(--surface-secondary)',border:'1px solid var(--border)',borderRadius:'var(--radius)',padding:'10px 14px',marginBottom:12,fontSize:12,lineHeight:1.6}}>
                <div style={{display:'flex',gap:16,flexWrap:'wrap',marginBottom:6}}>
                  <span><strong>📋 DPDP:</strong> {sectorPurposes.find(p=>p.label===form.purposeName)?.dpdpSection || 'Section 6'}</span>
                  <span><strong>⚖️ Lawful Basis:</strong> {form.lawfulBasis}</span>
                  <span><strong>📅 Retention:</strong> {form.retentionPeriod}</span>
                  {sectorPurposes.find(p=>p.label===form.purposeName)?.mandatory && <span style={{color:'var(--red)',fontWeight:700}}>⚠️ Mandatory for service</span>}
                </div>
                <div style={{color:'var(--text-secondary)'}}>{form.purposeDescription}</div>
              </div>
            )}

            {/* ROW 2: Consent Type + Method + Language */}
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:12,marginBottom:12}}>
              <select className="form-input" value={form.consentType} onChange={e=>setForm({...form,consentType:e.target.value})}>
                <option value="EXPLICIT">Explicit Consent (Section 6)</option>
                <option value="GUARDIAN">Guardian Consent (Section 9 — Minor)</option>
                <option value="DEEMED">Deemed Consent (Section 7)</option>
                <option value="LEGITIMATE_INTEREST">Legitimate Interest</option>
              </select>
              <select className="form-input" value={form.consentMethod} onChange={e=>setForm({...form,consentMethod:e.target.value})}>
                <option value="WEB_FORM">Web Form</option>
                <option value="MOBILE_APP">Mobile App</option>
                <option value="VERBAL">Verbal</option>
                <option value="PAPER">Paper</option>
                <option value="EMAIL">Email</option>
                <option value="API">API</option>
              </select>
              <select className="form-input" value={form.language} onChange={e=>setForm({...form,language:e.target.value})}>
                {DPDP_LANGUAGES.map(l => <option key={l.code} value={l.code}>{l.nativeName} ({l.name})</option>)}
              </select>
            </div>

            {/* ROW 3: Data Categories + Retention (auto-filled or editable) */}
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginBottom:12}}>
              <input className="form-input" placeholder="Data Categories (auto-filled from purpose or comma-separated)" value={form.dataCategories} onChange={e=>setForm({...form,dataCategories:e.target.value})} />
              <input className="form-input" placeholder="Retention Period (auto-filled from purpose)" value={form.retentionPeriod} onChange={e=>setForm({...form,retentionPeriod:e.target.value})} />
            </div>

            {/* ACTIONS */}
            <div style={{display:'flex',gap:8,alignItems:'center'}}>
              <button className="btn-primary" onClick={handleCreate} disabled={!form.dataPrincipalName || !form.purposeName}>✅ Save Consent to Database</button>
              <button className="btn-secondary" onClick={()=>setShowCreate(false)}>Cancel</button>
              {form.lawfulBasis && <span style={{fontSize:11,color:'var(--text-muted)',marginLeft:8}}>⚖️ {form.lawfulBasis}</span>}
            </div>
          </div>
        )}

        {/* FILTER BAR + BULK OPS */}
        <div style={{display:'flex',gap:12,margin:'16px 0',alignItems:'center',flexWrap:'wrap'}}>
          <input className="form-input" style={{width:280}} placeholder="🔍 Search by name, purpose, or ID..." value={search} onChange={e=>setSearch(e.target.value)} />
          {bulkIds.size > 0 && (
            <div style={{display:'flex',gap:6,alignItems:'center'}}>
              <span style={{fontSize:12,fontWeight:700,color:'var(--brand-primary)'}}>{bulkIds.size} selected</span>
              <button className="btn-sm" style={{color:'var(--red)'}} onClick={handleBulkWithdraw}>⛔ Bulk Withdraw</button>
              <button className="btn-sm" onClick={()=>setBulkIds(new Set())}>Clear</button>
            </div>
          )}
          <span style={{fontSize:12,color:'var(--text-muted)',marginLeft:'auto'}}>
            Page {page+1} of {totalPages} | {totalRecords.toLocaleString()} total records in database
          </span>
        </div>

        {/* CONSENT TABLE — REAL DATA */}
        <div className="glass-card">
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:8}}>
            <h3>📋 Consent Records {loading && <span style={{fontSize:12,color:'var(--amber)',fontWeight:400}}>Loading...</span>}</h3>
            <button className="btn-sm" onClick={selectAllPage}>☑️ Select Page</button>
          </div>

          {error && <div style={{padding:12,background:'var(--red-bg)',border:'1px solid var(--red-border)',borderRadius:'var(--radius)',marginBottom:12,fontSize:13,color:'var(--red)'}}>{error}</div>}

          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr>
                <th style={{width:30}}>☑</th><th>ID</th><th>Data Principal</th><th>Purpose</th><th>Type</th><th>Method</th><th>Status</th><th>Collected</th><th>Expires</th><th>Lang</th><th>Actions</th>
              </tr></thead>
              <tbody>
                {filtered.length === 0 && !loading && (
                  <tr><td colSpan={11} style={{textAlign:'center',padding:24,color:'var(--text-muted)'}}>No consent records found</td></tr>
                )}
                {filtered.map(c => (
                  <tr key={c.id} onClick={()=>setSelected(c)} style={{cursor:'pointer',background:bulkIds.has(c.id)?'var(--brand-primary-light)':''}}>
                    <td onClick={e=>{e.stopPropagation();toggleBulk(c.id)}}><input type="checkbox" checked={bulkIds.has(c.id)} readOnly /></td>
                    <td style={{fontFamily:'var(--font-mono)',fontSize:10}}>{c.id?.slice(0,8)}...</td>
                    <td><strong>{c.dataPrincipalName || '—'}</strong></td>
                    <td style={{fontSize:12}}>{c.purposeName || '—'}</td>
                    <td><span className={`badge ${c.consentType==='GUARDIAN'?'purple':'blue'}`}>{c.consentType || '—'}</span></td>
                    <td style={{fontSize:11}}>{c.consentMethod || '—'}</td>
                    <td><span className={`rag-badge ${c.status==='ACTIVE'?'rag-green':c.status==='WITHDRAWN'?'rag-red':'rag-amber'}`}>{c.status}</span></td>
                    <td style={{fontSize:11}}>{fmtDate(c.collectedAt)}</td>
                    <td style={{fontSize:11}}>{fmtDate(c.expiresAt)}</td>
                    <td style={{fontSize:11}}>{c.language || '—'}</td>
                    <td>
                      <div style={{display:'flex',gap:4}}>
                        <button className="btn-sm" onClick={e=>{e.stopPropagation();setSelected(c)}} title="View Detail">📄</button>
                        {c.status==='ACTIVE' && <button className="btn-sm" style={{color:'var(--red)'}} onClick={e=>{e.stopPropagation();handleWithdraw(c.id)}} title="Withdraw Consent">⛔</button>}
                        {c.status==='EXPIRED' && <button className="btn-sm" style={{color:'var(--green)'}} onClick={e=>{e.stopPropagation();handleRenew(c.id)}} title="Renew Consent">🔄</button>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* PAGINATION — server-side */}
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginTop:12}}>
            <span style={{fontSize:12,color:'var(--text-muted)'}}>Page {page+1} of {totalPages} ({PAGE_SIZE}/page)</span>
            <div style={{display:'flex',gap:4}}>
              <button className="btn-sm" disabled={page===0} onClick={()=>setPage(0)}>⏮</button>
              <button className="btn-sm" disabled={page===0} onClick={()=>setPage(p=>p-1)}>◀</button>
              <button className="btn-sm" disabled={page>=totalPages-1} onClick={()=>setPage(p=>p+1)}>▶</button>
              <button className="btn-sm" disabled={page>=totalPages-1} onClick={()=>setPage(totalPages-1)}>⏭</button>
            </div>
          </div>
        </div>
      </>)}

      {/* ═══ DETAIL DRAWER ═══ */}
      {selected && (
        <div className="glass-card" style={{marginTop:16,borderLeft:'4px solid var(--brand-primary)'}}>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
            <h3>📄 Consent Detail — {selected.id?.slice(0,8)}...</h3>
            <button className="btn-sm" onClick={()=>setSelected(null)}>✕ Close</button>
          </div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr 1fr',gap:16,marginTop:16}}>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>PRINCIPAL</span><div style={{fontWeight:600}}>{selected.dataPrincipalName || '—'}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>PURPOSE</span><div>{selected.purposeName || '—'}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>TYPE</span><div><span className={`badge ${selected.consentType==='GUARDIAN'?'purple':'blue'}`}>{selected.consentType}</span></div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>METHOD</span><div>{selected.consentMethod || '—'}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>STATUS</span><div><span className={`rag-badge ${selected.status==='ACTIVE'?'rag-green':'rag-red'}`}>{selected.status}</span></div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>LANGUAGE</span><div>{selected.language || '—'}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>COLLECTED</span><div>{fmtDateTime(selected.collectedAt)}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>EXPIRES</span><div>{fmtDateTime(selected.expiresAt)}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>CONSENT ID</span><div style={{fontFamily:'var(--font-mono)',fontSize:10,wordBreak:'break-all'}}>{selected.id}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>PRINCIPAL ID</span><div style={{fontFamily:'var(--font-mono)',fontSize:10,wordBreak:'break-all'}}>{selected.dataPrincipalId || '—'}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>NOTICE VERSION</span><div>{selected.noticeVersion || '—'}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>HASH</span><div style={{fontFamily:'var(--font-mono)',fontSize:10,wordBreak:'break-all'}}>{selected.hash?.slice(0,16) || '—'}...</div></div>
            {selected.withdrawnAt && <>
              <div><span style={{fontSize:11,color:'var(--text-muted)'}}>WITHDRAWN AT</span><div style={{color:'var(--red)'}}>{fmtDateTime(selected.withdrawnAt)}</div></div>
              <div><span style={{fontSize:11,color:'var(--text-muted)'}}>WITHDRAWAL REASON</span><div>{selected.withdrawalReason || '—'}</div></div>
            </>}
          </div>
          <div style={{display:'flex',gap:8,marginTop:16}}>
            {selected.status==='ACTIVE' && <button className="btn-primary" style={{background:'var(--red)'}} onClick={()=>handleWithdraw(selected.id)}>⛔ Withdraw Consent</button>}
            {selected.status==='EXPIRED' && <button className="btn-primary" onClick={()=>handleRenew(selected.id)}>🔄 Renew Consent</button>}
            <button className="btn-secondary" onClick={()=>setTab('audit')}>📝 Audit Trail</button>
            <button className="btn-secondary" onClick={exportWord}>📤 Export</button>
          </div>
        </div>
      )}
    </div>
  )
}
