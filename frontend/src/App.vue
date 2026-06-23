<script setup lang="ts">
import { useWebSocket } from './composables/useWebSocket'
import { useAuthStore } from './stores/auth.store'
import AlertPanel from './components/AlertPanel.vue'

const authStore = useAuthStore()

// Initialize WebSocket nếu đã authenticated
if (authStore.isAuthenticated) {
  useWebSocket()
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Main Layout (nếu authenticated) -->
    <div v-if="authStore.isAuthenticated" class="flex h-screen flex-col">
      <!-- Header -->
      <header class="bg-white border-b border-gray-200 shadow-sm sticky top-0 z-40">
        <div class="max-w-full mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex items-center justify-between h-16">
            <!-- Logo/Title -->
            <div class="flex items-center gap-3">
              <span class="text-2xl">🏠</span>
              <h1 class="text-xl font-bold text-gray-900">Global ICT IoT</h1>
            </div>

            <!-- Header Actions -->
            <div class="flex items-center gap-4">
              <!-- Alert Panel -->
              <AlertPanel />

              <!-- User Menu -->
              <div class="flex items-center gap-3 pl-4 border-l border-gray-200">
                <span class="text-sm text-gray-600">{{ authStore.username }}</span>
                <button
                  @click="authStore.logout()"
                  class="px-3 py-2 text-sm font-medium text-red-600 hover:bg-red-50 rounded-lg transition"
                  title="Đăng xuất"
                >
                  🚪
                </button>
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- Content -->
      <div class="flex flex-1 overflow-hidden">
        <!-- Sidebar -->
        <aside class="w-56 bg-white border-r border-gray-200 overflow-y-auto">
          <nav class="p-4 space-y-1">
            <router-link
              to="/"
              class="flex items-center gap-3 px-4 py-3 text-sm font-medium text-gray-700 rounded-lg hover:bg-blue-50 hover:text-blue-600 transition"
              active-class="bg-blue-50 text-blue-600 font-semibold"
            >
              <span>🏠</span>
              <span>Dashboard</span>
            </router-link>
            <router-link
              to="/devices"
              class="flex items-center gap-3 px-4 py-3 text-sm font-medium text-gray-700 rounded-lg hover:bg-blue-50 hover:text-blue-600 transition"
              active-class="bg-blue-50 text-blue-600 font-semibold"
            >
              <span>📡</span>
              <span>Thiết bị</span>
            </router-link>
          </nav>
        </aside>

        <!-- Main Content -->
        <main class="flex-1 overflow-auto">
          <router-view />
        </main>
      </div>
    </div>

    <!-- Login View (nếu chưa authenticated) -->
    <div v-else>
      <router-view />
    </div>
  </div>
</template>
