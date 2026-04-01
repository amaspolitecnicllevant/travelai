<script setup>
import { ref } from 'vue'
import { useConsentStore } from '@/stores/consent'

const consentStore  = useConsentStore()
const showConfigure = ref(false)

// Preferències granulars
const preferences = ref({
  necessary:  true,   // sempre true, no es pot desactivar
  analytics:  false,
  marketing:  false,
})

async function acceptAll() {
  preferences.value.analytics = true
  preferences.value.marketing = true
  await consentStore.acceptAll()
}

function rejectNonEssential() {
  preferences.value.analytics = false
  preferences.value.marketing = false
  consentStore.rejectAll()
}

async function savePreferences() {
  // Guardar preferències al localStorage
  localStorage.setItem('cookiePreferences', JSON.stringify(preferences.value))
  // Si ha acceptat almenys les necessàries, considerem que ha interaccionat
  if (preferences.value.analytics || preferences.value.marketing) {
    await consentStore.acceptAll()
  } else {
    consentStore.rejectAll()
  }
  showConfigure.value = false
}
</script>

<template>
  <Transition name="slide-up">
    <div
      v-if="consentStore.showBanner"
      class="fixed bottom-0 inset-x-0 z-50 bg-white border-t border-gray-200 shadow-2xl"
    >
      <!-- Panel de configuració granular -->
      <div v-if="showConfigure" class="border-b border-gray-200 px-6 py-4 bg-gray-50">
        <h3 class="font-semibold text-gray-900 mb-3 text-sm">Configuració de cookies</h3>
        <div class="space-y-3">
          <!-- Cookies necessàries (sempre actives) -->
          <div class="flex items-start gap-3">
            <div class="relative mt-0.5">
              <input
                type="checkbox"
                checked
                disabled
                class="h-4 w-4 rounded border-gray-300 text-indigo-600 cursor-not-allowed opacity-60"
              />
            </div>
            <div>
              <p class="text-sm font-medium text-gray-900">Necessàries
                <span class="ml-1 text-xs bg-gray-200 text-gray-600 px-1.5 py-0.5 rounded-full">Sempre actives</span>
              </p>
              <p class="text-xs text-gray-500 mt-0.5">
                Essencials per al funcionament de la plataforma: autenticació, seguretat, preferències bàsiques.
              </p>
            </div>
          </div>

          <!-- Cookies analítiques -->
          <div class="flex items-start gap-3">
            <div class="relative mt-0.5">
              <input
                v-model="preferences.analytics"
                type="checkbox"
                class="h-4 w-4 rounded border-gray-300 text-indigo-600 cursor-pointer
                       focus:ring-2 focus:ring-indigo-400"
              />
            </div>
            <div>
              <p class="text-sm font-medium text-gray-900">Analítiques</p>
              <p class="text-xs text-gray-500 mt-0.5">
                Ens ajuden a entendre com s'usa la plataforma per millorar l'experiència.
              </p>
            </div>
          </div>

          <!-- Cookies de màrqueting -->
          <div class="flex items-start gap-3">
            <div class="relative mt-0.5">
              <input
                v-model="preferences.marketing"
                type="checkbox"
                class="h-4 w-4 rounded border-gray-300 text-indigo-600 cursor-pointer
                       focus:ring-2 focus:ring-indigo-400"
              />
            </div>
            <div>
              <p class="text-sm font-medium text-gray-900">Màrqueting</p>
              <p class="text-xs text-gray-500 mt-0.5">
                Per mostrar contingut rellevant basat en els teus interessos.
              </p>
            </div>
          </div>
        </div>

        <div class="flex gap-2 mt-4">
          <button
            class="bg-indigo-600 text-white text-xs font-medium px-4 py-2 rounded-lg
                   hover:bg-indigo-700 transition-colors"
            @click="savePreferences"
          >
            Guardar preferències
          </button>
          <button
            class="text-xs text-gray-500 hover:text-gray-700 px-3 py-2 transition-colors"
            @click="showConfigure = false"
          >
            Cancel·lar
          </button>
        </div>
      </div>

      <!-- Missatge principal del bàner -->
      <div class="max-w-6xl mx-auto px-4 sm:px-6 py-4 flex flex-col sm:flex-row items-start sm:items-center gap-4">
        <!-- Text -->
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="text-base">🍪</span>
            <p class="font-semibold text-gray-900 text-sm">Ús de cookies</p>
          </div>
          <p class="text-xs text-gray-600 leading-relaxed">
            Usem cookies pròpies per garantir el funcionament de la plataforma. Pots acceptar-les
            totes, rebutjar les no essencials o
            <button class="text-indigo-600 underline hover:no-underline" @click="showConfigure = !showConfigure">
              configurar les teves preferències
            </button>.
            Consulta la nostra
            <router-link to="/cookies" class="text-indigo-600 underline hover:no-underline">
              Política de Cookies
            </router-link>
            i la
            <router-link to="/privacy" class="text-indigo-600 underline hover:no-underline">
              Política de Privacitat
            </router-link>.
          </p>
        </div>

        <!-- Botons -->
        <div class="flex items-center gap-2 flex-shrink-0">
          <button
            class="text-xs text-gray-500 hover:text-gray-700 px-3 py-2 rounded-lg
                   hover:bg-gray-100 transition-colors font-medium whitespace-nowrap"
            @click="showConfigure = !showConfigure"
          >
            Configurar
          </button>
          <button
            class="text-xs text-gray-600 hover:text-gray-900 px-3 py-2 rounded-lg border border-gray-300
                   hover:bg-gray-50 transition-colors font-medium whitespace-nowrap"
            @click="rejectNonEssential"
          >
            Nomes necessaries
          </button>
          <button
            class="bg-indigo-600 text-white text-xs font-semibold px-4 py-2 rounded-lg
                   hover:bg-indigo-700 transition-colors whitespace-nowrap"
            @click="acceptAll"
          >
            Acceptar totes
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}
.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>
