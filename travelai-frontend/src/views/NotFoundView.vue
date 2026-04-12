<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth   = useAuthStore()
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex items-center justify-center px-4">
    <div class="text-center max-w-md">
      <div class="text-8xl mb-6">🗺</div>
      <h1 class="text-6xl font-bold text-indigo-600 mb-2">404</h1>
      <h2 class="text-xl font-semibold text-gray-900 mb-3">Pàgina no trobada</h2>
      <p class="text-gray-500 mb-8">
        La pàgina que cerques no existeix o ha estat moguda.
      </p>
      <div class="flex flex-col sm:flex-row gap-3 justify-center">
        <button
          @click="router.back()"
          class="px-6 py-2.5 rounded-xl border border-gray-300 text-gray-700 text-sm font-medium hover:bg-gray-50 transition-colors"
        >
          ← Tornar enrere
        </button>
        <router-link
          :to="auth.isLoggedIn ? '/feed' : '/'"
          class="px-6 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition-colors"
        >
          Anar a l'inici
        </router-link>
      </div>
    </div>
  </div>
</template>
