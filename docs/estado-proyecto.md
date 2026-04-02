# TravelAI — Informe de estado del proyecto

> Última actualització: 2026-04-02 (sessió 3 — v1.2.0)

---

## Resum executiu

L'aplicació és **completament funcional en el flux principal**: registre, login, creació de viatges i **generació d'itineraris amb IA** funcionen de cap a cap. El model `qwen2.5:7b` (Ollama local) genera itineraris en streaming, els parseja i els desa a PostgreSQL. El frontend mostra l'itinerari generat al TripPlannerView.

---

## Stack tecnològic

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

---

## Flux principal — Estat ✅ FUNCIONAL

| Pas | Estat | Notes |
|---|---|---|
| Registre (`POST /auth/register`) | ✅ | Validació GDPR, consentiment, edat mínima |
| Login (`POST /auth/login`) | ✅ | JWT access + refresh token |
| Logout (`POST /auth/logout`) | ✅ | |
| Crear viatge (amb o sense dates) | ✅ | Correccions V16+V17 (nullable trip_type, budget, start/end_date) |
| Generar itinerari IA (`POST /ai/trips/{id}/generate`) | ✅ | Streaming SSE, desa a BD al completar |
| Veure itinerari al planner | ✅ | Frontend recarrega de BD després de generar |
| Feed públic (`GET /trips/feed`) | ✅ | |

---

## Correccions aplicades (sessions 2–3)

### Infraestructura (sessió 3)
- **Docker snap → Docker CE (apt)**: resolt problema de permisos i bind mounts amb inodes obsolets
- **Nginx `nosniff` global**: eliminat (bloquejava Vite `/@vite/client` com `text/html`)
- **sockjs-client `global is not defined`**: eliminat; substituït per `brokerURL` nadiu de `@stomp/stompjs`

### Auth (sessió 3)
- **`JwtAuthFilter`**: ara estableix l'entitat `User` com a principal (no el `UUID`), corregint NPE en tots els endpoints que usaven `@AuthenticationPrincipal User user`
- **`AuthController.logout()`**: adaptat al nou principal `User`
- **`auth.js` store**: `_save()` compatible amb camps `token`/`accessToken` i estructura plana `userId/username/role`
- **`RegisterView.vue`**: camps GDPR enviats com a estructura plana (backend espera `privacyPolicyAccepted`, no `consents.privacy`)

### Base de dades (sessió 3)
| Migració | Descripció |
|---|---|
| `V9`–`V15` | Correccions d'esquema acumulades (ratings, refresh_tokens, trips, users, inet columns) |
| `V16` | `trip_type` i `budget` ara nullable (entitat `Trip` no els estableix) |
| `V17` | `start_date` i `end_date` ara nullable (creació sense dates) |

### IA (sessió 3)
- **`ItineraryParser`** reescrit completament:
  - Repara JSON truncat (compta `{[` i `]}` i tanca els que falten)
  - Mapeig flexible de camps: `day`/`dayNumber`, `cost`/`estimatedCost`, `name`/`activity`, etc.
  - Acepta formats alternatius que genera el model (activitats en castellà, camps variats)
- **`ItineraryResponse.DayPlan`**: afegit `@JsonAlias("name")` per `activity` i `@JsonAlias("category")` per `type`
- **`TripPlannerView`**: recarrega itinerari de BD (`fetchItinerary`) després de completar la generació
- **`itinerary.js` store**: transforma `List<ItineraryResponse>` (format backend) al format `{days:[{activities:[]}]}` que espera `ItineraryDay`

---

## Endpoints implementats i verificats

| Mètode | Endpoint | Estat |
|---|---|---|
| POST | `/api/v1/auth/register` | ✅ |
| POST | `/api/v1/auth/login` | ✅ |
| POST | `/api/v1/auth/logout` | ✅ |
| POST | `/api/v1/auth/refresh` | ✅ |
| GET | `/api/v1/users/me` | ✅ |
| POST | `/api/v1/trips` | ✅ |
| GET | `/api/v1/trips/feed` | ✅ |
| GET | `/api/v1/trips/{id}` | ✅ |
| PUT | `/api/v1/trips/{id}` | ✅ |
| DELETE | `/api/v1/trips/{id}` | ✅ |
| POST | `/api/v1/trips/{id}/publish` | ✅ |
| POST | `/api/v1/trips/{id}/unpublish` | ✅ |
| POST | `/api/v1/trips/{id}/duplicate` | ✅ |
| GET | `/api/v1/trips/{id}/itinerary` | ✅ |
| POST | `/api/v1/ai/trips/{id}/generate` | ✅ SSE + desa a BD |
| POST | `/api/v1/ai/trips/{id}/days/{day}/refine` | ✅ SSE |
| POST | `/api/v1/ai/trips/{id}/days/{day}/activities/suggest` | ✅ SSE |
| POST | `/api/v1/ai/trips/{id}/edit` | ✅ SSE |
| POST | `/api/v1/ai/trips/{id}/optimize-social` | ✅ SSE |
| POST | `/api/v1/ai/trips/{id}/refine-all` | ✅ SSE |
| GET | `/api/v1/ai/trips/{id}/budget-estimate` | ✅ SSE |
| POST | `/api/v1/trips/{id}/ratings` | ✅ |
| POST | `/api/v1/users/me/consent` | ✅ |
| GET | `/api/v1/legal/**` | ✅ |

