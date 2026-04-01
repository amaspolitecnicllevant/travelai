package com.travelai.domain.ai;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record GenerateItineraryRequest(
        @NotNull UUID tripId,
        @NotBlank @Size(max = 200) String destination,
        @Min(1) @Max(30) int days,
        List<@NotBlank String> preferences,
        @Pattern(regexp = "ca|es|en", message = "Idioma ha de ser ca, es o en") String language
) {
    public GenerateItineraryRequest {
        if (language == null || language.isBlank()) language = "ca";
        if (preferences == null) preferences = List.of();
    }
}
