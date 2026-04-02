package com.travelai.domain.trip.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ItineraryResponse(
    UUID id,
    Integer dayNumber,
    LocalDate date,
    String title,
    List<DayPlan> plans,
    boolean generatedByAi,
    Integer version,
    Instant createdAt,
    Instant updatedAt
) {
    public record DayPlan(
        String time,
        @JsonAlias("name") String activity,
        String description,
        String location,
        @JsonAlias("category") String type
    ) {}
}
