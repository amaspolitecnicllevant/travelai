package com.travelai.domain.trip.dto;

import java.util.List;

public record UpdateItineraryDayRequest(
    String title,
    List<ActivityRequest> activities
) {
    public record ActivityRequest(
        String time,
        String endTime,
        String name,
        String description,
        String location,
        Double cost,
        String category,
        String transportMode,
        String travelTime
    ) {}
}
