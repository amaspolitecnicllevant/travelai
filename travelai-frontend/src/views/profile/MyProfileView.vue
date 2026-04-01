<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usersApi } from '@/api/users'
import { legalApi } from '@/api/legal'
import { tripsApi } from '@/api/trips'
import TripCard from '@/components/trip/TripCard.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const auth = useAuthStore()

// ── Tabs ──────────────────────────────────────────────────────────────────────
const activeTab = ref('trips')
const tabs = [
  { id: 'trips',    label: 'Mis viajes' },
  { id: 'settings', label: 'Ajustes' },
  { id: 'data',     label: 'Mis datos' },
]

// ── Avatar inicials ───────────────────────────────────────────────────────────
const initials = computed(() => {
  const name = auth.user?.name || auth.user?.username || ''
  return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()
})

const formattedDate = computed(() => {
  const d = auth.user?.createdAt
  if (!d) return ''
  return new Date(d).toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' })
})

// ── Trips ─────────────────────────────────────────────────────────────────────
const trips      = ref([])
const tripsLoading = ref(false)
const tripsError   = ref(null)

async function fetchTrips() {
  tripsLoading.value = true
  tripsError.value   = null
  try {
    const { data } = await tripsApi.getAll({ mine: true })
    trips.value = Array.isArray(data) ? data : (data.content ?? [])
  } catch {
    tripsError.value = 'Error carregant els viatges. Torna-ho a intentar.'
  } finally {
    tripsLoading.value = false
  }
}

// ── Settings form ─────────────────────────────────────────────────────────────
const settingsForm    = ref({ name: auth.user?.name || '', password: '', passwordConfirm: '' })
const settingsLoading = ref(false)
const settingsError   = ref(null)
const settingsSuccess = ref(false)

async function saveSettings() {
  settingsError.value   = null
  settingsSuccess.value = false

  if (settingsForm.value.password && settingsForm.value.password !== settingsForm.value.passwordConfirm) {
    settingsError.value = 'Les contrasenyes no coincideixen.'
    return
  }

  settingsLoading.value = true
  try {
    const payload = { name: settingsForm.value.name }
    if (settingsForm.value.password) payload.password = settingsForm.value.password
    await usersApi.updateMe(payload)
    await auth.fetchMe()
    settingsSuccess.value = true
    settingsForm.value.password        = ''
    settingsForm.value.passwordConfirm = ''
  } catch (e) {
    settingsError.value = e.response?.data?.message || 'Error desant els canvis.'
  } finally {
    settingsLoading.value = false
  }
}

// ── Data export & deletion ────────────────────────────────────────────────────
const dataLoading    = ref(false)
const dataError      = ref(null)
const deleteRequested = ref(false)
const showDeleteModal = ref(false)

async function exportData() {
  dataLoading.value = true
  dataError.value   = null
  try {
    const res  = await legalApi.exportMyData()
    const url  = URL.createObjectURL(res.data)
    const a    = document.createElement('a')
    a.href     = url
    a.download = 'mis-datos.zip'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    dataError.value = 'Error exportant les dades. Torna-ho a intentar.'
  } finally {
    dataLoading.value = false
  }
}

async function confirmDeleteRequest() {
  dataLoading.value = true
  dataError.value   = null
  try {
    await legalApi.requestDeletion('Sol·licitud de l\'usuari')
    deleteRequested.value = true
    showDeleteModal.value = false
  } catch {
    dataError.value = 'Error en la sol·licitud. Contacta amb privacidad@travelai.local'
  } finally {
    dataLoading.value = false
  }
}

