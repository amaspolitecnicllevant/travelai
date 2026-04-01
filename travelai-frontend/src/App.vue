<script setup>
import { onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocket } from '@/composables/useWebSocket'
import CookieBanner from '@/components/common/CookieBanner.vue'

const auth = useAuthStore()
const ws   = useWebSocket()

onMounted(async () => {
  await auth.fetchMe()
  if (auth.isLoggedIn) ws.connect()
})

watch(() => auth.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    ws.connect()
  } else {
    ws.disconnect()
  }
})
</script>

<template>
  <RouterView />
  <CookieBanner />
</template>
