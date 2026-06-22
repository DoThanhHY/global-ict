import { onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import { useDeviceStore } from '../stores/device.store'
import SockJS from 'sockjs-client'

export function useWebSocket() {
  const deviceStore = useDeviceStore()
  let stompClient: Client | null = null

  onMounted(() => {
    stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        console.log('✅ WebSocket connected')

        // Subscribe real-time data từng device
        stompClient!.subscribe('/topic/devices/#', (message) => {
          const payload = JSON.parse(message.body)
          deviceStore.updateDeviceStatus(payload.deviceId, payload)
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