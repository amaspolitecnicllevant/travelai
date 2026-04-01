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
