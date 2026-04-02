<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tripsApi } from '@/api/trips'
import { useAiStream } from '@/composables/useAiStream'
import { useItineraryStore } from '@/stores/itinerary'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ItineraryDay from '@/components/trip/ItineraryDay.vue'
import AiChatBox from '@/components/ai/AiChatBox.vue'

const route  = useRoute()
const router = useRouter()
const itineraryStore = useItineraryStore()

const { streaming, progress, rawBuffer, days, error: streamError, generate, refineDay, cancel } = useAiStream()

const trip        = ref(null)
const loadingTrip = ref(true)
const tripError   = ref(null)

// Panel de refinament
const refinePanel     = ref(false)
const refineDayNumber = ref(null)
const refinePrompt    = ref('')

// Mostrar dies des de l'stream o des del store
const displayDays = computed(() => {
  if (days.value && days.value.length > 0) return days.value
  return itineraryStore.currentItinerary?.days || []
})

const hasItinerary = computed(() => displayDays.value.length > 0)

onMounted(async () => {
  try {
    const { data } = await tripsApi.getById(route.params.id)
    trip.value = data
  } catch (e) {
    tripError.value = e.response?.data?.message || 'Error carregant el viatge'
  } finally {
    loadingTrip.value = false
  }

  // Intentar carregar itinerari existent
  await itineraryStore.fetchItinerary(route.params.id)
})

async function handleGenerate() {
  await generate(route.params.id)
  // Reload from DB — the backend saves the parsed itinerary after streaming
  await itineraryStore.fetchItinerary(route.params.id)
}

function openRefinePanel(dayNumber) {
  refineDayNumber.value = dayNumber
  refinePanel.value     = true
  refinePrompt.value    = ''
}

function closeRefinePanel() {
  refinePanel.value     = false
  refineDayNumber.value = null
}

