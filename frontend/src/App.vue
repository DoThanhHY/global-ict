<script setup lang="ts">
import { useWebSocket } from './composables/useWebSocket';
import { useAuthStore } from './stores/auth.store';


const authStore = useAuthStore()
useWebSocket()
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex">
    <aside v-if="authStore.isAuthenticated" class="w-56 bg-white border-r flex flex-col py-6 px-4 gap-1 shadow-sm">
      <p class="text-xs text-gray-400 font-semibold uppercase mb-3 px-2">Global ICT</p>
      <router-link to="/">🏠 Dashboard</router-link>
      <router-link to="/devices">📡 Thiết bị</router-link>

      <div class="mt-auto">
        <p class="text-xs text-gray-400 px-2 mb-1">{{ authStore.username }}</p>
        <button
          class="w-full text-left px-3 py-2 rounded-xl text-sm text-red-500 hover:bg-red-50 transition"
          @click="authStore.logout()"
        >
          🚪 Đăng xuất
        </button>
      </div>
    </aside>

    <main class="flex-1 overflow-auto">
      <router-view />
    </main>
  </div>
</template>