/**
 * Data Principal Dashboard — Comprehensive privacy UI
 * 
 * Renders:
 * - Data footprint (what data is held, where, by whom)
 * - Consent status (active, expired, revoked)
 * - Privacy risk score
 * - Data usage timeline
 * - Consent grant/revoke UI
 * - Violation alerts
 * - GRC policy viewer
 * - RBI compliance dashboard
 * - Sector connector status
 * 
 * Integrates with: ConsentValidation, PIIDiscovery, RBI Compliance,
 * GRC Engine, Sync Service, and Breach modules.
 * 
 * @version 1.0.0
 * @since Phase 7 — Web App Enhancement
 */
const DataPrincipalDashboard = (function() {
    'use strict';

    // ═══════════════════════════════════════════════════════════
    // DATA PRINCIPAL FOOTPRINT
    // ═══════════════════════════════════════════════════════════

    function renderDataPrincipalView(container) {
        container.innerHTML = `
        <div style="padding:24px;">
            <h2 style="margin:0 0 8px;color:#e0e6f0;">🛡️ Data Principal Dashboard</h2>
            <p style="color:#8892a8;margin:0 0 24px;">Comprehensive view of your data, consents, and privacy risk.</p>
            
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px;margin-bottom:24px;">
                <div class="dp-stat-card" style="background:linear-gradient(135deg,rgba(56,189,248,0.15),rgba(56,189,248,0.05));border:1px solid rgba(56,189,248,0.3);border-radius:12px;padding:20px;">
                    <div style="font-size:32px;font-weight:700;color:#38bdf8;" id="dp-consent-count">—</div>
                    <div style="color:#8892a8;margin-top:4px;">Active Consents</div>
                </div>
                <div class="dp-stat-card" style="background:linear-gradient(135deg,rgba(52,211,153,0.15),rgba(52,211,153,0.05));border:1px solid rgba(52,211,153,0.3);border-radius:12px;padding:20px;">
                    <div style="font-size:32px;font-weight:700;color:#34d399;" id="dp-data-categories">—</div>
                    <div style="color:#8892a8;margin-top:4px;">Data Categories</div>
                </div>
                <div class="dp-stat-card" style="background:linear-gradient(135deg,rgba(251,146,60,0.15),rgba(251,146,60,0.05));border:1px solid rgba(251,146,60,0.3);border-radius:12px;padding:20px;">
                    <div style="font-size:32px;font-weight:700;color:#fb923c;" id="dp-risk-score">—</div>
                    <div style="color:#8892a8;margin-top:4px;">Privacy Risk Score</div>
                </div>
                <div class="dp-stat-card" style="background:linear-gradient(135deg,rgba(239,68,68,0.15),rgba(239,68,68,0.05));border:1px solid rgba(239,68,68,0.3);border-radius:12px;padding:20px;">
                    <div style="font-size:32px;font-weight:700;color:#ef4444;" id="dp-violations">—</div>
                    <div style="color:#8892a8;margin-top:4px;">Open Violations</div>
                </div>
            </div>

            <!-- Tab Navigation -->
            <div style="display:flex;gap:8px;margin-bottom:20px;flex-wrap:wrap;">
                <button onclick="DataPrincipalDashboard.showTab('consents')" class="dp-tab active" id="dp-tab-consents"
                    style="padding:10px 20px;border-radius:8px;border:1px solid rgba(56,189,248,0.3);background:rgba(56,189,248,0.2);color:#38bdf8;cursor:pointer;font-size:14px;">
                    📋 Consents</button>
                <button onclick="DataPrincipalDashboard.showTab('validation')" class="dp-tab" id="dp-tab-validation"
                    style="padding:10px 20px;border-radius:8px;border:1px solid rgba(255,255,255,0.1);background:rgba(255,255,255,0.05);color:#8892a8;cursor:pointer;font-size:14px;">
                    ✅ Validation</button>
                <button onclick="DataPrincipalDashboard.showTab('pii')" class="dp-tab" id="dp-tab-pii"
                    style="padding:10px 20px;border-radius:8px;border:1px solid rgba(255,255,255,0.1);background:rgba(255,255,255,0.05);color:#8892a8;cursor:pointer;font-size:14px;">
                    🔍 PII Discovery</button>
                <button onclick="DataPrincipalDashboard.showTab('rbi')" class="dp-tab" id="dp-tab-rbi"
                    style="padding:10px 20px;border-radius:8px;border:1px solid rgba(255,255,255,0.1);background:rgba(255,255,255,0.05);color:#8892a8;cursor:pointer;font-size:14px;">
                    🏛️ RBI Compliance</button>
                <button onclick="DataPrincipalDashboard.showTab('grc')" class="dp-tab" id="dp-tab-grc"
                    style="padding:10px 20px;border-radius:8px;border:1px solid rgba(255,255,255,0.1);background:rgba(255,255,255,0.05);color:#8892a8;cursor:pointer;font-size:14px;">
                    📊 GRC</button>
                <button onclick="DataPrincipalDashboard.showTab('connectors')" class="dp-tab" id="dp-tab-connectors"
                    style="padding:10px 20px;border-radius:8px;border:1px solid rgba(255,255,255,0.1);background:rgba(255,255,255,0.05);color:#8892a8;cursor:pointer;font-size:14px;">
                    🔗 Connectors</button>
                <button onclick="DataPrincipalDashboard.showTab('sync')" class="dp-tab" id="dp-tab-sync"
                    style="padding:10px 20px;border-radius:8px;border:1px solid rgba(255,255,255,0.1);background:rgba(255,255,255,0.05);color:#8892a8;cursor:pointer;font-size:14px;">
                    🔄 Sync</button>
            </div>

            <div id="dp-tab-content" style="background:rgba(255,255,255,0.03);border:1px solid rgba(255,255,255,0.08);border-radius:12px;padding:20px;min-height:300px;">
                <div style="color:#8892a8;text-align:center;padding:40px;">Loading...</div>
            </div>
        </div>`;

        loadSummaryStats();
        showTab('consents');
    }

    // ═══════════════════════════════════════════════════════════
    // STATS LOADER
    // ═══════════════════════════════════════════════════════════

    async function loadSummaryStats() {
        try {
            const [validationRes, syncRes] = await Promise.allSettled([
                fetch('/api/consent-validation/statistics').then(r => r.ok ? r.json() : {}),
                fetch('/api/sync/status').then(r => r.ok ? r.json() : {})
            ]);
            const vStats = validationRes.status === 'fulfilled' ? validationRes.value : {};
            const sStats = syncRes.status === 'fulfilled' ? syncRes.value : {};

            const el = id => document.getElementById(id);
            el('dp-consent-count') && (el('dp-consent-count').textContent = vStats.totalValidations || 0);
            el('dp-data-categories') && (el('dp-data-categories').textContent = Object.keys(vStats.resultBreakdown || {}).length || 0);
            el('dp-risk-score') && (el('dp-risk-score').textContent = calculateRiskScore(vStats));
            el('dp-violations') && (el('dp-violations').textContent =
                Object.values(vStats.violationsByStatus || {}).reduce((a,b) => a+b, 0) || 0);
        } catch (e) { console.warn('Stats load error:', e); }
    }

    function calculateRiskScore(stats) {
        const total = stats.totalValidations || 1;
        const violations = Object.values(stats.violationsByStatus || {}).reduce((a,b) => a+b, 0);
        const score = Math.max(0, 100 - Math.round((violations / total) * 100));
        return score + '/100';
    }

    // ═══════════════════════════════════════════════════════════
    // TAB SYSTEM
    // ═══════════════════════════════════════════════════════════

    function showTab(tabName) {
        // Update tab styles
        document.querySelectorAll('.dp-tab').forEach(tab => {
            tab.style.background = 'rgba(255,255,255,0.05)';
            tab.style.borderColor = 'rgba(255,255,255,0.1)';
            tab.style.color = '#8892a8';
        });
        const activeTab = document.getElementById('dp-tab-' + tabName);
        if (activeTab) {
            activeTab.style.background = 'rgba(56,189,248,0.2)';
            activeTab.style.borderColor = 'rgba(56,189,248,0.3)';
            activeTab.style.color = '#38bdf8';
        }

        const content = document.getElementById('dp-tab-content');
        if (!content) return;

        switch (tabName) {
            case 'consents': loadConsentsTab(content); break;
            case 'validation': loadValidationTab(content); break;
            case 'pii': loadPIITab(content); break;
            case 'rbi': loadRBITab(content); break;
            case 'grc': loadGRCTab(content); break;
            case 'connectors': loadConnectorsTab(content); break;
            case 'sync': loadSyncTab(content); break;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONSENTS TAB
    // ═══════════════════════════════════════════════════════════

    async function loadConsentsTab(container) {
        container.innerHTML = '<div style="color:#8892a8;padding:20px;">Loading consents...</div>';
        try {
            const res = await fetch('/api/consent/statistics');
            const stats = res.ok ? await res.json() : {};
            container.innerHTML = `
            <h3 style="color:#e0e6f0;margin:0 0 16px;">📋 Consent Management</h3>
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin-bottom:20px;">
                ${statCard('Total', stats.totalConsents || 0, '#38bdf8')}
                ${statCard('Active', stats.activeConsents || 0, '#34d399')}
                ${statCard('Revoked', stats.revokedConsents || 0, '#ef4444')}
                ${statCard('Expired', stats.expiredConsents || 0, '#fb923c')}
            </div>
            <div style="background:rgba(56,189,248,0.08);border-radius:8px;padding:16px;border:1px solid rgba(56,189,248,0.2);">
                <h4 style="color:#38bdf8;margin:0 0 8px;">Quick Actions</h4>
                <button onclick="if(typeof loadView==='function')loadView('consent')" style="padding:8px 16px;border-radius:6px;background:rgba(56,189,248,0.2);border:1px solid rgba(56,189,248,0.3);color:#38bdf8;cursor:pointer;margin-right:8px;">View All Consents</button>
                <button onclick="if(typeof loadView==='function')loadView('consent')" style="padding:8px 16px;border-radius:6px;background:rgba(52,211,153,0.2);border:1px solid rgba(52,211,153,0.3);color:#34d399;cursor:pointer;">Grant New Consent</button>
            </div>`;
        } catch (e) {
            container.innerHTML = `<div style="color:#fb923c;">Consent data unavailable: ${e.message}</div>`;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // VALIDATION TAB
    // ═══════════════════════════════════════════════════════════

    async function loadValidationTab(container) {
        container.innerHTML = '<div style="color:#8892a8;padding:20px;">Loading validation data...</div>';
        try {
            const [statsRes, violRes] = await Promise.all([
                fetch('/api/consent-validation/statistics').then(r => r.json()),
                fetch('/api/consent-validation/violations?status=OPEN&limit=10').then(r => r.json())
            ]);
            const violations = violRes.violations || [];
            container.innerHTML = `
            <h3 style="color:#e0e6f0;margin:0 0 16px;">✅ Consent Validation Engine</h3>
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin-bottom:20px;">
                ${statCard('Validations', statsRes.totalValidations || 0, '#38bdf8')}
                ${Object.entries(statsRes.resultBreakdown || {}).map(([k,v]) =>
                    statCard(k, v, k === 'VALID' ? '#34d399' : k === 'MISUSE' ? '#ef4444' : '#fb923c')).join('')}
            </div>
            <h4 style="color:#fb923c;margin:16px 0 8px;">⚠️ Open Violations (${violations.length})</h4>
            <div style="max-height:300px;overflow-y:auto;">
                ${violations.length === 0 ? '<div style="color:#34d399;padding:12px;">✅ No open violations</div>' :
                violations.map(v => `
                    <div style="background:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.2);border-radius:8px;padding:12px;margin-bottom:8px;">
                        <div style="display:flex;justify-content:space-between;">
                            <span style="color:#ef4444;font-weight:600;">${v.violationType}</span>
                            <span style="color:${v.severity==='CRITICAL'?'#ef4444':'#fb923c'};font-size:12px;">${v.severity}</span>
                        </div>
                        <div style="color:#8892a8;font-size:13px;margin-top:4px;">${v.details || ''}</div>
                        <div style="color:#64748b;font-size:12px;margin-top:4px;">Principal: ${v.principalId || 'N/A'} | ${v.detectedAt || ''}</div>
                    </div>`).join('')}
            </div>`;
        } catch (e) {
            container.innerHTML = `<div style="color:#fb923c;">Validation data unavailable</div>`;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PII DISCOVERY TAB
    // ═══════════════════════════════════════════════════════════

    async function loadPIITab(container) {
        container.innerHTML = '<div style="color:#8892a8;padding:20px;">Loading PII classification rules...</div>';
        try {
            const rules = await fetch('/api/pii-discovery/rules').then(r => r.json());
            const entries = Object.entries(rules);
            const critical = entries.filter(([,v]) => v.sensitivity === 'CRITICAL');
            const sensitive = entries.filter(([,v]) => v.sensitivity === 'SENSITIVE');
            const personal = entries.filter(([,v]) => v.sensitivity === 'PERSONAL');

            container.innerHTML = `
            <h3 style="color:#e0e6f0;margin:0 0 16px;">🔍 PII Classification Engine</h3>
            <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:20px;">
                ${statCard('Critical PII', critical.length, '#ef4444')}
                ${statCard('Sensitive PII', sensitive.length, '#fb923c')}
                ${statCard('Personal PII', personal.length, '#38bdf8')}
            </div>
            ${renderPIICategory('🔴 CRITICAL', critical, '#ef4444')}
            ${renderPIICategory('🟠 SENSITIVE', sensitive, '#fb923c')}
            ${renderPIICategory('🔵 PERSONAL', personal, '#38bdf8')}`;
        } catch (e) {
            container.innerHTML = `<div style="color:#fb923c;">PII data unavailable</div>`;
        }
    }

    function renderPIICategory(title, entries, color) {
        if (entries.length === 0) return '';
        return `
        <div style="margin-bottom:16px;">
            <h4 style="color:${color};margin:0 0 8px;">${title}</h4>
            <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:8px;">
                ${entries.map(([type, rule]) => `
                    <div style="background:rgba(255,255,255,0.03);border:1px solid ${color}33;border-radius:8px;padding:10px;">
                        <div style="color:${color};font-weight:600;font-size:13px;">${type}</div>
                        <div style="color:#8892a8;font-size:12px;margin-top:2px;">${rule.description}</div>
                        <div style="color:#64748b;font-size:11px;margin-top:4px;">Legal: ${rule.legalBasis} | Retain: ${rule.retentionDays}d</div>
                    </div>`).join('')}
            </div>
        </div>`;
    }

    // ═══════════════════════════════════════════════════════════
    // RBI COMPLIANCE TAB
    // ═══════════════════════════════════════════════════════════

    async function loadRBITab(container) {
        container.innerHTML = '<div style="color:#8892a8;padding:20px;">Loading RBI compliance...</div>';
        try {
            const [scoreRes, domainsRes] = await Promise.all([
                fetch('/api/rbi-compliance/score').then(r => r.json()),
                fetch('/api/rbi-compliance/domains').then(r => r.json())
            ]);
            const domains = domainsRes.domains || [];
            container.innerHTML = `
            <h3 style="color:#e0e6f0;margin:0 0 16px;">🏛️ RBI Advisory 3/2026 Compliance</h3>
            <div style="display:flex;align-items:center;gap:20px;margin-bottom:20px;">
                <div style="width:100px;height:100px;border-radius:50%;display:flex;align-items:center;justify-content:center;
                    background:conic-gradient(${getScoreColor(scoreRes.overallScore)} ${scoreRes.overallScore * 3.6}deg,rgba(255,255,255,0.05) 0);
                    font-size:28px;font-weight:700;color:#e0e6f0;">${scoreRes.overallScore}%</div>
                <div>
                    <div style="color:#e0e6f0;font-size:18px;font-weight:600;">Overall Compliance</div>
                    <div style="color:#8892a8;">${scoreRes.domainCount} domains • ${scoreRes.totalControls} controls</div>
                </div>
            </div>
            <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:12px;">
                ${domains.map(d => `
                    <div style="background:rgba(255,255,255,0.03);border:1px solid rgba(255,255,255,0.08);border-radius:10px;padding:14px;">
                        <div style="display:flex;justify-content:space-between;align-items:center;">
                            <span style="color:#e0e6f0;font-weight:600;">${d.name}</span>
                            <span style="color:#38bdf8;font-size:12px;">${d.controlCount} controls</span>
                        </div>
                        <div style="color:#8892a8;font-size:12px;margin-top:4px;">${d.description}</div>
                        <div style="background:rgba(255,255,255,0.05);border-radius:4px;height:6px;margin-top:8px;">
                            <div style="height:100%;border-radius:4px;background:${getScoreColor(0)};width:0%;transition:width 0.5s;"></div>
                        </div>
                    </div>`).join('')}
            </div>`;
        } catch (e) {
            container.innerHTML = `<div style="color:#fb923c;">RBI compliance data unavailable</div>`;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GRC TAB
    // ═══════════════════════════════════════════════════════════

    async function loadGRCTab(container) {
        container.innerHTML = '<div style="color:#8892a8;padding:20px;">Loading GRC data...</div>';
        try {
            const [overviewRes, templatesRes, controlsRes] = await Promise.all([
                fetch('/api/grc/overview').then(r => r.json()),
                fetch('/api/grc/templates').then(r => r.json()),
                fetch('/api/grc/controls').then(r => r.json())
            ]);
            const templates = templatesRes.templates || [];
            const controls = controlsRes.controls || [];
            container.innerHTML = `
            <h3 style="color:#e0e6f0;margin:0 0 16px;">📊 GRC Engine</h3>
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px;margin-bottom:20px;">
                ${statCard('Templates', templates.length, '#a78bfa')}
                ${statCard('Controls', controls.length, '#38bdf8')}
                ${statCard('Policies', overviewRes.policies || 0, '#34d399')}
                ${statCard('Risks', overviewRes.risks || 0, '#fb923c')}
            </div>
            <h4 style="color:#a78bfa;margin:16px 0 8px;">Policy Templates</h4>
            <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:8px;margin-bottom:16px;">
                ${templates.map(t => `
                    <div style="background:rgba(167,139,250,0.08);border:1px solid rgba(167,139,250,0.2);border-radius:8px;padding:10px;">
                        <div style="color:#a78bfa;font-weight:600;font-size:13px;">${t.name}</div>
                        <div style="color:#8892a8;font-size:12px;">${t.sector} • ${t.id}</div>
                    </div>`).join('')}
            </div>
            <h4 style="color:#38bdf8;margin:16px 0 8px;">Controls (${controls.length})</h4>
            <table style="width:100%;border-collapse:collapse;font-size:13px;">
                <tr style="border-bottom:1px solid rgba(255,255,255,0.1);">
                    <th style="text-align:left;padding:8px;color:#8892a8;">ID</th>
                    <th style="text-align:left;padding:8px;color:#8892a8;">Control</th>
                    <th style="text-align:left;padding:8px;color:#8892a8;">Category</th>
                    <th style="text-align:left;padding:8px;color:#8892a8;">Priority</th>
                </tr>
                ${controls.map(c => `
                    <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
                        <td style="padding:6px 8px;color:#64748b;">${c.id}</td>
                        <td style="padding:6px 8px;color:#e0e6f0;">${c.name}</td>
                        <td style="padding:6px 8px;color:#8892a8;">${c.category}</td>
                        <td style="padding:6px 8px;color:${c.priority==='CRITICAL'?'#ef4444':c.priority==='HIGH'?'#fb923c':'#34d399'};">${c.priority}</td>
                    </tr>`).join('')}
            </table>`;
        } catch (e) {
            container.innerHTML = `<div style="color:#fb923c;">GRC data unavailable</div>`;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // CONNECTORS TAB
    // ═══════════════════════════════════════════════════════════

    async function loadConnectorsTab(container) {
        container.innerHTML = '<div style="color:#8892a8;padding:20px;">Loading connectors...</div>';
        try {
            const data = await fetch('/api/connectors').then(r => r.json());
            const adapters = data.adapters || [];
            const sectors = {};
            adapters.forEach(a => {
                if (!sectors[a.sector]) sectors[a.sector] = [];
                sectors[a.sector].push(a);
            });

            container.innerHTML = `
            <h3 style="color:#e0e6f0;margin:0 0 16px;">🔗 Sector Connectors (${adapters.length})</h3>
            ${Object.entries(sectors).map(([sector, items]) => `
                <div style="margin-bottom:16px;">
                    <h4 style="color:#38bdf8;margin:0 0 8px;">${sector} (${items.length})</h4>
                    <div style="display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:8px;">
                        ${items.map(a => `
                            <div style="background:rgba(255,255,255,0.03);border:1px solid ${a.enabled?'rgba(52,211,153,0.3)':'rgba(255,255,255,0.08)'};border-radius:8px;padding:10px;">
                                <div style="display:flex;justify-content:space-between;">
                                    <span style="color:#e0e6f0;font-weight:600;font-size:13px;">${a.name}</span>
                                    <span style="font-size:11px;padding:2px 8px;border-radius:4px;
                                        background:${a.enabled?'rgba(52,211,153,0.2)':'rgba(255,255,255,0.05)'};
                                        color:${a.enabled?'#34d399':'#64748b'};">${a.status}</span>
                                </div>
                                <div style="color:#8892a8;font-size:12px;margin-top:4px;">${a.description}</div>
                            </div>`).join('')}
                    </div>
                </div>`).join('')}`;
        } catch (e) {
            container.innerHTML = `<div style="color:#fb923c;">Connector data unavailable</div>`;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SYNC TAB
    // ═══════════════════════════════════════════════════════════

    async function loadSyncTab(container) {
        container.innerHTML = '<div style="color:#8892a8;padding:20px;">Loading sync status...</div>';
        try {
            const status = await fetch('/api/sync/status').then(r => r.json());
            const wsConnected = typeof QSSyncClient !== 'undefined' && QSSyncClient.isConnected();
            container.innerHTML = `
            <h3 style="color:#e0e6f0;margin:0 0 16px;">🔄 Real-time Sync Status</h3>
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px;margin-bottom:20px;">
                ${statCard('Sequence', status.currentSequence || 0, '#38bdf8')}
                ${statCard('Clients', status.registeredClients || 0, '#a78bfa')}
                ${statCard('WebSocket', status.websocketAvailable ? 'Active' : 'Off', status.websocketAvailable ? '#34d399' : '#ef4444')}
                ${statCard('EventBus', status.eventBusConnected ? 'Connected' : 'Off', status.eventBusConnected ? '#34d399' : '#ef4444')}
            </div>
            <div style="background:rgba(255,255,255,0.03);border:1px solid rgba(255,255,255,0.08);border-radius:8px;padding:16px;">
                <h4 style="color:#e0e6f0;margin:0 0 8px;">Client Status</h4>
                <div style="color:#8892a8;">Browser WebSocket: <span style="color:${wsConnected?'#34d399':'#fb923c'};">${wsConnected ? '🟢 Connected' : '🟠 Not connected'}</span></div>
                <div style="color:#8892a8;margin-top:4px;">Topics: ${(status.topics || []).join(', ')}</div>
                ${!wsConnected ? `<button onclick="if(typeof QSSyncClient!=='undefined')QSSyncClient.connect()" 
                    style="margin-top:12px;padding:8px 16px;border-radius:6px;background:rgba(52,211,153,0.2);border:1px solid rgba(52,211,153,0.3);color:#34d399;cursor:pointer;">
                    Connect WebSocket</button>` : ''}
            </div>`;
        } catch (e) {
            container.innerHTML = `<div style="color:#fb923c;">Sync status unavailable</div>`;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    function statCard(label, value, color) {
        return `<div style="background:rgba(255,255,255,0.03);border:1px solid ${color}33;border-radius:8px;padding:12px;text-align:center;">
            <div style="font-size:24px;font-weight:700;color:${color};">${value}</div>
            <div style="color:#8892a8;font-size:12px;margin-top:2px;">${label}</div>
        </div>`;
    }

    function getScoreColor(score) {
        if (score >= 80) return '#34d399';
        if (score >= 60) return '#38bdf8';
        if (score >= 40) return '#fb923c';
        return '#ef4444';
    }

    // ═══════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════

    return {
        render: renderDataPrincipalView,
        showTab
    };
})();
