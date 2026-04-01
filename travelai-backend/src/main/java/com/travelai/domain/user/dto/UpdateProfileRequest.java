package com.travelai.domain.user.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO per actualitzar el perfil de l'usuari autenticat.
 * Tots els camps són opcionals (patch parcial).
 */
public record UpdateProfileRequest(
        @Size(max = 100) String name,
        @Size(max = 500) String bio
) {}
