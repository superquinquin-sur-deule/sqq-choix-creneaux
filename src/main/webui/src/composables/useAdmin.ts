import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import type { Ref } from 'vue'
import { customFetch } from '@/api/mutator/custom-fetch'
import type { SlotResponse } from './useSlots'

export interface DashboardResponse {
  totalCooperators: number
  registeredCooperators: number
  pendingCooperators: number
  slotsUnderMinimum: number
  allMinimumsReached: boolean
  seatsToReachMinimum: number
  slotsNeedingPeople: SlotResponse[]
}

export interface CooperatorSlotResponse {
  week: string
  dayOfWeek: string
  startTime: string
  endTime: string
}

export interface CooperatorResponse {
  id: string
  email: string
  firstName: string
  lastName: string
  lastReminderAt: string | null
  slot: CooperatorSlotResponse | null
  exemptionReason: string | null
}

export interface RegistrantResponse {
  firstName: string
  lastName: string
  lastNameInitial: string
}

export interface AdminSlotResponse {
  id: string
  week: string
  dayOfWeek: string
  startTime: string
  endTime: string
  minCapacity: number
  maxCapacity: number
  registrationCount: number
  status: 'NEEDS_PEOPLE' | 'LOCKED' | 'OPEN' | 'FULL'
  registrants: RegistrantResponse[]
}

export function useDashboard() {
  return useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: () =>
      customFetch<{ data: DashboardResponse }>('/api/admin/dashboard', { method: 'GET' }).then((r) => r.data),
    refetchInterval: 15_000,
  })
}

export function useAdminSlots() {
  return useQuery({
    queryKey: ['admin', 'slots'],
    queryFn: () =>
      customFetch<{ data: AdminSlotResponse[] }>('/api/admin/slots', { method: 'GET' }).then((r) => r.data),
    refetchInterval: 15_000,
  })
}

export interface CooperatorsPage {
  items: CooperatorResponse[]
  total: number
  page: number
  size: number
}

export type CooperatorSortField = 'name' | 'email' | 'lastReminder'
export type SortDirection = 'asc' | 'desc'

export function usePendingCooperators(
  page: Ref<number>,
  size: Ref<number>,
  q?: Ref<string>,
  withoutSlotOnly?: Ref<boolean>,
  neverRemindedOnly?: Ref<boolean>,
  sortBy?: Ref<CooperatorSortField>,
  sortDir?: Ref<SortDirection>,
  withSlotOnly?: Ref<boolean>,
  exemptedOnly?: Ref<boolean>,
) {
  return useQuery({
    queryKey: [
      'admin', 'cooperators', page, size,
      q ?? '', withoutSlotOnly ?? true, neverRemindedOnly ?? false,
      sortBy ?? 'name', sortDir ?? 'asc', withSlotOnly ?? false, exemptedOnly ?? false,
    ],
    queryFn: () => {
      const search = q?.value?.trim() ?? ''
      const onlyPending = withoutSlotOnly?.value ?? true
      const onlyWithSlot = withSlotOnly?.value ?? false
      const onlyNew = neverRemindedOnly?.value ?? false
      const onlyExempted = exemptedOnly?.value ?? false
      const by = sortBy?.value ?? 'name'
      const dir = sortDir?.value ?? 'asc'
      const qs =
        `page=${page.value}&size=${size.value}` +
        (search ? `&q=${encodeURIComponent(search)}` : '') +
        `&withoutSlotOnly=${onlyPending}` +
        `&withSlotOnly=${onlyWithSlot}` +
        `&neverRemindedOnly=${onlyNew}` +
        `&exemptedOnly=${onlyExempted}` +
        `&sortBy=${by}&sortDir=${dir}`
      return customFetch<{ data: CooperatorsPage }>(
        `/api/admin/cooperators?${qs}`,
        { method: 'GET' },
      ).then((r) => r.data)
    },
    placeholderData: (previous) => previous,
  })
}

export function useSearchCooperators(q: Ref<string>, page: Ref<number>, size: Ref<number>) {
  return useQuery({
    queryKey: ['admin', 'cooperators', 'search', q, page, size],
    queryFn: () =>
      customFetch<{ data: CooperatorsPage }>(
        `/api/admin/cooperators/search?q=${encodeURIComponent(q.value)}&page=${page.value}&size=${size.value}`,
        { method: 'GET' },
      ).then((r) => r.data),
    placeholderData: (previous) => previous,
  })
}

export function useAssignSlot() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ slotId, cooperatorId }: { slotId: string; cooperatorId: string }) =>
      customFetch<{ data: { moved: boolean } }>(`/api/admin/slots/${slotId}/assign`, {
        method: 'POST',
        body: JSON.stringify({ cooperatorId }),
        headers: { 'Content-Type': 'application/json' },
      }).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
}

export function useSyncCooperators() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () =>
      customFetch<{ data: { cooperatorsImported: number } }>('/api/admin/sync/cooperators', {
        method: 'POST',
      }).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
}

export function useSyncSlots() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () =>
      customFetch<{ data: { slotsImported: number; cooperatorsImported: number } }>(
        '/api/admin/sync/pull',
        { method: 'POST' },
      ).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
}

export function usePushOneRegistration() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (cooperatorId: string) =>
      customFetch<{ data: { pushed: boolean; reason: string | null; outcome: string | null } }>(
        '/api/admin/sync/push-one',
        {
          method: 'POST',
          body: JSON.stringify({ cooperatorId }),
          headers: { 'Content-Type': 'application/json' },
        },
      ).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
}

export function useSendReminders() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { cooperatorIds?: string[]; all?: boolean; onlyNeverReminded?: boolean }) =>
      customFetch<{ data: { sentCount: number; scheduled: boolean } }>('/api/admin/reminders', {
        method: 'POST',
        body: JSON.stringify(payload),
        headers: { 'Content-Type': 'application/json' },
      }).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
}
