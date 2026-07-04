import { get } from '@/utils/request'

export const getRealtimeOverview = (system?: string) =>
  get('/v1/analytics/realtime/overview', { system })

export const getHotPages = (system?: string, topN = 10) =>
  get('/v1/analytics/realtime/hot-pages', { system, topN })

export const getPageRanking = (params: { system?: string; startDate?: string; endDate?: string; module?: string }) =>
  get('/v1/analytics/pages/ranking', params)

export const getActionStats = (params: { system?: string; startDate?: string; endDate?: string }) =>
  get('/v1/analytics/actions/stats', params)

export const getFunnelData = (id: number, params: { startDate?: string; endDate?: string }) =>
  get(`/v1/analytics/funnel/${id}/data`, params)

export const getRetentionData = (params: { system?: string; startDate?: string; days?: number }) =>
  get('/v1/analytics/retention', params)

export const getDeviceDistribution = (params: { system?: string; startDate?: string; endDate?: string }) =>
  get('/v1/analytics/device/distribution', params)

export const getPerformanceOverview = (params: { system?: string; startDate?: string; endDate?: string }) =>
  get('/v1/analytics/performance/overview', params)

export const getFeatureAdoption = (params: { system?: string; startDate?: string; endDate?: string }) =>
  get('/v1/analytics/features/adoption', params)

export const getUserSegments = (params: { system?: string }) =>
  get('/v1/analytics/segments', params)

export const getUserPaths = (params: { system?: string; startPage?: string; endPage?: string; startDate?: string; endDate?: string }) =>
  get('/v1/analytics/user-paths', params)

export const getEventList = (params: { system?: string; eventType?: string }) =>
  get('/v1/analytics/events', params)

export const getSegmentUsers = (segmentId: string, params: { page?: number; pageSize?: number }) =>
  get(`/v1/analytics/segments/${segmentId}/users`, params)
