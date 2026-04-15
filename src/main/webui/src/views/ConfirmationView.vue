<template>
  <div class="mx-auto max-w-lg">
    <h1 class="mb-6 text-2xl font-bold text-dark">Confirmer mon créneau</h1>

    <div v-if="isPending" class="py-12 text-center text-brown/60">Chargement…</div>

    <template v-else-if="slot">
      <!-- Slot summary -->
      <div class="mb-6 rounded-lg border border-gray-200 bg-white p-5 shadow-sm">
        <h2 class="mb-3 text-lg font-semibold text-dark">Récapitulatif</h2>
        <dl class="space-y-2 text-sm">
          <div class="flex justify-between">
            <dt class="text-brown/70">Semaine</dt>
            <dd class="font-medium text-dark">{{ slot.week }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-brown/70">Jour</dt>
            <dd class="font-medium text-dark">{{ dayLabel(slot.dayOfWeek) }}</dd>
          </div>
          <div class="flex justify-between">
            <dt class="text-brown/70">Horaire</dt>
            <dd class="font-medium text-dark">{{ formatTime(slot.startTime) }} – {{ formatTime(slot.endTime) }}</dd>
          </div>
        </dl>
      </div>

      <!-- Warning -->
      <div class="mb-6 flex items-start gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-amber-900">
        <svg class="mt-0.5 h-5 w-5 shrink-0 text-amber-500" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd" />
        </svg>
        <p class="text-sm font-medium">Ce choix est définitif. Vous ne pourrez pas le modifier vous-même.</p>
      </div>

      <!-- Error display -->
      <div v-if="errorMessage" class="mb-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-alert">
        {{ errorMessage }}
      </div>

      <!-- Actions -->
      <div class="flex gap-3">
        <router-link
          to="/choisir"
          class="flex-1 rounded-lg border border-gray-300 px-4 py-3 text-center font-medium text-dark transition hover:bg-gray-50"
        >
          ← Retour
        </router-link>
        <button
          type="button"
          class="flex-1 rounded-lg bg-dark px-4 py-3 font-semibold text-white transition hover:bg-brown disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="isConfirming"
          @click="confirm"
        >
          <span v-if="isConfirming">Confirmation…</span>
          <span v-else>Confirmer mon choix</span>
        </button>
      </div>
    </template>

    <div v-else-if="!isPending" class="rounded-lg bg-red-50 p-4 text-alert">
      Créneau introuvable.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQueryClient } from '@tanstack/vue-query'
import { customFetch } from '@/api/mutator/custom-fetch'
import { ApiError } from '@/api/mutator/custom-fetch'
import { useSlots, dayLabel, formatTime } from '@/composables/useSlots'

const props = defineProps<{
  slotId: string
}>()

const router = useRouter()
const queryClient = useQueryClient()

const { data: slotsData, isPending } = useSlots()

const slot = computed(() => slotsData.value?.slots.find((s) => s.id === props.slotId) ?? null)

const isConfirming = ref(false)
const errorMessage = ref<string | null>(null)

async function confirm() {
  isConfirming.value = true
  errorMessage.value = null
  try {
    await customFetch(`/api/slots/${props.slotId}/register`, { method: 'POST' })
    await queryClient.invalidateQueries({ queryKey: ['me'] })
    await queryClient.invalidateQueries({ queryKey: ['slots'] })
    router.push('/termine')
  } catch (err) {
    if (err instanceof ApiError) {
      errorMessage.value = err.message
    } else {
      errorMessage.value = 'Une erreur inattendue est survenue.'
    }
  } finally {
    isConfirming.value = false
  }
}
</script>
