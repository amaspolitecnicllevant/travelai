package com.travelai.domain.trip.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ItineraryResponse(
    UUID id,
    Integer dayNumber,
    LocalDate date,
    List<DayPlan> plans,
    boolean generatedByAi,
    Integer version,
    Instant createdAt,
    Instant updatedAt
) {
    public record DayPlan(
        String time,
        String activity,
        String description,
        String location,
        String type
    ) {}
}
