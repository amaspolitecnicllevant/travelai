<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAuth } from '@/composables/useAuth'
import { useNotificationsStore } from '@/stores/notifications'

const auth          = useAuthStore()
const { logout }    = useAuth()
const notifications = useNotificationsStore()
const router        = useRouter()

const menuOpen      = ref(false)
const bellOpen      = ref(false)

function formatRelativeTime(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins  = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days  = Math.floor(diff / 86400000)
  if (mins < 1)   return 'Ara mateix'
  if (mins < 60)  return `Fa ${mins} min`
  if (hours < 24) return `Fa ${hours} h`
  return `Fa ${days} dia${days !== 1 ? 's' : ''}`
}

function notifIcon(type) {
  const icons = {
    FOLLOW: '👤',
    LIKE: '❤️',
    COMMENT: '💬',
    TRIP_UPDATE: '✈',
    SYSTEM: 'ℹ️',
  }
  return icons[type] || '🔔'
}

function closeBell(e) {
  if (!e.target.closest('[data-bell]')) bellOpen.value = false
}

onMounted(() => {
  document.addEventListener('click', closeBell)
  if (auth.isLoggedIn) notifications.fetchUnread()
})

onUnmounted(() => {
  document.removeEventListener('click', closeBell)
})
</script>

<template>
  <nav class="bg-white border-b border-gray-200 sticky top-0 z-40">
    <div class="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
      <!-- Logo -->
      <router-link to="/" class="flex items-center gap-2 font-bold text-xl text-indigo-600">
        ✈ TravelAI
      </router-link>

      <!-- Desktop nav -->
      <div class="hidden md:flex items-center gap-6">
        <!-- Search icon -->
        <button
          @click="router.push('/search')"
          class="text-gray-500 hover:text-indigo-600 transition-colors"
          title="Cercar"
        >
          <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
          </svg>
        </button>

        <router-link to="/explore" class="text-gray-600 hover:text-indigo-600 text-sm font-medium">
          Explorar
        </router-link>
        <template v-if="auth.isLoggedIn">
          <router-link to="/feed" class="text-gray-600 hover:text-indigo-600 text-sm font-medium">
            Feed
          </router-link>
          <router-link to="/trips/new" class="text-gray-600 hover:text-indigo-600 text-sm font-medium">
            Nou viatge
          </router-link>
          <router-link to="/profile" class="text-gray-600 hover:text-indigo-600 text-sm font-medium">
            Mi perfil
          </router-link>
          <router-link to="/my-data" class="text-gray-600 hover:text-indigo-600 text-sm font-medium">
            Mis datos
          </router-link>

          <!-- Bell notification -->
          <div class="relative" data-bell>
            <button
              @click.stop="bellOpen = !bellOpen"
              class="relative text-gray-500 hover:text-indigo-600 transition-colors p-1"
              title="Notificacions"
            >
              <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6 6 0 10-12 0v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
              </svg>
              <span
                v-if="notifications.unreadCount > 0"
                class="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full h-4 w-4 flex items-center justify-center leading-none"
              >
                {{ notifications.unreadCount > 9 ? '9+' : notifications.unreadCount }}
              </span>
            </button>

            <!-- Notifications dropdown -->
            <div
              v-if="bellOpen"
              data-bell
              class="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border border-gray-200 z-50 overflow-hidden"
            >
              <div class="px-4 py-3 border-b border-gray-100">
                <h3 class="text-sm font-semibold text-gray-900">Notificacions</h3>
              </div>

              <div class="max-h-80 overflow-y-auto">
                <div v-if="notifications.notifications.length === 0"
                     class="px-4 py-8 text-center text-sm text-gray-500">
                  No tens notificacions
                </div>
                <div
                  v-for="notif in notifications.notifications"
                  :key="notif.id"
                  class="px-4 py-3 hover:bg-gray-50 transition-colors border-b border-gray-50 last:border-0"
                  :class="{ 'bg-indigo-50/40': !notif.read }"
                >
                  <div class="flex items-start gap-3">
                    <span class="text-lg flex-shrink-0 mt-0.5">{{ notifIcon(notif.type) }}</span>
                    <div class="flex-1 min-w-0">
                      <p class="text-sm text-gray-800 leading-snug">{{ notif.message }}</p>
                      <p class="text-xs text-gray-400 mt-1">{{ formatRelativeTime(notif.createdAt) }}</p>
                    </div>
                    <span v-if="!notif.read" class="h-2 w-2 rounded-full bg-indigo-500 flex-shrink-0 mt-2"></span>
                  </div>
                </div>
              </div>

              <div v-if="notifications.notifications.length > 0"
                   class="px-4 py-3 border-t border-gray-100 bg-gray-50">
                <button
                  @click="notifications.markAllRead(); bellOpen = false"
                  class="text-xs text-indigo-600 hover:text-indigo-800 font-medium transition-colors"
                >
                  Marcar totes com a llegides
                </button>
              </div>
            </div>
          </div>

          <button @click="logout" class="text-sm text-red-500 hover:text-red-700 font-medium">
            Sortir
          </button>
        </template>
        <template v-else>
          <router-link to="/login" class="text-sm font-medium text-gray-600 hover:text-indigo-600">
            Entrar
          </router-link>
          <router-link to="/register"
            class="bg-indigo-600 text-white text-sm font-medium px-4 py-2 rounded-lg hover:bg-indigo-700">
            Registrar-se
          </router-link>
        </template>
      </div>

      <!-- Mobile burger -->
      <button class="md:hidden p-2 rounded-lg text-gray-500 hover:bg-gray-100"
              @click="menuOpen = !menuOpen">
        <svg class="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path v-if="!menuOpen" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 6h16M4 12h16M4 18h16"/>
          <path v-else stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>

    <!-- Mobile menu -->
    <div v-if="menuOpen" class="md:hidden border-t border-gray-200 bg-white px-4 py-3 flex flex-col gap-3">
      <button @click="router.push('/search'); menuOpen=false"
              class="text-left text-gray-700 text-sm flex items-center gap-2">
        <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
        </svg>
        Cercar
      </button>
      <router-link to="/explore" @click="menuOpen=false" class="text-gray-700 text-sm">Explorar</router-link>
      <template v-if="auth.isLoggedIn">
        <router-link to="/feed"      @click="menuOpen=false" class="text-gray-700 text-sm">Feed</router-link>
        <router-link to="/trips/new" @click="menuOpen=false" class="text-gray-700 text-sm">Nou viatge</router-link>
        <router-link to="/profile"   @click="menuOpen=false" class="text-gray-700 text-sm">Mi perfil</router-link>
        <router-link to="/my-data"  @click="menuOpen=false" class="text-gray-700 text-sm">Mis datos</router-link>
        <button @click="logout(); menuOpen=false" class="text-left text-red-500 text-sm">Sortir</button>
      </template>
      <template v-else>
        <router-link to="/login"    @click="menuOpen=false" class="text-gray-700 text-sm">Entrar</router-link>
        <router-link to="/register" @click="menuOpen=false" class="text-indigo-600 font-medium text-sm">Registrar-se</router-link>
      </template>
    </div>
  </nav>
</template>
