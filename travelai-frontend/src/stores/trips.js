import { defineStore } from 'pinia'
import { ref } from 'vue'
import { tripsApi } from '@/api/trips'

export const useTripsStore = defineStore('trips', () => {
  const trips   = ref([])
  const current = ref(null)
  const loading = ref(false)
  const error   = ref(null)

  async function fetchFeed(params = {}) {
    loading.value = true
    try { const { data } = await tripsApi.getFeed(params); trips.value = data.content }
    catch (e) { error.value = e.message }
    finally { loading.value = false }
  }

  async function fetchById(id) {
    loading.value = true
    try { const { data } = await tripsApi.getById(id); current.value = data; return data }
    catch (e) { error.value = e.message; return null }
    finally { loading.value = false }
  }

  async function create(payload) {
    const { data } = await tripsApi.create(payload)
    return data
  }

  async function update(id, payload) {
    const { data } = await tripsApi.update(id, payload)
    if (current.value?.id === id) current.value = data
    return data
  }

  return { trips, current, loading, error, fetchFeed, fetchById, create, update }
})
