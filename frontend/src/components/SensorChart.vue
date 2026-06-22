<script setup lang="ts">
import { computed } from 'vue'
import type { SensorData } from '../utils';

const props = defineProps<{
  data: SensorData[]
  field: 'temperature' | 'humidity'
}>()

const label = computed(() =>
  props.field === 'temperature' ? 'Nhiệt độ (°C)' : 'Độ ẩm (%)'
)

const chartData = computed(() =>
  [...props.data].reverse().slice(-20).map(d => ({
    time: new Date(d.recordedAt).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    value: props.field === 'temperature' ? d.temperature : d.humidity,
  }))
)

const max = computed(() =>
  Math.max(...chartData.value.map(d => d.value ?? 0), props.field === 'temperature' ? 40 : 100)
)
</script>

<template>
  <div class="bg-white rounded-2xl p-4 shadow">
    <h3 class="text-sm font-semibold text-gray-600 mb-3">{{ label }}</h3>
    <div class="flex items-end gap-1 h-24">
      <template v-for="(point, i) in chartData" :key="i">
        <div class="flex flex-col items-center flex-1 group">
          <div
            class="w-full rounded-t transition-all"
            :class="field === 'temperature' ? 'bg-orange-400' : 'bg-blue-400'"
            :style="{ height: `${((point.value ?? 0) / max) * 100}%` }"
          />
          <span class="text-[9px] text-gray-400 mt-1 hidden group-hover:block">
            {{ point.value }}
          </span>
        </div>
      </template>
    </div>
    <div class="flex justify-between text-[10px] text-gray-400 mt-1">
      <span>{{ chartData[0]?.time }}</span>
      <span>{{ chartData[chartData.length - 1]?.time }}</span>
    </div>
  </div>
</template>