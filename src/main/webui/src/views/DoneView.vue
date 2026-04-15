<template>
  <div class="mx-auto max-w-lg text-center">
    <!-- Green checkmark -->
    <div class="mb-6 flex justify-center">
      <div class="flex h-20 w-20 items-center justify-center rounded-full bg-green-100">
        <svg class="h-10 w-10 text-success" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
        </svg>
      </div>
    </div>

    <h1 class="mb-2 text-2xl font-bold text-dark">Créneau confirmé !</h1>
    <p class="mb-8 text-brown">Merci pour votre engagement. Voici le récapitulatif de votre créneau.</p>

    <!-- Slot summary -->
    <div v-if="slot" class="mb-8 rounded-lg border border-gray-200 bg-white p-6 text-left shadow-sm">
      <h2 class="mb-4 text-lg font-semibold text-dark">Mon créneau</h2>
      <dl class="space-y-3 text-sm">
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

    <div v-else-if="isPendingMe || isPendingSlots" class="mb-8 py-6 text-brown/60">
      Chargement…
    </div>

    <!-- Info about changes -->
    <div class="rounded-lg border border-blue-200 bg-blue-50 px-5 py-4 text-left text-sm text-blue-900">
      <p class="font-medium">Besoin de modifier votre créneau ?</p>
      <p class="mt-1 opacity-80">
        Si vous souhaitez changer votre créneau, contactez le bureau des membres : <a class="underline" href="mailto://bureaudesmembres.deule@superquinquin.net">bureaudesmembres.deule@superquinquin.net</a>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useMe } from '@/composables/useMe'
import { useSlots, dayLabel, formatTime } from '@/composables/useSlots'

const { data: me, isPending: isPendingMe } = useMe()
const { data: slotsData, isPending: isPendingSlots } = useSlots()

const slot = computed(() => {
  if (!me.value?.registeredSlotId || !slotsData.value) return null
  return slotsData.value.slots.find((s) => s.id === me.value!.registeredSlotId) ?? null
})
</script>
