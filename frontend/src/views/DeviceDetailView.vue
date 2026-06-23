<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useSensorDataStore } from '../stores/sensorData.store'
import { useDeviceStore } from '../stores/device.store'
import SensorChart from '../components/SensorChart.vue'
import ThresholdSettings from '../components/ThresholdSettings.vue'

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
    <router-link to="/devices" class="text-blue-500 text-sm hover:underline">← Quay lại</router-link>

    <div v-if="device" class="mt-4">
      <!-- Header -->
      <div class="flex items-center gap-3 mb-6">
        <h1 class="text-2xl font-bold">{{ device.name }}</h1>
        <span
          class="text-xs px-2 py-1 rounded-full font-semibold"
          :class="device.online ? 'bg-green-100 text-green-600' : 'bg-red-100 text-red-600'"
        >
          {{ device.online ? '🟢 Online' : '🔴 Offline' }}
        </span>
      </div>

      <!-- Info Grid -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div class="bg-white rounded-lg p-4 shadow border border-gray-200">
          <p class="text-gray-500 text-xs font-semibold uppercase">Device ID</p>
          <p class="font-mono font-semibold text-gray-900 mt-1">{{ device.deviceId }}</p>
        </div>
        <div class="bg-white rounded-lg p-4 shadow border border-gray-200">
          <p class="text-gray-500 text-xs font-semibold uppercase">Loại</p>
          <p class="font-semibold text-gray-900 mt-1">{{ device.type }}</p>
        </div>
        <div class="bg-white rounded-lg p-4 shadow border border-gray-200">
          <p class="text-gray-500 text-xs font-semibold uppercase">Vị trí</p>
          <p class="font-semibold text-gray-900 mt-1">{{ device.location || '—' }}</p>
        </div>
        <div class="bg-white rounded-lg p-4 shadow border border-gray-200">
          <p class="text-gray-500 text-xs font-semibold uppercase">Lần cuối</p>
          <p class="font-semibold text-gray-900 mt-1 text-sm">
            {{ device.lastSeen ? new Date(device.lastSeen).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '—' }}
          </p>
        </div>
      </div>

      <!-- Threshold Settings -->
      <ThresholdSettings :device="device" />

      <!-- Charts (chỉ hiện với TEMPERATURE_HUMIDITY) -->
      <template v-if="device.type === 'TEMPERATURE_HUMIDITY' && sensorStore.history.length">
        <div class="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6">
          <SensorChart :data="sensorStore.history" field="temperature" />
          <SensorChart :data="sensorStore.history" field="humidity" />
        </div>
      </template>

      <!-- Sensor Data History Table -->
      <div class="mt-6 bg-white rounded-lg shadow border border-gray-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-200">
          <h2 class="text-lg font-semibold text-gray-900">📊 Lịch sử dữ liệu</h2>
        </div>

        <div class="overflow-x-auto">
          <table class="w-full text-sm">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Thời gian</th>
                <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Dữ liệu</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-200">
              <tr v-for="row in sensorStore.history" :key="row.id" class="hover:bg-gray-50 transition">
                <td class="px-6 py-3 text-gray-600 text-xs font-mono">
                  {{ new Date(row.recordedAt).toLocaleString('vi-VN') }}
                </td>
                <td class="px-6 py-3 text-gray-900 font-medium">
                  <span v-if="row.temperature != null">
                    🌡️ {{ row.temperature }}°C &nbsp;&nbsp; 💧 {{ row.humidity }}%
                  </span>
                  <span v-else-if="row.doorOpen != null">
                    {{ row.doorOpen ? '🔓 Mở' : '🔒 Đóng' }}
                  </span>
                  <span v-else-if="row.switchOn != null">
                    {{ row.switchOn ? '✅ Bật' : '⭕ Tắt' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-if="!sensorStore.history.length" class="px-6 py-8 text-center text-gray-500">
          <p>Chưa có dữ liệu cảm biến</p>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-8 text-gray-400">
      <p>Không tìm thấy thiết bị.</p>
    </div>
  </div>
</template>