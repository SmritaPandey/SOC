import { Routes, Route, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useState, useEffect, useRef } from 'react'
import Dashboard from './pages/Dashboard'
import ConsentPage from './pages/ConsentPage'
import BreachPage from './pages/BreachPage'
import SIEMPage from './pages/SIEMPage'
import DLPPage from './pages/DLPPage'
import EDRPage from './pages/EDRPage'
import GovernancePage from './pages/GovernancePage'
import PolicyEnginePage from './pages/PolicyEnginePage'
import GapAnalysisPage from './pages/GapAnalysisPage'
import DPIAPage from './pages/DPIAPage'
import SettingsPage from './pages/SettingsPage'
import LicensingPage from './pages/LicensingPage'
import ReportsPage from './pages/ReportsPage'
import RightsPage from './pages/RightsPage'
import PaymentPage from './pages/PaymentPage'
import IAMPage from './pages/IAMPage'
import APIIntegrationPage from './pages/APIIntegrationPage'
import CertificationPage from './pages/CertificationPage'
import Chatbot from './components/Chatbot'
import { useAppContext } from './context/AppContext'
import { showExportDialog, exportToCSV, exportToPDF, exportToWord, exportToExcel } from './utils/exportUtils'

/* ─── SVG Product Icons ─── */
const icons = {
  dashboard: <svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>,
  consent: <svg viewBox="0 0 24 24"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>,
  breach: <svg viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>,
  rights: <svg viewBox="0 0 24 24"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>,
  siem: <svg viewBox="0 0 24 24"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>,
  dlp: <svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>,
  edr: <svg viewBox="0 0 24 24"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>,
  policy: <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>,
  dpia: <svg viewBox="0 0 24 24"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 14l2 2 4-4"/></svg>,
  gap: <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>,
  reports: <svg viewBox="0 0 24 24"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>,
  licensing: <svg viewBox="0 0 24 24"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>,
  payment: <svg viewBox="0 0 24 24"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>,
  settings: <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>,
  iam: <svg viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>,
  api: <svg viewBox="0 0 24 24"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><line x1="2" y1="10" x2="22" y2="10"/><line x1="6" y1="14" x2="6.01" y2="14"/><line x1="10" y1="14" x2="10.01" y2="14"/></svg>,
  cert: <svg viewBox="0 0 24 24"><path d="M12 15l-2 5 2-1 2 1-2-5z"/><circle cx="12" cy="8" r="6"/><path d="M9 12l2 2 4-4"/></svg>,
}

/* ─── Navigation Structure ─── */
const navGroups = [
  { section: 'HOME', items: [{ path: '/', icon: icons.dashboard, label: 'Dashboard' }] },
  { section: 'DATA PROTECTION', items: [
    { path: '/consent', icon: icons.consent, label: 'Consent' },
    { path: '/breach', icon: icons.breach, label: 'Breach' },
    { path: '/rights', icon: icons.rights, label: 'Rights' },
  ]},
  { section: 'SECURITY', items: [
    { path: '/siem', icon: icons.siem, label: 'SIEM / SOAR' },
    { path: '/dlp', icon: icons.dlp, label: 'DLP' },
    { path: '/edr', icon: icons.edr, label: 'EDR / XDR' },
  ]},
  { section: 'GOVERNANCE', items: [
    { path: '/policy', icon: icons.policy, label: 'Policy' },
    { path: '/dpia', icon: icons.dpia, label: 'DPIA' },
    { path: '/gap', icon: icons.gap, label: 'Assessment' },
    { path: '/certification', icon: icons.cert, label: 'Certification' },
  ]},
  { section: 'ACCESS MGMT', items: [
    { path: '/iam', icon: icons.iam, label: 'IAM / PAM' },
    { path: '/api-hub', icon: icons.api, label: 'API Hub' },
  ]},
  { section: 'ADMIN', items: [
    { path: '/reports', icon: icons.reports, label: 'Reports' },
    { path: '/licensing', icon: icons.licensing, label: 'Licensing' },
    { path: '/payment', icon: icons.payment, label: 'Payment' },
    { path: '/settings', icon: icons.settings, label: 'Settings' },
  ]},
]

