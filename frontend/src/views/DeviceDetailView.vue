<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSensorDataStore } from '../stores/sensorData.store'
import { useDeviceStore } from '../stores/device.store'
import SensorChart from '../components/SensorChart.vue'
import UptimeDonutChart from '../components/UptimeDonutChart.vue'
import ThresholdSettings from '../components/ThresholdSettings.vue'

const route = useRoute()
const deviceId = route.params.deviceId as string

const deviceStore = useDeviceStore()
const sensorStore = useSensorDataStore()
const selectedRange = ref<'1h' | '24h' | '7d'>('24h')
const rangeOptions = [
  { label: '1h', value: '1h' },
  { label: '24h', value: '24h' },
  { label: '7d', value: '7d' },
] as const

const device = computed(() =>
  deviceStore.devices.find(d => d.deviceId === deviceId)
)

const isTempHumidityDevice = computed(() => device.value?.type === 'TEMPERATURE_HUMIDITY')
const isDoorSensorDevice = computed(() => device.value?.type === 'DOOR_SENSOR')
const isSwitchDevice = computed(() => device.value?.type === 'SWITCH')
const stateFilter = ref<'all' | 'on' | 'off'>('all')

const filteredHistory = computed(() => {
  if (stateFilter.value === 'all') {
    return sensorStore.history
  }

  if (isDoorSensorDevice.value) {
    return sensorStore.history.filter((row) =>
      stateFilter.value === 'on' ? row.doorOpen === true : row.doorOpen === false
    )
  }

  if (isSwitchDevice.value) {
    return sensorStore.history.filter((row) =>
      stateFilter.value === 'on' ? row.switchOn === true : row.switchOn === false
    )
  }

  return sensorStore.history
})

const tableHistory = computed(() => filteredHistory.value.slice(0, 100))

function changeRange(range: '1h' | '24h' | '7d') {
  selectedRange.value = range
  stateFilter.value = 'all'
  sensorStore.fetchByDevice(deviceId, range)
}

function handleStateFilter(value: 'on' | 'off') {
  stateFilter.value = stateFilter.value === value ? 'all' : value
}

onMounted(async () => {
  if (!deviceStore.devices.length) await deviceStore.fetchAll()
  await sensorStore.fetchByDevice(deviceId, selectedRange.value)
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

      <div class="flex gap-2 mb-6">
        <button
          v-for="option in rangeOptions"
          :key="option.value"
          class="px-3 py-1.5 text-sm font-medium border transition-all duration-200 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-300 cursor-pointer"
          :class="selectedRange === option.value
            ? 'bg-blue-600 text-white border-blue-600 shadow-md scale-[1.02]'
            : 'bg-white text-gray-700 border-gray-200 hover:bg-blue-50 hover:border-blue-300 hover:text-blue-700 hover:shadow-md active:scale-[0.98] active:bg-blue-100'"
          @click="changeRange(option.value)"
        >
          {{ option.label }}
        </button>
      </div>

      <div v-if="isTempHumidityDevice" class="mt-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div>
          <div class="mb-2 text-sm font-semibold text-gray-700">🌡️ Nhiệt độ</div>
          <SensorChart :data="sensorStore.history" field="temperature" :range="selectedRange" />
        </div>
        <div>
          <div class="mb-2 text-sm font-semibold text-gray-700">💧 Độ ẩm</div>
          <SensorChart :data="sensorStore.history" field="humidity" :range="selectedRange" />
        </div>
      </div>

      <div v-if="isDoorSensorDevice" class="mt-6">
        <div class="mb-2 text-sm font-semibold text-gray-700">🚪 Open / Closed</div>
        <UptimeDonutChart :data="sensorStore.history" field="doorOpen" @filter-change="handleStateFilter" />
      </div>

      <div v-if="isSwitchDevice" class="mt-6">
        <div class="mb-2 text-sm font-semibold text-gray-700">🟢 Uptime / Downtime</div>
        <UptimeDonutChart :data="sensorStore.history" field="switchOn" @filter-change="handleStateFilter" />
      </div>

      <div v-if="(isDoorSensorDevice || isSwitchDevice) && stateFilter !== 'all'" class="mt-3 text-sm text-blue-700">
        Đang lọc theo trạng thái: <b>{{ stateFilter === 'on' ? 'ON / Open' : 'OFF / Closed' }}</b>
        <button class="ml-3 text-blue-600 hover:underline" @click="stateFilter = 'all'">Xóa lọc</button>
      </div>

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
              <tr v-for="row in tableHistory" :key="row.id" class="hover:bg-gray-50 transition">
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

        <div v-if="!tableHistory.length" class="px-6 py-8 text-center text-gray-500">
          <p>Chưa có dữ liệu cảm biến</p>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-8 text-gray-400">
      <p>Không tìm thấy thiết bị.</p>
    </div>
  </div>
</template>