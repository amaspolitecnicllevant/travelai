#!/usr/bin/env bash
# =============================================================
# TravelAI — Genera tots els CLAUDE.md del projecte
# v2 — Inclou domini legal/GDPR i bones pràctiques de privacitat
# Executar des de l'arrel del monorepo
# =============================================================
set -e

GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
log()  { echo -e "${GREEN}✓${NC} $1"; }
info() { echo -e "${CYAN}→${NC} $1"; }

ROOT="$(pwd)"
BACK="$ROOT/travelai-backend"
FRONT="$ROOT/travelai-frontend"
JAVA="$BACK/src/main/java/com/travelai"

# =============================================================
# CLAUDE.md ARREL
# =============================================================
info "Escrivint CLAUDE.md arrel..."
cat > "$ROOT/CLAUDE.md" <<'EOF'
# TravelAI — Context global del projecte

## Què és
Plataforma social de viatges amb IA local (Ollama). Els usuaris creen viatges,
reben itineraris generats per IA i els refinen conversacionalment dia a dia.
Compleix amb RGPD (UE 2016/679) i LOPD-GDD (Llei Orgànica 3/2018).

## Estructura del monorepo
```
travelai/
├── travelai-backend/     Java 21 + Spring Boot 3.3 + Spring AI
├── travelai-frontend/    Vue 3 + Tailwind CSS + Pinia
├── nginx/                Gateway reverse proxy
├── docs/legal/           Documents legals (plantilles)
├── docker-compose.yml
├── .env                  Variables locals (NO commitear)
├── requests.http         Peticions de prova
└── CLAUDE.md             Aquest arxiu
```

## Stack
| Capa | Tecnologia | Versió |
|---|---|---|
| Backend | Java + Spring Boot | 21 / 3.3.5 |
| IA local | Ollama + qwen2.5:7b | latest |
| IA client | Spring AI Ollama | 1.0.0 |
| Base de dades | PostgreSQL | 16 |
| Caché | Redis | 7 |
| Storage | MinIO | latest |
| Frontend | Vue 3 + Vite | 3.4 / 5.4 |
| CSS | Tailwind CSS | 3.4 |
| Estat | Pinia | 2.2 |
| Gateway | Nginx | 1.25 |

## Ports
| Servei | Port | Ús |
|---|---|---|
| nginx | 80 | Entrada única |
| backend | 8080 | API REST + SSE + WS |
| frontend | 5173 | Vue dev server |
| postgres | 5432 | BD principal |
| redis | 6379 | Caché + sessions |
| minio | 9000/9001 | Storage / consola |
| ollama | 11434 | Inferència IA |

## Credencials de desenvolupament
- Admin: admin@travelai.local / Admin1234!
- Demo:  demo@travelai.local / Demo1234!
- MinIO: http://localhost:9001 → minioadmin / minioadmin123

## Compliment legal (RGPD + LOPD-GDD)
- **Privacy by Default**: viatges en PRIVATE per defecte
- **Privacy by Design**: recollir només dades estrictament necessàries
- **Edat mínima**: 14 anys (LOPD-GDD Art. 7)
- **Consentiment explícit** al registre amb log de versió i timestamp
- **Dret a l'oblit**: soft-delete + purga als 30 dies
- **Portabilitat**: endpoint d'exportació JSON de dades de l'usuari
- **Auditoria**: taula audit_logs per a accions sensibles
- **Anti brute-force**: bloqueig de compte després de 5 intents fallits
- **Capçaleres de seguretat**: configurades a Nginx (HSTS, CSP, etc.)
- Documents legals a: /privacy, /terms, /cookies, /legal
- Gestió de dades pròpies: /my-data

## Convencions
- Codi en anglès, commits en català/castellà
- Format commits: `tipus(domini): descripció`
- Rames: main, develop, feat/nom, fix/nom

## Fases
1. MVP — Auth, CRUD viatges, IA itineraris, feed, valoracions
2. Social — Follows, feed personalitzat, cerca, notificacions RT
3. IA avançada — Multi-turn, cerca web, clima, pressupost
4. Producció — Stripe, Kubernetes, CDN, auditoria RGPD completa
EOF
log "CLAUDE.md arrel"

