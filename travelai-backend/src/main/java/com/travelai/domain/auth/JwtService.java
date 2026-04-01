package com.travelai.domain.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    // Access token: 15 minutes
    private static final long ACCESS_EXPIRATION_MS = 15 * 60 * 1000L;
    // Refresh token: 7 days
    private static final long REFRESH_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.secret:dev-secret-change-in-prod-please-use-env-var}")
    private String secret;

    public String generateAccessToken(UUID userId, Role role) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_ROLE, role.name())
            .claim(CLAIM_TYPE, TYPE_ACCESS)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION_MS))
            .signWith(getSigningKey())
            .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim(CLAIM_TYPE, TYPE_REFRESH)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION_MS))
            .signWith(getSigningKey())
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expirat: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("Signatura JWT invàlida");
        } catch (MalformedJwtException e) {
            log.warn("JWT mal format");
        } catch (JwtException e) {
            log.warn("JWT invàlid: {}", e.getMessage());
        }
        return false;
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = getClaims(token);
            return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public Role extractRole(String token) {
        String roleName = getClaims(token).get(CLAIM_ROLE, String.class);
        return Role.valueOf(roleName);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        try {
            // Derive a 256-bit key via SHA-256 to ensure minimum key length for HS256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
