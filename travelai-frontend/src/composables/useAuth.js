import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { useToast } from './useToast'

export function useAuth() {
  const store  = useAuthStore()
  const router = useRouter()
  const toast  = useToast()

  async function login(email, password) {
    const ok = await store.login(email, password)
    if (ok) {
      toast.success('Benvingut/da!')
      const redirect = router.currentRoute.value.query.redirect || '/feed'
      router.push(redirect)
    } else {
      toast.error(store.error || 'Error en iniciar sessió')
    }
    return ok
  }

  async function register(payload) {
    const ok = await store.register(payload)
    if (ok) {
      toast.success('Compte creat correctament!')
      router.push('/feed')
    } else {
      toast.error(store.error || 'Error en registrar-se')
    }
    return ok
  }

  async function logout() {
    await store.logout()
    toast.info('Sessió tancada')
    router.push('/login')
  }

  return { store, login, register, logout }
}
