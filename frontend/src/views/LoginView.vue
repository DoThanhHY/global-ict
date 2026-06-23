<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthStore } from '../stores/auth.store'

const authStore = useAuthStore()
const loading = ref(false)
const error = ref('')

const form = reactive({
  username: '',
  password: '',
})

async function handleLogin() {
  if (!form.username || !form.password) return
  loading.value = true
  error.value = ''
  try {
    await authStore.login(form)
  } catch (e) {
    error.value = 'Sai tên đăng nhập hoặc mật khẩu'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex items-center justify-center">
    <div class="bg-white rounded-2xl shadow p-8 w-full max-w-sm">
      <h1 class="text-2xl font-bold text-center mb-6">🏠 Global ICT</h1>

      <div class="flex flex-col gap-4">
        <div>
          <label class="text-sm text-gray-600">Tên đăng nhập</label>
          <input
            v-model="form.username"
            type="text"
            placeholder="admin"
            class="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
            @keyup.enter="handleLogin"
          />
        </div>
        <div>
          <label class="text-sm text-gray-600">Mật khẩu</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="••••••••"
            class="w-full border rounded-lg px-3 py-2 mt-1 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
            @keyup.enter="handleLogin"
          />
        </div>

        <p v-if="error" class="text-red-500 text-sm">{{ error }}</p>

        <button
          class="bg-blue-500 text-white rounded-lg py-2 font-medium hover:bg-blue-600 transition disabled:opacity-50"
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? 'Đang đăng nhập...' : 'Đăng nhập' }}
        </button>
      </div>
    </div>
  </div>
</template>