<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-dark">Synchronisation Odoo</h1>
      <p class="mt-1 text-sm text-brown/70">
        Page accessible uniquement par lien direct.
      </p>
    </div>

    <div class="space-y-4">
      <div class="flex items-center gap-3">
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
          :disabled="isSyncing"
          @click="syncCooperators"
        >
          <span v-if="isSyncing">Synchronisation…</span>
          <span v-else>Synchroniser les coopérateur·ices</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
          :disabled="isPullingSlots"
          @click="pullSlots"
        >
          <span v-if="isPullingSlots">Récupération…</span>
          <span v-else>Récupérer les créneaux d'Odoo</span>
        </button>
      </div>

      <p v-if="syncMessage" class="text-sm text-green-700">{{ syncMessage }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSyncCooperators, useSyncSlots } from '@/composables/useAdmin'

const syncMessage = ref<string | null>(null)

const { mutateAsync: runSync, isPending: isSyncing } = useSyncCooperators()
const { mutateAsync: runPullSlots, isPending: isPullingSlots } = useSyncSlots()

async function syncCooperators() {
  try {
    const result = await runSync()
    syncMessage.value = `${result.cooperatorsImported} coopérateur·ices synchronisé·es.`
  } catch {
    syncMessage.value = 'Échec de la synchronisation.'
  }
  setTimeout(() => { syncMessage.value = null }, 5000)
}

async function pullSlots() {
  try {
    const result = await runPullSlots()
    syncMessage.value = `${result.slotsImported} créneaux récupérés d'Odoo.`
  } catch {
    syncMessage.value = 'Échec de la récupération des créneaux.'
  }
  setTimeout(() => { syncMessage.value = null }, 5000)
}
</script>
