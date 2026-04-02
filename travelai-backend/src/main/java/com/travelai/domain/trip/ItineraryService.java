package com.travelai.domain.trip;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelai.domain.auth.User;
import com.travelai.domain.trip.dto.ItineraryResponse;
import com.travelai.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final TripService tripService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ItineraryResponse saveItinerary(UUID tripId, Integer dayNumber, String contentJson,
                                           boolean generatedByAi, User requester) {
        Trip trip = tripService.findActiveOrThrow(tripId);
        assertOwner(trip, requester);

        Itinerary itinerary = itineraryRepository.findByTripAndDayNumber(trip, dayNumber)
            .map(existing -> {
                existing.setContentJson(contentJson);
                existing.setGeneratedByAi(generatedByAi);
                existing.setVersion(existing.getVersion() + 1);
                return existing;
            })
            .orElseGet(() -> Itinerary.builder()
                .trip(trip)
                .dayNumber(dayNumber)
                .contentJson(contentJson)
                .generatedByAi(generatedByAi)
                .version(1)
                .build());

        return toResponse(itineraryRepository.save(itinerary));
    }

    @Transactional(readOnly = true)
    public List<ItineraryResponse> getItinerary(UUID tripId, User requester) {
        Trip trip = tripService.findActiveOrThrow(tripId);

        boolean isOwner = trip.getOwner().getId().equals(requester.getId());
        if (!isOwner && trip.getVisibility() == Visibility.PRIVATE) {
            throw new AccessDeniedException("No tens permisos per veure l'itinerari d'aquest viatge");
        }

        return itineraryRepository.findByTripOrderByDayNumber(trip)
            .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ItineraryResponse updateItinerary(UUID itineraryId, String contentJson, User requester) {
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
            .orElseThrow(() -> new ResourceNotFoundException("ITINERARY_NOT_FOUND", "Itinerari no trobat"));
        assertOwner(itinerary.getTrip(), requester);

        itinerary.setContentJson(contentJson);
        itinerary.setVersion(itinerary.getVersion() + 1);

        return toResponse(itineraryRepository.save(itinerary));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void assertOwner(Trip trip, User requester) {
        if (!trip.getOwner().getId().equals(requester.getId())) {
            throw new AccessDeniedException("No tens permisos per modificar aquest itinerari");
        }
    }

    private ItineraryResponse toResponse(Itinerary itinerary) {
        String title = null;
        List<ItineraryResponse.DayPlan> plans;

        try {
            // contentJson pot ser {title, activities:[...]} o bé directament [...]
            var node = objectMapper.readTree(itinerary.getContentJson());
            if (node.isObject() && node.has("activities")) {
                title = node.has("title") ? node.get("title").asText() : null;
                plans = objectMapper.readerForListOf(ItineraryResponse.DayPlan.class)
                                    .readValue(node.get("activities"));
            } else {
                plans = objectMapper.readValue(itinerary.getContentJson(),
                            new TypeReference<List<ItineraryResponse.DayPlan>>() {});
            }
        } catch (Exception e) {
            log.warn("No s'ha pogut parsejar el contentJson de l'itinerari: {}", e.getMessage());
            plans = Collections.emptyList();
        }

        return new ItineraryResponse(
            itinerary.getId(),
            itinerary.getDayNumber(),
            itinerary.getDate(),
            title,
            plans,
            itinerary.isGeneratedByAi(),
            itinerary.getVersion(),
            itinerary.getCreatedAt(),
            itinerary.getUpdatedAt()
        );
    }
}
