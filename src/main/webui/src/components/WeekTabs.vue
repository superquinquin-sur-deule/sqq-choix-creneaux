<template>
  <div>
    <p class="mb-2 text-sm font-medium text-dark">Étape 1 — Choisissez votre semaine</p>
    <div class="mb-2 flex flex-wrap gap-2">
      <button
        v-for="week in weeks"
        :key="week"
        type="button"
        class="flex flex-col items-center rounded-lg border-2 px-4 py-2 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary/50"
        :class="
          activeWeek === week
            ? 'border-dark bg-primary text-dark font-semibold'
            : 'border-gray-200 bg-white text-brown hover:border-primary hover:bg-primary/10'
        "
        @click="emit('update:activeWeek', week)"
      >
        <span class="text-lg font-bold">Semaine {{ week }}</span>
        <span v-if="campaign" class="text-xs opacity-70">
          {{ dateRangeForWeek(week) }}
        </span>
        <span
          v-if="needingCountForWeek(week) > 0"
          class="mt-1 inline-flex items-center rounded-full bg-amber-400 px-2 py-0.5 text-xs font-medium text-amber-900"
        >
          {{ needingCountForWeek(week) }} besoin{{ needingCountForWeek(week) > 1 ? 's' : '' }}
        </span>
      </button>
    </div>
    <p class="mb-6 text-xs text-brown/60">
      Les dates indiquées correspondent à votre premier créneau. Il se répétera ensuite toutes les 4 semaines.
    </p>
  </div>
</template>

<script setup lang="ts">
import { firstMondayAfterOpening, type SlotResponse } from '@/composables/useSlots'

const weeks = ['A', 'B', 'C', 'D']

const props = defineProps<{
  activeWeek: string
  slots: SlotResponse[]
  campaign: { storeOpening: string; weekAReference: string } | null
}>()

const emit = defineEmits<{
  (e: 'update:activeWeek', week: string): void
}>()

function dateRangeForWeek(week: string): string {
  if (!props.campaign) return ''
  return firstMondayAfterOpening(week, props.campaign.storeOpening, props.campaign.weekAReference)
}

function needingCountForWeek(week: string): number {
  return props.slots.filter((s) => s.week === week && s.status === 'NEEDS_PEOPLE').length
}
</script>
