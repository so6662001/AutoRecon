import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { AxiosInstance, InternalAxiosRequestConfig } from 'axios'

const safeMessages: Record<number, string> = {
  400: '请求参数有误',
  401: '登录已过期，请重新登录',
  403: '没有操作权限',
  404: '请求的资源不存在',
  500: '服务器内部错误，请稍后重试',
  429: '操作过于频繁，请稍后重试',
}

function getBaseURL(): string {
  return import.meta.env.VITE_API_BASE_URL || '/api'
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
      const isBusinessError = typeof code === 'number' && code >= 10000 && code <= 50000
      const displayMsg = isBusinessError ? (message || '操作失败') : '操作失败'
      ElMessage.error(displayMsg)
      return Promise.reject(new Error(displayMsg))
    }
    return response.data
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    } else {
      const safeMsg = status != null ? safeMessages[status] : undefined
      ElMessage.error(safeMsg ?? '操作失败，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export const get = <T = unknown>(url: string, params?: object, config?: object): Promise<T> =>
  instance.get(url, { params, ...config }) as Promise<T>
export const post = <T = unknown>(url: string, data?: object | FormData | null, config?: object): Promise<T> =>
  instance.post(url, data, config) as Promise<T>
export const put = <T = unknown>(url: string, data?: object | null, config?: object): Promise<T> =>
  instance.put(url, data, config) as Promise<T>
export const del = <T = unknown>(url: string, params?: object): Promise<T> =>
  instance.delete(url, { params }) as Promise<T>
