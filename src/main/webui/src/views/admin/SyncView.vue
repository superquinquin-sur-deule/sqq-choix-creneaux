<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-dark">Synchronisation Odoo</h1>
      <p class="mt-1 text-sm text-brown/70">
        Page accessible uniquement par lien direct.
      </p>
    </div>

    <div class="space-y-4">
      <div class="flex items-center gap-3">
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
          :disabled="isSyncing"
          @click="syncCooperators"
        >
          <span v-if="isSyncing">Synchronisation…</span>
          <span v-else>Synchroniser les coopérateur·ices</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
          :disabled="isPullingSlots"
          @click="pullSlots"
        >
          <span v-if="isPullingSlots">Récupération…</span>
          <span v-else>Récupérer les créneaux d'Odoo</span>
        </button>
      </div>

      <div class="rounded-lg border border-gray-200 p-4">
        <h2 class="text-sm font-semibold text-dark">Pousser une inscription vers Odoo</h2>
        <p class="mt-1 text-xs text-brown/70">
          Choisissez un·e coopérateur·ice ayant déjà choisi un créneau, puis poussez son
          inscription vers Odoo.
        </p>
        <div class="mt-3 flex flex-wrap items-center gap-3">
          <select
            v-model="selectedCooperatorId"
            class="min-w-[18rem] rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-dark"
            :disabled="isLoadingCooperators"
          >
            <option value="">
              {{ isLoadingCooperators ? 'Chargement…' : '— Sélectionner —' }}
            </option>
            <option
              v-for="coop in cooperatorsWithSlot"
              :key="coop.id"
              :value="coop.id"
            >
              {{ coop.firstName }} {{ coop.lastName }}{{ formatSlot(coop.slot) }}
            </option>
          </select>
          <button
            type="button"
            class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
            :disabled="!selectedCooperatorId || isPushingOne"
            @click="pushOne"
          >
            <span v-if="isPushingOne">Envoi…</span>
            <span v-else>Inscrire au créneau</span>
          </button>
        </div>
      </div>

      <p v-if="syncMessage" class="text-sm" :class="messageClass">{{ syncMessage }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  useSyncCooperators,
  useSyncSlots,
  usePendingCooperators,
  usePushOneRegistration,
  type CooperatorSlotResponse,
} from '@/composables/useAdmin'

const syncMessage = ref<string | null>(null)
const messageIsError = ref(false)
const messageClass = computed(() => (messageIsError.value ? 'text-red-700' : 'text-green-700'))

const { mutateAsync: runSync, isPending: isSyncing } = useSyncCooperators()
const { mutateAsync: runPullSlots, isPending: isPullingSlots } = useSyncSlots()
const { mutateAsync: runPushOne, isPending: isPushingOne } = usePushOneRegistration()

const page = ref(1)
const size = ref(500)
const q = ref('')
const withoutSlotOnly = ref(false)
const withSlotOnly = ref(true)

const { data: cooperatorsPage, isLoading: isLoadingCooperators } = usePendingCooperators(
  page, size, q, withoutSlotOnly, undefined, undefined, undefined, withSlotOnly,
)

const cooperatorsWithSlot = computed(() => cooperatorsPage.value?.items ?? [])
const selectedCooperatorId = ref('')

function showMessage(msg: string, error = false) {
  syncMessage.value = msg
  messageIsError.value = error
  setTimeout(() => { syncMessage.value = null }, 5000)
}

const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Lundi', TUESDAY: 'Mardi', WEDNESDAY: 'Mercredi', THURSDAY: 'Jeudi',
  FRIDAY: 'Vendredi', SATURDAY: 'Samedi', SUNDAY: 'Dimanche',
}

function formatSlot(slot: CooperatorSlotResponse | null) {
  if (!slot) return ''
  const day = DAY_LABELS[slot.dayOfWeek] ?? slot.dayOfWeek
  return ` — ${slot.week} ${day} ${slot.startTime}-${slot.endTime}`
}

async function syncCooperators() {
  try {
    const result = await runSync()
    showMessage(`${result.cooperatorsImported} coopérateur·ices synchronisé·es.`)
  } catch {
    showMessage('Échec de la synchronisation.', true)
  }
}

async function pullSlots() {
  try {
    const result = await runPullSlots()
    showMessage(`${result.slotsImported} créneaux récupérés d'Odoo.`)
  } catch {
    showMessage('Échec de la récupération des créneaux.', true)
  }
}

async function pushOne() {
  if (!selectedCooperatorId.value) return
  try {
    const result = await runPushOne(selectedCooperatorId.value)
    if (result.pushed) {
      showMessage('Inscription poussée vers Odoo.')
    } else if (result.reason === 'no_registration') {
      showMessage("Aucune inscription pour ce·tte coopérateur·ice.", true)
    } else if (result.reason === 'missing_odoo_ids') {
      showMessage('IDs Odoo manquants pour ce créneau ou ce·tte coopérateur·ice.', true)
    } else {
      showMessage("Échec de l'envoi vers Odoo.", true)
    }
  } catch {
    showMessage("Échec de l'envoi vers Odoo.", true)
  }
}
</script>
