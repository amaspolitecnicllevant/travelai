<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tripsApi } from '@/api/trips'
import { useToast } from '@/composables/useToast'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const route  = useRoute()
const router = useRouter()
const toast  = useToast()

const loading  = ref(true)
const saving   = ref(false)
const error    = ref(null)
const trip     = ref(null)

const form = ref({
  title:                '',
  destination:          '',
  tripTypes:            [],
  budget:               '',
  budgetLevel:          '',
  startDate:            '',
  endDate:              '',
  arrivalTime:          '',
  departureTime:        '',
  arrivalLocation:      '',
  accommodationAddress: '',
  preferredTransport:   '',
  visibility:           'PRIVATE',
  description:          '',
})

// ── Options ───────────────────────────────────────────────────────────────────
const transports = [
  { id: 'WALK',   label: 'A peu',            icon: '🚶' },
  { id: 'PUBLIC', label: 'Transport públic', icon: '🚌' },
  { id: 'CAR',    label: 'Cotxe',            icon: '🚗' },
  { id: 'TAXI',   label: 'Taxi',             icon: '🚕' },
]

const tripTypes = [
  { id: 'CULTURAL',   label: 'Cultural',    icon: '🏛️' },
  { id: 'ADVENTURE',  label: 'Aventura',    icon: '🏔️' },
  { id: 'RELAX',      label: 'Relax',       icon: '🏖️' },
  { id: 'GASTRONOMY', label: 'Gastronomia', icon: '🍜' },
  { id: 'NATURE',     label: 'Naturalesa',  icon: '🌿' },
  { id: 'CITY',       label: 'Ciutat',      icon: '🏙️' },
  { id: 'FAMILY',     label: 'Familiar',    icon: '👨‍👩‍👧‍👦' },
  { id: 'ROMANTIC',   label: 'Romàntic',    icon: '💑' },
]

const budgetLevels = [
  { id: 'BUDGET',  label: 'Econòmic', description: 'Hostels, menjar local, transport públic', color: 'green'  },
  { id: 'MEDIUM',  label: 'Mitjà',   description: 'Hotels 3★, restaurants, excursions',      color: 'blue'   },
  { id: 'HIGH',    label: 'Alt',     description: 'Hotels 4-5★, activitats premium',         color: 'purple' },
  { id: 'LUXURY',  label: 'Luxe',    description: 'Resorts exclusius, experiències VIP',     color: 'amber'  },
]

const budgetColorMap = {
  green:  { border: 'border-green-400',  bg: 'bg-green-50',  text: 'text-green-700'  },
  blue:   { border: 'border-blue-400',   bg: 'bg-blue-50',   text: 'text-blue-700'   },
  purple: { border: 'border-purple-400', bg: 'bg-purple-50', text: 'text-purple-700' },
  amber:  { border: 'border-amber-400',  bg: 'bg-amber-50',  text: 'text-amber-700'  },
}

// ── Helpers ───────────────────────────────────────────────────────────────────
const today = new Date().toISOString().split('T')[0]

const calculatedDays = computed(() => {
  if (!form.value.startDate || !form.value.endDate) return null
  const diff = new Date(form.value.endDate) - new Date(form.value.startDate)
  if (diff < 0) return null
  return Math.round(diff / (1000 * 60 * 60 * 24)) + 1
})

function toggleTripType(id) {
  const idx = form.value.tripTypes.indexOf(id)
  if (idx !== -1) {
    form.value.tripTypes.splice(idx, 1)
  } else if (form.value.tripTypes.length < 2) {
    form.value.tripTypes.push(id)
  }
}

// ── Load existing trip ────────────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { data } = await tripsApi.getById(route.params.id)
    trip.value = data
    form.value = {
      title:                data.title               || '',
      destination:          data.destination         || '',
      tripTypes:            Array.isArray(data.tripTypes) ? [...data.tripTypes] : [],
      budget:               data.budget              || '',
      budgetLevel:          data.budgetLevel         || '',
      startDate:            data.startDate           || '',
      endDate:              data.endDate             || '',
      arrivalTime:          data.arrivalTime         || '',
      departureTime:        data.departureTime       || '',
      arrivalLocation:      data.arrivalLocation     || '',
      accommodationAddress: data.accommodationAddress || '',
      preferredTransport:   data.preferredTransport  || '',
      visibility:           data.visibility          || 'PRIVATE',
      description:          data.description         || '',
    }
  } catch (e) {
    error.value = e.response?.data?.message || 'Error carregant el viatge.'
  } finally {
    loading.value = false
  }
})

