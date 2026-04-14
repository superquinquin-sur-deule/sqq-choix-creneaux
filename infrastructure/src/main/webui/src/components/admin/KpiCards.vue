<template>
  <div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
    <!-- Pending -->
    <div class="rounded-lg border border-orange-200 bg-orange-50 p-4">
      <p class="text-xs font-medium uppercase tracking-wide text-orange-600">En attente</p>
      <p class="mt-1 text-3xl font-bold text-orange-800">{{ dashboard.pendingCooperators }}</p>
      <p class="mt-1 text-xs text-orange-600/70">coopérateur·ices sans créneau</p>
    </div>

    <!-- Registered -->
    <div class="rounded-lg border border-gray-200 bg-dark p-4">
      <p class="text-xs font-medium uppercase tracking-wide text-surface/60">Inscrits</p>
      <p class="mt-1 text-3xl font-bold text-white">{{ dashboard.registeredCooperators }}</p>
      <p class="mt-1 text-xs text-surface/40">sur {{ dashboard.totalCooperators }} total</p>
    </div>

    <!-- Slots under minimum -->
    <div
      class="rounded-lg border p-4"
      :class="dashboard.slotsUnderMinimum > 0 ? 'border-orange-200 bg-orange-50' : 'border-gray-200 bg-gray-50'"
    >
      <p
        class="text-xs font-medium uppercase tracking-wide"
        :class="dashboard.slotsUnderMinimum > 0 ? 'text-orange-600' : 'text-brown/60'"
      >
        Créneaux sous minimum
      </p>
      <p
        class="mt-1 text-3xl font-bold"
        :class="dashboard.slotsUnderMinimum > 0 ? 'text-orange-800' : 'text-dark'"
      >
        {{ dashboard.slotsUnderMinimum }}
      </p>
      <p
        class="mt-1 text-xs"
        :class="dashboard.slotsUnderMinimum > 0 ? 'text-orange-600/70' : 'text-brown/40'"
      >
        créneaux insuffisamment remplis
      </p>
    </div>

    <!-- Phase -->
    <div
      class="rounded-lg border p-4"
      :class="dashboard.allMinimumsReached ? 'border-green-200 bg-green-50' : 'border-gray-200 bg-gray-50'"
    >
      <p
        class="text-xs font-medium uppercase tracking-wide"
        :class="dashboard.allMinimumsReached ? 'text-green-700' : 'text-brown/60'"
      >
        Phase
      </p>
      <p
        class="mt-1 text-3xl font-bold"
        :class="dashboard.allMinimumsReached ? 'text-green-800' : 'text-dark'"
      >
        {{ dashboard.allMinimumsReached ? '2' : '1' }}
      </p>
      <p
        class="mt-1 text-xs"
        :class="dashboard.allMinimumsReached ? 'text-green-700/70' : 'text-brown/40'"
      >
        {{ dashboard.allMinimumsReached ? 'Tous les minimums atteints' : 'Minimums non atteints' }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DashboardResponse } from '@/composables/useAdmin'

defineProps<{
  dashboard: DashboardResponse
}>()
</script>
