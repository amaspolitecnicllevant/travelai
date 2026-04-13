<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTrips } from '@/composables/useTrips'
import { useDestinationAutocomplete } from '@/composables/useDestinationAutocomplete'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const router = useRouter()
const { createTrip, store } = useTrips()
const { suggestions, loading: destLoading, search: searchDest, clear: clearSuggestions } = useDestinationAutocomplete()

const showSuggestions = ref(false)

function onDestinationInput(e) {
  searchDest(e.target.value)
  showSuggestions.value = true
}

function selectSuggestion(s) {
  form.value.destination = s.label
  showSuggestions.value = false
  clearSuggestions()
}

function onDestinationBlur() {
  // Delay to allow click on suggestion to fire first
  setTimeout(() => { showSuggestions.value = false }, 150)
}

// ── Form state ───────────────────────────────────────────────────────────────
const form = ref({
  title:              '',
  destination:        '',
  tripTypes:          [],
  budget:             '',
  budgetLevel:        '',
  startDate:          '',
  endDate:            '',
  arrivalTime:          '',
  departureTime:        '',
  arrivalLocation:      '',
  accommodationAddress: '',
  preferredTransport:   '',
  visibility:         'PRIVATE',
  description:        '',
})

function toggleTripType(id) {
  const idx = form.value.tripTypes.indexOf(id)
  if (idx !== -1) {
    form.value.tripTypes.splice(idx, 1)
  } else if (form.value.tripTypes.length < 2) {
    form.value.tripTypes.push(id)
  }
}

const transports = [
  { id: 'WALK',   label: 'A peu',            icon: '🚶' },
  { id: 'PUBLIC', label: 'Transport públic', icon: '🚌' },
  { id: 'CAR',    label: 'Cotxe',            icon: '🚗' },
  { id: 'TAXI',   label: 'Taxi',             icon: '🚕' },
]

// ── Trip types ────────────────────────────────────────────────────────────────
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

// ── Budget levels ─────────────────────────────────────────────────────────────
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

// ── Date computed ─────────────────────────────────────────────────────────────
const today = new Date().toISOString().split('T')[0]

const calculatedDays = computed(() => {
  if (!form.value.startDate || !form.value.endDate) return null
  const diff = new Date(form.value.endDate) - new Date(form.value.startDate)
  if (diff < 0) return null
  return Math.round(diff / (1000 * 60 * 60 * 24)) + 1
})

// ── Submit ────────────────────────────────────────────────────────────────────
async function submit() {
  const payload = {
    title:       form.value.title,
    destination: form.value.destination,
    days:               calculatedDays.value ?? 3,
    budget:             form.value.budget ? String(form.value.budget) : null,
    budgetLevel:        form.value.budgetLevel        || null,
    tripTypes:          form.value.tripTypes.length ? form.value.tripTypes : null,
    startDate:          form.value.startDate          || null,
    endDate:            form.value.endDate            || null,
    arrivalTime:          form.value.arrivalTime          || null,
    departureTime:        form.value.departureTime        || null,
    arrivalLocation:      form.value.arrivalLocation      || null,
    accommodationAddress: form.value.accommodationAddress || null,
    preferredTransport:   form.value.preferredTransport   || null,
    visibility:         form.value.visibility,
    description:        form.value.description,
  }
  const trip = await createTrip(payload)
  if (trip) router.push({ name: 'trip-planner', params: { id: trip.id } })
}
</script>

