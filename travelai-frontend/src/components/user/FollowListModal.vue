<script setup>
import { ref, watch } from 'vue'
import { usersApi } from '@/api/users'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps({
  username: { type: String, required: true },
  mode:     { type: String, required: true }, // 'followers' | 'following'
  modelValue: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const users   = ref([])
const loading = ref(false)
const error   = ref(null)
const page    = ref(0)
const hasMore = ref(false)

const title = props.mode === 'followers' ? 'Seguidors' : 'Seguint'

function initials(u) {
  const name = u.name || u.username || ''
  return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() || '?'
}

async function load(reset = true) {
  if (reset) { page.value = 0; users.value = [] }
  loading.value = true
  error.value   = null
  try {
    const fn = props.mode === 'followers' ? usersApi.getFollowers : usersApi.getFollowing
    const { data } = await fn(props.username, { page: page.value, size: 20 })
    const items = data.content ?? data
    users.value = reset ? items : [...users.value, ...items]
    hasMore.value = data.last === false
  } catch {
    error.value = 'Error carregant la llista.'
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  page.value++
  await load(false)
}

watch(() => props.modelValue, (open) => {
  if (open) load(true)
})
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/50" @click="emit('update:modelValue', false)" />

      <div class="relative bg-white rounded-2xl shadow-xl w-full max-w-sm max-h-[70vh] flex flex-col">
        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-gray-100">
          <h3 class="font-semibold text-gray-900">{{ title }}</h3>
          <button
            @click="emit('update:modelValue', false)"
            class="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- Body -->
        <div class="flex-1 overflow-y-auto px-4 py-3">
          <div v-if="loading && users.length === 0" class="flex justify-center py-10">
            <LoadingSpinner size="md" />
          </div>

          <div v-else-if="error" class="text-sm text-red-600 text-center py-6">{{ error }}</div>

          <div v-else-if="users.length === 0" class="text-sm text-gray-400 text-center py-10">
            {{ mode === 'followers' ? 'Encara no hi ha seguidors' : 'No segueix ningú' }}
          </div>

          <ul v-else class="space-y-1">
            <li v-for="u in users" :key="u.id ?? u.username">
              <router-link
                :to="`/profile/${u.username}`"
                @click="emit('update:modelValue', false)"
                class="flex items-center gap-3 px-2 py-2 rounded-xl hover:bg-gray-50 transition-colors"
              >
                <div class="h-9 w-9 rounded-full bg-indigo-100 flex items-center justify-center flex-shrink-0">
                  <img v-if="u.avatarUrl" :src="u.avatarUrl" class="h-9 w-9 rounded-full object-cover" />
                  <span v-else class="text-xs font-bold text-indigo-600">{{ initials(u) }}</span>
                </div>
                <div class="min-w-0">
                  <p class="text-sm font-medium text-gray-900 truncate">{{ u.name || u.username }}</p>
                  <p class="text-xs text-gray-400 truncate">@{{ u.username }}</p>
                </div>
              </router-link>
            </li>
          </ul>

          <div v-if="hasMore && !loading" class="flex justify-center mt-3 pb-2">
            <button @click="loadMore" class="text-sm text-indigo-600 hover:text-indigo-700 font-medium">
              Veure més
            </button>
          </div>
          <div v-if="loading && users.length > 0" class="flex justify-center mt-3">
            <LoadingSpinner size="sm" />
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
