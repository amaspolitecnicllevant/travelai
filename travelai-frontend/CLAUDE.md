# TravelAI Frontend — Guia per a Claude Code

## Tecnologies
- Vue 3 Composition API (<script setup>) — sense Options API
- Vite 5, Tailwind CSS 3, Pinia, Vue Router 4, Axios, @vueuse/core

## Estructura src/
```
src/
├── api/
│   ├── index.js      Axios + interceptors JWT auto-refresh
│   ├── auth.js       authApi
│   ├── trips.js      tripsApi
│   ├── users.js      usersApi
│   └── legal.js      legalApi (GDPR: consent, export, deletion)
├── composables/
│   └── useAiStream.js  SSE consumer (ja implementat)
├── stores/
│   ├── auth.js       Usuari + tokens
│   ├── trips.js      Estat de viatges
│   └── consent.js    Bàner de cookies + consentiment
├── router/index.js   Rutes + guards (inclou rutes legals)
├── views/
│   ├── auth/         LoginView, RegisterView
│   ├── trips/        TripDetailView, CreateTripView, TripPlannerView...
│   ├── profile/      MyProfileView, PublicProfileView
│   └── legal/        PrivacyPolicyView, TermsView, CookiePolicyView,
│                     LegalNoticeView, MyDataView (ja creades com a placeholder)
└── components/
    ├── common/       BaseButton, BaseInput, BaseModal...
    ├── trip/         TripCard, DayEditor...
    ├── ai/           AiPromptInput, StreamingText...
    └── legal/        CookieBanner, ConsentCheckbox, GdprFooter
```

## Rutes legals (ja al router)
- /privacy   → PrivacyPolicyView
- /terms     → TermsView
- /cookies   → CookiePolicyView
- /legal     → LegalNoticeView
- /my-data   → MyDataView (requiresAuth)

## Components legals a crear (legal/)

### CookieBanner.vue
Bàner inferior que apareix si !consentStore.cookiesAccepted
Botons: "Acceptar totes" / "Rebutjar no essencials"
Enllaç a /cookies

### ConsentCheckbox.vue
Props: modelValue, label, required, version
Emits: update:modelValue
Checkbox amb text legal i enllaç al document corresponent
Mostrar en el formulari de registre

### GdprFooter.vue
Footer amb enllaços a /privacy, /terms, /cookies, /legal, /my-data
Ha d'aparèixer a totes les pàgines

## Formulari de registre — camps GDPR obligatoris
```vue
<!-- A RegisterView.vue, camps addicionals obligatoris: -->
<ConsentCheckbox
  v-model="form.privacyPolicyAccepted"
  label="He llegit i accepto la"
  link-text="Política de Privacitat"
  link-to="/privacy"
  :required="true"
  version="1.0"
/>
<ConsentCheckbox
  v-model="form.termsAccepted"
  label="Accepto els"
  link-text="Termes d'Ús"
  link-to="/terms"
  :required="true"
  version="1.0"
/>
<ConsentCheckbox
  v-model="form.ageConfirmed"
  label="Confirmo que tinc 14 anys o més"
  :required="true"
/>
```

## Variables d'entorn
```
VITE_API_BASE_URL    URL base API
VITE_WS_URL          URL WebSocket
VITE_APP_NAME        Nom de l'app
VITE_GDPR_MIN_AGE    Edat mínima (14)
```

## Convencions
- <script setup> sempre
- defineProps amb tipus explícits
- No Options API, no this
- Pinia per a estat global
- Tailwind utility-first
- Classes .btn-primary, .btn-secondary, .input, .card, .checkbox-label a main.css
