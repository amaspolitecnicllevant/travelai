<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTrips } from '@/composables/useTrips'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()
const { createTrip, store } = useTrips()

const form = ref({
  title: '',
  destination: '',
  days: 3,
  budget: '',
  visibility: 'PRIVATE',
  description: '',
})

async function submit() {
  const trip = await createTrip({
    ...form.value,
    days:   Number(form.value.days),
    budget: form.value.budget ? Number(form.value.budget) : null,
  })
  if (trip) router.push({ name: 'trip-planner', params: { id: trip.id } })
}
</script>

<template>
  <div class="max-w-2xl mx-auto px-4 py-10">
    <!-- Header -->
    <div class="mb-8">
      <router-link to="/feed" class="text-sm text-gray-400 hover:text-gray-600">← Tornar al feed</router-link>
      <h1 class="text-2xl font-bold text-gray-900 mt-2">Nou viatge</h1>
      <p class="text-gray-500 text-sm mt-1">Omple les dades bàsiques i genera l'itinerari amb IA</p>
    </div>

    <form @submit.prevent="submit" class="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 space-y-5">
      <!-- Title -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Títol del viatge *</label>
        <input v-model="form.title" type="text" required placeholder="Ex: Ruta pel Japó"
          class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
      </div>

      <!-- Destination -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Destinació *</label>
        <input v-model="form.destination" type="text" required placeholder="Ex: Tòquio, Japó"
          class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
      </div>

      <!-- Days + Budget -->
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Nombre de dies *</label>
          <input v-model="form.days" type="number" min="1" max="30" required
            class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Pressupost (€)</label>
          <input v-model="form.budget" type="number" min="0" placeholder="Opcional"
            class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
        </div>
      </div>

      <!-- Description -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">Descripció / Preferències</label>
        <textarea v-model="form.description" rows="3"
          placeholder="Ex: M'agrada la cultura local, la gastronomia i els parcs naturals..."
          class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
      </div>

      <!-- Visibility — PRIVATE by default (GDPR privacy by design) -->
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">Visibilitat</label>
        <div class="flex gap-3">
          <label class="flex items-center gap-2 cursor-pointer">
            <input v-model="form.visibility" type="radio" value="PRIVATE"
              class="text-indigo-600 focus:ring-indigo-500"/>
            <span class="text-sm text-gray-700">🔒 Privat (recomanat)</span>
          </label>
          <label class="flex items-center gap-2 cursor-pointer">
            <input v-model="form.visibility" type="radio" value="PUBLIC"
              class="text-indigo-600 focus:ring-indigo-500"/>
            <span class="text-sm text-gray-700">🌍 Públic</span>
          </label>
        </div>
        <p class="text-xs text-gray-400 mt-1">El viatge és privat per defecte (Privacitat per disseny — RGPD)</p>
      </div>

      <button type="submit" :disabled="store.loading"
        class="w-full bg-indigo-600 text-white py-3 rounded-xl font-semibold text-sm
               hover:bg-indigo-700 disabled:opacity-60 disabled:cursor-not-allowed transition-colors
               flex items-center justify-center gap-2">
        <LoadingSpinner v-if="store.loading" size="sm" color="white" />
        <span>{{ store.loading ? 'Creant...' : 'Crear viatge i generar itinerari →' }}</span>
      </button>
    </form>
  </div>
</template>
