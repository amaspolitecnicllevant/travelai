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
