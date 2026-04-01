<script setup>
import { ref } from 'vue'
import { legalApi } from '@/api/legal'

const loading    = ref(false)
const requested  = ref(false)
const error      = ref(null)

async function exportData() {
  loading.value = true
  try {
    const res  = await legalApi.exportMyData()
    const url  = URL.createObjectURL(res.data)
    const a    = document.createElement('a')
    a.href     = url
    a.download = 'les-meves-dades-travelai.json'
    a.click()
    URL.revokeObjectURL(url)
  } catch { error.value = 'Error exportant les dades. Torna-ho a intentar.' }
  finally { loading.value = false }
}

async function requestDeletion() {
  if (!confirm('Estàs segur? El compte s\'esborrarà definitivament en 30 dies.')) return
  loading.value = true
  try { await legalApi.requestDeletion('Sol·licitud de l\'usuari'); requested.value = true }
  catch { error.value = 'Error en la sol·licitud. Contacta amb privacidad@travelai.local' }
  finally { loading.value = false }
}
</script>
<template>
  <div class="max-w-2xl mx-auto px-4 py-12">
    <h1 class="text-2xl font-semibold text-gray-900 mb-2">Les meves dades</h1>
    <p class="text-gray-500 mb-8">Gestiona les teves dades personals d'acord amb el RGPD.</p>

    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 text-red-700 text-sm">{{ error }}</div>
    <div v-if="requested" class="bg-green-50 border border-green-200 rounded-lg p-4 mb-6 text-green-700 text-sm">
      Sol·licitud rebuda. El teu compte s'esborrarà en 30 dies. Rebràs un email de confirmació.
    </div>

    <div class="space-y-4">
      <div class="card">
        <h2 class="font-medium text-gray-900 mb-1">Descarregar les meves dades</h2>
        <p class="text-sm text-gray-500 mb-4">Exporta totes les teves dades en format JSON (dret d'accés i portabilitat — RGPD Art. 15 i 20).</p>
        <button @click="exportData" :disabled="loading" class="btn-primary">
          {{ loading ? 'Exportant...' : 'Descarregar dades' }}
        </button>
      </div>

      <div class="card border-red-100">
        <h2 class="font-medium text-gray-900 mb-1">Esborrar el meu compte</h2>
        <p class="text-sm text-gray-500 mb-4">El teu compte i totes les dades personals s'esborraran definitivament en 30 dies (dret a l'oblit — RGPD Art. 17). Els viatges públics quedaran anonimitzats.</p>
        <button @click="requestDeletion" :disabled="loading || requested"
                class="inline-flex items-center px-4 py-2 rounded-lg bg-red-600 text-white text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors">
          Sol·licitar esborrat del compte
        </button>
      </div>

      <div class="card">
        <h2 class="font-medium text-gray-900 mb-1">Contacte per a drets RGPD</h2>
        <p class="text-sm text-gray-500">Per a qualsevol consulta sobre les teves dades (rectificació, limitació, oposició) contacta amb:</p>
        <p class="text-sm font-medium text-primary-600 mt-2">privacidad@travelai.local</p>
      </div>
    </div>
  </div>
</template>
