package com.travelai.domain.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EditItineraryRequest(
        @NotNull UUID tripId,
        @NotBlank String currentContentJson,
        @NotBlank @Size(max = 1000) String userPrompt
) {}
