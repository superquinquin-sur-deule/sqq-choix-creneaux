import { useQuery } from '@tanstack/vue-query'
import { customFetch } from '@/api/mutator/custom-fetch'

export interface AppInfoResponse {
  production: boolean
}

export function useAppInfo() {
  return useQuery({
    queryKey: ['app-info'],
    queryFn: () =>
      customFetch<{ data: AppInfoResponse }>('/api/app-info', { method: 'GET' }).then((r) => r.data),
    staleTime: Infinity,
  })
}
