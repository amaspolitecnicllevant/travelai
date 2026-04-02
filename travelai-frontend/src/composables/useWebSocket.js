import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'
import { useToast } from '@/composables/useToast'

const client = ref(null)
const connected = ref(false)

export function useWebSocket() {
  const auth          = useAuthStore()
  const notifications = useNotificationsStore()
  const toast         = useToast()

  const wsUrl = (import.meta.env.VITE_WS_URL || 'ws://localhost/ws').replace(/^http/, 'ws')

  function connect() {
    if (!auth.isLoggedIn || connected.value) return

    client.value = new Client({
      brokerURL: wsUrl,
      connectHeaders: {
        Authorization: `Bearer ${auth.accessToken}`
      },
      reconnectDelay: 5000,
      onConnect: () => {
        connected.value = true
        client.value.subscribe('/user/queue/notifications', (message) => {
          try {
            const notification = JSON.parse(message.body)
            notifications.addNotification(notification)
            toast.info(notification.message || 'Nova notificació')
          } catch {
            // malformed message
          }
        })
      },
      onDisconnect: () => {
        connected.value = false
      },
      onStompError: () => {
        connected.value = false
      }
    })

    client.value.activate()
  }

  function disconnect() {
    if (client.value) {
      client.value.deactivate()
      client.value  = null
      connected.value = false
    }
  }

  return { connect, disconnect, connected }
}
