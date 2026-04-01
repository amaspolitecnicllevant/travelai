package com.travelai.domain.search;

import com.travelai.domain.trip.dto.TripResponse;
import com.travelai.domain.user.dto.UserProfileResponse;
import org.springframework.data.domain.Page;

/**
 * DTO de resposta per a la cerca global.
 */
public record SearchResponse(
        Page<UserProfileResponse> users,
        Page<TripResponse> trips
) {}
