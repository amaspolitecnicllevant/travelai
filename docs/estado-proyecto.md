# TravelAI — Informe de estado del proyecto

> Generado el 2026-04-01

---

## Resumen ejecutivo

El proyecto tiene una **arquitectura excelente y bien estructurada** (DDD, Flyway, JWT, SSE, Docker todo en orden). Lo que hizo cada agente en la primera sesión fue construir el **andamiaje completo** — la base está lista. Lo que falta es la **capa de negocio de IA** (el controller y los agentes) que es el corazón de la aplicación, más completar los flujos GDPR y las vistas del frontend.

---

## Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Backend | Java + Spring Boot | 21 / 3.3.5 |
| IA local | Ollama + qwen2.5:7b | latest |
| IA client | Spring AI Ollama | 1.0.0 |
| Base de datos | PostgreSQL | 16 |
| Caché | Redis | 7 |
| Storage | MinIO | latest |
| Frontend | Vue 3 + Vite | 3.4 / 5.4 |
| CSS | Tailwind CSS | 3.4 |
| Estado | Pinia | 2.2 |
| Gateway | Nginx | 1.25 |

---

## Lo que está completamente construido

### BackendBuilderAgent

- Auth completo (JWT + refresh tokens, anti-brute-force, registro con consentimiento GDPR)
- CRUD de viajes (create/read/update/delete con soft-delete y privacy-by-default)
- Modelo de datos completo via Flyway (4 migraciones: esquema core, seed data, vista de ratings, tabla itinerarios)
- Configuración Redis, MinIO, SecurityConfig
- Infraestructura SSE lista (`OllamaService` con `Flux<String>`, retry logic) — **sin controlador REST todavía**
- Entidades GDPR: `AuditLog`, `ConsentLog`, `DataDeletionRequest`, `LegalDocument` con esquema en BD

### DevOpsAgent

- Docker Compose con 8 servicios y health checks en todos
- Nginx con cabeceras de seguridad (CSP, HSTS, X-Frame-Options, Referrer-Policy) y rutas SSE/WS configuradas
- Dockerfiles multi-stage para backend (Java 21, Virtual Threads) y frontend (Node 20 Alpine)

### FrontendBuilderAgent / FrontendVueAgent

- 16 vistas Vue 3 con Composition API
- 9 componentes reutilizables (NavBar, TripCard, RatingStars, AiChatBox, etc.)
- 5 stores Pinia: `auth`, `trips`, `itinerary`, `consent`, `ui`
- Capa API completa con interceptor de refresh automático de JWT
- Router con 16 rutas y guards de autenticación (requiresAuth / guestOnly)
- `useAiStream.js` — consumidor SSE listo para conectar con el backend

---

