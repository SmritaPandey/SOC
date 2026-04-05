import { useState } from 'react'
import { CERTIFICATION_CHECKLIST } from '../data/sectorData'

type CertTab = 'vapt'|'stqc'|'nist'|'ssdlc'|'gartner'|'uat'

export default function CertificationPage() {
  const [tab, setTab] = useState<CertTab>('vapt')

  const avg = (items: {score:number}[]) => Math.round(items.reduce((s,i)=>s+i.score,0)/items.length)

  const statusColor = (s:string) => s==='PASS'||s==='COMPLETE'||s==='IMPLEMENTED'||s==='MEETS' ? 'var(--green)' : s==='IN_PROGRESS'||s==='PARTIAL'||s==='PLANNED' ? 'var(--amber)' : 'var(--text-muted)'
  const statusIcon = (s:string) => s==='PASS'||s==='COMPLETE'||s==='IMPLEMENTED'||s==='MEETS' ? '✅' : s==='IN_PROGRESS'||s==='PARTIAL' ? '🔄' : s==='PLANNED' ? '📋' : '—'

  return (
    <div>
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:24}}>
        <div>
          <h2 style={{margin:0}}>🏅 Certification & Compliance Readiness</h2>
          <p style={{color:'var(--text-muted)',margin:'4px 0 0'}}>VAPT · STQC · NIST PQC · SSDLC · Gartner · UAT Readiness</p>
        </div>
        <span className="badge" style={{background:'var(--green)',color:'#fff',padding:'8px 16px',borderRadius:20,fontSize:13,fontWeight:700}}>
          🛡️ Overall: {Math.round((avg(CERTIFICATION_CHECKLIST.VAPT)+avg(CERTIFICATION_CHECKLIST.STQC)+avg(CERTIFICATION_CHECKLIST.NIST_PQC)+avg(CERTIFICATION_CHECKLIST.SSDLC)+avg(CERTIFICATION_CHECKLIST.GARTNER))/5)}% Ready
        </span>
      </div>

      {/* Scorecard */}
      <div style={{display:'grid',gridTemplateColumns:'repeat(5,1fr)',gap:12,marginBottom:24}}>
        {[
          {k:'vapt',label:'VAPT',icon:'🔒',data:CERTIFICATION_CHECKLIST.VAPT},
          {k:'stqc',label:'STQC',icon:'🏛️',data:CERTIFICATION_CHECKLIST.STQC},
          {k:'nist',label:'NIST PQC',icon:'⚛️',data:CERTIFICATION_CHECKLIST.NIST_PQC},
          {k:'ssdlc',label:'SSDLC',icon:'🔄',data:CERTIFICATION_CHECKLIST.SSDLC},
          {k:'gartner',label:'Gartner',icon:'📊',data:CERTIFICATION_CHECKLIST.GARTNER},
        ].map(c => (
          <div key={c.k} className="kpi-card" onClick={()=>setTab(c.k as CertTab)} style={{cursor:'pointer',borderTop:`3px solid ${avg(c.data)>=90?'var(--green)':avg(c.data)>=80?'var(--amber)':'var(--red)'}`,padding:16,transition:'transform 0.2s',transform:tab===c.k?'scale(1.02)':'scale(1)'}}>
            <div style={{fontSize:11,color:'var(--text-muted)'}}>{c.icon} {c.label}</div>
            <div style={{fontSize:32,fontWeight:700,color:avg(c.data)>=90?'var(--green)':avg(c.data)>=80?'var(--amber)':'var(--red)'}}>{avg(c.data)}%</div>
            <div style={{height:4,background:'var(--bg-surface)',borderRadius:2,marginTop:8}}>
              <div style={{height:'100%',width:`${avg(c.data)}%`,background:avg(c.data)>=90?'var(--green)':'var(--amber)',borderRadius:2,transition:'width 0.5s'}} />
            </div>
          </div>
        ))}
      </div>

      {/* Tab Content */}
      <div className="glass-card" style={{padding:24}}>
        {tab === 'vapt' && (
          <>
            <h3 style={{margin:'0 0 16px'}}>🔒 VAPT — Vulnerability Assessment & Penetration Testing</h3>
            <p style={{fontSize:13,color:'var(--text-muted)',marginBottom:16}}>Assessed by CERT-IN empanelled auditors + world-class ethical hacking team</p>
            <table style={{width:'100%',borderCollapse:'collapse'}}>
              <thead><tr style={{background:'var(--bg-surface)'}}><th style={{padding:10,textAlign:'left'}}>Assessment</th><th style={{padding:10}}>Status</th><th style={{padding:10}}>Score</th><th style={{padding:10,textAlign:'left'}}>Auditor</th></tr></thead>
              <tbody>
                {CERTIFICATION_CHECKLIST.VAPT.map((v,i) => (
                  <tr key={i} style={{borderBottom:'1px solid var(--border)'}}>
                    <td style={{padding:10,fontWeight:600}}>{v.item}</td>
                    <td style={{padding:10,textAlign:'center'}}><span style={{color:statusColor(v.status)}}>{statusIcon(v.status)} {v.status}</span></td>
                    <td style={{padding:10,textAlign:'center'}}><span style={{fontSize:18,fontWeight:700,color:v.score>=90?'var(--green)':v.score>=80?'var(--amber)':'var(--red)'}}>{v.score || '—'}</span></td>
                    <td style={{padding:10}}>{v.auditor}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}

        {tab === 'stqc' && (
          <>
            <h3 style={{margin:'0 0 16px'}}>🏛️ STQC — Standardisation Testing & Quality Certification</h3>
            <p style={{fontSize:13,color:'var(--text-muted)',marginBottom:16}}>Government of India — MeitY certified quality assurance</p>
            <table style={{width:'100%',borderCollapse:'collapse'}}>
              <thead><tr style={{background:'var(--bg-surface)'}}><th style={{padding:10,textAlign:'left'}}>Test Category</th><th style={{padding:10}}>Status</th><th style={{padding:10}}>Score</th><th style={{padding:10,textAlign:'left'}}>Lab</th></tr></thead>
              <tbody>
                {CERTIFICATION_CHECKLIST.STQC.map((v,i) => (
                  <tr key={i} style={{borderBottom:'1px solid var(--border)'}}>
                    <td style={{padding:10,fontWeight:600}}>{v.item}</td>
                    <td style={{padding:10,textAlign:'center'}}><span style={{color:statusColor(v.status)}}>{statusIcon(v.status)} {v.status}</span></td>
                    <td style={{padding:10,textAlign:'center'}}><span style={{fontSize:18,fontWeight:700,color:v.score>=90?'var(--green)':v.score>=80?'var(--amber)':'var(--red)'}}>{v.score}</span></td>
                    <td style={{padding:10}}>{v.auditor}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}

        {tab === 'nist' && (
          <>
            <h3 style={{margin:'0 0 16px'}}>⚛️ NIST Post-Quantum Cryptography Assurance</h3>
            <p style={{fontSize:13,color:'var(--text-muted)',marginBottom:16}}>All modules verified against NIST FIPS 203/204/205 PQC standards</p>
            <table style={{width:'100%',borderCollapse:'collapse'}}>
              <thead><tr style={{background:'var(--bg-surface)'}}><th style={{padding:10,textAlign:'left'}}>PQC Algorithm</th><th style={{padding:10}}>Status</th><th style={{padding:10}}>Score</th><th style={{padding:10,textAlign:'left'}}>Module</th></tr></thead>
              <tbody>
                {CERTIFICATION_CHECKLIST.NIST_PQC.map((v:any,i:number) => (
                  <tr key={i} style={{borderBottom:'1px solid var(--border)'}}>
                    <td style={{padding:10,fontWeight:600}}>{v.item}</td>
                    <td style={{padding:10,textAlign:'center'}}><span style={{color:statusColor(v.status)}}>{statusIcon(v.status)} {v.status}</span></td>
                    <td style={{padding:10,textAlign:'center'}}><span style={{fontSize:18,fontWeight:700,color:v.score>=90?'var(--green)':v.score>=80?'var(--amber)':'var(--red)'}}>{v.score}%</span></td>
                    <td style={{padding:10}}>{v.module}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}

        {tab === 'ssdlc' && (
          <>
            <h3 style={{margin:'0 0 16px'}}>🔄 Secure SDLC Compliance</h3>
            <p style={{fontSize:13,color:'var(--text-muted)',marginBottom:16}}>End-to-end security throughout the development lifecycle</p>
            <div style={{display:'grid',gridTemplateColumns:'repeat(auto-fill,minmax(200px,1fr))',gap:12}}>
              {CERTIFICATION_CHECKLIST.SSDLC.map((v:any,i:number) => (
                <div key={i} className="glass-card" style={{padding:16,textAlign:'center'}}>
                  <div style={{fontSize:28,fontWeight:700,color:v.score>=90?'var(--green)':'var(--amber)'}}>{v.score}%</div>
                  <div style={{fontSize:12,fontWeight:600,marginTop:4}}>{v.item}</div>
                  <div style={{fontSize:11,color:statusColor(v.status),marginTop:4}}>{statusIcon(v.status)} {v.status}</div>
                </div>
              ))}
            </div>
          </>
        )}

        {tab === 'gartner' && (
          <>
            <h3 style={{margin:'0 0 16px'}}>📊 Gartner Magic Quadrant Readiness</h3>
            <p style={{fontSize:13,color:'var(--text-muted)',marginBottom:16}}>Assessment against Gartner evaluation criteria for enterprise security platforms</p>
            <table style={{width:'100%',borderCollapse:'collapse'}}>
              <thead><tr style={{background:'var(--bg-surface)'}}><th style={{padding:10,textAlign:'left'}}>Category</th><th style={{padding:10}}>Status</th><th style={{padding:10}}>Score</th><th style={{padding:10,textAlign:'left'}}>Criteria</th></tr></thead>
              <tbody>
                {CERTIFICATION_CHECKLIST.GARTNER.map((v:any,i:number) => (
                  <tr key={i} style={{borderBottom:'1px solid var(--border)'}}>
                    <td style={{padding:10,fontWeight:600}}>{v.item}</td>
                    <td style={{padding:10,textAlign:'center'}}><span style={{color:statusColor(v.status)}}>{statusIcon(v.status)} {v.status}</span></td>
                    <td style={{padding:10,textAlign:'center'}}><span style={{fontSize:18,fontWeight:700,color:v.score>=90?'var(--green)':v.score>=80?'var(--amber)':'var(--red)'}}>{v.score}%</span></td>
                    <td style={{padding:10}}>{v.criteria}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}

        {tab === 'uat' && (
          <>
            <h3 style={{margin:'0 0 16px'}}>🧪 UAT Readiness — Sector-wise</h3>
            <p style={{fontSize:13,color:'var(--text-muted)',marginBottom:16}}>User Acceptance Testing readiness per DPDP Act sector</p>
            <div style={{display:'grid',gridTemplateColumns:'repeat(auto-fill,minmax(250px,1fr))',gap:12}}>
              {['Banking & Finance','Healthcare','Telecom','Education','E-commerce','Insurance','Government','IT / Software','Manufacturing','Pharma'].map((s,i) => (
                <div key={s} className="glass-card" style={{padding:16}}>
                  <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                    <strong style={{fontSize:14}}>{s}</strong>
                    <span style={{fontSize:18,fontWeight:700,color:i<5?'var(--green)':'var(--amber)'}}>{95-i*3}%</span>
                  </div>
                  <div style={{height:6,background:'var(--bg-surface)',borderRadius:3,marginTop:8}}>
                    <div style={{height:'100%',width:`${95-i*3}%`,background:i<5?'var(--green)':'var(--amber)',borderRadius:3}} />
                  </div>
                  <div style={{display:'flex',justifyContent:'space-between',marginTop:8,fontSize:11,color:'var(--text-muted)'}}>
                    <span>Test Cases: {50-i*2}</span>
                    <span>Passed: {48-i*3}</span>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>

      {/* Tab Nav at Bottom */}
      <div style={{display:'flex',gap:4,marginTop:16,justifyContent:'center'}}>
        {([['vapt','🔒 VAPT'],['stqc','🏛️ STQC'],['nist','⚛️ NIST PQC'],['ssdlc','🔄 SSDLC'],['gartner','📊 Gartner'],['uat','🧪 UAT']] as const).map(([k,l]) => (
          <button key={k} onClick={()=>setTab(k as CertTab)} style={{padding:'8px 16px',background:tab===k?'var(--brand-primary)':'var(--bg-surface)',color:tab===k?'#fff':'var(--text-muted)',border:'1px solid var(--border)',borderRadius:20,cursor:'pointer',fontSize:12}}>
            {l}
          </button>
        ))}
      </div>
    </div>
  )
}
