package com.travelai.domain.user.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de perfil d'usuari.
 * - email: null en perfils públics, informat en perfil propi
 * - following / isFollowing: informació social
 */
public record UserProfileResponse(
        UUID id,
        String username,
        String name,
        String bio,
        String avatarUrl,
        String email,          // null per a perfils públics
        long followersCount,
        long followingCount,
        long tripsCount,
        boolean isFollowing,   // si l'usuari autenticat segueix aquest perfil
        Instant createdAt
) {}
