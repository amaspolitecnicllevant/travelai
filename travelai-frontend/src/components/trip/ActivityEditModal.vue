<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  activity:  { type: Object, default: null },
  dayNumber: { type: Number, required: true },
  show:      { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'save'])

const form = ref({})

watch(() => props.activity, (act) => {
  if (act) form.value = { ...act }
}, { immediate: true })

const categories = [
  { value: 'CULTURE',   label: '🏛 Cultura' },
  { value: 'FOOD',      label: '🍽 Gastronomia' },
  { value: 'LEISURE',   label: '🌿 Lleure' },
  { value: 'TRANSPORT', label: '🚌 Transport' },
  { value: 'NATURE',    label: '🏞 Natura' },
  { value: 'SHOPPING',  label: '🛍 Compres' },
  { value: 'SPORT',     label: '⚽ Esport' },
]

const transports = [
  { value: 'WALK',   label: '🚶 A peu' },
  { value: 'PUBLIC', label: '🚌 Transport públic' },
  { value: 'CAR',    label: '🚗 Cotxe' },
  { value: 'TAXI',   label: '🚕 Taxi' },
]

function save() {
  emit('save', { ...form.value })
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <!-- Backdrop -->
      <div class="absolute inset-0 bg-black/50" @click="emit('close')" />

      <!-- Modal -->
      <div class="relative bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <!-- Header -->
        <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100">
          <h3 class="font-semibold text-gray-900">Editar activitat · Dia {{ dayNumber }}</h3>
          <button class="text-gray-400 hover:text-gray-600 transition-colors" @click="emit('close')">
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- Form -->
        <div class="px-6 py-4 space-y-4">
          <!-- Nom -->
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Nom de l'activitat *</label>
            <input v-model="form.name" type="text" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>

          <!-- Horari -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Hora d'arribada</label>
              <input v-model="form.time" type="time" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Hora de sortida</label>
              <input v-model="form.endTime" type="time" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
          </div>

          <!-- Ubicació -->
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Ubicació</label>
            <input v-model="form.location" type="text" placeholder="Ex: Museu del Prado, Madrid" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
          </div>

          <!-- Descripció -->
          <div>
            <label class="block text-xs font-medium text-gray-700 mb-1">Descripció</label>
            <textarea v-model="form.description" rows="2" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none" />
          </div>

          <!-- Transport + Temps -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Mitjà de transport</label>
              <select v-model="form.transportMode" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white">
                <option value="">— cap —</option>
                <option v-for="t in transports" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Temps de trajecte</label>
              <input v-model="form.travelTime" type="text" placeholder="Ex: 15 min" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
          </div>

          <!-- Categoria + Cost -->
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Categoria</label>
              <select v-model="form.type" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 bg-white">
                <option v-for="c in categories" :key="c.value" :value="c.value">{{ c.label }}</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-medium text-gray-700 mb-1">Cost estimat (€)</label>
              <input v-model="form.cost" type="number" min="0" step="0.5" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="flex items-center justify-end gap-3 px-6 py-4 border-t border-gray-100">
          <button class="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 transition-colors" @click="emit('close')">
            Cancel·lar
          </button>
          <button
            class="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-lg transition-colors"
            @click="save"
          >
            Desar canvis
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
