<template>
  <div>
    <h1 class="mb-2 text-2xl font-bold text-dark">Choisir mon créneau</h1>
    <p class="mb-6 text-sm text-brown">Sélectionnez un créneau de bénévolat parmi les disponibles.</p>

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
import { ref, computed } from 'vue'
import { useSlots } from '@/composables/useSlots'
import PhaseBanner from '@/components/PhaseBanner.vue'
import WeekTabs from '@/components/WeekTabs.vue'
import SlotCalendar from '@/components/SlotCalendar.vue'

const { data, isPending, isError } = useSlots()

const activeWeek = ref('A')
const selectedSlotId = ref<string | null>(null)

const slotsNeedingCount = computed(
  () => data.value?.slots.filter((s) => s.status === 'NEEDS_PEOPLE').length ?? 0,
)

function handleSelect(id: string) {
  selectedSlotId.value = selectedSlotId.value === id ? null : id
}
</script>
