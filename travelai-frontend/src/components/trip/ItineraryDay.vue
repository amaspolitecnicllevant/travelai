<script setup>
import { computed, ref } from 'vue'
import DayMap from './DayMap.vue'
import ActivityEditModal from './ActivityEditModal.vue'

const props = defineProps({
  day:       { type: Object, required: true },
  streaming: { type: Boolean, default: false },
  isOwner:   { type: Boolean, default: false },
})
const emit = defineEmits(['refine', 'edit', 'save-activity'])

const showMap         = ref(false)
const editingActivity = ref(null)
const editingIndex    = ref(null)

const typeConfig = {
  CULTURE:       { icon: '🏛', label: 'Cultura',       color: 'bg-purple-100 text-purple-700' },
  FOOD:          { icon: '🍽', label: 'Gastronomia',   color: 'bg-orange-100 text-orange-700' },
  LEISURE:       { icon: '🌿', label: 'Lleure',        color: 'bg-green-100  text-green-700'  },
  TRANSPORT:     { icon: '🚌', label: 'Transport',     color: 'bg-blue-100   text-blue-700'   },
  ACCOMMODATION: { icon: '🏨', label: 'Allotjament',   color: 'bg-teal-100   text-teal-700'   },
  SHOPPING:      { icon: '🛍', label: 'Compres',       color: 'bg-pink-100   text-pink-700'   },
  NATURE:        { icon: '🏞', label: 'Natura',        color: 'bg-lime-100   text-lime-700'   },
  SPORT:         { icon: '⚽', label: 'Esport',        color: 'bg-yellow-100 text-yellow-700' },
  NIGHTLIFE:     { icon: '🎉', label: 'Nit',           color: 'bg-fuchsia-100 text-fuchsia-700'},
}

const transportConfig = {
  WALK:   { icon: '🚶', label: 'A peu',            color: 'bg-emerald-50 text-emerald-600 border-emerald-100' },
  PUBLIC: { icon: '🚌', label: 'Transport públic', color: 'bg-blue-50    text-blue-600    border-blue-100'    },
  CAR:    { icon: '🚗', label: 'Cotxe',            color: 'bg-gray-50    text-gray-600    border-gray-200'    },
  TAXI:   { icon: '🚕', label: 'Taxi',             color: 'bg-yellow-50  text-yellow-700  border-yellow-100'  },
}

function getTypeConfig(type) {
  return typeConfig[type] || { icon: '📌', label: type || 'Activitat', color: 'bg-gray-100 text-gray-600' }
}

function getTransportConfig(mode) {
  return transportConfig[mode] || null
}

const totalCost = computed(() => {
  if (!props.day.activities?.length) return null
  const sum = props.day.activities.reduce((acc, a) => acc + (parseFloat(a.cost) || 0), 0)
  return sum > 0 ? sum.toFixed(0) : null
})

const hasLocations = computed(() =>
  props.day.activities?.some(a => a.location)
)

function handleRefine() {
  emit('refine', props.day.dayNumber)
  emit('edit', props.day)
}

function openEdit(activity, idx) {
  editingActivity.value = { ...activity }
  editingIndex.value = idx
}

function onActivitySaved(updatedActivity) {
  emit('save-activity', {
    dayNumber: props.day.dayNumber,
    index:     editingIndex.value,
    activity:  updatedActivity,
  })
  editingActivity.value = null
  editingIndex.value    = null
}
</script>

