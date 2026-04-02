package com.travelai.domain.auth;

import com.travelai.domain.auth.dto.*;
import com.travelai.domain.legal.AuditLog;
import com.travelai.domain.legal.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.gdpr.min-age:14}")
    private int minAge;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.lockout-minutes:15}")
    private int lockoutMinutes;

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        validateMinimumAge(request.birthDate());

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new IllegalArgumentException("Aquest email ja està en ús");
        }
        if (userRepository.existsByUsernameAndDeletedAtIsNull(request.username())) {
            throw new IllegalArgumentException("Aquest nom d'usuari ja està en ús");
        }

        User user = User.builder()
            .email(request.email().toLowerCase().strip())
            .passwordHash(passwordEncoder.encode(request.password()))
            .username(request.username())
            .birthDate(request.birthDate())
            .role(Role.USER)
            .active(true)
            .consentVersion(request.consentVersion())
            .consentAt(Instant.now())
            .build();

        user = userRepository.save(user);

        audit(user.getId(), "USER_REGISTER", httpRequest);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = createAndSaveRefreshToken(user.getId());

        log.info("Nou usuari registrat: {} ({})", user.getUsername(), user.getId());

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getUsername(), user.getRole().name());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.email().toLowerCase().strip();

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new BadCredentialsException("Credencials invàlides"));

        if (user.isLocked()) {
            audit(user.getId(), "LOGIN_BLOCKED", httpRequest);
            throw new LockedException("Compte bloquejat temporalment per massa intents fallits");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BadCredentialsException("Credencials invàlides");
        }

        if (!user.isAccountActive()) {
            throw new BadCredentialsException("Compte inactiu");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        audit(user.getId(), "USER_LOGIN", httpRequest);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = createAndSaveRefreshToken(user.getId());

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getUsername(), user.getRole().name());
    }

    @Transactional
    public void logout(UUID userId, String rawRefreshToken, HttpServletRequest httpRequest) {
        refreshTokenRepository.revokeAllByUserId(userId);
        audit(userId, "USER_LOGOUT", httpRequest);
        log.debug("Sessió tancada per usuari {}", userId);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
            .orElseThrow(() -> new BadCredentialsException("Refresh token invàlid o revocat"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new BadCredentialsException("Refresh token expirat");
        }

        // Validate JWT signature as well
        if (!jwtService.validateToken(request.refreshToken())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new BadCredentialsException("Refresh token invàlid");
        }

        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
            .orElseThrow(() -> new BadCredentialsException("Usuari no trobat"));

        if (!user.isAccountActive()) {
            throw new BadCredentialsException("Compte inactiu");
        }

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole());
        String newRefreshToken = createAndSaveRefreshToken(user.getId());

        return new AuthResponse(newAccessToken, newRefreshToken, user.getId(), user.getUsername(), user.getRole().name());
    }

    // --- Helpers ---

    private void validateMinimumAge(LocalDate birthDate) {
        if (birthDate == null) return;
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < minAge) {
            throw new IllegalArgumentException(
                "Has de tenir almenys " + minAge + " anys per registrar-te (LOPD-GDD Art. 7)"
            );
        }
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxLoginAttempts) {
            user.setLockedUntil(Instant.now().plusSeconds(lockoutMinutes * 60L));
            log.warn("Compte bloquejat per brute-force: {} ({} intents)", user.getEmail(), attempts);
        }

        userRepository.save(user);
    }

    private String createAndSaveRefreshToken(UUID userId) {
        String rawToken = jwtService.generateRefreshToken(userId);
        String tokenHash = hashToken(rawToken);

        RefreshToken entity = RefreshToken.builder()
            .userId(userId)
            .tokenHash(tokenHash)
            .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60L))
            .revoked(false)
            .createdAt(Instant.now())
            .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private void audit(UUID userId, String action, HttpServletRequest request) {
        try {
            AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .ipAddress(extractClientIp(request))
                .userAgent(request != null ? truncate(request.getHeader("User-Agent"), 512) : null)
                .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Audit failures must not break auth flow
            log.warn("Error guardant audit log per acció {}: {}", action, e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