/* ═══════════════════════════════════════════════════════════════
   MS Office-Style Ribbon Tab Definitions
   Each module has contextual sub-features displayed in ribbon groups
   ═══════════════════════════════════════════════════════════════ */

interface RibbonGroup { groupName: string; items: { label: string; icon?: string; action?: string; path?: string }[] }
interface RibbonTabDef { id: string; label: string; path: string; groups: RibbonGroup[] }

const ribbonTabs: RibbonTabDef[] = [
  { id:'dashboard', label:'Dashboard', path:'/', groups:[
    { groupName:'Overview', items:[
      {label:'Compliance Score',icon:'📊',path:'/',action:'scroll:rag-compliance'},{label:'KPI Drill-Down',icon:'🔍',path:'/',action:'scroll:module-compliance'},
      {label:'RAG Analysis',icon:'🚦',path:'/',action:'scroll:rag-compliance'},{label:'Activity Feed',icon:'📋',path:'/',action:'scroll:recent-activity'},
    ]},
    { groupName:'Quick Navigate', items:[
      {label:'Consent',icon:'✅',path:'/consent'},{label:'Breach',icon:'⚠️',path:'/breach'},
      {label:'Rights',icon:'👤',path:'/rights'},{label:'DLP',icon:'🛡️',path:'/dlp'},
      {label:'Assessment',icon:'📝',path:'/gap'},
    ]},
    { groupName:'Reports', items:[
      {label:'Export Report',icon:'📄',path:'/reports'},{label:'Run Assessment',icon:'🎯',path:'/gap'},
    ]},
  ]},
  { id:'consent', label:'Consent Mgmt', path:'/consent', groups:[
    { groupName:'Records', items:[
      {label:'All Records',icon:'📋',action:'tab:records'},{label:'Collect Consent',icon:'➕',action:'collect'},
      {label:'Bulk Actions',icon:'📦',action:'tab:records'},{label:'Search',icon:'🔍',action:'tab:records'},
    ]},
    { groupName:'Analysis', items:[
      {label:'Analytics',icon:'📊',action:'tab:analytics'},{label:'Purpose Chart',icon:'📈',action:'tab:analytics'},
      {label:'Trends',icon:'📉',action:'tab:analytics'},{label:'Type Distribution',icon:'🎯',action:'tab:analytics'},
    ]},
    { groupName:'Compliance', items:[
      {label:'Notices (§5)',icon:'📜',action:'tab:notices'},{label:'Guardian (§9)',icon:'👶',action:'tab:records'},
      {label:'Audit Trail',icon:'📝',action:'tab:audit'},{label:'Withdrawal (§7)',icon:'⛔',action:'tab:records'},
    ]},
    { groupName:'Export', items:[
      {label:'Word',icon:'📄',action:'export:word'},{label:'CSV',icon:'📊',action:'export:csv'},
      {label:'Print',icon:'🖨️',action:'export:print'},{label:'PDF',icon:'📑',action:'export:pdf'},
    ]},
  ]},
  { id:'breach', label:'Breach', path:'/breach', groups:[
    { groupName:'Incidents', items:[
      {label:'Active Breaches',icon:'🔴',path:'/breach'},{label:'New Incident',icon:'➕',path:'/breach'},
      {label:'Timeline',icon:'📅',path:'/breach'},{label:'Severity Map',icon:'🗺️',path:'/breach'},
    ]},
    { groupName:'Notifications', items:[
      {label:'DPBI (72h)',icon:'🏛️',path:'/breach'},{label:'CERT-IN (6h)',icon:'🔔',path:'/breach'},
      {label:'Notices (22 Lang)',icon:'🌍',path:'/breach'},{label:'Status Track',icon:'📋',path:'/breach'},
    ]},
    { groupName:'Reports', items:[
      {label:'Templates',icon:'📄',path:'/breach'},{label:'Export',icon:'📤',path:'/reports'},
    ]},
  ]},
  { id:'rights', label:'Rights', path:'/rights', groups:[
    { groupName:'Requests', items:[
      {label:'All Requests',icon:'📋',path:'/rights'},{label:'New Request',icon:'➕',path:'/rights'},
      {label:'Access (§11)',icon:'🔍',path:'/rights'},{label:'Correction (§12)',icon:'✏️',path:'/rights'},
    ]},
    { groupName:'Processing', items:[
      {label:'Erasure (§13)',icon:'🗑️',path:'/rights'},{label:'Grievance (§14)',icon:'📣',path:'/rights'},
      {label:'Nomination (§14A)',icon:'👥',path:'/rights'},{label:'SLA Monitor',icon:'⏱️',path:'/rights'},
    ]},
  ]},
  { id:'policy', label:'Policy', path:'/policy', groups:[
    { groupName:'Registry', items:[
      {label:'All Policies',icon:'📋',path:'/policy'},{label:'New Policy',icon:'➕',path:'/policy'},
      {label:'Templates',icon:'📑',path:'/policy'},{label:'Version History',icon:'🕐',path:'/policy'},
    ]},
    { groupName:'Lifecycle', items:[
      {label:'Draft → Review → Approve',icon:'🔄',path:'/policy'},{label:'Import/Export',icon:'📤',path:'/policy'},
    ]},
  ]},
  { id:'assessment', label:'Assessment', path:'/gap', groups:[
    { groupName:'Self-Assessment', items:[
      {label:'Start Assessment',icon:'🎯',path:'/gap'},{label:'Sector Questions',icon:'📝',path:'/gap'},
      {label:'Results',icon:'📊',path:'/gap'},{label:'Gap Analysis',icon:'🔍',path:'/gap'},
    ]},
    { groupName:'Remediation', items:[
      {label:'Action Plan',icon:'📋',path:'/gap'},{label:'Controls Map',icon:'🗺️',path:'/gap'},
      {label:'Saved Reports',icon:'💾',path:'/gap'},{label:'ISO Format',icon:'📄',path:'/gap'},
    ]},
  ]},
  { id:'dpia', label:'DPIA', path:'/dpia', groups:[
    { groupName:'Assessments', items:[
      {label:'All DPIAs',icon:'📋',path:'/dpia'},{label:'New DPIA',icon:'➕',path:'/dpia'},
      {label:'Risk Matrix',icon:'🔴',path:'/dpia'},{label:'Templates',icon:'📑',path:'/dpia'},
    ]},
    { groupName:'Reports', items:[
      {label:'Audit Log',icon:'📝',path:'/dpia'},{label:'Export',icon:'📤',path:'/reports'},
    ]},
  ]},
  { id:'dlp', label:'DLP', path:'/dlp', groups:[
    { groupName:'Protection', items:[
      {label:'Overview',icon:'🛡️',path:'/dlp'},{label:'Incidents',icon:'🔴',path:'/dlp'},
      {label:'Policies',icon:'📋',path:'/dlp'},{label:'Scanner',icon:'🔍',path:'/dlp'},
    ]},
    { groupName:'Channels (8)', items:[
      {label:'Endpoint',icon:'💻',path:'/dlp'},{label:'Network',icon:'🌐',path:'/dlp'},
      {label:'Email',icon:'📧',path:'/dlp'},{label:'Cloud',icon:'☁️',path:'/dlp'},
    ]},
    { groupName:'AI & Crypto', items:[
      {label:'AI Analytics',icon:'🤖',path:'/dlp'},{label:'NIST PQC',icon:'🔐',path:'/dlp'},
      {label:'Classification',icon:'🏷️',path:'/dlp'},
    ]},
  ]},
  { id:'siem', label:'SIEM / SOAR', path:'/siem', groups:[
    { groupName:'Events', items:[
      {label:'Live Feed',icon:'📡',path:'/siem'},{label:'Alerts',icon:'🔔',path:'/siem'},
      {label:'Correlation',icon:'🔗',path:'/siem'},{label:'MITRE ATT&CK',icon:'🎯',path:'/siem'},
    ]},
    { groupName:'SOAR', items:[
      {label:'Playbooks',icon:'📖',path:'/siem'},{label:'Automation',icon:'⚡',path:'/siem'},
      {label:'UEBA',icon:'👤',path:'/siem'},{label:'Threat Intel',icon:'🕵️',path:'/siem'},
    ]},
  ]},
  { id:'edr', label:'EDR / XDR', path:'/edr', groups:[
    { groupName:'Endpoint', items:[
      {label:'Agents',icon:'🖥️',path:'/edr'},{label:'Threats',icon:'🔴',path:'/edr'},
      {label:'Isolation',icon:'🔒',path:'/edr'},{label:'Forensics',icon:'🔬',path:'/edr'},
    ]},
    { groupName:'Management', items:[
      {label:'Updates',icon:'📥',path:'/edr'},{label:'Policies',icon:'📋',path:'/edr'},
    ]},
  ]},
  { id:'iam', label:'IAM / PAM', path:'/iam', groups:[
    { groupName:'Access', items:[
      {label:'RBAC',icon:'🔐',path:'/iam'},{label:'PBAC',icon:'📋',path:'/iam'},
      {label:'Users',icon:'👥',path:'/iam'},{label:'Roles',icon:'🏷️',path:'/iam'},
    ]},
    { groupName:'Audit', items:[
      {label:'Access Logs',icon:'📝',path:'/iam'},{label:'PAM Sessions',icon:'🖥️',path:'/iam'},
    ]},
  ]},
  { id:'reports', label:'Reports', path:'/reports', groups:[
    { groupName:'Generate', items:[
      {label:'Report Builder',icon:'🔧',path:'/reports'},{label:'Templates',icon:'📑',path:'/reports'},
      {label:'Schedule',icon:'📅',path:'/reports'},{label:'Saved Reports',icon:'💾',path:'/reports'},
    ]},
    { groupName:'Export', items:[
      {label:'Word',icon:'📄',action:'export:word'},{label:'PDF',icon:'📑',action:'export:pdf'},
      {label:'Excel',icon:'📊',action:'export:csv'},{label:'Print',icon:'🖨️',action:'export:print'},
    ]},
  ]},
  { id:'settings', label:'Settings', path:'/settings', groups:[
    { groupName:'Configuration', items:[
      {label:'Organization',icon:'🏢',path:'/settings'},{label:'Sector',icon:'🏭',path:'/settings'},
      {label:'Language',icon:'🌍',path:'/settings'},{label:'Database',icon:'🗄️',path:'/settings'},
    ]},
    { groupName:'System', items:[
      {label:'Hierarchy',icon:'📊',path:'/settings'},{label:'API Keys',icon:'🔑',path:'/settings'},
      {label:'Licensing',icon:'📜',path:'/licensing'},{label:'Payment',icon:'💳',path:'/payment'},
    ]},
  ]},
]

