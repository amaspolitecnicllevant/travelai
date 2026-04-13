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

  // Refresc de token per a crides fetch() (SSE) — l'interceptor Axios no cobreix fetch
  async function _refreshToken() {
    const rt = localStorage.getItem('refreshToken')
    if (!rt) { window.location.href = '/login'; throw new Error('No refresh token') }
    const resp = await fetch(`${base}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: rt }),
    })
    if (!resp.ok) { localStorage.clear(); window.location.href = '/login'; throw new Error('Session expired') }
    const data = await resp.json()
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    return data.accessToken
  }

  const generate      = (id)              => _stream(`${base}/ai/trips/${id}/generate`, 'POST')
  const refineDay     = (id, day, prompt) => _stream(`${base}/ai/trips/${id}/days/${day}/refine`, 'POST', { prompt })
  const refineAll     = (id, prompt)      => _stream(`${base}/ai/trips/${id}/refine-all`, 'POST', { prompt })
  const editItinerary = (id, prompt)      => _stream(`${base}/ai/trips/${id}/edit`, 'POST', { prompt })
  const cancel        = ()                => { controller.value?.abort(); streaming.value = false }

  // ── Budget estimate (independent state) ────────────────────────────────────
  const budgetStreaming = ref(false)
  const budgetRaw      = ref('')
  const budgetError    = ref(null)
  const budgetResult   = ref(null)
  const budgetCtrl     = ref(null)

  async function estimateBudget(id) {
    budgetCtrl.value?.abort()
    budgetStreaming.value = true
    budgetRaw.value = ''; budgetError.value = null; budgetResult.value = null
    budgetCtrl.value = new AbortController()
    try {
      let tk = token()
      let res = await fetch(`${base}/ai/trips/${id}/budget-estimate`, {
        method: 'GET',
        headers: { Accept: 'text/event-stream', Authorization: `Bearer ${tk}` },
        signal: budgetCtrl.value.signal,
      })
      if (res.status === 401) {
        tk = await _refreshToken()
        res = await fetch(`${base}/ai/trips/${id}/budget-estimate`, {
          method: 'GET',
          headers: { Accept: 'text/event-stream', Authorization: `Bearer ${tk}` },
          signal: budgetCtrl.value.signal,
        })
      }
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const reader = res.body.getReader()
      const dec    = new TextDecoder()
      let pending  = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        pending += dec.decode(value, { stream: true })
        const messages = pending.split('\n\n')
        pending = messages.pop()
        for (const msg of messages) {
          for (const line of msg.split('\n')) {
            if (line.startsWith('data:')) {
              const chunk = line.slice(5).trim()
              if (chunk && chunk !== '[DONE]') budgetRaw.value += chunk
            }
          }
        }
      }
      try { budgetResult.value = JSON.parse(budgetRaw.value) } catch { /* raw text, not JSON yet */ }
    } catch (e) {
      if (e.name !== 'AbortError') budgetError.value = e.message
    } finally { budgetStreaming.value = false }
  }

  async function _stream(url, method, body = null) {
    cancel()
    streaming.value = true; error.value = null
    rawBuffer.value = ''; days.value = []
    progress.value  = 'Connectant amb la IA...'
    controller.value = new AbortController()
    try {
      let currentToken = token()
      let res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream',
                   Authorization: `Bearer ${currentToken}` },
        body: body ? JSON.stringify(body) : null,
        signal: controller.value.signal
      })
      // Si token expirat, refresc automàtic i reintent (igual que l'interceptor Axios)
      if (res.status === 401) {
        currentToken = await _refreshToken()
        res = await fetch(url, {
          method,
          headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream',
                     Authorization: `Bearer ${currentToken}` },
          body: body ? JSON.stringify(body) : null,
          signal: controller.value.signal
        })
      }
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const reader = res.body.getReader()
      const dec    = new TextDecoder()
      let pending  = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        pending += dec.decode(value, { stream: true })
        // SSE messages are separated by blank lines (\n\n)
        const messages = pending.split('\n\n')
        pending = messages.pop() // keep incomplete last chunk
        for (const message of messages) {
          if (!message.trim()) continue
          _handleSseMessage(message)
        }
      }
    } catch (e) {
      if (e.name !== 'AbortError') { error.value = e.message; progress.value = 'Error en la generació' }
    } finally { streaming.value = false }
    return days.value
  }

  // Parse a full SSE message block (may contain event: and data: lines)
  function _handleSseMessage(message) {
    let eventType = null
    let dataLines = []
    for (const line of message.split('\n')) {
      if (line.startsWith('event:')) {
        eventType = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    }
    const raw = dataLines.join('\n')
    if (!raw) return
    _handle(raw, eventType)
  }

  function _handle(raw, sseEvent = null) {
    try {
      const e = JSON.parse(raw)
      // Resolve event type: prefer SSE event field, fall back to JSON type field
      const type = sseEvent || e.type
      if (type === 'start')              progress.value = e.message || 'Generant...'
      else if (type === 'chunk')       { rawBuffer.value += e.content || ''; progress.value = 'Escrivint...' }
      else if (type === 'day_complete' && e.day) { days.value = [...days.value, e.day]; progress.value = `Dia ${e.dayNumber} llest` }
      else if (type === 'complete')      progress.value = 'Itinerari completat'
      else if (type === 'error')         error.value = e.message
    } catch {
      // Plain-text chunk (non-JSON): treat as raw content appended to buffer
      if (sseEvent === 'chunk') rawBuffer.value += raw
    }
  }

  // ── Activity suggestions (independent state) ───────────────────────────────
  const suggestStreaming = ref(false)
  const suggestRaw      = ref('')
  const suggestError    = ref(null)
  const suggestResult   = ref([])
  const suggestCtrl     = ref(null)

  async function suggestActivities(tripId, dayNumber, category) {
    suggestCtrl.value?.abort()
    suggestStreaming.value = true
    suggestRaw.value = ''; suggestError.value = null; suggestResult.value = []
    suggestCtrl.value = new AbortController()
    try {
      const suggestUrl = `${base}/ai/trips/${tripId}/days/${dayNumber}/activities/suggest`
      let tk = token()
      let res = await fetch(suggestUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream',
                   Authorization: `Bearer ${tk}` },
        body: JSON.stringify({ category }),
        signal: suggestCtrl.value.signal,
      })
      if (res.status === 401) {
        tk = await _refreshToken()
        res = await fetch(suggestUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream',
                     Authorization: `Bearer ${tk}` },
          body: JSON.stringify({ category }),
          signal: suggestCtrl.value.signal,
        })
      }
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const reader = res.body.getReader()
      const dec    = new TextDecoder()
      let pending  = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        pending += dec.decode(value, { stream: true })
        const messages = pending.split('\n\n')
        pending = messages.pop()
        for (const msg of messages) {
          for (const line of msg.split('\n')) {
            if (!line.startsWith('data:')) continue
            const payload = line.slice(5).trim()
            if (!payload) continue
            try {
              const e = JSON.parse(payload)
              if (e.type === 'chunk')    suggestRaw.value += e.content || ''
              else if (e.type === 'error') suggestError.value = e.message
            } catch { suggestRaw.value += payload }
          }
        }
      }
      if (!suggestResult.value.length && suggestRaw.value) {
        try { suggestResult.value = JSON.parse(suggestRaw.value) } catch { /* raw text */ }
      }
    } catch (e) {
      if (e.name !== 'AbortError') suggestError.value = e.message
    } finally { suggestStreaming.value = false }
  }

  return { streaming: readonly(streaming), progress: readonly(progress),
           rawBuffer: readonly(rawBuffer), days: readonly(days), error: readonly(error),
           generate, refineDay, refineAll, editItinerary, cancel,
           budgetStreaming: readonly(budgetStreaming), budgetRaw: readonly(budgetRaw),
           budgetError: readonly(budgetError), budgetResult: readonly(budgetResult),
           estimateBudget,
           suggestStreaming: readonly(suggestStreaming), suggestRaw: readonly(suggestRaw),
           suggestError: readonly(suggestError), suggestResult: readonly(suggestResult),
           suggestActivities }
}
