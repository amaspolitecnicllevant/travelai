<script setup>
import { ref, watch, computed } from 'vue'
import { searchApi } from '@/api/search'
import { usersApi } from '@/api/users'
import TripCard from '@/components/trip/TripCard.vue'

// ── State ──────────────────────────────────────────────────────────────────────
const query       = ref('')
const activeTab   = ref('all')
const loading     = ref(false)
const results     = ref({ trips: [], users: [] })
const followingIds = ref(new Set())

const tabs = [
  { id: 'all',   label: 'Tot' },
  { id: 'trips', label: 'Viatges' },
  { id: 'users', label: 'Usuaris' },
]

// ── Computed ───────────────────────────────────────────────────────────────────
const visibleTrips = computed(() =>
  activeTab.value === 'users' ? [] : results.value.trips
)
const visibleUsers = computed(() =>
  activeTab.value === 'trips' ? [] : results.value.users
)
const hasResults = computed(() =>
  visibleTrips.value.length > 0 || visibleUsers.value.length > 0
)
const hasSearched = computed(() => query.value.trim().length > 0)

// ── Debounce ───────────────────────────────────────────────────────────────────
let debounceTimer = null

watch(query, (val) => {
  clearTimeout(debounceTimer)
  if (!val.trim()) { results.value = { trips: [], users: [] }; return }
  debounceTimer = setTimeout(() => doSearch(val.trim()), 300)
})

watch(activeTab, () => {
  if (query.value.trim()) doSearch(query.value.trim())
})

// ── Search ─────────────────────────────────────────────────────────────────────
async function doSearch(q) {
  loading.value = true
  try {
    const type = activeTab.value === 'all' ? undefined : activeTab.value
    const { data } = await searchApi.search(q, type)
    results.value = {
      trips: data.trips ?? [],
      users: data.users ?? [],
    }
  } catch {
    results.value = { trips: [], users: [] }
  } finally {
    loading.value = false
  }
}

// ── Follow / Unfollow ──────────────────────────────────────────────────────────
async function toggleFollow(user) {
  try {
    if (followingIds.value.has(user.username)) {
      await usersApi.unfollow(user.username)
      followingIds.value.delete(user.username)
    } else {
      await usersApi.follow(user.username)
      followingIds.value.add(user.username)
    }
    // trigger reactivity
    followingIds.value = new Set(followingIds.value)
  } catch {
    // silently ignore
  }
}

