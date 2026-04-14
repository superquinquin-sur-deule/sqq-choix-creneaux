<template>
  <div class="h-2 w-full overflow-hidden rounded-full bg-gray-200">
    <div
      class="h-full rounded-full transition-all duration-300"
      :class="barClass"
      :style="{ width: `${Math.min(100, fillPercent)}%` }"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  current: number
  min: number
  max: number
}>()

const fillPercent = computed(() => (props.max > 0 ? (props.current / props.max) * 100 : 0))
const ratioToMin = computed(() => (props.min > 0 ? props.current / props.min : 1))

const barClass = computed(() => {
  if (ratioToMin.value >= 1) return 'bg-success'
  if (ratioToMin.value >= 0.5) return 'bg-amber-500'
  return 'bg-primary'
})
</script>
