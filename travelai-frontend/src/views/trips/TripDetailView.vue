<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tripsApi } from '@/api/trips'
import { itineraryApi } from '@/api/itinerary'
import { useAuthStore } from '@/stores/auth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ItineraryDay from '@/components/trip/ItineraryDay.vue'
import RatingStars from '@/components/trip/RatingStars.vue'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()

const trip        = ref(null)
const days        = ref([])
const loading     = ref(true)
const error       = ref(null)
const ratingError = ref(null)

const isOwner = computed(() =>
  auth.isLoggedIn && auth.user?.username === trip.value?.ownerUsername
)

onMounted(async () => {
  try {
    const [tripRes, itinRes] = await Promise.all([
      tripsApi.getById(route.params.id),
      itineraryApi.get(route.params.id).catch(() => ({ data: [] })),
    ])
    trip.value = tripRes.data

    // Transform List<ItineraryResponse> → [{dayNumber, date, title, activities}]
    days.value = (Array.isArray(itinRes.data) ? itinRes.data : []).map(d => {
      const activities = (d.plans || []).map(p => ({
        time:        p.time,
        name:        p.activity || p.name,
        description: p.description,
        location:    p.location,
        type:        p.type,
        cost:        p.cost,
        duration:    p.duration,
      }))
      const summary = activities.slice(0, 3).map(a => a.name).filter(Boolean).join(' · ')
      return {
        dayNumber:   d.dayNumber,
        date:        d.date,
        title:       d.title || `Dia ${d.dayNumber}`,
        description: summary || null,
        activities,
      }
    })
  } catch (e) {
    error.value = e.response?.data?.message || 'Error carregant el viatge'
  } finally {
    loading.value = false
  }
})

async function submitRating(score) {
  ratingError.value = null
  try {
    await itineraryApi.rate(route.params.id, score)
  } catch (e) {
    ratingError.value = e.response?.data?.message || 'Error valorant el viatge'
  }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long', year: 'numeric' })
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center min-h-screen">
      <LoadingSpinner size="xl" />
    </div>

    <!-- Error -->
    <div v-else-if="error" class="flex items-center justify-center min-h-screen">
      <div class="text-center">
        <p class="text-red-500 font-medium mb-4">{{ error }}</p>
        <button @click="router.back()" class="text-indigo-600 hover:underline text-sm">Tornar</button>
      </div>
    </div>

    <!-- Contingut -->
    <div v-else class="max-w-4xl mx-auto px-4 py-8">

      <!-- Capçalera -->
      <div class="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 mb-6">
        <div class="flex items-start justify-between gap-4">
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-xs px-2 py-0.5 rounded-full font-medium"
                    :class="trip.visibility === 'PUBLIC' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'">
                {{ trip.visibility === 'PUBLIC' ? '🌍 Públic' : '🔒 Privat' }}
              </span>
              <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 font-medium">
                {{ trip.status === 'PUBLISHED' ? 'Publicat' : 'Esborrany' }}
              </span>
            </div>
            <h1 class="text-2xl font-bold text-gray-900 truncate">{{ trip.title }}</h1>
            <p class="text-indigo-600 font-medium mt-1 flex items-center gap-1">
              <span>📍</span> {{ trip.destination }}
            </p>
            <div class="flex flex-wrap gap-4 mt-3 text-sm text-gray-500">
              <span v-if="trip.startDate">📅 {{ formatDate(trip.startDate) }}</span>
              <span v-if="trip.endDate">🏁 {{ formatDate(trip.endDate) }}</span>
              <span v-if="trip.ownerUsername">👤 @{{ trip.ownerUsername }}</span>
            </div>
            <p v-if="trip.description" class="mt-3 text-sm text-gray-600 leading-relaxed">
              {{ trip.description }}
            </p>
          </div>

          <!-- Accions propietari -->
          <div v-if="isOwner" class="flex flex-col gap-2 flex-shrink-0">
            <router-link :to="`/trips/${trip.id}/planner`"
              class="bg-indigo-600 text-white text-sm font-medium px-4 py-2 rounded-lg
                     hover:bg-indigo-700 transition-colors text-center whitespace-nowrap">
              Obrir planner
            </router-link>
            <router-link :to="`/trips/${trip.id}/edit`"
              class="border border-gray-300 text-gray-700 text-sm font-medium px-4 py-2 rounded-lg
                     hover:bg-gray-50 transition-colors text-center">
              Editar
            </router-link>
          </div>
        </div>

        <!-- Valoració (per a no propietaris autenticats) -->
        <div v-if="auth.isLoggedIn && !isOwner" class="mt-5 pt-5 border-t border-gray-100">
          <p class="text-sm font-medium text-gray-700 mb-2">Valorar aquest viatge:</p>
          <RatingStars :model-value="trip.averageRating || 0" @update:model-value="submitRating" />
          <p v-if="ratingError" class="text-xs text-red-500 mt-1">{{ ratingError }}</p>
        </div>
        <div v-else-if="trip.averageRating" class="mt-4 flex items-center gap-2">
          <RatingStars :model-value="trip.averageRating" :readonly="true" />
          <span class="text-sm text-gray-500">{{ trip.averageRating.toFixed(1) }}</span>
        </div>
      </div>

      <!-- Itinerari -->
      <div v-if="days.length" class="space-y-4">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">Itinerari</h2>
        <ItineraryDay
          v-for="day in days"
          :key="day.dayNumber"
          :day="day"
          :streaming="false"
        />
      </div>

      <!-- Sense itinerari -->
      <div v-else class="bg-white rounded-2xl border border-gray-200 shadow-sm p-12 text-center">
        <div class="text-5xl mb-4">🗺</div>
        <p class="text-gray-500 font-medium">Encara no hi ha itinerari per a aquest viatge</p>
        <router-link v-if="isOwner" :to="`/trips/${trip.id}/planner`"
          class="mt-4 inline-block bg-indigo-600 text-white px-5 py-2.5 rounded-lg text-sm font-medium hover:bg-indigo-700">
          Generar itinerari amb IA
        </router-link>
      </div>

      <!-- Tornar -->
      <div class="mt-6">
        <button @click="router.back()" class="text-sm text-gray-400 hover:text-gray-600 transition-colors">
          ← Tornar
        </button>
      </div>
    </div>
  </div>
</template>
