import api from './index'

export const legalApi = {
  getPrivacyPolicy:  ()       => api.get('/legal/privacy-policy'),
  getTerms:          ()       => api.get('/legal/terms'),
  getCookiePolicy:   ()       => api.get('/legal/cookies'),
  saveConsent:       (data)   => api.post('/users/me/consent', data),
  exportMyData:      ()       => api.get('/users/me/data-export', { responseType: 'blob' }),
  requestDeletion:   (reason) => api.post('/users/me/delete-request', { reason }),
  cancelDeletion:    ()       => api.delete('/users/me/delete-request'),
}