// ── Avatar initials ────────────────────────────────────────────────────────────
function initials(user) {
  const name = user.name || user.username || ''
  return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Search header -->
    <div class="bg-white border-b border-gray-200 sticky top-16 z-30">
      <div class="max-w-4xl mx-auto px-4 py-4">
        <!-- Input -->
        <div class="relative">
          <svg class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400"
               fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
          </svg>
          <input
            v-model="query"
            type="search"
            placeholder="Cerca viatges, usuaris..."
            class="input w-full pl-10 pr-4"
            autofocus
          />
          <div v-if="loading" class="absolute right-3 top-1/2 -translate-y-1/2">
            <svg class="animate-spin h-4 w-4 text-indigo-500" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
          </div>
        </div>

        <!-- Tabs -->
        <div class="flex gap-1 mt-3">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            class="px-4 py-2 text-sm font-medium rounded-lg transition-colors"
            :class="activeTab === tab.id
              ? 'bg-indigo-100 text-indigo-700'
              : 'text-gray-500 hover:text-gray-700 hover:bg-gray-100'"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- Results -->
    <div class="max-w-4xl mx-auto px-4 py-6">

      <!-- Empty state: not searched yet -->
      <div v-if="!hasSearched" class="text-center py-20 text-gray-400">
        <svg class="h-12 w-12 mx-auto mb-4 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
        </svg>
        <p class="text-base">Escriu per cercar viatges i usuaris</p>
      </div>

      <!-- No results -->
      <div v-else-if="!loading && hasSearched && !hasResults"
           class="text-center py-20 text-gray-500">
        <p class="text-lg font-medium text-gray-700">No s'han trobat resultats per "<span class="text-indigo-600">{{ query }}</span>"</p>
        <p class="text-sm mt-1">Prova amb una altra paraula clau</p>
      </div>

      <template v-else>
        <!-- Skeleton loading -->
        <template v-if="loading">
          <!-- Trip skeletons -->
          <div v-if="activeTab !== 'users'" class="mb-8">
            <div class="h-5 bg-gray-200 rounded w-24 mb-4 animate-pulse"></div>
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="i in 3" :key="i" class="bg-white rounded-xl border border-gray-100 overflow-hidden animate-pulse">
                <div class="h-44 bg-gray-200"></div>
                <div class="p-4 space-y-2">
                  <div class="h-4 bg-gray-200 rounded w-3/4"></div>
                  <div class="h-3 bg-gray-200 rounded w-1/2"></div>
                </div>
              </div>
            </div>
          </div>
          <!-- User skeletons -->
          <div v-if="activeTab !== 'trips'">
            <div class="h-5 bg-gray-200 rounded w-24 mb-4 animate-pulse"></div>
            <div class="space-y-3">
              <div v-for="i in 4" :key="i" class="bg-white rounded-xl border border-gray-100 p-4 flex items-center gap-3 animate-pulse">
                <div class="h-10 w-10 rounded-full bg-gray-200 flex-shrink-0"></div>
                <div class="flex-1 space-y-1.5">
                  <div class="h-4 bg-gray-200 rounded w-1/3"></div>
                  <div class="h-3 bg-gray-200 rounded w-1/4"></div>
                </div>
                <div class="h-8 w-20 bg-gray-200 rounded-lg"></div>
              </div>
            </div>
          </div>
        </template>

        <!-- Actual results -->
        <template v-else>
          <!-- Trips -->
          <div v-if="visibleTrips.length > 0" class="mb-8">
            <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-4">
              Viatges ({{ visibleTrips.length }})
            </h2>
            <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <TripCard v-for="trip in visibleTrips" :key="trip.id" :trip="trip" />
            </div>
          </div>

          <!-- Users -->
          <div v-if="visibleUsers.length > 0">
            <h2 class="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-4">
              Usuaris ({{ visibleUsers.length }})
            </h2>
            <div class="space-y-3">
              <div
                v-for="user in visibleUsers"
                :key="user.username"
                class="bg-white rounded-xl border border-gray-100 p-4 flex items-center gap-4 hover:border-gray-200 transition-colors"
              >
                <!-- Avatar -->
                <router-link :to="`/profile/${user.username}`"
                             class="h-10 w-10 rounded-full bg-indigo-500 flex items-center justify-center flex-shrink-0 hover:opacity-90">
                  <img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="user.name"
                       class="h-10 w-10 rounded-full object-cover" />
                  <span v-else class="text-white text-sm font-semibold">{{ initials(user) }}</span>
                </router-link>

                <!-- Info -->
                <div class="flex-1 min-w-0">
                  <router-link :to="`/profile/${user.username}`"
                               class="font-medium text-gray-900 hover:text-indigo-600 transition-colors block truncate">
                    {{ user.name || user.username }}
                  </router-link>
                  <p class="text-sm text-gray-500 truncate">@{{ user.username }}</p>
                </div>

                <!-- Follow button -->
                <button
                  @click="toggleFollow(user)"
                  class="flex-shrink-0 px-4 py-1.5 text-sm font-medium rounded-lg transition-colors"
                  :class="followingIds.has(user.username)
                    ? 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    : 'bg-indigo-600 text-white hover:bg-indigo-700'"
                >
                  {{ followingIds.has(user.username) ? 'Seguit' : 'Seguir' }}
                </button>
              </div>
            </div>
          </div>
        </template>
      </template>
    </div>
  </div>
</template>
