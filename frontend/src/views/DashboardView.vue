<script setup lang="ts">
import { onMounted } from 'vue'
import { useWebSocket } from '../composables/useWebSocket'
import { useDeviceStore } from '../stores/device.store'
import DeviceCard from '../components/DeviceCard.vue';
const deviceStore = useDeviceStore()

useWebSocket()   // Connect WebSocket khi mount

onMounted(() => {
  deviceStore.fetchAll()
})
</script>

<template>
  <div class="p-6">
    <h1 class="text-2xl font-bold mb-6">🏠 IoT Dashboard</h1>

    <!-- Stats -->
    <div class="grid grid-cols-3 gap-4 mb-8">
      <div class="bg-white rounded-xl p-4 shadow">
        <p class="text-gray-500 text-sm">Tổng thiết bị</p>
        <p class="text-3xl font-bold">{{ deviceStore.devices.length }}</p>
      </div>
      <div class="bg-green-50 rounded-xl p-4 shadow">
        <p class="text-green-600 text-sm">Đang online</p>
        <p class="text-3xl font-bold text-green-600">{{ deviceStore.onlineDevices.length }}</p>
      </div>
      <div class="bg-red-50 rounded-xl p-4 shadow">
        <p class="text-red-500 text-sm">Offline</p>
        <p class="text-3xl font-bold text-red-500">{{ deviceStore.offlineDevices.length }}</p>
      </div>
    </div>

    <!-- Device list -->
    <div v-if="deviceStore.loading">Đang tải...</div>
    <div v-else class="grid grid-cols-2 md:grid-cols-3 gap-4">
      <DeviceCard
        v-for="device in deviceStore.devices"
        :key="device.id"
        :device="device"
      />
    </div>
  </div>
</template>