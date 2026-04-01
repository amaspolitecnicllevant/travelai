<script setup>
defineProps({
  day: { type: Object, required: true },
})
defineEmits(['edit'])

const typeIcons = {
  CULTURE:     '🏛',
  FOOD:        '🍽',
  LEISURE:     '🌿',
  TRANSPORT:   '🚌',
  ACCOMMODATION:'🏨',
  SHOPPING:    '🛍',
}
</script>

<template>
  <div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
    <!-- Day header -->
    <div class="bg-indigo-600 text-white px-5 py-3 flex items-center justify-between">
      <h3 class="font-semibold text-sm">
        Dia {{ day.dayNumber }}
        <span v-if="day.date" class="font-normal opacity-80 ml-2">{{ day.date }}</span>
      </h3>
      <button
        v-if="$attrs.onEdit"
        @click="$emit('edit', day)"
        class="text-xs bg-white/20 hover:bg-white/30 px-2 py-1 rounded transition-colors"
      >
        Editar amb IA
      </button>
    </div>

    <!-- Activities -->
    <div class="divide-y divide-gray-50">
      <div
        v-for="(activity, idx) in day.activities"
        :key="idx"
        class="px-5 py-4 flex gap-4"
      >
        <!-- Time -->
        <div class="w-14 text-xs text-gray-400 pt-0.5 shrink-0 text-right">
          {{ activity.time || '' }}
        </div>

        <!-- Icon -->
        <div class="text-xl shrink-0">
          {{ typeIcons[activity.type] || '📌' }}
        </div>

        <!-- Content -->
        <div class="flex-1 min-w-0">
          <p class="font-medium text-gray-900 text-sm">{{ activity.title }}</p>
          <p v-if="activity.description" class="text-gray-500 text-xs mt-1 leading-relaxed">
            {{ activity.description }}
          </p>
          <p v-if="activity.cost" class="text-indigo-600 text-xs font-medium mt-1">
            ~{{ activity.cost }}€
          </p>
        </div>
      </div>

      <div v-if="!day.activities?.length" class="px-5 py-6 text-center text-sm text-gray-400">
        Sense activitats per aquest dia
      </div>
    </div>
  </div>
</template>
