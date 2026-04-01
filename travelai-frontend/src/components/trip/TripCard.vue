<script setup>
import RatingStars from './RatingStars.vue'

defineProps({
  trip: { type: Object, required: true },
})
</script>

<template>
  <router-link :to="`/trips/${trip.id}`" class="block group">
    <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow">
      <!-- Cover image -->
      <div class="relative h-44 bg-indigo-100 overflow-hidden">
        <img
          v-if="trip.coverImageUrl"
          :src="trip.coverImageUrl"
          :alt="trip.title"
          class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        <div v-else class="w-full h-full flex items-center justify-center text-5xl">✈</div>

        <!-- Visibility badge -->
        <span
          class="absolute top-2 right-2 text-xs font-medium px-2 py-1 rounded-full"
          :class="trip.visibility === 'PUBLIC'
            ? 'bg-green-100 text-green-700'
            : 'bg-gray-100 text-gray-600'"
        >
          {{ trip.visibility === 'PUBLIC' ? 'Públic' : 'Privat' }}
        </span>
      </div>

      <!-- Content -->
      <div class="p-4">
        <h3 class="font-semibold text-gray-900 text-base truncate group-hover:text-indigo-600 transition-colors">
          {{ trip.title }}
        </h3>
        <p class="text-sm text-gray-500 mt-1 flex items-center gap-1">
          <span>📍</span> {{ trip.destination }}
        </p>

        <div class="mt-3 flex items-center justify-between">
          <span class="text-xs text-gray-400">{{ trip.days }} {{ trip.days === 1 ? 'dia' : 'dies' }}</span>
          <RatingStars v-if="trip.averageRating" :model-value="trip.averageRating" :readonly="true" size="sm" />
        </div>

        <!-- Author -->
        <p v-if="trip.author?.username" class="text-xs text-gray-400 mt-2">
          per @{{ trip.author.username }}
        </p>
      </div>
    </div>
  </router-link>
</template>
