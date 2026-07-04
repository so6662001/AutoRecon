import { DEMO_MODE, demoUsers, mockDashboard, mockContracts } from '@/config/demo'

function getMockLoginResponse(body?: { username?: string; password?: string }) {
  const username = body?.username ?? ''
  const password = body?.password ?? ''
  const user = demoUsers.find((u) => u.username === username && u.password === password)
  if (user) {
    const roleMap: Record<string, { id: number; roleType: number; enterpriseName: string }> = {
      admin: { id: 1, roleType: 0, enterpriseName: '平台' },
      seller1: { id: 2, roleType: 1, enterpriseName: 'XX钢铁' },
      buyer1: { id: 3, roleType: 2, enterpriseName: 'YY建设' },
      buyer2: { id: 4, roleType: 2, enterpriseName: 'ZZ贸易' },
    }
    const r = roleMap[user.username] ?? { id: 5, roleType: 2, enterpriseName: '其他' }
    return {
      code: 200,
      token: `demo-token-${user.username}`,
      data: {
        id: r.id,
        username: user.username,
        realName: user.realName,
        enterpriseId: 1,
        roleType: r.roleType,
        enterpriseName: r.enterpriseName,
      },
    }
  }
  return { code: 401, message: 'Invalid credentials' }
}

export function setupDemoInterceptor(instance: any) {
  if (!DEMO_MODE) return

  instance.interceptors.response.use(
    (response: any) => response,
    (error: any) => {
      const status = error.response?.status
      if (status === 401 || status === 403) return Promise.reject(error)

      const url = error.config?.url || ''
      const method = (error.config?.method || 'get').toLowerCase()

      if (url.includes('/auth/login') && method === 'post') {
        const body = error.config?.data
        const parsed = typeof body === 'string' ? (() => { try { return JSON.parse(body) } catch { return {} } })() : (body ?? {})
        const mock = getMockLoginResponse(parsed)
        if (mock.code === 200) return Promise.resolve({ data: mock })
        return Promise.reject(error)
      }
      if (url.includes('/user/info') || url.includes('/userinfo')) {
        const token = localStorage.getItem('token') || ''
        const username = token.replace('demo-token-', '')
        const user = demoUsers.find((u) => u.username === username)
        if (user) {
          const roleMap: Record<string, { id: number; roleType: number; enterpriseName: string }> = {
            admin: { id: 1, roleType: 0, enterpriseName: '平台' },
            seller1: { id: 2, roleType: 1, enterpriseName: 'XX钢铁' },
            buyer1: { id: 3, roleType: 2, enterpriseName: 'YY建设' },
            buyer2: { id: 4, roleType: 2, enterpriseName: 'ZZ贸易' },
          }
          const r = roleMap[user.username] ?? { id: 5, roleType: 2, enterpriseName: '其他' }
          const mock = {
            code: 200,
            data: {
              id: r.id,
              username: user.username,
              realName: user.realName,
              enterpriseId: 1,
              roleType: r.roleType,
              enterpriseName: r.enterpriseName,
            },
          }
          return Promise.resolve({ data: mock })
        }
      }
      if (url.includes('/dashboard')) return Promise.resolve({ data: mockDashboard })
      if (url.includes('/contracts') && !url.includes('/contracts/')) return Promise.resolve({ data: mockContracts })

      return Promise.reject(error)
    }
  )
}
