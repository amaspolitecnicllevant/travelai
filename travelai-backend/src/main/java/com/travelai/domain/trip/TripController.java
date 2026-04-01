package com.travelai.domain.trip;

import com.travelai.domain.auth.User;
import com.travelai.domain.trip.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final ItineraryService itineraryService;
    private final RatingService ratingService;

    // ── Trips ────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tripService.createTrip(request, user));
    }

    @GetMapping("/public")
    public ResponseEntity<Page<TripResponse>> getPublicTrips(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(tripService.getPublicTrips(pageable));
    }

    /**
     * Personalised feed — public trips sorted by rating DESC, date DESC.
     * If authenticated, the current user's own trips are excluded.
     * The requester may be null (anonymous access).
     */
    @GetMapping("/feed")
    public ResponseEntity<Page<TripResponse>> getFeed(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.getFeed(user, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<TripResponse>> getMyTrips(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.getMyTrips(user, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.getTripById(id, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponse> updateTrip(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTripRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.updateTrip(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        tripService.deleteTrip(id, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<TripResponse>> searchTrips(
            @RequestParam String destination,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(tripService.searchByDestination(destination, pageable));
    }

    // ── Trip lifecycle ───────────────────────────────────────────────────────

    @PostMapping("/{id}/publish")
    public ResponseEntity<TripResponse> publishTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.publishTrip(id, user));
    }

    @PostMapping("/{id}/unpublish")
    public ResponseEntity<TripResponse> unpublishTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tripService.unpublishTrip(id, user));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<TripResponse> duplicateTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tripService.duplicateTrip(id, user));
    }

    // ── Itinerary ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/itinerary")
    public ResponseEntity<List<ItineraryResponse>> getItinerary(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(itineraryService.getItinerary(id, user));
    }

    // ── Ratings ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/ratings")
    public ResponseEntity<Void> rateTrip(
            @PathVariable UUID id,
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal User user) {
        ratingService.rateTrip(id, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