// La crida a refineDay la fa AiChatBox internament.
// Quan acaba, emet 'days-updated' i recarreguem de BD.
async function handleRefineDone() {
  closeRefinePanel()
  await itineraryStore.fetchItinerary(route.params.id)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long', year: 'numeric' })
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Loading del trip -->
    <div v-if="loadingTrip" class="flex items-center justify-center min-h-screen">
      <LoadingSpinner size="xl" />
    </div>

    <!-- Error carregant el trip -->
    <div v-else-if="tripError" class="flex items-center justify-center min-h-screen">
      <div class="text-center">
        <p class="text-red-500 font-medium mb-4">{{ tripError }}</p>
        <button @click="router.back()" class="text-indigo-600 hover:underline text-sm">Tornar</button>
      </div>
    </div>

    <!-- Contingut principal -->
    <div v-else class="max-w-7xl mx-auto px-4 py-6">
      <div class="flex gap-6">

        <!-- Sidebar esquerra -->
        <aside class="w-72 flex-shrink-0">
          <div class="bg-white rounded-2xl shadow-sm border border-gray-200 p-5 sticky top-20">
            <!-- Info del viatge -->
            <div class="mb-5">
              <div class="flex items-center gap-2 mb-3">
                <span class="text-2xl">✈</span>
                <div>
                  <h1 class="font-bold text-gray-900 text-lg leading-tight">{{ trip.title }}</h1>
                  <p class="text-indigo-600 text-sm font-medium">{{ trip.destination }}</p>
                </div>
              </div>
              <div class="space-y-1 text-sm text-gray-500">
                <div v-if="trip.startDate" class="flex items-center gap-2">
                  <span>📅</span>
                  <span>{{ formatDate(trip.startDate) }}</span>
                </div>
                <div v-if="trip.endDate" class="flex items-center gap-2">
                  <span>🏁</span>
                  <span>{{ formatDate(trip.endDate) }}</span>
                </div>
                <div v-if="trip.durationDays" class="flex items-center gap-2">
                  <span>🗓</span>
                  <span>{{ trip.durationDays }} dies</span>
                </div>
                <div v-if="trip.budget" class="flex items-center gap-2">
                  <span>💰</span>
                  <span>{{ trip.budget }}€</span>
                </div>
              </div>
            </div>

            <hr class="border-gray-100 mb-5" />

            <!-- Botó generar -->
            <button
              :disabled="streaming"
              class="w-full bg-indigo-600 text-white font-semibold py-3 px-4 rounded-xl
                     hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed
                     transition-colors flex items-center justify-center gap-2 text-sm"
              @click="handleGenerate"
            >
              <LoadingSpinner v-if="streaming" size="sm" color="white" />
              <span v-if="!streaming">Generar itinerari amb IA</span>
              <span v-else>Generant...</span>
            </button>

            <!-- Botó cancel·lar -->
            <button
              v-if="streaming"
              class="w-full mt-2 text-sm text-gray-500 hover:text-red-500 py-2 transition-colors"
              @click="cancel"
            >
              Cancel·lar generació
            </button>

            <!-- Barra de progrés / estat -->
            <div v-if="streaming || progress" class="mt-4">
              <div v-if="streaming" class="flex items-center gap-2 mb-2">
                <div class="flex-1 bg-gray-200 rounded-full h-1.5 overflow-hidden">
                  <div class="bg-indigo-500 h-1.5 rounded-full animate-pulse w-3/4"></div>
                </div>
              </div>
              <p class="text-xs text-gray-500 text-center">{{ progress }}</p>
            </div>

            <!-- Error stream -->
            <div v-if="streamError" class="mt-3 text-xs text-red-500 bg-red-50 rounded-lg p-2 text-center">
              {{ streamError }}
            </div>

            <!-- Nav dies -->
            <div v-if="hasItinerary" class="mt-5">
              <hr class="border-gray-100 mb-4" />
              <p class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">Dies</p>
              <nav class="space-y-1">
                <a
                  v-for="day in displayDays"
                  :key="day.dayNumber"
                  :href="`#day-${day.dayNumber}`"
                  class="block text-sm text-gray-600 hover:text-indigo-600 hover:bg-indigo-50
                         rounded-lg px-3 py-1.5 transition-colors"
                >
                  Dia {{ day.dayNumber }}: {{ day.title || day.destination || '' }}
                </a>
              </nav>
            </div>

            <!-- Accions addicionals -->
            <div class="mt-5 pt-4 border-t border-gray-100 flex flex-col gap-2">
              <router-link
                :to="`/trips/${route.params.id}/edit`"
                class="text-sm text-gray-600 hover:text-indigo-600 text-center py-1 transition-colors"
              >
                Editar informació
              </router-link>
              <router-link
                :to="`/trips/${route.params.id}`"
                class="text-sm text-gray-600 hover:text-indigo-600 text-center py-1 transition-colors"
              >
                Veure pàgina pública
              </router-link>
            </div>
          </div>
        </aside>

        <!-- Zona central: dies del itinerari -->
        <main class="flex-1 min-w-0">

          <!-- Streaming en temps real (text parcial) -->
          <div v-if="streaming && rawBuffer" class="bg-white rounded-2xl shadow-sm border border-indigo-200 p-6 mb-6">
            <div class="flex items-center gap-3 mb-4">
              <LoadingSpinner size="sm" />
              <h2 class="font-semibold text-indigo-700">Generant itinerari...</h2>
            </div>
            <pre class="text-sm text-gray-700 whitespace-pre-wrap font-mono bg-gray-50 rounded-xl p-4 max-h-64 overflow-y-auto">{{ rawBuffer }}<span class="animate-pulse text-indigo-500">▌</span></pre>
          </div>

          <!-- Dies de l'itinerari -->
          <div v-if="hasItinerary" class="space-y-4">
            <ItineraryDay
              v-for="day in displayDays"
              :key="day.dayNumber"
              :day="day"
              :streaming="streaming"
              @refine="openRefinePanel"
            />
          </div>

          <!-- Estat buit: sense itinerari -->
          <div v-else-if="!streaming" class="flex flex-col items-center justify-center py-24 text-center">
            <div class="text-6xl mb-6">🗺</div>
            <h2 class="text-xl font-semibold text-gray-700 mb-2">Encara no hi ha itinerari</h2>
            <p class="text-gray-500 text-sm mb-6 max-w-sm">
              Fes clic a "Generar itinerari amb IA" per deixar que la intel·ligència artificial
              planifiqui el teu viatge a <strong>{{ trip.destination }}</strong>.
            </p>
            <button
              class="bg-indigo-600 text-white font-semibold py-3 px-6 rounded-xl
                     hover:bg-indigo-700 transition-colors text-sm"
              @click="handleGenerate"
            >
              Generar itinerari amb IA
            </button>
          </div>

          <!-- Spinner central durant generació sense buffer -->
          <div v-else-if="streaming && !rawBuffer" class="flex flex-col items-center justify-center py-24">
            <LoadingSpinner size="xl" />
            <p class="mt-4 text-gray-500 text-sm">{{ progress }}</p>
          </div>
        </main>

      </div>
    </div>

    <!-- Panel lateral de refinament (overlay dret) -->
    <Transition name="slide-right">
      <div
        v-if="refinePanel"
        class="fixed inset-y-0 right-0 w-96 bg-white shadow-2xl border-l border-gray-200 z-50
               flex flex-col"
      >
        <!-- Capçalera panel -->
        <div class="flex items-center justify-between p-4 border-b border-gray-200">
          <div>
            <h3 class="font-semibold text-gray-900">Refinar dia {{ refineDayNumber }}</h3>
            <p class="text-xs text-gray-500">Descriu com vols modificar aquest dia</p>
          </div>
          <button
            class="p-2 rounded-lg text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors"
            @click="closeRefinePanel"
          >
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- Contingut panel -->
        <div class="flex-1 overflow-y-auto p-4">
          <!-- Text streaming en el panel -->
          <div v-if="streaming" class="mb-4">
            <div class="flex items-center gap-2 mb-2">
              <LoadingSpinner size="sm" />
              <span class="text-sm text-indigo-600 font-medium">{{ progress }}</span>
            </div>
            <div v-if="rawBuffer"
                 class="bg-gray-50 rounded-xl p-3 text-sm text-gray-700 font-mono whitespace-pre-wrap
                        max-h-64 overflow-y-auto">
              {{ rawBuffer }}<span class="animate-pulse text-indigo-500">▌</span>
            </div>
          </div>

          <!-- Error -->
          <div v-if="streamError" class="bg-red-50 border border-red-200 rounded-xl p-3 mb-4">
            <p class="text-sm text-red-600">{{ streamError }}</p>
          </div>

          <!-- Chat box -->
          <AiChatBox
            :trip-id="route.params.id"
            :day-number="refineDayNumber"
            :disabled="streaming"
            @days-updated="handleRefineDone"
          />
        </div>
      </div>
    </Transition>

    <!-- Backdrop del panel -->
    <Transition name="fade">
      <div
        v-if="refinePanel"
        class="fixed inset-0 bg-black/30 z-40"
        @click="closeRefinePanel"
      />
    </Transition>
  </div>
</template>

<style scoped>
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.3s ease;
}
.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
