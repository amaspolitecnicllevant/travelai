import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const user        = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const accessToken = ref(localStorage.getItem('accessToken') || null)
  const loading     = ref(false)
  const error       = ref(null)

  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin    = computed(() => user.value?.role === 'ADMIN')

  async function login(email, password) {
    loading.value = true; error.value = null
    try {
      const { data } = await api.post('/auth/login', { email, password })
      _save(data); return true
    } catch (e) { error.value = e.response?.data?.message || 'Credencials incorrectes'; return false }
    finally { loading.value = false }
  }

  async function register(payload) {
    loading.value = true; error.value = null
    try {
      const { data } = await api.post('/auth/register', payload)
      _save(data); return true
    } catch (e) { error.value = e.response?.data?.message || 'Error en registrar-se'; return false }
    finally { loading.value = false }
  }

  async function logout() {
    try { await api.post('/auth/logout') } catch { /* ok */ } finally { _clear() }
  }

  async function fetchMe() {
    if (!accessToken.value) return
    try {
      const { data } = await api.get('/users/me')
      user.value = data
      localStorage.setItem('user', JSON.stringify(data))
    } catch { _clear() }
  }

  function _save({ token, accessToken: at, refreshToken: rt, userId, username, role }) {
    const resolvedToken = token || at
    const resolvedUser  = { id: userId, username, role }
    user.value = resolvedUser; accessToken.value = resolvedToken
    localStorage.setItem('user', JSON.stringify(resolvedUser))
    localStorage.setItem('accessToken', resolvedToken)
    localStorage.setItem('refreshToken', rt)
  }
  function _clear() {
    user.value = null; accessToken.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  return { user, accessToken, loading, error, isLoggedIn, isAdmin,
           login, register, logout, fetchMe }
})
