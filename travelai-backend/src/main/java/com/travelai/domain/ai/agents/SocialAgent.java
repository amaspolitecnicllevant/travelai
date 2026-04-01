package com.travelai.domain.ai.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelai.domain.ai.AiException;
import com.travelai.domain.ai.DayPlan;
import com.travelai.domain.ai.ItineraryParser;
import com.travelai.domain.ai.OllamaService;
import com.travelai.domain.trip.Itinerary;
import com.travelai.domain.trip.ItineraryRepository;
import com.travelai.domain.trip.Rating;
import com.travelai.domain.trip.RatingRepository;
import com.travelai.domain.trip.Trip;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SocialAgent — optimitza l'itinerari d'un viatge tenint en compte
 * les valoracions d'altres viatgers (puntuació mitjana i comentaris).
 * Carrega les ratings de la BD, construeix el context i envia el prompt
 * a Ollama en streaming; quan el flux es completa persisteix els dies optimitzats.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SocialAgent {

    private final OllamaService ollamaService;
    private final ItineraryParser itineraryParser;
    private final ItineraryRepository itineraryRepository;
    private final RatingRepository ratingRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Eres un experto en viajes con acceso a las valoraciones de otros viajeros.
            El itinerario tiene una puntuación media de %.1f/5.
            Optimiza el itinerario teniendo en cuenta el feedback social: \
            mejora las actividades peor valoradas, refuerza las mejor valoradas.
            Devuelve el itinerario completo en JSON.
            SOLO responde con JSON válido, sin texto adicional, sin markdown, sin bloques de código.
            La respuesta empieza con { y acaba con }.
            Formato: {"days": [{"day": 1, "title": "...", "activities": \
            [{"time": "09:00", "name": "...", "description": "...", "duration": "2h", "cost": 0}]}]}
            """;

    /**
     * Optimitza l'itinerari d'un viatge basant-se en el feedback social (ratings i comentaris).
     * Quan el flux es completa, parseja i persisteix els dies optimitzats a la BD.
     *
     * @param trip el viatge a optimitzar
     * @return Flux de chunks de text (SSE payload)
     */
    public Flux<String> optimize(Trip trip) {
        List<Rating> ratings = ratingRepository.findByTrip(trip);
        double avgRating = ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);

        List<Itinerary> existingDays = itineraryRepository.findByTripOrderByDayNumber(trip);
        String currentItinerary = buildCurrentItineraryJson(existingDays);

        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(avgRating);
        String userPrompt = buildUserPrompt(currentItinerary, ratings, avgRating);

        log.info("SocialAgent: optimitzant itinerari del trip {} — avg rating: {}, {} valoracions",
                trip.getId(), String.format("%.1f", avgRating), ratings.size());

        StringBuilder fullResponse = new StringBuilder();

        return ollamaService.streamChat(systemPrompt, userPrompt)
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> persistOptimizedItinerary(trip, fullResponse.toString()))
                .doOnError(e -> log.error("SocialAgent: error optimitzant itinerari del trip {}: {}",
                        trip.getId(), e.getMessage()));
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String buildCurrentItineraryJson(List<Itinerary> days) {
        if (days.isEmpty()) {
            return "{\"days\": []}";
        }
        StringBuilder sb = new StringBuilder("{\"days\": [");
        for (int i = 0; i < days.size(); i++) {
            Itinerary day = days.get(i);
            sb.append("{\"day\": ").append(day.getDayNumber())
              .append(", \"activities\": ").append(day.getContentJson()).append("}");
            if (i < days.size() - 1) sb.append(", ");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String buildUserPrompt(String currentItinerary, List<Rating> ratings, double avgRating) {
        StringBuilder sb = new StringBuilder();
        sb.append("Itinerario actual:\n").append(currentItinerary).append("\n\n");
        sb.append("Puntuación media: ").append(String.format("%.1f", avgRating)).append("/5\n");
        sb.append("Total de valoraciones: ").append(ratings.size()).append("\n");

        List<String> comments = ratings.stream()
                .filter(r -> r.getComment() != null && !r.getComment().isBlank())
                .map(r -> "- [" + r.getScore() + "/5] " + r.getComment())
                .collect(Collectors.toList());

        if (!comments.isEmpty()) {
            sb.append("\nComentarios de viajeros:\n");
            comments.forEach(c -> sb.append(c).append("\n"));
        }

        sb.append("\nOptimiza el itinerario basándote en este feedback y devuelve el itinerario COMPLETO en JSON.");
        return sb.toString();
    }

    private void persistOptimizedItinerary(Trip trip, String fullJson) {
        try {
            List<DayPlan> dayPlans = itineraryParser.parse(fullJson);
            LocalDate baseDate = trip.getStartDate() != null ? trip.getStartDate() : LocalDate.now();

            for (int i = 0; i < dayPlans.size(); i++) {
                DayPlan plan = dayPlans.get(i);
                int dayNumber = plan.dayNumber() > 0 ? plan.dayNumber() : (i + 1);
                LocalDate dayDate = baseDate.plusDays(dayNumber - 1L);

                String contentJson = objectMapper.writeValueAsString(plan.activities());

                Itinerary itinerary = itineraryRepository
                        .findByTripAndDayNumber(trip, dayNumber)
                        .map(existing -> {
                            existing.setContentJson(contentJson);
                            existing.setGeneratedByAi(true);
                            existing.setVersion(existing.getVersion() + 1);
                            existing.setDate(dayDate);
                            return existing;
                        })
                        .orElseGet(() -> Itinerary.builder()
                                .trip(trip)
                                .dayNumber(dayNumber)
                                .date(dayDate)
                                .contentJson(contentJson)
                                .generatedByAi(true)
                                .version(1)
                                .build());

                itineraryRepository.save(itinerary);
            }

            log.info("SocialAgent: {} dies optimitzats per trip {}", dayPlans.size(), trip.getId());
        } catch (Exception e) {
            log.error("SocialAgent: error persistint itinerari optimitzat per trip {}: {}",
                    trip.getId(), e.getMessage());
            throw new AiException("Error desant l'itinerari optimitzat: " + e.getMessage());
        }
    }
}