<template>
  <div class="max-w-4xl mx-auto px-4 py-10">
    <!-- Header -->
    <div class="mb-8">
      <router-link to="/feed" class="text-sm text-gray-400 hover:text-gray-600 inline-flex items-center gap-1">
        ← Tornar al feed
      </router-link>
      <h1 class="text-2xl font-bold text-gray-900 mt-2">Nou viatge</h1>
      <p class="text-gray-500 text-sm mt-1">Omple les dades i genera l'itinerari amb IA</p>
    </div>

    <form @submit.prevent="submit" class="space-y-8">

      <!-- ── Informació bàsica ─────────────────────────────────────────── -->
      <div class="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
        <h2 class="text-base font-semibold text-gray-800 mb-5">Informació bàsica</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
          <!-- Title -->
          <div class="md:col-span-2">
            <label class="block text-sm font-medium text-gray-700 mb-1">Títol del viatge *</label>
            <input v-model="form.title" type="text" required placeholder="Ex: Ruta pel Japó"
              class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                     focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
          </div>

          <!-- Destination with autocomplete -->
          <div class="relative">
            <label class="block text-sm font-medium text-gray-700 mb-1">Destinació *</label>
            <div class="relative">
              <input
                v-model="form.destination"
                type="text"
                required
                autocomplete="off"
                placeholder="Ex: Tòquio, Roma, Barcelona..."
                class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"
                @input="onDestinationInput"
                @blur="onDestinationBlur"
                @focus="showSuggestions = suggestions.length > 0"
              />
              <span v-if="destLoading"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs animate-pulse">
                Cercant...
              </span>
            </div>

            <!-- Suggestions dropdown -->
            <ul v-if="showSuggestions && suggestions.length"
              class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg
                     max-h-56 overflow-y-auto">
              <li
                v-for="s in suggestions"
                :key="s.label"
                @mousedown.prevent="selectSuggestion(s)"
                class="px-4 py-2.5 cursor-pointer hover:bg-indigo-50 flex items-start gap-2 text-sm"
              >
                <span class="text-gray-400 mt-0.5 flex-shrink-0">📍</span>
                <span class="text-gray-800">{{ s.label }}</span>
              </li>
            </ul>
          </div>

          <!-- Budget amount -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Pressupost total (€)</label>
            <input v-model="form.budget" type="number" min="0" placeholder="Opcional"
              class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                     focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
          </div>

          <!-- Start date -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Data d'inici</label>
            <input v-model="form.startDate" type="date" :min="today"
              class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                     focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
          </div>

          <!-- End date -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Data de fi</label>
            <input v-model="form.endDate" type="date" :min="form.startDate || today"
              class="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm
                     focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
          </div>

          <!-- Hora d'arribada i sortida -->
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

          <!-- Punt d'arribada -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Punt d'arribada</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-base">✈️</span>
              <input v-model="form.arrivalLocation" type="text"
                placeholder="Ex: Aeroport El Prat T1, Estació Sants, Port de Barcelona..."
                class="w-full border border-gray-300 rounded-lg pl-9 pr-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
            </div>
            <p class="text-xs text-gray-400 mt-1">Aeroport, port, estació de tren o autobús</p>
          </div>

          <!-- Allotjament -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Adreça de l'allotjament</label>
            <div class="relative">
              <span class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-base">🏨</span>
              <input v-model="form.accommodationAddress" type="text"
                placeholder="Ex: Hotel Arts, Carrer de la Marina 19-21, Barcelona"
                class="w-full border border-gray-300 rounded-lg pl-9 pr-4 py-2.5 text-sm
                       focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent"/>
            </div>
            <p class="text-xs text-gray-400 mt-1">L'IA planificarà primer anar a deixar les maletes</p>
          </div>

          <!-- Days preview -->
          <div v-if="calculatedDays !== null" class="md:col-span-2">
            <div class="flex items-center gap-2 bg-indigo-50 text-indigo-700 rounded-lg px-4 py-2.5 text-sm font-medium">
              <span>📅</span>
              <span>{{ calculatedDays }} {{ calculatedDays === 1 ? 'dia' : 'dies' }} de viatge</span>
            </div>
          </div>

          <!-- Description -->
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
        <p v-if="form.tripTypes.length === 2" class="text-xs text-indigo-600 mt-3">
          ✓ {{ form.tripTypes.map(id => tripTypes.find(t => t.id === id)?.label).join(' + ') }}
        </p>
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
            <div v-if="form.budgetLevel === level.id"
                 class="flex-shrink-0 w-4 h-4 rounded-full mt-0.5"
                 :class="budgetColorMap[level.color].border.replace('border-', 'bg-')">
            </div>
          </button>
        </div>
      </div>

      <!-- ── Visibilitat (GDPR) ────────────────────────────────────────── -->
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

      <!-- ── Submit ────────────────────────────────────────────────────── -->
      <button type="submit" :disabled="store.loading"
        class="w-full bg-indigo-600 text-white py-3.5 rounded-xl font-semibold text-sm
               hover:bg-indigo-700 disabled:opacity-60 disabled:cursor-not-allowed transition-colors
               flex items-center justify-center gap-2">
        <LoadingSpinner v-if="store.loading" size="sm" color="white" />
        <span>{{ store.loading ? 'Creant viatge...' : 'Crear viatge i generar itinerari amb IA →' }}</span>
      </button>

    </form>
  </div>
</template>
