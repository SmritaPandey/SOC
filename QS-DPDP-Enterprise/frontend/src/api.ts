const API_BASE = '/api/v1';

async function apiFetch<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!res.ok) throw new Error(`API ${res.status}: ${res.statusText}`);
  return res.json();
}

export const api = {
  // ═══ Health ═══
  health: () => apiFetch<any>('/health'),

  // ═══ Dashboard ═══
  dashboard: () => apiFetch<any>('/dashboard'),
  complianceScore: (module: string) => apiFetch<any>(`/dashboard/compliance-score/${module}`),

  // ═══ Chatbot ═══
  chatQuery: (query: string, userId = 'anonymous') =>
    apiFetch<any>('/chatbot/query', { method: 'POST', body: JSON.stringify({ query, userId }) }),

  // ═══ Consent — Universal Consent Manager ═══
  consentStats: () => apiFetch<any>('/consent/stats'),
  consentList: (offset = 0, limit = 50) =>
    apiFetch<any>(`/consent?offset=${offset}&limit=${limit}`),
  consentByPrincipal: (principalId: string) =>
    apiFetch<any>(`/consent/principal/${principalId}`),
  consentCreate: (data: any) =>
    apiFetch<any>('/consent', { method: 'POST', body: JSON.stringify(data) }),
  consentRequest: (data: any) =>
    apiFetch<any>('/consent/request', { method: 'POST', body: JSON.stringify(data) }),
  consentGet: (id: string) => apiFetch<any>(`/consent/${id}`),
  consentWithdraw: (id: string, reason: string, actorId = 'DATA_PRINCIPAL') =>
    apiFetch<any>(`/consent/${id}/withdraw`, { method: 'POST', body: JSON.stringify({ reason, actorId }) }),
  consentModify: (id: string, modifications: any) =>
    apiFetch<any>(`/consent/${id}/modify`, { method: 'PUT', body: JSON.stringify(modifications) }),
  consentRenew: (id: string, extensionDays = 365) =>
    apiFetch<any>(`/consent/${id}/renew`, { method: 'POST', body: JSON.stringify({ extensionDays }) }),
  consentExpiring: (days = 30) => apiFetch<any>(`/consent/expiring?days=${days}`),
  consentAuditTrail: (consentId?: string, limit = 100) =>
    apiFetch<any>(`/consent/audit-trail?${consentId ? `consentId=${consentId}&` : ''}limit=${limit}`),
  consentVerifyAudit: () => apiFetch<any>('/consent/verify-audit-chain'),
  consentPreferences: (consentId: string) => apiFetch<any>(`/consent/${consentId}/preferences`),
  consentSavePreference: (consentId: string, pref: any) =>
    apiFetch<any>(`/consent/${consentId}/preferences`, { method: 'POST', body: JSON.stringify(pref) }),
  guardianConsents: (status?: string) =>
    apiFetch<any>(`/consent/guardian${status ? `?status=${status}` : ''}`),
  guardianConsentSave: (data: any) =>
    apiFetch<any>('/consent/guardian', { method: 'POST', body: JSON.stringify(data) }),
  guardianConsentVerify: (id: string, method = 'OTP') =>
    apiFetch<any>(`/consent/guardian/${id}/verify`, { method: 'POST', body: JSON.stringify({ verificationMethod: method }) }),
  sectorTemplates: (sector?: string) =>
    apiFetch<any>(`/consent/sector-templates${sector ? `?sector=${sector}` : ''}`),
  dataCategories: () => apiFetch<any>('/consent/data-categories'),
  dataCategorySave: (data: any) =>
    apiFetch<any>('/consent/data-categories', { method: 'POST', body: JSON.stringify(data) }),
  consentNotices: () => apiFetch<any>('/consent/notices'),
  consentNoticeSave: (data: any) =>
    apiFetch<any>('/consent/notices', { method: 'POST', body: JSON.stringify(data) }),
  consentDelegations: (status?: string) =>
    apiFetch<any>(`/consent/delegations${status ? `?status=${status}` : ''}`),
  consentDelegationSave: (data: any) =>
    apiFetch<any>('/consent/delegations', { method: 'POST', body: JSON.stringify(data) }),
  legitimateUses: () => apiFetch<any>('/consent/legitimate-uses'),
  legitimateUseSave: (data: any) =>
    apiFetch<any>('/consent/legitimate-uses', { method: 'POST', body: JSON.stringify(data) }),
  dataAccessLog: (principalId?: string, limit = 100) =>
    apiFetch<any>(`/consent/data-access-log?${principalId ? `principalId=${principalId}&` : ''}limit=${limit}`),
  dataAccessLogEntry: (data: any) =>
    apiFetch<any>('/consent/data-access-log', { method: 'POST', body: JSON.stringify(data) }),
  consentAnalytics: () => apiFetch<any>('/consent/analytics/patterns'),
  consentCompliance: () => apiFetch<any>('/consent/analytics/compliance'),

  // ═══ Notifications ═══
  notificationSend: (data: any) =>
    apiFetch<any>('/notifications/send', { method: 'POST', body: JSON.stringify(data) }),
  notifications: (principalId?: string, limit = 50) =>
    apiFetch<any>(`/notifications?${principalId ? `principalId=${principalId}&` : ''}limit=${limit}`),
  breachNotify: (breachId: string, principalIds: string[], language = 'en') =>
    apiFetch<any>(`/notifications/breach/${breachId}/notify`, { method: 'POST', body: JSON.stringify({ principalIds, language }) }),

  // ═══ Breach ═══
  breachStats: () => apiFetch<any>('/breach/stats'),
  breachList: (status?: string) =>
    apiFetch<any>(status ? `/breach?status=${status}` : '/breach'),
  breachCreate: (data: any) =>
    apiFetch<any>('/breach', { method: 'POST', body: JSON.stringify(data) }),
  breachGet: (id: string) => apiFetch<any>(`/breach/${id}`),
  breachOverdue: () => apiFetch<any>('/breach/overdue'),

  // ═══ SIEM ═══
  siemStats: () => apiFetch<any>('/siem/stats'),
  siemHealth: () => apiFetch<any>('/siem/health'),
  siemIngestEvent: (data: any) =>
    apiFetch<any>('/siem/events', { method: 'POST', body: JSON.stringify(data) }),
  threatIntelStats: () => apiFetch<any>('/siem/threat-intel/stats'),
  lookupIndicator: (indicator: string) =>
    apiFetch<any>('/siem/threat-intel/lookup', { method: 'POST', body: JSON.stringify({ indicator }) }),
  refreshFeeds: () => apiFetch<any>('/siem/threat-intel/refresh-feeds', { method: 'POST' }),
  uebaRiskUsers: (limit = 10) => apiFetch<any>(`/siem/ueba/risk-users?limit=${limit}`),

  // ═══ DLP ═══
  dlpStats: () => apiFetch<any>('/dlp/stats'),
  dlpHealth: () => apiFetch<any>('/dlp/health'),
  piiScan: (content: string) =>
    apiFetch<any>('/dlp/pii-scan', { method: 'POST', body: JSON.stringify({ content }) }),
  dlpIncidents: () => apiFetch<any>('/dlp/incidents'),
  dlpPolicies: () => apiFetch<any>('/dlp/policies'),

  // ═══ EDR/XDR ═══
  edrStats: () => apiFetch<any>('/edr/stats'),
  edrDashboard: () => apiFetch<any>('/edr/dashboard'),
  edrAgents: (filter?: string) => apiFetch<any>(filter ? `/edr/agents?filter=${filter}` : '/edr/agents'),
  edrThreats: (agentId?: string) =>
    apiFetch<any>(agentId ? `/edr/threats?agentId=${agentId}` : '/edr/threats'),
  edrIsolate: (agentId: string, reason: string) =>
    apiFetch<any>(`/edr/agents/${agentId}/isolate`, { method: 'POST', body: JSON.stringify({ reason }) }),
  edrPolicies: () => apiFetch<any>('/edr/policies'),
  xdrStats: () => apiFetch<any>('/edr/xdr/stats'),
  xdrIncidents: (filter?: string) =>
    apiFetch<any>(filter ? `/edr/xdr/incidents?filter=${filter}` : '/edr/xdr/incidents'),

  // ═══ Policy Engine ═══
  policyStats: () => apiFetch<any>('/policies/stats'),
  policyList: (status?: string, category?: string) => {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    if (category) params.set('category', category);
    return apiFetch<any>(`/policies?${params}`);
  },
  policyGet: (id: string) => apiFetch<any>(`/policies/${id}`),
  policyCreate: (data: any) =>
    apiFetch<any>('/policies', { method: 'POST', body: JSON.stringify(data) }),
  policyActive: () => apiFetch<any>('/policies/active'),
  policyReviewRequired: () => apiFetch<any>('/policies/review-required'),
  policyVersions: (id: string) => apiFetch<any>(`/policies/${id}/versions`),

  // ═══ DPIA ═══
  dpiaStats: () => apiFetch<any>('/dpia/stats'),
  dpiaList: () => apiFetch<any>('/dpia'),
  dpiaGet: (id: string) => apiFetch<any>(`/dpia/${id}`),
  dpiaCreate: (data: any) =>
    apiFetch<any>('/dpia', { method: 'POST', body: JSON.stringify(data) }),
  dpiaReviewRequired: () => apiFetch<any>('/dpia/review-required'),

  // ═══ Rights Management ═══
  rightsStats: () => apiFetch<any>('/rights/stats'),
  rightsList: () => apiFetch<any>('/rights/requests'),
  rightsGet: (id: string) => apiFetch<any>(`/rights/requests/${id}`),
  rightsCreate: (data: any) =>
    apiFetch<any>('/rights/requests', { method: 'POST', body: JSON.stringify(data) }),
  rightsPending: () => apiFetch<any>('/rights/requests/pending'),
  rightsOverdue: () => apiFetch<any>('/rights/requests/overdue'),

  // ═══ Gap Analysis ═══
  gapStats: () => apiFetch<any>('/gap/stats'),

  // ═══ Crypto ═══
  cryptoCapabilities: () => apiFetch<any>('/crypto/capabilities'),
  cryptoEncrypt: (data: string) =>
    apiFetch<any>('/crypto/encrypt', { method: 'POST', body: JSON.stringify({ data }) }),
  cryptoMaskPII: (value: string, type = 'GENERIC') =>
    apiFetch<any>('/crypto/mask-pii', { method: 'POST', body: JSON.stringify({ value, type }) }),

  // ═══ Agent Management ═══
  agentList: (filter?: string) =>
    apiFetch<any>(filter ? `/agents?filter=${filter}` : '/agents'),
  agentDashboard: () => apiFetch<any>('/agents/dashboard'),
  agentStats: () => apiFetch<any>('/agents/stats'),
  agentIsolate: (id: string, reason: string) =>
    apiFetch<any>(`/agents/${id}/isolate`, { method: 'POST', body: JSON.stringify({ reason }) }),

  // ═══ Settings ═══
  settingsGet: () => apiFetch<any>('/settings').catch(() => null),
  settingsSave: (data: any) =>
    apiFetch<any>('/settings', { method: 'POST', body: JSON.stringify(data) }).catch(() => null),

  // ═══ Reports ═══
  reportsList: () => apiFetch<any>('/reports').catch(() => ({ reports: [] })),
  reportGenerate: (type: string, params?: any) =>
    apiFetch<any>('/reports/generate', { method: 'POST', body: JSON.stringify({ type, ...params }) }).catch(() => null),

  // ═══ Licensing ═══
  licensingStats: () => apiFetch<any>('/licensing/stats').catch(() => null),
};

export default api;
