import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

let isRefreshing = false
let queue = []
const drain = (err, token) => { queue.forEach(p => err ? p.reject(err) : p.resolve(token)); queue = [] }

api.interceptors.response.use(res => res, async err => {
  const orig = err.config
  if (err.response?.status !== 401 || orig._retry) return Promise.reject(err)
  if (isRefreshing) return new Promise((res, rej) => queue.push({ resolve: res, reject: rej }))
    .then(t => { orig.headers.Authorization = `Bearer ${t}`; return api(orig) })
  orig._retry = true
  isRefreshing = true
  const rt = localStorage.getItem('refreshToken')
  if (!rt) { isRefreshing = false; localStorage.clear(); window.location = '/login'; return Promise.reject(err) }
  try {
    const { data } = await axios.post(`${api.defaults.baseURL}/auth/refresh`, { refreshToken: rt })
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    drain(null, data.accessToken)
    orig.headers.Authorization = `Bearer ${data.accessToken}`
    return api(orig)
  } catch (e) { drain(e, null); localStorage.clear(); window.location = '/login'; return Promise.reject(e) }
  finally { isRefreshing = false }
})

export default api
