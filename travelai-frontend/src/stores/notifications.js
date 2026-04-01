import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useNotificationsStore = defineStore('notifications', () => {
  const notifications = ref([])
  const loading       = ref(false)

  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  async function fetchUnread() {
    loading.value = true
    try {
      const { data } = await api.get('/notifications')
      notifications.value = Array.isArray(data) ? data : (data.content ?? [])
    } catch {
      // silently ignore — non-critical
    } finally {
      loading.value = false
    }
  }

  async function markAllRead() {
    try {
      await api.put('/notifications/read-all')
      notifications.value = notifications.value.map(n => ({ ...n, read: true }))
    } catch {
      // silently ignore
    }
  }

  function addNotification(notification) {
    notifications.value.unshift(notification)
  }

  return { notifications, unreadCount, loading, fetchUnread, markAllRead, addNotification }
})
