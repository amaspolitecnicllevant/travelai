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
