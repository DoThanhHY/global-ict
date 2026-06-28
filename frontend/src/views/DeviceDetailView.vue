<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSensorDataStore } from '../stores/sensorData.store'
import { useDeviceStore } from '../stores/device.store'
import LineChart from '../components/LineChart.vue'
import ThresholdSettings from '../components/ThresholdSettings.vue'

const route = useRoute()
const deviceId = route.params.deviceId as string

const deviceStore = useDeviceStore()
const sensorStore = useSensorDataStore()

const device = computed(() =>
  deviceStore.devices.find(d => d.deviceId === deviceId)
)

const timeRanges = [
  { label: '1h', value: 1 },
  { label: '6h', value: 6 },
  { label: '24h', value: 24 },
  { label: '7d', value: 168 },
  { label: 'Tùy chỉnh', value: 0 },
]

const activeRange = ref(24)
const customFrom = ref('')
const customTo = ref('')

function toISO(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function fetchWithRange(range: number) {
  activeRange.value = range
  if (range === 0) {
    if (!customFrom.value || !customTo.value) return
    await sensorStore.fetchByDevice(deviceId, customFrom.value, customTo.value)
  } else {
    const to = new Date()
    const from = new Date(to.getTime() - range * 60 * 60 * 1000)
    await sensorStore.fetchByDevice(deviceId, toISO(from), toISO(to))
  }
}

onMounted(async () => {
  if (!deviceStore.devices.length) await deviceStore.fetchAll()
  await fetchWithRange(24)
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
        <!-- Time Range Tabs -->
        <div class="mt-6 flex items-center gap-2 flex-wrap">
          <button
            v-for="r in timeRanges"
            :key="r.value"
            @click="fetchWithRange(r.value)"
            class="px-3 py-1.5 text-sm font-medium rounded-lg transition"
            :class="activeRange === r.value
              ? 'bg-blue-600 text-white shadow'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
          >
            {{ r.label }}
          </button>
        </div>

        <!-- Custom date inputs -->
        <div v-if="activeRange === 0" class="mt-3 flex items-center gap-3">
          <input
            v-model="customFrom"
            type="datetime-local"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-lg"
          />
          <span class="text-gray-500">→</span>
          <input
            v-model="customTo"
            type="datetime-local"
            class="px-3 py-1.5 text-sm border border-gray-300 rounded-lg"
          />
          <button
            @click="fetchWithRange(0)"
            class="px-3 py-1.5 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition"
          >
            Tra cứu
          </button>
        </div>

        <!-- Line Charts -->
        <div class="mt-4 grid grid-cols-1 md:grid-cols-2 gap-6">
          <LineChart :data="sensorStore.history" field="temperature" title="🌡️ Nhiệt độ (°C)" />
          <LineChart :data="sensorStore.history" field="humidity" title="💧 Độ ẩm (%)" />
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
