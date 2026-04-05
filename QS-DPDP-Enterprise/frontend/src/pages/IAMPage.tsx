import { useState } from 'react'
import { IAM_ROLES, PURPOSE_ACCESS_CONTROLS, ACCESS_LOG_ENTRIES } from '../data/sectorData'

const PERM_ICONS: Record<string,string> = { VIEW_ALL:'👁️', ALL:'⚡', BREACH_MANAGE:'🚨', DPIA_MANAGE:'📋', POLICY_MANAGE:'📄', AUDIT_VIEW:'📊', CONSENT_VIEW:'✅', CONSENT_MANAGE:'✅', RIGHTS_MANAGE:'👤', RIGHTS_VIEW:'👤', RIGHTS_CREATE:'➕', SIEM_VIEW:'📡', SIEM_MANAGE:'📡', EDR_VIEW:'💻', EDR_MANAGE:'💻', DLP_VIEW:'🛡️', THREAT_MANAGE:'⚔️', REPORT_GENERATE:'📈', GAP_MANAGE:'🔍', DASHBOARD_VIEW:'📊', OWN_DATA_VIEW:'🔑', CONSENT_MANAGE_OWN:'🔐', RIGHTS_CREATE_OWN:'📝', GRIEVANCE_FILE:'📩', POLICY_VIEW:'📄' }

