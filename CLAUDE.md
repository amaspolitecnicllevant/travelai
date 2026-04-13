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

## Model IA
- **Model actiu:** `gemma4:26b-a4b-it-q4_K_M` (Ollama local)
- **numCtx:** 8192 — NO augmentar, causa OOM
- Sistema de prompts en anglès concís

## Fases
1. ✅ MVP — Auth, CRUD viatges, IA itineraris, feed, valoracions, RGPD complet
   - Branca: main (commit 344df2f)
   - Totes les vistes implementades, V1–V22 Flyway, cap stub pendent
2. ✅ Social — Follows, feed personalitzat, comentaris, notificacions RT
   - Branques: feat/fase-2-sessio8, feat/fase-2-sessio9
   - ✅ Comentaris: Comment entitat, V23, GET/POST/DELETE /trips/{id}/comments, CommentSection.vue
   - ✅ Seguidors/seguint: GET /users/{username}/followers|following, FollowListModal.vue
   - ✅ Feed personalitzat: trips de follows primer (findPersonalizedFeed)
   - ✅ Notificacions RT: useWebSocket.js (STOMP/SockJS), NavBar badge+dropdown, useNotificationsStore
   - ✅ CommentSection: propietari viatge pot eliminar qualsevol comentari (tripOwnerUsername prop)
   - ✅ MyProfileView: stats seguidors/seguint clicables + FollowListModal
3. 🔄 IA avançada — Edició global, multi-turn, cerca web, clima
   - Branca activa: `feat/fase-3-sessio11`
   - ✅ EditorAgent global: useAiStream.editItinerary() → POST /ai/trips/{id}/edit
   - ✅ AiChatBox: prop useEditorAgent + historial de conversa multi-turn (bulles de chat)
   - ✅ TripPlannerView: botó + panel lateral "Editar itinerari amb IA"
   - ✅ ActivitySuggestions: fix SSE (era Axios), integrat al panel de refinament
   - ✅ useAiStream.suggestActivities() → SSE POST /ai/trips/{id}/days/{n}/activities/suggest
   - ✅ handleAddActivity a TripPlannerView: afegeix activitat suggeria al dia via itineraryStore
   - Pendent: integració clima (Open-Meteo) + cerca web (backend) als prompts de generació
4. ⬜ Producció — Docker multi-stage, HTTPS, CI/CD, auditoria RGPD completa
