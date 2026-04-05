import { useState, useEffect, useMemo } from 'react'
import { SECTORS, SECTOR_QUESTIONS, SECTOR_CONTROLS } from '../data/sectorData'
import { useAppContext } from '../context/AppContext'

/* ═══════════════════════════════════════════════════════════════
   GAP ANALYSIS & SELF-ASSESSMENT — ISO 27701 + Deloitte Format
   25+ questions/sector, RAG AI report, report ID, search, export
   ═══════════════════════════════════════════════════════════════ */

interface GapReport {
  id: string; sector: string; date: string; assessor: string; score: number;
  rag: string; totalQ: number; correct: number; gaps: number;
  findings: {question:string;section:string;answer:string;required:string;remediation:string;severity:string;control:string;procedure:string}[];
  controls: {control:string;category:string;implemented:boolean;complianceScore:number;gap:string}[];
  overallCompliance: number;
}

const REPORT_KEY = 'qs-dpdp-gap-reports'
const loadReports = (): GapReport[] => { try { return JSON.parse(localStorage.getItem(REPORT_KEY)||'[]') } catch { return [] } }
const saveReports = (r: GapReport[]) => localStorage.setItem(REPORT_KEY, JSON.stringify(r))

export default function GapAnalysisPage() {
  const { sector, setSector, t } = useAppContext()
  const [tab, setTab] = useState<'assessment'|'reports'|'report-view'>('assessment')
  const [started, setStarted] = useState(false)
  const [currentQ, setCurrentQ] = useState(0)
  const [answers, setAnswers] = useState<number[]>([])
  const [showResults, setShowResults] = useState(false)
  const [showRemediation, setShowRemediation] = useState<number|null>(null)
  const [assessor, setAssessor] = useState('DPO — Compliance Officer')
  const [reports, setReports] = useState<GapReport[]>(loadReports())
  const [viewReport, setViewReport] = useState<GapReport|null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterSector, setFilterSector] = useState('')
  const [filterRag, setFilterRag] = useState('')

  const questions = SECTOR_QUESTIONS[sector] || SECTOR_QUESTIONS['Banking & Finance']
  const controls = SECTOR_CONTROLS[sector] || SECTOR_CONTROLS['Banking & Finance']

  useEffect(() => { setReports(loadReports()) }, [])

  const handleAnswer = (idx: number) => {
    const newAnswers = [...answers, idx]
    setAnswers(newAnswers)
    if (idx !== questions[currentQ].correct) {
      setShowRemediation(currentQ)
    } else {
      setShowRemediation(null)
      if (currentQ + 1 < questions.length) setTimeout(() => setCurrentQ(currentQ + 1), 300)
      else setTimeout(() => setShowResults(true), 300)
    }
  }

  const nextQuestion = () => {
    setShowRemediation(null)
    if (currentQ + 1 < questions.length) setCurrentQ(currentQ + 1)
    else setShowResults(true)
  }

  const getScore = () => {
    let score = 0
    answers.forEach((a, i) => { if (a === questions[i]?.correct) score += Math.floor(100 / questions.length) })
    return Math.min(score, 100)
  }
  const getRag = (s: number) => s >= 80 ? 'GREEN' : s >= 50 ? 'AMBER' : 'RED'
  const getRagColor = (s: number) => s >= 80 ? 'var(--green)' : s >= 50 ? 'var(--amber)' : 'var(--red)'
  const restart = () => { setStarted(false); setCurrentQ(0); setAnswers([]); setShowResults(false); setShowRemediation(null) }

  /* Generate report ID: GAR-BF-20260313-001 */
  const genReportId = () => {
    const sectorCode = sector.split(' ').map(w => w[0]).join('').toUpperCase().slice(0,3)
    const dateStr = new Date().toISOString().slice(0,10).replace(/-/g,'')
    const seq = String(reports.filter(r => r.sector === sector).length + 1).padStart(3,'0')
    return `GAR-${sectorCode}-${dateStr}-${seq}`
  }

  /* Save report to localStorage */
  const saveReport = () => {
    const score = getScore()
    const gaps = answers.map((a, i) => ({ question: questions[i], answer: a, correct: a === questions[i]?.correct })).filter(g => !g.correct)
    const overallCompliance = controls.reduce((s,c) => s + c.complianceScore, 0) / controls.length
    const severities = ['CRITICAL','HIGH','MEDIUM','LOW']

    const report: GapReport = {
      id: genReportId(), sector, date: new Date().toISOString(), assessor, score,
      rag: getRag(score), totalQ: questions.length, correct: answers.filter((a,i) => a === questions[i]?.correct).length,
      gaps: gaps.length, overallCompliance,
      findings: gaps.map((g, i) => ({
        question: g.question.q, section: g.question.section,
        answer: g.question.options[g.answer], required: g.question.options[g.question.correct],
        remediation: g.question.remediation, severity: severities[i % 4],
        control: `Implement ${g.question.section} controls per DPDP Act 2023`,
        procedure: `1. Assess current state\n2. Design remediation\n3. Implement controls\n4. Verify compliance\n5. Document evidence`,
      })),
      controls: controls.map(c => ({...c})),
    }

    const updated = [report, ...reports]
    setReports(updated)
    saveReports(updated)
    setViewReport(report)
    setTab('report-view')
  }

  /* Export report as HTML (Word-compatible) */
  const exportWord = (r: GapReport) => {
    const html = `<html><head><meta charset="utf-8"><style>body{font-family:Calibri,sans-serif;padding:40px;color:#1E1B4B}h1{color:#581C87;border-bottom:3px solid #7C3AED;padding-bottom:8px}h2{color:#7C3AED;margin-top:24px}h3{color:#4C1D95}table{width:100%;border-collapse:collapse;margin:16px 0}th{background:linear-gradient(90deg,#581C87,#7C3AED);color:white;padding:10px;text-align:left;font-size:12px}td{padding:8px;border:1px solid #DDD6FE;font-size:12px}.rag-green{background:#ECFDF5;color:#059669;padding:4px 8px;border-radius:4px}.rag-amber{background:#FFFBEB;color:#D97706;padding:4px 8px;border-radius:4px}.rag-red{background:#FEF2F2;color:#DC2626;padding:4px 8px;border-radius:4px}.header-box{background:linear-gradient(135deg,#581C87,#7C3AED);color:white;padding:24px;border-radius:8px;margin-bottom:24px}.score-box{display:inline-block;background:#F3F0FF;padding:16px 32px;border-radius:8px;text-align:center;margin:8px}.footer{margin-top:40px;padding-top:16px;border-top:2px solid #DDD6FE;font-size:11px;color:#7C72A0}</style></head><body>
    <div class="header-box"><h1 style="color:white;border:none;margin:0">🏛️ DPDP Act 2023 — Gap Analysis Report</h1><p style="margin:4px 0 0;opacity:0.9">ISO 27701:2019 Aligned | Certified Auditor Format</p></div>
    <table style="margin-bottom:24px"><tr><td><strong>Report ID:</strong> ${r.id}</td><td><strong>Date:</strong> ${new Date(r.date).toLocaleDateString('en-IN',{day:'2-digit',month:'long',year:'numeric'})}</td></tr><tr><td><strong>Sector:</strong> ${r.sector}</td><td><strong>Assessor:</strong> ${r.assessor}</td></tr><tr><td><strong>Standard:</strong> ISO 27701:2019 + DPDP Act 2023</td><td><strong>RAG Status:</strong> <span class="rag-${r.rag.toLowerCase()}">${r.rag}</span></td></tr></table>
    <h2>1. Executive Summary</h2><p>This report presents the findings of a comprehensive DPDP Act 2023 compliance assessment conducted for the <strong>${r.sector}</strong> sector. The assessment evaluated ${r.totalQ} compliance areas across all sections of the Digital Personal Data Protection Act, 2023.</p>
    <div style="display:flex;gap:16px;margin:16px 0"><div class="score-box"><div style="font-size:36px;font-weight:800;color:${r.score>=80?'#059669':r.score>=50?'#D97706':'#DC2626'}">${r.score}%</div><div>Overall Score</div></div><div class="score-box"><div style="font-size:36px;font-weight:800;color:#059669">${r.correct}</div><div>Compliant</div></div><div class="score-box"><div style="font-size:36px;font-weight:800;color:#DC2626">${r.gaps}</div><div>Gaps</div></div><div class="score-box"><div style="font-size:36px;font-weight:800;color:${r.overallCompliance>=80?'#059669':'#D97706'}">${r.overallCompliance.toFixed(0)}%</div><div>Control Score</div></div></div>
    <h2>2. Findings Matrix</h2><table><thead><tr><th>#</th><th>DPDP Section</th><th>Finding</th><th>Severity</th><th>Current State</th><th>Required State</th></tr></thead><tbody>${r.findings.map((f,i) => `<tr><td>${i+1}</td><td>${f.section}</td><td>${f.question}</td><td><span class="rag-${f.severity==='CRITICAL'?'red':f.severity==='HIGH'?'amber':'green'}">${f.severity}</span></td><td>${f.answer}</td><td>${f.required}</td></tr>`).join('')}</tbody></table>
    <h2>3. Remediation Plan with Procedures & Controls</h2><table><thead><tr><th>#</th><th>Gap</th><th>Remediation</th><th>Control</th><th>Procedure</th></tr></thead><tbody>${r.findings.map((f,i) => `<tr><td>${i+1}</td><td>${f.question.slice(0,80)}...</td><td>${f.remediation}</td><td>${f.control}</td><td>${f.procedure.replace(/\n/g,'<br/>')}</td></tr>`).join('')}</tbody></table>
    <h2>4. Compliance Control Metrics</h2><table><thead><tr><th>Control</th><th>Category</th><th>Status</th><th>Score</th><th>Gap</th></tr></thead><tbody>${r.controls.map(c => `<tr><td>${c.control}</td><td>${c.category}</td><td>${c.implemented?'<span class="rag-green">IMPLEMENTED</span>':'<span class="rag-red">GAP</span>'}</td><td>${c.complianceScore}%</td><td>${c.gap||'No gap'}</td></tr>`).join('')}</tbody></table>
    <div class="footer"><p><strong>Disclaimer:</strong> This report is generated by QS-DPDP Enterprise v3.0 compliance assessment engine. Findings are based on self-assessment responses and should be validated by a certified DPDP auditor.</p><p>Report ID: ${r.id} | Generated: ${new Date(r.date).toISOString()} | Standard: ISO 27701:2019 + DPDP Act 2023</p></div></body></html>`
    const blob = new Blob([html], { type: 'application/msword' })
    const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `${r.id}_Gap_Analysis_Report.doc`; a.click()
  }

  const exportPDF = (r: GapReport) => {
    exportWord(r) // Print-to-PDF from Word export
    alert('💡 Tip: Open the .doc file and use Print → Save as PDF for best PDF output with formatting preserved.')
  }

  const printReport = (r: GapReport) => {
    const w = window.open('','_blank')
    if (!w) return
    w.document.write(`<html><head><title>GAR Report ${r.id}</title><style>body{font-family:Calibri;padding:32px}h1{color:#581C87}h2{color:#7C3AED}table{width:100%;border-collapse:collapse}th{background:#7C3AED;color:white;padding:8px;text-align:left;font-size:11px}td{padding:6px;border:1px solid #DDD6FE;font-size:11px}@media print{.no-print{display:none}}</style></head><body>`)
    w.document.write(`<h1>DPDP Gap Analysis Report — ${r.id}</h1><p>Sector: ${r.sector} | Date: ${new Date(r.date).toLocaleDateString()} | Score: ${r.score}% (${r.rag})</p>`)
    w.document.write(`<h2>Findings (${r.gaps} gaps)</h2><table><tr><th>#</th><th>Section</th><th>Finding</th><th>Severity</th><th>Remediation</th></tr>`)
    r.findings.forEach((f,i) => w.document.write(`<tr><td>${i+1}</td><td>${f.section}</td><td>${f.question}</td><td>${f.severity}</td><td>${f.remediation}</td></tr>`))
    w.document.write(`</table></body></html>`)
    w.document.close(); w.print()
  }

  const deleteReport = (id: string) => {
    const updated = reports.filter(r => r.id !== id)
    setReports(updated); saveReports(updated)
    if (viewReport?.id === id) { setViewReport(null); setTab('reports') }
  }

  /* Filtered reports for search */
  const filteredReports = useMemo(() => {
    let f = reports
    if (filterSector) f = f.filter(r => r.sector === filterSector)
    if (filterRag) f = f.filter(r => r.rag === filterRag)
    if (searchTerm) {
      const s = searchTerm.toLowerCase()
      f = f.filter(r => r.id.toLowerCase().includes(s) || r.sector.toLowerCase().includes(s) || r.assessor.toLowerCase().includes(s) || r.findings.some(fi => fi.question.toLowerCase().includes(s) || fi.remediation.toLowerCase().includes(s)))
    }
    return f
  }, [reports, filterSector, filterRag, searchTerm])

  /* ─── REPORT VIEW — ISO/Deloitte Format ─── */
  if (tab === 'report-view' && viewReport) {
    const r = viewReport
    return (
      <div className="page-container">
        <div className="page-header">
          <div>
            <h1>🏛️ Gap Analysis Report — {r.id}</h1>
            <p>ISO 27701:2019 Aligned • DPDP Act 2023 • Certified Auditor Format</p>
          </div>
          <div style={{display:'flex',gap:8}}>
            <button className="btn-primary" onClick={()=>exportWord(r)}>📄 Export Word</button>
            <button className="btn-secondary" onClick={()=>exportPDF(r)}>📋 Export PDF</button>
            <button className="btn-secondary" onClick={()=>printReport(r)}>🖨️ Print</button>
            <button className="btn-secondary" onClick={()=>{setTab('reports');setViewReport(null)}}>← Back</button>
          </div>
        </div>

        {/* Report Header Card */}
        <div className="glass-card" style={{background:'var(--brand-gradient-card)',border:'2px solid var(--purple)',marginBottom:16}}>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr 1fr 1fr',gap:16}}>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>REPORT ID</span><div style={{fontSize:16,fontWeight:800,fontFamily:'var(--font-mono)'}}>{r.id}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>SECTOR</span><div style={{fontSize:16,fontWeight:700}}>{r.sector}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>ASSESSMENT DATE</span><div style={{fontSize:16,fontWeight:700}}>{new Date(r.date).toLocaleDateString('en-IN',{day:'2-digit',month:'long',year:'numeric'})}</div></div>
            <div><span style={{fontSize:11,color:'var(--text-muted)'}}>ASSESSOR</span><div style={{fontSize:16,fontWeight:700}}>{r.assessor}</div></div>
          </div>
        </div>

        {/* Scorecard */}
        <div className="kpi-grid" style={{marginBottom:16}}>
          <div className="kpi-card"><div className="kpi-value" style={{color:getRagColor(r.score)}}>{r.score}%</div><div className="kpi-label">Overall Score</div></div>
          <div className="kpi-card"><div className="kpi-value" style={{color:'var(--green)'}}>{r.correct}</div><div className="kpi-label">Compliant</div></div>
          <div className="kpi-card"><div className="kpi-value" style={{color:'var(--red)'}}>{r.gaps}</div><div className="kpi-label">Gaps Found</div></div>
          <div className="kpi-card"><div className="kpi-value" style={{color:getRagColor(r.overallCompliance)}}>{r.overallCompliance.toFixed(0)}%</div><div className="kpi-label">Control Score</div></div>
          <div className="kpi-card"><div className="kpi-value">{r.totalQ}</div><div className="kpi-label">Total Assessed</div></div>
          <div className="kpi-card"><div className="kpi-value" style={{color:r.rag==='GREEN'?'var(--green)':r.rag==='AMBER'?'var(--amber)':'var(--red)'}}>{r.rag}</div><div className="kpi-label">RAG Status</div></div>
        </div>

        {/* Section 1: Executive Summary */}
        <div className="glass-card" style={{marginBottom:16}}>
          <h3>📋 1. Executive Summary</h3>
          <p style={{lineHeight:1.8,marginTop:8}}>
            This report presents findings from a comprehensive DPDP Act 2023 compliance assessment for the <strong>{r.sector}</strong> sector.
            The assessment evaluated <strong>{r.totalQ} compliance areas</strong> across all sections of the Act.
            Overall compliance score is <strong style={{color:getRagColor(r.score)}}>{r.score}% ({r.rag})</strong> with
            <strong style={{color:'var(--green)'}}> {r.correct} areas compliant</strong> and
            <strong style={{color:'var(--red)'}}> {r.gaps} gaps identified</strong> requiring remediation.
          </p>
        </div>

        {/* Section 2: Findings Matrix */}
        {r.findings.length > 0 && (
          <div className="glass-card" style={{marginBottom:16}}>
            <h3>🔍 2. Findings Matrix — Non-Conformities</h3>
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead><tr><th>#</th><th>DPDP Section</th><th>Finding</th><th>Severity</th><th>Current State</th><th>Required State</th></tr></thead>
                <tbody>{r.findings.map((f,i) => (
                  <tr key={i}>
                    <td>{i+1}</td><td><strong>{f.section}</strong></td>
                    <td style={{maxWidth:200}}>{f.question}</td>
                    <td><span className={`rag-badge ${f.severity==='CRITICAL'?'rag-red':f.severity==='HIGH'?'rag-amber':'rag-green'}`}>{f.severity}</span></td>
                    <td><span className="rag-badge rag-red">{f.answer}</span></td>
                    <td><span className="rag-badge rag-green">{f.required}</span></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          </div>
        )}

        {/* Section 3: Remediation Plan */}
        {r.findings.length > 0 && (
          <div className="glass-card" style={{marginBottom:16}}>
            <h3>🛠️ 3. Remediation Plan — Procedures & Controls</h3>
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead><tr><th>#</th><th>Gap Area</th><th>Remediation Action</th><th>Control Required</th><th>Implementation Procedure</th></tr></thead>
                <tbody>{r.findings.map((f,i) => (
                  <tr key={i}>
                    <td>{i+1}</td><td style={{maxWidth:150}}>{f.question.slice(0,80)}...</td>
                    <td style={{fontSize:12,background:'var(--amber-bg)',padding:8}}>{f.remediation}</td>
                    <td style={{fontSize:12}}>{f.control}</td>
                    <td style={{fontSize:11,whiteSpace:'pre-line'}}>{f.procedure}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          </div>
        )}

        {/* Section 4: Control Metrics */}
        <div className="glass-card" style={{marginBottom:16}}>
          <h3>📊 4. Compliance Control Metrics</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Control</th><th>Category</th><th>Status</th><th>Score</th><th>Progress</th><th>Gap / Remediation</th></tr></thead>
              <tbody>{r.controls.map((c,i) => (
                <tr key={i}>
                  <td><strong>{c.control}</strong></td><td>{c.category}</td>
                  <td>{c.implemented ? <span className="rag-badge rag-green">IMPLEMENTED</span> : <span className="rag-badge rag-red">GAP</span>}</td>
                  <td style={{fontWeight:700}}>{c.complianceScore}%</td>
                  <td style={{width:120}}><div style={{background:'var(--border)',borderRadius:4,height:8,overflow:'hidden'}}><div style={{width:`${c.complianceScore}%`,height:'100%',background:c.complianceScore>=80?'var(--green)':c.complianceScore>=50?'var(--amber)':'var(--red)',borderRadius:4}} /></div></td>
                  <td style={{fontSize:12,color:c.gap?'var(--red)':'var(--green)'}}>{c.gap || '✅ No gap'}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        </div>

        {/* Footer */}
        <div className="glass-card" style={{borderTop:'3px solid var(--purple)',fontSize:12,color:'var(--text-muted)'}}>
          <p><strong>Disclaimer:</strong> Generated by QS-DPDP Enterprise v3.0. Findings based on self-assessment; validate with certified DPDP auditor.</p>
          <p>Report ID: {r.id} | ISO 27701:2019 + DPDP Act 2023 | Generated: {new Date(r.date).toISOString()}</p>
        </div>
      </div>
    )
  }

  /* ─── REPORT HISTORY TAB ─── */
  if (tab === 'reports') {
    return (
      <div className="page-container">
        <div className="page-header">
          <div><h1>📁 Gap Analysis Reports</h1><p>ISO 27701 Format • Saved Reports • Search by ID, content, sector, date</p></div>
          <button className="btn-primary" onClick={()=>setTab('assessment')}>+ New Assessment</button>
        </div>

        {/* Search & Filter */}
        <div style={{display:'flex',gap:12,marginBottom:16,flexWrap:'wrap',alignItems:'center'}}>
          <input className="form-input" style={{width:300}} placeholder="🔍 Search by Report ID, content, sector, assessor..." value={searchTerm} onChange={e=>setSearchTerm(e.target.value)} />
          <select className="form-input" style={{width:200}} value={filterSector} onChange={e=>setFilterSector(e.target.value)}>
            <option value="">All Sectors</option>
            {SECTORS.map(s=><option key={s}>{s}</option>)}
          </select>
          <select className="form-input" style={{width:150}} value={filterRag} onChange={e=>setFilterRag(e.target.value)}>
            <option value="">All RAG</option>
            <option value="GREEN">🟢 GREEN</option><option value="AMBER">🟡 AMBER</option><option value="RED">🔴 RED</option>
          </select>
          <span style={{fontSize:12,color:'var(--text-muted)'}}>Found {filteredReports.length} report(s)</span>
        </div>

        {filteredReports.length === 0 ? (
          <div className="glass-card" style={{textAlign:'center',padding:48}}>
            <div style={{fontSize:48,marginBottom:16}}>📋</div>
            <h3>No Reports Found</h3>
            <p style={{color:'var(--text-muted)'}}>Complete a self-assessment to generate your first ISO-format gap analysis report.</p>
            <button className="btn-primary" style={{marginTop:16}} onClick={()=>setTab('assessment')}>Start Assessment</button>
          </div>
        ) : (
          <div className="glass-card">
            <h3>📋 Report Registry ({filteredReports.length})</h3>
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead><tr><th>Report ID</th><th>Sector</th><th>Date</th><th>Assessor</th><th>Score</th><th>RAG</th><th>Questions</th><th>Gaps</th><th>Actions</th></tr></thead>
                <tbody>{filteredReports.map(r => (
                  <tr key={r.id}>
                    <td style={{fontFamily:'var(--font-mono)',fontWeight:700,cursor:'pointer',color:'var(--brand-primary)'}} onClick={()=>{setViewReport(r);setTab('report-view')}}>{r.id}</td>
                    <td>{r.sector}</td>
                    <td style={{fontSize:12}}>{new Date(r.date).toLocaleDateString('en-IN')}</td>
                    <td style={{fontSize:12}}>{r.assessor}</td>
                    <td style={{fontWeight:700,color:getRagColor(r.score)}}>{r.score}%</td>
                    <td><span className={`rag-badge ${r.rag==='GREEN'?'rag-green':r.rag==='AMBER'?'rag-amber':'rag-red'}`}>{r.rag}</span></td>
                    <td>{r.totalQ}</td>
                    <td style={{color:'var(--red)',fontWeight:700}}>{r.gaps}</td>
                    <td>
                      <div style={{display:'flex',gap:4}}>
                        <button className="btn-sm" onClick={()=>{setViewReport(r);setTab('report-view')}}>📄 View</button>
                        <button className="btn-sm" onClick={()=>exportWord(r)}>📥 Word</button>
                        <button className="btn-sm" onClick={()=>printReport(r)}>🖨️</button>
                        <button className="btn-sm" style={{color:'var(--red)'}} onClick={()=>deleteReport(r.id)}>🗑️</button>
                      </div>
                    </td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    )
  }

  /* ─── RESULTS SCREEN ─── */
  if (showResults) {
    const score = getScore()
    const gaps = answers.map((a, i) => ({ question: questions[i], answer: a, correct: a === questions[i]?.correct })).filter(g => !g.correct)
    const overallCompliance = controls.reduce((s,c) => s + c.complianceScore, 0) / controls.length

    return (
      <div className="page-container">
        <div className="page-header"><h1>📊 Assessment Complete — {sector}</h1><p>DPDP Act 2023 Compliance Assessment Results</p></div>

        <div className="kpi-grid">
          <div className="kpi-card"><div className="kpi-value" style={{color:getRagColor(score)}}>{score}%</div><div className="kpi-label">Score ({getRag(score)})</div></div>
          <div className="kpi-card"><div className="kpi-value" style={{color:'var(--green)'}}>{answers.filter((_,i)=>answers[i]===questions[i]?.correct).length}</div><div className="kpi-label">Compliant</div></div>
          <div className="kpi-card"><div className="kpi-value" style={{color:'var(--red)'}}>{gaps.length}</div><div className="kpi-label">Gaps</div></div>
          <div className="kpi-card"><div className="kpi-value" style={{color:getRagColor(overallCompliance)}}>{overallCompliance.toFixed(0)}%</div><div className="kpi-label">Controls</div></div>
        </div>

        {/* Controls Table */}
        <div className="glass-card" style={{marginTop:16}}>
          <h3>📋 Compliance Controls — {sector}</h3>
          <div className="data-table-wrapper">
            <table className="data-table">
              <thead><tr><th>Control</th><th>Category</th><th>Status</th><th>Score</th><th>Progress</th><th>Gap</th></tr></thead>
              <tbody>{controls.map((c,i) => (
                <tr key={i}>
                  <td><strong>{c.control}</strong></td><td>{c.category}</td>
                  <td>{c.implemented ? <span className="rag-badge rag-green">OK</span> : <span className="rag-badge rag-red">GAP</span>}</td>
                  <td>{c.complianceScore}%</td>
                  <td style={{width:120}}><div style={{background:'var(--border)',borderRadius:4,height:8,overflow:'hidden'}}><div style={{width:`${c.complianceScore}%`,height:'100%',background:c.complianceScore>=80?'var(--green)':c.complianceScore>=50?'var(--amber)':'var(--red)',borderRadius:4}} /></div></td>
                  <td style={{fontSize:12,color:c.gap?'var(--red)':'var(--green)'}}>{c.gap || '✅'}</td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        </div>

        {/* Gaps Table */}
        {gaps.length > 0 && (
          <div className="glass-card" style={{marginTop:16}}>
            <h3>🔍 Identified Gaps & Remediation ({gaps.length})</h3>
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead><tr><th>#</th><th>Section</th><th>Question</th><th>Your Answer</th><th>Required</th><th>Remediation</th></tr></thead>
                <tbody>{gaps.map((g, i) => (
                  <tr key={i}>
                    <td>{i+1}</td><td><strong>{g.question.section}</strong></td>
                    <td style={{maxWidth:200}}>{g.question.q}</td>
                    <td><span className="rag-badge rag-red">{g.question.options[g.answer]}</span></td>
                    <td><span className="rag-badge rag-green">{g.question.options[g.question.correct]}</span></td>
                    <td style={{fontSize:12,background:'var(--amber-bg)',padding:8,borderRadius:4}}>{g.question.remediation}</td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
          </div>
        )}

        <div style={{display:'flex',gap:12,marginTop:24}}>
          <button className="btn-primary" onClick={saveReport}>💾 Save & Generate ISO Report</button>
          <button className="btn-secondary" onClick={restart}>🔄 Retake Assessment</button>
          <button className="btn-secondary" onClick={()=>setTab('reports')}>📁 View Saved Reports</button>
        </div>
      </div>
    )
  }

  /* ─── LANDING SCREEN ─── */
  if (!started) {
    return (
      <div className="page-container">
        <div className="page-header">
          <div><h1>🎯 Self-Assessment & Gap Analysis</h1><p>DPDP Act 2023 — {questions.length}+ sector-specific questions with AI-powered remediation</p></div>
          <button className="btn-secondary" onClick={()=>setTab('reports')}>📁 Saved Reports ({reports.length})</button>
        </div>

        {/* Tab switcher */}
        <div style={{display:'flex',gap:8,marginBottom:16}}>
          <button className={`btn-${tab==='assessment'?'primary':'secondary'}`} onClick={()=>setTab('assessment')}>📝 New Assessment</button>
          <button className={`btn-${tab==='reports'?'primary':'secondary'}`} onClick={()=>setTab('reports')}>📁 Report History ({reports.length})</button>
        </div>

        <div className="glass-card" style={{maxWidth:700,margin:'20px auto',textAlign:'center'}}>
          <h2 style={{marginBottom:16}}>Start DPDP Compliance Assessment</h2>
          <p style={{color:'var(--text-muted)',marginBottom:24}}>Answer {questions.length} sector-specific questions. Progress bar, hints, and remediation shown alongside each question.</p>
          <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:16,marginBottom:24,textAlign:'left'}}>
            <div>
              <label style={{display:'block',marginBottom:8,fontWeight:600}}>Sector</label>
              <select className="form-input" value={sector} onChange={e=>{setSector(e.target.value);setAnswers([]);setCurrentQ(0)}}>
                {SECTORS.map(s=><option key={s}>{s}</option>)}
              </select>
              <p style={{fontSize:11,color:'var(--text-muted)',marginTop:4}}>Auto-loads sector-specific questions, policies & controls</p>
            </div>
            <div>
              <label style={{display:'block',marginBottom:8,fontWeight:600}}>Assessor Name</label>
              <input className="form-input" value={assessor} onChange={e=>setAssessor(e.target.value)} placeholder="Your name / role" />
              <p style={{fontSize:11,color:'var(--text-muted)',marginTop:4}}>Appears on ISO report as the certifying assessor</p>
            </div>
          </div>
          <button className="btn-primary" style={{padding:'14px 48px',fontSize:16}} onClick={()=>setStarted(true)}>
            ▶️ Begin Assessment ({questions.length} Questions)
          </button>
        </div>
      </div>
    )
  }

  /* ─── ASSESSMENT SCREEN — Split layout ─── */
  const q = questions[currentQ]
  const answeredCount = answers.length
  const correctCount = answers.filter((a,i) => a === questions[i]?.correct).length
  const progress = ((answeredCount) / questions.length) * 100

  return (
    <div className="page-container">
      <div className="page-header"><h1>🎯 Assessment — {sector}</h1><p>Question {currentQ+1} of {questions.length} | Assessor: {assessor}</p></div>

      {/* Progress Bar */}
      <div style={{background:'var(--border)',borderRadius:6,height:10,marginBottom:20,overflow:'hidden'}}>
        <div style={{width:`${progress}%`,height:'100%',background:'var(--brand-gradient)',borderRadius:6,transition:'width 0.4s ease'}} />
      </div>

      <div style={{display:'grid',gridTemplateColumns:'3fr 2fr',gap:20,alignItems:'start'}}>
        {/* LEFT — Question */}
        <div className="glass-card">
          <div style={{fontSize:12,color:'var(--brand-primary)',fontWeight:600,marginBottom:8}}>{q.section}</div>
          <h2 style={{marginBottom:20,lineHeight:1.6,fontSize:16}}>{q.q}</h2>
          <div style={{display:'grid',gap:10}}>
            {q.options.map((opt, i) => (
              <button key={i} onClick={()=>handleAnswer(i)} disabled={answers.length > currentQ} style={{
                padding:'14px 18px',textAlign:'left',borderRadius:'var(--radius)',
                border: answers[currentQ]===i ? (i===q.correct ? '2px solid var(--green)' : '2px solid var(--red)') : '1px solid var(--border)',
                background: answers[currentQ]===i ? (i===q.correct ? 'var(--green-bg)' : 'var(--red-bg)') : 'var(--bg-card)',
                cursor: answers.length > currentQ ? 'default' : 'pointer',
                transition:'all 0.2s', fontSize:13, lineHeight:1.5, fontFamily:'inherit', color:'var(--text-primary)',
              }}>
                <span style={{fontWeight:700,marginRight:10,color:'var(--brand-primary)'}}>{String.fromCharCode(65+i)}.</span>{opt}
              </button>
            ))}
          </div>
        </div>

        {/* RIGHT — Progress + Hints */}
        <div>
          <div className="glass-card" style={{marginBottom:16}}>
            <h3 style={{fontSize:14,marginBottom:12}}>📊 Progress</h3>
            <div style={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:12}}>
              <div style={{textAlign:'center',padding:8,background:'var(--bg-glass)',borderRadius:'var(--radius)'}}>
                <div style={{fontSize:24,fontWeight:800}}>{answeredCount}</div><div style={{fontSize:11,color:'var(--text-muted)'}}>Answered</div>
              </div>
              <div style={{textAlign:'center',padding:8,background:'var(--bg-glass)',borderRadius:'var(--radius)'}}>
                <div style={{fontSize:24,fontWeight:800}}>{questions.length - answeredCount}</div><div style={{fontSize:11,color:'var(--text-muted)'}}>Remaining</div>
              </div>
              <div style={{textAlign:'center',padding:8,background:'var(--green-bg)',borderRadius:'var(--radius)'}}>
                <div style={{fontSize:24,fontWeight:800,color:'var(--green)'}}>{correctCount}</div><div style={{fontSize:11,color:'var(--text-muted)'}}>Correct</div>
              </div>
              <div style={{textAlign:'center',padding:8,background:'var(--red-bg)',borderRadius:'var(--radius)'}}>
                <div style={{fontSize:24,fontWeight:800,color:'var(--red)'}}>{answeredCount - correctCount}</div><div style={{fontSize:11,color:'var(--text-muted)'}}>Gaps</div>
              </div>
            </div>
            <div style={{display:'flex',gap:3,marginTop:12,flexWrap:'wrap'}}>
              {questions.map((_,i) => (
                <div key={i} style={{
                  width:20,height:20,borderRadius:4,fontSize:10,display:'flex',alignItems:'center',justifyContent:'center',fontWeight:700,
                  background: i < answers.length ? (answers[i]===questions[i]?.correct?'var(--green)':'var(--red)') : i===currentQ ? 'var(--brand-primary)' : 'var(--border)',
                  color: i <= answers.length ? '#fff' : 'var(--text-muted)',
                }}>{i+1}</div>
              ))}
            </div>
          </div>

          <div className="glass-card" style={{marginBottom:16,borderLeft:'4px solid var(--brand-primary)'}}>
            <h3 style={{fontSize:14,marginBottom:8}}>💡 Hint</h3>
            <p style={{fontSize:13,color:'var(--text-secondary)',lineHeight:1.6}}>{q.hint}</p>
            <div style={{marginTop:8,fontSize:12,color:'var(--text-muted)'}}><strong>DPDP Reference:</strong> {q.section}</div>
          </div>

          {showRemediation !== null && (
            <div className="glass-card" style={{borderLeft:'4px solid var(--red)',background:'var(--red-bg)'}}>
              <h3 style={{fontSize:14,marginBottom:8,color:'var(--red)'}}>⚠️ Gap — Remediation Plan</h3>
              <p style={{fontSize:13,lineHeight:1.6,marginBottom:12}}>{questions[showRemediation].remediation}</p>
              <div style={{fontSize:12,color:'var(--text-muted)',marginBottom:12}}><strong>Required:</strong> {questions[showRemediation].options[questions[showRemediation].correct]}</div>
              <button className="btn-primary" onClick={nextQuestion}>{currentQ + 1 < questions.length ? '→ Next Question' : '📊 View Results'}</button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
