package com.travelai.domain.trip.dto;

import com.travelai.domain.trip.TripStatus;
import com.travelai.domain.trip.Visibility;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TripResponse(
    UUID id,
    String title,
    String description,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    Visibility visibility,
    TripStatus status,
    String coverImageUrl,
    String ownerUsername,
    Double averageRating,
    Instant createdAt,
    Instant updatedAt
) {}
