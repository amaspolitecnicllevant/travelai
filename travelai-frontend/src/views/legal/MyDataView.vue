<script setup>
import { ref, onMounted } from 'vue'
import { legalApi } from '@/api/legal'
import { usersApi } from '@/api/users'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

// ── Data we store info ────────────────────────────────────────────────────────
const dataCategories = [
  { label: 'Nom i cognoms', detail: 'Proporcionat al registre.' },
  { label: 'Adreça de correu electrònic', detail: 'Identificador únic de compte.' },
  { label: 'Viatges i itineraris', detail: 'Tot el contingut que has creat.' },
  { label: 'Valoracions', detail: 'Puntuacions de viatges públics.' },
  { label: 'Logs d\'auditoria', detail: 'Accions sensibles (login, canvis, etc.).' },
  { label: 'Historial de consentiment', detail: 'Versions acceptades i timestamps.' },
  { label: 'Metadades de sessió', detail: 'IP i timestamps d\'accés (eliminem als 90 dies).' },
]

// ── Export ────────────────────────────────────────────────────────────────────
const exportLoading = ref(false)
const exportError   = ref(null)
const exportSuccess = ref(false)

async function exportData() {
  exportLoading.value = true
  exportError.value   = null
  exportSuccess.value = false
  try {
    const res  = await legalApi.exportMyData()
    const url  = URL.createObjectURL(res.data)
    const a    = document.createElement('a')
    a.href     = url
    a.download = 'mis-datos.zip'
    a.click()
    URL.revokeObjectURL(url)
    exportSuccess.value = true
  } catch {
    exportError.value = 'Error exportant les dades. Torna-ho a intentar.'
  } finally {
    exportLoading.value = false
  }
}

// ── Deletion request ──────────────────────────────────────────────────────────
const deleteStatus   = ref(null)   // null | { scheduledAt: string }
const deleteLoading  = ref(false)
const deleteError    = ref(null)
const showModal      = ref(false)

async function fetchDeleteStatus() {
  try {
    const { data } = await usersApi.getMe()
    if (data.deleteScheduledAt) {
      deleteStatus.value = { scheduledAt: data.deleteScheduledAt }
    }
  } catch { /* non-critical */ }
}

function formatDate(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleDateString('es-ES', {
    year: 'numeric', month: 'long', day: 'numeric'
  })
}

async function requestDeletion() {
  deleteLoading.value = true
  deleteError.value   = null
  try {
    const { data } = await legalApi.requestDeletion('Sol·licitud de l\'usuari')
    deleteStatus.value = { scheduledAt: data?.scheduledAt || new Date(Date.now() + 30 * 86400000).toISOString() }
    showModal.value = false
  } catch {
    deleteError.value = 'Error en la sol·licitud. Contacta amb privacidad@travelai.local'
  } finally {
    deleteLoading.value = false
  }
}

async function cancelDeletion() {
  deleteLoading.value = true
  deleteError.value   = null
  try {
    await legalApi.cancelDeletion()
    deleteStatus.value = null
  } catch {
    deleteError.value = 'Error cancel·lant la sol·licitud. Torna-ho a intentar.'
  } finally {
    deleteLoading.value = false
  }
}

// ── Consent history ───────────────────────────────────────────────────────────
const consentHistory  = ref([])
const consentLoading  = ref(false)
const consentError    = ref(null)

async function fetchConsentHistory() {
  consentLoading.value = true
  consentError.value   = null
  try {
    const { data } = await usersApi.getMe()
    // Try dedicated endpoint first
    try {
      const res = await import('@/api').then(m => m.default.get('/users/me/consent-history'))
      consentHistory.value = Array.isArray(res.data) ? res.data : []
    } catch {
      // Fallback: build from user data if available
      if (data.consentHistory) {
        consentHistory.value = data.consentHistory
      }
    }
  } catch {
    consentError.value = 'Error carregant l\'historial de consentiment.'
  } finally {
    consentLoading.value = false
  }
}

