import { useUiStore } from '@/stores/ui'

export function useToast() {
  const ui = useUiStore()
  return {
    success: (msg, duration) => ui.success(msg, duration),
    error:   (msg, duration) => ui.error(msg, duration),
    info:    (msg, duration) => ui.info(msg, duration),
    warning: (msg, duration) => ui.warning(msg, duration),
  }
}
