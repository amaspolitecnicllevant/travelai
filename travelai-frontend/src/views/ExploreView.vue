<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { tripsApi } from '@/api/trips'
import TripCard from '@/components/trip/TripCard.vue'

const router = useRouter()

// ── Search ──────────────────────────────────────────────────────────────────
const searchQuery = ref('')

function doSearch() {
  const q = searchQuery.value.trim()
  if (q) router.push({ name: 'search', query: { q } })
}

// ── Destinations ────────────────────────────────────────────────────────────
const destinations = [
  { name: 'Barcelona', country: 'Espanya',   gradient: 'from-orange-500 to-red-500',    emoji: '🥘', trips: 142 },
  { name: 'París',     country: 'França',    gradient: 'from-pink-500 to-rose-600',     emoji: '🗼', trips: 218 },
  { name: 'Tokyo',     country: 'Japó',      gradient: 'from-red-500 to-pink-500',      emoji: '⛩️', trips: 195 },
  { name: 'Nova York', country: 'EUA',       gradient: 'from-blue-600 to-indigo-700',   emoji: '🗽', trips: 176 },
  { name: 'Roma',      country: 'Itàlia',    gradient: 'from-yellow-500 to-orange-500', emoji: '🏛️', trips: 134 },
  { name: 'Bangkok',   country: 'Tailàndia', gradient: 'from-emerald-500 to-teal-600',  emoji: '🛺', trips: 89  },
]

function exploreDestination(dest) {
  router.push({ name: 'search', query: { q: dest.name } })
}

// ── Public trips ─────────────────────────────────────────────────────────────
const recentTrips    = ref([])
const topRatedTrips  = ref([])
const loadingRecent  = ref(false)
const loadingRated   = ref(false)

onMounted(async () => {
  loadingRecent.value = true
  loadingRated.value  = true

  try {
    const { data } = await tripsApi.getAll({ size: 6, sort: 'createdAt,desc', visibility: 'PUBLIC' })
    recentTrips.value = data.content ?? data
  } catch { recentTrips.value = [] }
  finally { loadingRecent.value = false }

  try {
    const { data } = await tripsApi.getAll({ size: 6, sort: 'avgRating,desc', visibility: 'PUBLIC' })
    topRatedTrips.value = data.content ?? data
  } catch { topRatedTrips.value = [] }
  finally { loadingRated.value = false }
})
</script>

