import { useQuery, useMutation, useQueryClient } from '@tanstack/vue-query'
import { customFetch } from '@/api/mutator/custom-fetch'
import type { SlotResponse } from './useSlots'

export interface DashboardResponse {
  totalCooperators: number
  registeredCooperators: number
  pendingCooperators: number
  slotsUnderMinimum: number
  allMinimumsReached: boolean
  slotsNeedingPeople: SlotResponse[]
}

export interface CooperatorResponse {
  id: string
  email: string
  firstName: string
  lastName: string
}

export function useDashboard() {
  return useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: () =>
      customFetch<{ data: DashboardResponse }>('/api/admin/dashboard', { method: 'GET' }).then((r) => r.data),
    refetchInterval: 15_000,
  })
}

export function usePendingCooperators() {
  return useQuery({
    queryKey: ['admin', 'cooperators'],
    queryFn: () =>
      customFetch<{ data: CooperatorResponse[] }>('/api/admin/cooperators', { method: 'GET' }).then((r) => r.data),
  })
}

export function useSendReminders() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: { cooperatorIds?: string[]; all?: boolean }) =>
      customFetch<{ data: { sentCount: number } }>('/api/admin/reminders', {
        method: 'POST',
        body: JSON.stringify(payload),
        headers: { 'Content-Type': 'application/json' },
      }).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
  })
}
