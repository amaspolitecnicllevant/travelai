package com.travelai.domain.trip.dto;

import com.travelai.domain.trip.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateTripRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @NotBlank @Size(max = 200) String destination,
    LocalDate startDate,
    LocalDate endDate,
    String arrivalTime,
    String departureTime,
    String arrivalLocation,
    String accommodationAddress,
    String preferredTransport,
    List<String> tripTypes,
    String budget,
    String budgetLevel,
    Visibility visibility
) {
    public CreateTripRequest {
        if (visibility == null) {
            visibility = Visibility.PRIVATE;
        }
    }
}
