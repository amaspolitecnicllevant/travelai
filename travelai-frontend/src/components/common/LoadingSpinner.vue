<script setup>
import { computed } from 'vue'

const props = defineProps({
  size:  { type: String, default: 'md' },
  color: { type: String, default: 'indigo' },
})

const sizeClass = computed(() =>
  ({ sm: 'h-4 w-4', md: 'h-8 w-8', lg: 'h-12 w-12', xl: 'h-16 w-16' }[props.size] || 'h-8 w-8')
)
const colorClass = computed(() =>
  ({ indigo: 'text-indigo-600', white: 'text-white', gray: 'text-gray-400' }[props.color] || 'text-indigo-600')
)
</script>

<template>
  <div class="flex items-center justify-center">
    <svg
      class="animate-spin"
      :class="[sizeClass, colorClass]"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
      <path class="opacity-75" fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  </div>
</template>
