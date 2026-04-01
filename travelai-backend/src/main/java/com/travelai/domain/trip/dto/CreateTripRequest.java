package com.travelai.domain.trip.dto;

import com.travelai.domain.trip.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTripRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotBlank @Size(max = 200) String destination,
    LocalDate startDate,
    LocalDate endDate,
    Visibility visibility
) {
    public CreateTripRequest {
        // Privacy by Default (RGPD): si no s'especifica, PRIVATE
        if (visibility == null) {
            visibility = Visibility.PRIVATE;
        }
    }
}
