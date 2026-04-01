<script setup>
import { ref } from 'vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps({
  disabled:    { type: Boolean, default: false },
  placeholder: { type: String, default: 'Escriu com vols modificar l\'itinerari...' },
})
const emit = defineEmits(['submit'])

const prompt = ref('')

function send() {
  const text = prompt.value.trim()
  if (!text || props.disabled) return
  emit('submit', text)
  prompt.value = ''
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() }
}
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl shadow-sm p-4">
    <div class="flex items-center gap-2 mb-3">
      <span class="text-lg">🤖</span>
      <h4 class="font-semibold text-sm text-gray-700">Edita amb IA</h4>
      <LoadingSpinner v-if="disabled" size="sm" class="ml-auto" />
    </div>

    <div class="flex gap-2">
      <textarea
        v-model="prompt"
        :disabled="disabled"
        :placeholder="placeholder"
        rows="2"
        class="flex-1 text-sm border border-gray-200 rounded-lg px-3 py-2 resize-none
               focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent
               disabled:bg-gray-50 disabled:text-gray-400"
        @keydown="onKeydown"
      />
      <button
        :disabled="disabled || !prompt.trim()"
        class="self-end bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium
               hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        @click="send"
      >
        Enviar
      </button>
    </div>
    <p class="text-xs text-gray-400 mt-2">Prem Enter per enviar · Shift+Enter per nova línia</p>
  </div>
</template>
