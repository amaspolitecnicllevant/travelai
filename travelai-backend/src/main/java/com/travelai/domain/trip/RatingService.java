package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import com.travelai.domain.trip.dto.RatingRequest;
import com.travelai.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TripService tripService;

    @Transactional
    public void rateTrip(UUID tripId, RatingRequest request, User requester) {
        Trip trip = tripService.findActiveOrThrow(tripId);

        if (trip.getVisibility() == Visibility.PRIVATE
                && !trip.getOwner().getId().equals(requester.getId())) {
            throw new AccessDeniedException("No pots valorar un viatge privat");
        }

        Rating rating = ratingRepository.findByTripAndUser(trip, requester)
            .map(existing -> {
                existing.setScore(request.score());
                existing.setComment(request.comment());
                return existing;
            })
            .orElseGet(() -> Rating.builder()
                .trip(trip)
                .user(requester)
                .score(request.score())
                .comment(request.comment())
                .build());

        ratingRepository.save(rating);
    }

    @Transactional(readOnly = true)
    public double getAverageRating(UUID tripId) {
        Trip trip = tripService.findActiveOrThrow(tripId);
        return ratingRepository.averageScoreByTrip(trip).orElse(0.0);
    }
}
