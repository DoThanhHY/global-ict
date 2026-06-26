<script setup lang="ts">
import { computed } from 'vue'
import type { Device } from '../utils';

const props = defineProps<{ device: Device }>()
const emit = defineEmits<{
  (e: 'toggle', device: Device): void
  (e: 'delete', id: number): void
}>()

const icon = computed(() => {
  switch (props.device.type) {
    case 'TEMPERATURE_HUMIDITY': return '🌡️'
    case 'DOOR_SENSOR':          return '🚪'
    case 'SWITCH':               return '💡'
    default:                     return '📡'
  }
})

const statusLabel = computed(() =>
  props.device.online ? 'Online' : 'Offline'
)
</script>

<template>
  <div class="bg-white rounded-2xl shadow p-4 flex flex-col gap-3 border border-gray-100">
    <!-- Header -->
    <div class="flex justify-between items-start">
      <div>
        <span class="text-3xl">{{ icon }}</span>
        <h3 class="font-semibold text-gray-800 mt-1">{{ device.name }}</h3>
        <p class="text-xs text-gray-400">{{ device.location || '—' }}</p>
      </div>
      <span
        class="text-xs px-2 py-1 rounded-full font-medium"
        :class="device.online
          ? 'bg-green-100 text-green-600'
          : 'bg-red-100 text-red-500'"
      >
        {{ statusLabel }}
      </span>
    </div>

    <!-- Latest data -->
    <div v-if="device.latestData" class="text-sm text-gray-600 bg-gray-50 rounded-lg p-2">
      <template v-if="device.type === 'TEMPERATURE_HUMIDITY'">
        <span>🌡 {{ device.latestData.temperature }}°C</span>
        <span class="ml-3">💧 {{ device.latestData.humidity }}%</span>
      </template>
      <template v-else-if="device.type === 'DOOR_SENSOR'">
        <span>{{ device.latestData.open ? '🔓 Đang mở' : '🔒 Đang đóng' }}</span>
      </template>
      <template v-else-if="device.type === 'SWITCH'">
        <span>{{ device.latestData.on ? '✅ Đang bật' : '⭕ Đang tắt' }}</span>
      </template>
    </div>
    <div v-else class="text-xs text-gray-400 italic">Chưa có dữ liệu</div>

    <!-- Actions -->
    <div class="flex gap-2 mt-auto">
      <router-link
        :to="`/devices/${device.deviceId}`"
        class="flex-1 text-center text-sm bg-blue-50 text-blue-600 rounded-lg py-1.5 hover:bg-blue-100 transition"
      >
        Chi tiết
      </router-link>
      <button
        class="flex-1 text-sm bg-yellow-50 text-yellow-600 rounded-lg py-1.5 hover:bg-yellow-100 transition"
        @click="emit('toggle', device)"
      >
        Toggle
      </button>
      <button
        class="text-sm bg-red-50 text-red-500 rounded-lg px-3 py-1.5 hover:bg-red-100 transition"
        @click="emit('delete', device.id)"
      >
        🗑
      </button>
    </div>
  </div>
</template>