# =============================================================
# CLAUDE.md BACKEND
# =============================================================
info "Escrivint CLAUDE.md backend..."
cat > "$BACK/CLAUDE.md" <<'EOF'
# TravelAI Backend — Guia per a Claude Code

## Tecnologies
- Java 21 amb Virtual Threads
- Spring Boot 3.3.5
- Spring AI 1.0.0 amb Ollama (streaming SSE via Reactor Flux)
- Spring Security + JWT (jjwt 0.12.6) + OAuth2
- Spring Data JPA + PostgreSQL 16 + Flyway
- Redis (caché + sessions + Spring Session)
- MinIO per a storage
- Lombok + MapStruct
- Spring Retry (@Retryable) per a Ollama
- Spring Scheduling (@Scheduled) per a tasques GDPR

## Estructura de paquets
```
com.travelai/
├── TravelAiApplication.java     @EnableRetry @EnableAsync @EnableScheduling
├── config/
│   ├── SecurityConfig.java      CORS, JWT filter, OAuth2, rutas públiques
│   ├── JwtConfig.java           @ConfigurationProperties jwt.*
│   ├── RedisConfig.java         RedisTemplate + CacheManager
│   ├── MinioConfig.java         MinioClient bean
│   └── WebSocketConfig.java     STOMP sobre SockJS
├── domain/
│   ├── auth/                    Login, register, refresh, JWT
│   ├── user/                    User entity + perfil + follows
│   ├── trip/                    Trip, Day, Activity + CRUD
│   ├── ai/                      OllamaService + agents + parser
│   ├── rating/                  Valoracions 1-5
│   ├── notification/            Notificacions + WebSocket
│   └── legal/                   GDPR: consentiment, esborrat, exportació, auditoria
└── shared/
    ├── dto/                     PageResponse, etc.
    ├── exception/               GlobalExceptionHandler
    ├── mapper/                  Interfícies MapStruct
    ├── audit/                   AuditService (log d'accions sensibles)
    └── util/                    Helpers
```

## Convencions
- Records Java per a DTOs immutables
- @Transactional només a Service
- @AuthenticationPrincipal UserDetails als controllers
- Paginació amb Pageable + Page<T> → PageResponse<DTO>
- Enums amb @Enumerated(STRING)
- IDs UUID generats a PostgreSQL
- Timestamps TIMESTAMPTZ sempre

## Migracions Flyway
- V1__init_schema.sql — schema complet (inclou taules GDPR)
- V2__seed_data.sql   — usuaris demo + documents legals plantilla
- V3__rating_view.sql — vista trip_rating_summary
- Noves: V4__, V5__, etc. — mai modificar les ja aplicades

## Taules GDPR (V1)
- audit_logs         — registre d'accions sensibles (login, canvi pwd, exportació)
- consent_logs       — registre de consentiments acceptats (versió, data, IP)
- deletion_requests  — sol·licituds d'esborrat (estat PENDING/COMPLETED)
- login_attempts     — intents de login per anti brute-force
- legal_documents    — versions dels documents legals (privacitat, termes, cookies)

## Regles GDPR al codi
- Privacy by Default: Trip.visibility = PRIVATE per defecte
- Consentiment: RegisterRequest ha de tenir privacyPolicyAccepted=true i ageConfirmed=true
- Edat mínima: validar que ageConfirmed=true (edat mínima 14 anys configurable)
- Esborrat: soft-delete immediat + job @Scheduled que purga als 30 dies
- Exportació: /users/me/data-export retorna ZIP amb JSON de totes les dades
- Auditoria: AuditService.log() a totes les accions sensibles
- Bloqueig: comptar login_attempts, bloquejar després de 5 fallits en 15 min

## Configuració rellevant
```yaml
app.gdpr.deletion-days: 30
app.gdpr.log-retention-days: 365
app.gdpr.min-age: 14
app.gdpr.privacy-policy-version: "1.0"
app.security.max-login-attempts: 5
app.security.lockout-minutes: 15
```
EOF
log "travelai-backend/CLAUDE.md"

