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
          @assign="openAssign"
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

  <AssignCooperatorModal
    v-if="assignSlot"
    :slot="assignSlot"
    @close="assignSlot = null"
    @assigned="onAssigned"
  />

  <div
    v-if="toast"
    class="fixed bottom-4 right-4 z-50 rounded-lg border border-green-200 bg-green-50 px-4 py-2 text-sm text-green-800 shadow"
  >
    {{ toast }}
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AdminSlotCard from './AdminSlotCard.vue'
import AssignCooperatorModal from './AssignCooperatorModal.vue'
import { sortedDays, dayLabel, slotsForWeekAndDay } from '@/composables/useSlots'
import type { AdminSlotResponse } from '@/composables/useAdmin'

const props = defineProps<{
  slots: AdminSlotResponse[]
  activeWeek: string
  showOnlyUnderMin: boolean
}>()

const assignSlot = ref<AdminSlotResponse | null>(null)
const toast = ref<string | null>(null)

function slotsForDay(day: string): AdminSlotResponse[] {
  return slotsForWeekAndDay(props.slots, props.activeWeek, day)
}

function isDimmed(slot: AdminSlotResponse): boolean {
  return props.showOnlyUnderMin && slot.status !== 'NEEDS_PEOPLE'
}

function openAssign(slotId: string) {
  assignSlot.value = props.slots.find((s) => s.id === slotId) ?? null
}

function onAssigned(payload: { moved: boolean }) {
  toast.value = payload.moved
    ? 'Coopérateur·ice déplacé·e vers ce créneau.'
    : 'Coopérateur·ice affecté·e.'
  setTimeout(() => { toast.value = null }, 4000)
}
</script>
