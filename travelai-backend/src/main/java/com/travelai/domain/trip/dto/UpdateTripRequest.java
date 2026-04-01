package com.travelai.domain.trip.dto;

import com.travelai.domain.trip.TripStatus;
import com.travelai.domain.trip.Visibility;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTripRequest(
    @Size(max = 200) String title,
    @Size(max = 2000) String description,
    @Size(max = 200) String destination,
    LocalDate startDate,
    LocalDate endDate,
    Visibility visibility,
    TripStatus status,
    String coverImageUrl
) {}
