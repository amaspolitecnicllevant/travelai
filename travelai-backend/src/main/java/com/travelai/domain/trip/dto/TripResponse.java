package com.travelai.domain.trip.dto;

import com.travelai.domain.trip.TripStatus;
import com.travelai.domain.trip.Visibility;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TripResponse(
    UUID id,
    String title,
    String description,
    String destination,
    LocalDate startDate,
    LocalDate endDate,
    String arrivalTime,
    String departureTime,
    String arrivalLocation,
    String accommodationAddress,
    String preferredTransport,
    String budget,
    String budgetLevel,
    List<String> tripTypes,
    Integer durationDays,
    Visibility visibility,
    TripStatus status,
    String coverImageUrl,
    String ownerUsername,
    Double averageRating,
    Instant createdAt,
    Instant updatedAt
) {}
