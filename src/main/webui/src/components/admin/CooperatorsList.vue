<template>
  <div>
    <div class="mb-3 flex items-center justify-between">
      <h2 class="text-lg font-semibold text-dark">
        {{ titleByFilter[filter] }}
        <span class="ml-1 text-base font-normal text-brown/60">({{ total }})</span>
      </h2>
      <div class="flex gap-2">
        <a
          href="/api/admin/cooperators/export.csv"
          target="_blank"
          class="inline-flex items-center gap-1 rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          Exporter CSV
        </a>
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-lg border border-dark px-3 py-1.5 text-sm font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
          :disabled="isSending || total === 0"
          @click="inviteNew"
        >
          <span v-if="isSending">Envoi…</span>
          <span v-else>Inviter les nouveaux coops</span>
        </button>
        <button
          type="button"
          class="inline-flex items-center gap-1 rounded-lg bg-dark px-3 py-1.5 text-sm font-medium text-white transition hover:bg-brown disabled:opacity-50"
          :disabled="isSending || total === 0"
          @click="remindAll"
        >
          <span v-if="isSending">Envoi…</span>
          <span v-else>Relancer tous</span>
        </button>
      </div>
    </div>

    <div class="mb-3 flex flex-wrap items-center gap-3">
      <div class="relative min-w-0 flex-1">
        <input
          v-model="search"
          type="search"
          placeholder="Rechercher par nom ou email…"
          class="w-full rounded-lg border border-gray-300 px-3 py-2 pr-9 text-sm text-dark placeholder-brown/40 focus:border-dark focus:outline-none [&::-webkit-search-cancel-button]:hidden"
        />
        <button
          v-if="search"
          type="button"
          aria-label="Effacer la recherche"
          class="absolute inset-y-0 right-0 flex items-center px-3 text-brown/50 hover:text-dark"
          @click="search = ''"
        >
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
      <div class="inline-flex shrink-0 rounded-lg border border-gray-300 p-0.5 text-sm">
        <button
          type="button"
          class="rounded-md px-3 py-1 font-medium transition"
          :class="filter === 'pending' ? 'bg-dark text-white' : 'text-dark hover:bg-gray-100'"
          @click="filter = 'pending'"
        >
          Sans créneau
        </button>
        <button
          type="button"
          class="rounded-md px-3 py-1 font-medium transition"
          :class="filter === 'new' ? 'bg-dark text-white' : 'text-dark hover:bg-gray-100'"
          @click="filter = 'new'"
        >
          Nouveaux coops
        </button>
        <button
          type="button"
          class="rounded-md px-3 py-1 font-medium transition"
          :class="filter === 'withSlot' ? 'bg-dark text-white' : 'text-dark hover:bg-gray-100'"
          @click="filter = 'withSlot'"
        >
          Avec créneau
        </button>
        <button
          type="button"
          class="rounded-md px-3 py-1 font-medium transition"
          :class="filter === 'exempted' ? 'bg-dark text-white' : 'text-dark hover:bg-gray-100'"
          @click="filter = 'exempted'"
        >
          Exempté·es
        </button>
        <button
          type="button"
          class="rounded-md px-3 py-1 font-medium transition"
          :class="filter === 'all' ? 'bg-dark text-white' : 'text-dark hover:bg-gray-100'"
          @click="filter = 'all'"
        >
          Tous
        </button>
      </div>
    </div>

    <div v-if="successMessage" class="mb-3 rounded-lg border border-green-200 bg-green-50 px-4 py-2 text-sm text-green-800">
      {{ successMessage }}
    </div>

    <div class="flex min-h-[34rem] flex-col">
      <div v-if="isPending && !pageData" class="flex-1 rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/60">
        Chargement des coopérateur·ices…
      </div>

      <div v-else-if="total === 0 && search.trim()" class="flex-1 rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/50">
        Aucun coopérateur·ice trouvé·e pour « {{ search }} ».
      </div>

      <div v-else-if="total === 0 && filter === 'pending'" class="flex-1 rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/50">
        Tous les coopérateur·ices ont choisi un créneau !
      </div>

      <div v-else-if="total === 0 && filter === 'new'" class="flex-1 rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/50">
        Aucun·e nouveau·elle coopérateur·ice à relancer.
      </div>

      <div v-else-if="total === 0 && filter === 'withSlot'" class="flex-1 rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/50">
        Aucun·e coopérateur·ice n'a encore choisi de créneau.
      </div>

      <div v-else-if="total === 0 && filter === 'exempted'" class="flex-1 rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/50">
        Aucun·e coopérateur·ice exempté·e.
      </div>

      <div v-else-if="total === 0" class="flex-1 rounded-lg border border-dashed border-gray-200 py-8 text-center text-brown/50">
        Aucun·e coopérateur·ice.
      </div>

      <template v-else>
        <div class="overflow-x-auto rounded-lg border border-gray-200">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-gray-200 bg-gray-50">
                <th class="px-4 py-3 text-left font-medium text-brown/70">
                  <button type="button" class="inline-flex items-center gap-1 hover:text-dark" @click="toggleSort('name')">
                    Nom<span class="w-3 text-xs">{{ sortIndicator('name') }}</span>
                  </button>
                </th>
                <th class="px-4 py-3 text-left font-medium text-brown/70">
                  <button type="button" class="inline-flex items-center gap-1 hover:text-dark" @click="toggleSort('email')">
                    Email<span class="w-3 text-xs">{{ sortIndicator('email') }}</span>
                  </button>
                </th>
                <th class="px-4 py-3 text-left font-medium text-brown/70">{{ filter === 'exempted' ? 'Raison d’exemption' : 'Créneau choisi' }}</th>
                <th class="px-4 py-3 text-left font-medium text-brown/70">
                  <button type="button" class="inline-flex items-center gap-1 hover:text-dark" @click="toggleSort('lastReminder')">
                    Dernière relance<span class="w-3 text-xs">{{ sortIndicator('lastReminder') }}</span>
                  </button>
                </th>
                <th class="px-4 py-3 text-right font-medium text-brown/70">Action</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="coop in items" :key="coop.id" class="bg-white hover:bg-gray-50">
                <td class="px-4 py-3 font-medium text-dark">{{ coop.firstName }} {{ coop.lastName }}</td>
                <td class="px-4 py-3 text-brown/70">{{ coop.email }}</td>
                <td class="px-4 py-3 text-brown/70">
                  <span v-if="coop.exemptionReason">{{ coop.exemptionReason }}</span>
                  <span v-else-if="coop.slot">{{ formatSlot(coop.slot) }}</span>
                  <span v-else class="text-brown/40">—</span>
                </td>
                <td class="px-4 py-3 text-brown/70">
                  <span v-if="coop.lastReminderAt">{{ formatDate(coop.lastReminderAt) }}</span>
                  <span v-else class="text-brown/40">Jamais</span>
                </td>
                <td class="px-4 py-3 text-right">
                  <button
                    type="button"
                    class="rounded px-2 py-1 text-xs font-medium text-dark transition hover:bg-gray-100 disabled:opacity-50"
                    :disabled="isSending"
                    @click="remindOne(coop.id)"
                  >
                    Relancer
                  </button>
                </td>
              </tr>
              <tr v-for="i in emptyRows" :key="`empty-${i}`" class="bg-white" aria-hidden="true">
                <td class="px-4 py-3">&nbsp;</td>
                <td class="px-4 py-3"></td>
                <td class="px-4 py-3"></td>
                <td class="px-4 py-3"></td>
                <td class="px-4 py-3"></td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="mt-3 flex items-center justify-between text-sm text-brown/70" :class="{ invisible: pageCount <= 1 }">
          <span>
            {{ rangeStart }}–{{ rangeEnd }} sur {{ total }}
          </span>
          <div class="flex items-center gap-2">
            <button
              type="button"
              class="rounded border border-gray-300 px-2 py-1 text-xs font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
              :disabled="page === 1"
              @click="page--"
            >
              Précédent
            </button>
            <span>Page {{ page }} / {{ pageCount }}</span>
            <button
              type="button"
              class="rounded border border-gray-300 px-2 py-1 text-xs font-medium text-dark transition hover:bg-gray-50 disabled:opacity-50"
              :disabled="page === pageCount"
              @click="page++"
            >
              Suivant
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { usePendingCooperators, useSendReminders, type CooperatorSlotResponse, type CooperatorSortField, type SortDirection } from '@/composables/useAdmin'
import { dayLabel, formatTime } from '@/composables/useSlots'

