<template>
  <div>
    <h2 class="mb-3 text-lg font-semibold text-dark">Créneaux sous minimum</h2>

    <div v-if="slots.length === 0" class="rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/50">
      Tous les créneaux ont atteint leur minimum.
    </div>

    <div v-else class="overflow-x-auto rounded-lg border border-gray-200">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left font-medium text-brown/70">Semaine</th>
            <th class="px-4 py-3 text-left font-medium text-brown/70">Jour</th>
            <th class="px-4 py-3 text-left font-medium text-brown/70">Horaire</th>
            <th class="px-4 py-3 text-left font-medium text-brown/70">Remplissage</th>
            <th class="px-4 py-3 text-right font-medium text-brown/70">Inscrits</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-for="slot in slots" :key="slot.id" class="bg-white hover:bg-gray-50">
            <td class="px-4 py-3 font-medium text-dark">{{ slot.week }}</td>
            <td class="px-4 py-3 text-dark">{{ dayLabel(slot.dayOfWeek) }}</td>
            <td class="px-4 py-3 text-dark">{{ formatTime(slot.startTime) }} – {{ formatTime(slot.endTime) }}</td>
            <td class="px-4 py-3">
              <div class="w-32">
                <FillBar :current="slot.registrationCount" :min="slot.minCapacity" :max="slot.maxCapacity" />
              </div>
            </td>
            <td class="px-4 py-3 text-right text-dark">
              {{ slot.registrationCount }} / {{ slot.maxCapacity }}
              <span class="text-brown/50">(min {{ slot.minCapacity }})</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import FillBar from '@/components/FillBar.vue'
import { dayLabel, formatTime, type SlotResponse } from '@/composables/useSlots'

defineProps<{
  slots: SlotResponse[]
}>()
</script>
