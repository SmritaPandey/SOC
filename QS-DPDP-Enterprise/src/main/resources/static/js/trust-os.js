/**
 * Trust OS Frontend Modules — Universal Trust Operating System v3.0
 * Renders interactive dashboards for all 7 Trust OS modules.
 * All data fetched live from backend REST APIs.
 *
 * @version 1.0.0
 * @since 2026-03-25
 */

// ═══════════════════════════════════════════════════════════
// MASTER LOADER
// ═══════════════════════════════════════════════════════════

window.loadTrustOSModule = function(moduleId) {
    // Update sidebar active state
    document.querySelectorAll('.tool-sidebar-item').forEach(t => t.classList.remove('active'));
    var toolEl = document.getElementById('tool-' + moduleId.replace('credit-score','credit').replace('ai-governance','ai-gov'));
    if (toolEl) toolEl.classList.add('active');

    // Get main content area
    var mainContent = document.getElementById('main-content') || document.querySelector('.main-content');
    if (!mainContent) {
        mainContent = document.querySelector('.content-area') || document.querySelector('.ribbon-content');
    }
    if (!mainContent) return;

    // Show loading
    mainContent.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:60vh;"><div style="text-align:center"><div class="spinner" style="width:48px;height:48px;border:4px solid #e2e8f0;border-top-color:#6366f1;border-radius:50%;animation:spin 1s linear infinite;margin:0 auto 16px"></div><p style="color:#64748b;font-size:14px">Loading module...</p></div></div>';

    // Route to module renderer
    switch(moduleId) {
        case 'ledger': renderLedgerModule(mainContent); break;
        case 'credit-score': renderCreditScoreModule(mainContent); break;
        case 'ndce': renderNDCEModule(mainContent); break;
        case 'pet': renderPETModule(mainContent); break;
        case 'ai-governance': renderAIGovernanceModule(mainContent); break;
        case 'wallet': renderWalletModule(mainContent); break;
        case 'interop': renderInteropModule(mainContent); break;
        default: mainContent.innerHTML = '<p>Unknown module: ' + moduleId + '</p>';
    }
};

// ═══════════════════════════════════════════════════════════
// SHARED UTILITIES
// ═══════════════════════════════════════════════════════════

function trustCard(title, icon, color, content) {
    return '<div style="background:#fff;border-radius:16px;padding:24px;box-shadow:0 2px 12px rgba(0,0,0,0.06);border:1px solid #f1f5f9;margin-bottom:20px">' +
        '<div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">' +
        '<div style="width:40px;height:40px;border-radius:12px;background:' + color + '15;display:flex;align-items:center;justify-content:center">' +
        '<i class="fas ' + icon + '" style="color:' + color + ';font-size:18px"></i></div>' +
        '<h3 style="margin:0;font-size:16px;font-weight:700;color:#1e293b">' + title + '</h3></div>' +
        content + '</div>';
}

function statBox(label, value, color, icon) {
    return '<div style="background:linear-gradient(135deg,' + color + '08,' + color + '15);border:1px solid ' + color + '25;border-radius:12px;padding:16px;text-align:center;min-width:140px">' +
        '<div style="font-size:28px;font-weight:800;color:' + color + '">' + value + '</div>' +
        '<div style="font-size:12px;color:#64748b;margin-top:4px">' + label + '</div></div>';
}

function riskBadge(level) {
    var colors = { LOW: '#22c55e', MEDIUM: '#f59e0b', HIGH: '#ef4444', INTACT: '#22c55e', VERIFIED: '#22c55e' };
    var c = colors[level] || '#64748b';
    return '<span style="background:' + c + '18;color:' + c + ';padding:4px 12px;border-radius:20px;font-size:12px;font-weight:600">' + level + '</span>';
}

function moduleHeader(title, subtitle, icon, gradient) {
    return '<div style="background:linear-gradient(135deg,' + gradient + ');border-radius:20px;padding:32px;margin-bottom:24px;position:relative;overflow:hidden">' +
        '<div style="position:absolute;top:-20px;right:-20px;width:120px;height:120px;border-radius:50%;background:rgba(255,255,255,0.1)"></div>' +
        '<div style="position:relative;z-index:1">' +
        '<div style="display:flex;align-items:center;gap:16px;margin-bottom:8px">' +
        '<i class="fas ' + icon + '" style="font-size:32px;color:rgba(255,255,255,0.9)"></i>' +
        '<div><h2 style="margin:0;color:#fff;font-size:24px;font-weight:800">' + title + '</h2>' +
        '<p style="margin:4px 0 0;color:rgba(255,255,255,0.8);font-size:14px">' + subtitle + '</p></div></div></div></div>';
}

async function fetchJSON(url) {
    try {
        var r = await fetch(url);
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return await r.json();
    } catch(e) {
        console.error('Trust OS fetch error:', url, e);
        return null;
    }
}

// ═══════════════════════════════════════════════════════════
// 1. CONSENT LEDGER NETWORK (CLN)
// ═══════════════════════════════════════════════════════════

