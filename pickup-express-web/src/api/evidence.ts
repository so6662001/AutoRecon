import { get, post, put, del } from '@/utils/request'

// 合同
export const queryContracts = (params: any) => get('/v1/evidence/contracts', params)
export const getContractDetail = (id: number) => get(`/v1/evidence/contracts/${id}`)
export const initiateSign = (id: number) => post(`/v1/evidence/contracts/${id}/sign-request`)
export const customerSign = (id: number) => post(`/v1/evidence/contracts/${id}/sign`)
export const getContractProgress = (id: number) => get(`/v1/evidence/contracts/${id}/progress`)

// 派车
export const requestDispatch = (data: any) => post('/v1/evidence/dispatch/request', data)
export const confirmDispatch = (id: number, confirmed: boolean) =>
  put(`/v1/evidence/dispatch/${id}/confirm`, { pickupOrderId: id, confirmed })
export const assignDriver = (data: any) =>
  put(`/v1/evidence/dispatch/${data.pickupOrderId}/assign-driver`, data)

// 提货单
export const queryPickupOrders = (params: any) => get('/v1/evidence/pickup', params)
export const getPickupOrderDetail = (id: number) => get(`/v1/evidence/pickup/${id}`)
export const cancelPickupOrder = (id: number) => put(`/v1/evidence/pickup/${id}/cancel`)
export const getPickupQrcode = (id: number) => get(`/v1/evidence/pickup/${id}/qrcode`)

// 驾驶员
export const getDriverOrders = (phone: string) =>
  get('/v1/evidence/driver/orders', { driverPhone: phone })
export const driverAccept = (id: number) => put(`/v1/evidence/driver/orders/${id}/accept`)
export const driverArrive = (id: number, lat: number, lng: number) =>
  put(`/v1/evidence/driver/orders/${id}/arrive`, null, { params: { lat, lng } })
export const getDeliveryProgress = (id: number) =>
  get(`/v1/evidence/driver/orders/${id}/progress`)

// 发货
export const verifyPickupCode = (code: string, plate: string) =>
  post('/v1/evidence/delivery/verify-code', null, {
    params: { pickupCode: code, vehiclePlate: plate },
  })
export const uploadLift = (data: any) => post('/v1/evidence/delivery/lift-upload', data)
export const completeDelivery = (data: any) => post('/v1/evidence/delivery/complete', data)
export const uploadDeliveryPhoto = (data: FormData) =>
  post('/v1/evidence/delivery/photo-upload', data)

// 结算
export const generateSettlement = (pickupOrderId: number) =>
  post('/v1/evidence/settlement/generate', { pickupOrderId })
export const getSettlement = (id: number) => get(`/v1/evidence/settlement/${id}`)
export const markSettlementViewed = (id: number) =>
  put(`/v1/evidence/settlement/${id}/viewed`)
export const listSettlementsByContract = (contractId: number) =>
  get(`/v1/evidence/settlement/contract/${contractId}`)
export const listSettlements = (params: any) => get('/v1/evidence/settlement/list', params)

// 证据
export const archiveEvidence = (pickupOrderId: number) =>
  post(`/v1/evidence/archive/${pickupOrderId}`)
export const getEvidencePackage = (pickupOrderId: number) =>
  get(`/v1/evidence/archive/${pickupOrderId}`)
export const verifyEvidence = (pickupOrderId: number) =>
  get(`/v1/evidence/archive/${pickupOrderId}/verify`)

// 确权
export const verifyPickup = (data: any) =>
  post('/v1/evidence/verification/verify', null, { params: data })
export const listVerificationRecords = (params?: any) =>
  get('/v1/evidence/verification/records', params)

// 授权提货人
export const registerPickupPerson = (data: any) => post('/v1/evidence/authorized-persons', data)
export const listPickupPersons = (params?: { buyerId?: number; buyerName?: string }) =>
  get('/v1/evidence/authorized-persons', params)
export const confirmPickupPerson = (id: number) =>
  put(`/v1/evidence/authorized-persons/${id}/confirm`)
export const disablePickupPerson = (id: number) =>
  put(`/v1/evidence/authorized-persons/${id}/disable`)

// 仓库
export const listWarehouses = () => get('/v1/evidence/warehouse')
export const createWarehouse = (data: any) => post('/v1/evidence/warehouse', data)
export const updateWarehouse = (id: number, data: any) =>
  put(`/v1/evidence/warehouse/${id}`, data)

// 承运公司
export const listCarriers = () => get('/v1/evidence/carrier')
export const createCarrier = (data: any) => post('/v1/evidence/carrier', data)
export const updateCarrier = (id: number, data: any) => put(`/v1/evidence/carrier/${id}`, data)

// 补录
export const createSupplement = (data: any) => post('/v1/evidence/supplement', data)
export const getSupplement = (id: number) => get(`/v1/evidence/supplement/${id}`)
export const approveSupplement = (id: number, comment: string) =>
  put(`/v1/evidence/supplement/${id}/approve`, null, { params: { comment } })
export const rejectSupplement = (id: number, comment: string) =>
  put(`/v1/evidence/supplement/${id}/reject`, null, { params: { comment } })
export const listPendingSupplements = () => get('/v1/evidence/supplement/pending')

// 模板
export const listTemplates = (contractType?: number) =>
  get('/v1/evidence/templates', { contractType })
export const createTemplate = (data: any) => post('/v1/evidence/templates', data)
export const updateTemplate = (id: number, data: any) =>
  put(`/v1/evidence/templates/${id}`, data)
export const deleteTemplate = (id: number) => del(`/v1/evidence/templates/${id}`)

// 时效配置
export const getTimeoutConfig = (params: any) => get('/v1/evidence/timeout-config', params)
export const saveTimeoutConfig = (data: any) => post('/v1/evidence/timeout-config', data)

// 进度
export const getPickupTimeline = (pickupOrderId: number) =>
  get(`/v1/evidence/progress/pickup/${pickupOrderId}`)
export const getContractTimeline = (contractId: number) =>
  get(`/v1/evidence/progress/contract/${contractId}`)

// 工作台
export const getDashboard = () => get('/v1/evidence/dashboard')

// 交易习惯
export const listTradingHabits = (params?: { buyerId?: number; buyerName?: string }) =>
  get('/v1/evidence/trading-habits', params)
export const getTradingHabitReport = (buyerId: number) =>
  get(`/v1/evidence/trading-habits/${buyerId}/report`)

// 客户/买方
export const queryBuyers = (params?: any) => get('/v1/evidence/buyers', params)

// 证据包下载
export const downloadEvidenceZip = (pickupOrderId: number) =>
  get<Blob>(`/v1/evidence/archive/${pickupOrderId}/download`, undefined, { responseType: 'blob' })
