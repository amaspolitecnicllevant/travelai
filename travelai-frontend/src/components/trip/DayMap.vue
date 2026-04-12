<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import 'leaflet/dist/leaflet.css'

const props = defineProps({
  activities: { type: Array, required: true },
  dayNumber:  { type: Number, required: true },
})

const mapContainer = ref(null)
const status       = ref('idle')   // idle | loading | ready | error
const errorMsg     = ref('')
let mapInstance    = null

const transportIcons = { WALK: '🚶', PUBLIC: '🚌', CAR: '🚗', TAXI: '🚕' }

async function geocode(query) {
  try {
    const res  = await fetch(
      `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`,
      { headers: { 'Accept-Language': 'ca,es,en', 'User-Agent': 'TravelAI/1.0' } }
    )
    const data = await res.json()
    if (!data.length) return null
    return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon) }
  } catch { return null }
}

async function getRoute(coords, mode) {
  try {
    const profile = mode === 'WALK' ? 'foot' : 'driving'
    const points  = coords.map(c => `${c.lng},${c.lat}`).join(';')
    const res  = await fetch(`https://router.project-osrm.org/route/v1/${profile}/${points}?overview=full&geometries=geojson`)
    const data = await res.json()
    if (data.routes?.[0]) return data.routes[0].geometry.coordinates.map(([lng, lat]) => [lat, lng])
  } catch { /* fallback */ }
  return null
}

async function initMap() {
  if (!props.activities?.length) return

  // Destroy previous instance
  if (mapInstance) { mapInstance.remove(); mapInstance = null }

  status.value = 'loading'
  errorMsg.value = ''

  try {
    // Wait for Vue to render the container (it's always visible now)
    await nextTick()
    if (!mapContainer.value) return

    const L = (await import('leaflet')).default

    delete L.Icon.Default.prototype._getIconUrl
    L.Icon.Default.mergeOptions({
      iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
      iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
      shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    })

    // Init map NOW while container is visible
    mapInstance = L.map(mapContainer.value, { zoomControl: true })
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      maxZoom: 19,
    }).addTo(mapInstance)

    // Geocode locations
    const locActivities = props.activities.filter(a => a.location)
    if (!locActivities.length) {
      errorMsg.value = 'Cap activitat té ubicació definida'
      status.value = 'error'
      return
    }

    const geocoded = []
    for (const act of locActivities) {
      const coord = await geocode(act.location)
      if (coord) geocoded.push({ ...coord, activity: act })
    }

    if (!geocoded.length) {
      errorMsg.value = 'No s\'han pogut localitzar les ubicacions'
      status.value = 'error'
      return
    }

    // Numbered markers
    geocoded.forEach((g, i) => {
      const icon = L.divIcon({
        className: '',
        html: `<div style="background:#4f46e5;color:white;border-radius:50%;width:28px;height:28px;
                           display:flex;align-items:center;justify-content:center;font-weight:bold;
                           font-size:12px;border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,.35)">
                 ${i + 1}
               </div>`,
        iconSize: [28, 28], iconAnchor: [14, 14],
      })
      const act = g.activity
      const transport = act.transportMode
        ? `${transportIcons[act.transportMode] || ''} ${act.travelTime || ''}`.trim()
        : ''
      L.marker([g.lat, g.lng], { icon })
        .addTo(mapInstance)
        .bindPopup(`
          <div style="min-width:150px;font-family:sans-serif">
            <b style="font-size:13px">${i+1}. ${act.name}</b>
            ${act.time ? `<div style="color:#6b7280;font-size:11px;margin-top:2px">${act.time}${act.endTime ? ` – ${act.endTime}` : ''}</div>` : ''}
            ${transport ? `<div style="color:#6b7280;font-size:11px">${transport}</div>` : ''}
            <div style="color:#9ca3af;font-size:10px;margin-top:3px">${act.location}</div>
          </div>`)
    })

    // Draw route
    if (geocoded.length >= 2) {
      const mode = geocoded.find(g => g.activity.transportMode)?.activity?.transportMode || 'CAR'
      const routeCoords = await getRoute(geocoded, mode)
      if (routeCoords) {
        L.polyline(routeCoords, { color: '#4f46e5', weight: 4, opacity: 0.65 }).addTo(mapInstance)
      } else {
        L.polyline(geocoded.map(g => [g.lat, g.lng]), {
          color: '#4f46e5', weight: 2, opacity: 0.5, dashArray: '6,6'
        }).addTo(mapInstance)
      }
    }

    const bounds = L.latLngBounds(geocoded.map(g => [g.lat, g.lng]))
    mapInstance.fitBounds(bounds, { padding: [30, 30] })
    mapInstance.invalidateSize()

    status.value = 'ready'
  } catch (e) {
    console.error('DayMap:', e)
    errorMsg.value = 'Error carregant el mapa'
    status.value = 'error'
  }
}

onMounted(initMap)
watch(() => props.activities, initMap, { deep: true })
onUnmounted(() => { if (mapInstance) { mapInstance.remove(); mapInstance = null } })
</script>

<template>
  <div class="rounded-xl border border-gray-200 overflow-hidden bg-white relative">

    <!-- Overlay de loading sobre el mapa -->
    <div
      v-if="status === 'loading'"
      class="absolute inset-0 z-10 flex items-center justify-center bg-white/80"
    >
      <div class="flex flex-col items-center gap-2 text-gray-400">
        <svg class="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
        </svg>
        <span class="text-sm">Carregant mapa...</span>
      </div>
    </div>

    <!-- Error -->
    <div v-if="status === 'error'" class="h-24 flex items-center justify-center">
      <p class="text-sm text-gray-400">{{ errorMsg }}</p>
    </div>

    <!-- Map container — SEMPRE visible perquè Leaflet pugui mesurar-lo -->
    <div
      ref="mapContainer"
      style="height:288px;width:100%"
      :style="status === 'error' ? 'display:none' : ''"
    />

    <!-- Legend -->
    <div v-if="status === 'ready'" class="px-3 py-2 bg-gray-50 border-t border-gray-100 flex flex-wrap gap-2">
      <span
        v-for="(act, i) in activities.filter(a => a.location)"
        :key="i"
        class="flex items-center gap-1 text-xs text-gray-500"
      >
        <span class="inline-flex items-center justify-center w-4 h-4 rounded-full bg-indigo-600 text-white text-[9px] font-bold shrink-0">{{ i+1 }}</span>
        {{ act.name }}
      </span>
    </div>
  </div>
</template>
