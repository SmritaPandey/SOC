import { useState } from 'react'
import { SECTOR_API_SPECS, SECTORS } from '../data/sectorData'

export default function APIIntegrationPage() {
  const [sector, setSector] = useState<string>('Banking & Finance')
  const [testResult, setTestResult] = useState<Record<string,string>>({})
  const [expandedApi, setExpandedApi] = useState<string|null>(null)

  const apis = SECTOR_API_SPECS[sector] || []
  const readyCount = apis.filter(a=>a.status==='READY').length
  const configuredCount = apis.filter(a=>a.status==='CONFIGURED').length

  const handleTest = (apiName: string) => {
    setTestResult(prev => ({...prev, [apiName]: 'TESTING'}))
    setTimeout(() => {
      setTestResult(prev => ({...prev, [apiName]: Math.random()>0.15 ? 'SUCCESS' : 'TIMEOUT'}))
    }, 1500 + Math.random()*1000)
  }

  return (
    <div>
      <div style={{display:'flex',justifyContent:'space-between',alignItems:'center',marginBottom:24}}>
        <div>
          <h2 style={{margin:0}}>🔗 API Integration Hub</h2>
          <p style={{color:'var(--text-muted)',margin:'4px 0 0'}}>Sector-specific API connectors with DPDP compliance checks</p>
        </div>
        <select value={sector} onChange={e=>setSector(e.target.value)} style={{padding:'8px 16px',background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:8,color:'var(--text-primary)',fontSize:14}}>
          {SECTORS.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {/* Status Overview */}
      <div style={{display:'grid',gridTemplateColumns:'repeat(4,1fr)',gap:12,marginBottom:24}}>
        <div className="kpi-card" style={{borderTop:'3px solid var(--green)',padding:16}}>
          <div style={{fontSize:11,color:'var(--text-muted)'}}>READY</div>
          <div style={{fontSize:28,fontWeight:700,color:'var(--green)'}}>{readyCount}</div>
        </div>
        <div className="kpi-card" style={{borderTop:'3px solid var(--amber)',padding:16}}>
          <div style={{fontSize:11,color:'var(--text-muted)'}}>CONFIGURED</div>
          <div style={{fontSize:28,fontWeight:700,color:'var(--amber)'}}>{configuredCount}</div>
        </div>
        <div className="kpi-card" style={{borderTop:'3px solid var(--blue)',padding:16}}>
          <div style={{fontSize:11,color:'var(--text-muted)'}}>TOTAL APIs</div>
          <div style={{fontSize:28,fontWeight:700}}>{apis.length}</div>
        </div>
        <div className="kpi-card" style={{borderTop:'3px solid var(--purple)',padding:16}}>
          <div style={{fontSize:11,color:'var(--text-muted)'}}>DPDP COMPLIANT</div>
          <div style={{fontSize:28,fontWeight:700,color:'var(--green)'}}>100%</div>
        </div>
      </div>

      {/* API List */}
      <div style={{display:'flex',flexDirection:'column',gap:12}}>
        {apis.map((api,i) => (
          <div key={i} className="glass-card" style={{padding:0,overflow:'hidden'}}>
            <div 
              style={{padding:16,cursor:'pointer',display:'flex',justifyContent:'space-between',alignItems:'center'}}
              onClick={() => setExpandedApi(expandedApi===api.name ? null : api.name)}
            >
              <div style={{display:'flex',alignItems:'center',gap:12}}>
                <span style={{
                  width:10,height:10,borderRadius:'50%',
                  background:api.status==='READY'?'var(--green)':api.status==='CONFIGURED'?'var(--amber)':'var(--text-muted)',
                  boxShadow:api.status==='READY'?'0 0 8px var(--green)':'none'
                }} />
                <div>
                  <div style={{fontWeight:600,fontSize:15}}>{api.name}</div>
                  <div style={{fontSize:12,color:'var(--text-muted)',fontFamily:'monospace'}}>{api.endpoint}</div>
                </div>
              </div>
              <div style={{display:'flex',alignItems:'center',gap:8}}>
                <span style={{fontSize:11,padding:'3px 8px',borderRadius:4,background:api.status==='READY'?'rgba(0,200,100,0.15)':'rgba(255,200,0,0.15)',color:api.status==='READY'?'var(--green)':'var(--amber)',fontWeight:600}}>{api.status}</span>
                <span style={{fontSize:11,padding:'3px 8px',borderRadius:4,background:'var(--bg-surface)'}}>{api.protocol}</span>
                <span style={{fontSize:16,transform:expandedApi===api.name?'rotate(180deg)':'rotate(0)',transition:'transform 0.2s'}}>▼</span>
              </div>
            </div>

            {expandedApi === api.name && (
              <div style={{borderTop:'1px solid var(--border)',padding:16,background:'var(--bg-surface)'}}>
                <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr',gap:16,marginBottom:16}}>
                  <div>
                    <div style={{fontSize:11,color:'var(--text-muted)',marginBottom:4}}>Authentication</div>
                    <div style={{fontSize:13,fontWeight:600}}>{api.auth}</div>
                  </div>
                  <div>
                    <div style={{fontSize:11,color:'var(--text-muted)',marginBottom:4}}>Protocol</div>
                    <div style={{fontSize:13,fontWeight:600}}>{api.protocol}</div>
                  </div>
                  <div>
                    <div style={{fontSize:11,color:'var(--text-muted)',marginBottom:4}}>DPDP Compliance Note</div>
                    <div style={{fontSize:13,fontWeight:600,color:'var(--brand-primary)'}}>{api.dpdpNote}</div>
                  </div>
                </div>

                {/* Data Flow Diagram */}
                <div style={{background:'var(--bg-main)',padding:12,borderRadius:8,marginBottom:12}}>
                  <div style={{fontSize:11,color:'var(--text-muted)',marginBottom:8}}>📊 Data Flow</div>
                  <div style={{display:'flex',alignItems:'center',gap:8,flexWrap:'wrap'}}>
                    {['Source App','→','Consent Engine','→',api.name.split('(')[0].trim(),'→','DPDP Audit','→','Response'].map((step,j) => (
                      step === '→' ? <span key={j} style={{color:'var(--brand-primary)',fontWeight:700}}>→</span> :
                      <span key={j} style={{padding:'4px 10px',background:'var(--bg-surface)',borderRadius:6,fontSize:12,border:'1px solid var(--border)'}}>{step}</span>
                    ))}
                  </div>
                </div>

                {/* Test Connection */}
                <div style={{display:'flex',gap:8}}>
                  <button onClick={()=>handleTest(api.name)} disabled={testResult[api.name]==='TESTING'} style={{padding:'8px 16px',background:'var(--brand-primary)',color:'#fff',border:'none',borderRadius:8,cursor:'pointer',fontSize:13}}>
                    {testResult[api.name]==='TESTING' ? '⏳ Testing...' : '🔌 Test Connection'}
                  </button>
                  <button style={{padding:'8px 16px',background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:8,cursor:'pointer',color:'var(--text-primary)',fontSize:13}}>📖 API Docs</button>
                  <button style={{padding:'8px 16px',background:'var(--bg-surface)',border:'1px solid var(--border)',borderRadius:8,cursor:'pointer',color:'var(--text-primary)',fontSize:13}}>⚙️ Configure</button>
                  {testResult[api.name] && testResult[api.name]!=='TESTING' && (
                    <span style={{display:'flex',alignItems:'center',padding:'0 12px',borderRadius:8,fontSize:12,fontWeight:700,background:testResult[api.name]==='SUCCESS'?'rgba(0,200,100,0.15)':'rgba(255,0,0,0.15)',color:testResult[api.name]==='SUCCESS'?'var(--green)':'var(--red)'}}>
                      {testResult[api.name]==='SUCCESS' ? '✅ Connected' : '❌ Timeout'}
                    </span>
                  )}
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
