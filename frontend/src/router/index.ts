import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import DevicesView from '../views/DevicesView.vue'
import DeviceDetailView from '../views/DeviceDetailView.vue'
import { useAuthStore } from '../stores/auth.store.ts'
export const router = createRouter({
  history: createWebHistory(),
  routes: [
     {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    { path: '/',           name: 'dashboard', component: DashboardView },
    { path: '/devices',    name: 'devices',   component: DevicesView },
    { path: '/devices/:deviceId', name: 'device-detail', component: DeviceDetailView },
  ]
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.name === 'login' && authStore.isAuthenticated) {
    return { name: 'dashboard' }
  }
})