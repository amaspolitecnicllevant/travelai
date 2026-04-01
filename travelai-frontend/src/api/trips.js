import api from './index'

export const tripsApi = {
  getAll:    (params) => api.get('/trips', { params }),
  getFeed:   (params) => api.get('/trips/feed', { params }),
  getById:   (id)     => api.get(`/trips/${id}`),
  create:    (data)   => api.post('/trips', data),
  update:    (id, d)  => api.put(`/trips/${id}`, d),
  remove:    (id)     => api.delete(`/trips/${id}`),
  publish:   (id)     => api.post(`/trips/${id}/publish`),
  unpublish: (id)     => api.post(`/trips/${id}/unpublish`),
  search:    (params) => api.get('/trips/search', { params }),
  duplicate: (id)     => api.post(`/trips/${id}/duplicate`),
}
