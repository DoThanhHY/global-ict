<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart as EChartsLine } from 'echarts/charts'
import { GridComponent, TooltipComponent, DataZoomComponent } from 'echarts/components'
import type { SensorData } from '../utils'

use([CanvasRenderer, EChartsLine, GridComponent, TooltipComponent, DataZoomComponent])

const props = defineProps<{
  data: SensorData[]
  field: 'temperature' | 'humidity' 
  title: string
}>()

const seriesData = computed(() =>
  [...props.data].reverse().map(d => ({
    value: [new Date(d.recordedAt).getTime(), props.field === 'temperature' ? d.temperature : d.humidity],
  }))
)

const unit = computed(() => props.field === 'temperature' ? '°C' : '%')

const option = computed(() => ({
  tooltip: {
    trigger: 'axis',
    valueFormatter: (v: number) => `${v}${unit.value}`,
  },
  grid: { left: 50, right: 20, top: 10, bottom: 30 },
  xAxis: {
    type: 'time',
    axisLabel: { fontSize: 10 },
  },
  yAxis: {
    type: 'value',
    axisLabel: { fontSize: 10, formatter: `{value}${unit.value}` },
  },
  dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 0, height: 20 }],
  series: [{
    type: 'line',
    data: seriesData.value,
    smooth: true,
    symbol: 'none',
    lineStyle: { width: 2 },
    areaStyle: { opacity: 0.1 },
    itemStyle: { color: props.field === 'temperature' ? '#f97316' : '#3b82f6' },
  }],
}))
</script>

<template>
  <div class="bg-white rounded-2xl p-4 shadow">
    <h3 class="text-sm font-semibold text-gray-600 mb-3">{{ title }}</h3>
    <VChart class="w-full h-64" :option="option" autoresize />
  </div>
</template>
