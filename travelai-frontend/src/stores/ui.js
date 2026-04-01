import { defineStore } from 'pinia'
import { ref } from 'vue'

let _id = 0

export const useUiStore = defineStore('ui', () => {
  const globalLoading = ref(false)
  const toasts        = ref([])

  function addToast(message, type = 'info', duration = 4000) {
    const id = ++_id
    toasts.value.push({ id, message, type })
    if (duration > 0) setTimeout(() => removeToast(id), duration)
    return id
  }

  function removeToast(id) {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  const success = (msg, duration) => addToast(msg, 'success', duration)
  const error   = (msg, duration) => addToast(msg, 'error', duration)
  const info    = (msg, duration) => addToast(msg, 'info', duration)
  const warning = (msg, duration) => addToast(msg, 'warning', duration)

  return { globalLoading, toasts, addToast, removeToast, success, error, info, warning }
})
