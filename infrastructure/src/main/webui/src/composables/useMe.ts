import { useQuery } from '@tanstack/vue-query'
import { customFetch } from '@/api/mutator/custom-fetch'

export interface MeResponse {
  cooperatorId: string
  email: string
  firstName: string
  lastName: string
  registeredSlotId: string | null
}

export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: () =>
      customFetch<{ data: MeResponse }>('/api/me', { method: 'GET' }).then((r) => r.data),
  })
}
