<script setup>
import { ref } from 'vue'
import { useAiStream } from '@/composables/useAiStream'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps({
  tripId:    { type: [String, Number], required: true },
  dayNumber: { type: Number,           required: true },
})

const emit = defineEmits(['add-activity'])

const CATEGORIES = [
  { value: 'CULTURE',   label: 'Cultura',     icon: '🏛' },
  { value: 'FOOD',      label: 'Gastronomia', icon: '🍽' },
  { value: 'LEISURE',   label: 'Lleure',      icon: '🎭' },
  { value: 'NATURE',    label: 'Natura',      icon: '🌿' },
  { value: 'SPORT',     label: 'Esport',      icon: '⚽' },
  { value: 'NIGHTLIFE', label: 'Nit',         icon: '🌙' },
]

const selectedCategory = ref('CULTURE')

const { suggestStreaming, suggestError, suggestResult, suggestActivities } = useAiStream()

function suggest() {
  suggestActivities(props.tripId, props.dayNumber, selectedCategory.value)
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
        :disabled="suggestStreaming"
        class="w-full bg-indigo-600 text-white text-sm font-medium py-2.5 rounded-lg
               hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed
               transition-colors flex items-center justify-center gap-2"
      >
        <LoadingSpinner v-if="suggestStreaming" size="sm" color="white" />
        <span>{{ suggestStreaming ? 'Generant...' : 'Suggerir activitats' }}</span>
      </button>
    </div>

    <!-- Error -->
    <div v-if="suggestError" class="mx-4 mb-4 bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-600">
      {{ suggestError }}
    </div>

    <!-- Streaming indicator -->
    <div v-if="suggestStreaming" class="mx-4 mb-4 flex items-center gap-2 text-xs text-gray-400">
      <div class="flex-1 bg-gray-100 rounded-full h-1 overflow-hidden">
        <div class="bg-indigo-400 h-1 rounded-full animate-pulse w-2/3"></div>
      </div>
      <span>Generant suggeriments...</span>
    </div>

    <!-- Suggestions list -->
    <div v-if="suggestResult.length > 0" class="border-t border-gray-100 divide-y divide-gray-50">
      <div
        v-for="(activity, idx) in suggestResult"
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
            🕐 {{ activity.time }}
          </span>
          <span v-if="activity.duration"
                class="inline-flex items-center gap-1 text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
            ⏱ {{ activity.duration }}
          </span>
          <span v-if="activity.location"
                class="inline-flex items-center gap-1 text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
            📍 {{ activity.location }}
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

  </div>
</template>