const page = ref(1)
const size = ref(10)
const search = ref('')
type Filter = 'pending' | 'new' | 'withSlot' | 'exempted' | 'all'
const filter = ref<Filter>('pending')
const withoutSlotOnly = computed(() => filter.value === 'pending' || filter.value === 'new')
const withSlotOnly = computed(() => filter.value === 'withSlot')
const neverRemindedOnly = computed(() => filter.value === 'new')
const exemptedOnly = computed(() => filter.value === 'exempted')
const sortBy = ref<CooperatorSortField>('name')
const sortDir = ref<SortDirection>('asc')

function toggleSort(field: CooperatorSortField) {
  if (sortBy.value === field) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = field
    sortDir.value = 'asc'
  }
  page.value = 1
}

function sortIndicator(field: CooperatorSortField) {
  if (sortBy.value !== field) return ''
  return sortDir.value === 'asc' ? '▲' : '▼'
}
const titleByFilter: Record<Filter, string> = {
  pending: 'Coopérateur·ices sans créneau',
  new: 'Nouveaux coopérateur·ices',
  withSlot: 'Coopérateur·ices avec créneau',
  exempted: 'Coopérateur·ices exempté·es',
  all: 'Coopérateur·ices',
}

watch([search, filter], () => { page.value = 1 })

