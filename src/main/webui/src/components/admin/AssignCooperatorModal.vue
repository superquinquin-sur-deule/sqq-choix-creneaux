<template>
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
    role="dialog"
    aria-modal="true"
    @click.self="$emit('close')"
  >
    <div class="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
      <div class="mb-4 flex items-start justify-between gap-4">
        <div>
          <h3 class="text-lg font-semibold text-dark">Ajouter un·e coopérateur·ice</h3>
          <p class="mt-1 text-sm text-brown/70">
            {{ slotLabel }}
          </p>
        </div>
        <button
          type="button"
          class="rounded p-1 text-brown/60 transition hover:bg-gray-100 hover:text-dark"
          aria-label="Fermer"
          @click="$emit('close')"
        >
          ✕
        </button>
      </div>

      <input
        v-model="searchInput"
        type="text"
        placeholder="Rechercher par nom ou email…"
        class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/30"
        autofocus
      />

      <div v-if="errorMessage" class="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-alert">
        {{ errorMessage }}
      </div>

      <div class="mt-3 max-h-80 overflow-y-auto rounded-lg border border-gray-200">
        <div v-if="isPending && !pageData" class="py-6 text-center text-sm text-brown/60">
          Chargement…
        </div>
        <div v-else-if="items.length === 0" class="py-6 text-center text-sm text-brown/50">
          Aucun résultat.
        </div>
        <ul v-else class="divide-y divide-gray-100">
          <li
            v-for="coop in items"
            :key="coop.id"
            class="flex items-center justify-between gap-3 px-3 py-2"
          >
            <div class="min-w-0">
              <p class="truncate text-sm font-medium text-dark">
                {{ coop.firstName }} {{ coop.lastName }}
              </p>
              <p class="truncate text-xs text-brown/60">{{ coop.email }}</p>
            </div>
            <button
              type="button"
              class="shrink-0 rounded bg-dark px-3 py-1 text-xs font-medium text-white transition hover:bg-brown disabled:opacity-50"
              :disabled="isAssigning"
              @click="assign(coop.id)"
            >
              Affecter
            </button>
          </li>
        </ul>
      </div>

      <div v-if="pageCount > 1" class="mt-3 flex items-center justify-between text-xs text-brown/70">
        <span>{{ rangeStart }}–{{ rangeEnd }} sur {{ total }}</span>
        <div class="flex items-center gap-2">
          <button
            type="button"
            class="rounded border border-gray-300 px-2 py-1 transition hover:bg-gray-50 disabled:opacity-50"
            :disabled="page === 1"
            @click="page--"
          >
            Précédent
          </button>
          <span>Page {{ page }} / {{ pageCount }}</span>
          <button
            type="button"
            class="rounded border border-gray-300 px-2 py-1 transition hover:bg-gray-50 disabled:opacity-50"
            :disabled="page === pageCount"
            @click="page++"
          >
            Suivant
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useSearchCooperators, useAssignSlot, type AdminSlotResponse } from '@/composables/useAdmin'
import { formatTime } from '@/composables/useSlots'
import { ApiError } from '@/api/mutator/custom-fetch'

const props = defineProps<{ slot: AdminSlotResponse }>()
const emit = defineEmits<{ close: []; assigned: [{ moved: boolean }] }>()

const searchInput = ref('')
const search = ref('')
const page = ref(1)
const size = ref(8)
const errorMessage = ref<string | null>(null)

let debounceTimer: ReturnType<typeof setTimeout> | null = null
watch(searchInput, (val) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    search.value = val
    page.value = 1
  }, 300)
})

const { data: pageData, isPending } = useSearchCooperators(search, page, size)
const items = computed(() => pageData.value?.items ?? [])
const total = computed(() => pageData.value?.total ?? 0)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / size.value)))
const rangeStart = computed(() => (total.value === 0 ? 0 : (page.value - 1) * size.value + 1))
const rangeEnd = computed(() => Math.min(page.value * size.value, total.value))

const { mutateAsync, isPending: isAssigning } = useAssignSlot()

const slotLabel = computed(() => {
  const s = props.slot
  return `Semaine ${s.week} · ${s.dayOfWeek} · ${formatTime(s.startTime)}–${formatTime(s.endTime)}`
})

async function assign(cooperatorId: string) {
  errorMessage.value = null
  try {
    const res = await mutateAsync({ slotId: props.slot.id, cooperatorId })
    emit('assigned', { moved: res.moved })
    emit('close')
  } catch (e) {
    if (e instanceof ApiError) {
      errorMessage.value = e.status === 409 ? 'Créneau complet.' : e.problem.detail || e.problem.title
    } else {
      errorMessage.value = 'Erreur inattendue.'
    }
  }
}
</script>
