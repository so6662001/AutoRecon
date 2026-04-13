import { get, post, put, del } from '@/utils/request'

// ERP连接
export const listErpConnections = () => get('/v1/erp/connections')
export const createErpConnection = (data: Record<string, unknown>) =>
  post('/v1/erp/connections', data)
export const updateErpConnection = (id: number, data: Record<string, unknown>) =>
  put(`/v1/erp/connections/${id}`, data)
export const deleteErpConnection = (id: number) => del(`/v1/erp/connections/${id}`)
export const testErpConnection = (id: number) => post(`/v1/erp/connections/${id}/test`)

export const pullErpData = (
  connectionId: number,
  dataType: string,
  periodStart?: string,
  periodEnd?: string
) =>
  post('/v1/recon/data/erp-pull', undefined, {
    params: { connectionId, dataType, periodStart, periodEnd },
  })

export const importErpData = (connectionId: number, billId: number, dataType?: string) =>
  post('/v1/recon/data/erp-import', undefined, {
    params: { connectionId, billId, dataType: dataType || 'order' },
  })

// 买方配置
export const getBuyerConfig = () => get('/v1/buyer/config')
export const saveBuyerConfig = (data: Record<string, unknown>) => put('/v1/buyer/config', data)

// 用户管理
export const listUsers = (params: Record<string, unknown>) => get('/v1/users', params)
export const createUser = (data: Record<string, unknown>) => post('/v1/users', data)
export const updateUser = (id: number, data: Record<string, unknown>) => put(`/v1/users/${id}`, data)
export const disableUser = (id: number) => put(`/v1/users/${id}/disable`)
export const enableUser = (id: number) => put(`/v1/users/${id}/enable`)

// 提醒订阅
export const getSubscriptions = () => get('/v1/recon/subscriptions')
export const updateSubscriptions = (data: Record<string, unknown>) =>
  put('/v1/recon/subscriptions', data)

// 计费
export const getCurrentSubscription = () => get('/v1/billing/current')
export const getUsage = () => get('/v1/billing/usage')
export const getBillingBills = (params: Record<string, unknown>) => get('/v1/billing/bills', params)
export const subscribe = (data: Record<string, unknown>) => post('/v1/billing/subscribe', data)
export const getSealQuota = () => get('/v1/billing/seal-quota')
export const purchaseSealPackage = (packageType: number) =>
  post('/v1/billing/seal-package', {}, { params: { packageType } })

// 企业信息
export const getEnterprise = (id: number) => get(`/v1/enterprises/${id}`)
export const updateEnterprise = (id: number, data: Record<string, unknown>) =>
  put(`/v1/enterprises/${id}`, data)
export const submitAuth = (id: number, data: Record<string, unknown>) =>
  post(`/v1/enterprises/${id}/auth`, data)
export const getAuthStatus = (id: number) => get(`/v1/enterprises/${id}/auth`)

// 自动对账计划
export const listAutoPlans = () => get('/v1/recon/auto-plans')
export const createAutoPlan = (data: Record<string, unknown>) =>
  post('/v1/recon/auto-plans', data)
export const updateAutoPlan = (id: number, data: Record<string, unknown>) =>
  put(`/v1/recon/auto-plans/${id}`, data)
export const toggleAutoPlan = (id: number) => put(`/v1/recon/auto-plans/${id}/toggle`)
export const triggerAutoPlan = (id: number) => post(`/v1/recon/auto-plans/${id}/trigger`)
export const deleteAutoPlan = (id: number) => del(`/v1/recon/auto-plans/${id}`)
export const getAutoPlanLogs = (id: number) => get(`/v1/recon/auto-plans/${id}/logs`)

// 对账日历
export const getCalendarEvents = (params: Record<string, unknown>) =>
  get('/v1/recon/calendar', params)
export const getUpcomingEvents = (days: number) =>
  get('/v1/recon/calendar/upcoming', { days })

// 买方引导
export const listEngagementBuyers = (params: Record<string, unknown>) =>
  get('/v1/engagement/buyers', params)
export const getEngagementFunnel = () => get('/v1/engagement/funnel')
export const sendEngagementInvite = (engagementId: number) =>
  post('/v1/engagement/invite', {}, { params: { engagementId } })
export const getEngagementScripts = () => get('/v1/engagement/scripts')
export const getEngagementMaterials = () => get('/v1/engagement/materials')

// 数据安全
export const getSecurityStatus = () => get('/v1/security/status')
export const getSecurityAccessLog = (params?: Record<string, unknown>) =>
  get('/v1/security/access-log', params)
export const exportAllSecurityData = () => post('/v1/security/export-all')
export const getSecurityUsageReport = () => get('/v1/security/usage-report')

// 超时确认 / 通知模板 / 对账规则（企业级设置）
export const getTimeoutConfig = () => get('/v1/system/timeout-config')
export const saveTimeoutConfig = (data: Record<string, unknown>) =>
  put('/v1/system/timeout-config', data)
export const getNotificationTemplates = () => get('/v1/system/notification-templates')
export const saveNotificationTemplates = (data: Record<string, unknown>) =>
  put('/v1/system/notification-templates', data)
export const getReconRulesConfig = () => get('/v1/system/recon-rules')
export const saveReconRulesConfig = (data: Record<string, unknown>) =>
  put('/v1/system/recon-rules', data)

// 协议管理
export const checkAgreementStatus = () => get('/v1/agreements/check')
export const confirmAgreement = (data: { agreementVersionId: number; agreementType: number }) =>
  post('/v1/agreements/confirm', data)
export const getCurrentAgreement = (agreementType: number) =>
  get('/v1/agreements/current', { agreementType })
export const listAgreementVersions = (agreementType?: number) =>
  get('/v1/agreements/versions', { agreementType })
export const publishAgreement = (data: Record<string, unknown>) =>
  post('/v1/agreements/publish', data as object)
export const getAgreementVersion = (id: number) => get(`/v1/agreements/versions/${id}`)
export const getAgreementConfirmations = () => get('/v1/agreements/confirmations')
export const deprecateAgreementVersion = (id: number) => put(`/v1/agreements/versions/${id}/deprecate`)

// 企业数据授权
export const getDataAuthStatus = () => get('/v1/data-auth')
export const initializeDataAuth = () => post('/v1/data-auth/initialize')
export const updateDataAuth = (data: Record<string, unknown>) =>
  put('/v1/data-auth/update', data)
export const revokeDataAuth = (type: string) =>
  put('/v1/data-auth/revoke', undefined, { params: { authorizationType: type } })
export const checkDataAuth = () => get('/v1/data-auth/check')
export const listAuthChanges = () => get('/v1/data-auth/changes')
export const respondToAuthChange = (id: number, data: Record<string, unknown>) =>
  put(`/v1/data-auth/changes/${id}/respond`, data)