---

## GDPR — Estat

| Requisit | Estat |
|---|---|
| Privacy by Default (viatges en PRIVATE) | ✅ |
| Consentiment explícit en registre | ✅ |
| Log de versió i timestamp de consentiment | ✅ |
| Esquema d'auditoria (`audit_logs`) | ✅ Esquema i servei implementats |
| Soft-delete amb purga als 30 dies | ✅ Scheduler implementat |
| Anti brute-force (bloqueig a 5 intents) | ✅ |
| Portabilitat (exportació JSON) | ✅ Endpoint implementat |
| Dret a l'oblit (sol·licitud d'esborrat) | ✅ Endpoint implementat |
| Vistes legals (/privacy, /terms, /cookies) | ⚠️ Placeholders (contingut pendent) |

---

## Frontend — Vistes

| Vista | Estat |
|---|---|
| `LoginView` | ✅ Funcional |
| `RegisterView` | ✅ Funcional (GDPR checkboxes) |
| `HomeView` | ✅ Landing + redirect si autenticat |
| `ExploreView` | ✅ Hero, cerca, destins populars |
| `CreateTripView` | ✅ Formulari complet (tipus, pressupost, dates, visibilitat) |
| `TripPlannerView` | ✅ Streaming SSE + visualització itinerari + refinament per dia |
| `TripDetailView` | ✅ Itinerari complet + ratings |
| `MyProfileView` | ⚠️ Esquelet parcialment implementat |
| `PublicProfileView` | ⚠️ Esquelet |
| `MyDataView` | ✅ GDPR: exportació + sol·licitud esborrat |
| Vistes legals | ⚠️ Placeholders (contingut a completar) |

---

## Tests — Estat

| Arxiu | Tipus | Tests |
|---|---|---|
| `AuthServiceTest.java` | Unitari (Mockito) | register, login, brute-force, locked account |
| `TripServiceTest.java` | Unitari (Mockito) | create, delete, duplicate, ownership check |
| `AuthControllerIT.java` | Integració (H2) | register 201, login 200, wrong credentials 401 |
| `auth.test.js` | Vitest | 3 tests store Pinia |
| `TripCard.test.js` | Vitest | 3 tests component |

---

## Versions publicades

| Versió | Data | Descripció |
|---|---|---|
| v1.0.0 | 2026-04-01 | Primera versió desplegada |
| v1.1.0 | 2026-04-01 | Frontend i backend funcionals |
| v1.2.0 | 2026-04-02 | **IA completament funcional**: generació, streaming i desament d'itineraris |

---

## Pendent de verificar (inici sessió 4)

Canvis fets al final de la sessió 3 que cal verificar manualment al navegador:

| Que cal verificar | On |
|---|---|
| Refinament de dia: dia actualitzat es mostra al planner després de refinar | `/trips/{id}/planner` → botó "Refinar" |
| TripCard menú 3 punts (Editar/Duplicar/Eliminar) apareix per al propietari | `/profile` → "Mis viajes" |
| Títol i resum de dia visibles a la capçalera de cada dia | `/trips/{id}/planner` i `/trips/{id}` |
| `DayRefinerAgent`: format `{title, activities}` desat correctament després de refinar | Verificar via API `/trips/{id}/itinerary` |
| TripDetailView per a visitants: error correcte en viatge PRIVATE | Accedir a `/trips/{id}` sense ser propietari |

---

## Prioritats per a la propera sessió

1. **Verificar ítems de la taula anterior** abans de continuar
2. **Vistes legals** — Completar contingut de `/privacy`, `/terms`, `/cookies`
3. **`MyProfileView`** — Verificar pestanya viatges i navegació al planner
4. **`PublicProfileView`** — Mostrar viatges públics d'un usuari
5. **`BudgetAgent` frontend** — Connectar botó "Estimar pressupost"
6. **Tests IA** — Tests unitaris per a `ItineraryParser` (JSON truncat, camps variats)
7. **Follows + notificacions** — Fase 2 social
