<script setup>
import { ref, computed } from 'vue'
import { useAiStream } from '@/composables/useAiStream'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps({
  tripId:      { type: [String, Number], required: true },
  dayNumber:   { type: Number, default: null },
  disabled:    { type: Boolean, default: false },
  placeholder: { type: String, default: null },
})
const emit = defineEmits(['submit', 'days-updated'])

const { streaming, progress, rawBuffer, days, error, generate, refineDay, refineAll, cancel } = useAiStream()

const prompt = ref('')

const isDisabled = computed(() => props.disabled || streaming.value)

const effectivePlaceholder = computed(() => {
  if (props.placeholder) return props.placeholder
  if (props.dayNumber)   return `Descriu com vols modificar el dia ${props.dayNumber}...`
  return "Descriu com vols modificar l'itinerari..."
})

const modeLabel = computed(() => {
  if (props.dayNumber) return `Refinant dia ${props.dayNumber}`
  return 'Refinant itinerari complet'
})

async function send() {
  const text = prompt.value.trim()
  if (!text || isDisabled.value) return

  // Emetre a la vista parent per si vol gestionar-ho ella
  emit('submit', text)

  let result
  if (props.dayNumber) {
    result = await refineDay(props.tripId, props.dayNumber, text)
  } else {
    result = await refineAll(props.tripId, text)
  }

  if (result && result.length > 0) {
    emit('days-updated', result)
  }

  prompt.value = ''
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() }
}
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">
    <!-- Capçalera -->
    <div class="flex items-center gap-2 px-4 py-3 bg-gray-50 border-b border-gray-100">
      <span class="text-lg">🤖</span>
      <h4 class="font-semibold text-sm text-gray-700 flex-1">Edita amb IA</h4>
      <span v-if="dayNumber" class="text-xs bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded-full font-medium">
        Dia {{ dayNumber }}
      </span>
      <LoadingSpinner v-if="streaming" size="sm" class="ml-1" />
    </div>

    <!-- Àrea de resposta streaming -->
    <div v-if="streaming || rawBuffer || error" class="px-4 py-3 border-b border-gray-100">
      <!-- Progrés -->
      <div v-if="streaming" class="flex items-center gap-2 mb-2">
        <div class="flex-1 bg-gray-100 rounded-full h-1 overflow-hidden">
          <div class="bg-indigo-500 h-1 rounded-full animate-pulse w-2/3"></div>
        </div>
        <span class="text-xs text-gray-400 whitespace-nowrap">{{ progress }}</span>
      </div>

      <!-- Text en temps real -->
      <div v-if="rawBuffer"
           class="bg-gray-50 rounded-lg p-3 text-sm text-gray-700 font-mono whitespace-pre-wrap
                  max-h-48 overflow-y-auto leading-relaxed">
        {{ rawBuffer }}<span v-if="streaming" class="animate-pulse text-indigo-500">▌</span>
      </div>

      <!-- Dies completats -->
      <div v-if="days.length > 0 && !streaming"
           class="mt-2 flex items-center gap-2 text-sm text-emerald-600">
        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
        </svg>
        <span>{{ days.length }} {{ days.length === 1 ? 'dia' : 'dies' }} actualitzats</span>
      </div>

      <!-- Error -->
      <div v-if="error" class="mt-2 text-sm text-red-600 bg-red-50 rounded-lg px-3 py-2">
        {{ error }}
      </div>
    </div>

    <!-- Input area -->
    <div class="p-4">
      <div class="flex gap-2">
        <textarea
          v-model="prompt"
          :disabled="isDisabled"
          :placeholder="effectivePlaceholder"
          rows="2"
          class="flex-1 text-sm border border-gray-200 rounded-lg px-3 py-2 resize-none
                 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent
                 disabled:bg-gray-50 disabled:text-gray-400 transition-colors"
          @keydown="onKeydown"
        />
        <div class="flex flex-col gap-2">
          <button
            :disabled="isDisabled || !prompt.trim()"
            class="self-start bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium
                   hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed
                   transition-colors whitespace-nowrap"
            @click="send"
          >
            Enviar
          </button>
          <button
            v-if="streaming"
            class="text-xs text-red-500 hover:text-red-700 text-center transition-colors"
            @click="cancel"
          >
            Cancel·lar
          </button>
        </div>
      </div>
      <p class="text-xs text-gray-400 mt-2">
        Prem <kbd class="bg-gray-100 px-1 rounded text-xs">Enter</kbd> per enviar ·
        <kbd class="bg-gray-100 px-1 rounded text-xs">Shift+Enter</kbd> per nova línia
      </p>
    </div>
  </div>
</template>
