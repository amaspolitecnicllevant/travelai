<script setup>
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
</script>

<template>
  <div class="min-h-screen bg-white">
    <!-- Hero -->
    <section class="relative bg-gradient-to-br from-indigo-600 to-blue-700 text-white overflow-hidden">
      <div class="max-w-4xl mx-auto px-4 py-24 text-center relative z-10">
        <div class="text-6xl mb-6">✈</div>
        <h1 class="text-4xl sm:text-5xl font-bold leading-tight mb-4">
          Planifica el teu viatge<br class="hidden sm:block"/> amb intel·ligència artificial
        </h1>
        <p class="text-indigo-200 text-lg max-w-xl mx-auto mb-10">
          Genera itineraris personalitzats, comparteix experiències i descobreix destinacions increïbles amb TravelAI.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <router-link v-if="auth.isLoggedIn" to="/trips/new"
            class="bg-white text-indigo-700 font-semibold px-8 py-3 rounded-xl hover:bg-indigo-50 transition-colors">
            Crear nou viatge
          </router-link>
          <template v-else>
            <router-link to="/register"
              class="bg-white text-indigo-700 font-semibold px-8 py-3 rounded-xl hover:bg-indigo-50 transition-colors">
              Comença gratis
            </router-link>
            <router-link to="/explore"
              class="border border-white/50 text-white font-semibold px-8 py-3 rounded-xl hover:bg-white/10 transition-colors">
              Explorar viatges
            </router-link>
          </template>
        </div>
      </div>
      <!-- Decorative circles -->
      <div class="absolute -top-24 -right-24 w-96 h-96 bg-white/5 rounded-full"></div>
      <div class="absolute -bottom-24 -left-24 w-80 h-80 bg-white/5 rounded-full"></div>
    </section>

    <!-- Features -->
    <section class="max-w-5xl mx-auto px-4 py-20">
      <h2 class="text-2xl font-bold text-gray-900 text-center mb-12">Tot el que necessites per viatjar</h2>
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-8">
        <div class="text-center p-6">
          <div class="text-4xl mb-4">🤖</div>
          <h3 class="font-semibold text-gray-900 mb-2">IA Local i Privada</h3>
          <p class="text-gray-500 text-sm">Genera itineraris amb Ollama. Les teves dades no surten del servidor.</p>
        </div>
        <div class="text-center p-6">
          <div class="text-4xl mb-4">📅</div>
          <h3 class="font-semibold text-gray-900 mb-2">Itineraris per dies</h3>
          <p class="text-gray-500 text-sm">Plans detallats amb activitats, horaris i costos estimats per cada dia.</p>
        </div>
        <div class="text-center p-6">
          <div class="text-4xl mb-4">🌍</div>
          <h3 class="font-semibold text-gray-900 mb-2">Comunitat de viatgers</h3>
          <p class="text-gray-500 text-sm">Comparteix viatges, llegeix valoracions i inspira't en experiències reals.</p>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="bg-indigo-50 py-16">
      <div class="max-w-xl mx-auto px-4 text-center">
        <h2 class="text-2xl font-bold text-gray-900 mb-4">Preparat per al teu proper viatge?</h2>
        <p class="text-gray-500 mb-8">Registra't gratis i genera el teu primer itinerari en minuts.</p>
        <router-link to="/register"
          class="inline-block bg-indigo-600 text-white font-semibold px-8 py-3 rounded-xl hover:bg-indigo-700 transition-colors">
          Crear compte gratis
        </router-link>
      </div>
    </section>
  </div>
</template>