// ── Submit ────────────────────────────────────────────────────────────────────
async function submit() {
  saving.value = true
  try {
    const payload = {
      title:                form.value.title                || null,
      destination:          form.value.destination          || null,
      description:          form.value.description          || null,
      startDate:            form.value.startDate            || null,
      endDate:              form.value.endDate              || null,
      arrivalTime:          form.value.arrivalTime          || null,
      departureTime:        form.value.departureTime        || null,
      arrivalLocation:      form.value.arrivalLocation      || null,
      accommodationAddress: form.value.accommodationAddress || null,
      preferredTransport:   form.value.preferredTransport   || null,
      tripTypes:            form.value.tripTypes.length ? form.value.tripTypes : null,
      budget:               form.value.budget ? String(form.value.budget) : null,
      budgetLevel:          form.value.budgetLevel          || null,
      visibility:           form.value.visibility,
    }
    await tripsApi.update(route.params.id, payload)
    toast.success('Canvis desats correctament')
    router.push({ name: 'trip-planner', params: { id: route.params.id } })
  } catch (e) {
    toast.error(e.response?.data?.message || 'Error desant els canvis.')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-10">

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-24">
      <LoadingSpinner size="xl" />
    </div>

    <!-- Error -->
    <div v-else-if="error" class="text-center py-24">
      <p class="text-red-500 font-medium mb-4">{{ error }}</p>
      <button @click="router.back()" class="text-indigo-600 hover:underline text-sm">Tornar</button>
    </div>

    <!-- Form -->
    <template v-else>
      <!-- Header -->
      <div class="mb-8 flex items-center justify-between">
        <div>
          <button @click="router.push({ name: 'trip-planner', params: { id: route.params.id } })"
            class="text-sm text-gray-400 hover:text-gray-600 inline-flex items-center gap-1 mb-2">
            ← Tornar al planner
          </button>
          <h1 class="text-2xl font-bold text-gray-900">Editar viatge</h1>
          <p class="text-gray-500 text-sm mt-1">{{ trip?.title }}</p>
        </div>
      </div>

      <form @submit.prevent="submit" class="space-y-8">

        <!-- ── Informació bàsica ─────────────────────────────────────────── -->
        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <h2 class="text-base font-semibold text-gray-800 mb-5">Informació bàsica</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">

            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">Títol del viatge *</label>
              <input v-model="form.title" type="text" required placeholder="Ex: Ruta pel Japó"
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Destinació *</label>
              <input v-model="form.destination" type="text" required placeholder="Ex: Tòquio, Japó"
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Pressupost total (€)</label>
              <input v-model="form.budget" type="number" min="0" placeholder="Opcional"
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Data d'inici</label>
              <input v-model="form.startDate" type="date"
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Data de fi</label>
              <input v-model="form.endDate" type="date" :min="form.startDate || today"
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                Hora d'arribada al destí
                <span class="text-gray-400 font-normal">(primer dia)</span>
              </label>
              <input v-model="form.arrivalTime" type="time"
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
              <p class="text-xs text-gray-400 mt-1">El primer dia s'organitzarà a partir d'aquesta hora</p>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                Hora de sortida del destí
                <span class="text-gray-400 font-normal">(últim dia)</span>
              </label>
              <input v-model="form.departureTime" type="time"
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
              <p class="text-xs text-gray-400 mt-1">L'últim dia s'organitzarà fins a aquesta hora</p>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Punt d'arribada</label>
              <div class="relative">
                <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-base">✈️</span>
                <input v-model="form.arrivalLocation" type="text"
                  placeholder="Ex: Aeroport El Prat T1, Estació Sants..."
                  class="w-full border border-gray-300 rounded-lg pl-9 pr-4 py-2.5 text-sm
                         focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Adreça de l'allotjament</label>
              <div class="relative">
                <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-base">🏨</span>
                <input v-model="form.accommodationAddress" type="text"
                  placeholder="Ex: Hotel Arts, Carrer de la Marina 19-21, Barcelona"
                  class="w-full border border-gray-300 rounded-lg pl-9 pr-4 py-2.5 text-sm
                         focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
              </div>
            </div>

            <div v-if="calculatedDays !== null" class="md:col-span-2">
              <div class="flex items-center gap-2 bg-indigo-50 text-indigo-700 rounded-lg px-4 py-2.5 text-sm font-medium">
                <span>📅</span>
                <span>{{ calculatedDays }} {{ calculatedDays === 1 ? 'dia' : 'dies' }} de viatge</span>
              </div>
            </div>

            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">Descripció / Preferències</label>
              <textarea v-model="form.description" rows="3"
                placeholder="Ex: M'agrada la cultura local, la gastronomia i els parcs naturals..."
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm resize-none
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
            </div>
          </div>
        </div>

        <!-- ── Mitjà de transport preferit ─────────────────────────────── -->
        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <h2 class="text-base font-semibold text-gray-800 mb-1">Mitjà de transport preferit</h2>
          <p class="text-xs text-gray-400 mb-4">Per desplaçar-se entre els llocs del viatge</p>
          <div class="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <button
              v-for="t in transports"
              :key="t.id"
              type="button"
              @click="form.preferredTransport = form.preferredTransport === t.id ? '' : t.id"
              class="flex flex-col items-center gap-2 rounded-xl py-4 px-2 border-2 transition-all"
              :class="form.preferredTransport === t.id
                ? 'border-indigo-500 bg-indigo-50'
                : 'border-gray-100 bg-gray-50 hover:border-gray-300'"
            >
              <span class="text-2xl">{{ t.icon }}</span>
              <span class="text-xs font-medium text-center"
                    :class="form.preferredTransport === t.id ? 'text-indigo-700' : 'text-gray-600'">
                {{ t.label }}
              </span>
            </button>
          </div>
        </div>

        <!-- ── Tipus de viatge ───────────────────────────────────────────── -->
        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-base font-semibold text-gray-800">Tipus de viatge</h2>
            <span class="text-xs text-gray-400">Selecciona fins a 2</span>
          </div>
          <div class="grid grid-cols-4 sm:grid-cols-8 gap-3">
            <button
              v-for="type in tripTypes"
              :key="type.id"
              type="button"
              :disabled="!form.tripTypes.includes(type.id) && form.tripTypes.length >= 2"
              @click="toggleTripType(type.id)"
              class="flex flex-col items-center gap-2 rounded-xl py-4 px-2 border-2 transition-all"
              :class="[
                form.tripTypes.includes(type.id)
                  ? 'border-indigo-500 bg-indigo-50'
                  : (!form.tripTypes.includes(type.id) && form.tripTypes.length >= 2)
                    ? 'border-gray-100 bg-gray-50 opacity-40 cursor-not-allowed'
                    : 'border-gray-100 bg-gray-50 hover:border-gray-300'
              ]"
            >
              <span class="text-2xl">{{ type.icon }}</span>
              <span class="text-xs font-medium text-center leading-tight"
                    :class="form.tripTypes.includes(type.id) ? 'text-indigo-700' : 'text-gray-600'">
                {{ type.label }}
              </span>
            </button>
          </div>
        </div>

        <!-- ── Nivell de pressupost ──────────────────────────────────────── -->
        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <h2 class="text-base font-semibold text-gray-800 mb-5">Nivell de pressupost</h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <button
              v-for="level in budgetLevels"
              :key="level.id"
              type="button"
              @click="form.budgetLevel = form.budgetLevel === level.id ? '' : level.id"
              class="flex items-start gap-3 rounded-xl p-4 border-2 text-left transition-all"
              :class="form.budgetLevel === level.id
                ? `border-2 ${budgetColorMap[level.color].border} ${budgetColorMap[level.color].bg}`
                : 'border-gray-100 bg-gray-50 hover:border-gray-300'"
            >
              <div class="flex-1 min-w-0">
                <p class="font-semibold text-sm"
                   :class="form.budgetLevel === level.id ? budgetColorMap[level.color].text : 'text-gray-800'">
                  {{ level.label }}
                </p>
                <p class="text-xs text-gray-500 mt-0.5">{{ level.description }}</p>
              </div>
            </button>
          </div>
        </div>

        <!-- ── Visibilitat ──────────────────────────────────────────────── -->
        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
          <h2 class="text-base font-semibold text-gray-800 mb-2">Visibilitat</h2>
          <p class="text-xs text-gray-400 mb-4">El viatge és privat per defecte (Privacitat per disseny — RGPD)</p>
          <div class="flex gap-4">
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
        </div>

        <!-- ── Botons ───────────────────────────────────────────────────── -->
        <div class="flex gap-3">
          <button type="submit" :disabled="saving"
            class="flex-1 bg-indigo-600 text-white py-3.5 rounded-xl font-semibold text-sm
                   hover:bg-indigo-700 disabled:opacity-60 disabled:cursor-not-allowed transition-colors
                   flex items-center justify-center gap-2">
            <LoadingSpinner v-if="saving" size="sm" color="white" />
            <span>{{ saving ? 'Desant...' : 'Desar canvis' }}</span>
          </button>
          <button type="button"
            @click="router.push({ name: 'trip-planner', params: { id: route.params.id } })"
            class="px-6 py-3.5 rounded-xl font-semibold text-sm border border-gray-300
                   text-gray-700 hover:bg-gray-50 transition-colors">
            Cancel·lar
          </button>
        </div>

      </form>
    </template>
  </div>
</template>