function formatDateTime(iso) {
  if (!iso) return ''
  return new Date(iso).toLocaleString('es-ES', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

// ── Init ──────────────────────────────────────────────────────────────────────
onMounted(async () => {
  await fetchDeleteStatus()
  fetchConsentHistory()
})
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-3xl mx-auto px-4 py-10">

      <div class="mb-8">
        <h1 class="text-2xl font-bold text-gray-900">Les meves dades</h1>
        <p class="text-gray-500 mt-1">
          Gestiona les teves dades personals d'acord amb el RGPD (UE 2016/679) i la LOPD-GDD.
        </p>
      </div>

      <!-- ── Secció: Quines dades guardem ─────────────────────────────────── -->
      <section class="card mb-4">
        <h2 class="font-semibold text-gray-900 mb-4">Quines dades guardem</h2>
        <ul class="divide-y divide-gray-100">
          <li v-for="cat in dataCategories" :key="cat.label" class="py-3 flex items-start gap-3">
            <span class="mt-0.5 h-2 w-2 rounded-full bg-indigo-400 flex-shrink-0"></span>
            <div>
              <span class="text-sm font-medium text-gray-800">{{ cat.label }}</span>
              <span class="text-sm text-gray-500"> — {{ cat.detail }}</span>
            </div>
          </li>
        </ul>
      </section>

      <!-- ── Secció: Exportar dades ─────────────────────────────────────────── -->
      <section class="card mb-4">
        <h2 class="font-semibold text-gray-900 mb-1">Exportar totes les meves dades</h2>
        <p class="text-sm text-gray-500 mb-4">
          Rebràs un arxiu ZIP amb totes les teves dades en format llegible
          (dret d'accés i portabilitat — RGPD Art. 15 i 20).
        </p>

        <div v-if="exportError" class="bg-red-50 border border-red-200 rounded-lg p-3 mb-3 text-red-700 text-sm">
          {{ exportError }}
        </div>
        <div v-if="exportSuccess" class="bg-green-50 border border-green-200 rounded-lg p-3 mb-3 text-green-700 text-sm">
          Descàrrega iniciada correctament.
        </div>

        <button @click="exportData" :disabled="exportLoading" class="btn-primary">
          {{ exportLoading ? 'Exportant...' : 'Exportar totes les meves dades (ZIP)' }}
        </button>
      </section>

      <!-- ── Secció: Sol·licitud d'eliminació ──────────────────────────────── -->
      <section class="card border-red-100 mb-4">
        <h2 class="font-semibold text-gray-900 mb-1">Sol·licitud d'eliminació de compte</h2>
        <p class="text-sm text-gray-500 mb-4">
          El teu compte i totes les dades personals s'esborraran definitivament en 30 dies
          (dret a l'oblit — RGPD Art. 17). Els viatges públics quedaran anonimitzats.
        </p>

        <div v-if="deleteError" class="bg-red-50 border border-red-200 rounded-lg p-3 mb-3 text-red-700 text-sm">
          {{ deleteError }}
        </div>

        <!-- Active deletion request -->
        <div v-if="deleteStatus" class="bg-amber-50 border border-amber-200 rounded-lg p-4 mb-4">
          <p class="text-sm font-medium text-amber-800 mb-1">Sol·licitud activa</p>
          <p class="text-sm text-amber-700">
            El teu compte està programat per esborrar-se el
            <strong>{{ formatDate(deleteStatus.scheduledAt) }}</strong>.
            Pots cancel·lar-ho abans d'aquesta data.
          </p>
          <button
            @click="cancelDeletion"
            :disabled="deleteLoading"
            class="mt-3 px-4 py-2 rounded-lg border border-amber-400 text-amber-800 text-sm font-medium hover:bg-amber-100 disabled:opacity-50 transition-colors"
          >
            {{ deleteLoading ? 'Cancel·lant...' : 'Cancel·lar sol·licitud d\'eliminació' }}
          </button>
        </div>

        <!-- No active request -->
        <button
          v-else
          @click="showModal = true"
          :disabled="deleteLoading"
          class="inline-flex items-center px-4 py-2 rounded-lg bg-red-600 text-white text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors"
        >
          Sol·licitar eliminació de compte
        </button>
      </section>

      <!-- ── Secció: Historial de consentiment ─────────────────────────────── -->
      <section class="card">
        <h2 class="font-semibold text-gray-900 mb-4">Historial de consentiment</h2>

        <div v-if="consentLoading" class="flex justify-center py-6">
          <LoadingSpinner size="sm" />
        </div>

        <div v-else-if="consentError" class="text-sm text-red-600">{{ consentError }}</div>

        <div v-else-if="consentHistory.length === 0" class="text-sm text-gray-500 py-2">
          No hi ha registres de consentiment disponibles.
        </div>

        <div v-else class="overflow-x-auto">
          <table class="min-w-full text-sm">
            <thead>
              <tr class="border-b border-gray-200">
                <th class="text-left text-xs font-medium text-gray-500 uppercase tracking-wide pb-2 pr-4">Versió</th>
                <th class="text-left text-xs font-medium text-gray-500 uppercase tracking-wide pb-2 pr-4">Data</th>
                <th class="text-left text-xs font-medium text-gray-500 uppercase tracking-wide pb-2">IP</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="entry in consentHistory" :key="entry.id ?? entry.version + entry.acceptedAt">
                <td class="py-2.5 pr-4 text-gray-800 font-mono">{{ entry.version }}</td>
                <td class="py-2.5 pr-4 text-gray-600">{{ formatDateTime(entry.acceptedAt) }}</td>
                <td class="py-2.5 text-gray-500 font-mono text-xs">{{ entry.ipAddress || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- ── Contacte RGPD ────────────────────────────────────────────────── -->
      <section class="mt-4 p-4 bg-gray-100 rounded-xl text-sm text-gray-600">
        Per a qualsevol consulta sobre les teves dades (rectificació, limitació, oposició) contacta amb:
        <a href="mailto:privacidad@travelai.local" class="font-medium text-indigo-600 ml-1 hover:underline">
          privacidad@travelai.local
        </a>
      </section>
    </div>

    <!-- Delete confirmation modal -->
    <Teleport to="body">
      <div v-if="showModal" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="absolute inset-0 bg-black/50" @click="showModal = false"></div>
        <div class="relative bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
          <h3 class="text-lg font-semibold text-gray-900 mb-2">Confirmar eliminació del compte</h3>
          <p class="text-sm text-gray-600 mb-6">
            El teu compte serà eliminat en <strong>30 dies</strong>. Pots cancel·lar-ho abans des d'aquesta mateixa pàgina.
            Els viatges públics quedaran anonimitzats i les teves dades personals s'esborraran permanentment.
          </p>
          <div class="flex gap-3 justify-end">
            <button
              @click="showModal = false"
              class="px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              Cancel·lar
            </button>
            <button
              @click="requestDeletion"
              :disabled="deleteLoading"
              class="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded-lg hover:bg-red-700 disabled:opacity-50"
            >
              {{ deleteLoading ? 'Processant...' : 'Sí, sol·licitar eliminació' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
