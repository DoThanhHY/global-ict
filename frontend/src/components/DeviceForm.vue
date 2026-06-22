<script setup lang="ts">
import { reactive } from 'vue'
import type { Device } from '../utils';

const emit = defineEmits<{
  (e: 'submit', data: Partial<Device>): void
  (e: 'cancel'): void
}>()

const form = reactive({
  deviceId: '',
  name: '',
  location: '',
  type: 'TEMPERATURE_HUMIDITY' as Device['type'],
})

function handleSubmit() {
  if (!form.deviceId || !form.name) return
  emit('submit', { ...form })
}
</script>

<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
    <div class="bg-white rounded-2xl p-6 w-full max-w-md shadow-xl">
      <h2 class="text-lg font-bold mb-4">➕ Thêm thiết bị</h2>

      <div class="flex flex-col gap-3">
        <div>
          <label class="text-sm text-gray-600">Device ID *</label>
          <input
            v-model="form.deviceId"
            placeholder="esp32-001"
            class="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
        </div>
        <div>
          <label class="text-sm text-gray-600">Tên thiết bị *</label>
          <input
            v-model="form.name"
            placeholder="Cảm biến phòng khách"
            class="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
        </div>
        <div>
          <label class="text-sm text-gray-600">Vị trí</label>
          <input
            v-model="form.location"
            placeholder="Phòng khách"
            class="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
        </div>
        <div>
          <label class="text-sm text-gray-600">Loại thiết bị</label>
          <select
            v-model="form.type"
            class="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          >
            <option value="TEMPERATURE_HUMIDITY">🌡️ Nhiệt độ / Độ ẩm</option>
            <option value="DOOR_SENSOR">🚪 Cảm biến cửa</option>
            <option value="SWITCH">💡 Công tắc</option>
          </select>
        </div>
      </div>

      <div class="flex gap-3 mt-6">
        <button
          class="flex-1 bg-blue-500 text-white rounded-lg py-2 text-sm font-medium hover:bg-blue-600 transition"
          @click="handleSubmit"
        >
          Thêm
        </button>
        <button
          class="flex-1 bg-gray-100 text-gray-600 rounded-lg py-2 text-sm hover:bg-gray-200 transition"
          @click="emit('cancel')"
        >
          Hủy
        </button>
      </div>
    </div>
  </div>
</template>