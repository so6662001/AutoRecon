import { get, post, put, del } from '@/utils/request'

// 对账单
export const createBill = (data: Record<string, unknown>) => post('/v1/recon/bills', data)
export const queryBills = (params: Record<string, unknown>) => get('/v1/recon/bills', params)
export const getBillDetail = (id: number) => get(`/v1/recon/bills/${id}`)
export const updateBill = (id: number, data: Record<string, unknown>) =>
  put(`/v1/recon/bills/${id}`, data)
export const sendBill = (id: number) => put(`/v1/recon/bills/${id}/send`)
export const confirmBill = (id: number) => put(`/v1/recon/bills/${id}/confirm`)
export const voidBill = (id: number) => put(`/v1/recon/bills/${id}/void`)
export const generatePdf = (id: number) => post(`/v1/recon/bills/${id}/pdf`)

// 工作台
export const getDashboard = () => get('/v1/recon/dashboard')

// 模板
export const listTemplates = () => get('/v1/recon/templates')
export const getDefaultTemplate = () => get('/v1/recon/templates/default')
export const createTemplate = (data: Record<string, unknown>) => post('/v1/recon/templates', data)
export const updateTemplate = (id: number, data: Record<string, unknown>) =>
  put(`/v1/recon/templates/${id}`, data)
export const deleteTemplate = (id: number) => del(`/v1/recon/templates/${id}`)

// 比对
export const executeMatch = (billId: number) => post(`/v1/recon/match/${billId}/execute`)
export const getMatchResult = (billId: number) => get(`/v1/recon/match/${billId}/result`)
export const getDiffItems = (billId: number) => get(`/v1/recon/match/${billId}/diff`)
export const uploadExcelForBill = (billId: number, file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return post(`/v1/recon/data/upload-excel?billId=${billId}`, fd)
}

// 异议
export const createDispute = (data: Record<string, unknown>) => post('/v1/recon/disputes', data)
export const escalateDispute = (id: number) => put(`/v1/recon/disputes/${id}/escalate`)
export const listDisputes = (params: Record<string, unknown>) => get('/v1/recon/disputes', params)
export const getDisputeDetail = (id: number) => get(`/v1/recon/disputes/${id}`)
export const sendDisputeMessage = (id: number, data: Record<string, unknown>) =>
  post(`/v1/recon/disputes/${id}/messages`, data)
export const uploadDisputeAttachment = (id: number, file: File) => {
  const fd = new FormData()
  fd.append('file', file)
  return post<{ url?: string; attachmentUrl?: string }>(`/v1/recon/disputes/${id}/upload`, fd)
}
export const resolveDispute = (id: number, data: { resolution: string }) =>
  put(`/v1/recon/disputes/${id}/resolve`, data)
export const getDisputeMessages = (id: number) => get(`/v1/recon/disputes/${id}/messages`)

// 签章
export const initiateSign = (billId: number, signOrderType: number) =>
  post('/v1/recon/sign/flows', { billId, signOrderType })
export const executeSign = (signRecordId: number, sealId: number, verifyCode: string) =>
  post(`/v1/recon/sign/flows/${signRecordId}/sign`, undefined, { params: { sealId, verifyCode } })
export const getSignStatus = (billId: number) => get(`/v1/recon/sign/flows/${billId}/status`)
export const listPendingSigns = () => get('/v1/recon/sign/pending')
export const listSeals = () => get('/v1/recon/sign/seals')
export const createSeal = (data: Record<string, unknown>) => post('/v1/recon/sign/seals', data)
export const disableSeal = (id: number) => put(`/v1/recon/sign/seals/${id}/disable`)
export const enableSeal = (id: number) => put(`/v1/recon/sign/seals/${id}/enable`)
export const revokeSeal = (id: number) => del(`/v1/recon/sign/seals/${id}`)
export const listOperators = () => get('/v1/recon/sign/operators')
export const createOperator = (data: Record<string, unknown>) => post('/v1/recon/sign/operators', data)
export const updateOperator = (id: number, data: Record<string, unknown>) =>
  put(`/v1/recon/sign/operators/${id}`, data)
export const disableOperator = (id: number) => put(`/v1/recon/sign/operators/${id}/disable`)