<template>
  <div class="min-h-screen bg-gray-50">

    <!-- ── Hero ─────────────────────────────────────────────────────────── -->
    <section class="relative bg-gradient-to-br from-indigo-600 via-blue-600 to-cyan-500 text-white overflow-hidden">
      <div class="absolute -top-20 -right-20 w-80 h-80 bg-white/5 rounded-full"></div>
      <div class="absolute -bottom-16 -left-16 w-64 h-64 bg-white/5 rounded-full"></div>

      <div class="relative z-10 max-w-4xl mx-auto px-4 py-20 text-center">
        <p class="text-indigo-200 text-sm font-medium uppercase tracking-widest mb-3">TravelAI · Explora</p>
        <h1 class="text-4xl sm:text-5xl font-bold leading-tight mb-4">Descobreix el món</h1>
        <p class="text-indigo-100 text-lg max-w-xl mx-auto mb-10">
          Inspira't amb viatges reals, descobreix destinacions i crea els teus propis itineraris amb IA.
        </p>

        <!-- Search bar -->
        <form @submit.prevent="doSearch" class="max-w-xl mx-auto">
          <div class="relative">
            <svg class="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400"
                 fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
            </svg>
            <input
              v-model="searchQuery"
              type="search"
              placeholder="Cerca destins, viatges, usuaris..."
              class="w-full pl-12 pr-32 py-4 rounded-2xl text-gray-900 text-base shadow-xl
                     focus:outline-none focus:ring-4 focus:ring-white/30 placeholder-gray-400"
            />
            <button
              type="submit"
              class="absolute right-2 top-1/2 -translate-y-1/2 bg-indigo-600 text-white
                     px-5 py-2.5 rounded-xl font-semibold text-sm hover:bg-indigo-700 transition-colors"
            >
              Cercar
            </button>
          </div>
        </form>
      </div>
    </section>

    <!-- ── Destins populars ──────────────────────────────────────────────── -->
    <section class="max-w-6xl mx-auto px-4 py-14">
      <h2 class="text-xl font-bold text-gray-900 mb-6">Destins populars</h2>
      <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
        <button
          v-for="dest in destinations"
          :key="dest.name"
          @click="exploreDestination(dest)"
          class="group relative rounded-2xl overflow-hidden aspect-square shadow-sm
                 hover:shadow-xl transition-all duration-300 hover:-translate-y-1"
        >
          <div :class="`absolute inset-0 bg-gradient-to-br ${dest.gradient}`"></div>
          <div class="absolute inset-0 bg-black/20 group-hover:bg-black/10 transition-colors"></div>
          <div class="relative z-10 flex flex-col items-center justify-center h-full p-3 text-white">
            <span class="text-3xl mb-1">{{ dest.emoji }}</span>
            <span class="font-bold text-sm text-center leading-tight">{{ dest.name }}</span>
            <span class="text-xs text-white/80 mt-0.5">{{ dest.country }}</span>
            <span class="text-xs text-white/60 mt-1">{{ dest.trips }} viatges</span>
          </div>
        </button>
      </div>
    </section>

    <!-- ── Últims viatges ────────────────────────────────────────────────── -->
    <section class="max-w-6xl mx-auto px-4 pb-14">
      <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-bold text-gray-900">Últims viatges</h2>
        <router-link to="/search" class="text-sm text-indigo-600 hover:text-indigo-700 font-medium">
          Veure tots →
        </router-link>
      </div>

      <div v-if="loadingRecent" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <div v-for="i in 6" :key="i"
             class="bg-white rounded-xl border border-gray-100 overflow-hidden animate-pulse">
          <div class="h-44 bg-gray-200"></div>
          <div class="p-4 space-y-2">
            <div class="h-4 bg-gray-200 rounded w-3/4"></div>
            <div class="h-3 bg-gray-200 rounded w-1/2"></div>
          </div>
        </div>
      </div>

      <div v-else-if="recentTrips.length"
           class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <TripCard v-for="trip in recentTrips" :key="trip.id" :trip="trip" />
      </div>

      <div v-else class="text-center py-12 text-gray-400">
        <div class="text-4xl mb-3">🗺</div>
        <p class="text-sm">Encara no hi ha viatges públics</p>
      </div>
    </section>

    <!-- ── Millor valorats ───────────────────────────────────────────────── -->
    <section class="bg-white border-t border-gray-100">
      <div class="max-w-6xl mx-auto px-4 py-14">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-xl font-bold text-gray-900">Millor valorats ⭐</h2>
          <router-link to="/search" class="text-sm text-indigo-600 hover:text-indigo-700 font-medium">
            Veure tots →
          </router-link>
        </div>

        <div v-if="loadingRated" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <div v-for="i in 6" :key="i"
               class="bg-white rounded-xl border border-gray-100 overflow-hidden animate-pulse">
            <div class="h-44 bg-gray-200"></div>
            <div class="p-4 space-y-2">
              <div class="h-4 bg-gray-200 rounded w-3/4"></div>
              <div class="h-3 bg-gray-200 rounded w-1/2"></div>
            </div>
          </div>
        </div>

        <div v-else-if="topRatedTrips.length"
             class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <TripCard v-for="trip in topRatedTrips" :key="trip.id" :trip="trip" />
        </div>

        <div v-else class="text-center py-12 text-gray-400">
          <div class="text-4xl mb-3">⭐</div>
          <p class="text-sm">Encara no hi ha viatges valorats</p>
        </div>
      </div>
    </section>

    <!-- ── Footer legal ──────────────────────────────────────────────────── -->
    <footer class="bg-gray-900 text-gray-400 py-8">
      <div class="max-w-6xl mx-auto px-4">
        <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
          <p class="text-sm font-semibold text-white">TravelAI</p>
          <nav class="flex flex-wrap gap-x-6 gap-y-2 text-xs justify-center">
            <router-link to="/privacy" class="hover:text-white transition-colors">Privacitat</router-link>
            <router-link to="/terms"   class="hover:text-white transition-colors">Termes d'ús</router-link>
            <router-link to="/cookies" class="hover:text-white transition-colors">Cookies</router-link>
            <router-link to="/legal"   class="hover:text-white transition-colors">Avís legal</router-link>
          </nav>
          <p class="text-xs">© 2026 TravelAI · IA local i privada</p>
        </div>
      </div>
    </footer>

  </div>
</template>