export default function IAMPage() {
  const [tab, setTab] = useState<'roles'|'pbac'|'users'|'logs'>('roles')
  const [logFilter, setLogFilter] = useState('')
  const [logPage, setLogPage] = useState(0)

  const filteredLogs = ACCESS_LOG_ENTRIES.filter(l =>
    !logFilter || l.action.includes(logFilter) || l.role.includes(logFilter) || l.result.includes(logFilter)
  )
  const pageSize = 15
  const pagedLogs = filteredLogs.slice(logPage*pageSize, (logPage+1)*pageSize)

  return (
    <div>
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:24}}>
        <div>
          <h2 style={{margin:0}}>🔐 IAM & PAM — Data Access Management</h2>
          <p style={{color:'var(--text-muted)',margin:'4px 0 0'}}>RBAC + Purpose-Based Access Control per DPDP Act 2023</p>
        </div>
        <div style={{display:'flex',gap:8}}>
          <span className="badge" style={{background:'var(--green)',color:'#fff',padding:'6px 12px',borderRadius:20,fontSize:12}}>👥 {IAM_ROLES.reduce((s,r)=>s+r.users,0)} Total Users</span>
          <span className="badge" style={{background:'var(--blue)',color:'#fff',padding:'6px 12px',borderRadius:20,fontSize:12}}>🔑 {IAM_ROLES.length} Roles</span>
          <span className="badge" style={{background:'var(--purple)',color:'#fff',padding:'6px 12px',borderRadius:20,fontSize:12}}>🎯 {PURPOSE_ACCESS_CONTROLS.length} Purposes</span>
        </div>
      </div>

      {/* Tab Switcher */}
      <div style={{display:'flex',gap:2,marginBottom:20,borderBottom:'2px solid var(--border)'}}>
        {(['roles','pbac','users','logs'] as const).map(t => (
          <button key={t} onClick={()=>setTab(t)} style={{padding:'10px 20px',background:tab===t?'var(--brand-primary)':'transparent',color:tab===t?'#fff':'var(--text-muted)',border:'none',borderRadius:'8px 8px 0 0',cursor:'pointer',fontWeight:tab===t?700:400,fontSize:14}}>
            {t==='roles'?'🛡️ RBAC Roles':t==='pbac'?'🎯 Purpose-Based (PBAC)':t==='users'?'👥 User Management':'📜 Access Logs'}
          </button>
        ))}
      </div>

      {/* RBAC Roles Tab */}
      {tab === 'roles' && (
        <div>
          <div style={{display:'grid',gridTemplateColumns:'repeat(auto-fill,minmax(350px,1fr))',gap:16}}>
            {IAM_ROLES.map((r,i) => (
              <div key={i} className="glass-card" style={{padding:20,borderLeft:`4px solid ${r.color}`}}>
                <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:12}}>
                  <div>
                    <h3 style={{margin:0,fontSize:16}}>{r.role}</h3>
                    <span style={{fontSize:11,color:'var(--text-muted)'}}>Level {r.level} • {r.dpdpRef}</span>
                  </div>
                  <span style={{background:'var(--bg-surface)',padding:'4px 10px',borderRadius:12,fontSize:12,fontWeight:600}}>
                    {r.users} user{r.users>1?'s':''}
                  </span>
                </div>
                <div style={{display:'flex',flexWrap:'wrap',gap:4}}>
                  {r.permissions.map((p,j) => (
                    <span key={j} style={{fontSize:11,background:'var(--bg-surface)',padding:'2px 8px',borderRadius:4,display:'flex',alignItems:'center',gap:4}}>
                      {PERM_ICONS[p]||'📌'} {p.replace(/_/g,' ')}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>

          {/* Permission Matrix */}
          <h3 style={{marginTop:32}}>📊 Permission Matrix</h3>
          <div style={{overflowX:'auto'}}>
            <table style={{width:'100%',borderCollapse:'collapse',fontSize:12}}>
              <thead>
                <tr style={{background:'var(--bg-surface)'}}>
                  <th style={{padding:8,textAlign:'left',position:'sticky',left:0,background:'var(--bg-surface)'}}>Module</th>
                  {IAM_ROLES.map(r => <th key={r.role} style={{padding:8,textAlign:'center',minWidth:80}}>{r.role.split(' ')[0]}</th>)}
                </tr>
              </thead>
              <tbody>
                {['Dashboard','Consent','Breach','SIEM','DLP','EDR','Policy','DPIA','Assessment','Reports','Settings'].map(mod => (
                  <tr key={mod} style={{borderBottom:'1px solid var(--border)'}}>
                    <td style={{padding:8,fontWeight:600,position:'sticky',left:0,background:'var(--bg-main)'}}>{mod}</td>
                    {IAM_ROLES.map(r => {
                      const has = r.permissions.includes('ALL') || r.permissions.includes('VIEW_ALL') ||
                        r.permissions.some(p => p.startsWith(mod.toUpperCase().slice(0,4))) ||
                        (r.role==='Data Principal' && ['Dashboard','Consent'].includes(mod))
                      return <td key={r.role} style={{padding:8,textAlign:'center'}}>{has ? '✅' : '❌'}</td>
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* PBAC Tab */}
      {tab === 'pbac' && (
        <div>
          <div style={{overflowX:'auto'}}>
            <table style={{width:'100%',borderCollapse:'collapse'}}>
              <thead>
                <tr style={{background:'var(--bg-surface)'}}>
                  <th style={{padding:10,textAlign:'left'}}>Purpose</th>
                  <th style={{padding:10,textAlign:'left'}}>DPDP Section</th>
                  <th style={{padding:10,textAlign:'left'}}>Allowed Roles</th>
                  <th style={{padding:10,textAlign:'left'}}>Data Categories</th>
                  <th style={{padding:10,textAlign:'left'}}>Retention</th>
                  <th style={{padding:10,textAlign:'left'}}>Lawful Basis</th>
                </tr>
              </thead>
              <tbody>
                {PURPOSE_ACCESS_CONTROLS.map((pac,i) => (
                  <tr key={i} style={{borderBottom:'1px solid var(--border)'}}>
                    <td style={{padding:10,fontWeight:600}}>{pac.purpose}</td>
                    <td style={{padding:10}}><span className="badge" style={{background:'var(--brand-primary)',color:'#fff',padding:'2px 8px',borderRadius:4,fontSize:11}}>{pac.dpdpSection}</span></td>
                    <td style={{padding:10}}>
                      <div style={{display:'flex',flexWrap:'wrap',gap:4}}>
                        {pac.allowedRoles.map(r => <span key={r} style={{fontSize:11,background:'var(--bg-surface)',padding:'2px 6px',borderRadius:4}}>{r}</span>)}
                      </div>
                    </td>
                    <td style={{padding:10,fontSize:12}}>{pac.dataCategories.join(', ')}</td>
                    <td style={{padding:10,fontSize:12}}>{pac.retention}</td>
                    <td style={{padding:10,fontSize:12}}>{pac.lawfulBasis}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* User Management Tab */}
      {tab === 'users' && (
        <div>
          <div style={{display:'flex',justifyContent:'space-between',marginBottom:16}}>
            <h3 style={{margin:0}}>👥 Active Users by Role</h3>
            <button style={{padding:'8px 16px',background:'var(--brand-primary)',color:'#fff',border:'none',borderRadius:8,cursor:'pointer'}}>➕ Add User</button>
          </div>
          <div style={{display:'grid',gridTemplateColumns:'repeat(auto-fill,minmax(300px,1fr))',gap:12}}>
            {IAM_ROLES.map((r,i) => (
              <div key={i} className="glass-card" style={{padding:16}}>
                <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                  <div>
                    <strong>{r.role}</strong>
                    <div style={{fontSize:11,color:'var(--text-muted)'}}>{r.dpdpRef}</div>
                  </div>
                  <div style={{fontSize:24,fontWeight:700,color:r.color}}>{r.users}</div>
                </div>
                <div style={{marginTop:8,display:'flex',gap:4}}>
                  <button style={{fontSize:11,padding:'4px 8px',background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:4,cursor:'pointer',color:'var(--text-primary)'}}>View</button>
                  <button style={{fontSize:11,padding:'4px 8px',background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:4,cursor:'pointer',color:'var(--text-primary)'}}>Edit</button>
                  <button style={{fontSize:11,padding:'4px 8px',background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:4,cursor:'pointer',color:'var(--text-primary)'}}>Permissions</button>
                </div>
              </div>
            ))}
          </div>

          {/* PAM — Privileged Access */}
          <div className="glass-card" style={{padding:20,marginTop:24,borderLeft:'4px solid var(--red)'}}>
            <h3 style={{margin:'0 0 12px'}}>🔒 Privileged Access Management (PAM)</h3>
            <div style={{display:'grid',gridTemplateColumns:'repeat(auto-fill,minmax(200px,1fr))',gap:12}}>
              <div style={{background:'var(--bg-surface)',padding:12,borderRadius:8}}>
                <div style={{fontSize:11,color:'var(--text-muted)'}}>Active Privileged Sessions</div>
                <div style={{fontSize:24,fontWeight:700,color:'var(--amber)'}}>2</div>
              </div>
              <div style={{background:'var(--bg-surface)',padding:12,borderRadius:8}}>
                <div style={{fontSize:11,color:'var(--text-muted)'}}>Break-Glass Incidents (30d)</div>
                <div style={{fontSize:24,fontWeight:700,color:'var(--red)'}}>0</div>
              </div>
              <div style={{background:'var(--bg-surface)',padding:12,borderRadius:8}}>
                <div style={{fontSize:11,color:'var(--text-muted)'}}>Credential Rotations Due</div>
                <div style={{fontSize:24,fontWeight:700,color:'var(--green)'}}>3</div>
              </div>
              <div style={{background:'var(--bg-surface)',padding:12,borderRadius:8}}>
                <div style={{fontSize:11,color:'var(--text-muted)'}}>Session Recordings</div>
                <div style={{fontSize:24,fontWeight:700}}>128</div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Access Logs Tab */}
      {tab === 'logs' && (
        <div>
          <div style={{display:'flex',gap:8,marginBottom:16}}>
            <input placeholder="Filter by action, role, result..." value={logFilter} onChange={e=>setLogFilter(e.target.value)} style={{flex:1,padding:10,background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:8,color:'var(--text-primary)'}} />
            <select onChange={e=>setLogFilter(e.target.value)} style={{padding:10,background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:8,color:'var(--text-primary)'}}>
              <option value="">All Results</option>
              <option value="ALLOWED">ALLOWED</option>
              <option value="DENIED">DENIED</option>
            </select>
          </div>
          <table style={{width:'100%',borderCollapse:'collapse',fontSize:12}}>
            <thead>
              <tr style={{background:'var(--bg-surface)'}}>
                <th style={{padding:8,textAlign:'left'}}>Timestamp</th>
                <th style={{padding:8,textAlign:'left'}}>User</th>
                <th style={{padding:8,textAlign:'left'}}>Role</th>
                <th style={{padding:8,textAlign:'left'}}>Action</th>
                <th style={{padding:8,textAlign:'left'}}>Resource</th>
                <th style={{padding:8,textAlign:'left'}}>Purpose</th>
                <th style={{padding:8,textAlign:'left'}}>Result</th>
                <th style={{padding:8,textAlign:'left'}}>IP</th>
              </tr>
            </thead>
            <tbody>
              {pagedLogs.map(log => (
                <tr key={log.id} style={{borderBottom:'1px solid var(--border)',background:log.result==='DENIED'?'rgba(255,0,0,0.05)':'transparent'}}>
                  <td style={{padding:8,fontFamily:'monospace',fontSize:11}}>{new Date(log.timestamp).toLocaleString('en-IN')}</td>
                  <td style={{padding:8}}>{log.user}</td>
                  <td style={{padding:8}}>{log.role}</td>
                  <td style={{padding:8}}><span style={{background:log.action==='DELETE'?'var(--red)':log.action==='EXPORT'?'var(--amber)':'var(--bg-surface)',color:log.action==='DELETE'||log.action==='EXPORT'?'#fff':'inherit',padding:'2px 6px',borderRadius:4,fontSize:11}}>{log.action}</span></td>
                  <td style={{padding:8}}>{log.resource}</td>
                  <td style={{padding:8}}>{log.purpose}</td>
                  <td style={{padding:8}}><span style={{color:log.result==='DENIED'?'var(--red)':'var(--green)',fontWeight:700}}>{log.result==='DENIED'?'❌ DENIED':'✅ ALLOWED'}</span></td>
                  <td style={{padding:8,fontFamily:'monospace',fontSize:11}}>{log.ip}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{display:'flex',justifyContent:'space-between',marginTop:12}}>
            <span style={{fontSize:12,color:'var(--text-muted)'}}>Showing {logPage*pageSize+1}–{Math.min((logPage+1)*pageSize,filteredLogs.length)} of {filteredLogs.length}</span>
            <div style={{display:'flex',gap:4}}>
              <button disabled={logPage===0} onClick={()=>setLogPage(p=>p-1)} style={{padding:'4px 12px',background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:4,cursor:'pointer',color:'var(--text-primary)'}}>← Prev</button>
              <button disabled={(logPage+1)*pageSize>=filteredLogs.length} onClick={()=>setLogPage(p=>p+1)} style={{padding:'4px 12px',background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:4,cursor:'pointer',color:'var(--text-primary)'}}>Next →</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
