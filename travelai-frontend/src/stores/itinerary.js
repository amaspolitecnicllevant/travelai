import { defineStore } from 'pinia'
import { ref } from 'vue'
import { itineraryApi } from '@/api/itinerary'

export const useItineraryStore = defineStore('itinerary', () => {
  const currentItinerary = ref(null)
  const loading          = ref(false)
  const error            = ref(null)

  async function fetchItinerary(tripId) {
    loading.value = true; error.value = null
    try {
      const { data } = await itineraryApi.get(tripId)
      // Backend returns List<ItineraryResponse>: [{dayNumber, date, plans:[{time,activity,description,location,type}]}]
      // Transform to the format ItineraryDay expects: [{dayNumber, date, title, activities:[{time,name,...}]}]
      const days = (Array.isArray(data) ? data : []).map(d => ({
        dayNumber: d.dayNumber,
        date:      d.date,
        title:     d.title || `Dia ${d.dayNumber}`,
        activities: (d.plans || d.activities || []).map(p => ({
          time:        p.time,
          name:        p.activity || p.name || p.title,
          description: p.description,
          location:    p.location,
          type:        p.type,
          duration:    p.duration,
          cost:        p.cost,
        })),
        generatedByAi: d.generatedByAi,
      }))
      currentItinerary.value = { days }
      return currentItinerary.value
    } catch (e) { error.value = e.message; return null }
    finally { loading.value = false }
  }

  async function saveItinerary(tripId, payload) {
    loading.value = true; error.value = null
    try {
      const { data } = await itineraryApi.save(tripId, payload)
      currentItinerary.value = data
      return data
    } catch (e) { error.value = e.message; return null }
    finally { loading.value = false }
  }

  function setDays(days) {
    if (!currentItinerary.value) currentItinerary.value = { days: [] }
    currentItinerary.value.days = days
  }

  function clear() {
    currentItinerary.value = null
    error.value = null
  }

  return { currentItinerary, loading, error, fetchItinerary, saveItinerary, setDays, clear }
})
