import api from './index'

export const itineraryApi = {
  get:      (tripId)             => api.get(`/trips/${tripId}/itinerary`),
  save:     (tripId, data)       => api.put(`/trips/${tripId}/itinerary`, data),
  updateDay:(tripId, day, data)  => api.put(`/trips/${tripId}/itinerary/${day}`, data),
  generate: (tripId)         => `/api/v1/ai/trips/${tripId}/generate`,
  refineAll:(tripId)         => `/api/v1/ai/trips/${tripId}/refine-all`,
  refineDay:(tripId, day)    => `/api/v1/ai/trips/${tripId}/days/${day}/refine`,
  budget:   (tripId, data)   => api.post(`/ai/trips/${tripId}/budget`, data),
  enrich:   (tripId)         => api.post(`/ai/trips/${tripId}/enrich`),
  rate:          (tripId, score)             => api.post(`/trips/${tripId}/ratings`, { score }),
  getRatings:    (tripId)                    => api.get(`/trips/${tripId}/ratings`),
  getComments:   (tripId, params)            => api.get(`/trips/${tripId}/comments`, { params }),
  addComment:    (tripId, data)              => api.post(`/trips/${tripId}/comments`, data),
  deleteComment: (tripId, commentId)         => api.delete(`/trips/${tripId}/comments/${commentId}`),
}
