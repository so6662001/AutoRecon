export const EventType = {
  PAGE_VIEW: 'page_view',
  PAGE_LEAVE: 'page_leave',
  CLICK: 'click',
  ACTION: 'action',
  SEARCH: 'search',
  API_CALL: 'api_call',
  PERFORMANCE: 'performance',
  ERROR: 'error',
  EXPOSURE: 'exposure',
  SCROLL_DEPTH: 'scroll_depth',
  TAB_SWITCH: 'tab_switch',
  FORM_SUBMIT: 'form_submit',
} as const

export type EventType = (typeof EventType)[keyof typeof EventType]

export interface AnalyticsConfig {
  system: string
  endpoint: string
  batchSize: number
  flushInterval: number
  sampleRate: number
  enablePerformance: boolean
  enableApiMonitor: boolean
  enableErrorCapture: boolean
  enableScrollDepth: boolean
  enableExposure: boolean
  debug: boolean
  privacyMode: boolean | {
    maskPhone?: boolean
    noInputCapture?: boolean
    amountAsRange?: boolean
  }
}

export interface SessionInfo {
  sessionId: string
  startTime: number
  pageCount: number
}

export interface UserInfo {
  userId: string
  enterpriseId?: string
  roleType?: string
  isGuest?: boolean
}

export interface DeviceInfo {
  deviceId: string
  platform: string
  os: string
  browser: string
  screenWidth: number
  screenHeight: number
  isMobile: boolean
  userAgent: string
}

export interface PageInfo {
  path: string
  name?: string
  title?: string
  module?: string
  referrer?: string
  duration?: number
  isFirstVisit?: boolean
}

export interface ActionInfo {
  element?: string
  elementId?: string
  elementText?: string
  category?: string
  label?: string
  value?: number
  extra?: Record<string, unknown>
}

export interface PerformanceInfo {
  fcp?: number
  lcp?: number
  fid?: number
  cls?: number
  ttfb?: number
  domReady?: number
  loadComplete?: number
}

export interface NetworkInfo {
  url?: string
  method?: string
  status?: number
  duration?: number
  success?: boolean
}

export interface ApiInfo {
  url?: string
  method?: string
  status?: number
  duration?: number
  errorMessage?: string
}

export interface ErrorInfo {
  message: string
  filename?: string
  lineno?: number
  colno?: number
  stack?: string
}

export interface AnalyticsEvent {
  eventId: string
  eventType: EventType
  eventName: string
  timestamp: number
  system: string
  session: SessionInfo
  user?: UserInfo | null
  device: DeviceInfo
  page?: Partial<PageInfo> & { query?: Record<string, unknown> }
  action?: Partial<ActionInfo>
  performance?: Partial<PerformanceInfo>
  network?: Partial<NetworkInfo>
  api?: Partial<ApiInfo>
  error?: Partial<ErrorInfo>
  duration?: number
}