async function renderLedgerModule(container) {
    var data = await fetchJSON('/api/ledger/chain-status');
    if (!data) { container.innerHTML = '<p style="color:#ef4444;padding:24px">Failed to load Ledger data</p>'; return; }

    var html = moduleHeader('Consent Ledger Network', 'Tamper-proof hash-chained consent ledger with Merkle audit proofs', 'fa-link', '#6366f1,#8b5cf6');

    // Stats row
    html += '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:24px">';
    html += statBox('Total Blocks', data.totalBlocks || 0, '#6366f1', 'fa-cubes');
    html += statBox('Chain Status', data.status || 'N/A', data.status === 'INTACT' ? '#22c55e' : '#ef4444', 'fa-link');
    html += statBox('Integrity', (data.integrityPercentage || 0).toFixed(1) + '%', '#22c55e', 'fa-check-circle');
    html += statBox('Consent Records', data.consentRecords || 0, '#0ea5e9', 'fa-file-signature');
    html += statBox('Fiduciaries', data.fiduciaries || 0, '#f59e0b', 'fa-building');
    html += '</div>';

    // Chain info card
    html += trustCard('Chain Information', 'fa-info-circle', '#6366f1',
        '<table style="width:100%;border-collapse:collapse">' +
        '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:10px;color:#64748b;width:200px">Hash Algorithm</td><td style="padding:10px;font-weight:600">' + (data.hashAlgorithm || 'SHA3-256') + '</td></tr>' +
        '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:10px;color:#64748b">Signature Algorithm</td><td style="padding:10px;font-weight:600">' + (data.signatureAlgorithm || 'ML-DSA-87') + '</td></tr>' +
        '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:10px;color:#64748b">Merkle Root</td><td style="padding:10px;font-family:monospace;font-size:12px;word-break:break-all">' + (data.merkleRoot || '').substring(0, 48) + '...</td></tr>' +
        '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:10px;color:#64748b">Merkle Tree Depth</td><td style="padding:10px;font-weight:600">' + (data.merkleTreeDepth || 0) + '</td></tr>' +
        '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:10px;color:#64748b">Offline Verification</td><td style="padding:10px">' + riskBadge('SUPPORTED') + '</td></tr>' +
        '<tr><td style="padding:10px;color:#64748b">Genesis Timestamp</td><td style="padding:10px;font-size:13px">' + (data.genesisTimestamp || '') + '</td></tr>' +
        '</table>');

    // Compliance card
    var complianceItems = (data.compliance || []).map(function(c) {
        return '<div style="display:inline-block;background:#eff6ff;color:#2563eb;padding:6px 14px;border-radius:8px;font-size:12px;font-weight:500;margin:4px">' + c + '</div>';
    }).join('');
    html += trustCard('Compliance Standards', 'fa-certificate', '#22c55e', '<div style="display:flex;flex-wrap:wrap;gap:4px">' + complianceItems + '</div>');

    // Add consent form
    html += trustCard('Add Consent to Ledger', 'fa-plus-circle', '#0ea5e9',
        '<div id="ledger-add-form">' +
        '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px">' +
        '<input id="ledger-consentId" placeholder="Consent ID (e.g. C-100)" style="padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px">' +
        '<input id="ledger-dp" placeholder="Data Principal Email" style="padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px">' +
        '<input id="ledger-fiduciary" placeholder="Fiduciary Org ID" style="padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px">' +
        '<input id="ledger-purpose" placeholder="Purpose" style="padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px">' +
        '</div>' +
        '<button onclick="addConsentToLedger()" style="background:linear-gradient(135deg,#6366f1,#8b5cf6);color:#fff;border:none;padding:12px 24px;border-radius:10px;font-weight:600;cursor:pointer;font-size:14px"><i class="fas fa-plus" style="margin-right:8px"></i>Add to Ledger</button>' +
        '<div id="ledger-result" style="margin-top:16px"></div></div>');

    container.innerHTML = html;
}

