<script setup>
import { useUiStore } from '@/stores/ui'
const ui = useUiStore()

const icons = {
  success: '✓',
  error:   '✕',
  info:    'ℹ',
  warning: '⚠',
}
const colors = {
  success: 'bg-green-500',
  error:   'bg-red-500',
  info:    'bg-indigo-500',
  warning: 'bg-amber-500',
}
</script>

<template>
  <teleport to="body">
    <div class="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      <transition-group name="toast" tag="div" class="flex flex-col gap-2">
        <div
          v-for="toast in ui.toasts"
          :key="toast.id"
          class="flex items-start gap-3 rounded-lg shadow-lg p-4 text-white pointer-events-auto cursor-pointer"
          :class="colors[toast.type] || 'bg-gray-700'"
          @click="ui.removeToast(toast.id)"
        >
          <span class="text-lg font-bold leading-none mt-0.5">{{ icons[toast.type] }}</span>
          <span class="text-sm flex-1">{{ toast.message }}</span>
        </div>
      </transition-group>
    </div>
  </teleport>
</template>

<style scoped>
.toast-enter-active, .toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from { opacity: 0; transform: translateX(100%); }
.toast-leave-to   { opacity: 0; transform: translateX(100%); }
</style>
