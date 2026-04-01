# TravelAI — Guia d'scripts

## Visió general

El projecte té 4 scripts que cobreixen tot el cicle de desenvolupament.
Cada script té una responsabilitat concreta i un moment específic d'ús.

```
carpeta-pare/
├── launch.sh                 ← s'executa des d'AQUÍ
└── travelai/
    ├── init-script.sh        ← s'executa des d'AQUÍ
    ├── claude_md_script.sh   ← s'executa des d'AQUÍ
    └── parallel_agents.sh    ← s'executa des d'AQUÍ
```

---

## Cicle de vida

```
DIA 1 (única vegada)              CADA DIA
─────────────────────             ──────────────────────────────────────
1. init-script.sh          →      4. launch.sh --start
2. claude_md_script.sh     →      3. parallel_agents.sh
3. launch.sh (setup)
```

---

## Script 1 — `init-script.sh`

**Ubicació**: dins de `travelai/`
**Quan**: una sola vegada, el primer dia, abans de res

### Què fa
Crea des de zero tota l'estructura del projecte amb tots els fitxers de
configuració necessaris perquè pugui arrancar.

### Fitxers que genera
| Fitxer | Descripció |
|---|---|
| `docker-compose.yml` | Defineix els 7 serveis Docker |
| `.env` | Variables d'entorn (passwords, JWT, etc.) |
| `.env.example` | Plantilla pública del .env |
| `.gitignore` | Fitxers a ignorar pel git |
| `nginx/nginx.dev.conf` | Configuració del gateway |
| `travelai-backend/Dockerfile` | Imatge Docker del backend |
| `travelai-backend/pom.xml` | Dependències Java (Spring Boot, Spring AI, JWT...) |
| `src/main/resources/application.yml` | Configuració completa del backend |
| `db/migration/V1__init_schema.sql` | Schema de la base de dades (inclou taules GDPR) |
| `db/migration/V2__seed_data.sql` | Usuaris de prova i documents legals plantilla |
| `db/migration/V3__rating_view.sql` | Vista de valoracions |
| `TravelAiApplication.java` | Classe principal Spring Boot |
| `OllamaService.java` | Client Ollama amb streaming |
| `GlobalExceptionHandler.java` | Gestió centralitzada d'errors |
| `travelai-frontend/Dockerfile.dev` | Imatge Docker del frontend |
| `package.json` | Dependències Vue (Vue 3, Pinia, Tailwind...) |
| `vite.config.js` | Configuració del bundler |
| `tailwind.config.js` | Configuració de Tailwind CSS |
| `src/main.js` + `App.vue` | Punt d'entrada de l'app Vue |
| `src/assets/main.css` | Estils base + classes Tailwind reutilitzables |
| `src/api/*.js` | Mòduls Axios (auth, trips, users, legal) |
| `src/stores/*.js` | Stores Pinia (auth, trips, consent) |
| `src/composables/useAiStream.js` | Consumer SSE per al streaming d'Ollama |
| `src/router/index.js` | Rutes amb guards d'autenticació |
| `src/views/**/*.vue` | Totes les vistes com a placeholders |
| `travelai.code-workspace` | Workspace VSCode (3 carpetes) |
| `.vscode/tasks.json` | Tasques ràpides (agents, Docker, tests) |
| `requests.http` | Peticions de prova per a Thunder Client |

### Execució
```bash
cd travelai
chmod +x init-script.sh
./init-script.sh
```

### ⚠️ Avís
Si es torna a executar, sobreescriu els fitxers existents.
**Executar només una vegada.**

---

## Script 2 — `claude_md_script.sh`

**Ubicació**: dins de `travelai/`
**Quan**: una vegada després de `init-script.sh`. Tornar a executar si canvien les convencions del projecte.

### Què fa
Genera fitxers `CLAUDE.md` a cada carpeta del projecte. Claude Code els llegeix
automàticament quan obre una carpeta, donant-li el context necessari per
implementar codi correctament sense necessitat d'explicar res cada vegada.

