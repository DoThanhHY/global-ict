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