const { data: pageData, isPending } = usePendingCooperators(
  page, size, search, withoutSlotOnly, neverRemindedOnly, sortBy, sortDir, withSlotOnly, exemptedOnly,
)

const items = computed(() => pageData.value?.items ?? [])
const total = computed(() => pageData.value?.total ?? 0)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
const rangeStart = computed(() => (total.value === 0 ? 0 : (page.value - 1) * size.value + 1))
const rangeEnd = computed(() => Math.min(page.value * size.value, total.value))
const emptyRows = computed(() => Math.max(0, size.value - items.value.length))

// Keep page in range when total shrinks (e.g. after an action).
watch(pageCount, (count) => {
  if (page.value > count) page.value = count
})

const { mutateAsync, isPending: isSending } = useSendReminders()
const successMessage = ref<string | null>(null)

const dateFormatter = new Intl.DateTimeFormat('fr-FR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

function formatDate(iso: string) {
  return dateFormatter.format(new Date(iso))
}

function formatSlot(slot: CooperatorSlotResponse): string {
  return `Semaine ${slot.week} - ${dayLabel(slot.dayOfWeek)} ${formatTime(slot.startTime)}-${formatTime(slot.endTime)}`
}

async function remindAll() {
  const result = await mutateAsync({ all: true })
  successMessage.value = result.scheduled
    ? 'Envoi des rappels lancé en arrière-plan.'
    : `${result.sentCount} rappel${result.sentCount > 1 ? 's' : ''} envoyé${result.sentCount > 1 ? 's' : ''}.`
  setTimeout(() => { successMessage.value = null }, 5000)
}

async function inviteNew() {
  const result = await mutateAsync({ all: true, onlyNeverReminded: true })
  successMessage.value = result.scheduled
    ? 'Envoi aux nouveaux coopérateur·ices lancé en arrière-plan.'
    : `${result.sentCount} invitation${result.sentCount > 1 ? 's' : ''} envoyée${result.sentCount > 1 ? 's' : ''}.`
  setTimeout(() => { successMessage.value = null }, 5000)
}

async function remindOne(id: string) {
  const result = await mutateAsync({ cooperatorIds: [id] })
  successMessage.value = `${result.sentCount} rappel envoyé.`
  setTimeout(() => { successMessage.value = null }, 5000)
}
</script>