## Endpoints que funcionan

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/auth/register` | Registro con validación de consentimiento GDPR |
| POST | `/api/v1/auth/login` | Login con anti-brute-force |
| POST | `/api/v1/auth/logout` | Cierre de sesión |
| POST | `/api/v1/auth/refresh` | Renovación de tokens |
| POST | `/api/v1/trips` | Crear viaje (visibility=PRIVATE por defecto) |
| GET | `/api/v1/trips` | Listar viajes del usuario (paginado) |
| GET | `/api/v1/trips/public` | Listar viajes públicos (paginado) |
| GET | `/api/v1/trips/{id}` | Obtener viaje (con verificación de propietario) |
| PUT | `/api/v1/trips/{id}` | Actualizar viaje |
| DELETE | `/api/v1/trips/{id}` | Soft-delete de viaje |
| GET | `/api/v1/trips/{id}/itinerary` | Obtener días y actividades |
| POST | `/api/v1/trips/{id}/ratings` | Valorar un viaje |

---

## Gaps críticos — Lo que falta

### 1. IA — Feature principal

| Qué | Descripción | Estado |
|---|---|---|
| `AiController` | Endpoints SSE `/api/v1/ai/trips/{id}/generate` y `/days/{day}/refine` | ✅ Implementado |
| `ItineraryAgent` | Genera itinerario completo por días en JSON y lo persiste | ✅ Implementado |
| `DayRefinerAgent` | Refina un día específico según prompt del usuario | ✅ Implementado |
| `BudgetAgent` | Estima costes y ajusta el itinerario al presupuesto | Pendiente |
| `ActivityAgent` | Sugiere actividades adicionales | Pendiente |

**Endpoints implementados:**

```
POST /api/v1/ai/trips/{id}/generate          → SSE generación de itinerario completo  ✅
POST /api/v1/ai/trips/{id}/days/{day}/refine → Refinar día específico                 ✅
POST /api/v1/ai/trips/{id}/refine-all        → Refinamiento en lote                   Pendiente
GET  /api/v1/ai/trips/{id}/budget-estimate   → Análisis de costes                     Pendiente
```

**Archivos nuevos creados:**
- `domain/ai/AiController.java`
- `domain/ai/agents/ItineraryAgent.java`
- `domain/ai/agents/DayRefinerAgent.java`

### 2. Trips — Ciclo de vida

| Endpoint | Estado |
|---|---|
| `POST /api/v1/trips/{id}/publish` | ✅ Implementado |
| `POST /api/v1/trips/{id}/unpublish` | ✅ Implementado |
| `POST /api/v1/trips/{id}/duplicate` | ✅ Implementado (crea copia con `visibility=PRIVATE` y título "Copia de {nombre}") |
| `GET /api/v1/trips/feed` | Pendiente — Feed personalizado |

### 3. GDPR — Esquema listo, lógica de negocio pendiente

| Qué falta | Descripción |
|---|---|
| `GdprService` | Exportación de datos, programación de borrado |
| `AuditService` | Utilidad de logging (el esquema `audit_logs` existe en BD) |
| `DeletionScheduler` | Job `@Scheduled` para purga de cuentas a los 30 días |
| Endpoints legales | `GET /api/v1/legal/privacy-policy`, `/terms`, `/cookies` |
| `GET /api/v1/users/me/data-export` | Exportación ZIP de datos del usuario |
| `POST /api/v1/users/me/delete-request` | Solicitud de borrado de cuenta |

### 4. Frontend — Vistas sin implementar

| Vista / Componente | Estado actual |
|---|---|
| `TripPlannerView` | Esqueleto sin integración con SSE |
| `AiChatBox` | Parcial, sin conectar con `useAiStream` |
| `PrivacyPolicyView`, `TermsView`, `CookiePolicyView`, `LegalNoticeView`, `MyDataView` | Placeholders vacíos |
| `MyProfileView`, `PublicProfileView` | Esqueletos sin implementar |
| Cookie banner / Consent checkbox | No construidos |

---

## Cumplimiento GDPR — Estado

| Requisito | Estado |
|---|---|
| Privacy by Default (viajes en PRIVATE) | Implementado |
| Consentimiento explícito en registro | Implementado |
| Log de versión y timestamp de consentimiento | Implementado |
| Esquema de auditoría (`audit_logs`) | Esquema listo, servicio pendiente |
| Soft-delete con purga a 30 días | Soft-delete implementado, scheduler pendiente |
| Anti brute-force (bloqueo a 5 intentos) | Esquema implementado |
| Portabilidad (exportación JSON) | Pendiente |
| Derecho al olvido (solicitud borrado) | Pendiente |
| Vistas legales (/privacy, /terms, /cookies) | Placeholders vacíos |

---

## Prioridad sugerida para la próxima sesión

1. **`AiController` + agentes de itinerario** → desbloquea el feature principal de la plataforma
2. **Endpoints publish/unpublish/duplicate** → completa el ciclo de vida de los viajes
3. **`TripPlannerView` con streaming UI** → conecta el frontend con la IA via SSE
4. **`GdprService` + `DeletionScheduler`** → completa el cumplimiento legal
5. **Vistas legales** → documentos servidos desde BD + cookie banner

---

## Estructura de archivos relevante

```
travelai/
├── travelai-backend/
│   └── src/main/java/.../
│       ├── auth/           ✅ Completo
│       ├── trip/           ✅ publish/unpublish/duplicate implementados
│       ├── ai/             ✅ AiController + ItineraryAgent + DayRefinerAgent implementados
│       ├── legal/          ❌ Entidades y esquema listos, sin Service ni Controller
│       └── config/         ✅ Completo
├── travelai-frontend/
│   └── src/
│       ├── api/            ✅ Completo
│       ├── stores/         ✅ Completo
│       ├── composables/    ✅ Completo (useAiStream listo para conectar)
│       ├── views/          ⚠️  Auth y Trips OK, AI/Legal/Profile pendientes
│       └── components/     ⚠️  TripCard/NavBar OK, AiChatBox parcial
├── nginx/                  ✅ Completo
├── docker-compose.yml      ✅ Completo
└── docs/
    └── legal/              ❌ Vacío
```

---

*Generado por análisis estático del código fuente del proyecto.*