/* ═══ Menu bar definitions (File, Edit, View, Tools, Reports, Help) ═══ */
interface MenuItem { label: string; shortcut?: string; divider?: boolean; action?: string; path?: string }
interface MenuDef { label: string; items: MenuItem[] }

const menuDefs: MenuDef[] = [
  { label:'File', items:[
    {label:'New Consent Record',shortcut:'Ctrl+N',path:'/consent'},
    {label:'New Breach Incident',shortcut:'Ctrl+Shift+B',path:'/breach'},
    {label:'New Rights Request',path:'/rights'},
    {label:'New DPIA',path:'/dpia'},
    {label:'',divider:true},
    {label:'Export to Word',shortcut:'Ctrl+E',action:'export:word'},
    {label:'Export to CSV',action:'export:csv'},
    {label:'Export to PDF',action:'export:pdf'},
    {label:'',divider:true},
    {label:'Print',shortcut:'Ctrl+P',action:'print'},
    {label:'Print Preview',action:'print-preview'},
    {label:'',divider:true},
    {label:'Settings',path:'/settings'},
  ]},
  { label:'Edit', items:[
    {label:'Undo',shortcut:'Ctrl+Z',action:'undo'},
    {label:'Redo',shortcut:'Ctrl+Y',action:'redo'},
    {label:'',divider:true},
    {label:'Find Record',shortcut:'Ctrl+F',action:'find'},
    {label:'Find & Replace',shortcut:'Ctrl+H',action:'find-replace'},
    {label:'',divider:true},
    {label:'Select All',shortcut:'Ctrl+A',action:'select-all'},
    {label:'Bulk Withdraw Consent',action:'bulk-withdraw'},
  ]},
  { label:'View', items:[
    {label:'Dashboard',shortcut:'Ctrl+1',path:'/'},
    {label:'Consent Management',shortcut:'Ctrl+2',path:'/consent'},
    {label:'Breach Management',shortcut:'Ctrl+3',path:'/breach'},
    {label:'Rights Management',shortcut:'Ctrl+4',path:'/rights'},
    {label:'',divider:true},
    {label:'SIEM / SOAR',path:'/siem'},
    {label:'DLP Protection',path:'/dlp'},
    {label:'EDR / XDR',path:'/edr'},
    {label:'',divider:true},
    {label:'Toggle Dark/Light Mode',shortcut:'Ctrl+D',action:'toggle-theme'},
    {label:'Toggle Sidebar',action:'toggle-sidebar'},
    {label:'Full Screen',shortcut:'F11',action:'fullscreen'},
  ]},
  { label:'Tools', items:[
    {label:'Self-Assessment',path:'/gap'},
    {label:'DPIA Wizard',path:'/dpia'},
    {label:'PII Scanner (DLP)',path:'/dlp'},
    {label:'Policy Engine',path:'/policy'},
    {label:'',divider:true},
    {label:'API Hub',path:'/api-hub'},
    {label:'IAM / PAM',path:'/iam'},
    {label:'Certification',path:'/certification'},
    {label:'',divider:true},
    {label:'AI Chatbot',action:'chatbot'},
    {label:'Audit Trail',action:'audit'},
  ]},
  { label:'Reports', items:[
    {label:'Report Builder',path:'/reports'},
    {label:'Compliance Report',path:'/reports'},
    {label:'Gap Analysis Report',path:'/gap'},
    {label:'DPIA Report',path:'/dpia'},
    {label:'',divider:true},
    {label:'Scheduled Reports',path:'/reports'},
    {label:'Saved Reports',path:'/reports'},
    {label:'',divider:true},
    {label:'Sector Compliance Summary',path:'/reports'},
    {label:'DPBI Submission Report',path:'/breach'},
  ]},
  { label:'Help', items:[
    {label:'DPDP Act 2023 Overview',action:'help:dpdp'},
    {label:'User Guide',action:'help:guide'},
    {label:'Keyboard Shortcuts',action:'help:shortcuts'},
    {label:'',divider:true},
    {label:'About QS-DPDP Enterprise',action:'help:about'},
    {label:'Check for Updates',action:'help:updates'},
    {label:'License Information',path:'/licensing'},
  ]},
]

