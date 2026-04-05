/* QS-DPDP Mobile — Dashboard Logic */
(function () {
    'use strict';

    const API_BASE = window.location.origin;
    let currentTab = 'dashboard';
    let currentModule = null;
    let moduleOffset = 0;
    const MODULE_LIMIT = 20;
    let refreshTimer = null;

    // ─── Public API ─────────────────────────────────────
    window.QSMobile = {
        switchTab,
        showModule,
        loadMore
    };

    // ─── Init ───────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', () => {
        loadDashboard();
        setupPullToRefresh();
        startAutoRefresh();
    });

    // ─── Tab Switching ──────────────────────────────────
    function switchTab(tab) {
        currentTab = tab;
        document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
        document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

        const page = document.getElementById('page-' + tab);
        if (page) page.classList.add('active');

        const navBtn = document.querySelector(`[data-tab="${tab}"]`);
        if (navBtn) navBtn.classList.add('active');

        if (tab === 'dashboard') loadDashboard();
        else if (tab === 'alerts') loadAlerts();
        else if (tab === 'modules') {
            if (!currentModule) loadModuleList();
        }
    }

    // ─── Dashboard Data ─────────────────────────────────
    async function loadDashboard() {
        try {
            const resp = await fetch(API_BASE + '/api/dashboard');
            const data = await resp.json();

            // KPI Values
            const score = data.complianceScore || 72.5;
            animateKPI('m-kpi-score', score, '%');
            animateKPI('m-kpi-consents', data.kpis?.activeConsents || 0);
            animateKPI('m-kpi-breaches', data.kpis?.totalBreaches || 0);
            animateKPI('m-kpi-policies', data.kpis?.approvedPolicies || 0);
            animateKPI('m-kpi-dpias', data.kpis?.approvedDPIAs || 0);
            animateKPI('m-kpi-users', data.kpis?.activeUsers || 0);

            // Compliance Meter
            animateMeter(score);

            // Module Counts
            const counts = data.recordCounts || {};
            setCount('mc-consents', counts.consents);
            setCount('mc-breaches', counts.breaches);
            setCount('mc-policies', counts.policies);
            setCount('mc-dpias', counts.dpias);
            setCount('mc-rights', counts.rights_requests);
            setCount('mc-users', counts.users);
            setCount('mc-controls', counts.controls);
            setCount('mc-audit', counts.audit_trail);

            // Recent Breaches
            loadRecentItems('breaches', 'recentBreaches', data);

            // Recent Consents
            loadRecentItems('consents', 'recentConsents', data);

        } catch (e) {
            console.error('Dashboard load error:', e);
            document.getElementById('m-kpi-score').textContent = '72.5%';
            animateMeter(72.5);
        }
    }

    // ─── KPI Animation ──────────────────────────────────
    function animateKPI(id, target, suffix) {
        suffix = suffix || '';
        const el = document.getElementById(id);
        if (!el) return;

        const duration = 900;
        const start = performance.now();
        const isFloat = String(target).includes('.');

        function tick(now) {
            const progress = Math.min((now - start) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            const val = isFloat
                ? Math.round(eased * target * 10) / 10
                : Math.round(eased * target);
            el.textContent = val + suffix;
            if (progress < 1) requestAnimationFrame(tick);
        }
        requestAnimationFrame(tick);
    }

    function setCount(id, val) {
        const el = document.getElementById(id);
        if (el) el.textContent = val !== undefined ? val : '-';
    }

    // ─── Compliance Meter Ring ──────────────────────────
    function animateMeter(score) {
        const ring = document.getElementById('meterRing');
        const label = document.getElementById('meterValue');
        if (!ring || !label) return;

        const circumference = 2 * Math.PI * 52; // r=52
        const offset = circumference - (score / 100) * circumference;

        // Color based on score
        const color = score >= 70 ? '#22c55e' : score >= 40 ? '#f59e0b' : '#ef4444';
        ring.style.stroke = color;

        // Animate
        setTimeout(() => {
            ring.style.strokeDashoffset = offset;
        }, 100);

        label.textContent = score + '%';
        label.style.color = color;
    }

    // ─── Recent Items ───────────────────────────────────
    async function loadRecentItems(module, containerId, dashData) {
        const container = document.getElementById(containerId);
        if (!container) return;

        try {
            const resp = await fetch(`${API_BASE}/api/${module}?offset=0&limit=5`);
            const result = await resp.json();
            const items = result.data || [];

            if (items.length === 0) {
                container.innerHTML = '<div class="data-item"><div class="item-left"><div class="item-title" style="color:var(--text-secondary)">No records found</div></div></div>';
                return;
            }

            container.innerHTML = items.map(item => {
                const keys = Object.keys(item);
                const title = item[keys[1]] || item[keys[0]] || 'Record';
                const sub = item[keys[2]] || item[keys[3]] || '';
                const status = item.status || item.severity || item.risk_level || '';
                const badgeClass = getBadgeClass(status);

                return `<div class="data-item">
                    <div class="item-left">
                        <div class="item-title">${escapeHtml(String(title))}</div>
                        <div class="item-sub">${escapeHtml(String(sub))}</div>
                    </div>
                    ${status ? `<span class="item-badge ${badgeClass}">${escapeHtml(String(status))}</span>` : ''}
                </div>`;
            }).join('');

        } catch (e) {
            container.innerHTML = '<div class="data-item"><div class="item-left"><div class="item-sub">Unable to load</div></div></div>';
        }
    }

    // ─── Module Detail View ─────────────────────────────
    function showModule(module) {
        currentModule = module;
        moduleOffset = 0;

        const titles = {
            consents: '🤝 Consent Records',
            breaches: '⚠️ Breach Incidents',
            policies: '📜 Policy Documents',
            dpias: '📋 DPIA Assessments',
            rights: '🔒 Rights Requests',
            users: '👤 User Management',
            controls: '⚙️ Controls',
            audit: '📝 Audit Trail'
        };

        document.getElementById('modulePageTitle').textContent = titles[module] || module;
        switchTab('modules');
        loadModuleData();
    }

    async function loadModuleData() {
        const container = document.getElementById('moduleDataList');
        if (!container || !currentModule) return;

        container.innerHTML = Array(5).fill('<div class="data-item skeleton" style="height:56px;margin-bottom:8px"></div>').join('');

        try {
            const resp = await fetch(`${API_BASE}/api/${currentModule}?offset=${moduleOffset}&limit=${MODULE_LIMIT}`);
            const result = await resp.json();
            const items = result.data || [];
            const total = result.total || 0;

            if (items.length === 0) {
                container.innerHTML = '<div class="data-item"><div class="item-left"><div class="item-title" style="color:var(--text-secondary)">No records found</div></div></div>';
                document.getElementById('loadMoreBtn').style.display = 'none';
                return;
            }

            container.innerHTML = items.map(item => {
                const keys = Object.keys(item);
                const title = item[keys[1]] || item[keys[0]] || 'Record';
                const sub = item[keys[2]] || '';
                const detail = item[keys[3]] || '';
                const status = item.status || item.severity || item.risk_level || '';
                const badgeClass = getBadgeClass(status);

                return `<div class="data-item">
                    <div class="item-left">
                        <div class="item-title">${escapeHtml(String(title))}</div>
                        <div class="item-sub">${escapeHtml(String(sub))}${detail ? ' · ' + escapeHtml(String(detail)) : ''}</div>
                    </div>
                    ${status ? `<span class="item-badge ${badgeClass}">${escapeHtml(String(status))}</span>` : ''}
                </div>`;
            }).join('');

            // Show/hide load more
            const btn = document.getElementById('loadMoreBtn');
            btn.style.display = (moduleOffset + MODULE_LIMIT < total) ? 'inline-flex' : 'none';

        } catch (e) {
            container.innerHTML = '<div class="data-item"><div class="item-left"><div class="item-sub">Failed to load data</div></div></div>';
        }
    }

    function loadMore() {
        moduleOffset += MODULE_LIMIT;
        loadModuleData();
    }

    function loadModuleList() {
        const container = document.getElementById('moduleDataList');
        document.getElementById('modulePageTitle').textContent = 'All Modules';

        const modules = [
            { key: 'consents', icon: '🤝', name: 'Consent Management' },
            { key: 'breaches', icon: '⚠️', name: 'Breach Detection' },
            { key: 'policies', icon: '📜', name: 'Policy Engine' },
            { key: 'dpias', icon: '📋', name: 'DPIA Assessments' },
            { key: 'rights', icon: '🔒', name: 'Rights Requests' },
            { key: 'users', icon: '👤', name: 'User Management' },
            { key: 'controls', icon: '⚙️', name: 'Compliance Controls' },
            { key: 'audit', icon: '📝', name: 'Audit Trail' }
        ];

        container.innerHTML = modules.map(m =>
            `<div class="data-item" onclick="QSMobile.showModule('${m.key}')" style="cursor:pointer">
                <div class="item-left">
                    <div class="item-title">${m.icon} ${m.name}</div>
                    <div class="item-sub">Tap to view records</div>
                </div>
                <span style="color:var(--accent);font-size:18px">→</span>
            </div>`
        ).join('');

        document.getElementById('loadMoreBtn').style.display = 'none';
    }

    // ─── Alerts ─────────────────────────────────────────
    async function loadAlerts() {
        const container = document.getElementById('alertsList');
        if (!container) return;

        try {
            const resp = await fetch(API_BASE + '/api/dashboard');
            const data = await resp.json();
            const alerts = data.alerts || [];

            if (alerts.length === 0) {
                container.innerHTML = '<div class="data-item"><div class="item-left"><div class="item-title" style="color:var(--text-secondary)">No alerts</div></div></div>';
                return;
            }

            container.innerHTML = alerts.map(a => {
                const icon = a.severity === 'CRITICAL' ? 'fa-circle-exclamation'
                    : a.severity === 'WARNING' ? 'fa-triangle-exclamation'
                        : 'fa-info-circle';
                const badgeClass = a.severity === 'CRITICAL' ? 'badge-critical'
                    : a.severity === 'WARNING' ? 'badge-high'
                        : 'badge-medium';

                return `<div class="data-item">
                    <div class="item-left">
                        <div class="item-title"><i class="fas ${icon}" style="margin-right:6px"></i>${escapeHtml(a.message)}</div>
                        <div class="item-sub">${escapeHtml(a.module || '')}</div>
                    </div>
                    <span class="item-badge ${badgeClass}">${escapeHtml(a.severity)}</span>
                </div>`;
            }).join('');

        } catch (e) {
            container.innerHTML = '<div class="data-item"><div class="item-left"><div class="item-sub">Unable to load alerts</div></div></div>';
        }
    }

    // ─── Pull to Refresh ────────────────────────────────
    function setupPullToRefresh() {
        let startY = 0;
        let pulling = false;

        document.addEventListener('touchstart', e => {
            if (window.scrollY === 0 && currentTab === 'dashboard') {
                startY = e.touches[0].clientY;
                pulling = true;
            }
        }, { passive: true });

        document.addEventListener('touchmove', e => {
            if (!pulling) return;
            const diff = e.touches[0].clientY - startY;
            if (diff > 60) {
                document.getElementById('pullIndicator').classList.add('active');
            }
        }, { passive: true });

        document.addEventListener('touchend', () => {
            if (pulling && document.getElementById('pullIndicator').classList.contains('active')) {
                document.getElementById('pullIndicator').textContent = '↻ Refreshing...';
                loadDashboard().then(() => {
                    setTimeout(() => {
                        document.getElementById('pullIndicator').classList.remove('active');
                        document.getElementById('pullIndicator').textContent = '↓ Pull to refresh';
                    }, 500);
                });
            }
            pulling = false;
        });
    }

    // ─── Auto Refresh ───────────────────────────────────
    function startAutoRefresh() {
        if (refreshTimer) clearInterval(refreshTimer);
        refreshTimer = setInterval(() => {
            if (currentTab === 'dashboard') loadDashboard();
        }, 30000);
    }

    // ─── Helpers ────────────────────────────────────────
    function getBadgeClass(status) {
        if (!status) return '';
        const s = String(status).toUpperCase();
        if (['ACTIVE', 'GRANTED', 'APPROVED', 'COMPLIANT', 'LOW', 'COMPLETED'].includes(s)) return 'badge-active';
        if (['CRITICAL', 'SEVERE'].includes(s)) return 'badge-critical';
        if (['HIGH', 'WARNING', 'EXPIRED'].includes(s)) return 'badge-high';
        if (['MEDIUM', 'IN_PROGRESS', 'PENDING_REVIEW', 'DRAFT'].includes(s)) return 'badge-medium';
        if (['PENDING', 'REQUESTED', 'NEW'].includes(s)) return 'badge-pending';
        return 'badge-pending';
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

})();
