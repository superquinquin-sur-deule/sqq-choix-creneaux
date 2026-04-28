import { createRouter, createWebHistory } from 'vue-router'
import { customFetch } from '@/api/mutator/custom-fetch'
import {
  ADMIN_ROLES,
  SYNC_ROLES,
  hasAnyRole,
  type MeResponse,
} from '@/composables/useMe'

declare module 'vue-router' {
  interface RouteMeta {
    roles?: readonly string[]
    requiresSynchronizedCooperator?: boolean
  }
}

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
      meta: { requiresSynchronizedCooperator: true },
    },
    {
      path: '/confirmer/:slotId',
      name: 'confirmation',
      component: () => import('@/views/ConfirmationView.vue'),
      props: true,
      meta: { requiresSynchronizedCooperator: true },
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
      meta: { roles: ADMIN_ROLES },
    },
    {
      path: '/admin/sync',
      name: 'admin-sync',
      component: () => import('@/views/admin/SyncView.vue'),
      meta: { roles: SYNC_ROLES },
    },
  ],
})

let cachedMe: MeResponse | null = null

async function fetchMe(): Promise<MeResponse | null> {
  if (cachedMe) return cachedMe
  try {
    const r = await customFetch<{ data: MeResponse }>('/api/me', { method: 'GET' })
    cachedMe = r.data
    return cachedMe
  } catch {
    return null
  }
}

router.beforeEach(async (to) => {
  const required = to.meta.roles
  const requiresSync = to.meta.requiresSynchronizedCooperator === true
  if ((!required || required.length === 0) && !requiresSync) return true
  const me = await fetchMe()
  if (!me) return true
  if (requiresSync && !me.cooperatorSynchronized) {
    return { name: 'home' }
  }
  if (required && required.length > 0 && !hasAnyRole(me.roles, required)) {
    return { name: 'home' }
  }
  return true
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
