<script setup>
import { onMounted } from 'vue'
import { useTrips } from '@/composables/useTrips'
import { useAuthStore } from '@/stores/auth'
import TripCard from '@/components/trip/TripCard.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const { store, publicTrips, fetchPublicTrips } = useTrips()
const auth = useAuthStore()

onMounted(() => fetchPublicTrips({ page: 0, size: 18 }))
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">
          Bon dia, {{ auth.user?.name?.split(' ')[0] || 'Viatger' }}! 👋
        </h1>
        <p class="text-gray-500 mt-1 text-sm">Viatges recents de la comunitat</p>
      </div>
      <router-link to="/trips/new"
        class="bg-indigo-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-indigo-700 transition-colors">
        + Nou viatge
      </router-link>
    </div>

    <!-- Quick actions -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-8">
      <router-link to="/trips/new"
        class="flex flex-col items-center gap-2 bg-indigo-50 hover:bg-indigo-100 rounded-xl p-4 transition-colors text-center">
        <span class="text-2xl">✈</span>
        <span class="text-xs font-medium text-indigo-700">Nou viatge</span>
      </router-link>
      <router-link to="/explore"
        class="flex flex-col items-center gap-2 bg-blue-50 hover:bg-blue-100 rounded-xl p-4 transition-colors text-center">
        <span class="text-2xl">🌍</span>
        <span class="text-xs font-medium text-blue-700">Explorar</span>
      </router-link>
      <router-link to="/profile"
        class="flex flex-col items-center gap-2 bg-green-50 hover:bg-green-100 rounded-xl p-4 transition-colors text-center">
        <span class="text-2xl">👤</span>
        <span class="text-xs font-medium text-green-700">El meu perfil</span>
      </router-link>
      <router-link to="/my-data"
        class="flex flex-col items-center gap-2 bg-gray-50 hover:bg-gray-100 rounded-xl p-4 transition-colors text-center">
        <span class="text-2xl">🔒</span>
        <span class="text-xs font-medium text-gray-600">Les meves dades</span>
      </router-link>
    </div>

    <!-- Feed title -->
    <h2 class="text-lg font-semibold text-gray-800 mb-5">Viatges recents</h2>

    <!-- Loading -->
    <div v-if="store.loading" class="flex justify-center py-16">
      <LoadingSpinner size="lg" />
    </div>

    <!-- Grid -->
    <div v-else-if="publicTrips.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <TripCard v-for="trip in publicTrips" :key="trip.id" :trip="trip" />
    </div>

    <div v-else class="text-center py-16">
      <div class="text-5xl mb-4">🗺</div>
      <p class="text-gray-500 mb-4">Encara no hi ha viatges al feed</p>
      <router-link to="/trips/new" class="text-indigo-600 text-sm font-medium hover:underline">
        Crea el teu primer viatge →
      </router-link>
    </div>
  </div>
</template>