// 免注册
export const guestViewBill = (token: string) => get(`/v1/guest/view/${token}`)
export const guestDownloadPdf = (token: string) =>
  get<Blob>(`/v1/guest/pdf/${token}`, undefined, { responseType: 'blob' })
export const guestConfirm = (token: string, data: Record<string, unknown>) =>
  post(`/v1/guest/confirm/${token}`, data)
export const verifyPhone = (data: { phone: string; code: string }) =>
  post('/v1/guest/verify-phone', data)
export const sendVerifyCode = (data: { phone: string }) =>
  post('/v1/guest/send-code', data)

// 付款
export const listPayments = (params: Record<string, unknown>) => get('/v1/recon/payments', params)
export const createPayment = (data: Record<string, unknown>) => post('/v1/recon/payments', data)

// Payment allocation
export const allocatePayment = (id: number, data: Record<string, unknown>) =>
  post(`/v1/recon/payments/${id}/allocate`, data)
export const autoAllocateFIFO = (id: number) =>
  post(`/v1/recon/payments/${id}/auto-allocate`, undefined, { params: { strategy: 1 } })
export const autoAllocateProportional = (id: number) =>
  post(`/v1/recon/payments/${id}/auto-allocate`, undefined, { params: { strategy: 3 } })
export const batchAutoAllocate = (strategy: number) =>
  post('/v1/recon/payments/auto-allocate', undefined, { params: { strategy } })
export const getPaymentBalance = (sellerId: number, buyerId: number) =>
  get('/v1/recon/payments/balance', { sellerId, buyerId })

// 催收
export const listCollectionPlans = (params: Record<string, unknown>) =>
  get('/v1/recon/collection/plans', params)
export const createCollectionPlan = (data: Record<string, unknown>) =>
  post('/v1/recon/collection/plans', data)
export const executePlan = (id: number, data: Record<string, unknown>) =>
  post(`/v1/recon/collection/plans/${id}/execute`, undefined, { params: data })
export const registerPaymentToCollection = (id: number, data: Record<string, unknown>) =>
  post(`/v1/recon/collection/plans/${id}/register-payment`, undefined, { params: data })
export const pausePlan = (id: number) => put(`/v1/recon/collection/plans/${id}/pause`)
export const resumePlan = (id: number) => put(`/v1/recon/collection/plans/${id}/resume`)
export const getCollectionLogs = (planId: number) =>
  get(`/v1/recon/collection/plans/${planId}/logs`)

// 信用评分
export const getCreditScore = (buyerId: number) => get(`/v1/recon/credit/${buyerId}`)
export const getCreditRanking = (limit: number) => get('/v1/recon/credit/ranking', { limit })
export const getCreditDetail = (buyerId: number) => get(`/v1/recon/credit/${buyerId}/detail`)
export const getCreditTrend = (buyerId: number) => get(`/v1/recon/credit/${buyerId}/trend`)
export const adjustCredit = (buyerId: number, data: Record<string, unknown>) =>
  put(`/v1/recon/credit/${buyerId}/adjust`, undefined, { params: data })

// 发票
export const createInvoice = (data: Record<string, unknown>) => post('/v1/recon/invoices', data)
export const listInvoices = (params: Record<string, unknown>) => get('/v1/recon/invoices', params)
export const linkInvoice = (data: Record<string, unknown>) => post('/v1/recon/invoices/link', data)
export const getInvoiceLinks = (billId: number) => get(`/v1/recon/invoices/links/${billId}`)

// 合同
export const listContracts = (params: Record<string, unknown>) => get('/v1/recon/contracts', params)
export const getContractSummary = (contractNo: string) =>
  get(`/v1/recon/contracts/${contractNo}/summary`)
export const getContractItems = (contractNo: string) =>
  get(`/v1/recon/contracts/${contractNo}/items`)

// 融资
export const applyFinance = (data: Record<string, unknown>) => post('/v1/recon/finance/apply', data)
export const listFinanceApplies = (params: Record<string, unknown>) =>
  get('/v1/recon/finance', params)
export const getEligibleBills = () => get('/v1/recon/finance/eligible-bills')

// 三单匹配
export const getTriMatch = (billId: number) => get(`/v1/recon/tri-match/${billId}`)

// 批量对账
export const batchCreate = (data: Record<string, unknown>) => post('/v1/recon/batch/create', data)
export const getBatchBills = (batchId: string) => get(`/v1/recon/batch/${batchId}/bills`)
