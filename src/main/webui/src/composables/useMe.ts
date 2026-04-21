import { useQuery } from '@tanstack/vue-query'
import { customFetch } from '@/api/mutator/custom-fetch'

export interface MeResponse {
  barcodeBase: string
  email: string
  firstName: string
  lastName: string
  roles: string[]
}

export const ADMIN_ROLES = ['Member Manager', 'Foodcoop Admin'] as const
export const SYNC_ROLES = ['Foodcoop Admin'] as const

export function hasAnyRole(userRoles: string[] | undefined, required: readonly string[]): boolean {
  if (!userRoles) return false
  return required.some((r) => userRoles.includes(r))
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
