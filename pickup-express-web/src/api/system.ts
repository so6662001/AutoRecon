import { get, post, put } from '@/utils/request'

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
