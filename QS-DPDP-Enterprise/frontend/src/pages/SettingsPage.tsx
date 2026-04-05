import { useState } from 'react'
import { SECTORS, SECTOR_POLICIES, SECTOR_CONTROLS, SECTOR_QUESTIONS } from '../data/sectorData'
import { useAppContext } from '../context/AppContext'

const LANGUAGES = ['English','Hindi','Bengali','Marathi','Tamil','Telugu','Kannada','Gujarati','Malayalam','Odia','Punjabi','Assamese','Urdu','Sanskrit','Sindhi','Kashmiri','Nepali','Konkani','Manipuri','Dogri','Maithili','Santali','Bodo']
const DATABASES = ['SQLite (Default — Embedded)','PostgreSQL','MySQL','Oracle','Microsoft SQL Server','MariaDB']

const MAX_RESETS = 3
const RESET_LABELS = ['1st Reset — Training','2nd Reset — Testing','3rd Reset — Production (Final)']

export default function SettingsPage() {
  const { sector: globalSector, setSector: setGlobalSector, language: globalLanguage, setLanguage: setGlobalLanguage, t } = useAppContext()
  const [tab, setTab] = useState<'sector'|'language'|'db'|'org'|'hierarchy'|'notifications'|'security'|'backup'>('sector')
  const [org, setOrg] = useState({name:'NeurQ AI Labs Pvt Ltd',address:'Mumbai, Maharashtra',city:'Mumbai',pin:'400001',phone:'+91-9876543210',email:'admin@neurqai.com',altEmail:'support@neurqai.com',gst:'27AABCN1234A1Z5',pan:'AABCN1234A',cin:'U72200MH2024PTC123456',sector:'IT / Software',website:'www.neurqai.com'})
  const [hierarchy, setHierarchy] = useState([
    {level:0,title:'Assistant',dpdpRole:'Data Processor Operator'},
    {level:1,title:'Assistant Manager',dpdpRole:'Data Handler'},
    {level:2,title:'Manager',dpdpRole:'Consent Manager'},
    {level:3,title:'AGM',dpdpRole:'Data Protection Analyst'},
    {level:4,title:'DGM',dpdpRole:'Grievance Officer (Sec 14)'},
    {level:5,title:'GM',dpdpRole:'DPO — Data Protection Officer (Sec 10)'},
    {level:6,title:'MD',dpdpRole:'Data Fiduciary Representative'},
    {level:7,title:'Chairman',dpdpRole:'Data Fiduciary (Sec 2(i))'},
  ])
  const [selectedDb, setSelectedDb] = useState('SQLite (Default — Embedded)')
  const [dbConn, setDbConn] = useState({host:'localhost',port:'5432',database:'qsdpdp_db',username:'admin',password:''})
  const [dbTestResult, setDbTestResult] = useState<'idle'|'testing'|'success'|'fail'>('idle')
  const [selectedLangs, setSelectedLangs] = useState(['English','Hindi'])
  const selectedSector = globalSector
  const [saved, setSaved] = useState(false)
  const [sectorLoaded, setSectorLoaded] = useState(false)
  const [resetCount, setResetCount] = useState(() => parseInt(localStorage.getItem('qsdpdp_reset_count')||'0'))
  const [showResetConfirm, setShowResetConfirm] = useState(false)
  const [resetInProgress, setResetInProgress] = useState(false)
  // Notification settings
  const [notif, setNotif] = useState({emailEnabled:true,smsEnabled:false,webhookEnabled:true,slackEnabled:false,
    smtpHost:'smtp.gmail.com',smtpPort:'587',smtpUser:'alerts@neurqai.com',smtpPass:'',
    smsGateway:'MSG91',smsApiKey:'',webhookUrl:'https://hooks.neurqai.com/dpdp-alerts',
    slackWebhook:'',breachAlerts:true,consentAlerts:true,rightsAlerts:true,slaAlerts:true,
    dpbiNotifyHrs:'72',certinNotifyHrs:'6',escalationEnabled:true})
  // Security / PQC settings
  const [sec, setSec] = useState({mfaEnabled:true,mfaMethod:'TOTP',sessionTimeout:'30',maxLoginAttempts:'5',
    passwordMinLength:'12',passwordComplexity:true,ipWhitelist:'',
    pqcKeyEncap:'ML-KEM-1024',pqcSignature:'ML-DSA-87',pqcSymmetric:'AES-256-GCM',pqcFallback:'RSA-4096',
    tlsVersion:'TLS 1.3',hsm:'Software (SoftHSM2)',auditLogRetention:'365',dataEncryptAtRest:true,
    consentTokenExpiry:'365',apiRateLimit:'1000',corsOrigins:'*'})
  // Backup settings
  const [backup, setBackup] = useState({autoBackup:true,frequency:'Daily',time:'02:00',
    retention:'90',destination:'Local + Cloud',cloudProvider:'AWS S3',
    s3Bucket:'qsdpdp-backups',s3Region:'ap-south-1',s3AccessKey:'',s3SecretKey:'',
    encryptBackup:true,lastBackup:'2026-03-13 02:00:15',lastBackupSize:'142 MB',
    backupHistory:[
      {date:'2026-03-13 02:00',size:'142 MB',status:'Success',type:'Full'},
      {date:'2026-03-12 02:00',size:'138 MB',status:'Success',type:'Full'},
      {date:'2026-03-11 02:00',size:'135 MB',status:'Success',type:'Full'},
      {date:'2026-03-10 14:30',size:'12 MB',status:'Success',type:'Incremental'},
      {date:'2026-03-10 02:00',size:'131 MB',status:'Success',type:'Full'},
    ]})

  const save = () => { setSaved(true); setTimeout(()=>setSaved(false),2000) }
  const testDbConnection = () => {
    setDbTestResult('testing')
    setTimeout(()=>{
      if(selectedDb==='SQLite (Default — Embedded)' || (dbConn.host && dbConn.port && dbConn.database && dbConn.username)){
        setDbTestResult('success')
      } else { setDbTestResult('fail') }
      setTimeout(()=>setDbTestResult('idle'),4000)
    },1500)
  }

  const handleResetDb = () => {
    if (resetCount >= MAX_RESETS) return
    setResetInProgress(true)
    setTimeout(() => {
      const newCount = resetCount + 1
      setResetCount(newCount)
      localStorage.setItem('qsdpdp_reset_count', String(newCount))
      setResetInProgress(false)
      setShowResetConfirm(false)
      setSaved(true)
      setTimeout(()=>setSaved(false),3000)
    }, 2500)
  }

  // Auto-load sector resources → pushes to global context
  const applySector = (s: string) => {
    setGlobalSector(s)
    setSectorLoaded(true)
    setTimeout(()=>setSectorLoaded(false),3000)
  }

  const sectorPolicies = SECTOR_POLICIES[selectedSector] || SECTOR_POLICIES['Banking & Finance']
  const sectorControls = SECTOR_CONTROLS[selectedSector] || SECTOR_CONTROLS['Banking & Finance']
  const sectorQuestions = SECTOR_QUESTIONS[selectedSector] || SECTOR_QUESTIONS['Banking & Finance']

  return (
    <div className="page-container">
      <div className="page-header"><h1>⚙️ Settings</h1><p>Organization, hierarchy, database, language, and sector configuration</p></div>

      <div style={{display:'flex',gap:8,marginBottom:24,flexWrap:'wrap'}}>
        {(['sector','language','db','org','hierarchy','notifications','security','backup'] as const).map(t=>(
          <button key={t} className={tab===t?'btn-primary':'btn-secondary'} onClick={()=>setTab(t)}>
            {t==='sector'?'🏭 Sector':t==='language'?'🌐 Language':t==='db'?'🗄️ Database':t==='org'?'🏢 Organization':t==='hierarchy'?'👥 Hierarchy':t==='notifications'?'🔔 Notifications':t==='security'?'🔐 Security':'💾 Backup'}
          </button>
        ))}
      </div>

      {saved && <div style={{background:'var(--green)',color:'#fff',padding:'12px 20px',borderRadius:8,marginBottom:16,fontWeight:600}}>✅ Settings saved successfully!</div>}

      {/* ORGANIZATION */}
      {tab==='org' && (
        <div className="glass-card">
          <h3>🏢 Organization Details</h3>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginTop:16}}>
            {Object.entries(org).filter(([k])=>k!=='sector').map(([k,v])=>(
              <div key={k}>
                <label style={{display:'block',fontSize:11,fontWeight:600,color:'var(--text-muted)',marginBottom:4,textTransform:'uppercase'}}>{k.replace(/([A-Z])/g,' $1')}</label>
                <input className="form-input" value={v} onChange={e=>setOrg({...org,[k]:e.target.value})} />
              </div>
            ))}
            {/* SECTOR — Google Material-style dropdown with auto-load */}
            <div style={{gridColumn:'1 / -1'}}>
              <label style={{display:'block',fontSize:11,fontWeight:700,color:'var(--brand-primary)',marginBottom:6,textTransform:'uppercase',letterSpacing:'0.5px'}}>🏭 Sector — Auto-loads sector-specific policies, consents, assessments & demo data</label>
              <div style={{position:'relative'}}>
                <select
                  className="form-input"
                  value={org.sector}
                  onChange={e=>{
                    const s = e.target.value
                    setOrg({...org, sector: s})
                    applySector(s)
                  }}
                  style={{
                    padding:'14px 48px 14px 16px',fontSize:15,fontWeight:600,cursor:'pointer',
                    appearance:'none',WebkitAppearance:'none',
                    background:'var(--bg-card)',
                    border:'2px solid var(--brand-primary)',
                    borderRadius:12,color:'var(--text-primary)',
                    boxShadow:'0 2px 8px rgba(106,17,203,0.15)',
                    transition:'all 0.3s ease'
                  }}
                >
                  <option value="">— Select Sector —</option>
                  {SECTORS.map(s=>(
                    <option key={s} value={s}>{s}</option>
                  ))}
                </select>
                <span style={{position:'absolute',right:16,top:'50%',transform:'translateY(-50%)',pointerEvents:'none',fontSize:18}}>▼</span>
              </div>
              {sectorLoaded && (
                <div style={{marginTop:12,padding:'12px 16px',borderRadius:10,background:'linear-gradient(135deg,rgba(16,185,129,0.12),rgba(16,185,129,0.05))',border:'1px solid rgba(16,185,129,0.3)',display:'flex',alignItems:'center',gap:10,animation:'fadeIn 0.3s ease'}}>
                  <span style={{fontSize:24}}>✅</span>
                  <div>
                    <strong style={{color:'var(--green)'}}>Sector "{selectedSector}" loaded successfully!</strong>
                    <div style={{fontSize:12,color:'var(--text-muted)',marginTop:2}}>
                      📋 {sectorPolicies.length} policies • 🛡️ {sectorControls.length} controls • ❓ {sectorQuestions.length} assessment questions • 📊 Demo data activated
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
          <button className="btn-primary" style={{marginTop:20}} onClick={save}>💾 Save Organization Settings</button>
        </div>
      )}

      {/* HIERARCHY */}
      {tab==='hierarchy' && (
        <div className="glass-card">
          <h3>👥 Organizational Hierarchy → DPDP Role Mapping</h3>
          <p style={{color:'var(--text-muted)',marginBottom:16}}>Map your organization's levels to DPDP Act roles (Section 2, 10, 14)</p>
          <div className="data-table-wrapper"><table className="data-table">
            <thead><tr><th>Level</th><th>Designation</th><th>DPDP Act Role</th><th>Actions</th></tr></thead>
            <tbody>{hierarchy.map((h,i)=>(
              <tr key={i}>
                <td><span style={{background:'var(--brand-primary)',color:'#fff',padding:'2px 10px',borderRadius:12,fontWeight:700,fontSize:12}}>L{h.level}</span></td>
                <td><input className="form-input" value={h.title} onChange={e=>{const n=[...hierarchy];n[i].title=e.target.value;setHierarchy(n)}} /></td>
                <td><input className="form-input" value={h.dpdpRole} onChange={e=>{const n=[...hierarchy];n[i].dpdpRole=e.target.value;setHierarchy(n)}} /></td>
                <td><button className="btn-sm" onClick={()=>setHierarchy(hierarchy.filter((_,j)=>j!==i))}>🗑️</button></td>
              </tr>
            ))}</tbody>
          </table></div>
          <div style={{display:'flex',gap:8,marginTop:16}}>
            <button className="btn-secondary" onClick={()=>setHierarchy([...hierarchy,{level:hierarchy.length,title:'',dpdpRole:''}])}>+ Add Level</button>
            <button className="btn-primary" onClick={save}>💾 Save Hierarchy</button>
          </div>
        </div>
      )}

      {/* DATABASE */}
      {tab==='db' && (
        <div className="glass-card">
          <h3>🗄️ Database Configuration</h3>
          <p style={{color:'var(--text-muted)',marginBottom:16}}>Select database backend. SQLite is pre-configured and requires no setup.</p>
          <div style={{display:'grid',gap:12}}>
            {DATABASES.map(db=>(
              <button key={db} onClick={()=>setSelectedDb(db)} style={{
                padding:'16px 20px',textAlign:'left',borderRadius:'var(--radius)',border:selectedDb===db?'2px solid var(--brand-primary)':'1px solid var(--border)',
                background:selectedDb===db?'var(--brand-primary-light)':'var(--bg-card)',cursor:'pointer',transition:'all 0.2s',fontFamily:'inherit',color:'var(--text-primary)'
              }}>
                <strong>{db}</strong>
                {db.includes('SQLite') && <span style={{display:'block',fontSize:12,color:'var(--text-muted)'}}>Zero configuration — pre-created and ready</span>}
                {db.includes('PostgreSQL') && <span style={{display:'block',fontSize:12,color:'var(--text-muted)'}}>Enterprise-grade, recommended for production</span>}
                {db.includes('Oracle') && <span style={{display:'block',fontSize:12,color:'var(--text-muted)'}}>Enterprise database with advanced features</span>}
              </button>
            ))}
          </div>
          {selectedDb!=='SQLite (Default — Embedded)' && (
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginTop:16,padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <input className="form-input" placeholder="Host" value={dbConn.host} onChange={e=>setDbConn({...dbConn,host:e.target.value})} />
              <input className="form-input" placeholder="Port" value={dbConn.port} onChange={e=>setDbConn({...dbConn,port:e.target.value})} />
              <input className="form-input" placeholder="Database name" value={dbConn.database} onChange={e=>setDbConn({...dbConn,database:e.target.value})} />
              <input className="form-input" placeholder="Username" value={dbConn.username} onChange={e=>setDbConn({...dbConn,username:e.target.value})} />
              <input className="form-input" placeholder="Password" type="password" value={dbConn.password} onChange={e=>setDbConn({...dbConn,password:e.target.value})} />
              <button className="btn-primary" onClick={testDbConnection} disabled={dbTestResult==='testing'}>
                {dbTestResult==='testing'?'⏳ Testing...':dbTestResult==='success'?'✅ Connected!':dbTestResult==='fail'?'❌ Failed':'🔗 Test Connection'}
              </button>
            </div>
          )}
          <button className="btn-primary" style={{marginTop:16}} onClick={save}>💾 Save Database Settings</button>

          {/* ═══ DANGER ZONE — RESET DATABASE ═══ */}
          <div style={{marginTop:32,border:'2px solid var(--red)',borderRadius:'var(--radius)',padding:24,background:'var(--red-bg)'}}>
            <div style={{display:'flex',alignItems:'center',gap:12,marginBottom:16}}>
              <span style={{fontSize:28}}>⚠️</span>
              <div>
                <h3 style={{color:'var(--red)',margin:0}}>Danger Zone — Reset Database</h3>
                <p style={{fontSize:12,color:'var(--text-muted)',marginTop:4}}>This action will permanently delete ALL data and reset the database to factory defaults.</p>
              </div>
            </div>

            {/* Usage Indicators */}
            <div style={{display:'flex',gap:8,marginBottom:16}}>
              {RESET_LABELS.map((label, i) => (
                <div key={i} style={{
                  flex:1,padding:'10px 12px',borderRadius:'var(--radius)',textAlign:'center',fontSize:12,fontWeight:600,
                  background: i < resetCount ? 'var(--red)' : 'var(--bg-glass)',
                  color: i < resetCount ? '#fff' : 'var(--text-muted)',
                  border: `1px solid ${i < resetCount ? 'var(--red)' : 'var(--border)'}`,
                }}>
                  {i < resetCount ? '✅ Used' : `${i + 1}`} — {label.split(' — ')[1]}
                </div>
              ))}
            </div>

            <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:16}}>
              <strong>Resets Used: {resetCount} / {MAX_RESETS}</strong> — 
              {resetCount === 0 && ' All 3 resets available. Use wisely: 1st for Training, 2nd for Testing, 3rd for Production start.'}
              {resetCount === 1 && ' 2 resets remaining. Next reset: Testing environment.'}
              {resetCount === 2 && ' ⚠️ Last reset remaining! This will be your FINAL reset for Production start.'}
              {resetCount >= MAX_RESETS && ' 🚫 All resets exhausted. Database cannot be reset again.'}
            </p>

            <button
              onClick={() => setShowResetConfirm(true)}
              disabled={resetCount >= MAX_RESETS || resetInProgress}
              style={{
                padding:'14px 32px',borderRadius:'var(--radius)',fontWeight:700,fontSize:14,cursor:resetCount>=MAX_RESETS?'not-allowed':'pointer',
                background:resetCount>=MAX_RESETS?'var(--border)':'var(--red)',color:resetCount>=MAX_RESETS?'var(--text-muted)':'#fff',
                border:'2px solid var(--red)',opacity:resetCount>=MAX_RESETS?0.5:1,fontFamily:'inherit',
              }}
            >
              {resetCount >= MAX_RESETS ? '🚫 Reset Disabled (All 3 Used)' : resetInProgress ? '⏳ Resetting...' : `⚠️ Reset Database (${MAX_RESETS - resetCount} remaining)`}
            </button>
          </div>

          {/* CONFIRMATION POPUP */}
          {showResetConfirm && (
            <div style={{position:'fixed',top:0,left:0,right:0,bottom:0,background:'rgba(0,0,0,0.7)',display:'flex',alignItems:'center',justifyContent:'center',zIndex:9999}}>
              <div style={{background:'var(--bg-card)',padding:32,borderRadius:'var(--radius)',maxWidth:500,border:'2px solid var(--red)',boxShadow:'0 20px 40px rgba(0,0,0,0.3)'}}>
                <div style={{textAlign:'center',marginBottom:20}}>
                  <div style={{fontSize:48,marginBottom:8}}>⚠️</div>
                  <h2 style={{color:'var(--red)',margin:'0 0 8px'}}>Confirm Database Reset</h2>
                  <p style={{fontSize:14,color:'var(--text-muted)'}}>This is <strong>Reset #{resetCount + 1} of {MAX_RESETS}</strong></p>
                </div>

                <div style={{background:'var(--red-bg)',padding:16,borderRadius:'var(--radius)',marginBottom:20,fontSize:13}}>
                  <strong style={{color:'var(--red)'}}>⚠️ WARNING:</strong> This will:
                  <ul style={{margin:'8px 0 0',paddingLeft:20,lineHeight:2}}>
                    <li>Delete ALL records from all modules</li>
                    <li>Reset consent, breach, DPIA, and rights records</li>
                    <li>Clear SIEM events, DLP incidents, and EDR data</li>
                    <li>Remove all policies and assessment results</li>
                    <li>Reset settings to factory defaults</li>
                  </ul>
                </div>

                <div style={{background:'var(--bg-glass)',padding:12,borderRadius:'var(--radius)',marginBottom:20,fontSize:12}}>
                  <strong>Purpose of this reset:</strong> {RESET_LABELS[resetCount] || 'N/A'}
                  {resetCount === MAX_RESETS - 1 && <div style={{color:'var(--red)',fontWeight:700,marginTop:4}}>⚠️ This is your LAST reset. After this, the reset button will be permanently disabled.</div>}
                </div>

                {resetInProgress ? (
                  <div style={{textAlign:'center'}}>
                    <div style={{background:'var(--border)',borderRadius:8,height:8,overflow:'hidden',marginBottom:12}}>
                      <div style={{width:'100%',height:'100%',background:'var(--red)',borderRadius:8,animation:'pulse 1s infinite'}} />
                    </div>
                    <p style={{color:'var(--red)',fontWeight:600}}>⏳ Resetting database... Please wait...</p>
                  </div>
                ) : (
                  <div style={{display:'flex',gap:12,justifyContent:'center'}}>
                    <button onClick={handleResetDb} style={{
                      padding:'12px 32px',background:'var(--red)',color:'#fff',border:'none',borderRadius:'var(--radius)',fontWeight:700,cursor:'pointer',fontFamily:'inherit',fontSize:14
                    }}>⚠️ YES — Reset Database</button>
                    <button onClick={()=>setShowResetConfirm(false)} className="btn-secondary" style={{padding:'12px 32px'}}>Cancel</button>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* LANGUAGE */}
      {tab==='language' && (
        <div className="glass-card">
          <h3>🌐 Language Configuration</h3>
          <p style={{color:'var(--text-muted)',marginBottom:16}}>Select primary UI language. All modules will auto-update to display in selected language.</p>

          {/* Primary UI Language */}
          <div style={{marginBottom:20}}>
            <label style={{display:'block',fontSize:12,fontWeight:700,marginBottom:8,color:'var(--brand-primary)'}}>PRIMARY UI LANGUAGE</label>
            <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:8}}>
              {['English','Hindi','Bengali','Tamil','Telugu','Marathi'].map(lang=>(
                <button key={lang} onClick={()=>setGlobalLanguage(lang)} style={{
                  padding:'14px',borderRadius:'var(--radius)',border:globalLanguage===lang?'2px solid var(--brand-primary)':'1px solid var(--border)',
                  background:globalLanguage===lang?'var(--brand-primary-light)':'var(--bg-card)',cursor:'pointer',fontSize:14,fontWeight:globalLanguage===lang?700:400,fontFamily:'inherit',color:'var(--text-primary)',
                  boxShadow:globalLanguage===lang?'0 0 12px rgba(124,58,237,0.3)':'none',
                }}>
                  {globalLanguage===lang?'✅ ':''}{lang}
                </button>
              ))}
            </div>
            <p style={{marginTop:8,fontSize:12,color:'var(--text-muted)'}}>Active UI Language: <strong>{globalLanguage}</strong> — All labels, notices, and reports will use this language</p>
          </div>

          {/* Additional languages for notices */}
          <label style={{display:'block',fontSize:12,fontWeight:700,marginBottom:8,color:'var(--text-muted)'}}>ADDITIONAL NOTICE LANGUAGES</label>
          <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:8}}>
            {LANGUAGES.map(lang=>(
              <button key={lang} onClick={()=>setSelectedLangs(selectedLangs.includes(lang)?selectedLangs.filter(l=>l!==lang):[...selectedLangs,lang])} style={{
                padding:'12px',borderRadius:'var(--radius)',border:selectedLangs.includes(lang)?'2px solid var(--brand-primary)':'1px solid var(--border)',
                background:selectedLangs.includes(lang)?'var(--brand-primary-light)':'var(--bg-card)',cursor:'pointer',fontSize:13,fontWeight:selectedLangs.includes(lang)?700:400,fontFamily:'inherit',color:'var(--text-primary)'
              }}>
                {selectedLangs.includes(lang)?'✅ ':''}{lang}
              </button>
            ))}
          </div>
          <p style={{marginTop:16,fontSize:13,color:'var(--text-muted)'}}>Notice languages: {selectedLangs.join(', ')}</p>
          <button className="btn-primary" style={{marginTop:12}} onClick={save}>💾 {t('save')} Language Settings</button>
        </div>
      )}

      {/* SECTOR — Auto-loads policies, controls, questions */}
      {tab==='sector' && (
        <div>
          <div className="glass-card">
            <h3>🏭 Sector Selection</h3>
            <p style={{color:'var(--text-muted)',marginBottom:16}}>Selecting a sector automatically loads sector-specific policies, procedures, controls, assessment questions, and demo data.</p>

            {sectorLoaded && (
              <div style={{background:'var(--green-bg)',border:'1px solid var(--green-border)',padding:'12px 20px',borderRadius:'var(--radius)',marginBottom:16}}>
                ✅ <strong>{selectedSector}</strong> — Auto-loaded: {sectorPolicies.length} policies, {sectorControls.length} controls, {sectorQuestions.length} assessment questions, 500+ consent records, 50+ breach records
              </div>
            )}

            <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:12}}>
              {SECTORS.map(s=>(
                <button key={s} onClick={()=>applySector(s)} style={{
                  padding:'20px',borderRadius:'var(--radius)',border:selectedSector===s?'2px solid var(--brand-primary)':'1px solid var(--border)',
                  background:selectedSector===s?'var(--brand-primary-light)':'var(--bg-card)',cursor:'pointer',textAlign:'center',fontSize:14,fontWeight:selectedSector===s?700:400,fontFamily:'inherit',color:'var(--text-primary)'
                }}>
                  {selectedSector===s?'✅ ':''}{s}
                </button>
              ))}
            </div>
          </div>

          {/* Auto-loaded Resources Summary */}
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginTop:16}}>
            <div className="glass-card">
              <h3>📋 Loaded Policies ({sectorPolicies.length})</h3>
              <div className="data-table-wrapper" style={{maxHeight:300,overflow:'auto'}}>
                <table className="data-table">
                  <thead><tr><th>ID</th><th>Title</th><th>DPDP</th><th>Score</th></tr></thead>
                  <tbody>{sectorPolicies.map((p,i) => (
                    <tr key={i}>
                      <td style={{fontSize:11,fontFamily:'monospace'}}>{p.id}</td>
                      <td><strong>{p.title}</strong></td>
                      <td style={{fontSize:11}}>{p.dpdpSection}</td>
                      <td><span style={{fontWeight:700,color:p.complianceScore>=80?'var(--green)':'var(--amber)'}}>{p.complianceScore}%</span></td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            </div>

            <div className="glass-card">
              <h3>🔒 Compliance Controls ({sectorControls.length})</h3>
              <div className="data-table-wrapper" style={{maxHeight:300,overflow:'auto'}}>
                <table className="data-table">
                  <thead><tr><th>Control</th><th>Status</th><th>Score</th></tr></thead>
                  <tbody>{sectorControls.map((c,i) => (
                    <tr key={i}>
                      <td>{c.control}</td>
                      <td>{c.implemented ? <span className="rag-badge rag-green" style={{fontSize:10}}>YES</span> : <span className="rag-badge rag-red" style={{fontSize:10}}>GAP</span>}</td>
                      <td>
                        <div style={{display:'flex',alignItems:'center',gap:6}}>
                          <div style={{background:'var(--border)',borderRadius:3,height:5,width:40,overflow:'hidden'}}>
                            <div style={{width:`${c.complianceScore}%`,height:'100%',background:c.complianceScore>=80?'var(--green)':'var(--amber)',borderRadius:3}} />
                          </div>
                          <span style={{fontSize:11}}>{c.complianceScore}%</span>
                        </div>
                      </td>
                    </tr>
                  ))}</tbody>
                </table>
              </div>
            </div>
          </div>

          <div className="glass-card" style={{marginTop:16}}>
            <h3>📝 Assessment Questions ({sectorQuestions.length})</h3>
            <p style={{fontSize:12,color:'var(--text-muted)'}}>Sector-specific self-assessment questions with DPDP section references</p>
            <div className="data-table-wrapper" style={{maxHeight:200,overflow:'auto'}}>
              <table className="data-table">
                <thead><tr><th>#</th><th>Question</th><th>DPDP Section</th></tr></thead>
                <tbody>{sectorQuestions.slice(0,10).map((q,i) => (
                  <tr key={i}>
                    <td>{i+1}</td>
                    <td style={{fontSize:12}}>{q.q}</td>
                    <td style={{fontSize:11,color:'var(--brand-primary)'}}>{q.section}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          </div>
        </div>
      )}
      {/* NOTIFICATIONS */}
      {tab==='notifications' && (
        <div className="glass-card">
          <h3>🔔 Notification & Alert Configuration</h3>
          <p style={{color:'var(--text-muted)',marginBottom:16}}>Configure alert channels for breach notifications (DPBI §8(6)), consent events, SLA deadlines, and system alerts.</p>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16}}>
            <div style={{padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <label style={{display:'flex',alignItems:'center',gap:8,marginBottom:12,cursor:'pointer'}}><input type="checkbox" checked={notif.emailEnabled} onChange={e=>setNotif({...notif,emailEnabled:e.target.checked})} /> <strong>📧 Email Notifications</strong></label>
              {notif.emailEnabled && <div style={{display:'grid',gap:8}}>
                <input className="form-input" placeholder="SMTP Host" value={notif.smtpHost} onChange={e=>setNotif({...notif,smtpHost:e.target.value})} />
                <input className="form-input" placeholder="SMTP Port" value={notif.smtpPort} onChange={e=>setNotif({...notif,smtpPort:e.target.value})} />
                <input className="form-input" placeholder="SMTP User" value={notif.smtpUser} onChange={e=>setNotif({...notif,smtpUser:e.target.value})} />
                <input className="form-input" placeholder="SMTP Password" type="password" value={notif.smtpPass} onChange={e=>setNotif({...notif,smtpPass:e.target.value})} />
              </div>}
            </div>
            <div style={{padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <label style={{display:'flex',alignItems:'center',gap:8,marginBottom:12,cursor:'pointer'}}><input type="checkbox" checked={notif.smsEnabled} onChange={e=>setNotif({...notif,smsEnabled:e.target.checked})} /> <strong>📱 SMS Notifications</strong></label>
              {notif.smsEnabled && <div style={{display:'grid',gap:8}}>
                <select className="form-input" value={notif.smsGateway} onChange={e=>setNotif({...notif,smsGateway:e.target.value})}><option>MSG91</option><option>Twilio</option><option>AWS SNS</option><option>Kaleyra</option></select>
                <input className="form-input" placeholder="API Key" type="password" value={notif.smsApiKey} onChange={e=>setNotif({...notif,smsApiKey:e.target.value})} />
              </div>}
            </div>
            <div style={{padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <label style={{display:'flex',alignItems:'center',gap:8,marginBottom:12,cursor:'pointer'}}><input type="checkbox" checked={notif.webhookEnabled} onChange={e=>setNotif({...notif,webhookEnabled:e.target.checked})} /> <strong>🔗 Webhook</strong></label>
              {notif.webhookEnabled && <input className="form-input" placeholder="Webhook URL" value={notif.webhookUrl} onChange={e=>setNotif({...notif,webhookUrl:e.target.value})} />}
            </div>
            <div style={{padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <label style={{display:'flex',alignItems:'center',gap:8,marginBottom:12,cursor:'pointer'}}><input type="checkbox" checked={notif.slackEnabled} onChange={e=>setNotif({...notif,slackEnabled:e.target.checked})} /> <strong>💬 Slack Integration</strong></label>
              {notif.slackEnabled && <input className="form-input" placeholder="Slack Webhook URL" value={notif.slackWebhook} onChange={e=>setNotif({...notif,slackWebhook:e.target.value})} />}
            </div>
          </div>
          <h4 style={{marginTop:20}}>⏰ DPDP Compliance Timers</h4>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr 1fr',gap:12,marginTop:8}}>
            <div><label style={{fontSize:11,color:'var(--text-muted)'}}>DPBI Notify (hrs)</label><input className="form-input" type="number" value={notif.dpbiNotifyHrs} onChange={e=>setNotif({...notif,dpbiNotifyHrs:e.target.value})} /></div>
            <div><label style={{fontSize:11,color:'var(--text-muted)'}}>CERT-IN Notify (hrs)</label><input className="form-input" type="number" value={notif.certinNotifyHrs} onChange={e=>setNotif({...notif,certinNotifyHrs:e.target.value})} /></div>
            <label style={{display:'flex',alignItems:'center',gap:6,cursor:'pointer'}}><input type="checkbox" checked={notif.breachAlerts} onChange={e=>setNotif({...notif,breachAlerts:e.target.checked})} /> Breach Alerts</label>
            <label style={{display:'flex',alignItems:'center',gap:6,cursor:'pointer'}}><input type="checkbox" checked={notif.consentAlerts} onChange={e=>setNotif({...notif,consentAlerts:e.target.checked})} /> Consent Alerts</label>
          </div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr 1fr',gap:12,marginTop:8}}>
            <label style={{display:'flex',alignItems:'center',gap:6,cursor:'pointer'}}><input type="checkbox" checked={notif.rightsAlerts} onChange={e=>setNotif({...notif,rightsAlerts:e.target.checked})} /> Rights SLA Alerts</label>
            <label style={{display:'flex',alignItems:'center',gap:6,cursor:'pointer'}}><input type="checkbox" checked={notif.slaAlerts} onChange={e=>setNotif({...notif,slaAlerts:e.target.checked})} /> SLA Breach Alerts</label>
            <label style={{display:'flex',alignItems:'center',gap:6,cursor:'pointer'}}><input type="checkbox" checked={notif.escalationEnabled} onChange={e=>setNotif({...notif,escalationEnabled:e.target.checked})} /> Auto Escalation</label>
          </div>
          <button className="btn-primary" style={{marginTop:16}} onClick={save}>💾 Save Notification Settings</button>
        </div>
      )}

      {/* SECURITY / PQC */}
      {tab==='security' && (
        <div className="glass-card">
          <h3>🔐 Security & Quantum-Safe Cryptography</h3>
          <p style={{color:'var(--text-muted)',marginBottom:16}}>Configure authentication, PQC algorithms (NIST), TLS, HSM, and access controls.</p>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16}}>
            <div style={{padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <h4 style={{margin:'0 0 12px'}}>🔑 Authentication</h4>
              <label style={{display:'flex',alignItems:'center',gap:6,marginBottom:8}}><input type="checkbox" checked={sec.mfaEnabled} onChange={e=>setSec({...sec,mfaEnabled:e.target.checked})} /> Enable MFA</label>
              {sec.mfaEnabled && <select className="form-input" style={{marginBottom:8}} value={sec.mfaMethod} onChange={e=>setSec({...sec,mfaMethod:e.target.value})}><option>TOTP</option><option>SMS OTP</option><option>Email OTP</option><option>FIDO2 / WebAuthn</option></select>}
              <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8}}>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Session Timeout (min)</label><input className="form-input" type="number" value={sec.sessionTimeout} onChange={e=>setSec({...sec,sessionTimeout:e.target.value})} /></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Max Login Attempts</label><input className="form-input" type="number" value={sec.maxLoginAttempts} onChange={e=>setSec({...sec,maxLoginAttempts:e.target.value})} /></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Min Password Length</label><input className="form-input" type="number" value={sec.passwordMinLength} onChange={e=>setSec({...sec,passwordMinLength:e.target.value})} /></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>API Rate Limit (/hr)</label><input className="form-input" type="number" value={sec.apiRateLimit} onChange={e=>setSec({...sec,apiRateLimit:e.target.value})} /></div>
              </div>
              <label style={{display:'flex',alignItems:'center',gap:6,marginTop:8}}><input type="checkbox" checked={sec.passwordComplexity} onChange={e=>setSec({...sec,passwordComplexity:e.target.checked})} /> Enforce Password Complexity</label>
              <label style={{display:'flex',alignItems:'center',gap:6,marginTop:8}}><input type="checkbox" checked={sec.dataEncryptAtRest} onChange={e=>setSec({...sec,dataEncryptAtRest:e.target.checked})} /> 🔒 Encrypt Data at Rest</label>
            </div>
            <div style={{padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <h4 style={{margin:'0 0 12px'}}>⚛️ Post-Quantum Cryptography (NIST PQC)</h4>
              <div style={{display:'grid',gap:8}}>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Key Encapsulation</label><select className="form-input" value={sec.pqcKeyEncap} onChange={e=>setSec({...sec,pqcKeyEncap:e.target.value})}><option>ML-KEM-512</option><option>ML-KEM-768</option><option>ML-KEM-1024</option></select></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Digital Signature</label><select className="form-input" value={sec.pqcSignature} onChange={e=>setSec({...sec,pqcSignature:e.target.value})}><option>ML-DSA-44</option><option>ML-DSA-65</option><option>ML-DSA-87</option></select></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Symmetric</label><select className="form-input" value={sec.pqcSymmetric} onChange={e=>setSec({...sec,pqcSymmetric:e.target.value})}><option>AES-128-GCM</option><option>AES-256-GCM</option><option>ChaCha20-Poly1305</option></select></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Classical Fallback</label><select className="form-input" value={sec.pqcFallback} onChange={e=>setSec({...sec,pqcFallback:e.target.value})}><option>RSA-2048</option><option>RSA-4096</option><option>ECDSA P-256</option><option>ECDSA P-384</option></select></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>TLS Version</label><select className="form-input" value={sec.tlsVersion} onChange={e=>setSec({...sec,tlsVersion:e.target.value})}><option>TLS 1.2</option><option>TLS 1.3</option></select></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>HSM</label><select className="form-input" value={sec.hsm} onChange={e=>setSec({...sec,hsm:e.target.value})}><option>Software (SoftHSM2)</option><option>AWS CloudHSM</option><option>Azure Dedicated HSM</option><option>Thales Luna</option><option>nShield</option></select></div>
              </div>
            </div>
          </div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginTop:16}}>
            <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Audit Log Retention (days)</label><input className="form-input" type="number" value={sec.auditLogRetention} onChange={e=>setSec({...sec,auditLogRetention:e.target.value})} /></div>
            <div><label style={{fontSize:11,color:'var(--text-muted)'}}>IP Whitelist (comma-separated)</label><input className="form-input" value={sec.ipWhitelist} onChange={e=>setSec({...sec,ipWhitelist:e.target.value})} placeholder="e.g. 10.0.0.0/8, 192.168.1.0/24" /></div>
          </div>
          <button className="btn-primary" style={{marginTop:16}} onClick={save}>💾 Save Security Settings</button>
        </div>
      )}

      {/* BACKUP */}
      {tab==='backup' && (
        <div className="glass-card">
          <h3>💾 Backup & Recovery</h3>
          <p style={{color:'var(--text-muted)',marginBottom:16}}>Configure automated backups, cloud storage, and view backup history.</p>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16}}>
            <div style={{padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <h4 style={{margin:'0 0 12px'}}>⚙️ Backup Schedule</h4>
              <label style={{display:'flex',alignItems:'center',gap:6,marginBottom:12}}><input type="checkbox" checked={backup.autoBackup} onChange={e=>setBackup({...backup,autoBackup:e.target.checked})} /> Enable Auto-Backup</label>
              <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:8}}>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Frequency</label><select className="form-input" value={backup.frequency} onChange={e=>setBackup({...backup,frequency:e.target.value})}><option>Hourly</option><option>Daily</option><option>Weekly</option><option>Monthly</option></select></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Time</label><input className="form-input" type="time" value={backup.time} onChange={e=>setBackup({...backup,time:e.target.value})} /></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Retention (days)</label><input className="form-input" type="number" value={backup.retention} onChange={e=>setBackup({...backup,retention:e.target.value})} /></div>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Destination</label><select className="form-input" value={backup.destination} onChange={e=>setBackup({...backup,destination:e.target.value})}><option>Local Only</option><option>Cloud Only</option><option>Local + Cloud</option></select></div>
              </div>
              <label style={{display:'flex',alignItems:'center',gap:6,marginTop:8}}><input type="checkbox" checked={backup.encryptBackup} onChange={e=>setBackup({...backup,encryptBackup:e.target.checked})} /> 🔒 Encrypt Backups (AES-256)</label>
            </div>
            <div style={{padding:16,border:'1px solid var(--border)',borderRadius:'var(--radius)'}}>
              <h4 style={{margin:'0 0 12px'}}>☁️ Cloud Storage</h4>
              <div style={{display:'grid',gap:8}}>
                <div><label style={{fontSize:11,color:'var(--text-muted)'}}>Provider</label><select className="form-input" value={backup.cloudProvider} onChange={e=>setBackup({...backup,cloudProvider:e.target.value})}><option>AWS S3</option><option>Azure Blob</option><option>Google Cloud Storage</option><option>MinIO (Self-hosted)</option></select></div>
                <input className="form-input" placeholder="Bucket" value={backup.s3Bucket} onChange={e=>setBackup({...backup,s3Bucket:e.target.value})} />
                <input className="form-input" placeholder="Region" value={backup.s3Region} onChange={e=>setBackup({...backup,s3Region:e.target.value})} />
                <input className="form-input" placeholder="Access Key" value={backup.s3AccessKey} onChange={e=>setBackup({...backup,s3AccessKey:e.target.value})} />
                <input className="form-input" placeholder="Secret Key" type="password" value={backup.s3SecretKey} onChange={e=>setBackup({...backup,s3SecretKey:e.target.value})} />
              </div>
            </div>
          </div>
          <div style={{marginTop:20,display:'flex',gap:12,alignItems:'center'}}>
            <button className="btn-primary" onClick={save}>💾 Save Backup Settings</button>
            <button className="btn-secondary" onClick={()=>{setSaved(true);setTimeout(()=>setSaved(false),2000)}}>▶️ Run Backup Now</button>
            <span style={{fontSize:12,color:'var(--text-muted)'}}>Last backup: <strong>{backup.lastBackup}</strong> ({backup.lastBackupSize})</span>
          </div>
          <h4 style={{marginTop:20}}>📋 Backup History</h4>
          <div className="data-table-wrapper"><table className="data-table">
            <thead><tr><th>Date</th><th>Type</th><th>Size</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>{backup.backupHistory.map((b,i)=>(
              <tr key={i}><td style={{fontFamily:'monospace',fontSize:12}}>{b.date}</td><td>{b.type}</td><td>{b.size}</td>
              <td><span className="rag-badge rag-green">{b.status}</span></td>
              <td><button className="btn-sm" onClick={()=>alert(`Restoring backup from ${b.date}...\nThis will replace all current data.\nIn production, this triggers the Spring Boot /api/backup/restore endpoint.`)}>♻️ Restore</button></td></tr>
            ))}</tbody>
          </table></div>
        </div>
      )}
    </div>
  )
}
