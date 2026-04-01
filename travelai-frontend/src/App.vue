<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import CookieBanner from '@/components/common/CookieBanner.vue'

const auth = useAuthStore()
onMounted(() => auth.fetchMe())
</script>
<template>
  <RouterView />
  <CookieBanner />
</template>
