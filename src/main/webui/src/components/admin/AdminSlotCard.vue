<template>
  <div
    class="rounded-lg border-2 p-3 transition-opacity duration-300"
    :class="[cardClass, dimmed ? 'opacity-30' : 'opacity-100']"
  >
    <div class="flex items-start justify-between gap-2">
      <p class="text-sm font-semibold text-dark">
        {{ formatTime(slot.startTime) }} – {{ formatTime(slot.endTime) }}
      </p>
      <span
        v-if="slot.status === 'NEEDS_PEOPLE'"
        class="inline-flex shrink-0 items-center rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800"
      >
        Besoin !
      </span>
      <span
        v-else-if="slot.status === 'LOCKED'"
        class="inline-flex shrink-0 items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500"
      >
        Verrouillé
      </span>
      <span
        v-else-if="slot.status === 'FULL'"
        class="inline-flex shrink-0 items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500"
      >
        Complet
      </span>
    </div>

    <div class="mt-2">
      <FillBar :current="slot.registrationCount" :min="slot.minCapacity" :max="slot.maxCapacity" />
      <p class="mt-1 text-xs text-brown/70">
        {{ slot.registrationCount }} / {{ slot.maxCapacity }}
        <span class="text-brown/50">(min {{ slot.minCapacity }})</span>
      </p>
    </div>

    <div class="mt-2">
      <div v-if="slot.registrants.length > 0" class="flex flex-wrap gap-1">
        <span
          v-for="(r, i) in slot.registrants"
          :key="i"
          class="inline-flex items-center rounded bg-surface px-1.5 py-0.5 text-xs text-dark"
        >
          {{ r.firstName }}<span v-if="r.lastNameInitial"> {{ r.lastNameInitial }}</span>
        </span>
      </div>
    </div>

    <div v-if="slot.status !== 'FULL'" class="mt-2 flex justify-end">
      <button
        type="button"
        class="rounded px-2 py-0.5 text-xs font-medium text-dark transition hover:bg-white/70"
        @click="$emit('assign', slot.id)"
      >
        + Ajouter
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FillBar from '@/components/FillBar.vue'
import { formatTime } from '@/composables/useSlots'
import type { AdminSlotResponse } from '@/composables/useAdmin'

const props = defineProps<{
  slot: AdminSlotResponse
  dimmed?: boolean
}>()

defineEmits<{ assign: [string] }>()

const cardClass = computed(() => {
  switch (props.slot.status) {
    case 'NEEDS_PEOPLE':
      return 'border-primary bg-amber-50'
    case 'LOCKED':
      return 'border-gray-200 bg-gray-50'
    case 'FULL':
      return 'border-gray-200 bg-gray-50'
    case 'OPEN':
    default:
      return 'border-gray-200 bg-white'
  }
})
</script>
