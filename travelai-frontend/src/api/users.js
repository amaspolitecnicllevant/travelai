import api from './index'

export const usersApi = {
  getMe:         ()           => api.get('/users/me'),
  updateMe:      (data)       => api.put('/users/me', data),
  getByUsername: (username)   => api.get(`/users/${username}`),
  follow:        (username)   => api.post(`/users/${username}/follow`),
  unfollow:      (username)   => api.delete(`/users/${username}/follow`),
  getTrips:      (username)   => api.get(`/users/${username}/trips`),
  getStats:      (username)   => api.get(`/users/${username}/stats`),
  uploadAvatar:  (formData)   => api.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
}
