<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth   = useAuthStore()
const router = useRouter()

// Redirect authenticated users straight to the feed
onMounted(() => {
  if (auth.isLoggedIn) router.replace({ name: 'feed' })
})

const features = [
  {
    icon: '🤖',
    title: 'IA local i privada',
    desc: 'Genera itineraris amb Ollama. Les teves dades no surten del servidor. Compliment RGPD garantit.',
  },
  {
    icon: '🌍',
    title: 'Comunitat de viatgers',
    desc: 'Comparteix viatges, llegeix valoracions i inspira\'t en experiències reals d\'altres usuaris.',
  },
  {
    icon: '📅',
    title: 'Itineraris personalitzats',
    desc: 'Plans detallats amb activitats, horaris i costos estimats per cada dia del teu viatge.',
  },
]
</script>

<template>
  <!-- Show nothing while redirect is happening for logged-in users -->
  <div v-if="!auth.isLoggedIn" class="min-h-screen bg-white">

    <!-- ── Hero ─────────────────────────────────────────────────────────── -->
    <section class="relative bg-gradient-to-br from-indigo-600 via-blue-600 to-cyan-500 text-white overflow-hidden">
      <div class="absolute -top-24 -right-24 w-96 h-96 bg-white/5 rounded-full"></div>
      <div class="absolute -bottom-24 -left-24 w-80 h-80 bg-white/5 rounded-full"></div>

      <div class="relative z-10 max-w-4xl mx-auto px-4 py-24 text-center">
        <div class="text-6xl mb-6">✈</div>
        <h1 class="text-4xl sm:text-5xl font-bold leading-tight mb-4">
          Planifica el teu viatge<br class="hidden sm:block"/> amb intel·ligència artificial
        </h1>
        <p class="text-indigo-100 text-lg max-w-xl mx-auto mb-10">
          Genera itineraris personalitzats, comparteix experiències i descobreix destinacions increïbles.
          Privat per disseny. Compliment RGPD.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <router-link to="/register"
            class="bg-white text-indigo-700 font-semibold px-8 py-3.5 rounded-xl
                   hover:bg-indigo-50 transition-colors shadow-lg">
            Empieza gratis
          </router-link>
          <router-link to="/explore"
            class="border border-white/50 text-white font-semibold px-8 py-3.5 rounded-xl
                   hover:bg-white/10 transition-colors">
            Ver viajes
          </router-link>
        </div>
      </div>
    </section>

    <!-- ── Features ─────────────────────────────────────────────────────── -->
    <section class="max-w-5xl mx-auto px-4 py-20">
      <h2 class="text-2xl font-bold text-gray-900 text-center mb-12">
        Tot el que necessites per viatjar
      </h2>
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-8">
        <div
          v-for="feat in features"
          :key="feat.title"
          class="text-center p-6 rounded-2xl border border-gray-100 hover:shadow-md transition-shadow"
        >
          <div class="text-4xl mb-4">{{ feat.icon }}</div>
          <h3 class="font-semibold text-gray-900 mb-2">{{ feat.title }}</h3>
          <p class="text-gray-500 text-sm leading-relaxed">{{ feat.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ── CTA ──────────────────────────────────────────────────────────── -->
    <section class="bg-indigo-50 py-16">
      <div class="max-w-xl mx-auto px-4 text-center">
        <h2 class="text-2xl font-bold text-gray-900 mb-4">Preparat per al teu proper viatge?</h2>
        <p class="text-gray-500 mb-8">
          Registra't gratis i genera el teu primer itinerari en minuts. Sense subscripció, sense trampes.
        </p>
        <div class="flex flex-col sm:flex-row gap-3 justify-center">
          <router-link to="/register"
            class="inline-block bg-indigo-600 text-white font-semibold px-8 py-3 rounded-xl
                   hover:bg-indigo-700 transition-colors">
            Crear compte gratis
          </router-link>
          <router-link to="/login"
            class="inline-block border border-indigo-300 text-indigo-600 font-semibold px-8 py-3 rounded-xl
                   hover:bg-indigo-50 transition-colors">
            Iniciar sessió
          </router-link>
        </div>
      </div>
    </section>

    <!-- ── Footer legal ──────────────────────────────────────────────────── -->
    <footer class="bg-gray-900 text-gray-400 py-8">
      <div class="max-w-5xl mx-auto px-4">
        <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
          <p class="text-sm font-semibold text-white">TravelAI</p>
          <nav class="flex flex-wrap gap-x-6 gap-y-2 text-xs justify-center">
            <router-link to="/privacy" class="hover:text-white transition-colors">Privacitat</router-link>
            <router-link to="/terms"   class="hover:text-white transition-colors">Termes d'ús</router-link>
            <router-link to="/cookies" class="hover:text-white transition-colors">Cookies</router-link>
            <router-link to="/legal"   class="hover:text-white transition-colors">Avís legal</router-link>
          </nav>
          <p class="text-xs">© 2026 TravelAI · IA local i privada</p>
        </div>
      </div>
    </footer>

  </div>
</template>
