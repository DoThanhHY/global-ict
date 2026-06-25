<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { commandLogApi } from '../api/commandLog.api'
import type { CommandLog } from '../utils'

const logs = ref<CommandLog[]>([])
const loading = ref(true)
const filterDeviceId = ref('')

async function fetchLogs() {
  loading.value = true
  try {
    logs.value = filterDeviceId.value
      ? await commandLogApi.getByDevice(filterDeviceId.value)
      : await commandLogApi.getAll()
  } catch {
    logs.value = []
  } finally {
    loading.value = false
  }
}

function statusClass(status: string) {
  return status === 'SUCCESS'
    ? 'bg-green-100 text-green-700'
    : 'bg-red-100 text-red-700'
}

onMounted(fetchLogs)
</script>

<template>
  <div class="p-6">
    <h1 class="text-2xl font-bold mb-6">Audit Log — Command History</h1>

    <div class="flex items-center gap-3 mb-4">
      <input
        v-model="filterDeviceId"
        placeholder="Filter by device ID…"
        class="border border-gray-300 rounded px-3 py-2 text-sm w-64"
        @input="fetchLogs"
      />
      <button
        class="bg-blue-500 text-white px-4 py-2 rounded text-sm hover:bg-blue-600 transition"
        @click="fetchLogs"
      >
        Refresh
      </button>
    </div>

    <div class="bg-white rounded-lg shadow border border-gray-200 overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Time</th>
              <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Device</th>
              <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Action</th>
              <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Sent By</th>
              <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Status</th>
              <th class="px-6 py-3 text-left text-xs font-semibold text-gray-600 uppercase">Error</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="log in logs" :key="log.id" class="hover:bg-gray-50 transition">
              <td class="px-6 py-3 text-gray-600 text-xs font-mono">
                {{ new Date(log.sentAt).toLocaleString('vi-VN') }}
              </td>
              <td class="px-6 py-3 font-mono text-gray-900">{{ log.deviceId }}</td>
              <td class="px-6 py-3">
                <span class="font-semibold">{{ log.action }}</span>
              </td>
              <td class="px-6 py-3 text-gray-700">{{ log.sentBy }}</td>
              <td class="px-6 py-3">
                <span
                  class="text-xs px-2 py-1 rounded-full font-semibold"
                  :class="statusClass(log.status)"
                >
                  {{ log.status }}
                </span>
              </td>
              <td class="px-6 py-3 text-red-600 text-xs">{{ log.errorMessage || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="!loading && !logs.length" class="px-6 py-8 text-center text-gray-500">
        <p>No command logs found.</p>
      </div>

      <div v-if="loading" class="px-6 py-8 text-center text-gray-400">
        <p>Loading…</p>
      </div>
    </div>
  </div>
</template>
