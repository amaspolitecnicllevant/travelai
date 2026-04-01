<script setup>
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const { login, store } = useAuth()
const form = ref({ email: '', password: '' })
async function submit() { await login(form.value.email, form.value.password) }
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-50 to-blue-100 flex items-center justify-center px-4">
    <div class="bg-white rounded-2xl shadow-lg w-full max-w-md p-8">
      <div class="text-center mb-8">
        <div class="text-4xl mb-3">✈</div>
        <h1 class="text-2xl font-bold text-gray-900">Benvingut/da!</h1>
        <p class="text-gray-500 mt-1 text-sm">Inicia sessió al teu compte TravelAI</p>
      </div>

      <form @submit.prevent="submit" class="space-y-5">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Correu electrònic</label>
          <input v-model="form.email" type="email" required placeholder="tu@exemple.com"
            class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                   focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Contrasenya</label>
          <input v-model="form.password" type="password" required placeholder="••••••••"
            class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                   focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
        </div>

        <p v-if="store.error" class="text-sm text-red-500 bg-red-50 rounded-lg px-3 py-2">{{ store.error }}</p>

        <button type="submit" :disabled="store.loading"
          class="w-full bg-indigo-600 text-white py-2.5 rounded-lg font-medium text-sm
                 hover:bg-indigo-700 disabled:opacity-60 disabled:cursor-not-allowed transition-colors
                 flex items-center justify-center gap-2">
          <LoadingSpinner v-if="store.loading" size="sm" color="white" />
          <span>{{ store.loading ? 'Entrant...' : 'Iniciar sessió' }}</span>
        </button>
      </form>

      <p class="text-center text-sm text-gray-500 mt-6">
        No tens compte?
        <router-link to="/register" class="text-indigo-600 font-medium hover:underline">Registra't</router-link>
      </p>
    </div>
  </div>
</template>
