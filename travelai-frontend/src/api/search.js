import api from './index'

export const searchApi = {
  search: (q, type = 'all', params = {}) =>
    api.get('/search', { params: { q, type, ...params } }),
}
