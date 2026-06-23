import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginRequest } from '../utils'
import { authApi } from '../api/auth.api'
import { router } from '../router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const username = ref<string | null>(localStorage.getItem('username'))

  const isAuthenticated = computed(() => !!token.value)

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    token.value = res.token
    username.value = res.username
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', res.username)
    router.push('/')
  }

  function logout() {
    token.value = null
    username.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    router.push('/login')
  }

  return { token, username, isAuthenticated, login, logout }
})