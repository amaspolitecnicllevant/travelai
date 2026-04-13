<script setup>
import { ref, computed, nextTick } from 'vue'
import { useAiStream } from '@/composables/useAiStream'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps({
  tripId:          { type: [String, Number], required: true },
  dayNumber:       { type: Number, default: null },
  useEditorAgent:  { type: Boolean, default: false },
  disabled:        { type: Boolean, default: false },
  placeholder:     { type: String, default: null },
})
const emit = defineEmits(['submit', 'days-updated'])

const { streaming, progress, rawBuffer, days, error, refineDay, refineAll, editItinerary, cancel } = useAiStream()

const prompt     = ref('')
const history    = ref([])  // [{ role: 'user'|'ai', content: string, daysCount?: number }]
const historyEl  = ref(null)

const isDisabled = computed(() => props.disabled || streaming.value)

const effectivePlaceholder = computed(() => {
  if (props.placeholder)    return props.placeholder
  if (props.dayNumber)      return `Descriu com vols modificar el dia ${props.dayNumber}...`
  if (props.useEditorAgent) return "Exemple: «Fes el dia 2 més relaxat», «Afegeix més gastronomia local»..."
  return "Descriu com vols modificar l'itinerari..."
})

async function send() {
  const text = prompt.value.trim()
  if (!text || isDisabled.value) return

  history.value.push({ role: 'user', content: text })
  prompt.value = ''
  await nextTick()
  scrollHistory()

  let result
  if (props.dayNumber) {
    result = await refineDay(props.tripId, props.dayNumber, text)
  } else if (props.useEditorAgent) {
    result = await editItinerary(props.tripId, text)
  } else {
    result = await refineAll(props.tripId, text)
  }

  const daysCount = result?.length || 0
  if (error.value) {
    history.value.push({ role: 'ai', content: error.value, isError: true })
  } else {
    history.value.push({
      role: 'ai',
      content: daysCount > 0
        ? `Itinerari actualitzat (${daysCount} ${daysCount === 1 ? 'dia' : 'dies'} modificats)`
        : 'Modificació aplicada',
      daysCount,
    })
  }
  await nextTick()
  scrollHistory()

  emit('days-updated', result || [])
}

function scrollHistory() {
  if (historyEl.value) historyEl.value.scrollTop = historyEl.value.scrollHeight
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

    <!-- Historial de conversa -->
    <div
      v-if="history.length > 0 || streaming"
      ref="historyEl"
      class="max-h-56 overflow-y-auto px-4 py-3 space-y-2 border-b border-gray-100"
    >
      <div
        v-for="(msg, i) in history"
        :key="i"
        class="flex gap-2"
        :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
      >
        <div
          class="max-w-[85%] px-3 py-2 rounded-xl text-sm leading-snug"
          :class="msg.role === 'user'
            ? 'bg-indigo-600 text-white rounded-br-none'
            : msg.isError
              ? 'bg-red-50 text-red-700 border border-red-200 rounded-bl-none'
              : 'bg-gray-100 text-gray-800 rounded-bl-none'"
        >
          <span v-if="msg.daysCount" class="flex items-center gap-1">
            <svg class="h-3.5 w-3.5 text-emerald-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
            </svg>
            {{ msg.content }}
          </span>
          <span v-else>{{ msg.content }}</span>
        </div>
      </div>

      <!-- Streaming indicator -->
      <div v-if="streaming" class="flex justify-start">
        <div class="bg-gray-100 rounded-xl rounded-bl-none px-3 py-2 flex items-center gap-2">
          <span class="flex gap-0.5">
            <span class="h-1.5 w-1.5 bg-gray-400 rounded-full animate-bounce" style="animation-delay:0ms"></span>
            <span class="h-1.5 w-1.5 bg-gray-400 rounded-full animate-bounce" style="animation-delay:150ms"></span>
            <span class="h-1.5 w-1.5 bg-gray-400 rounded-full animate-bounce" style="animation-delay:300ms"></span>
          </span>
          <span class="text-xs text-gray-400">{{ progress }}</span>
        </div>
      </div>
    </div>

    <!-- Streaming raw buffer (visible durant la generació) -->
    <div v-if="streaming && rawBuffer" class="px-4 py-3 border-b border-gray-100">
      <div class="bg-gray-50 rounded-lg p-3 text-xs text-gray-600 font-mono whitespace-pre-wrap
                  max-h-32 overflow-y-auto leading-relaxed">
        {{ rawBuffer }}<span class="animate-pulse text-indigo-500">▌</span>
      </div>
      <div class="flex items-center gap-2 mt-2">
        <div class="flex-1 bg-gray-100 rounded-full h-1 overflow-hidden">
          <div class="bg-indigo-500 h-1 rounded-full animate-pulse w-2/3"></div>
        </div>
        <button class="text-xs text-red-500 hover:text-red-700" @click="cancel">Cancel·lar</button>
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
        <button
          :disabled="isDisabled || !prompt.trim()"
          class="self-start bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium
                 hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed
                 transition-colors whitespace-nowrap"
          @click="send"
        >
          Enviar
        </button>
      </div>
      <p class="text-xs text-gray-400 mt-2">
        <kbd class="bg-gray-100 px-1 rounded text-xs">Enter</kbd> per enviar ·
        <kbd class="bg-gray-100 px-1 rounded text-xs">Shift+Enter</kbd> per nova línia
      </p>
    </div>
  </div>
</template>
