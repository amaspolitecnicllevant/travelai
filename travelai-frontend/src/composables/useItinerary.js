import { useItineraryStore } from '@/stores/itinerary'
import { useAiStream } from './useAiStream'
import { useToast } from './useToast'

export function useItinerary() {
  const store  = useItineraryStore()
  const stream = useAiStream()
  const toast  = useToast()

  async function fetchItinerary(tripId) {
    const data = await store.fetchItinerary(tripId)
    if (!data) toast.error('Error carregant l\'itinerari')
    return data
  }

  async function saveItinerary(tripId) {
    const payload = store.currentItinerary
    if (!payload) return
    const data = await store.saveItinerary(tripId, payload)
    if (data) toast.success('Itinerari desat!')
    else toast.error('Error desant l\'itinerari')
    return data
  }

  async function generate(tripId) {
    toast.info('Generant itinerari amb IA...')
    const days = await stream.generate(tripId)
    if (stream.error.value) {
      toast.error('Error generant l\'itinerari: ' + stream.error.value)
    } else if (days?.length) {
      store.setDays(days)
      toast.success('Itinerari generat! Recorda desar-lo.')
    }
    return days
  }

  async function refineAll(tripId, prompt) {
    toast.info('Editant itinerari amb IA...')
    const days = await stream.refineAll(tripId, prompt)
    if (stream.error.value) {
      toast.error('Error editant: ' + stream.error.value)
    } else if (days?.length) {
      store.setDays(days)
      toast.success('Itinerari actualitzat!')
    }
    return days
  }

  async function refineDay(tripId, dayNumber, prompt) {
    toast.info(`Editant dia ${dayNumber}...`)
    const days = await stream.refineDay(tripId, dayNumber, prompt)
    if (stream.error.value) {
      toast.error('Error editant el dia: ' + stream.error.value)
    } else if (days?.length) {
      store.setDays(days)
      toast.success(`Dia ${dayNumber} actualitzat!`)
    }
    return days
  }

  return { store, stream, fetchItinerary, saveItinerary, generate, refineAll, refineDay }
}