### CLAUDE.md que genera
| Fitxer | Contingut |
|---|---|
| `CLAUDE.md` (arrel) | Visió global, stack, ports, credencials, fases, regles GDPR |
| `travelai-backend/CLAUDE.md` | Estructura Java, convencions, taules GDPR, Flyway |
| `domain/auth/CLAUDE.md` | JWT, registre amb consentiment, anti brute-force |
| `domain/trip/CLAUDE.md` | Entitats JPA, queries, privacy by default |
| `domain/ai/CLAUDE.md` | Agents Ollama, system prompts, format SSE |
| `domain/legal/CLAUDE.md` | Tot el GDPR: esborrat, exportació, auditoria, scheduler |
| `domain/user/CLAUDE.md` | User entity, follows, soft-delete |
| `travelai-frontend/CLAUDE.md` | Vue 3, Pinia, rutes legals, components GDPR |
| `src/components/CLAUDE.md` | Catàleg de components amb props i emits |
| `src/views/trips/CLAUDE.md` | Layout del TripPlannerView |

### Execució
```bash
cd travelai
chmod +x claude_md_script.sh
./claude_md_script.sh
```

### ⚠️ Avís
No sobreescriu codi Java ni Vue, només els fitxers `CLAUDE.md`.
Es pot tornar a executar de manera segura.

---

## Script 3 — `parallel_agents.sh`

**Ubicació**: dins de `travelai/`
**Quan**: cada dia quan es comença a programar

### Prerequisits
- `tmux` instal·lat (`brew install tmux` / `apt install tmux`)
- `claude` instal·lat (`npm install -g @anthropic-ai/claude-code`)

### Què fa
Obre una sessió tmux amb 7 panells simultanis. Cada panell té Claude Code
ja posicionat a la carpeta del seu domini i amb el `CLAUDE.md` carregat.
És com tenir 7 programadors especialitzats treballant en paral·lel.

### Els 7 panells
| Panell | Nom | Carpeta | Responsabilitat |
|---|---|---|---|
| 0 | AUTH | `domain/auth/` | Login, registre, JWT, refresh tokens |
| 1 | TRIP | `domain/trip/` | CRUD viatges, dies, activitats |
| 2 | AI | `domain/ai/` | Agents Ollama, streaming SSE |
| 3 | LEGAL | `domain/legal/` | GDPR: consentiment, esborrat, exportació |
| 4 | FRONTEND | `frontend/src/` | Components Vue, vistes, stores |
| 5 | INFRA | `resources/` | Migracions Flyway, configuració |
| 6 | MONITOR | arrel | Logs de Docker en temps real |

### Execució
```bash
cd travelai
chmod +x parallel_agents.sh
./parallel_agents.sh

# Connectar-se a la sessió
tmux attach -t travelai-agents
```

### Navegació tmux
| Drecera | Acció |
|---|---|
| `Ctrl+b` + fletxes | Moure's entre panells |
| `Ctrl+b z` | Zoom al panell actual (tornar a prémer per desfer) |
| `Ctrl+b d` | Desconnectar (els agents continuen en segon pla) |
| `Ctrl+b [` | Mode scroll (q per sortir) |

### Ordre recomanat per als agents
1. Llançar **AUTH** primer i esperar que creï `User.java`
2. Llançar **TRIP**, **AI**, **LEGAL**, **FRONTEND** i **INFRA** en paral·lel

### ⚠️ Avís
Cada cop que es tanca el terminal o s'apaga l'ordinador, cal tornar a
executar aquest script per recuperar els agents.

---

## Script 4 — `launch.sh`

**Ubicació**: a la carpeta **PARE** de `travelai/` (un nivell per sobre)
**Quan**: cada dia per arrancar l'aplicació i veure-la al navegador

