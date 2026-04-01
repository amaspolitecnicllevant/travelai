package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import com.travelai.domain.trip.dto.CreateTripRequest;
import com.travelai.domain.trip.dto.TripResponse;
import com.travelai.domain.trip.dto.UpdateTripRequest;
import com.travelai.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final RatingRepository ratingRepository;

    @Transactional
    public TripResponse createTrip(CreateTripRequest request, User owner) {
        Trip trip = Trip.builder()
            .owner(owner)
            .title(request.title())
            .description(request.description())
            .destination(request.destination())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .visibility(request.visibility()) // PRIVATE per defecte (Privacy by Default)
            .status(TripStatus.DRAFT)
            .build();

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request, User requester) {
        Trip trip = findActiveOrThrow(tripId);
        assertOwner(trip, requester);

        if (request.title() != null)        trip.setTitle(request.title());
        if (request.description() != null)  trip.setDescription(request.description());
        if (request.destination() != null)  trip.setDestination(request.destination());
        if (request.startDate() != null)    trip.setStartDate(request.startDate());
        if (request.endDate() != null)      trip.setEndDate(request.endDate());
        if (request.visibility() != null)   trip.setVisibility(request.visibility());
        if (request.status() != null)       trip.setStatus(request.status());
        if (request.coverImageUrl() != null) trip.setCoverImageUrl(request.coverImageUrl());

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public void deleteTrip(UUID tripId, User requester) {
        Trip trip = findActiveOrThrow(tripId);
        assertOwner(trip, requester);
        trip.setDeletedAt(Instant.now());
        tripRepository.save(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> getPublicTrips(Pageable pageable) {
        return tripRepository.findPublicTrips(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> getMyTrips(User owner, Pageable pageable) {
        return tripRepository.findByOwnerAndDeletedAtNull(owner, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID tripId, User requester) {
        Trip trip = findActiveOrThrow(tripId);

        boolean isOwner = trip.getOwner().getId().equals(requester.getId());
        if (!isOwner && trip.getVisibility() == Visibility.PRIVATE) {
            throw new AccessDeniedException("No tens permisos per veure aquest viatge");
        }

        return toResponse(trip);
    }

    @Transactional(readOnly = true)
    public Page<TripResponse> searchByDestination(String destination, Pageable pageable) {
        return tripRepository.findByDestination(destination, pageable).map(this::toResponse);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    Trip findActiveOrThrow(UUID tripId) {
        return tripRepository.findByIdAndDeletedAtNull(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND", "Viatge no trobat"));
    }

    private void assertOwner(Trip trip, User requester) {
        if (!trip.getOwner().getId().equals(requester.getId())) {
            throw new AccessDeniedException("No tens permisos per modificar aquest viatge");
        }
    }

    TripResponse toResponse(Trip trip) {
        Double avg = ratingRepository.averageScoreByTrip(trip).orElse(null);
        return new TripResponse(
            trip.getId(),
            trip.getTitle(),
            trip.getDescription(),
            trip.getDestination(),
            trip.getStartDate(),
            trip.getEndDate(),
            trip.getVisibility(),
            trip.getStatus(),
            trip.getCoverImageUrl(),
            trip.getOwner().getUsername(),
            avg,
            trip.getCreatedAt(),
            trip.getUpdatedAt()
        );
    }
}
