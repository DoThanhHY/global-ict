import { deviceApi } from "./../api/device.api";
import { defineStore } from "pinia";
import { ref, computed } from "vue";
import type { Device } from "../utils";

export const useDeviceStore = defineStore("device", () => {
  const devices = ref<Device[]>([]);
  const loading = ref(false);

  const onlineDevices = computed(() => devices.value.filter((d) => d.online));

  const offlineDevices = computed(() => devices.value.filter((d) => !d.online));

  async function fetchAll() {
    loading.value = true;
    try {
      devices.value = await deviceApi.getAll();
    } finally {
      loading.value = false;
    }
  }

  async function createDevice(data: Partial<Device>) {
    const created = await deviceApi.create(data);
    devices.value.push(created);
  }

  async function deleteDevice(id: number) {
    await deviceApi.delete(id);
    devices.value = devices.value.filter((d: any) => d.id !== id);
  }

  // Cập nhật real-time từ WebSocket
  function updateDeviceStatus(deviceId: string, payload: any) {
    const device = devices.value.find((d: any) => d.deviceId === deviceId);
    if (device) {
      device.online = true;
      device.lastSeen = new Date().toISOString();
      device.latestData = payload;
    }
  }

  async function sendCommand(
    deviceId: string,
    command: Record<string, unknown>,
  ) {
    await deviceApi.sendCommand(deviceId, command);
  }

  return {
    devices,
    loading,
    onlineDevices,
    offlineDevices,
    sendCommand,
    fetchAll,
    createDevice,
    deleteDevice,
    updateDeviceStatus,
  };
});
