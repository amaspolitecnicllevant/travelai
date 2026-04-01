<script setup>
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const { register, store } = useAuth()
const form = ref({
  name: '', username: '', email: '', password: '', birthDate: '',
  privacyAccepted: false, termsAccepted: false, ageConfirmed: false,
})

async function submit() {
  await register({
    name: form.value.name, username: form.value.username,
    email: form.value.email, password: form.value.password,
    birthDate: form.value.birthDate,
    consents: {
      privacy: { accepted: form.value.privacyAccepted, version: '1.0' },
      terms:   { accepted: form.value.termsAccepted,   version: '1.0' },
    },
  })
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-50 to-blue-100 flex items-center justify-center px-4 py-8">
    <div class="bg-white rounded-2xl shadow-lg w-full max-w-md p-8">
      <div class="text-center mb-8">
        <div class="text-4xl mb-3">✈</div>
        <h1 class="text-2xl font-bold text-gray-900">Crea el teu compte</h1>
        <p class="text-gray-500 mt-1 text-sm">Uneix-te a TravelAI avui</p>
      </div>

      <form @submit.prevent="submit" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Nom complet</label>
            <input v-model="form.name" type="text" required placeholder="Joan Garcia"
              class="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Nom d'usuari</label>
            <input v-model="form.username" type="text" required placeholder="joan_garcia"
              class="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Correu electrònic</label>
          <input v-model="form.email" type="email" required placeholder="tu@exemple.com"
            class="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Contrasenya</label>
          <input v-model="form.password" type="password" required placeholder="Mínim 8 caràcters"
            class="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Data de naixement</label>
          <input v-model="form.birthDate" type="date" required
            class="w-full border border-gray-300 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
        </div>

        <!-- GDPR -->
        <div class="space-y-3 pt-2 border-t border-gray-100">
          <label class="flex items-start gap-3 cursor-pointer">
            <input v-model="form.privacyAccepted" type="checkbox" required
              class="mt-0.5 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"/>
            <span class="text-xs text-gray-600">
              He llegit i accepto la
              <router-link to="/privacy" target="_blank" class="text-indigo-600 underline">Política de Privacitat</router-link>
              (v1.0) — RGPD Art. 13
            </span>
          </label>
          <label class="flex items-start gap-3 cursor-pointer">
            <input v-model="form.termsAccepted" type="checkbox" required
              class="mt-0.5 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"/>
            <span class="text-xs text-gray-600">
              Accepto els
              <router-link to="/terms" target="_blank" class="text-indigo-600 underline">Termes d'Ús</router-link>
              (v1.0)
            </span>
          </label>
          <label class="flex items-start gap-3 cursor-pointer">
            <input v-model="form.ageConfirmed" type="checkbox" required
              class="mt-0.5 h-4 w-4 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500"/>
            <span class="text-xs text-gray-600">Confirmo que tinc 14 anys o més (LOPD-GDD Art. 7)</span>
          </label>
        </div>

        <p v-if="store.error" class="text-sm text-red-500 bg-red-50 rounded-lg px-3 py-2">{{ store.error }}</p>

        <button type="submit"
          :disabled="store.loading || !form.privacyAccepted || !form.termsAccepted || !form.ageConfirmed"
          class="w-full bg-indigo-600 text-white py-2.5 rounded-lg font-medium text-sm
                 hover:bg-indigo-700 disabled:opacity-60 disabled:cursor-not-allowed transition-colors
                 flex items-center justify-center gap-2">
          <LoadingSpinner v-if="store.loading" size="sm" color="white" />
          <span>{{ store.loading ? 'Creant compte...' : 'Crear compte' }}</span>
        </button>
      </form>

      <p class="text-center text-sm text-gray-500 mt-6">
        Ja tens compte?
        <router-link to="/login" class="text-indigo-600 font-medium hover:underline">Inicia sessió</router-link>
      </p>
    </div>
  </div>
</template>
