<script setup>
import { ref } from 'vue'
import api from '@/api'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps({
  tripId:    { type: [String, Number], required: true },
  dayNumber: { type: Number,           required: true },
})

const emit = defineEmits(['add-activity'])

// ── Category selector ─────────────────────────────────────────────────────────
const CATEGORIES = [
  { value: 'CULTURE',   label: 'Cultura',    icon: '🏛' },
  { value: 'FOOD',      label: 'Gastronomia',icon: '🍽' },
  { value: 'LEISURE',   label: 'Lleure',     icon: '🎭' },
  { value: 'NATURE',    label: 'Natura',     icon: '🌿' },
  { value: 'SPORT',     label: 'Esport',     icon: '⚽' },
  { value: 'NIGHTLIFE', label: 'Nit',        icon: '🌙' },
]

const selectedCategory = ref('CULTURE')

// ── Suggestions state ─────────────────────────────────────────────────────────
const suggestions = ref([])
const loading     = ref(false)
const error       = ref(null)

async function suggest() {
  loading.value     = true
  error.value       = null
  suggestions.value = []
  try {
    const { data } = await api.post(
      `/ai/trips/${props.tripId}/days/${props.dayNumber}/activities/suggest`,
      { category: selectedCategory.value }
    )
    suggestions.value = Array.isArray(data) ? data : (data.suggestions ?? [])
  } catch (e) {
    error.value = e.response?.data?.message || 'Error generant suggeriments. Torna-ho a intentar.'
  } finally {
    loading.value = false
  }
}

function addActivity(activity) {
  emit('add-activity', { ...activity, dayNumber: props.dayNumber })
}
</script>

<template>
  <div class="bg-white border border-gray-200 rounded-xl shadow-sm overflow-hidden">

    <!-- Header -->
    <div class="flex items-center gap-2 px-4 py-3 bg-gray-50 border-b border-gray-100">
      <span class="text-lg">✨</span>
      <h4 class="font-semibold text-sm text-gray-700 flex-1">Suggeriments d'activitats</h4>
      <span class="text-xs bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded-full font-medium">
        Dia {{ dayNumber }}
      </span>
    </div>

    <!-- Category selector -->
    <div class="px-4 pt-4 pb-3">
      <p class="text-xs font-medium text-gray-500 mb-2">Categoria</p>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="cat in CATEGORIES"
          :key="cat.value"
          @click="selectedCategory = cat.value"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium
                 border transition-colors"
          :class="selectedCategory === cat.value
            ? 'bg-indigo-600 border-indigo-600 text-white'
            : 'bg-white border-gray-200 text-gray-600 hover:border-indigo-300 hover:text-indigo-700'"
        >
          <span>{{ cat.icon }}</span>
          {{ cat.label }}
        </button>
      </div>
    </div>

    <!-- Action button -->
    <div class="px-4 pb-4">
      <button
        @click="suggest"
        :disabled="loading"
        class="w-full bg-indigo-600 text-white text-sm font-medium py-2.5 rounded-lg
               hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed
               transition-colors flex items-center justify-center gap-2"
      >
        <LoadingSpinner v-if="loading" size="sm" color="white" />
        <span>{{ loading ? 'Generant...' : 'Suggerir activitats' }}</span>
      </button>
    </div>

    <!-- Error -->
    <div v-if="error" class="mx-4 mb-4 bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-600">
      {{ error }}
    </div>

    <!-- Suggestions list -->
    <div v-if="suggestions.length > 0" class="border-t border-gray-100 divide-y divide-gray-50">
      <div
        v-for="(activity, idx) in suggestions"
        :key="idx"
        class="p-4"
      >
        <!-- Activity header -->
        <div class="flex items-start justify-between gap-2 mb-2">
          <h5 class="font-semibold text-sm text-gray-900 leading-snug">{{ activity.name }}</h5>
          <span v-if="activity.cost != null"
                class="flex-shrink-0 text-xs font-medium bg-green-100 text-green-700
                       px-2 py-0.5 rounded-full whitespace-nowrap">
            {{ activity.cost === 0 ? 'Gratis' : `${activity.cost}€` }}
          </span>
        </div>

        <!-- Description -->
        <p v-if="activity.description" class="text-xs text-gray-600 mb-3 leading-relaxed">
          {{ activity.description }}
        </p>

        <!-- Meta pills -->
        <div class="flex flex-wrap gap-2 mb-3">
          <span v-if="activity.time"
                class="inline-flex items-center gap-1 text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
            <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            {{ activity.time }}
          </span>
          <span v-if="activity.duration"
                class="inline-flex items-center gap-1 text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
            <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            {{ activity.duration }}
          </span>
        </div>

        <!-- Add button -->
        <button
          @click="addActivity(activity)"
          class="w-full border border-indigo-300 text-indigo-700 text-xs font-medium
                 py-1.5 rounded-lg hover:bg-indigo-50 transition-colors"
        >
          + Afegir a l'itinerari
        </button>
      </div>
    </div>

    <!-- Empty after suggest (no results) -->
    <div v-else-if="!loading && !error && suggestions.length === 0 && false" class="px-4 pb-4 text-center text-sm text-gray-500">
      Prova una altra categoria.
    </div>

  </div>
</template>