# =============================================================
# CLAUDE.md domini AI
# =============================================================
mkdir -p "$JAVA/domain/ai"
cat > "$JAVA/domain/ai/CLAUDE.md" <<'EOF'
# Domini AI — Guia d'implementació

## Arxius a crear
```
ai/
├── OllamaService.java        Ja creat — client Ollama base
├── AiException.java          Ja creat
├── AiController.java         Endpoints SSE
├── ItineraryAgent.java       Genera itinerari complet
├── DayRefinerAgent.java      Refina un dia amb prompt
├── BudgetAgent.java          Estima pressupost
├── ActivityAgent.java        Suggereix activitats
└── ItineraryParser.java      Parseja JSON del model
```

## Endpoints SSE
```
POST /api/v1/ai/trips/{tripId}/generate
POST /api/v1/ai/trips/{tripId}/days/{dayNumber}/refine  { prompt }
POST /api/v1/ai/trips/{tripId}/refine-all               { prompt }
GET  /api/v1/ai/trips/{tripId}/budget-estimate
```

## Format SSE
```
data: {"type":"start","message":"Generant..."}
data: {"type":"chunk","content":"text parcial"}
data: {"type":"day_complete","dayNumber":1,"day":{DayDTO}}
data: {"type":"complete"}
data: {"type":"error","message":"..."}
```

## System prompt crític (Ollama necessita instruccions explícites)
```
Ets un planificador de viatges expert.
NOMÉS respons amb JSON vàlid, sense text addicional, sense markdown,
sense blocs de codi. La resposta comença amb { i acaba amb }.
Exemple d'activitat correcta:
{"time":"10:00","title":"...","lat":35.67,"lng":139.65,"type":"SIGHTSEEING","estimatedCost":0,"currency":"EUR"}
```

## Temperatura per agent
| Agent | Temperatura | Motiu |
|---|---|---|
| ItineraryAgent | 0.7 | Varietat |
| DayRefinerAgent | 0.5 | Mantenir estructura |
| BudgetAgent | 0.2 | Precisió numèrica |
| ActivityAgent | 0.8 | Creativitat |

## GDPR a la capa AI
- Ollama és LOCAL → les dades dels usuaris NO surten del servidor
- No guardar prompts a la BD (dades personals implícites)
- Si en el futur es migra a Claude API: afegir DPA i actualitzar política de privacitat
EOF
log "domain/ai/CLAUDE.md"

# =============================================================
# CLAUDE.md domini LEGAL (nou)
# =============================================================
mkdir -p "$JAVA/domain/legal"
cat > "$JAVA/domain/legal/CLAUDE.md" <<'EOF'
# Domini Legal/GDPR — Guia d'implementació

## Responsabilitat
Gestiona tot el compliment RGPD/LOPD-GDD:
consentiments, dret a l'oblit, exportació de dades,
documents legals i auditoria d'accions sensibles.

## Arxius a crear
```
legal/
├── LegalController.java         GET /api/v1/legal/**
├── GdprController.java          POST /api/v1/users/me/consent|data-export|delete-request
├── GdprService.java             Lògica GDPR principal
├── AuditService.java            Log d'accions sensibles
├── DeletionScheduler.java       @Scheduled — purga comptes als 30 dies
├── ConsentLog.java              Entitat JPA
├── DeletionRequest.java         Entitat JPA
├── AuditLog.java                Entitat JPA
├── LegalDocument.java           Entitat JPA
├── ConsentLogRepository.java
├── DeletionRequestRepository.java
├── AuditLogRepository.java
├── LegalDocumentRepository.java
└── dto/
    ├── ConsentRequest.java      { type, version, accepted }
    ├── DataExportDTO.java       Totes les dades de l'usuari
    └── DeletionRequestDTO.java  { reason }
```

## Endpoints
```
GET  /api/v1/legal/privacy-policy    → LegalDocument actiu tipus PRIVACY_POLICY
GET  /api/v1/legal/terms             → LegalDocument actiu tipus TERMS
GET  /api/v1/legal/cookies           → LegalDocument actiu tipus COOKIES

POST /api/v1/users/me/consent        → Guardar ConsentLog
GET  /api/v1/users/me/data-export    → ZIP amb JSON de totes les dades (Art. 15+20)
POST /api/v1/users/me/delete-request → Crear DeletionRequest (Art. 17)
DELETE /api/v1/users/me/delete-request → Cancel·lar sol·licitud pendent
```

