<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-dark">Tableau de bord admin</h1>
      <span class="text-xs text-brown/50">Actualisation automatique toutes les 15 s</span>
    </div>

    <div v-if="isPendingDashboard" class="py-12 text-center text-brown/60">Chargement…</div>

    <div v-else-if="isErrorDashboard" class="rounded-lg bg-red-50 p-4 text-alert">
      Impossible de charger le tableau de bord.
    </div>

    <template v-else-if="dashboard">
      <!-- KPI Cards -->
      <KpiCards :dashboard="dashboard" class="mb-8" />

      <!-- Slots needing people -->
      <div class="mb-8">
        <SlotsNeedTable :slots="dashboard.slotsNeedingPeople" />
      </div>

      <!-- Cooperators list -->
      <div>
        <CooperatorsList
          v-if="cooperators"
          :cooperators="cooperators"
        />
        <div v-else-if="isPendingCooperators" class="py-4 text-center text-brown/60">
          Chargement des coopérateur·ices…
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useDashboard, usePendingCooperators } from '@/composables/useAdmin'
import KpiCards from '@/components/admin/KpiCards.vue'
import SlotsNeedTable from '@/components/admin/SlotsNeedTable.vue'
import CooperatorsList from '@/components/admin/CooperatorsList.vue'

const { data: dashboard, isPending: isPendingDashboard, isError: isErrorDashboard } = useDashboard()
const { data: cooperators, isPending: isPendingCooperators } = usePendingCooperators()
</script>
