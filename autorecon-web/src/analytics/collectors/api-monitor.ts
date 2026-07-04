import type { AxiosResponse, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { EventType } from '../types'
import type { Tracker } from '../core/tracker'

const ID_PATTERN = /\/\d+/g
const UUID_PATTERN = /\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi

interface TimedConfig extends InternalAxiosRequestConfig {
  _startTime?: number
}

function sanitizeUrl(url: string): string {
  return url
    .replace(UUID_PATTERN, '/:id')
    .replace(ID_PATTERN, '/:id')
}

function isAnalyticsEndpoint(url: string): boolean {
  return url.includes('/analytics/') || url.includes('/collect')
}

export function createApiMonitorInterceptor(tracker: Tracker) {
  const requestInterceptor = (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig => {
    (config as TimedConfig)._startTime = Date.now()
    return config
  }

  const responseInterceptor = (response: AxiosResponse): AxiosResponse => {
    const config = response.config as TimedConfig
    const url = config.url || ''

    if (isAnalyticsEndpoint(url)) return response

    const duration = config._startTime ? Date.now() - config._startTime : 0

    tracker.track(EventType.API_CALL, 'api_call', {
      network: {
        url: sanitizeUrl(url),
        method: (config.method || 'GET').toUpperCase(),
        status: response.status,
        duration,
        success: true,
      },
    })

    return response
  }

  const errorInterceptor = (error: AxiosError): Promise<never> => {
    const config = error.config as TimedConfig | undefined
    const url = config?.url || ''

    if (!isAnalyticsEndpoint(url)) {
      const duration = config?._startTime ? Date.now() - config._startTime : 0

      tracker.track(EventType.API_CALL, 'api_error', {
        network: {
          url: sanitizeUrl(url),
          method: (config?.method || 'GET').toUpperCase(),
          status: error.response?.status || 0,
          duration,
          success: false,
        },
      })
    }

    return Promise.reject(error)
  }

  return {
    request: requestInterceptor,
    response: responseInterceptor,
    responseError: errorInterceptor,
  }
}
