import { useQuery } from '@tanstack/vue-query'
import { customFetch } from '@/api/mutator/custom-fetch'

export interface SlotResponse {
  id: string
  week: string
  dayOfWeek: string
  startTime: string
  endTime: string
  minCapacity: number
  maxCapacity: number
  registrationCount: number
  status: 'NEEDS_PEOPLE' | 'LOCKED' | 'OPEN' | 'FULL'
}

export interface SlotsPageResponse {
  slots: SlotResponse[]
  campaign: { storeOpening: string; weekAReference: string } | null
}

export function useSlots() {
  return useQuery({
    queryKey: ['slots'],
    queryFn: () =>
      customFetch<{ data: SlotsPageResponse }>('/api/slots', { method: 'GET' }).then((r) => r.data),
    refetchInterval: 10_000,
  })
}

const DAY_ORDER: Record<string, number> = {
  MONDAY: 0, TUESDAY: 1, WEDNESDAY: 2, THURSDAY: 3, FRIDAY: 4, SATURDAY: 5,
}
const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Lundi', TUESDAY: 'Mardi', WEDNESDAY: 'Mercredi',
  THURSDAY: 'Jeudi', FRIDAY: 'Vendredi', SATURDAY: 'Samedi',
}

export function dayLabel(day: string): string { return DAY_LABELS[day] ?? day }
export function sortedDays(): string[] {
  return Object.keys(DAY_ORDER).sort((a, b) => DAY_ORDER[a] - DAY_ORDER[b])
}
export function slotsForWeekAndDay(slots: SlotResponse[], week: string, day: string): SlotResponse[] {
  return slots.filter((s) => s.week === week && s.dayOfWeek === day).sort((a, b) => a.startTime.localeCompare(b.startTime))
}
export function formatTime(time: string): string {
  const [h, m] = time.split(':')
  return `${h}h${m === '00' ? '' : m}`
}

export function computeWeekLetter(date: Date, weekARef: Date): string {
  const msPerWeek = 7 * 24 * 60 * 60 * 1000
  const diffWeeks = Math.floor((date.getTime() - weekARef.getTime()) / msPerWeek)
  const index = ((diffWeeks % 4) + 4) % 4
  return ['A', 'B', 'C', 'D'][index]
}

export function firstMondayAfterOpening(targetWeek: string, storeOpening: string, weekAReference: string): string {
  const opening = new Date(storeOpening)
  const ref = new Date(weekAReference)
  const openingWeek = computeWeekLetter(opening, ref)
  const weeks = ['A', 'B', 'C', 'D']
  const offset = ((weeks.indexOf(targetWeek) - weeks.indexOf(openingWeek)) % 4 + 4) % 4
  const monday = new Date(opening)
  monday.setDate(monday.getDate() + offset * 7)
  const saturday = new Date(monday)
  saturday.setDate(saturday.getDate() + 5)
  const fmt = (d: Date) => `${d.getDate()} ${d.toLocaleDateString('fr-FR', { month: 'short' })}`
  return `${fmt(monday)} - ${fmt(saturday)}`
}