## GdprService — mètodes principals

### exportUserData(UUID userId)
Recull i serialitza:
- Dades de perfil (User)
- Tots els viatges, dies i activitats
- Valoracions emeses i rebudes
- Favorits
- Follows
- Logs de consentiment (sense IPs)
Retorna byte[] d'un ZIP amb un fitxer JSON

### requestDeletion(UUID userId, String reason)
1. Crear DeletionRequest amb scheduled_for = NOW() + 30 dies
2. Marcar user.active = false
3. Revocar tots els refresh tokens
4. Enviar email de confirmació
5. Guardar AuditLog

### DeletionScheduler — @Scheduled(cron = "0 2 * * *")
Cada nit a les 2:00 AM:
1. Buscar DeletionRequest on scheduled_for <= NOW() and status = PENDING
2. Per cada sol·licitud:
   a. Anonimitzar viatges públics (author → "Usuari eliminat")
   b. Esborrar dades personals (nom, email, bio, avatar)
   c. Esborrar fitxers de MinIO (avatar)
   d. Esborrar notificacions, favorits, follows
   e. Marcar DeletionRequest.status = COMPLETED
   f. Guardar AuditLog de l'esborrat

## AuditService
Cridar a:
- Login exitós i fallit
- Canvi de contrasenya
- Canvi d'email
- Exportació de dades
- Sol·licitud d'esborrat
- Canvi de visibilitat d'un viatge
- Accés admin a dades d'usuari

```java
auditService.log(userId, "USER_LOGIN", "user", userId, request);
auditService.log(userId, "DATA_EXPORT", "user", userId, request);
```

## Regles de retenció
- audit_logs: 365 dies (configurable via app.gdpr.log-retention-days)
- login_attempts: 90 dies
- consent_logs: durada del compte + 30 dies
- deletion_requests: 5 anys (prova de compliment)

## Anti brute-force (login_attempts)
Comprovar a AuthService abans de validar password:
1. Comptar intents fallits dels últims 15 min per email
2. Si >= 5: llençar LockedException
3. Guardar cada intent (èxit i fallada) a login_attempts
4. Guardar AuditLog si es bloqueja el compte
EOF
log "domain/legal/CLAUDE.md"

# =============================================================
# CLAUDE.md domini TRIP
# =============================================================
mkdir -p "$JAVA/domain/trip"
cat > "$JAVA/domain/trip/CLAUDE.md" <<'EOF'
# Domini Trip — Guia d'implementació

## Arxius a crear
```
trip/
├── Trip.java           Entitat JPA — visibility=PRIVATE per defecte (GDPR)
├── Day.java            Entitat JPA
├── Activity.java       Entitat JPA
├── TripType.java       Enum: CULTURAL, ADVENTURE, RELAX, GASTRONOMY, NATURE, CITY, BEACH
├── Budget.java         Enum: LOW, MEDIUM, HIGH, LUXURY
├── Visibility.java     Enum: PRIVATE, PUBLIC, FOLLOWERS
├── ActivityType.java   Enum: SIGHTSEEING, FOOD, TRANSPORT, ACCOMMODATION, ACTIVITY
├── TripRepository.java
├── TripService.java
├── TripController.java
├── TripMapper.java     MapStruct
└── dto/
    ├── CreateTripRequest.java   visibility default = PRIVATE
    ├── UpdateTripRequest.java
    ├── TripSummaryDTO.java
    ├── TripDetailDTO.java
    └── DayDTO.java
```

## GDPR al domini Trip
- **Privacy by Default**: visibility = PRIVATE per defecte a CreateTripRequest
- **Anonimització**: quan s'esborra un compte, els viatges públics passen a author="Usuari eliminat"
- **Auditoria**: cridar AuditService quan es publica o despublica un viatge

