<template>
  <div>
    <div class="mb-3 flex items-center justify-between">
      <h2 class="text-lg font-semibold text-dark">
        Coopérateur·ices sans créneau
        <span class="ml-1 text-base font-normal text-brown/60">({{ cooperators.length }})</span>
      </h2>
      <div class="flex gap-2">
        <a
          href="/api/admin/cooperators/export.csv"
          target="_blank"
          class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          Exporter CSV
        </a>
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-lg bg-dark px-3 py-1.5 text-sm font-medium text-white transition hover:bg-brown disabled:opacity-50"
          :disabled="isSending || cooperators.length === 0"
          @click="remindAll"
        >
          <span v-if="isSending">Envoi…</span>
          <span v-else>Relancer tous</span>
        </button>
      </div>
    </div>

    <div v-if="successMessage" class="mb-3 rounded-lg border border-green-200 bg-green-50 px-4 py-2 text-sm text-green-800">
      {{ successMessage }}
    </div>

    <div v-if="cooperators.length === 0" class="rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/50">
      Tous les coopérateur·ices ont choisi un créneau !
    </div>

    <div v-else class="overflow-x-auto rounded-lg border border-gray-200">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left font-medium text-brown/70">Nom</th>
            <th class="px-4 py-3 text-left font-medium text-brown/70">Email</th>
            <th class="px-4 py-3 text-right font-medium text-brown/70">Action</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="coop in cooperators" :key="coop.id" class="bg-white hover:bg-gray-50">
            <td class="px-4 py-3 font-medium text-dark">{{ coop.firstName }} {{ coop.lastName }}</td>
            <td class="px-4 py-3 text-brown/70">{{ coop.email }}</td>
            <td class="px-4 py-3 text-right">
              <button
                type="button"
                class="rounded px-2 py-1 text-xs font-medium text-dark transition hover:bg-gray-100 disabled:opacity-50"
                :disabled="isSending"
                @click="remindOne(coop.id)"
              >
                Relancer
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useSendReminders, type CooperatorResponse } from '@/composables/useAdmin'

defineProps<{
  cooperators: CooperatorResponse[]
}>()

const { mutateAsync, isPending: isSending } = useSendReminders()
const successMessage = ref<string | null>(null)

async function remindAll() {
  const result = await mutateAsync({ all: true })
  successMessage.value = `${result.sentCount} rappel${result.sentCount > 1 ? 's' : ''} envoyé${result.sentCount > 1 ? 's' : ''}.`
  setTimeout(() => { successMessage.value = null }, 5000)
}

async function remindOne(id: string) {
  const result = await mutateAsync({ cooperatorIds: [id] })
  successMessage.value = `${result.sentCount} rappel envoyé.`
  setTimeout(() => { successMessage.value = null }, 5000)
}
</script>
