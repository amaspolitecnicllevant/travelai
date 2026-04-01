import { ref } from 'vue'
import { useTripsStore } from '@/stores/trips'
import { tripsApi } from '@/api/trips'
import { useToast } from './useToast'

export function useTrips() {
  const store       = useTripsStore()
  const toast       = useToast()
  const pagination  = ref({ page: 0, size: 12, totalPages: 0, totalElements: 0 })
  const publicTrips = ref([])

  async function fetchMyTrips(params = {}) {
    store.loading = true
    try {
      const { data } = await tripsApi.getAll({ ...params, mine: true })
      store.trips = data.content || data
      if (data.totalPages !== undefined) {
        pagination.value = { page: data.number, size: data.size,
                             totalPages: data.totalPages, totalElements: data.totalElements }
      }
    } catch (e) { toast.error('Error carregant els viatges') }
    finally { store.loading = false }
  }

  async function fetchPublicTrips(params = {}) {
    store.loading = true
    try {
      const { data } = await tripsApi.getFeed(params)
      publicTrips.value = data.content || data
      if (data.totalPages !== undefined) {
        pagination.value = { page: data.number, size: data.size,
                             totalPages: data.totalPages, totalElements: data.totalElements }
      }
    } catch (e) { toast.error('Error carregant el feed') }
    finally { store.loading = false }
  }

  async function createTrip(payload) {
    try {
      const trip = await store.create(payload)
      toast.success('Viatge creat correctament!')
      return trip
    } catch (e) { toast.error('Error creant el viatge'); return null }
  }

  async function updateTrip(id, payload) {
    try {
      const trip = await store.update(id, payload)
      toast.success('Viatge actualitzat!')
      return trip
    } catch (e) { toast.error('Error actualitzant el viatge'); return null }
  }

  async function deleteTrip(id) {
    try {
      await tripsApi.remove(id)
      store.trips = store.trips.filter(t => t.id !== id)
      toast.success('Viatge eliminat')
      return true
    } catch (e) { toast.error('Error eliminant el viatge'); return false }
  }

  return { store, pagination, publicTrips,
           fetchMyTrips, fetchPublicTrips, createTrip, updateTrip, deleteTrip }
}
