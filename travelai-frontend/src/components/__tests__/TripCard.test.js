import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import TripCard from '../trip/TripCard.vue'

// Mock child component and composables
vi.mock('../trip/RatingStars.vue', () => ({
  default: { template: '<div class="rating-stars" />' },
}))

vi.mock('@/composables/useTrips', () => ({
  useTrips: () => ({ deleteTrip: vi.fn() }),
}))

vi.mock('@/api/trips', () => ({
  tripsApi: { duplicate: vi.fn() },
}))

// Minimal router to satisfy router-link
const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }],
})

const tripPublic = {
  id: '1',
  title: 'Viatge a Tokyo',
  destination: 'Tokyo, Japó',
  days: 7,
  visibility: 'PUBLIC',
  status: 'PUBLISHED',
  averageRating: null,
  coverImageUrl: null,
  author: { id: 99, username: 'altreuser' },
}

const tripPrivate = {
  ...tripPublic,
  id: '2',
  title: 'Viatge privat',
  visibility: 'PRIVATE',
}

function mountCard(trip, authUser = null) {
  const pinia = createPinia()
  setActivePinia(pinia)

  // Patch auth store state directly
  const wrapper = mount(TripCard, {
    props: { trip },
    global: {
      plugins: [pinia, router],
      stubs: { Transition: true },
    },
  })

  // Override auth store after mount if needed
  if (authUser) {
    const { useAuthStore } = require('@/stores/auth')
    const auth = useAuthStore()
    auth.user = authUser
    auth.accessToken = 'fake-token'
  }

  return wrapper
}

describe('TripCard', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renderitza el títol del trip', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const wrapper = mount(TripCard, {
      props: { trip: tripPublic },
      global: { plugins: [pinia, router], stubs: { Transition: true } },
    })
    expect(wrapper.text()).toContain('Viatge a Tokyo')
  })

  it('mostra el badge PRIVATE quan visibility = PRIVATE', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const wrapper = mount(TripCard, {
      props: { trip: tripPrivate },
      global: { plugins: [pinia, router], stubs: { Transition: true } },
    })
    expect(wrapper.text()).toContain('Privat')
    expect(wrapper.text()).not.toContain('Públic')
  })

  it('no mostra el menú d\'accions si no és el propietari', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)

    // Auth store with a different user
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    auth.user = { id: 1, username: 'differentuser' }
    auth.accessToken = 'fake-token'

    const wrapper = mount(TripCard, {
      props: { trip: tripPublic }, // author is 'altreuser' (id 99)
      global: { plugins: [pinia, router], stubs: { Transition: true } },
    })

    // The ··· menu button should not exist
    const menuButton = wrapper.find('button[title="Accions"]')
    expect(menuButton.exists()).toBe(false)
  })
})
