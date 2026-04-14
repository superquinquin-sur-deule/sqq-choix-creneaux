import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      path: '/choisir',
      name: 'slot-selection',
      component: () => import('@/views/SlotSelectionView.vue'),
    },
    {
      path: '/confirmer/:slotId',
      name: 'confirmation',
      component: () => import('@/views/ConfirmationView.vue'),
      props: true,
    },
    {
      path: '/termine',
      name: 'done',
      component: () => import('@/views/DoneView.vue'),
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/admin/DashboardView.vue'),
    },
  ],
})

router.onError((error, to) => {
  if (
    error.message?.includes('Failed to fetch dynamically imported module') ||
    error.message?.includes('Importing a module script failed')
  ) {
    window.location.href = to.fullPath
  }
})

export default router