## Endpoints
```
POST   /api/v1/trips
GET    /api/v1/trips              Feed públic paginat
GET    /api/v1/trips/feed         Feed personalitzat (auth)
GET    /api/v1/trips/{id}
PUT    /api/v1/trips/{id}
DELETE /api/v1/trips/{id}
POST   /api/v1/trips/{id}/publish
POST   /api/v1/trips/{id}/unpublish
POST   /api/v1/trips/{id}/duplicate
GET    /api/v1/trips/search
```

## Regles de negoci
- Ownership check: AccessDeniedException si no és el propietari
- No publicar sense itinerari (aiGenerated = false → error)
- Duplicar: nou propietari, visibility = PRIVATE (privacy by default)
EOF
log "domain/trip/CLAUDE.md"

# =============================================================
# CLAUDE.md domini AUTH
# =============================================================
mkdir -p "$JAVA/domain/auth"
cat > "$JAVA/domain/auth/CLAUDE.md" <<'EOF'
# Domini Auth — Guia d'implementació

## Arxius a crear
```
auth/
├── AuthController.java
├── AuthService.java
├── JwtService.java
├── JwtAuthenticationFilter.java
├── RefreshToken.java
├── RefreshTokenRepository.java
└── dto/
    ├── RegisterRequest.java    Inclou camps GDPR
    ├── LoginRequest.java
    └── AuthResponse.java
```

## RegisterRequest — camps GDPR obligatoris
```java
public record RegisterRequest(
    @NotBlank String name,
    @NotBlank String username,
    @Email @NotBlank String email,
    @NotBlank @Size(min=8) String password,
    // GDPR — tots obligatoris
    @AssertTrue(message = "Has de confirmar que tens 14 anys o més")
    Boolean ageConfirmed,
    @AssertTrue(message = "Has d'acceptar la política de privacitat")
    Boolean privacyPolicyAccepted,
    @NotBlank String privacyPolicyVersion,
    @AssertTrue(message = "Has d'acceptar els termes d'ús")
    Boolean termsAccepted,
    @NotBlank String termsVersion,
    // Opcional
    Boolean marketingAccepted
) {}
```

## AuthService — lògica GDPR al registre
1. Validar RegisterRequest (inclosos camps GDPR)
2. Crear User amb age_verified = ageConfirmed
3. Guardar ConsentLog per privacyPolicy, terms (i marketing si acceptat)
4. Guardar AuditLog "USER_REGISTER"
5. Generar access + refresh tokens

## Anti brute-force
Abans de validar password:
1. Comptar login_attempts fallits dels últims 15 min per email
2. Si >= 5: LockedException (HTTP 423)
3. Guardar login_attempt (success=false)
4. Si login ok: guardar login_attempt (success=true) + AuditLog "USER_LOGIN"

## Rutas públiques (SecurityConfig)
```java
POST /api/v1/auth/**
GET  /api/v1/trips/**
GET  /api/v1/users/{username}
GET  /api/v1/legal/**
/actuator/health
/ws/**
```
EOF
log "domain/auth/CLAUDE.md"

# =============================================================
# CLAUDE.md domini USER
# =============================================================
mkdir -p "$JAVA/domain/user"
cat > "$JAVA/domain/user/CLAUDE.md" <<'EOF'
# Domini User — Guia d'implementació

## Arxius a crear
```
user/
├── User.java                Entitat + UserDetails + camp age_verified
├── UserRepository.java
├── UserService.java
├── UserController.java
├── Follow.java
├── FollowRepository.java
├── UserMapper.java
└── dto/
    ├── UserProfileDTO.java
    ├── UserPublicDTO.java
    ├── UserSummaryDTO.java
    └── UpdateProfileRequest.java
```

## Endpoints
```
GET    /api/v1/users/me
PUT    /api/v1/users/me
DELETE /api/v1/users/me        Soft-delete + crea DeletionRequest
GET    /api/v1/users/{username}
POST   /api/v1/users/{username}/follow
DELETE /api/v1/users/{username}/follow
GET    /api/v1/users/{username}/trips
GET    /api/v1/users/{username}/stats
```

