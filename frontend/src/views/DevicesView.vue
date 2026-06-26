<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useDeviceStore } from '../stores/device.store'
import DeviceCard from '../components/DeviceCard.vue'
import DeviceForm from '../components/DeviceForm.vue'
import type { Device } from '../utils'


const deviceStore = useDeviceStore()
const showForm = ref(false)

onMounted(() => deviceStore.fetchAll())

async function handleCreate(data: Partial<Device>) {
  await deviceStore.createDevice(data)
  showForm.value = false
}

async function handleDelete(id: number) {
  if (confirm('Xóa thiết bị này?')) {
    await deviceStore.deleteDevice(id)
  }
}

async function handleToggle(device: Device) {
  const currentOn = device.latestData?.on ?? false
  const action = currentOn ? 'off' : 'on'
  await deviceStore.sendCommand(device.deviceId, { action })
}
</script>

<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-bold">📡 Quản lý thiết bị</h1>
      <button
        class="bg-blue-500 text-white px-4 py-2 rounded-xl text-sm font-medium hover:bg-blue-600 transition"
        @click="showForm = true"
      >
        + Thêm thiết bị
      </button>
    </div>

    <div v-if="deviceStore.loading" class="text-gray-400">Đang tải...</div>
    <div v-else-if="deviceStore.devices.length === 0" class="text-gray-400 text-center py-16">
      Chưa có thiết bị nào. Thêm thiết bị mới!
    </div>
    <div v-else class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4">
      <DeviceCard
        v-for="device in deviceStore.devices"
        :key="device.id"
        :device="device"
        @toggle="handleToggle"
        @delete="handleDelete"
      />
    </div>

    <DeviceForm
      v-if="showForm"
      @submit="handleCreate"
      @cancel="showForm = false"
    />
  </div>
</template>