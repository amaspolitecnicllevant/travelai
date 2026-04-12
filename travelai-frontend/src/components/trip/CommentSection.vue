<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { itineraryApi } from '@/api/itinerary'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

const props = defineProps({
  tripId: { type: String, required: true },
})

const auth      = useAuthStore()
const comments  = ref([])
const loading   = ref(false)
const sending   = ref(false)
const newComment = ref('')
const error     = ref(null)
const page      = ref(0)
const hasMore   = ref(false)
const PAGE_SIZE = 10

function initials(c) {
  const name = c.authorName || c.authorUsername || ''
  return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() || '?'
}

function formatDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60)   return 'ara mateix'
  if (diff < 3600) return `fa ${Math.floor(diff / 60)} min`
  if (diff < 86400) return `fa ${Math.floor(diff / 3600)} h`
  return d.toLocaleDateString('ca-ES', { day: 'numeric', month: 'short' })
}

async function loadComments(reset = false) {
  if (reset) { page.value = 0; comments.value = [] }
  loading.value = true
  error.value   = null
  try {
    const { data } = await itineraryApi.getComments(props.tripId, { page: page.value, size: PAGE_SIZE })
    const items = data.content ?? data
    comments.value = reset ? items : [...comments.value, ...items]
    hasMore.value  = !data.last ?? false
  } catch {
    error.value = 'Error carregant els comentaris.'
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  page.value++
  await loadComments(false)
}

async function submitComment() {
  const text = newComment.value.trim()
  if (!text || sending.value) return
  sending.value = true
  try {
    const { data } = await itineraryApi.addComment(props.tripId, { content: text })
    comments.value.unshift(data)
    newComment.value = ''
  } catch {
    error.value = 'Error enviant el comentari.'
  } finally {
    sending.value = false
  }
}

async function deleteComment(commentId) {
  try {
    await itineraryApi.deleteComment(props.tripId, commentId)
    comments.value = comments.value.filter(c => c.id !== commentId)
  } catch {
    error.value = 'Error eliminant el comentari.'
  }
}

onMounted(() => loadComments(true))
</script>

<template>
  <section class="mt-8">
    <h2 class="text-lg font-semibold text-gray-900 mb-4">
      Comentaris
      <span v-if="comments.length" class="ml-1 text-sm font-normal text-gray-400">
        ({{ comments.length }})
      </span>
    </h2>

    <!-- Error -->
    <div v-if="error" class="bg-red-50 border border-red-200 rounded-lg p-3 mb-4 text-sm text-red-700">
      {{ error }}
    </div>

    <!-- Add comment (if authenticated) -->
    <div v-if="auth.isLoggedIn" class="flex gap-3 mb-6">
      <!-- Avatar -->
      <div class="h-8 w-8 rounded-full bg-indigo-500 flex items-center justify-center flex-shrink-0">
        <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" class="h-8 w-8 rounded-full object-cover" />
        <span v-else class="text-xs font-bold text-white">
          {{ (auth.user?.name || auth.user?.username || '?')[0].toUpperCase() }}
        </span>
      </div>
      <!-- Input -->
      <div class="flex-1">
        <textarea
          v-model="newComment"
          rows="2"
          maxlength="1000"
          placeholder="Escriu un comentari..."
          class="w-full border border-gray-300 rounded-xl px-3 py-2 text-sm
                 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent
                 resize-none placeholder-gray-400"
          @keydown.ctrl.enter="submitComment"
        />
        <div class="flex items-center justify-between mt-1.5">
          <span class="text-xs text-gray-400">{{ newComment.length }}/1000</span>
          <button
            @click="submitComment"
            :disabled="!newComment.trim() || sending"
            class="px-4 py-1.5 bg-indigo-600 text-white text-xs font-medium rounded-lg
                   hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed
                   transition-colors flex items-center gap-1.5"
          >
            <LoadingSpinner v-if="sending" size="xs" color="white" />
            <span>{{ sending ? 'Enviant...' : 'Comentar' }}</span>
          </button>
        </div>
      </div>
    </div>
    <div v-else class="mb-6 text-sm text-gray-500 bg-gray-50 rounded-xl p-4 text-center">
      <router-link to="/login" class="text-indigo-600 font-medium hover:underline">Inicia sessió</router-link>
      per deixar un comentari.
    </div>

    <!-- Loading -->
    <div v-if="loading && comments.length === 0" class="flex justify-center py-8">
      <LoadingSpinner size="md" />
    </div>

    <!-- Empty state -->
    <div v-else-if="!loading && comments.length === 0" class="text-center py-8 text-gray-400 text-sm">
      Encara no hi ha comentaris. Sigues el primer!
    </div>

    <!-- Comment list -->
    <ul v-else class="space-y-4">
      <li
        v-for="comment in comments"
        :key="comment.id"
        class="flex gap-3 group"
      >
        <!-- Avatar -->
        <router-link :to="`/profile/${comment.authorUsername}`" class="flex-shrink-0">
          <div class="h-8 w-8 rounded-full bg-indigo-100 flex items-center justify-center">
            <img
              v-if="comment.authorAvatarUrl"
              :src="comment.authorAvatarUrl"
              class="h-8 w-8 rounded-full object-cover"
            />
            <span v-else class="text-xs font-bold text-indigo-600">{{ initials(comment) }}</span>
          </div>
        </router-link>

        <!-- Bubble -->
        <div class="flex-1 min-w-0">
          <div class="bg-gray-50 rounded-xl px-3 py-2">
            <div class="flex items-center justify-between gap-2 mb-1">
              <router-link
                :to="`/profile/${comment.authorUsername}`"
                class="text-xs font-semibold text-gray-800 hover:text-indigo-600 transition-colors"
              >
                {{ comment.authorName || comment.authorUsername }}
              </router-link>
              <span class="text-xs text-gray-400 whitespace-nowrap">{{ formatDate(comment.createdAt) }}</span>
            </div>
            <p class="text-sm text-gray-700 whitespace-pre-wrap break-words">{{ comment.content }}</p>
          </div>

          <!-- Delete (own comment or trip owner) -->
          <button
            v-if="auth.user?.username === comment.authorUsername"
            @click="deleteComment(comment.id)"
            class="mt-1 text-xs text-gray-400 hover:text-red-500 transition-colors opacity-0 group-hover:opacity-100"
          >
            Eliminar
          </button>
        </div>
      </li>
    </ul>

    <!-- Load more -->
    <div v-if="hasMore && !loading" class="flex justify-center mt-4">
      <button
        @click="loadMore"
        class="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
      >
        Veure més comentaris
      </button>
    </div>
    <div v-if="loading && comments.length > 0" class="flex justify-center mt-4">
      <LoadingSpinner size="sm" />
    </div>
  </section>
</template>
