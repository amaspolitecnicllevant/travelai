import { defineStore } from 'pinia'
import { ref } from 'vue'
import { legalApi } from '@/api/legal'

export const useConsentStore = defineStore('consent', () => {
  const cookiesAccepted = ref(localStorage.getItem('cookiesAccepted') === 'true')
  const showBanner      = ref(!cookiesAccepted.value)

  async function acceptAll() {
    cookiesAccepted.value = true
    showBanner.value      = false
    localStorage.setItem('cookiesAccepted', 'true')
    try {
      await legalApi.saveConsent({
        type: 'COOKIES', version: '1.0', accepted: true
      })
    } catch { /* continuar encara que falli */ }
  }

  function rejectAll() {
    cookiesAccepted.value = false
    showBanner.value      = false
    localStorage.setItem('cookiesAccepted', 'false')
  }

  return { cookiesAccepted, showBanner, acceptAll, rejectAll }
})
