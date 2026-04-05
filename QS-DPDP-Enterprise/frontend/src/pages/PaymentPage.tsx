import { useState } from 'react'

const GATEWAYS = [
  {id:'razorpay',name:'Razorpay',desc:'India\'s leading payment gateway — UPI, Cards, Net Banking',logo:'💳',status:'CONFIGURED'},
  {id:'cashfree',name:'Cashfree Payments',desc:'NPCI approved — PayLater, UPI, EMI',logo:'🏦',status:'CONFIGURED'},
  {id:'payu',name:'PayU',desc:'PCI DSS Level 1 certified payment gateway',logo:'💰',status:'NOT_CONFIGURED'},
  {id:'paypal',name:'PayPal',desc:'International payments — Credit/Debit cards globally',logo:'🌐',status:'NOT_CONFIGURED'},
  {id:'getepay',name:'GetePay',desc:'Indian payment aggregator — UPI, Wallets, Cards',logo:'📱',status:'NOT_CONFIGURED'},
  {id:'stripe',name:'Stripe',desc:'Developer-first payment processing',logo:'⚡',status:'NOT_CONFIGURED'},
]

const demoTransactions = [
  {id:'TXN-001',date:'2026-03-13',customer:'Acme Corp',amount:'₹14,999',plan:'Enterprise',gateway:'Razorpay',status:'SUCCESS',mode:'UPI'},
  {id:'TXN-002',date:'2026-03-12',customer:'TechStart Inc',amount:'₹4,999',plan:'Professional',gateway:'Cashfree',status:'SUCCESS',mode:'Net Banking'},
  {id:'TXN-003',date:'2026-03-11',customer:'HealthPlus',amount:'₹999',plan:'Starter',gateway:'Razorpay',status:'SUCCESS',mode:'Credit Card'},
  {id:'TXN-004',date:'2026-03-10',customer:'EduLearn Pvt Ltd',amount:'₹4,999',plan:'Professional',gateway:'Razorpay',status:'FAILED',mode:'Debit Card'},
  {id:'TXN-005',date:'2026-03-09',customer:'FinServ Ltd',amount:'₹49,999',plan:'Sovereign',gateway:'Cashfree',status:'SUCCESS',mode:'NEFT'},
]

export default function PaymentPage() {
  const [tab, setTab] = useState<'gateways'|'transactions'|'links'>('gateways')
  const [selectedGw, setSelectedGw] = useState<any>(null)

  return (
    <div className="page-container">
      <div className="page-header"><h1>💳 Payment Gateway</h1><p>NPCI-approved payment processing for license subscriptions</p></div>

      <div className="kpi-grid">
        <div className="kpi-card"><div className="kpi-value">{GATEWAYS.filter(g=>g.status==='CONFIGURED').length}</div><div className="kpi-label">Active Gateways</div></div>
        <div className="kpi-card"><div className="kpi-value">{demoTransactions.length}</div><div className="kpi-label">Transactions</div></div>
        <div className="kpi-card"><div className="kpi-value rag-green">₹75,995</div><div className="kpi-label">Total Revenue</div></div>
        <div className="kpi-card"><div className="kpi-value rag-green">80%</div><div className="kpi-label">Success Rate</div></div>
      </div>

      <div style={{display:'flex',gap:8,margin:'24px 0'}}>
        {(['gateways','transactions','links'] as const).map(t=>(
          <button key={t} className={tab===t?'btn-primary':'btn-secondary'} onClick={()=>setTab(t)}>
            {t==='gateways'?'🏦 Payment Gateways':t==='transactions'?'📊 Transactions':'🔗 Payment Links'}
          </button>
        ))}
      </div>

      {tab==='gateways' && (
        <div>
          <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:16}}>
            {GATEWAYS.map((g,i)=>(
              <div key={i} className="glass-card" onClick={()=>setSelectedGw(g)} style={{cursor:'pointer',border:selectedGw?.id===g.id?'2px solid var(--accent)':'1px solid var(--border)'}}>
                <div style={{display:'flex',justifyContent:'space-between',alignItems:'center'}}>
                  <span style={{fontSize:32}}>{g.logo}</span>
                  <span className={`rag-badge ${g.status==='CONFIGURED'?'rag-green':'rag-amber'}`}>{g.status}</span>
                </div>
                <h3 style={{margin:'12px 0 4px'}}>{g.name}</h3>
                <p style={{fontSize:13,color:'var(--text-muted)'}}>{g.desc}</p>
                <div style={{marginTop:12,display:'flex',gap:8}}>
                  <button className={g.status==='CONFIGURED'?'btn-primary':'btn-secondary'} style={{fontSize:12}}>
                    {g.status==='CONFIGURED'?'✅ Connected':'Configure'}
                  </button>
                </div>
              </div>
            ))}
          </div>

          {selectedGw && selectedGw.status!=='CONFIGURED' && (
            <div className="glass-card" style={{marginTop:24}}>
              <h3>⚙️ Configure {selectedGw.name}</h3>
              <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginTop:16}}>
                <input className="form-input" placeholder="Merchant ID" />
                <input className="form-input" placeholder="API Key" type="password" />
                <input className="form-input" placeholder="API Secret" type="password" />
                <input className="form-input" placeholder="Webhook URL" />
                <select className="form-input">
                  <option>Test / Sandbox Mode</option>
                  <option>Production Mode</option>
                </select>
                <button className="btn-primary">🔗 Connect & Verify</button>
              </div>
              <div style={{marginTop:16}}>
                <h4>Supported Payment Modes</h4>
                <div style={{display:'flex',gap:8,marginTop:8,flexWrap:'wrap'}}>
                  {['UPI','Credit Card','Debit Card','Net Banking','NEFT/RTGS','Wallets','EMI','PayLater'].map(m=>(
                    <span key={m} style={{padding:'6px 14px',borderRadius:20,background:'var(--bg-primary)',fontSize:12,border:'1px solid var(--border)'}}>{m}</span>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {tab==='transactions' && (
        <div className="glass-card">
          <h3>📊 Transaction History</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>ID</th><th>Date</th><th>Customer</th><th>Plan</th><th>Amount</th><th>Gateway</th><th>Mode</th><th>Status</th></tr></thead>
              <tbody>
                {demoTransactions.map((t,i)=>(
                  <tr key={i}>
                    <td style={{fontFamily:'monospace'}}>{t.id}</td>
                    <td>{t.date}</td>
                    <td><strong>{t.customer}</strong></td>
                    <td>{t.plan}</td>
                    <td style={{fontWeight:700}}>{t.amount}</td>
                    <td>{t.gateway}</td>
                    <td>{t.mode}</td>
                    <td><span className={`rag-badge ${t.status==='SUCCESS'?'rag-green':'rag-red'}`}>{t.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {tab==='links' && (
        <div className="glass-card">
          <h3>🔗 Generate Payment Link</h3>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12,marginTop:16}}>
            <input className="form-input" placeholder="Customer Name" />
            <input className="form-input" placeholder="Email" />
            <select className="form-input">
              <option>Starter — ₹999/mo</option>
              <option>Professional — ₹4,999/mo</option>
              <option>Enterprise — ₹14,999/mo</option>
              <option>Sovereign — ₹49,999/mo</option>
              <option>Custom Amount</option>
            </select>
            <select className="form-input">
              <option>Razorpay</option><option>Cashfree</option><option>PayU</option>
            </select>
            <button className="btn-primary" style={{gridColumn:'1/3'}}>🔗 Generate & Send Payment Link</button>
          </div>
        </div>
      )}
    </div>
  )
}
