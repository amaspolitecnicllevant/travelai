package com.travelai.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
    @NotBlank(message = "El refresh token és obligatori")
    String refreshToken
) {}
