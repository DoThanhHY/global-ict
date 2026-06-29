<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { LegendComponent, TooltipComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { SensorData } from '../utils'

use([
  PieChart,
  LegendComponent,
  TooltipComponent,
  TitleComponent,
  CanvasRenderer,
])

const props = defineProps<{
  data: SensorData[]
  field: 'doorOpen' | 'switchOn'
}>()

const emit = defineEmits<{
  (e: 'filter-change', value: 'on' | 'off'): void
}>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const labels = computed(() =>
  props.field === 'doorOpen'
    ? {
        title: 'Open / Closed Duration',
        onLabel: 'Open',
        offLabel: 'Closed',
      }
    : {
        title: 'Uptime / Downtime',
        onLabel: 'Uptime',
        offLabel: 'Downtime',
      }
)

const durationStats = computed(() => {
  const rows = [...props.data]
    .filter((row) => row[props.field] != null)
    .sort((a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime())

  if (!rows.length) {
    return { onMs: 0, offMs: 0 }
  }

  let onMs = 0
  let offMs = 0
  const deltas: number[] = []

  for (let i = 0; i < rows.length - 1; i++) {
    const current = rows[i]
    const next = rows[i + 1]
    const delta = Math.max(0, new Date(next.recordedAt).getTime() - new Date(current.recordedAt).getTime())
    deltas.push(delta)

    if (current[props.field]) {
      onMs += delta
    } else {
      offMs += delta
    }
  }

  const fallbackDelta = deltas.length
    ? Math.round(deltas.reduce((sum, d) => sum + d, 0) / deltas.length)
    : 60000

  const last = rows[rows.length - 1]
  if (last[props.field]) {
    onMs += fallbackDelta
  } else {
    offMs += fallbackDelta
  }

  return { onMs, offMs }
})

const totalMs = computed(() => durationStats.value.onMs + durationStats.value.offMs)

const onPercent = computed(() =>
  totalMs.value ? Math.round((durationStats.value.onMs / totalMs.value) * 100) : 0
)

function renderChart() {
  if (!chartRef.value) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
    chart.on('click', (params: any) => {
      const clickedName = String(params?.name ?? '')
      if (clickedName === labels.value.onLabel) {
        emit('filter-change', 'on')
      } else if (clickedName === labels.value.offLabel) {
        emit('filter-change', 'off')
      }
    })
  }

  chart.setOption({
    title: {
      text: labels.value.title,
      left: 'center',
      textStyle: {
        fontSize: 14,
        fontWeight: 'normal' as const,
        color: '#374151',
      },
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        const value = Number(params?.value ?? 0)
        const minutes = Math.max(1, Math.round(value / 60000))
        return `${params?.name}: <b>${params?.percent}%</b><br/>~${minutes} phút`
      },
    },
    legend: {
      bottom: 10,
      left: 'center',
    },
    series: [
      {
        name: labels.value.title,
        type: 'pie',
        radius: ['48%', '72%'],
        center: ['50%', '52%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          formatter: '{d}%',
        },
        data: [
          {
            name: labels.value.onLabel,
            value: Math.max(durationStats.value.onMs, 0),
            itemStyle: { color: '#16a34a' },
          },
          {
            name: labels.value.offLabel,
            value: Math.max(durationStats.value.offMs, 0),
            itemStyle: { color: '#ef4444' },
          },
        ],
      },
    ],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '45%',
        style: {
          text: `${onPercent.value}%`,
          textAlign: 'center',
          fill: '#111827',
          fontSize: 24,
          fontWeight: 'bold',
        },
      },
      {
        type: 'text',
        left: 'center',
        top: '55%',
        style: {
          text: labels.value.onLabel,
          textAlign: 'center',
          fill: '#6b7280',
          fontSize: 12,
        },
      },
    ],
  })

  chart.resize()
}

function attachResizeObserver() {
  if (!chartRef.value || resizeObserver) return

  resizeObserver = new ResizeObserver(() => {
    chart?.resize()
  })
  resizeObserver.observe(chartRef.value)
}

async function scheduleRender() {
  await nextTick()

  if (!chartRef.value) {
    chart?.dispose()
    chart = null
    return
  }

  attachResizeObserver()
  requestAnimationFrame(() => {
    renderChart()
  })
}

watch([durationStats, labels, totalMs], () => {
  void scheduleRender()
}, { deep: true })

onMounted(async () => {
  await scheduleRender()
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
    <div v-if="totalMs > 0" ref="chartRef" class="w-full" style="height: 320px;" />
    <div v-else class="h-44 flex items-center justify-center text-sm text-gray-400">
      Chưa có dữ liệu trạng thái
    </div>
  </div>
</template>
