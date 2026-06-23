<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { thresholdApi, type Threshold } from '../api/threshold.api'
import type { Device } from '../utils'

interface Props {
  device: Device
}

const props = defineProps<Props>()

const thresholds = ref<Threshold[]>([])
const loading = ref(false)
const showForm = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  field: 'temperature',
  minValue: undefined as number | undefined,
  maxValue: undefined as number | undefined,
})

const formError = ref<string | null>(null)

const fieldOptions = [
  { value: 'temperature', label: '🌡️ Nhiệt độ' },
  { value: 'humidity', label: '💧 Độ ẩm' },
  { value: 'doorOpen', label: '🚪 Cửa' },
  { value: 'switchOn', label: '🔌 Công tắc' },
]

onMounted(async () => {
  await loadThresholds()
})

async function loadThresholds() {
  loading.value = true
  try {
    thresholds.value = await thresholdApi.getByDevice(props.device.id)
  } catch (err) {
    console.error('Error loading thresholds:', err)
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  formError.value = null

  // Validation
  if (!form.value.field) {
    formError.value = 'Vui lòng chọn trường'
    return
  }

  if (form.value.minValue === undefined && form.value.maxValue === undefined) {
    formError.value = 'Vui lòng nhập ít nhất một giá trị (min hoặc max)'
    return
  }

  if (form.value.minValue !== undefined && form.value.maxValue !== undefined) {
    if (form.value.minValue >= form.value.maxValue) {
      formError.value = 'Giá trị min phải nhỏ hơn max'
      return
    }
  }

  try {
    if (editingId.value) {
      await thresholdApi.update(editingId.value, {
        field: form.value.field,
        minValue: form.value.minValue,
        maxValue: form.value.maxValue,
      } as Partial<Threshold>)
    } else {
      await thresholdApi.create({
        deviceId: props.device.id,
        deviceName: props.device.name,
        field: form.value.field,
        minValue: form.value.minValue,
        maxValue: form.value.maxValue,
      } as Omit<Threshold, 'id' | 'createdAt' | 'updatedAt'>)
    }
    await loadThresholds()
    resetForm()
  } catch (err) {
    formError.value = err instanceof Error ? err.message : 'Lỗi khi lưu threshold'
    console.error('Error saving threshold:', err)
  }
}

async function handleDelete(id: number) {
  if (!confirm('Bạn có chắc chắn muốn xóa threshold này?')) {
    return
  }

  try {
    await thresholdApi.delete(id)
    await loadThresholds()
  } catch (err) {
    console.error('Error deleting threshold:', err)
  }
}

function handleEdit(threshold: Threshold) {
  editingId.value = threshold.id
  form.value = {
    field: threshold.field,
    minValue: threshold.minValue,
    maxValue: threshold.maxValue,
  }
  showForm.value = true
}

function resetForm() {
  editingId.value = null
  form.value = {
    field: 'temperature',
    minValue: undefined,
    maxValue: undefined,
  }
  formError.value = null
  showForm.value = false
}

function getFieldLabel(field: string): string {
  const option = fieldOptions.find(o => o.value === field)
  return option?.label || field
}
</script>

<template>
  <div class="p-6 bg-white rounded-lg border border-gray-200">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-lg font-semibold text-gray-900">⚙️ Cấu hình Ngưỡng Cảnh báo</h2>
      <button
        @click="showForm = !showForm"
        class="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition"
      >
        {{ showForm ? '✕ Hủy' : '+ Thêm Ngưỡng' }}
      </button>
    </div>

    <!-- Form -->
    <div v-if="showForm" class="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
      <div class="space-y-4">
        <!-- Field Selection -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Trường
          </label>
          <select
            v-model="form.field"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option v-for="opt in fieldOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>

        <!-- Min Value -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Giá trị Tối thiểu
          </label>
          <input
            v-model.number="form.minValue"
            type="number"
            placeholder="Để trống nếu không cần"
            step="0.1"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <!-- Max Value -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Giá trị Tối đa
          </label>
          <input
            v-model.number="form.maxValue"
            type="number"
            placeholder="Để trống nếu không cần"
            step="0.1"
            class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <!-- Error Message -->
        <div v-if="formError" class="p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">
          {{ formError }}
        </div>

        <!-- Buttons -->
        <div class="flex gap-2 pt-2">
          <button
            @click="handleSubmit"
            class="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition"
          >
            {{ editingId ? 'Cập nhật' : 'Thêm' }}
          </button>
          <button
            @click="resetForm"
            class="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-200 hover:bg-gray-300 rounded-lg transition"
          >
            Hủy
          </button>
        </div>
      </div>
    </div>

    <!-- Thresholds List -->
    <div v-if="loading" class="text-center py-8 text-gray-500">
      Đang tải...
    </div>

    <div v-else-if="thresholds.length === 0" class="text-center py-8 text-gray-500">
      <p>Chưa có ngưỡng cảnh báo nào</p>
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="threshold in thresholds"
        :key="threshold.id"
        class="p-4 border border-gray-200 rounded-lg hover:border-gray-300 hover:shadow-sm transition"
      >
        <div class="flex items-center justify-between">
          <div class="flex-1">
            <p class="font-medium text-gray-900">
              {{ getFieldLabel(threshold.field) }}
            </p>
            <p class="text-sm text-gray-600 mt-1">
              <span v-if="threshold.minValue !== undefined">
                Min: {{ threshold.minValue }}
              </span>
              <span v-if="threshold.minValue !== undefined && threshold.maxValue !== undefined" class="text-gray-400 mx-2">
                —
              </span>
              <span v-if="threshold.maxValue !== undefined">
                Max: {{ threshold.maxValue }}
              </span>
            </p>
          </div>

          <div class="flex gap-2">
            <button
              @click="handleEdit(threshold)"
              class="px-3 py-1 text-xs font-medium text-blue-600 hover:text-blue-700 bg-blue-50 hover:bg-blue-100 rounded transition"
            >
              ✏️ Sửa
            </button>
            <button
              @click="handleDelete(threshold.id)"
              class="px-3 py-1 text-xs font-medium text-red-600 hover:text-red-700 bg-red-50 hover:bg-red-100 rounded transition"
            >
              🗑️ Xóa
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
