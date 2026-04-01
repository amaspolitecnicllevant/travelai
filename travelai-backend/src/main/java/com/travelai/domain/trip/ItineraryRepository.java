package com.travelai.domain.trip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    List<Itinerary> findByTripOrderByDayNumber(Trip trip);

    Optional<Itinerary> findByTripAndDayNumber(Trip trip, Integer dayNumber);
}