### Prerequisits
- Docker Desktop corrent
- `docker-compose` instal·lat
- Node.js instal·lat

### Què fa
Gestiona tot el cicle de vida de l'aplicació: arranca els contenidors Docker,
verifica que tots els serveis estiguin sans, comprova si el model d'Ollama
ja està descarregat i obre els agents. És el script del dia a dia.

### Opcions
| Opció | Quan usar-la |
|---|---|
| `./launch.sh` | **Primera vegada**: setup complet (instal·la deps, descarrega model, arranca tot) |
| `./launch.sh --start` | **Cada dia**: arranca tots els contenidors (ja instal·lat) |
| `./launch.sh --stop` | Quan vols alliberar RAM i parar tot |
| `./launch.sh --restart` | Si alguna cosa s'ha penjat |
| `./launch.sh --logs` | Per veure els logs en temps real mentre es programa |
| `./launch.sh --status` | Per saber si tot funciona correctament |
| `./launch.sh --agents` | Per obrir els agents sense haver d'arrancar tot |
| `./launch.sh --help` | Veure totes les opcions |

### Execució
```bash
# Des de la carpeta PARE (la que conté travelai/)
chmod +x launch.sh

# Primera vegada
./launch.sh

# Les vegades següents
./launch.sh --start
```

### Passos que executa la primera vegada (`./launch.sh`)
1. Comprova prerequisits (Docker, Node, tmux)
2. Verifica que existeix `travelai/docker-compose.yml`
3. Crea `.env` si no existeix
4. Executa `npm install` al frontend (només primera vegada)
5. Arranca postgres, redis, minio i ollama
6. Espera que postgres i redis estiguin sans
7. Comprova si el model Ollama ja és descarregat; si no, el descarrega (~4.5 GB)
8. Arranca backend, frontend i nginx
9. Espera que el backend respongui al health check
10. Obre els agents tmux
11. Mostra totes les URLs i credencials

### URLs de l'aplicació
| Servei | URL |
|---|---|
| App | http://localhost |
| API | http://localhost/api/v1 |
| Health | http://localhost:8080/actuator/health |
| MinIO consola | http://localhost:9001 |
| Ollama | http://localhost:11434 |
| Política de privacitat | http://localhost/privacy |
| Termes d'ús | http://localhost/terms |

### Credencials de desenvolupament
| Usuari | Email | Password |
|---|---|---|
| Admin | admin@travelai.local | Admin1234! |
| Demo | demo@travelai.local | Demo1234! |
| MinIO | minioadmin | minioadmin123 |

### ⚠️ Avís
És l'únic script que s'executa des de la carpeta **PARE**,
no des de dins de `travelai/`.

---

## Resum — flux complet

### Primera vegada (dia 1)
```bash
# 1. Crear la carpeta i entrar-hi
mkdir travelai && cd travelai

# 2. Copiar els scripts dins de travelai/ i donar permisos
chmod +x init-script.sh claude_md_script.sh parallel_agents.sh

# 3. Crear l'estructura del projecte
./init-script.sh

# 4. Generar els CLAUDE.md pels agents
./claude_md_script.sh

# 5. Tornar a la carpeta pare i arrancar tot
cd ..
chmod +x launch.sh
./launch.sh
```

### Cada dia (a partir del dia 2)
```bash
# Des de la carpeta pare
./launch.sh --start

# Connectar-se als agents
tmux attach -t travelai-agents
# o si no estaven oberts:
./launch.sh --agents
```

### Final de la jornada
```bash
# Des de la carpeta pare
./launch.sh --stop
```

---

## Compliment legal — nota important

Els documents legals generats (`/privacy`, `/terms`, `/cookies`) contenen
**plantilles** que cal substituir per text real redactat per un advocat
especialitzat en RGPD/LOPD abans del llançament de l'aplicació.

Contact GDPR: `privacidad@travelai.local`
DPO: `dpo@travelai.local`
