import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { useAppContext } from '../context/AppContext'
import { generateConsents, generateBreaches, generateRightsRequests, SECTOR_POLICIES, SECTOR_CONTROLS, SECTOR_QUESTIONS } from '../data/sectorData'

/* ═══════════════════════════════════════════════════════════════
   DASHBOARD — Drill-down KPIs, RAG Compliance, Activity Feed
   Everything clickable → expands detail panel at atomic level
   ═══════════════════════════════════════════════════════════════ */

export default function Dashboard() {
  const navigate = useNavigate()
  const { sector } = useAppContext()
  const [drillDown, setDrillDown] = useState<{type:string,title:string}|null>(null)

  // Generate sector-specific data for drill-down
  const consents = useMemo(() => generateConsents(sector, 500), [sector])
  const breaches = useMemo(() => generateBreaches(sector, 50), [sector])
  const rights = useMemo(() => generateRightsRequests(sector, 200), [sector])
  const policies = useMemo(() => SECTOR_POLICIES[sector] || SECTOR_POLICIES['Banking & Finance'], [sector])
  const controls = useMemo(() => SECTOR_CONTROLS[sector] || SECTOR_CONTROLS['Banking & Finance'], [sector])
  const questions = useMemo(() => SECTOR_QUESTIONS[sector] || SECTOR_QUESTIONS['Banking & Finance'], [sector])

  const consentStats = {
    active: consents.filter(c=>c.status==='ACTIVE').length,
    withdrawn: consents.filter(c=>c.status==='WITHDRAWN').length,
    pending: consents.filter(c=>c.status==='PENDING').length,
    expired: consents.filter(c=>c.status==='EXPIRED').length,
    guardian: consents.filter(c=>c.type==='GUARDIAN').length,
  }

  const ragModules = [
    {id:'consent',name:'Consent',score:Math.round(consentStats.active/consents.length*100),detail:`${consentStats.active} active, ${consentStats.withdrawn} withdrawn`,path:'/consent',records:consents.length},
    {id:'breach',name:'Breach',score:Math.max(60, Math.round((1 - breaches.filter(b=>b.severity==='CRITICAL').length/breaches.length)*100)),detail:`${breaches.length} incidents, ${breaches.filter(b=>b.severity==='CRITICAL').length} critical`,path:'/breach',records:breaches.length},
    {id:'rights',name:'Rights',score:Math.round(rights.filter(r=>r.status==='COMPLETED').length/rights.length*100),detail:`${rights.filter(r=>r.status==='COMPLETED').length} completed / ${rights.length}`,path:'/rights',records:rights.length},
    {id:'dlp',name:'DLP',score:81,detail:'PII scanning active, 8 channels',path:'/dlp',records:200},
    {id:'siem',name:'SIEM',score:95,detail:'Real-time monitoring, MITRE mapped',path:'/siem',records:1000},
    {id:'policy',name:'Policy',score:Math.round(policies.reduce((s:number,p:any)=>s+p.complianceScore,0)/policies.length),detail:`${policies.length} policies loaded`,path:'/policy',records:policies.length},
    {id:'dpia',name:'DPIA',score:72,detail:'Impact assessments tracked',path:'/dpia',records:25},
    {id:'gap',name:'Assessment',score:85,detail:`${questions.length} questions, ${controls.length} controls`,path:'/gap',records:questions.length},
  ]

  const overallScore = Math.round(ragModules.reduce((s,m) => s+m.score, 0) / ragModules.length)

  const recentActivity = [
    {time:'14:30',user:'admin',action:'Consent CON-4521 created',module:'Consent',type:'CREATE',path:'/consent',detail:`Principal: Data Subject #4521, Purpose: ${consents[0]?.purpose||'KYC'}, Status: ACTIVE`},
    {time:'14:28',user:'admin',action:`Policy ${policies[0]?.id||'POL-001'} updated`,module:'Policy',type:'UPDATE',path:'/policy',detail:`Title: ${policies[0]?.title||'Data Protection Policy'}, Score: ${policies[0]?.complianceScore||85}%`},
    {time:'14:25',user:'system',action:'Threat feed refresh: 342 new IOCs',module:'SIEM',type:'ALERT',path:'/siem',detail:'Source: CERT-IN + MITRE ATT&CK, Severity: Medium, Auto-correlated with existing rules'},
    {time:'14:20',user:'admin',action:'DPIA assessment initiated for KYC',module:'DPIA',type:'CREATE',path:'/dpia',detail:'Scope: KYC data processing, Risk Level: High, Assessor: DPO'},
    {time:'14:15',user:'system',action:'PII scan: 3 Aadhaar numbers found',module:'DLP',type:'SCAN',path:'/dlp',detail:'Channel: Email, Action: Quarantined, Policy: Aadhaar detection rule, Sensitivity: Critical'},
    {time:'14:10',user:'admin',action:`Rights request completed`,module:'Rights',type:'COMPLETE',path:'/rights',detail:`Type: ACCESS (Section 11), Principal: Data Subject, SLA: Within 30 days`},
    {time:'13:55',user:'system',action:'Anomalous login from IP 103.x.x.x',module:'EDR',type:'ALERT',path:'/edr',detail:'Risk Score: 87/100, Location: Unknown VPN, Action: Session blocked, MFA required'},
    {time:'13:40',user:'admin',action:'Breach report filed to DPBI',module:'Breach',type:'CREATE',path:'/breach',detail:`Severity: HIGH, Records affected: ${breaches[0]?.recordsAffected||1200}, CERT-IN notified within 6h`},
  ]

  const getColor = (score:number) => score >= 80 ? 'var(--green)' : score >= 50 ? 'var(--amber)' : 'var(--red)'
  const getStatus = (score:number) => score >= 80 ? 'green' : score >= 50 ? 'amber' : 'red'

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1>DPDP Compliance Dashboard</h1>
          <p>Sector: <strong>{sector}</strong> • {new Date().toLocaleDateString('en-IN')} • Overall: <strong style={{color:getColor(overallScore)}}>{overallScore}%</strong></p>
        </div>
        <div style={{display:'flex',gap:8}}>
          <button className="btn-primary" onClick={()=>navigate('/reports')}>📊 Export Report</button>
          <button className="btn-secondary" onClick={()=>navigate('/gap')}>🎯 Run Assessment</button>
        </div>
      </div>

      {/* KPI Cards — Clickable with drill-down */}
      <div className="kpi-grid">
        <div className="kpi-card green" style={{cursor:'pointer'}} onClick={()=>setDrillDown(drillDown?.type==='consents'?null:{type:'consents',title:'Consent Records'})}>
          <div className="kpi-value" style={{color:'var(--green)'}}>{consentStats.active}</div>
          <div className="kpi-label">Active Consents</div>
          <div style={{fontSize:10,color:'var(--text-muted)',marginTop:4}}>Click to drill down ↓</div>
        </div>
        <div className="kpi-card red" style={{cursor:'pointer'}} onClick={()=>setDrillDown(drillDown?.type==='breaches'?null:{type:'breaches',title:'Breach Incidents'})}>
          <div className="kpi-value" style={{color:'var(--red)'}}>{breaches.filter(b=>b.severity==='CRITICAL').length}</div>
          <div className="kpi-label">Critical Breaches</div>
          <div style={{fontSize:10,color:'var(--text-muted)',marginTop:4}}>Click to drill down ↓</div>
        </div>
        <div className="kpi-card amber" style={{cursor:'pointer'}} onClick={()=>setDrillDown(drillDown?.type==='rights'?null:{type:'rights',title:'Rights Requests'})}>
          <div className="kpi-value" style={{color:'var(--amber)'}}>{rights.filter(r=>r.status==='PENDING').length}</div>
          <div className="kpi-label">Pending Rights</div>
          <div style={{fontSize:10,color:'var(--text-muted)',marginTop:4}}>Click to drill down ↓</div>
        </div>
        <div className="kpi-card blue" style={{cursor:'pointer'}} onClick={()=>setDrillDown(drillDown?.type==='policies'?null:{type:'policies',title:'Policy Compliance'})}>
          <div className="kpi-value" style={{color:'var(--blue)'}}>{policies.length}</div>
          <div className="kpi-label">Active Policies</div>
          <div style={{fontSize:10,color:'var(--text-muted)',marginTop:4}}>Click to drill down ↓</div>
        </div>
        <div className="kpi-card purple" style={{cursor:'pointer'}} onClick={()=>setDrillDown(drillDown?.type==='controls'?null:{type:'controls',title:'Compliance Controls'})}>
          <div className="kpi-value" style={{color:'var(--purple)'}}>{controls.length}</div>
          <div className="kpi-label">Controls</div>
          <div style={{fontSize:10,color:'var(--text-muted)',marginTop:4}}>Click to drill down ↓</div>
        </div>
        <div className="kpi-card" style={{cursor:'pointer'}} onClick={()=>navigate('/gap')}>
          <div className="kpi-value">{questions.length}</div>
          <div className="kpi-label">Assessment Qs</div>
        </div>
      </div>

      {/* ═══ DRILL-DOWN PANEL ═══ */}
      {drillDown && (
        <div className="glass-card" style={{marginBottom:16,borderLeft:'4px solid var(--brand-primary)',animation:'fadeIn 0.3s'}}>
          <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:12}}>
            <h3>🔍 {drillDown.title} — {sector} (Atomic Detail)</h3>
            <div style={{display:'flex',gap:8}}>
              <button className="btn-sm" onClick={()=>navigate(drillDown.type==='consents'?'/consent':drillDown.type==='breaches'?'/breach':drillDown.type==='rights'?'/rights':drillDown.type==='policies'?'/policy':'/gap')}>↗ Open Full Module</button>
              <button className="btn-sm" onClick={()=>setDrillDown(null)}>✕ Close</button>
            </div>
          </div>

          {drillDown.type==='consents' && (
            <div className="data-table-wrapper" style={{maxHeight:300}}>
              <table className="data-table">
                <thead><tr><th>ID</th><th>Principal</th><th>Purpose</th><th>Type</th><th>Status</th><th>Granted</th><th>Notice</th></tr></thead>
                <tbody>{consents.slice(0,20).map(c=>(
                  <tr key={c.id} style={{cursor:'pointer'}} onClick={()=>navigate('/consent')}>
                    <td style={{fontFamily:'var(--font-mono)',fontSize:11}}>{c.id}</td>
                    <td><strong>{c.principal}</strong></td><td style={{fontSize:12}}>{c.purpose}</td>
                    <td><span className={`badge ${c.type==='GUARDIAN'?'purple':'blue'}`}>{c.type}</span></td>
                    <td><span className={`rag-badge ${c.status==='ACTIVE'?'rag-green':c.status==='WITHDRAWN'?'rag-red':'rag-amber'}`}>{c.status}</span></td>
                    <td style={{fontSize:11}}>{c.grantedAt}</td><td>{c.noticeProvided?'✅':'❌'}</td>
                  </tr>
                ))}</tbody>
              </table>
              <div style={{textAlign:'center',padding:8,fontSize:12,color:'var(--text-muted)'}}>Showing 20 of {consents.length} — <span style={{cursor:'pointer',color:'var(--brand-primary)',fontWeight:700}} onClick={()=>navigate('/consent')}>View All →</span></div>
            </div>
          )}

          {drillDown.type==='breaches' && (
            <div className="data-table-wrapper" style={{maxHeight:300}}>
              <table className="data-table">
                <thead><tr><th>ID</th><th>Title</th><th>Severity</th><th>Records</th><th>Status</th><th>Detected</th><th>DPBI</th></tr></thead>
                <tbody>{breaches.slice(0,15).map((b:any)=>(
                  <tr key={b.id} style={{cursor:'pointer'}} onClick={()=>navigate('/breach')}>
                    <td style={{fontFamily:'var(--font-mono)',fontSize:11}}>{b.id}</td>
                    <td><strong>{b.title}</strong></td>
                    <td><span className={`rag-badge ${b.severity==='CRITICAL'?'rag-red':b.severity==='HIGH'?'rag-amber':'rag-green'}`}>{b.severity}</span></td>
                    <td style={{fontWeight:700}}>{b.recordsAffected?.toLocaleString()}</td>
                    <td><span className={`rag-badge ${b.status==='RESOLVED'?'rag-green':'rag-red'}`}>{b.status}</span></td>
                    <td style={{fontSize:11}}>{b.detectedAt}</td>
                    <td>{b.dpbiNotified?'✅':'⏳'}</td>
                  </tr>
                ))}</tbody>
              </table>
              <div style={{textAlign:'center',padding:8,fontSize:12}}><span style={{cursor:'pointer',color:'var(--brand-primary)',fontWeight:700}} onClick={()=>navigate('/breach')}>View All {breaches.length} →</span></div>
            </div>
          )}

          {drillDown.type==='rights' && (
            <div className="data-table-wrapper" style={{maxHeight:300}}>
              <table className="data-table">
                <thead><tr><th>ID</th><th>Principal</th><th>Type</th><th>DPDP §</th><th>Status</th><th>Submitted</th><th>SLA</th></tr></thead>
                <tbody>{rights.slice(0,15).map((r:any)=>(
                  <tr key={r.id} style={{cursor:'pointer'}} onClick={()=>navigate('/rights')}>
                    <td style={{fontFamily:'var(--font-mono)',fontSize:11}}>{r.id}</td>
                    <td><strong>{r.principal}</strong></td>
                    <td><span className="badge blue">{r.type}</span></td>
                    <td style={{fontSize:11,color:'var(--brand-primary)'}}>{r.dpdpSection}</td>
                    <td><span className={`rag-badge ${r.status==='COMPLETED'?'rag-green':r.status==='PENDING'?'rag-amber':'rag-red'}`}>{r.status}</span></td>
                    <td style={{fontSize:11}}>{r.submittedAt}</td>
                    <td style={{fontSize:11,fontWeight:600}}>{r.slaDeadline}</td>
                  </tr>
                ))}</tbody>
              </table>
              <div style={{textAlign:'center',padding:8,fontSize:12}}><span style={{cursor:'pointer',color:'var(--brand-primary)',fontWeight:700}} onClick={()=>navigate('/rights')}>View All {rights.length} →</span></div>
            </div>
          )}

          {drillDown.type==='policies' && (
            <div className="data-table-wrapper" style={{maxHeight:300}}>
              <table className="data-table">
                <thead><tr><th>ID</th><th>Title</th><th>DPDP</th><th>Status</th><th>Score</th><th>Progress</th></tr></thead>
                <tbody>{policies.map((p:any)=>(
                  <tr key={p.id} style={{cursor:'pointer'}} onClick={()=>navigate('/policy')}>
                    <td style={{fontFamily:'var(--font-mono)',fontSize:11}}>{p.id}</td>
                    <td><strong>{p.title}</strong></td>
                    <td style={{fontSize:11}}>{p.dpdpSection}</td>
                    <td><span className={`rag-badge ${p.status==='ACTIVE'||p.status==='PUBLISHED'?'rag-green':'rag-amber'}`}>{p.status}</span></td>
                    <td style={{fontWeight:700,color:p.complianceScore>=80?'var(--green)':'var(--amber)'}}>{p.complianceScore}%</td>
                    <td style={{width:80}}><div style={{background:'var(--border)',borderRadius:4,height:6}}><div style={{width:`${p.complianceScore}%`,height:'100%',background:p.complianceScore>=80?'var(--green)':'var(--amber)',borderRadius:4}}/></div></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}

          {drillDown.type==='controls' && (
            <div className="data-table-wrapper" style={{maxHeight:300}}>
              <table className="data-table">
                <thead><tr><th>Control</th><th>Category</th><th>Implemented</th><th>Score</th><th>Progress</th><th>Gap</th></tr></thead>
                <tbody>{controls.map((c:any,i:number)=>(
                  <tr key={i}>
                    <td><strong>{c.control}</strong></td><td>{c.category}</td>
                    <td>{c.implemented?<span className="rag-badge rag-green">YES</span>:<span className="rag-badge rag-red">GAP</span>}</td>
                    <td style={{fontWeight:700}}>{c.complianceScore}%</td>
                    <td style={{width:80}}><div style={{background:'var(--border)',borderRadius:4,height:6}}><div style={{width:`${c.complianceScore}%`,height:'100%',background:c.complianceScore>=80?'var(--green)':'var(--amber)',borderRadius:4}}/></div></td>
                    <td style={{fontSize:11,color:c.gap?'var(--red)':'var(--green)'}}>{c.gap||'✅'}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* RAG Compliance Scores — Clickable */}
      <div className="glass-card" data-section="rag-compliance" style={{ marginBottom: 16 }}>
        <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:16}}>
          <h3>📊 RAG Compliance Scores — {sector}</h3>
          <div style={{display:'flex',gap:8,alignItems:'center'}}>
            <span style={{fontSize:13,fontWeight:700,color:getColor(overallScore)}}>{overallScore}% Overall</span>
            <span className="rag-badge rag-green">LIVE</span>
          </div>
        </div>
        <div className="rag-grid">
          {ragModules.map(module => (
            <div key={module.id} className="rag-item" onClick={()=>navigate(module.path)} style={{cursor:'pointer',transition:'transform 0.2s'}}>
              <span className={`rag-dot ${getStatus(module.score)}`} />
              <div style={{ fontSize: 13, fontWeight: 600 }}>{module.name}</div>
              <div style={{ fontSize: 24, fontWeight: 800, marginTop: 4, color:getColor(module.score) }}>{module.score}%</div>
              <div style={{fontSize:11,color:'var(--text-muted)'}}>{module.records} records</div>
              <div style={{fontSize:10,color:'var(--text-muted)',marginTop:2}}>{module.detail}</div>
            </div>
          ))}
        </div>
      </div>

      {/* ═══ VISUAL ANALYTICS — Pie Chart, Heat Map, Animated Bars ═══ */}
      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:16,marginBottom:16}}>

        {/* ── PIE CHART — Consent Status Distribution ── */}
        <div className="glass-card">
          <h3 style={{marginBottom:16}}>🥧 Consent Distribution</h3>
          <div style={{display:'flex',alignItems:'center',gap:24}}>
            <div style={{
              width:140, height:140, borderRadius:'50%', flexShrink:0,
              background:`conic-gradient(
                #22C55E 0deg ${consentStats.active/consents.length*360}deg,
                #F59E0B ${consentStats.active/consents.length*360}deg ${(consentStats.active+consentStats.pending)/consents.length*360}deg,
                #EF4444 ${(consentStats.active+consentStats.pending)/consents.length*360}deg ${(consentStats.active+consentStats.pending+consentStats.withdrawn)/consents.length*360}deg,
                #6366F1 ${(consentStats.active+consentStats.pending+consentStats.withdrawn)/consents.length*360}deg 360deg
              )`,
              display:'flex',alignItems:'center',justifyContent:'center',
              boxShadow:'0 4px 20px rgba(0,0,0,0.3)',
              animation:'pieRotateIn 1s ease-out',
            }}>
              <div style={{
                width:70,height:70,borderRadius:'50%',
                background:'var(--bg-card)',display:'flex',alignItems:'center',justifyContent:'center',
                flexDirection:'column',
              }}>
                <div style={{fontSize:18,fontWeight:800,color:'var(--text-primary)'}}>{consents.length}</div>
                <div style={{fontSize:8,color:'var(--text-muted)'}}>TOTAL</div>
              </div>
            </div>
            <div style={{display:'flex',flexDirection:'column',gap:8,flex:1}}>
              {[
                {label:'Active',count:consentStats.active,color:'#22C55E'},
                {label:'Pending',count:consentStats.pending,color:'#F59E0B'},
                {label:'Withdrawn',count:consentStats.withdrawn,color:'#EF4444'},
                {label:'Expired',count:consentStats.expired,color:'#6366F1'},
                {label:'Guardian §9',count:consentStats.guardian,color:'#A855F7'},
              ].map(item => (
                <div key={item.label} style={{display:'flex',alignItems:'center',gap:8}}>
                  <div style={{width:10,height:10,borderRadius:2,background:item.color,flexShrink:0}} />
                  <span style={{fontSize:11,flex:1}}>{item.label}</span>
                  <span style={{fontSize:12,fontWeight:700,color:item.color}}>{item.count}</span>
                  <span style={{fontSize:10,color:'var(--text-muted)'}}>{Math.round(item.count/consents.length*100)}%</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── ANIMATED BAR CHART — Module Compliance Scores ── */}
        <div className="glass-card" data-section="module-compliance">
          <h3 style={{marginBottom:16}}>📊 Module Compliance Scores</h3>
          <div style={{display:'flex',flexDirection:'column',gap:10}}>
            {ragModules.map((m,i) => (
              <div key={m.id} style={{display:'flex',alignItems:'center',gap:8}}>
                <span style={{fontSize:11,width:60,textAlign:'right',fontWeight:600}}>{m.name}</span>
                <div style={{flex:1,background:'var(--border)',borderRadius:6,height:18,overflow:'hidden',position:'relative'}}>
                  <div style={{
                    width:`${m.score}%`,
                    height:'100%',
                    borderRadius:6,
                    background: m.score >= 80 ? 'linear-gradient(90deg, #22C55E, #10B981)' : m.score >= 50 ? 'linear-gradient(90deg, #F59E0B, #EAB308)' : 'linear-gradient(90deg, #EF4444, #DC2626)',
                    transition: 'width 1.2s cubic-bezier(0.4,0,0.2,1)',
                    animation: `barGrow 1.2s ease-out ${i*0.1}s both`,
                    boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
                  }} />
                  <span style={{position:'absolute',right:6,top:0,bottom:0,display:'flex',alignItems:'center',fontSize:10,fontWeight:700,color:'var(--text-primary)'}}>{m.score}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* ── COMPLIANCE HEAT MAP — DPDP Sections ── */}
        <div className="glass-card">
          <h3 style={{marginBottom:12}}>🔥 DPDP Compliance Heat Map</h3>
          <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:6}}>
            {[
              {sec:'§5 Notice',val:95},{sec:'§6 Consent',val:Math.round(consentStats.active/consents.length*100)},
              {sec:'§7 Deemed',val:88},{sec:'§8 Obligations',val:82},
              {sec:'§9 Children',val:consentStats.guardian>0?75:100},{sec:'§10 DPO',val:90},
              {sec:'§11 Access',val:Math.round(rights.filter((r:any)=>r.type==='ACCESS'&&r.status==='COMPLETED').length/(rights.filter((r:any)=>r.type==='ACCESS').length||1)*100)},
              {sec:'§12 Correction',val:85},{sec:'§13 Erasure',val:78},{sec:'§14 Grievance',val:92},
              {sec:'§15 DPBI',val:breaches.filter((b:any)=>b.dpbiNotified).length>0?80:60},
              {sec:'§33 Penalty',val:overallScore},
            ].map((item,i) => (
              <div key={i} style={{
                padding:'8px 4px',borderRadius:'var(--radius)',textAlign:'center',
                background: item.val >= 85 ? 'rgba(34,197,94,0.2)' : item.val >= 60 ? 'rgba(245,158,11,0.2)' : 'rgba(239,68,68,0.2)',
                border: `1px solid ${item.val >= 85 ? 'rgba(34,197,94,0.3)' : item.val >= 60 ? 'rgba(245,158,11,0.3)' : 'rgba(239,68,68,0.3)'}`,
                transition:'transform 0.2s',cursor:'pointer',
              }}
              onMouseEnter={e=>(e.currentTarget.style.transform='scale(1.05)')}
              onMouseLeave={e=>(e.currentTarget.style.transform='scale(1)')}>
                <div style={{fontSize:9,fontWeight:700,color:'var(--text-muted)',textTransform:'uppercase'}}>{item.sec}</div>
                <div style={{fontSize:18,fontWeight:800,color: item.val >= 85 ? '#22C55E' : item.val >= 60 ? '#F59E0B' : '#EF4444',marginTop:2}}>{item.val}%</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ═══ TREND SPARKLINES + BREACH SEVERITY GAUGE ═══ */}
      <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginBottom:16}}>
        {/* Monthly Consent Trends */}
        <div className="glass-card">
          <h3 style={{marginBottom:12}}>📈 Monthly Consent Trends (6 months)</h3>
          <div style={{display:'flex',gap:12,alignItems:'flex-end',height:120}}>
            {[
              {month:'Oct',granted:85,withdrawn:12},{month:'Nov',granted:92,withdrawn:15},
              {month:'Dec',granted:78,withdrawn:8},{month:'Jan',granted:105,withdrawn:18},
              {month:'Feb',granted:112,withdrawn:14},{month:'Mar',granted:Math.round(consentStats.active*0.3),withdrawn:Math.round(consentStats.withdrawn*0.2)},
            ].map((m,i) => (
              <div key={i} style={{flex:1,display:'flex',flexDirection:'column',alignItems:'center',gap:4}}>
                <div style={{display:'flex',gap:2,alignItems:'flex-end',height:90,width:'100%'}}>
                  <div style={{
                    flex:1,borderRadius:'4px 4px 0 0',
                    background:'linear-gradient(180deg, #22C55E, #16A34A)',
                    height:`${(m.granted/120)*100}%`,
                    animation:`barGrow 0.8s ease-out ${i*0.15}s both`,
                    boxShadow:'0 2px 6px rgba(34,197,94,0.3)',
                  }} />
                  <div style={{
                    flex:1,borderRadius:'4px 4px 0 0',
                    background:'linear-gradient(180deg, #EF4444, #DC2626)',
                    height:`${(m.withdrawn/120)*100}%`,
                    animation:`barGrow 0.8s ease-out ${i*0.15+0.1}s both`,
                    boxShadow:'0 2px 6px rgba(239,68,68,0.3)',
                  }} />
                </div>
                <div style={{fontSize:9,fontWeight:600,color:'var(--text-muted)'}}>{m.month}</div>
              </div>
            ))}
          </div>
          <div style={{display:'flex',gap:16,marginTop:10,justifyContent:'center'}}>
            <div style={{display:'flex',alignItems:'center',gap:4}}><div style={{width:8,height:8,borderRadius:2,background:'#22C55E'}}/><span style={{fontSize:10}}>Granted</span></div>
            <div style={{display:'flex',alignItems:'center',gap:4}}><div style={{width:8,height:8,borderRadius:2,background:'#EF4444'}}/><span style={{fontSize:10}}>Withdrawn</span></div>
          </div>
        </div>

        {/* Breach Severity Distribution */}
        <div className="glass-card">
          <h3 style={{marginBottom:12}}>⚠️ Breach Severity Distribution</h3>
          <div style={{display:'flex',gap:16,alignItems:'center'}}>
            {/* Radial Gauge */}
            <div style={{position:'relative',width:120,height:120,flexShrink:0}}>
              <svg viewBox="0 0 100 100" style={{width:120,height:120,transform:'rotate(-90deg)'}}>
                <circle cx="50" cy="50" r="42" fill="none" stroke="var(--border)" strokeWidth="8"/>
                <circle cx="50" cy="50" r="42" fill="none" stroke="#EF4444" strokeWidth="8"
                  strokeDasharray={`${breaches.filter((b:any)=>b.severity==='CRITICAL').length/breaches.length*264} 264`}
                  strokeLinecap="round" style={{animation:'gaugeAnimate 1.5s ease-out'}}/>
                <circle cx="50" cy="50" r="42" fill="none" stroke="#F59E0B" strokeWidth="8"
                  strokeDasharray={`${breaches.filter((b:any)=>b.severity==='HIGH').length/breaches.length*264} 264`}
                  strokeDashoffset={`-${breaches.filter((b:any)=>b.severity==='CRITICAL').length/breaches.length*264}`}
                  strokeLinecap="round"/>
                <circle cx="50" cy="50" r="42" fill="none" stroke="#22C55E" strokeWidth="8"
                  strokeDasharray={`${breaches.filter((b:any)=>['MEDIUM','LOW'].includes(b.severity)).length/breaches.length*264} 264`}
                  strokeDashoffset={`-${(breaches.filter((b:any)=>b.severity==='CRITICAL').length+breaches.filter((b:any)=>b.severity==='HIGH').length)/breaches.length*264}`}
                  strokeLinecap="round"/>
              </svg>
              <div style={{position:'absolute',inset:0,display:'flex',alignItems:'center',justifyContent:'center',flexDirection:'column'}}>
                <div style={{fontSize:22,fontWeight:800,color:'var(--text-primary)'}}>{breaches.length}</div>
                <div style={{fontSize:8,color:'var(--text-muted)'}}>TOTAL</div>
              </div>
            </div>
            <div style={{display:'flex',flexDirection:'column',gap:10,flex:1}}>
              {[
                {label:'CRITICAL',count:breaches.filter((b:any)=>b.severity==='CRITICAL').length,color:'#EF4444',bg:'rgba(239,68,68,0.15)'},
                {label:'HIGH',count:breaches.filter((b:any)=>b.severity==='HIGH').length,color:'#F59E0B',bg:'rgba(245,158,11,0.15)'},
                {label:'MEDIUM',count:breaches.filter((b:any)=>b.severity==='MEDIUM').length,color:'#3B82F6',bg:'rgba(59,130,246,0.15)'},
                {label:'LOW',count:breaches.filter((b:any)=>b.severity==='LOW').length,color:'#22C55E',bg:'rgba(34,197,94,0.15)'},
              ].map(s => (
                <div key={s.label} style={{display:'flex',alignItems:'center',gap:8}}>
                  <div style={{width:10,height:10,borderRadius:2,background:s.color}} />
                  <span style={{fontSize:11,flex:1}}>{s.label}</span>
                  <span style={{fontSize:13,fontWeight:700,color:s.color,background:s.bg,padding:'2px 8px',borderRadius:4}}>{s.count}</span>
                  <div style={{width:60,background:'var(--border)',borderRadius:4,height:6,overflow:'hidden'}}>
                    <div style={{width:`${s.count/breaches.length*100}%`,height:'100%',background:s.color,borderRadius:4}}/>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>


      {/* Activity + Crypto — each activity row clickable with drill-down */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 16 }}>
        <div className="glass-card" data-section="recent-activity">
          <h3 style={{marginBottom:12}}>🕐 Recent Activity (Click to drill down)</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Time</th><th>User</th><th>Action</th><th>Module</th><th>Type</th></tr></thead>
              <tbody>
                {recentActivity.map((a,i)=>(
                  <tr key={i} style={{cursor:'pointer'}} onClick={()=>navigate(a.path)} title={a.detail}>
                    <td style={{fontFamily:'var(--font-mono)',fontSize:12}}>{a.time}</td>
                    <td>{a.user}</td>
                    <td>
                      <div>{a.action}</div>
                      <div style={{fontSize:10,color:'var(--text-muted)',marginTop:2,maxWidth:300}}>{a.detail}</div>
                    </td>
                    <td><span className="rag-badge rag-green" style={{cursor:'pointer'}}>{a.module}</span></td>
                    <td><span className={`rag-badge ${a.type==='ALERT'?'rag-red':a.type==='CREATE'?'rag-green':'rag-amber'}`}>{a.type}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div>
          <div className="glass-card" style={{marginBottom:16}}>
            <h3 style={{marginBottom:12}}>🔐 Crypto</h3>
            <table className="data-table">
              <tbody>
                <tr><td>Key Encap.</td><td><span className="rag-badge" style={{background:'var(--purple-bg)',color:'var(--purple)'}}>ML-KEM-1024</span></td></tr>
                <tr><td>Signature</td><td><span className="rag-badge" style={{background:'var(--purple-bg)',color:'var(--purple)'}}>ML-DSA-87</span></td></tr>
                <tr><td>Symmetric</td><td><span className="rag-badge" style={{background:'var(--blue-bg)',color:'var(--blue)'}}>AES-256-GCM</span></td></tr>
                <tr><td>Fallback</td><td><span className="rag-badge rag-green">RSA-4096</span></td></tr>
              </tbody>
            </table>
          </div>
          <div className="glass-card">
            <h3 style={{marginBottom:12}}>🏛️ Frameworks</h3>
            <table className="data-table">
              <tbody>
                <tr><td>DPDP 2023</td><td><span className="rag-badge rag-green">Primary</span></td></tr>
                <tr><td>GDPR</td><td><span className="rag-badge" style={{background:'var(--blue-bg)',color:'var(--blue)'}}>Mapped</span></td></tr>
                <tr><td>ISO 27001</td><td><span className="rag-badge" style={{background:'var(--blue-bg)',color:'var(--blue)'}}>Mapped</span></td></tr>
                <tr><td>NIST CSF</td><td><span className="rag-badge" style={{background:'var(--blue-bg)',color:'var(--blue)'}}>Mapped</span></td></tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}
