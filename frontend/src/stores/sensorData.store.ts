import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { SensorData } from '../utils'
import { sensorDataApi } from '../api/sensorData.api'

export const useSensorDataStore = defineStore('sensorData', () => {
  const history = ref<SensorData[]>([])
  const stats = ref<Record<string, number>>({})
  const loading = ref(false)

  async function fetchByDevice(deviceId: string, from?: string, to?: string) {
    loading.value = true
    try {
      history.value = await sensorDataApi.getByDevice(deviceId, 1000, from, to)
    } finally {
      loading.value = false
    }
  }

  async function fetchStats() {
    stats.value = await sensorDataApi.getStats()
  }

  // Prepend real-time data từ WebSocket
  function appendRealtime(data: SensorData) {
    history.value.unshift(data)
    if (history.value.length > 100) history.value.pop()
  }

  return { history, stats, loading, fetchByDevice, fetchStats, appendRealtime }
})