<template>
  <div>
    <h1 class="mb-2 text-2xl font-bold text-dark">Choisir mon créneau</h1>
    <p class="mb-6 text-sm text-brown">Sélectionnez un créneau de bénévolat parmi les disponibles.</p>

    <div class="mb-6 rounded-lg border border-primary/40 bg-primary/10 px-4 py-3 text-sm text-dark">
      <p class="font-semibold">Comment ça marche ?</p>
      <p class="mt-1">
        Les semaines se répètent selon un cycle de 4 semaines (A → B → C → D → A…).
        Choisissez la semaine qui vous convient : <strong>votre créneau se répétera toutes les 4 semaines</strong>, toujours le même jour et à la même heure.
      </p>
    </div>

    <div v-if="isPending" class="py-12 text-center text-brown/60">Chargement des créneaux…</div>

    <div v-else-if="isError" class="rounded-lg bg-red-50 p-4 text-alert">
      Impossible de charger les créneaux. Veuillez réessayer.
    </div>

    <template v-else-if="data">
      <PhaseBanner :slots-needing-count="slotsNeedingCount" />

      <WeekTabs
        v-model:activeWeek="activeWeek"
        :slots="data.slots"
        :campaign="data.campaign"
      />

      <p class="mb-2 text-sm font-medium text-dark">Étape 2 — Choisissez votre créneau</p>
      <SlotCalendar
        :slots="data.slots"
        :active-week="activeWeek"
        :selected-slot-id="selectedSlotId"
        @select="handleSelect"
      />

      <div v-if="selectedSlotId" class="mt-8 flex justify-end">
        <router-link
          :to="`/confirmer/${selectedSlotId}`"
          class="inline-block rounded-lg bg-dark px-6 py-3 font-semibold text-white transition hover:bg-brown"
        >
          Continuer vers la confirmation →
        </router-link>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useSlots, type SlotResponse } from '@/composables/useSlots'
import PhaseBanner from '@/components/PhaseBanner.vue'
import WeekTabs from '@/components/WeekTabs.vue'
import SlotCalendar from '@/components/SlotCalendar.vue'

const { data, isPending, isError } = useSlots()

const activeWeek = ref('A')
const selectedSlotId = ref<string | null>(null)

const slotsNeedingCount = computed(
  () => data.value?.slots.filter((s) => s.status === 'NEEDS_PEOPLE').length ?? 0,
)

let autoWeekPicked = false
watch(
  data,
  (d) => {
    if (autoWeekPicked || !d?.slots?.length) return
    autoWeekPicked = true
    activeWeek.value = leastFilledWeek(d.slots) ?? activeWeek.value
  },
  { immediate: true },
)

function leastFilledWeek(slots: SlotResponse[]): string | null {
  const stats = new Map<string, { filled: number; capacity: number }>()
  for (const s of slots) {
    const cur = stats.get(s.week) ?? { filled: 0, capacity: 0 }
    cur.filled += s.registrationCount
    cur.capacity += s.maxCapacity
    stats.set(s.week, cur)
  }
  let best: string | null = null
  let bestRatio = Infinity
  for (const [week, { filled, capacity }] of stats) {
    if (capacity === 0) continue
    const ratio = filled / capacity
    if (ratio < bestRatio) {
      bestRatio = ratio
      best = week
    }
  }
  return best
}

function handleSelect(id: string) {
  selectedSlotId.value = selectedSlotId.value === id ? null : id
}
</script>
