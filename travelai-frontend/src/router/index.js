import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/',                   name: 'home',           component: () => import('@/views/HomeView.vue') },
  { path: '/explore',            name: 'explore',        component: () => import('@/views/ExploreView.vue') },
  { path: '/trips/:id',          name: 'trip-detail',    component: () => import('@/views/trips/TripDetailView.vue') },
  { path: '/profile/:username',  name: 'public-profile', component: () => import('@/views/profile/PublicProfileView.vue') },
  { path: '/login',              name: 'login',          component: () => import('@/views/auth/LoginView.vue'),    meta: { guestOnly: true } },
  { path: '/register',           name: 'register',       component: () => import('@/views/auth/RegisterView.vue'), meta: { guestOnly: true } },
  { path: '/feed',               name: 'feed',           component: () => import('@/views/FeedView.vue'),          meta: { requiresAuth: true } },
  { path: '/trips/new',          name: 'trip-create',    component: () => import('@/views/trips/CreateTripView.vue'),  meta: { requiresAuth: true } },
  { path: '/trips/:id/edit',     name: 'trip-edit',      component: () => import('@/views/trips/EditTripView.vue'),    meta: { requiresAuth: true } },
  { path: '/trips/:id/planner',  name: 'trip-planner',   component: () => import('@/views/trips/TripPlannerView.vue'), meta: { requiresAuth: true } },
  { path: '/profile',            name: 'my-profile',     component: () => import('@/views/profile/MyProfileView.vue'), meta: { requiresAuth: true } },
  // Pàgines legals — sempre públiques
  { path: '/privacy',            name: 'privacy',        component: () => import('@/views/legal/PrivacyPolicyView.vue') },
  { path: '/terms',              name: 'terms',          component: () => import('@/views/legal/TermsView.vue') },
  { path: '/cookies',            name: 'cookies',        component: () => import('@/views/legal/CookiePolicyView.vue') },
  { path: '/legal',              name: 'legal-notice',   component: () => import('@/views/legal/LegalNoticeView.vue') },
  { path: '/my-data',            name: 'my-data',        component: () => import('@/views/legal/MyDataView.vue'), meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*',    name: 'not-found',      component: () => import('@/views/NotFoundView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: (to, from, saved) => saved || { top: 0 }
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) next({ name: 'login', query: { redirect: to.fullPath } })
  else if (to.meta.guestOnly && auth.isLoggedIn) next({ name: 'feed' })
  else next()
})

export default router
