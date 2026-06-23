// import axios from 'axios'
import type { AuthResponse, LoginRequest } from '../utils'
import { http } from '../api/http'

export const authApi = {
  login: (data: LoginRequest) =>
    http.post<AuthResponse>('/auth/login', data).then(r => r.data),

  register: (data: LoginRequest) =>
    http.post('/auth/register', data),

  logout: () =>
    http.post('/auth/logout'),
}