// ── Init ──────────────────────────────────────────────────────────────────────
onMounted(fetchTrips)
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Profile header -->
    <div class="bg-white border-b border-gray-200">
      <div class="max-w-5xl mx-auto px-4 py-10 flex items-center gap-6">
        <!-- Avatar -->
        <div class="h-20 w-20 rounded-full bg-indigo-600 flex items-center justify-center flex-shrink-0">
          <span class="text-2xl font-bold text-white">{{ initials }}</span>
        </div>

        <div>
          <h1 class="text-2xl font-bold text-gray-900">{{ auth.user?.name || auth.user?.username }}</h1>
          <p class="text-sm text-gray-500 mt-0.5">{{ auth.user?.email }}</p>
          <p v-if="formattedDate" class="text-xs text-gray-400 mt-1">Membre des de {{ formattedDate }}</p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="max-w-5xl mx-auto px-4">
        <nav class="flex gap-1 -mb-px">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            class="px-4 py-3 text-sm font-medium border-b-2 transition-colors"
            :class="activeTab === tab.id
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'"
          >
            {{ tab.label }}
          </button>
        </nav>
      </div>
    </div>

    <!-- Tab content -->
    <div class="max-w-5xl mx-auto px-4 py-8">

      <!-- TAB: Mis viajes -->
      <div v-if="activeTab === 'trips'">
        <div v-if="tripsLoading" class="flex justify-center py-16">
          <LoadingSpinner size="lg" />
        </div>

        <div v-else-if="tripsError" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700 text-sm">
          {{ tripsError }}
          <button @click="fetchTrips" class="ml-2 underline">Reintentar</button>
        </div>

        <div v-else-if="trips.length === 0" class="text-center py-16 text-gray-500">
          <div class="text-5xl mb-4">✈</div>
          <p class="font-medium text-gray-700">Encara no tens viatges</p>
          <p class="text-sm mt-1">Crea el teu primer viatge i l'IA generarà un itinerari per a tu.</p>
          <router-link to="/trips/new"
            class="mt-4 inline-block bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-700">
            Nou viatge
          </router-link>
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          <TripCard v-for="trip in trips" :key="trip.id" :trip="trip" />
        </div>
      </div>

      <!-- TAB: Ajustes -->
      <div v-if="activeTab === 'settings'" class="max-w-lg">
        <h2 class="text-lg font-semibold text-gray-900 mb-6">Ajustes del perfil</h2>

        <div v-if="settingsError" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-4 text-red-700 text-sm">
          {{ settingsError }}
        </div>
        <div v-if="settingsSuccess" class="bg-green-50 border border-green-200 rounded-lg p-4 mb-4 text-green-700 text-sm">
          Canvis desats correctament.
        </div>

        <form @submit.prevent="saveSettings" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Nom</label>
            <input
              v-model="settingsForm.name"
              type="text"
              class="input w-full"
              placeholder="El teu nom"
            />
          </div>

          <hr class="border-gray-200" />
          <p class="text-sm text-gray-500">Deixa el camp buit si no vols canviar la contrasenya.</p>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Nova contrasenya</label>
            <input
              v-model="settingsForm.password"
              type="password"
              class="input w-full"
              placeholder="Mínim 8 caràcters"
              autocomplete="new-password"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Confirma la contrasenya</label>
            <input
              v-model="settingsForm.passwordConfirm"
              type="password"
              class="input w-full"
              placeholder="Repeteix la contrasenya"
              autocomplete="new-password"
            />
          </div>

          <button
            type="submit"
            :disabled="settingsLoading"
            class="btn-primary"
          >
            {{ settingsLoading ? 'Desant...' : 'Desar canvis' }}
          </button>
        </form>
      </div>

      <!-- TAB: Mis datos -->
      <div v-if="activeTab === 'data'" class="max-w-lg space-y-4">
        <h2 class="text-lg font-semibold text-gray-900 mb-6">Les meves dades</h2>

        <div v-if="dataError" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700 text-sm">
          {{ dataError }}
        </div>
        <div v-if="deleteRequested" class="bg-green-50 border border-green-200 rounded-lg p-4 text-green-700 text-sm">
          Sol·licitud rebuda. El teu compte s'esborrarà en 30 dies.
        </div>

        <div class="card">
          <h3 class="font-medium text-gray-900 mb-1">Descarregar les meves dades (ZIP)</h3>
          <p class="text-sm text-gray-500 mb-4">
            Exporta totes les teves dades (dret d'accés i portabilitat — RGPD Art. 15 i 20).
          </p>
          <button @click="exportData" :disabled="dataLoading" class="btn-primary">
            {{ dataLoading ? 'Exportant...' : 'Descarregar les meves dades (ZIP)' }}
          </button>
        </div>

        <div class="card border-red-100">
          <h3 class="font-medium text-gray-900 mb-1">Esborrar el meu compte</h3>
          <p class="text-sm text-gray-500 mb-4">
            El teu compte i totes les dades personals s'esborraran definitivament en 30 dies (dret a l'oblit — RGPD Art. 17).
          </p>
          <button
            @click="showDeleteModal = true"
            :disabled="dataLoading || deleteRequested"
            class="inline-flex items-center px-4 py-2 rounded-lg bg-red-600 text-white text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors"
          >
            Sol·licitar eliminació del compte
          </button>
        </div>

        <router-link to="/my-data" class="inline-block text-sm text-indigo-600 hover:underline">
          Veure gestió completa de dades RGPD →
        </router-link>
      </div>
    </div>

    <!-- Delete confirmation modal -->
    <Teleport to="body">
      <div v-if="showDeleteModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/50" @click="showDeleteModal = false"></div>
        <div class="relative bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-2">Confirmar eliminació del compte</h3>
          <p class="text-sm text-gray-600 mb-6">
            El teu compte serà eliminat en <strong>30 dies</strong>. Pots cancel·lar-ho abans des d'aquesta mateixa pàgina.
            Els viatges públics quedaran anonimitzats.
          </p>
          <div class="flex gap-3 justify-end">
            <button @click="showDeleteModal = false"
              class="px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">
              Cancel·lar
            </button>
            <button @click="confirmDeleteRequest" :disabled="dataLoading"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50">
              {{ dataLoading ? 'Processant...' : 'Sí, sol·licitar eliminació' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
