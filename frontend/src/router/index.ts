import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import DevicesView from '../views/DevicesView.vue'
import DeviceDetailView from '../views/DeviceDetailView.vue'
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/',           name: 'dashboard', component: DashboardView },
    { path: '/devices',    name: 'devices',   component: DevicesView },
    { path: '/devices/:deviceId', name: 'device-detail', component: DeviceDetailView },
  ]
})