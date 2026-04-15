<template>
  <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6">
    <div v-for="day in sortedDays()" :key="day">
      <h3 class="mb-2 text-center text-sm font-semibold uppercase tracking-wide text-brown/70">
        {{ dayLabel(day) }}
      </h3>
      <div class="space-y-2">
        <AdminSlotCard
          v-for="slot in slotsForDay(day)"
          :key="slot.id"
          :slot="slot"
          :dimmed="isDimmed(slot)"
        />
        <p
          v-if="slotsForDay(day).length === 0"
          class="rounded-lg border border-dashed border-gray-200 py-4 text-center text-xs text-brown/40"
        >
          —
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AdminSlotCard from './AdminSlotCard.vue'
import { sortedDays, dayLabel, slotsForWeekAndDay } from '@/composables/useSlots'
import type { AdminSlotResponse } from '@/composables/useAdmin'

const props = defineProps<{
  slots: AdminSlotResponse[]
  activeWeek: string
  showOnlyUnderMin: boolean
}>()

function slotsForDay(day: string): AdminSlotResponse[] {
  return slotsForWeekAndDay(props.slots, props.activeWeek, day)
}

function isDimmed(slot: AdminSlotResponse): boolean {
  return props.showOnlyUnderMin && slot.status !== 'NEEDS_PEOPLE'
}
</script>
