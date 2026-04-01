<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import TripCard from '@/components/trip/TripCard.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const route  = useRoute()
const router = useRouter()
const auth   = useAuthStore()

const username = computed(() => route.params.username)

// ── Redirect si és el propi usuari ───────────────────────────────────────────
watch(
  () => auth.user,
  (u) => {
    if (u && u.username === username.value) router.replace({ name: 'my-profile' })
  },
  { immediate: true }
)

// ── Data ─────────────────────────────────────────────────────────────────────
const profile      = ref(null)
const trips        = ref([])
const stats        = ref(null)
const loading      = ref(false)
const error        = ref(null)
const following    = ref(false)  // placeholder — Social phase 2
const followLoading = ref(false)

const initials = computed(() => {
  const name = profile.value?.name || profile.value?.username || ''
  return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
})

const formattedDate = computed(() => {
  const d = profile.value?.createdAt
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' })
})

const avgRating = computed(() => {
  if (!stats.value?.averageRating) return null
  return Number(stats.value.averageRating).toFixed(1)
})

async function fetchProfile() {
  loading.value = true
  error.value   = null
  try {
    const [profileRes, tripsRes] = await Promise.all([
      usersApi.getByUsername(username.value),
      usersApi.getTrips(username.value),
    ])
    profile.value = profileRes.data

    // Try stats (non-critical)
    try {
      const statsRes = await usersApi.getStats(username.value)
      stats.value = statsRes.data
    } catch { /* stats optional */ }

    const raw = tripsRes.data
    trips.value = Array.isArray(raw) ? raw : (raw.content ?? [])
  } catch (e) {
    if (e.response?.status === 404) {
      error.value = `L'usuari "@${username.value}" no existeix.`
    } else {
      error.value = 'Error carregant el perfil. Torna-ho a intentar.'
    }
  } finally {
    loading.value = false
  }
}

// ── Follow / unfollow (placeholder visual — fase 2) ──────────────────────────
async function toggleFollow() {
  if (!auth.isLoggedIn) { router.push({ name: 'login' }); return }
  followLoading.value = true
  try {
    if (following.value) {
      await usersApi.unfollow(username.value)
      following.value = false
    } else {
      await usersApi.follow(username.value)
      following.value = true
    }
  } catch { /* ignore — social phase 2 */ }
  finally { followLoading.value = false }
}

// ── Init ──────────────────────────────────────────────────────────────────────
onMounted(fetchProfile)
watch(username, fetchProfile)
</script>

<template>
  <div class="min-h-screen bg-gray-50">

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-24">
      <LoadingSpinner size="lg" />
    </div>

    <!-- Error -->
    <div v-else-if="error" class="max-w-2xl mx-auto px-4 py-20 text-center">
      <div class="text-5xl mb-4">😕</div>
      <p class="text-gray-700 font-medium">{{ error }}</p>
      <router-link to="/explore" class="mt-4 inline-block text-indigo-600 hover:underline text-sm">
        Tornar a Explorar
      </router-link>
    </div>

    <!-- Profile -->
    <template v-else-if="profile">
      <!-- Header -->
      <div class="bg-white border-b border-gray-200">
        <div class="max-w-5xl mx-auto px-4 py-10">
          <div class="flex flex-col sm:flex-row items-start sm:items-center gap-6">

            <!-- Avatar -->
            <div class="h-20 w-20 rounded-full bg-indigo-600 flex items-center justify-center flex-shrink-0">
              <span class="text-2xl font-bold text-white">{{ initials }}</span>
            </div>

            <!-- Info -->
            <div class="flex-1 min-w-0">
              <div class="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4">
                <h1 class="text-2xl font-bold text-gray-900 truncate">
                  {{ profile.name || profile.username }}
                </h1>
                <span class="text-sm text-gray-500">@{{ profile.username }}</span>
              </div>

              <p v-if="profile.bio" class="text-sm text-gray-600 mt-2 max-w-md">{{ profile.bio }}</p>
              <p v-if="formattedDate" class="text-xs text-gray-400 mt-2">Membre des de {{ formattedDate }}</p>

              <!-- Stats -->
              <div class="flex items-center gap-6 mt-3">
                <div class="text-center">
                  <span class="text-lg font-semibold text-gray-900">{{ trips.length }}</span>
                  <span class="text-xs text-gray-500 ml-1">viatges</span>
                </div>
                <div v-if="avgRating" class="text-center">
                  <span class="text-lg font-semibold text-gray-900">{{ avgRating }}</span>
                  <span class="text-xs text-gray-500 ml-1">⭐ valoració mitja</span>
                </div>
                <div v-if="stats?.followersCount != null" class="text-center">
                  <span class="text-lg font-semibold text-gray-900">{{ stats.followersCount }}</span>
                  <span class="text-xs text-gray-500 ml-1">seguidors</span>
                </div>
              </div>
            </div>

            <!-- Follow button -->
            <div v-if="auth.isLoggedIn" class="flex-shrink-0">
              <button
                @click="toggleFollow"
                :disabled="followLoading"
                class="px-5 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
                :class="following
                  ? 'border border-gray-300 text-gray-700 hover:bg-gray-50'
                  : 'bg-indigo-600 text-white hover:bg-indigo-700'"
              >
                {{ followLoading ? '...' : following ? 'Deixar de seguir' : 'Seguir' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Trips grid -->
      <div class="max-w-5xl mx-auto px-4 py-8">
        <h2 class="text-lg font-semibold text-gray-900 mb-4">Viatges de @{{ profile.username }}</h2>

        <div v-if="trips.length === 0" class="text-center py-16 text-gray-500">
          <div class="text-4xl mb-3">✈</div>
          <p>Aquest usuari encara no té viatges públics.</p>
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <TripCard v-for="trip in trips" :key="trip.id" :trip="trip" />
        </div>
      </div>
    </template>
  </div>
</template>
