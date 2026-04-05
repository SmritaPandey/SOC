import { useState, useEffect } from 'react'
import api from '../api'

export default function LicensingPage() {
  const [stats, setStats] = useState<any>(null)
  const [tab, setTab] = useState<'licenses'|'plans'|'modules'>('licenses')

  useEffect(() => { api.licensingStats().then(setStats).catch(()=>{}) }, [])

  return (
    <div className="page-container">
      <div className="page-header"><h1>🔑 Licensing & Pricing</h1><p>Module-level licensing, subscription plans, and agreement management</p></div>

      <div className="kpi-grid">
        <div className="kpi-card"><div className="kpi-value">12</div><div className="kpi-label">Available Modules</div></div>
        <div className="kpi-card"><div className="kpi-value">4</div><div className="kpi-label">Pricing Plans</div></div>
        <div className="kpi-card"><div className="kpi-value rag-green">ENTERPRISE</div><div className="kpi-label">Current Plan</div></div>
        <div className="kpi-card"><div className="kpi-value">365</div><div className="kpi-label">Days Remaining</div></div>
      </div>

      <div style={{display:'flex',gap:8,margin:'24px 0'}}>
        {(['licenses','plans','modules'] as const).map(t=>(
          <button key={t} className={tab===t?'btn-primary':'btn-secondary'} onClick={()=>setTab(t)}>
            {t==='licenses'?'🔑 Active Licenses':t==='plans'?'💰 Pricing Plans':'📦 Module Catalog'}
          </button>
        ))}
      </div>

      {tab==='licenses' && (
        <div className="glass-card">
          <h3>🔑 Active License Keys</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Key</th><th>Type</th><th>Modules</th><th>Status</th><th>Expires</th><th>Actions</th></tr></thead>
              <tbody>
                {[
                  {key:'QS-ENT-2026-A7F3-K9D2',type:'Enterprise',modules:'ALL (12)',status:'ACTIVE',expires:'2027-03-13'},
                  {key:'QS-SIEM-TRIAL-B4E8',type:'Trial',modules:'QS-SIEM only',status:'ACTIVE',expires:'2026-03-27'},
                  {key:'QS-DLP-2025-C2M6-P8R1',type:'Professional',modules:'DLP + PII Scanner',status:'EXPIRED',expires:'2025-12-31'},
                ].map((l,i)=>(
                  <tr key={i}>
                    <td style={{fontFamily:'monospace',fontSize:12}}>{l.key}</td>
                    <td>{l.type}</td>
                    <td>{l.modules}</td>
                    <td><span className={`rag-badge ${l.status==='ACTIVE'?'rag-green':'rag-red'}`}>{l.status}</span></td>
                    <td>{l.expires}</td>
                    <td><button className="btn-sm">Renew</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab==='plans' && (
        <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:16}}>
          {[
            {name:'Starter',price:'₹999',modules:['Consent','Breach','Rights','Gap'],agents:10,users:5,color:'#64748b'},
            {name:'Professional',price:'₹4,999',modules:['+ DLP','+ Policy','+ DPIA','+ Chatbot'],agents:100,users:25,color:'#6366f1'},
            {name:'Enterprise',price:'₹14,999',modules:['ALL Modules'],agents:10000,users:500,color:'#10b981',current:true},
            {name:'Sovereign',price:'₹49,999',modules:['ALL + Dedicated','+ Custom SLA'],agents:100000,users:5000,color:'#f59e0b'},
          ].map((p,i)=>(
            <div key={i} className="glass-card" style={{textAlign:'center',border:p.current?'2px solid var(--accent)':'1px solid var(--border)',position:'relative'}}>
              {p.current && <div style={{position:'absolute',top:-12,left:'50%',transform:'translateX(-50%)',background:'var(--accent)',color:'#fff',padding:'2px 16px',borderRadius:12,fontSize:11,fontWeight:700}}>CURRENT</div>}
              <h3 style={{color:p.color,marginTop:p.current?12:0}}>{p.name}</h3>
              <div style={{fontSize:28,fontWeight:800,margin:'16px 0'}}>{p.price}<span style={{fontSize:14,fontWeight:400,color:'var(--text-muted)'}}>/mo</span></div>
              <div style={{fontSize:13,color:'var(--text-muted)',marginBottom:16}}>{p.agents.toLocaleString()} agents • {p.users} users</div>
              <div style={{textAlign:'left',fontSize:13}}>
                {p.modules.map((m,j)=><div key={j} style={{padding:'4px 0'}}>✅ {m}</div>)}
              </div>
              <button className={p.current?'btn-primary':'btn-secondary'} style={{marginTop:16,width:'100%'}}>{p.current?'Current Plan':'Upgrade'}</button>
            </div>
          ))}
        </div>
      )}

      {tab==='modules' && (
        <div className="glass-card">
          <h3>📦 Module Catalog — Individually Licensed Products</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Module</th><th>Product Name</th><th>Description</th><th>Price/mo</th><th>Status</th></tr></thead>
              <tbody>
                {[
                  {id:'CONSENT',name:'Consent Manager',desc:'DPDP Section 6-7 consent lifecycle',price:'₹500',enabled:true},
                  {id:'BREACH',name:'Breach Notifier',desc:'72h DPBI + 6h CERT-IN automation',price:'₹750',enabled:true},
                  {id:'DLP',name:'QS-DLP',desc:'Data Loss Prevention + PII Scanner',price:'₹1,200',enabled:true},
                  {id:'SIEM',name:'QS-SIEM',desc:'SOC + SOAR + Threat Intelligence',price:'₹2,000',enabled:true},
                  {id:'EDR',name:'QS-EDR',desc:'Endpoint Detection & Response',price:'₹1,500',enabled:true},
                  {id:'XDR',name:'QS-XDR',desc:'Extended Detection & Response',price:'₹1,800',enabled:true},
                  {id:'POLICY',name:'Policy Engine',desc:'ISO-aligned policy lifecycle',price:'₹400',enabled:true},
                  {id:'DPIA',name:'DPIA & Audit',desc:'Impact assessment + audit trail',price:'₹600',enabled:true},
                  {id:'RIGHTS',name:'Rights Manager',desc:'Section 11-14 rights workflow',price:'₹500',enabled:true},
                  {id:'GAP',name:'Gap Analysis',desc:'Self-assessment + MCQ + scoring',price:'₹300',enabled:true},
                  {id:'CHATBOT',name:'AI Assistant',desc:'LLM-powered RAG compliance chatbot',price:'₹800',enabled:true},
                  {id:'PQC',name:'Quantum-Safe Crypto',desc:'ML-KEM-1024 + ML-DSA-87',price:'₹1,000',enabled:true},
                ].map((m,i)=>(
                  <tr key={i}>
                    <td style={{fontFamily:'monospace',fontWeight:700}}>{m.id}</td>
                    <td><strong>{m.name}</strong></td>
                    <td style={{fontSize:13,color:'var(--text-muted)'}}>{m.desc}</td>
                    <td style={{fontWeight:700}}>{m.price}</td>
                    <td><span className="rag-badge rag-green">ENABLED</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
