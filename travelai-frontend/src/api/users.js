import api from './index'

export const usersApi = {
  getMe:         ()           => api.get('/users/me'),
  updateMe:      (data)       => api.put('/users/me', data),
  getByUsername: (username)   => api.get(`/users/${username}`),
  follow:        (username)   => api.post(`/users/${username}/follow`),
  unfollow:      (username)   => api.delete(`/users/${username}/follow`),
  getTrips:      (username, params) => api.get(`/users/${username}/trips`, { params }),
  getStats:      (username)         => api.get(`/users/${username}/stats`),
  getFollowers:  (username, params) => api.get(`/users/${username}/followers`, { params }),
  getFollowing:  (username, params) => api.get(`/users/${username}/following`, { params }),
  uploadAvatar:  (formData)   => api.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
}
