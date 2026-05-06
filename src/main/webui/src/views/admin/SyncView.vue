<template>
  <div>
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-dark">Synchronisation Odoo</h1>
      <p class="mt-1 text-sm text-brown/70">
        Page accessible uniquement par lien direct.
      </p>
    </div>

    <div class="space-y-4">
      <div class="flex flex-wrap items-center gap-3">
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
        <div class="inline-flex items-center gap-2">
          <button
            type="button"
            class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
            :disabled="isStreaming"
            @click="pushAll"
          >
            <span v-if="isStreaming">
              Envoi… {{ progress.processed }}/{{ progress.total }}
            </span>
            <span v-else>Pousser toutes les inscriptions vers Odoo</span>
          </button>
          <div class="flex items-center gap-1 rounded-lg border border-gray-300 px-2 py-1 text-xs text-dark">
            <span class="text-brown/70">Semaines&nbsp;:</span>
            <label class="flex items-center gap-1">
              <input
                type="checkbox"
                :checked="allWeeksSelected"
                :disabled="isStreaming"
                @change="toggleAllWeeks(($event.target as HTMLInputElement).checked)"
              />
              <span>Toutes</span>
            </label>
            <label
              v-for="w in WEEK_OPTIONS"
              :key="w"
              class="flex items-center gap-1"
            >
              <input
                type="checkbox"
                :value="w"
                :checked="selectedWeeks.includes(w)"
                :disabled="isStreaming"
                @change="toggleWeek(w, ($event.target as HTMLInputElement).checked)"
              />
              <span>{{ w }}</span>
            </label>
          </div>
        </div>
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

      <div v-if="logs.length > 0" class="rounded-lg border border-gray-200 bg-gray-900 p-3">
        <div class="mb-2 flex items-center justify-between">
          <h2 class="text-xs font-semibold uppercase tracking-wide text-gray-300">
            Journal de synchronisation
          </h2>
          <button
            type="button"
            class="text-xs text-gray-400 hover:text-white"
            @click="clearLogs"
            :disabled="isStreaming"
          >
            Effacer
          </button>
        </div>
        <pre
          ref="logPanel"
          class="max-h-80 overflow-y-auto whitespace-pre-wrap break-words font-mono text-xs leading-relaxed"
        ><span
          v-for="(line, idx) in logs"
          :key="idx"
          :class="logColor(line.level)"
        >{{ formatLine(line) }}
</span></pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
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

const WEEK_OPTIONS = ['A', 'B', 'C', 'D'] as const
type WeekOption = (typeof WEEK_OPTIONS)[number]
const selectedWeeks = ref<WeekOption[]>([...WEEK_OPTIONS])
const allWeeksSelected = computed(() => selectedWeeks.value.length === WEEK_OPTIONS.length)

function toggleAllWeeks(checked: boolean) {
  selectedWeeks.value = checked ? [...WEEK_OPTIONS] : []
}

function toggleWeek(week: WeekOption, checked: boolean) {
  if (checked) {
    if (!selectedWeeks.value.includes(week)) {
      selectedWeeks.value = [...selectedWeeks.value, week]
    }
  } else {
    selectedWeeks.value = selectedWeeks.value.filter((w) => w !== week)
  }
}

interface LogLine {
  level: string
  message: string
  processed: number
  total: number
}

const logs = ref<LogLine[]>([])
const isStreaming = ref(false)
const progress = ref({ processed: 0, total: 0 })
const logPanel = ref<HTMLElement | null>(null)
let eventSource: EventSource | null = null

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

function logColor(level: string) {
  switch (level) {
    case 'error': return 'text-red-400'
    case 'warn': return 'text-yellow-300'
    default: return 'text-gray-100'
  }
}

function formatLine(line: LogLine) {
  const prefix = line.total > 0 ? `[${line.processed}/${line.total}] ` : ''
  return `${prefix}${line.message}`
}

async function appendLog(line: LogLine) {
  logs.value.push(line)
  progress.value = { processed: line.processed, total: line.total }
  await nextTick()
  const el = logPanel.value
  if (el) el.scrollTop = el.scrollHeight
}

function closeStream() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  isStreaming.value = false
}

function clearLogs() {
  logs.value = []
  progress.value = { processed: 0, total: 0 }
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
      const outcomeLabel: Record<string, string> = {
        created: 'Inscription créée dans Odoo.',
        moved: 'Inscription déplacée vers le nouveau créneau.',
        unchanged: 'Déjà à jour, rien à faire.',
      }
      showMessage(outcomeLabel[result.outcome ?? ''] ?? 'Inscription poussée vers Odoo.')
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

function pushAll() {
  if (isStreaming.value) return
  if (selectedWeeks.value.length === 0) {
    showMessage('Sélectionnez au moins une semaine.', true)
    return
  }
  clearLogs()
  isStreaming.value = true
  const params = allWeeksSelected.value
    ? ''
    : '?' + selectedWeeks.value.map((w) => `week=${w}`).join('&')
  const es = new EventSource('/api/admin/sync/push-stream' + params, { withCredentials: true })
  eventSource = es
  es.onmessage = (event) => {
    try {
      const line = JSON.parse(event.data) as LogLine
      void appendLog(line)
    } catch (e) {
      console.error('Failed to parse SSE event', e, event.data)
    }
  }
  es.onerror = () => {
    // EventSource fires onerror on normal close as well; treat readyState=CLOSED as end.
    if (es.readyState === EventSource.CLOSED) {
      closeStream()
    } else {
      void appendLog({
        level: 'error',
        message: 'Connexion au flux interrompue.',
        processed: progress.value.processed,
        total: progress.value.total,
      })
      closeStream()
    }
  }
}

onBeforeUnmount(() => {
  closeStream()
})
</script>
