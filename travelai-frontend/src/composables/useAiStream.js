import { ref, readonly } from 'vue'

export function useAiStream() {
  const streaming  = ref(false)
  const progress   = ref('')
  const rawBuffer  = ref('')
  const days       = ref([])
  const error      = ref(null)
  const controller = ref(null)

  const base  = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const token = () => localStorage.getItem('accessToken')

  const generate  = (id)              => _stream(`${base}/ai/trips/${id}/generate`, 'POST')
  const refineDay = (id, day, prompt) => _stream(`${base}/ai/trips/${id}/days/${day}/refine`, 'POST', { prompt })
  const refineAll = (id, prompt)      => _stream(`${base}/ai/trips/${id}/refine-all`, 'POST', { prompt })
  const cancel    = ()                => { controller.value?.abort(); streaming.value = false }

  async function _stream(url, method, body = null) {
    cancel()
    streaming.value = true; error.value = null
    rawBuffer.value = ''; days.value = []
    progress.value  = 'Connectant amb la IA...'
    controller.value = new AbortController()
    try {
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream',
                   Authorization: `Bearer ${token()}` },
        body: body ? JSON.stringify(body) : null,
        signal: controller.value.signal
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const reader = res.body.getReader()
      const dec    = new TextDecoder()
      let pending  = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        pending += dec.decode(value, { stream: true })
        const lines = pending.split('\n'); pending = lines.pop()
        for (const line of lines) {
          if (!line.startsWith('data:')) continue
          const raw = line.slice(5).trim()
          if (raw) _handle(raw)
        }
      }
    } catch (e) {
      if (e.name !== 'AbortError') { error.value = e.message; progress.value = 'Error en la generació' }
    } finally { streaming.value = false }
    return days.value
  }

  function _handle(raw) {
    try {
      const e = JSON.parse(raw)
      if (e.type === 'start')              progress.value = e.message || 'Generant...'
      else if (e.type === 'chunk')       { rawBuffer.value += e.content || ''; progress.value = 'Escrivint...' }
      else if (e.type === 'day_complete' && e.day) { days.value = [...days.value, e.day]; progress.value = `Dia ${e.dayNumber} llest` }
      else if (e.type === 'complete')      progress.value = 'Itinerari completat'
      else if (e.type === 'error')         error.value = e.message
    } catch { /* chunk parcial */ }
  }

  return { streaming: readonly(streaming), progress: readonly(progress),
           rawBuffer: readonly(rawBuffer), days: readonly(days), error: readonly(error),
           generate, refineDay, refineAll, cancel }
}
