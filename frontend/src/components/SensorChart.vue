<script setup lang="ts">
import { computed, nextTick, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { SensorData } from '../utils'

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

use([
  LineChart,
  GridComponent,
  TooltipComponent,
  TitleComponent,
  LegendComponent,
  CanvasRenderer,
])

const props = withDefaults(defineProps<{
  data: SensorData[]
  field: 'temperature' | 'humidity'
  range?: '1h' | '24h' | '7d'
}>(), {
  range: '24h',
})

const label = computed(() =>
  props.field === 'temperature' ? 'Nhiệt độ (°C)' : 'Độ ẩm (%)'
)

const bucketMs = computed(() => {
  if (props.range === '1h') return 5 * 60 * 1000
  if (props.range === '24h') return 60 * 60 * 1000
  return 24 * 60 * 60 * 1000
})

const rangeMs = computed(() => {
  if (props.range === '1h') return 60 * 60 * 1000
  if (props.range === '24h') return 24 * 60 * 60 * 1000
  return 7 * 24 * 60 * 60 * 1000
})

const chartData = computed(() => {
  const rows = [...props.data]
    .filter((d) => props.field === 'temperature' ? d.temperature != null : d.humidity != null)
    .sort((a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime())

  if (!rows.length) return []

  const latestTs = new Date(rows[rows.length - 1].recordedAt).getTime()
  const startTs = latestTs - rangeMs.value
  const startBucket = Math.floor(startTs / bucketMs.value) * bucketMs.value
  const endBucket = Math.floor(latestTs / bucketMs.value) * bucketMs.value

  const grouped = new Map<number, { sum: number; count: number }>()

  for (const row of rows) {
    const timestamp = new Date(row.recordedAt).getTime()
    if (timestamp < startTs || timestamp > latestTs) continue

    const key = Math.floor(timestamp / bucketMs.value) * bucketMs.value
    const value = props.field === 'temperature' ? Number(row.temperature) : Number(row.humidity)

    if (!grouped.has(key)) {
      grouped.set(key, { sum: 0, count: 0 })
    }

    const bucket = grouped.get(key)!
    bucket.sum += value
    bucket.count += 1
  }

  const points: Array<{ time: string; value: number | null }> = []

  for (let key = startBucket; key <= endBucket; key += bucketMs.value) {
    const bucket = grouped.get(key)
    const date = new Date(key)
    const timeLabel = props.range === '7d'
      ? date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
      : date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })

    points.push({
      time: timeLabel,
      value: bucket ? Number((bucket.sum / bucket.count).toFixed(2)) : null,
    })
  }

  return points
})

function renderChart() {
  if (!chartRef.value) return

  const option = {
    title: {
      text: label.value,
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal' as const,
        color: '#4b5563',
      },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = Array.isArray(params) ? params[0] : params
        const value = p?.value == null ? '-' : p?.value
        return `${p?.name}<br/>${label.value}: ${value}`
      },
    },
    grid: {
      left: '8%',
      right: '4%',
      top: '20%',
      bottom: '12%',
    },
    xAxis: {
      type: 'category',
      data: chartData.value.map((item) => item.time),
      boundaryGap: false,
      axisLabel: {
        color: '#6b7280',
        fontSize: 11,
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        color: '#6b7280',
        fontSize: 11,
      },
      splitLine: {
        lineStyle: {
          color: '#e5e7eb',
        },
      },
    },
    series: [
      {
        name: label.value,
        type: 'line',
        smooth: true,
        connectNulls: false,
        showSymbol: false,
        data: chartData.value.map((item) => item.value),
        lineStyle: {
          width: 2.5,
          color: props.field === 'temperature' ? '#f59e0b' : '#3b82f6',
        },
        itemStyle: {
          color: props.field === 'temperature' ? '#f59e0b' : '#3b82f6',
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: props.field === 'temperature' ? 'rgba(245, 158, 11, 0.25)' : 'rgba(59, 130, 246, 0.25)' },
              { offset: 1, color: props.field === 'temperature' ? 'rgba(245, 158, 11, 0.03)' : 'rgba(59, 130, 246, 0.03)' },
            ],
          },
        },
      },
    ],
  }

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  chart.setOption(option)
  chart.resize()
}

watch([chartData, label], () => {
  renderChart()
}, { deep: true })

onMounted(async () => {
  await nextTick()
  requestAnimationFrame(() => {
    renderChart()
  })

  if (chartRef.value) {
    resizeObserver = new ResizeObserver(() => {
      chart?.resize()
    })
    resizeObserver.observe(chartRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="bg-white rounded-2xl p-4 shadow border border-gray-100">
    <div ref="chartRef" class="w-full" style="height: 320px;" />
  </div>
</template>