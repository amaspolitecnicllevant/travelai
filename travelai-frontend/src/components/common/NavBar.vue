<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useAuth } from '@/composables/useAuth'

const auth     = useAuthStore()
const { logout } = useAuth()
const menuOpen = ref(false)
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
