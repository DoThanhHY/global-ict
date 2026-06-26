import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginRequest } from '../utils'
import { authApi } from '../api/auth.api'
import { router } from '../router'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const username = ref<string | null>(localStorage.getItem('username'))

  const isAuthenticated = computed(() => !!accessToken.value)

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    accessToken.value = res.accessToken
    refreshToken.value = res.refreshToken
    username.value = res.username
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    localStorage.setItem('username', res.username)
    router.push('/')
  }

  async function logout() {
    try {
      if (refreshToken.value) {
        await authApi.logout({ refreshToken: refreshToken.value })
      }
    } catch {
      // ignore — vẫn logout local dù api fail
    }
    accessToken.value = null
    refreshToken.value = null
    username.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('username')
    router.push('/login')
  }

  return { accessToken, refreshToken, username, isAuthenticated, login, logout }
})