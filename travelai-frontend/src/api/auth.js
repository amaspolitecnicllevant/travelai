import api from './index'

export const authApi = {
  login:          (data)  => api.post('/auth/login', data),
  register:       (data)  => api.post('/auth/register', data),
  logout:         ()      => api.post('/auth/logout'),
  refresh:        (rt)    => api.post('/auth/refresh', { refreshToken: rt }),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  resetPassword:  (data)  => api.post('/auth/reset-password', data),
}
