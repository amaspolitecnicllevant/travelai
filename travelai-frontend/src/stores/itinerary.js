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
      currentItinerary.value = data
      return data
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
