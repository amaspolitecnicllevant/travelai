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
      const days = (Array.isArray(data) ? data : []).map(d => {
        const activities = (d.plans || d.activities || []).map(p => ({
          time:          p.time,
          endTime:       p.endTime,
          name:          p.activity || p.name || p.title,
          description:   p.description,
          location:      p.location,
          type:          p.type || p.category,
          duration:      p.duration,
          cost:          p.cost ?? p.estimatedCost,
          transportMode: p.transportMode,
          travelTime:    p.travelTime,
        }))
        const summary = activities.slice(0, 3).map(a => a.name).filter(Boolean).join(' · ')
        return {
          dayNumber:    d.dayNumber,
          date:         d.date,
          title:        d.title || `Dia ${d.dayNumber}`,
          description:  summary || null,
          activities,
          generatedByAi: d.generatedByAi,
        }
      })
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

  async function updateDay(tripId, dayNumber, dayData) {
    loading.value = true; error.value = null
    try {
      const payload = {
        title: dayData.title,
        activities: dayData.activities.map(a => ({
          time:          a.time,
          endTime:       a.endTime,
          name:          a.name,
          description:   a.description,
          location:      a.location,
          cost:          a.cost ? parseFloat(a.cost) : 0,
          category:      a.type || 'LEISURE',
          transportMode: a.transportMode,
          travelTime:    a.travelTime,
        }))
      }
      await itineraryApi.updateDay(tripId, dayNumber, payload)
      // update local state
      if (currentItinerary.value?.days) {
        const idx = currentItinerary.value.days.findIndex(d => d.dayNumber === dayNumber)
        if (idx !== -1) currentItinerary.value.days[idx] = { ...currentItinerary.value.days[idx], ...dayData }
      }
      return true
    } catch (e) { error.value = e.message; return false }
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

  return { currentItinerary, loading, error, fetchItinerary, saveItinerary, updateDay, setDays, clear }
})
