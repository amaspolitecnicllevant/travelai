package com.travelai.domain.auth.dto;

import java.util.UUID;

public record AuthResponse(
    String token,
    String refreshToken,
    UUID userId,
    String username,
    String role
) {}
