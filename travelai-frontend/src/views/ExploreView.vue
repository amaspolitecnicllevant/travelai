<script setup>
import { ref, onMounted } from 'vue'
import { useTrips } from '@/composables/useTrips'
import TripCard from '@/components/trip/TripCard.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const { store, pagination, publicTrips, fetchPublicTrips } = useTrips()
const search = ref('')
let debounce = null

async function doSearch() {
  await fetchPublicTrips({ destination: search.value || undefined, page: 0 })
}

function onSearchInput() {
  clearTimeout(debounce)
  debounce = setTimeout(doSearch, 400)
}

async function changePage(page) {
  await fetchPublicTrips({ destination: search.value || undefined, page })
}

onMounted(() => fetchPublicTrips())
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-8">
    <!-- Header -->
    <div class="mb-8">
      <h1 class="text-2xl font-bold text-gray-900">Explorar viatges</h1>
      <p class="text-gray-500 mt-1 text-sm">Descobreix itineraris compartits per la comunitat</p>
    </div>

    <!-- Search -->
    <div class="relative mb-8">
      <input
        v-model="search"
        type="search"
        placeholder="Cerca per destí (Barcelona, Tokyo, Bali...)"
        class="w-full border border-gray-300 rounded-xl px-5 py-3 pl-11 text-sm
               focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
        @input="onSearchInput"
      />
      <span class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">🔍</span>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="flex justify-center py-16">
      <LoadingSpinner size="lg" />
    </div>

    <!-- Grid -->
    <div v-else-if="publicTrips.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      <TripCard v-for="trip in publicTrips" :key="trip.id" :trip="trip" />
    </div>

    <!-- Empty -->
    <div v-else class="text-center py-16">
      <div class="text-5xl mb-4">🗺</div>
      <p class="text-gray-500">No s'han trobat viatges per a "{{ search }}"</p>
      <button v-if="search" @click="search=''; doSearch()" class="mt-4 text-indigo-600 text-sm hover:underline">
        Veure tots els viatges
      </button>
    </div>

    <!-- Pagination -->
    <div v-if="pagination.totalPages > 1" class="flex justify-center items-center gap-2 mt-10">
      <button
        v-for="p in pagination.totalPages" :key="p"
        @click="changePage(p - 1)"
        class="w-9 h-9 rounded-lg text-sm font-medium transition-colors"
        :class="pagination.page === p - 1
          ? 'bg-indigo-600 text-white'
          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
      >
        {{ p }}
      </button>
    </div>
  </div>
</template>
