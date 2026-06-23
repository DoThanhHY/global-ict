<script setup lang="ts">
import { useThresholdAlertStore } from '../stores/threshold.store'

const alertStore = useThresholdAlertStore()
</script>

<template>
  <!-- Badge for unread alerts -->
  <div v-if="alertStore.unreadCount > 0" class="relative">
    <span class="absolute -top-2 -right-2 inline-flex items-center justify-center h-6 w-6 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-red-600 rounded-full">
      {{ alertStore.unreadCount }}
    </span>
  </div>
</template>
