<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-2xl font-bold text-dark">Tableau de bord BDM</h1>
      <span class="text-xs text-brown/50">Actualisation automatique toutes les 15 s</span>
    </div>

    <div v-if="isPendingDashboard" class="py-12 text-center text-brown/60">Chargement…</div>

    <div v-else-if="isErrorDashboard" class="rounded-lg bg-red-50 p-4 text-alert">
      Impossible de charger le tableau de bord.
    </div>

    <template v-else-if="dashboard">
      <!-- KPI Cards -->
      <KpiCards :dashboard="dashboard" class="mb-8" />

      <!-- Calendar -->
      <section class="mb-8">
        <div class="mb-3 flex items-center justify-between gap-4">
          <h2 class="text-lg font-semibold text-dark">Vue calendrier</h2>
          <label class="flex cursor-pointer items-center gap-2 text-sm text-dark">
            <input
              v-model="showOnlyUnderMin"
              type="checkbox"
              class="h-4 w-4 rounded border-gray-300 text-dark focus:ring-primary/50"
            />
            Afficher uniquement les créneaux sous minimum
          </label>
        </div>

        <!-- Week tabs -->
        <div class="mb-3 flex flex-wrap gap-2">
          <button
            v-for="week in weeks"
            :key="week"
            type="button"
            class="flex items-center gap-2 rounded-lg border-2 px-3 py-1.5 text-sm font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary/50"
            :class="
              activeWeek === week
                ? 'border-dark bg-primary text-dark'
                : 'border-gray-200 bg-white text-brown hover:border-primary hover:bg-primary/10'
            "
            @click="activeWeek = week"
          >
            Semaine {{ week }}
            <span
              v-if="needingCountForWeek(week) > 0"
              class="inline-flex items-center rounded-full bg-amber-400 px-2 py-0.5 text-xs font-semibold text-amber-900"
            >
              {{ needingCountForWeek(week) }}
            </span>
          </button>
        </div>

        <!-- Calendar grid -->
        <div v-if="isPendingSlots" class="py-8 text-center text-brown/60">Chargement des créneaux…</div>
        <div v-else-if="isErrorSlots" class="rounded-lg bg-red-50 p-4 text-alert">
          Impossible de charger les créneaux.
        </div>
        <AdminSlotCalendar
          v-else-if="adminSlots"
          :slots="adminSlots"
          :active-week="activeWeek"
          :show-only-under-min="showOnlyUnderMin"
        />
      </section>

      <!-- Cooperators list -->
      <CooperatorsList />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useDashboard, useAdminSlots } from '@/composables/useAdmin'
import KpiCards from '@/components/admin/KpiCards.vue'
import CooperatorsList from '@/components/admin/CooperatorsList.vue'
import AdminSlotCalendar from '@/components/admin/AdminSlotCalendar.vue'

const { data: dashboard, isPending: isPendingDashboard, isError: isErrorDashboard } = useDashboard()
const { data: adminSlots, isPending: isPendingSlots, isError: isErrorSlots } = useAdminSlots()

const weeks = ['A', 'B', 'C', 'D'] as const
const activeWeek = ref<string>('A')
const showOnlyUnderMin = ref(false)

function needingCountForWeek(week: string): number {
  return (adminSlots.value ?? []).filter((s) => s.week === week && s.status === 'NEEDS_PEOPLE').length
}
</script>