export default function App() {
  const location = useLocation()
  const navigate = useNavigate()
  const { sector } = useAppContext()
  const [theme, setTheme] = useState(() => localStorage.getItem('qs-theme') || 'dark')
  const [activeRibbonTab, setActiveRibbonTab] = useState(0)
  const [openMenu, setOpenMenu] = useState<string|null>(null)
  const [isMaximized, setIsMaximized] = useState(false)
  const [ribbonCollapsed, setRibbonCollapsed] = useState(false)
  const [sidebarVisible, setSidebarVisible] = useState(true)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('qs-theme', theme)
  }, [theme])

  // Auto-select correct ribbon tab when route changes
  useEffect(() => {
    const idx = ribbonTabs.findIndex(t => t.path === location.pathname)
    if (idx >= 0) setActiveRibbonTab(idx)
  }, [location.pathname])

  // Close menu on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setOpenMenu(null)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const toggleTheme = () => setTheme(t => t === 'dark' ? 'light' : 'dark')

  const handleMenuAction = (item: MenuItem) => {
    setOpenMenu(null)
    if (item.path) { navigate(item.path); return }
    if (item.action === 'toggle-theme') { toggleTheme(); return }
    if (item.action === 'toggle-sidebar') { setSidebarVisible(v => !v); return }
    if (item.action === 'print' || item.action === 'print-preview') { window.print(); return }
    if (item.action === 'fullscreen') { document.documentElement.requestFullscreen?.(); return }
    if (item.action === 'find') {
      const searchInput = document.querySelector('.search-input, input[placeholder*="Search"]') as HTMLInputElement
      if (searchInput) { searchInput.focus(); searchInput.select() }
      return
    }
    // Export actions — dispatch event for active page to handle
    if (item.action === 'export:word' || item.action === 'export:csv' || item.action === 'export:pdf' || item.action === 'export:excel') {
      window.dispatchEvent(new CustomEvent('qs-export', { detail: { format: item.action.split(':')[1] } }))
      return
    }
    if (item.action === 'undo' || item.action === 'redo') { document.execCommand(item.action); return }
    if (item.action === 'select-all') { document.execCommand('selectAll'); return }
    if (item.action === 'chatbot') {
      const chatBtn = document.querySelector('.chatbot-toggle') as HTMLButtonElement
      if (chatBtn) chatBtn.click()
      return
    }
    if (item.action === 'audit') { navigate('/consent?tab=audit'); return }
    if (item.action === 'bulk-withdraw') { navigate('/consent?tab=records'); return }
    if (item.action?.startsWith('help:')) {
      const helpTopic = item.action.split(':')[1]
      if (helpTopic === 'about') alert('QS-DPDP Enterprise v3.0\nQuantum-Safe Digital Personal Data Protection Platform\nDPDP Act 2023 Compliant\nPQC: ML-KEM-1024 + ML-DSA-87\n\n© 2024-2026 QS-DPDP Enterprise')
      else if (helpTopic === 'shortcuts') alert('Keyboard Shortcuts:\n\nCtrl+N → New Record\nCtrl+P → Print\nCtrl+F → Find\nCtrl+D → Toggle Dark/Light Mode\nCtrl+E → Export\nCtrl+1-4 → Navigate modules\nF11 → Full Screen')
      else if (helpTopic === 'dpdp') window.open('https://www.meity.gov.in/data-protection-framework', '_blank')
      return
    }
  }

  const handleRibbonTabClick = (tab: RibbonTabDef, idx: number) => {
    setActiveRibbonTab(idx)
    if (tab.path !== location.pathname) navigate(tab.path)
  }

  const handleRibbonItemClick = (item: { label: string; path?: string; action?: string }) => {
    if (item.path && item.path !== location.pathname) navigate(item.path)
    if (item.action) {
      // Tab switching — dispatch event for the page component to handle
      if (item.action.startsWith('tab:')) {
        window.dispatchEvent(new CustomEvent('qs-tab', { detail: { tab: item.action.split(':')[1] } }))
      }
      // Export actions
      if (item.action.startsWith('export:')) {
        const format = item.action.split(':')[1]
        if (format === 'print') { window.print(); return }
        window.dispatchEvent(new CustomEvent('qs-export', { detail: { format } }))
      }
      // Collect consent
      if (item.action === 'collect') {
        window.dispatchEvent(new CustomEvent('qs-action', { detail: { action: 'collect-consent' } }))
      }
      // Scroll-to-section
      if (item.action.startsWith('scroll:')) {
        const sectionId = item.action.split(':')[1]
        setTimeout(() => {
          const el = document.getElementById(sectionId) || document.querySelector(`[data-section="${sectionId}"]`)
          if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }, 100)
      }
    }
  }

  const handleMinimize = () => {
    // Web app: collapse ribbon as "minimize" behavior
    setRibbonCollapsed(true)
  }

  const handleMaximize = () => {
    setIsMaximized(!isMaximized)
    if (!isMaximized) {
      document.documentElement.requestFullscreen?.()
    } else {
      document.exitFullscreen?.()
    }
  }

  const handleClose = () => {
    if (confirm('Close QS-DPDP Enterprise?')) window.close()
  }

  const currentRibbonTab = ribbonTabs[activeRibbonTab]

  return (
    <div className={`app-layout ${isMaximized ? 'maximized' : ''}`}>
      {/* ═══ TITLE BAR — MS Office Style ═══ */}
      <div className="office-titlebar">
        <div className="titlebar-left">
          <div className="titlebar-icon">QS</div>
          <span className="titlebar-text">QS-DPDP Enterprise v3.0</span>
          <span className="titlebar-sector">— {sector}</span>
        </div>
        <div className="titlebar-center">
          <span className="titlebar-doctitle">
            {ribbonTabs.find(t => t.path === location.pathname)?.label || 'Dashboard'} — DPDP Act 2023
          </span>
        </div>
        <div className="titlebar-controls">
          <button className="titlebar-btn minimize" onClick={handleMinimize} title="Minimize Ribbon">
            <svg viewBox="0 0 12 12"><line x1="2" y1="6" x2="10" y2="6"/></svg>
          </button>
          <button className="titlebar-btn maximize" onClick={handleMaximize} title={isMaximized?'Restore Down':'Maximize'}>
            {isMaximized
              ? <svg viewBox="0 0 12 12"><rect x="1" y="3" width="8" height="8" fill="none" stroke="currentColor" strokeWidth="1"/><polyline points="3,3 3,1 11,1 11,9 9,9" fill="none" stroke="currentColor" strokeWidth="1"/></svg>
              : <svg viewBox="0 0 12 12"><rect x="2" y="2" width="8" height="8" fill="none" stroke="currentColor" strokeWidth="1"/></svg>
            }
          </button>
          <button className="titlebar-btn close" onClick={handleClose} title="Close">
            <svg viewBox="0 0 12 12"><line x1="2" y1="2" x2="10" y2="10"/><line x1="10" y1="2" x2="2" y2="10"/></svg>
          </button>
        </div>
      </div>

      {/* ═══ MENU BAR — File, Edit, View, Tools, Reports, Help ═══ */}
      <div className="office-menubar" ref={menuRef}>
        {menuDefs.map(menu => (
          <div key={menu.label} className="menu-item-wrapper">
            <button
              className={`menu-trigger ${openMenu === menu.label ? 'active' : ''}`}
              onClick={() => setOpenMenu(openMenu === menu.label ? null : menu.label)}
              onMouseEnter={() => { if (openMenu) setOpenMenu(menu.label) }}
            >
              {menu.label}
            </button>
            {openMenu === menu.label && (
              <div className="menu-dropdown">
                {menu.items.map((item, i) =>
                  item.divider
                    ? <div key={i} className="menu-divider" />
                    : <button key={i} className="menu-dropdown-item" onClick={() => handleMenuAction(item)}>
                        <span>{item.label}</span>
                        {item.shortcut && <span className="menu-shortcut">{item.shortcut}</span>}
                      </button>
                )}
              </div>
            )}
          </div>
        ))}
        <div style={{flex:1}} />
        <div className="menubar-quick-actions">
          <button className="quick-action-btn" onClick={() => navigate('/reports')} title="Export">📄</button>
          <button className="quick-action-btn" onClick={() => window.print()} title="Print">🖨️</button>
          <button className="quick-action-btn" title="Help — AI Assistant" onClick={()=>{const b=document.querySelector('.chatbot-toggle') as HTMLButtonElement;if(b)b.click()}}>❓</button>
        </div>
      </div>

      <div className="app-body">
        {/* ═══ SIDEBAR ═══ */}
        {sidebarVisible && (
        <aside className="sidebar">
          <div className="sidebar-brand">
            <div className="brand-icon">QS</div>
            <div className="brand-text">
              <h1>QS-DPDP</h1>
              <span>Enterprise v3.0</span>
            </div>
          </div>

          <nav style={{flex:1,overflowY:'auto',padding:'8px 0'}}>
            {navGroups.map((group, gi) => (
              <div key={gi}>
                <div className="sidebar-group">{group.section}</div>
                {group.items.map(item => (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) => `sidebar-item ${isActive ? 'active' : ''}`}
                    end={item.path === '/'}
                  >
                    {item.icon}
                    <span>{item.label}</span>
                  </NavLink>
                ))}
              </div>
            ))}
          </nav>

          <div className="theme-toggle-container">
            <button className="theme-toggle-btn" onClick={toggleTheme}>
              <span>{theme === 'dark' ? '🌙' : '☀️'}</span>
              <span>{theme === 'dark' ? 'Dark Mode' : 'Light Mode'}</span>
            </button>
          </div>

          <div style={{padding:'8px 16px',fontSize:10,color:'var(--text-muted)',borderTop:'1px solid var(--border)'}}>
            PQC: ML-KEM-1024 + ML-DSA-87
          </div>
        </aside>
        )}

        {/* ═══ MAIN AREA ═══ */}
        <main className="main-content">
          {/* ═══ RIBBON TABS — All modules displayed ═══ */}
          <div className="ribbon-toolbar">
            <div className="ribbon-tabs">
              {ribbonTabs.map((tab, i) => (
                <button
                  key={tab.id}
                  className={`ribbon-tab ${activeRibbonTab === i ? 'active' : ''}`}
                  onClick={() => handleRibbonTabClick(tab, i)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
            <div className="ribbon-collapse-toggle">
              <button
                className="ribbon-collapse-btn"
                onClick={() => setRibbonCollapsed(!ribbonCollapsed)}
                title={ribbonCollapsed ? 'Expand Ribbon' : 'Collapse Ribbon'}
              >
                {ribbonCollapsed ? '▼' : '▲'}
              </button>
            </div>
          </div>

          {/* ═══ RIBBON CONTENT — Contextual sub-features per active tab ═══ */}
          {!ribbonCollapsed && currentRibbonTab && (
            <div className="ribbon-content">
              {currentRibbonTab.groups.map((group, gi) => (
                <div key={gi} className="ribbon-group">
                  <div className="ribbon-group-items">
                    {group.items.map((item, ii) => (
                      <button
                        key={ii}
                        className="ribbon-item-btn"
                        onClick={() => handleRibbonItemClick(item)}
                        title={item.label}
                      >
                        <span className="ribbon-item-icon">{item.icon || '📋'}</span>
                        <span className="ribbon-item-label">{item.label}</span>
                      </button>
                    ))}
                  </div>
                  <div className="ribbon-group-label">{group.groupName}</div>
                </div>
              ))}
            </div>
          )}

          {/* Page Content */}
          <div className="content-area">
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/consent" element={<ConsentPage />} />
              <Route path="/breach" element={<BreachPage />} />
              <Route path="/rights" element={<RightsPage />} />
              <Route path="/siem" element={<SIEMPage />} />
              <Route path="/dlp" element={<DLPPage />} />
              <Route path="/edr" element={<EDRPage />} />
              <Route path="/governance" element={<GovernancePage />} />
              <Route path="/policy" element={<PolicyEnginePage />} />
              <Route path="/dpia" element={<DPIAPage />} />
              <Route path="/gap" element={<GapAnalysisPage />} />
              <Route path="/iam" element={<IAMPage />} />
              <Route path="/api-hub" element={<APIIntegrationPage />} />
              <Route path="/certification" element={<CertificationPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/licensing" element={<LicensingPage />} />
              <Route path="/payment" element={<PaymentPage />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Routes>
          </div>

          {/* Status Bar */}
          <div className="status-bar">
            <span><span className="status-dot" />System Online</span>
            <span>PQC: Active</span>
            <span>18 Modules Loaded</span>
            <span>Sector: {sector}</span>
            <span style={{marginLeft:'auto'}}>DPDP Act 2023 Compliant</span>
            <span>{new Date().toLocaleString('en-IN')}</span>
          </div>
        </main>
      </div>

      <Chatbot />
    </div>
  )
}
