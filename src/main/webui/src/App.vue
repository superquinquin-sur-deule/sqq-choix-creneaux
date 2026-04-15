<template>
  <div class="min-h-screen bg-surface font-sans">
    <header class="bg-dark shadow-sm">
      <div class="mx-auto max-w-6xl px-4 py-3 flex items-center justify-between">
        <router-link to="/" class="flex items-center gap-2 no-underline">
          <span class="text-xl font-bold text-primary">SuperQuinquin :</span>
          <span class="text-xl font-bold text-white">choix des créneaux</span>
        </router-link>
        <div class="text-sm text-surface/80">
          <span v-if="me">{{ me.firstName }} {{ me.lastName }}</span>
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

const { data: me } = useMe()
</script>