<template>
  <div :id="`day-${day.dayNumber}`" class="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
    <!-- Capçalera -->
    <div class="bg-gradient-to-r from-indigo-600 to-indigo-500 text-white px-5 py-4
                flex items-center justify-between">
      <div>
        <div class="flex items-center gap-2">
          <span class="bg-white/20 text-white text-xs font-bold px-2 py-0.5 rounded-full">
            Dia {{ day.dayNumber }}
          </span>
          <span v-if="day.date" class="text-white/70 text-xs">{{ day.date }}</span>
        </div>
        <h3 v-if="day.title" class="font-semibold mt-1">{{ day.title }}</h3>
      </div>

      <div class="flex items-center gap-2">
        <div v-if="totalCost" class="text-right mr-1">
          <p class="text-white/60 text-xs">Cost estimat</p>
          <p class="font-semibold text-sm">~{{ totalCost }}€</p>
        </div>

        <!-- Mapa -->
        <button
          v-if="hasLocations"
          class="flex items-center gap-1 bg-white/20 hover:bg-white/30 text-white text-xs
                 font-medium px-2.5 py-1.5 rounded-lg transition-colors"
          @click="showMap = !showMap"
        >
          <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7"/>
          </svg>
          {{ showMap ? 'Amagar' : 'Mapa' }}
        </button>

        <!-- Refinar IA (només propietari) -->
        <button
          v-if="isOwner"
          :disabled="streaming"
          class="flex items-center gap-1.5 bg-white/20 hover:bg-white/30 disabled:opacity-50
                 disabled:cursor-not-allowed text-white text-xs font-medium px-3 py-1.5
                 rounded-lg transition-colors"
          @click="handleRefine"
        >
          <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/>
          </svg>
          Refinar
        </button>
      </div>
    </div>

    <!-- Mapa del dia -->
    <div v-if="showMap" class="p-3 bg-gray-50 border-b border-gray-100">
      <DayMap :activities="day.activities" :day-number="day.dayNumber" />
    </div>

    <!-- Descripció del dia -->
    <div v-if="day.description" class="px-5 py-3 bg-indigo-50 border-b border-indigo-100">
      <p class="text-sm text-indigo-800">{{ day.description }}</p>
    </div>

    <!-- Activitats -->
    <div class="divide-y divide-gray-50">
      <template v-for="(activity, idx) in day.activities" :key="idx">

        <!-- Badge de transport entre activitats (des de la 2a) -->
        <div v-if="idx > 0 && activity.transportMode" class="px-5 py-1.5 flex items-center gap-2 bg-gray-50/60">
          <div class="w-14 shrink-0" />
          <span
            class="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-full border font-medium"
            :class="getTransportConfig(activity.transportMode)?.color || 'bg-gray-50 text-gray-500 border-gray-200'"
          >
            {{ getTransportConfig(activity.transportMode)?.icon }}
            {{ getTransportConfig(activity.transportMode)?.label }}
            <span v-if="activity.travelTime" class="opacity-70">· {{ activity.travelTime }}</span>
          </span>
        </div>

        <!-- Fila de l'activitat -->
        <div class="px-5 py-4 flex gap-4 hover:bg-gray-50 transition-colors group">
          <!-- Hora -->
          <div class="w-14 shrink-0 text-right pt-1">
            <span class="text-xs font-medium text-gray-600">{{ activity.time || '' }}</span>
            <span v-if="activity.endTime" class="block text-[10px] text-gray-400">{{ activity.endTime }}</span>
          </div>

          <!-- Icona + categoria -->
          <div class="shrink-0 flex flex-col items-center gap-1">
            <span class="text-xl">{{ getTypeConfig(activity.type).icon }}</span>
            <span v-if="activity.type"
                  class="text-[10px] px-1.5 py-0.5 rounded-full font-medium whitespace-nowrap"
                  :class="getTypeConfig(activity.type).color">
              {{ getTypeConfig(activity.type).label }}
            </span>
          </div>

          <!-- Contingut -->
          <div class="flex-1 min-w-0">
            <p class="font-semibold text-gray-900 text-sm">{{ activity.name || activity.title }}</p>
            <p v-if="activity.description" class="text-gray-500 text-xs mt-1 leading-relaxed">
              {{ activity.description }}
            </p>
            <div class="flex items-center gap-3 mt-2 flex-wrap">
              <span v-if="activity.duration" class="text-xs text-gray-400 flex items-center gap-1">
                <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                {{ activity.duration }}
              </span>
              <span v-if="activity.cost" class="text-xs text-indigo-600 font-semibold">
                ~{{ activity.cost }}€
              </span>
              <span v-if="activity.location" class="text-xs text-gray-400 flex items-center gap-1">
                <svg class="h-3 w-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"/>
                </svg>
                {{ activity.location }}
              </span>
            </div>
          </div>

          <!-- Botó editar (sempre visible si propietari) -->
          <div v-if="isOwner" class="shrink-0 self-start">
            <button
              class="p-1.5 rounded-lg text-gray-300 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"
              title="Editar activitat"
              @click="openEdit(activity, idx)"
            >
              <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
            </button>
          </div>
        </div>
      </template>

      <!-- Buit -->
      <div v-if="!day.activities?.length" class="px-5 py-8 text-center">
        <p class="text-gray-400 text-sm">Sense activitats per aquest dia</p>
        <button v-if="isOwner" class="mt-2 text-xs text-indigo-600 hover:underline" @click="handleRefine">
          Generar activitats amb IA
        </button>
      </div>
    </div>
  </div>

  <!-- Modal edició manual -->
  <ActivityEditModal
    :show="!!editingActivity"
    :activity="editingActivity"
    :day-number="day.dayNumber"
    @close="editingActivity = null; editingIndex = null"
    @save="onActivitySaved"
  />
</template>
