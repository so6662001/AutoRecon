import { get, post, put } from '@/utils/request'

// 对账单
export const createBill = (data: Record<string, unknown>) => post('/v1/recon/bills', data)
export const queryBills = (params: Record<string, unknown>) => get('/v1/recon/bills', params)
export const getBillDetail = (id: number) => get(`/v1/recon/bills/${id}`)
export const sendBill = (id: number) => put(`/v1/recon/bills/${id}/send`)
export const confirmBill = (id: number) => put(`/v1/recon/bills/${id}/confirm`)
export const voidBill = (id: number) => put(`/v1/recon/bills/${id}/void`)
export const generatePdf = (id: number) => post(`/v1/recon/bills/${id}/pdf`)

// 工作台
export const getDashboard = () => get('/v1/recon/dashboard')

// 模板
export const listTemplates = () => get('/v1/recon/templates')
export const getDefaultTemplate = () => get('/v1/recon/templates/default')

// 比对
export const executeMatch = (billId: number) => post(`/v1/recon/match/${billId}/execute`)
export const getMatchResult = (billId: number) => get(`/v1/recon/match/${billId}/result`)
export const getDiffItems = (billId: number) => get(`/v1/recon/match/${billId}/diff`)

// 异议
export const createDispute = (data: Record<string, unknown>) => post('/v1/recon/disputes', data)
export const listDisputes = (billId: number) => get('/v1/recon/disputes', { billId })
export const getDisputeDetail = (id: number) => get(`/v1/recon/disputes/${id}`)
export const sendDisputeMessage = (id: number, data: Record<string, unknown>) =>
  post(`/v1/recon/disputes/${id}/messages`, data)
export const resolveDispute = (id: number, data: { resolution: string }) =>
  put(`/v1/recon/disputes/${id}/resolve`, data)
export const getDisputeMessages = (id: number) => get(`/v1/recon/disputes/${id}/messages`)

// 签章
export const initiateSign = (billId: number, signOrderType: number) =>
  post('/v1/recon/sign/flows', { billId, signOrderType })
export const getSignStatus = (billId: number) => get(`/v1/recon/sign/flows/${billId}/status`)
export const listPendingSigns = () => get('/v1/recon/sign/pending')
export const listSeals = () => get('/v1/recon/sign/seals')
export const createSeal = (data: Record<string, unknown>) => post('/v1/recon/sign/seals', data)
export const disableSeal = (id: number) => put(`/v1/recon/sign/seals/${id}/disable`)
export const enableSeal = (id: number) => put(`/v1/recon/sign/seals/${id}/enable`)
export const listOperators = () => get('/v1/recon/sign/operators')
export const createOperator = (data: Record<string, unknown>) => post('/v1/recon/sign/operators', data)

// 免注册
export const guestViewBill = (token: string) => get(`/v1/guest/view/${token}`)
export const guestConfirm = (token: string, data: Record<string, unknown>) =>
  post(`/v1/guest/confirm/${token}`, data)
export const verifyPhone = (data: { phone: string; code: string }) =>
  post('/v1/guest/verify-phone', data)
export const sendVerifyCode = (data: { phone: string }) =>
  post('/v1/guest/send-code', data)

// 付款
export const listPayments = (params: Record<string, unknown>) => get('/v1/recon/payments', params)
export const createPayment = (data: Record<string, unknown>) => post('/v1/recon/payments', data)

// 催收
export const listCollectionPlans = (params: Record<string, unknown>) =>
  get('/v1/recon/collection/plans', params)

// 信用评分
export const getCreditScore = (buyerId: number) => get(`/v1/recon/credit/${buyerId}`)
export const getCreditRanking = (limit: number) => get('/v1/recon/credit/ranking', { limit })
