import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { getEmbedConfig } from '@/config/embed'
import { getDemoMock } from '@/utils/demo-interceptor'

function getBaseURL(): string {
  return getEmbedConfig().apiBaseUrl || import.meta.env.VITE_API_BASE_URL || '/api'
}

const instance: AxiosInstance = axios.create({
  baseURL: getBaseURL(),
  timeout: 10000,
})

instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  config.baseURL = getBaseURL()
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => {
    const { code, message } = response.data ?? {}
    if (code !== undefined && code !== 200) {
      ElMessage.error(message || 'Request failed')
      return Promise.reject(new Error(message || 'Request failed'))
    }
    return response.data
  },
  (error) => {
    const config = getEmbedConfig()
    const mockData = getDemoMock(
      error.config?.url ?? '',
      error.config?.method ?? 'get',
      error.config as InternalAxiosRequestConfig
    )
    if (mockData) {
      return Promise.resolve(mockData)
    }
    if (error.response?.status === 401) {
      if (config.embedded && config.onError) {
        config.onError(error)
      } else {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }
    } else {
      ElMessage.error(error.response?.data?.message || error.message || 'Request failed')
    }
    return Promise.reject(error)
  }
)

export const get = <T = unknown>(url: string, params?: object, config?: object): Promise<T> =>
  instance.get(url, { params, ...config }) as Promise<T>
export const post = <T = unknown>(url: string, data?: object, config?: object): Promise<T> =>
  instance.post(url, data, config) as Promise<T>
export const put = <T = unknown>(url: string, data?: object, config?: object): Promise<T> =>
  instance.put(url, data, config) as Promise<T>
export const del = <T = unknown>(url: string, params?: object): Promise<T> =>
  instance.delete(url, { params }) as Promise<T>
