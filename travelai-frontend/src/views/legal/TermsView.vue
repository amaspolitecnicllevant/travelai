<script setup>
import { ref, onMounted } from 'vue'
import { legalApi } from '@/api/legal'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const loading   = ref(true)
const content   = ref('')
const title     = ref("Termes d'Ús")
const updatedAt = ref(null)
const error     = ref(null)

onMounted(async () => {
  try {
    const { data } = await legalApi.getTerms()
    content.value   = data.content || data.body || data
    title.value     = data.title || "Termes d'Ús"
    updatedAt.value = data.updatedAt || data.updated_at || null
  } catch (e) {
    error.value = e.response?.status === 404
      ? 'Document no disponible.'
      : 'Error carregant el contingut. Torna-ho a intentar més tard.'
  } finally {
    loading.value = false
  }
})

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('ca-ES', { day: 'numeric', month: 'long', year: 'numeric' })
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-3xl mx-auto px-4 py-12">
      <!-- Breadcrumb -->
      <nav class="flex items-center gap-2 text-sm text-gray-400 mb-8">
        <router-link to="/" class="hover:text-indigo-600 transition-colors">Inici</router-link>
        <span>/</span>
        <span class="text-gray-700">Termes d'Ús</span>
      </nav>

      <!-- Loading -->
      <div v-if="loading" class="flex justify-center py-24">
        <LoadingSpinner size="xl" />
      </div>

      <!-- Error -->
      <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-2xl p-8 text-center">
        <p class="text-red-600 font-medium">{{ error }}</p>
        <button class="mt-4 text-sm text-indigo-600 hover:underline" @click="$router.back()">
          Tornar
        </button>
      </div>

      <!-- Contingut -->
      <div v-else class="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
        <div class="bg-slate-700 px-8 py-10 text-white">
          <h1 class="text-3xl font-bold">{{ title }}</h1>
          <p v-if="updatedAt" class="mt-2 text-slate-300 text-sm">
            Darrera actualització: {{ formatDate(updatedAt) }}
          </p>
        </div>

        <div class="px-8 py-8 prose prose-gray max-w-none
                    prose-headings:text-gray-900 prose-p:text-gray-600
                    prose-a:text-indigo-600 prose-a:no-underline hover:prose-a:underline"
             v-html="content" />

        <!-- Footer legal -->
        <div class="border-t border-gray-100 px-8 py-6 bg-gray-50 flex flex-wrap gap-4 text-sm text-gray-500">
          <router-link to="/privacy" class="hover:text-indigo-600 transition-colors">Política de Privacitat</router-link>
          <router-link to="/cookies" class="hover:text-indigo-600 transition-colors">Política de Cookies</router-link>
          <router-link to="/legal"   class="hover:text-indigo-600 transition-colors">Avís Legal</router-link>
          <router-link to="/my-data" class="hover:text-indigo-600 transition-colors">Les meves dades</router-link>
        </div>
      </div>
    </div>
  </div>
</template>
