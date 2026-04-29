<template>
  <div class="min-h-screen bg-surface font-sans">
    <div
      v-if="appInfo && !appInfo.production"
      class="fixed top-0 left-0 z-50 w-40 h-40 overflow-hidden pointer-events-none"
    >
      <div
        class="absolute bg-red-600 text-white text-xs font-bold text-center shadow py-1"
        style="width: 200px; top: 35px; left: -55px; transform: rotate(-45deg);"
      >
        version de test
      </div>
    </div>
    <header class="bg-dark shadow-sm">
      <div class="mx-auto max-w-6xl px-4 py-3 flex items-center justify-between">
        <router-link to="/" class="flex items-center gap-2 no-underline">
          <span class="text-xl font-bold text-primary">SuperQuinquin :</span>
          <span class="text-xl font-bold text-white">choix des créneaux</span>
        </router-link>
        <div class="text-sm text-surface/80 flex items-center gap-3">
          <span v-if="me">{{ me.firstName }} {{ me.lastName }}</span>
          <a
            v-if="me"
            href="/api/logout"
            class="text-primary hover:underline"
          >Se déconnecter</a>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-6xl px-4 py-6">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useMe } from '@/composables/useMe'
import { useAppInfo } from '@/composables/useAppInfo'

const { data: me } = useMe()
const { data: appInfo } = useAppInfo()
</script>
