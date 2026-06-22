<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useSensorDataStore } from '../stores/sensorData.store'
import { useDeviceStore } from '../stores/device.store'

const route = useRoute()
const deviceId = route.params.deviceId as string

const deviceStore = useDeviceStore()
const sensorStore = useSensorDataStore()

const device = computed(() =>
  deviceStore.devices.find(d => d.deviceId === deviceId)
)

onMounted(async () => {
  if (!deviceStore.devices.length) await deviceStore.fetchAll()
  await sensorStore.fetchByDevice(deviceId)
})
</script>

<template>
  <div class="p-6">
    <router-link to="/devices" class="text-blue-500 text-sm">← Quay lại</router-link>

    <div v-if="device" class="mt-4">
      <div class="flex items-center gap-3 mb-6">
        <h1 class="text-2xl font-bold">{{ device.name }}</h1>
        <span
          class="text-xs px-2 py-1 rounded-full"
          :class="device.online ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-500'"
        >
          {{ device.online ? 'Online' : 'Offline' }}
        </span>
      </div>

      <!-- Info -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="bg-white rounded-xl p-3 shadow text-sm">
          <p class="text-gray-400">Device ID</p>
          <p class="font-mono font-semibold">{{ device.deviceId }}</p>
        </div>
        <div class="bg-white rounded-xl p-3 shadow text-sm">
          <p class="text-gray-400">Loại</p>
          <p class="font-semibold">{{ device.type }}</p>
        </div>
        <div class="bg-white rounded-xl p-3 shadow text-sm">
          <p class="text-gray-400">Vị trí</p>
          <p class="font-semibold">{{ device.location || '—' }}</p>
        </div>
        <div class="bg-white rounded-xl p-3 shadow text-sm">
          <p class="text-gray-400">Lần cuối online</p>
          <p class="font-semibold">{{ device.lastSeen ? new Date(device.lastSeen).toLocaleString('vi-VN') : '—' }}</p>
        </div>
      </div>

      <!-- Charts (chỉ hiện với TEMPERATURE_HUMIDITY) -->
      <template v-if="device.type === 'TEMPERATURE_HUMIDITY' && sensorStore.history.length">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <SensorChart :data="sensorStore.history" field="temperature" />
          <SensorChart :data="sensorStore.history" field="humidity" />
        </div>
      </template>

      <!-- History table -->
      <div class="bg-white rounded-2xl shadow overflow-hidden">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-gray-500">
            <tr>
              <th class="text-left px-4 py-3">Thời gian</th>
              <th class="text-left px-4 py-3">Dữ liệu</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in sensorStore.history"
              :key="row.id"
              class="border-t hover:bg-gray-50"
            >
              <td class="px-4 py-3 text-gray-500">
                {{ new Date(row.recordedAt).toLocaleString('vi-VN') }}
              </td>
              <td class="px-4 py-3">
                <span v-if="row.temperature != null">🌡 {{ row.temperature }}°C  💧 {{ row.humidity }}%</span>
                <span v-else-if="row.doorOpen != null">{{ row.doorOpen ? '🔓 Mở' : '🔒 Đóng' }}</span>
                <span v-else-if="row.switchOn != null">{{ row.switchOn ? '✅ Bật' : '⭕ Tắt' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-else class="text-gray-400">Không tìm thấy thiết bị.</div>
  </div>
</template>