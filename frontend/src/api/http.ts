import axios from 'axios'
import { useAuthStore } from '../stores/auth.store'

export const http = axios.create({
  baseURL: 'http://localhost:8080/api',
})

// Request interceptor — gắn token vào mọi request
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor — token hết hạn thì logout
http.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
    }
    return Promise.reject(err)
  }
)