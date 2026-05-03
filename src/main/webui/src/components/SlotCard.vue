<template>
  <button
    type="button"
    class="w-full rounded-lg border-2 p-3 text-left transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary/50"
    :class="cardClass"
    :disabled="slot.status === 'LOCKED' || slot.status === 'FULL'"
    @click="emit('select', slot.id)"
  >
    <!-- Time -->
    <p class="text-sm font-semibold text-dark">
      {{ formatTime(slot.startTime) }} – {{ formatTime(slot.endTime) }}
    </p>

    <!-- Fill bar -->
    <div class="mt-2">
      <FillBar :current="slot.registrationCount" :min="slot.minCapacity" :max="slot.maxCapacity" />
      <p class="mt-1 text-xs text-brown/70">
        {{ slot.registrationCount }} / {{ slot.maxCapacity }}
        <span v-if="slot.minCapacity">(min {{ slot.minCapacity }})</span>
      </p>
    </div>

    <!-- Status badge -->
    <div class="mt-2 flex items-center gap-1">
      <span
        v-if="slot.status === 'NEEDS_PEOPLE'"
        class="inline-flex items-center rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800"
      >
        Besoin !
      </span>
      <span
        v-else-if="slot.status === 'LOCKED'"
        class="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500"
      >
        <svg class="h-3 w-3" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd" />
        </svg>
        Verrouillé
      </span>
      <span
        v-else-if="slot.status === 'FULL'"
        class="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-400"
      >
        Complet
      </span>
    </div>

    <!-- Locked explanation -->
    <p v-if="slot.status === 'LOCKED'" class="mt-1 text-xs text-brown/50 italic">
      Ce créneau est verrouillé en attendant que le minimum soit atteint ailleurs.
    </p>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FillBar from './FillBar.vue'
import { formatTime, type SlotResponse } from '@/composables/useSlots'

const props = defineProps<{
  slot: SlotResponse
  selected?: boolean
}>()

const emit = defineEmits<{
  (e: 'select', id: string): void
}>()

const cardClass = computed(() => {
  if (props.selected) return 'border-success bg-green-50 opacity-100'
  if (props.slot.registrationCount === 0 && props.slot.status !== 'LOCKED') {
    return 'border-red-500 bg-red-50 opacity-100 hover:bg-red-100 cursor-pointer'
  }
  switch (props.slot.status) {
    case 'NEEDS_PEOPLE':
      return 'border-primary bg-amber-50 opacity-100 hover:bg-amber-100 cursor-pointer'
    case 'LOCKED':
      return 'border-gray-200 bg-gray-100 opacity-35 cursor-not-allowed'
    case 'OPEN':
      return 'border-gray-200 bg-white opacity-55 hover:opacity-80 hover:border-gray-300 cursor-pointer'
    case 'FULL':
      return 'border-gray-200 bg-gray-50 opacity-25 cursor-not-allowed'
    default:
      return 'border-gray-200 bg-white cursor-pointer'
  }
})
</script>
