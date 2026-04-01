package com.travelai.domain.user.dto;

/**
 * DTO returned by GET /api/v1/users/{username}/stats.
 * Contains aggregated social and activity stats for a user profile.
 */
public record UserStatsResponse(
        long tripsCount,
        double avgRating,
        long followersCount,
        long followingCount
) {}
