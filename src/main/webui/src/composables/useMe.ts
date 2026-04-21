import { useQuery } from '@tanstack/vue-query'
import { customFetch } from '@/api/mutator/custom-fetch'

export interface MeResponse {
  barcodeBase: string
  email: string
  firstName: string
  lastName: string
}

export interface MyRegistrationResponse {
  registeredSlotId: string | null
}

export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: () =>
      customFetch<{ data: MeResponse }>('/api/me', { method: 'GET' }).then((r) => r.data),
  })
}

export function useMyRegistration() {
  return useQuery({
    queryKey: ['me', 'registration'],
    queryFn: () =>
      customFetch<{ data: MyRegistrationResponse }>('/api/me/registration', { method: 'GET' }).then(
        (r) => r.data,
      ),
  })
}