## GDPR al domini User
- DELETE /users/me: posar active=false + cridar GdprService.requestDeletion()
- No retornar email ni dades sensibles a UserPublicDTO
- AuditService.log() a canvi de password i canvi d'email
EOF
log "domain/user/CLAUDE.md"

# =============================================================
# CLAUDE.md FRONTEND
# =============================================================
info "Escrivint CLAUDE.md frontend..."
cat > "$FRONT/CLAUDE.md" <<'EOF'
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
EOF
log "travelai-frontend/CLAUDE.md"

# =============================================================
# CLAUDE.md components
# =============================================================
cat > "$FRONT/src/components/CLAUDE.md" <<'EOF'
# Components — Guia d'implementació

## common/
```
BaseButton.vue       Props: label, variant(primary|secondary|danger), loading, disabled, size
BaseInput.vue        Props: modelValue, label, placeholder, error, type, required
BaseModal.vue        Props: show, title. Slots: default, footer
BaseCard.vue         Wrapper .card, slot default
BaseAvatar.vue       Props: src, name, size. Inicials si no hi ha imatge
BaseBadge.vue        Props: label, color
LoadingSpinner.vue   Props: size, fullscreen
EmptyState.vue       Props: title, description, actionLabel, actionTo
```

## trip/
```
TripCard.vue         Props: trip(TripSummaryDTO)
TripForm.vue         Props: initialData, loading. Emits: submit(formData)
DayEditor.vue        Props: day, tripId. Inclou AiPromptInput
ActivityCard.vue     Props: activity
RatingStars.vue      Props: score, interactive, count. Emits: rate(score)
```

## ai/
```
AiPromptInput.vue    Props: placeholder, loading. Emits: submit(prompt)
StreamingText.vue    Props: text, streaming. Cursor parpadeant
AiStatusBadge.vue    Props: status(idle|generating|complete|error)
```

## legal/ (GDPR — tots obligatoris)
```
CookieBanner.vue     Bàner inferior cookies. Usa consentStore
ConsentCheckbox.vue  Props: modelValue, label, linkText, linkTo, required, version
GdprFooter.vue       Footer amb tots els enllaços legals
```
EOF
log "src/components/CLAUDE.md"

cat > "$FRONT/src/views/trips/CLAUDE.md" <<'EOF'
# Vistes de Trips

## TripPlannerView.vue — vista central
Layout:
- Header: títol + destí + dates + botons (Generar IA, Publicar, Editar)
- Sidebar: llista de dies
- Panel: DayEditor + AiPromptInput + mapa bàsic

## CreateTripView.vue — 2 passos
1. Formulari metadades (visibility = PRIVATE per defecte — GDPR)
2. Redirect al planner amb botó "Generar amb IA"

## TripDetailView.vue — vista pública
- Itinerari complet (sense edició)
- RatingStars interactiu (si autenticat i no és el propietari)
- Botó "Desar en favorits"
EOF
log "src/views/trips/CLAUDE.md"

# =============================================================
# RESUM
# =============================================================
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  CLAUDE.md generats (v2 amb GDPR/LOPD)    ${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "  ${CYAN}CLAUDE.md${NC} arrel               — visió global + regles GDPR"
echo -e "  ${CYAN}travelai-backend/CLAUDE.md${NC}    — stack, paquets, taules GDPR"
echo -e "  ${CYAN}domain/ai/CLAUDE.md${NC}           — agents IA + nota privacitat"
echo -e "  ${CYAN}domain/legal/CLAUDE.md${NC}        — domini GDPR complet (NOU)"
echo -e "  ${CYAN}domain/trip/CLAUDE.md${NC}         — privacy by default"
echo -e "  ${CYAN}domain/auth/CLAUDE.md${NC}         — consentiment + brute-force"
echo -e "  ${CYAN}domain/user/CLAUDE.md${NC}         — soft-delete + GDPR"
echo -e "  ${CYAN}travelai-frontend/CLAUDE.md${NC}   — rutes legals + components GDPR"
echo -e "  ${CYAN}src/components/CLAUDE.md${NC}      — CookieBanner, ConsentCheckbox, GdprFooter"
echo -e "  ${CYAN}src/views/trips/CLAUDE.md${NC}     — privacy by default al crear viatge"
echo ""
