import { defineStore } from 'pinia'
import { ref } from 'vue'
import { get, post } from '@/utils/request'

export interface UserInfo {
  id: number
  username: string
  realName: string
  enterpriseId: number
  roleType: number
  enterpriseName: string
}

interface LoginResponse {
  token?: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function clearToken() {
    token.value = ''
    localStorage.removeItem('token')
  }

  async function login(username: string, password: string) {
    const data = await post<LoginResponse>('/auth/login', { username, password })
    const tokenVal = (data as LoginResponse)?.token
    if (tokenVal) {
      setToken(tokenVal)
      return data
    }
    throw new Error('Login failed')
  }

  function logout() {
    clearToken()
    userInfo.value = null
  }

  async function getUserInfo() {
    const data = await get<{ data: UserInfo }>('/user/info')
    const info = data?.data
    if (info) {
      userInfo.value = info
      return info
    }
    return null
  }

  return {
    token,
    userInfo,
    login,
    logout,
    getUserInfo,
  }
})
