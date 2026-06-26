export interface Device {
  id: number
  deviceId: string
  name: string
  location: string
  type: 'TEMPERATURE_HUMIDITY' | 'DOOR_SENSOR' | 'SWITCH'
  online: boolean
  lastSeen: string
  createdAt: string
  latestData?: SensorPayload
}

export interface SensorPayload {
  deviceId: string
  temperature?: number
  humidity?: number
  open?: boolean
  on?: boolean
  timestamp: number
}

export interface SensorData {
  id: number
  device: Device
  temperature?: number
  humidity?: number
  doorOpen?: boolean
  switchOn?: boolean
  recordedAt: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

export interface RefreshRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
}

export interface CommandLog {
  id: number
  deviceId: string
  action: string
  sentBy: string
  sentAt: string
  status: string
  errorMessage?: string
}