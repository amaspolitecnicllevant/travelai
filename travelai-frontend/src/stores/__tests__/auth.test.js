import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../auth'

// Mock the API module
vi.mock('@/api', () => ({
  default: {
    post: vi.fn(),
    get:  vi.fn(),
  },
}))

import api from '@/api'

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('té l\'estat inicial correcte (isLoggedIn = false, user = null)', () => {
    const auth = useAuthStore()
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.user).toBeNull()
    expect(auth.accessToken).toBeNull()
  })

  it('login() actualitza l\'estat correctament amb mock de l\'API', async () => {
    const fakeUser = { id: 1, name: 'Test User', username: 'testuser', role: 'USER' }
    api.post.mockResolvedValueOnce({
      data: { user: fakeUser, accessToken: 'fake-at', refreshToken: 'fake-rt' },
    })

    const auth = useAuthStore()
    const result = await auth.login('test@example.com', 'password123')

    expect(result).toBe(true)
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.user).toEqual(fakeUser)
    expect(auth.accessToken).toBe('fake-at')
    expect(localStorage.getItem('accessToken')).toBe('fake-at')
  })

  it('login() retorna false i guarda l\'error si l\'API falla', async () => {
    api.post.mockRejectedValueOnce({
      response: { data: { message: 'Credencials incorrectes' } },
    })

    const auth = useAuthStore()
    const result = await auth.login('bad@example.com', 'wrong')

    expect(result).toBe(false)
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.error).toBe('Credencials incorrectes')
  })

  it('logout() neteja l\'estat i el localStorage', async () => {
    // Seed localStorage as if already logged in
    localStorage.setItem('accessToken', 'old-token')
    localStorage.setItem('refreshToken', 'old-rt')
    localStorage.setItem('user', JSON.stringify({ id: 1, name: 'Alice' }))

    api.post.mockResolvedValueOnce({}) // logout endpoint

    // Re-create store so it picks up localStorage values
    setActivePinia(createPinia())
    const auth = useAuthStore()

    await auth.logout()

    expect(auth.isLoggedIn).toBe(false)
    expect(auth.user).toBeNull()
    expect(auth.accessToken).toBeNull()
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })
})
