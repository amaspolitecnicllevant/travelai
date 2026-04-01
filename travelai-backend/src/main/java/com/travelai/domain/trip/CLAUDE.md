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
