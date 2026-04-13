import { ref } from 'vue'

const PHOTON_URL = 'https://photon.komoot.io/api/'

export function useDestinationAutocomplete() {
  const suggestions = ref([])
  const loading     = ref(false)
  const error       = ref(null)

  let debounceTimer = null

  function search(query) {
    clearTimeout(debounceTimer)
    suggestions.value = []
    error.value = null

    if (!query || query.trim().length < 2) {
      loading.value = false
      return
    }

    loading.value = true
    debounceTimer = setTimeout(() => _fetch(query.trim()), 300)
  }

  async function _fetch(query) {
    try {
      const params = new URLSearchParams({ q: query, limit: 6, lang: 'en' })
      const res = await fetch(`${PHOTON_URL}?${params}`)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      suggestions.value = (data.features || [])
        .map(_toSuggestion)
        .filter(Boolean)
        .filter((s, i, arr) => arr.findIndex(x => x.label === s.label) === i) // deduplicate
    } catch (e) {
      error.value = e.message
      suggestions.value = []
    } finally {
      loading.value = false
    }
  }

  function clear() {
    clearTimeout(debounceTimer)
    suggestions.value = []
    loading.value = false
    error.value = null
  }

  return { suggestions, loading, error, search, clear }
}

// ── helpers ──────────────────────────────────────────────────────────────────

function _toSuggestion(feature) {
  const p = feature.properties || {}
  const coords = feature.geometry?.coordinates // [lon, lat]

  // Build a human-readable label: "City, State, Country" or "City, Country"
  const parts = [p.name, p.state, p.country].filter(Boolean)
  if (!parts.length) return null

  // Deduplicate consecutive identical parts (e.g. "Paris, Paris, France")
  const deduped = parts.filter((v, i) => i === 0 || v !== parts[i - 1])

  return {
    label: deduped.join(', '),
    name:  p.name    || deduped[0],
    city:  p.city    || p.name,
    state: p.state   || null,
    country: p.country || null,
    lat: coords ? coords[1] : null,
    lon: coords ? coords[0] : null,
    type: p.osm_value || p.type || null,
  }
}
