<script setup lang="ts">
import { ref } from 'vue'
import { useThresholdAlertStore } from '../stores/threshold.store'

const alertStore = useThresholdAlertStore()
const isOpen = ref(false)

function formatTime(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleTimeString('vi-VN', { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  })
}

function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return date.toLocaleDateString('vi-VN')
}

function getAlertColor(alertType: string): string {
  return alertType === 'MAX_EXCEEDED' ? 'bg-red-50 border-red-200' : 'bg-blue-50 border-blue-200'
}

function getAlertIcon(alertType: string): string {
  return alertType === 'MAX_EXCEEDED' ? '📈' : '📉'
}

async function handleResolveAlert(alertId: number) {
  await alertStore.resolveAlert(alertId)
}
</script>

<template>
  <div class="relative">
    <!-- Toggle Button -->
    <button
      @click="isOpen = !isOpen"
      class="relative inline-flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-100 transition"
    >
      🔔
      <span v-if="alertStore.unreadCount > 0" class="inline-flex items-center justify-center h-5 w-5 text-xs font-bold text-white bg-red-600 rounded-full">
        {{ alertStore.unreadCount }}
      </span>
    </button>

    <!-- Dropdown Panel -->
    <div
      v-if="isOpen"
      class="absolute right-0 mt-2 w-96 bg-white rounded-lg shadow-lg border border-gray-200 z-50"
    >
      <!-- Header -->
      <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
        <h3 class="text-sm font-semibold text-gray-900">Cảnh báo ngưỡng</h3>
        <button
          @click="isOpen = false"
          class="text-gray-400 hover:text-gray-600"
        >
          ✕
        </button>
      </div>

      <!-- Alert List -->
      <div class="max-h-96 overflow-y-auto">
        <div v-if="alertStore.unresolvedAlerts.length === 0" class="px-4 py-8 text-center text-gray-500">
          <p class="text-sm">Không có cảnh báo nào</p>
        </div>

        <div
          v-for="alert in alertStore.unresolvedAlerts"
          :key="alert.id"
          :class="['px-4 py-3 border-b border-gray-100 hover:bg-gray-50 transition', getAlertColor(alert.alertType)]"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1 min-w-0">
              <!-- Device Name -->
              <p class="text-xs font-semibold text-gray-900 truncate">
                {{ getAlertIcon(alert.alertType) }} {{ alert.deviceName }}
              </p>

              <!-- Alert Details -->
              <p class="text-xs text-gray-600 mt-1">
                <span class="font-medium">{{ alert.fieldName }}</span>
                <span v-if="alert.alertType === 'MAX_EXCEEDED'" class="text-red-600">
                  vượt quá {{ alert.thresholdValue }}°
                </span>
                <span v-else class="text-blue-600">
                  dưới {{ alert.thresholdValue }}°
                </span>
              </p>

              <!-- Value Display -->
              <p class="text-xs text-gray-700 mt-1">
                Giá trị hiện tại: <span class="font-bold">{{ alert.actualValue.toFixed(1) }}</span>
              </p>

              <!-- Time -->
              <p class="text-xs text-gray-500 mt-1">
                {{ formatDate(alert.triggeredAt) }} {{ formatTime(alert.triggeredAt) }}
              </p>
            </div>

            <!-- Resolve Button -->
            <button
              @click="handleResolveAlert(alert.id)"
              class="px-2 py-1 text-xs font-medium text-gray-600 hover:text-gray-900 bg-gray-200 hover:bg-gray-300 rounded transition whitespace-nowrap"
            >
              ✓
            </button>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div v-if="alertStore.unresolvedAlerts.length > 0" class="px-4 py-2 border-t border-gray-200 flex items-center justify-between">
        <p class="text-xs text-gray-500">
          {{ alertStore.unresolvedAlerts.length }} cảnh báo chưa đọc
        </p>
        <button
          @click="alertStore.clearResolvedAlerts()"
          class="text-xs text-gray-600 hover:text-gray-900 underline"
        >
          Xóa đã xử lý
        </button>
      </div>
    </div>

    <!-- Overlay to close panel -->
    <div
      v-if="isOpen"
      @click="isOpen = false"
      class="fixed inset-0 z-40"
    />
  </div>
</template>
