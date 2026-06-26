import { onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useDeviceStore } from '../stores/device.store'
import { useThresholdAlertStore } from '../stores/threshold.store'
import { useAuthStore } from '../stores/auth.store'
import SockJS from 'sockjs-client'
import type { ThresholdAlert } from '../api/threshold.api'

export function useWebSocket() {
  const deviceStore = useDeviceStore()
  const alertStore = useThresholdAlertStore()
  const authStore = useAuthStore()
  let stompClient: Client | null = null

  onMounted(() => {
    stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${authStore.accessToken}`,
      },
      onConnect: () => {
        console.log('✅ WebSocket connected')

        // Fetch all unresolved alerts từ DB (lần đầu load)
        alertStore.fetchAllUnresolvedAlerts().catch(err => {
          console.error('Failed to fetch initial alerts:', err)
        })

        // Subscribe real-time sensor data từng device
        stompClient!.subscribe('/topic/devices/#', (message) => {
          const payload = JSON.parse(message.body)
          deviceStore.updateDeviceStatus(payload.deviceId, payload)
        })

        // Subscribe threshold alerts
        stompClient!.subscribe('/topic/alerts/#', (message) => {
          console.log("alert in stomp client", message);
          
          const alert = JSON.parse(message.body) as ThresholdAlert
          alertStore.addAlert(alert)
          console.log(`🚨 Threshold alert received: ${alert.fieldName} on ${alert.deviceName}`)
        })
      },
      onDisconnect: () => console.log('WebSocket disconnected'),
    })

    stompClient.activate()
  })

  onUnmounted(() => {
    stompClient?.deactivate()
  })
}