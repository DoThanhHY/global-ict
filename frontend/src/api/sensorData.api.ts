// import axios from 'axios'
import type { SensorData } from '../utils'
import { http } from '../api/http'

export const sensorDataApi = {
  getByDevice: (deviceId: string, limit = 50) =>
    http.get<SensorData[]>(`/sensor-data/device/${deviceId}`, {
      params: { limit }
    }).then(r => r.data),

  getStats: () =>
    http.get<Record<string, number>>('/sensor-data/stats').then(r => r.data),
}