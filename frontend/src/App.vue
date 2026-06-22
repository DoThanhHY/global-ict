<script setup lang="ts">
import { useWebSocket } from './composables/useWebSocket';

useWebSocket()
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex">
    <!-- Sidebar -->
    <aside class="w-56 bg-white border-r flex flex-col py-6 px-4 gap-1 shadow-sm">
      <p class="text-xs text-gray-400 font-semibold uppercase mb-3 px-2">Global ICT</p>
      <router-link
        to="/"
        class="flex items-center gap-2 px-3 py-2 rounded-xl text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-600 transition"
        active-class="bg-blue-50 text-blue-600 font-semibold"
      >
        🏠 Dashboard
      </router-link>
      <router-link
        to="/devices"
        class="flex items-center gap-2 px-3 py-2 rounded-xl text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-600 transition"
        active-class="bg-blue-50 text-blue-600 font-semibold"
      >
        📡 Thiết bị
      </router-link>
    </aside>

    <!-- Main -->
    <main class="flex-1 overflow-auto">
      <router-view />
    </main>
  </div>
</template>