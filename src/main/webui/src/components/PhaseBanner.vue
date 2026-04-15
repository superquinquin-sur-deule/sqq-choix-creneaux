<template>
  <div
    class="mb-6 flex items-start gap-3 rounded-lg border px-4 py-3"
    :class="isPhase1 ? 'border-amber-300 bg-amber-50 text-amber-900' : 'border-green-300 bg-green-50 text-green-900'"
  >
    <svg class="mt-0.5 h-5 w-5 shrink-0" :class="isPhase1 ? 'text-amber-500' : 'text-success'" fill="currentColor" viewBox="0 0 20 20">
      <path
        v-if="isPhase1"
        fill-rule="evenodd"
        d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
        clip-rule="evenodd"
      />
      <path
        v-else
        fill-rule="evenodd"
        d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
        clip-rule="evenodd"
      />
    </svg>
    <div>
      <p class="font-semibold">
        {{ isPhase1 ? 'Phase 1 – Remplir les créneaux prioritaires' : 'Phase 2 – Tous les créneaux sont disponibles' }}
      </p>
      <p class="mt-0.5 text-sm opacity-80">
        <template v-if="isPhase1">
          {{ slotsNeedingCount }} créneau{{ slotsNeedingCount > 1 ? 'x' : '' }} n'ont pas encore atteint leur minimum.
          Privilégiez les créneaux marqués <strong>« Besoin ! »</strong>.
        </template>
        <template v-else>
          Tous les minimums sont atteints. Choisissez librement parmi tous les créneaux ouverts.
        </template>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  slotsNeedingCount: number
}>()

const isPhase1 = computed(() => props.slotsNeedingCount > 0)
</script>
