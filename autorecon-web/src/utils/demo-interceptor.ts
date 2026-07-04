import type { InternalAxiosRequestConfig } from 'axios'
import {
  DEMO_MODE,
  mockDashboard,
  mockBillList,
  mockCreditRanking,
  demoUsers,
} from '@/config/demo'

interface DemoMock {
  urlPattern: RegExp | string
  method?: string
  getData: (config: InternalAxiosRequestConfig, body?: unknown) => unknown
}

function normalizeUrl(url: string): string {
  return (url || '').replace(/^\//, '').split('?')[0]
}

function transformDashboardForFrontend() {
  const d = mockDashboard.data as Record<string, unknown>
  const recentTodos = (d.recentTodos as Array<{ billNo: string; buyerName: string; status: string; time: string }>) ?? []
  const todos = recentTodos.map((t, i) => ({
    id: 1000 + i,
    billNo: t.billNo,
    buyerName: t.buyerName,
    status: t.status,
    updatedAt: t.time,
  }))
  return {
    code: 200,
    data: {
      ...d,
      todos,
      completionRate: d.monthlyCompletionRate ?? d.completionRate ?? 78,
    },
  }
}

const demoMocks: DemoMock[] = [
  {
    urlPattern: 'v1/recon/dashboard',
    method: 'get',
    getData: () => transformDashboardForFrontend(),
  },
  {
    urlPattern: 'v1/recon/bills',
    method: 'get',
    getData: () => mockBillList,
  },
  {
    urlPattern: /v1\/recon\/credit\/ranking/,
    method: 'get',
    getData: () => mockCreditRanking,
  },
  {
    urlPattern: 'auth/login',
    method: 'post',
    getData: (config, body?: unknown) => {
      const reqBody = (body ?? (config as { data?: unknown }).data) ?? {}
      const { username, password } = reqBody as { username?: string; password?: string }
      const user = demoUsers.find((u) => u.username === username && u.password === password)
      if (user) {
        return {
          code: 200,
          token: `demo-token-${user.username}`,
          data: { ...user },
        }
      }
      return { code: 401, message: 'Invalid credentials' }
    },
  },
]

export function getDemoMock(
  url: string,
  method: string,
  config: InternalAxiosRequestConfig
): unknown | null {
  if (!DEMO_MODE) return null

  const normalizedUrl = normalizeUrl(url)
  const body = (config as unknown as { data?: unknown }).data

  for (const mock of demoMocks) {
    const methodMatch = !mock.method || mock.method.toLowerCase() === method.toLowerCase()
    const urlMatch =
      typeof mock.urlPattern === 'string'
        ? normalizedUrl === mock.urlPattern || normalizedUrl.includes(mock.urlPattern)
        : (mock.urlPattern as RegExp).test(normalizedUrl)

    if (methodMatch && urlMatch) {
      const data = mock.getData(config, body)
      return data
    }
  }
  return null
}
