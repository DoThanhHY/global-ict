// import axios from 'axios'
import type { AuthResponse, LoginRequest, LogoutRequest } from '../utils'
import { http } from '../api/http'

export const authApi = {
  login: (data: LoginRequest) =>
    http.post<AuthResponse>('/auth/login', data).then(r => r.data),

  register: (data: LoginRequest) =>
    http.post('/auth/register', data),

  logout: (data: LogoutRequest) =>
    http.post('/auth/logout', data),

  refresh: (refreshToken: string) =>
    http.post<AuthResponse>('/auth/refresh', { refreshToken }).then(r => r.data),
}