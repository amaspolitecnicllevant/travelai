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
