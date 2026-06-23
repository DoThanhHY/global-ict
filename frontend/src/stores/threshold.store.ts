import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { thresholdApi, type ThresholdAlert } from '../api/threshold.api'

export const useThresholdAlertStore = defineStore('thresholdAlert', () => {
  // State
  const alerts = ref<ThresholdAlert[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const unreadCount = computed(() => {
    return alerts.value.filter(a => !a.isResolved).length
  })

  const unresolvedAlerts = computed(() => {
    return alerts.value.filter(a => !a.isResolved).sort((a, b) => 
      new Date(b.triggeredAt).getTime() - new Date(a.triggeredAt).getTime()
    )
  })

  const alertsByDevice = computed(() => {
    const map = new Map<number, ThresholdAlert[]>()
    alerts.value.forEach(alert => {
      if (!map.has(alert.deviceId)) {
        map.set(alert.deviceId, [])
      }
      map.get(alert.deviceId)!.push(alert)
    })
    return map
  })

  // Actions
  async function fetchUnresolvedByDevice(deviceId: number) {
    loading.value = true
    error.value = null
    try {
      const data = await thresholdApi.getUnresolvedAlertsByDevice(deviceId)
      // Merge with existing alerts (avoid duplicates)
      const existing = alerts.value.filter(a => a.deviceId !== deviceId)
      alerts.value = [...existing, ...data]
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch alerts'
      console.error('Error fetching alerts:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchAlertsByDevice(deviceId: number) {
    loading.value = true
    error.value = null
    try {
      const data = await thresholdApi.getAlertsByDevice(deviceId)
      const existing = alerts.value.filter(a => a.deviceId !== deviceId)
      alerts.value = [...existing, ...data]
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch alerts'
      console.error('Error fetching alerts:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * Fetch ALL unresolved alerts (load từ DB lần đầu)
   */
  async function fetchAllUnresolvedAlerts() {
    loading.value = true
    error.value = null
    try {
      const data = await thresholdApi.getAllUnresolvedAlerts()
      alerts.value = data
      console.log(`✅ Loaded ${data.length} unresolved alerts`)
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to fetch all alerts'
      console.error('Error fetching all alerts:', err)
    } finally {
      loading.value = false
    }
  }

  /**
   * Add new alert from WebSocket
   */
  function addAlert(alert: ThresholdAlert) {
    // Check if alert already exists (by id)
    const exists = alerts.value.find(a => a.id === alert.id)
    if (!exists) {
      alerts.value.unshift(alert) // Add to beginning
      console.log(`🔔 New alert received: ${alert.fieldName} on ${alert.deviceName}`)
    }
  }

  /**
   * Resolve alert
   */
  async function resolveAlert(alertId: number) {
    try {
      const resolved = await thresholdApi.resolveAlert(alertId)
      const index = alerts.value.findIndex(a => a.id === alertId)
      if (index !== -1) {
        alerts.value[index] = resolved
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Failed to resolve alert'
      console.error('Error resolving alert:', err)
    }
  }

  /**
   * Clear all resolved alerts
   */
  function clearResolvedAlerts() {
    alerts.value = alerts.value.filter(a => !a.isResolved)
  }

  /**
   * Clear all alerts
   */
  function clearAll() {
    alerts.value = []
  }

  return {
    // State
    alerts,
    loading,
    error,
    // Getters
    unreadCount,
    unresolvedAlerts,
    alertsByDevice,
    // Actions
    fetchAllUnresolvedAlerts,
    fetchUnresolvedByDevice,
    fetchAlertsByDevice,
    addAlert,
    resolveAlert,
    clearResolvedAlerts,
    clearAll,
  }
})
