<template>
  <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6">
    <div v-for="day in sortedDays()" :key="day">
      <h3 class="mb-2 text-center text-sm font-semibold uppercase tracking-wide text-brown/70">
        {{ dayLabel(day) }}
      </h3>
      <div class="space-y-2">
        <SlotCard
          v-for="slot in slotsForDay(day)"
          :key="slot.id"
          :slot="slot"
          :selected="selectedSlotId === slot.id"
          @select="emit('select', $event)"
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
import SlotCard from './SlotCard.vue'
import { sortedDays, dayLabel, slotsForWeekAndDay, type SlotResponse } from '@/composables/useSlots'

const props = defineProps<{
  slots: SlotResponse[]
  activeWeek: string
  selectedSlotId: string | null
}>()

const emit = defineEmits<{
  (e: 'select', id: string): void
}>()

function slotsForDay(day: string): SlotResponse[] {
  return slotsForWeekAndDay(props.slots, props.activeWeek, day)
}
</script>
