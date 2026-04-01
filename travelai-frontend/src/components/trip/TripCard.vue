<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { tripsApi } from '@/api/trips'
import { useTrips } from '@/composables/useTrips'
import RatingStars from './RatingStars.vue'

const props = defineProps({
  trip: { type: Object, required: true },
})
const emit = defineEmits(['deleted', 'duplicated'])

const auth   = useAuthStore()
const router = useRouter()
const { deleteTrip } = useTrips()

const isOwner = computed(() =>
  auth.isLoggedIn && (
    auth.user?.id === props.trip?.author?.id ||
    auth.user?.username === props.trip?.author?.username
  )
)

const menuOpen    = ref(false)
const duplicating = ref(false)
const deleting    = ref(false)

function toggleMenu(e) {
  e.preventDefault()
  e.stopPropagation()
  menuOpen.value = !menuOpen.value
}

function closeMenu() {
  menuOpen.value = false
}

function handleEdit(e) {
  e.preventDefault()
  closeMenu()
  router.push({ name: 'trip-edit', params: { id: props.trip.id } })
}

async function handleDuplicate(e) {
  e.preventDefault()
  closeMenu()
  duplicating.value = true
  try {
    const { data } = await tripsApi.duplicate(props.trip.id)
    emit('duplicated', data)
    router.push({ name: 'trip-edit', params: { id: data.id } })
  } catch { /* silently ignore */ }
  finally { duplicating.value = false }
}

async function handleDelete(e) {
  e.preventDefault()
  closeMenu()
  if (!confirm(`Segur que vols eliminar "${props.trip.title}"?`)) return
  deleting.value = true
  const ok = await deleteTrip(props.trip.id)
  if (ok) emit('deleted', props.trip.id)
  deleting.value = false
}

// Status badge config
const statusConfig = {
  DRAFT:     { label: 'Esborrany', cls: 'bg-gray-100 text-gray-600' },
  PUBLISHED: { label: 'Publicat',  cls: 'bg-indigo-100 text-indigo-700' },
}
</script>

<template>
  <div class="relative group">
    <router-link :to="`/trips/${trip.id}`" class="block">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden
                  hover:shadow-lg hover:scale-[1.02] transition-all duration-200">

        <!-- Cover image -->
        <div class="relative h-44 bg-indigo-100 overflow-hidden">
          <img
            v-if="trip.coverImageUrl"
            :src="trip.coverImageUrl"
            :alt="trip.title"
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
          />
          <div v-else class="w-full h-full flex items-center justify-center text-5xl">✈</div>

          <!-- Top badges row -->
          <div class="absolute top-2 left-2 right-2 flex items-center justify-between gap-2">
            <!-- Visibility badge -->
            <span
              class="text-xs font-medium px-2 py-1 rounded-full flex items-center gap-1"
              :class="trip.visibility === 'PUBLIC'
                ? 'bg-green-100 text-green-700'
                : 'bg-gray-100/90 text-gray-600'"
            >
              <span v-if="trip.visibility === 'PUBLIC'">🌍</span>
              <span v-else>🔒</span>
              {{ trip.visibility === 'PUBLIC' ? 'Públic' : 'Privat' }}
            </span>

            <!-- Status badge -->
            <span
              v-if="trip.status && statusConfig[trip.status]"
              class="text-xs font-medium px-2 py-1 rounded-full"
              :class="statusConfig[trip.status].cls"
            >
              {{ statusConfig[trip.status].label }}
            </span>
          </div>

          <!-- Owner action menu -->
          <div v-if="isOwner" class="absolute bottom-2 right-2">
            <button
              @click="toggleMenu"
              class="w-8 h-8 rounded-full bg-black/40 hover:bg-black/60 text-white
                     flex items-center justify-center transition-all text-sm font-bold leading-none
                     opacity-0 group-hover:opacity-100"
              :class="{ 'opacity-100': menuOpen }"
              title="Accions"
            >
              ···
            </button>

            <!-- Dropdown -->
            <Transition
              enter-active-class="transition duration-100 ease-out"
              enter-from-class="transform scale-95 opacity-0"
              enter-to-class="transform scale-100 opacity-100"
              leave-active-class="transition duration-75 ease-in"
              leave-from-class="transform scale-100 opacity-100"
              leave-to-class="transform scale-95 opacity-0"
            >
              <div
                v-if="menuOpen"
                class="absolute bottom-10 right-0 z-50 w-40 bg-white rounded-xl shadow-xl
                       border border-gray-100 overflow-hidden"
                @click.stop
              >
                <button
                  @click="handleEdit"
                  class="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-gray-700
                         hover:bg-gray-50 transition-colors text-left"
                >
                  <span>✏️</span> Editar
                </button>
                <button
                  @click="handleDuplicate"
                  :disabled="duplicating"
                  class="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-gray-700
                         hover:bg-gray-50 transition-colors text-left disabled:opacity-50"
                >
                  <span>{{ duplicating ? '⏳' : '📋' }}</span>
                  {{ duplicating ? 'Duplicant...' : 'Duplicar' }}
                </button>
                <button
                  @click="handleDelete"
                  :disabled="deleting"
                  class="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-red-600
                         hover:bg-red-50 transition-colors text-left disabled:opacity-50"
                >
                  <span>{{ deleting ? '⏳' : '🗑️' }}</span>
                  {{ deleting ? 'Eliminant...' : 'Eliminar' }}
                </button>
              </div>
            </Transition>
          </div>
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
            <span class="text-xs text-gray-400">
              {{ trip.days }} {{ trip.days === 1 ? 'dia' : 'dies' }}
            </span>
            <RatingStars v-if="trip.averageRating" :model-value="trip.averageRating" :readonly="true" size="sm" />
          </div>

          <!-- Author -->
          <p v-if="trip.author?.username" class="text-xs text-gray-400 mt-2">
            per @{{ trip.author.username }}
          </p>
        </div>
      </div>
    </router-link>
  </div>
</template>
