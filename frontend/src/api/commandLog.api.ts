import type { CommandLog } from '../utils'
import { http } from './http'

export const commandLogApi = {
  getAll: () => http.get<CommandLog[]>('/command-logs').then(r => r.data),
  getByDevice: (deviceId: string) => http.get<CommandLog[]>(`/command-logs/device/${deviceId}`).then(r => r.data),
}
