<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly:   { type: Boolean, default: false },
  size:       { type: String, default: 'md' },
})
const emit = defineEmits(['update:modelValue'])

const starSize = computed(() => ({ sm: 'text-sm', md: 'text-xl', lg: 'text-2xl' }[props.size] || 'text-xl'))

function select(n) {
  if (!props.readonly) emit('update:modelValue', n)
}
</script>

<template>
  <div class="flex items-center gap-0.5">
    <button
      v-for="n in 5"
      :key="n"
      type="button"
      :disabled="readonly"
      :class="[starSize, readonly ? 'cursor-default' : 'cursor-pointer hover:scale-110 transition-transform']"
      @click="select(n)"
    >
      <span :class="n <= modelValue ? 'text-amber-400' : 'text-gray-300'">★</span>
    </button>
    <span v-if="!readonly && modelValue" class="ml-1 text-sm text-gray-500">{{ modelValue }}/5</span>
  </div>
</template>
