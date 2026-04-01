<script setup>
import { ref, onMounted } from 'vue'
import { legalApi } from '@/api/legal'
const content = ref('Carregant...')
onMounted(async () => {
  try {
    const map = { PrivacyPolicyView: 'getPrivacyPolicy', TermsView: 'getTerms',
                  CookiePolicyView: 'getCookiePolicy' }
    // TODO: carregar contingut real de l'API
    content.value = 'Contingut pendent de redacció per un advocat especialitzat en protecció de dades.'
  } catch { content.value = 'Error carregant el contingut.' }
})
</script>
<template>
  <div class="max-w-3xl mx-auto px-4 py-12">
    <h1 class="text-2xl font-semibold text-gray-900 mb-6">CookiePolicyView</h1>
    <div class="prose prose-gray max-w-none">
      <p class="text-amber-700 bg-amber-50 border border-amber-200 rounded-lg p-4 mb-6">
        Aquest document és una plantilla. Cal substituir-lo per text redactat per un advocat especialitzat en RGPD/LOPD.
      </p>
      <p>{{ content }}</p>
    </div>
  </div>
</template>