window.addConsentToLedger = async function() {
    var body = {
        consentId: document.getElementById('ledger-consentId').value || 'C-' + Date.now(),
        dataPrincipal: document.getElementById('ledger-dp').value || 'user@example.com',
        fiduciary: document.getElementById('ledger-fiduciary').value || 'ORG-001',
        purpose: document.getElementById('ledger-purpose').value || 'data-processing',
        action: 'GRANT'
    };
    var r = await fetch('/api/ledger/add-consent', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    var d = await r.json();
    var el = document.getElementById('ledger-result');
    if (el) el.innerHTML = '<div style="background:#f0fdf4;border:1px solid #22c55e;border-radius:10px;padding:16px;margin-top:12px"><i class="fas fa-check-circle" style="color:#22c55e;margin-right:8px"></i><strong>Block #' + (d.block ? d.block.blockIndex : '?') + '</strong> added to ledger — Hash: <code style="font-size:11px">' + (d.block ? d.block.blockHash : '').substring(0, 32) + '...</code></div>';
};

// ═══════════════════════════════════════════════════════════
// 2. CONSENT CREDIT SCORE (CCS)
// ═══════════════════════════════════════════════════════════

async function renderCreditScoreModule(container) {
    var score = await fetchJSON('/api/credit-score/ORG-001');
    var benchmark = await fetchJSON('/api/credit-score/benchmark');
    if (!score) { container.innerHTML = '<p style="color:#ef4444;padding:24px">Failed to load Credit Score data</p>'; return; }

    var html = moduleHeader('Consent Credit Score', 'KPMG/Deloitte/EY-aligned compliance scoring engine', 'fa-star', '#f59e0b,#ef4444');

    // Score gauge
    var sc = score.overallScore || 0;
    var riskColor = sc >= 80 ? '#22c55e' : sc >= 50 ? '#f59e0b' : '#ef4444';
    html += '<div style="display:flex;gap:24px;margin-bottom:24px;flex-wrap:wrap">';
    html += '<div style="background:#fff;border-radius:20px;padding:32px;box-shadow:0 4px 20px rgba(0,0,0,0.08);flex:1;min-width:280px;text-align:center">' +
        '<div style="width:160px;height:160px;border-radius:50%;border:8px solid ' + riskColor + '25;display:flex;align-items:center;justify-content:center;margin:0 auto 16px;position:relative">' +
        '<div style="position:absolute;inset:4px;border-radius:50%;border:4px solid ' + riskColor + ';border-top-color:transparent;animation:spin 3s linear infinite"></div>' +
        '<div><div style="font-size:48px;font-weight:900;color:' + riskColor + '">' + sc.toFixed(1) + '</div><div style="font-size:12px;color:#64748b">/ 100</div></div></div>' +
        '<div style="font-size:18px;font-weight:700;margin-bottom:4px">' + (score.orgId || '') + '</div>' +
        '<div>' + riskBadge(score.riskLevel || 'N/A') + '</div></div>';

    // Component scores
    var components = score.componentScores || {};
    html += '<div style="flex:2;min-width:300px">';
    html += trustCard('Component Scores', 'fa-chart-bar', '#6366f1', (function() {
        var bars = '';
        Object.keys(components).forEach(function(key) {
            var comp = components[key];
            var s = comp.score || 0;
            var barColor = s >= 80 ? '#22c55e' : s >= 50 ? '#f59e0b' : '#ef4444';
            bars += '<div style="margin-bottom:14px">' +
                '<div style="display:flex;justify-content:space-between;margin-bottom:4px"><span style="font-size:13px;color:#475569;font-weight:500">' + key.replace(/([A-Z])/g, ' $1').trim() + ' <span style="color:#94a3b8">(' + (comp.weight || '') + ')</span></span><span style="font-weight:700;color:' + barColor + '">' + s.toFixed(1) + '</span></div>' +
                '<div style="height:8px;background:#f1f5f9;border-radius:4px;overflow:hidden"><div style="height:100%;width:' + s + '%;background:' + barColor + ';border-radius:4px;transition:width 1s"></div></div></div>';
        });
        return bars;
    })());
    html += '</div></div>';

    // Recommendations
    var recs = (score.recommendations || []).map(function(r) {
        var isC = r.startsWith('CRITICAL') || r.startsWith('PRIORITY');
        return '<div style="padding:12px 16px;background:' + (isC ? '#fef2f2' : '#f8fafc') + ';border-left:4px solid ' + (isC ? '#ef4444' : '#6366f1') + ';border-radius:0 8px 8px 0;margin-bottom:8px;font-size:13px">' +
            '<i class="fas ' + (isC ? 'fa-exclamation-triangle' : 'fa-lightbulb') + '" style="color:' + (isC ? '#ef4444' : '#f59e0b') + ';margin-right:8px"></i>' + r + '</div>';
    }).join('');
    html += trustCard('DPDP Compliance Recommendations', 'fa-tasks', '#ef4444', recs);

    // Benchmarks table
    if (benchmark && benchmark.sectorBenchmarks) {
        var rows = benchmark.sectorBenchmarks.slice(0, 10).map(function(b) {
            var bc = b.benchmark >= 80 ? '#22c55e' : b.benchmark >= 50 ? '#f59e0b' : '#ef4444';
            return '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:10px;font-weight:500">' + b.sector + '</td><td style="padding:10px;text-align:center"><span style="font-weight:700;color:' + bc + '">' + b.benchmark.toFixed(1) + '</span></td><td style="padding:10px;text-align:center">' + riskBadge(b.riskLevel) + '</td></tr>';
        }).join('');
        html += trustCard('Industry Benchmarks (Top 10)', 'fa-industry', '#0ea5e9',
            '<table style="width:100%;border-collapse:collapse"><thead><tr style="background:#f8fafc"><th style="padding:10px;text-align:left;font-size:13px;color:#64748b">Sector</th><th style="padding:10px;text-align:center;font-size:13px;color:#64748b">Score</th><th style="padding:10px;text-align:center;font-size:13px;color:#64748b">Risk</th></tr></thead><tbody>' + rows + '</tbody></table>');
    }

    // Penalty reference
    if (benchmark && benchmark.penaltyReference) {
        var p = benchmark.penaltyReference;
        html += trustCard('DPDP Penalty Reference', 'fa-gavel', '#dc2626',
            '<div style="display:flex;gap:16px;flex-wrap:wrap">' +
            statBox('Non-Compliance', p.maxPenalty_NonCompliance || '₹250 Cr', '#ef4444') +
            statBox('Child Data', p.maxPenalty_ChildDataViolation || '₹200 Cr', '#dc2626') +
            statBox('Breach Notice', p.maxPenalty_BreachNotification || '₹200 Cr', '#f97316') + '</div>');
    }

    container.innerHTML = html;
}

// ═══════════════════════════════════════════════════════════
// 3. NATIONAL DPDP COMPLIANCE EXCHANGE (NDCE)
// ═══════════════════════════════════════════════════════════

async function renderNDCEModule(container) {
    var reg = await fetchJSON('/api/ndce/registry');
    if (!reg) { container.innerHTML = '<p style="color:#ef4444;padding:24px">Failed to load NDCE data</p>'; return; }

    var html = moduleHeader('National DPDP Compliance Exchange', 'UPI-like federated consent exchange for inter-fiduciary operations', 'fa-exchange-alt', '#0ea5e9,#6366f1');

    // Stats
    html += '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:24px">';
    html += statBox('Consent Artifacts', reg.totalConsentArtifacts || 0, '#0ea5e9');
    html += statBox('Active Consents', reg.activeConsents || 0, '#22c55e');
    html += statBox('Revoked', reg.revokedConsents || 0, '#ef4444');
    html += statBox('Trusted Orgs', reg.trustedOrganizations || 0, '#6366f1');
    html += statBox('Share Txns', reg.totalShareTransactions || 0, '#f59e0b');
    html += '</div>';

    // Trust Framework Participants
    var participants = (reg.participants || []).map(function(p) {
        var lvlColor = { PLATINUM: '#a78bfa', GOLD: '#f59e0b', SILVER: '#94a3b8', BRONZE: '#d97706' };
        return '<tr style="border-bottom:1px solid #f1f5f9">' +
            '<td style="padding:10px;font-weight:500">' + p.orgId + '</td>' +
            '<td style="padding:10px">' + p.orgName + '</td>' +
            '<td style="padding:10px">' + p.sector + '</td>' +
            '<td style="padding:10px;text-align:center"><span style="background:' + (lvlColor[p.trustLevel] || '#64748b') + '18;color:' + (lvlColor[p.trustLevel] || '#64748b') + ';padding:3px 10px;border-radius:12px;font-size:11px;font-weight:600">' + p.trustLevel + '</span></td>' +
            '<td style="padding:10px;text-align:center">' + riskBadge(p.status) + '</td></tr>';
    }).join('');
    html += trustCard('Trust Framework Participants', 'fa-shield-alt', '#6366f1',
        '<table style="width:100%;border-collapse:collapse"><thead><tr style="background:#f8fafc"><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Org ID</th><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Name</th><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Sector</th><th style="padding:10px;text-align:center;font-size:12px;color:#64748b">Trust</th><th style="padding:10px;text-align:center;font-size:12px;color:#64748b">Status</th></tr></thead><tbody>' + participants + '</tbody></table>');

    // Verify consent form
    html += trustCard('Cross-Org Consent Verification', 'fa-search', '#0ea5e9',
        '<div style="display:flex;gap:12px;align-items:end;flex-wrap:wrap">' +
        '<div style="flex:1;min-width:200px"><label style="font-size:12px;color:#64748b;display:block;margin-bottom:4px">Consent ID</label><input id="ndce-verify-id" value="C-001" style="width:100%;padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px"></div>' +
        '<div style="flex:1;min-width:200px"><label style="font-size:12px;color:#64748b;display:block;margin-bottom:4px">Requesting Org</label><input id="ndce-verify-org" value="ORG-002" style="width:100%;padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px"></div>' +
        '<button onclick="verifyNDCEConsent()" style="background:linear-gradient(135deg,#0ea5e9,#6366f1);color:#fff;border:none;padding:12px 24px;border-radius:10px;font-weight:600;cursor:pointer;white-space:nowrap"><i class="fas fa-check-double" style="margin-right:8px"></i>Verify</button></div>' +
        '<div id="ndce-verify-result" style="margin-top:16px"></div>');

    container.innerHTML = html;
}

window.verifyNDCEConsent = async function() {
    var body = { consentId: document.getElementById('ndce-verify-id').value, requestingOrg: document.getElementById('ndce-verify-org').value };
    var r = await fetch('/api/ndce/verify', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    var d = await r.json();
    var el = document.getElementById('ndce-verify-result');
    var isOk = d.status === 'VERIFIED';
    if (el) el.innerHTML = '<div style="background:' + (isOk ? '#f0fdf4' : '#fef2f2') + ';border:1px solid ' + (isOk ? '#22c55e' : '#ef4444') + ';border-radius:12px;padding:16px"><div style="font-weight:700;font-size:16px;color:' + (isOk ? '#22c55e' : '#ef4444') + ';margin-bottom:8px"><i class="fas ' + (isOk ? 'fa-check-circle' : 'fa-times-circle') + '" style="margin-right:8px"></i>' + d.status + '</div>' +
        (d.purpose ? '<div style="font-size:13px;color:#475569"><strong>Purpose:</strong> ' + d.purpose + '</div>' : '') +
        (d.issuingFiduciary ? '<div style="font-size:13px;color:#475569"><strong>Issuing Fiduciary:</strong> ' + d.issuingFiduciary + '</div>' : '') +
        (d.reason ? '<div style="font-size:13px;color:#ef4444;margin-top:4px">' + d.reason + '</div>' : '') + '</div>';
};

// ═══════════════════════════════════════════════════════════
// 4. PRIVACY ENHANCING TECHNOLOGY (PET)
// ═══════════════════════════════════════════════════════════

async function renderPETModule(container) {
    var caps = await fetchJSON('/api/pet/capabilities');
    if (!caps) { container.innerHTML = '<p style="color:#ef4444;padding:24px">Failed to load PET data</p>'; return; }

    var html = moduleHeader('Privacy Enhancing Technology', 'Differential Privacy • Zero-Knowledge Proofs • Federated Learning • Secure MPC', 'fa-user-secret', '#8b5cf6,#6366f1');

    var techniques = caps.techniques || {};
    // Technique cards
    html += '<div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:20px;margin-bottom:24px">';

    html += '<div style="background:linear-gradient(160deg,#8b5cf615,#6366f110);border:1px solid #8b5cf625;border-radius:16px;padding:24px">' +
        '<div style="font-size:20px;margin-bottom:8px">🔒</div><h4 style="margin:0 0 8px;color:#6366f1">Differential Privacy</h4>' +
        '<p style="font-size:13px;color:#64748b;margin:0">Laplace & Gaussian noise injection for analytics. Mathematically proven privacy guarantees.</p>' +
        '<div style="margin-top:12px;font-size:12px;color:#8b5cf6">' + ((techniques.differentialPrivacy || {}).standard || 'NIST SP 800-188') + '</div></div>';

    html += '<div style="background:linear-gradient(160deg,#22c55e15,#14b8a610);border:1px solid #22c55e25;border-radius:16px;padding:24px">' +
        '<div style="font-size:20px;margin-bottom:8px">🛡️</div><h4 style="margin:0 0 8px;color:#22c55e">Zero-Knowledge Proofs</h4>' +
        '<p style="font-size:13px;color:#64748b;margin:0">Prove claims (age > 18, identity) without revealing underlying data.</p>' +
        '<div style="margin-top:12px;font-size:12px;color:#22c55e">Pedersen Commitment Protocol</div></div>';

    html += '<div style="background:linear-gradient(160deg,#0ea5e915,#0284c710);border:1px solid #0ea5e925;border-radius:16px;padding:24px">' +
        '<div style="font-size:20px;margin-bottom:8px">🤖</div><h4 style="margin:0 0 8px;color:#0ea5e9">Federated Learning</h4>' +
        '<p style="font-size:13px;color:#64748b;margin:0">Train models across organizations. Raw data never leaves participant boundary.</p>' +
        '<div style="margin-top:12px;font-size:12px;color:#0ea5e9">FedAvg — McMahan et al. 2017</div></div>';

    html += '<div style="background:linear-gradient(160deg,#f59e0b15,#ef444410);border:1px solid #f59e0b25;border-radius:16px;padding:24px">' +
        '<div style="font-size:20px;margin-bottom:8px">🔐</div><h4 style="margin:0 0 8px;color:#f59e0b">Secure MPC</h4>' +
        '<p style="font-size:13px;color:#64748b;margin:0">Shamir\'s Secret Sharing + Lagrange reconstruction. No single party sees full data.</p>' +
        '<div style="margin-top:12px;font-size:12px;color:#f59e0b">k-of-n Threshold Scheme</div></div>';
    html += '</div>';

    // ZKP Demo
    html += trustCard('Zero-Knowledge Proof Demo', 'fa-key', '#8b5cf6',
        '<p style="font-size:13px;color:#64748b;margin-bottom:16px">Prove a claim without revealing the underlying value. Try it:</p>' +
        '<div style="display:flex;gap:12px;flex-wrap:wrap;align-items:end">' +
        '<div><label style="font-size:12px;color:#64748b;display:block;margin-bottom:4px">Claim</label>' +
        '<select id="pet-claim" style="padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px"><option value="age_above_18">Age ≥ 18</option><option value="age_above_21">Age ≥ 21</option><option value="identity_verified">Identity Verified</option></select></div>' +
        '<div><label style="font-size:12px;color:#64748b;display:block;margin-bottom:4px">Value</label><input id="pet-value" value="25" type="number" style="width:80px;padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px"></div>' +
        '<button onclick="generateZKP()" style="background:linear-gradient(135deg,#8b5cf6,#6366f1);color:#fff;border:none;padding:12px 24px;border-radius:10px;font-weight:600;cursor:pointer"><i class="fas fa-magic" style="margin-right:8px"></i>Generate Proof</button></div>' +
        '<div id="pet-zkp-result" style="margin-top:16px"></div>');

    // DP Demo
    html += trustCard('Differential Privacy Query', 'fa-chart-line', '#22c55e',
        '<p style="font-size:13px;color:#64748b;margin-bottom:16px">Add calibrated noise to protect individual privacy in analytics:</p>' +
        '<div style="display:flex;gap:12px;flex-wrap:wrap;align-items:end">' +
        '<div><label style="font-size:12px;color:#64748b;display:block;margin-bottom:4px">True Value</label><input id="pet-true" value="42" type="number" style="width:80px;padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px"></div>' +
        '<div><label style="font-size:12px;color:#64748b;display:block;margin-bottom:4px">Epsilon (ε)</label><input id="pet-eps" value="1.0" type="number" step="0.1" style="width:80px;padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px"></div>' +
        '<button onclick="runDPQuery()" style="background:linear-gradient(135deg,#22c55e,#14b8a6);color:#fff;border:none;padding:12px 24px;border-radius:10px;font-weight:600;cursor:pointer"><i class="fas fa-play" style="margin-right:8px"></i>Run Query</button></div>' +
        '<div id="pet-dp-result" style="margin-top:16px"></div>');

    container.innerHTML = html;
}

window.generateZKP = async function() {
    var body = { claim: document.getElementById('pet-claim').value, value: parseInt(document.getElementById('pet-value').value) };
    var r = await fetch('/api/pet/zkp/prove', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    var d = await r.json();
    var el = document.getElementById('pet-zkp-result');
    var valid = d.claimValid;
    if (el) el.innerHTML = '<div style="background:' + (valid ? '#f0fdf4' : '#fef2f2') + ';border:1px solid ' + (valid ? '#22c55e' : '#ef4444') + ';border-radius:12px;padding:16px">' +
        '<div style="font-weight:700;color:' + (valid ? '#22c55e' : '#ef4444') + ';margin-bottom:8px"><i class="fas ' + (valid ? 'fa-check-circle' : 'fa-times-circle') + '" style="margin-right:8px"></i>Claim: ' + (valid ? 'VALID' : 'INVALID') + '</div>' +
        '<div style="font-size:12px;color:#64748b">Proof ID: <code>' + d.proofId + '</code></div>' +
        '<div style="font-size:12px;color:#64748b">Data Revealed: <strong>NONE</strong> — Zero knowledge preserved</div>' +
        '<div style="font-size:12px;color:#64748b">Protocol: ' + (d.protocol || 'Pedersen Commitment') + '</div></div>';
};

window.runDPQuery = async function() {
    var body = { trueValue: parseFloat(document.getElementById('pet-true').value), sensitivity: 1.0, epsilon: parseFloat(document.getElementById('pet-eps').value) };
    var r = await fetch('/api/pet/differential-privacy/query', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    var d = await r.json();
    var el = document.getElementById('pet-dp-result');
    if (el) el.innerHTML = '<div style="background:#eff6ff;border:1px solid #3b82f6;border-radius:12px;padding:16px">' +
        '<div style="display:flex;gap:24px;flex-wrap:wrap"><div><div style="font-size:12px;color:#64748b">Noisy Result</div><div style="font-size:28px;font-weight:800;color:#3b82f6">' + d.noisyResult + '</div></div>' +
        '<div><div style="font-size:12px;color:#64748b">Privacy Level</div><div style="font-size:16px;font-weight:600;margin-top:4px">' + riskBadge(d.privacyLevel || 'MODERATE') + '</div></div>' +
        '<div><div style="font-size:12px;color:#64748b">Noise Added</div><div style="font-size:16px;font-weight:600;color:#6366f1;margin-top:4px">' + d.noiseAdded + '</div></div></div></div>';
};

// ═══════════════════════════════════════════════════════════
// 5. AI GOVERNANCE ENGINE
// ═══════════════════════════════════════════════════════════

async function renderAIGovernanceModule(container) {
    var models = await fetchJSON('/api/ai/models');
    var audit = await fetchJSON('/api/ai/audit?page=0&size=10');
    if (!models) { container.innerHTML = '<p style="color:#ef4444;padding:24px">Failed to load AI Governance data</p>'; return; }

    var html = moduleHeader('AI Governance & Accountability', 'Model Registry • Decision Logging • Bias Detection • Explainability', 'fa-brain', '#ec4899,#8b5cf6');

    // Stats
    html += '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:24px">';
    html += statBox('AI Models', models.totalModels || 0, '#ec4899');
    html += statBox('Decisions Logged', (audit ? audit.totalDecisions : 0), '#6366f1');
    html += statBox('Governance', 'FULL', '#22c55e');
    html += '</div>';

    // Model Registry
    var modelRows = (models.models || []).map(function(m) {
        var riskC = m.riskLevel === 'HIGH' ? '#ef4444' : m.riskLevel === 'MEDIUM' ? '#f59e0b' : '#22c55e';
        return '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:12px;font-weight:600;color:#1e293b">' + m.modelId + '</td>' +
            '<td style="padding:12px">' + m.name + '</td>' +
            '<td style="padding:12px">' + m.version + '</td>' +
            '<td style="padding:12px">' + m.type + '</td>' +
            '<td style="padding:12px;text-align:center">' + riskBadge(m.riskLevel) + '</td>' +
            '<td style="padding:12px;text-align:center">' + (m.decisionCount || 0) + '</td>' +
            '<td style="padding:12px;text-align:center"><button onclick="loadBiasReport(\'' + m.modelId + '\')" style="background:#ec489918;color:#ec4899;border:1px solid #ec4899;padding:6px 14px;border-radius:8px;cursor:pointer;font-size:12px;font-weight:500"><i class="fas fa-chart-pie" style="margin-right:4px"></i>Bias</button></td></tr>';
    }).join('');
    html += trustCard('Model Registry', 'fa-database', '#6366f1',
        '<table style="width:100%;border-collapse:collapse"><thead><tr style="background:#f8fafc"><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Model ID</th><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Name</th><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Version</th><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Type</th><th style="padding:10px;text-align:center;font-size:12px;color:#64748b">Risk</th><th style="padding:10px;text-align:center;font-size:12px;color:#64748b">Decisions</th><th style="padding:10px;text-align:center;font-size:12px;color:#64748b">Actions</th></tr></thead><tbody>' + modelRows + '</tbody></table>');

    // Bias report placeholder
    html += '<div id="ai-bias-report-area"></div>';

    // Framework
    html += trustCard('Governance Framework', 'fa-gavel', '#8b5cf6',
        '<div style="display:flex;flex-wrap:wrap;gap:8px">' +
        ['EU AI Act', 'NIST AI RMF 1.0', 'IEEE 7010', 'DPDP Act 2023 S.8'].map(function(f) {
            return '<div style="background:#eff6ff;color:#3b82f6;padding:8px 16px;border-radius:10px;font-size:13px;font-weight:500"><i class="fas fa-check" style="margin-right:6px;color:#22c55e"></i>' + f + '</div>';
        }).join('') + '</div>');

    container.innerHTML = html;
}

window.loadBiasReport = async function(modelId) {
    var d = await fetchJSON('/api/ai/bias-report/' + modelId);
    if (!d) return;
    var el = document.getElementById('ai-bias-report-area');
    if (!el) return;
    var metrics = d.biasMetrics || {};
    var metricHTML = Object.keys(metrics).map(function(key) {
        var m = metrics[key];
        var pct = ((m.value || 0) * 100).toFixed(1);
        var color = m.status === 'PASS' ? '#22c55e' : '#ef4444';
        return '<div style="margin-bottom:16px">' +
            '<div style="display:flex;justify-content:space-between;margin-bottom:6px"><span style="font-size:13px;font-weight:500;color:#1e293b">' + key.replace(/([A-Z])/g, ' $1').trim() + '</span><span style="font-weight:700;color:' + color + '">' + pct + '% ' + riskBadge(m.status || 'N/A') + '</span></div>' +
            '<div style="height:10px;background:#f1f5f9;border-radius:5px;overflow:hidden"><div style="height:100%;width:' + pct + '%;background:' + color + ';border-radius:5px"></div></div>' +
            '<div style="font-size:11px;color:#94a3b8;margin-top:4px">' + (m.description || '') + '</div></div>';
    }).join('');

    el.innerHTML = trustCard('Bias Report — ' + d.modelName, 'fa-balance-scale', '#ec4899',
        '<div style="margin-bottom:12px"><strong>Overall Bias Risk:</strong> ' + riskBadge(d.overallBiasRisk || 'N/A') + '</div>' + metricHTML +
        '<div style="margin-top:16px"><strong>Recommendations:</strong></div>' +
        (d.recommendations || []).map(function(r) { return '<div style="padding:8px;background:#fef7ff;border-left:3px solid #ec4899;border-radius:0 6px 6px 0;margin-top:6px;font-size:13px">' + r + '</div>'; }).join(''));
};

// ═══════════════════════════════════════════════════════════
// 6. CONSENT WALLET & ECONOMY
// ═══════════════════════════════════════════════════════════

async function renderWalletModule(container) {
    var wallet = await fetchJSON('/api/wallet/DP-001');
    var listings = await fetchJSON('/api/marketplace/listings');
    if (!wallet) { container.innerHTML = '<p style="color:#ef4444;padding:24px">Failed to load Wallet data</p>'; return; }

    var html = moduleHeader('Consent Wallet & Economy', 'Data Principal consent management, marketplace, and notifications', 'fa-wallet', '#14b8a6,#0ea5e9');

    // Wallet stats
    html += '<div style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:24px">';
    html += statBox('Total Consents', wallet.totalConsents || 0, '#0ea5e9');
    html += statBox('Active', wallet.activeConsents || 0, '#22c55e');
    html += statBox('Revoked', wallet.revokedConsents || 0, '#ef4444');
    html += statBox('Privacy Score', (wallet.privacyScore || 0).toFixed(1), '#8b5cf6');
    html += '</div>';

    // Consent list
    var consents = (wallet.consents || []).map(function(c) {
        var stColor = c.status === 'ACTIVE' ? '#22c55e' : '#ef4444';
        return '<div style="display:flex;justify-content:space-between;align-items:center;padding:14px 16px;border-bottom:1px solid #f1f5f9">' +
            '<div><div style="font-weight:600;font-size:14px;color:#1e293b">' + c.fiduciary + '</div><div style="font-size:12px;color:#64748b">' + c.purpose + '</div></div>' +
            '<div style="display:flex;align-items:center;gap:12px">' +
            '<div style="font-size:11px;color:#94a3b8">' + (c.dataCategories || []).join(', ') + '</div>' +
            riskBadge(c.status) + '</div></div>';
    }).join('');
    html += trustCard('Your Consents (DP-001)', 'fa-file-signature', '#14b8a6', consents);

    // Rights panel
    if (wallet.rights) {
        var rightsHTML = Object.keys(wallet.rights).map(function(key) {
            return '<div style="display:flex;gap:12px;padding:10px 0;border-bottom:1px solid #f8fafc"><div style="width:100px;font-weight:600;color:#6366f1;font-size:13px;text-transform:capitalize">' + key + '</div><div style="font-size:13px;color:#475569">' + wallet.rights[key] + '</div></div>';
        }).join('');
        html += trustCard('Data Principal Rights (DPDP Act)', 'fa-user-shield', '#6366f1', rightsHTML);
    }

    // Marketplace
    if (listings && listings.listings) {
        var listingCards = listings.listings.map(function(l) {
            return '<div style="background:linear-gradient(160deg,#f8fafc,#eff6ff);border:1px solid #e2e8f0;border-radius:12px;padding:16px;flex:1;min-width:240px">' +
                '<div style="font-weight:600;color:#1e293b;margin-bottom:8px">' + l.title + '</div>' +
                '<div style="font-size:12px;color:#64748b;margin-bottom:4px"><i class="fas fa-building" style="margin-right:6px;color:#6366f1"></i>' + l.fiduciary + '</div>' +
                '<div style="font-size:12px;color:#64748b"><i class="fas fa-tag" style="margin-right:6px;color:#f59e0b"></i>' + l.compensationType + '</div></div>';
        }).join('');
        html += trustCard('Consent Marketplace', 'fa-store', '#f59e0b',
            '<div style="display:flex;gap:16px;flex-wrap:wrap">' + listingCards + '</div>' +
            '<div style="margin-top:16px;padding:12px;background:#fefce8;border:1px solid #fde047;border-radius:10px;font-size:13px;color:#854d0e"><i class="fas fa-info-circle" style="margin-right:8px"></i>' + (listings.notice || 'Marketplace pending regulatory clarity') + '</div>');
    }

    container.innerHTML = html;
}

// ═══════════════════════════════════════════════════════════
// 7. GLOBAL INTEROPERABILITY
// ═══════════════════════════════════════════════════════════

async function renderInteropModule(container) {
    var frameworks = await fetchJSON('/api/interop/frameworks');
    var mapping = await fetchJSON('/api/interop/gdpr-mapping/C-001');
    if (!frameworks) { container.innerHTML = '<p style="color:#ef4444;padding:24px">Failed to load Interop data</p>'; return; }

    var html = moduleHeader('Global Interoperability', 'DPDP ↔ GDPR ↔ OECD ↔ ISO 27701 mapping and cross-border validation', 'fa-globe-americas', '#f43f5e,#ec4899');

    // Framework table
    var fwRows = (frameworks.frameworks || []).map(function(f) {
        var stColor = f.status === 'ACTIVE' ? '#22c55e' : '#0ea5e9';
        return '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:10px;font-weight:600;color:#1e293b">' + f.name + '</td>' +
            '<td style="padding:10px">' + f.jurisdiction + '</td>' +
            '<td style="padding:10px">' + f.type + '</td>' +
            '<td style="padding:10px;text-align:center"><span style="background:' + stColor + '18;color:' + stColor + ';padding:3px 10px;border-radius:12px;font-size:11px;font-weight:600">' + f.status + '</span></td></tr>';
    }).join('');
    html += trustCard('Supported Regulatory Frameworks', 'fa-sitemap', '#6366f1',
        '<table style="width:100%;border-collapse:collapse"><thead><tr style="background:#f8fafc"><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Framework</th><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Jurisdiction</th><th style="padding:10px;text-align:left;font-size:12px;color:#64748b">Type</th><th style="padding:10px;text-align:center;font-size:12px;color:#64748b">Status</th></tr></thead><tbody>' + fwRows + '</tbody></table>');

    // DPDP ↔ GDPR mapping
    if (mapping && mapping.equivalenceMatrix) {
        var matrix = mapping.equivalenceMatrix;
        var mapRows = Object.keys(matrix).slice(0, 8).map(function(key) {
            var m = matrix[key];
            return '<tr style="border-bottom:1px solid #f1f5f9"><td style="padding:10px;font-weight:600;color:#6366f1;font-size:12px">' + key.replace(/_/g, ' ') + '</td>' +
                '<td style="padding:10px;font-size:12px">' + (m.dpdp || '') + '</td>' +
                '<td style="padding:10px;font-size:12px">' + (m.gdpr || '') + '</td></tr>';
        }).join('');
        html += trustCard('DPDP ↔ GDPR Equivalence Matrix', 'fa-columns', '#f43f5e',
            '<div style="margin-bottom:12px"><strong>Overall Equivalence:</strong> ' + riskBadge(mapping.overallEquivalence || 'SUBSTANTIALLY_EQUIVALENT') + '</div>' +
            '<div style="overflow-x:auto"><table style="width:100%;border-collapse:collapse"><thead><tr style="background:#f8fafc"><th style="padding:10px;text-align:left;font-size:11px;color:#64748b;white-space:nowrap">Principle</th><th style="padding:10px;text-align:left;font-size:11px;color:#64748b">DPDP Act 2023</th><th style="padding:10px;text-align:left;font-size:11px;color:#64748b">GDPR 2016/679</th></tr></thead><tbody>' + mapRows + '</tbody></table></div>');

        // Key differences
        if (mapping.keyDifferences) {
            html += trustCard('Key Differences', 'fa-not-equal', '#f59e0b',
                mapping.keyDifferences.map(function(d) {
                    return '<div style="padding:8px 12px;background:#fefce8;border-left:3px solid #f59e0b;border-radius:0 8px 8px 0;margin-bottom:6px;font-size:13px"><i class="fas fa-exclamation-triangle" style="color:#f59e0b;margin-right:8px"></i>' + d + '</div>';
                }).join(''));
        }
    }

    // Cross-border validation form
    html += trustCard('Cross-Border Consent Validation', 'fa-plane', '#0ea5e9',
        '<div style="display:flex;gap:12px;flex-wrap:wrap;align-items:end">' +
        '<div><label style="font-size:12px;color:#64748b;display:block;margin-bottom:4px">Source</label><select id="interop-source" style="padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px"><option>INDIA</option><option>EU</option><option>USA</option></select></div>' +
        '<div><label style="font-size:12px;color:#64748b;display:block;margin-bottom:4px">Target</label><select id="interop-target" style="padding:10px 14px;border:1px solid #e2e8f0;border-radius:10px;font-size:14px"><option>EU</option><option>USA</option><option>INDIA</option><option>JAPAN</option><option>SINGAPORE</option></select></div>' +
        '<button onclick="validateCrossBorder()" style="background:linear-gradient(135deg,#f43f5e,#ec4899);color:#fff;border:none;padding:12px 24px;border-radius:10px;font-weight:600;cursor:pointer"><i class="fas fa-globe" style="margin-right:8px"></i>Validate</button></div>' +
        '<div id="interop-result" style="margin-top:16px"></div>');

    container.innerHTML = html;
}

window.validateCrossBorder = async function() {
    var body = { consentId: 'C-001', sourceJurisdiction: document.getElementById('interop-source').value, targetJurisdiction: document.getElementById('interop-target').value };
    var r = await fetch('/api/interop/validate-cross-border', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    var d = await r.json();
    var el = document.getElementById('interop-result');
    var allowed = d.transferAllowed;
    if (el) el.innerHTML = '<div style="background:' + (allowed ? '#f0fdf4' : '#fef2f2') + ';border:1px solid ' + (allowed ? '#22c55e' : '#ef4444') + ';border-radius:12px;padding:16px">' +
        '<div style="font-weight:700;font-size:16px;color:' + (allowed ? '#22c55e' : '#ef4444') + ';margin-bottom:8px"><i class="fas ' + (allowed ? 'fa-check-circle' : 'fa-ban') + '" style="margin-right:8px"></i>Transfer ' + (allowed ? 'ALLOWED' : 'RESTRICTED') + '</div>' +
        '<div style="font-size:13px;color:#475569"><strong>Legal Basis:</strong> ' + (d.legalBasis || '') + '</div>' +
        '<div style="font-size:13px;color:#475569"><strong>Adequacy Decision:</strong> ' + (d.adequacyDecision ? 'Yes' : 'No') + '</div>' +
        '<div style="font-size:13px;color:#475569"><strong>Risk Level:</strong> ' + riskBadge(d.riskLevel || 'N/A') + '</div>' +
        (d.additionalSafeguards ? '<div style="margin-top:8px"><strong style="font-size:12px;color:#64748b">Required Safeguards:</strong>' + d.additionalSafeguards.map(function(s) { return '<div style="font-size:12px;color:#475569;padding:4px 0">• ' + s + '</div>'; }).join('') + '</div>' : '') + '</div>';
};

// Add CSS animation for spinners
(function() {
    if (!document.getElementById('trust-os-styles')) {
        var style = document.createElement('style');
        style.id = 'trust-os-styles';
        style.textContent = '@keyframes spin{from{transform:rotate(0deg)}to{transform:rotate(360deg)}}';
        document.head.appendChild(style);
    }
})();
